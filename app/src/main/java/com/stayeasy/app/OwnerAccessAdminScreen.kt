package com.stayeasy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
internal fun AdminOwnerRequestsScreen(
    baseUrl: String,
    onBack: () -> Unit,
    notify: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf("pending_approval") }
    var refreshKey by remember { mutableIntStateOf(0) }
    var requests by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf("") }
    var selectedRequest by remember { mutableStateOf<JSONObject?>(null) }
    var selectedDecision by remember { mutableStateOf("") }
    var decisionNote by remember { mutableStateOf("") }
    var decisionError by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }

    LaunchedEffect(baseUrl, filter, refreshKey) {
        loading = true
        loadError = ""
        val token = if (baseUrl.isNotBlank()) StayApiClient.currentSessionToken(baseUrl) else null
        if (baseUrl.isBlank()) {
            requests = emptyList()
            loadError = "Configure the StayEasy API connection before reviewing access requests."
            loading = false
        } else if (token == null) {
            requests = emptyList()
            loadError = "Sign in with an administrator account to review owner requests."
            loading = false
        } else {
            try {
                requests = withContext(Dispatchers.IO) {
                    StayApiClient.ownerAccessRequests(baseUrl, token, filter)
                }
            } catch (e: Exception) {
                requests = emptyList()
                loadError = e.message ?: "Could not load owner access requests."
            } finally {
                loading = false
            }
        }
    }

    Column(
        Modifier.fillMaxSize().background(SE.Background).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onBack,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
                Text("Back")
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Owner access", color = SE.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Review requests before granting Owner privileges", color = SE.Muted, fontSize = 10.sp)
            }
            OutlinedButton(
                onClick = { refreshKey++ },
                enabled = !loading && !submitting,
                contentPadding = PaddingValues(10.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(17.dp))
            }
        }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Request queue", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Pending" to "pending_approval", "Approved" to "approved", "Rejected" to "rejected", "All" to "all").forEach { (label, value) ->
                        FilterChip(
                            selected = filter == value,
                            onClick = { filter = value },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        if (loading) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = SE.Blue, strokeWidth = 2.dp)
                Spacer(Modifier.width(9.dp))
                Text("Loading requests from StayEasy…", color = SE.Muted, fontSize = 12.sp)
            }
        }
        if (loadError.isNotBlank()) {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SE.RedSoft)) {
                Text(loadError, color = SE.Red, fontSize = 11.sp, modifier = Modifier.padding(13.dp))
            }
        }
        if (!loading && loadError.isBlank() && requests.isEmpty()) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
                Column(Modifier.padding(16.dp)) {
                    Text("No requests in this view", color = SE.Ink, fontWeight = FontWeight.Bold)
                    Text(
                        "New Owner requests appear after an applicant verifies their email and requests access.",
                        color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(bottom = 18.dp)
        ) {
            items(requests, key = { it.optString("id") }) { item ->
                val status = item.optString("owner_access_status", "pending_approval")
                val verified = item.optBoolean("email_verified", false)
                val active = item.optBoolean("account_active", false)
                Card(
                    shape = RoundedCornerShape(17.dp),
                    colors = CardDefaults.cardColors(containerColor = SE.Surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(item.optString("display_name", "Applicant"), color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(item.optString("email"), color = SE.Muted, fontSize = 11.sp)
                            }
                            val statusColor = if (status == "pending_approval") SE.Orange else SE.Blue
                            val statusBg = if (status == "pending_approval") SE.OrangeSoft else SE.BlueSoft
                            Card(shape = RoundedCornerShape(9.dp), colors = CardDefaults.cardColors(containerColor = statusBg)) {
                                Text(
                                    status.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                    color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                        Text(
                            if (verified) "Email verified" else "Email verification required",
                            color = if (verified) SE.Blue else SE.Orange,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                        val requestedAt = item.optString("requested_at")
                        if (requestedAt.isNotBlank()) Text("Requested: $requestedAt", color = SE.Muted, fontSize = 9.sp)
                        val reviewNote = item.optString("review_note")
                        if (reviewNote.isNotBlank()) Text("Review note: $reviewNote", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)

                        if (status == "pending_approval") {
                            if (!active) Text("This account is suspended and cannot be approved.", color = SE.Red, fontSize = 10.sp)
                            else if (!verified) Text("Approve becomes available after the applicant verifies their email.", color = SE.Muted, fontSize = 10.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        selectedRequest = item
                                        selectedDecision = "approve"
                                        decisionNote = ""
                                        decisionError = ""
                                    },
                                    enabled = verified && active && !submitting,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(11.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Approve access", fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = {
                                        selectedRequest = item
                                        selectedDecision = "reject"
                                        decisionNote = ""
                                        decisionError = ""
                                    },
                                    enabled = active && !submitting,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(11.dp)
                                ) {
                                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text("Reject request", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val candidate = selectedRequest
    if (candidate != null) {
        val approving = selectedDecision == "approve"
        AlertDialog(
            onDismissRequest = { if (!submitting) selectedRequest = null },
            title = { Text(if (approving) "Approve Owner access?" else "Reject Owner request?", color = SE.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        if (approving) {
                            "This promotes ${candidate.optString("display_name")} to the server-authorized Owner role. Their current session is invalidated and they must sign in again."
                        } else {
                            "The applicant remains a customer. They can review the note and re-submit a request."
                        },
                        color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = decisionNote,
                        onValueChange = { decisionNote = it.take(500); decisionError = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Review note (optional)") },
                        placeholder = { Text(if (approving) "Verified identity and property details" else "What should the applicant address?") },
                        minLines = 2,
                        maxLines = 4
                    )
                    if (decisionError.isNotBlank()) Text(decisionError, color = SE.Red, fontSize = 11.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!submitting) {
                            submitting = true
                            decisionError = ""
                            scope.launch {
                                try {
                                    val token = StayApiClient.currentSessionToken(baseUrl)
                                        ?: throw IllegalStateException("Administrator session expired. Sign in again.")
                                    withContext(Dispatchers.IO) {
                                        StayApiClient.decideOwnerAccessRequest(
                                            baseUrl, token, candidate.optString("id"), selectedDecision, decisionNote
                                        )
                                    }
                                    notify(if (approving) "Owner access approved. The applicant must sign in again." else "Owner request rejected and review note saved.")
                                    selectedRequest = null
                                    refreshKey++
                                } catch (e: Exception) {
                                    decisionError = e.message ?: "Could not save the decision."
                                } finally {
                                    submitting = false
                                }
                            }
                        }
                    },
                    enabled = !submitting,
                    colors = ButtonDefaults.buttonColors(containerColor = if (approving) SE.Blue else SE.Red)
                ) {
                    Text(if (submitting) "Saving…" else if (approving) "Confirm approval" else "Confirm rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!submitting) selectedRequest = null }, enabled = !submitting) {
                    Text("Cancel", color = SE.Muted)
                }
            },
            containerColor = SE.Surface
        )
    }
}
