package com.stayeasy.app

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
        Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Ink), modifier = Modifier.fillMaxWidth()) {
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
                Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { click() }) {
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

@Composable
internal fun FeatureRecordsScreen(category: String, ownerMode: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val title = StayRecordConfig.titles[category] ?: category.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    val records = remember(category) { mutableStateListOf<StayRecord>().apply { addAll(StayRecordStore.load(context, category)) } }
    val apiPreferences = remember { StayPreferenceStore.load(context) }
    val sessionToken = StayApiClient.currentSessionToken(apiPreferences.apiBaseUrl)
    val remoteScope = rememberCoroutineScope()
    var remoteSyncMessage by remember(category) { mutableStateOf("") }
    LaunchedEffect(category, apiPreferences.apiBaseUrl, sessionToken) {
        if (category in setOf("enquiries", "reservations", "visits", "issues") && apiPreferences.apiBaseUrl.isNotBlank() && !sessionToken.isNullOrBlank()) {
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
                StayRecordStore.replaceServerRecords(context, category, pulled)
                records.clear(); records.addAll(StayRecordStore.load(context, category))
                remoteSyncMessage = "Synced ${pulled.size} server record(s)"
            } catch (e: Exception) {
                remoteSyncMessage = "Server sync unavailable: ${e.message ?: "check the current API session"}"
            }
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
    var priority by remember(category) { mutableStateOf("Normal") }
    var formError by remember(category) { mutableStateOf("") }
    var showDeleteId by remember(category) { mutableStateOf<String?>(null) }
    var showServerStatusId by remember(category) { mutableStateOf<String?>(null) }
    var updatingServerStatus by remember(category) { mutableStateOf(false) }

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
        updatingServerStatus = true
        remoteScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    when (category) {
                        "reservations" -> StayApiClient.updateReservation(apiPreferences.apiBaseUrl, token, serverId, nextStatus)
                        "visits" -> StayApiClient.updateVisit(apiPreferences.apiBaseUrl, token, serverId, nextStatus)
                        "issues" -> StayApiClient.updateIssue(apiPreferences.apiBaseUrl, token, serverId, nextStatus)
                        else -> StayApiClient.updateEnquiry(apiPreferences.apiBaseUrl, token, serverId, nextStatus)
                    }
                }
                val rawStatus = response.optString("status", nextStatus.lowercase(Locale.ROOT).replace(' ', '_'))
                val displayStatus = rawStatus.replace('_', ' ').replaceFirstChar { it.uppercase() }
                StayRecordStore.update(context, category, record.copy(status = displayStatus, updatedAt = System.currentTimeMillis()))
                refresh()
                StayRecordStore.notify(context, "$title updated on server", "${record.title}: $displayStatus", record.property)
                remoteSyncMessage = "Server confirmed status: $displayStatus"
            } catch (e: Exception) {
                remoteSyncMessage = "Server update failed: ${e.message ?: "check owner permission"}"
            } finally {
                updatingServerStatus = false
                showServerStatusId = null
            }
        }
    }
    fun clearForm() {
        editingId = null; titleText = ""; detailsText = ""; personText = ""; propertyText = ""; unitText = ""; amountText = ""; dateText = ""; priority = "Normal"; formError = ""
    }
    fun openCreate() { clearForm(); showEditor = true }
    fun openEdit(r: StayRecord) {
        editingId = r.id; titleText = r.title; detailsText = r.details; personText = r.person; propertyText = r.property; unitText = r.unit
        amountText = if (r.amount == 0) "" else r.amount.toString(); dateText = r.date; priority = r.priority; formError = ""; showEditor = true
    }
    val isFinance = category in setOf("payments", "charges", "deposits", "expenses", "stays", "reservations", "food")
    val isOwnerOnly = category in setOf("beds", "residents", "charges", "deposits", "expenses", "staff", "notices", "food", "housekeeping", "visitors", "reports", "quality", "tasks", "admin")
    val visible = records.filter { r -> (query.isBlank() || listOf(r.title, r.details, r.person, r.property, r.unit).any { it.contains(query, true) }) && (filter == "All" || r.status == filter) }
    Column(Modifier.fillMaxSize().background(SE.Background).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = SE.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (StayRecordConfig.descriptions[category].isNullOrBlank()) "Local workflow record" else StayRecordConfig.descriptions[category]!!, color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                if (remoteSyncMessage.isNotBlank()) Text(remoteSyncMessage, color = if (remoteSyncMessage.startsWith("Synced")) SE.Blue else SE.Orange, fontSize = 9.sp, lineHeight = 13.sp)
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
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Search records, people or units") }, shape = RoundedCornerShape(14.dp))
        Spacer(Modifier.height(7.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (listOf("All") + StayRecordConfig.statuses(category)).distinct().forEach { status -> FilterChip(selected = filter == status, onClick = { filter = status }, label = { Text(status, fontSize = 10.sp) }) }
        }
        Spacer(Modifier.height(8.dp))
        if (visible.isEmpty()) {
            Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.FilterAlt, null, tint = SE.Blue, modifier = Modifier.size(30.dp)); Spacer(Modifier.height(7.dp))
                    Text(if (records.isEmpty()) "No records yet" else "No matching records", color = SE.Ink, fontWeight = FontWeight.Bold)
                    Text(if (records.isEmpty()) "Create the first record to start this workflow. Data will remain on this device in the prototype." else "Try clearing the filter or search text.", color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp)
                    if (records.isEmpty()) { Spacer(Modifier.height(8.dp)); Button(onClick = { openCreate() }, colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(5.dp)); Text("Add record") } }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(visible, key = { it.id }) { r ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
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
                                if (r.source != "Server") TextButton(onClick = { openEdit(r) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) { Text("Edit", color = SE.Blue, fontSize = 10.sp) }
                                if (r.source != "Server") TextButton(onClick = {
                                    val next = cycleStatus(category, r.status)
                                    val updated = r.copy(status = next, updatedAt = System.currentTimeMillis())
                                    StayRecordStore.update(context, category, updated); refresh()
                                    StayRecordStore.notify(context, "$title updated", "${r.title}: $next", r.property)
                                }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) { Text("Next status", color = SE.Blue, fontSize = 10.sp) }
                                if (r.source == "Server" && ownerMode && category in setOf("enquiries", "reservations", "visits", "issues") && serverStatusOptions(category, r.status).isNotEmpty()) {
                                    TextButton(onClick = { showServerStatusId = r.id }, enabled = !updatingServerStatus,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text(if (updatingServerStatus) "Updating…" else "Update status", color = SE.Blue, fontSize = 10.sp)
                                    }
                                }
                                if (r.source != "Server" && category in setOf("saved_searches", "tasks", "notices", "messages", "profile", "privacy", "documents", "quality", "admin", "housekeeping", "food", "visits", "enquiries", "reservations")) TextButton(onClick = { showDeleteId = r.id }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) { Text("Remove", color = SE.Red, fontSize = 10.sp) }
                            }
                        }
                    }
                }
            }
        }
        if (visible.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Button(onClick = { openCreate() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(7.dp)); Text("Add ${title.removeSuffix("s").lowercase(Locale.getDefault())}") }
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
                    OutlinedTextField(personText, { personText = it }, modifier = Modifier.fillMaxWidth(), label = { Text(when (category) { "staff" -> "Staff member and role"; "visitors" -> "Visitor name / purpose"; "saved_searches" -> "Optional alert recipient"; else -> "Person / resident / customer" }) }, singleLine = true)
                    OutlinedTextField(propertyText, { propertyText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Property / location") }, singleLine = true)
                    if (category in setOf("beds", "reservations", "stays", "residents", "issues", "housekeeping", "visitors", "documents")) OutlinedTextField(unitText, { unitText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Room / bed / unit (if applicable)") }, singleLine = true)
                    if (StayRecordConfig.usesAmount(category)) OutlinedTextField(amountText, { amountText = it.filter(Char::isDigit).take(9) }, modifier = Modifier.fillMaxWidth(), label = { Text(StayRecordConfig.amountLabel(category)) }, singleLine = true)
                    OutlinedTextField(dateText, { dateText = it }, modifier = Modifier.fillMaxWidth(), label = { Text(when (category) { "visits" -> "Visit date & time"; "reservations" -> "Expiry / move-in date"; "payments", "charges" -> "Due / payment date"; "stays" -> "Joining / move-out date"; else -> "Date / due date (optional)" }) }, singleLine = true, placeholder = { Text("e.g. 28 Oct 2026") })
                    Text("Priority", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Low", "Normal", "High", "Urgent").forEach { FilterChip(selected = priority == it, onClick = { priority = it }, label = { Text(it, fontSize = 10.sp) }) } }
                    if (category == "reservations") Text("A matching active reservation for the same property + unit is blocked on this device. Production must enforce this atomically on the server.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                    if (category in setOf("payments", "deposits")) Text("Manual ledger entry only; this is not a bank or payment-provider confirmation.", color = SE.Orange, fontSize = 10.sp, lineHeight = 14.sp)
                    if (formError.isNotBlank()) Text(formError, color = SE.Red, fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(onClick = {
                if (titleText.isBlank()) { formError = "Please enter a title."; return@TextButton }
                val amount = amountText.toIntOrNull() ?: 0
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
            }) { Text("Save record") } },
            dismissButton = { TextButton(onClick = { showEditor = false }) { Text("Cancel") } },
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
            title = { Text("Update server status", color = SE.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(selectedServerStatusRecord.title, color = SE.Text, fontWeight = FontWeight.SemiBold)
                    Text("Choose an allowed action. StayEasy will save the change only after the server confirms it.", color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp)
                    options.forEach { target ->
                        Button(
                            onClick = { updateServerStatus(selectedServerStatusRecord, target) },
                            enabled = !updatingServerStatus,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)
                        ) { Text(if (updatingServerStatus) "Updating…" else target) }
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
                Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
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
        Spacer(Modifier.height(17.dp)); Text("COLOUR PALETTE", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        themes.forEach { value ->
            val pair = themeSwatches(value)
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(14.dp)).background(SE.Surface).border(1.dp, if (theme == value) SE.Blue else SE.Border, RoundedCornerShape(14.dp)).clickable { theme = value; update() }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { pair.forEach { color -> Box(Modifier.size(23.dp).clip(CircleShape).background(color)) } }
                Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(value, color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text(themeDescription(value), color = SE.Muted, fontSize = 10.sp) }
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
        Spacer(Modifier.height(12.dp)); Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SE.BlueSoft)) { Text("Your preferences are saved locally on this device. Production notification preferences must sync to your account and be respected by the push service.", color = SE.Text, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(13.dp)) }
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
private fun themeDescription(theme: String) = when (theme) {
    "Midnight Cobalt" -> "Cool blue accents · deep midnight base"; "Ocean" -> "Blue-teal accents · clear cool surfaces"; "Violet" -> "Violet accents · soft neutral surfaces"
    "Graphite" -> "Slate accents · restrained contrast"; "Porcelain" -> "Cobalt accents · bright clean surfaces"; else -> "Copper accents · warm, muted neutral surfaces"
}
private fun themeSwatches(theme: String) = when (theme) {
    "Ocean" -> listOf(Color(0xFF087F9E), Color(0xFF64B6C8), Color(0xFFEAF5F8))
    "Violet" -> listOf(Color(0xFF7556D9), Color(0xFFB09BEF), Color(0xFFF2EEFC))
    "Graphite" -> listOf(Color(0xFF536174), Color(0xFF9CA9B8), Color(0xFFEDF0F4))
    "Porcelain" -> listOf(Color(0xFF315BE8), Color(0xFFF1845B), Color(0xFFF6F7FA))
    "Copper" -> listOf(Color(0xFFA35B40), Color(0xFFD3AA78), Color(0xFFF7F0E9))
    else -> listOf(Color(0xFF5478FF), Color(0xFFFF9C73), Color(0xFF141C2B))
}

@Composable
internal fun ApiConnectionScreen(
    initialBaseUrl: String,
    onBaseUrlChanged: (String) -> Unit,
    onListingsLoaded: (List<PgListing>) -> Unit,
    onBack: () -> Unit
) {
    var baseUrl by remember { mutableStateOf(initialBaseUrl) }
    var token by remember { mutableStateOf("") }
    var tokenSubject by remember { mutableStateOf("stayeasy-owner-demo") }
    var tokenRole by remember { mutableStateOf("owner") }
    var devPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Not connected yet") }
    var healthText by remember { mutableStateOf("") }
    var dashboardText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
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
                    Triple(health, rows, dashboard)
                }
                healthText = "${result.first.optString("service", "API")} · ${result.first.optString("status", "unknown")} · ${result.first.optString("environment", "")}".trim()
                dashboardText = result.third?.toString(2) ?: if (token.isNotBlank()) "Owner dashboard not available for this token/role." else "No bearer token provided; public listing search does not require one."
                if (loadListings) onListingsLoaded(result.second)
                status = if (loadListings) "Connected. Loaded ${result.second.size} published server listing(s)." else "API health check succeeded."
            } catch (e: Exception) {
                status = "Connection failed: ${e.message ?: "Unknown network error"}"
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
                Text("Use HTTPS for remote services. For the API scaffold, see backend/README.md. Demo data remains available if no API is configured.", color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp)
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
                    listOf("customer", "owner", "manager", "staff", "admin").forEach { role -> FilterChip(selected = tokenRole == role, onClick = { tokenRole = role }, label = { Text(role, fontSize = 10.sp) }) }
                }
                OutlinedTextField(value = devPassword, onValueChange = { devPassword = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Development token password") }, shape = RoundedCornerShape(12.dp))
                Button(onClick = {
                    if (busy) return@Button
                    if (baseUrl.isBlank() || tokenSubject.trim().length < 2 || devPassword.isBlank()) { status = "Enter the API URL, subject, and configured development password."; return@Button }
                    busy = true; status = "Requesting development session token…"
                    scope.launch {
                        try {
                            token = withContext(Dispatchers.IO) { StayApiClient.requestDevelopmentToken(baseUrl, tokenSubject, tokenRole, devPassword) }
                            status = "Development token issued for role '$tokenRole'. It is held in memory only."
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
        Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
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
