package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StorageQuota
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DocColor
import com.example.ui.theme.OtherColor
import com.example.ui.theme.PhotoColor
import com.example.ui.theme.TextColor
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultGreen
import com.example.ui.theme.VideoColor

@Composable
fun StorageHeaderCard(
    storageQuota: StorageQuota,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("storage_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(DarkSurfaceBorder, Color(0xFF1E293B))
            )
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Title Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Storage Quota",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "10.00 GB Vault Storage",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Local Encrypted Phone Folder",
                            fontSize = 11.sp,
                            color = VaultGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 100% Offline Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF263353), Color(0xFF1E293B))))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Zero Cloud",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CyberCyan
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Storage Breakdown Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Used Storage",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = StorageQuota.formatBytes(storageQuota.usedBytes),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = " / 10.00 GB",
                            fontSize = 13.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Free Remaining",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = StorageQuota.formatBytes(storageQuota.freeBytes),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented Progress Bar
            SegmentedStorageBar(
                quota = storageQuota,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CategoryQuotaPill(
                    name = "Photos",
                    color = PhotoColor,
                    count = storageQuota.photosCount,
                    bytes = storageQuota.photosBytes
                )
                CategoryQuotaPill(
                    name = "Videos",
                    color = VideoColor,
                    count = storageQuota.videosCount,
                    bytes = storageQuota.videosBytes
                )
                CategoryQuotaPill(
                    name = "Docs/PDF",
                    color = DocColor,
                    count = storageQuota.docsCount,
                    bytes = storageQuota.docsBytes
                )
                CategoryQuotaPill(
                    name = "Notes",
                    color = TextColor,
                    count = storageQuota.textCount,
                    bytes = storageQuota.textBytes
                )
                CategoryQuotaPill(
                    name = "Other",
                    color = OtherColor,
                    count = storageQuota.otherCount,
                    bytes = storageQuota.otherBytes
                )
            }
        }
    }
}

@Composable
private fun SegmentedStorageBar(quota: StorageQuota, modifier: Modifier = Modifier) {
    val total = quota.totalBytes.toFloat()

    val pWeight = (quota.photosBytes / total).coerceAtLeast(0f)
    val vWeight = (quota.videosBytes / total).coerceAtLeast(0f)
    val dWeight = (quota.docsBytes / total).coerceAtLeast(0f)
    val tWeight = (quota.textBytes / total).coerceAtLeast(0f)
    val oWeight = (quota.otherBytes / total).coerceAtLeast(0f)
    val freeWeight = (quota.freeBytes / total).coerceAtLeast(0.001f)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
    ) {
        if (pWeight > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(pWeight)
                    .background(PhotoColor)
            )
        }
        if (vWeight > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(vWeight)
                    .background(VideoColor)
            )
        }
        if (dWeight > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(dWeight)
                    .background(DocColor)
            )
        }
        if (tWeight > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(tWeight)
                    .background(TextColor)
            )
        }
        if (oWeight > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(oWeight)
                    .background(OtherColor)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .weight(freeWeight)
                .background(Color(0xFF1E293B))
        )
    }
}

@Composable
private fun CategoryQuotaPill(
    name: String,
    color: Color,
    count: Int,
    bytes: Long
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
        Text(
            text = if (count > 0) StorageQuota.formatBytes(bytes) else "0 B",
            fontSize = 10.sp,
            color = TextMuted,
            fontWeight = FontWeight.Normal
        )
    }
}
