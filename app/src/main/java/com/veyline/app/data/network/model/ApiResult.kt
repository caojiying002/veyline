package com.veyline.app.data.network.model

import java.io.IOException

/**
 * Repository 向上层暴露的 API 调用结果。
 *
 * [Success] 携带业务数据，[Failure] 表示本次调用未成功。`T` 声明为协变类型，因此不携带
 * 成功数据的 [Failure] 可以安全地实现 `ApiResult<Nothing>`，并用于任意 `ApiResult<T>`。
 * `Nothing` 在这里表示失败结果不可能提供 `T`，不表示失败状态不会发生。
 *
 * ## 失败类型的层级设计
 *
 * 所有具体失败类型都直接放在 [Failure] 下，而不是继续拆成 `BusinessFailure`、
 * `TransportFailure` 等多层继承结构。当前项目需要同时满足两种使用方式：
 *
 * - 页面可以只匹配 [Failure]，通过统一映射展示用户提示；
 * - 日志、监控和测试可以匹配具体类型，保留业务、网络、HTTP 和解析失败的诊断信息。
 *
 * 单层分类已经能够满足这两个目标，也能避免调用方出现
 * `ApiResult.Failure.Business.Validation` 一类过深且收益有限的类型引用。只有未来确实出现
 * 一组失败类型需要共享字段或独立处理策略时，才考虑增加中间层级。
 */
sealed interface ApiResult<out T> {

    /**
     * API 与业务均成功。
     *
     * @property data 调用方请求的业务数据。
     */
    data class Success<T>(
        val data: T
    ) : ApiResult<T>

    /**
     * 本次 API 调用未成功，不携带成功数据。
     *
     * 具体类型用于内部诊断；UI 通常应通过统一的错误映射处理本接口，而不是在每个页面
     * 重复维护所有失败分支。
     */
    sealed interface Failure : ApiResult<Nothing> {

        /**
         * HTTP 请求成功，但服务端返回普通业务失败。
         *
         * @property code 服务端业务错误码。
         * @property message 服务端提供的错误信息。
         */
        data class Business(
            val code: Int,
            val message: String,
        ) : Failure

        /**
         * HTTP 请求成功，但服务端返回字段验证失败。
         *
         * 具体协议状态码和响应字段解析规则属于 [ApiResponse] 与网络解析器的职责，本类型只
         * 保存转换后的结果，不依赖服务端使用的魔法数字或 JSON 字段结构。
         *
         * @property code 服务端业务错误码。
         * @property message 服务端提供的错误信息。
         * @property fieldErrors 字段名与对应的验证错误信息。
         */
        data class Validation(
            val code: Int,
            val message: String,
            val fieldErrors: Map<String, String>,
        ) : Failure

        /**
         * 网络连接、超时或其他 IO 失败。
         *
         * @property exception 导致调用失败的 IO 异常。
         */
        data class Network(
            val exception: IOException,
        ) : Failure

        /**
         * HTTP 状态码不在成功范围内。
         *
         * [statusCode] 是处理该失败所需的稳定信息；[exception] 仅用于在底层库提供原始异常
         * 时保留诊断上下文，因此允许为空，避免结果模型强制依赖某个 HTTP 客户端的异常类型。
         *
         * @property statusCode HTTP 状态码。
         * @property exception 可选的原始异常。
         */
        data class Http(
            val statusCode: Int,
            val exception: Throwable? = null,
        ) : Failure

        /**
         * 响应内容无法按客户端协议完成反序列化。
         *
         * 使用 `Serialization` 命名而不是 `Json` 或 `Parse`，避免结果模型绑定具体数据
         * 格式或解析库；即使未来替换 Moshi 或使用其他编码格式，该分类仍然成立。
         *
         * @property exception 序列化框架抛出的原始异常。
         */
        data class Serialization(
            val exception: Throwable,
        ) : Failure

        /**
         * 已知失败分类之外、且允许在当前边界降级处理的异常。
         *
         * 后续网络调用封装不得通过 `catch (Exception)` 把所有编程错误转换到这里；协程取消
         * 和客户端代码缺陷应继续抛出，避免在开发与测试阶段被伪装成普通请求失败。
         *
         * @property exception 未预期的原始异常。
         */
        data class Unexpected(
            val exception: Throwable,
        ) : Failure
    }
}
