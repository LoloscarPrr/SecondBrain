package com.secondbrain.app.domain.capture

import java.time.LocalDate
import java.time.YearMonth

/** Extracts common Chilean bill line-items using OCR text plus visual coordinates. */
class ImageBillInterpreter {
    data class BillItem(val label: String, val amount: Int, val dueDate: LocalDate?)

    private val amountRegex = Regex("\\$\\s*([0-9]{1,3}(?:[.]?[0-9]{3})+|[0-9]+)")
    private val dateRegex = Regex("(?i)(?:pagar\\s+hasta\\s*)?(\\d{1,2})/(\\d{1,2})")

    fun extract(lines: List<OcrTextLine>, today: LocalDate = LocalDate.now()): List<BillItem> {
        if (lines.isEmpty()) return emptyList()

        val ordered = lines.sortedWith(compareBy<OcrTextLine> { it.top }.thenBy { it.left })
        val totalLine = ordered.firstOrNull { it.text.contains("total a pagar", ignoreCase = true) }
        val totalAmountLine = totalLine?.let { total ->
            ordered.firstOrNull { it.top >= total.top && amountRegex.containsMatchIn(it.text) }
        }
        val totalAmount = totalAmountLine?.let { parseAmount(it.text) }

        val upperBoundaryY = ordered
            .filter { line ->
                line.text.contains("revisar detalle", ignoreCase = true) ||
                    line.text.equals("pagar", ignoreCase = true) ||
                    line.text.contains("tus boletas", ignoreCase = true)
            }
            .map { it.top }
            .minOrNull()
            ?: Int.MAX_VALUE

        val startY = totalLine?.top ?: 0
        val candidateLines = ordered.filter { it.top >= startY && it.bottom < upperBoundaryY }

        val amountCandidates = candidateLines
            .mapNotNull { line -> parseAmount(line.text)?.let { amount -> line to amount } }
            .filter { (_, amount) -> amount != totalAmount }
            .distinctBy { it.second }
            .sortedBy { it.first.centerY }

        val dueDates = candidateLines
            .mapNotNull { line ->
                dateRegex.find(line.text)?.let { match ->
                    resolveDate(match.groupValues[1].toInt(), match.groupValues[2].toInt(), today)
                }
            }

        // For the Entel-style bill layout, the two detail amounts appear vertically in the
        // same order as the service icons: mobile first, home second. Restricting this to the
        // upper bill region prevents lower-page amounts such as monthly variation from leaking in.
        if (amountCandidates.size >= 2) {
            val mobile = amountCandidates[0].second
            val home = amountCandidates[1].second
            val mobileDue = dueDates.getOrNull(0) ?: dueDates.firstOrNull()
            val homeDue = dueDates.getOrNull(1) ?: dueDates.firstOrNull()

            if (totalAmount == null || mobile + home == totalAmount) {
                return listOf(
                    BillItem("plan móvil", mobile, mobileDue),
                    BillItem("hogar", home, homeDue)
                )
            }
        }

        return emptyList()
    }

    fun extract(text: String, today: LocalDate = LocalDate.now()): List<BillItem> {
        val synthetic = text.lines()
            .mapIndexedNotNull { index, raw ->
                val line = raw.trim()
                if (line.isBlank()) null else OcrTextLine(line, 0, index * 20, 1000, index * 20 + 18)
            }
        return extract(synthetic, today)
    }

    private fun parseAmount(text: String): Int? = amountRegex.find(text)
        ?.groupValues?.get(1)
        ?.replace(".", "")
        ?.toIntOrNull()

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
