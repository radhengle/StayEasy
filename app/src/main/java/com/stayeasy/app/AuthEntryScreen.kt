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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * First-open entry point. Role selection only chooses which authentication path to open;
 * the server's account role remains authoritative for access to protected workspaces.
 */
@Composable
internal fun AuthEntryScreen(
    onTenantLogin: () -> Unit,
    onTenantRegister: () -> Unit,
    onOwnerLogin: () -> Unit,
    onOwnerRegister: () -> Unit,
    onBrowsePreview: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF3F6FF), colors.background, Color(0xFFFFF8F1))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            Brush.linearGradient(listOf(colors.primary, Color(0xFF6D83E8))),
                            RoundedCornerShape(18.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("S", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("StayEasy", color = colors.onBackground, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold)
                    Text("A better way to stay and manage", color = colors.onBackground.copy(alpha = 0.68f), fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                "Welcome home.",
                color = colors.onBackground,
                fontSize = 32.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Choose how you'll use StayEasy. Sign in to continue with your existing account, or register if you're new.",
                color = colors.onBackground.copy(alpha = 0.72f),
                fontSize = 14.sp,
                lineHeight = 21.sp
            )

            RoleAccessCard(
                title = "Tenant / Customer",
                subtitle = "Find a place, manage enquiries and keep your stay details in one place.",
                icon = { androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = null, tint = colors.primary, modifier = Modifier.size(25.dp)) },
                onLogin = onTenantLogin,
                onRegister = onTenantRegister
            )

            RoleAccessCard(
                title = "Property Owner",
                subtitle = "Manage properties, room and bed inventory, residents and rent.",
                icon = { androidx.compose.material3.Icon(Icons.Filled.Apartment, contentDescription = null, tint = colors.secondary, modifier = Modifier.size(25.dp)) },
                onLogin = onOwnerLogin,
                onRegister = onOwnerRegister,
                owner = true
            )

            TextButton(
                onClick = onBrowsePreview,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Browse sample listings", fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Owner access is approved by the server. Choosing Property Owner here never grants owner permissions.",
                color = colors.onBackground.copy(alpha = 0.62f),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
            )
        }
    }
}

@Composable
private fun RoleAccessCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    owner: Boolean = false
) {
    val colors = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (owner) Color(0xFFFFE9DA) else Color(0xFFE7EDFF),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, color = colors.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = colors.onSurface.copy(alpha = 0.68f), fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onLogin,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Text("Login")
                }
                Button(
                    onClick = onRegister,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (owner) colors.secondary else colors.primary)
                ) {
                    Text("Register")
                }
            }
        }
    }
}
