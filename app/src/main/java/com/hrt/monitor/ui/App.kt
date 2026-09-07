package com.hrt.monitor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.hrt.monitor.R
import com.hrt.monitor.data.AppRepo
import com.hrt.monitor.data.LabRecord
import com.hrt.monitor.data.Prefs
import com.hrt.monitor.ui.screens.ChartScreen
import com.hrt.monitor.ui.screens.HistoryScreen
import com.hrt.monitor.ui.screens.HomeScreen
import com.hrt.monitor.ui.screens.RangeScreen
import com.hrt.monitor.ui.screens.RecordScreen
import com.hrt.monitor.ui.screens.ReportScreen
import com.hrt.monitor.ui.screens.SettingsScreen
import com.hrt.monitor.ui.screens.TimelineScreen
import androidx.compose.foundation.layout.Column

enum class Tab(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.nav_home, Icons.Filled.Home),
    CHARTS(R.string.nav_charts, Icons.Filled.ShowChart),
    TIMELINE(R.string.nav_timeline, Icons.Filled.Timeline),
    REPORT(R.string.nav_report, Icons.Filled.Article),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings)
}

@Composable
fun App() {
    AppRepo.data.collectAsState()
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var editingRecord by rememberSaveable { mutableStateOf<LabRecord?>(null) }
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var showRanges by rememberSaveable { mutableStateOf(false) }
    var showDisclaimer by rememberSaveable { mutableStateOf(!Prefs.agreedDisclaimer) }
    var showProfileSetup by rememberSaveable { mutableStateOf(!Prefs.profileAsked) }
    var editorNonce by rememberSaveable { mutableIntStateOf(0) }

    fun openNewRecord() {
        editingRecord = LabRecord("", System.currentTimeMillis(), "", emptyList())
        editorNonce++
    }

    if (editingRecord != null) {
        key(editorNonce) {
            RecordScreen(
                record = editingRecord!!,
                onClose = { editingRecord = null }
            )
        }
    } else if (showHistory) {
        HistoryScreen(
            onClose = { showHistory = false },
            onEdit = { r -> editingRecord = r; editorNonce++ },
            onAdd = { openNewRecord() }
        )
    } else if (showRanges) {
        RangeScreen(onClose = { showRanges = false })
    } else {
        MainScaffold(
            tabIndex = tabIndex,
            onTab = { tabIndex = it },
            onAddRecord = { openNewRecord() },
            onOpenHistory = { showHistory = true },
            onOpenRanges = { showRanges = true }
        )
    }

    if (showDisclaimer) {
        DisclaimerDialog(onAgree = {
            Prefs.agreedDisclaimer = true
            showDisclaimer = false
        })
    } else if (showProfileSetup) {
        ProfileDialog(onClose = { showProfileSetup = false }, allowSkip = true)
    }
}

@Composable
private fun MainScaffold(
    tabIndex: Int,
    onTab: (Int) -> Unit,
    onAddRecord: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRanges: () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tabIndex == i,
                        onClick = { onTab(i) },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(stringResource(t.labelRes)) }
                    )
                }
            }
        },
        floatingActionButton = {
            if (tabIndex == 0) {
                FloatingActionButton(onClick = onAddRecord) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_record))
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tabIndex) {
                0 -> HomeScreen(onOpenHistory = onOpenHistory)
                1 -> ChartScreen(onOpenRanges = onOpenRanges)
                2 -> TimelineScreen()
                3 -> ReportScreen(onGoSettings = { onTab(4) })
                else -> SettingsScreen(onOpenRanges = onOpenRanges)
            }
        }
    }
}

@Composable
private fun DisclaimerDialog(onAgree: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* 必须确认 */ },
        title = { Text(stringResource(R.string.first_launch_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.disclaimer_full))
            }
        },
        confirmButton = {
            TextButton(onClick = onAgree) { Text(stringResource(R.string.first_launch_agree)) }
        }
    )
}
