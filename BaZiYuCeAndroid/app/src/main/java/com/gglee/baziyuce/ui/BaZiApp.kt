package com.gglee.baziyuce.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gglee.baziyuce.BaZiSessionViewModel
import com.gglee.baziyuce.ui.about.AboutScreen
import com.gglee.baziyuce.ui.birth.BirthInputScreen
import com.gglee.baziyuce.ui.calendar.FortuneCalendarScreen
import com.gglee.baziyuce.ui.chart.ChartResultScreen
import com.gglee.baziyuce.ui.people.PeopleScreen
import com.gglee.baziyuce.ui.theme.BaZiTheme
import com.gglee.baziyuce.ui.theme.Celadon
import com.gglee.baziyuce.ui.theme.Mist
import com.gglee.baziyuce.ui.theme.Slate

/**
 * Shared UX for iOS mirror: 档案 → 起盘 → 八字 → 日历 → 关于.
 * Calendar replaces the old single-date「运势」tab; day/month/year detail lives in calendar.
 */
@Composable
fun BaZiApp(session: BaZiSessionViewModel) {
    BaZiTheme {
        val context = LocalContext.current
        LaunchedEffect(Unit) { session.attachStore(context.applicationContext) }

        var tab by rememberSaveable { mutableIntStateOf(0) }
        val hasChart = session.chart != null

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = Mist.copy(alpha = 0.96f),
                    tonalElevation = 0.dp,
                    contentColor = Celadon,
                ) {
                    val items = listOf(
                        Triple("档案", Icons.Outlined.PersonOutline, true),
                        Triple("起盘", Icons.Outlined.Schedule, true),
                        Triple("八字", Icons.Outlined.GridView, hasChart),
                        Triple("日历", Icons.Outlined.CalendarMonth, hasChart),
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
                                unselectedIconColor = SoftTabGray,
                                unselectedTextColor = SoftTabGray,
                                disabledIconColor = SoftTabGray.copy(alpha = 0.35f),
                                disabledTextColor = SoftTabGray.copy(alpha = 0.35f),
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            when (tab) {
                0 -> PeopleScreen(
                    session = session,
                    modifier = Modifier.padding(padding),
                    onEditActive = { tab = 1 },
                    onOpenChart = { tab = 2 },
                )
                1 -> BirthInputScreen(
                    session = session,
                    modifier = Modifier.padding(padding),
                    onOpenChart = { tab = 2 },
                    onOpenCalendar = { tab = 3 },
                )
                2 -> ChartResultScreen(session = session, modifier = Modifier.padding(padding))
                3 -> FortuneCalendarScreen(session = session, modifier = Modifier.padding(padding))
                else -> AboutScreen(modifier = Modifier.padding(padding))
            }
        }
    }
}

private val SoftTabGray = Slate.copy(alpha = 0.55f)
