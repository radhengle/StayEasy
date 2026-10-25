package com.stayeasy.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

private val LiveNavy = Color(0xFF101B2D)
private val LiveBlue = Color(0xFF4169F5)
private val LiveLime = Color(0xFFD6F36A)
private val LiveMuted = Color(0xFF718096)
private val LiveINR = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

@Composable
internal fun LiveHomeScreen(
    location: String, onLocationChange: (String) -> Unit, guestChoice: String, onGuestChoice: (String) -> Unit,
    maxBudget: Float, listings: List<PgListing>, favorites: List<Long>, onListing: (Long) -> Unit, onFavorite: (Long) -> Unit,
    onSearch: () -> Unit, onMap: () -> Unit, onFilters: () -> Unit, onOwner: () -> Unit, onNotifications: () -> Unit, onProfile: () -> Unit
) {
    val liveListings = listings.filter { !it.isDemo && it.dataSource != "demo-fixture" }
    val previewListings = listings.filter { it.isDemo || it.dataSource == "demo-fixture" }
    Column(Modifier.fillMaxSize().background(Color(0xFFF5F7FB)).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(LiveNavy), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.HomeWork, null, tint = LiveLime, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("StayEasy", color = LiveNavy, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, letterSpacing = 1.2.sp)
                Text("The smarter way to find a stay", color = LiveMuted, fontSize = 11.sp)
            }
            Icon(Icons.Filled.Notifications, "Notifications", tint = LiveNavy, modifier = Modifier.size(23.dp).clickable(onClick = onNotifications))
            Spacer(Modifier.width(14.dp))
            Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFE6EBF6)).clickable(onClick = onProfile), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, "Profile and connections", tint = LiveNavy)
            }
        }
        Spacer(Modifier.height(20.dp))
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = LiveNavy), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF172942), Color(0xFF263E69)))).padding(20.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.clip(CircleShape).background(if (liveListings.isNotEmpty()) LiveLime else Color(0xFF42516A)).padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Text(if (liveListings.isNotEmpty()) "● LIVE SERVER DATA" else "● PREVIEW MODE", color = if (liveListings.isNotEmpty()) LiveNavy else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Text("PUNE · INDIA", color = Color(0xFFB8C7DF), fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Your next chapter\nstarts here.", color = Color.White, fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Compare server listings, see clear costs, and keep your stay in one place.", color = Color(0xFFD1D9E7), fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = LiveLime, modifier = Modifier.size(17.dp))
                        Text(location.ifBlank { "Pune" }, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.weight(1f))
                        Text(liveListings.size.toString() + " live · " + previewListings.size + " preview", color = Color(0xFFBDCAE0), fontSize = 10.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth().clickable(onClick = onSearch), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, null, tint = LiveBlue, modifier = Modifier.size(23.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Where do you want to live?", color = LiveNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Search neighbourhoods, rooms and budgets", color = LiveMuted, fontSize = 11.sp)
                }
                Icon(Icons.Filled.ArrowForward, null, tint = LiveBlue)
            }
        }
        Spacer(Modifier.height(15.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Find your fit", color = LiveNavy, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                Text("Start with who you're looking for", color = LiveMuted, fontSize = 11.sp)
            }
            OutlinedButton(onClick = onFilters, shape = RoundedCornerShape(13.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                Icon(Icons.Filled.Tune, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Filters", fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All genders", "Boys only", "Girls only").forEach { choice ->
                FilterChip(selected = guestChoice == choice || (choice == "All genders" && guestChoice == "Mixed"),
                    onClick = { onGuestChoice(if (choice == "All genders") "Mixed" else choice) },
                    label = { Text(choice, fontSize = 12.sp) }, shape = RoundedCornerShape(14.dp))
            }
        }
        Spacer(Modifier.height(21.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text("Stays around you", color = LiveNavy, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (liveListings.isNotEmpty()) "Fresh from the StayEasy API" else "Connect a backend to replace previews", color = LiveMuted, fontSize = 11.sp)
            }
            Text(listings.size.toString() + " total", color = LiveBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        if (listings.isEmpty()) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("No published stays yet", color = LiveNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Open Profile → Feature hub → API connection & sync to connect your backend and load server data.", color = LiveMuted, fontSize = 12.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onProfile, colors = ButtonDefaults.buttonColors(containerColor = LiveBlue)) { Text("Connect live data") }
                }
            }
        } else {
            listings.take(5).forEachIndexed { index, listing ->
                Card(Modifier.fillMaxWidth().clickable { onListing(listing.id) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        val tileBrush = if (index % 2 == 0) Brush.linearGradient(listOf(Color(0xFFDBE6FF), Color(0xFFF0F4FF))) else Brush.linearGradient(listOf(Color(0xFFFFE8D9), Color(0xFFFFF5EA)))
                        Box(Modifier.size(width = 94.dp, height = 116.dp).clip(RoundedCornerShape(16.dp)).background(tileBrush), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Bed, null, tint = LiveNavy, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(listing.name, color = LiveNavy, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Spacer(Modifier.width(4.dp))
                                Text(when { listing.isDemo || listing.dataSource == "demo-fixture" -> "DEMO DATA"; listing.dataSource != "owner" -> "EXTERNAL"; !listing.isVerified -> "SERVER · UNVERIFIED"; else -> "VERIFIED" }, color = if (listing.isDemo || listing.dataSource == "demo-fixture") LiveMuted else if (!listing.isVerified) Color(0xFF99611C) else Color(0xFF28775D), fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(listing.area, color = LiveMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(10.dp))
                            Text(LiveINR.format(listing.monthlyRent), color = LiveNavy, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text("per month", color = LiveMuted, fontSize = 10.sp)
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(listing.availableBeds.toString() + " beds available", color = LiveMuted, fontSize = 10.sp)
                                Spacer(Modifier.weight(1f))
                                Box(Modifier.size(33.dp).clip(CircleShape).background(LiveBlue).clickable { onListing(listing.id) }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF0FE)), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onOwner).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Color.White), contentAlignment = Alignment.Center) { Icon(Icons.Filled.HomeWork, null, tint = LiveBlue) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Manage a property?", color = LiveNavy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Open the owner workspace for residents, rent and operations.", color = LiveMuted, fontSize = 11.sp, lineHeight = 16.sp)
                }
                Icon(Icons.Filled.ArrowForward, null, tint = LiveBlue)
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}
