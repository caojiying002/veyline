package com.veyline.app.feature.merchant.presentation.list

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.veyline.app.feature.merchant.data.MerchantRepository
import com.veyline.app.feature.merchant.domain.model.MerchantProvince
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
 * 这里只保存会长期影响页面的城市筛选条件，以及首次加载是否已经真正发起。列表数据本身
 * 通过 [MerchantListViewModel.merchants] 以 [PagingData] 流的形式单独下发，不进入本状态；
 * 加载中、错误、重试、末页等分页状态由 UI 层通过 Paging 的 `CombinedLoadStates` 观察，
 * 不在这里重复维护。
 *
 * @property selectedProvince 当前选中的筛选地区；`null` 表示不限制地区、查询全部。
 * @property hasTriggeredInitialLoad 是否已经真正发起过首次加载请求。在
 * [MerchantListViewModel.merchants] 尚未开始收集任何分页数据之前，Paging 3 的
 * `LazyPagingItems.loadState.refresh` 默认值就是 `Loading`——如果 UI 只依赖
 * `CombinedLoadStates` 判断状态，会把"根本还没请求过"误判成"正在加载中"，提前显示
 * 一次全屏加载态。这个字段专门用来让 UI 区分"尚未发起过任何请求，应保持空白"和
 * "请求已经在进行"这两种情况，跟具体由什么机制把它置为 `true` 无关。
 */
data class MerchantListUiState(
    val selectedProvince: MerchantProvince? = null,
    val hasTriggeredInitialLoad: Boolean = false,
) {
    /** 当前是否限制为具体地区；`false` 表示查询全部地区。 */
    val hasProvinceFilter: Boolean
        get() = selectedProvince != null
}

/**
 * 商家列表页面支持的操作。
 *
 * [InitialLoad] 由 UI 在页面首次可见时发送，并在同一个 ViewModel 实例内保证幂等：只有
 * 收到该操作后，[MerchantListViewModel.merchants] 才会真正向 Repository 请求分页数据。
 *
 * [SelectProvince] 与 [SelectAllProvinces] 用于切换地区筛选，允许在 [InitialLoad] 之前发送；
 * 此时只更新筛选条件，等首次加载被触发时再据此发起首个请求。
 */
sealed interface MerchantListAction {

    /** 页面首次可见，请求开始加载商家列表；重复发送不会重复触发加载。 */
    @Deprecated(
        message = "首次加载时机的设计尚未最终确定，取决于后续首页 5 个 Tab 容器" +
            "和省份筛选 DataStore 持久化的具体接入方式；当前实现暂不发送本 Action，" +
            "等场景明确后再决定去留",
    )
    data object InitialLoad : MerchantListAction

    /**
     * 清除具体地区筛选并查询全部地区。
     *
     * 使用独立 Action，而不是允许 [SelectProvince] 接收 `null`，使调用方能够直接表达用户操作，
     * 也避免把“全部城市”与缺少参数混为一谈。
     */
    data object SelectAllProvinces : MerchantListAction

    /**
     * 将商家列表筛选为指定地区。
     *
     * @property province 用户选中的地区；与当前选中地区相同时不会触发重新加载。
     */
    data class SelectProvince(
        val province: MerchantProvince,
    ) : MerchantListAction
}

/**
 * 管理商家列表的分页数据源与地区筛选状态。
 *
 * 与城市选择页不同，本页的列表数据完全交给 Paging 3，ViewModel 只负责维护地区筛选条件，
 * 并在筛选变化时通过 [merchants] 切换到新的分页数据源；[merchants] 一旦被订阅就会立即
 * 按当前筛选条件发起请求，不等待任何"页面可见"信号。
 *
 * 列表的加载中 / 错误 / 重试 / 末页等状态由 UI 层通过 Paging 的 `CombinedLoadStates`
 * 观察，不在 [uiState] 中重复维护；[MerchantListUiState.hasTriggeredInitialLoad] 是
 * 唯一的例外，用来弥补 Paging 3 `refresh` 状态在数据源尚未开始收集时默认就是 `Loading`
 * 这一问题，具体原因见该属性的文档。
 */
@HiltViewModel
class MerchantListViewModel @Inject constructor(
    private val merchantRepository: MerchantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MerchantListUiState())
    val uiState: StateFlow<MerchantListUiState> = _uiState.asStateFlow()

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
    @Deprecated(message = "同 MerchantListAction.InitialLoad，去留待后续场景明确")
    private val initialLoadSignal = MutableSharedFlow<Unit>(replay = 1)

    /**
     * 商家列表分页数据流。
     *
     * 管道分两段：
     * 1. 观察 [uiState] 中的地区代码并 [distinctUntilChanged]：无关的状态变化、或重复选中
     *    同一地区都不会重建数据源。
     * 2. 每个不同的地区代码用 `flatMapLatest` 切换到 [MerchantRepository.getMerchants] 新建的
     *    分页流，切换筛选时自动取消上一个数据源；切换前顺带把
     *    [MerchantListUiState.hasTriggeredInitialLoad] 标记为 `true`。
     *
     * 本流一旦被订阅（例如 UI 首次调用 `collectAsLazyPagingItems()`）就会立即按当前筛选
     * 条件发起请求，不等待任何外部触发信号。
     *
     * `cachedIn(viewModelScope)` 必须是最后一个操作符：它把 [PagingData] 缓存在 ViewModel
     * 作用域内，使配置变更后无需重新加载，并允许多个收集者共享同一份分页数据。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val merchants: Flow<PagingData<MerchantSummary>> =
        uiState
            .map { it.selectedProvince?.code }
            .distinctUntilChanged()
            .flatMapLatest { cityCode ->
                _uiState.update {
                    if (it.hasTriggeredInitialLoad) it
                    else it.copy(hasTriggeredInitialLoad = true)
                }

                merchantRepository.getMerchants(cityCode)
            }.cachedIn(viewModelScope)

    @MainThread
    fun onAction(action: MerchantListAction) {
        when (action) {
            MerchantListAction.InitialLoad -> requestInitialLoad()
            MerchantListAction.SelectAllProvinces -> selectAllProvinces()
            is MerchantListAction.SelectProvince -> selectProvince(action.province)
        }
    }

    @Deprecated(message = "同 MerchantListAction.InitialLoad，去留待后续场景明确")
    private fun requestInitialLoad() {
        if (uiState.value.hasTriggeredInitialLoad) {
            return
        }

        _uiState.update {
            it.copy(hasTriggeredInitialLoad = true)
        }
        // 有 replay 缓冲，tryEmit 必定成功；即使此刻还没有收集者，信号也会重放给后续订阅
        initialLoadSignal.tryEmit(Unit)
    }

    private fun selectAllProvinces() {
        _uiState.update { currentState ->
            // 已是“全部城市”时返回原状态，省去一次 copy 分配（StateFlow 本身也会按值去重）
            if (currentState.selectedProvince == null) {
                currentState
            } else {
                currentState.copy(selectedProvince = null)
            }
        }
    }

    private fun selectProvince(province: MerchantProvince) {
        _uiState.update { currentState ->
            // 选中的仍是当前地区时返回原状态，省去一次 copy 分配（StateFlow 本身也会按值去重）
            if (currentState.selectedProvince == province) {
                currentState
            } else {
                currentState.copy(selectedProvince = province)
            }
        }
    }
}
