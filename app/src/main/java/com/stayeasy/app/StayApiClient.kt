package com.stayeasy.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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
    private const val SESSION_PREFS = "stayeasy_account_session_v1"
    private const val SESSION_CIPHER = "encrypted_access_token"
    private const val SESSION_IV = "access_token_iv"
    private const val SESSION_ORIGIN = "api_origin"
    private const val SESSION_KEY_ALIAS = "stayeasy.account.session.aes.v1"

    @Volatile private var sessionToken: String? = null
    @Volatile private var sessionTokenOrigin: String? = null

    private fun sessionKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(SESSION_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                SESSION_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun persistAccountToken(context: Context, token: String, baseUrl: String) {
        val origin = normalizedOrigin(baseUrl) ?: throw IllegalArgumentException("Invalid API origin")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE).edit()
            .putString(SESSION_CIPHER, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(SESSION_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(SESSION_ORIGIN, origin)
            .apply()
    }

    /** Restores only a token encrypted with this device's Android Keystore key and for this API origin. */
    fun restoreAccountSession(context: Context, baseUrl: String): Boolean {
        val prefs = context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
        val origin = normalizedOrigin(baseUrl)
        val savedOrigin = prefs.getString(SESSION_ORIGIN, null)
        val encryptedText = prefs.getString(SESSION_CIPHER, null)
        val ivText = prefs.getString(SESSION_IV, null)
        if (origin == null || savedOrigin != origin || encryptedText.isNullOrBlank() || ivText.isNullOrBlank()) {
            if (!encryptedText.isNullOrBlank() || savedOrigin != null) clearPersistedAccountSession(context)
            clearSessionToken()
            return false
        }
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                sessionKey(),
                GCMParameterSpec(128, Base64.decode(ivText, Base64.NO_WRAP))
            )
            val token = cipher.doFinal(Base64.decode(encryptedText, Base64.NO_WRAP)).toString(Charsets.UTF_8)
            require(token.isNotBlank()) { "Stored session token is empty" }
            setSessionToken(token, baseUrl)
            true
        }.getOrElse {
            clearPersistedAccountSession(context)
            clearSessionToken()
            false
        }
    }

    fun clearPersistedAccountSession(context: Context) {
        context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun normalizedOrigin(baseUrl: String): String? = runCatching {
        val parsed = java.net.URI(baseUrl.trim().trimEnd('/'))
        val scheme = parsed.scheme?.lowercase(Locale.ROOT) ?: return@runCatching null
        if (scheme !in setOf("http", "https")) return@runCatching null
        val host = parsed.host?.lowercase(Locale.ROOT) ?: return@runCatching null
        val port = when {
            parsed.port >= 0 -> parsed.port
            scheme == "https" -> 443
            else -> 80
        }
        "$scheme://$host:$port"
    }.getOrNull()

    /** Returns a normalized origin for safe credential binding, excluding paths and queries. */
    fun apiOrigin(baseUrl: String): String? = normalizedOrigin(baseUrl)

    /** Session credentials are bound to one API origin and are never reused for another host. */
    fun setSessionToken(token: String, baseUrl: String) {
        val cleaned = token.trim().ifBlank { null }
        sessionToken = cleaned
        sessionTokenOrigin = if (cleaned == null) null else normalizedOrigin(baseUrl)
    }

    fun clearSessionToken() {
        sessionToken = null
        sessionTokenOrigin = null
    }

    fun currentSessionToken(baseUrl: String): String? {
        val current = sessionToken ?: return null
        val requestedOrigin = normalizedOrigin(baseUrl) ?: return null
        return current.takeIf { sessionTokenOrigin == requestedOrigin }
    }

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
        return parsePropertyItems(result.optJSONArray("items") ?: JSONArray())
    }

    /** Includes draft/pending listings, but only for the authenticated owner (or admin). */
    fun ownerProperties(baseUrl: String, token: String): List<PgListing> =
        parsePropertyItems(JSONArray(request(baseUrl, "/owner/properties", token)))

    /** Verified customer accounts already connected to this property through an enquiry or reservation. */
    fun ownerResidentCandidates(baseUrl: String, token: String, propertyId: Long): List<RemoteResidentCandidate> {
        require(propertyId > 0L) { "Select a valid server property." }
        val rows = JSONArray(request(baseUrl, "/owner/resident-candidates?property_id=$propertyId", token))
        return (0 until rows.length()).mapNotNull { index ->
            val row = rows.optJSONObject(index) ?: return@mapNotNull null
            val id = row.optString("id").trim()
            val name = row.optString("display_name").trim()
            val email = row.optString("email").trim()
            if (id.isBlank() || name.isBlank() || email.isBlank()) null
            else RemoteResidentCandidate(
                id = id,
                displayName = name,
                email = email,
                emailVerified = row.optBoolean("email_verified", false)
            )
        }.filter { it.emailVerified }
    }

    /** Creates an owner-authorized server stay; inventory is updated by the API only after validation. */
    fun createOwnerStay(
        baseUrl: String,
        token: String,
        propertyId: Long,
        unitId: Long,
        residentId: String,
        joiningDate: String,
        agreedMonthlyRent: Int,
        depositHeld: Int
    ): JSONObject {
        require(propertyId > 0L) { "Select a valid server property." }
        require(unitId > 0L) { "Select an available room or bed." }
        require(residentId.isNotBlank()) { "Select a verified resident account." }
        require(agreedMonthlyRent >= 0) { "Enter a valid monthly rent." }
        require(depositHeld >= 0) { "Enter a valid deposit amount." }
        val normalizedDate = normalizeDate(joiningDate)
            ?: throw IllegalArgumentException("Enter a valid joining date, for example 2026-11-01.")
        val body = JSONObject()
            .put("property_id", propertyId)
            .put("unit_id", unitId)
            .put("resident_id", residentId)
            .put("joining_date", normalizedDate)
            .put("agreed_monthly_rent", agreedMonthlyRent)
            .put("deposit_held", depositHeld)
        return JSONObject(request(baseUrl, "/owner/stays", token, "POST", body))
    }

    private fun parsePropertyItems(items: JSONArray): List<PgListing> {
        return (0 until items.length()).mapNotNull { index ->
            val p = items.optJSONObject(index) ?: return@mapNotNull null
            val inventory = p.optJSONObject("inventory") ?: JSONObject()
            val units = p.optJSONArray("units") ?: JSONArray()
            val parsedUnits = (0 until units.length()).mapNotNull { i ->
                val u = units.optJSONObject(i) ?: return@mapNotNull null
                val occupant = u.optJSONObject("occupant")
                val reservedFor = u.optJSONObject("reserved_for")
                RemoteUnit(
                    id = u.optLong("id"), roomCode = u.optString("room_code"), unitCode = u.optString("unit_code"),
                    rentableType = u.optString("rentable_type", "bed"), sharingCapacity = u.optInt("sharing_capacity", 1).coerceAtLeast(1),
                    monthlyRent = u.optInt("monthly_rent", p.optInt("rent_from", 0)).coerceAtLeast(0),
                    deposit = u.optInt("deposit", p.optInt("deposit_from", 0)).coerceAtLeast(0),
                    status = u.optString("status", "available"),
                    floorLabel = u.optString("floor_label", "Ground").ifBlank { "Ground" },
                    occupantName = occupant?.optString("display_name").orEmpty().takeUnless { it == "null" }.orEmpty(),
                    reservedForName = reservedFor?.optString("display_name").orEmpty().takeUnless { it == "null" }.orEmpty()
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

    /** Registers a customer-level account; Owner workspace requests remain pending server approval. */
    fun registerAccount(baseUrl: String, email: String, displayName: String, password: String, workspace: String): JSONObject {
        val body = JSONObject().put("email", email.trim()).put("display_name", displayName.trim())
            .put("password", password).put("requested_workspace", workspace)
        return JSONObject(request(baseUrl, "/auth/register", method = "POST", body = body))
    }

    fun verifyAccountEmail(baseUrl: String, email: String, code: String): JSONObject {
        val body = JSONObject().put("email", email.trim()).put("code", code.trim())
        return JSONObject(request(baseUrl, "/auth/verify-email", method = "POST", body = body))
    }

    /** On successful sign-in, bind the short-lived session to this API origin. */
    fun loginAccount(context: Context, baseUrl: String, email: String, password: String): JSONObject {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        val response = JSONObject(request(baseUrl, "/auth/login", method = "POST", body = body))
        val token = response.getString("access_token")
        persistAccountToken(context, token, baseUrl)
        setSessionToken(token, baseUrl)
        return response
    }

    fun resendAccountVerification(baseUrl: String, email: String, password: String): JSONObject {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        return JSONObject(request(baseUrl, "/auth/resend-verification", method = "POST", body = body))
    }

    fun requestPasswordReset(baseUrl: String, email: String): JSONObject {
        val body = JSONObject().put("email", email.trim())
        return JSONObject(request(baseUrl, "/auth/forgot-password", method = "POST", body = body))
    }

    fun resetAccountPassword(baseUrl: String, email: String, code: String, newPassword: String): JSONObject {
        val body = JSONObject().put("email", email.trim()).put("code", code.trim()).put("new_password", newPassword)
        return JSONObject(request(baseUrl, "/auth/reset-password", method = "POST", body = body))
    }

    fun currentAccount(baseUrl: String): JSONObject {
        val token = currentSessionToken(baseUrl) ?: throw IllegalStateException("Sign in before requesting account details")
        return JSONObject(request(baseUrl, "/auth/me", token = token))
    }

    /** Requests Owner workspace access without changing the server-authorized role. */
    fun requestOwnerAccess(baseUrl: String, token: String): JSONObject =
        JSONObject(request(baseUrl, "/auth/owner-access-request", token, "POST", JSONObject()))

    /** Admin-only Owner access queue; the backend enforces the administrator role. */
    fun ownerAccessRequests(baseUrl: String, token: String, status: String = "pending_approval"): List<JSONObject> {
        require(status in setOf("pending_approval", "approved", "rejected", "all")) { "Unsupported Owner request filter." }
        val result = JSONArray(request(baseUrl, "/admin/owner-requests?status=$status", token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    /** Admin-only decision; role promotion remains server-side and invalidates prior customer tokens. */
    fun decideOwnerAccessRequest(
        baseUrl: String,
        token: String,
        userId: String,
        decision: String,
        note: String = ""
    ): JSONObject {
        require(userId.isNotBlank()) { "Select an Owner access request." }
        require(decision in setOf("approve", "reject")) { "Unsupported Owner access decision." }
        val encodedUserId = java.net.URLEncoder.encode(userId, "UTF-8")
        val body = JSONObject().put("decision", decision).put("note", note.trim().take(500))
        return JSONObject(request(baseUrl, "/admin/owner-requests/$encodedUserId/decision", token, "POST", body))
    }

    fun logoutAccount(context: Context, baseUrl: String): JSONObject {
        val token = currentSessionToken(baseUrl) ?: throw IllegalStateException("No active account session")
        return try {
            JSONObject(request(baseUrl, "/auth/logout", token = token, method = "POST", body = JSONObject()))
        } finally {
            clearSessionToken()
            clearPersistedAccountSession(context)
        }
    }

    fun requestDevelopmentToken(baseUrl: String, subject: String, role: String, devPassword: String): String {
        val body = JSONObject().put("subject", subject.trim()).put("role", role).put("dev_password", devPassword)
        val response = JSONObject(request(baseUrl, "/auth/dev-token", method = "POST", body = body))
        return response.getString("access_token").also { setSessionToken(it, baseUrl) }
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
        if (!moveInDate.isNullOrBlank()) {
            val normalizedDate = normalizeDate(moveInDate)
                ?: throw IllegalArgumentException("Enter a valid move-in date, for example 2026-11-15 or 15 Nov 2026.")
            body.put("move_in_date", normalizedDate)
        }
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

    fun updateEnquiry(
        baseUrl: String,
        token: String,
        enquiryId: Long,
        status: String,
        ownerResponse: String? = null
    ): JSONObject {
        require(enquiryId > 0L) { "Select a valid enquiry." }
        val normalized = when (status.lowercase(Locale.ROOT)) {
            "visit scheduled" -> "visit_scheduled"
            else -> status.lowercase(Locale.ROOT).replace(' ', '_')
        }
        val body = JSONObject().put("status", normalized)
        ownerResponse?.let { body.put("owner_response", it.trim().take(4000)) }
        return JSONObject(request(baseUrl, "/owner/enquiries/$enquiryId/status", token, "POST", body))
    }

    fun createTenantInvitation(
        baseUrl: String,
        token: String,
        propertyId: Long,
        unitId: Long,
        enquiryId: Long?,
        recipientEmail: String,
        recipientName: String,
        recipientPhone: String,
        moveInDate: String,
        expiresInHours: Int = 168
    ): JSONObject {
        require(propertyId > 0L && unitId > 0L) { "Select a property and available bed." }
        require(recipientEmail.contains("@")) { "An invitation email is required." }
        require(recipientName.trim().length >= 2) { "Enter the tenant's name." }
        val normalizedDate = normalizeDate(moveInDate)
            ?: throw IllegalArgumentException("Enter the proposed move-in date as YYYY-MM-DD.")
        val body = JSONObject()
            .put("property_id", propertyId)
            .put("unit_id", unitId)
            .put("recipient_email", recipientEmail.trim())
            .put("recipient_name", recipientName.trim())
            .put("recipient_phone", recipientPhone.trim())
            .put("move_in_date", normalizedDate)
            .put("expires_in_hours", expiresInHours)
        enquiryId?.takeIf { it > 0L }?.let { body.put("enquiry_id", it) }
        return JSONObject(request(baseUrl, "/owner/invitations", token, "POST", body))
    }

    fun ownerInvitations(baseUrl: String, token: String, propertyId: Long? = null): List<JSONObject> {
        val path = propertyId?.let { "/owner/invitations?property_id=$it" } ?: "/owner/invitations"
        val result = JSONArray(request(baseUrl, path, token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    fun reassignTenantInvitation(
        baseUrl: String,
        token: String,
        invitationId: Long,
        newUnitId: Long,
        reason: String
    ): JSONObject {
        require(invitationId > 0L && newUnitId > 0L) { "Select an invitation and replacement bed." }
        require(reason.trim().length >= 3) { "Explain why the proposed bed is changing." }
        val body = JSONObject().put("new_unit_id", newUnitId).put("reason", reason.trim().take(1000))
        return JSONObject(request(baseUrl, "/owner/invitations/$invitationId/reassign", token, "POST", body))
    }

        fun resolveTenantInvitation(baseUrl: String, invitationToken: String): JSONObject {
        require(invitationToken.length in 20..200) { "Invitation link is invalid." }
        val encoded = java.net.URLEncoder.encode(invitationToken, "UTF-8")
        return JSONObject(request(baseUrl, "/invitations/resolve?token=$encoded"))
    }

    fun tenantInvitations(baseUrl: String, token: String): List<JSONObject> {
        val result = JSONArray(request(baseUrl, "/tenant/invitations", token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    fun acceptTenantInvitation(
        baseUrl: String,
        token: String,
        invitationToken: String?,
        invitationId: Long?,
        billableStartDate: String,
        billableTermsAcknowledged: Boolean
    ): JSONObject {
        require((!invitationToken.isNullOrBlank()) xor (invitationId != null)) {
            "Use either a share link or an invitation from your account."
        }
        val normalizedDate = normalizeDate(billableStartDate)
            ?: throw IllegalArgumentException("Confirm the billable start date.")
        val body = JSONObject()
            .put("billable_start_date", normalizedDate)
            .put("billable_terms_acknowledged", billableTermsAcknowledged)
        if (!invitationToken.isNullOrBlank()) body.put("token", invitationToken)
        invitationId?.takeIf { it > 0L }?.let { body.put("invitation_id", it) }
        return JSONObject(request(baseUrl, "/invitations/accept", token, "POST", body))
    }

    fun updateVisit(baseUrl: String, token: String, visitId: Long, status: String): JSONObject {
        val normalized = status.lowercase(Locale.ROOT).replace(' ', '_').replace('-', '_')
        return JSONObject(request(baseUrl, "/owner/visits/$visitId/status", token, "POST", JSONObject().put("status", normalized)))
    }

    fun enquiries(baseUrl: String, token: String): List<JSONObject> {
        val result = JSONArray(request(baseUrl, "/enquiries", token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    fun reservations(baseUrl: String, token: String): List<JSONObject> {
        val result = JSONArray(request(baseUrl, "/reservations", token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    fun visits(baseUrl: String, token: String): List<JSONObject> {
        val result = JSONArray(request(baseUrl, "/visits", token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    fun myStays(baseUrl: String, token: String): List<JSONObject> {
        val result = JSONArray(request(baseUrl, "/stays/me", token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    fun myMoveOutRequests(baseUrl: String, token: String): List<JSONObject> {
        val result = JSONArray(request(baseUrl, "/stays/me/move-out-requests", token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    fun requestMoveOut(
        baseUrl: String,
        token: String,
        stayId: Long,
        requestedMoveOutDate: String,
        reason: String
    ): JSONObject {
        require(stayId > 0L) { "Select a valid active stay." }
        val normalizedDate = normalizeDate(requestedMoveOutDate)
            ?: throw IllegalArgumentException("Enter a valid requested move-out date, for example 2026-12-15.")
        val body = JSONObject()
            .put("requested_move_out_date", normalizedDate)
            .put("reason", reason.trim().take(1200))
        return JSONObject(request(baseUrl, "/stays/me/$stayId/move-out-requests", token, "POST", body))
    }

    fun ownerMoveOutRequests(
        baseUrl: String,
        token: String,
        status: String = "pending",
        propertyId: Long? = null
    ): List<JSONObject> {
        require(status in setOf("pending", "approved", "rejected", "cancelled", "all")) {
            "Unsupported move-out request filter."
        }
        val query = buildList {
            add("request_status=$status")
            propertyId?.let { add("property_id=$it") }
        }.joinToString("&")
        val result = JSONArray(request(baseUrl, "/owner/move-out-requests?$query", token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    fun reviewMoveOutRequest(
        baseUrl: String,
        token: String,
        requestId: Long,
        decision: String,
        ownerNote: String = ""
    ): JSONObject {
        require(requestId > 0L) { "Select a valid move-out request." }
        val normalizedDecision = decision.trim().lowercase(Locale.ROOT)
        require(normalizedDecision in setOf("approve", "reject")) {
            "Choose approve or reject for a move-out request."
        }
        val body = JSONObject()
            .put("decision", normalizedDecision)
            .put("owner_note", ownerNote.trim().take(1000))
        return JSONObject(request(baseUrl, "/owner/move-out-requests/$requestId", token, "PATCH", body))
    }

    /** Server-authoritative resident stays, limited by the backend to the actor's resident-view scope. */
    fun ownerStays(baseUrl: String, token: String, propertyId: Long? = null): List<JSONObject> {
        val path = propertyId?.let { "/owner/stays?property_id=$it" } ?: "/owner/stays"
        val result = JSONArray(request(baseUrl, path, token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    /** Updates a server-backed stay lifecycle and sends only the date fields the owner changed. */
    fun updateOwnerStay(
        baseUrl: String,
        token: String,
        stayId: Long,
        status: String,
        noticeDate: String? = null,
        moveOutDate: String? = null
    ): JSONObject {
        require(stayId > 0L) { "Select a valid server stay." }
        val normalizedStatus = status.trim().lowercase(Locale.ROOT).replace(' ', '_')
        require(normalizedStatus in setOf("active", "notice_given", "completed", "cancelled")) {
            "Unsupported resident stay status."
        }
        if (normalizedStatus == "notice_given") {
            require(!noticeDate.isNullOrBlank()) { "Enter the notice date as YYYY-MM-DD." }
        }
        if (normalizedStatus == "completed") {
            require(!moveOutDate.isNullOrBlank()) { "Enter the move-out date as YYYY-MM-DD." }
        }
        val body = JSONObject().put("status", normalizedStatus)
        if (noticeDate != null) {
            body.put("notice_date", normalizeDate(noticeDate)
                ?: throw IllegalArgumentException("Enter a valid notice date, for example 2026-11-01."))
        }
        if (moveOutDate != null) {
            body.put("move_out_date", normalizeDate(moveOutDate)
                ?: throw IllegalArgumentException("Enter a valid move-out date, for example 2026-11-30."))
        }
        return JSONObject(request(baseUrl, "/owner/stays/$stayId", token, "PATCH", body))
    }

    fun createIssue(
        baseUrl: String,
        token: String,
        propertyId: Long,
        unitId: Long?,
        category: String,
        title: String,
        description: String,
        priority: String
    ): JSONObject {
        val safeCategory = category.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "_").trim('_').take(70).ifBlank { "other" }
        val safePriority = priority.lowercase(Locale.ROOT)
            .takeIf { it in setOf("low", "normal", "high", "urgent") } ?: "normal"
        val body = JSONObject()
            .put("property_id", propertyId)
            .put("category", safeCategory)
            .put("title", title.trim().take(180))
            .put("description", description.trim().take(5000))
            .put("priority", safePriority)
        unitId?.let { body.put("unit_id", it) }
        return JSONObject(request(baseUrl, "/issues", token, "POST", body))
    }

    fun issues(baseUrl: String, token: String, propertyId: Long? = null): List<JSONObject> {
        val path = propertyId?.let { "/issues?property_id=$it" } ?: "/issues"
        val result = JSONArray(request(baseUrl, path, token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    fun updateIssue(baseUrl: String, token: String, issueId: Long, status: String): JSONObject {
        val normalized = status.lowercase(Locale.ROOT).replace(' ', '_').replace('-', '_')
        return JSONObject(request(baseUrl, "/owner/issues/$issueId", token, "PATCH", JSONObject().put("status", normalized)))
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
        val trimmed = value.trim()
        for (format in listOf("yyyy-MM-dd", "d MMM yyyy", "dd MMM yyyy", "d MMMM yyyy", "dd MMMM yyyy")) {
            val parser = SimpleDateFormat(format, Locale.ENGLISH).apply { isLenient = false; timeZone = TimeZone.getTimeZone("UTC") }
            val position = java.text.ParsePosition(0)
            val date = runCatching { parser.parse(trimmed, position) }.getOrNull() ?: continue
            if (position.index != trimmed.length) continue
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(date)
        }
        return null
    }

    /** Read the shared owner ledger, optionally scoped to one property. */
    fun ownerLedger(baseUrl: String, token: String, propertyId: Long? = null): List<JSONObject> {
        val path = propertyId?.let { "/owner/ledger?property_id=$it" } ?: "/owner/ledger"
        val result = JSONArray(request(baseUrl, path, token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    /** Residents can only read entries linked to their own server-backed stays. */
    fun myLedger(baseUrl: String, token: String): List<JSONObject> {
        val result = JSONArray(request(baseUrl, "/ledger/me", token))
        return (0 until result.length()).mapNotNull { result.optJSONObject(it) }
    }

    /** Financial records remain manual until a real payment provider and webhook verification exist. */
    fun createManualLedgerEntry(
        baseUrl: String,
        token: String,
        propertyId: Long,
        residentStayId: Long?,
        category: String,
        amount: Int,
        billingPeriod: String,
        dueDate: String,
        paymentReference: String,
        description: String,
        reservationId: Long? = null,
        amountPaise: Long? = null
    ): JSONObject {
        require(propertyId > 0L) { "Enter a valid server property ID." }
        require(amount > 0) { "Enter an amount greater than zero." }
        val allowedCategories = setOf("rent_charge", "payment", "deposit", "refund", "deduction", "expense", "food", "utility", "late_fee")
        require(category in allowedCategories) { "Unsupported financial record type." }
        val period = billingPeriod.trim()
        if (category == "rent_charge" || period.isNotBlank()) {
            require(Regex("^\\d{4}-(0[1-9]|1[0-2])$").matches(period)) { "Enter the billing period as YYYY-MM, for example 2026-11." }
        }
        val normalizedDueDate = if (dueDate.isBlank()) null else
            normalizeDate(dueDate) ?: throw IllegalArgumentException("Enter a valid date, for example 2026-11-05.")
        val safeStatus = when (category) {
            "rent_charge", "food", "utility", "late_fee", "expense" -> "open"
            "deposit" -> "held"
            "payment" -> "recorded"
            "refund", "deduction" -> "recorded"
            else -> "open"
        }
        val body = JSONObject()
            .put("property_id", propertyId)
            .put("category", category)
            .put("amount", amount)
            .put("billing_period", period)
            .put("payment_method", "manual")
            .put("payment_reference", paymentReference.trim())
            .put("provider_status", "manual_record")
            .put("description", description.trim())
            .put("status", safeStatus)
        amountPaise?.let {
            require(it > 0L) { "Enter an amount greater than zero." }
            body.put("amount_paise", it)
        }
        reservationId?.takeIf { it > 0L }?.let { body.put("reservation_id", it) }
        if (residentStayId != null) {
            require(residentStayId > 0L) { "Enter a valid resident stay ID." }
            body.put("resident_stay_id", residentStayId)
        }
        if (normalizedDueDate != null) {
            if (category == "payment") body.put("paid_at", "${normalizedDueDate}T00:00:00Z")
            else body.put("due_date", normalizedDueDate)
        }
        return JSONObject(request(baseUrl, "/owner/ledger", token, "POST", body))
    }

    /** Update a server-owned unit; the UI must wait for this response before reflecting the change. */
    fun updateUnitStatus(baseUrl: String, token: String, unitId: Long, status: String): JSONObject {
        require(unitId > 0L) { "Enter a valid server unit ID." }
        val normalized = status.trim().lowercase(Locale.ROOT)
        require(normalized in setOf("available", "reserved", "occupied", "maintenance", "inactive")) {
            "Unsupported inventory status."
        }
        val body = JSONObject().put("status", normalized)
        return JSONObject(request(baseUrl, "/owner/units/$unitId/status", token, "PATCH", body))
    }

    /** Idempotent server-side rent generation for a property's billing period. */
    fun generateRentCharges(
        baseUrl: String,
        token: String,
        propertyId: Long,
        billingPeriod: String,
        dueDate: String,
        proRataPolicy: String = "actual_days"
    ): JSONObject {
        require(propertyId > 0L) { "Enter a valid server property ID." }
        require(Regex("^\\d{4}-(0[1-9]|1[0-2])$").matches(billingPeriod.trim())) {
            "Enter the billing period as YYYY-MM, for example 2026-11."
        }
        val normalizedDueDate = normalizeDate(dueDate)
            ?: throw IllegalArgumentException("Enter a valid due date, for example 2026-11-05.")
        require(proRataPolicy in setOf("actual_days", "fixed_30")) { "Unsupported pro-rata policy." }
        val body = JSONObject()
            .put("property_id", propertyId)
            .put("billing_period", billingPeriod.trim())
            .put("due_date", normalizedDueDate)
            .put("pro_rata_policy", proRataPolicy)
        return JSONObject(request(baseUrl, "/owner/billing/generate", token, "POST", body))
    }

    fun ownerDashboard(baseUrl: String, token: String): JSONObject = JSONObject(request(baseUrl, "/owner/dashboard", token))
}
