package com.stayeasy.app

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Small dependency-free API client for public discovery and connection diagnostics. */
internal object StayApiClient {
    @Volatile private var sessionToken: String? = null

    fun setSessionToken(token: String) { sessionToken = token.trim().ifBlank { null } }
    fun clearSessionToken() { sessionToken = null }
    fun currentSessionToken(): String? = sessionToken

    private fun request(
        baseUrl: String,
        path: String,
        token: String? = null,
        method: String = "GET",
        body: JSONObject? = null
    ): String {
        val trimmedBase = baseUrl.trim().trimEnd('/')
        val parsed = java.net.URI(trimmedBase)
        val host = parsed.host?.lowercase().orEmpty()
        val localDevelopmentHost = host in setOf("localhost", "127.0.0.1", "10.0.2.2")
        require(parsed.scheme.equals("https", ignoreCase = true) || (parsed.scheme.equals("http", ignoreCase = true) && localDevelopmentHost)) {
            "Use HTTPS for remote API URLs. HTTP is accepted only for localhost, 127.0.0.1, or the Android emulator host 10.0.2.2."
        }
        val connection = (URL(trimmedBase + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer ${token.trim()}")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.use { input -> BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { it.readText() } }.orEmpty()
            if (code !in 200..299) {
                val detail = runCatching { JSONObject(responseBody).optString("detail", responseBody) }.getOrDefault(responseBody)
                throw IllegalStateException("API returned HTTP $code${if (detail.isNotBlank()) ": $detail" else ""}")
            }
            return responseBody
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
            val parsedUnits = (0 until units.length()).mapNotNull { i ->
                val u = units.optJSONObject(i) ?: return@mapNotNull null
                RemoteUnit(
                    id = u.optLong("id"), roomCode = u.optString("room_code"), unitCode = u.optString("unit_code"),
                    rentableType = u.optString("rentable_type", "bed"), sharingCapacity = u.optInt("sharing_capacity", 1).coerceAtLeast(1),
                    monthlyRent = u.optInt("monthly_rent", p.optInt("rent_from", 0)).coerceAtLeast(0),
                    deposit = u.optInt("deposit", p.optInt("deposit_from", 0)).coerceAtLeast(0), status = u.optString("status", "available")
                )
            }
            val availableUnit = parsedUnits.firstOrNull { it.status == "available" }
            val cap = availableUnit?.sharingCapacity ?: 1
            val rentableType = availableUnit?.rentableType ?: "bed"
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
                expectedVacancy = availableUnit?.let { "${it.unitCode} available" } ?: "Availability needs confirmation",
                nearbyPlaces = nearbyText, serverPropertyId = sourceId, serverUnits = parsedUnits
            )
        }
    }

    fun requestDevelopmentToken(baseUrl: String, subject: String, role: String, devPassword: String): String {
        val body = JSONObject().put("subject", subject.trim()).put("role", role).put("dev_password", devPassword)
        val response = JSONObject(request(baseUrl, "/auth/dev-token", method = "POST", body = body))
        return response.getString("access_token").also(::setSessionToken)
    }

