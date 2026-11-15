package com.lapakhunian.app.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
fun AppRoot(initialOpenChat: Boolean = false, initialPropertySlug: String? = null) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // Tap notifikasi chat → langsung ke tab Chat
    LaunchedEffect(initialOpenChat) {
        if (initialOpenChat) navController.navigate(Routes.CHAT)
    }

    // App Links (share link /properti/{slug} dibuka dari luar app)
    LaunchedEffect(initialPropertySlug) {
        if (initialPropertySlug != null) navController.navigate("detail/$initialPropertySlug")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                topLevelDestinations.forEach { dest ->
                    // bandingkan base route (sebelum '?') agar "explore?category=…" tetap
                    // menyorot tab Jelajah saat dinavigasi dari kategori Home
                    val selected = currentDestination?.hierarchy
                        ?.any { it.route?.substringBefore('?') == dest.route.substringBefore('?') } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            PhIcon(
                                name = dest.icon,
                                size = 24.dp,
                                weight = if (selected) PhosphorWeight.FILL else PhosphorWeight.DUOTONE,
                            )
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
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
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
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                )
            }
            composable(Routes.EXPLORE) {
                ExploreScreen(
                    initialCategory = null,
                    onOpenDetail = { navController.navigate("detail/$it") },
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
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
                SettingsScreen(
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
                    onOpenThread = { navController.navigate("thread/$it") },
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
                    onRenew = { navController.navigate("payment?listingId=$it") },
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
                    onBack = { navController.popBackStack() },
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
                    onPayPurchase = { navController.navigate("payment?purchaseId=$it") },
                )
            }
            composable("agent-plans") {
                AgentPlansScreen(
                    onBack = { navController.popBackStack() },
                    onGoLogin = { navController.navigate(Routes.LOGIN) },
                    onPayment = { url -> navController.navigate("payment?paymentUrl=${android.net.Uri.encode(url)}") },
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
            composable(
                "payment?listingId={listingId}&paymentUrl={paymentUrl}&purchaseId={purchaseId}",
                arguments = listOf(
                    navArgument("listingId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("paymentUrl") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("purchaseId") { type = NavType.StringType; nullable = true; defaultValue = null },
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
                    onNeedsRoleSelection = {
                        navController.navigate(Routes.ROLE_PICKER) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.ROLE_PICKER) {
                RolePickerScreen(onDone = { navController.popBackStack() })
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegisterSuccess = { navController.popBackStack() },
                    onGoLogin = { navController.popBackStack() },
                )
            }
        }
    }
}
