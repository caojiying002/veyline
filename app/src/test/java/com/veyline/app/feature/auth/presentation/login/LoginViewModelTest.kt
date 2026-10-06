package com.veyline.app.feature.auth.presentation.login

import app.cash.turbine.test
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.auth.data.AuthRepository
import com.veyline.app.test.MainDispatcherRule
import com.veyline.app.ui.error.UiError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
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

    /** 验证提交期间重复提交不会重复请求，且请求进行中 isSubmitting 为 true */
    @Test
    fun onAction_submitTwice_keepsSubmittingAndRequestsLoginOnce() = runTest {
        // Given：登录结果由测试控制，在主动完成前保持挂起
        val loginResult = CompletableDeferred<ApiResult<Unit>>()
        val repository = mockk<AuthRepository>()
        coEvery {
            repository.login(any(), any())
        } coAnswers {
            loginResult.await()
        }

        val viewModel = LoginViewModel(repository)
        val action = LoginAction.Submit(
            name = "user-a",
            password = "password-a",
        )

        // When：第一次提交，让登录请求执行到等待结果的位置
        viewModel.onAction(action)
        runCurrent()

        // Then：请求进行中，处于提交状态
        assertEquals(LoginUiState(isSubmitting = true), viewModel.uiState.value)

        // When：第一次请求尚未完成，再次提交
        viewModel.onAction(action)
        runCurrent()

        // Then：仍处于提交状态，且只发起一次登录请求
        assertEquals(LoginUiState(isSubmitting = true), viewModel.uiState.value)
        coVerify(exactly = 1) {
            repository.login(any(), any())
        }

        // 完成挂起的请求并消费成功事件，收尾本次测试
        viewModel.effect.test {
            loginResult.complete(ApiResult.Success(Unit))
            assertEquals(LoginEffect.LoginSucceeded, awaitItem())
        }

        advanceUntilIdle()
        assertEquals(LoginUiState(), viewModel.uiState.value)
    }

    /** 验证接口返回字段验证失败时，字段错误写入对应的 UiState 字段，且不发出 Effect */
    @Test
    fun onAction_submitWithValidationFailure_updatesFieldErrors() = runTest {
        // Given：Repository 返回用户名和密码的字段验证错误
        val repository = mockk<AuthRepository>()
        coEvery {
            repository.login(any(), any())
        } returns ApiResult.Failure.Validation(
            code = -1,
            message = "Validation Error",
            fieldErrors = mapOf(
                "name" to "请输入正确的用户名或者邮箱",
                "password" to "请输入正确的密码",
            ),
        )

        val viewModel = LoginViewModel(repository)

        viewModel.effect.test {
            // When：提交登录请求
            viewModel.onAction(
                LoginAction.Submit(name = "user-a", password = "short"),
            )
            advanceUntilIdle()

            // Then：字段验证分别写入对应状态，且不再处于提交状态
            assertEquals(
                LoginUiState(
                    isSubmitting = false,
                    nameError = "请输入正确的用户名或者邮箱",
                    passwordError = "请输入正确的密码",
                ),
                viewModel.uiState.value,
            )

            // Then：字段验证失败不发出 Effect
            expectNoEvents()
        }
    }

    /** 验证账号被限制登录时，转换成可直接展示的错误文案并发出 ShowGeneralError */
    @Test
    fun onAction_submitWithLoginRestrictedBusinessFailure_emitsDisplayReadyError() = runTest {
        // Given：Repository 返回限制登录的业务错误，包含可直接展示的文案
        val repository = mockk<AuthRepository>()
        coEvery {
            repository.login(any(), any())
        } returns ApiResult.Failure.Business(
            code = -2,
            message = "账号已被限制登录，请联系客服",
        )

        val viewModel = LoginViewModel(repository)

        viewModel.effect.test {
            // When：提交登录请求
            viewModel.onAction(
                LoginAction.Submit(name = "user-a", password = "password-a"),
            )

            // Then：收到通用错误事件，错误类型为 DisplayReady，保留服务端文案
            assertEquals(
                LoginEffect.ShowGeneralError(
                    error = UiError.DisplayReady(
                        message = "账号已被限制登录，请联系客服",
                    ),
                ),
                awaitItem(),
            )
        }

        advanceUntilIdle()
        assertEquals(LoginUiState(), viewModel.uiState.value)
    }

    /** 验证其他业务错误转换为 Technical，不直接展示服务端文案 */
    @Test
    fun onAction_submitWithOtherBusinessFailure_emitsTechnicalError() = runTest {
        // Given：Repository 返回未单独处理的业务错误，包含内部诊断信息
        val repository = mockk<AuthRepository>()
        coEvery {
            repository.login(any(), any())
        } returns ApiResult.Failure.Business(
            code = -99,
            message = "Internal authentication service error",
        )

        val viewModel = LoginViewModel(repository)

        viewModel.effect.test {
            // When：提交登录请求
            viewModel.onAction(
                LoginAction.Submit(name = "user-a", password = "password-a"),
            )

            // Then：收到 Technical 错误事件，不携带服务端文案
            assertEquals(
                LoginEffect.ShowGeneralError(
                    error = UiError.Technical,
                ),
                awaitItem(),
            )
        }

        advanceUntilIdle()
        assertEquals(LoginUiState(), viewModel.uiState.value)
    }

    /** 验证 ClearNameError/ClearPasswordError 只清除各自对应的字段错误 */
    @Test
    fun onAction_clearError_clearsOnlyTargetField() = runTest {
        val repository = mockk<AuthRepository>()
        coEvery {
            repository.login(any(), any())
        } returns ApiResult.Failure.Validation(
            code = -1,
            message = "Validation Error",
            fieldErrors = mapOf(
                "name" to "请输入正确的用户名或者邮箱",
                "password" to "请输入正确的密码",
            ),
        )

        val viewModel = LoginViewModel(repository)
        viewModel.onAction(LoginAction.Submit(name = "user-a", password = "password-a"))
        advanceUntilIdle()

        viewModel.onAction(LoginAction.ClearNameError)

        assertEquals(
            LoginUiState(nameError = null, passwordError = "请输入正确的密码"),
            viewModel.uiState.value,
        )

        // 重新提交，让两个字段错误都恢复，再清密码错误才能验证不会误清用户名错误
        viewModel.onAction(LoginAction.Submit(name = "user-a", password = "password-a"))
        advanceUntilIdle()

        viewModel.onAction(LoginAction.ClearPasswordError)

        assertEquals(
            LoginUiState(nameError = "请输入正确的用户名或者邮箱", passwordError = null),
            viewModel.uiState.value,
        )
    }
}
