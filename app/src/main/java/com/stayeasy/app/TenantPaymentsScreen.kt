package com.stayeasy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import java.util.Locale
import java.util.UUID

private fun tenantMoneyPaise(paise: Long): String =
    "₹" + String.format(Locale.US, "%,.2f", paise / 100.0)

private fun JSONObject.tenantText(key: String, fallback: String = ""): String =
    optString(key, fallback).takeUnless { it == "null" } ?: fallback

@Composable
internal fun TenantPaymentsScreen(
    baseUrl: String,
    token: String?,
    refreshToken: Int,
    onCheckout: (JSONObject) -> Unit,
    onBack: () -> Unit,
    notify: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var charges by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var paymentHistory by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var busyChargeId by remember { mutableLongStateOf(0L) }
    var refreshingAttemptId by remember { mutableLongStateOf(0L) }
    var loadCycle by remember { mutableIntStateOf(0) }

    LaunchedEffect(baseUrl, token, refreshToken, loadCycle) {
        busyChargeId = 0L
        if (baseUrl.isBlank() || token.isNullOrBlank()) {
            charges = emptyList()
            paymentHistory = emptyList()
            loading = false
            error = "Sign in with your verified tenant account and connect a staging or production API to view actual rent charges."
        } else {
            loading = true
            error = ""
            try {
                val result = withContext(Dispatchers.IO) {
                    StayApiClient.payableCharges(baseUrl, token) to StayApiClient.myPayments(baseUrl, token)
                }
                charges = result.first
                paymentHistory = result.second
            } catch (e: Exception) {
                error = e.message ?: "Could not load your rent charges. Retry to check the latest status."
            } finally {
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(SE.Background).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                Spacer(Modifier.width(4.dp))
                androidx.compose.material3.Text("Back")
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { loadCycle++ }) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                androidx.compose.material3.Text("Refresh")
            }
        }
        Text("Rent & payments", color = SE.Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("Your actual charges, secure checkout and payment history.", color = SE.Muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))

        Card(shape = SE.cardShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = SE.Blue)
                    Spacer(Modifier.width(8.dp))
                    Text("Secure online payment", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text(
                    "Checkout is handled by Razorpay. StayEasy confirms payment with the provider before marking a charge paid. Never share your UPI PIN or card PIN with anyone.",
                    color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp
                )
            }
        }
        Spacer(Modifier.height(13.dp))

        if (loading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SE.Blue)
            }
        }
        if (error.isNotBlank()) {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SE.OrangeSoft), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(error, color = SE.Ink, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { loadCycle++ }) { Text("Try again", color = SE.Blue) }
                }
            }
        }

        Text("Amount due", color = SE.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        when {
            loading -> Unit
            charges.isEmpty() && error.isBlank() -> Card(
                shape = SE.cardShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SE.Blue)
                    Spacer(Modifier.height(7.dp))
                    Text("You're all caught up", color = SE.Ink, fontWeight = FontWeight.Bold)
                    Text("No open online-payable charges were found.", color = SE.Muted, fontSize = 11.sp)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(charges, key = { it.optLong("id") }) { charge ->
                    val id = charge.optLong("id")
                    val amountDue = charge.optLong("amount_due_paise")
                    val isBusy = busyChargeId == id
                    Card(shape = SE.cardShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(charge.tenantText("property_name", "Accommodation"), color = SE.Ink, fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        listOf(charge.tenantText("category").replace('_', ' ').replaceFirstChar { it.uppercase() },
                                            charge.tenantText("billing_period")).filter { it.isNotBlank() }.joinToString(" · "),
                                        color = SE.Muted, fontSize = 11.sp
                                    )
                                }
                                Text(tenantMoneyPaise(amountDue), color = SE.Ink, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            val dueDate = charge.tenantText("due_date")
                            if (dueDate.isNotBlank()) Text("Due date · $dueDate", color = SE.Muted, fontSize = 11.sp)
                            Button(
                                onClick = {
                                    if (id <= 0L || busyChargeId != 0L) return@Button
                                    busyChargeId = id
                                    scope.launch {
                                        try {
                                            val order = withContext(Dispatchers.IO) {
                                                StayApiClient.createPaymentOrder(baseUrl, token.orEmpty(), id, UUID.randomUUID().toString())
                                            }
                                            notify("Secure checkout is opening. The amount comes from your server-side rent balance.")
                                            onCheckout(order)
                                        } catch (e: Exception) {
                                            notify(e.message ?: "Checkout could not be started. No payment was confirmed.")
                                            busyChargeId = 0L
                                            loadCycle++
                                        }
                                    }
                                },
                                enabled = busyChargeId == 0L && !loading,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)
                            ) {
                                if (isBusy) {
                                    CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp,
                                        modifier = Modifier.width(18.dp).height(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Preparing secure checkout")
                                } else {
                                    Icon(Icons.Filled.Payments, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Pay ${tenantMoneyPaise(amountDue)}")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(15.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.History, contentDescription = null, tint = SE.Blue)
            Spacer(Modifier.width(7.dp))
            Text("Payment history", color = SE.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(7.dp))
        if (paymentHistory.isEmpty() && !loading && error.isBlank()) {
            Text("No online payment attempts yet.", color = SE.Muted, fontSize = 11.sp)
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 12.dp)
            ) {
                items(paymentHistory, key = { it.optLong("id") }) { payment ->
                    val state = payment.tenantText("status", "unknown")
                    val stateLabel = when (state) {
                        "captured" -> "Paid · provider confirmed"
                        "captured_unapplied" -> "Captured · reconciliation needed"
                        "authorized" -> "Authorized · waiting for capture"
                        "created", "creating" -> "Payment not confirmed"
                        "unknown" -> "Status needs reconciliation"
                        "failed" -> "Failed"
                        "refunded" -> "Refunded"
                        else -> state.replace('_', ' ')
                    }
                    Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(payment.tenantText("property_name", "Accommodation"), color = SE.Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Payment #${payment.optLong("id")} · $stateLabel", color = SE.Muted, fontSize = 10.sp)
                                }
                                Text(tenantMoneyPaise(payment.optLong("amount_paise")), color = SE.Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            if (state in setOf("created", "authorized", "unknown")) {
                                OutlinedButton(
                                    enabled = refreshingAttemptId == 0L,
                                    onClick = {
                                        val attemptId = payment.optLong("id")
                                        if (attemptId <= 0L || refreshingAttemptId != 0L) return@OutlinedButton
                                        refreshingAttemptId = attemptId
                                        scope.launch {
                                            try {
                                                val result = withContext(Dispatchers.IO) {
                                                    StayApiClient.refreshPaymentStatus(baseUrl, token.orEmpty(), attemptId)
                                                }
                                                val latest = result.optJSONObject("payment")
                                                if (result.optBoolean("ledger_posted") && latest?.optString("status") == "captured") {
                                                    notify("Razorpay confirmed this payment and StayEasy recorded it.")
                                                } else if (latest?.optString("status") == "captured_unapplied") {
                                                    notify("Razorpay confirms the money was captured, but finance review is required. Do not pay again.")
                                                } else {
                                                    notify(result.optString("message", "Status refreshed. The payment is not confirmed as captured; do not pay again until you review the latest status."))
                                                }
                                            } catch (e: Exception) {
                                                notify("StayEasy could not resolve the payment yet. Do not pay again; retry status check or wait for the verified webhook. ${e.message.orEmpty()}")
                                            } finally {
                                                refreshingAttemptId = 0L
                                                loadCycle++
                                            }
                                        }
                                    }
                                ) {
                                    if (refreshingAttemptId == payment.optLong("id")) {
                                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.width(16.dp).height(16.dp))
                                        Spacer(Modifier.width(7.dp))
                                        Text("Checking with provider…")
                                    } else Text("Check payment status")
                                }
                            }
                            val refunds = payment.optJSONArray("refunds")
                            if (refunds != null) {
                                for (i in 0 until refunds.length()) {
                                    val refund = refunds.optJSONObject(i) ?: continue
                                    Text(
                                        "Refund #${refund.optLong("id")} · ${tenantMoneyPaise(refund.optLong("amount_paise"))} · ${refund.tenantText("status")}",
                                        color = SE.Muted, fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Text(
            "A checkout success message is not a receipt. Use Check payment status to reconcile with Razorpay; general Refresh reloads StayEasy's latest record.",
            color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
    }
}
