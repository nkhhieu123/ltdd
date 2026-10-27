package com.nhom4.subtrack.domain

/** Tạo nội dung CSV (RFC 4180). Không phụ thuộc Android để kiểm thử bằng JUnit. */
object CsvWriter {
    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + field.replace("\"", "\"\"") + "\""
        else field

    fun build(header: String, rows: List<List<String>>): String = buildString {
        append(header).append("\r\n")
        rows.forEach { row -> append(row.joinToString(",") { escape(it) }).append("\r\n") }
    }
}
