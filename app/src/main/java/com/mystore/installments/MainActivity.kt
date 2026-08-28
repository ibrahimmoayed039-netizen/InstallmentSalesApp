package com.mystore.installments

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.mystore.installments.ui.nav.AppNavGraph
import com.mystore.installments.ui.theme.InstallmentSalesTheme
import com.mystore.installments.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // فرض اتجاه الكتابة من اليمين لليسار لكامل الواجهة العربية
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                InstallmentSalesTheme {
                    AppNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}
