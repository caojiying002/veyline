package com.veyline.app.feature.merchant.presentation.city

import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.feature.merchant.data.MerchantRepository
import com.veyline.app.feature.merchant.domain.model.MerchantCity
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
 * 验证 [MerchantCitySelectionViewModel] 对页面操作的处理和 UI 状态转换。
 *
 * 测试使用 MockK 隔离 [MerchantRepository]，使测试只关注 ViewModel 的首次加载控制、重试行为和
 * 状态转换；Repository 的网络调用、模型转换与缓存行为由其独立测试覆盖。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MerchantCitySelectionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** 验证创建 ViewModel 时只提供初始状态，不会自动请求城市数据。 */
    @Test
    fun `create ViewModel does not request cities`() = runTest {
        val repository = mockk<MerchantRepository>()
        val viewModel = MerchantCitySelectionViewModel(repository)

        // 推进 testScheduler 直到不再有任务待执行，避免“未发起请求”的断言因任务尚未运行而错误通过。
        advanceUntilIdle()

        assertEquals(MerchantCitySelectionUiState(), viewModel.uiState.value)
        coVerify(exactly = 0) {
            repository.getMerchantCities()
        }
    }

    /** 验证首次加载成功后更新城市列表并结束加载状态。 */
    @Test
    fun `InitialLoad with successful response updates cities`() = runTest {
        val cities = listOf(
            MerchantCity(
                code = "code-a",
                name = "城市甲",
            ),
        )

        val repository = mockk<MerchantRepository>()

        // 为严格 Mock 配置 suspend 方法的返回值，不执行 Repository 的真实实现。
        coEvery {
            repository.getMerchantCities()
        } returns ApiResult.Success(cities)

        val viewModel = MerchantCitySelectionViewModel(repository)
        val expected = MerchantCitySelectionUiState(
            cities = cities,
            isLoading = false,
            error = null,
        )

        viewModel.onAction(MerchantCitySelectionAction.InitialLoad)
        // 执行 testScheduler 中排队的 viewModelScope 任务，等待城市加载和状态更新全部完成。
        advanceUntilIdle()

        assertEquals(expected, viewModel.uiState.value)
    }

    /** 验证重复发送首次加载操作时只请求一次城市数据。 */
    @Test
    fun `repeated InitialLoad requests cities once`() = runTest {
        val repository = mockk<MerchantRepository>()

        coEvery {
            repository.getMerchantCities()
        } returns ApiResult.Success(emptyList())

        val viewModel = MerchantCitySelectionViewModel(repository)

        viewModel.onAction(MerchantCitySelectionAction.InitialLoad)
        viewModel.onAction(MerchantCitySelectionAction.InitialLoad)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.getMerchantCities()
        }
    }

    /** 验证首次加载返回空列表时结束加载，并保持无错误的空数据状态。 */
    @Test
    fun `InitialLoad with empty cities updates empty state`() = runTest {
        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantCities()
        } returns ApiResult.Success(emptyList())

        val viewModel = MerchantCitySelectionViewModel(repository)
        val expected = MerchantCitySelectionUiState(
            cities = emptyList(),
            isLoading = false,
            error = null,
        )

        viewModel.onAction(MerchantCitySelectionAction.InitialLoad)
        advanceUntilIdle()

        assertEquals(expected, viewModel.uiState.value)
    }

    /** 验证网络连接失败时结束加载，并转换为连接类 UI 错误。 */
    @Test
    fun `InitialLoad with Network failure updates Connection error`() = runTest {
        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantCities()
        } returns ApiResult.Failure.Network(
            IOException("test network failure"),
        )

        val viewModel = MerchantCitySelectionViewModel(repository)
        val expected = MerchantCitySelectionUiState(
            cities = emptyList(),
            isLoading = false,
            error = UiError.Connection,
        )

        viewModel.onAction(MerchantCitySelectionAction.InitialLoad)
        advanceUntilIdle()

        assertEquals(expected, viewModel.uiState.value)
    }

    /** 验证普通业务失败时结束加载，并转换为技术类 UI 错误。 */
    @Test
    fun `InitialLoad with Business failure updates Technical error`() = runTest {
        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantCities()
        } returns ApiResult.Failure.Business(
            code = 1000,
            message = "business failed",
        )

        val viewModel = MerchantCitySelectionViewModel(repository)
        val expected = MerchantCitySelectionUiState(
            cities = emptyList(),
            isLoading = false,
            error = UiError.Technical,
        )

        viewModel.onAction(MerchantCitySelectionAction.InitialLoad)
        advanceUntilIdle()

        assertEquals(expected, viewModel.uiState.value)
    }

    /** 验证首次加载失败后用户主动重试，可以重新请求并恢复为成功状态。 */
    @Test
    fun `Retry after failed InitialLoad updates successful state`() = runTest {
        val cities = listOf(
            MerchantCity(
                code = "code-a",
                name = "城市甲",
            ),
        )

        val repository = mockk<MerchantRepository>()
        coEvery {
            repository.getMerchantCities()
        } returnsMany listOf(
            ApiResult.Failure.Network(IOException("test network failure")),
            ApiResult.Success(cities),
        )
        val viewModel = MerchantCitySelectionViewModel(repository)

        // 首次加载失败
        viewModel.onAction(MerchantCitySelectionAction.InitialLoad)
        advanceUntilIdle()
        assertEquals(
            MerchantCitySelectionUiState(
                cities = emptyList(),
                isLoading = false,
                error = UiError.Connection,
            ),
            viewModel.uiState.value,
        )

        // 用户主动重试后加载成功
        viewModel.onAction(MerchantCitySelectionAction.Retry)
        advanceUntilIdle()
        assertEquals(
            MerchantCitySelectionUiState(
                cities = cities,
                isLoading = false,
                error = null,
            ),
            viewModel.uiState.value,
        )

        // 确认重试确实再次调用了 Repository
        coVerify(exactly = 2) {
            repository.getMerchantCities()
        }
    }

    /** 验证城市加载尚未完成时发送重试，不会发起并发请求。 */
    @Ignore("待补充加载期间重试的并发请求测试")
    @Test
    fun `Retry while loading requests cities once`() = runTest {

    }
}
