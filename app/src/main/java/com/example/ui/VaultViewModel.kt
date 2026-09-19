package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthManager
import com.example.auth.AuthState
import com.example.data.VaultDatabase
import com.example.data.VaultFileEntity
import com.example.data.VaultFileType
import com.example.data.VaultRepository
import com.example.model.StorageQuota
import com.example.security.EncryptionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

enum class VaultCategoryFilter(val label: String) {
    ALL("All Files"),
    PHOTOS("Photos"),
    VIDEOS("Videos"),
    DOCUMENTS("Documents"),
    TEXT("Notes"),
    OTHER("Other")
}

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val authManager = AuthManager(application)
    private val encryptionManager = EncryptionManager(application)
    private val database = VaultDatabase.getDatabase(application)
    private val repository = VaultRepository(database.vaultDao(), encryptionManager)

    private val _authState = MutableStateFlow(authManager.getAuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _selectedCategory = MutableStateFlow(VaultCategoryFilter.ALL)
    val selectedCategory: StateFlow<VaultCategoryFilter> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val storageQuota: StateFlow<StorageQuota> = repository.storageQuota
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StorageQuota()
        )

    val displayedFiles: StateFlow<List<VaultFileEntity>> = combine(
        repository.allFiles,
        _selectedCategory,
        _searchQuery
    ) { files, category, query ->
        files.filter { file ->
            val matchesCategory = when (category) {
                VaultCategoryFilter.ALL -> true
                VaultCategoryFilter.PHOTOS -> file.fileType == VaultFileType.PHOTO
                VaultCategoryFilter.VIDEOS -> file.fileType == VaultFileType.VIDEO
                VaultCategoryFilter.DOCUMENTS -> file.fileType == VaultFileType.DOCUMENT
                VaultCategoryFilter.TEXT -> file.fileType == VaultFileType.TEXT
                VaultCategoryFilter.OTHER -> file.fileType == VaultFileType.OTHER || file.fileType == VaultFileType.AUDIO
            }
            val matchesQuery = if (query.isBlank()) true else {
                file.fileName.contains(query, ignoreCase = true) ||
                        file.notes.contains(query, ignoreCase = true)
            }
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun refreshAuthState() {
        _authState.value = authManager.getAuthState()
    }

    fun completePhoneSetup(phoneNumber: String, pin: String, recoveryHint: String = ""): Boolean {
        val success = authManager.completeSetup(phoneNumber, pin, recoveryHint)
        if (success) {
            refreshAuthState()
            _statusMessage.value = "Secure Local Vault Initialized! 10 GB Ready."
        }
        return success
    }

    fun unlockWithPin(pin: String): Boolean {
        val valid = authManager.verifyPin(pin)
        if (valid) {
            refreshAuthState()
        }
        return valid
    }

    fun lockVault() {
        authManager.lockVault()
        encryptionManager.clearDecryptedCache()
        refreshAuthState()
    }

    fun setCategory(category: VaultCategoryFilter) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun importFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isProcessing.value = true
            var importedCount = 0
            var errorCount = 0

            for (uri in uris) {
                val result = repository.importFileFromUri(getApplication(), uri)
                if (result.isSuccess) {
                    importedCount++
                } else {
                    errorCount++
                }
            }

            _isProcessing.value = false
            _statusMessage.value = when {
                errorCount == 0 -> "$importedCount file(s) encrypted & saved to local vault"
                importedCount > 0 -> "$importedCount imported, $errorCount failed"
                else -> "Failed to import selected files"
            }
        }
    }

    fun createTextNote(title: String, content: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.createTextNote(title, content)
            _isProcessing.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Encrypted note created"
                onDone()
            } else {
                _statusMessage.value = "Failed to create note: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun updateTextNote(file: VaultFileEntity, newTitle: String, newContent: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.updateTextNote(file, newTitle, newContent)
            _isProcessing.value = false
            if (result.isSuccess) {
                _statusMessage.value = "Note updated securely"
                onDone()
            } else {
                _statusMessage.value = "Failed to update note: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    suspend fun readTextContent(file: VaultFileEntity): String {
        return try {
            val bytes = repository.decryptToBytes(file)
            String(bytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "Error decrypting note: ${e.message}"
        }
    }

    suspend fun decryptBytesForPreview(file: VaultFileEntity): ByteArray? {
        return try {
            repository.decryptToBytes(file)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun prepareTempCacheFile(file: VaultFileEntity): File? {
        return try {
            repository.decryptToTempCache(file)
        } catch (e: Exception) {
            null
        }
    }

    fun exportFileToUri(file: VaultFileEntity, targetUri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val context: Context = getApplication()
                val os: OutputStream? = context.contentResolver.openOutputStream(targetUri)
                if (os != null) {
                    os.use { stream ->
                        repository.exportFile(file, stream)
                    }
                    _statusMessage.value = "Decrypted copy exported successfully"
                } else {
                    _statusMessage.value = "Failed to open destination"
                }
            } catch (e: Exception) {
                _statusMessage.value = "Export failed: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun deleteFile(file: VaultFileEntity) {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.deleteFile(file)
            _isProcessing.value = false
            _statusMessage.value = "File permanently wiped from local vault"
        }
    }

    fun toggleFavorite(file: VaultFileEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(file)
        }
    }

    fun wipeAllAndReset() {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.wipeAllVault()
            authManager.resetVaultAuth()
            refreshAuthState()
            _isProcessing.value = false
            _statusMessage.value = "Vault completely wiped & reset"
        }
    }
}
