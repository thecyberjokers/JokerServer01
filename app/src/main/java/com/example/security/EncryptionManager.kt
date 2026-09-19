package com.example.security

import android.content.Context
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class EncryptionResult(
    val encryptedFile: File,
    val originalSize: Long,
    val encryptedSize: Long,
    val ivBase64: String
)

/**
 * Manages local AES-256 GCM encryption at rest for the 10 GB Secure Vault.
 * All files are stored in the private internal sandboxed directory and cannot
 * be decrypted or accessed by external applications without the vault master key.
 */
class EncryptionManager(private val context: Context) {

    private val vaultDir: File by lazy {
        File(context.filesDir, "encrypted_vault").apply {
            if (!exists()) mkdirs()
        }
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, "vault_cache").apply {
            if (!exists()) mkdirs()
        }
    }

    private val secretKey: SecretKey by lazy {
        getOrCreateMasterKey()
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyPref = context.getSharedPreferences("vault_crypto_pref", Context.MODE_PRIVATE)
        val savedKeyBase64 = keyPref.getString("master_key_v1", null)
        if (savedKeyBase64 != null) {
            val keyBytes = Base64.decode(savedKeyBase64, Base64.NO_WRAP)
            return SecretKeySpec(keyBytes, "AES")
        }

        // Generate a new 256-bit AES master key
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256, SecureRandom())
        val generatedKey = keyGen.generateKey()
        val encoded = Base64.encodeToString(generatedKey.encoded, Base64.NO_WRAP)
        keyPref.edit().putString("master_key_v1", encoded).apply()
        return generatedKey
    }

    /**
     * Encrypts the incoming input stream into an AES-256 GCM encrypted file inside the private vault folder.
     */
    fun encryptStream(inputStream: InputStream, suggestedName: String): EncryptionResult {
        val uniqueId = UUID.randomUUID().toString()
        val encryptedFile = File(vaultDir, "vault_$uniqueId.enc")

        // 12 bytes IV for AES-GCM
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(128, iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        var originalSize = 0L
        val buffer = ByteArray(32 * 1024)

        FileOutputStream(encryptedFile).use { fos ->
            CipherOutputStream(fos, cipher).use { cos ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    cos.write(buffer, 0, bytesRead)
                    originalSize += bytesRead
                }
            }
        }

        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        return EncryptionResult(
            encryptedFile = encryptedFile,
            originalSize = originalSize,
            encryptedSize = encryptedFile.length(),
            ivBase64 = ivBase64
        )
    }

    /**
     * Decrypts an encrypted vault file into memory (useful for photos, text, small files).
     */
    fun decryptToByteArray(encryptedFile: File, ivBase64: String): ByteArray {
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(128, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val output = ByteArrayOutputStream()
        FileInputStream(encryptedFile).use { fis ->
            CipherInputStream(fis, cipher).use { cis ->
                val buffer = ByteArray(32 * 1024)
                var bytesRead: Int
                while (cis.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
        return output.toByteArray()
    }

    /**
     * Decrypts an encrypted vault file to an OutputStream (e.g. for exporting to device downloads).
     */
    fun decryptToStream(encryptedFile: File, ivBase64: String, targetStream: OutputStream) {
        val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
        val spec = GCMParameterSpec(128, iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        FileInputStream(encryptedFile).use { fis ->
            CipherInputStream(fis, cipher).use { cis ->
                val buffer = ByteArray(32 * 1024)
                var bytesRead: Int
                while (cis.read(buffer).also { bytesRead = it } != -1) {
                    targetStream.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    /**
     * Decrypts temporarily to cache directory so external viewers (like PDF reader, Video player)
     * can open it securely via FileProvider.
     */
    fun decryptToTempCacheFile(encryptedFile: File, ivBase64: String, fileName: String): File {
        // Clear previous cache items to avoid lingering files
        cacheDir.listFiles()?.forEach { it.delete() }

        val tempFile = File(cacheDir, fileName)
        FileOutputStream(tempFile).use { fos ->
            decryptToStream(encryptedFile, ivBase64, fos)
        }
        return tempFile
    }

    /**
     * Securely deletes the file by overwriting bytes with zeros before removing.
     */
    fun securelyDelete(encryptedFile: File): Boolean {
        return try {
            if (encryptedFile.exists()) {
                val length = encryptedFile.length()
                if (length > 0) {
                    FileOutputStream(encryptedFile).use { fos ->
                        val zeros = ByteArray((minOf(length, 64 * 1024)).toInt())
                        var remaining = length
                        while (remaining > 0) {
                            val toWrite = minOf(remaining, zeros.size.toLong()).toInt()
                            fos.write(zeros, 0, toWrite)
                            remaining -= toWrite
                        }
                    }
                }
                encryptedFile.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            encryptedFile.delete()
        }
    }

    /**
     * Clears all cached decrypted temporary files.
     */
    fun clearDecryptedCache() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Securely wipes all encrypted files in the vault.
     */
    fun wipeAllVaultFiles() {
        vaultDir.listFiles()?.forEach { securelyDelete(it) }
        clearDecryptedCache()
    }
}
