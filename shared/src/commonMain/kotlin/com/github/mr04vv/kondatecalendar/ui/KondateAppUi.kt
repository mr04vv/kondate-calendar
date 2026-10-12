package com.github.mr04vv.kondatecalendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.mr04vv.kondatecalendar.auth.GoogleSignIn
import com.github.mr04vv.kondatecalendar.domain.ShoppingSource
import com.github.mr04vv.kondatecalendar.feedback.FeedbackScreen
import kotlinx.coroutines.launch

private val SnackbarBottomOffset = 88.dp

private val Tab.label: String
    get() = when (this) {
        Tab.CALENDAR -> "カレンダー"
        Tab.TODAY -> "今日"
        Tab.SHOPPING -> "買い物リスト"
    }

private val Tab.icon: ImageVector
    get() = when (this) {
        Tab.CALENDAR -> Icons.Default.DateRange
        Tab.TODAY -> Icons.Default.Home
        Tab.SHOPPING -> Icons.Default.ShoppingCart
    }

/** The whole app: a notice without Firebase config, the login screen when signed out, otherwise the tabs. */
@Composable
fun KondateAppUi(vm: KondateViewModel, signIn: GoogleSignIn) {
    val user by vm.user.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when {
            !vm.configured -> NotConfiguredScreen()
            user == null -> LoginScreen(onSignIn = { vm.signIn(signIn) })
            else -> SignedInUi(vm, user?.email, snackbar)
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = SnackbarBottomOffset),
        )
    }
}

@Composable
private fun SignedInUi(vm: KondateViewModel, email: String?, snackbar: SnackbarHostState) {
    val dishes by vm.dishes.collectAsStateWithLifecycle()
    val plan by vm.plan.collectAsStateWithLifecycle()
    val shopping by vm.shopping.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    // Keeps each tab's rememberSaveable state (calendar mode, scroll, day) while another tab is shown.
    val tabStates = rememberSaveableStateHolder()
    val addToShopping: (List<ShoppingSource>) -> Unit = { sources ->
        vm.addToShopping(sources) { added ->
            val message = if (added == 0) "すでに買い物リストに入っています" else "材料 $added 品を買い物リストに追加しました"
            scope.launch { snackbar.showSnackbar(message) }
        }
    }

    val replaceShopping: (List<ShoppingSource>) -> Unit = { sources ->
        vm.replaceShopping(sources) { count ->
            scope.launch { snackbar.showSnackbar("買い物リストをこの週の材料 $count 品に入れ替えました") }
        }
    }
    // The week whose ingredients wait for the user to confirm replacing a non-empty list.
    var confirmingWeek by remember { mutableStateOf<List<ShoppingSource>?>(null) }

    // Rows for days already past leave the list each time it is opened.
    LaunchedEffect(vm.tab) { if (vm.tab == Tab.SHOPPING) vm.deleteExpiredShopping() }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = vm.tab == tab,
                            onClick = { vm.tab = tab },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                    // Not a tab: opens the settings on top of the current tab.
                    NavigationBarItem(
                        selected = false,
                        onClick = { vm.open(Screen.Settings) },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("設定") },
                    )
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                tabStates.SaveableStateProvider(vm.tab) {
                    when (vm.tab) {
                        Tab.CALENDAR -> CalendarScreen(
                            plan = plan,
                            onTapSlot = vm::tapSlot,
                            onMoveSlot = vm::moveSlot,
                            onReplaceShopping = { sources ->
                                if (shopping.isEmpty()) replaceShopping(sources) else confirmingWeek = sources
                            },
                        )
                        Tab.TODAY -> TodayScreen(
                            plan = plan,
                            onOpenRecipe = { vm.open(Screen.Recipe(it.id)) },
                            onPick = { vm.picking = it },
                            onAddToShopping = addToShopping,
                        )
                        Tab.SHOPPING -> ShoppingScreen(
                            items = shopping,
                            onToggle = vm::toggle,
                            onDelete = vm::delete,
                            onDeleteChecked = vm::deleteChecked,
                            onAdd = vm::addManualItem,
                        )
                    }
                }
            }
        }
        // Pushed screens cover the tabs instead of replacing them, so tab state (mode, month, scroll) survives.
        vm.stack.lastOrNull()?.let { screen ->
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                when (screen) {
                    is Screen.Recipe -> {
                        val dish = vm.dishById(screen.dishId)
                        if (dish == null) {
                            LaunchedEffect(screen) { vm.back() }
                        } else {
                            RecipeScreen(
                                dish = dish,
                                onBack = vm::back,
                                onEdit = { vm.open(Screen.Edit(dish.id)) },
                                onToggleCanCook = { vm.toggleCanCook(dish) },
                                onAddToShopping = { addToShopping(listOf(ShoppingSource.Recipe(dish))) },
                            )
                        }
                    }
                    is Screen.Edit -> EditScreen(
                        original = screen.dishId?.let(vm::dishById),
                        onSave = { vm.save(it, screen.assignTo) },
                        onBack = vm::back,
                    )
                    Screen.Settings -> SettingsScreen(
                        email = email,
                        onFeedback = { vm.open(Screen.Feedback) },
                        onSignOut = vm::signOut,
                        onBack = vm::back,
                    )
                    Screen.Feedback -> FeedbackScreen(onBack = vm::back)
                }
            }
        }
    }

    confirmingWeek?.let { sources ->
        AlertDialog(
            onDismissRequest = { confirmingWeek = null },
            title = { Text("買い物リストを入れ替えますか") },
            text = { Text("今の買い物リストをすべて消して、この週の今日以降の材料に入れ替えます。手入力した材料やチェック済みの材料も消えます。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingWeek = null
                    replaceShopping(sources)
                }) { Text("入れ替える") }
            },
            dismissButton = { TextButton(onClick = { confirmingWeek = null }) { Text("キャンセル") } },
        )
    }

    vm.migration?.let { state ->
        MigrationDialog(
            state = state,
            onMove = { vm.moveToServer(it.data) },
            onStartEmpty = vm::startWithoutLocalData,
            onDiscard = vm::discardLocalData,
            onRetry = vm::checkMigration,
            onSignOut = vm::signOut,
        )
    }

    vm.picking?.let { slot ->
        PickerSheet(
            slot = slot,
            dishes = dishes,
            onPick = { vm.assign(slot, it.id) },
            onNewDish = { vm.open(Screen.Edit(dishId = null, assignTo = slot)) },
            canCookOnly = vm.pickerCanCookOnly,
            onCanCookOnlyChange = { vm.pickerCanCookOnly = it },
            onToggleCanCook = vm::toggleCanCook,
            onDismiss = { vm.picking = null },
        )
    }
    vm.slotMenu?.let { slot ->
        val dish = plan[slot]
        if (dish == null) {
            LaunchedEffect(slot) { vm.slotMenu = null }
        } else {
            SlotMenuSheet(
                slot = slot,
                dish = dish,
                onOpenRecipe = { vm.open(Screen.Recipe(dish.id)) },
                onSwap = {
                    vm.slotMenu = null
                    vm.picking = slot
                },
                onClear = { vm.clear(slot) },
                onDismiss = { vm.slotMenu = null },
            )
        }
    }
}
