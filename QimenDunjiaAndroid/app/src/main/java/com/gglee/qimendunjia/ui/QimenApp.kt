package com.gglee.qimendunjia.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gglee.qimendunjia.AppContainer
import com.gglee.qimendunjia.engine.QimenChart
import com.gglee.qimendunjia.ui.cast.CastScreen
import com.gglee.qimendunjia.ui.chart.ChartScreen
import com.gglee.qimendunjia.ui.history.HistoryScreen
import com.gglee.qimendunjia.ui.interpretation.InterpretationScreen
import com.gglee.qimendunjia.ui.settings.SettingsScreen
import com.gglee.qimendunjia.ui.theme.QimenTheme

private sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Cast : Tab("tab_cast", "起局", Icons.Outlined.Star)
    data object History : Tab("tab_history", "历史", Icons.Outlined.History)
    data object Settings : Tab("tab_settings", "设置", Icons.Outlined.Settings)
}

private const val ROUTE_CHART = "chart"
private const val ROUTE_INTERPRETATION = "interpretation"

@Composable
fun QimenApp(container: AppContainer) {
    QimenTheme {
        val navController = rememberNavController()
        val session: ChartSessionViewModel = viewModel()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route
        val showBottomBar = currentRoute in listOf(
            Tab.Cast.route,
            Tab.History.route,
            Tab.Settings.route,
        )

        fun openChart(chart: QimenChart) {
            session.currentChart = chart
            navController.navigate(ROUTE_CHART) {
                launchSingleTop = true
            }
        }

        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar {
                        listOf(Tab.Cast, Tab.History, Tab.Settings).forEach { tab ->
                            NavigationBarItem(
                                selected = currentRoute == tab.route,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Tab.Cast.route,
                modifier = Modifier.padding(padding),
            ) {
                composable(Tab.Cast.route) {
                    CastScreen(
                        container = container,
                        onChartReady = ::openChart,
                    )
                }
                composable(Tab.History.route) {
                    HistoryScreen(
                        container = container,
                        onOpenChart = ::openChart,
                    )
                }
                composable(Tab.Settings.route) {
                    SettingsScreen(aiSettings = container.aiSettings)
                }
                composable(ROUTE_CHART) {
                    val chart = session.currentChart
                    if (chart == null) {
                        Text("无盘面数据")
                    } else {
                        ChartScreen(
                            chart = chart,
                            onOpenInterpretation = {
                                navController.navigate(ROUTE_INTERPRETATION)
                            },
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
                composable(ROUTE_INTERPRETATION) {
                    val chart = session.currentChart
                    if (chart == null) {
                        Text("无盘面数据")
                    } else {
                        InterpretationScreen(
                            chart = chart,
                            container = container,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
