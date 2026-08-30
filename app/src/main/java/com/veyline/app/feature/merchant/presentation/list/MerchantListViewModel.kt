package com.veyline.app.feature.merchant.presentation.list

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.veyline.app.feature.merchant.data.MerchantRepository
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import com.veyline.app.feature.merchant.domain.model.MerchantSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * 商家列表页面的可持续 UI 状态。
 *
 * 这里只保存会长期影响页面的城市筛选条件。列表数据本身通过
 * [MerchantListViewModel.merchants] 以 [PagingData] 流的形式单独下发，不进入本状态；
 * 加载中、错误、重试、末页等分页状态由 UI 层通过 Paging 的 `CombinedLoadStates` 观察，
 * 同样不在这里维护。
 *
 * @property selectedCity 当前选中的筛选城市；`null` 表示不限制城市、查询全部。
 */
data class MerchantListUiState(
    val selectedCity: MerchantCity? = null,
) {
    /** 当前是否限制为具体城市；`false` 表示查询全部城市。 */
    val hasCityFilter: Boolean
        get() = selectedCity != null
}

/**
 * 商家列表页面支持的操作。
 *
 * [InitialLoad] 由 UI 在页面首次可见时发送，并在同一个 ViewModel 实例内保证幂等：只有
 * 收到该操作后，[MerchantListViewModel.merchants] 才会真正向 Repository 请求分页数据。
 *
 * [SelectCity] 与 [SelectAllCities] 用于切换城市筛选，允许在 [InitialLoad] 之前发送；
 * 此时只更新筛选条件，等首次加载被触发时再据此发起首个请求。
 */
sealed interface MerchantListAction {

    /** 页面首次可见，请求开始加载商家列表；重复发送不会重复触发加载。 */
    data object InitialLoad : MerchantListAction

    /**
     * 清除具体城市筛选并查询全部城市。
     *
     * 使用独立 Action，而不是允许 [SelectCity] 接收 `null`，使调用方能够直接表达用户操作，
     * 也避免把“全部城市”与缺少参数混为一谈。
     */
    data object SelectAllCities : MerchantListAction

    /**
     * 将商家列表筛选为指定城市。
     *
     * @property city 用户选中的城市；与当前选中城市相同时不会触发重新加载。
     */
    data class SelectCity(
        val city: MerchantCity,
    ) : MerchantListAction
}

/**
 * 管理商家列表的分页加载触发时机与城市筛选状态。
 *
 * 与城市选择页不同，本页的列表数据完全交给 Paging 3，ViewModel 只承担两件事：
 * 1. 控制「首次加载」的触发时机——页面首次可见时，而不是 ViewModel 创建或 [merchants]
 *    被订阅时；
 * 2. 维护城市筛选条件，并在筛选变化时切换到新的分页数据源。
 *
 * 列表的加载中 / 错误 / 重试 / 末页等状态由 UI 层通过 Paging 的 `CombinedLoadStates`
 * 观察，不在 [uiState] 中重复维护。
 */
@HiltViewModel
class MerchantListViewModel @Inject constructor(
    private val merchantRepository: MerchantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MerchantListUiState())
    val uiState: StateFlow<MerchantListUiState> = _uiState.asStateFlow()

    /**
     * 防止页面重组等原因重复触发首次加载；加载失败后也不会自动重置——列表的失败重试由
     * Paging 自带的 `retry()` 负责，不需要再次发送 [MerchantListAction.InitialLoad]。
     */
    private var hasRequestedInitialLoad = false

    // TODO 后续实现城市选择之后可能会改变这里的加载控制逻辑，但仍然要受“页面首次可见时”这一条件的制约

    /**
     * 「页面首次可见」信号。在收到第一个元素之前，[merchants] 不会向 Repository 发起任何请求。
     *
     * 选用 `MutableSharedFlow(replay = 1)` 而不是 `StateFlow<Boolean>`：
     * - 需要表达“尚未发生 / 已发生”的一次性事件，而不是一个总是带初始值的状态；`StateFlow`
     *   的初始值要么会立即触发加载，要么还得在下游额外过滤掉。
     * - `replay = 1` 保证即使 [requestInitialLoad] 在 [merchants] 被订阅之前就发出信号，
     *   之后订阅的收集者仍能重放到该信号并开始加载，事件不会丢失。
     * - 存在重放缓冲时 [MutableSharedFlow.tryEmit] 一定成功，可以在非挂起的 [onAction] 中安全调用。
     */
    private val initialLoadSignal = MutableSharedFlow<Unit>(replay = 1)

    /**
     * 商家列表分页数据流。
     *
     * 管道分三段：
     * 1. [initialLoadSignal]：在收到首次可见信号前不进入下游，页面因此不会“进入即加载”。
     * 2. 观察 [uiState] 中的城市代码并 [distinctUntilChanged]：无关的状态变化、或重复选中
     *    同一城市都不会重建数据源。
     * 3. 每个不同的城市代码用 `flatMapLatest` 切换到 [MerchantRepository.getMerchants] 新建的
     *    分页流，切换筛选时自动取消上一个数据源。
     *
     * `cachedIn(viewModelScope)` 必须是最后一个操作符：它把 [PagingData] 缓存在 ViewModel
     * 作用域内，使配置变更后无需重新加载，并允许多个收集者共享同一份分页数据。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val merchants: Flow<PagingData<MerchantSummary>> =
        initialLoadSignal.flatMapLatest {
            uiState
                .map { it.selectedCity?.code }
                .distinctUntilChanged()
                .flatMapLatest { cityCode ->
                    merchantRepository.getMerchants(cityCode)
                }
        }.cachedIn(viewModelScope)

    @MainThread
    fun onAction(action: MerchantListAction) {
        when (action) {
            MerchantListAction.InitialLoad -> requestInitialLoad()
            MerchantListAction.SelectAllCities -> selectAllCities()
            is MerchantListAction.SelectCity -> selectCity(action.city)
        }
    }

    private fun requestInitialLoad() {
        if (hasRequestedInitialLoad) {
            return
        }

        hasRequestedInitialLoad = true
        // 有 replay 缓冲，tryEmit 必定成功；即使此刻还没有收集者，信号也会重放给后续订阅
        initialLoadSignal.tryEmit(Unit)
    }

    private fun selectAllCities() {
        _uiState.update { currentState ->
            // 已是“全部城市”时返回原状态，省去一次 copy 分配（StateFlow 本身也会按值去重）
            if (currentState.selectedCity == null) {
                currentState
            } else {
                currentState.copy(selectedCity = null)
            }
        }
    }

    private fun selectCity(city: MerchantCity) {
        _uiState.update { currentState ->
            // 选中的仍是当前城市时返回原状态，省去一次 copy 分配（StateFlow 本身也会按值去重）
            if (currentState.selectedCity == city) {
                currentState
            } else {
                currentState.copy(selectedCity = city)
            }
        }
    }
}
