package com.lapakhunian.app.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lapakhunian.app.R
import com.lapakhunian.app.core.data.session.SessionManager
import com.lapakhunian.app.core.designsystem.PhIcon
import com.lapakhunian.app.core.designsystem.PhosphorWeight
import com.lapakhunian.app.core.designsystem.PrimaryDark
import com.lapakhunian.app.core.designsystem.TintGreen
import com.lapakhunian.app.core.util.NotificationLinkMapper
import com.lapakhunian.app.feature.auth.LoginScreen
import com.lapakhunian.app.feature.auth.RegisterScreen
import com.lapakhunian.app.feature.auth.RolePickerScreen
import com.lapakhunian.app.feature.home.TitipJualScreen
import com.lapakhunian.app.feature.settings.ChangePasswordScreen
import com.lapakhunian.app.feature.dashboard.AgentDashboardScreen
import com.lapakhunian.app.feature.dashboard.AgentLeadsScreen
import com.lapakhunian.app.feature.dashboard.AgentPlansScreen
import com.lapakhunian.app.feature.dashboard.HistoryScreen
import com.lapakhunian.app.feature.dashboard.LeadWaScreen
import com.lapakhunian.app.feature.dashboard.ListingWizardScreen
import com.lapakhunian.app.feature.dashboard.OwnerDashboardScreen
import com.lapakhunian.app.feature.chat.ChatListScreen
import com.lapakhunian.app.feature.chat.ChatListViewModel
import com.lapakhunian.app.feature.chat.ContactScreen
import com.lapakhunian.app.feature.chat.ThreadScreen
import com.lapakhunian.app.feature.detail.DetailScreen
import com.lapakhunian.app.feature.explore.ExploreScreen
import com.lapakhunian.app.feature.favorites.FavoritesScreen
import com.lapakhunian.app.feature.help.HelpScreen
import com.lapakhunian.app.feature.home.HomeScreen
import com.lapakhunian.app.feature.kpr.KprScreen
import com.lapakhunian.app.feature.notifications.NotificationsScreen
import com.lapakhunian.app.feature.profile.ProfileScreen
import com.lapakhunian.app.feature.settings.PrivacyPolicyScreen
import com.lapakhunian.app.feature.settings.SettingsScreen
import com.lapakhunian.app.feature.settings.TermsScreen
import com.lapakhunian.app.feature.voucher.VoucherScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

object Routes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val EXPLORE_CATEGORY = "explore?category={category}&city={city}"
    const val DETAIL = "detail/{propertyId}"
    const val FAVORITES = "favorites"
    const val CHAT = "chat"
    const val PROFILE = "profile"
    const val NOTIFICATIONS = "notifications"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CONTACT = "contact/{propertyId}"
    const val THREAD = "thread/{conversationId}"
    const val ROLE_PICKER = "role-picker"
    const val UBAH_PASSWORD = "ubah-password"
    const val TITIP_JUAL = "titip-jual"
}

@HiltViewModel
class NavSessionViewModel @Inject constructor(session: SessionManager) : ViewModel() {
    val isLoggedIn: StateFlow<Boolean> = session.tokenFlow
        .map { it != null }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            session.tokenFlow.value != null,
        )
}

private data class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: String,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, R.string.nav_home, "house"),
    TopLevelDestination(Routes.EXPLORE, R.string.nav_explore, "magnifying-glass"),
    TopLevelDestination(Routes.FAVORITES, R.string.nav_favorites, "heart"),
    TopLevelDestination(Routes.CHAT, R.string.nav_chat, "chat-circle-dots"),
    TopLevelDestination(Routes.PROFILE, R.string.nav_profile, "user"),
)

@Composable
private fun LaunchedEffectOnce(key: Any? = Unit, block: suspend () -> Unit) {
    LaunchedEffect(key) { block() }
}

