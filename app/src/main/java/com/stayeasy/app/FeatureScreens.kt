package com.stayeasy.app

import org.json.JSONObject
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HubBlue = Color(0xFF315BE8)

@Composable
internal fun FeatureHubScreen(
    ownerMode: Boolean,
    onOpenRecord: (String) -> Unit,
    onCompare: () -> Unit,
    onThemes: () -> Unit,
    onApi: () -> Unit,
    onBack: () -> Unit
) {
    val groups = if (ownerMode) listOf(
        "PROPERTY OPERATIONS" to listOf(
            Triple("Property portfolio", "Listing setup, publish status and quality", "quality"),
            Triple("Room & bed inventory", "Availability, occupancy, assignment and maintenance", "beds"),
            Triple("Residents & stays", "Joining, renewals, transfers and move-outs", "residents"),
            Triple("Owner action queue", "Today’s arrivals, due items and issues", "tasks"),
            Triple("Food & meal plans", "Meals, menus and optional charges", "food"),
            Triple("Housekeeping", "Schedules, completion and common areas", "housekeeping"),
            Triple("Staff & permissions", "Roles, property scope and access history", "staff")
        ),
        "CUSTOMER OPERATIONS" to listOf(
            Triple("Enquiries inbox", "Questions, response status and follow-ups", "enquiries"),
            Triple("Visits & appointments", "Schedule, reschedule and record outcomes", "visits"),
            Triple("Reservations", "Confirmed terms, expiry and unit conflict checks", "reservations"),
            Triple("Communication history", "Notes associated with enquiries and residents", "messages"),
            Triple("Notices & announcements", "Draft, publish and archive resident notices", "notices"),
            Triple("Reviews & responses", "Feedback queue and owner responses", "reviews"),
            Triple("Visitor log", "Privacy-conscious visitor records where appropriate", "visitors")
        ),
        "FINANCE & PROPERTY CARE" to listOf(
            Triple("Rent & charges", "Monthly charges, billing periods and balances", "charges"),
            Triple("Payments ledger", "Full/partial payments, methods and references", "payments"),
            Triple("Security deposits", "Held deposits, deductions and settlements", "deposits"),
            Triple("Operating expenses", "Utilities, food, repairs and housekeeping", "expenses"),
            Triple("Maintenance queue", "Priority, staff assignment and resolution", "issues"),
            Triple("Business reports", "Occupancy, collection, expenses and export", "reports")
        ),
        "PLATFORM & TRUST" to listOf(
            Triple("Listing verification", "Completeness, review, complaints and moderation", "admin"),
            Triple("Notifications", "In-app notices and delivery preferences", "notifications"),
            Triple("Privacy & security", "Data requests and document access notes", "privacy"),
            Triple("API connection & sync", "Test your backend and load server-published properties", "__api")
        )
    ) else listOf(
        "DISCOVER & DECIDE" to listOf(
            Triple("Saved searches & alerts", "Revisit criteria and note availability changes", "saved_searches"),
            Triple("Compare PGs", "Compare up to three shortlisted properties", "__compare"),
            Triple("Enquiries inbox", "Owner replies and conversation history", "enquiries"),
            Triple("Visits & appointments", "Request, confirm, reschedule and record visits", "visits"),
            Triple("Bookings & reservations", "Track the reservation request and agreed terms", "reservations"),
            Triple("Communication history", "Keep conversations with the right property", "messages"),
            Triple("Notifications", "Visit, enquiry and availability updates", "notifications")
        ),
        "MY ACCOMMODATION" to listOf(
            Triple("My stay & lifecycle", "Move-in, renewal, transfer, notice and move-out", "stays"),
            Triple("Rent & charges", "Invoice statements, food/utilities and due dates", "charges"),
            Triple("Payment history", "Receipts and payment reconciliation status", "payments"),
            Triple("Maintenance & support", "Report an issue and follow its resolution", "issues"),
            Triple("Stay documents", "Agreements and document status metadata", "documents"),
            Triple("Reviews & feedback", "Post-stay feedback and moderation status", "reviews")
        ),
        "ACCOUNT & ACCESSIBILITY" to listOf(
            Triple("Appearance & themes", "System, light/dark and six colour palettes", "__themes"),
            Triple("Privacy & account data", "Data requests, consent and deletion workflow", "privacy"),
            Triple("Account preferences", "Profile notes, language and accessibility", "profile"),
            Triple("API connection & sync", "Test your backend and load server-published properties", "__api")
        )
    )
    Column(Modifier.fillMaxSize().background(SE.Background).verticalScroll(rememberScrollState()).padding(horizontal = 17.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(if (ownerMode) "Owner workspace" else "Your StayEasy", color = SE.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(if (ownerMode) "Operations, people and finances" else "Your shortlist and stay tools", color = SE.Muted, fontSize = 12.sp)
            }
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 11.dp, vertical = 8.dp)) {
                Icon(Icons.Filled.ArrowBack, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Back")
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(shape = SE.cardShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Ink), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (ownerMode) Icons.Filled.HomeWork else Icons.Filled.Search, null, tint = Color.White, modifier = Modifier.size(27.dp))
                Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) {
                    Text(if (ownerMode) "Daily operations" else "Make a confident decision", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(if (ownerMode) "Start with records needing attention, then update their status." else "Keep visits, enquiries, bookings and stay records in separate workflows.", color = Color.White.copy(alpha = .78f), fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
        groups.forEach { (group, entries) ->
            Spacer(Modifier.height(20.dp)); Text(group, color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            entries.forEach { (title, subtitle, category) ->
                val click = when (category) { "__compare" -> onCompare; "__themes" -> onThemes; "__api" -> onApi; else -> { { onOpenRecord(category) } } }
                Card(shape = SE.cardShape(15.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { click() }) {
                    Row(Modifier.padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(41.dp).clip(RoundedCornerShape(13.dp)).background(SE.BlueSoft), contentAlignment = Alignment.Center) {
                            Icon(when (category) { "beds" -> Icons.Filled.Bed; "notifications" -> Icons.Filled.Notifications; "privacy", "admin" -> Icons.Filled.Security; "__themes" -> Icons.Filled.Palette; "__compare" -> Icons.Filled.CompareArrows; else -> Icons.Filled.Settings }, null, tint = SE.Blue)
                        }
                        Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) {
                            Text(title, color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(subtitle, color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                        }
                        Text("›", color = SE.Blue, fontSize = 25.sp, fontWeight = FontWeight.Light)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Demo mode: records are saved on this device. Real sign-in, cross-device sync, provider payments and push delivery require the production services.", color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(18.dp))
    }
}

private fun candidateRecordForLedger(
    id: String,
    category: String,
    entryType: String,
    title: String,
    description: String,
    amount: Int,
    propertyId: Long,
    residentStayId: Long?,
    billingPeriod: String,
    dueDate: String,
    reference: String,
    status: String,
    updatedAt: Long
): StayRecord {
    val typeLabel = when (entryType) {
        "payment" -> "Payment record"
        "rent_charge" -> "Rent charge"
        "deposit" -> "Security deposit"
        "refund" -> "Deposit refund"
        "deduction" -> "Deposit deduction"
        "expense" -> "Operating expense"
        "food" -> "Food charge"
        "utility" -> "Utility charge"
        "late_fee" -> "Late fee"
        else -> entryType.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    val parts = mutableListOf<String>()
    if (billingPeriod.isNotBlank()) parts += "Billing period: $billingPeriod"
    if (dueDate.isNotBlank()) parts += if (entryType == "payment") "Payment date recorded: $dueDate" else "Due date: $dueDate"
    if (residentStayId != null) parts += "Resident stay ID: $residentStayId"
    if (reference.isNotBlank()) parts += "Reference: $reference"
    if (description.isNotBlank()) parts += description
    parts += "Manually recorded · not verified by a payment provider"
    val displayStatus = when (entryType) {
        "payment" -> "Recorded manually"
        "deposit" -> "Held"
        "rent_charge", "food", "utility", "late_fee", "expense" -> "Open"
        else -> status.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }
    return StayRecord(
        id = id,
        category = category,
        title = "$typeLabel · ${title.ifBlank { "Property #$propertyId" }}",
        details = parts.joinToString("\n"),
        status = displayStatus,
        amount = amount,
        property = "Property #$propertyId",
        date = dueDate.ifBlank { billingPeriod },
        updatedAt = updatedAt,
        createdAt = updatedAt,
        source = "Server"
    )
}

@Composable
internal fun FeatureRecordsScreen(category: String, ownerMode: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val title = StayRecordConfig.titles[category] ?: category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val records = remember(category) { mutableStateListOf<StayRecord>().apply { addAll(StayRecordStore.load(context, category)) } }
    val apiPreferences = remember { StayPreferenceStore.load(context) }
    val sessionToken = StayApiClient.currentSessionToken(apiPreferences.apiBaseUrl)
    val remoteScope = rememberCoroutineScope()
    var remoteSyncMessage by remember(category) { mutableStateOf("") }
    var remoteSyncing by remember(category) { mutableStateOf(false) }
    var serverRefreshRequest by remember(category) { mutableIntStateOf(0) }
    val financeCategories = setOf("payments", "charges", "deposits", "expenses", "food")
    val isFinanceCategory = category in financeCategories
    val serverFinanceMode = ownerMode && isFinanceCategory && apiPreferences.apiBaseUrl.isNotBlank() && !sessionToken.isNullOrBlank()
    val readOnlyServerFinance = !ownerMode && isFinanceCategory && apiPreferences.apiBaseUrl.isNotBlank() && !sessionToken.isNullOrBlank()
    val serverOwnerStayMode = ownerMode && category in setOf("residents", "stays") &&
        apiPreferences.apiBaseUrl.isNotBlank() && !sessionToken.isNullOrBlank()
    val serverMoveOutRequestMode = ownerMode && category == "move_out_requests" &&
        apiPreferences.apiBaseUrl.isNotBlank() && !sessionToken.isNullOrBlank()
    var showBillingDialog by remember(category) { mutableStateOf(false) }
    var billingPropertyIdText by remember(category) { mutableStateOf("") }
    var billingPeriodText by remember(category) { mutableStateOf(java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))) }
    var billingDueDateText by remember(category) { mutableStateOf(java.time.LocalDate.now().plusDays(7).toString()) }
    var generatingBilling by remember(category) { mutableStateOf(false) }
    var billingError by remember(category) { mutableStateOf("") }
    var savingServerRecord by remember(category) { mutableStateOf(false) }

    fun mapLedgerRows(rows: List<org.json.JSONObject>): List<StayRecord> = rows.mapNotNull { row ->
        val ledgerCategory = row.optString("category")
        val matches = when (category) {
            "payments" -> ledgerCategory == "payment"
            "charges" -> ledgerCategory in setOf("rent_charge", "food", "utility", "late_fee")
            "deposits" -> ledgerCategory in setOf("deposit", "refund", "deduction")
            "expenses" -> ownerMode && ledgerCategory == "expense"
            "food" -> ownerMode && ledgerCategory == "food"
            else -> false
        }
        if (!matches) return@mapNotNull null
        val propertyName = row.optString("property_name", "Property #${row.optLong("property_id")}")
        val providerStatus = row.optString("provider_status", "manual_record")
        val description = row.optString("description")
        val period = row.optString("billing_period")
        val dueDate = row.optString("due_date")
        val paidAt = row.optString("paid_at").take(10)
        val reference = row.optString("payment_reference")
        val typeLabel = when (ledgerCategory) {
            "rent_charge" -> "Rent charge"; "payment" -> "Payment record"; "deposit" -> "Security deposit"
            "refund" -> "Deposit refund"; "deduction" -> "Deposit deduction"; "expense" -> "Operating expense"
            "food" -> "Food charge"; "utility" -> "Utility charge"; "late_fee" -> "Late fee"
            else -> ledgerCategory.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
        val detailParts = mutableListOf<String>()
        if (period.isNotBlank()) detailParts += "Billing period: $period"
        if (ledgerCategory == "payment" && paidAt.isNotBlank()) detailParts += "Payment date recorded: $paidAt"
        else if (dueDate.isNotBlank()) detailParts += "Due: $dueDate"
        if (reference.isNotBlank()) detailParts += "Reference: $reference"
        if (description.isNotBlank()) detailParts += description
        detailParts += if (providerStatus == "manual_record") "Manually recorded · not verified by a payment provider" else "Provider status: $providerStatus"
        val rawStatus = row.optString("status", "open").replace('_', ' ')
        val displayStatus = if (ledgerCategory == "payment" && providerStatus == "manual_record") "Recorded manually"
            else rawStatus.replaceFirstChar { it.uppercase() }
        val residentId = row.optString("resident_id")
        val createdAt = runCatching { java.time.Instant.parse(row.optString("created_at")).toEpochMilli() }
            .getOrDefault(System.currentTimeMillis())
        StayRecord(
            id = "server-ledger-${row.optLong("id")}",
            category = category,
            title = "$typeLabel · $propertyName",
            details = detailParts.joinToString("\n"),
            status = displayStatus,
            amount = row.optInt("amount"),
            person = if (ownerMode && residentId.isNotBlank()) "Resident account · ${residentId.take(8)}" else "",
            property = propertyName,
            unit = row.optString("unit_code"),
            date = if (ledgerCategory == "payment") paidAt.ifBlank { dueDate.ifBlank { period } } else dueDate.ifBlank { period },
            createdAt = createdAt,
            updatedAt = createdAt,
            source = "Server"
        )
    }
    LaunchedEffect(category, apiPreferences.apiBaseUrl, sessionToken, ownerMode, serverRefreshRequest) {
        if ((category in setOf("enquiries", "reservations", "visits", "issues") || isFinanceCategory || serverOwnerStayMode || serverMoveOutRequestMode) && apiPreferences.apiBaseUrl.isNotBlank() && !sessionToken.isNullOrBlank()) {
            remoteSyncing = true
            try {
                val pulled = withContext(Dispatchers.IO) {
                    when (category) {
                        "enquiries" -> StayApiClient.enquiries(apiPreferences.apiBaseUrl, sessionToken).map { row ->
                            val id = row.optLong("id")
                            val property = row.optString("property_name", "Property #${row.optLong("property_id")}")
                            val rawStatus = row.optString("status", "new").replace('_', ' ')
                            val detail = buildString {
                                append(row.optString("message"))
                                val response = row.optString("owner_response")
                                if (response.isNotBlank()) append("\nOwner response: ").append(response)
                            }
                            StayRecord(id = "server-enquiry-$id", category = "enquiries", title = "Enquiry · $property", details = detail,
                                status = rawStatus.replaceFirstChar { it.uppercase() }, person = row.optString("customer_id"), property = property,
                                date = row.optString("move_in_date"), source = "Server")
                        }
                        "reservations" -> StayApiClient.reservations(apiPreferences.apiBaseUrl, sessionToken).map { row ->
                            val id = row.optLong("id")
                            val property = row.optString("property_name", "Property #${row.optLong("property_id")}")
                            val rawStatus = row.optString("status", "pending").replace('_', ' ')
                            val expires = row.optString("expires_at")
                            StayRecord(id = "server-reservation-$id", category = "reservations", title = "Reservation · $property",
                                details = "${row.optString("preferred_room_type")} requested.${if (expires.isNotBlank()) " Expires: $expires" else ""}",
                                status = rawStatus.replaceFirstChar { it.uppercase() }, person = row.optString("customer_id"), property = property,
                                unit = row.optLong("unit_id").takeIf { it > 0 }?.toString().orEmpty(), date = row.optString("move_in_date"),
                                amount = row.optInt("rent_snapshot") + row.optInt("deposit_snapshot"), source = "Server")
                        }
                        "issues" -> StayApiClient.issues(apiPreferences.apiBaseUrl, sessionToken).map { row ->
                            val id = row.optLong("id")
                            val rawStatus = row.optString("status", "open").replace('_', ' ')
                            val rawPriority = row.optString("priority", "normal")
                            StayRecord(
                                id = "server-issue-$id",
                                category = "issues",
                                title = row.optString("title", "Maintenance issue"),
                                details = row.optString("description"),
                                status = rawStatus.replaceFirstChar { it.uppercase() },
                                property = "Property #${row.optLong("property_id")}",
                                unit = row.optLong("unit_id").takeIf { it > 0 }?.toString().orEmpty(),
                                date = row.optString("created_at"),
                                priority = rawPriority.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                source = "Server"
                            )
                        }
                        "move_out_requests" -> StayApiClient.ownerMoveOutRequests(apiPreferences.apiBaseUrl, sessionToken).mapNotNull { row ->
                            val id = row.optLong("id")
                            if (id <= 0L) return@mapNotNull null
                            val residentName = row.optString("resident_name").ifBlank { "Resident" }
                            val propertyName = row.optString("property_name", "Property #${row.optLong("property_id")}")
                            val rawStatus = row.optString("status", "pending").lowercase(Locale.ROOT).replace('_', ' ')
                            val reason = row.optString("reason")
                            val ownerNote = row.optString("owner_note")
                            val details = buildString {
                                append("Requested move-out: ").append(row.optString("requested_move_out_date"))
                                if (reason.isNotBlank()) append("\nResident reason: ").append(reason)
                                if (ownerNote.isNotBlank()) append("\nOwner response: ").append(ownerNote)
                            }
                            val timestamp = runCatching {
                                java.time.Instant.parse(row.optString("created_at")).toEpochMilli()
                            }.getOrDefault(System.currentTimeMillis())
                            StayRecord(
                                id = "server-move-out-request-$id",
                                category = "move_out_requests",
                                title = "Move-out · $residentName",
                                details = details,
                                status = rawStatus.replaceFirstChar { it.uppercase(Locale.ROOT) },
                                person = row.optString("resident_email"),
                                property = propertyName,
                                unit = listOf(row.optString("room_code"), row.optString("unit_code"))
                                    .filter { it.isNotBlank() }.joinToString(" · "),
                                date = row.optString("requested_move_out_date"),
                                createdAt = timestamp,
                                updatedAt = timestamp,
                                source = "Server"
                            )
                        }
                        "residents", "stays" -> {
                            val stayRows = StayApiClient.ownerStays(apiPreferences.apiBaseUrl, sessionToken)
                            val displayRows = if (category == "residents") {
                                stayRows.distinctBy { row -> "${row.optLong("property_id")}:${row.optString("resident_id")}" }
                            } else stayRows
                            displayRows.mapNotNull { row ->
                            val stayId = row.optLong("id")
                            if (stayId <= 0L) return@mapNotNull null
                            val residentName = row.optString("resident_name").ifBlank { "Resident ${row.optString("resident_id").take(8)}" }
                            val residentEmail = row.optString("resident_email")
                            val propertyName = row.optString("property_name", "Property #${row.optLong("property_id")}")
                            val roomCode = row.optString("room_code")
                            val unitCode = row.optString("unit_code")
                            val unitLabel = listOf(roomCode, unitCode).filter { it.isNotBlank() }.joinToString(" · ")
                            val rawStatus = row.optString("status", "active").lowercase(Locale.ROOT).replace('_', ' ')
                            val displayStatus = if (category == "residents") {
                                when (rawStatus) {
                                    "active" -> "Active"
                                    "notice given" -> "Notice given"
                                    "completed", "cancelled" -> "Former"
                                    else -> "Upcoming"
                                }
                            } else {
                                when (rawStatus) {
                                    "active" -> "Active"
                                    "notice given" -> "Notice given"
                                    "completed" -> "Completed"
                                    "cancelled" -> "Cancelled"
                                    else -> rawStatus.replaceFirstChar { it.uppercase(Locale.ROOT) }
                                }
                            }
                            val details = buildString {
                                append("Server-confirmed stay #").append(stayId)
                                if (residentEmail.isNotBlank()) append("\nCustomer email: ").append(residentEmail)
                                if (row.optInt("deposit_held") > 0) append("\nSecurity deposit recorded: ").append(moneyText(row.optInt("deposit_held")))
                                val noticeDate = row.optString("notice_date")
                                if (noticeDate.isNotBlank()) append("\nNotice date: ").append(noticeDate)
                                val moveOutDate = row.optString("move_out_date")
                                if (moveOutDate.isNotBlank()) append("\nMove-out date: ").append(moveOutDate)
                                val agreement = row.optString("agreement_reference")
                                if (agreement.isNotBlank()) append("\nAgreement reference: ").append(agreement)
                            }
                            val timestamp = runCatching {
                                java.time.Instant.parse(row.optString("created_at")).toEpochMilli()
                            }.getOrDefault(System.currentTimeMillis())
                            StayRecord(
                                id = if (category == "residents") "server-resident-stay-$stayId" else "server-stay-$stayId",
                                category = category,
                                title = if (category == "residents") residentName else "Stay · $residentName",
                                details = details,
                                status = displayStatus,
                                amount = row.optInt("agreed_monthly_rent"),
                                person = residentEmail,
                                property = propertyName,
                                unit = unitLabel,
                                date = row.optString("joining_date"),
                                createdAt = timestamp,
                                updatedAt = timestamp,
                                source = "Server"
                            )
                            }
                        }
                        "payments", "charges", "deposits", "expenses", "food" -> {
                            val ledgerRows = if (ownerMode) StayApiClient.ownerLedger(apiPreferences.apiBaseUrl, sessionToken)
                                else StayApiClient.myLedger(apiPreferences.apiBaseUrl, sessionToken)
                            mapLedgerRows(ledgerRows)
                        }
                        else -> StayApiClient.visits(apiPreferences.apiBaseUrl, sessionToken).map { row ->
                            val id = row.optLong("id")
                            val property = row.optString("property_name", "Property #${row.optLong("property_id")}")
                            val rawStatus = row.optString("status", "requested").replace('_', ' ')
                            val outcome = row.optString("outcome")
                            val preferredAt = row.optString("preferred_at")
                            StayRecord(id = "server-visit-$id", category = "visits", title = "Visit · $property",
                                details = outcome.ifBlank { "Requested appointment: $preferredAt" },
                                status = rawStatus.replaceFirstChar { it.uppercase() }, person = row.optString("customer_id"),
                                property = property, date = preferredAt, source = "Server")
                        }
                    }
                }
                StayRecordStore.replaceServerRecords(
                    context,
                    category,
                    pulled,
                    preserveLocal = category !in financeCategories && !serverOwnerStayMode && !serverMoveOutRequestMode
                )
                records.clear(); records.addAll(StayRecordStore.load(context, category))
                remoteSyncMessage = "Synced ${pulled.size} server record(s)"
            } catch (e: Exception) {
                remoteSyncMessage = "Server sync unavailable: ${e.message ?: "check the current API session"}"
            } finally {
                remoteSyncing = false
            }
        } else {
            remoteSyncing = false
        }
    }
    var showEditor by remember(category) { mutableStateOf(false) }
    var editingId by remember(category) { mutableStateOf<String?>(null) }
    var query by remember(category) { mutableStateOf("") }
    var filter by remember(category) { mutableStateOf("All") }
    var titleText by remember(category) { mutableStateOf("") }
    var detailsText by remember(category) { mutableStateOf("") }
    var personText by remember(category) { mutableStateOf("") }
    var propertyText by remember(category) { mutableStateOf("") }
    var unitText by remember(category) { mutableStateOf("") }
    var amountText by remember(category) { mutableStateOf("") }
    var dateText by remember(category) { mutableStateOf("") }
    var recordBillingPeriodText by remember(category) { mutableStateOf("") }
    var paymentReferenceText by remember(category) { mutableStateOf("") }
    var financialEntryType by remember(category) {
        mutableStateOf(when (category) {
            "payments" -> "payment"; "charges" -> "rent_charge"; "deposits" -> "deposit"
            "expenses" -> "expense"; "food" -> "food"; else -> ""
        })
    }
    var priority by remember(category) { mutableStateOf("Normal") }
    var formError by remember(category) { mutableStateOf("") }
    var showDeleteId by remember(category) { mutableStateOf<String?>(null) }
    var showServerStatusId by remember(category) { mutableStateOf<String?>(null) }
    var updatingServerStatus by remember(category) { mutableStateOf(false) }
    var serverStatusError by remember(category) { mutableStateOf("") }
    var serverStayDateText by remember(category) { mutableStateOf(java.time.LocalDate.now().toString()) }
    var serverOwnerNoteText by remember(category) { mutableStateOf("") }

    fun refresh() { records.clear(); records.addAll(StayRecordStore.load(context, category)) }
    fun updateServerStatus(record: StayRecord, nextStatus: String) {
        val token = StayApiClient.currentSessionToken(apiPreferences.apiBaseUrl)
        val serverId = record.id.substringAfterLast('-').toLongOrNull()
        if (token == null || serverId == null || apiPreferences.apiBaseUrl.isBlank()) {
            remoteSyncMessage = "Connect a valid owner API session before changing server records."
            showServerStatusId = null
            return
        }
        if (updatingServerStatus) return
        val normalizedStatus = nextStatus.trim().lowercase(Locale.ROOT).replace(' ', '_')
        val lifecycleDate = serverStayDateText.trim()
        if (category == "stays" && normalizedStatus in setOf("notice_given", "completed") &&
            runCatching { java.time.LocalDate.parse(lifecycleDate) }.isFailure
        ) {
            serverStatusError = if (normalizedStatus == "notice_given") {
                "Enter the notice date as YYYY-MM-DD."
            } else {
                "Enter the move-out date as YYYY-MM-DD."
            }
            return
        }
        updatingServerStatus = true
        serverStatusError = ""
        remoteScope.launch {
            var updatedOnServer = false
            try {
                val response = withContext(Dispatchers.IO) {
                    when (category) {
                        "reservations" -> StayApiClient.updateReservation(apiPreferences.apiBaseUrl, token, serverId, nextStatus)
                        "visits" -> StayApiClient.updateVisit(apiPreferences.apiBaseUrl, token, serverId, nextStatus)
                        "issues" -> StayApiClient.updateIssue(apiPreferences.apiBaseUrl, token, serverId, nextStatus)
                        "stays" -> StayApiClient.updateOwnerStay(
                            apiPreferences.apiBaseUrl,
                            token,
                            serverId,
                            nextStatus,
                            noticeDate = lifecycleDate.takeIf { normalizedStatus == "notice_given" },
                            moveOutDate = lifecycleDate.takeIf { normalizedStatus == "completed" }
                        )
                        "move_out_requests" -> StayApiClient.reviewMoveOutRequest(
                            apiPreferences.apiBaseUrl, token, serverId, normalizedStatus, serverOwnerNoteText
                        )
                        else -> StayApiClient.updateEnquiry(apiPreferences.apiBaseUrl, token, serverId, nextStatus)
                    }
                }
                val rawStatus = response.optString("status", nextStatus.lowercase(Locale.ROOT).replace(' ', '_'))
                val displayStatus = rawStatus.replace('_', ' ').replaceFirstChar { it.uppercase() }
                if (category in setOf("stays", "move_out_requests")) {
                    // Re-read canonical server state after stay lifecycle or request review.
                    serverRefreshRequest++
                } else {
                    StayRecordStore.update(context, category, record.copy(status = displayStatus, updatedAt = System.currentTimeMillis()))
                    refresh()
                }
                StayRecordStore.notify(context, "$title updated on server", "${record.title}: $displayStatus", record.property)
                remoteSyncMessage = "Server confirmed status: $displayStatus"
                updatedOnServer = true
            } catch (e: Exception) {
                val message = e.message ?: "check owner permission and the date values"
                remoteSyncMessage = "Server update failed: $message"
                serverStatusError = message
            } finally {
                updatingServerStatus = false
                if (updatedOnServer || category !in setOf("stays", "move_out_requests")) showServerStatusId = null
            }
        }
    }
    fun clearForm() {
        editingId = null; titleText = ""; detailsText = ""; personText = ""; propertyText = ""; unitText = ""; amountText = ""; dateText = ""; paymentReferenceText = ""
        recordBillingPeriodText = if (category == "charges") java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")) else ""
        financialEntryType = when (category) {
            "payments" -> "payment"; "charges" -> "rent_charge"; "deposits" -> "deposit"
            "expenses" -> "expense"; "food" -> "food"; else -> ""
        }
        priority = "Normal"; formError = ""
    }
    fun openCreate() { clearForm(); showEditor = true }
    fun openEdit(r: StayRecord) {
        editingId = r.id; titleText = r.title; detailsText = r.details; personText = r.person; propertyText = r.property; unitText = r.unit
        amountText = if (r.amount == 0) "" else r.amount.toString(); dateText = r.date; priority = r.priority; formError = ""; showEditor = true
    }
    val isFinance = category in setOf("payments", "charges", "deposits", "expenses", "stays", "reservations", "food")
    val isOwnerOnly = category in setOf("beds", "residents", "move_out_requests", "charges", "deposits", "expenses", "staff", "notices", "food", "housekeeping", "visitors", "reports", "quality", "tasks", "admin")
    val visible = records.filter { r -> (query.isBlank() || listOf(r.title, r.details, r.person, r.property, r.unit).any { it.contains(query, true) }) && (filter == "All" || r.status == filter) }
    Column(Modifier.fillMaxSize().background(SE.Background).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = SE.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (StayRecordConfig.descriptions[category].isNullOrBlank()) "Local workflow record" else StayRecordConfig.descriptions[category]!!, color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                if (isFinanceCategory && !serverFinanceMode && !readOnlyServerFinance) {
                    Text("Demo-only records · connect the API and a valid account session for shared financial data.", color = SE.Orange, fontSize = 9.sp, lineHeight = 13.sp)
                }
                if (category in setOf("residents", "stays") && !serverOwnerStayMode) {
                    Text("Local records are device-only. Connect the API and sign in with an authorized owner account to view shared stay history.", color = SE.Orange, fontSize = 9.sp, lineHeight = 13.sp)
                }
                if (serverOwnerStayMode || serverMoveOutRequestMode) {
                    val serverStateCopy = when {
                        remoteSyncing -> if (serverMoveOutRequestMode) "Loading move-out requests…" else "Loading server-backed records…"
                        remoteSyncMessage.startsWith("Synced") -> if (serverMoveOutRequestMode) "Server-synced · each decision is saved by StayEasy." else "Server-synced · records come from properties you are authorized to manage."
                        remoteSyncMessage.startsWith("Server sync unavailable") -> "Server refresh failed. Cached rows may be shown; refresh before acting on this information."
                        else -> "Waiting for the first server refresh."
                    }
                    Text(serverStateCopy, color = if (remoteSyncMessage.startsWith("Server sync unavailable")) SE.Orange else SE.Blue, fontSize = 9.sp, lineHeight = 13.sp)
                }
                if (category == "move_out_requests" && !serverMoveOutRequestMode) {
                    Text("Connect the API and sign in with owner access to review shared move-out requests.", color = SE.Orange, fontSize = 9.sp, lineHeight = 13.sp)
                }
                if (readOnlyServerFinance) Text("Read-only · these entries are linked to your server-backed stay.", color = SE.Blue, fontSize = 9.sp, lineHeight = 13.sp)
                if (remoteSyncMessage.isNotBlank()) Text(remoteSyncMessage, color = if (remoteSyncMessage.startsWith("Synced") || remoteSyncMessage.startsWith("Server confirmed") || remoteSyncMessage.startsWith("Saved to server")) SE.Blue else SE.Orange, fontSize = 9.sp, lineHeight = 13.sp)
            }
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) { Icon(Icons.Filled.ArrowBack, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Back") }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            SmallInfoPill("${records.size} records", SE.BlueSoft, SE.Blue)
            if (category == "reservations") SmallInfoPill("Conflict check on device", SE.OrangeSoft, SE.Orange)
            if (isOwnerOnly) SmallInfoPill("Owner workspace", SE.Lavender, SE.Ink)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = {
                val csv = buildString {
                    appendLine("id,title,details,status,amount,person,property,unit,date,priority,createdAt")
                    records.forEach { r -> appendLine(listOf(r.id, r.title, r.details, r.status, r.amount.toString(), r.person, r.property, r.unit, r.date, r.priority, StayRecordStore.dateLabel(r.createdAt)).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" }) }
                }
                val intent = Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_SUBJECT, "StayEasy $title export"); putExtra(Intent.EXTRA_TEXT, csv) }
                runCatching { context.startActivity(Intent.createChooser(intent, "Export $title")) }
            }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp), shape = RoundedCornerShape(11.dp)) { Icon(Icons.Filled.Download, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Export") }
        }
        Spacer(Modifier.height(8.dp))
        if (serverOwnerStayMode || serverMoveOutRequestMode) {
            OutlinedButton(
                onClick = { serverRefreshRequest++ },
                enabled = !remoteSyncing,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp)
            ) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    if (remoteSyncing) "Refreshing from server…"
                    else if (serverMoveOutRequestMode) "Refresh move-out requests"
                    else "Refresh from server"
                )
            }
        }
        if (category == "charges" && serverFinanceMode) {
            OutlinedButton(
                onClick = { billingError = ""; showBillingDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp)
            ) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("Generate monthly rent charges")
            }
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Search records, people or units") }, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(7.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (listOf("All") + StayRecordConfig.statuses(category)).distinct().forEach { status -> FilterChip(selected = filter == status, onClick = { filter = status }, label = { Text(status, fontSize = 10.sp) }) }
        }
        Spacer(Modifier.height(8.dp))
        if (visible.isEmpty()) {
            Card(shape = SE.cardShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FilterAlt, null, tint = SE.Blue, modifier = Modifier.size(30.dp)); Spacer(Modifier.height(7.dp))
                    Text(
                        if (records.isEmpty()) {
                            when {
                                serverOwnerStayMode -> "No server-backed stays found"
                                serverMoveOutRequestMode -> "No move-out requests"
                                else -> "No records yet"
                            }
                        } else "No matching records",
                        color = SE.Ink,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            records.isEmpty() && serverOwnerStayMode ->
                                "Refresh to check the authorized server records, or assign a verified resident from Rooms & beds. Empty server results are not replaced with demo residents."
                            records.isEmpty() && serverMoveOutRequestMode ->
                                "New resident requests will appear here. Approving a request records notice; the unit stays occupied until actual move-out is completed."
                            records.isEmpty() ->
                                "Create the first record to start this workflow. Data will remain on this device in the prototype."
                            else -> "Try clearing the filter or search text."
                        },
                        color = SE.Muted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    if (records.isEmpty() && !readOnlyServerFinance && !serverOwnerStayMode && !serverMoveOutRequestMode) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { openCreate() }, colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)) {
                            Icon(Icons.Filled.Add, null)
                            Spacer(Modifier.width(5.dp))
                            Text("Add record")
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(visible, key = { it.id }) { r ->
                    Card(shape = SE.cardShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Column(Modifier.weight(1f)) {
                                    Text(r.title, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    if (r.person.isNotBlank()) Text(r.person, color = SE.Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                SmallInfoPill(r.status, statusBg(r.status), statusFg(r.status))
                            }
                            if (r.details.isNotBlank()) { Spacer(Modifier.height(5.dp)); Text(r.details, color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp) }
                            val meta = listOfNotNull(r.property.takeIf { it.isNotBlank() }, r.unit.takeIf { it.isNotBlank() }, r.date.takeIf { it.isNotBlank() }, r.priority.takeIf { it.isNotBlank() && it != "Normal" })
                            if (meta.isNotEmpty()) { Spacer(Modifier.height(5.dp)); Text(meta.joinToString("  ·  "), color = SE.Muted, fontSize = 10.sp) }
                            if (r.amount > 0) { Spacer(Modifier.height(7.dp)); Text("₹${String.format(Locale.getDefault(), "%,d", r.amount)}", color = SE.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
                            Spacer(Modifier.height(8.dp)); Divider(color = SE.Border.copy(alpha = .65f))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${r.source} · ${StayRecordStore.dateLabel(r.updatedAt)}", color = SE.Muted, fontSize = 8.sp, modifier = Modifier.weight(1f), maxLines = 2)
                                if (r.source != "Server" && !readOnlyServerFinance) TextButton(onClick = { openEdit(r) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) { Text("Edit", color = SE.Blue, fontSize = 10.sp) }
                                if (r.source != "Server" && !readOnlyServerFinance) TextButton(onClick = {
                                    val next = cycleStatus(category, r.status)
                                    val updated = r.copy(status = next, updatedAt = System.currentTimeMillis())
                                    StayRecordStore.update(context, category, updated); refresh()
                                    StayRecordStore.notify(context, "$title updated", "${r.title}: $next", r.property)
                                }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) { Text("Next status", color = SE.Blue, fontSize = 10.sp) }
                                if (r.source == "Server" && ownerMode && category in setOf("enquiries", "reservations", "visits", "issues", "stays", "move_out_requests") && serverStatusOptions(category, r.status).isNotEmpty()) {
                                    TextButton(onClick = {
                                        showServerStatusId = r.id
                                        serverStatusError = ""
                                        serverStayDateText = java.time.LocalDate.now().toString()
                                        serverOwnerNoteText = ""
                                    }, enabled = !updatingServerStatus,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(
                                            if (updatingServerStatus) "Updating…"
                                            else when (category) {
                                                "stays" -> "Manage stay"
                                                "move_out_requests" -> "Review request"
                                                else -> "Update status"
                                            },
                                            color = SE.Blue,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                                if (r.source != "Server" && category in setOf("saved_searches", "tasks", "notices", "messages", "profile", "privacy", "documents", "quality", "admin", "housekeeping", "food", "visits", "enquiries", "reservations")) TextButton(onClick = { showDeleteId = r.id }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) { Text("Remove", color = SE.Red, fontSize = 10.sp) }
                            }
                        }
                    }
                }
            }
        }
        if (visible.isNotEmpty() && !readOnlyServerFinance && !serverOwnerStayMode && !serverMoveOutRequestMode) {
            Spacer(Modifier.height(6.dp))
            Button(onClick = { openCreate() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(7.dp)); Text(if (serverFinanceMode) "Add manual ledger entry" else "Add ${title.removeSuffix("s").lowercase(Locale.getDefault())}") }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingId == null) "Add $title record" else "Edit $title record", color = SE.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(titleText, { titleText = it }, modifier = Modifier.fillMaxWidth(), label = { Text(when (category) { "beds" -> "Bed / unit identifier"; "reservations" -> "Property or booking title"; "saved_searches" -> "Search name"; else -> "Title / subject" }) }, singleLine = true)
                    OutlinedTextField(detailsText, { detailsText = it }, modifier = Modifier.fillMaxWidth(), label = { Text(when (category) { "saved_searches" -> "Search filters / criteria"; "issues" -> "Issue details"; "reservations" -> "Agreed terms / expiry"; "charges" -> "Billing period and description"; else -> "Details / notes" }) }, minLines = 2)
                    if (!serverFinanceMode) OutlinedTextField(personText, { personText = it }, modifier = Modifier.fillMaxWidth(), label = { Text(when (category) { "staff" -> "Staff member and role"; "visitors" -> "Visitor name / purpose"; "saved_searches" -> "Optional alert recipient"; else -> "Person / resident / customer" }) }, singleLine = true)
                    OutlinedTextField(
                        propertyText,
                        { propertyText = if (serverFinanceMode) it.filter(Char::isDigit).take(12) else it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (serverFinanceMode) "Server property ID" else "Property / location") },
                        singleLine = true,
                        placeholder = { if (serverFinanceMode) Text("Numeric ID from your server listing") }
                    )
                    if (category in setOf("beds", "reservations", "stays", "residents", "issues", "housekeeping", "visitors", "documents") ||
                        (serverFinanceMode && category in setOf("payments", "charges", "deposits", "food"))) {
                        OutlinedTextField(
                            unitText,
                            { unitText = if (serverFinanceMode) it.filter(Char::isDigit).take(12) else it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (serverFinanceMode && category in setOf("payments", "charges", "deposits", "food")) "Resident stay ID" else "Room / bed / unit (if applicable)") },
                            singleLine = true,
                            placeholder = { if (serverFinanceMode) Text("Find it in the server-backed stay record") }
                        )
                    }
                    if (category in setOf("payments", "charges", "deposits", "expenses", "food") && serverFinanceMode) {
                        val entryTypeOptions = when (category) {
                            "deposits" -> listOf("deposit" to "Deposit", "refund" to "Refund", "deduction" to "Deduction")
                            else -> listOf(financialEntryType to when (financialEntryType) {
                                "payment" -> "Manual payment"; "rent_charge" -> "Rent charge"; "expense" -> "Operating expense"; "food" -> "Food charge"; else -> "Record"
                            })
                        }
                        if (category == "deposits") {
                            Text("ENTRY TYPE", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                entryTypeOptions.forEach { (value, label) ->
                                    FilterChip(selected = financialEntryType == value, onClick = { financialEntryType = value }, label = { Text(label, fontSize = 10.sp) })
                                }
                            }
                        }
                        if (category == "charges" || category == "payments") {
                            if (category == "charges") {
                                OutlinedTextField(
                                    recordBillingPeriodText,
                                    { recordBillingPeriodText = it.filter { ch -> ch.isDigit() || ch == '-' }.take(7) },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Billing period (YYYY-MM)") },
                                    singleLine = true,
                                    placeholder = { Text("2026-11") }
                                )
                            }
                            if (category == "payments") {
                                OutlinedTextField(paymentReferenceText, { paymentReferenceText = it.take(180) }, modifier = Modifier.fillMaxWidth(), label = { Text("Manual receipt / reference (optional)") }, singleLine = true)
                            }
                        }
                    }
                    if (StayRecordConfig.usesAmount(category)) OutlinedTextField(amountText, { amountText = it.filter(Char::isDigit).take(9) }, modifier = Modifier.fillMaxWidth(), label = { Text(StayRecordConfig.amountLabel(category)) }, singleLine = true)
                    OutlinedTextField(dateText, { dateText = it }, modifier = Modifier.fillMaxWidth(), label = { Text(when (category) { "visits" -> "Visit date & time"; "reservations" -> "Expiry / move-in date"; "payments" -> "Payment date (optional)"; "charges" -> "Rent due date"; "stays" -> "Joining / move-out date"; else -> "Date / due date (optional)" }) }, singleLine = true, placeholder = { Text("e.g. 2026-11-05") })
                    Text("Priority", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Low", "Normal", "High", "Urgent").forEach { FilterChip(selected = priority == it, onClick = { priority = it }, label = { Text(it, fontSize = 10.sp) }) } }
                    if (category == "reservations") Text("A matching active reservation for the same property + unit is blocked on this device. Production must enforce this atomically on the server.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                    if (category in setOf("payments", "deposits")) Text("Manual ledger entry only; this is not a bank or payment-provider confirmation.", color = SE.Orange, fontSize = 10.sp, lineHeight = 14.sp)
                    if (formError.isNotBlank()) Text(formError, color = SE.Red, fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(enabled = !savingServerRecord, onClick = {
                if (titleText.isBlank()) { formError = "Please enter a title."; return@TextButton }
                val amount = amountText.toIntOrNull() ?: 0
                if (serverFinanceMode) {
                    val propertyId = propertyText.toLongOrNull()
                    val residentStayId = unitText.toLongOrNull()
                    if (propertyId == null || propertyId <= 0L) { formError = "Enter a valid server property ID."; return@TextButton }
                    if (amount <= 0) { formError = "Enter an amount greater than zero."; return@TextButton }
                    if (category in setOf("payments", "charges", "deposits") && (residentStayId == null || residentStayId <= 0L)) {
                        formError = "Enter a resident stay ID so the tenant can see this entry in their own ledger."; return@TextButton
                    }
                    if (category == "charges" && !Regex("^\\d{4}-(0[1-9]|1[0-2])$").matches(recordBillingPeriodText.trim())) {
                        formError = "Enter the billing period as YYYY-MM, for example 2026-11."; return@TextButton
                    }
                    if (category == "charges" && dateText.isBlank()) { formError = "Enter the rent due date."; return@TextButton }
                    if (savingServerRecord) return@TextButton
                    savingServerRecord = true
                    formError = ""
                    val entryType = when (category) {
                        "payments" -> "payment"; "charges" -> "rent_charge"; "deposits", "expenses", "food" -> financialEntryType
                        else -> financialEntryType
                    }
                    val recordDescription = listOf(titleText.trim(), detailsText.trim()).filter { it.isNotBlank() }.joinToString(" — ")
                    remoteScope.launch {
                        try {
                            val created = withContext(Dispatchers.IO) {
                                StayApiClient.createManualLedgerEntry(
                                    apiPreferences.apiBaseUrl, sessionToken!!, propertyId, residentStayId,
                                    entryType, amount, if (category == "charges") recordBillingPeriodText.trim() else "",
                                    dateText.trim(), paymentReferenceText.trim(), recordDescription
                                )
                            }
                            val entryId = created.optLong("id")
                            require(entryId > 0L) { "The server did not return a valid ledger entry ID." }
                            val serverRecord = candidateRecordForLedger(
                                id = "server-ledger-$entryId", category = category, entryType = entryType,
                                title = titleText.trim(), description = recordDescription,
                                amount = amount, propertyId = propertyId, residentStayId = residentStayId,
                                billingPeriod = if (category == "charges") recordBillingPeriodText.trim() else "",
                                dueDate = dateText.trim(), reference = paymentReferenceText.trim(),
                                status = created.optString("status", "recorded"), updatedAt = System.currentTimeMillis()
                            )
                            val latestRows = try {
                                withContext(Dispatchers.IO) { StayApiClient.ownerLedger(apiPreferences.apiBaseUrl, sessionToken) }
                            } catch (_: Exception) {
                                null
                            }
                            if (latestRows != null) {
                                StayRecordStore.replaceServerRecords(context, category, mapLedgerRows(latestRows), preserveLocal = false)
                            } else {
                                val existingServerRows = StayRecordStore.load(context, category).filter { it.source == "Server" && it.id != serverRecord.id }
                                StayRecordStore.replaceServerRecords(context, category, existingServerRows + serverRecord, preserveLocal = false)
                            }
                            refresh()
                            showEditor = false
                            remoteSyncMessage = "Saved to server. This is a manual record; StayEasy did not collect a payment."
                            StayRecordStore.notify(context, "$title saved", serverRecord.title, serverRecord.property)
                        } catch (e: Exception) {
                            formError = "Server save failed: ${e.message ?: "check the API session and property/stay IDs"}"
                        } finally {
                            savingServerRecord = false
                        }
                    }
                    return@TextButton
                }
                val existing = editingId?.let { id -> records.firstOrNull { it.id == id } }
                val nextStatus = existing?.status ?: StayRecordConfig.defaultStatus(category)
                val candidate = StayRecord(id = editingId ?: java.util.UUID.randomUUID().toString(), category = category, title = titleText.trim(), details = detailsText.trim(), status = nextStatus,
                    amount = amount, person = personText.trim(), property = propertyText.trim(), unit = unitText.trim(), date = dateText.trim(), priority = priority,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis(), updatedAt = System.currentTimeMillis(), source = "Local demo")
                if (category == "reservations" && candidate.unit.isNotBlank() && candidate.status in listOf("Pending", "Confirmed", "Checked in")) {
                    val conflict = records.firstOrNull { it.id != candidate.id && it.unit.equals(candidate.unit, true) && it.property.equals(candidate.property, true) && it.status in listOf("Pending", "Confirmed", "Checked in") }
                    if (conflict != null) { formError = "${candidate.unit} already has an active reservation (${conflict.title}). Choose another unit or resolve that reservation."; return@TextButton }
                }
                if (editingId == null) StayRecordStore.add(context, candidate) else StayRecordStore.update(context, category, candidate)
                refresh(); showEditor = false
                StayRecordStore.notify(context, "$title saved", candidate.title, candidate.property)
            }) { Text(if (savingServerRecord) "Saving…" else if (serverFinanceMode) "Save to server" else "Save record") } },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } },
            containerColor = SE.Surface
        )
    }
    if (showBillingDialog) {
        AlertDialog(
            onDismissRequest = { if (!generatingBilling) showBillingDialog = false },
            title = { Text("Generate monthly rent", color = SE.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Create rent charges for eligible stays at one property. Existing charges for the same stay and month are skipped. No payment is collected.", color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                    OutlinedTextField(
                        billingPropertyIdText,
                        { billingPropertyIdText = it.filter(Char::isDigit).take(12) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Server property ID") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        billingPeriodText,
                        { billingPeriodText = it.filter { ch -> ch.isDigit() || ch == '-' }.take(7) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Billing period (YYYY-MM)") },
                        placeholder = { Text("2026-11") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        billingDueDateText,
                        { billingDueDateText = it.take(30) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Rent due date") },
                        placeholder = { Text("2026-11-05") },
                        singleLine = true
                    )
                    if (billingError.isNotBlank()) Text(billingError, color = SE.Red, fontSize = 11.sp)
                }
            },
            confirmButton = {
                TextButton(enabled = !generatingBilling, onClick = {
                    val propertyId = billingPropertyIdText.toLongOrNull()
                    if (propertyId == null || propertyId <= 0L) {
                        billingError = "Enter a valid server property ID."
                        return@TextButton
                    }
                    if (!Regex("^\\d{4}-(0[1-9]|1[0-2])$").matches(billingPeriodText.trim())) {
                        billingError = "Enter the billing period as YYYY-MM, for example 2026-11."
                        return@TextButton
                    }
                    if (billingDueDateText.isBlank()) {
                        billingError = "Enter the rent due date."
                        return@TextButton
                    }
                    if (generatingBilling) return@TextButton
                    generatingBilling = true
                    billingError = ""
                    remoteScope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                StayApiClient.generateRentCharges(
                                    apiPreferences.apiBaseUrl, sessionToken!!, propertyId,
                                    billingPeriodText.trim(), billingDueDateText.trim()
                                )
                            }
                            val rows = withContext(Dispatchers.IO) {
                                StayApiClient.ownerLedger(apiPreferences.apiBaseUrl, sessionToken!!)
                            }
                            StayRecordStore.replaceServerRecords(context, category, mapLedgerRows(rows), preserveLocal = false)
                            refresh()
                            remoteSyncMessage = "Billing complete: ${result.optInt("created_count")} created, ${result.optInt("skipped_existing")} existing skipped."
                            showBillingDialog = false
                        } catch (e: Exception) {
                            billingError = "Billing failed: ${e.message ?: "check the API session, property ID and dates"}"
                        } finally {
                            generatingBilling = false
                        }
                    }
                }) { Text(if (generatingBilling) "Generating…" else "Generate charges") }
            },
            dismissButton = { TextButton(enabled = !generatingBilling, onClick = { showBillingDialog = false }) { Text("Cancel") } },
            containerColor = SE.Surface
        )
    }

    if (showDeleteId != null) {
        AlertDialog(onDismissRequest = { showDeleteId = null }, title = { Text("Remove this record?", color = SE.Ink) }, text = { Text("This only removes the local prototype record from this device.", color = SE.Muted) },
            confirmButton = { TextButton(onClick = { showDeleteId?.let { StayRecordStore.remove(context, category, it) }; refresh(); showDeleteId = null }) { Text("Remove", color = SE.Red) } },
            dismissButton = { TextButton(onClick = { showDeleteId = null }) { Text("Cancel") } }, containerColor = SE.Surface)
    }
    val selectedServerStatusRecord = showServerStatusId?.let { id -> records.firstOrNull { it.id == id && it.source == "Server" } }
    if (selectedServerStatusRecord != null) {
        val options = serverStatusOptions(category, selectedServerStatusRecord.status)
        AlertDialog(
            onDismissRequest = { if (!updatingServerStatus) showServerStatusId = null },
            title = {
                Text(
                    when (category) {
                        "stays" -> "Manage resident stay"
                        "move_out_requests" -> "Review move-out request"
                        else -> "Update server status"
                    },
                    color = SE.Ink,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(selectedServerStatusRecord.title, color = SE.Text, fontWeight = FontWeight.SemiBold)
                    Text(
                        when (category) {
                            "stays" -> "Record notice or move-out dates. The server validates the transition and releases the room/bed only after confirmation."
                            "move_out_requests" -> "The request and requested date come from the server. Approval records notice; the unit stays occupied until the owner confirms actual move-out."
                            else -> "Choose an allowed action. StayEasy will save the change only after the server confirms it."
                        },
                        color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp
                    )
                    if (category == "stays") {
                        OutlinedTextField(
                            value = serverStayDateText,
                            onValueChange = { serverStayDateText = it.trim().take(10); serverStatusError = "" },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Notice / move-out date (YYYY-MM-DD)") },
                            placeholder = { Text("2026-11-30") },
                            singleLine = true
                        )
                        Text("Required for “Notice given” and “Completed”. Leave it unchanged for “Active”.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                    if (category == "move_out_requests") {
                        OutlinedTextField(
                            value = serverOwnerNoteText,
                            onValueChange = { serverOwnerNoteText = it.take(1000); serverStatusError = "" },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Response note to tenant (optional)") },
                            placeholder = { Text("Confirm key return, final inspection or next steps") },
                            minLines = 2,
                            maxLines = 4
                        )
                    }
                    if (serverStatusError.isNotBlank()) Text(serverStatusError, color = SE.Red, fontSize = 11.sp, lineHeight = 15.sp)
                    options.forEach { target ->
                        val actionLabel = when (category) {
                            "stays" -> when (target) {
                                "Notice given" -> "Give notice"
                                "Completed" -> "Complete stay"
                                "Active" -> "Reactivate stay"
                                else -> target
                            }
                            "move_out_requests" -> when (target) {
                                "Approve" -> "Approve request"
                                "Reject" -> "Reject request"
                                else -> target
                            }
                            else -> target
                        }
                        Button(
                            onClick = { updateServerStatus(selectedServerStatusRecord, target) },
                            enabled = !updatingServerStatus,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)
                        ) { Text(if (updatingServerStatus) "Updating…" else actionLabel) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { if (!updatingServerStatus) showServerStatusId = null }) { Text("Cancel") } },
            containerColor = SE.Surface
        )
    }
}

@Composable
internal fun ComparePropertiesScreen(listings: List<PgListing>, initialIds: List<Long>, onBack: () -> Unit) {
    var selected by remember { mutableStateOf((initialIds.ifEmpty { listings.take(3).map { it.id } }).distinct().take(3)) }
    val candidates = listings.filter { selected.contains(it.id) }
    Column(Modifier.fillMaxSize().background(SE.Background).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Compare stays", color = SE.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("Select up to three PGs", color = SE.Muted, fontSize = 12.sp) }; OutlinedButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null); Text("Back") } }
        Spacer(Modifier.height(12.dp)); Text("SHORTLIST", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .9.sp)
        LazyColumn(modifier = Modifier.heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listings, key = { it.id }) { p ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SE.Surface).clickable { selected = if (selected.contains(p.id)) selected - p.id else if (selected.size < 3) selected + p.id else selected }.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = selected.contains(p.id), onCheckedChange = { checked -> selected = if (checked && selected.size < 3) selected + p.id else if (!checked) selected - p.id else selected })
                    Column(Modifier.weight(1f)) { Text(p.name, color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(p.area, color = SE.Muted, fontSize = 10.sp) }
                    Text("${moneyText(p.monthlyRent)}/mo", color = SE.Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp)); Text("SIDE-BY-SIDE COMPARISON (${candidates.size}/3)", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
            items(candidates, key = { it.id }) { p ->
                Card(shape = SE.cardShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
                    Column(Modifier.padding(13.dp)) {
                        Text(p.name, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(p.area, color = SE.Muted, fontSize = 11.sp)
                        Divider(Modifier.padding(vertical = 7.dp), color = SE.Border)
                        CompareLine("Monthly rent", moneyText(p.monthlyRent)); CompareLine("Security deposit", moneyText(p.deposit)); CompareLine("Food", if (p.foodMonthly > 0) "${moneyText(p.foodMonthly)}/month" else "Not included / not specified")
                        CompareLine("Indicative move-in cost", moneyText(p.monthlyRent + p.deposit + p.foodMonthly)); CompareLine("Sharing", p.sharing); CompareLine("Eligibility", p.allowedFor + if (p.couplesAllowed) " · Couples welcome" else "")
                        CompareLine("Availability", "${p.availableBeds} of ${p.totalBeds} demo beds"); CompareLine("Amenities", p.amenities.joinToString(", "))
                        CompareLine("Trust", if (p.isDemo) "Sample listing · not verified" else "Owner-created local listing")
                    }
                }
            }
        }
        Text("Indicative totals exclude any unlisted compulsory fees/utilities. Sample listings are not verified offers.", color = SE.Muted, fontSize = 9.sp, lineHeight = 13.sp)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
internal fun CompareLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = SE.Muted, fontSize = 10.sp, modifier = Modifier.weight(.9f))
        Text(value, color = SE.Text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.1f))
    }
}

@Composable
internal fun ThemeSettingsScreen(
    initial: StayPreferences,
    onChanged: (StayPreferences) -> Unit,
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf(initial.mode) }
    var theme by remember { mutableStateOf(initial.theme) }
    var language by remember { mutableStateOf(initial.language) }
    var largeText by remember { mutableStateOf(initial.largeText) }
    var enquiryAlerts by remember { mutableStateOf(initial.enquiryAlerts) }
    var rentAlerts by remember { mutableStateOf(initial.rentAlerts) }
    var maintenanceAlerts by remember { mutableStateOf(initial.maintenanceAlerts) }
    var availabilityAlerts by remember { mutableStateOf(initial.availabilityAlerts) }
    val themes = listOf("Midnight Cobalt", "Ocean", "Violet", "Graphite", "Porcelain", "Copper")
    fun update() = onChanged(initial.copy(mode = mode, theme = theme, language = language, largeText = largeText, enquiryAlerts = enquiryAlerts, rentAlerts = rentAlerts, maintenanceAlerts = maintenanceAlerts, availabilityAlerts = availabilityAlerts))
    Column(Modifier.fillMaxSize().background(SE.Background).verticalScroll(rememberScrollState()).padding(17.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Appearance & settings", color = SE.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Theme changes apply across the app", color = SE.Muted, fontSize = 11.sp) }; OutlinedButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null); Text("Back") } }
        Spacer(Modifier.height(18.dp)); Text("APPEARANCE MODE", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("System", "Light", "Dark").forEach { option -> FilterChip(selected = mode == option, onClick = { mode = option; update() }, label = { Text(option) }) } }
        Spacer(Modifier.height(17.dp)); Text("DESIGN SYSTEMS", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text("Each theme changes the visual language: card geometry, typography, graphic motifs and surface treatment—not just the accent colour.", color = SE.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        themes.forEach { value ->
            val pair = themeSwatches(value)
            Row(
                Modifier.fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(SE.cardShape(16.dp))
                    .background(SE.Surface)
                    .border(1.dp, if (theme == value) SE.Blue else SE.Border, SE.cardShape(16.dp))
                    .clickable { theme = value; update() }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThemeStylePreview(value, pair)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(themeStyleName(value), color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(value, color = SE.Blue, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                    Text(themeDescription(value), color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                }
                RadioButton(selected = theme == value, onClick = { theme = value; update() })
            }
        }
        Spacer(Modifier.height(18.dp)); Text("ACCESSIBILITY & LANGUAGE", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        SettingSwitch("Larger text preference", "Scales app text by 12% for easier reading.", largeText) { largeText = it; update() }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Language", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("English now; strings are structured for localization later", color = SE.Muted, fontSize = 10.sp) }; Row { listOf("English", "Hindi", "Marathi").forEach { FilterChip(selected = language == it, onClick = { language = it; update() }, label = { Text(it, fontSize = 9.sp) }) } } }
        Spacer(Modifier.height(13.dp)); Text("NOTIFICATION PREFERENCES", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        SettingSwitch("Enquiry and visit alerts", "Owner replies, requests and appointment changes", enquiryAlerts) { enquiryAlerts = it; update() }
        SettingSwitch("Rent reminders", "Upcoming charges and payment status", rentAlerts) { rentAlerts = it; update() }
        SettingSwitch("Maintenance updates", "Ticket status and resident notices", maintenanceAlerts) { maintenanceAlerts = it; update() }
        SettingSwitch("Availability alerts", "Matching properties and saved-search updates", availabilityAlerts) { availabilityAlerts = it; update() }
        Spacer(Modifier.height(12.dp)); Card(shape = SE.cardShape(14.dp), colors = CardDefaults.cardColors(containerColor = SE.BlueSoft)) { Text("Your preferences are saved locally on this device. Production notification preferences must sync to your account and be respected by the push service.", color = SE.Text, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(13.dp)) }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = SE.Ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp) }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

private fun cycleStatus(category: String, current: String): String {
    val statuses = StayRecordConfig.statuses(category)
    val index = statuses.indexOf(current)
    return statuses[(index + 1).let { if (it < 0 || it >= statuses.size) 0 else it }]
}

private fun serverStatusOptions(category: String, current: String): List<String> = when (category) {
    "enquiries" -> when (current.trim().lowercase(Locale.ROOT)) {
        "new" -> listOf("Contacted", "Visit scheduled", "Closed")
        "contacted" -> listOf("Visit scheduled", "Converted", "Closed")
        "visit scheduled" -> listOf("Contacted", "Converted", "Closed")
        "converted" -> listOf("Closed")
        else -> emptyList()
    }
    "reservations" -> when (current.trim().lowercase(Locale.ROOT)) {
        "pending" -> listOf("Confirmed", "Cancelled", "Expired")
        "confirmed" -> listOf("Checked in", "Cancelled", "Expired")
        else -> emptyList()
    }
    "visits" -> when (current.trim().lowercase(Locale.ROOT)) {
        "requested" -> listOf("Confirmed", "Cancelled")
        "confirmed" -> listOf("Completed", "Cancelled", "No-show")
        else -> emptyList()
    }
    "issues" -> when (current.trim().lowercase(Locale.ROOT)) {
        "open" -> listOf("In progress", "Escalated")
        "in progress" -> listOf("Waiting for resident", "Resolved", "Escalated")
        "waiting for resident" -> listOf("In progress", "Resolved", "Escalated")
        "escalated" -> listOf("In progress", "Resolved")
        "reopened" -> listOf("In progress", "Resolved", "Escalated")
        else -> emptyList()
    }
    "stays" -> when (current.trim().lowercase(Locale.ROOT).replace('_', ' ')) {
        "active" -> listOf("Notice given", "Completed")
        "notice given" -> listOf("Active", "Completed")
        else -> emptyList()
    }
    "move_out_requests" -> if (current.trim().equals("pending", ignoreCase = true)) {
        listOf("Approve", "Reject")
    } else emptyList()
    else -> emptyList()
}

@Composable
private fun SmallInfoPill(text: String, bg: Color, fg: Color) {
    Box(Modifier.clip(CircleShape).background(bg).padding(horizontal = 9.dp, vertical = 5.dp)) { Text(text, color = fg, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

private fun statusBg(status: String) = when (status.lowercase(Locale.ROOT)) {
    "resolved", "paid", "reconciled", "completed", "active", "published", "verified", "refunded", "checked out", "done" -> Color(0xFFE5ECFA)
    "urgent", "overdue", "rejected", "escalated", "disputed", "maintenance", "suspended" -> Color(0xFFFFE8EC)
    "pending", "new", "requested", "due", "open", "needs review", "unread", "draft" -> Color(0xFFFFF0E7)
    else -> SE.BlueSoft
}
private fun statusFg(status: String) = when (status.lowercase(Locale.ROOT)) {
    "resolved", "paid", "reconciled", "completed", "active", "published", "verified", "refunded", "checked out", "done" -> SE.Blue
    "urgent", "overdue", "rejected", "escalated", "disputed", "maintenance", "suspended" -> SE.Red
    "pending", "new", "requested", "due", "open", "needs review", "unread", "draft" -> SE.Orange
    else -> SE.Blue
}
private fun moneyText(amount: Int): String = "₹" + String.format(Locale.getDefault(), "%,d", amount)
private fun themeStyleName(theme: String) = when (theme) {
    "Midnight Cobalt" -> "Bento Night"
    "Ocean" -> "Coastal Drift"
    "Violet" -> "Studio Editorial"
    "Graphite" -> "Industrial Grid"
    "Porcelain" -> "Gallery Minimal"
    else -> "Courtyard Heritage"
}

private fun themeDescription(theme: String) = when (theme) {
    "Midnight Cobalt" -> "Layered dashboard tiles, luminous accents and high-contrast hierarchy."
    "Ocean" -> "Airy spacing, flowing circular motifs and softer oversized surfaces."
    "Violet" -> "Editorial headline scale, asymmetric cards and strong section dividers."
    "Graphite" -> "Compact data-dense layouts, square geometry and a technical grid."
    "Porcelain" -> "Quiet typography, thin outlines, generous whitespace and gallery-like panels."
    else -> "Warm architectural lines, framed sections and rounded courtyard-inspired details."
}
@Composable
private fun ThemeStylePreview(theme: String, swatches: List<Color>) {
    val base = swatches[0]
    val pale = swatches[2]
    Box(
        Modifier.size(width = 66.dp, height = 58.dp)
            .clip(
                when (theme) {
                    "Graphite" -> RoundedCornerShape(4.dp)
                    "Porcelain" -> RoundedCornerShape(2.dp)
                    "Violet" -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomEnd = 4.dp, bottomStart = 4.dp)
                    "Copper" -> RoundedCornerShape(topStart = 17.dp, topEnd = 4.dp, bottomEnd = 17.dp, bottomStart = 4.dp)
                    "Ocean" -> RoundedCornerShape(21.dp)
                    else -> RoundedCornerShape(12.dp)
                }
            )
            .background(pale)
            .padding(6.dp)
    ) {
        when (theme) {
            "Ocean" -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.fillMaxWidth(.62f).height(4.dp).clip(CircleShape).background(base))
                    Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp)).background(base.copy(alpha = .15f)))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(3) { Box(Modifier.weight(1f).height(5.dp).clip(CircleShape).background(base.copy(alpha = .55f))) }
                    }
                }
            }
            "Violet" -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.fillMaxWidth(.9f).height(5.dp).background(base))
                    Box(Modifier.fillMaxWidth(.56f).height(3.dp).background(swatches[1]))
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(base.copy(alpha = .15f)))
                        Box(Modifier.weight(1f).fillMaxHeight().background(swatches[1].copy(alpha = .35f)))
                    }
                }
            }
            "Graphite" -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(4) { i ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(Modifier.weight(1f).height(6.dp).background(if (i == 0) base else base.copy(alpha = .18f)))
                            Box(Modifier.weight(1.5f).height(6.dp).background(base.copy(alpha = .10f)))
                            Box(Modifier.weight(.6f).height(6.dp).background(swatches[1].copy(alpha = .5f)))
                        }
                    }
                }
            }
            "Porcelain" -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(38.dp).border(1.dp, base.copy(alpha = .65f), CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(22.dp).border(1.dp, swatches[1], CircleShape))
                    }
                }
            }
            "Copper" -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 2.dp)).background(base))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topStart = 7.dp, bottomEnd = 7.dp)).background(base.copy(alpha = .18f)))
                        Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(topEnd = 7.dp, bottomStart = 7.dp)).background(swatches[1].copy(alpha = .5f)))
                    }
                }
            }
            else -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.weight(1f).height(17.dp).clip(RoundedCornerShape(4.dp)).background(base))
                        Box(Modifier.weight(1f).height(17.dp).clip(RoundedCornerShape(4.dp)).background(swatches[1]))
                    }
                    Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(4.dp)).background(base.copy(alpha = .12f)))
                }
            }
        }
    }
}

