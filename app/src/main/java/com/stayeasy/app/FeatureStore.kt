package com.stayeasy.app

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Device-local persistence used by the interactive prototype. Production data must live on the API. */
data class StayRecord(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val title: String,
    val details: String = "",
    val status: String = "Open",
    val amount: Int = 0,
    val person: String = "",
    val property: String = "",
    val unit: String = "",
    val date: String = "",
    val priority: String = "Normal",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val source: String = "Local demo"
)

object StayRecordStore {
    private const val PREFS = "stayeasy_records_v1"
    private fun key(category: String) = "records_${category.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9_]+"), "_")}"

    fun load(context: Context, category: String): List<StayRecord> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key(category), null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            StayRecord(
                id = o.optString("id", UUID.randomUUID().toString()), category = o.optString("category", category),
                title = o.optString("title"), details = o.optString("details"), status = o.optString("status", "Open"),
                amount = o.optInt("amount"), person = o.optString("person"), property = o.optString("property"),
                unit = o.optString("unit"), date = o.optString("date"), priority = o.optString("priority", "Normal"),
                createdAt = o.optLong("createdAt", System.currentTimeMillis()), updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                source = o.optString("source", "Local demo")
            )
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, category: String, records: List<StayRecord>) {
        val array = JSONArray()
        records.forEach { r ->
            array.put(JSONObject().apply {
                put("id", r.id); put("category", r.category); put("title", r.title); put("details", r.details)
                put("status", r.status); put("amount", r.amount); put("person", r.person); put("property", r.property)
                put("unit", r.unit); put("date", r.date); put("priority", r.priority); put("createdAt", r.createdAt)
                put("updatedAt", r.updatedAt); put("source", r.source)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(key(category), array.toString()) }
    }

    fun add(context: Context, record: StayRecord): StayRecord {
        val records = load(context, record.category).toMutableList()
        records.add(0, record)
        save(context, record.category, records)
        return record
    }

    fun update(context: Context, category: String, record: StayRecord) {
        val records = load(context, category).toMutableList()
        val index = records.indexOfFirst { it.id == record.id }
        if (index >= 0) records[index] = record.copy(updatedAt = System.currentTimeMillis()) else records.add(0, record)
        save(context, category, records)
    }

    /**
     * Replace server-backed rows. Finance categories can set preserveLocal=false so stale demo
     * amounts never appear alongside the authoritative server ledger after sync is configured.
     */
    fun replaceServerRecords(
        context: Context,
        category: String,
        serverRecords: List<StayRecord>,
        preserveLocal: Boolean = true
    ) {
        val localRecords = if (preserveLocal) load(context, category).filter { it.source != "Server" } else emptyList()
        val merged = (serverRecords + localRecords).distinctBy { it.id }
            .sortedByDescending { it.updatedAt }
        save(context, category, merged)
    }

    fun remove(context: Context, category: String, id: String) {
        save(context, category, load(context, category).filterNot { it.id == id })
    }

    fun notify(context: Context, title: String, details: String, property: String = "") {
        add(context, StayRecord(category = "notifications", title = title, details = details, property = property, status = "Unread"))
    }

    fun addIfAbsent(context: Context, record: StayRecord) {
        if (load(context, record.category).none { it.id == record.id }) add(context, record)
    }

    fun dateLabel(epoch: Long): String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(epoch))
}

data class StayPreferences(
    val mode: String = "System",
    val theme: String = "Midnight Cobalt",
    val language: String = "English",
    val largeText: Boolean = false,
    val enquiryAlerts: Boolean = true,
    val rentAlerts: Boolean = true,
    val maintenanceAlerts: Boolean = true,
    val availabilityAlerts: Boolean = true,
    val apiBaseUrl: String = "http://10.0.2.2:8000"
)

object StayPreferenceStore {
    private const val PREFS = "stayeasy_preferences_v1"
    private const val DEFAULT_EMULATOR_API_URL = "http://10.0.2.2:8000"

    fun load(context: Context): StayPreferences {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedApiBaseUrl = p.getString("apiBaseUrl", null)
        // A blank value persisted by earlier builds is migrated to the local test API.
        // Users can still change the URL in API connection & sync for staging/physical devices.
        val apiBaseUrl = savedApiBaseUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_EMULATOR_API_URL
        return StayPreferences(
            mode = p.getString("mode", "System") ?: "System", theme = p.getString("theme", "Midnight Cobalt") ?: "Midnight Cobalt",
            language = p.getString("language", "English") ?: "English", largeText = p.getBoolean("largeText", false),
            enquiryAlerts = p.getBoolean("enquiryAlerts", true), rentAlerts = p.getBoolean("rentAlerts", true),
            maintenanceAlerts = p.getBoolean("maintenanceAlerts", true), availabilityAlerts = p.getBoolean("availabilityAlerts", true),
            apiBaseUrl = apiBaseUrl
        )
    }
    fun save(context: Context, value: StayPreferences) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString("mode", value.mode); putString("theme", value.theme); putString("language", value.language)
            putBoolean("largeText", value.largeText); putBoolean("enquiryAlerts", value.enquiryAlerts)
            putBoolean("rentAlerts", value.rentAlerts); putBoolean("maintenanceAlerts", value.maintenanceAlerts)
            putBoolean("availabilityAlerts", value.availabilityAlerts)
            putString("apiBaseUrl", value.apiBaseUrl)
        }
    }
}

