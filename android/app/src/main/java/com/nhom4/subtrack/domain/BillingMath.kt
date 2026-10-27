package com.nhom4.subtrack.domain

import com.nhom4.subtrack.data.BillingCycle
import com.nhom4.subtrack.data.Currency
import com.nhom4.subtrack.data.Subscription
import com.nhom4.subtrack.data.SubscriptionStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RenewalLevel { SOON, THIS_MONTH, LATER }

/** Quy tắc tính tiền và ngày, tách khỏi Android để kiểm thử bằng JUnit. */
object BillingMath {

    /**
     * Quy đổi về chi phí một tháng:
     * tuần × 52 / 12, tháng × 1, quý / 3, năm / 12.
     */
    fun monthlyCost(amount: Double, cycle: BillingCycle): Double = when (cycle) {
        BillingCycle.WEEKLY -> amount * 52.0 / 12.0
        BillingCycle.MONTHLY -> amount
        BillingCycle.QUARTERLY -> amount / 3.0
        BillingCycle.YEARLY -> amount / 12.0
    }

    fun weeklyFromMonthly(monthly: Double) = monthly * 12.0 / 52.0
    fun quarterlyFromMonthly(monthly: Double) = monthly * 3.0
    fun yearlyFromMonthly(monthly: Double) = monthly * 12.0

    /** Tổng chi phí tháng của các gói đang hoạt động bằng [currency]. */
    fun totalMonthly(subs: List<Subscription>, currency: Currency): Double =
        subs.filter { it.status == SubscriptionStatus.ACTIVE && it.currency == currency }
            .sumOf { monthlyCost(it.amount, it.cycle) }

    fun advance(date: LocalDate, cycle: BillingCycle): LocalDate = when (cycle) {
        BillingCycle.WEEKLY -> date.plusWeeks(1)
        BillingCycle.MONTHLY -> date.plusMonths(1)
        BillingCycle.QUARTERLY -> date.plusMonths(3)
        BillingCycle.YEARLY -> date.plusYears(1)
    }

    /** Ngày thanh toán đầu tiên không trước [today], tính từ [date] theo chu kỳ. */
    fun nextOnOrAfter(date: LocalDate, cycle: BillingCycle, today: LocalDate): LocalDate {
        var d = date
        var guard = 0
        while (d.isBefore(today) && guard < 10_000) {
            d = advance(d, cycle)
            guard++
        }
        return d
    }

    fun daysUntil(date: LocalDate, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, date)

    /** Đỏ: trong 7 ngày; cam: trong 30 ngày; còn lại: xa. */
    fun renewalLevel(date: LocalDate, today: LocalDate = LocalDate.now()): RenewalLevel {
        val days = daysUntil(date, today)
        return when {
            days <= 7 -> RenewalLevel.SOON
            days <= 30 -> RenewalLevel.THIS_MONTH
            else -> RenewalLevel.LATER
        }
    }

    /**
     * Đọc số tiền người dùng nhập. VND không có phần thập phân nên bỏ mọi dấu
     * phân cách; USD/EUR chấp nhận cả "4.99" và "4,99". Trả về null nếu sai.
     */
    fun parseAmount(input: String, currency: Currency): Double? {
        val raw = input.trim().replace(" ", "")
        if (raw.isEmpty()) return null
        val normalized = if (currency == Currency.VND) {
            raw.replace(".", "").replace(",", "")
        } else {
            val lastSep = maxOf(raw.lastIndexOf('.'), raw.lastIndexOf(','))
            if (lastSep >= 0 && raw.length - lastSep - 1 in 1..2) {
                raw.substring(0, lastSep).replace(".", "").replace(",", "") + "." + raw.substring(lastSep + 1)
            } else {
                raw.replace(".", "").replace(",", "")
            }
        }
        if (!normalized.matches(Regex("""\d+(\.\d{1,2})?"""))) return null
        return normalized.toDoubleOrNull()
    }

    fun isValidUrl(url: String): Boolean =
        Regex("""^https?://[^\s/$.?#].[^\s]*$""", RegexOption.IGNORE_CASE).matches(url.trim())
}
