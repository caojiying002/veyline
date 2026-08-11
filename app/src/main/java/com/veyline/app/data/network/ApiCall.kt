package com.veyline.app.data.network

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.veyline.app.data.network.exception.EmptyResponseBodyException
import com.veyline.app.data.network.exception.MissingDataException
import com.veyline.app.data.network.model.ApiResponse
import com.veyline.app.data.network.model.ApiResult
import com.veyline.app.data.network.model.NoData
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import retrofit2.HttpException
import retrofit2.Response

/**
 * 执行要求返回 [ApiResponse] 响应壳的 Retrofit 调用，并转换为 [ApiResult]。
 *
 * 本函数只处理一次请求，不额外创建 `Flow`，也不切换协程 Dispatcher。Retrofit 的 suspend
 * 调用已经异步执行网络请求；Repository 可以根据缓存、轮询或多次发射等真实需求决定是否
 * 在更高层使用 `Flow`。
 *
 * 本函数适用于绝大多数成功时必须返回 HTTP Body，且 Body 为 [ApiResponse] 响应壳的接口。
 * 对于 `204 No Content` 等协议明确允许成功响应没有 Body 的少数接口，应改用
 * `apiCallNoContent()`；如果误用本函数，空 Body 会以 [EmptyResponseBodyException] 为原因
 * 返回 [ApiResult.Failure.Serialization]。
 *
 * @param call 返回 `Response<ApiResponse<T>>` 的 Retrofit suspend 调用。
 */
suspend fun <T : Any> apiCall(
    call: suspend () -> Response<ApiResponse<T>>,
): ApiResult<T> =
    executeApiCall(call) { response ->
        val apiResponse: ApiResponse<T> = response.body()
            ?: return@executeApiCall ApiResult.Failure.Serialization(
                EmptyResponseBodyException(),
            )

        return@executeApiCall when {
            apiResponse.isValidationError() -> ApiResult.Failure.Validation(
                code = apiResponse.code,
                message = apiResponse.msg.orEmpty(),
                fieldErrors = apiResponse.fieldErrors.orEmpty(),
            )

            !apiResponse.isSuccessful() -> ApiResult.Failure.Business(
                code = apiResponse.code,
                message = apiResponse.msg.orEmpty(),
            )

            // 响应壳存在但 data 没有业务含义时应声明 ApiResponse<NoData>；
            // 其他成功响应缺少必需 data 属于协议异常。
            apiResponse.data == null -> ApiResult.Failure.Serialization(
                MissingDataException(),
            )

            else -> ApiResult.Success(apiResponse.data)
        }
    }

/**
 * 执行成功响应不包含 HTTP Body 的 Retrofit 调用，并转换为 [ApiResult]。
 *
 * 当前项目的后端接口成功时均返回 Body，暂无调用方使用本函数。网络层仍保留该能力，
 * 用于兼容 `204 No Content` 等协议明确允许成功响应没有 Body 的接口，以及响应按语义
 * 不包含 Body 的 HTTP `HEAD` 请求。
 *
 * 任何 2XX 响应都转换为包含 [NoData] 的 [ApiResult.Success]；HTTP 错误、网络异常与
 * 协程取消仍由通用调用逻辑统一处理。对于成功时必须返回 [ApiResponse] 响应壳的接口，
 * 应使用 [apiCall]，不能通过本函数绕过业务状态码和响应数据校验。
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
