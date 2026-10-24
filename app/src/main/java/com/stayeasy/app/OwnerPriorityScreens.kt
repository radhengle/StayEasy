package com.stayeasy.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale

private fun priorityMoney(paise: Long): String = "₹" + String.format(Locale.US, "%,.2f", paise / 100.0)
private fun priorityRupees(rupees: Long): String = "₹" + String.format(Locale.US, "%,d", rupees)
private fun JSONObject.pText(key: String, fallback: String = ""): String =
    optString(key, fallback).takeUnless { it == "null" } ?: fallback
private fun JSONObject.pDate(key: String): LocalDate? = try {
    pText(key).takeIf { it.isNotBlank() }?.let(LocalDate::parse)
} catch (_: DateTimeParseException) { null }
private fun JSONObject.amountPaise(): Long =
    if (has("amount_paise") && !isNull("amount_paise")) optLong("amount_paise") else optLong("amount") * 100L

@Composable
private fun PriorityHeader(title: String, subtitle: String, onBack: () -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = SE.Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = SE.Muted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            TextButton(onClick = onRefresh) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh"); Text("Refresh") }
        }
        Row {
            TextButton(onClick = onBack) { Text("Back") }
        }
        HorizontalDivider(color = SE.Border)
    }
}

@Composable
private fun PriorityState(loading: Boolean, error: String, onRetry: () -> Unit) {
    if (loading) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(9.dp))
            Text("Loading live records…", color = SE.Muted, fontSize = 12.sp)
        }
    } else if (error.isNotBlank()) {
        Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Live records unavailable", color = SE.Ink, fontWeight = FontWeight.Bold)
                Text(error, color = SE.Muted, fontSize = 12.sp, lineHeight = 17.sp)
                OutlinedButton(onClick = onRetry) { Text("Retry") }
            }
        }
    }
}

