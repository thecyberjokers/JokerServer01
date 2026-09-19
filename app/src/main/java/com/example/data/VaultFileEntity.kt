package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class VaultFileType {
    PHOTO,
    VIDEO,
    DOCUMENT,
    TEXT,
    AUDIO,
    OTHER;

    companion object {
        fun fromMimeAndName(mimeType: String?, fileName: String): VaultFileType {
            val lowerName = fileName.lowercase()
            val lowerMime = (mimeType ?: "").lowercase()

            return when {
                lowerMime.startsWith("image/") ||
                        lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") ||
                        lowerName.endsWith(".png") || lowerName.endsWith(".webp") ||
                        lowerName.endsWith(".gif") || lowerName.endsWith(".heic") ||
                        lowerName.endsWith(".bmp") -> PHOTO

                lowerMime.startsWith("video/") ||
                        lowerName.endsWith(".mp4") || lowerName.endsWith(".mkv") ||
                        lowerName.endsWith(".mov") || lowerName.endsWith(".avi") ||
                        lowerName.endsWith(".webm") || lowerName.endsWith(".3gp") -> VIDEO

                lowerMime == "application/pdf" || lowerName.endsWith(".pdf") ||
                        lowerName.endsWith(".doc") || lowerName.endsWith(".docx") ||
                        lowerName.endsWith(".xls") || lowerName.endsWith(".xlsx") ||
                        lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx") ||
                        lowerMime.contains("officedocument") || lowerMime.contains("msword") -> DOCUMENT

                lowerMime.startsWith("text/") ||
                        lowerName.endsWith(".txt") || lowerName.endsWith(".csv") ||
                        lowerName.endsWith(".json") || lowerName.endsWith(".log") ||
                        lowerName.endsWith(".md") || lowerName.endsWith(".xml") -> TEXT

                lowerMime.startsWith("audio/") ||
                        lowerName.endsWith(".mp3") || lowerName.endsWith(".wav") ||
                        lowerName.endsWith(".aac") || lowerName.endsWith(".m4a") ||
                        lowerName.endsWith(".ogg") || lowerName.endsWith(".flac") -> AUDIO

                else -> OTHER
            }
        }
    }
}

class VaultTypeConverters {
    @TypeConverter
    fun fromVaultFileType(value: VaultFileType): String = value.name

    @TypeConverter
    fun toVaultFileType(value: String): VaultFileType = try {
        VaultFileType.valueOf(value)
    } catch (e: Exception) {
        VaultFileType.OTHER
    }
}

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val fileType: VaultFileType,
    val mimeType: String,
    val encryptedPath: String,
    val fileSizeBytes: Long,
    val encryptedSizeBytes: Long,
    val dateAddedMillis: Long = System.currentTimeMillis(),
    val encryptionIv: String,
    val notes: String = "",
    val isFavorite: Boolean = false
)
