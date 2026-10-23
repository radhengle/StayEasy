package com.stayeasy.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/** Small dependency-free API client for public discovery and connection diagnostics. */
internal object StayApiClient {
    private fun request(baseUrl: String, path: String, token: String? = null): String {
        val trimmedBase = baseUrl.trim().trimEnd('/')
        val parsed = java.net.URI(trimmedBase)
        val host = parsed.host?.lowercase().orEmpty()
        val localDevelopmentHost = host in setOf("localhost", "127.0.0.1", "10.0.2.2")
        require(parsed.scheme.equals("https", ignoreCase = true) || (parsed.scheme.equals("http", ignoreCase = true) && localDevelopmentHost)) {
            "Use HTTPS for remote API URLs. HTTP is accepted only for localhost, 127.0.0.1, or the Android emulator host 10.0.2.2."
        }
        val connection = (URL(trimmedBase + path).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 6_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer ${token.trim()}")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input -> BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() } }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("API returned HTTP $code${if (body.isNotBlank()) ": $body" else ""}")
            return body
        } finally {
            connection.disconnect()
        }
    }

    fun health(baseUrl: String): JSONObject = JSONObject(request(baseUrl, "/health"))

    fun publishedProperties(baseUrl: String, city: String = "Pune"): List<PgListing> {
        val encodedCity = java.net.URLEncoder.encode(city, "UTF-8")
        val result = JSONObject(request(baseUrl, "/properties?city=$encodedCity"))
        val items = result.optJSONArray("items") ?: JSONArray()
        return (0 until items.length()).mapNotNull { index ->
            val p = items.optJSONObject(index) ?: return@mapNotNull null
            val inventory = p.optJSONObject("inventory") ?: JSONObject()
            val units = p.optJSONArray("units") ?: JSONArray()
            val availableUnit = (0 until units.length()).mapNotNull { units.optJSONObject(it) }
                .firstOrNull { it.optString("status") == "available" }
            val cap = availableUnit?.optInt("sharing_capacity", 1)?.coerceAtLeast(1) ?: 1
            val rentableType = availableUnit?.optString("rentable_type", "bed") ?: "bed"
            val sharing = if (rentableType == "room") "Private room" else if (cap == 1) "Single occupancy" else "$cap sharing"
            val amenityRows = p.optJSONArray("amenities") ?: JSONArray()
            val amenities = (0 until amenityRows.length()).mapNotNull { i ->
                val a = amenityRows.optJSONObject(i) ?: return@mapNotNull null
                val label = a.optString("label")
                if (label.isBlank()) null else if (a.optString("pricing") == "extra_charge") "$label (extra)" else label
            }.distinct()
            val latitudeSet = !p.isNull("latitude") && p.optDouble("latitude", 0.0) != 0.0
            val longitudeSet = !p.isNull("longitude") && p.optDouble("longitude", 0.0) != 0.0
            val nearby = p.optJSONArray("nearby_places") ?: JSONArray()
            val nearbyText = (0 until nearby.length()).mapNotNull { i ->
                val value = nearby.opt(i)
                when (value) {
                    is String -> value
                    is JSONObject -> value.optString("name").ifBlank { null }
                    else -> null
                }
            }.joinToString(", ")
            val sourceId = p.optLong("id", 1L).coerceAtLeast(1L)
            PgListing(
                id = -sourceId,
                name = p.optString("name", "PG listing"),
                area = listOf(p.optString("locality"), p.optString("city")).filter { it.isNotBlank() }.joinToString(", "),
                monthlyRent = p.optInt("rent_from", 0).coerceAtLeast(0),
                deposit = p.optInt("deposit_from", 0).coerceAtLeast(0),
                sharing = sharing,
                allowedFor = p.optString("eligibility", "Mixed"),
                couplesAllowed = p.optBoolean("couples_welcome", false),
                amenities = amenities,
                distanceKm = 0.0,
                rating = p.optDouble("rating", 0.0),
                availableBeds = inventory.optInt("available", 0).coerceAtLeast(0),
                totalBeds = inventory.optInt("total", 0).coerceAtLeast(0),
                description = p.optString("description", ""),
                foodMonthly = p.optInt("food_monthly", 0).coerceAtLeast(0),
                isDemo = false,
                totalRooms = (0 until units.length()).mapNotNull { units.optJSONObject(it)?.optString("room_code") }.distinct().size.coerceAtLeast(1),
                isVerified = p.optString("verification_status") == "verified",
                isPublished = p.optString("status") == "published",
                isFurnished = p.optString("furnishing", "Furnished").equals("Furnished", true),
                bathroomType = p.optString("bathroom_policy", "Shared"),
                cookingAllowed = p.optBoolean("cooking_allowed", false),
                curfewPolicy = p.optString("curfew_policy", "Ask owner"),
                visitorPolicy = p.optString("visitor_policy", "Ask owner"),
                minimumStayMonths = p.optInt("minimum_stay_months", 1).coerceAtLeast(0),
                noticePeriodDays = p.optInt("notice_period_days", 30).coerceAtLeast(0),
                utilityMonthlyEstimate = p.optInt("utilities_estimate", 0).coerceAtLeast(0),
                foodPolicy = p.optString("food_policy", "Not offered"),
                pinCode = p.optString("pin_code", ""),
                latitude = p.optDouble("latitude", 0.0),
                longitude = p.optDouble("longitude", 0.0),
                hasCoordinates = latitudeSet && longitudeSet,
                distanceIsVerified = false,
                expectedVacancy = availableUnit?.optString("expected_vacancy")?.takeIf { !it.isNullOrBlank() } ?: "Availability needs confirmation",
                nearbyPlaces = nearbyText
            )
        }
    }

    fun ownerDashboard(baseUrl: String, token: String): JSONObject = JSONObject(request(baseUrl, "/owner/dashboard", token))
}
