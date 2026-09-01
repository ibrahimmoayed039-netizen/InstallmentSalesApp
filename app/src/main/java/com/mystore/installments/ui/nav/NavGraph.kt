package com.mystore.installments.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mystore.installments.ui.screens.*
import com.mystore.installments.viewmodel.AppViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val CUSTOMERS = "customers"
    const val CUSTOMER_FORM = "customer_form?customerId={customerId}"
    fun customerForm(id: Long? = null) = "customer_form?customerId=${id ?: 0L}"
    const val NEW_SALE = "new_sale"
    const val INSTALLMENTS = "installments"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val RECEIPT_PREVIEW = "receipt_preview"
    const val CUSTOMER_DETAIL = "customer_detail/{customerId}"
    fun customerDetail(id: Long) = "customer_detail/$id"
    const val PRODUCTS = "products"
    const val PRODUCT_FORM = "product_form?productId={productId}"
    fun productForm(id: Long? = null) = "product_form?productId=${id ?: 0L}"
}

@Composable
fun AppNavGraph(viewModel: AppViewModel) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.DASHBOARD) {

        composable(Routes.DASHBOARD) {
            DashboardScreen(viewModel = viewModel, navController = navController)
        }
        composable(Routes.CUSTOMERS) {
            CustomersScreen(viewModel = viewModel, navController = navController)
        }
        composable(
            Routes.CUSTOMER_FORM,
            arguments = listOf(navArgument("customerId") { type = NavType.LongType; defaultValue = 0L })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            CustomerFormScreen(
                viewModel = viewModel,
                navController = navController,
                customerId = if (customerId == 0L) null else customerId
            )
        }
        composable(
            Routes.CUSTOMER_DETAIL,
            arguments = listOf(navArgument("customerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: 0L
            CustomerDetailScreen(customerId = customerId, viewModel = viewModel, navController = navController)
        }
        composable(Routes.NEW_SALE) {
            NewSaleScreen(viewModel = viewModel, navController = navController)
        }
        composable(Routes.INSTALLMENTS) {
            InstallmentsScreen(viewModel = viewModel, navController = navController)
        }
        composable(Routes.REPORTS) {
            ReportsScreen(viewModel = viewModel)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(viewModel = viewModel)
        }
        composable(Routes.RECEIPT_PREVIEW) {
            ReceiptPreviewScreen(viewModel = viewModel, navController = navController)
        }
        composable(Routes.PRODUCTS) {
            ProductsScreen(viewModel = viewModel, navController = navController)
        }
        composable(
            Routes.PRODUCT_FORM,
            arguments = listOf(navArgument("productId") { type = NavType.LongType; defaultValue = 0L })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getLong("productId") ?: 0L
            ProductFormScreen(
                viewModel = viewModel,
                navController = navController,
                productId = if (productId == 0L) null else productId
            )
        }
    }
}
