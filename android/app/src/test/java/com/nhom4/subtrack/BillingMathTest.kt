package com.nhom4.subtrack

import com.nhom4.subtrack.data.BillingCycle
import com.nhom4.subtrack.data.Category
import com.nhom4.subtrack.data.Currency
import com.nhom4.subtrack.data.Subscription
import com.nhom4.subtrack.data.SubscriptionStatus
import com.nhom4.subtrack.domain.BillingMath
import com.nhom4.subtrack.domain.CsvWriter
import com.nhom4.subtrack.domain.RenewalLevel
import com.nhom4.subtrack.reminder.ReminderWorker
import com.nhom4.subtrack.ui.formatMoney
import com.nhom4.subtrack.ui.reports.ReportsViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BillingMathTest {
    private val today = LocalDate.of(2026, 10, 27)

    private fun sub(
        amount: Double, cycle: BillingCycle, date: LocalDate = today.plusDays(10),
        status: SubscriptionStatus = SubscriptionStatus.ACTIVE, currency: Currency = Currency.VND,
        remind: Int? = 3, name: String = "Gói",
    ) = Subscription(
        name = name, amount = amount, currency = currency, cycle = cycle, nextPaymentDate = date,
        category = Category.OTHER, status = status, remindDaysBefore = remind,
    )

    @Test
    fun monthlyCost_convertsEachCycle() {
        assertEquals(52_000.0, BillingMath.monthlyCost(12_000.0, BillingCycle.WEEKLY), 0.001)
        assertEquals(100_000.0, BillingMath.monthlyCost(100_000.0, BillingCycle.MONTHLY), 0.001)
        assertEquals(100_000.0, BillingMath.monthlyCost(300_000.0, BillingCycle.QUARTERLY), 0.001)
        assertEquals(100_000.0, BillingMath.monthlyCost(1_200_000.0, BillingCycle.YEARLY), 0.001)
    }

    @Test
    fun totalMonthly_onlyCountsActiveInCurrency() {
        val subs = listOf(
            sub(100_000.0, BillingCycle.MONTHLY),
            sub(1_200_000.0, BillingCycle.YEARLY),
            sub(50_000.0, BillingCycle.MONTHLY, status = SubscriptionStatus.PAUSED),
            sub(9.99, BillingCycle.MONTHLY, currency = Currency.USD),
        )
        assertEquals(200_000.0, BillingMath.totalMonthly(subs, Currency.VND), 0.001)
        assertEquals(9.99, BillingMath.totalMonthly(subs, Currency.USD), 0.001)
    }

    @Test
    fun nextOnOrAfter_rollsPastDatesForward() {
        assertEquals(LocalDate.of(2026, 11, 15), BillingMath.nextOnOrAfter(LocalDate.of(2026, 9, 15), BillingCycle.MONTHLY, today))
        assertEquals(LocalDate.of(2026, 10, 27), BillingMath.nextOnOrAfter(LocalDate.of(2026, 10, 20), BillingCycle.WEEKLY, today))
        assertEquals(LocalDate.of(2027, 1, 1), BillingMath.nextOnOrAfter(LocalDate.of(2025, 1, 1), BillingCycle.YEARLY, today))
        // Ngày tương lai giữ nguyên
        assertEquals(today.plusDays(5), BillingMath.nextOnOrAfter(today.plusDays(5), BillingCycle.MONTHLY, today))
    }

    @Test
    fun monthEnd_isHandledByJavaTime() {
        assertEquals(LocalDate.of(2026, 2, 28), BillingMath.advance(LocalDate.of(2026, 1, 31), BillingCycle.MONTHLY))
    }

    @Test
    fun renewalLevel_matchesIndicatorRules() {
        assertEquals(RenewalLevel.SOON, BillingMath.renewalLevel(today.plusDays(7), today))
        assertEquals(RenewalLevel.THIS_MONTH, BillingMath.renewalLevel(today.plusDays(8), today))
        assertEquals(RenewalLevel.THIS_MONTH, BillingMath.renewalLevel(today.plusDays(30), today))
        assertEquals(RenewalLevel.LATER, BillingMath.renewalLevel(today.plusDays(31), today))
    }

    @Test
    fun parseAmount_vnd() {
        assertEquals(260_000.0, BillingMath.parseAmount("260000", Currency.VND)!!, 0.0)
        assertEquals(260_000.0, BillingMath.parseAmount("260.000", Currency.VND)!!, 0.0)
        assertEquals(1_190_000.0, BillingMath.parseAmount("1,190,000", Currency.VND)!!, 0.0)
        assertEquals(0.0, BillingMath.parseAmount("0", Currency.VND)!!, 0.0)
        assertNull(BillingMath.parseAmount("-5000", Currency.VND))
        assertNull(BillingMath.parseAmount("abc", Currency.VND))
        assertNull(BillingMath.parseAmount("  ", Currency.VND))
    }

    @Test
    fun parseAmount_usd() {
        assertEquals(9.99, BillingMath.parseAmount("9.99", Currency.USD)!!, 0.0)
        assertEquals(9.99, BillingMath.parseAmount("9,99", Currency.USD)!!, 0.0)
        assertEquals(1234.5, BillingMath.parseAmount("1,234.5", Currency.USD)!!, 0.0)
        assertEquals(12.0, BillingMath.parseAmount("12", Currency.USD)!!, 0.0)
        assertNull(BillingMath.parseAmount("1.2.3.4", Currency.USD))
    }

    @Test
    fun urlValidation() {
        assertTrue(BillingMath.isValidUrl("https://www.netflix.com/account"))
        assertTrue(BillingMath.isValidUrl("http://example.com"))
        assertFalse(BillingMath.isValidUrl("netflix.com"))
        assertFalse(BillingMath.isValidUrl("https://"))
    }

    @Test
    fun reminder_firesInsideWindowOnlyForActive() {
        assertTrue(ReminderWorker.shouldRemind(sub(1.0, BillingCycle.MONTHLY, today.plusDays(3), remind = 3), today))
        assertTrue(ReminderWorker.shouldRemind(sub(1.0, BillingCycle.MONTHLY, today, remind = 0), today))
        assertFalse(ReminderWorker.shouldRemind(sub(1.0, BillingCycle.MONTHLY, today.plusDays(4), remind = 3), today))
        assertFalse(ReminderWorker.shouldRemind(sub(1.0, BillingCycle.MONTHLY, today.plusDays(1), remind = null), today))
        assertFalse(ReminderWorker.shouldRemind(sub(1.0, BillingCycle.MONTHLY, today.plusDays(1), status = SubscriptionStatus.CANCELLED), today))
    }

    @Test
    fun reports_totalsSharesAndOthersSlice() {
        val subs = (1..8).map { sub(it * 10_000.0, BillingCycle.MONTHLY, name = "G$it") } +
            sub(5.0, BillingCycle.MONTHLY, currency = Currency.USD)
        val r = ReportsViewModel.build(subs, Currency.VND)
        assertEquals(360_000.0, r.monthly, 0.001)
        assertEquals(4_320_000.0, r.yearly, 0.001)
        assertEquals(8, r.activeCount)
        assertEquals(1, r.excludedCount)
        assertEquals(6, r.slices.size)
        assertNull(r.slices.last().label) // "Các gói khác"
        assertEquals(100.0, r.slices.sumOf { it.share }, 0.001)
        assertEquals("G8", r.top.first().name)
    }

    @Test
    fun csv_escapesSpecialCharacters() {
        assertEquals("abc", CsvWriter.escape("abc"))
        assertEquals("\"a,b\"", CsvWriter.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvWriter.escape("say \"hi\""))
        val csv = CsvWriter.build("Tên,Số tiền", listOf(listOf("Gói, gia đình", "1000")))
        assertEquals("Tên,Số tiền\r\n\"Gói, gia đình\",1000\r\n", csv)
    }

    @Test
    fun money_isFormattedVietnamese() {
        val vnd = formatMoney(260_000.0, Currency.VND)
        assertTrue(vnd, vnd.contains("260.000") && vnd.contains("₫"))
        val usd = formatMoney(9.99, Currency.USD)
        assertTrue(usd, usd.contains("9,99"))
    }
}
