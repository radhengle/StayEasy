package com.stayeasy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AccountScreen(
    apiBaseUrl: String,
    onBack: () -> Unit,
    onConfigureApi: () -> Unit,
    onAccountReady: (accessToken: String, role: String, displayName: String) -> Unit
) {
    var isRegistering by remember { mutableStateOf(true) }
    var selectedRole by remember { mutableStateOf("customer") }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().background(SE.Background).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SE.Ink)
            }
            Column(Modifier.weight(1f)) {
                Text(if (isRegistering) "Create your account" else "Welcome back", color = SE.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("StayEasy · Find a place. Feel at home.", color = SE.Muted, fontSize = 11.sp)
            }
        }

        if (isRegistering) {
            Text("How will you use StayEasy?", color = SE.Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Choose the option that best describes you. We’ll set up the right experience.", color = SE.Muted, fontSize = 12.sp, lineHeight = 17.sp)

            RoleChoiceCard(
                selected = selectedRole == "customer",
                title = "Tenant",
                subtitle = "Looking for a place to stay? Explore homes, save favourites, request visits and manage your stay.",
                onClick = { selectedRole = "customer"; message = "" }
            ) {
                Icon(Icons.Filled.Home, contentDescription = null, tint = SE.Blue, modifier = Modifier.size(25.dp))
            }
            RoleChoiceCard(
                selected = selectedRole == "owner",
                title = "Owner",
                subtitle = "Own or manage a property? List your rooms, respond to enquiries and manage residents.",
                onClick = { selectedRole = "owner"; message = "" }
            ) {
                Icon(Icons.Filled.Apartment, contentDescription = null, tint = SE.Blue, modifier = Modifier.size(25.dp))
            }

            OutlinedTextField(
                value = displayName, onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Full name") }, shape = RoundedCornerShape(13.dp)
            )
            OutlinedTextField(
                value = phone, onValueChange = { phone = it.take(40) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Phone number (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(13.dp)
            )
        }

        OutlinedTextField(
            value = email, onValueChange = { email = it.trimStart() },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Email address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(13.dp)
        )
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(if (isRegistering) "Password (at least 10 characters)" else "Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(13.dp)
        )

        if (apiBaseUrl.isBlank()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SE.OrangeSoft),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Connect StayEasy first", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Account creation and sign-in need a configured StayEasy API. Your account won’t be created offline.", color = SE.Text, fontSize = 11.sp, lineHeight = 16.sp)
                    OutlinedButton(onClick = onConfigureApi, modifier = Modifier.fillMaxWidth()) { Text("Configure API connection") }
                }
            }
        }

        if (message.isNotBlank()) {
            Text(message, color = if (message.startsWith("Success")) SE.Blue else SE.Red, fontSize = 12.sp, lineHeight = 17.sp)
        }

        Button(
            onClick = {
                if (busy) return@Button
                if (apiBaseUrl.isBlank()) {
                    message = "Connect the StayEasy API before creating an account."
                    return@Button
                }
                if (email.isBlank() || password.isBlank() || (isRegistering && displayName.trim().length < 2)) {
                    message = if (isRegistering) "Enter your name, email address and password." else "Enter your email address and password."
                    return@Button
                }
                if (isRegistering && password.length < 10) {
                    message = "Use a password with at least 10 characters."
                    return@Button
                }
                busy = true
                message = if (isRegistering) "Creating your account…" else "Signing you in…"
                scope.launch {
                    try {
                        val response = withContext(Dispatchers.IO) {
                            if (isRegistering) {
                                StayApiClient.registerAccount(apiBaseUrl, displayName, email, phone, password, selectedRole)
                            } else {
                                StayApiClient.loginAccount(apiBaseUrl, email, password)
                            }
                        }
                        val token = response.optString("access_token")
                        val user = response.optJSONObject("user")
                        val role = user?.optString("role").orEmpty()
                        val name = user?.optString("display_name").orEmpty().ifBlank { email.substringBefore("@") }
                        check(token.isNotBlank() && role in setOf("customer", "owner")) {
                            "The server did not return a valid account session."
                        }
                        message = "Success — account session ready."
                        onAccountReady(token, role, name)
                    } catch (e: Exception) {
                        message = e.message?.take(240) ?: "Unable to continue. Check your connection and try again."
                    } finally {
                        busy = false
                    }
                }
            },
            enabled = !busy && apiBaseUrl.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)
        ) {
            Text(
                when {
                    busy -> "Please wait…"
                    isRegistering && selectedRole == "customer" -> "Create Tenant account"
                    isRegistering -> "Create Owner account"
                    else -> "Sign in"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Text(
                if (isRegistering) "Already have an account?" else "New to StayEasy?",
                color = SE.Muted, fontSize = 12.sp
            )
            TextButton(onClick = {
                isRegistering = !isRegistering
                message = ""
            }) {
                Text(if (isRegistering) "Sign in" else "Create account", color = SE.Blue, fontWeight = FontWeight.Bold)
            }
        }

        Text(
            "Your account type controls which StayEasy features are available. Passwords are sent only to your configured HTTPS API (or local development API).",
            color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun RoleChoiceCard(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leading: @Composable () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) SE.BlueSoft else SE.Surface)
            .border(1.dp, if (selected) SE.Blue else SE.Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        leading()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(title, color = SE.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (selected) Text("SELECTED", color = SE.Blue, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            Text(subtitle, color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}
