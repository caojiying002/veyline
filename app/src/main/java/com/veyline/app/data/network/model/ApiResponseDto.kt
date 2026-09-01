package com.veyline.app.data.network.model

/**
 * 网络层使用的通用 API 响应结构。
 *
 * 当前协议约定：
 * - `code == 0`：业务处理成功，`data` 承载正常业务数据；
 * - `code == -1`：表单字段验证失败，仅用于登录、注册、内容发布等表单提交场景，
 *   原始 `data` 承载字段名与对应错误信息；
 * - 其他状态码：业务处理失败，具体含义由相应业务或全局错误处理逻辑判断。
 *
 * 本类型保留 [msg] 命名以直接对应服务端传输字段；它只描述响应协议，不负责将错误转换为
 * `ApiResult`、抛出异常或生成用户提示。后续的自定义 Moshi Adapter 会根据 [code] 解析
 * 多态的 `data` 字段：业务成功时写入 [data]，字段验证失败时写入 [fieldErrors]。
 *
 * ## Moshi Adapter 策略
 *
 * 本类型不使用 `@JsonClass(generateAdapter = true)`。`ApiResponseDto<T>` 是所有接口共享的泛型
 * 响应包装结构，并且 `data` 会随业务状态码在 `T` 与字段错误对象之间变化，需要由自定义
 * `JsonAdapter.Factory` 统一选择解析策略。具体的 Request、Response 等业务数据类没有这种
 * 多态结构，仍应优先使用 `@JsonClass(generateAdapter = true)` 生成 Adapter，避免反射解析。
 *
 * [code] 是判断响应语义所必需的字段；[msg] 与 [data] 允许为空。自定义
 * Adapter 会显式处理字段缺失和 JSON `null`，因此这里是否提供 Kotlin 默认值不会改变
 * 网络解析行为。
 *
 * @property code 服务端业务状态码。
 * @property msg 服务端返回的提示信息，字段名与传输协议保持一致。
 * @property data 业务成功时的响应数据。
 * @property fieldErrors 字段名与对应错误信息，仅在字段验证失败时由解析器填充。
 */
data class ApiResponseDto<T>(
    val code: Int,
    val msg: String?,
    val data: T?,
    val fieldErrors: Map<String, String>? = null,
) {
    /** 当前响应是否为业务处理成功（`code == 0`）。 */
    fun isSuccessful(): Boolean = code == CODE_SUCCESS

    /** 当前响应是否为表单字段验证失败（`code == -1`）。 */
    fun isValidationError(): Boolean = code == CODE_VALIDATION_ERROR

    companion object {
        /** 业务处理成功状态码，协议值为 `0`。 */
        const val CODE_SUCCESS = 0

        /**
         * 表单字段验证失败状态码，协议值为 `-1`。
         *
         * 仅用于登录、注册、内容发布等表单提交场景。此状态表示响应中的 `data` 承载字段名
         * 与对应错误信息，而不是正常业务数据。
         */
        const val CODE_VALIDATION_ERROR = -1
    }
}
