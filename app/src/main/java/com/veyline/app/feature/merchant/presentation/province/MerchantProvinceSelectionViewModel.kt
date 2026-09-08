package com.veyline.app.feature.merchant.presentation.province

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.merchant.data.MerchantRepository
import com.veyline.app.feature.merchant.domain.model.MerchantProvince
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.error.toUiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 商家城市选择页面的可持续 UI 状态。 */
data class MerchantProvinceSelectionUiState(
    val provinces: List<MerchantProvince> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiError? = null,
) {
    /** 页面是否已有可供展示的城市内容，等价于 `provinces.isNotEmpty()`。 */
    val hasContent: Boolean
        get() = provinces.isNotEmpty()

    /**
     * 是否正在无已有内容的情况下加载，等价于 `isLoading && !hasContent`。
     *
     * 包括首次请求以及首次加载失败后的重新加载。
     */
    val isInitialLoading: Boolean
        get() = isLoading && !hasContent
}

/**
 * 商家城市选择页面支持的操作。
 *
 * [InitialLoad] 由 UI 首次进入页面时发送，并在同一个 ViewModel 实例中保证幂等；加载失败后的
 * 主动重试应发送 [Retry]。
 */
sealed interface MerchantProvinceSelectionAction {

    data object InitialLoad : MerchantProvinceSelectionAction

    data object Retry : MerchantProvinceSelectionAction
}

/**
 * 管理商家城市列表的加载过程和页面状态。
 *
 * 首次加载由 UI 通过 [MerchantProvinceSelectionAction.InitialLoad] 明确触发，而不是在初始化时自动执行。
 */
@HiltViewModel
class MerchantProvinceSelectionViewModel @Inject constructor(
    private val merchantRepository: MerchantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MerchantProvinceSelectionUiState())
    val uiState: StateFlow<MerchantProvinceSelectionUiState> = _uiState.asStateFlow()

    /** 防止页面重组等原因重复触发首次加载；加载失败后也不会自动重置。 */
    private var hasRequestedInitialLoad = false

    /** 防止城市加载尚未完成时再次发起相同请求。 */
    private var loadProvincesJob: Job? = null

    @MainThread
    fun onAction(action: MerchantProvinceSelectionAction) {
        when (action) {
            MerchantProvinceSelectionAction.InitialLoad -> requestInitialLoad()
            MerchantProvinceSelectionAction.Retry -> loadProvinces()
        }
    }

    private fun requestInitialLoad() {
        if (hasRequestedInitialLoad) {
            return
        }

        hasRequestedInitialLoad = true
        loadProvinces()
    }

    private fun loadProvinces() {
        if (loadProvincesJob?.isActive == true) {
            return
        }

        loadProvincesJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            when (val result = merchantRepository.getMerchantProvinces()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            provinces = result.data,
                            isLoading = false,
                            error = null,
                        )
                    }
                }
                is ApiResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.toUiError(),
                        )
                    }
                }
            }
        }
    }
}
