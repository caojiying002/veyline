package com.veyline.app.feature.merchant.presentation.city

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.feature.merchant.data.MerchantRepository
import com.veyline.app.feature.merchant.domain.model.MerchantCity
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
data class MerchantCitySelectionUiState(
    val cities: List<MerchantCity> = emptyList(),
    val isLoading: Boolean = true,
    val error: UiError? = null,
)

/**
 * 商家城市选择页面支持的操作。
 *
 * [InitialLoad] 由 UI 首次进入页面时发送，并在同一个 ViewModel 实例中保证幂等；加载失败后的
 * 主动重试应发送 [Retry]。
 */
sealed interface MerchantCitySelectionAction {

    data object InitialLoad : MerchantCitySelectionAction

    data object Retry : MerchantCitySelectionAction
}

/**
 * 管理商家城市列表的加载过程和页面状态。
 *
 * 首次加载由 UI 通过 [MerchantCitySelectionAction.InitialLoad] 明确触发，而不是在初始化时自动执行。
 */
@HiltViewModel
class MerchantCitySelectionViewModel @Inject constructor(
    private val merchantRepository: MerchantRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MerchantCitySelectionUiState())
    val uiState: StateFlow<MerchantCitySelectionUiState> = _uiState.asStateFlow()

    /** 防止页面重组等原因重复触发首次加载；加载失败后也不会自动重置。 */
    private var hasRequestedInitialLoad = false

    /** 防止城市加载尚未完成时再次发起相同请求。 */
    private var loadCitiesJob: Job? = null

    fun onAction(action: MerchantCitySelectionAction) {
        when (action) {
            MerchantCitySelectionAction.InitialLoad -> requestInitialLoad()
            MerchantCitySelectionAction.Retry -> loadCities()
        }
    }

    private fun requestInitialLoad() {
        if (hasRequestedInitialLoad) {
            return
        }

        hasRequestedInitialLoad = true
        loadCities()
    }

    private fun loadCities() {
        if (loadCitiesJob?.isActive == true) {
            return
        }

        loadCitiesJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, error = null)
            }

            when (val result = merchantRepository.getMerchantCities()) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            cities = result.data,
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
