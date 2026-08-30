package com.veyline.app.feature.merchant.presentation.list

import com.veyline.app.feature.merchant.data.MerchantRepository
import com.veyline.app.test.MainDispatcherRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

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
            @Suppress("UnusedFlow")
            repository.getMerchants(any())
        }
    }
}