    /** Creates listing metadata and room/bed inventory, then submits it for review. */
    fun createOwnerListing(baseUrl: String, token: String, listing: PgListing): JSONObject {
        val locality = listing.area.substringBefore(',').trim().ifBlank { listing.area.trim() }
        val city = listing.area.substringAfter(',', "Pune").trim().ifBlank { "Pune" }
        val property = JSONObject()
            .put("name", listing.name).put("description", listing.description).put("address", listing.area)
            .put("locality", locality).put("city", city).put("pin_code", listing.pinCode)
            .put("eligibility", if (listing.allowedFor == "All genders") "Mixed" else listing.allowedFor)
            .put("couples_welcome", listing.couplesAllowed).put("furnishing", if (listing.isFurnished) "Furnished" else "Unfurnished")
            .put("bathroom_policy", listing.bathroomType).put("food_policy", listing.foodPolicy)
            .put("visitor_policy", listing.visitorPolicy).put("curfew_policy", listing.curfewPolicy)
            .put("cooking_allowed", listing.cookingAllowed).put("minimum_stay_months", listing.minimumStayMonths)
            .put("notice_period_days", listing.noticePeriodDays).put("rent_from", listing.monthlyRent)
            .put("deposit_from", listing.deposit).put("food_monthly", listing.foodMonthly).put("utilities_estimate", listing.utilityMonthlyEstimate)
        val created = JSONObject(request(baseUrl, "/owner/properties", token, "POST", property))
        val propertyId = created.getLong("id")
        val privateRoom = listing.sharing.contains("Private", ignoreCase = true)
        val unitsPerRoom = Regex("(\\d+)").find(listing.sharing)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 30) ?: 1
        val unitCount = if (privateRoom) listing.totalRooms.coerceAtLeast(1) else listing.totalBeds.coerceAtLeast(1)
        for (i in 0 until unitCount) {
            val roomNumber = 101 + if (privateRoom) i else i / unitsPerRoom
            val roomCode = "R$roomNumber"
            val unitCode = if (privateRoom) "ROOM-$roomNumber" else "$roomCode-B${(i % unitsPerRoom) + 1}"
            val unit = JSONObject()
                .put("floor_label", "Unspecified").put("room_code", roomCode).put("unit_code", unitCode)
                .put("rentable_type", if (privateRoom) "room" else "bed").put("sharing_capacity", unitsPerRoom)
                .put("monthly_rent", listing.monthlyRent).put("deposit", listing.deposit)
            request(baseUrl, "/owner/properties/$propertyId/units", token, "POST", unit)
        }
        val amenityRows = JSONArray()
        listing.amenities.distinct().forEach { rawLabel ->
            val extra = rawLabel.endsWith("(extra)", true)
            val label = rawLabel.removeSuffix(" (extra)").trim()
            val code = label.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-').take(65).ifBlank { "amenity-${amenityRows.length()}" }
            amenityRows.put(JSONObject().put("code", code).put("label", label).put("category", "general").put("scope", "property")
                .put("availability", "available").put("pricing", if (extra) "extra_charge" else "included"))
        }
        if (amenityRows.length() > 0) request(baseUrl, "/owner/properties/$propertyId/amenities", token, "PUT", JSONObject().put("amenities", amenityRows))
        return JSONObject(request(baseUrl, "/owner/properties/$propertyId/submit-review", token, "POST"))
    }

    fun createEnquiry(baseUrl: String, token: String, propertyId: Long, roomType: String, message: String, moveInDate: String? = null): JSONObject {
        val body = JSONObject().put("property_id", propertyId).put("preferred_room_type", roomType).put("message", message)
        normalizeDate(moveInDate)?.let { body.put("move_in_date", it) }
        return JSONObject(request(baseUrl, "/enquiries", token, "POST", body))
    }

    fun createVisit(baseUrl: String, token: String, propertyId: Long, enquiryId: Long?, preferredAt: String): JSONObject {
        val body = JSONObject().put("property_id", propertyId).put("preferred_at", preferredAt)
        enquiryId?.let { body.put("enquiry_id", it) }
        return JSONObject(request(baseUrl, "/visits", token, "POST", body))
    }

    fun createReservation(baseUrl: String, token: String, propertyId: Long, unitId: Long?, roomType: String, moveInDate: String, note: String): JSONObject {
        val normalizedDate = normalizeDate(moveInDate) ?: throw IllegalArgumentException("Enter a valid move-in date, for example 2026-11-15 or 15 Nov 2026.")
        val terms = JSONObject().put("currency", "INR").put("note", note).put("source", "Android app")
        val body = JSONObject().put("property_id", propertyId).put("preferred_room_type", roomType)
            .put("move_in_date", normalizedDate).put("terms_snapshot", terms)
        unitId?.let { body.put("unit_id", it) }
        return JSONObject(request(baseUrl, "/reservations", token, "POST", body))
    }

    fun updateReservation(baseUrl: String, token: String, reservationId: Long, status: String): JSONObject {
        val normalized = status.lowercase(Locale.ROOT).replace(' ', '_')
        return JSONObject(request(baseUrl, "/owner/reservations/$reservationId/status", token, "POST", JSONObject().put("status", normalized)))
    }

    fun updateEnquiry(baseUrl: String, token: String, enquiryId: Long, status: String): JSONObject {
        val normalized = when (status.lowercase(Locale.ROOT)) {
            "visit scheduled" -> "visit_scheduled"
            else -> status.lowercase(Locale.ROOT).replace(' ', '_')
        }
        return JSONObject(request(baseUrl, "/owner/enquiries/$enquiryId/status", token, "POST", JSONObject().put("status", normalized)))
    }

    fun enquiries(baseUrl: String, token: String): List<JSONObject> {
        val result = JSONArray(request(baseUrl, "/enquiries", token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    fun reservations(baseUrl: String, token: String): List<JSONObject> {
        val result = JSONArray(request(baseUrl, "/reservations", token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    fun visitTimeFromLabel(label: String): String {
        val now = java.time.ZonedDateTime.now()
        val date = when {
            label.startsWith("Today", true) -> now.toLocalDate()
            label.startsWith("Tomorrow", true) -> now.toLocalDate().plusDays(1)
            else -> {
                val today = now.toLocalDate()
                val daysUntilSaturday = ((java.time.DayOfWeek.SATURDAY.value - today.dayOfWeek.value + 7) % 7).let { if (it == 0) 7 else it }
                today.plusDays(daysUntilSaturday.toLong())
            }
        }
        val timeText = label.substringAfter('·', "11:00 AM").trim()
        val time = runCatching { java.time.LocalTime.parse(timeText, java.time.format.DateTimeFormatter.ofPattern("h:mm a", Locale.US)) }
            .getOrDefault(java.time.LocalTime.of(11, 0))
        return java.time.OffsetDateTime.of(date, time, now.zone.rules.getOffset(date.atTime(time))).toString()
    }

    private fun normalizeDate(value: String?): String? {
        if (value.isNullOrBlank()) return null
        for (format in listOf("yyyy-MM-dd", "d MMM yyyy", "dd MMM yyyy", "d MMMM yyyy", "dd MMMM yyyy")) {
            val parser = SimpleDateFormat(format, Locale.ENGLISH).apply { isLenient = false; timeZone = TimeZone.getTimeZone("UTC") }
            val date = runCatching { parser.parse(value.trim()) }.getOrNull() ?: continue
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(date)
        }
        return null
    }

    fun ownerDashboard(baseUrl: String, token: String): JSONObject = JSONObject(request(baseUrl, "/owner/dashboard", token))
}
