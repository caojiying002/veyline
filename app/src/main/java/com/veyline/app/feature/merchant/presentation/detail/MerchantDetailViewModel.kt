package com.veyline.app.feature.merchant.presentation.detail

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.merchant.data.MerchantRepository
import com.veyline.app.feature.merchant.domain.model.MerchantDetail
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.error.toUiError
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 商家详情页面的可持续 UI 状态。
 *
 * @property merchant 商家详情领域模型；尚未加载成功时为 `null`
 * @property isLoading 是否有加载请求正在进行，涵盖首次加载和刷新两种场景
 * @property error 最近一次请求失败对应的 UI 错误；请求成功后清空
 */
data class MerchantDetailUiState(
    val merchant: MerchantDetail? = null,
    val isLoading: Boolean = false,
    val error: UiError? = null,
) {
    /** 是否已有可展示的商家详情 */
    val hasContent: Boolean
        get() = merchant != null

    /**
     * 是否应按"已有内容时的刷新"展示，而不是全屏加载。
     *
     * [isLoading] 本身不区分首次加载和刷新，由页面结合 [hasContent] 决定具体展示
     * 形式：驱动 `PullToRefreshBox` 的刷新指示器，而不是整页替换成加载态。
     */
    val isRefreshing: Boolean
        get() = isLoading && hasContent
}

/**
 * 商家详情页面支持的操作。
 *
 * [Refresh] 与 [Retry] 目前触发完全相同的加载逻辑，区分成两个 Action 只是为了让调用方
 * （下拉刷新手势、错误态重试按钮）各自表达清晰的用户意图，不代表未来一定会分叉。
 */
sealed interface MerchantDetailAction {

    /** 页面首次可见，请求开始加载商家详情；重复发送不会重复触发加载 */
    data object InitialLoad : MerchantDetailAction

    /** 用户下拉刷新，重新请求商家详情 */
    data object Refresh : MerchantDetailAction

    /** 用户在错误态点击重试，重新请求商家详情 */
    data object Retry : MerchantDetailAction
}

/**
 * 管理商家详情的加载、刷新与首次加载触发时机。
 *
 * [merchantId] 通过 `AssistedInject` 在创建时作为构造参数注入，不依赖 `SavedStateHandle`
 * 或路由参数解析，也不受 `hiltViewModel(key = ...)` 自定义 key 的影响。
 *
 * [MerchantDetailAction.Refresh] 与 [MerchantDetailAction.Retry] 复用同一个
 * [loadDetail]：请求进行中时通过 [loadDetailJob] 拦截重复触发；请求失败时保留已有的
 * [MerchantDetailUiState.merchant] 不清空，避免刷新失败导致已展示的内容消失。
 */
@HiltViewModel(assistedFactory = MerchantDetailViewModel.Factory::class)
class MerchantDetailViewModel @AssistedInject constructor(
    @Assisted private val merchantId: String,
    private val repository: MerchantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MerchantDetailUiState())
    val uiState: StateFlow<MerchantDetailUiState> = _uiState.asStateFlow()

    /** 防止页面重组等原因重复触发首次加载；加载失败后也不会自动重置 */
    private var hasTriggeredInitialLoad = false

    /** 跟踪当前是否有详情请求在进行中，用于拦截重复的刷新/重试触发 */
    private var loadDetailJob: Job? = null

    @MainThread
    fun onAction(action: MerchantDetailAction) {
        when (action) {
            MerchantDetailAction.InitialLoad -> triggerInitialLoad()
            MerchantDetailAction.Refresh,
            MerchantDetailAction.Retry -> loadDetail()
        }
    }

    private fun triggerInitialLoad() {
        if (hasTriggeredInitialLoad) {
            return
        }

        hasTriggeredInitialLoad = true
        loadDetail()
    }

    private fun loadDetail() {
        if (loadDetailJob?.isActive == true) {
            return
        }

        loadDetailJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = repository.getMerchantDetail(merchantId)

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            merchant = result.data,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(isLoading = false, error = result.toUiError())
                    }
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(merchantId: String): MerchantDetailViewModel
    }
}
