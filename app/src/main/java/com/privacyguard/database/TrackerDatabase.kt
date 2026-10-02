package com.privacyguard.database

enum class TrackerCategory(val displayName: String, val badgeColorHex: Long) {
    ANALYTICS("Analytics & Behavior", 0xFF1976D2),
    ATTRIBUTION("Device Fingerprinting & Attribution", 0xFF7B1FA2),
    ADVERTISING("Ad Networks & Profiling", 0xFFE65100),
    CRASH_HARVESTING("Crash & Telemetry Harvest", 0xFFC2185B),
    PUSH_ENGAGEMENT("Retention & Notification Tracking", 0xFF00796B),
    DATA_BROKER("Location & Data Broker", 0xFFD32F2F)
}

data class TrackerDefinition(
    val id: String,
    val name: String,
    val company: String,
    val category: TrackerCategory,
    val description: String,
    val codeSignatures: List<String>,
    val domainSignatures: List<String>
)

object TrackerDatabase {

    private val trackers = listOf(
        // Analytics & User Tracking
        TrackerDefinition(
            id = "google_firebase_analytics",
            name = "Google Firebase Analytics",
            company = "Alphabet Inc.",
            category = TrackerCategory.ANALYTICS,
            description = "Logs in-app screen views, user actions, sessions, and uploads advertising IDs to Google servers.",
            codeSignatures = listOf(
                "com.google.android.gms.measurement",
                "com.google.firebase.analytics"
            ),
            domainSignatures = listOf(
                "app-measurement.com",
                "firebaseinstallations.googleapis.com",
                "google-analytics.com",
                "analytics.google.com"
            )
        ),
        TrackerDefinition(
            id = "meta_app_events",
            name = "Facebook App Events / Graph",
            company = "Meta Platforms, Inc.",
            category = TrackerCategory.ADVERTISING,
            description = "Collects app installations, user engagement, device IDs, and personal interests to build Facebook shadow profiles.",
            codeSignatures = listOf(
                "com.facebook.appevents",
                "com.facebook.internal"
            ),
            domainSignatures = listOf(
                "graph.facebook.com",
                "facebook.com/tr",
                "connect.facebook.net",
                "b-graph.facebook.com"
            )
        ),
        TrackerDefinition(
            id = "appsflyer",
            name = "AppsFlyer Attribution",
            company = "AppsFlyer Ltd.",
            category = TrackerCategory.ATTRIBUTION,
            description = "Cross-app attribution engine that fingerprints hardware specs, Wi-Fi SSIDs, battery levels, and install sources.",
            codeSignatures = listOf(
                "com.appsflyer"
            ),
            domainSignatures = listOf(
                "appsflyer.com",
                "t.appsflyer.com",
                "conversions.appsflyer.com",
                "launches.appsflyer.com",
                "events.appsflyer.com"
            )
        ),
        TrackerDefinition(
            id = "adjust",
            name = "Adjust Attribution SDK",
            company = "Adjust GmbH",
            category = TrackerCategory.ATTRIBUTION,
            description = "Tracks attribution, re-engagement campaigns, deep link clicks, and installs with device fingerprinting.",
            codeSignatures = listOf(
                "com.adjust.sdk"
            ),
            domainSignatures = listOf(
                "adjust.com",
                "app.adjust.com",
                "adjust.net",
                "adjust.io",
                "ulink.adjust.com"
            )
        ),
        TrackerDefinition(
            id = "amplitude",
            name = "Amplitude Analytics",
            company = "Amplitude, Inc.",
            category = TrackerCategory.ANALYTICS,
            description = "Product intelligence SDK capturing high-resolution user event sequences and behavioral funnels.",
            codeSignatures = listOf(
                "com.amplitude.api"
            ),
            domainSignatures = listOf(
                "amplitude.com",
                "api2.amplitude.com",
                "analytics.amplitude.com",
                "api.amplitude.com"
            )
        ),
        TrackerDefinition(
            id = "branch_metrics",
            name = "Branch Metrics",
            company = "Branch Metrics, Inc.",
            category = TrackerCategory.ATTRIBUTION,
            description = "Deep-linking and cross-platform referral tracker logging attribution source and user identities.",
            codeSignatures = listOf(
                "io.branch.referral"
            ),
            domainSignatures = listOf(
                "branch.io",
                "api2.branch.io",
                "bnc.lt",
                "app.link"
            )
        ),
        TrackerDefinition(
            id = "mixpanel",
            name = "Mixpanel",
            company = "Mixpanel, Inc.",
            category = TrackerCategory.ANALYTICS,
            description = "Event-driven mobile analytics harvesting custom user properties, geolocation, and app interactions.",
            codeSignatures = listOf(
                "com.mixpanel.android"
            ),
            domainSignatures = listOf(
                "mixpanel.com",
                "api.mixpanel.com",
                "decide.mixpanel.com"
            )
        ),
        TrackerDefinition(
            id = "kochava",
            name = "Kochava Attribution",
            company = "Kochava, Inc.",
            category = TrackerCategory.ATTRIBUTION,
            description = "Mobile measurement partner harvesting device telemetry, install referral chains, and network carrier data.",
            codeSignatures = listOf(
                "com.kochava.base",
                "com.kochava.android"
            ),
            domainSignatures = listOf(
                "kochava.com",
                "control.kochava.com",
                "api.kochava.com"
            )
        ),
        TrackerDefinition(
            id = "google_admob",
            name = "Google AdMob / DoubleClick",
            company = "Alphabet Inc.",
            category = TrackerCategory.ADVERTISING,
            description = "Ad mediation and auction network collecting demographic profiles and real-time ad request telemetry.",
            codeSignatures = listOf(
                "com.google.android.gms.ads"
            ),
            domainSignatures = listOf(
                "doubleclick.net",
                "googleads.g.doubleclick.net",
                "pagead2.googlesyndication.com",
                "adservice.google.com",
                "admob.com"
            )
        ),
        TrackerDefinition(
            id = "unity_ads",
            name = "Unity3D Ads & Analytics",
            company = "Unity Technologies",
            category = TrackerCategory.ADVERTISING,
            description = "Embedded game ad network and telemetry engine profiling player behavior and game session lengths.",
            codeSignatures = listOf(
                "com.unity3d.services.ads",
                "com.unity3d.services.analytics"
            ),
            domainSignatures = listOf(
                "unityads.unity3d.com",
                "auction.unityads.unity3d.com",
                "webview.unityads.unity3d.com",
                "cdp.cloud.unity3d.com",
                "config.unityads.unity3d.com"
            )
        ),
        TrackerDefinition(
            id = "applovin",
            name = "AppLovin / MAX",
            company = "AppLovin Corporation",
            category = TrackerCategory.ADVERTISING,
            description = "Mobile advertising and bidding engine capturing user profiles, install velocity, and ad engagement.",
            codeSignatures = listOf(
                "com.applovin"
            ),
            domainSignatures = listOf(
                "applovin.com",
                "applvn.com",
                "rt.applovin.com",
                "d.applovin.com",
                "pdn.applovin.com",
                "ms.applovin.com"
            )
        ),
        TrackerDefinition(
            id = "vungle",
            name = "Vungle / Liftoff",
            company = "Liftoff Mobile, Inc.",
            category = TrackerCategory.ADVERTISING,
            description = "Video ad network harvesting device identifiers, video watch completion, and user demographic data.",
            codeSignatures = listOf(
                "com.vungle"
            ),
            domainSignatures = listOf(
                "vungle.com",
                "ads.vungle.com",
                "api.vungle.com"
            )
        ),
        TrackerDefinition(
            id = "inmobi",
            name = "InMobi",
            company = "InMobi Pte. Ltd.",
            category = TrackerCategory.ADVERTISING,
            description = "Global mobile ad network collecting location coordinates, device sensors, and audience segments.",
            codeSignatures = listOf(
                "com.inmobi"
            ),
            domainSignatures = listOf(
                "inmobi.com",
                "config.inmobi.com",
                "telemetry.inmobi.com"
            )
        ),
        TrackerDefinition(
            id = "ironsource",
            name = "ironSource / Supersonic",
            company = "Unity Technologies",
            category = TrackerCategory.ADVERTISING,
            description = "Ad mediation and user monetization platform tracking ad views, device specs, and in-app events.",
            codeSignatures = listOf(
                "com.ironsource"
            ),
            domainSignatures = listOf(
                "supersonicads.com",
                "ironsrc.mobi",
                "supersonic.com",
                "is.com"
            )
        ),
        TrackerDefinition(
            id = "bytedance_pangle",
            name = "ByteDance Pangle / TikTok Ads",
            company = "ByteDance Ltd.",
            category = TrackerCategory.ADVERTISING,
            description = "TikTok ad network and tracking SDK harvesting device IDs, IP geolocation, and app usage logs.",
            codeSignatures = listOf(
                "com.bytedance.sdk.openadsdk"
            ),
            domainSignatures = listOf(
                "pangle.io",
                "byteoversea.com",
                "tiktokv.com",
                "pangolin-sdk-toutiao.com",
                "bytegoofy.com"
            )
        ),
        TrackerDefinition(
            id = "mintegral",
            name = "Mintegral SDK",
            company = "Mobvista Inc.",
            category = TrackerCategory.ADVERTISING,
            description = "Programmatic advertising platform collecting package names, screen specs, and device IDs.",
            codeSignatures = listOf(
                "com.mintegral.msdk"
            ),
            domainSignatures = listOf(
                "mintegral.com",
                "pg-cross.mintegral.com",
                "hb.mintegral.com"
            )
        ),
        TrackerDefinition(
            id = "chartboost",
            name = "Chartboost",
            company = "Chartboost, Inc.",
            category = TrackerCategory.ADVERTISING,
            description = "Gaming ad network collecting advertising IDs, gameplay sessions, and attribution events.",
            codeSignatures = listOf(
                "com.chartboost.sdk"
            ),
            domainSignatures = listOf(
                "chartboost.com",
                "live.chartboost.com"
            )
        ),
        TrackerDefinition(
            id = "tapjoy",
            name = "Tapjoy",
            company = "Tapjoy, Inc.",
            category = TrackerCategory.ADVERTISING,
            description = "Offerwall and reward ad tracking SDK logging device parameters, virtual currency, and engagement.",
            codeSignatures = listOf(
                "com.tapjoy"
            ),
            domainSignatures = listOf(
                "tapjoy.com",
                "ws.tapjoyads.com"
            )
        ),
        TrackerDefinition(
            id = "firebase_crashlytics",
            name = "Firebase Crashlytics",
            company = "Alphabet Inc.",
            category = TrackerCategory.CRASH_HARVESTING,
            description = "Crash reporter that collects memory state, device hardware details, battery level, and stack traces.",
            codeSignatures = listOf(
                "com.google.firebase.crashlytics",
                "com.crashlytics"
            ),
            domainSignatures = listOf(
                "crashlytics.com",
                "reports.crashlytics.com",
                "settings.crashlytics.com"
            )
        ),
        TrackerDefinition(
            id = "sentry",
            name = "Sentry Error Monitoring",
            company = "Functional Software, Inc.",
            category = TrackerCategory.CRASH_HARVESTING,
            description = "Real-time error tracking that frequently includes user breadcrumbs, session recordings, and device context.",
            codeSignatures = listOf(
                "io.sentry"
            ),
            domainSignatures = listOf(
                "sentry.io",
                "browser.sentry-cdn.com"
            )
        ),
        TrackerDefinition(
            id = "bugsnag",
            name = "Bugsnag",
            company = "SmartBear Software",
            category = TrackerCategory.CRASH_HARVESTING,
            description = "Application stability monitor transmitting diagnostic telemetry, thread states, and device metadata.",
            codeSignatures = listOf(
                "com.bugsnag.android"
            ),
            domainSignatures = listOf(
                "bugsnag.com",
                "notify.bugsnag.com"
            )
        ),
        TrackerDefinition(
            id = "onesignal",
            name = "OneSignal Push Tracker",
            company = "OneSignal, Inc.",
            category = TrackerCategory.PUSH_ENGAGEMENT,
            description = "Push notification engine profiling user notification engagement, device language, and active hours.",
            codeSignatures = listOf(
                "com.onesignal"
            ),
            domainSignatures = listOf(
                "onesignal.com",
                "api.onesignal.com"
            )
        ),
        TrackerDefinition(
            id = "braze",
            name = "Braze / Appboy",
            company = "Braze, Inc.",
            category = TrackerCategory.PUSH_ENGAGEMENT,
            description = "Customer engagement platform harvesting user profiles, push token clicks, and lifetime value metrics.",
            codeSignatures = listOf(
                "com.braze",
                "com.appboy"
            ),
            domainSignatures = listOf(
                "braze.com",
                "appboy.com",
                "iad-01.braze.com",
                "iad-02.braze.com"
            )
        ),
        TrackerDefinition(
            id = "clevertap",
            name = "CleverTap",
            company = "WizRocket, Inc.",
            category = TrackerCategory.PUSH_ENGAGEMENT,
            description = "User retention analytics platform recording screen views, custom events, and location breadcrumbs.",
            codeSignatures = listOf(
                "com.clevertap.android"
            ),
            domainSignatures = listOf(
                "clevertap.com",
                "wzrkt.com"
            )
        ),
        TrackerDefinition(
            id = "yandex_metrica",
            name = "Yandex AppMetrica",
            company = "Yandex N.V.",
            category = TrackerCategory.ANALYTICS,
            description = "Mobile analytics capturing device parameters, push stats, crash dumps, and ad attribution data.",
            codeSignatures = listOf(
                "com.yandex.metrica"
            ),
            domainSignatures = listOf(
                "appmetrica.yandex.net",
                "metrika.yandex"
            )
        ),
        TrackerDefinition(
            id = "segment",
            name = "Segment / Twilio",
            company = "Twilio Inc.",
            category = TrackerCategory.ANALYTICS,
            description = "Customer data platform aggregating telemetry from multiple in-app SDKs and piping it to cloud destinations.",
            codeSignatures = listOf(
                "com.segment.analytics"
            ),
            domainSignatures = listOf(
                "segment.io",
                "api.segment.io",
                "cdn.segment.com"
            )
        ),
        TrackerDefinition(
            id = "singular",
            name = "Singular Attribution",
            company = "Singular Labs, Inc.",
            category = TrackerCategory.ATTRIBUTION,
            description = "Marketing analytics platform performing deep hardware fingerprinting and ad spend attribution.",
            codeSignatures = listOf(
                "net.singular.sdk"
            ),
            domainSignatures = listOf(
                "singular.net"
            )
        ),
        TrackerDefinition(
            id = "flurry",
            name = "Flurry Analytics",
            company = "Yahoo / Apollo",
            category = TrackerCategory.ANALYTICS,
            description = "Longstanding mobile analytics suite capturing device IDs, application usage, and crash metrics.",
            codeSignatures = listOf(
                "com.flurry.android"
            ),
            domainSignatures = listOf(
                "flurry.com",
                "data.flurry.com"
            )
        ),
        TrackerDefinition(
            id = "tenjin",
            name = "Tenjin Attribution",
            company = "Tenjin, Inc.",
            category = TrackerCategory.ATTRIBUTION,
            description = "Mobile measurement and LTV prediction SDK harvesting ad impressions, device IDs, and purchase receipts.",
            codeSignatures = listOf(
                "com.tenjin.android"
            ),
            domainSignatures = listOf(
                "tenjin.com",
                "track.tenjin.io"
            )
        )
    )

