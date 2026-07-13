package com.cemcakmak.hydrotracker.data.backup

import android.content.Context
import android.net.Uri
import com.cemcakmak.hydrotracker.data.database.entities.WaterIntakeEntry
import com.cemcakmak.hydrotracker.data.database.repository.WaterIntakeRepository
import com.cemcakmak.hydrotracker.data.models.EntrySource
import com.cemcakmak.hydrotracker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Handles exporting and importing HydroTracker intake data as CSV or JSON.
 *
 * Only non-hidden, HydroTracker-owned entries are exported. Hidden entries come from other health
 * apps and are intentionally excluded.
 */
object DataBackupManager {

    private const val BACKUP_VERSION = 1
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    enum class BackupFormat {
        CSV,
        JSON
    }

    @Serializable
    private data class BackupContainer(
        val version: Int = BACKUP_VERSION,
        val exportDate: String,
        val appVersion: String,
        val entries: List<BackupEntry>
    )

    @Serializable
    private data class BackupEntry(
        val timestamp: Long,
        val date: String,
        val amount: Double,
        val effectiveAmount: Double,
        val beverageType: String,
        val beverageMultiplier: Double?,
        val containerType: String,
        val containerVolume: Double,
        val note: String?,
        val iconType: String,
        val iconName: String
    )

    /**
     * Export non-hidden HydroTracker entries to the supplied URI.
     */
    suspend fun export(
        context: Context,
        repository: WaterIntakeRepository,
        uri: Uri,
        format: BackupFormat
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val entries = repository.getAllEntriesForExport()
            val backupEntries = entries.map { it.toBackupEntry() }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream).use { writer ->
                    when (format) {
                        BackupFormat.CSV -> writer.write(backupEntries.toCsv())
                        BackupFormat.JSON -> writer.write(backupEntries.toJson())
                    }
                }
            } ?: return@withContext Result.failure(Exception("Could not open output stream"))

            Result.success(backupEntries.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import entries from the supplied URI, merging with existing data.
     */
    suspend fun import(
        context: Context,
        repository: WaterIntakeRepository,
        uri: Uri
    ): Result<DataImportResult> = withContext(Dispatchers.IO) {
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
            } ?: return@withContext Result.failure(Exception("Could not open input stream"))

            val backupEntries = when {
                text.trim().startsWith("{") -> parseJson(text)
                else -> parseCsv(text)
            }

            var imported = 0
            var skipped = 0

            backupEntries.forEach { backupEntry ->
                val entry = WaterIntakeEntry(
                    amount = backupEntry.amount,
                    timestamp = backupEntry.timestamp,
                    date = backupEntry.date,
                    containerType = backupEntry.containerType,
                    containerVolume = backupEntry.containerVolume,
                    note = backupEntry.note,
                    beverageType = backupEntry.beverageType,
                    beverageMultiplier = backupEntry.beverageMultiplier,
                    iconType = backupEntry.iconType,
                    iconName = backupEntry.iconName,
                    source = EntrySource.LOCAL
                )

                if (repository.isDuplicateEntry(entry)) {
                    skipped++
                } else {
                    repository.addImportedWaterEntry(entry)
                    imported++
                }
            }

            Result.success(DataImportResult(imported, skipped))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun WaterIntakeEntry.toBackupEntry(): BackupEntry = BackupEntry(
        timestamp = timestamp,
        date = date,
        amount = amount,
        effectiveAmount = getEffectiveHydrationAmount(),
        beverageType = beverageType,
        beverageMultiplier = beverageMultiplier,
        containerType = containerType,
        containerVolume = containerVolume,
        note = note,
        iconType = iconType,
        iconName = iconName
    )

    private fun List<BackupEntry>.toJson(): String {
        val container = BackupContainer(
            exportDate = isoFormatter.format(Instant.now()),
            appVersion = BuildConfig.VERSION_NAME,
            entries = this
        )
        return json.encodeToString(container)
    }

    private fun parseJson(text: String): List<BackupEntry> {
        val container = json.decodeFromString(BackupContainer.serializer(), text)
        return container.entries
    }

    private fun List<BackupEntry>.toCsv(): String {
        val lines = mutableListOf<String>()
        lines.add(
            "timestamp,date,amount,effective_amount,beverage_type,beverage_multiplier," +
                "container_type,container_volume,note,icon_type,icon_name"
        )
        forEach { entry ->
            lines.add(
                buildString {
                    append(entry.timestamp).append(",")
                    append(escapeCsv(entry.date)).append(",")
                    append(entry.amount).append(",")
                    append(entry.effectiveAmount).append(",")
                    append(escapeCsv(entry.beverageType)).append(",")
                    append(entry.beverageMultiplier ?: "").append(",")
                    append(escapeCsv(entry.containerType)).append(",")
                    append(entry.containerVolume).append(",")
                    append(escapeCsv(entry.note ?: "")).append(",")
                    append(escapeCsv(entry.iconType)).append(",")
                    append(escapeCsv(entry.iconName))
                }
            )
        }
        return lines.joinToString("\n")
    }

    private fun parseCsv(text: String): List<BackupEntry> {
        val lines = text.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) return emptyList()

        val dataLines = lines.drop(1)
        return dataLines.map { line ->
            val values = parseCsvLine(line)
            BackupEntry(
                timestamp = values[0].toLong(),
                date = values[1],
                amount = values[2].toDouble(),
                effectiveAmount = values[3].toDoubleOrNull() ?: values[2].toDouble(),
                beverageType = values[4],
                beverageMultiplier = values[5].toDoubleOrNull(),
                containerType = values[6],
                containerVolume = values[7].toDouble(),
                note = values[8].takeIf { it.isNotBlank() },
                iconType = values.getOrElse(9) { "DRAWABLE" },
                iconName = values.getOrElse(10) { "water_filled" }
            )
        }
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            when (val char = line[i]) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        result.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(char)
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    data class DataImportResult(
        val imported: Int,
        val skipped: Int
    )
}