@Composable
private fun PriorityEmpty(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(shape = SE.cardShape(20.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = SE.Blue, modifier = Modifier.size(32.dp))
            Text(title, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(subtitle, color = SE.Muted, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun PriorityTag(label: String, positive: Boolean = false) {
    Box(Modifier.background(if (positive) SE.BlueSoft else SE.OrangeSoft, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 5.dp)) {
        Text(label.replace('_', ' '), color = if (positive) SE.Blue else SE.Orange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun OwnerEnquiriesInboxScreen(baseUrl: String, token: String?, onBack: () -> Unit, notify: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var enquiries by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var properties by remember { mutableStateOf<List<PgListing>>(emptyList()) }
    var candidates by remember { mutableStateOf<Map<String, RemoteResidentCandidate>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var reply by remember { mutableStateOf("") }
    var moveInDate by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var unitId by remember { mutableLongStateOf(0L) }
    var inviteLink by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(baseUrl, token, refresh) {
        if (baseUrl.isBlank() || token.isNullOrBlank()) {
            error = "Connect the app to the StayEasy API and sign in with an approved owner account to load real enquiries."
            loading = false
            enquiries = emptyList()
        } else {
            loading = true; error = ""
            try {
                val result = withContext(Dispatchers.IO) {
                    val props = StayApiClient.ownerProperties(baseUrl, token)
                    val rows = StayApiClient.enquiries(baseUrl, token)
                    val people = linkedMapOf<String, RemoteResidentCandidate>()
                    props.filter { (it.serverPropertyId ?: 0L) > 0L }.forEach { prop ->
                        runCatching { StayApiClient.ownerResidentCandidates(baseUrl, token, prop.serverPropertyId!!) }
                            .getOrDefault(emptyList()).forEach { people[it.id] = it }
                    }
                    Triple(props, rows, people)
                }
                properties = result.first; enquiries = result.second; candidates = result.third
            } catch (e: Exception) {
                error = e.message ?: "Could not load the enquiry inbox."
                enquiries = emptyList()
            } finally { loading = false }
        }
    }

    val openRows = enquiries.filter { it.pText("status").lowercase(Locale.ROOT) !in setOf("closed", "converted") }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PriorityHeader("Reply to new enquiries", "Review the actual request, reply, or invite the same customer to a specific available bed.", onBack) { refresh++ }
        PriorityState(loading, error) { refresh++ }
        if (!loading && error.isBlank() && openRows.isEmpty()) PriorityEmpty("You're all caught up", "No open enquiries were returned by the server. New customer enquiries will appear here.", Icons.Filled.ChatBubbleOutline)
        if (!loading && error.isBlank()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(openRows, key = { it.optLong("id") }) { row ->
                    val customer = candidates[row.pText("customer_id")]
                    Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth().clickable {
                        selected = row
                        reply = row.pText("owner_response")
                        moveInDate = row.pText("move_in_date").ifBlank { LocalDate.now().plusDays(7).toString() }
                        val prop = properties.firstOrNull { it.serverPropertyId == row.optLong("property_id") }
                        unitId = prop?.serverUnits?.firstOrNull { it.status.equals("available", true) }?.id ?: 0L
                        inviteLink = ""
                    }) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ChatBubbleOutline, null, tint = SE.Blue)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(customer?.displayName ?: "Customer enquiry", color = SE.Ink, fontWeight = FontWeight.Bold)
                                    Text(row.pText("property_name", "Property"), color = SE.Muted, fontSize = 11.sp)
                                }
                                PriorityTag(row.pText("status", "new"), row.pText("status") != "new")
                            }
                            Text("Requested room: " + row.pText("preferred_room_type", "Not specified"), color = SE.Ink, fontSize = 12.sp)
                            Text("Move-in: " + row.pText("move_in_date", "Not specified") + " · Budget: not provided", color = SE.Muted, fontSize = 11.sp)
                            Text(row.pText("message", "No message supplied."), color = SE.Ink, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Open details and actions", color = SE.Blue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Spacer(Modifier.weight(1f)); Icon(Icons.Filled.ChevronRight, null, tint = SE.Blue)
                            }
                        }
                    }
                }
            }
        }
    }

    val enquiry = selected
    if (enquiry != null) {
        val customer = candidates[enquiry.pText("customer_id")]
        val prop = properties.firstOrNull { it.serverPropertyId == enquiry.optLong("property_id") }
        val available = prop?.serverUnits?.filter { it.status.equals("available", true) }.orEmpty()
        val dateIsValid = runCatching { LocalDate.parse(moveInDate) >= LocalDate.now() }.getOrDefault(false)
        AlertDialog(
            onDismissRequest = { if (!busy) selected = null },
            title = { Text("Enquiry details", color = SE.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(customer?.displayName ?: "Customer account", color = SE.Ink, fontWeight = FontWeight.Bold)
                    Text(customer?.email ?: "Verified customer email is unavailable. Refresh before sending a bed-specific invitation.", color = SE.Muted, fontSize = 11.sp)
                    Text("Property: " + enquiry.pText("property_name"), color = SE.Ink, fontSize = 12.sp)
                    Text("Requested room: " + enquiry.pText("preferred_room_type", "Not specified"), color = SE.Ink, fontSize = 12.sp)
                    Text("Move-in: " + enquiry.pText("move_in_date", "Not specified") + " · Budget: not provided", color = SE.Muted, fontSize = 11.sp)
                    Text("Message", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text(enquiry.pText("message", "No message supplied."), color = SE.Muted, fontSize = 12.sp, lineHeight = 17.sp)
                    if (enquiry.pText("owner_response").isNotBlank()) Text("Previous reply: " + enquiry.pText("owner_response"), color = SE.Muted, fontSize = 11.sp)
                    HorizontalDivider(color = SE.Border)
                    OutlinedTextField(reply, { reply = it.take(4000) }, label = { Text("Reply or request more details") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
                    Text("Invite the tenant to a specific available bed", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    OutlinedTextField(moveInDate, { moveInDate = it }, label = { Text("Proposed move-in date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    if (available.isEmpty()) Text("No available server-backed beds are listed. Add/release a bed in inventory and refresh.", color = SE.Orange, fontSize = 11.sp)
                    else available.forEach { unit ->
                        FilterChip(selected = unitId == unit.id, onClick = { unitId = unit.id }, label = { Text("Room ${unit.roomCode} · ${unit.unitCode} · ${unit.sharingCapacity}-sharing · ${priorityRupees(unit.monthlyRent)}/mo") })
                    }
                    if (inviteLink.isNotBlank()) {
                        Text("Invitation link created once. Share it with this tenant:", color = SE.Blue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(inviteLink, color = SE.Ink, fontSize = 10.sp, lineHeight = 14.sp)
                        OutlinedButton(onClick = {
                            val message = "Complete StayEasy registration for ${prop?.name ?: "this property"}: $inviteLink"
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message), "Share tenant invitation"))
                        }, modifier = Modifier.fillMaxWidth()) { Text("Share invitation link") }
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(enabled = !busy && !token.isNullOrBlank() && reply.isNotBlank(), onClick = {
                        busy = true
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { StayApiClient.updateEnquiry(baseUrl, token!!, enquiry.optLong("id"), "contacted", reply) }
                                notify("Reply saved to the server enquiry.")
                                selected = null; refresh++
                            } catch (e: Exception) { notify("Reply failed: ${e.message ?: "check the API connection"}") }
                            finally { busy = false }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save reply") }
                    Button(enabled = !busy && !token.isNullOrBlank() && customer?.email?.isNotBlank() == true &&
                        (prop?.serverPropertyId ?: 0L) > 0L && unitId > 0L && available.any { it.id == unitId } && dateIsValid,
                        onClick = {
                            busy = true
                            scope.launch {
                                try {
                                    val invitation = withContext(Dispatchers.IO) {
                                        StayApiClient.createTenantInvitation(baseUrl, token!!, prop!!.serverPropertyId!!, unitId,
                                            enquiry.optLong("id"), customer!!.email, customer.displayName, "", moveInDate, 168)
                                    }
                                    inviteLink = invitation.pText("invitation_url")
                                    runCatching {
                                        withContext(Dispatchers.IO) {
                                            StayApiClient.updateEnquiry(baseUrl, token, enquiry.optLong("id"), "contacted",
                                                listOf(reply, "Invitation sent for ${invitation.pText("room_code")} · ${invitation.pText("unit_code")}.").filter { it.isNotBlank() }.joinToString("\n"))
                                        }
                                    }
                                    refresh++
                                    notify("Bed-specific invitation created. Share the one-time link in this panel.")
                                } catch (e: Exception) { notify("Invitation failed: ${e.message ?: "refresh inventory and retry"}") }
                                finally { busy = false }
                            }
                        }, modifier = Modifier.fillMaxWidth()
                    ) { Text("Invite tenant to selected bed") }
                }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { selected = null }) { Text("Close") } },
            containerColor = SE.Surface
        )
    }
}

private data class RentDueKey(val propertyId: Long, val stayId: Long, val reservationId: Long, val period: String)

private data class RentDueLine(
    val key: String, val propertyId: Long, val propertyName: String, val stayId: Long, val reservationId: Long?,
    val tenantName: String, val roomCode: String, val unitCode: String, val period: String,
    val dueDate: String, val billedPaise: Long, val paidPaise: Long, val status: String, val description: String
) {
    val balancePaise: Long get() = (billedPaise - paidPaise).coerceAtLeast(0L)
}

@Composable
internal fun OwnerRentDueScreen(baseUrl: String, token: String?, onBack: () -> Unit, notify: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var ledger by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var stays by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<RentDueLine?>(null) }
    var amountText by remember { mutableStateOf("") }
    var paymentReference by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(baseUrl, token, refresh) {
        if (baseUrl.isBlank() || token.isNullOrBlank()) {
            error = "Connect the app to the API and sign in as an approved owner to review real rent balances."
            loading = false; ledger = emptyList(); stays = emptyList()
        } else {
            loading = true; error = ""
            try {
                val result = withContext(Dispatchers.IO) { StayApiClient.ownerLedger(baseUrl, token) to StayApiClient.ownerStays(baseUrl, token) }
                ledger = result.first; stays = result.second
            } catch (e: Exception) { error = e.message ?: "Could not load rent ledger"; ledger = emptyList(); stays = emptyList() }
            finally { loading = false }
        }
    }
    val lines = remember(ledger, stays) {
        val charges = ledger.filter { it.pText("category").lowercase(Locale.ROOT) == "rent_charge" && it.pText("status").lowercase(Locale.ROOT) !in setOf("cancelled", "void", "written_off") }
        charges.groupBy {
            RentDueKey(it.optLong("property_id"), it.optLong("resident_stay_id"), it.optLong("reservation_id"), it.pText("billing_period"))
        }.mapNotNull { (key, items) ->
                val propertyId = key.propertyId
                val stayId = key.stayId
                val reservationId = key.reservationId
                val period = key.period
                if (stayId <= 0L && reservationId <= 0L) return@mapNotNull null
                val payments = ledger.filter { payment ->
                    payment.pText("category").lowercase(Locale.ROOT) == "payment" &&
                    payment.optLong("property_id") == propertyId &&
                    (if (reservationId > 0L) payment.optLong("reservation_id") == reservationId else payment.optLong("resident_stay_id") == stayId) &&
                    payment.pText("billing_period") == period && payment.pText("status").lowercase(Locale.ROOT) !in setOf("cancelled", "void", "rejected")
                }
                val billed = items.sumOf { if (it.has("amount_paise") && !it.isNull("amount_paise")) it.optLong("amount_paise") else it.optLong("amount") * 100L }
                val paid = payments.sumOf { if (it.has("amount_paise") && !it.isNull("amount_paise")) it.optLong("amount_paise") else it.optLong("amount") * 100L }
                if (billed <= paid) return@mapNotNull null
                val first = items.minByOrNull { it.pText("due_date", "9999-12-31") } ?: items.first()
                val stay = stays.firstOrNull { it.optLong("id") == stayId }
                RentDueLine("${propertyId}:${stayId}:${reservationId}:$period", propertyId, first.pText("property_name", stay?.pText("property_name", "Property") ?: "Property"),
                    stayId, reservationId.takeIf { it > 0L },
                    stay?.pText("resident_name", first.pText("resident_name", "Tenant")) ?: first.pText("resident_name", "Tenant"),
                    stay?.pText("room_code") ?: "", stay?.pText("unit_code", first.pText("unit_code")) ?: "",
                    period, first.pText("due_date", "Not set"), billed, paid, first.pText("status", "open"),
                    items.map { it.pText("description") }.filter { it.isNotBlank() }.distinct().joinToString(" · "))
            }.sortedWith(compareBy<RentDueLine> { it.dueDate }.thenBy { it.propertyName })
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PriorityHeader("Review rent due", "Server ledger balances and recorded payment history. Manual entries are not bank-verified.", onBack) { refresh++ }
        PriorityState(loading, error) { refresh++ }
        if (!loading && error.isBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Card(shape = SE.cardShape(15.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Outstanding", color = SE.Muted, fontSize = 10.sp)
                        Text(priorityMoney(lines.sumOf { it.balancePaise }), color = SE.Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Card(shape = SE.cardShape(15.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.weight(1f)) {
                    Column(Modifier.padding(12.dp)) { Text("Periods due", color = SE.Muted, fontSize = 10.sp); Text(lines.size.toString(), color = SE.Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold) }
                }
            }
            if (lines.isEmpty()) PriorityEmpty("No rent currently due", "No open rent charges with an outstanding balance were returned by the server.", Icons.Filled.Payments)
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(lines, key = { it.key }) { line ->
                    Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(line.tenantName, color = SE.Ink, fontWeight = FontWeight.Bold)
                                    Text("${line.propertyName} · ${line.roomCode} ${line.unitCode}", color = SE.Muted, fontSize = 11.sp)
                                }
                                PriorityTag(line.status, line.balancePaise == 0L)
                            }
                            Text("Billing period: ${line.period.ifBlank { "Not specified" }} · Due: ${line.dueDate}", color = SE.Muted, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("Billed ${priorityMoney(line.billedPaise)}", color = SE.Muted, fontSize = 11.sp)
                                    Text("Recorded ${priorityMoney(line.paidPaise)}", color = SE.Muted, fontSize = 11.sp)
                                }
                                Text(priorityMoney(line.balancePaise), color = SE.Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            }
                            if (line.description.isNotBlank()) Text(line.description, color = SE.Muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Button(onClick = {
                                selected = line
                                amountText = String.format(Locale.US, "%.2f", line.balancePaise / 100.0)
                                paymentReference = ""
                            }, modifier = Modifier.fillMaxWidth()) { Text("Record payment") }
                        }
                    }
                }
            }
        }
    }
    val line = selected
    if (line != null) AlertDialog(
        onDismissRequest = { if (!busy) selected = null },
        title = { Text("Record payment", color = SE.Ink, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${line.tenantName} · ${line.propertyName} · ${line.period}", color = SE.Muted, fontSize = 12.sp)
                Text("Outstanding: ${priorityMoney(line.balancePaise)}", color = SE.Ink, fontWeight = FontWeight.Bold)
                OutlinedTextField(amountText, { amountText = it.filter { ch -> ch.isDigit() || ch == '.' }.take(12) }, label = { Text("Payment amount (₹)") }, singleLine = true)
                OutlinedTextField(paymentReference, { paymentReference = it.take(180) }, label = { Text("Reference / receipt note (optional)") }, singleLine = true)
                Text("This saves a manual ledger row only. It does not verify that money reached the bank.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        },
        confirmButton = {
            Button(enabled = !busy && (amountText.toBigDecimalOrNull()?.toDouble() ?: 0.0) > 0.0, onClick = {
                val paise = runCatching { (amountText.toBigDecimal() * java.math.BigDecimal(100)).longValueExact() }.getOrNull()
                if (paise == null || paise <= 0L || paise > line.balancePaise) {
                    notify("Enter a valid payment amount no greater than the outstanding balance.")
                    return@Button
                }
                busy = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            StayApiClient.createManualLedgerEntry(
                                baseUrl, token!!, line.propertyId, line.stayId.takeIf { it > 0L }, "payment",
                                ((paise + 50L) / 100L).toInt(), line.period, "", paymentReference,
                                "Manual rent payment for ${line.period}; bank settlement not verified.",
                                reservationId = line.reservationId, amountPaise = paise
                            )
                        }
                        notify("Manual payment entry saved. Verify settlement separately.")
                        selected = null; refresh++
                    } catch (e: Exception) { notify("Payment could not be recorded: ${e.message ?: "check the API connection"}") }
                    finally { busy = false }
                }
            }) { Text(if (busy) "Saving…" else "Save payment record") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = { selected = null }) { Text("Cancel") } },
        containerColor = SE.Surface
    )
}

private data class ArrivalRow(
    val reservation: JSONObject, val property: PgListing?,
    val invitation: JSONObject?, val customer: RemoteResidentCandidate?, val stay: JSONObject?
)

@Composable
internal fun OwnerMoveInArrivalsScreen(
    baseUrl: String, token: String?, initialTab: String = "Arrivals", onBack: () -> Unit, notify: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var reservations by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var stays by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var properties by remember { mutableStateOf<List<PgListing>>(emptyList()) }
    var invitations by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var candidates by remember { mutableStateOf<Map<String, RemoteResidentCandidate>>(emptyMap()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var tab by remember(initialTab) { mutableStateOf(initialTab) }
    var busyId by remember { mutableLongStateOf(0L) }

    LaunchedEffect(baseUrl, token, refresh) {
        if (baseUrl.isBlank() || token.isNullOrBlank()) {
            error = "Connect the app to your API and sign in as an approved owner to load move-ins."
            loading = false; reservations = emptyList(); stays = emptyList(); properties = emptyList(); invitations = emptyList()
        } else {
            loading = true; error = ""
            try {
                val result = withContext(Dispatchers.IO) {
                    val props = StayApiClient.ownerProperties(baseUrl, token)
                    val rs = StayApiClient.reservations(baseUrl, token)
                    val ss = StayApiClient.ownerStays(baseUrl, token)
                    val invites = runCatching { StayApiClient.ownerInvitations(baseUrl, token) }.getOrDefault(emptyList())
                    val people = linkedMapOf<String, RemoteResidentCandidate>()
                    props.filter { (it.serverPropertyId ?: 0L) > 0L }.forEach { prop ->
                        runCatching { StayApiClient.ownerResidentCandidates(baseUrl, token, prop.serverPropertyId!!) }
                            .getOrDefault(emptyList()).forEach { people[it.id] = it }
                    }
                    ArrivalLoad(props, rs, ss, invites, people)
                }
                properties = result.properties; reservations = result.reservations
                stays = result.stays; invitations = result.invitations; candidates = result.candidates
            } catch (e: Exception) {
                error = e.message ?: "Could not load reservations and stays."
                reservations = emptyList(); stays = emptyList(); properties = emptyList(); invitations = emptyList()
            } finally { loading = false }
        }
    }

    val today = LocalDate.now()
    val rows = remember(reservations, properties, invitations, candidates, stays) {
        reservations.filter { it.pText("status").lowercase(Locale.ROOT) in setOf("pending", "confirmed") }
            .filter { it.pDate("move_in_date")?.isAfter(today.minusDays(45)) ?: true }
            .map { reservation ->
                val property = properties.firstOrNull { it.serverPropertyId == reservation.optLong("property_id") }
                val invitation = invitations.firstOrNull { it.optLong("reservation_id") == reservation.optLong("id") ||
                    (it.pText("recipient_user_id") == reservation.pText("customer_id") && it.pText("status") == "accepted") }
                val customer = candidates[reservation.pText("customer_id")]
                val stay = stays.firstOrNull { it.optLong("reservation_id") == reservation.optLong("id") }
                ArrivalRow(reservation, property, invitation, customer, stay)
            }.sortedBy { it.reservation.pDate("move_in_date") ?: LocalDate.MAX }
    }
    val pendingInvites = invitations.filter { it.pText("status") == "pending" && it.optLong("reservation_id") <= 0L }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PriorityHeader(if (tab == "Arrivals") "Move-in arrivals" else "Residents & stays",
            "Actual invitation, reservation, check-in and stay records.", onBack) { refresh++ }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = tab == "Arrivals", onClick = { tab = "Arrivals" }, label = { Text("Arrivals") })
            FilterChip(selected = tab == "Residents", onClick = { tab = "Residents" }, label = { Text("Resident roster") })
        }
        PriorityState(loading, error) { refresh++ }
        if (!loading && error.isBlank() && tab == "Arrivals") {
            if (pendingInvites.isNotEmpty()) {
                Text("Waiting for tenant registration", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 9.dp, bottom = 7.dp))
                pendingInvites.forEach { invite ->
                    Card(shape = SE.cardShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(invite.pText("recipient_name", "Invited tenant"), color = SE.Ink, fontWeight = FontWeight.Bold)
                            Text("${invite.pText("property_name")} · Room ${invite.pText("room_code")} · ${invite.pText("unit_code")}", color = SE.Muted, fontSize = 11.sp)
                            Text("Move-in: ${invite.pText("move_in_date")} · Expires: ${invite.pText("expires_at").take(10)}", color = SE.Muted, fontSize = 11.sp)
                            PriorityTag("awaiting registration")
                        }
                    }
                }
            }
            Text("Pending and confirmed arrivals", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp, bottom = 7.dp))
            if (rows.isEmpty()) PriorityEmpty("No upcoming move-ins", "Reservations appear here when tenants submit booking or invitation registration.", Icons.Filled.CalendarToday)
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(rows, key = { it.reservation.optLong("id") }) { row ->
                    val res = row.reservation
                    val status = res.pText("status")
                    val invite = row.invitation
                    val personName = invite?.pText("recipient_name")?.takeIf { it.isNotBlank() }
                        ?: row.customer?.displayName ?: "Tenant ${res.pText("customer_id")}"
                    val unit = row.property?.serverUnits?.firstOrNull { it.id == res.optLong("unit_id") }
                    val checkedIn = row.stay != null
                    val moveDate = res.pDate("move_in_date")
                    Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(personName, color = SE.Ink, fontWeight = FontWeight.Bold)
                                    Text(row.property?.name ?: res.pText("property_name", "Property"), color = SE.Muted, fontSize = 11.sp)
                                }
                                PriorityTag(if (checkedIn) "checked in" else status, checkedIn || status == "confirmed")
                            }
                            Text("Room ${unit?.roomCode ?: "—"} · Bed ${unit?.unitCode ?: "—"} · ${row.customer?.email ?: invite?.pText("recipient_email", "")}", color = SE.Ink, fontSize = 12.sp)
                            Text("Expected move-in: ${res.pText("move_in_date")}", color = SE.Muted, fontSize = 11.sp)
                            Text(when {
                                checkedIn -> "Registration and actual check-in are recorded."
                                invite?.pText("status") == "accepted" && status == "pending" -> "Registration submitted · awaiting owner review."
                                status == "pending" -> "Reservation request · owner decision required."
                                else -> "Confirmed booking · check-in can be recorded on or after the move-in date."
                            }, color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                            if (!checkedIn && status == "pending") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(enabled = busyId == 0L, onClick = {
                                        busyId = res.optLong("id")
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) { StayApiClient.updateReservation(baseUrl, token!!, res.optLong("id"), "confirmed") }
                                                notify("Reservation approved. The bed remains reserved until actual check-in.")
                                                refresh++
                                            } catch (e: Exception) { notify("Approval failed: ${e.message ?: "refresh and retry"}") }
                                            finally { busyId = 0L }
                                        }
                                    }, modifier = Modifier.weight(1f)) { Text(if (busyId == res.optLong("id")) "Saving…" else "Approve booking") }
                                    OutlinedButton(enabled = busyId == 0L, onClick = {
                                        busyId = res.optLong("id")
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) { StayApiClient.updateReservation(baseUrl, token!!, res.optLong("id"), "cancelled") }
                                                notify("Reservation declined and the bed hold was released.")
                                                refresh++
                                            } catch (e: Exception) { notify("Decline failed: ${e.message ?: "refresh and retry"}") }
                                            finally { busyId = 0L }
                                        }
                                    }, modifier = Modifier.weight(1f)) { Text("Decline") }
                                }
                            } else if (!checkedIn && status == "confirmed") {
                                Button(
                                    enabled = busyId == 0L && moveDate != null && !moveDate.isAfter(today),
                                    onClick = {
                                        busyId = res.optLong("id")
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    val propertyId = res.optLong("property_id"); val unitId = res.optLong("unit_id")
                                                    require(propertyId > 0L && unitId > 0L) { "This reservation has no assigned bed." }
                                                    StayApiClient.createOwnerStay(baseUrl, token!!, propertyId, unitId, res.pText("customer_id"),
                                                        res.pText("move_in_date"), res.optInt("rent_snapshot", unit?.monthlyRent ?: 0),
                                                        res.optInt("deposit_snapshot", unit?.deposit ?: 0), res.optLong("id"))
                                                }
                                                notify("Check-in saved against the confirmed reservation.")
                                                refresh++
                                            } catch (e: Exception) { notify("Check-in failed: ${e.message ?: "confirm the reservation and refresh"}") }
                                            finally { busyId = 0L }
                                        }
                                    }, modifier = Modifier.fillMaxWidth()
                                ) { Text(if (busyId == res.optLong("id")) "Saving check-in…" else if (moveDate?.isAfter(today) == true) "Check-in opens ${moveDate}" else "Mark moved in") }
                            }
                        }
                    }
                }
            }
        } else if (!loading && error.isBlank() && tab == "Residents") {
            if (stays.isEmpty()) PriorityEmpty("No active resident stays", "A confirmed booking becomes an active stay after the owner records actual check-in.", Icons.Filled.Person)
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
                items(stays, key = { it.optLong("id") }) { stay ->
                    Card(shape = SE.cardShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(stay.pText("resident_name", "Resident"), color = SE.Ink, fontWeight = FontWeight.Bold)
                            Text("${stay.pText("property_name")} · ${stay.pText("room_code")} · ${stay.pText("unit_code")}", color = SE.Muted, fontSize = 11.sp)
                            Text("Joined ${stay.pText("joining_date")} · Rent ${priorityRupees(stay.optLong("agreed_monthly_rent"))}/month · Deposit ${priorityRupees(stay.optLong("deposit_held"))}", color = SE.Ink, fontSize = 12.sp)
                            PriorityTag(stay.pText("status"), stay.pText("status") == "active")
                        }
                    }
                }
            }
        }
    }
}

