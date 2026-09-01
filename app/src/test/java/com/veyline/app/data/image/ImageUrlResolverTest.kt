package com.veyline.app.data.image

import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class ImageUrlResolverTest {

    /** 验证普通相对路径被拼接到图片基础地址之后。 */
    @Test
    fun resolve_withRelativePath_returnsCompleteUrl() {
        val resolver = ImageUrlResolver(
            baseUrl = "https://example.test/images/",
        )

        val result = resolver.resolve("merchant-a.jpg")

        assertEquals(
            "https://example.test/images/merchant-a.jpg",
            result,
        )
    }

    /** 验证路径开头多余的斜杠会在拼接前被移除。 */
    @Ignore("待实现")
    @Test
    fun resolve_withLeadingSlashes_returnsNormalizedUrl() {
    }

    /** 验证路径首尾的空白字符会在拼接前被移除。 */
    @Ignore("待实现")
    @Test
    fun resolve_withSurroundingWhitespace_returnsNormalizedUrl() {
    }

    /** 验证空白路径会被拒绝。 */
    @Ignore("待实现")
    @Test
    fun resolve_withBlankPath_throwsIllegalArgumentException() {
    }

    /** 验证图片基础地址必须以斜杠结尾。 */
    @Ignore("待实现")
    @Test
    fun create_withBaseUrlWithoutTrailingSlash_throwsIllegalArgumentException() {
    }
}
