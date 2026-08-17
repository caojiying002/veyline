package com.veyline.app.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * 在单元测试期间用测试调度器替换 Main Dispatcher。
 *
 * ViewModel 的 viewModelScope 默认使用 Main Dispatcher，而本地 JVM 测试没有 Android 主线程。
 * 该规则会在每个测试开始前完成替换，并在测试结束后恢复原有的 Main Dispatcher。
 *
 * @property testDispatcher 执行和控制 Main Dispatcher 中协程任务的测试调度器。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
