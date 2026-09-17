package com.veyline.app.feature.merchant.presentation.detail

import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.merchant.data.MerchantRepository
import com.veyline.app.feature.merchant.domain.model.MerchantDetail
import com.veyline.app.test.MainDispatcherRule
import com.veyline.app.ui.error.UiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * 验证 [MerchantDetailViewModel] 的首次加载控制、详情刷新和 UI 状态转换。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MerchantDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** 验证首次加载需要显式触发，重复触发只请求一次，并在成功后更新详情状态。 */
    @Test
    fun initialLoad_whenTriggeredRepeatedly_requestsOnceAndReturnsDetail() = runTest {
        val merchant = MerchantDetail(
            id = "merchant-a",
            name = "商家甲",
            provinceCode = "province-a",
            imageUrls = emptyList(),
            description = "商家详情",
            contact = null,
        )

        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantDetail("merchant-a")
        } returns ApiResult.Success(merchant)

        val viewModel = MerchantDetailViewModel(
            merchantId = "merchant-a",
            repository = repository,
        )

        // 创建 ViewModel 不会自动请求数据
        val initialState = MerchantDetailUiState()
        advanceUntilIdle()
        assertEquals(initialState, viewModel.uiState.value)
        coVerify(exactly = 0) {
            repository.getMerchantDetail(any())
        }

        // 重复发送 InitialLoad 只请求一次
        val loadedState = MerchantDetailUiState(
            merchant = merchant,
            isLoading = false,
            error = null,
        )
        viewModel.onAction(MerchantDetailAction.InitialLoad)
        viewModel.onAction(MerchantDetailAction.InitialLoad)
        advanceUntilIdle()
        assertEquals(loadedState, viewModel.uiState.value)
        coVerify(exactly = 1) {
            repository.getMerchantDetail(
                merchantId = "merchant-a",
            )
        }
    }

    /** 验证首次加载失败时结束加载，并在无内容状态下返回对应的 UI 错误。 */
    @Test
    fun initialLoad_withFailure_returnsErrorWithoutContent() = runTest {
        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantDetail(
                merchantId = "merchant-a",
            )
        } returns ApiResult.Failure.Network(
            IOException("network unavailable"),
        )
        val viewModel = MerchantDetailViewModel(
            merchantId = "merchant-a",
            repository = repository,
        )

        val expected = MerchantDetailUiState(
            merchant = null,
            isLoading = false,
            error = UiError.Connection,
        )
        viewModel.onAction(MerchantDetailAction.InitialLoad)
        advanceUntilIdle()
        assertEquals(expected, viewModel.uiState.value)
    }

    /** 验证刷新期间保留已有内容，阻止重复刷新，并在成功后替换详情。 */
    @Test
    fun refresh_withSuccessfulResponse_keepsContentWhileLoadingAndReplacesDetail() = runTest {
        val initialMerchant = MerchantDetail(
            id = "merchant-a",
            name = "商家甲",
            provinceCode = "province-a",
            imageUrls = emptyList(),
            description = "原商家详情",
            contact = null,
        )
        val refreshedMerchant = initialMerchant.copy(
            description = "更新后的商家详情",
        )

        var requestCount = 0
        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantDetail(
                merchantId = "merchant-a",
            )
        } coAnswers {
            requestCount++

            if (requestCount == 1) {
                ApiResult.Success(initialMerchant)
            } else {
                delay(100.milliseconds)
                ApiResult.Success(refreshedMerchant)
            }
        }

        val viewModel = MerchantDetailViewModel(
            merchantId = "merchant-a",
            repository = repository,
        )
        viewModel.onAction(MerchantDetailAction.InitialLoad)
        advanceUntilIdle()
        viewModel.onAction(MerchantDetailAction.Refresh)
        runCurrent()
        
        val refreshingState = MerchantDetailUiState(
            merchant = initialMerchant,
            isLoading = true,
            error = null,
        )
        assertEquals(refreshingState, viewModel.uiState.value)
        assertTrue(viewModel.uiState.value.isRefreshing)

        // 刷新请求进行中，重复刷新应该被 loadDetailJob 拦截
        viewModel.onAction(MerchantDetailAction.Refresh)
        advanceUntilIdle()

        val finalState = MerchantDetailUiState(
            merchant = refreshedMerchant,
            isLoading = false,
            error = null,
        )
        assertEquals(finalState, viewModel.uiState.value)

        // 重复刷新没有产生第三次请求
        coVerify(exactly = 2) {
            repository.getMerchantDetail(
                merchantId = "merchant-a",
            )
        }
    }

    /** 验证刷新失败时保留已有内容，并记录对应的 UI 错误。 */
    @Test
    fun refresh_withFailure_keepsExistingContentAndReturnsError() = runTest {
        val merchant = MerchantDetail(
            id = "merchant-a",
            name = "商家甲",
            provinceCode = "province-a",
            imageUrls = emptyList(),
            description = "商家详情",
            contact = null,
        )
        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantDetail(
                merchantId = "merchant-a",
            )
        } returnsMany listOf(
            ApiResult.Success(merchant),
            ApiResult.Failure.Network(
                IOException("network unavailable"),
            ),
        )

        val viewModel = MerchantDetailViewModel(
            merchantId = "merchant-a",
            repository = repository,
        )
        viewModel.onAction(MerchantDetailAction.InitialLoad)
        advanceUntilIdle()
        viewModel.onAction(MerchantDetailAction.Refresh)
        advanceUntilIdle()

        val expected = MerchantDetailUiState(
            merchant = merchant,
            isLoading = false,
            error = UiError.Connection,
        )
        assertEquals(expected, viewModel.uiState.value)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }
}