private fun themeSwatches(theme: String) = when (theme) {
    "Ocean" -> listOf(Color(0xFF087F9E), Color(0xFF64B6C8), Color(0xFFEAF5F8))
    "Violet" -> listOf(Color(0xFF7556D9), Color(0xFFB09BEF), Color(0xFFF2EEFC))
    "Graphite" -> listOf(Color(0xFF536174), Color(0xFF9CA9B8), Color(0xFFEDF0F4))
    "Porcelain" -> listOf(Color(0xFF315BE8), Color(0xFFF1845B), Color(0xFFF6F7FA))
    "Copper" -> listOf(Color(0xFFA35B40), Color(0xFFD3AA78), Color(0xFFF7F0E9))
    else -> listOf(Color(0xFF5478FF), Color(0xFFFF9C73), Color(0xFF141C2B))
}

private fun summarizeIntegrationReadiness(payload: JSONObject?): String {
    if (payload == null) {
        return "Use an owner/admin token and tap Test connection to inspect provider setup. This screen never displays feed URLs, access tokens or Razorpay secrets."
    }
    val feed = payload.optJSONObject("property_feed") ?: JSONObject()
    val batch = feed.optJSONObject("last_successful_batch") ?: JSONObject()
    val configured = feed.optBoolean("configured", false)
    val syncStatus = feed.optString("sync_status", "not_run")
    val feedState = when {
        !configured -> "Not configured"
        syncStatus == "running" -> "Sync running"
        syncStatus == "success" -> "Last sync successful"
        syncStatus == "error" -> "Last sync failed"
        else -> "Configured · awaiting first sync"
    }
    val lastAttempt = if (feed.isNull("last_attempt_at")) "Never" else feed.optString("last_attempt_at")
    val lastSuccess = if (feed.isNull("last_success_at")) "Never" else feed.optString("last_success_at")
    val lastError = feed.optString("last_error_category").takeIf { it.isNotBlank() }
    val payment = payload.optJSONObject("razorpay") ?: JSONObject()
    val checkoutState = payment.optString("checkout_state", "not_configured")
        .replace('_', ' ')
    val webhookState = if (payment.optBoolean("webhook_secret_configured", false)) "Configured" else "Not configured"
    val mode = payment.optString("mode_hint", "unknown")
    return buildString {
        append("PROPERTY FEED\n")
        append("Provider: ").append(feed.optString("source", "partner-feed")).append('\n')
        append("Configuration: ").append(if (configured) "Present" else "Missing").append('\n')
        append("Sync status: ").append(feedState).append('\n')
        append("Last attempt: ").append(lastAttempt).append('\n')
        append("Last successful sync: ").append(lastSuccess).append('\n')
        append("Last successful batch: ").append(batch.optInt("received")).append(" received · ")
            .append(batch.optInt("created")).append(" created · ")
            .append(batch.optInt("updated")).append(" updated · ")
            .append(batch.optInt("skipped")).append(" skipped\n")
        append("Imported feed listings: ").append(feed.optInt("imported_listing_count")).append('\n')
        append("Polling interval: ").append(feed.optInt("poll_interval_seconds", 300)).append(" seconds")
        if (lastError != null) append("\nLast error category: ").append(lastError)
        append("\n\nRAZORPAY PAYMENTS\n")
        append("Checkout credentials: ").append(checkoutState).append('\n')
        append("Webhook secret: ").append(webhookState).append('\n')
        append("Mode hint: ").append(mode).append('\n')
        append("Configuration presence is not a live provider test. Checkout and signed webhooks still need staging acceptance.")
    }
}

