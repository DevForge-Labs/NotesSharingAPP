package com.pravor.notessharing.ui.features.document.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DocumentDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(8.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(6.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .width(100.dp)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
        )

        repeat(2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(16.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.3f)
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}