    fun getAllTrackers(): List<TrackerDefinition> = trackers

    /**
     * Fast domain matcher for live DNS/SNI interception.
     * Matches exact domains or suffix subdomains (e.g. "t.appsflyer.com" matches "appsflyer.com").
     */
    fun matchDomain(domain: String): TrackerDefinition? {
        val cleanDomain = domain.lowercase().trim().trimEnd('.')
        if (cleanDomain.isEmpty()) return null

        // 1. Direct suffix matching against curated signatures
        for (tracker in trackers) {
            for (sig in tracker.domainSignatures) {
                if (cleanDomain == sig || cleanDomain.endsWith(".$sig")) {
                    return tracker
                }
            }
        }

        // 2. Generic heuristics for stealth telemetry endpoints
        if (cleanDomain.contains("telemetry") ||
            cleanDomain.contains("user-analytics") ||
            cleanDomain.contains("app-analytics") ||
            cleanDomain.contains("track.metrics") ||
            cleanDomain.contains("device-metrics")
        ) {
            return TrackerDefinition(
                id = "generic_telemetry",
                name = "Stealth Telemetry Endpoint",
                company = "Unknown Exfiltrator",
                category = TrackerCategory.ANALYTICS,
                description = "Domain matches generic surveillance & user tracking patterns.",
                codeSignatures = emptyList(),
                domainSignatures = listOf(cleanDomain)
            )
        }

        return null
    }

    /**
     * Component / Class matcher for static APK auditing.
     */
    fun matchComponent(componentName: String): TrackerDefinition? {
        for (tracker in trackers) {
            for (sig in tracker.codeSignatures) {
                if (componentName.startsWith(sig) || componentName.contains(sig)) {
                    return tracker
                }
            }
        }
        return null
    }
}