@Composable
internal fun ApiConnectionScreen(
    initialBaseUrl: String,
    onBaseUrlChanged: (String) -> Unit,
    onListingsLoaded: (List<PgListing>) -> Unit,
    onBack: () -> Unit
) {
    var baseUrl by remember { mutableStateOf(initialBaseUrl.ifBlank { "http://10.0.2.2:8000" }) }
    var token by remember { mutableStateOf("") }
    var tokenSubject by remember { mutableStateOf("stayeasy-demo-owner") }
    var tokenRole by remember { mutableStateOf("owner") }
    var devPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Not connected yet") }
    var healthText by remember { mutableStateOf("") }
    var dashboardText by remember { mutableStateOf("") }
    var integrationText by remember { mutableStateOf("Use an owner/admin token and tap Test connection to inspect feed and payment configuration.") }
    val scope = rememberCoroutineScope()
    LaunchedEffect(baseUrl) { onBaseUrlChanged(baseUrl.trimEnd('/')) }
    fun runCheck(loadListings: Boolean) {
        if (busy) return
        if (baseUrl.isBlank()) { status = "Enter your deployed HTTPS API URL first."; return }
        busy = true; status = "Connecting to StayEasy API…"
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val health = StayApiClient.health(baseUrl)
                    if (token.isNotBlank()) StayApiClient.setSessionToken(token, baseUrl)
                    val rows = if (loadListings) StayApiClient.publishedProperties(baseUrl) else emptyList()
                    val dashboard = if (token.isNotBlank()) runCatching { StayApiClient.ownerDashboard(baseUrl, token) }.getOrNull() else null
                    val integrations = if (token.isNotBlank()) runCatching {
                        StayApiClient.integrationStatus(baseUrl, token)
                    }.getOrNull() else null
                    Triple(health, rows, dashboard to integrations)
                }
                healthText = "${result.first.optString("service", "API")} · ${result.first.optString("status", "unknown")} · ${result.first.optString("environment", "")}".trim()
                dashboardText = result.third.first?.toString(2) ?: if (token.isNotBlank()) "Owner dashboard not available for this token/role." else "No bearer token provided; public listing search does not require one."
                integrationText = summarizeIntegrationReadiness(result.third.second)
                if (loadListings) onListingsLoaded(result.second)
                status = if (loadListings) "Connected. Loaded ${result.second.size} published server listing(s)." else "API health check succeeded."
            } catch (e: Exception) {
                status = "Connection failed: ${e.message ?: "Unknown network error"}"
                integrationText = "Could not retrieve integration status. Check the API URL and use a token with owner/admin access."
            } finally { busy = false }
        }
    }
    Column(Modifier.fillMaxSize().background(SE.Background).verticalScroll(rememberScrollState()).padding(17.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("API connection & sync", color = SE.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Connect discovery to shared server data", color = SE.Muted, fontSize = 11.sp) }
            OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp)) { Icon(Icons.Filled.ArrowBack, null); Text("Back") }
        }
        Spacer(Modifier.height(15.dp))
        Card(colors = CardDefaults.cardColors(containerColor = SE.Surface), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(15.dp)) {
                Text("API base URL", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = baseUrl, onValueChange = { nextBaseUrl ->
                    val originChanged = StayApiClient.apiOrigin(baseUrl) != StayApiClient.apiOrigin(nextBaseUrl)
                    if (originChanged && (token.isNotBlank() || StayApiClient.currentSessionToken(baseUrl) != null)) {
                        token = ""
                        StayApiClient.clearSessionToken()
                        status = "API origin changed. Enter a token issued for the new server."
                    }
                    baseUrl = nextBaseUrl
                    onBaseUrlChanged(nextBaseUrl)
                }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("https://api.your-domain.com") }, shape = RoundedCornerShape(12.dp))
                Text("Emulator default: http://10.0.2.2:8000. For a physical phone use a trusted HTTPS tunnel. The test API uses seeded DEMO DATA; real external listings need an authorised feed.", color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp)
                Spacer(Modifier.height(12.dp)); Text("API session token", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                OutlinedTextField(value = token, onValueChange = { token = it; if (it.isBlank()) StayApiClient.clearSessionToken() }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Paste a short-lived bearer token") }, shape = RoundedCornerShape(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = {
                        if (StayApiClient.apiOrigin(baseUrl) == null) status = "Enter a valid API URL before enabling the token."
                        else {
                            StayApiClient.setSessionToken(token, baseUrl)
                            status = "Token enabled for this API server in this app session."
                        }
                    }, enabled = !busy && token.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)) { Text("Use token") }
                    Text("Memory only; not saved to preferences.", color = SE.Muted, fontSize = 10.sp)
                }
                Spacer(Modifier.height(13.dp)); Text("Development token helper", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text("For a development API only. This endpoint should be disabled in production.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                OutlinedTextField(value = tokenSubject, onValueChange = { tokenSubject = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Session subject / user ID") }, shape = RoundedCornerShape(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("customer", "owner", "manager", "staff", "admin").forEach { role -> FilterChip(selected = tokenRole == role, onClick = { tokenRole = role; if (role == "customer") tokenSubject = "stayeasy-demo-tenant" else if (role == "owner") tokenSubject = "stayeasy-demo-owner" }, label = { Text(role, fontSize = 10.sp) }) }
                }
                OutlinedTextField(value = devPassword, onValueChange = { devPassword = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Development token password") }, shape = RoundedCornerShape(12.dp))
                Button(onClick = {
                    if (busy) return@Button
                    if (baseUrl.isBlank() || tokenSubject.trim().length < 2 || devPassword.isBlank()) { status = "Enter the API URL, subject, and configured development password."; return@Button }
                    busy = true; status = "Requesting development session token…"
                    scope.launch {
                        try {
                            token = withContext(Dispatchers.IO) { StayApiClient.requestDevelopmentToken(baseUrl, tokenSubject, tokenRole, devPassword) }
                            StayApiClient.setSessionToken(token, baseUrl)
                            status = "Development token issued and enabled for role '$tokenRole'. It is held in memory only."
                        } catch (e: Exception) { status = "Token request failed: ${e.message ?: "Unknown error"}" }
                        finally { busy = false }
                    }
                }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)) { Text(if (busy) "Working…" else "Create development token") }
                Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { runCheck(false) }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = SE.Blue), modifier = Modifier.weight(1f)) { Text(if (busy) "Connecting…" else "Test connection") }
                    OutlinedButton(onClick = { runCheck(true) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Refresh listings") }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = SE.BlueSoft), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(13.dp)) {
                Text("Connection status", color = SE.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp)); Text(status, color = SE.Text, fontSize = 11.sp, lineHeight = 16.sp)
                if (healthText.isNotBlank()) { Spacer(Modifier.height(7.dp)); Text(healthText, color = SE.Muted, fontSize = 10.sp) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = SE.Surface), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(13.dp)) {
                Text("Integration readiness", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(Modifier.height(7.dp))
                Text(integrationText, color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = SE.Surface), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(13.dp)) {
                Text("Owner dashboard response", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(7.dp)); Text(dashboardText, color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("What this sync currently does", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("Refresh loads published server listings into the discovery session. A session token enables remote owner listing creation and customer enquiry, visit and reservation requests. Finance, resident and maintenance records remain local until their individual workflows are connected.", color = SE.Muted, fontSize = 11.sp, lineHeight = 17.sp)
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
internal fun RequestBookingScreen(
    listing: PgListing,
    onBack: () -> Unit,
    onSubmit: (roomType: String, moveInDate: String, unit: String, note: String) -> Unit
) {
    var roomType by remember(listing.id) { mutableStateOf(listing.sharing) }
    var moveInDate by remember(listing.id) { mutableStateOf(java.time.LocalDate.now().plusDays(7).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US))) }
    var unit by remember(listing.id) { mutableStateOf("") }
    var note by remember(listing.id) { mutableStateOf("Please confirm the selected room type, total move-in charges and cancellation terms.") }
    var agreed by remember(listing.id) { mutableStateOf(false) }
    val availableServerUnits = listing.serverUnits.filter { it.status.equals("available", ignoreCase = true) }
    fun roomTypeForUnit(slot: RemoteUnit): String = when {
        slot.rentableType.equals("room", ignoreCase = true) -> "Private room"
        slot.sharingCapacity <= 1 -> "Individual bed"
        else -> "${slot.sharingCapacity} sharing"
    }
    val serverRoomTypes = availableServerUnits.map(::roomTypeForUnit).distinct()
    val roomTypeOptions = if (listing.serverPropertyId != null) serverRoomTypes else listOf(listing.sharing, "Private room", "Individual bed", "Flexible").distinct()
    val selectableServerUnits = availableServerUnits.filter { roomTypeForUnit(it) == roomType }
    LaunchedEffect(listing.id, roomTypeOptions) {
        if (listing.serverPropertyId != null && roomType !in roomTypeOptions) {
            roomType = roomTypeOptions.firstOrNull() ?: listing.sharing
            unit = ""
        }
    }
    Column(Modifier.fillMaxSize().background(SE.Background).verticalScroll(rememberScrollState()).padding(17.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Request a reservation", color = SE.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("The owner must confirm the exact unit and terms", color = SE.Muted, fontSize = 11.sp) }; OutlinedButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null); Text("Back") } }
        Spacer(Modifier.height(14.dp))
        Card(shape = SE.cardShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
            Column(Modifier.padding(14.dp)) {
                Text(listing.name, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(listing.area, color = SE.Muted, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp)); CompareLine("Rent per month", moneyText(listing.monthlyRent)); CompareLine("Security deposit", moneyText(listing.deposit))
                if (listing.foodMonthly > 0) CompareLine("Food", moneyText(listing.foodMonthly))
                if (listing.utilityMonthlyEstimate > 0) CompareLine("Utility estimate", moneyText(listing.utilityMonthlyEstimate))
                Divider(Modifier.padding(vertical = 7.dp), color = SE.Border)
                CompareLine("Indicative first month + deposit", moneyText(listing.monthlyRent + listing.deposit + listing.foodMonthly + listing.utilityMonthlyEstimate))
                Text("This estimate includes only amounts recorded in the listing. Confirm extra fees, actual utility policy and refund terms.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
        Spacer(Modifier.height(14.dp)); Text("ROOM / BED OPTION", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
        if (roomTypeOptions.isEmpty()) {
            Text("No room or bed types are currently available. Refresh availability before requesting a reservation.", color = SE.Orange, fontSize = 11.sp, lineHeight = 15.sp)
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                roomTypeOptions.forEach { option ->
                    FilterChip(selected = roomType == option, onClick = {
                        if (roomType != option) {
                            roomType = option
                            unit = ""
                        }
                    }, label = { Text(option, fontSize = 10.sp) })
                }
            }
        }
        Spacer(Modifier.height(12.dp)); Text("MOVE-IN DATE", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
        OutlinedTextField(moveInDate, { moveInDate = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("e.g. 15 Nov 2026") }, shape = RoundedCornerShape(12.dp))
        Spacer(Modifier.height(12.dp)); Text(if (listing.serverPropertyId != null) "SELECT AN AVAILABLE ROOM / BED" else "SPECIFIC ROOM / BED (OPTIONAL)", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
        if (listing.serverPropertyId != null) {
            if (selectableServerUnits.isEmpty()) {
                Text("No available slot matches this room type. Choose another available type or refresh availability.", color = SE.Orange, fontSize = 11.sp, lineHeight = 15.sp)
            } else {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    selectableServerUnits.take(20).forEach { slot ->
                        val label = "${slot.roomCode} · ${slot.unitCode}"
                        FilterChip(selected = unit == label, onClick = { unit = label }, label = { Text("${slot.unitCode} · ₹${slot.monthlyRent}", fontSize = 10.sp) })
                    }
                }
                Text("Only slots matching the selected room type are shown. The backend checks availability atomically when the request is submitted.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        } else {
            OutlinedTextField(unit, { unit = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("e.g. Room 204 · Bed B") }, shape = RoundedCornerShape(12.dp))
        }
        Spacer(Modifier.height(12.dp)); Text("NOTE TO OWNER", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp)
        OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp), shape = RoundedCornerShape(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = agreed, onCheckedChange = { agreed = it }); Text("I understand this is a request, not a confirmed booking or payment.", color = SE.Text, fontSize = 11.sp, lineHeight = 15.sp) }
        Spacer(Modifier.height(8.dp)); Button(onClick = { if (moveInDate.isNotBlank() && agreed) onSubmit(roomType, moveInDate, unit.trim(), note.trim()) }, enabled = moveInDate.isNotBlank() && agreed && (listing.serverPropertyId == null || (selectableServerUnits.any { "${it.roomCode} · ${it.unitCode}" == unit })), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)) { Icon(Icons.Filled.CheckCircle, null); Spacer(Modifier.width(7.dp)); Text("Send reservation request") }
        Spacer(Modifier.height(9.dp)); Text(if (listing.serverPropertyId != null) "No payment is taken here. The server holds the selected unit request for 24 hours and rejects conflicting active reservations; the owner must confirm the terms." else "No payment is taken here. Local demo requests do not reserve real inventory.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
    }
}
