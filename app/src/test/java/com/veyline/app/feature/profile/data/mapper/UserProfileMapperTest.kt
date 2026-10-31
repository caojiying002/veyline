package com.veyline.app.feature.profile.data.mapper

import com.veyline.app.data.network.exception.InvalidApiDataException
import com.veyline.app.feature.profile.data.remote.model.UserProfileDto
import com.veyline.app.feature.profile.domain.model.Membership
import com.veyline.app.feature.profile.domain.model.UserProfile
import com.veyline.app.feature.profile.domain.model.VipExpiry
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserProfileMapperTest {

    /** 验证字段完整的 DTO 被清洗并转换为用户资料领域模型 */
    @Test
    fun map_withValidVipDto_returnsCleanedDomainModel() {
        val profileDto = UserProfileDto(
            id = " user-a ",
            name = " 用户甲 ",
            score = 100,
            publishedInfoCount = 3,
            isVip = true,
            expiredAt = 1_700_000_000_000L,
        )
        val expected = UserProfile(
            id = "user-a",
            name = "用户甲",
            score = 100,
            publishedInfoCount = 3,
            membership = Membership.Vip(VipExpiry.Until(1_700_000_000_000L)),
        )

        val result = UserProfileMapper.map(profileDto)
        assertEquals(expected, result)
    }

    /** 验证 VIP 的到期时间等于永久标记时转换为永久会员 */
    @Test
    fun map_withPermanentVipExpiry_returnsPermanentVip() {
        val result = UserProfileMapper.map(
            createDto(isVip = true, expiredAt = PERMANENT_EXPIRY_MILLIS),
        )

        assertEquals(
            Membership.Vip(VipExpiry.Permanent),
            result.membership,
        )
    }

    /** 验证任一必要字段缺失或空白时抛出 [InvalidApiDataException] */
    @Test
    fun map_withMissingRequiredFields_throwsInvalidApiDataException() {
        val validDto = UserProfileDto(
            id = "user-a",
            name = "用户甲",
            score = 100,
            publishedInfoCount = 3,
            isVip = false,
            expiredAt = null,
        )
        val invalidDtos = listOf(
            validDto.copy(id = null),
            validDto.copy(id = ""),
            validDto.copy(id = "   "),
            validDto.copy(name = null),
            validDto.copy(name = ""),
            validDto.copy(name = "   "),
            validDto.copy(score = null),
        )

        invalidDtos.forEach { profileDto ->
            assertFailsWith<InvalidApiDataException> {
                UserProfileMapper.map(profileDto)
            }
        }
    }

    /** 验证负积分原样保留，不归零或判定为无效数据 */
    @Test
    fun map_withNegativeScore_preservesScore() {
        val profileDto = createDto(score = -50)

        val result = UserProfileMapper.map(profileDto)
        assertEquals(-50, result.score)
    }

    /** 验证发布数量缺失或为负数时按 0 降级处理 */
    @Test
    fun map_withMissingOrNegativePublishedInfoCount_defaultsToZero() {
        val missingCountResult = UserProfileMapper.map(
            createDto(publishedInfoCount = null),
        )
        val negativeCountResult = UserProfileMapper.map(
            createDto(publishedInfoCount = -1),
        )

        assertEquals(0, missingCountResult.publishedInfoCount)
        assertEquals(0, negativeCountResult.publishedInfoCount)
    }

    /** 验证 isVip 缺失或 false 时按普通用户处理，并忽略到期时间 */
    @Test
    fun map_withMissingOrFalseVip_returnsRegularAndIgnoresExpiry() {
        // 刻意使用永久标记值：即使是最特殊的到期时间，isVip!=true 也视为普通用户
        val missingVipResult = UserProfileMapper.map(
            createDto(isVip = null, expiredAt = PERMANENT_EXPIRY_MILLIS),
        )
        val falseVipResult = UserProfileMapper.map(
            createDto(isVip = false, expiredAt = PERMANENT_EXPIRY_MILLIS),
        )

        assertEquals(Membership.Regular, missingVipResult.membership)
        assertEquals(Membership.Regular, falseVipResult.membership)
    }

    /** 验证 VIP 到期时间缺失或非正数时保留 VIP 身份，有效期按未知处理 */
    @Test
    fun map_withMissingOrNonPositiveVipExpiry_returnsUnknownVip() {
        val missingExpiryResult = UserProfileMapper.map(
            createDto(isVip = true, expiredAt = null),
        )
        val zeroExpiryResult = UserProfileMapper.map(
            createDto(isVip = true, expiredAt = 0L),
        )
        val negativeExpiryResult = UserProfileMapper.map(
            createDto(isVip = true, expiredAt = -1L),
        )

        val expected = Membership.Vip(VipExpiry.Unknown)

        assertEquals(expected, missingExpiryResult.membership)
        assertEquals(expected, zeroExpiryResult.membership)
        assertEquals(expected, negativeExpiryResult.membership)
    }

    /** 创建一个所有字段都合法的普通用户 DTO，各测试只覆盖自己关心的字段 */
    private fun createDto(
        id: String? = "user-a",
        name: String? = "用户甲",
        score: Int? = 100,
        publishedInfoCount: Int? = 3,
        isVip: Boolean? = false,
        expiredAt: Long? = null,
    ): UserProfileDto =
        UserProfileDto(
            id = id,
            name = name,
            score = score,
            publishedInfoCount = publishedInfoCount,
            isVip = isVip,
            expiredAt = expiredAt,
        )

    private companion object {
        // 服务端约定的永久 VIP 标记，对应北京时间 2038-01-19 00:00:00；
        // 测试中刻意不引用 Mapper 的私有常量，防止常量被误改时测试仍然通过
        const val PERMANENT_EXPIRY_MILLIS = 2_147_443_200_000L
    }
}
