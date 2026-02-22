package com.sohan.diutransportschedule.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sohan.diutransportschedule.ui.theme.PrimaryBlue

enum class BottomTab(val label: String) {
    HOME("Home"),
    PROFILE("Profile")
}

@Composable
fun PremiumBottomBar(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit
) {
    // floating bar padding
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 6.dp,
            shadowElevation = 16.dp,
            color = Color.White
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PremiumTabItem(
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = "Home",
                    selected = selected == BottomTab.HOME,
                    onClick = { onSelect(BottomTab.HOME) }
                )
                PremiumTabItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = "Profile",
                    selected = selected == BottomTab.PROFILE,
                    onClick = { onSelect(BottomTab.PROFILE) }
                )
            }
        }
    }
}

@Composable
private fun PremiumTabItem(
    icon: @Composable () -> Unit,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha = if (selected) 0.10f else 0f
    val pad by animateDpAsState(if (selected) 12.dp else 10.dp, label = "pad")

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = PrimaryBlue.copy(alpha = bgAlpha),
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = pad),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CompositionLocalProvider(
                LocalContentColor provides if (selected) PrimaryBlue else Color.Black.copy(alpha = 0.55f)
            ) {
                icon()
            }

            Text(
                text = label,
                color = if (selected) PrimaryBlue else Color.Black.copy(alpha = 0.55f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}