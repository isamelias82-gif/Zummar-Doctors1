package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SectionsControlCard(
    isPharmaciesEnabled: Boolean,
    isLaboratoriesEnabled: Boolean,
    onTogglePharmacies: (Boolean) -> Unit,
    onToggleLaboratories: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("sections_control_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(TealPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = TealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "التحكم في الأقسام الإضافية (الصيدليات والمختبرات)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "التحكم في ظهور شارة 'قريباً' وتفعيل/تعطيل إمكانية النقر",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pharmacies Section Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isPharmaciesEnabled) TealPrimary.copy(alpha = 0.15f)
                                else Color(0xFFFF9800).copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalPharmacy,
                            contentDescription = null,
                            tint = if (isPharmaciesEnabled) TealPrimary else Color(0xFFE65100),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "قسم الصيدليات الخافرة",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isPharmaciesEnabled) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = if (isPharmaciesEnabled) "مفعّل ومتاح للنقر" else "شارة (قريباً) معطلة للنقر",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isPharmaciesEnabled) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Switch(
                    checked = isPharmaciesEnabled,
                    onCheckedChange = { onTogglePharmacies(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TealPrimary
                    ),
                    modifier = Modifier.testTag("toggle_pharmacies_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Laboratories Section Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isLaboratoriesEnabled) TealPrimary.copy(alpha = 0.15f)
                                else Color(0xFFFF9800).copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Biotech,
                            contentDescription = null,
                            tint = if (isLaboratoriesEnabled) TealPrimary else Color(0xFFE65100),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "قسم المختبرات والتحليلات",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLaboratoriesEnabled) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        ) {
                            Text(
                                text = if (isLaboratoriesEnabled) "مفعّل ومتاح للنقر" else "شارة (قريباً) معطلة للنقر",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (isLaboratoriesEnabled) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Switch(
                    checked = isLaboratoriesEnabled,
                    onCheckedChange = { onToggleLaboratories(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TealPrimary
                    ),
                    modifier = Modifier.testTag("toggle_laboratories_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Explanation / Info Tip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F5F9))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "عند تعطيل أي قسم، تظهر شارة 'قريباً' فوقه في شريط التنقل السفلي ويتم إيقاف النقر نهائياً. عند التفعيل، تختفي شارة 'قريباً' ويتمكن المرضى من الدخول للقسم فوراً.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}
