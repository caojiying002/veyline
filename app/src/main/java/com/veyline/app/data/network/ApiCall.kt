package com.veyline.app.data.network

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.veyline.app.data.network.exception.EmptyResponseBodyException
import com.veyline.app.data.network.exception.MissingDataException
import com.veyline.app.data.network.model.ApiResponseDto
import com.veyline.app.data.network.model.NoData
import com.veyline.app.data.network.result.ApiResult
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException
import retrofit2.Response

/**
 * 执行要求返回 [ApiResponseDto] 通用响应结构的 Retrofit 调用，并转换为 [ApiResult]。
 *
 * 本函数只处理一次请求，不额外创建 `Flow`，也不切换协程 Dispatcher。Retrofit 的 suspend
 * 调用已经异步执行网络请求；Repository 可以根据缓存、轮询或多次发射等真实需求决定是否
 * 在更高层使用 `Flow`。
 *
 * 本函数适用于绝大多数成功时必须返回 HTTP 响应体，且响应体内容为 [ApiResponseDto]
 * 响应结构的接口。对于 `204 No Content` 等协议明确允许成功响应没有响应体的
 * 少数接口，应改用 `apiCallNoContent()`；如果误用本函数，空响应体会以
 * [EmptyResponseBodyException] 为原因返回 [ApiResult.Failure.Serialization]。
 *
 * @param call 返回 `Response<ApiResponseDto<T>>` 的 Retrofit suspend 调用。
 */
suspend fun <T : Any> apiCall(
    call: suspend () -> Response<ApiResponseDto<T>>,
): ApiResult<T> =
    executeApiCall(call) { response ->
        val apiResponseDto: ApiResponseDto<T> = response.body()
            ?: return@executeApiCall ApiResult.Failure.Serialization(
                EmptyResponseBodyException(),
            )

        return@executeApiCall when {
            apiResponseDto.isValidationError() -> ApiResult.Failure.Validation(
                code = apiResponseDto.code,
                message = apiResponseDto.msg.orEmpty(),
                fieldErrors = apiResponseDto.fieldErrors.orEmpty(),
            )

            !apiResponseDto.isSuccessful() -> ApiResult.Failure.Business(
                code = apiResponseDto.code,
                message = apiResponseDto.msg.orEmpty(),
            )

            // 接口成功时没有业务数据，应将通用响应类型声明为 ApiResponseDto<NoData>；
            // 其他成功响应缺少必需的 data 属于协议异常。
            apiResponseDto.data == null -> ApiResult.Failure.Serialization(
                MissingDataException(),
            )

            else -> ApiResult.Success(apiResponseDto.data)
        }
    }

/**
 * 执行成功响应不包含 HTTP 响应体的 Retrofit 调用，并转换为 [ApiResult]。
 *
 * 当前项目的后端接口成功时均返回响应体，暂无调用方使用本函数。网络层仍保留该能力，
 * 用于兼容 `204 No Content` 等协议明确允许成功响应没有响应体的接口，以及按语义不包含
 * 响应体的 HTTP `HEAD` 请求。
 *
 * 任何 2XX 响应都转换为包含 [NoData] 的 [ApiResult.Success]；HTTP 错误、网络异常与
 * 协程取消仍由通用调用逻辑统一处理。对于成功时必须在响应体中返回 [ApiResponseDto]
 * 结构的接口，应使用 [apiCall]，不能通过本函数绕过业务状态码和响应数据校验。
 *
 * @param call 返回 `Response<Unit>` 的 Retrofit suspend 调用。
 */
suspend fun apiCallNoContent(
    call: suspend () -> Response<Unit>,
): ApiResult<NoData> =
    executeApiCall(call) { _ ->
        ApiResult.Success(NoData)
    }

/** 执行 HTTP 调用并统一处理与响应业务结构无关的失败。 */
private suspend fun <Body, Result> executeApiCall(
    call: suspend () -> Response<Body>,
    onSuccessfulResponse: (Response<Body>) -> ApiResult<Result>,
): ApiResult<Result> =
    try {
        val response = call()
        if (response.isSuccessful)
            onSuccessfulResponse(response)
        else
            ApiResult.Failure.Http(statusCode = response.code())
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: JsonDataException) {
        ApiResult.Failure.Serialization(exception)
    } catch (exception: JsonEncodingException) {
        // JsonEncodingException 继承 IOException，必须在通用 IOException 之前捕获。
        ApiResult.Failure.Serialization(exception)
    } catch (exception: HttpException) {
        ApiResult.Failure.Http(
            statusCode = exception.code(),
            exception = exception,
        )
    } catch (exception: IOException) {
        ApiResult.Failure.Network(exception)
    }
