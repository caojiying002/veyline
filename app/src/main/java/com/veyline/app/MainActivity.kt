package com.veyline.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.veyline.app.feature.merchant.presentation.city.MerchantCitySelectionRoute
import com.veyline.app.ui.theme.VeylineTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeylineTheme {
                MerchantCitySelectionRoute(
                    onNavigateBack = onBackPressedDispatcher::onBackPressed,
                )
            }
        }
    }
}
