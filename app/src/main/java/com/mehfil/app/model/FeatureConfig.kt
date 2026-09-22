package com.mehfil.app.model

/**
 * Every switchable capability in the app, in one place.
 *
 * These are **build-time** decisions: edit [default] below and rebuild. There is deliberately no in-app
 * switchboard — a shipped app should not let anyone turn parts of itself on and off, and a buyer reading this
 * file sees the whole configuration surface at once.
 */
enum class Feature(val default: Boolean) {

    // ---- Sign-in methods ----

    /**
     * Sign in with Apple. On Android this is Firebase's web OAuth flow and needs an Apple Services ID
     * (backend/README step 5), so it ships off until that is configured.
     */
    APPLE_SIGN_IN(false),

    /** Continue with Google (Credential Manager + Firebase). */
    GOOGLE_SIGN_IN(true),

    /** Email and password, with a verification email before the account can be used. */
    EMAIL_PASSWORD(true),

    /**
     * Require a verified email address before the app opens. Off lets unverified accounts straight in —
     * convenient in testing, not recommended in production.
     */
    EMAIL_VERIFICATION_REQUIRED(true),

    /**
     * "Continue on this device" — the offline account used when no Firebase config is bundled.
     * Leave on: it is what lets the app run, and be reviewed, before the buyer wires up Firebase.
     */
    DEVICE_ONLY_MODE(true),

    // ---- Product modules ----
    //
    // Each hides a whole area of the app: its rows, its routes and its entry points. They exist so a
    // buyer can ship a narrower first version without deleting code.

    /**
     * Offer the twelve-event demo season: the onboarding toggle and the Account row that loads it.
     * Off by default — a buyer's production build should not ship demo data behind a one-tap button.
     */
    SAMPLE_SEASON(false),

    /**
     * Offer "Remove the sample season" in Account: deletes exactly the demo records by their fixed ids and
     * leaves everything the vendor entered untouched. Off by default, for the same reason.
     */
    REMOVE_SAMPLE_SEASON(false),

    /** Inventory: stock items, per-date holds, the commitment strip. */
    INVENTORY(true),

    /** Runsheet: the per-event task list with blocks, reorder and completion. */
    RUNSHEET(true),

    /** Expenses and the per-event margin. */
    EXPENSES(true),

    /** Enquiries: intake, conflict check, convert-to-event. */
    ENQUIRIES(true),

    /** Package templates used when quoting. */
    PACKAGES(true),

    /** Change requests with their costed impact and revised quote. */
    CHANGE_REQUESTS(true),

    /** The client-facing shared event page and its share sheet. */
    SHARED_CLIENT_PAGE(true),

    /** Reminders composed in three tones, sent over WhatsApp or SMS. */
    PAYMENT_REMINDERS(true),

    /** Dark appearance and the Appearance picker. Off pins the app to light. */
    DARK_MODE(true),
}

/** Reads feature flags. `Features[Feature.INVENTORY]` anywhere in the app. */
object Features {
    operator fun get(feature: Feature): Boolean = feature.default

    /** True when at least one cloud sign-in method is available — i.e. the welcome screen has something to show. */
    val hasCloudSignIn: Boolean get() = get(Feature.APPLE_SIGN_IN) || get(Feature.GOOGLE_SIGN_IN) || get(Feature.EMAIL_PASSWORD)
}
