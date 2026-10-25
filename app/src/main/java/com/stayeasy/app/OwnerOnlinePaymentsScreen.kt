
package com.stayeasy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.util.Locale
import java.util.UUID

private fun ownerPaymentText(value: JSONObject, key: String, fallback: String = ""): String =
    value.optString(key, fallback).takeUnless { it == "null" } ?: fallback

private fun ownerPaymentMoney(paise: Long): String =
    "₹" + String.format(Locale.US, "%,.2f", paise / 100.0)

@Composable
internal fun OwnerOnlinePaymentsScreen(
    baseUrl: String,
    token: String?,
    onBack: () -> Unit,
    notify: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var payments by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var refresh by remember { mutableIntStateOf(0) }
    var busyAttemptId by remember { mutableLongStateOf(0L) }
    var refundTarget by remember { mutableStateOf<JSONObject?>(null) }
    var refundAmountRupees by remember { mutableStateOf("") }
    var refundReason by remember { mutableStateOf("") }

    LaunchedEffect(baseUrl, token, refresh) {
        if (baseUrl.isBlank() || token.isNullOrBlank()) {
            payments = emptyList()
            error = "Connect the app to the API and sign in with an approved owner or finance account to review real online payments."
            loading = false
        } else {
            loading = true
            error = ""
            try {
                payments = withContext(Dispatchers.IO) { StayApiClient.ownerPaymentHistory(baseUrl, token) }
            } catch (e: Exception) {
                error = e.message ?: "Could not load online payment history."
                payments = emptyList()
            } finally {
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(SE.Background).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                Spacer(Modifier.width(5.dp))
                Text("Rent due")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { refresh++ }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                Text("Refresh")
            }
        }
        Text("Online payments", color = SE.Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Provider-confirmed collections and refund reconciliation for your properties.", color = SE.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(13.dp))
        Card(shape = SE.cardShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Payments, contentDescription = null, tint = SE.Blue)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("Payment integrity", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Captured payments and refunds are verified against Razorpay. Pending is not the same as paid.", color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (loading) {
            Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = SE.Blue)
            }
        }
        if (error.isNotBlank()) {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SE.OrangeSoft), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(error, color = SE.Ink, fontSize = 12.sp)
                    TextButton(onClick = { refresh++ }) { Text("Try again", color = SE.Blue) }
                }
            }
        } else if (!loading && payments.isEmpty()) {
            Card(shape = SE.cardShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Payments, contentDescription = null, tint = SE.Muted)
                    Spacer(Modifier.height(7.dp))
                    Text("No online payments yet", color = SE.Ink, fontWeight = FontWeight.Bold)
                    Text("Provider-backed transactions will appear here after a tenant checks out.", color = SE.Muted, fontSize = 11.sp)
                }
            }
        }
        if (payments.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(9.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 18.dp)
            ) {
                items(payments, key = { it.optLong("id") }) { payment ->
                    val attemptId = payment.optLong("id")
                    val status = ownerPaymentText(payment, "status", "unknown")
                    val captured = status in setOf("captured", "captured_unapplied")
                    val statusLabel = when (status) {
                        "captured" -> "Captured · provider confirmed"
                        "captured_unapplied" -> "Captured · finance review needed"
                        "authorized" -> "Authorized · awaiting capture"
                        "created", "creating" -> "Pending · not paid"
                        "unknown" -> "Unknown · reconcile with provider"
                        "failed" -> "Failed"
                        "refunded" -> "Refunded"
                        else -> status.replace('_', ' ')
                    }
                    Card(shape = SE.cardShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(ownerPaymentText(payment, "property_name", "Property"), color = SE.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Payment #$attemptId · Charge #${payment.optLong("charge_id")}", color = SE.Muted, fontSize = 10.sp)
                                }
                                Text(ownerPaymentMoney(payment.optLong("amount_paise")), color = SE.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                            }
                            Text(statusLabel, color = if (captured) SE.Blue else SE.Orange, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            val orderId = ownerPaymentText(payment, "provider_order_id")
                            val paymentId = ownerPaymentText(payment, "provider_payment_id")
                            if (orderId.isNotBlank()) Text("Order: $orderId", color = SE.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (paymentId.isNotBlank()) Text("Provider payment: $paymentId", color = SE.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            val refunds = payment.optJSONArray("refunds")
                            if (refunds != null) {
                                for (i in 0 until refunds.length()) {
                                    val refund = refunds.optJSONObject(i) ?: continue
                                    val refundState = ownerPaymentText(refund, "status", "unknown")
                                    Text(
                                        "Refund #${refund.optLong("id")} · ${ownerPaymentMoney(refund.optLong("amount_paise"))} · ${refundState.replace('_', ' ')}",
                                        color = if (refundState == "processed") SE.Blue else SE.Muted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            if (captured) {
                                OutlinedButton(
                                    enabled = busyAttemptId == 0L,
                                    onClick = {
                                        refundTarget = payment
                                        refundAmountRupees = ""
                                        refundReason = ""
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Request full or partial refund") }
                            }
                        }
                    }
                }
            }
        }
        Text(
            "Refunds are requests until Razorpay confirms processing. A processed refund creates a reversal in the rent ledger and may reopen an outstanding balance.",
            color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 9.dp, bottom = 8.dp)
        )
    }

    val selected = refundTarget
    if (selected != null) {
        AlertDialog(
            onDismissRequest = { if (busyAttemptId == 0L) refundTarget = null },
            title = { Text("Request refund", color = SE.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${ownerPaymentText(selected, "property_name", "Property")} · Payment #${selected.optLong("id")}", color = SE.Muted, fontSize = 12.sp)
                    Text("Captured: ${ownerPaymentMoney(selected.optLong("amount_paise"))}", color = SE.Ink, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = refundAmountRupees,
                        onValueChange = { value -> refundAmountRupees = value.filter { it.isDigit() || it == '.' }.take(12) },
                        label = { Text("Refund amount in ₹ (blank = full remaining amount)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = refundReason,
                        onValueChange = { refundReason = it.take(500) },
                        label = { Text("Reason (required)") },
                        minLines = 2,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("The server checks the remaining refundable amount. A request is not a completed refund.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                }
            },
            confirmButton = {
                Button(
                    enabled = busyAttemptId == 0L && refundReason.trim().length >= 5,
                    onClick = {
                        val rawAmount = refundAmountRupees.trim()
                        val amountPaise: Long? = if (rawAmount.isBlank()) null else runCatching {
                            BigDecimal(rawAmount).movePointRight(2).longValueExact()
                        }.getOrNull()
                        if (rawAmount.isNotBlank() && (amountPaise == null || amountPaise < 100L)) {
                            notify("Enter a valid refund amount of at least ₹1.00, or leave it blank for the full available amount.")
                            return@Button
                        }
                        val id = selected.optLong("id")
                        busyAttemptId = id
                        scope.launch {
                            try {
                                val created = withContext(Dispatchers.IO) {
                                    StayApiClient.requestOnlineRefund(
                                        baseUrl, token.orEmpty(), id, amountPaise,
                                        refundReason.trim(), UUID.randomUUID().toString()
                                    )
                                }
                                notify("Refund request recorded as ${ownerPaymentText(created, "status", "requested")}. It is complete only after provider confirmation.")
                                refundTarget = null
                                refresh++
                            } catch (e: Exception) {
                                notify("Refund request failed or needs reconciliation. Check the transaction before submitting a new request. ${e.message.orEmpty()}")
                                refresh++
                            } finally {
                                busyAttemptId = 0L
                            }
                        }
                    }
                ) { Text(if (busyAttemptId != 0L) "Submitting…" else "Submit refund request") }
            },
            dismissButton = {
                TextButton(enabled = busyAttemptId == 0L, onClick = { refundTarget = null }) { Text("Cancel") }
            },
            containerColor = SE.Surface
        )
    }
}
