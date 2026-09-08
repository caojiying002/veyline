package com.veyline.app.feature.merchant.presentation.province

import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.merchant.data.MerchantRepository
import com.veyline.app.feature.merchant.domain.model.MerchantProvince
import com.veyline.app.test.MainDispatcherRule
import com.veyline.app.ui.error.UiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

/**
 * 验证 [MerchantProvinceSelectionViewModel] 对页面操作的处理和 UI 状态转换。
 *
 * 测试使用 MockK 隔离 [MerchantRepository]，使测试只关注 ViewModel 的首次加载控制、重试行为和
 * 状态转换；Repository 的网络调用、模型转换与缓存行为由其独立测试覆盖。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MerchantProvinceSelectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** 验证创建 ViewModel 时只提供初始状态，不会自动请求地区数据。 */
    @Test
    fun createViewModel_withoutInitialLoad_doesNotRequestProvinces() = runTest {
        val repository = mockk<MerchantRepository>()
        val viewModel = MerchantProvinceSelectionViewModel(repository)

        // 推进 testScheduler 直到不再有任务待执行，避免“未发起请求”的断言因任务尚未运行而错误通过。
        advanceUntilIdle()

        assertEquals(MerchantProvinceSelectionUiState(), viewModel.uiState.value)
        coVerify(exactly = 0) {
            repository.getMerchantProvinces()
        }
    }

    /** 验证首次加载成功后更新地区列表并结束加载状态。 */
    @Test
    fun initialLoad_withSuccessfulResponse_updatesProvinces() = runTest {
        val provinces = listOf(
            MerchantProvince(
                code = "code-a",
                name = "省份甲",
            ),
        )

        val repository = mockk<MerchantRepository>()

        // 为严格 Mock 配置 suspend 方法的返回值，不执行 Repository 的真实实现。
        coEvery {
            repository.getMerchantProvinces()
        } returns ApiResult.Success(provinces)

        val viewModel = MerchantProvinceSelectionViewModel(repository)
        val expected = MerchantProvinceSelectionUiState(
            provinces = provinces,
            isLoading = false,
            error = null,
        )

        viewModel.onAction(MerchantProvinceSelectionAction.InitialLoad)
        // 执行 testScheduler 中排队的 viewModelScope 任务，等待地区加载和状态更新全部完成。
        advanceUntilIdle()

        assertEquals(expected, viewModel.uiState.value)
    }

    /** 验证重复发送首次加载操作时只请求一次地区数据。 */
    @Test
    fun initialLoad_whenRepeated_requestsProvincesOnce() = runTest {
        val repository = mockk<MerchantRepository>()

        coEvery {
            repository.getMerchantProvinces()
        } returns ApiResult.Success(emptyList())

        val viewModel = MerchantProvinceSelectionViewModel(repository)

        viewModel.onAction(MerchantProvinceSelectionAction.InitialLoad)
        viewModel.onAction(MerchantProvinceSelectionAction.InitialLoad)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.getMerchantProvinces()
        }
    }

    /** 验证首次加载返回空列表时结束加载，并保持无错误的空数据状态。 */
    @Test
    fun initialLoad_withEmptyProvinces_updatesEmptyState() = runTest {
        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantProvinces()
        } returns ApiResult.Success(emptyList())

        val viewModel = MerchantProvinceSelectionViewModel(repository)
        val expected = MerchantProvinceSelectionUiState(
            provinces = emptyList(),
            isLoading = false,
            error = null,
        )

        viewModel.onAction(MerchantProvinceSelectionAction.InitialLoad)
        advanceUntilIdle()

        assertEquals(expected, viewModel.uiState.value)
    }

    /** 验证网络连接失败时结束加载，并转换为连接类 UI 错误。 */
    @Test
    fun initialLoad_withNetworkFailure_updatesConnectionError() = runTest {
        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantProvinces()
        } returns ApiResult.Failure.Network(
            IOException("test network failure"),
        )

        val viewModel = MerchantProvinceSelectionViewModel(repository)
        val expected = MerchantProvinceSelectionUiState(
            provinces = emptyList(),
            isLoading = false,
            error = UiError.Connection,
        )

        viewModel.onAction(MerchantProvinceSelectionAction.InitialLoad)
        advanceUntilIdle()

        assertEquals(expected, viewModel.uiState.value)
    }

    /** 验证普通业务失败时结束加载，并转换为技术类 UI 错误。 */
    @Test
    fun initialLoad_withBusinessFailure_updatesTechnicalError() = runTest {
        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantProvinces()
        } returns ApiResult.Failure.Business(
            code = 1000,
            message = "business failed",
        )

        val viewModel = MerchantProvinceSelectionViewModel(repository)
        val expected = MerchantProvinceSelectionUiState(
            provinces = emptyList(),
            isLoading = false,
            error = UiError.Technical,
        )

        viewModel.onAction(MerchantProvinceSelectionAction.InitialLoad)
        advanceUntilIdle()

        assertEquals(expected, viewModel.uiState.value)
    }

    /** 验证首次加载失败后用户主动重试，可以重新请求并恢复为成功状态。 */
    @Test
    fun retry_afterFailedInitialLoad_updatesSuccessfulState() = runTest {
        val provinces = listOf(
            MerchantProvince(
                code = "code-a",
                name = "省份甲",
            ),
        )

        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantProvinces()
        } returnsMany listOf(
            ApiResult.Failure.Network(IOException("test network failure")),
            ApiResult.Success(provinces),
        )
        val viewModel = MerchantProvinceSelectionViewModel(repository)

        // 首次加载失败
        viewModel.onAction(MerchantProvinceSelectionAction.InitialLoad)
        advanceUntilIdle()
        assertEquals(
            MerchantProvinceSelectionUiState(
                provinces = emptyList(),
                isLoading = false,
                error = UiError.Connection,
            ),
            viewModel.uiState.value,
        )

        // 用户主动重试后加载成功
        viewModel.onAction(MerchantProvinceSelectionAction.Retry)
        advanceUntilIdle()
        assertEquals(
            MerchantProvinceSelectionUiState(
                provinces = provinces,
                isLoading = false,
                error = null,
            ),
            viewModel.uiState.value,
        )

        // 确认重试确实再次调用了 Repository
        coVerify(exactly = 2) {
            repository.getMerchantProvinces()
        }
    }

    /** 验证地区加载尚未完成时发送重试，不会发起并发请求。 */
    @Ignore("待补充加载期间重试的并发请求测试")
    @Test
    fun retry_whileLoading_requestsProvincesOnce() = runTest {

    }
}
