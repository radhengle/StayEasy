package com.stayeasy.app

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private fun invitationText(json: JSONObject?, key: String, fallback: String = ""): String =
    json?.optString(key, fallback)?.takeUnless { it == "null" } ?: fallback

private fun invitationCategories(json: JSONObject?): List<String> {
    val array = json?.optJSONArray("required_document_categories") ?: JSONArray()
    return (0 until array.length()).map { array.optString(it) }.filter { it.isNotBlank() }.distinct()
}

private fun documentLabel(category: String): String = when (category) {
    "government_id" -> "Government ID"
    "address_proof" -> "Address proof"
    "pan_optional" -> "PAN (optional)"
    "employment_optional" -> "Employment proof (optional)"
    "other" -> "Other requested document"
    else -> category.replace('_', ' ').replaceFirstChar { it.uppercase(Locale.ROOT) }
}

private fun invitedTokenFromInput(raw: String): String {
    val value = raw.trim()
    if (value.isBlank()) return ""
    val uri = runCatching { Uri.parse(value) }.getOrNull()
    val fromQuery = uri?.getQueryParameter("invitation") ?: uri?.getQueryParameter("token")
    if (!fromQuery.isNullOrBlank()) return fromQuery
    if (uri?.scheme.equals("stayeasy", ignoreCase = true)) return uri?.lastPathSegment.orEmpty()
    return value.substringAfterLast('/').substringBefore('?').trim()
}

