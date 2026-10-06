package com.veyline.app.feature.auth.presentation.login

import app.cash.turbine.test
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.auth.data.AuthRepository
import com.veyline.app.test.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** 验证提交时用户名被 trim、密码原样传递，登录成功后发出 LoginSucceeded */
    @Test
    fun onAction_submitWithPaddedName_trimsNameAndEmitsLoginSucceeded() = runTest {
        // Given：使用去除首尾空格的用户名和原样保留空格的密码登录时，Repository 返回成功
        val repository = mockk<AuthRepository>()
        coEvery {
            repository.login(name = "user-a", password = "  password-a  ")
        } returns ApiResult.Success(Unit)

        val viewModel = LoginViewModel(repository)

        viewModel.effect.test {
            // When：提交首尾带空格的用户名和密码
            viewModel.onAction(
                LoginAction.Submit(name = "  user-a  ", password = "  password-a  "),
            )

            // Then：收到登录成功事件
            assertEquals(LoginEffect.LoginSucceeded, awaitItem())
        }

        // 收到 Effect 不代表 submit() 协程已经跑完，推进到彻底结束再读状态，
        // 不依赖状态更新和发送 Effect 在代码里谁写在前面
        advanceUntilIdle()
        assertEquals(LoginUiState(), viewModel.uiState.value)
    }
}