object StayRecordConfig {
    val titles = mapOf(
        "saved_searches" to "Saved searches & alerts", "enquiries" to "Enquiries inbox", "visits" to "Visits & appointments",
        "reservations" to "Bookings & reservations", "messages" to "Communication history", "stays" to "Resident stays", "move_out_requests" to "Move-out requests",
        "payments" to "Payments ledger", "charges" to "Rent & charges", "deposits" to "Security deposits",
        "expenses" to "Property expenses", "issues" to "Maintenance & support", "residents" to "Residents",
        "beds" to "Room & bed inventory", "staff" to "Staff & permissions", "notices" to "Notices & announcements",
        "food" to "Food & meal plans", "housekeeping" to "Housekeeping schedule", "visitors" to "Visitor log",
        "reports" to "Business reports", "quality" to "Listing quality & verification", "tasks" to "Owner action queue",
        "notifications" to "Notification inbox", "reviews" to "Reviews & responses", "privacy" to "Privacy & security",
        "admin" to "Platform administration", "documents" to "Stay documents", "profile" to "Account details"
    )
    val descriptions = mapOf(
        "saved_searches" to "Save a search to revisit it and track matching availability. Alerts are local in this prototype.",
        "enquiries" to "Track customer questions separately from reservations and active resident stays.",
        "visits" to "Schedule, reschedule and record the outcome of property visits.",
        "reservations" to "Record a requested bed or room, confirmation terms and expiry. Active duplicate unit reservations are blocked on this device.",
        "messages" to "Keep communication notes linked to a property and customer enquiry.",
        "stays" to "View the server-backed owner stay history and manage actual move-out completion after notice is recorded.",
        "move_out_requests" to "Review tenant move-out requests, save an owner response and record notice. Units remain occupied until actual move-out is completed.",
        "payments" to "Record full or partial payments and the method/reference. Manual records are not bank-confirmed.",
        "charges" to "Record rent, food, utility and other charges with billing periods and due dates.",
        "deposits" to "Track deposits received, deductions, move-out settlement and refund status separately from rent.",
        "expenses" to "Categorize property operating costs and export a working CSV report.",
        "issues" to "Track urgency, affected space, assigned person and resolution history.",
        "residents" to "View residents linked to verified server stays when an owner API session is active; other resident profile and document actions remain limited.",
        "beds" to "Track unit assignment, availability and maintenance. For a shared backend, server-side conflict checks are required.",
        "staff" to "Document staff roles and property scopes. This local prototype does not grant server permissions.",
        "notices" to "Draft and publish resident announcements and track their local status.",
        "food" to "Manage meal availability, menu notes, preferences and optional monthly charges.",
        "housekeeping" to "Plan room/common-area cleaning tasks and mark completion.",
        "visitors" to "Maintain a privacy-conscious visit log only where appropriate and lawful.",
        "reports" to "Review recorded operations and export data; figures are only as complete as the local records.",
        "quality" to "Review listing completeness and owner verification notes before marking a listing ready.",
        "tasks" to "A practical queue for follow-ups, due dates, overdue balances and open issues.",
        "notifications" to "Local in-app notices. Push delivery needs a configured backend and notification provider.",
        "reviews" to "Capture feedback and owner responses. Verified-stay eligibility requires backend enforcement.",
        "privacy" to "Review data/privacy actions. Production account deletion must also erase or retain server records according to law.",
        "admin" to "Prototype moderation queue for listings, complaints and verification; protect this behind a real admin role in production.",
        "documents" to "Keep document metadata here only; store sensitive document files in secure, access-controlled storage.",
        "profile" to "Capture local account preferences while sign-in and identity verification are being integrated."
    )
    fun defaultStatus(category: String) = when (category) {
        "enquiries" -> "New"; "visits" -> "Requested"; "reservations" -> "Pending"; "saved_searches" -> "Active"
        "messages" -> "Sent"; "stays" -> "Upcoming"; "move_out_requests" -> "Pending"; "payments" -> "Due"; "charges" -> "Open"; "deposits" -> "Held"
        "expenses" -> "Recorded"; "issues" -> "Open"; "residents" -> "Active"; "beds" -> "Available"
        "staff" -> "Invited"; "notices" -> "Draft"; "food" -> "Available"; "housekeeping" -> "Scheduled"
        "visitors" -> "Expected"; "reports" -> "Ready"; "quality" -> "Needs review"; "tasks" -> "Open"
        "notifications" -> "Unread"; "reviews" -> "Received"; "privacy" -> "Requested"; "admin" -> "Needs review"
        "documents" -> "Pending"; else -> "Saved"
    }
    fun statuses(category: String) = when (category) {
        "enquiries" -> listOf("New", "Contacted", "Visit scheduled", "Converted", "Closed")
        "visits" -> listOf("Requested", "Confirmed", "Completed", "Cancelled", "No-show")
        "reservations" -> listOf("Pending", "Confirmed", "Cancelled", "Expired", "Checked in")
        "messages" -> listOf("Draft", "Sent", "Delivered", "Responded", "Closed")
        "stays" -> listOf("Upcoming", "Active", "Notice given", "Completed", "Cancelled")
        "move_out_requests" -> listOf("Pending", "Approved", "Rejected", "Cancelled")
        "payments" -> listOf("Due", "Part paid", "Paid", "Reconciled", "Reversed")
        "charges" -> listOf("Open", "Part paid", "Paid", "Waived", "Overdue")
        "deposits" -> listOf("Held", "Settlement pending", "Deduction proposed", "Refund due", "Refunded", "Disputed")
        "expenses" -> listOf("Recorded", "Approved", "Reimbursed", "Rejected")
        "issues" -> listOf("Open", "In progress", "Waiting for resident", "Resolved", "Reopened", "Escalated")
        "residents" -> listOf("Upcoming", "Active", "Notice given", "Former")
        "beds" -> listOf("Available", "Reserved", "Occupied", "Maintenance", "Inactive")
        "staff" -> listOf("Invited", "Active", "Suspended", "Revoked")
        "notices" -> listOf("Draft", "Published", "Archived")
        "food" -> listOf("Available", "Paused", "Included", "Extra charge")
        "housekeeping" -> listOf("Scheduled", "In progress", "Completed", "Skipped")
        "visitors" -> listOf("Expected", "Checked in", "Checked out", "Cancelled")
        "reports" -> listOf("Ready", "Exported", "Archived")
        "quality" -> listOf("Needs review", "Changes requested", "Verified", "Published", "Paused", "Rejected")
        "tasks" -> listOf("Open", "In progress", "Done", "Deferred")
        "notifications" -> listOf("Unread", "Read", "Archived")
        "reviews" -> listOf("Received", "Under review", "Responded", "Hidden")
        "privacy" -> listOf("Requested", "In progress", "Completed", "Needs identity check", "Declined")
        "admin" -> listOf("Needs review", "In progress", "Resolved", "Escalated", "Rejected")
        "documents" -> listOf("Pending", "Received", "Verified", "Expired", "Rejected")
        else -> listOf("Open", "In progress", "Done", "Archived")
    }
    fun amountLabel(category: String) = when (category) {
        "payments" -> "Payment amount (₹)"; "charges" -> "Charge amount (₹)"; "deposits" -> "Deposit / refund amount (₹)"
        "expenses" -> "Expense amount (₹)"; "reservations" -> "Agreed initial amount (₹)"; "stays" -> "Monthly rent (₹)"; "move_out_requests" -> "Requested move-out date"
        "food" -> "Monthly charge (₹)"; else -> "Amount (₹, optional)"
    }
    fun usesAmount(category: String) = category in setOf("payments", "charges", "deposits", "expenses", "reservations", "stays", "food")
}

