package com.orion.templete.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Launch-time country detection (v2.8.4). The backend resolves the request IP
 * to an ISO 3166-1 alpha-2 country (DB-IP Lite); the device locale country is
 * sent as a fallback for carrier IPs the database does not know. No location
 * permission is involved. The result is cached and refreshed on every launch;
 * the feed ranking and the default story language read the cached value.
 */
object GeoCountry {

    private const val PREFS = "geo_country_prefs"
    private const val KEY_COUNTRY = "country_iso2"

    private var launchLookup: Job? = null

    suspend fun awaitLaunch() {
        withTimeoutOrNull(5500) { launchLookup?.join() }
    }

    private var prefs: android.content.SharedPreferences? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .build()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    /** Manual narration choice, kept across launches. Wins over the country default. */
    fun savedLanguage(): String? = prefs?.getString("manual_language", null)
    fun savedVoice(): String? = prefs?.getString("manual_voice", null)
    fun saveManualLanguage(name: String) { prefs?.edit()?.putString("manual_language", name)?.remove("manual_voice")?.apply() }
    fun saveManualVoice(name: String?) { prefs?.edit()?.apply { if (name == null) remove("manual_voice") else putString("manual_voice", name) }?.apply() }

    /** Cached ISO2 country from the last successful detection, null until then. */
    fun cached(): String? = prefs?.getString(KEY_COUNTRY, null)
        ?: Locale.getDefault().country.takeIf { it.matches(Regex("[A-Z]{2}")) }

    /** Refresh the cached country from the backend; safe to call on every launch. */
    fun refresh(baseUrl: String) {
        val p = prefs ?: return
        launchLookup = scope.launch {
            try {
                val deviceCountry = Locale.getDefault().country ?: ""
                val request = Request.Builder()
                    .url(baseUrl + "geo/country")
                    .header("X-Device-Country", deviceCountry)
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val country = JSONObject(body).optString("country", "")
                    if (country.length == 2) {
                        p.edit().putString(KEY_COUNTRY, country.uppercase(Locale.ROOT)).apply()
                        Log.d("GeoCountry", "detected country=$country")
                    }
                }
            } catch (e: Exception) {
                Log.d("GeoCountry", "country detection failed, keeping cache: ${e.message}")
            }
        }
    }

    // Countries whose primary language matches one of the story audio languages.
    private val ARABIC_COUNTRIES = setOf(
        "SA", "EG", "AE", "IQ", "JO", "LB", "SY", "MA", "DZ", "TN", "LY",
        "KW", "QA", "OM", "YE", "BH", "PS", "SD", "MR"
    )

    /**
     * Default story/narration language for a country: the country's language
     * when narration exists in it, otherwise English (product decision
     * 2026-11-23: e.g. India gets Hindi, unsupported countries get English).
     */
    fun audioLanguageFor(countryIso2: String?): String {
        return when (countryIso2?.uppercase(Locale.ROOT)) {
            "IN" -> "hi"
            "TR" -> "tr"
            "IR" -> "fa"
            "ID" -> "id"
            "PL" -> "pl"
            "HU" -> "hu"
            "GR", "CY" -> "el"
            in ARABIC_COUNTRIES -> "ar"
            else -> "en"
        }
    }
}
