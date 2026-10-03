package com.veyline.app.feature.auth.data

import com.veyline.app.data.auth.AuthTokenStore
import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.data.network.model.ApiResponseDto
import com.veyline.app.data.network.result.ApiResult
import com.veyline.app.feature.auth.data.remote.AuthApiService
import com.veyline.app.feature.auth.data.remote.model.LoginRequestDto
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

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
}
