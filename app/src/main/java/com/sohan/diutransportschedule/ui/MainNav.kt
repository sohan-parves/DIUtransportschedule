package com.sohan.diutransportschedule.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.navigation.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNav(vm: HomeViewModel) {

    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "home"

    Scaffold(
        // Use themed background so dark mode doesn't reveal a light window background
        containerColor = MaterialTheme.colorScheme.background,

        // ✅ Premium Floating Bottom Bar (Custom)
        bottomBar = {
            PremiumBottomBar(
                currentRoute = currentRoute,
                onHome = {
                    nav.navigate("home") {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                    }
                },
                onProfile = {
                    nav.navigate("profile") {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                    }
                }
            )
        }

    ) { pad ->

        // ✅ IMPORTANT: NavHost এ padding দিও না (pad screen এ যাবে)
        NavHost(
            navController = nav,
            startDestination = "home"
        ) {
            composable("home") { HomeScreen(vm, pad) }
            composable("profile") { ProfileScreen(vm) }
        }
    }
}

/* ------------------ Premium Bottom Bar ------------------ */

@Composable
private fun PremiumBottomBar(
    currentRoute: String,
    onHome: () -> Unit,
    onProfile: () -> Unit
) {
    // Outer area must stay transparent (no big background behind the rounded bar)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Use app theme colors (respects vm.darkMode)
        val pillColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            shadowElevation = 10.dp,
            color = pillColor
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {

                PremiumBottomItem(
                    selected = currentRoute == "home",
                    label = "Home",
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    onClick = onHome
                )

                PremiumBottomItem(
                    selected = currentRoute == "profile",
                    label = "Profile",
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    onClick = onProfile
                )
            }
        }
    }
}

@Composable
private fun PremiumBottomItem(
    selected: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val activeColor = MaterialTheme.colorScheme.secondary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) activeColor.copy(alpha = 0.10f) else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides (if (selected) activeColor else inactiveColor)
        ) {
            icon()

            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) activeColor else inactiveColor
            )
        }
    }
}