package com.veyline.app.data.paging

import com.veyline.app.data.network.result.ApiResult

/**
 * 将结构化的 API 失败适配为 Paging 3 要求的异常形式。
 *
 * Paging 3 的 `LoadResult.Error` 只能通过 [Throwable] 表示加载失败，而项目网络层使用
 * [ApiResult.Failure] 保留具体的错误分类。本异常只负责连接这两种错误表达方式，不会取代
 * [ApiResult] 成为通用网络异常，也不负责生成面向用户的错误信息。
 *
 * 对于本身携带异常原因的失败类型，会将原始异常保留为 `cause`，便于日志记录和问题诊断；
 * 业务失败和字段验证失败没有对应的底层异常，因此 `cause` 为 `null`。
 *
 * @property failure 网络层已经完成分类的原始 API 失败。
 */
internal class PagingFailureException(
    val failure: ApiResult.Failure,
) : RuntimeException(
    "API request failed: ${failure::class.simpleName}",
    failure.causeOrNull(),
)

/** 提取失败中可用于异常链诊断的原始原因。 */
private fun ApiResult.Failure.causeOrNull(): Throwable? =
    when (this) {
        is ApiResult.Failure.Network -> exception
        is ApiResult.Failure.Http -> exception
        is ApiResult.Failure.Serialization -> exception
        is ApiResult.Failure.Unexpected -> exception
        is ApiResult.Failure.Business,
        is ApiResult.Failure.Validation -> null
    }
