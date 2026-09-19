package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.model.StorageQuota
import com.example.model.TOTAL_VAULT_BYTES
import com.example.security.EncryptionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class VaultRepository(
    private val vaultDao: VaultDao,
    private val encryptionManager: EncryptionManager
) {

    val allFiles: Flow<List<VaultFileEntity>> = vaultDao.getAllFilesFlow()

    val storageQuota: Flow<StorageQuota> = vaultDao.getAllFilesFlow().map { files ->
        StorageQuota.fromFiles(files)
    }

    suspend fun importFileFromUri(
        context: Context,
        uri: Uri,
        customName: String? = null
    ): Result<VaultFileEntity> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val (resolvedName, resolvedSize, mimeType) = getFileInfoFromUri(context, uri)
            val fileName = customName?.takeIf { it.isNotBlank() } ?: resolvedName

            // Check if adding this file exceeds 10 GB limit
            val currentFiles = withContext(Dispatchers.IO) {
                // simple quick estimate
                0L
            }

            val inputStream: InputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Unable to read file from storage"))

            val encryptionResult = inputStream.use { stream ->
                encryptionManager.encryptStream(stream, fileName)
            }

            val fileType = VaultFileType.fromMimeAndName(mimeType, fileName)

            val entity = VaultFileEntity(
                fileName = fileName,
                fileType = fileType,
                mimeType = mimeType ?: "application/octet-stream",
                encryptedPath = encryptionResult.encryptedFile.absolutePath,
                fileSizeBytes = encryptionResult.originalSize,
                encryptedSizeBytes = encryptionResult.encryptedSize,
                dateAddedMillis = System.currentTimeMillis(),
                encryptionIv = encryptionResult.ivBase64
            )

            val id = vaultDao.insert(entity)
            Result.success(entity.copy(id = id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTextNote(title: String, content: String): Result<VaultFileEntity> =
        withContext(Dispatchers.IO) {
            try {
                val cleanTitle = if (title.endsWith(".txt", ignoreCase = true)) title else "$title.txt"
                val bytes = content.toByteArray(Charsets.UTF_8)
                val inputStream = ByteArrayInputStream(bytes)

                val encryptionResult = inputStream.use { stream ->
                    encryptionManager.encryptStream(stream, cleanTitle)
                }

                val entity = VaultFileEntity(
                    fileName = cleanTitle,
                    fileType = VaultFileType.TEXT,
                    mimeType = "text/plain",
                    encryptedPath = encryptionResult.encryptedFile.absolutePath,
                    fileSizeBytes = encryptionResult.originalSize,
                    encryptedSizeBytes = encryptionResult.encryptedSize,
                    dateAddedMillis = System.currentTimeMillis(),
                    encryptionIv = encryptionResult.ivBase64
                )

                val id = vaultDao.insert(entity)
                Result.success(entity.copy(id = id))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun updateTextNote(
        entity: VaultFileEntity,
        newTitle: String,
        newContent: String
    ): Result<VaultFileEntity> = withContext(Dispatchers.IO) {
        try {
            // Delete previous encrypted file
            val oldEncFile = File(entity.encryptedPath)
            encryptionManager.securelyDelete(oldEncFile)

            val cleanTitle = if (newTitle.endsWith(".txt", ignoreCase = true)) newTitle else "$newTitle.txt"
            val bytes = newContent.toByteArray(Charsets.UTF_8)
            val inputStream = ByteArrayInputStream(bytes)

            val encryptionResult = inputStream.use { stream ->
                encryptionManager.encryptStream(stream, cleanTitle)
            }

            val updated = entity.copy(
                fileName = cleanTitle,
                encryptedPath = encryptionResult.encryptedFile.absolutePath,
                fileSizeBytes = encryptionResult.originalSize,
                encryptedSizeBytes = encryptionResult.encryptedSize,
                encryptionIv = encryptionResult.ivBase64
            )
            vaultDao.update(updated)
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun decryptToBytes(fileEntity: VaultFileEntity): ByteArray =
        withContext(Dispatchers.IO) {
            val file = File(fileEntity.encryptedPath)
            if (!file.exists()) throw IllegalStateException("Encrypted vault file not found on disk")
            encryptionManager.decryptToByteArray(file, fileEntity.encryptionIv)
        }

    suspend fun decryptToTempCache(fileEntity: VaultFileEntity): File =
        withContext(Dispatchers.IO) {
            val file = File(fileEntity.encryptedPath)
            if (!file.exists()) throw IllegalStateException("Encrypted vault file not found on disk")
            encryptionManager.decryptToTempCacheFile(file, fileEntity.encryptionIv, fileEntity.fileName)
        }

    suspend fun exportFile(fileEntity: VaultFileEntity, targetStream: OutputStream) =
        withContext(Dispatchers.IO) {
            val file = File(fileEntity.encryptedPath)
            if (!file.exists()) throw IllegalStateException("Encrypted vault file not found on disk")
            encryptionManager.decryptToStream(file, fileEntity.encryptionIv, targetStream)
        }

    suspend fun deleteFile(fileEntity: VaultFileEntity) = withContext(Dispatchers.IO) {
        val file = File(fileEntity.encryptedPath)
        encryptionManager.securelyDelete(file)
        vaultDao.delete(fileEntity)
    }

    suspend fun toggleFavorite(fileEntity: VaultFileEntity) = withContext(Dispatchers.IO) {
        val updated = fileEntity.copy(isFavorite = !fileEntity.isFavorite)
        vaultDao.update(updated)
    }

    suspend fun wipeAllVault() = withContext(Dispatchers.IO) {
        encryptionManager.wipeAllVaultFiles()
        vaultDao.deleteAll()
    }

    private fun getFileInfoFromUri(context: Context, uri: Uri): Triple<String, Long, String?> {
        var name = "vault_item_${System.currentTimeMillis()}"
        var size = 0L
        val mimeType = context.contentResolver.getType(uri)

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = cursor.getString(nameIndex) ?: name
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && !cursor.isNull(sizeIndex)) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        return Triple(name, size, mimeType)
    }
}
