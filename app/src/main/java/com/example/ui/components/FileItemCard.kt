package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultFileEntity
import com.example.data.VaultFileType
import com.example.model.StorageQuota
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DocColor
import com.example.ui.theme.OtherColor
import com.example.ui.theme.PhotoColor
import com.example.ui.theme.ShieldGold
import com.example.ui.theme.TextColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultGreen
import com.example.ui.theme.VideoColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FileItemCard(
    file: VaultFileEntity,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }

    val (typeColor, typeIcon) = when (file.fileType) {
        VaultFileType.PHOTO -> Pair(PhotoColor, Icons.Default.Image)
        VaultFileType.VIDEO -> Pair(VideoColor, Icons.Default.Videocam)
        VaultFileType.DOCUMENT -> Pair(DocColor, Icons.Default.Description)
        VaultFileType.TEXT -> Pair(TextColor, Icons.Default.EditNote)
        VaultFileType.AUDIO -> Pair(OtherColor, Icons.Default.FolderZip)
        VaultFileType.OTHER -> Pair(OtherColor, Icons.Default.FolderZip)
    }

    val formattedDate = remember(file.dateAddedMillis) {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        sdf.format(Date(file.dateAddedMillis))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("file_item_${file.id}"),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = file.fileName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (file.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred",
                            tint = ShieldGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = StorageQuota.formatBytes(file.fileSizeBytes),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = CyberCyan
                    )

                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = TextMuted
                    )

                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceElevated
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = VaultGreen,
                                modifier = Modifier.size(9.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "AES-256",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultGreen
                            )
                        }
                    }
                }
            }

            // Overflow Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(36.dp).testTag("file_options_${file.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(DarkSurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Open / View", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Visibility, null, tint = CyberCyan) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Export to Device", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.FileDownload, null, tint = VaultGreen) },
                        onClick = {
                            menuExpanded = false
                            onExport()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                if (file.isFavorite) "Remove from Starred" else "Add to Starred",
                                color = TextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (file.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                null,
                                tint = ShieldGold
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onToggleFavorite()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Cryptographic Info", color = TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Info, null, tint = TextSecondary) },
                        onClick = {
                            menuExpanded = false
                            onShowInfo()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Shred & Delete", color = DangerRed) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = DangerRed) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
