package com.example.model

import com.example.data.VaultFileEntity
import com.example.data.VaultFileType
import java.text.DecimalFormat

val TOTAL_VAULT_BYTES: Long = 10L * 1024L * 1024L * 1024L // Exactly 10 GB

data class StorageQuota(
    val totalBytes: Long = TOTAL_VAULT_BYTES,
    val usedBytes: Long = 0L,
    val photosBytes: Long = 0L,
    val photosCount: Int = 0,
    val videosBytes: Long = 0L,
    val videosCount: Int = 0,
    val docsBytes: Long = 0L,
    val docsCount: Int = 0,
    val textBytes: Long = 0L,
    val textCount: Int = 0,
    val otherBytes: Long = 0L,
    val otherCount: Int = 0
) {
    val freeBytes: Long
        get() = maxOf(0L, totalBytes - usedBytes)

    val progressFraction: Float
        get() = if (totalBytes > 0) {
            (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val percentString: String
        get() {
            val pct = (progressFraction * 100)
            return if (pct < 0.1 && pct > 0) "<0.1%" else "${String.format("%.1f", pct)}%"
        }

    val isNearlyFull: Boolean
        get() = progressFraction > 0.90f

    companion object {
        fun fromFiles(files: List<VaultFileEntity>): StorageQuota {
            var totalUsed = 0L
            var pBytes = 0L
            var pCount = 0
            var vBytes = 0L
            var vCount = 0
            var dBytes = 0L
            var dCount = 0
            var tBytes = 0L
            var tCount = 0
            var oBytes = 0L
            var oCount = 0

            for (f in files) {
                totalUsed += f.fileSizeBytes
                when (f.fileType) {
                    VaultFileType.PHOTO -> {
                        pBytes += f.fileSizeBytes
                        pCount++
                    }
                    VaultFileType.VIDEO -> {
                        vBytes += f.fileSizeBytes
                        vCount++
                    }
                    VaultFileType.DOCUMENT -> {
                        dBytes += f.fileSizeBytes
                        dCount++
                    }
                    VaultFileType.TEXT -> {
                        tBytes += f.fileSizeBytes
                        tCount++
                    }
                    VaultFileType.AUDIO, VaultFileType.OTHER -> {
                        oBytes += f.fileSizeBytes
                        oCount++
                    }
                }
            }

            return StorageQuota(
                totalBytes = TOTAL_VAULT_BYTES,
                usedBytes = totalUsed,
                photosBytes = pBytes,
                photosCount = pCount,
                videosBytes = vBytes,
                videosCount = vCount,
                docsBytes = dBytes,
                docsCount = dCount,
                textBytes = tBytes,
                textCount = tCount,
                otherBytes = oBytes,
                otherCount = oCount
            )
        }

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
            val df = DecimalFormat("#,##0.##")
            return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
        }
    }
}
