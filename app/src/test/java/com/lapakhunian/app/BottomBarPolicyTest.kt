package com.lapakhunian.app

import com.lapakhunian.app.core.ui.shouldShowBottomBar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarPolicyTest {

    @Test
    fun topLevelTabsKeepBottomBar() {
        listOf(
            "home",
            "explore",
            "explore?category={category}&city={city}",
            "favorites",
            "chat",
            "profile",
        ).forEach { route ->
            assertTrue("Bottom bar harus tampil di $route", shouldShowBottomBar(route))
        }
    }

    @Test
    fun nullRouteDefaultsToVisible() {
        assertTrue(shouldShowBottomBar(null))
    }

    @Test
    fun browsingSubPagesKeepBottomBar() {
        listOf(
            "detail/{propertyId}",
            "notifications",
            "settings",
            "help",
            "privacy",
            "terms",
            "owner-dashboard",
            "agent-dashboard",
            "agent-plans",
            "agent-leads",
            "lead-wa",
            "history",
            "kpr?price={price}",
            "voucher",
            "titip-jual",
        ).forEach { route ->
            assertTrue("Bottom bar harus tetap tampil di $route", shouldShowBottomBar(route))
        }
    }

    @Test
    fun focusedFlowsHideBottomBar() {
        listOf(
            "thread/{conversationId}",
            "listing-wizard",
            "listing-wizard?listingId={listingId}",
            "payment?paymentUrl={paymentUrl}&subscriptionId={subscriptionId}",
            "payment?listingId={listingId}",
            "payment?purchaseId={purchaseId}",
            "contact/{propertyId}",
            "contact-guest/{propertyId}",
            "login",
            "register",
            "forgot-password?email={email}",
            "role-picker",
            "compare?ids={ids}",
        ).forEach { route ->
            assertFalse("Bottom bar harus sembunyi di alur fokus $route", shouldShowBottomBar(route))
        }
    }
}