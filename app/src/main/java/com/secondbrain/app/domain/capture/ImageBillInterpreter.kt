package com.secondbrain.app.domain.capture

import java.time.LocalDate
import java.time.YearMonth

/** Extracts common Chilean bill line-items from OCR text without sending the image to cloud AI. */
class ImageBillInterpreter {
    data class BillItem(val label: String, val amount: Int, val dueDate: LocalDate?)

    fun extract(text: String, today: LocalDate = LocalDate.now()): List<BillItem> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        val dueDates = Regex("(?i)(?:pagar\\s+hasta\\s*)?(\\d{1,2})/(\\d{1,2})")
            .findAll(text)
            .mapNotNull { m -> resolveDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), today) }
            .toList()
        val defaultDue = dueDates.firstOrNull()

        val amountRegex = Regex("\\$\\s*([0-9]{1,3}(?:[.]?[0-9]{3})+|[0-9]+)")
        val amounts = lines.mapNotNull { line ->
            amountRegex.find(line)?.groupValues?.get(1)?.replace(".", "")?.toIntOrNull()?.let { line to it }
        }

        fun findAmountNear(vararg terms: String): Int? {
            val index = lines.indexOfFirst { line -> terms.any { line.contains(it, ignoreCase = true) } }
            if (index < 0) return null
            for (i in index downTo maxOf(0, index - 2)) amountRegex.find(lines[i])?.let { return it.groupValues[1].replace(".", "").toIntOrNull() }
            for (i in index..minOf(lines.lastIndex, index + 2)) amountRegex.find(lines[i])?.let { return it.groupValues[1].replace(".", "").toIntOrNull() }
            return null
        }

        val mobile = findAmountNear("móvil", "movil")
        val home = findAmountNear("hogar")
        if (mobile != null || home != null) {
            return buildList {
                mobile?.let { add(BillItem("plan móvil", it, defaultDue)) }
                home?.let { add(BillItem("hogar", it, dueDates.getOrNull(1) ?: defaultDue)) }
            }
        }

        // Fallback for layouts where OCR loses the icons/labels but preserves the two detail amounts.
        val total = Regex("(?i)total\\s+a\\s+pagar[\\s:]*\\$?\\s*([0-9.]+)").find(text)
            ?.groupValues?.get(1)?.replace(".", "")?.toIntOrNull()
        val detail = amounts.map { it.second }.filter { it != total }.distinct()
        return if (detail.size == 2 && total != null && detail.sum() == total) {
            listOf(BillItem("cuenta 1", detail[0], defaultDue), BillItem("cuenta 2", detail[1], dueDates.getOrNull(1) ?: defaultDue))
        } else emptyList()
    }

    private fun resolveDate(day: Int, month: Int, today: LocalDate): LocalDate? {
        var ym = runCatching { YearMonth.of(today.year, month) }.getOrNull() ?: return null
        if (day !in 1..ym.lengthOfMonth()) return null
        var date = ym.atDay(day)
        if (date.isBefore(today)) {
            ym = ym.plusYears(1)
            if (day !in 1..ym.lengthOfMonth()) return null
            date = ym.atDay(day)
        }
        return date
    }
}