data class DiscoveryFilters(
    val minRent: Int = 0,
    val minDeposit: Int = 0,
    val maxDeposit: Int = 100000,
    val filterDeposit: Boolean = false,
    val filterDistance: Boolean = true,
    val maxDistanceKm: Float = 5f,
    val roomTypes: Set<String> = emptySet(),
    val availableOnly: Boolean = false,
    val foodIncluded: Boolean = false,
    val acRequired: Boolean = false,
    val attachedBathroom: Boolean = false,
    val furnishedOnly: Boolean = false,
    val verifiedOnly: Boolean = false,
    val cookingAllowed: Boolean = false,
    val minStayMonths: Int = 0,
    val moveInDate: String = "",
    val sortBy: String = "Recommended"
)

object StayUserDataStore {
    private const val PREFS = "stayeasy_user_state_v1"
    fun loadFavorites(context: Context): List<Long> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("favorites", "[]") ?: "[]"
        val array = JSONArray(raw)
        (0 until array.length()).map { array.optLong(it) }.distinct()
    }.getOrDefault(emptyList())
    fun saveFavorites(context: Context, values: List<Long>) {
        val array = JSONArray(); values.distinct().forEach { array.put(it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString("favorites", array.toString()) }
    }
    fun recentLocations(context: Context): List<String> = runCatching {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("recent_locations", "[]") ?: "[]"
        val array = JSONArray(raw); (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }
    }.getOrDefault(emptyList())
    fun addRecentLocation(context: Context, location: String) {
        if (location.isBlank()) return
        val updated = (listOf(location.trim()) + recentLocations(context).filterNot { it.equals(location.trim(), true) }).take(8)
        val array = JSONArray(); updated.forEach { array.put(it) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString("recent_locations", array.toString()) }
    }
}
