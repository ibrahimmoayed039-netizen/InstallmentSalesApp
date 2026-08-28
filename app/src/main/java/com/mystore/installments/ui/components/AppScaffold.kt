package com.mystore.installments.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mystore.installments.ui.nav.Routes

data class BottomItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomItems = listOf(
    BottomItem(Routes.DASHBOARD, "الرئيسية", Icons.Filled.Home),
    BottomItem(Routes.CUSTOMERS, "العملاء", Icons.Filled.Person),
    BottomItem(Routes.NEW_SALE, "بيع جديد", Icons.Filled.AddShoppingCart),
    BottomItem(Routes.INSTALLMENTS, "الأقساط", Icons.Filled.Payments),
    BottomItem(Routes.REPORTS, "التقارير", Icons.Filled.Assessment)
)

// شريط تنقل سفلي مشترك بين الشاشات الرئيسية
@Composable
fun AppBottomBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        bottomItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}
