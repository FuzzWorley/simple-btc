package com.bitcoinportfolio.data.api

import com.bitcoinportfolio.domain.model.PricePoint
import okhttp3.ResponseBody
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object StooqCsvParser {

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun parse(body: ResponseBody): List<PricePoint> {
        val text = body.string().trim()
        val lines = text.lines()
        if (lines.size < 2) return emptyList()
        return lines.drop(1).mapNotNull { parseLine(it) }
    }

    private fun parseLine(line: String): PricePoint? {
        return try {
            val parts = line.split(",")
            if (parts.size < 5) return null
            val date = LocalDate.parse(parts[0].trim(), dateFormatter)
            val close = parts[4].trim().toDouble()
            val timestampMs = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            PricePoint(timestampMs = timestampMs, priceUsd = close)
        } catch (_: Exception) {
            null
        }
    }
}
