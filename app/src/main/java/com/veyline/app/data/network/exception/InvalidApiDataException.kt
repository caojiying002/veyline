package com.veyline.app.data.network.exception

/**
 * API 响应已成功解析，但其中的业务数据不满足客户端要求。
 *
 * 本异常用于表示 `data` 已存在，但其内部字段缺失、值无效或数据之间存在冲突，导致网络
 * 模型无法安全转换为领域模型。JSON 语法或字段类型无法解析时，应由 JSON 解析器抛出对应
 * 异常；整个 `data` 缺失时，应使用 [MissingDataException]。
 *
 * @param message 说明无效数据的位置及其违反的客户端约定。
 * @param cause 导致数据无效的可选原始异常。
 */
class InvalidApiDataException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)
