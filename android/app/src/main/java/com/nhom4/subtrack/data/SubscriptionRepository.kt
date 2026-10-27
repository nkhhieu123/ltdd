package com.nhom4.subtrack.data

import com.nhom4.subtrack.domain.BillingMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Nguồn dữ liệu duy nhất cho UI. Mọi thao tác đọc/ghi chạy trên Dispatchers.IO,
 * sau mỗi lần ghi thì làm mới [subscriptions] và [tags].
 */
class SubscriptionRepository(private val db: SubTrackDatabase) {

    private val _subscriptions = MutableStateFlow<List<Subscription>?>(null)
    /** null = đang tải lần đầu. */
    val subscriptions: StateFlow<List<Subscription>?> = _subscriptions.asStateFlow()

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        rollOverPastDates()
        _subscriptions.value = db.getSubscriptions()
        _tags.value = db.getTags()
    }

    suspend fun getSubscription(id: Long): Subscription? = withContext(Dispatchers.IO) {
        db.getSubscriptions().firstOrNull { it.id == id }
    }

    suspend fun save(sub: Subscription): Long = withContext(Dispatchers.IO) {
        db.upsertSubscription(sub).also { refresh() }
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        db.deleteSubscription(id)
        refresh()
    }

    suspend fun setStatus(sub: Subscription, status: SubscriptionStatus) = save(sub.copy(status = status))

    suspend fun saveTag(tag: Tag): Boolean = withContext(Dispatchers.IO) {
        db.upsertTag(tag).also { refresh() }
    }

    suspend fun deleteTag(id: Long) = withContext(Dispatchers.IO) {
        db.deleteTag(id)
        refresh()
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        db.clearAll()
        refresh()
    }

    /** Gói đang hoạt động có ngày thanh toán đã qua thì tự chuyển sang kỳ kế tiếp. */
    private fun rollOverPastDates(today: LocalDate = LocalDate.now()) {
        db.getSubscriptions()
            .filter { it.status == SubscriptionStatus.ACTIVE && it.nextPaymentDate.isBefore(today) }
            .forEach { db.updateNextPaymentDate(it.id, BillingMath.nextOnOrAfter(it.nextPaymentDate, it.cycle, today)) }
    }
}
