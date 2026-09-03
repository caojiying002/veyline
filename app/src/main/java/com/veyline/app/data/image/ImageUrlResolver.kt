package com.veyline.app.data.image

/**
 * 将服务端返回的图片相对路径转换为可直接请求的完整地址。
 *
 * 图片基础地址由构建配置提供，业务 UI 不应自行拼接域名或路径。
 *
 * @param baseUrl 图片基础地址，必须以斜杠结尾。
 */
class ImageUrlResolver(
    private val baseUrl: String,
) {

    init {
        require(baseUrl.endsWith('/')) {
            "Image base URL must end with '/'"
        }
    }

    /**
     * 将相对路径拼接到图片基础地址之后。
     *
     * 会先去掉首尾空白、再去掉开头的斜杠，避免最终地址出现重复斜杠；
     * 规整后为空则抛出 [IllegalArgumentException]。
     */
    fun resolve(relativePath: String): String {
        val normalizedPath = relativePath.trim().trimStart('/')

        require(normalizedPath.isNotEmpty()) {
            "Image path must not be empty"
        }

        return baseUrl + normalizedPath
    }
}
