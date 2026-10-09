package com.gglee.baziyuce.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gglee.baziyuce.BaZiSessionViewModel
import com.gglee.baziyuce.ui.about.AboutScreen
import com.gglee.baziyuce.ui.birth.BirthInputScreen
import com.gglee.baziyuce.ui.chart.ChartResultScreen
import com.gglee.baziyuce.ui.prediction.PredictionHubScreen
import com.gglee.baziyuce.ui.theme.BaZiTheme
import com.gglee.baziyuce.ui.theme.Celadon
import com.gglee.baziyuce.ui.theme.Mist
import com.gglee.baziyuce.ui.theme.Slate

@Composable
fun BaZiApp(session: BaZiSessionViewModel) {
    BaZiTheme {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        val hasChart = session.chart != null

        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Mist.copy(alpha = 0.92f)) {
                    val items = listOf(
                        Triple("起盘", Icons.Outlined.CalendarMonth, true),
                        Triple("八字", Icons.Outlined.GridView, hasChart),
                        Triple("运势", Icons.Outlined.AutoAwesome, hasChart),
                        Triple("关于", Icons.Outlined.Info, true),
                    )
                    items.forEachIndexed { index, (label, icon, enabled) ->
                        NavigationBarItem(
                            selected = tab == index,
                            enabled = enabled,
                            onClick = { tab = index },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Celadon,
                                selectedTextColor = Celadon,
                                unselectedIconColor = Slate,
                                unselectedTextColor = Slate,
                                indicatorColor = Celadon.copy(alpha = 0.12f),
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            when (tab) {
                0 -> BirthInputScreen(
                    session = session,
                    modifier = Modifier.padding(padding),
                    onOpenChart = { tab = 1 },
                )
                1 -> ChartResultScreen(
                    session = session,
                    modifier = Modifier.padding(padding),
                )
                2 -> PredictionHubScreen(
                    session = session,
                    modifier = Modifier.padding(padding),
                )
                else -> AboutScreen(modifier = Modifier.padding(padding))
            }
        }
    }
}
