package com.nhom4.subtrack.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

/** Lớp truy cập SQLite. Chỉ Repository gọi trực tiếp lớp này. */
class SubTrackDatabase(context: Context) :
    SQLiteOpenHelper(context, "subtrack.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE subscriptions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                amount REAL NOT NULL,
                currency TEXT NOT NULL,
                billing_cycle TEXT NOT NULL,
                next_payment_date INTEGER NOT NULL,
                category TEXT NOT NULL,
                status TEXT NOT NULL,
                remind_days_before INTEGER,
                management_url TEXT,
                notes TEXT,
                platforms TEXT NOT NULL DEFAULT '',
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE TABLE tags (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, color INTEGER NOT NULL)")
        db.execSQL(
            """
            CREATE TABLE subscription_tags (
                subscription_id INTEGER NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
                tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                PRIMARY KEY (subscription_id, tag_id)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    // ---------- Subscriptions ----------

    fun getSubscriptions(): List<Subscription> {
        val tagMap = HashMap<Long, MutableList<Long>>()
        readableDatabase.rawQuery("SELECT subscription_id, tag_id FROM subscription_tags", null).use { c ->
            while (c.moveToNext()) tagMap.getOrPut(c.getLong(0)) { mutableListOf() }.add(c.getLong(1))
        }
        return readableDatabase.rawQuery("SELECT * FROM subscriptions", null).use { c ->
            buildList { while (c.moveToNext()) add(c.toSubscription(tagMap)) }
        }
    }

    fun upsertSubscription(sub: Subscription): Long {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            val values = ContentValues().apply {
                put("name", sub.name)
                put("amount", sub.amount)
                put("currency", sub.currency.name)
                put("billing_cycle", sub.cycle.name)
                put("next_payment_date", sub.nextPaymentDate.toEpochDay())
                put("category", sub.category.key)
                put("status", sub.status.name)
                if (sub.remindDaysBefore == null) putNull("remind_days_before") else put("remind_days_before", sub.remindDaysBefore)
                put("management_url", sub.managementUrl)
                put("notes", sub.notes)
                put("platforms", sub.platforms.joinToString(",") { it.key })
                put("updated_at", now)
            }
            val id = if (sub.id == 0L) {
                values.put("created_at", now)
                db.insertOrThrow("subscriptions", null, values)
            } else {
                db.update("subscriptions", values, "id = ?", arrayOf(sub.id.toString()))
                sub.id
            }
            db.delete("subscription_tags", "subscription_id = ?", arrayOf(id.toString()))
            sub.tagIds.distinct().forEach { tagId ->
                db.insert("subscription_tags", null, ContentValues().apply {
                    put("subscription_id", id)
                    put("tag_id", tagId)
                })
            }
            db.setTransactionSuccessful()
            return id
        } finally {
            db.endTransaction()
        }
    }

    fun updateNextPaymentDate(id: Long, date: LocalDate) {
        writableDatabase.update(
            "subscriptions",
            ContentValues().apply { put("next_payment_date", date.toEpochDay()) },
            "id = ?", arrayOf(id.toString()),
        )
    }

    fun deleteSubscription(id: Long) {
        writableDatabase.delete("subscriptions", "id = ?", arrayOf(id.toString()))
    }

    // ---------- Tags ----------

    fun getTags(): List<Tag> =
        readableDatabase.rawQuery("SELECT id, name, color FROM tags ORDER BY name COLLATE NOCASE", null).use { c ->
            buildList { while (c.moveToNext()) add(Tag(c.getLong(0), c.getString(1), c.getLong(2))) }
        }

    /** Trả về false nếu trùng tên. */
    fun upsertTag(tag: Tag): Boolean {
        val values = ContentValues().apply {
            put("name", tag.name)
            put("color", tag.color)
        }
        return if (tag.id == 0L) {
            writableDatabase.insert("tags", null, values) != -1L
        } else {
            runCatching { writableDatabase.update("tags", values, "id = ?", arrayOf(tag.id.toString())) }.isSuccess
        }
    }

    fun deleteTag(id: Long) {
        writableDatabase.delete("tags", "id = ?", arrayOf(id.toString()))
    }

    fun clearAll() {
        writableDatabase.apply {
            delete("subscription_tags", null, null)
            delete("subscriptions", null, null)
            delete("tags", null, null)
        }
    }

    private fun Cursor.toSubscription(tagMap: Map<Long, List<Long>>): Subscription {
        val id = getLong(getColumnIndexOrThrow("id"))
        val remindIdx = getColumnIndexOrThrow("remind_days_before")
        return Subscription(
            id = id,
            name = getString(getColumnIndexOrThrow("name")),
            amount = getDouble(getColumnIndexOrThrow("amount")),
            currency = Currency.valueOf(getString(getColumnIndexOrThrow("currency"))),
            cycle = BillingCycle.valueOf(getString(getColumnIndexOrThrow("billing_cycle"))),
            nextPaymentDate = LocalDate.ofEpochDay(getLong(getColumnIndexOrThrow("next_payment_date"))),
            category = Category.fromKey(getString(getColumnIndexOrThrow("category"))),
            status = SubscriptionStatus.valueOf(getString(getColumnIndexOrThrow("status"))),
            remindDaysBefore = if (isNull(remindIdx)) null else getInt(remindIdx),
            managementUrl = getString(getColumnIndexOrThrow("management_url")),
            notes = getString(getColumnIndexOrThrow("notes")),
            platforms = getString(getColumnIndexOrThrow("platforms"))
                .split(",").filter { it.isNotBlank() }.mapNotNull { Platform.fromKey(it) },
            tagIds = tagMap[id].orEmpty(),
            createdAt = getLong(getColumnIndexOrThrow("created_at")),
            updatedAt = getLong(getColumnIndexOrThrow("updated_at")),
        )
    }
}
