package com.veyline.app.feature.merchant.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 可供商家业务与 UI 直接使用的省份信息。
 *
 * 只包含经过 data 层校验的稳定字段，不直接承载服务端响应中的可空数据。
 *
 * 实现 [Parcelable] 是一处实用取舍：领域模型本不应依赖 Android 框架，但省份选择结果要
 * 经导航层的 `SavedStateHandle` 在页面间传递。让本模型直接可 Parcelable，能省掉一个只
 * 重复 `code` / `name` 的中转类；代价是领域层引入 `android.os.Parcelable`，纯 Kotlin 或
 * KMP 模块无法原样复用。它只用于内存与进程间传递——本地持久化仍逐字段写入 DataStore，
 * 不把整个对象序列化存盘（Parcel 格式不保证跨版本稳定）。
 *
 * @property code 省份的业务标识。
 * @property name 向用户展示的省份名称。
 */
@Parcelize
data class MerchantProvince(
    val code: String,
    val name: String,
) : Parcelable
