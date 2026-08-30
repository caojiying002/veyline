package com.veyline.app.feature.merchant.presentation.list

import androidx.paging.PagingData
import com.veyline.app.feature.merchant.data.MerchantRepository
import com.veyline.app.test.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

/**
 * 验证 [MerchantListViewModel] 对分页加载触发时机和城市筛选的控制。
 *
 * 测试使用 MockK 隔离 [MerchantRepository]，只关注 ViewModel 何时、以什么筛选条件向
 * Repository 建立分页流；Repository 的分页配置、网络调用与模型转换由其独立测试覆盖。
 *
 * [MerchantListViewModel.merchants] 需要持续收集才会驱动上游，因此各用例统一在
 * `backgroundScope` 中收集该流（随测试结束自动取消），并在 `runCurrent()` 让收集协程
 * 跑起来之后再校验对 Repository 的调用。
 */
@Suppress("UnusedFlow")
@OptIn(ExperimentalCoroutinesApi::class)
class MerchantListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** 验证仅订阅 [MerchantListViewModel.merchants]、但未发送 InitialLoad 时不会请求分页数据。 */
    @Test
    fun `collect merchants before InitialLoad does not request merchants`() = runTest {
        val repository = mockk<MerchantRepository>()
        val viewModel = MerchantListViewModel(repository)

        // merchants 是需要持续收集的冷流，正常 launch 会让 runTest 因“存在未结束的协程”而失败。
        // backgroundScope 的协程会在测试体结束时自动取消，适合承载这类永不完成的收集。
        backgroundScope.launch {
            viewModel.merchants.collect()
        }
        // MainDispatcherRule 使用 StandardTestDispatcher，上面 launch 的收集协程不会立即运行。
        // runCurrent() 执行当前已排队的任务，让收集真正启动并有机会触达 Repository，
        // 使随后的 verify(exactly = 0) 是有意义的断言，而不是因为收集尚未开始而空过。
        runCurrent()

        verify(exactly = 0) {
            repository.getMerchants(any())
        }
    }

    /**
     * 验证在开始收集 [MerchantListViewModel.merchants] 之前发送 InitialLoad，之后的收集
     * 仍会以「全部城市」（`cityCode = null`）建立一次分页流。
     *
     * 这依赖 `initialLoadSignal` 的 `replay = 1`：信号先于订阅发出时会被缓冲，待收集者
     * 订阅后重放，加载不会因为“信号早于收集”而丢失。
     */
    @Test
    fun `InitialLoad before collection requests merchants for all cities`() = runTest {
        val repository = mockk<MerchantRepository>()
        every {
            repository.getMerchants(cityCode = null)
        } returns flowOf(
            PagingData.empty()
        )

        val viewModel = MerchantListViewModel(repository)
        // 先发送 InitialLoad：此时 merchants 尚未被收集，信号进入 replay 缓冲
        viewModel.onAction(MerchantListAction.InitialLoad)

        // 再开始收集：订阅时重放到缓冲的信号，管道据此建立分页流
        backgroundScope.launch {
            viewModel.merchants.collect()
        }
        // 推进调度器，让上面的收集协程真正启动并触达 Repository
        runCurrent()

        verify(exactly = 1) {
            repository.getMerchants(cityCode = null)
        }
    }

    /** 验证首次加载前选择城市时，分页流使用最新的城市代码。 */
    @Ignore("待补充首次加载前选择城市测试")
    @Test
    fun `SelectCity before InitialLoad requests merchants for selected city`() = runTest {
    }

    /** 验证分页流启动后选择其他城市时，切换到新城市的分页数据。 */
    @Ignore("待补充加载后切换城市测试")
    @Test
    fun `SelectCity after InitialLoad switches merchant flow`() = runTest {
    }

    /** 验证重复发送首次加载操作时只建立一次商家分页流。 */
    @Ignore("待补充重复首次加载测试")
    @Test
    fun `repeated InitialLoad requests merchants once`() = runTest {
    }
}
