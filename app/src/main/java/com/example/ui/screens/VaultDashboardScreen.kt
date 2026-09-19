package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VaultFileEntity
import com.example.data.VaultFileType
import com.example.ui.VaultCategoryFilter
import com.example.ui.VaultViewModel
import com.example.ui.components.AddFilesBottomSheet
import com.example.ui.components.CategoryFilterChips
import com.example.ui.components.CreateTextNoteDialog
import com.example.ui.components.FileCryptographicInfoDialog
import com.example.ui.components.FileItemCard
import com.example.ui.components.ImageViewerDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.StorageHeaderCard
import com.example.ui.components.TextNoteEditorDialog
import com.example.ui.components.openFileWithExternalApp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultGreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    viewModel: VaultViewModel,
    onLockVault: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val storageQuota by viewModel.storageQuota.collectAsStateWithLifecycle()
    val displayedFiles by viewModel.displayedFiles.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    var showSearch by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showCreateNoteDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    var previewImageFile by remember { mutableStateOf<VaultFileEntity?>(null) }
    var editTextFile by remember { mutableStateOf<VaultFileEntity?>(null) }
    var infoFile by remember { mutableStateOf<VaultFileEntity?>(null) }
    var deleteConfirmFile by remember { mutableStateOf<VaultFileEntity?>(null) }
    var fileToExport by remember { mutableStateOf<VaultFileEntity?>(null) }

    // Activity Result Launchers
    val pickVisualMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris)
        }
    }

    val openDocumentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris)
        }
    }

    val createExportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { targetUri: Uri? ->
        if (targetUri != null && fileToExport != null) {
            viewModel.exportFileToUri(fileToExport!!, targetUri)
            fileToExport = null
        }
    }

    // Status Message Toast / Snackbar
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Secure Vault",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "100% Offline • Encrypted",
                                fontSize = 10.sp,
                                color = VaultGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSearch = !showSearch },
                        modifier = Modifier.testTag("action_search_toggle")
                    ) {
                        Icon(
                            imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (showSearch) CyberCyan else TextSecondary
                        )
                    }

                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("action_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onLockVault,
                        modifier = Modifier.testTag("action_lock_vault")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = CyberCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DarkBg
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = CyberCyan,
                contentColor = Color(0xFF00363D),
                shape = CircleShape,
                modifier = Modifier
                    .padding(8.dp)
                    .testTag("fab_add_file")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add File to Vault",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar (Expandable)
            AnimatedVisibility(
                visible = showSearch,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search files by name...", color = TextMuted) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_input_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0xFF263353),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            }

            // Storage Meter Card (10 GB)
            StorageHeaderCard(
                storageQuota = storageQuota,
                onOpenDetails = { showSettingsDialog = true },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category Filter Chips
            CategoryFilterChips(
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.setCategory(it) },
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 12.dp)
            )

            // Processing indicator
            if (isProcessing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Encrypting / processing local files...",
                        fontSize = 12.sp,
                        color = CyberCyan
                    )
                }
            }

            // Main Files List or Empty State
            if (displayedFiles.isEmpty()) {
                EmptyVaultState(
                    category = selectedCategory,
                    onAddFiles = { showAddSheet = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    items(
                        items = displayedFiles,
                        key = { it.id }
                    ) { file ->
                        FileItemCard(
                            file = file,
                            onClick = {
                                when (file.fileType) {
                                    VaultFileType.PHOTO -> {
                                        previewImageFile = file
                                    }
                                    VaultFileType.TEXT -> {
                                        editTextFile = file
                                    }
                                    VaultFileType.DOCUMENT, VaultFileType.VIDEO, VaultFileType.AUDIO, VaultFileType.OTHER -> {
                                        scope.launch {
                                            val temp = viewModel.prepareTempCacheFile(file)
                                            if (temp != null) {
                                                openFileWithExternalApp(context, temp, file.mimeType)
                                            } else {
                                                Toast.makeText(context, "Error preparing file for viewing", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            },
                            onExport = {
                                fileToExport = file
                                createExportFileLauncher.launch(file.fileName)
                            },
                            onShowInfo = {
                                infoFile = file
                            },
                            onDelete = {
                                deleteConfirmFile = file
                            },
                            onToggleFavorite = {
                                viewModel.toggleFavorite(file)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet to Add Files
    if (showAddSheet) {
        AddFilesBottomSheet(
            onDismiss = { showAddSheet = false },
            onPickMedia = {
                pickVisualMediaLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onPickDocuments = {
                openDocumentsLauncher.launch(arrayOf("*/*"))
            },
            onCreateTextNote = {
                showCreateNoteDialog = true
            }
        )
    }

    // Create Note Dialog
    if (showCreateNoteDialog) {
        CreateTextNoteDialog(
            onDismiss = { showCreateNoteDialog = false },
            onSave = { title, content ->
                viewModel.createTextNote(title, content) {
                    showCreateNoteDialog = false
                }
            }
        )
    }

    // Image Viewer Dialog
    previewImageFile?.let { file ->
        ImageViewerDialog(
            file = file,
            onDecryptBytes = { viewModel.decryptBytesForPreview(it) },
            onExport = {
                fileToExport = file
                createExportFileLauncher.launch(file.fileName)
            },
            onDelete = {
                viewModel.deleteFile(file)
                previewImageFile = null
            },
            onDismiss = { previewImageFile = null }
        )
    }

    // Text Note Editor Dialog
    editTextFile?.let { file ->
        TextNoteEditorDialog(
            file = file,
            onReadContent = { viewModel.readTextContent(it) },
            onSaveContent = { f, title, content ->
                viewModel.updateTextNote(f, title, content) {
                    editTextFile = null
                }
            },
            onDismiss = { editTextFile = null }
        )
    }

    // Cryptographic Info Dialog
    infoFile?.let { file ->
        FileCryptographicInfoDialog(
            file = file,
            onDismiss = { infoFile = null }
        )
    }

    // Settings & Privacy Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            phoneNumber = authState.phoneNumber,
            storageQuota = storageQuota,
            onLockVault = {
                showSettingsDialog = false
                onLockVault()
            },
            onResetVault = {
                showSettingsDialog = false
                viewModel.wipeAllAndReset()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // Delete Confirmation Dialog
    deleteConfirmFile?.let { file ->
        AlertDialog(
            onDismissRequest = { deleteConfirmFile = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Permanently Shred File?",
                    color = DangerRed,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"${file.fileName}\"?\n\nThe file will be securely shredded from local storage with zeros and removed from the encrypted vault.",
                    color = TextPrimary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(file)
                        deleteConfirmFile = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Shred & Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteConfirmFile = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun EmptyVaultState(
    category: VaultCategoryFilter,
    onAddFiles: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF131B2E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (category == VaultCategoryFilter.ALL) "Your Vault is Empty" else "No ${category.label} Found",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Add photos, videos, documents, PDFs, or encrypted text notes to start using your 10 GB private offline vault.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onAddFiles,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color(0xFF00363D)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("empty_state_add_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Files Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}
