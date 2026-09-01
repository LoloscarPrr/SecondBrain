package com.secondbrain.app.core.diagnostics

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.secondbrain.app.BuildConfig

object CrashReporter {
    private const val TAG = "SecondBrainCrash"

    @Volatile
    private var enabled: Boolean = false

    fun initialize(context: Context) {
        if (!BuildConfig.FIREBASE_CONFIGURED) {
            Log.i(TAG, "Crashlytics disabled: app/google-services.json is not configured.")
            return
        }

        val app = FirebaseApp.initializeApp(context)
        if (app == null) {
            Log.w(TAG, "Crashlytics disabled: FirebaseApp could not initialize.")
            return
        }

        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        enabled = true
        log("SecondBrain started (${BuildConfig.VERSION_NAME})")
    }

    fun log(message: String) {
        if (enabled) FirebaseCrashlytics.getInstance().log(message)
        else Log.d(TAG, message)
    }

    fun record(error: Throwable, context: String? = null) {
        if (enabled) {
            val crashlytics = FirebaseCrashlytics.getInstance()
            context?.let { crashlytics.setCustomKey("context", it) }
            crashlytics.recordException(error)
        } else {
            Log.e(TAG, context ?: "Non-fatal error", error)
        }
    }
}