@Composable
fun AppRoot(
    initialOpenChat: Boolean = false,
    initialPropertySlug: String? = null,
    showBrandSplash: Boolean = false,
    onUiReady: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // Satu ChatListViewModel untuk badge tab Chat + layar Chat (hindari dobel polling);
    // polling sendiri sudah gated foreground via ProcessLifecycleOwner.
    val chatListViewModel: ChatListViewModel = hiltViewModel()
    val chatListState by chatListViewModel.state.collectAsState()

    // UI siap tergambar → system splash boleh lepas (transisi mulus ke brand splash)
    androidx.compose.runtime.SideEffect { onUiReady() }

    val isWizard = currentDestination?.route?.substringBefore('?') == "listing-wizard"
    var pendingExit by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<(() -> Unit)?>(null) }
    val navigateSafely: (() -> Unit) -> Unit = { action ->
        if (isWizard && backStackEntry?.savedStateHandle?.get<Boolean>(com.lapakhunian.app.feature.dashboard.ListingWizardViewModel.DIRTY_KEY) == true) {
            pendingExit = action
        } else {
            action()
        }
    }

    // Tap notifikasi chat → langsung ke tab Chat
    LaunchedEffect(initialOpenChat) {
        if (initialOpenChat) navigateSafely { navController.navigate(Routes.CHAT) }
    }

    // App Links (share link /properti/{slug} dibuka dari luar app)
    LaunchedEffect(initialPropertySlug) {
        if (initialPropertySlug != null) navigateSafely { navController.navigate("detail/$initialPropertySlug") }
    }

    // Cek update otomatis (sekali per 24 jam) — dialog hanya muncul jika ada versi baru
    AutoUpdatePrompt()

    pendingExit?.let { action ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingExit = null },
            title = { Text("Buang perubahan iklan?") },
            text = { Text("Isian belum dikirim. Keluar dari form akan membuang perubahan ini.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { pendingExit = null; action() }) { Text("Buang & keluar") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingExit = null }) { Text("Lanjut mengisi") }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (shouldShowBottomBar(currentDestination?.route)) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    topLevelDestinations.forEach { dest ->
                        // bandingkan base route (sebelum '?') agar "explore?category=…" tetap
                        // menyorot tab Jelajah saat dinavigasi dari kategori Home
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route?.substringBefore('?') == dest.route.substringBefore('?') } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navigateSafely {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = !isWizard }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                val unreadCount = if (dest.route == Routes.CHAT) chatListState.totalUnread else 0
                                val iconContent: @Composable () -> Unit = {
                                    PhIcon(
                                        name = dest.icon,
                                        size = 24.dp,
                                        weight = if (selected) PhosphorWeight.FILL else PhosphorWeight.DUOTONE,
                                    )
                                }
                                if (unreadCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                                Text(if (unreadCount > 99) "99+" else unreadCount.toString())
                                            }
                                        },
                                    ) { iconContent() }
                                } else {
                                    iconContent()
                                }
                            },
                            label = { Text(stringResource(dest.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryDark,
                                selectedTextColor = PrimaryDark,
                                indicatorColor = TintGreen,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenExplore = { category ->
                        if (category == null) {
                            navController.navigate(Routes.EXPLORE)
                        } else {
                            navController.navigate("explore?category=$category")
                        }
                    },
                    onOpenDetail = { navController.navigate("detail/$it") },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenCity = { city -> navController.navigate("explore?city=$city") },
                    onOpenTitipJual = { navController.navigate(Routes.TITIP_JUAL) },
                )
            }
            composable(
                route = Routes.EXPLORE_CATEGORY,
                arguments = listOf(
                    navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("city") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { entry ->
                ExploreScreen(
                    initialCategory = entry.arguments?.getString("category"),
                    initialCity = entry.arguments?.getString("city"),
                    onOpenDetail = { navController.navigate("detail/$it") },
                    onBack = { navController.popBackStack() },
                    onOpenCompare = { ids -> navController.navigate("compare?ids=$ids") },
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    initialCategory = null,
                    onOpenDetail = { navController.navigate("detail/$it") },
                    onBack = { navController.popBackStack() },
                    onOpenCompare = { ids -> navController.navigate("compare?ids=$ids") },
                )
            }
            composable(
                route = "compare?ids={ids}",
                arguments = listOf(
                    navArgument("ids") { type = NavType.StringType; nullable = true; defaultValue = "" },
                ),
            ) {
                com.lapakhunian.app.feature.compare.CompareScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { navController.navigate("detail/$it") },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
            ) {
                DetailScreen(
                    onBack = { navController.popBackStack() },
                    onContact = { propertyId ->
                        navController.navigate("contact/$propertyId")
                    },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                    onOpenKpr = { price -> navController.navigate("kpr?price=$price") },
                )
            }
            composable(
                route = "kpr?price={price}",
                arguments = listOf(
                    navArgument("price") { type = NavType.LongType; defaultValue = 0L },
                ),
            ) { entry ->
                KprScreen(
                    initialPrice = entry.arguments?.getLong("price") ?: 0L,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("help") {
                HelpScreen(
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable("settings") {
                val navSession: NavSessionViewModel = hiltViewModel()
                val loggedIn by navSession.isLoggedIn.collectAsState()
                SettingsScreen(
                    isLoggedIn = loggedIn,
                    onBack = { navController.popBackStack() },
                    onOpenPrivacy = { navController.navigate("privacy") },
                    onOpenTerms = { navController.navigate("terms") },
                    onOpenChangePassword = { navController.navigate(Routes.UBAH_PASSWORD) },
                )
            }
            composable(Routes.UBAH_PASSWORD) {
                ChangePasswordScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.TITIP_JUAL) {
                TitipJualScreen(onBack = { navController.popBackStack() })
            }
            composable("privacy") {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }
            composable("terms") {
                TermsScreen(onBack = { navController.popBackStack() })
            }
            composable("voucher") {
                VoucherScreen(
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable(
                route = Routes.CONTACT,
                arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
            ) { entry ->
                val navSession: NavSessionViewModel = hiltViewModel()
                val loggedIn = navSession.isLoggedIn.value
                if (loggedIn) {
                    ContactScreen(
                        onBack = { navController.popBackStack() },
                        onOpenThread = { navController.navigate("thread/$it") { popUpTo(Routes.HOME) } },
                    )
                } else {
                    // guest → lead tetap dicatat via guest-contact (nama + WA), tanpa paksa login
                    val propertyId = entry.arguments?.getString("propertyId").orEmpty()
                    LaunchedEffectOnce {
                        navController.navigate("contact-guest/$propertyId") {
                            popUpTo(Routes.CONTACT) { inclusive = true }
                        }
                    }
                }
            }
            composable(
                route = "contact-guest/{propertyId}",
                arguments = listOf(navArgument("propertyId") { type = NavType.StringType }),
            ) {
                com.lapakhunian.app.feature.chat.GuestContactScreen(
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable(
                route = Routes.THREAD,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
            ) {
                ThreadScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenLink = { link ->
                        // Klik notifikasi = tandai dibaca (di layar) + menuju tujuan native
                        when (val dest = NotificationLinkMapper.resolve(link)) {
                            is NotificationLinkMapper.Destination.Thread ->
                                navController.navigate("thread/${dest.conversationId}")
                            NotificationLinkMapper.Destination.ChatTab -> navController.navigate(Routes.CHAT) {
                                popUpTo(Routes.HOME)
                                launchSingleTop = true
                            }
                            NotificationLinkMapper.Destination.AgentLeads -> navController.navigate("agent-leads")
                            is NotificationLinkMapper.Destination.Property ->
                                navController.navigate("detail/${dest.propertyId}")
                            NotificationLinkMapper.Destination.Help -> navController.navigate("help")
                            NotificationLinkMapper.Destination.History -> navController.navigate("history")
                            null -> Unit // link tak dikenal: cukup tandai dibaca
                        }
                    },
                )
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(
                    onOpenDetail = { navController.navigate("detail/$it") },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable(Routes.CHAT) {
                ChatListScreen(
                    onOpenThread = { navController.navigate("thread/$it") },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                    onOpenLeadWa = { navController.navigate("lead-wa") },
                    viewModel = chatListViewModel,
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                    onGoRegister = { navController.navigate(Routes.REGISTER) },
                    onOpenOwnerDashboard = { navController.navigate("owner-dashboard") },
                    onOpenAgentDashboard = { navController.navigate("agent-dashboard") },
                    onOpenAgentLeads = { navController.navigate("agent-leads") },
                    onOpenWizard = { navController.navigate("listing-wizard") },
                    onOpenHistory = { navController.navigate("history") },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onEditProfile = { navController.navigate("edit-profile") },
                    onOpenKpr = { navController.navigate("kpr") },
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenHelp = { navController.navigate("help") },
                    onOpenVoucher = { navController.navigate("voucher") },
                    onOpenFavorites = {
                        navController.navigate(Routes.FAVORITES) {
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    },
                    onOpenChat = {
                        navController.navigate(Routes.CHAT) {
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable("edit-profile") {
                com.lapakhunian.app.feature.profile.EditProfileScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable("owner-dashboard") {
                OwnerDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onOpenWizard = { navController.navigate("listing-wizard") },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                    onRenew = { navController.navigate("nativepay?listingId=$it") },
                    onEditListing = { navController.navigate("listing-wizard?listingId=$it") },
                )
            }
            composable(
                "listing-wizard?listingId={listingId}",
                arguments = listOf(
                    navArgument("listingId") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                ListingWizardScreen(
                    onBack = { navigateSafely { navController.popBackStack() } },
                    onSubmitted = {
                        navController.navigate("owner-dashboard") { popUpTo(Routes.HOME) }
                    },
                )
            }
            composable("agent-dashboard") {
                AgentDashboardScreen(
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                    onOpenLeads = { navController.navigate("agent-leads") },
                    onOpenPlans = { navController.navigate("agent-plans") },
                    onOpenHistory = { navController.navigate("history") },
                    onOpenLeadWa = { navController.navigate("lead-wa") },
                    onOpenChat = {
                        navController.navigate(Routes.CHAT) {
                            popUpTo(Routes.HOME)
                            launchSingleTop = true
                        }
                    },
                    onOpenWizard = { navController.navigate("listing-wizard") },
                    onOpenVoucher = { navController.navigate("voucher") },
                    onOpenCompensation = { navController.navigate("compensation") },
                    onOpenAgentProfile = { navController.navigate("agent-profile") },
                    onOpenDetail = { navController.navigate("detail/$it") },
                )
            }
            composable("agent-profile") {
                com.lapakhunian.app.feature.dashboard.AgentProfileScreen(
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                    onOpenDetail = { navController.navigate("detail/$it") },
                    onEditProfile = { navController.navigate("agent-profile-edit") },
                )
            }
            composable("agent-profile-edit") {
                com.lapakhunian.app.feature.dashboard.AgentProfileEditScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable("compensation") {
                com.lapakhunian.app.feature.dashboard.CompensationScreen(
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable("agent-leads") {
                AgentLeadsScreen(
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                    onOpenPlans = { navController.navigate("agent-plans") },
                    onPayPurchase = { navController.navigate("nativepay?purchaseId=$it") },
                )
            }
            composable("agent-plans") {
                AgentPlansScreen(
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                    onChoosePlan = { planId, voucher ->
                        navController.navigate(
                            "nativepay?planId=${android.net.Uri.encode(planId)}&voucher=${android.net.Uri.encode(voucher)}",
                        )
                    },
                    onResumeSnap = { url, subscriptionId ->
                        navController.navigate("payment?paymentUrl=${android.net.Uri.encode(url)}&subscriptionId=${android.net.Uri.encode(subscriptionId)}")
                    },
                    onResumeNative = { subscriptionId ->
                        navController.navigate("nativepay?resumeSub=${android.net.Uri.encode(subscriptionId)}")
                    },
                )
            }
            composable("lead-wa") {
                LeadWaScreen(
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable("history") {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                )
            }
            // Pembayaran native VA/QRIS (Midtrans Core API) — semua alur app
            composable(
                "nativepay?listingId={listingId}&planId={planId}&voucher={voucher}&purchaseId={purchaseId}&resumeSub={resumeSub}",
                arguments = listOf(
                    navArgument("listingId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("planId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("voucher") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("purchaseId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("resumeSub") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                com.lapakhunian.app.feature.payments.NativePaymentScreen(
                    onBack = { navController.popBackStack() },
                    onFallbackPay = { url, extraQuery ->
                        navController.navigate("payment?paymentUrl=${android.net.Uri.encode(url)}$extraQuery")
                    },
                )
            }

            // Jalur web (Custom Tabs) — fallback kartu/GoPay + resume tagihan Snap lama
            composable(
                "payment?listingId={listingId}&paymentUrl={paymentUrl}&purchaseId={purchaseId}&subscriptionId={subscriptionId}",
                arguments = listOf(
                    navArgument("listingId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("paymentUrl") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("purchaseId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("subscriptionId") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                com.lapakhunian.app.feature.payments.PaymentScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = { navController.popBackStack() },
                    onGoRegister = { navController.navigate(Routes.REGISTER) },
                    onForgotPassword = { email -> navController.navigate("forgot-password?email=${android.net.Uri.encode(email)}") },
                    onNeedsRoleSelection = {
                        navController.navigate(Routes.ROLE_PICKER) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(
                "forgot-password?email={email}",
                arguments = listOf(navArgument("email") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                com.lapakhunian.app.feature.auth.ForgotPasswordScreen(
                    initialEmail = entry.arguments?.getString("email").orEmpty(),
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ROLE_PICKER) {
                RolePickerScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Routes.LOGIN) { popUpTo(Routes.REGISTER) { inclusive = true }; launchSingleTop = true }
                    },
                    onGoLogin = {
                        navController.navigate(Routes.LOGIN) { popUpTo(Routes.REGISTER) { inclusive = true }; launchSingleTop = true }
                    },
                )
            }
        }
    }

    // Brand splash (mockup PNG: logo + wordmark + bar loading + tagline) — cold start saja.
    // Di-compose SETELAH Scaffold supaya digambar paling atas.
    if (showBrandSplash) {
        var brandVisible by androidx.compose.runtime.saveable.rememberSaveable {
            androidx.compose.runtime.mutableStateOf(true)
        }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(1200)
            brandVisible = false
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = brandVisible,
            enter = androidx.compose.animation.fadeIn(
                androidx.compose.animation.core.tween(200),
            ),
            exit = androidx.compose.animation.fadeOut(
                androidx.compose.animation.core.tween(250),
            ),
        ) {
            BrandSplashScreen()
        }
    }
}
