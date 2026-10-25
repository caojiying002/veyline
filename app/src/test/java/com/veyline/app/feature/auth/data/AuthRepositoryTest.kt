package com.veyline.app.feature.auth.data

import com.veyline.app.data.auth.AuthTokenStore
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResponseDto
import com.veyline.app.data.network.model.NoData
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.auth.data.remote.AuthApiService
import com.veyline.app.feature.auth.data.remote.model.LoginRequestDto
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class AuthRepositoryTest {

    /** 验证登录成功时保存去除首尾空白后的 Token，并返回不暴露 Token 的成功结果 */
    @Test
    fun login_withSuccessfulResponse_savesTokenAndReturnsSuccess() = runTest {
        val apiService = mockk<AuthApiService>()
        val authTokenStore = mockk<AuthTokenStore>()

        coEvery {
            apiService.login(LoginRequestDto(name = "user-a", password = "password-a"))
        } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
                msg = "success",
                data = "  token-a  ",
            )
        )
        coEvery { authTokenStore.saveToken(any()) } just Runs

        val repository = AuthRepository(
            apiService = apiService,
            authTokenStore = authTokenStore,
        )

        val result = repository.login(name = "user-a", password = "password-a")

        assertEquals(ApiResult.Success(Unit), result)
        coVerify(exactly = 1) {
            // 验证调用过 saveToken，且参数已去除首尾空白
            authTokenStore.saveToken("token-a")
        }
    }

    /** 验证 API 失败时原样返回失败结果，并且不保存 Token */
    @Test
    fun login_withApiFailure_returnsFailureWithoutSavingToken() = runTest {
        val apiService = mockk<AuthApiService>()
        val authTokenStore = mockk<AuthTokenStore>()

        coEvery {
            apiService.login(LoginRequestDto(name = "user-a", password = "password-a"))
        } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_VALIDATION_ERROR,
                msg = "Validation Error",
                data = null,
                fieldErrors = mapOf("password" to "密码错误"),
            )
        )

        val repository = AuthRepository(
            apiService = apiService,
            authTokenStore = authTokenStore,
        )

        val result = repository.login(name = "user-a", password = "password-a")

        val expected = ApiResult.Failure.Validation(
            code = ApiResponseDto.CODE_VALIDATION_ERROR,
            message = "Validation Error",
            fieldErrors = mapOf("password" to "密码错误"),
        )
        assertEquals(expected, result)
        coVerify(exactly = 0) {
            authTokenStore.saveToken(any())
        }
    }

    /** 验证接口成功但 Token 为空白时返回序列化失败，并且不保存 Token */
    @Test
    fun login_withBlankToken_returnsSerializationFailureWithoutSavingToken() = runTest {
        val apiService = mockk<AuthApiService>()
        val authTokenStore = mockk<AuthTokenStore>()

        coEvery {
            apiService.login(LoginRequestDto(name = "user-a", password = "password-a"))
        } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
                msg = "success",
                data = "   ",
            )
        )

        val repository = AuthRepository(
            apiService = apiService,
            authTokenStore = authTokenStore,
        )

        val result = repository.login(name = "user-a", password = "password-a")

        assertIs<ApiResult.Failure.Serialization>(result)
        assertIs<InvalidApiDataException>(result.exception)
        coVerify(exactly = 0) {
            authTokenStore.saveToken(any())
        }
    }

    /** 验证 Token 持久化失败时转换为 Unexpected 类型失败，并保留原始 IOException */
    @Test
    fun login_whenSavingTokenThrowsIOException_returnsUnexpectedFailure() = runTest {
        val apiService = mockk<AuthApiService>()
        val authTokenStore = mockk<AuthTokenStore>()

        val ioException = IOException("disk full")

        coEvery {
            apiService.login(LoginRequestDto(name = "user-a", password = "password-a"))
        } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
                msg = "success",
                data = "token-a",
            )
        )
        coEvery { authTokenStore.saveToken(any()) } throws ioException

        val repository = AuthRepository(
            apiService = apiService,
            authTokenStore = authTokenStore,
        )

        val result = repository.login(name = "user-a", password = "password-a")

        assertIs<ApiResult.Failure.Unexpected>(result)
        assertSame(ioException, result.exception)
    }

    /** 验证退出登录时服务端返回成功，会先请求服务端、再清除 Token，并返回成功结果 */
    @Test
    fun logout_withSuccessfulResponse_clearsTokenAndReturnsSuccess() = runTest {
        // Given: 服务端注销成功，本地 Token 可以正常清除
        val apiService = mockk<AuthApiService>()
        val authTokenStore = mockk<AuthTokenStore>()

        coEvery { apiService.logout() } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
                msg = "success",
                data = NoData,
            )
        )
        coEvery { authTokenStore.clearToken() } just Runs

        val repository = AuthRepository(
            apiService = apiService,
            authTokenStore = authTokenStore,
        )

        // When: 调用退出登录
        val result = repository.logout()

        // Then: 返回成功，先请求服务端再清除 Token，且只清除一次
        assertEquals(ApiResult.Success(Unit), result)
        // 请求需要携带 Token，所以必须先请求服务端，再清除 Token
        coVerifyOrder {
            apiService.logout()
            authTokenStore.clearToken()
        }
        coVerify(exactly = 1) {
            authTokenStore.clearToken()
        }
    }

    /** 验证退出登录时服务端返回业务错误，仍会清除 Token 并返回成功结果 */
    @Test
    fun logout_withBusinessFailure_stillClearsTokenAndReturnsSuccess() = runTest {
        // Given: 服务端返回业务错误，本地 Token 可以正常清除
        val apiService = mockk<AuthApiService>()
        val authTokenStore = mockk<AuthTokenStore>()

        coEvery { apiService.logout() } returns Response.success(
            ApiResponseDto(
                code = -2,
                msg = "Logout rejected",
                data = null,
            )
        )
        coEvery { authTokenStore.clearToken() } just Runs

        val repository = AuthRepository(
            apiService = apiService,
            authTokenStore = authTokenStore,
        )

        // When: 调用退出登录
        val result = repository.logout()

        // Then: 服务端的错误不影响结果，仍然返回成功，并且清除了 Token
        assertEquals(ApiResult.Success(Unit), result)
        coVerify(exactly = 1) {
            authTokenStore.clearToken()
        }
    }

    /** 验证退出登录时网络请求失败，仍会清除 Token 并返回成功结果 */
    @Test
    fun logout_whenNetworkFails_stillClearsTokenAndReturnsSuccess() = runTest {
        // Given: 网络请求失败，本地 Token 可以正常清除
        val apiService = mockk<AuthApiService>()
        val authTokenStore = mockk<AuthTokenStore>()

        coEvery { apiService.logout() } throws IOException("network unavailable")
        coEvery { authTokenStore.clearToken() } just Runs

        val repository = AuthRepository(
            apiService = apiService,
            authTokenStore = authTokenStore,
        )

        // When: 调用退出登录
        val result = repository.logout()

        // Then: 网络失败不影响结果，仍然返回成功，并且清除了 Token
        assertEquals(ApiResult.Success(Unit), result)
        coVerify(exactly = 1) {
            authTokenStore.clearToken()
        }
    }

    /** 验证退出登录时清除 Token 失败，返回 Unexpected 类型失败并保留原始 IOException */
    @Test
    fun logout_whenClearingTokenThrowsIOException_returnsUnexpectedFailure() = runTest {
        // Given: 服务端注销成功，但本地 Token 清除失败
        val apiService = mockk<AuthApiService>()
        val authTokenStore = mockk<AuthTokenStore>()

        val ioException = IOException("disk error")

        coEvery { apiService.logout() } returns Response.success(
            ApiResponseDto(
                code = ApiResponseDto.CODE_SUCCESS,
                msg = "success",
                data = NoData,
            )
        )
        coEvery { authTokenStore.clearToken() } throws ioException

        val repository = AuthRepository(
            apiService = apiService,
            authTokenStore = authTokenStore,
        )

        // When: 调用退出登录
        val result = repository.logout()

        // Then: 本地仍处于登录状态，返回 Unexpected 失败并保留原始异常
        assertIs<ApiResult.Failure.Unexpected>(result)
        // 经过 withContext 后异常可能被协程库复制，原始异常会保留在 cause 链中
        assertTrue(
            generateSequence(result.exception) { it.cause }
                .any { it === ioException },
            "异常链中应保留原始的 IOException",
        )
    }

    /** 验证退出登录时协程在服务端请求期间被取消，仍会清除 Token */
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun logout_whenCancelledDuringRequest_stillClearsToken() = runTest {
        // Given: 服务端请求一直挂起，直到协程被取消
        val apiService = mockk<AuthApiService>()
        val authTokenStore = mockk<AuthTokenStore>()
        var tokenCleared = false    // 记录 clearToken() 是否真正执行完毕
        var logoutReturned = false  // 记录 logout() 是否正常返回；被取消时它应该抛出异常，不会返回

        coEvery { apiService.logout() } coAnswers {
            // 模拟请求一直没有返回，直到协程被取消，才会抛出 CancellationException
            awaitCancellation()
        }
        coEvery { authTokenStore.clearToken() } coAnswers {
            // yield() 是会检查取消状态的挂起点：如果清除 Token 没有用 NonCancellable 保护，
            // 协程已经被取消，这里会直接抛出 CancellationException，下一行就执行不到
            yield()
            tokenCleared = true
        }

        val repository = AuthRepository(
            apiService = apiService,
            authTokenStore = authTokenStore,
        )

        // When: 在子协程里启动退出登录，并运行到服务端请求发出、正在等待响应的状态
        // 必须放进子协程：logout() 会一直挂起，直接调用会卡住测试，也拿不到用来取消的 Job
        val job = launch {
            repository.logout()
            logoutReturned = true
        }
        // 让子协程一直运行到挂起点为止，也就是停在服务端请求上
        runCurrent()

        // 确认此时请求已经发出、Token 还没被清除，说明接下来的取消发生在请求期间
        coVerify(exactly = 1) {
            apiService.logout()
        }
        assertFalse(tokenCleared)

        // When: 取消协程，并等它完全结束
        // 用 cancelAndJoin() 而不是 cancel()：后者不会等 finally 里的清除执行完，断言会太早
        job.cancelAndJoin()

        // Then: 协程被取消后 Token 仍被清除；logout() 没有正常返回，说明取消没有被吞掉
        assertTrue(tokenCleared)
        assertFalse(logoutReturned)
        coVerify(exactly = 1) {
            authTokenStore.clearToken()
        }
    }

    /** 验证退出登录时协程被取消、同时清除 Token 也失败，取消会继续向外传播，不会被转换成失败结果 */
    @Test
    fun logout_whenCancelledAndClearingTokenFails_propagatesCancellation() = runTest {

    }
}
