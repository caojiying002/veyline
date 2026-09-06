package com.veyline.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import com.veyline.app.feature.merchant.presentation.list.MerchantListRoute
import com.veyline.app.ui.theme.VeylineTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VeylineTheme {
                // 当前以商家列表作为启动页，后续由首页容器承载
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VeylineTheme.colors.background)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                            ),
                        ),
                ) {
                    MerchantListRoute(
                        onNavigateToCitySelection = {
                            // TODO 接入城市选择导航
                        },
                        onNavigateToMerchantDetail = { _ ->
                            // TODO 接入商家详情导航
                        },
                    )
                }
            }
        }
    }
}
