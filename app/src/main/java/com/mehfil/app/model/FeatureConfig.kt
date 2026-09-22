package com.mehfil.app.model

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import com.mehfil.app.BuildConfig

/**
 * Every switchable capability in the app, in one place.
 *
 * [default] is the shipping configuration. To change what a build ships with, edit the value here —
 * nothing else in the app hard-codes these decisions.
 *
 * In debug builds each flag can also be flipped at runtime from Profile → Account → Feature flags,
 * which makes QA and demos possible without a rebuild. Release builds always use [default].
 */
enum class Feature(val default: Boolean, val title: String) {

    // ---- Sign-in methods ----

    /**
     * Sign in with Apple. On Android this is Firebase's web OAuth flow and needs an Apple Services ID
     * (backend/README step 5), so it ships off until that is configured.
     */
    APPLE_SIGN_IN(false, "Sign in with Apple"),

    /** Continue with Google (Credential Manager + Firebase). */
    GOOGLE_SIGN_IN(true, "Continue with Google"),

    /** Email and password, with a verification email before the account can be used. */
    EMAIL_PASSWORD(true, "Email and password"),

    /**
     * Require a verified email address before the app opens. Off lets unverified accounts straight in —
     * convenient in testing, not recommended in production.
     */
    EMAIL_VERIFICATION_REQUIRED(true, "Require a verified email"),

    /**
     * "Continue on this device" — the offline account used when no Firebase config is bundled.
     * Leave on: it is what lets the app run, and be reviewed, before the buyer wires up Firebase.
     */
    DEVICE_ONLY_MODE(true, "Continue on this device"),

    // ---- Product modules ----
    //
    // Each hides a whole area of the app: its rows, its routes and its entry points. They exist so a
    // buyer can ship a narrower first version without deleting code.

    /**
     * Offer the twelve-event demo season: the onboarding toggle and the Account row that loads it.
     * Off by default — a buyer's production build should not ship demo data behind a one-tap button.
     */
    SAMPLE_SEASON(false, "Load the sample season"),

    /**
     * Offer "Remove the sample season" in Account: deletes exactly the demo records by their fixed ids and
     * leaves everything the vendor entered untouched. Off by default, for the same reason.
     */
    REMOVE_SAMPLE_SEASON(false, "Remove the sample season"),

    /** Inventory: stock items, per-date holds, the commitment strip. */
    INVENTORY(true, "Inventory"),

    /** Runsheet: the per-event task list with blocks, reorder and completion. */
    RUNSHEET(true, "Runsheet"),

    /** Expenses and the per-event margin. */
    EXPENSES(true, "Expenses"),

    /** Enquiries: intake, conflict check, convert-to-event. */
    ENQUIRIES(true, "Enquiries"),

    /** Package templates used when quoting. */
    PACKAGES(true, "Package templates"),

    /** Change requests with their costed impact and revised quote. */
    CHANGE_REQUESTS(true, "Change requests"),

    /** The client-facing shared event page and its share sheet. */
    SHARED_CLIENT_PAGE(true, "Client page"),

    /** Reminders composed in three tones, sent over WhatsApp or SMS. */
    PAYMENT_REMINDERS(true, "Payment reminders"),

    /** Dark appearance and the Appearance picker. Off pins the app to light. */
    DARK_MODE(true, "Dark appearance"),
}

/**
 * Reads feature flags. `Features[Feature.INVENTORY]` anywhere in the app.
 *
 * [init] is called once from the Application/ViewModel so debug overrides survive a restart.
 */
object Features {
    private const val PREFS = "mehfil-features"
    private val overrides = mutableStateMapOf<String, Boolean>()
    private var prefs: android.content.SharedPreferences? = null

    fun init(context: Context) {
        if (!BuildConfig.DEBUG) return
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        p.all.forEach { (k, v) -> if (v is Boolean) overrides[k] = v }
    }

    /** True when the feature is on for this build. */
    operator fun get(feature: Feature): Boolean =
        if (BuildConfig.DEBUG) overrides[feature.name] ?: feature.default else feature.default

    /** True when at least one cloud sign-in method is available — i.e. the welcome screen has something to show. */
    val hasCloudSignIn: Boolean get() = get(Feature.APPLE_SIGN_IN) || get(Feature.GOOGLE_SIGN_IN) || get(Feature.EMAIL_PASSWORD)

    /** Flips a flag for this install (debug only; release builds ignore it). */
    fun override(feature: Feature, value: Boolean?) {
        if (!BuildConfig.DEBUG) return
        val editor = prefs?.edit() ?: return
        if (value == null) { overrides.remove(feature.name); editor.remove(feature.name) } else { overrides[feature.name] = value; editor.putBoolean(feature.name, value) }
        editor.apply()
    }

    /** Drops every override, returning the build to its shipping configuration. */
    fun resetOverrides() {
        if (!BuildConfig.DEBUG) return
        overrides.clear(); prefs?.edit()?.clear()?.apply()
    }

    /** True when the flag differs from what a release build would use. */
    fun isOverridden(feature: Feature) = BuildConfig.DEBUG && overrides.containsKey(feature.name)
}
