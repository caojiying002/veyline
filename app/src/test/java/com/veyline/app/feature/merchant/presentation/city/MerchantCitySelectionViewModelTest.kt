package com.veyline.app.feature.merchant.presentation.city

import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.feature.merchant.data.MerchantRepository
import com.veyline.app.feature.merchant.data.remote.MerchantApiService
import com.veyline.app.feature.merchant.data.remote.model.MerchantCityDto
import com.veyline.app.feature.merchant.domain.model.MerchantCity
import com.veyline.app.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import kotlin.test.assertEquals

/**
 * 验证 [MerchantCitySelectionViewModel] 对页面操作的处理和 UI 状态转换。
 *
 * 测试使用真实 [MerchantRepository] 和可控的 [MerchantApiService] 实现，既覆盖 ViewModel 的状态管理，
 * 也保留从接口响应到领域模型的实际调用链。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MerchantCitySelectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** 验证创建 ViewModel 时只提供初始状态，不会自动请求城市数据。 */
    @Test
    fun `create ViewModel does not request cities`() {
        var requestCount = 0
        val apiService = object : MerchantApiService {
            override suspend fun getMerchantCities():
                    Response<ApiResponse<List<MerchantCityDto>>> {
                requestCount++

                return Response.success(
                    ApiResponse(
                        code = ApiResponse.CODE_SUCCESS,
                        msg = "success",
                        data = emptyList(),
                    ),
                )
            }
        }
        val repository = MerchantRepository(apiService)

        val viewModel = MerchantCitySelectionViewModel(repository)

        assertEquals(MerchantCitySelectionUiState(), viewModel.uiState.value)
        assertEquals(0, requestCount)
    }

    /** 验证首次加载成功后更新城市列表并结束加载状态。 */
    @Test
    fun `InitialLoad with successful response updates cities`() = runTest {
        val apiService = object : MerchantApiService {
            override suspend fun getMerchantCities():
                    Response<ApiResponse<List<MerchantCityDto>>> =
                Response.success(
                    ApiResponse(
                        code = ApiResponse.CODE_SUCCESS,
                        msg = "success",
                        data = listOf(
                            MerchantCityDto(
                                code = "code-a",
                                name = "城市甲",
                            ),
                        ),
                    ),
                )
        }
        val repository = MerchantRepository(apiService)
        val viewModel = MerchantCitySelectionViewModel(repository)
        val expected = MerchantCitySelectionUiState(
            cities = listOf(
                MerchantCity(
                    code = "code-a",
                    name = "城市甲",
                ),
            ),
            isLoading = false,
            error = null,
        )

        viewModel.onAction(MerchantCitySelectionAction.InitialLoad)

        // 执行测试调度器中排队的 viewModelScope 任务，等待城市加载和状态更新全部完成。
        advanceUntilIdle()

        assertEquals(expected, viewModel.uiState.value)
    }
}
