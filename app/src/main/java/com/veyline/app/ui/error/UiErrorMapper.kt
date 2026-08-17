package com.veyline.app.ui.error

import com.veyline.app.data.network.model.ApiResult

/**
 * 将网络调用失败转换为大多数页面可以统一处理的 UI 错误。
 *
 * 默认采用保守策略：网络连接失败转换为 [UiError.Connection]，其余失败统一降级为
 * [UiError.Technical]。如果具体业务已经确认某个错误码的含义和展示文案，应先完成对应的
 * 特殊处理，再将未识别的失败交给本函数。
 *
 * 表单字段验证、登录失效，以及会改变页面处理流程的特殊业务错误，不应只依赖本函数的
 * 默认结果，需由相应页面或全局机制单独处理。
 */
internal fun ApiResult.Failure.toUiError(): UiError =
    when (this) {
        is ApiResult.Failure.Network -> UiError.Connection

        is ApiResult.Failure.Business,
        is ApiResult.Failure.Validation,
        is ApiResult.Failure.Http,
        is ApiResult.Failure.Serialization,
        is ApiResult.Failure.Unexpected -> UiError.Technical
    }
