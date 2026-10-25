package com.stayeasy.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Real email/password account flow backed by /auth/register, /auth/verify-email,
 * /auth/login and /auth/logout. The requested workspace is only a request; the API
 * returns the server-approved role and the parent chooses the actual workspace.
 */
@Composable
internal fun AccountAuthScreen(
    baseUrl: String,
    account: JSONObject?,
    startMode: String,
    initialWorkspace: String,
    onBack: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenAdminOwnerRequests: () -> Unit,
    onSignedIn: (JSONObject) -> Unit,
    onLoginSucceeded: (JSONObject) -> Unit,
    onSignedOut: () -> Unit
) {
    var mode by rememberSaveable { mutableStateOf(startMode) }
    var workspace by rememberSaveable { mutableStateOf(initialWorkspace) }
    var displayName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var verificationCode by rememberSaveable { mutableStateOf("") }
    var verificationPending by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(startMode, initialWorkspace) {
        mode = startMode
        workspace = initialWorkspace
        error = ""
        message = ""
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onBack, enabled = !busy, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                Text("Back", modifier = Modifier.padding(start = 6.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(if (workspace == "owner") "OWNER" else "TENANT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.primary)
        }

        Text(
            if (account != null) "Your StayEasy account" else if (verificationPending) "Verify your email" else if (mode == "signup") "Create your account" else "Sign in to StayEasy",
            fontSize = 27.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold, color = colors.onBackground
        )
        Text(
            if (account != null) "Your account identity and permissions come from the StayEasy server."
            else if (verificationPending) "Enter the six-digit code sent to $email. In local testing, open the Mailpit inbox on your computer."
            else if (mode == "signup") "Create a real account. You'll verify your email before signing in."
            else "Use the email and password you registered with. Existing accounts are signed in—not duplicated.",
            fontSize = 13.sp, lineHeight = 19.sp, color = colors.onSurfaceVariant
        )

        if (baseUrl.isBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Connect your StayEasy API first", fontWeight = FontWeight.Bold, color = colors.onSurface)
                    Text("Registration and sign-in use the backend. For the Android emulator, start the local test stack and use http://10.0.2.2:8000.", fontSize = 12.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant)
                    Button(onClick = onOpenConnection, modifier = Modifier.fillMaxWidth()) { Text("Configure API connection") }
                }
            }
        } else if (account != null) {
            val role = account.optString("role", "customer")
            val ownerStatus = account.optString("owner_access_status", "not_requested")
            Card(colors = CardDefaults.cardColors(containerColor = colors.surface), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = colors.primary)
                        Text(account.optString("display_name", "StayEasy user"), modifier = Modifier.padding(start = 10.dp), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.onSurface)
                    }
                    Text(account.optString("email"), fontSize = 13.sp, color = colors.onSurfaceVariant)
                    Text("Account role: ${role.uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                    if (role == "customer" && account.optString("requested_workspace") == "owner") {
                        Text("Owner access: ${ownerStatus.replace('_', ' ')}", fontSize = 12.sp, color = colors.onSurfaceVariant)
                    }
                }
            }

            if (role == "admin") {
                Button(onClick = onOpenAdminOwnerRequests, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Apartment, contentDescription = null)
                    Text("Review Owner access requests", modifier = Modifier.padding(start = 8.dp))
                }
            } else if (role == "customer") {
                if (ownerStatus == "pending_approval") {
                    Text("Your Owner request is waiting for an administrator. Your account remains in Tenant mode until approved.", fontSize = 12.sp, lineHeight = 17.sp, color = colors.onSurfaceVariant)
                } else {
                    OutlinedButton(
                        onClick = {
                            val token = StayApiClient.currentSessionToken(baseUrl)
                            if (token.isNullOrBlank()) {
                                error = "Your sign-in session has expired. Sign in again."
                            } else {
                                busy = true
                                error = ""
                                scope.launch {
                                    try {
                                        val updated = withContext(Dispatchers.IO) { StayApiClient.requestOwnerAccess(baseUrl, token) }
                                        onSignedIn(updated)
                                        message = "Owner access request submitted. An administrator must approve it."
                                    } catch (e: Exception) {
                                        error = e.message ?: "Owner access request was not submitted."
                                    } finally {
                                        busy = false
                                    }
                                }
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "Please wait…" else "Request Owner access") }
                }
            }

            OutlinedButton(
                onClick = {
                    busy = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { StayApiClient.logoutAccount(context = context, baseUrl = baseUrl) }
                            onSignedOut()
                        } catch (e: Exception) {
                            // Owner approval invalidates the old token server-side. Even if the
                            // resulting 401 prevents remote logout, StayApiClient has already
                            // cleared the device token, so return to the signed-out entry screen.
                            onSignedOut()
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (busy) "Signing out…" else "Sign out") }
        } else if (baseUrl.isNotBlank()) {
            if (verificationPending) {
                OutlinedTextField(value = email, onValueChange = { email = it.trim().take(255) }, label = { Text("Email address") }, leadingIcon = { Icon(Icons.Filled.Mail, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = verificationCode, onValueChange = { verificationCode = it.filter(Char::isDigit).take(6); error = "" }, label = { Text("6-digit verification code") }, leadingIcon = { Icon(Icons.Filled.CheckCircle, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (verificationCode.length != 6) { error = "Enter the six-digit code from your email."; return@Button }
                        busy = true; error = ""
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { StayApiClient.verifyAccountEmail(baseUrl, email, verificationCode) }
                                val result = withContext(Dispatchers.IO) { StayApiClient.loginAccount(context, baseUrl, email, password) }
                                verificationPending = false
                                val user = result.optJSONObject("user") ?: JSONObject()
                                onLoginSucceeded(user)
                            } catch (e: Exception) {
                                error = e.message ?: "Email verification failed. Check the code and try again."
                            } finally { busy = false }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "Verifying…" else "Verify email and continue") }
                TextButton(onClick = { verificationPending = false; mode = "signin"; message = ""; error = "" }, enabled = !busy) { Text("Back to sign in") }
            } else {
                if (mode == "signup") {
                    OutlinedTextField(value = displayName, onValueChange = { displayName = it.take(180); error = "" }, label = { Text("Full name") }, leadingIcon = { Icon(Icons.Filled.Person, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = email, onValueChange = { email = it.trim().take(255); error = "" }, label = { Text("Email address") }, leadingIcon = { Icon(Icons.Filled.Mail, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (mode == "signup") {
                    OutlinedTextField(value = phone, onValueChange = { phone = it.take(40); error = "" }, label = { Text("Phone (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(value = password, onValueChange = { password = it.take(128); error = "" }, label = { Text("Password (12+ characters for registration)") }, leadingIcon = { Icon(Icons.Filled.Lock, null) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (mode == "signup") {
                    Text(if (workspace == "owner") "We'll create your account and submit an Owner-access request. Owner tools unlock only after approval." else "You can complete optional profile details later.", fontSize = 11.sp, lineHeight = 16.sp, color = colors.onSurfaceVariant)
                }
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank() || (mode == "signup" && displayName.trim().length < 2)) {
                            error = if (mode == "signup") "Enter your name, email and password." else "Enter your email and password."
                            return@Button
                        }
                        if (mode == "signup" && password.length < 12) {
                            error = "Use a password with at least 12 characters."
                            return@Button
                        }
                        busy = true; error = ""; message = ""
                        scope.launch {
                            try {
                                if (mode == "signup") {
                                    withContext(Dispatchers.IO) {
                                        StayApiClient.registerAccount(baseUrl, email, displayName, password, workspace, phone)
                                    }
                                    verificationPending = true
                                    message = "Account created. Open the verification email to finish registration."
                                } else {
                                    val result = withContext(Dispatchers.IO) {
                                        StayApiClient.loginAccount(androidx.compose.ui.platform.LocalContext.current, baseUrl, email, password)
                                    }
                                    val user = result.optJSONObject("user") ?: JSONObject()
                                    onLoginSucceeded(user)
                                }
                            } catch (e: Exception) {
                                error = e.message ?: if (mode == "signup") "Registration failed." else "Sign-in failed."
                            } finally { busy = false }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "Please wait…" else if (mode == "signup") "Create account" else "Sign in") }

                if (mode == "signin") {
                    TextButton(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                error = "Enter your email and password first."
                            } else {
                                busy = true; error = ""
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) { StayApiClient.resendAccountVerification(baseUrl, email, password) }
                                        message = "If your account needs verification, a code has been sent. Check the local inbox at http://127.0.0.1:8025."
                                    } catch (e: Exception) {
                                        error = e.message ?: "Could not request a verification email."
                                    } finally { busy = false }
                                }
                            }
                        },
                        enabled = !busy
                    ) { Text("Resend verification code") }
                }
                TextButton(onClick = { mode = if (mode == "signup") "signin" else "signup"; verificationPending = false; error = ""; message = "" }, enabled = !busy) {
                    Text(if (mode == "signup") "Already have an account? Sign in" else "New to StayEasy? Create an account")
                }
            }
        }

        if (message.isNotBlank()) {
            Text(message, fontSize = 12.sp, lineHeight = 17.sp, color = colors.primary)
        }
        if (error.isNotBlank()) {
            Text(error, fontSize = 12.sp, lineHeight = 17.sp, color = colors.error)
        }
        Spacer(Modifier.height(18.dp))
    }
}