private data class ArrivalLoad(
    val properties: List<PgListing>, val reservations: List<JSONObject>, val stays: List<JSONObject>,
    val invitations: List<JSONObject>, val candidates: Map<String, RemoteResidentCandidate>
)

@Composable
internal fun OwnerMaintenancePriorityScreen(baseUrl: String, token: String?, onBack: () -> Unit, notify: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var issues by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var busyId by remember { mutableLongStateOf(0L) }
    LaunchedEffect(baseUrl, token, refresh) {
        if (baseUrl.isBlank() || token.isNullOrBlank()) {
            error = "Connect the app to your API and sign in as an approved owner to load maintenance tickets."
            loading = false; issues = emptyList()
        } else {
            loading = true; error = ""
            try { issues = withContext(Dispatchers.IO) { StayApiClient.issues(baseUrl, token) } }
            catch (e: Exception) { error = e.message ?: "Could not load maintenance tickets."; issues = emptyList() }
            finally { loading = false }
        }
    }
    val active = issues.filter { it.pText("status").lowercase(Locale.ROOT) !in setOf("resolved", "closed") }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PriorityHeader("Maintenance priorities", "Resolve urgent work and let the server update bed availability safely.", onBack) { refresh++ }
        PriorityState(loading, error) { refresh++ }
        if (!loading && error.isBlank() && active.isEmpty()) PriorityEmpty("No open maintenance tickets", "The server returned no unresolved maintenance work for this owner.", Icons.Filled.CheckCircle)
        if (!loading && error.isBlank()) LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            items(active, key = { it.optLong("id") }) { issue ->
                val id = issue.optLong("id")
                val status = issue.pText("status", "open")
                val priority = issue.pText("priority", "normal")
                Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ReportProblem, null, tint = if (priority.lowercase(Locale.ROOT) in setOf("urgent", "critical", "high")) SE.Red else SE.Orange)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(issue.pText("title", "Maintenance issue"), color = SE.Ink, fontWeight = FontWeight.Bold)
                                Text(issue.pText("property_name", "Property"), color = SE.Muted, fontSize = 11.sp)
                            }
                            PriorityTag(priority)
                        }
                        Text("Location: " + issue.pText("unit_code", issue.pText("room_code", "Common area")), color = SE.Ink, fontSize = 12.sp)
                        Text(issue.pText("description", "No description supplied."), color = SE.Muted, fontSize = 12.sp, lineHeight = 17.sp)
                        Text("Status: $status", color = SE.Muted, fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (status.lowercase(Locale.ROOT) != "in_progress") Button(enabled = busyId == 0L, onClick = {
                                busyId = id
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { StayApiClient.updateIssue(baseUrl, token!!, id, "in_progress") }
                                        notify("Maintenance issue moved to in progress."); refresh++
                                    } catch (e: Exception) { notify("Issue update failed: ${e.message ?: "refresh and retry"}") }
                                    finally { busyId = 0L }
                                }
                            }, modifier = Modifier.weight(1f)) { Text("In progress") }
                            Button(enabled = busyId == 0L, onClick = {
                                busyId = id
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { StayApiClient.updateIssue(baseUrl, token!!, id, "resolved") }
                                        notify("Resolution saved. Inventory remains blocked if other open tickets still affect the bed."); refresh++
                                    } catch (e: Exception) { notify("Resolution failed: ${e.message ?: "refresh and retry"}") }
                                    finally { busyId = 0L }
                                }
                            }, modifier = Modifier.weight(1f)) { Text("Resolve") }
                        }
                    }
                }
            }
        }
    }
}
