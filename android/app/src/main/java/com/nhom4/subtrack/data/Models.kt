package com.nhom4.subtrack.data

import java.time.LocalDate

enum class BillingCycle { WEEKLY, MONTHLY, QUARTERLY, YEARLY }

enum class SubscriptionStatus { ACTIVE, PAUSED, CANCELLED }

/** Danh mục cố định, nhãn hiển thị lấy từ strings.xml theo [key]. */
enum class Category(val key: String, val color: Long) {
    ENTERTAINMENT("entertainment", 0xFFE5484D),
    MUSIC("music", 0xFFF76B15),
    EDUCATION("education", 0xFF30A46C),
    STORAGE("storage", 0xFF0090FF),
    WORK("work", 0xFF6E56CF),
    UTILITIES("utilities", 0xFF12A594),
    OTHER("other", 0xFF8B8D98);

    companion object {
        fun fromKey(key: String?): Category = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/** Nền tảng có sẵn; lưu theo [key]. */
enum class Platform(val key: String) {
    ANDROID("android"), IPHONE("iphone"), IPAD("ipad"), TABLET("tablet"),
    WEB("web"), WINDOWS("windows"), MAC("mac"), SMART_TV("smart_tv"),
    PLAYSTATION("playstation"), XBOX("xbox"), NINTENDO("nintendo"), OTHER("other");

    companion object {
        fun fromKey(key: String): Platform? = entries.firstOrNull { it.key == key }
    }
}

enum class Currency(val code: String) { VND("VND"), USD("USD"), EUR("EUR") }

data class Tag(
    val id: Long = 0,
    val name: String,
    val color: Long,
)

data class Subscription(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val currency: Currency,
    val cycle: BillingCycle,
    val nextPaymentDate: LocalDate,
    val category: Category,
    val status: SubscriptionStatus = SubscriptionStatus.ACTIVE,
    /** Số ngày nhắc trước hạn; null = không nhắc. */
    val remindDaysBefore: Int? = 3,
    val managementUrl: String? = null,
    val notes: String? = null,
    val tagIds: List<Long> = emptyList(),
    val platforms: List<Platform> = emptyList(),
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