@Composable
internal fun TenantInvitationRegistrationScreen(
    baseUrl: String,
    sessionToken: String?,
    initialLink: String = "",
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    notify: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pastedLink by remember(initialLink) { mutableStateOf(initialLink) }
    var invitations by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var invitation by remember { mutableStateOf<JSONObject?>(null) }
    var invitationToken by remember { mutableStateOf<String?>(null) }
    var invitationId by remember { mutableStateOf<Long?>(null) }
    var documents by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var phone by remember { mutableStateOf("") }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var accommodationNotes by remember { mutableStateOf("") }
    var profileAcknowledged by remember { mutableStateOf(false) }
    var rentAcknowledged by remember { mutableStateOf(false) }
    var documentConsent by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var loading by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<JSONObject?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        selectedUri = uri
    }

    LaunchedEffect(baseUrl, sessionToken, refresh) {
        if (baseUrl.isBlank() || sessionToken.isNullOrBlank()) {
            invitations = emptyList()
        } else {
            runCatching {
                withContext(Dispatchers.IO) { StayApiClient.tenantInvitations(baseUrl, sessionToken!!) }
            }.onSuccess { invitations = it }
             .onFailure { error = it.message ?: "Could not load invitations for this account." }
        }
    }

    fun resetConfirmation() {
        result = null
        error = ""
        profileAcknowledged = false
        rentAcknowledged = false
        documentConsent = false
        selectedUri = null
    }

    fun loadByToken() {
        val parsed = invitedTokenFromInput(pastedLink)
        if (baseUrl.isBlank() || parsed.isBlank()) {
            error = "Paste the complete StayEasy invitation link or token."
            return
        }
        loading = true
        error = ""
        result = null
        scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { StayApiClient.resolveTenantInvitation(baseUrl, parsed) }
                invitation = loaded
                invitationToken = parsed
                invitationId = null
                phone = ""
                emergencyName = ""
                emergencyPhone = ""
                accommodationNotes = ""
                resetConfirmation()
                val id = loaded.optLong("id")
                if (!sessionToken.isNullOrBlank() && id > 0L) {
                    documents = withContext(Dispatchers.IO) { StayApiClient.tenantInvitationDocuments(baseUrl, sessionToken, id) }
                } else documents = emptyList()
            } catch (e: Exception) {
                error = e.message ?: "Invitation could not be loaded. Check the link and try again."
                invitation = null
            } finally { loading = false }
        }
    }

    fun selectAccountInvitation(row: JSONObject) {
        if (baseUrl.isBlank() || sessionToken.isNullOrBlank()) return
        val id = row.optLong("id")
        if (id <= 0L) return
        loading = true
        error = ""
        result = null
        scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { StayApiClient.tenantInvitations(baseUrl, sessionToken).firstOrNull { it.optLong("id") == id } ?: row }
                invitation = loaded
                invitationId = id
                invitationToken = null
                phone = ""
                emergencyName = ""
                emergencyPhone = ""
                accommodationNotes = ""
                resetConfirmation()
                documents = withContext(Dispatchers.IO) { StayApiClient.tenantInvitationDocuments(baseUrl, sessionToken, id) }
                val quote = loaded.optJSONObject("billable_quote")
                if (quote == null) {
                    val resolved = withContext(Dispatchers.IO) {
                        // ID-based invitations are authenticated and returned with the same server quote.
                        loaded
                    }
                    invitation = resolved
                }
            } catch (e: Exception) {
                error = e.message ?: "Could not open this invitation."
            } finally { loading = false }
        }
    }

    val current = invitation
    val requiredDocs = invitationCategories(current)
    val submittedCategories = documents.filter {
        invitationText(it, "status").lowercase(Locale.ROOT) in setOf("submitted", "approved")
    }.map { invitationText(it, "category") }.toSet()
    val missingDocs = requiredDocs.filterNot { it in submittedCategories }
    val quote = current?.optJSONObject("billable_quote")
    val monthlyRent = current?.optLong("monthly_rent") ?: 0L
    val deposit = current?.optLong("deposit") ?: 0L
    val canSubmit = !submitting && current != null && !sessionToken.isNullOrBlank() &&
        phone.filter(Char::isDigit).length >= 8 && profileAcknowledged && rentAcknowledged &&
        missingDocs.isEmpty() && current.optBoolean("can_accept", current.optString("status") == "pending")

    Column(Modifier.fillMaxSize().background(SE.Background).padding(horizontal = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = SE.Ink,
                modifier = Modifier.size(22.dp).clickable(onClick = onBack))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Your registration invitation", color = SE.Ink, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Confirm your profile, bed and first rent amount before submitting.", color = SE.Muted, fontSize = 11.sp)
            }
            TextButton(onClick = { refresh++ }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }
        HorizontalDivider(color = SE.Border)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sessionToken.isNullOrBlank()) {
                Card(shape = SE.cardShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.OrangeSoft)) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Sign in with the email address the property owner invited before submitting registration. You can review a pasted link, but only the invited account can accept it.",
                            color = SE.Ink, fontSize = 12.sp, lineHeight = 17.sp)
                        Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("Sign in to continue") }
                    }
                }
            }
            Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Open a share link", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    OutlinedTextField(
                        value = pastedLink, onValueChange = { pastedLink = it.take(500) },
                        modifier = Modifier.fillMaxWidth(), label = { Text("StayEasy invitation link or token") },
                        leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) }, singleLine = true
                    )
                    Button(
                        onClick = { loadByToken() }, enabled = !loading && baseUrl.isNotBlank() && pastedLink.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (loading) "Loading invitation…" else "Load invitation") }
                }
            }
            if (invitations.isNotEmpty()) {
                Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Invitations linked to your account", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        invitations.forEach { row ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                    .background(if (current?.optLong("id") == row.optLong("id")) SE.BlueSoft else SE.Background)
                                    .clickable { selectAccountInvitation(row) }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Description, null, tint = SE.Blue)
                                Spacer(Modifier.size(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(invitationText(row, "property_name", "Property invitation"), color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("Room ${invitationText(row, "room_code")} · ${invitationText(row, "unit_code")} · Move-in ${invitationText(row, "move_in_date")}",
                                        color = SE.Muted, fontSize = 10.sp)
                                }
                                Text("Open", color = SE.Blue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (error.isNotBlank()) {
                Card(shape = SE.cardShape(14.dp), colors = CardDefaults.cardColors(containerColor = SE.RedSoft)) {
                    Text(error, Modifier.padding(12.dp), color = SE.Red, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }

            if (current != null) {
                Card(shape = SE.cardShape(20.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(invitationText(current, "property_name", "Property invitation"), color = SE.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("${invitationText(current, "floor_label")} · Room ${invitationText(current, "room_code")} · Bed ${invitationText(current, "unit_code")}",
                            color = SE.Muted, fontSize = 12.sp)
                        Text("Proposed move-in: ${invitationText(current, "move_in_date")} · Expires: ${invitationText(current, "expires_at").take(10)}",
                            color = SE.Muted, fontSize = 11.sp)
                        if (invitationText(current, "reassignment_reason").isNotBlank()) {
                            Text("Owner note: ${invitationText(current, "reassignment_reason")}", color = SE.Orange, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f).background(SE.BlueSoft, SE.cardShape(12.dp)).padding(10.dp)) {
                                Text("Monthly rent", color = SE.Muted, fontSize = 10.sp)
                                Text("₹${String.format(Locale.US, "%,d", monthlyRent)}", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                            Column(Modifier.weight(1f).background(SE.OrangeSoft, SE.cardShape(12.dp)).padding(10.dp)) {
                                Text("Refundable deposit", color = SE.Muted, fontSize = 10.sp)
                                Text("₹${String.format(Locale.US, "%,d", deposit)}", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                        }
                        if (invitationText(current, "unit_status") != "available") {
                            Text("This bed is no longer available. Do not submit; contact the owner for another assignment.", color = SE.Red, fontSize = 12.sp)
                        }
                    }
                }

                Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Your tenant profile", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Only provide the information needed for this stay. Government ID or Aadhaar is not required unless the owner has disclosed a specific request in this invitation.", color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                        OutlinedTextField(phone, { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' }.take(30) },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Mobile number *") }, singleLine = true)
                        OutlinedTextField(emergencyName, { emergencyName = it.take(180) },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Emergency contact name (optional)") }, singleLine = true)
                        OutlinedTextField(emergencyPhone, { emergencyPhone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' || ch == '-' }.take(30) },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Emergency contact phone (optional)") }, singleLine = true)
                        OutlinedTextField(accommodationNotes, { accommodationNotes = it.take(500) },
                            modifier = Modifier.fillMaxWidth(), label = { Text("Accommodation preferences (optional)") }, minLines = 2)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = profileAcknowledged, onCheckedChange = { profileAcknowledged = it })
                            Text("I reviewed my profile and confirm these details are accurate.", color = SE.Ink, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }

                if (requiredDocs.isNotEmpty()) {
                    Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Requested registration documents", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(invitationText(current, "document_request_reason"), color = SE.Muted, fontSize = 12.sp, lineHeight = 17.sp)
                            requiredDocs.forEach { category ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(if (category in submittedCategories) Icons.Filled.CheckCircle else Icons.Filled.Description, null,
                                        tint = if (category in submittedCategories) SE.Blue else SE.Orange)
                                    Spacer(Modifier.size(7.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(documentLabel(category), color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                        Text(if (category in submittedCategories) "Uploaded / under owner review" else "Required by this invitation",
                                            color = SE.Muted, fontSize = 10.sp)
                                    }
                                }
                            }
                            OutlinedButton(onClick = { selectedCategory = requiredDocs.firstOrNull { it !in submittedCategories } ?: requiredDocs.first(); picker.launch(arrayOf("application/pdf", "image/png", "image/jpeg")) },
                                enabled = !sessionToken.isNullOrBlank() && !submitting, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.UploadFile, null); Spacer(Modifier.size(7.dp)); Text("Choose PDF or image")
                            }
                            if (selectedUri != null) {
                                Text("Selected: ${selectedUri?.lastPathSegment?.takeLast(90)}", color = SE.Ink, fontSize = 11.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    requiredDocs.forEach { category ->
                                        FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category },
                                            label = { Text(documentLabel(category), fontSize = 10.sp) })
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = documentConsent, onCheckedChange = { documentConsent = it })
                                    Text("I consent to upload this document for the disclosed purpose and understand access/retention are limited.", color = SE.Ink, fontSize = 10.sp, lineHeight = 14.sp)
                                }
                                Button(
                                    enabled = !submitting && !sessionToken.isNullOrBlank() && documentConsent &&
                                        selectedCategory in requiredDocs && invitationText(current, "document_request_reason").length >= 10,
                                    onClick = {
                                        val id = current.optLong("id")
                                        val uri = selectedUri
                                        if (uri == null) return@Button
                                        submitting = true
                                        scope.launch {
                                            try {
                                                val saved = withContext(Dispatchers.IO) {
                                                    StayApiClient.uploadInvitationDocument(
                                                        context, baseUrl, sessionToken!!, id, selectedCategory,
                                                        invitationText(current, "document_request_reason"), documentConsent, uri
                                                    )
                                                }
                                                documents = withContext(Dispatchers.IO) { StayApiClient.tenantInvitationDocuments(baseUrl, sessionToken!!, id) }
                                                selectedUri = null
                                                documentConsent = false
                                                notify("Document uploaded securely. Owner review status: ${invitationText(saved, "status", "submitted")}.")
                                            } catch (e: Exception) {
                                                error = e.message ?: "Document upload failed. Check the disclosed request and file type."
                                            } finally { submitting = false }
                                        }
                                    }, modifier = Modifier.fillMaxWidth()
                                ) { Text(if (submitting) "Uploading…" else "Upload with consent") }
                            }
                            documents.forEach { doc ->
                                Text("${documentLabel(invitationText(doc, "category"))} · ${invitationText(doc, "status")} · retention until ${invitationText(doc, "retention_until").take(10)}",
                                    color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                                if (invitationText(doc, "review_note").isNotBlank()) {
                                    Text("Owner note: ${invitationText(doc, "review_note")}", color = SE.Orange, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                } else {
                    Card(shape = SE.cardShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.BlueSoft)) {
                        Text("No registration documents were requested for this invitation. You do not need to upload sensitive documents to proceed.",
                            Modifier.padding(12.dp), color = SE.Ink, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }

                Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("First-period prorated rent", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("The server calculates the first rent period from the billable date you confirm. The date does not silently move if the owner reviews your request later.", color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                        if (quote != null) {
                            Text("Billable start: ${invitationText(quote, "billable_start_date")}", color = SE.Ink, fontSize = 12.sp)
                            Text("${quote.optInt("billable_days")} of ${quote.optInt("days_in_billing_month")} days in this month", color = SE.Muted, fontSize = 11.sp)
                            Text("First-period rent: ₹${invitationText(quote, "first_period_rent")} (INR)", color = SE.Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text("Daily rate: ₹${invitationText(quote, "daily_rate")}", color = SE.Muted, fontSize = 11.sp)
                        } else {
                            Text("Refresh the invitation to retrieve the current server quote.", color = SE.Muted, fontSize = 11.sp)
                        }
                        Text("Registration submission records the agreed rent charge in the ledger. No money is collected automatically.", color = SE.Orange, fontSize = 11.sp, lineHeight = 16.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = rentAcknowledged, onCheckedChange = { rentAcknowledged = it })
                            Text("I confirm the billable start date and the first-period rent shown above.", color = SE.Ink, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }
                Button(enabled = canSubmit, onClick = {
                    submitting = true
                    error = ""
                    scope.launch {
                        try {
                            val submitted = withContext(Dispatchers.IO) {
                                StayApiClient.acceptTenantInvitation(
                                    baseUrl, sessionToken!!, invitationToken, invitationId,
                                    invitationText(quote, "billable_start_date"),
                                    rentAcknowledged, phone, emergencyName, emergencyPhone,
                                    JSONObject().put("notes", accommodationNotes.trim()), profileAcknowledged
                                )
                            }
                            result = submitted
                            invitation = null
                            invitationToken = null
                            invitationId = null
                            documents = emptyList()
                            invitations = withContext(Dispatchers.IO) { StayApiClient.tenantInvitations(baseUrl, sessionToken!!) }
                            notify("Registration submitted. No payment was collected.")
                        } catch (e: Exception) {
                            error = e.message ?: "Registration could not be submitted. Refresh and try again."
                            if (error.contains("invitation", ignoreCase = true) || error.contains("billable", ignoreCase = true)) rentAcknowledged = false
                        } finally { submitting = false }
                    }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (submitting) "Submitting registration…" else "Confirm and submit registration")
                }
            }

            val submitted = result
            if (submitted != null) {
                Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.BlueSoft)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = SE.Blue, modifier = Modifier.size(30.dp))
                        Text("Registration submitted", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(invitationText(submitted, "message"), color = SE.Ink, fontSize = 12.sp, lineHeight = 17.sp)
                        Text("Reservation #${submitted.optLong("reservation_id")} · first rent charge: ₹${invitationText(submitted.optJSONObject("billable_quote"), "first_period_rent")}",
                            color = SE.Muted, fontSize = 11.sp)
                        Text("The owner still needs to review the registration. Check your invitation/arrival list for updates.", color = SE.Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
