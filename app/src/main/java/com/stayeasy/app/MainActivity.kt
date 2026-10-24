package com.stayeasy.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Refresh
import androidx.core.content.edit
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

internal object SE {
    var Ink by mutableStateOf(Color(0xFF17263D)); private set
    var Blue by mutableStateOf(Color(0xFF315BE8)); private set
    var BlueSoft by mutableStateOf(Color(0xFFEAF0FF)); private set
    var Orange by mutableStateOf(Color(0xFFF1845B)); private set
    var OrangeSoft by mutableStateOf(Color(0xFFFFEEE7)); private set
    var Background by mutableStateOf(Color(0xFFF6F7FA)); private set
    var Surface by mutableStateOf(Color(0xFFFFFFFF)); private set
    var Text by mutableStateOf(Color(0xFF1B2432)); private set
    var Muted by mutableStateOf(Color(0xFF737D8F)); private set
    var Border by mutableStateOf(Color(0xFFE5E8EF)); private set
    // Kept for compatibility with existing cards; these are intentionally blue/neutral, never green.
    var Mint by mutableStateOf(Color(0xFFE9EEF9)); private set
    var MintText by mutableStateOf(Color(0xFF315BE8)); private set
    var Lavender by mutableStateOf(Color(0xFFF0ECFF)); private set
    var RedSoft by mutableStateOf(Color(0xFFFFE9EA)); private set
    var Red by mutableStateOf(Color(0xFFB63A4B)); private set
    var isDark by mutableStateOf(false); private set

    fun applyPalette(theme: String, dark: Boolean) {
        isDark = dark
        val accent = when (theme) {
            "Ocean" -> Color(0xFF087F9E)
            "Violet" -> Color(0xFF7556D9)
            "Graphite" -> Color(0xFF536174)
            "Copper" -> Color(0xFFA35B40)
            else -> Color(0xFF315BE8)
        }
        val warm = when (theme) {
            "Ocean" -> Color(0xFFDA985E)
            "Violet" -> Color(0xFFE39A84)
            "Graphite" -> Color(0xFFC39B67)
            "Copper" -> Color(0xFFD3AA78)
            else -> Color(0xFFF1845B)
        }
        Blue = if (dark && theme == "Midnight Cobalt") Color(0xFF7895FF) else accent
        Orange = warm
        Ink = if (dark) Color(0xFFE8EDF8) else when (theme) { "Ocean" -> Color(0xFF102E3A); "Violet" -> Color(0xFF282044); "Graphite" -> Color(0xFF202A38); "Copper" -> Color(0xFF38251E); else -> Color(0xFF17263D) }
        Text = Ink
        Background = if (dark) when (theme) { "Ocean" -> Color(0xFF0C1B23); "Violet" -> Color(0xFF181421); "Graphite" -> Color(0xFF15191F); "Copper" -> Color(0xFF201713); else -> Color(0xFF101725) } else when (theme) { "Ocean" -> Color(0xFFF0F7F9); "Violet" -> Color(0xFFF6F3FC); "Graphite" -> Color(0xFFF2F4F7); "Copper" -> Color(0xFFFAF5F0); "Porcelain" -> Color(0xFFF8F9FC); else -> Color(0xFFF6F7FA) }
        Surface = if (dark) when (theme) { "Ocean" -> Color(0xFF142730); "Violet" -> Color(0xFF221C30); "Graphite" -> Color(0xFF20262F); "Copper" -> Color(0xFF2A1F1B); else -> Color(0xFF192337) } else Color.White
        Muted = if (dark) Color(0xFFA7B2C5) else Color(0xFF737D8F)
        Border = if (dark) Color(0xFF354155) else Color(0xFFE2E7F0)
        BlueSoft = if (dark) accent.copy(alpha = .23f) else accent.copy(alpha = .10f)
        OrangeSoft = if (dark) warm.copy(alpha = .19f) else warm.copy(alpha = .13f)
        Mint = if (dark) Color(0xFF29364A) else Color(0xFFE9EEF9)
        MintText = if (dark) Color(0xFFB8C8FF) else accent
        Lavender = if (dark) Color(0xFF302B47) else Color(0xFFF0ECFF)
        RedSoft = if (dark) Color(0xFF482A38) else Color(0xFFFFE9EA)
        Red = if (dark) Color(0xFFFF9BAC) else Color(0xFFB63A4B)
    }
}

internal data class RemoteUnit(
    val id: Long,
    val roomCode: String,
    val unitCode: String,
    val rentableType: String,
    val sharingCapacity: Int,
    val monthlyRent: Int,
    val deposit: Int,
    val status: String
)

internal data class RemoteResidentCandidate(
    val id: String,
    val displayName: String,
    val email: String,
    val emailVerified: Boolean
)

internal data class PgListing(
    val id: Long,
    val name: String,
    val area: String,
    val monthlyRent: Int,
    val deposit: Int,
    val sharing: String,
    val allowedFor: String,
    val couplesAllowed: Boolean,
    val amenities: List<String>,
    val distanceKm: Double,
    val rating: Double,
    val availableBeds: Int,
    val totalBeds: Int,
    val description: String,
    val foodMonthly: Int = 0,
    val photoUri: String = "",
    val isDemo: Boolean = true,
    val totalRooms: Int = 1,
    val isVerified: Boolean = false,
    val isPublished: Boolean = true,
    val isFurnished: Boolean = true,
    val bathroomType: String = "Shared",
    val cookingAllowed: Boolean = false,
    val curfewPolicy: String = "Ask owner",
    val visitorPolicy: String = "Ask owner",
    val minimumStayMonths: Int = 1,
    val noticePeriodDays: Int = 30,
    val utilityMonthlyEstimate: Int = 0,
    val foodPolicy: String = if (foodMonthly > 0) "Optional" else "Not offered",
    val pinCode: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val hasCoordinates: Boolean = false,
    val distanceIsVerified: Boolean = false,
    val featuredListing: Boolean = false,
    val availabilityConfirmedAt: Long = System.currentTimeMillis(),
    val expectedVacancy: String = "Available now",
    val nearbyPlaces: String = "",
    val serverPropertyId: Long? = null,
    val serverUnits: List<RemoteUnit> = emptyList()
)

private val demoListings = listOf(
    PgListing(101, "CityNest Living", "Baner, Pune", 9200, 18000, "2 sharing", "Boys only", false, listOf("Wi-Fi", "Meals", "Laundry", "Study desk"), 1.2, 4.7, 3, 24, "A calm, furnished stay with dedicated study space and easy access to Baner and Aundh.", 1800, totalRooms = 12),
    PgListing(102, "Maple House", "Wakad, Pune", 8800, 15000, "3 sharing", "Girls only", false, listOf("Wi-Fi", "Meals", "Housekeeping", "Power backup"), 2.4, 4.6, 2, 18, "Bright shared rooms, regular housekeeping, and a comfortable common lounge.", 1600, totalRooms = 8),
    PgListing(103, "The Courtyard Co-Living", "Hinjawadi Phase 1, Pune", 12500, 22000, "2 sharing", "Mixed", true, listOf("Wi-Fi", "AC", "Meals", "Laundry", "Parking"), 0.8, 4.8, 4, 32, "A modern co-living property with flexible room options, shared common areas, and transparent monthly charges.", 2200, totalRooms = 14),
    PgListing(104, "Casa Duo", "Viman Nagar, Pune", 16000, 32000, "Private room", "Mixed", true, listOf("Wi-Fi", "AC", "Attached bath", "Laundry", "Parking"), 1.1, 4.5, 1, 12, "Private rooms for people who value extra space, with couple-friendly options subject to the owner's published terms.", 0, totalRooms = 12),
    PgListing(105, "Willow Stay", "Kothrud, Pune", 7600, 12000, "4 sharing", "Mixed", false, listOf("Wi-Fi", "Meals", "Power backup", "Study desk"), 1.7, 4.4, 5, 28, "A budget-friendly stay with practical facilities and convenient access to local transport.", 1500, totalRooms = 9)
)

private object ListingStore {
    private const val PREF = "stayeasy_demo"
    private const val KEY = "custom_listings"
    fun load(context: Context): List<PgListing> = runCatching {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val array = JSONArray(json)
        (0 until array.length()).map { index ->
            val o = array.getJSONObject(index)
            val amenitiesArray = o.optJSONArray("amenities")
            val amenitiesList = if (amenitiesArray != null) {
                (0 until amenitiesArray.length()).mapNotNull { i -> amenitiesArray.optString(i) }
            } else emptyList()
            PgListing(
                id = o.optLong("id"), name = o.optString("name"), area = o.optString("area"),
                monthlyRent = o.optInt("monthlyRent"), deposit = o.optInt("deposit"), sharing = o.optString("sharing"),
                allowedFor = o.optString("allowedFor", "Mixed"), couplesAllowed = o.optBoolean("couplesAllowed"),
                amenities = amenitiesList,
                distanceKm = o.optDouble("distanceKm", 1.4), rating = o.optDouble("rating", 0.0),
                availableBeds = o.optInt("availableBeds", 1), totalBeds = o.optInt("totalBeds", 1),
                description = o.optString("description"), foodMonthly = o.optInt("foodMonthly"),
                photoUri = o.optString("photoUri"), isDemo = false, totalRooms = o.optInt("totalRooms", 1),
                isVerified = o.optBoolean("isVerified", false), isPublished = o.optBoolean("isPublished", true),
                isFurnished = o.optBoolean("isFurnished", true), bathroomType = o.optString("bathroomType", "Shared"),
                cookingAllowed = o.optBoolean("cookingAllowed", false), curfewPolicy = o.optString("curfewPolicy", "Ask owner"),
                visitorPolicy = o.optString("visitorPolicy", "Ask owner"), minimumStayMonths = o.optInt("minimumStayMonths", 1),
                noticePeriodDays = o.optInt("noticePeriodDays", 30), utilityMonthlyEstimate = o.optInt("utilityMonthlyEstimate", 0),
                foodPolicy = o.optString("foodPolicy", if (o.optInt("foodMonthly") > 0) "Optional" else "Not offered"), pinCode = o.optString("pinCode"),
                latitude = o.optDouble("latitude", 0.0), longitude = o.optDouble("longitude", 0.0), hasCoordinates = o.optBoolean("hasCoordinates", false),
                distanceIsVerified = o.optBoolean("distanceIsVerified", false), featuredListing = o.optBoolean("featuredListing", false), availabilityConfirmedAt = o.optLong("availabilityConfirmedAt", System.currentTimeMillis()),
                expectedVacancy = o.optString("expectedVacancy", "Available now"), nearbyPlaces = o.optString("nearbyPlaces", ""),
                serverPropertyId = o.optLong("serverPropertyId").takeIf { it > 0L }
            )
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, listings: List<PgListing>) {
        val array = JSONArray()
        listings.forEach { p ->
            val o = JSONObject()
            o.put("id", p.id); o.put("name", p.name); o.put("area", p.area)
            o.put("monthlyRent", p.monthlyRent); o.put("deposit", p.deposit); o.put("sharing", p.sharing)
            o.put("allowedFor", p.allowedFor); o.put("couplesAllowed", p.couplesAllowed)
            val amenities = JSONArray(); p.amenities.forEach { amenities.put(it) }; o.put("amenities", amenities)
            o.put("distanceKm", p.distanceKm); o.put("rating", p.rating); o.put("availableBeds", p.availableBeds)
            o.put("totalBeds", p.totalBeds); o.put("description", p.description); o.put("foodMonthly", p.foodMonthly)
            o.put("photoUri", p.photoUri); o.put("totalRooms", p.totalRooms)
            o.put("isVerified", p.isVerified); o.put("isPublished", p.isPublished); o.put("isFurnished", p.isFurnished)
            o.put("bathroomType", p.bathroomType); o.put("cookingAllowed", p.cookingAllowed); o.put("curfewPolicy", p.curfewPolicy)
            o.put("visitorPolicy", p.visitorPolicy); o.put("minimumStayMonths", p.minimumStayMonths); o.put("noticePeriodDays", p.noticePeriodDays)
            o.put("utilityMonthlyEstimate", p.utilityMonthlyEstimate); o.put("foodPolicy", p.foodPolicy); o.put("pinCode", p.pinCode)
            o.put("latitude", p.latitude); o.put("longitude", p.longitude); o.put("hasCoordinates", p.hasCoordinates); o.put("distanceIsVerified", p.distanceIsVerified)
            o.put("featuredListing", p.featuredListing); o.put("availabilityConfirmedAt", p.availabilityConfirmedAt); o.put("expectedVacancy", p.expectedVacancy); o.put("nearbyPlaces", p.nearbyPlaces)
            p.serverPropertyId?.let { o.put("serverPropertyId", it) }; array.put(o)
        }
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit { putString(KEY, array.toString()) }
    }
}

private fun money(amount: Int) = "₹" + String.format(Locale.getDefault(), "%,d", amount)

// Nonlinear rent scale: most slider travel stays useful below ₹30,000.
private fun rentSliderPosition(amount: Int): Float {
    val normalized = (amount.coerceIn(100, 100000) - 100) / 99900f
    return normalized.coerceIn(0f, 1f).pow(0.16f)
}
private fun rentFromSlider(position: Float): Float =
    (100f + 99900f * position.coerceIn(0f, 1f).pow(6.25f)).roundToInt().toFloat()

@Composable
private fun RentBudgetSlider(value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Slider(value = rentSliderPosition(value.roundToInt()), onValueChange = { onValueChange(rentFromSlider(it)) }, valueRange = 0f..1f)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("₹100", color = SE.Muted, fontSize = 10.sp)
            Text("₹1,00,000", color = SE.Muted, fontSize = 10.sp)
        }
        Text("Fine control through ₹30,000; higher rents use less slider travel.", color = SE.Blue, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.rgb(246, 247, 250)
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.rgb(246, 247, 250)
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent {
            var preferences by remember { mutableStateOf(StayPreferenceStore.load(this@MainActivity)) }
            StayEasyTheme(preferences) {
                StayEasyApp(preferences = preferences, onPreferencesChanged = { updated ->
                    preferences = updated
                    StayPreferenceStore.save(this@MainActivity, updated)
                    @Suppress("DEPRECATION")
                    window.statusBarColor = if (SE.isDark) android.graphics.Color.rgb(16, 23, 37) else android.graphics.Color.rgb(246, 247, 250)
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = if (SE.isDark) android.graphics.Color.rgb(16, 23, 37) else android.graphics.Color.rgb(246, 247, 250)
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = if (SE.isDark) 0 else android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                })
            }
        }
    }
}

@Composable
private fun StayEasyTheme(preferences: StayPreferences, content: @Composable () -> Unit) {
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dark = if (preferences.mode == "System") systemDark else preferences.mode == "Dark"
    SE.applyPalette(preferences.theme, dark)
    val colors = if (dark) androidx.compose.material3.darkColorScheme(
        primary = SE.Blue, onPrimary = Color.White, secondary = SE.Orange, background = SE.Background,
        surface = SE.Surface, onSurface = SE.Text, onBackground = SE.Text, outline = SE.Border
    ) else androidx.compose.material3.lightColorScheme(
        primary = SE.Blue, onPrimary = Color.White, secondary = SE.Orange, background = SE.Background,
        surface = SE.Surface, onSurface = SE.Text, onBackground = SE.Text, outline = SE.Border
    )
    val density = LocalDensity.current
    val accessibleDensity = Density(density.density, density.fontScale * if (preferences.largeText) 1.12f else 1f)
    CompositionLocalProvider(LocalDensity provides accessibleDensity) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StayEasyApp(preferences: StayPreferences, onPreferencesChanged: (StayPreferences) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val customListings = remember { mutableStateListOf<PgListing>().apply { addAll(ListingStore.load(context)) } }
    val remoteListings = remember { mutableStateListOf<PgListing>() }
    val remoteOwnerListings = remember { mutableStateListOf<PgListing>() }
    var ownerInventoryLoaded by remember { mutableStateOf(false) }
    val favorites = remember { mutableStateListOf<Long>().apply { addAll(StayUserDataStore.loadFavorites(context)) } }
    var route by remember { mutableStateOf("home") }
    var workspaceReturnRoute by remember { mutableStateOf("home") }
    var detailsReturnRoute by remember { mutableStateOf("search") }
    var filtersReturnRoute by remember { mutableStateOf("search") }
    var signedInAccount by remember { mutableStateOf<JSONObject?>(null) }
    LaunchedEffect(preferences.apiBaseUrl) {
        val baseUrl = preferences.apiBaseUrl.trim()
        if (baseUrl.isNotBlank()) {
            val restored = withContext(Dispatchers.IO) { StayApiClient.restoreAccountSession(context, baseUrl) }
            signedInAccount = if (restored) runCatching {
                withContext(Dispatchers.IO) { StayApiClient.currentAccount(baseUrl) }
            }.getOrNull() else null
            if (restored && signedInAccount == null) {
                StayApiClient.clearSessionToken()
                StayApiClient.clearPersistedAccountSession(context)
            }
        } else {
            signedInAccount = null
            StayApiClient.clearSessionToken()
        }
    }
    var ownerMode by remember { mutableStateOf(false) }
    var ownerReturnRoute by remember { mutableStateOf("home") }
    var chatReturnRoute by remember { mutableStateOf("details") }
    var notificationsReturnRoute by remember { mutableStateOf("home") }
    var selectedId by remember { mutableLongStateOf(101L) }
    var residentPropertyName by remember { mutableStateOf("CityNest Living") }
    var residentBedOptions by remember { mutableStateOf(listOf("Room 101 · Bed A", "Room 101 · Bed B")) }
    var residentListing by remember { mutableStateOf<PgListing?>(null) }
    var residentCandidates by remember { mutableStateOf<List<RemoteResidentCandidate>>(emptyList()) }
    var residentCandidatesLoading by remember { mutableStateOf(false) }
    var residentCandidatesError by remember { mutableStateOf("") }
    var residentStaySubmitting by remember { mutableStateOf(false) }
    var comparisonIds by remember { mutableStateOf<List<Long>>(emptyList()) }
    var location by remember { mutableStateOf("Pune, Maharashtra") }
    var searchQuery by remember { mutableStateOf("") }
    var guestChoice by remember { mutableStateOf("Any") }
    var maxBudget by remember { mutableFloatStateOf(30000f) }
    var advancedFilters by remember { mutableStateOf(DiscoveryFilters()) }
    val chosenAmenities = remember { mutableStateListOf<String>() }
    val toggleFavorite: (Long) -> Unit = { id ->
        if (favorites.contains(id)) favorites.remove(id) else favorites.add(id)
        StayUserDataStore.saveFavorites(context, favorites.toList())
    }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { message -> scope.launch { snackbar.showSnackbar(message) } }
    val loadResidentCandidates: (Long) -> Unit = { propertyId ->
        residentCandidates = emptyList()
        residentCandidatesError = ""
        val baseUrl = preferences.apiBaseUrl.trim()
        val token = if (baseUrl.isNotBlank()) StayApiClient.currentSessionToken(baseUrl) else null
        if (propertyId <= 0L) {
            residentCandidatesLoading = false
            residentCandidatesError = "This property is not connected to the StayEasy server."
        } else if (token == null) {
            residentCandidatesLoading = false
            residentCandidatesError = "Sign in with an owner-authorized account to load verified residents."
        } else {
            residentCandidatesLoading = true
            scope.launch {
                try {
                    val rows = withContext(Dispatchers.IO) {
                        StayApiClient.ownerResidentCandidates(baseUrl, token, propertyId)
                    }
                    if (residentListing?.serverPropertyId == propertyId) {
                        residentCandidates = rows
                        residentCandidatesError = ""
                    }
                } catch (e: Exception) {
                    if (residentListing?.serverPropertyId == propertyId) {
                        residentCandidatesError = e.message ?: "Could not load verified residents. Check owner access and retry."
                    }
                } finally {
                    if (residentListing?.serverPropertyId == propertyId) residentCandidatesLoading = false
                }
            }
        }
    }
    LaunchedEffect(preferences.apiBaseUrl, ownerMode) {
        val baseUrl = preferences.apiBaseUrl.trim()
        val token = if (baseUrl.isNotBlank()) StayApiClient.currentSessionToken(baseUrl) else null
        if (!ownerMode || baseUrl.isBlank() || token == null) {
            remoteOwnerListings.clear()
            ownerInventoryLoaded = false
        } else {
            try {
                val rows = withContext(Dispatchers.IO) { StayApiClient.ownerProperties(baseUrl, token) }
                remoteOwnerListings.clear()
                remoteOwnerListings.addAll(rows)
                ownerInventoryLoaded = true
            } catch (e: Exception) {
                ownerInventoryLoaded = false
                notify("Owner inventory could not sync; local records remain available. ${e.message ?: "Check the API connection and owner permissions."}")
            }
        }
    }
    val allListings = remoteListings.toList() + customListings.filter { local -> remoteListings.none { remote -> remote.id == local.id } } + demoListings.filter { demo -> customListings.none { it.id == demo.id } && remoteListings.none { it.id == demo.id } }
    val selectedListing = allListings.firstOrNull { it.id == selectedId } ?: allListings.firstOrNull() ?: demoListings.first()
    val localOwnerListings = customListings.filter { local ->
        if (ownerInventoryLoaded) local.serverPropertyId == null
        else remoteOwnerListings.none { remote -> remote.serverPropertyId != null && remote.serverPropertyId == local.serverPropertyId }
    }
    val ownerListings = remoteOwnerListings.toList() + localOwnerListings +
        if (!ownerInventoryLoaded && remoteOwnerListings.isEmpty() && localOwnerListings.isEmpty()) demoListings.take(2) else emptyList()
    val filteredListings = allListings.filter { p ->
        val textMatch = searchQuery.isBlank() || listOf(p.name, p.area, p.description, p.pinCode, p.sharing, p.allowedFor).any { it.contains(searchQuery, true) }
        val loc = location.substringBefore(",").trim()
        val locationMatch = loc.isBlank() || loc.equals("Pune", true) || loc.equals("Near me", true) || p.area.contains(loc, true) || p.area.contains(location, true) || (p.pinCode.isNotBlank() && p.pinCode.contains(loc, true))
        val guestMatch = when (guestChoice) {
            "Boys" -> p.allowedFor == "Boys only" || p.allowedFor == "Mixed"
            "Girls" -> p.allowedFor == "Girls only" || p.allowedFor == "Mixed"
            "Mixed" -> p.allowedFor == "Mixed"
            "Couples" -> p.couplesAllowed
            else -> true
        }
        val sharingMatch = advancedFilters.roomTypes.isEmpty() || advancedFilters.roomTypes.any { type ->
            when (type) { "Private room" -> p.sharing.contains("Private", true); "1 sharing" -> p.sharing.contains("1 sharing", true) || p.sharing.contains("single", true); "2 sharing" -> p.sharing.contains("2 sharing", true); "3 sharing" -> p.sharing.contains("3 sharing", true); "4+ sharing" -> p.sharing.contains("4", true) || p.sharing.contains("5", true); else -> true }
        }
        val amenityMatch = chosenAmenities.all { p.amenities.contains(it) }
        val dateMatch = advancedFilters.moveInDate.isBlank() || advancedFilters.moveInDate.equals("Any date", true) || p.expectedVacancy.contains(advancedFilters.moveInDate, true) || advancedFilters.moveInDate.equals("Available now", true) && p.availableBeds > 0
        textMatch && locationMatch && guestMatch && p.monthlyRent >= advancedFilters.minRent && p.monthlyRent <= maxBudget.toInt() &&
            (!advancedFilters.filterDeposit || p.deposit in advancedFilters.minDeposit..advancedFilters.maxDeposit) &&
            (!advancedFilters.filterDistance || !p.distanceIsVerified || p.distanceKm <= advancedFilters.maxDistanceKm) &&
            sharingMatch && amenityMatch &&
            (!advancedFilters.availableOnly || p.availableBeds > 0) && (!advancedFilters.foodIncluded || p.foodMonthly > 0) && (!advancedFilters.acRequired || p.amenities.any { it.equals("AC", true) }) &&
            (!advancedFilters.attachedBathroom || p.bathroomType.equals("Attached", true) || p.amenities.any { it.equals("Attached bath", true) }) && (!advancedFilters.furnishedOnly || p.isFurnished) &&
            (!advancedFilters.verifiedOnly || p.isVerified) && (!advancedFilters.cookingAllowed || p.cookingAllowed) && (advancedFilters.minStayMonths == 0 || p.minimumStayMonths <= advancedFilters.minStayMonths) && dateMatch && p.isPublished
    }.let { rows -> when (advancedFilters.sortBy) {
        "Price: low to high" -> rows.sortedBy { it.monthlyRent }; "Distance" -> rows.filter { it.distanceIsVerified }.sortedBy { it.distanceKm } + rows.filterNot { it.distanceIsVerified }; "Availability" -> rows.sortedByDescending { it.availableBeds }; "Rating" -> rows.sortedByDescending { it.rating }; else -> rows.sortedWith(compareByDescending<PgListing> { it.featuredListing }.thenByDescending { it.availableBeds })
    } }
    val hideNav = route in setOf("add_pg", "edit_pg", "add_resident", "map", "filters", "details", "chat", "issue", "search_location", "request_visit", "compare", "theme_settings", "api_connection", "account_auth", "account_type") || route.startsWith("records:")

    // Nested routes return to their parent instead of finishing the Activity.
    // The property wizard registers a more specific handler for its own steps.
    BackHandler(enabled = route != "home") {
        route = if (route.startsWith("records:")) when (route.substringAfter("records:")) {
            "saved_searches" -> "search"
            "residents" -> if (ownerMode) "owner_beds" else "profile"
            "move_out_requests" -> if (ownerMode) "owner_more" else "stay"
            "payments", "charges", "deposits", "stays" -> if (ownerMode) "owner_payments" else "stay"
            "issues" -> if (ownerMode) "owner_issues" else "stay"
            "enquiries" -> if (ownerMode) "owner_enquiries" else "search"
            "visits", "reservations" -> if (ownerMode) "owner_enquiries" else detailsReturnRoute
            "notifications" -> if (ownerMode) "owner_dashboard" else "profile"
            else -> if (ownerMode) "owner_dashboard" else "feature_hub"
        } else when (route) {
            "search" -> "home"
            "filters" -> filtersReturnRoute
            "map", "search_location" -> "search"
            "details" -> detailsReturnRoute
            "request_visit", "request_booking" -> "details"
            "chat" -> chatReturnRoute
            "saved", "stay" -> if (ownerMode) "owner_dashboard" else "home"
            "payments" -> if (ownerMode) "owner_dashboard" else "stay"
            "issue" -> if (ownerMode) "owner_dashboard" else "stay"
            "enquiries" -> if (ownerMode) "owner_dashboard" else "search"
            "notifications" -> notificationsReturnRoute
            "profile" -> "home"
            "account_type" -> workspaceReturnRoute
            "account_auth" -> "profile"
            "feature_hub" -> if (ownerMode) "owner_more" else "profile"
            "api_connection", "theme_settings", "compare" -> "feature_hub"
            "owner_dashboard" -> { ownerMode = false; ownerReturnRoute }
            "owner_properties", "owner_beds", "owner_enquiries", "owner_residents", "owner_payments", "owner_issues", "owner_analytics", "owner_more", "owner_settings" -> "owner_dashboard"
            "add_pg", "edit_pg" -> "owner_properties"
            "add_resident" -> "owner_beds"
            else -> if (ownerMode) "owner_dashboard" else "home"
        }
    }

    Scaffold(
        containerColor = SE.Background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (!hideNav) {
                val navItems = if (ownerMode) listOf(
                    NavItem("Overview", "owner_dashboard", Icons.Filled.Dashboard),
                    NavItem("Beds", "owner_beds", Icons.Filled.Bed),
                    NavItem("Enquiries", "owner_enquiries", Icons.Filled.ChatBubbleOutline),
                    NavItem("Payments", "owner_payments", Icons.Filled.Payments),
                    NavItem("More", "owner_more", Icons.Filled.MoreHoriz)
                ) else listOf(
                    NavItem("Home", "home", Icons.Filled.Home),
                    NavItem("Explore", "search", Icons.Filled.Search),
                    NavItem("Saved", "saved", Icons.Filled.FavoriteBorder),
                    NavItem("My stay", "stay", Icons.Filled.CalendarToday),
                    NavItem("Profile", "profile", Icons.Filled.Person)
                )
                NavigationBar(containerColor = SE.Surface, tonalElevation = 0.dp) {
                    navItems.forEach { item ->
                        val selected = route == item.route || (item.route == "search" && route == "map") || (item.route == "owner_more" && (route in setOf("owner_properties", "owner_residents", "owner_issues", "owner_analytics") || route == "records:move_out_requests"))
                        NavigationBarItem(
                            selected = selected,
                            onClick = { route = item.route },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label, fontSize = 10.sp, maxLines = 1) },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (route) {
                "home" -> HomeScreen(location, { location = it }, guestChoice, { guestChoice = it }, maxBudget, filteredListings, favorites, { detailsReturnRoute = "home"; selectedId = it; route = "details" }, toggleFavorite, { route = "search" }, { route = "map" }, { filtersReturnRoute = "home"; route = "filters" }, { workspaceReturnRoute = "home"; route = "account_type" }, { notificationsReturnRoute = "home"; route = "notifications" }, { route = "profile" })
                "search" -> SearchResultsScreen(location, { location = it; route = "search_location" }, searchQuery, { searchQuery = it }, guestChoice, { guestChoice = it }, maxBudget, { maxBudget = it }, filteredListings, favorites, { detailsReturnRoute = "search"; selectedId = it; route = "details" }, { route = "map" }, { filtersReturnRoute = "search"; route = "filters" }, toggleFavorite, {
                    val criteria = "Location: $location; query: ${searchQuery.ifBlank { "any PG" }}; eligibility: $guestChoice; max rent: ${money(maxBudget.toInt())}; room types: ${advancedFilters.roomTypes.joinToString().ifBlank { "any" }}; amenities: ${chosenAmenities.joinToString().ifBlank { "any" }}; availability: ${if (advancedFilters.availableOnly) "available now" else advancedFilters.moveInDate.ifBlank { "any" }}; sort: ${advancedFilters.sortBy}"
                    StayRecordStore.add(context, StayRecord(category = "saved_searches", title = "${location.substringBefore(",")}: ${guestChoice} stays", details = criteria, property = location, amount = maxBudget.toInt(), status = "Active"))
                    StayRecordStore.notify(context, "Search saved", "We'll track this criteria locally; live availability alerts require the backend.")
                    notify("Search criteria saved on this device"); route = "records:saved_searches"
                }, { guestChoice = "Any"; maxBudget = 30000f; chosenAmenities.clear(); advancedFilters = DiscoveryFilters(); searchQuery = "" })
                "search_location" -> LocationSearchScreen(location, { location = it; StayUserDataStore.addRecentLocation(context, it); route = "search" }, { route = "search" })
                "map" -> MapSearchScreen(location, { location = it }, filteredListings, { detailsReturnRoute = "map"; selectedId = it; route = "details" }, { route = "search" }, { filtersReturnRoute = "map"; route = "filters" })
                "filters" -> FilterScreen(guestChoice, { guestChoice = it }, maxBudget, { maxBudget = it }, chosenAmenities, advancedFilters, { advancedFilters = it }, { route = filtersReturnRoute }, { guestChoice = "Any"; maxBudget = 30000f; chosenAmenities.clear(); advancedFilters = DiscoveryFilters() })
                "details" -> ListingDetailsScreen(selectedListing, favorites.contains(selectedListing.id), { toggleFavorite(selectedListing.id) }, { chatReturnRoute = "details"; route = "chat" }, { route = "request_visit" }, { route = detailsReturnRoute }, { route = "request_booking" })
                "request_visit" -> VisitRequestScreen(selectedListing, { route = "details" }) { moveIn, roomType, visitSlot, note ->
                    val serverId = selectedListing.serverPropertyId
                    val token = StayApiClient.currentSessionToken(preferences.apiBaseUrl)
                    if (serverId != null && preferences.apiBaseUrl.isNotBlank()) {
                        if (token == null) { notify("Connect StayEasy API and set a customer token before sending a server visit request."); route = "api_connection" }
                        else scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) {
                                    val enquiry = StayApiClient.createEnquiry(preferences.apiBaseUrl, token, serverId, roomType, note, moveIn)
                                    val visit = StayApiClient.createVisit(preferences.apiBaseUrl, token, serverId, enquiry.optLong("id").takeIf { it > 0L }, StayApiClient.visitTimeFromLabel(visitSlot))
                                    Pair(enquiry, visit)
                                }
                                StayRecordStore.add(context, StayRecord(id = "server-enquiry-${result.first.optLong("id")}", category = "enquiries", title = "Enquiry · ${selectedListing.name}", details = note, property = selectedListing.name, status = result.first.optString("status", "new").replaceFirstChar { it.uppercase() }, source = "Server"))
                                StayRecordStore.add(context, StayRecord(id = "server-visit-${result.second.optLong("id")}", category = "visits", title = "Visit · ${selectedListing.name}", details = "Visit request sent to the property owner.", property = selectedListing.name, date = result.second.optString("preferred_at"), status = result.second.optString("status", "requested").replaceFirstChar { it.uppercase() }, source = "Server"))
                                StayRecordStore.notify(context, "Visit request sent", "${selectedListing.name} · awaiting owner response", selectedListing.name)
                                notify("Enquiry and visit request saved to StayEasy server."); route = "records:visits"
                            } catch (e: Exception) { notify("Visit request was not sent: ${e.message ?: "Check customer token and API connection."}") }
                        }
                    } else {
                        val visit = StayRecord(category = "visits", title = "Visit · ${selectedListing.name}", details = "$roomType requested. Visit preference: $visitSlot. $note", property = selectedListing.name, status = "Requested")
                        StayRecordStore.add(context, visit)
                        StayRecordStore.add(context, StayRecord(category = "enquiries", title = "Enquiry · ${selectedListing.name}", details = "Move-in: $moveIn. $note", property = selectedListing.name, status = "New"))
                        StayRecordStore.notify(context, "Visit request saved", "${selectedListing.name} · owner response pending", selectedListing.name)
                        notify("Visit and enquiry saved on this device only."); route = "records:visits"
                    }
                }
                "request_booking" -> RequestBookingScreen(selectedListing, { route = "details" }) { roomType, moveInDate, unit, note ->
                    val serverId = selectedListing.serverPropertyId
                    val token = StayApiClient.currentSessionToken(preferences.apiBaseUrl)
                    if (serverId != null && preferences.apiBaseUrl.isNotBlank()) {
                        if (token == null) { notify("Connect StayEasy API and set a customer token before sending a server reservation."); route = "api_connection" }
                        else {
                            val chosenUnit = selectedListing.serverUnits.firstOrNull { it.status == "available" && (it.unitCode == unit || "${it.roomCode} · ${it.unitCode}" == unit) }
                            if (selectedListing.serverUnits.any { it.status == "available" } && chosenUnit == null) {
                                notify("Select one of the available server beds / rooms to reserve it safely.")
                            } else scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) { StayApiClient.createReservation(preferences.apiBaseUrl, token, serverId, chosenUnit?.id, roomType, moveInDate, note) }
                                    StayRecordStore.add(context, StayRecord(id = "server-reservation-${result.optLong("id")}", category = "reservations", title = "Reservation · ${selectedListing.name}", details = "$roomType requested. $note · Server expiry: ${result.optString("expires_at", "to be confirmed")}", person = "Customer account", property = selectedListing.name, unit = chosenUnit?.let { "${it.roomCode} · ${it.unitCode}" } ?: unit, date = moveInDate, amount = result.optInt("rent_snapshot") + result.optInt("deposit_snapshot"), status = result.optString("status", "pending").replaceFirstChar { it.uppercase() }, source = "Server"))
                                    StayRecordStore.notify(context, "Reservation request sent", "${selectedListing.name} · waiting for owner confirmation", selectedListing.name)
                                    notify("Server reservation request created. No payment was taken."); route = "records:reservations"
                                } catch (e: Exception) { notify("Reservation was not sent: ${e.message ?: "Check date, availability and customer token."}") }
                            }
                        }
                    } else {
                        val existingReservations = StayRecordStore.load(context, "reservations")
                        val conflict = unit.isNotBlank() && existingReservations.any { it.property.equals(selectedListing.name, true) && it.unit.equals(unit, true) && it.status in listOf("Pending", "Confirmed", "Checked in") }
                        if (conflict) {
                            notify("That unit already has an active reservation. Choose another unit or ask the owner to confirm.")
                            route = "records:reservations"
                        } else {
                            val initialCost = selectedListing.monthlyRent + selectedListing.deposit + selectedListing.foodMonthly + selectedListing.utilityMonthlyEstimate
                            StayRecordStore.add(context, StayRecord(category = "reservations", title = "Reservation · ${selectedListing.name}", details = "$roomType requested. $note", person = "Guest user", property = selectedListing.name, unit = unit, date = moveInDate, amount = initialCost, status = "Pending", source = "Local demo"))
                            StayRecordStore.add(context, StayRecord(category = "enquiries", title = "Booking enquiry · ${selectedListing.name}", details = "Requested $roomType for $moveInDate; owner confirmation pending.", person = "Guest user", property = selectedListing.name, unit = unit, status = "New", source = "Local demo"))
                            StayRecordStore.notify(context, "Reservation request saved", "${selectedListing.name} · owner confirmation pending", selectedListing.name)
                            notify("Reservation request saved on this device; no booking or payment is confirmed")
                            route = "records:reservations"
                        }
                    }
                }
                "saved" -> SavedScreen(allListings.filter { favorites.contains(it.id) }, { detailsReturnRoute = "saved"; selectedId = it; route = "details" }, { id -> favorites.remove(id); StayUserDataStore.saveFavorites(context, favorites.toList()) }, { route = "search" })
                "enquiries" -> FeatureRecordsScreen("enquiries", ownerMode) { route = "feature_hub" }
                "chat" -> ChatScreen(selectedListing, { route = "enquiries" }, {
                    StayRecordStore.add(context, StayRecord(category = "messages", title = "Message · ${selectedListing.name}", details = "Demo message thread opened. Add any follow-up notes from Communication history.", property = selectedListing.name, status = "Sent"))
                    StayRecordStore.notify(context, "Message saved", "Conversation note for ${selectedListing.name}", selectedListing.name)
                    notify("Message note saved on this device")
                })
                "stay" -> MyStayScreen(
                    baseUrl = preferences.apiBaseUrl,
                    account = signedInAccount,
                    onPayments = { route = "records:payments" },
                    onIssue = { route = "issue" },
                    onChat = { chatReturnRoute = "stay"; route = "chat" }
                )
                "payments" -> FeatureRecordsScreen("payments", ownerMode = false) { route = "stay" }
                "issue" -> IssueScreen({ route = "stay" }, { category, details, urgency ->
                    val apiBaseUrl = preferences.apiBaseUrl
                    val token = StayApiClient.currentSessionToken(apiBaseUrl)
                    fun saveLocalIssue() {
                        val record = StayRecord(category = "issues", title = category, details = details, priority = urgency,
                            property = "My current stay", status = "Open", source = "Local demo")
                        StayRecordStore.add(context, record)
                        StayRecordStore.notify(context, "Maintenance issue saved locally", "$category · $urgency priority", "My current stay")
                    }
                    if (apiBaseUrl.isBlank() || token.isNullOrBlank()) {
                        saveLocalIssue()
                        notify("Saved on this device only. Connect the API and a customer session to submit a shared ticket.")
                        route = "records:issues"
                    } else scope.launch {
                        try {
                            val activeStay = withContext(Dispatchers.IO) {
                                StayApiClient.myStays(apiBaseUrl, token).firstOrNull {
                                    it.optString("status").lowercase(Locale.ROOT) in setOf("active", "notice_given")
                                }
                            }
                            if (activeStay == null) {
                                saveLocalIssue()
                                notify("No active server-backed stay found; this issue was saved locally only.")
                                route = "records:issues"
                                return@launch
                            }
                            val propertyId = activeStay.optLong("property_id")
                            require(propertyId > 0L) { "The active stay did not include a valid property." }
                            val unitId = activeStay.optLong("unit_id").takeIf { it > 0L }
                            val response = withContext(Dispatchers.IO) {
                                StayApiClient.createIssue(apiBaseUrl, token, propertyId, unitId, category, category, details, urgency)
                            }
                            val issueId = response.optLong("id")
                            require(issueId > 0L) { "The API did not return a maintenance ticket ID." }
                            val status = response.optString("status", "open").replace('_', ' ').replaceFirstChar { it.uppercase() }
                            StayRecordStore.addIfAbsent(context, StayRecord(
                                id = "server-issue-$issueId", category = "issues", title = category, details = details,
                                status = status, priority = urgency, property = "Property #$propertyId",
                                unit = unitId?.toString().orEmpty(), source = "Server"
                            ))
                            StayRecordStore.notify(context, "Maintenance ticket submitted", "$category · $urgency priority", "Property #$propertyId")
                            notify("Maintenance ticket #$issueId sent to the owner.")
                            route = "records:issues"
                        } catch (e: Exception) {
                            notify("Issue was not submitted: ${e.message ?: "check the customer session and API connection"}. No local copy was created.")
                        }
                    }
                })
                "profile" -> ProfileScreen(signedInAccount, { workspaceReturnRoute = "profile"; route = "account_type" }, { notificationsReturnRoute = "profile"; route = "notifications" }, { route = "feature_hub" }, { route = "account_auth" })
                "account_type" -> AccountTypeScreen(
                    onTenant = { ownerMode = false; route = "home" },
                    onOwner = { ownerReturnRoute = workspaceReturnRoute; ownerMode = true; route = "owner_dashboard" },
                    onBack = { route = workspaceReturnRoute }
                )
                "account_auth" -> AccountAuthScreen(
                    baseUrl = preferences.apiBaseUrl,
                    account = signedInAccount,
                    onBack = { route = "profile" },
                    onOpenConnection = { route = "api_connection" },
                    onSignedIn = { user ->
                        signedInAccount = user
                        notify("You're signed in to StayEasy.")
                    },
                    onSignedOut = {
                        signedInAccount = null
                        notify("You've signed out.")
                    }
                )
                "notifications" -> FeatureRecordsScreen("notifications", ownerMode) { route = if (ownerMode) "feature_hub" else "feature_hub" }
                "feature_hub" -> FeatureHubScreen(ownerMode, { route = "records:$it" }, { comparisonIds = favorites.take(3); route = "compare" }, { route = "theme_settings" }, { route = "api_connection" }, { route = if (ownerMode) "owner_more" else "profile" })
                "api_connection" -> ApiConnectionScreen(preferences.apiBaseUrl,
                    { onPreferencesChanged(preferences.copy(apiBaseUrl = it.trimEnd('/'))) },
                    { fetched -> remoteListings.clear(); remoteListings.addAll(fetched); notify("Loaded ${fetched.size} published server listing(s); distance is still unverified") },
                    { route = "feature_hub" })
                "compare" -> ComparePropertiesScreen(allListings, comparisonIds, { route = "feature_hub" })
                "theme_settings" -> ThemeSettingsScreen(preferences, { updated -> onPreferencesChanged(updated); notify("Appearance preferences saved") }, { route = "feature_hub" })
                "owner_dashboard" -> OwnerDashboardScreen(ownerListings, { route = "owner_properties" }, { route = "owner_beds" }, { route = "owner_enquiries" }, { route = "owner_payments" }, { route = "owner_issues" }, { ownerMode = false; route = "home" }, { route = "owner_analytics" }, { route = "owner_residents" })
                "owner_properties" -> OwnerPropertiesScreen(ownerListings, ownerInventoryLoaded, { route = "add_pg" }, { selectedId = it; route = "owner_beds" }, { selectedId = it; route = "edit_pg" })
                "add_pg" -> AddPropertyWizard(onPublish = { listing ->
                    val baseUrl = preferences.apiBaseUrl
                    val token = StayApiClient.currentSessionToken(preferences.apiBaseUrl)
                    if (baseUrl.isNotBlank() && token != null) {
                        route = "owner_properties"
                        scope.launch {
                            try {
                                val saved = withContext(Dispatchers.IO) { StayApiClient.createOwnerListing(baseUrl, token, listing) }
                                val serverId = saved.optLong("id").takeIf { it > 0L }
                                val refreshedRows = if (serverId != null) runCatching {
                                    withContext(Dispatchers.IO) { StayApiClient.ownerProperties(baseUrl, token) }
                                }.getOrNull() else null
                                if (refreshedRows != null) {
                                    remoteOwnerListings.clear()
                                    remoteOwnerListings.addAll(refreshedRows)
                                    ownerInventoryLoaded = true
                                }
                                val remoteDraft = refreshedRows?.firstOrNull { it.serverPropertyId == serverId }
                                    ?: listing.copy(id = -(serverId ?: listing.id), serverPropertyId = serverId, isPublished = false, serverUnits = emptyList())
                                if (serverId != null && remoteOwnerListings.none { it.serverPropertyId == serverId }) {
                                    remoteOwnerListings.add(0, remoteDraft)
                                }
                                val previous = customListings.indexOfFirst { it.serverPropertyId != null && it.serverPropertyId == serverId }
                                if (previous >= 0) customListings[previous] = remoteDraft else customListings.add(0, remoteDraft)
                                ListingStore.save(context, customListings.toList())
                                selectedId = remoteDraft.id
                                notify("${listing.name} was saved to the server and submitted for review. ${if (remoteDraft.serverUnits.isNotEmpty()) "Live room/bed inventory is loaded." else "Inventory will refresh when server access is available."}")
                            } catch (e: Exception) {
                                val localDraft = listing.copy(isPublished = false)
                                customListings.add(0, localDraft); ListingStore.save(context, customListings.toList()); selectedId = localDraft.id
                                notify("Server save failed; a local draft was kept. ${e.message ?: "Check API connection and owner token."}")
                            }
                        }
                    } else {
                        customListings.add(0, listing.copy(isPublished = false))
                        ListingStore.save(context, customListings.toList())
                        selectedId = listing.id
                        notify("Draft saved on this device. Configure an API URL and owner token to publish to the shared marketplace.")
                        route = "owner_properties"
                    }
                }, onCancel = { route = "owner_properties" })
                "edit_pg" -> AddPropertyWizard(onPublish = { listing ->
                    val index = customListings.indexOfFirst { it.id == listing.id }
                    if (index >= 0) customListings[index] = listing else customListings.add(0, listing)
                    ListingStore.save(context, customListings.toList())
                    selectedId = listing.id
                    notify("${listing.name} updated in local demo search")
                    route = "owner_properties"
                }, onCancel = { route = "owner_properties" }, initial = selectedListing)
                "owner_beds" -> OwnerBedsScreen(ownerListings, selectedId, { listing, bedOptions ->
                    residentPropertyName = listing.name
                    residentBedOptions = bedOptions
                    residentListing = listing
                    residentCandidates = emptyList()
                    residentCandidatesError = ""
                    residentCandidatesLoading = false
                    residentStaySubmitting = false
                    route = "add_resident"
                    listing.serverPropertyId?.let { loadResidentCandidates(it) }
                }, { listing, unit, status ->
                    val baseUrl = preferences.apiBaseUrl.trim()
                    val token = if (baseUrl.isNotBlank()) StayApiClient.currentSessionToken(baseUrl) else null
                    if (token == null) {
                        notify("Connect the API with an owner-authorized session before changing server inventory.")
                    } else scope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                StayApiClient.updateUnitStatus(baseUrl, token, unit.id, status.lowercase(Locale.ROOT))
                            }
                            val updatedUnit = unit.copy(status = response.optString("status", status.lowercase(Locale.ROOT)))
                            fun updateListing(source: PgListing): PgListing {
                                val units = source.serverUnits.map { if (it.id == unit.id) updatedUnit else it }
                                return source.copy(
                                    serverUnits = units,
                                    totalBeds = units.size,
                                    totalRooms = units.map { it.roomCode }.distinct().size.coerceAtLeast(1),
                                    availableBeds = units.count { it.status.equals("available", true) }
                                )
                            }
                            val remoteIndex = remoteOwnerListings.indexOfFirst { it.serverPropertyId == listing.serverPropertyId }
                            if (remoteIndex >= 0) remoteOwnerListings[remoteIndex] = updateListing(remoteOwnerListings[remoteIndex])
                            val localIndex = customListings.indexOfFirst { it.serverPropertyId == listing.serverPropertyId && listing.serverPropertyId != null }
                            if (localIndex >= 0) {
                                customListings[localIndex] = updateListing(customListings[localIndex])
                                ListingStore.save(context, customListings.toList())
                            }
                            notify("${listing.name}: ${unit.unitCode} updated to ${response.optString("status", status.lowercase(Locale.ROOT))} on the server.")
                        } catch (e: Exception) {
                            notify("Server did not confirm the inventory change; the displayed status is unchanged. ${e.message ?: "Check owner access and retry."}")
                        }
                    }
                }, { notify(it) }, { route = "owner_properties" })
                "owner_enquiries" -> FeatureRecordsScreen("enquiries", ownerMode = true) { route = "feature_hub" }
                "owner_residents" -> FeatureRecordsScreen("residents", ownerMode = true) { route = "feature_hub" }
                "add_resident" -> {
                    val activeListing = residentListing
                    if (activeListing?.serverPropertyId != null) {
                        AddServerResidentScreen(
                            listing = activeListing,
                            candidates = residentCandidates,
                            candidatesLoading = residentCandidatesLoading,
                            candidatesError = residentCandidatesError,
                            isSubmitting = residentStaySubmitting,
                            onRetryCandidates = { activeListing.serverPropertyId?.let { loadResidentCandidates(it) } },
                            onCancel = { route = "owner_beds" },
                            onCreateStay = { candidate, unit, rent, deposit, joiningDate ->
                                val serverPropertyId = activeListing.serverPropertyId
                                val baseUrl = preferences.apiBaseUrl.trim()
                                val token = if (baseUrl.isNotBlank()) StayApiClient.currentSessionToken(baseUrl) else null
                                if (serverPropertyId == null) {
                                    residentCandidatesError = "This property is not connected to the StayEasy server."
                                } else if (token == null) {
                                    residentCandidatesError = "Sign in with an owner-authorized account before assigning a resident."
                                } else if (!residentStaySubmitting) {
                                    residentStaySubmitting = true
                                    residentCandidatesError = ""
                                    scope.launch {
                                        try {
                                            val created = withContext(Dispatchers.IO) {
                                                StayApiClient.createOwnerStay(
                                                    baseUrl = baseUrl,
                                                    token = token,
                                                    propertyId = serverPropertyId,
                                                    unitId = unit.id,
                                                    residentId = candidate.id,
                                                    joiningDate = joiningDate,
                                                    agreedMonthlyRent = rent,
                                                    depositHeld = deposit
                                                )
                                            }
                                            val refreshedRows = runCatching {
                                                withContext(Dispatchers.IO) {
                                                    StayApiClient.ownerProperties(baseUrl, token)
                                                }
                                            }.getOrNull()
                                            if (refreshedRows != null) {
                                                remoteOwnerListings.clear()
                                                remoteOwnerListings.addAll(refreshedRows)
                                                val refreshedListing = refreshedRows.firstOrNull { it.serverPropertyId == serverPropertyId }
                                                if (refreshedListing != null) {
                                                    val localIndex = customListings.indexOfFirst { it.serverPropertyId == serverPropertyId }
                                                    if (localIndex >= 0) customListings[localIndex] = refreshedListing
                                                    ListingStore.save(context, customListings.toList())
                                                }
                                            }
                                            val roomNumber = unit.roomCode.removePrefix("ROOM-").removePrefix("R")
                                            val bedLabel = if (unit.rentableType.equals("room", true)) {
                                                "Private room"
                                            } else {
                                                "Bed ${unit.unitCode.substringAfterLast("-", unit.unitCode).removePrefix("B")}"
                                            }
                                            val unitLabel = "Room $roomNumber · $bedLabel"
                                            val stayId = created.optLong("id")
                                            StayRecordStore.add(context, StayRecord(
                                                id = "server-resident-${candidate.id}",
                                                category = "residents",
                                                title = candidate.displayName,
                                                person = candidate.email,
                                                property = activeListing.name,
                                                unit = unitLabel,
                                                date = joiningDate,
                                                amount = rent,
                                                status = "Active",
                                                details = "Verified StayEasy customer account · ${candidate.email}. Resident details are linked to the server account.",
                                                source = "Server"
                                            ))
                                            StayRecordStore.add(context, StayRecord(
                                                id = "server-stay-$stayId",
                                                category = "stays",
                                                title = "Stay · ${candidate.displayName}",
                                                details = "Server-confirmed resident stay. Deposit recorded: ${money(deposit)}.",
                                                person = candidate.displayName,
                                                property = activeListing.name,
                                                unit = unitLabel,
                                                date = created.optString("joining_date", joiningDate),
                                                amount = rent,
                                                status = created.optString("status", "active").replaceFirstChar { it.uppercase(Locale.ROOT) },
                                                source = "Server"
                                            ))
                                            StayRecordStore.notify(context, "Resident stay created", "${candidate.displayName} · $unitLabel")
                                            notify(
                                                if (refreshedRows != null) "Stay created on the server. Room/bed inventory has been refreshed."
                                                else "Stay created on the server, but inventory could not refresh. Reopen Rooms & beds to sync it."
                                            )
                                            route = "records:residents"
                                        } catch (e: Exception) {
                                            residentCandidatesError = "The server did not confirm this stay. No local assignment was made. ${e.message ?: "Check the connection and retry."}"
                                        } finally {
                                            residentStaySubmitting = false
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        AddResidentScreen(residentPropertyName, residentBedOptions, { name, phone, rent, bed, joiningDate ->
                            StayRecordStore.add(context, StayRecord(category = "residents", title = name, person = phone, property = residentPropertyName, amount = rent, unit = bed, status = "Active", details = "Resident manually added in the local prototype. Joining date: $joiningDate. Monthly rent: ${money(rent)}."))
                            StayRecordStore.add(context, StayRecord(category = "stays", title = "Stay · $name", details = "Resident assigned to $bed at $residentPropertyName. Joining date: $joiningDate. Confirm agreement terms and first payment.", person = name, property = residentPropertyName, unit = bed, date = joiningDate, amount = rent, status = "Active"))
                            val existingBed = StayRecordStore.load(context, "beds").firstOrNull { it.property == residentPropertyName && it.unit == bed }
                            if (existingBed != null) {
                                StayRecordStore.update(context, "beds", existingBed.copy(
                                    status = "Occupied",
                                    person = name,
                                    details = "Assigned to $name. Joining date: $joiningDate. Monthly rent: ${money(rent)}."
                                ))
                            } else {
                                StayRecordStore.add(context, StayRecord(
                                    category = "beds", title = bed, property = residentPropertyName, unit = bed,
                                    person = name, status = "Occupied",
                                    details = "Assigned to $name. Joining date: $joiningDate. Monthly rent: ${money(rent)}."
                                ))
                            }
                            StayRecordStore.notify(context, "Resident added", "$name · $bed")
                            notify("Resident and stay records saved; $bed marked occupied")
                            route = "records:residents"
                        }, { route = "owner_beds" })
                    }
                }
                "owner_payments" -> FeatureRecordsScreen("payments", ownerMode = true) { route = "feature_hub" }
                "owner_issues" -> FeatureRecordsScreen("issues", ownerMode = true) { route = "feature_hub" }
                "owner_analytics" -> FeatureRecordsScreen("reports", ownerMode = true) { route = "feature_hub" }
                "owner_settings" -> ThemeSettingsScreen(preferences, { updated -> onPreferencesChanged(updated) }, { route = "feature_hub" })
                else -> if (route.startsWith("records:")) {
                    FeatureRecordsScreen(route.substringAfter("records:"), ownerMode) { route = "feature_hub" }
                } else OwnerMoreScreen(
                    onProperties = { route = "owner_properties" },
                    onResidents = { route = "records:residents" },
                    onMoveOutRequests = { route = "records:move_out_requests" },
                    onIssues = { route = "records:issues" },
                    onAnalytics = { route = "records:reports" },
                    onTenant = { ownerMode = false; route = "home" },
                    onProfile = { route = "feature_hub" }
                )
            }
        }
    }
}

private data class NavItem(val label: String, val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun HomeScreen(
    location: String, onLocationChange: (String) -> Unit, guestChoice: String, onGuestChoice: (String) -> Unit,
    maxBudget: Float, listings: List<PgListing>, favorites: List<Long>, onListing: (Long) -> Unit, onFavorite: (Long) -> Unit,
    onSearch: () -> Unit, onMap: () -> Unit, onFilters: () -> Unit, onOwner: () -> Unit, onNotifications: () -> Unit, onProfile: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark()
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("StayEasy", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SE.Ink, letterSpacing = (-0.8).sp)
                Text("Find your next home", fontSize = 11.sp, color = SE.Muted)
            }
            RoundIconButton(Icons.Filled.Notifications, onNotifications)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(42.dp).clip(CircleShape).background(SE.BlueSoft).clickable(onClick = onProfile), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, null, tint = SE.Blue)
            }
        }
        Spacer(Modifier.height(25.dp))
        Text("Find a place that", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SE.Ink, lineHeight = 32.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("feels like ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SE.Ink, lineHeight = 32.sp)
            Text("you.", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = SE.Orange, lineHeight = 32.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text("Thoughtful stays for students, professionals, friends and couples.", color = SE.Muted, fontSize = 14.sp, lineHeight = 20.sp)
        Spacer(Modifier.height(18.dp))
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = SE.Blue)
                    Spacer(Modifier.width(7.dp))
                    Column(Modifier.weight(1f)) {
                        Text("WHERE TO?", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SE.Muted, letterSpacing = 1.sp)
                        TextField(
                            value = location, onValueChange = onLocationChange, singleLine = true,
                            placeholder = { Text("Area, office, college or landmark", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = SE.Text, unfocusedTextColor = SE.Text, cursorColor = SE.Blue),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }
                    RoundIconButton(Icons.Filled.Map, onMap, tint = SE.Blue)
                }
                HorizontalDivider(color = SE.Border)
                Spacer(Modifier.height(11.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CalendarToday, null, tint = SE.Muted, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MOVE-IN", color = SE.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Flexible / select later", color = SE.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.width(1.dp).height(34.dp).background(SE.Border))
                    Spacer(Modifier.width(13.dp))
                    Icon(Icons.Filled.Payments, null, tint = SE.Muted, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Column(Modifier.weight(1f)) {
                        Text("MONTHLY BUDGET", color = SE.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                        Text("Up to ${money(maxBudget.toInt())}", color = SE.Text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = SE.Muted)
                }
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Explore stays", Icons.Filled.Search, onSearch)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onFilters, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Filled.Tune, null, modifier = Modifier.size(16.dp), tint = SE.Blue)
                    Spacer(Modifier.width(6.dp)); Text("Filters & preferences", color = SE.Blue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("A stay that suits you", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SE.Ink)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Any", "Boys", "Girls", "Mixed", "Couples").forEach { option ->
                FilterChip(selected = guestChoice == option, onClick = { onGuestChoice(option) }, label = { Text(if (option == "Any") "All stays" else if (option == "Mixed") "Mixed / all genders" else if (option == "Couples") "Couples welcome" else "$option PGs") }, leadingIcon = if (guestChoice == option) ({ Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp)) }) else null, shape = RoundedCornerShape(15.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Loved by people like you", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SE.Ink)
                Text("Demo listings · Pune", color = SE.Muted, fontSize = 12.sp)
            }
            TextButton(onClick = onSearch) { Text("See all", color = SE.Blue) }
        }
        listings.take(3).forEach { ListingCard(it, favorites.contains(it.id), { onListing(it.id) }, { onFavorite(it.id) }, compact = false) }
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SE.Ink), modifier = Modifier.fillMaxWidth().clickable(onClick = onOwner)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Apartment, null, tint = Color.White) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Manage a PG?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Occupancy, enquiries and payments in one place.", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Demo listings are illustrative and are not real offers.", color = SE.Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SearchResultsScreen(
    location: String, onLocation: (String) -> Unit, query: String, onQuery: (String) -> Unit,
    guestChoice: String, onGuest: (String) -> Unit, budget: Float, onBudget: (Float) -> Unit,
    listings: List<PgListing>, favorites: List<Long>, onSelect: (Long) -> Unit, onMap: () -> Unit, onFilters: () -> Unit, onFavorite: (Long) -> Unit, onSaveSearch: () -> Unit, onClearFilters: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Explore stays", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = SE.Ink)
                Text("In and around ${location.ifBlank { "Pune" }}", color = SE.Muted, fontSize = 12.sp, modifier = Modifier.clickable { onLocation(location) })
            }
            RoundIconButton(Icons.Filled.Map, onMap, tint = SE.Blue)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("PG name, locality or landmark", fontSize = 13.sp) }, leadingIcon = { Icon(Icons.Filled.Search, null) }, shape = RoundedCornerShape(16.dp), colors = fieldColors())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onFilters, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Icon(Icons.Filled.FilterList, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Filters") }
            OutlinedButton(onClick = onMap, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) { Icon(Icons.Filled.Map, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Map") }
            Spacer(Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.Sort, null, tint = SE.Muted, modifier = Modifier.size(18.dp))
            Text("Recommended", fontSize = 11.sp, color = SE.Muted)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Any", "Boys", "Girls", "Mixed", "Couples").forEach { option ->
                FilterChip(selected = guestChoice == option, onClick = { onGuest(option) }, label = { Text(if (option == "Any") "All" else option) }, shape = RoundedCornerShape(12.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Monthly rent · Up to ${money(budget.toInt())}", fontSize = 12.sp, color = SE.Muted)
        RentBudgetSlider(budget, onBudget)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onSaveSearch, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 3.dp)) { Icon(Icons.Filled.FavoriteBorder, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Save this search", color = SE.Blue, fontSize = 11.sp) } }
        HorizontalDivider(color = SE.Border)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${listings.size} stays", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp)); Text("· sample listings", color = SE.Muted, fontSize = 11.sp)
        }
        if (listings.isEmpty()) {
            EmptyState("No stays match yet", "Try a wider rent limit, remove an optional filter, or clear all preferences.", "Clear all filters") { onClearFilters() }
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxSize()) {
                items(listings, key = { it.id }) { p -> ListingCard(p, favorites.contains(p.id), { onSelect(p.id) }, { onFavorite(p.id) }, compact = true) }
            }
        }
    }
}

@Composable
private fun LocationSearchScreen(location: String, onPick: (String) -> Unit, onBack: () -> Unit) {
    var text by remember { mutableStateOf(location) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val places = listOf("Hinjawadi Phase 1, Pune", "Baner, Pune", "Wakad, Pune", "Kothrud, Pune", "Viman Nagar, Pune", "Kharadi, Pune", "Aundh, Pune", "Pune Railway Station")
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        ScreenHeader("Search a location", "Try an area, office, college or landmark", onBack = onBack)
        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null, tint = SE.Blue) }, trailingIcon = { if (text.isNotEmpty()) Icon(Icons.Filled.Close, "Clear", modifier = Modifier.clickable { text = "" }) }, placeholder = { Text("Search Pune or nearby") }, shape = RoundedCornerShape(16.dp), colors = fieldColors())
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SE.BlueSoft).clickable { onPick("Near me, Pune") }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocationOn, null, tint = SE.Blue); Spacer(Modifier.width(10.dp)); Column { Text("Use nearby area (demo)", color = SE.Blue, fontWeight = FontWeight.SemiBold); Text("Real GPS will ask permission only after you opt in", color = SE.Muted, fontSize = 11.sp) }
        }
        val recentPlaces = remember(context) { StayUserDataStore.recentLocations(context) }
        if (recentPlaces.isNotEmpty()) {
            Spacer(Modifier.height(14.dp)); Text("RECENT SEARCHES", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            recentPlaces.forEach { place ->
                Row(Modifier.fillMaxWidth().clickable { onPick(place) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.History, null, tint = SE.Muted, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp))
                    Text(place, color = SE.Text, fontSize = 13.sp, modifier = Modifier.weight(1f)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = SE.Muted, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(15.dp)); Text("SUGGESTED AREAS", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        places.filter { text.isBlank() || text.equals("Pune, Maharashtra", true) || it.contains(text, true) || "Pune".contains(text, true) }.forEach { place ->
            Row(Modifier.fillMaxWidth().clickable { onPick(place) }.padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(SE.Background), contentAlignment = Alignment.Center) { Icon(Icons.Filled.LocationOn, null, tint = SE.Muted) }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(place.substringBefore(","), color = SE.Text, fontWeight = FontWeight.SemiBold); Text(place.substringAfter(", ", "Pune"), color = SE.Muted, fontSize = 11.sp) }; Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = SE.Muted, modifier = Modifier.size(17.dp))
            }
            HorizontalDivider(color = SE.Border)
        }
        Spacer(Modifier.weight(1f))
        Text("Demo autocomplete. Google Places Autocomplete will replace this list when map credentials are configured.", color = SE.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun MapSearchScreen(location: String, onLocation: (String) -> Unit, listings: List<PgListing>, onSelect: (Long) -> Unit, onList: () -> Unit, onFilters: () -> Unit) {
    var selected by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf(location) }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Find stays on the map", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = SE.Ink); Text("Search by area, office or landmark", fontSize = 11.sp, color = SE.Muted) }
            RoundIconButton(Icons.Filled.FilterList, onFilters, tint = SE.Blue)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(value = query, onValueChange = { query = it; onLocation(it) }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) }, trailingIcon = { Icon(Icons.Filled.Close, null, modifier = Modifier.clickable { query = "" }) }, placeholder = { Text("Search places") }, shape = RoundedCornerShape(15.dp), colors = fieldColors())
        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFFECEFEA))) {
            DemoMapArt(Modifier.fillMaxSize())
            val safe = listings.ifEmpty { demoListings }
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val positions = listOf(Offset(0.16f, 0.24f), Offset(0.62f, 0.19f), Offset(0.42f, 0.43f), Offset(0.76f, 0.57f), Offset(0.21f, 0.70f), Offset(0.57f, 0.79f))
                safe.take(6).forEachIndexed { index, listing ->
                    val pos = positions[index % positions.size]
                    val isSelected = index == selected
                    Box(Modifier.offset(x = maxWidth * pos.x, y = maxHeight * pos.y).clip(RoundedCornerShape(14.dp)).background(if (isSelected) SE.Ink else Color.White).border(1.dp, if (isSelected) SE.Ink else SE.Border, RoundedCornerShape(14.dp)).clickable { selected = index }.padding(horizontal = 10.dp, vertical = 7.dp)) {
                        Text(money(listing.monthlyRent), color = if (isSelected) Color.White else SE.Ink, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
            Column(Modifier.align(Alignment.TopStart).padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                MapActionPill("Pune sample", Icons.Filled.Map) { selected = 0 }
                MapActionPill("Near me", Icons.Filled.LocationOn) { query = "Near me, Pune"; onLocation(query) }
            }
            Box(Modifier.align(Alignment.BottomEnd).padding(12.dp).clip(CircleShape).background(SE.Surface).clickable { selected = 0 }.padding(12.dp)) { Icon(Icons.Filled.LocationOn, null, tint = SE.Blue) }
        }
        Spacer(Modifier.height(10.dp))
        val current = listings.getOrNull(selected) ?: demoListings.first()
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth().clickable { onSelect(current.id) }) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                PropertyCover(current.photoUri, selected, Modifier.size(94.dp, 96.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(current.name, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(current.area, color = SE.Muted, fontSize = 11.sp)
                    Text("${current.sharing} · ${current.availableBeds} beds available", color = SE.Muted, fontSize = 11.sp)
                    Text("${money(current.monthlyRent)} / month", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = SE.Muted)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onList, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Filled.Menu, null); Spacer(Modifier.width(8.dp)); Text("Switch to list results") }
        Spacer(Modifier.height(8.dp))
    }
}


@Composable
private fun FilterScreen(
    guestChoice: String, onGuest: (String) -> Unit, budget: Float, onBudget: (Float) -> Unit,
    selectedAmenities: MutableList<String>, filters: DiscoveryFilters, onFilters: (DiscoveryFilters) -> Unit,
    onApply: () -> Unit, onReset: () -> Unit
) {
    var draft by remember { mutableStateOf(filters) }
    var minRentText by remember { mutableStateOf(filters.minRent.takeIf { it > 0 }?.toString() ?: "") }
    var minDepositText by remember { mutableStateOf(if (filters.filterDeposit && filters.minDeposit > 0) filters.minDeposit.toString() else "") }
    var maxDepositText by remember { mutableStateOf(if (filters.filterDeposit && filters.maxDeposit < 100000) filters.maxDeposit.toString() else "") }
    var depositEnabled by remember { mutableStateOf(filters.filterDeposit) }
    var distanceEnabled by remember { mutableStateOf(filters.filterDistance) }
    BackHandler { onFilters(draft); onApply() }
    val amenities = listOf(
        "Wi-Fi", "AC", "Meals", "Attached bath", "Laundry", "Parking", "Power backup",
        "Housekeeping", "Study desk", "Hot water", "Lift", "Washing machine",
        "Refrigerator", "CCTV / security", "Water purifier", "Geyser", "Balcony",
        "Kitchen access", "Gym", "Work desk"
    )
    val activeCount = (if (guestChoice != "Any") 1 else 0) +
        (if (draft.minRent > 0) 1 else 0) + (if (draft.filterDeposit) 1 else 0) +
        (if (draft.filterDistance) 1 else 0) + draft.roomTypes.size + selectedAmenities.size +
        listOf(draft.availableOnly, draft.foodIncluded, draft.acRequired, draft.attachedBathroom,
            draft.furnishedOnly, draft.verifiedOnly, draft.cookingAllowed).count { it }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(13.dp))
        ScreenHeader(
            "Filters & preferences",
            "Choose what matters. Everything else stays flexible.",
            onBack = { onFilters(draft); onApply() },
            trailing = {
                TextButton(onClick = {
                    onReset()
                    draft = DiscoveryFilters()
                    minRentText = ""
                    minDepositText = ""
                    maxDepositText = ""
                    depositEnabled = false
                    distanceEnabled = true
                }) { Text("Reset all", color = SE.Blue) }
            }
        )
        Text("$activeCount preferences selected", color = SE.Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp, bottom = 14.dp))

        FilterSection("Who can stay?", "Choose eligibility that suits your household or room policy.") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Any", "Boys", "Girls", "Mixed", "Couples").forEach { option ->
                    FilterChip(selected = guestChoice == option, onClick = { onGuest(option) }, label = {
                        Text(when (option) { "Any" -> "All stays"; "Mixed" -> "All genders"; "Couples" -> "Couples welcome"; else -> option })
                    })
                }
            }
        }
        FilterSection("Monthly rent", "Choose your maximum per person. The slider gives more control below ₹30,000.") {
            Text("Up to ${money(budget.toInt())}", fontSize = 22.sp, color = SE.Ink, fontWeight = FontWeight.Bold)
            RentBudgetSlider(budget, onBudget)
            OutlinedTextField(
                value = minRentText,
                onValueChange = {
                    minRentText = it.filter(Char::isDigit).take(7)
                    draft = draft.copy(minRent = minRentText.toIntOrNull()?.coerceIn(100, 100000) ?: 0)
                },
                modifier = Modifier.fillMaxWidth(), label = { Text("Minimum rent (optional)") },
                supportingText = { Text("Leave empty to include any rent from ₹100.") },
                singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors()
            )
        }
        FilterSection("Security deposit", "Optional. Leave this off to include all deposit amounts.") {
            FilterSwitch("Set a deposit range", "Only match listings inside your chosen range.", depositEnabled) { enabled ->
                depositEnabled = enabled
                draft = draft.copy(
                    filterDeposit = enabled,
                    minDeposit = if (enabled) minDepositText.toIntOrNull() ?: 0 else 0,
                    maxDeposit = if (enabled) maxDepositText.toIntOrNull() ?: 100000 else 100000
                )
            }
            if (depositEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    OutlinedTextField(
                        value = minDepositText,
                        onValueChange = {
                            minDepositText = it.filter(Char::isDigit).take(8)
                            draft = draft.copy(minDeposit = minDepositText.toIntOrNull()?.coerceAtMost(100000) ?: 0)
                        },
                        modifier = Modifier.weight(1f), label = { Text("Minimum ₹") },
                        singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors()
                    )
                    OutlinedTextField(
                        value = maxDepositText,
                        onValueChange = {
                            maxDepositText = it.filter(Char::isDigit).take(8)
                            draft = draft.copy(maxDeposit = maxDepositText.toIntOrNull()?.coerceAtMost(100000) ?: 100000)
                        },
                        modifier = Modifier.weight(1f), label = { Text("Maximum ₹") },
                        singleLine = true, shape = RoundedCornerShape(12.dp), colors = fieldColors()
                    )
                }
            }
        }
        FilterSection("Distance", "Default radius: 5 km. Turn this off to search without a radius preference.") {
            FilterSwitch("Limit distance from search area", "Verified distances must be inside your radius.", distanceEnabled) { enabled ->
                distanceEnabled = enabled
                draft = draft.copy(filterDistance = enabled)
            }
            if (distanceEnabled) {
                Text("Within ${String.format(Locale.getDefault(), "%.0f", draft.maxDistanceKm)} km", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Slider(value = draft.maxDistanceKm, onValueChange = { draft = draft.copy(maxDistanceKm = it) }, valueRange = 1f..25f, steps = 23)
                Text("Listings without verified distance remain visible, but are not treated as confirmed matches to the radius.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }
        FilterSection("Room & stay preferences", "All of these are optional.") {
            Text("Room type", color = SE.Text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            FlowChips(
                listOf("Private room", "1 sharing", "2 sharing", "3 sharing", "4+ sharing"),
                remember(draft.roomTypes) { mutableStateListOf<String>().apply { addAll(draft.roomTypes) } },
                onPick = { option -> draft = draft.copy(roomTypes = if (draft.roomTypes.contains(option)) draft.roomTypes - option else draft.roomTypes + option) }
            )
            Spacer(Modifier.height(4.dp))
            FilterSwitch("Available now", "Only stays reporting a free bed.", draft.availableOnly) { draft = draft.copy(availableOnly = it) }
            FilterSwitch("Food available", "Include properties that list food or meal service.", draft.foodIncluded) { draft = draft.copy(foodIncluded = it) }
            FilterSwitch("Air conditioning", "AC is listed as a facility.", draft.acRequired) { draft = draft.copy(acRequired = it) }
            FilterSwitch("Attached bathroom", "Prefer a room with its own bathroom.", draft.attachedBathroom) { draft = draft.copy(attachedBathroom = it) }
            FilterSwitch("Furnished", "Show furnished stays.", draft.furnishedOnly) { draft = draft.copy(furnishedOnly = it) }
            FilterSwitch("Cooking permitted", "The owner lists cooking access as allowed.", draft.cookingAllowed) { draft = draft.copy(cookingAllowed = it) }
            FilterSwitch("Owner verified", "Only listings carrying a verification flag.", draft.verifiedOnly) { draft = draft.copy(verifiedOnly = it) }
            FormLabel("MOVE-IN DATE (OPTIONAL)")
            OutlinedTextField(
                draft.moveInDate, { draft = draft.copy(moveInDate = it) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                placeholder = { Text("e.g. Available now or 15 Nov 2026") },
                shape = RoundedCornerShape(12.dp), colors = fieldColors()
            )
            FormLabel("MINIMUM STAY (OPTIONAL)")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(0, 1, 3, 6, 12).forEach { months ->
                    FilterChip(selected = draft.minStayMonths == months, onClick = { draft = draft.copy(minStayMonths = months) }, label = { Text(if (months == 0) "Any" else "≤$months mo") })
                }
            }
        }
        FilterSection("Amenities", "Pick any number. With none selected, amenities will not narrow your results.") {
            FlowChips(amenities, selectedAmenities)
            if (selectedAmenities.isNotEmpty()) {
                Text("${selectedAmenities.size} selected", color = SE.Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { selectedAmenities.clear() }, modifier = Modifier.align(Alignment.End)) { Text("Clear amenities", color = SE.Blue) }
            }
        }
        FilterSection("Sort results", "Choose how matching stays should be ordered.") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Recommended", "Price: low to high", "Distance", "Availability", "Rating").forEach { option ->
                    FilterChip(selected = draft.sortBy == option, onClick = { draft = draft.copy(sortBy = option) }, label = { Text(option, fontSize = 10.sp) })
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Show matching stays", Icons.Filled.Search) { onFilters(draft); onApply() }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun FilterSection(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SE.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = SE.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (subtitle != null) Text(subtitle, color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp)
            content()
        }
    }
}
@Composable
private fun FilterSwitch(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = SE.Text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(subtitle, color = SE.Muted, fontSize = 10.sp) }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
@Composable
private fun ListingDetailsScreen(p: PgListing, isFavorite: Boolean, onFavorite: () -> Unit, onMessage: () -> Unit, onRequest: () -> Unit, onBack: () -> Unit, onBook: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box {
            PropertyCover(p.photoUri, (p.id % 4).toInt(), Modifier.fillMaxWidth().height(285.dp).clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)))
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, onBack, tint = SE.Ink)
                Spacer(Modifier.weight(1f))
                RoundIconButton(Icons.Filled.Share, { val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${p.name} · ${p.area} · from ${money(p.monthlyRent)}/month — view on StayEasy (demo listing)") }; context.startActivity(Intent.createChooser(intent, "Share StayEasy listing")) }, tint = SE.Ink)
                Spacer(Modifier.width(8.dp)); RoundIconButton(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, onFavorite, tint = if (isFavorite) SE.Orange else SE.Ink)
            }
            Box(Modifier.align(Alignment.BottomStart).padding(15.dp).clip(RoundedCornerShape(10.dp)).background(SE.Ink.copy(alpha = 0.90f)).padding(horizontal = 10.dp, vertical = 6.dp)) { Text(if (p.isDemo) "SAMPLE LISTING" else if (p.isVerified) "VERIFIED LISTING" else "OWNER-ADDED · NOT VERIFIED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) }
        }
        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = SE.Ink, lineHeight = 29.sp)
                    Spacer(Modifier.height(4.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.LocationOn, null, tint = SE.Muted, modifier = Modifier.size(15.dp)); Text("${p.area}${if (p.pinCode.isNotBlank()) " · ${p.pinCode}" else ""} · ${if (p.distanceIsVerified) "calculated distance" else if (p.isDemo) "sample distance" else "distance not verified"}${if (p.distanceIsVerified || p.isDemo) " ${p.distanceKm} km" else ""}", color = SE.Muted, fontSize = 11.sp) }
                }
                if (p.rating > 0) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF5DA)).padding(horizontal = 9.dp, vertical = 7.dp)) { Icon(Icons.Filled.Star, null, tint = Color(0xFFD99618), modifier = Modifier.size(15.dp)); Spacer(Modifier.width(3.dp)); Text("${p.rating}", fontWeight = FontWeight.Bold, color = SE.Ink, fontSize = 13.sp) }
            }
            Spacer(Modifier.height(13.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusPill(p.allowedFor, SE.BlueSoft, SE.Blue)
                if (p.couplesAllowed) StatusPill("Couples welcome", SE.OrangeSoft, SE.Orange)
                StatusPill("${p.availableBeds} beds available", SE.Mint, SE.MintText)
                if (p.isVerified) StatusPill("Owner verified", SE.BlueSoft, SE.Blue)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) { Text("Monthly rent", color = SE.Muted, fontSize = 11.sp); Text(money(p.monthlyRent), color = SE.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp); Text("per person / month", color = SE.Muted, fontSize = 11.sp) }
                Column(horizontalAlignment = Alignment.End) { Text("Security deposit", color = SE.Muted, fontSize = 11.sp); Text(p.deposit.takeIf { it > 0 }?.let { money(it) } ?: "Not specified", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("one-time · confirm terms", color = SE.Muted, fontSize = 9.sp) }
            }
            Spacer(Modifier.height(11.dp))
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SE.BlueSoft), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Move-in cost estimate", color = SE.Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp)); CompareLine("First month rent", money(p.monthlyRent)); CompareLine("Deposit", money(p.deposit))
                    if (p.foodMonthly > 0) CompareLine("First month food", money(p.foodMonthly))
                    if (p.utilityMonthlyEstimate > 0) CompareLine("Estimated utilities", money(p.utilityMonthlyEstimate))
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = SE.Blue.copy(alpha = .18f))
                    CompareLine("Indicative initial total", money(p.monthlyRent + p.deposit + p.foodMonthly + p.utilityMonthlyEstimate))
                    Text("Only amounts entered in this listing are included. Verify additional fees and refund terms directly with the owner.", color = SE.Muted, fontSize = 9.sp, lineHeight = 13.sp)
                }
            }
            if (p.foodMonthly > 0) {
                Spacer(Modifier.height(11.dp))
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SE.BlueSoft), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("Rent", color = SE.Muted, fontSize = 11.sp); Spacer(Modifier.weight(1f)); Text(money(p.monthlyRent), color = SE.Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        Spacer(Modifier.height(4.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text("Listed food charge", color = SE.Muted, fontSize = 11.sp); Spacer(Modifier.weight(1f)); Text(money(p.foodMonthly), color = SE.Text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        HorizontalDivider(Modifier.padding(vertical = 7.dp), color = SE.Blue.copy(alpha = 0.18f))
                        Row(verticalAlignment = Alignment.CenterVertically) { Text("Monthly total before utilities / other extras", color = SE.Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(money(p.monthlyRent + p.foodMonthly), color = SE.Ink, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            } else Text("No food charge is listed. Confirm any utilities or other compulsory charges with the owner.", color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(15.dp)); HorizontalDivider(color = SE.Border); Spacer(Modifier.height(15.dp))
            Text("A little about this place", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp)); Text(p.description, color = SE.Muted, fontSize = 13.sp, lineHeight = 20.sp)
            Spacer(Modifier.height(19.dp)); Text("What you'll have", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(10.dp)); FlowChips(p.amenities.ifEmpty { listOf("Furnished", "Shared lounge") }, remember { mutableStateListOf() }, selectable = false)
            Spacer(Modifier.height(19.dp)); Text("Room, policies & availability", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp)); DetailLine("Furnishing", if (p.isFurnished) "Furnished" else "Unfurnished", Icons.Filled.Home)
            DetailLine("Bathroom", p.bathroomType, Icons.Filled.Home)
            DetailLine("Food", p.foodPolicy + if (p.foodMonthly > 0) " · ${money(p.foodMonthly)}/month" else "", Icons.Filled.Restaurant)
            DetailLine("Cooking", if (p.cookingAllowed) "Permitted (confirm shared kitchen rules)" else "Not listed as permitted", Icons.Filled.Home)
            DetailLine("Visitors", p.visitorPolicy, Icons.Filled.Person)
            DetailLine("Curfew", p.curfewPolicy, Icons.Filled.Lock)
            DetailLine("Minimum stay", "${p.minimumStayMonths} month(s)", Icons.Filled.CalendarToday)
            DetailLine("Notice period", "${p.noticePeriodDays} days", Icons.Filled.CalendarToday)
            DetailLine("Expected vacancy", p.expectedVacancy, Icons.Filled.Bed)
            DetailLine("Availability check", "${if (p.isDemo) "Sample listing" else "Local listing"} · ${java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(java.util.Date(p.availabilityConfirmedAt))}", Icons.Filled.Refresh)
            if (p.isDemo) Text("All sample listing data, including ratings and distance, is illustrative and not verified.", color = SE.Orange, fontSize = 10.sp, lineHeight = 14.sp)
            Spacer(Modifier.height(20.dp)); Text("Room options", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(8.dp))
            RoomOption(p.sharing, p.monthlyRent, p.availableBeds, true, onRequest)
            Text("Looking for a different room type? Ask the owner about options and exact availability.", color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp)
            Spacer(Modifier.height(18.dp))
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Background)) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(SE.BlueSoft), contentAlignment = Alignment.Center) { Icon(Icons.Filled.VerifiedUser, null, tint = SE.Blue) }
                    Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Before you commit", fontWeight = FontWeight.Bold, color = SE.Ink, fontSize = 13.sp); Text("Confirm total charges, room rules, exact availability and refund terms with the owner.", fontSize = 11.sp, color = SE.Muted, lineHeight = 16.sp) }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onMessage, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Filled.ChatBubbleOutline, null); Spacer(Modifier.width(7.dp)); Text("Message") }
                Button(onClick = onRequest, modifier = Modifier.weight(1.35f).height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)) { Text("Request a visit", fontWeight = FontWeight.Bold); Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(17.dp)) }
            }
            Spacer(Modifier.height(4.dp)); TextButton(onClick = onBook, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Filled.Bed, null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Request a room / bed reservation", color = SE.Blue, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun VisitRequestScreen(p: PgListing, onBack: () -> Unit, onSubmit: (moveInDate: String, roomType: String, visitSlot: String, note: String) -> Unit) {
    var moveIn by remember { mutableStateOf(java.time.LocalDate.now().plusDays(7).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))) }
    var roomChoice by remember { mutableStateOf(p.sharing) }
    var visitChoice by remember { mutableStateOf("Tomorrow · 6:00 PM") }
    var note by remember { mutableStateOf("Hi, I am interested in this stay. Could I visit and confirm the total move-in cost?") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        ScreenHeader("Request a visit", "No booking or payment is taken at this step", onBack = onBack)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(SE.Surface).padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            PropertyCover(p.photoUri, (p.id % 4).toInt(), Modifier.size(82.dp, 86.dp)); Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(p.name, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(p.area, color = SE.Muted, fontSize = 10.sp); Text("${p.sharing} · ${money(p.monthlyRent)}/month", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 11.sp); Text("Deposit ${money(p.deposit)} · confirm terms", color = SE.Muted, fontSize = 9.sp) }
        }
        Spacer(Modifier.height(18.dp)); FormLabel("ROOM OPTION")
        FlowChips(listOf(p.sharing, "Private room", "Flexible"), remember { mutableStateListOf(roomChoice) }, singleSelection = true) { roomChoice = it }
        Spacer(Modifier.height(13.dp)); FormLabel("PLANNED MOVE-IN DATE")
        OutlinedTextField(moveIn, { moveIn = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Filled.CalendarToday, null) }, shape = RoundedCornerShape(14.dp), colors = fieldColors())
        Spacer(Modifier.height(14.dp)); FormLabel("PREFERRED VISIT TIME")
        listOf("Today · 7:00 PM", "Tomorrow · 11:00 AM", "Tomorrow · 6:00 PM", "Weekend · Flexible").forEach { slot ->
            Row(Modifier.fillMaxWidth().padding(bottom = 7.dp).clip(RoundedCornerShape(13.dp)).background(if(visitChoice == slot) SE.BlueSoft else Color.White).border(1.dp, if(visitChoice == slot) SE.Blue else SE.Border, RoundedCornerShape(13.dp)).clickable { visitChoice = slot }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(slot, color = SE.Text, fontWeight = if(visitChoice == slot) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f), fontSize = 12.sp); if(visitChoice == slot) Icon(Icons.Filled.CheckCircle, null, tint = SE.Blue) }
        }
        Spacer(Modifier.height(11.dp)); FormLabel("MESSAGE TO OWNER")
        OutlinedTextField(note, { note = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp), shape = RoundedCornerShape(14.dp), colors = fieldColors())
        Spacer(Modifier.height(14.dp)); Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = SE.OrangeSoft)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Filled.Lock, null, tint = SE.Orange); Spacer(Modifier.width(8.dp)); Text("This request does not reserve a bed. Wait for the owner to confirm availability, charges, visit time and reservation terms.", color = SE.Text, fontSize = 11.sp, lineHeight = 16.sp) } }
        Spacer(Modifier.height(19.dp)); PrimaryButton("Send request", Icons.AutoMirrored.Filled.ArrowForward) { onSubmit(moveIn, roomChoice, visitChoice, note) }
        Spacer(Modifier.height(10.dp)); Text(if (p.serverPropertyId != null) "Request is sent to the owner through the shared server. No bed is reserved and no payment is taken." else "This is a local demo listing; the request stays on this device.", color = SE.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun SavedScreen(listings: List<PgListing>, onSelect: (Long) -> Unit, onRemove: (Long) -> Unit, onExplore: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        ScreenHeader("Your shortlist", "Keep the good options together", trailing = { RoundIconButton(Icons.Filled.Share, {}) })
        Spacer(Modifier.height(14.dp))
        if (listings.isEmpty()) EmptyState("Your shortlist starts here", "Save places you like and compare them when you're ready.", "Explore PGs", onExplore)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(listings, key = { it.id }) { p -> ListingCard(p, true, { onSelect(p.id) }, { onRemove(p.id) }, compact = true, favoriteAction = true) } }
    }
}

@Composable
private fun EnquiriesScreen(onChat: () -> Unit, onDetails: () -> Unit, onExplore: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        ScreenHeader("Your enquiries", "Every conversation, in one place", trailing = { RoundIconButton(Icons.Filled.Add, onExplore, tint = SE.Blue) })
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatusPill("2 active", SE.BlueSoft, SE.Blue); StatusPill("1 visit planned", SE.Mint, SE.MintText) }
        Spacer(Modifier.height(15.dp))
        listOf(Triple("The Courtyard Co-Living", "Owner replied · 10 min ago", "Visit options available"), Triple("CityNest Living", "Waiting for owner · 1 hr ago", "Availability enquiry"), Triple("Maple House", "Visit planned · Tomorrow", "11:00 AM · Wakad")).forEachIndexed { i, item ->
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onChat() }) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                    PropertyCover("", i, Modifier.size(72.dp, 78.dp)); Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(item.first, fontWeight = FontWeight.Bold, color = SE.Ink, fontSize = 14.sp); Text(item.second, color = SE.Muted, fontSize = 11.sp); Text(item.third, color = SE.Blue, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = SE.Muted)
                }
            }
        }
        Spacer(Modifier.weight(1f)); TextButton(onClick = onDetails, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Browse more stays", color = SE.Blue) }
    }
}

@Composable
private fun ChatScreen(p: PgListing, onBack: () -> Unit, onSend: () -> Unit) {
    var message by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, onBack); Spacer(Modifier.width(10.dp)); Box(Modifier.size(40.dp).clip(CircleShape).background(SE.BlueSoft), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Apartment, null, tint = SE.Blue) }
            Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Text(p.name, color = SE.Ink, fontWeight = FontWeight.Bold); Text("Demo owner · usually replies quickly", color = SE.Muted, fontSize = 10.sp) }; Icon(Icons.Filled.Call, null, tint = SE.Blue, modifier = Modifier.clickable { onSend() })
        }
        Spacer(Modifier.height(16.dp)); HorizontalDivider(color = SE.Border)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.Center) { StatusPill("Today · Demo conversation", SE.Background, SE.Muted) }
        ChatBubble("Hi! I'm interested in the room. Is the bed available for my move-in date?", false)
        ChatBubble("Hello! Thanks for reaching out. We can confirm the exact bed and charges before you visit.", true)
        ChatBubble("Could I visit tomorrow evening?", false)
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Mint), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Column(Modifier.padding(13.dp)) { Text("Suggested next step", color = SE.MintText, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text("Arrange a visit and confirm the total move-in cost.", color = SE.Text, fontSize = 12.sp); Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = onSend, shape = RoundedCornerShape(11.dp)) { Icon(Icons.Filled.CalendarToday, null); Spacer(Modifier.width(6.dp)); Text("Request a visit") } } }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 9.dp)) {
            OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.weight(1f), placeholder = { Text("Write a message…", fontSize = 12.sp) }, shape = RoundedCornerShape(17.dp), colors = fieldColors(), maxLines = 3)
            Spacer(Modifier.width(7.dp)); Box(Modifier.size(48.dp).clip(CircleShape).background(SE.Blue).clickable { if (message.isNotBlank()) { onSend(); message = "" } }.padding(12.dp), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White) }
        }
        Text("Demo messages only. In production, messaging requires an authenticated backend.", color = SE.Muted, fontSize = 9.sp, modifier = Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun MyStayScreen(
    baseUrl: String,
    account: JSONObject?,
    onPayments: () -> Unit,
    onIssue: () -> Unit,
    onChat: () -> Unit
) {
    val accountRole = account?.optString("role").orEmpty()
    val accountId = account?.optString("id").orEmpty()
    val scope = rememberCoroutineScope()
    var stays by remember(accountId) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var moveOutRequests by remember(accountId) { mutableStateOf<List<JSONObject>>(emptyList()) }
    var loading by remember(accountId) { mutableStateOf(false) }
    var error by remember(accountId) { mutableStateOf("") }
    var syncMessage by remember(accountId) { mutableStateOf("") }
    var moveOutMessage by remember(accountId) { mutableStateOf("") }
    var refreshKey by remember(accountId) { mutableIntStateOf(0) }
    var moveOutDialogVisible by remember(accountId) { mutableStateOf(false) }
    var moveOutDateText by remember(accountId) { mutableStateOf("") }
    var moveOutReasonText by remember(accountId) { mutableStateOf("") }
    var moveOutError by remember(accountId) { mutableStateOf("") }
    var moveOutSubmitting by remember(accountId) { mutableStateOf(false) }

    LaunchedEffect(baseUrl, accountRole, accountId, refreshKey) {
        stays = emptyList()
        moveOutRequests = emptyList()
        error = ""
        syncMessage = ""
        if (accountRole != "customer") {
            syncMessage = if (account == null) {
                "Sign in with your verified tenant account to load shared stay details."
            } else {
                "My Stay is available to customer accounts. The account currently signed in is not a tenant."
            }
            loading = false
            return@LaunchedEffect
        }
        if (baseUrl.isBlank()) {
            error = "Connect the StayEasy API to load your stay from the server."
            loading = false
            return@LaunchedEffect
        }
        val token = StayApiClient.currentSessionToken(baseUrl)
        if (token == null) {
            error = "Your tenant session is unavailable. Sign in again to load shared stay details."
            loading = false
            return@LaunchedEffect
        }
        loading = true
        try {
            stays = withContext(Dispatchers.IO) { StayApiClient.myStays(baseUrl, token) }
            moveOutRequests = withContext(Dispatchers.IO) { StayApiClient.myMoveOutRequests(baseUrl, token) }
            syncMessage = "Synced from StayEasy · account-scoped"
        } catch (e: Exception) {
            error = e.message ?: "Could not load your stay or move-out requests. Check the API connection and retry."
        } finally {
            loading = false
        }
    }

    val currentStay = stays.firstOrNull {
        it.optString("status").lowercase(Locale.ROOT) in setOf("active", "notice_given")
    } ?: stays.firstOrNull()
    val status = currentStay?.optString("status", "").orEmpty()
        .replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    val propertyName = currentStay?.optString("property_name").orEmpty()
    val unitLabel = currentStay?.let {
        listOf(it.optString("room_code"), it.optString("unit_code"))
            .filter(String::isNotBlank).joinToString(" · ")
    }.orEmpty()
    val locality = currentStay?.let {
        listOf(it.optString("locality"), it.optString("city")).filter(String::isNotBlank).joinToString(", ")
    }.orEmpty()
    val currentMoveOutRequest = currentStay?.let { stay ->
        moveOutRequests.firstOrNull { it.optLong("resident_stay_id") == stay.optLong("id") }
    }
    val currentRequestStatus = currentMoveOutRequest?.optString("status").orEmpty().lowercase(Locale.ROOT)
    val canRequestMoveOut = currentStay != null &&
        currentStay.optString("status").equals("active", ignoreCase = true) &&
        currentRequestStatus !in setOf("pending", "approved")
    val earliestMoveOutDate = currentStay?.let { stay ->
        val today = java.time.LocalDate.now()
        val joining = runCatching { java.time.LocalDate.parse(stay.optString("joining_date")) }.getOrNull()
        val startDate = if (joining != null && joining.isAfter(today)) joining else today
        startDate.plusDays(stay.optInt("notice_period_days", 30).coerceIn(0, 365).toLong()).toString()
    }.orEmpty()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        ScreenHeader(
            "My stay",
            "Your home, payments and requests",
            trailing = { RoundIconButton(Icons.Filled.Notifications, {}) }
        )
        Spacer(Modifier.height(10.dp))
        if (accountRole == "customer") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (loading) "Refreshing your server stay…" else syncMessage.ifBlank { "Server stay not loaded" },
                    color = if (error.isNotBlank()) SE.Orange else SE.Blue,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { refreshKey++ },
                    enabled = !loading && !moveOutSubmitting,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(if (loading) "Loading…" else "Refresh", fontSize = 10.sp) }
            }
        }
        if (error.isNotBlank()) {
            Spacer(Modifier.height(9.dp))
            Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = SE.OrangeSoft)) {
                Text(error, color = SE.Orange, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(12.dp))
            }
        }
        if (moveOutMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = SE.BlueSoft)) {
                Text(moveOutMessage, color = SE.Blue, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(12.dp))
            }
        }

        if (account == null) {
            Spacer(Modifier.height(14.dp))
            Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = SE.Ink), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(17.dp)) {
                    StatusPill("SAMPLE PREVIEW", Color.White.copy(alpha = 0.13f), Color.White)
                    Spacer(Modifier.height(13.dp))
                    Text("Illustrative shared-living stay", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text("Room 204 · Bed B · Pune", color = Color.White.copy(alpha = 0.73f), fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Sample data only · no real stay is attached to this device.", color = Color.White.copy(alpha = 0.72f), fontSize = 10.sp)
                }
            }
        } else if (accountRole == "customer" && !loading && error.isBlank() && currentStay == null) {
            Spacer(Modifier.height(14.dp))
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Icon(Icons.Filled.Apartment, null, tint = SE.Blue)
                    Text("No server-backed stay yet", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Once an owner confirms your assignment, the property, room/bed, rent and deposit will appear here automatically.",
                        color = SE.Muted, fontSize = 12.sp, lineHeight = 18.sp
                    )
                }
            }
        } else if (currentStay != null) {
            Spacer(Modifier.height(14.dp))
            Card(shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = SE.Ink), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(status.uppercase(Locale.ROOT), Color.White.copy(alpha = 0.13f), Color.White)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.VerifiedUser, null, tint = Color(0xFFB7D2FF))
                    }
                    Spacer(Modifier.height(13.dp))
                    Text(propertyName.ifBlank { "Your property" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        listOf(unitLabel, locality).filter(String::isNotBlank).joinToString(" · ").ifBlank { "Unit details unavailable" },
                        color = Color.White.copy(alpha = 0.73f), fontSize = 12.sp
                    )
                    Spacer(Modifier.height(13.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("AGREED MONTHLY RENT", color = Color.White.copy(alpha = 0.62f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (currentStay.optInt("agreed_monthly_rent") > 0) money(currentStay.optInt("agreed_monthly_rent")) else "Not recorded",
                                color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp
                            )
                            Text("From the server stay record · no payment status implied", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
                        }
                        Button(onClick = onPayments, colors = ButtonDefaults.buttonColors(containerColor = SE.Orange), shape = RoundedCornerShape(13.dp)) {
                            Text("Payments", color = Color.White)
                        }
                    }
                }
            }
        } else if (loading) {
            Spacer(Modifier.height(18.dp))
            Text("Loading your current stay…", color = SE.Muted, fontSize = 12.sp)
        } else {
            Spacer(Modifier.height(12.dp))
            Text("Connect a verified tenant account to see your shared stay.", color = SE.Muted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(18.dp))
        Text("QUICK ACTIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SE.Muted, letterSpacing = 1.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            QuickAction("Payments", Icons.Filled.Payments, SE.BlueSoft, SE.Blue, onPayments, Modifier.weight(1f))
            QuickAction("Raise issue", Icons.Filled.ReportProblem, SE.OrangeSoft, SE.Orange, onIssue, Modifier.weight(1f))
            QuickAction("Contact owner", Icons.Filled.ChatBubbleOutline, SE.Lavender, SE.Ink, onChat, Modifier.weight(1f))
        }

        if (currentStay != null) {
            Spacer(Modifier.height(20.dp))
            Text("Your stay details", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SE.Ink)
            Spacer(Modifier.height(9.dp))
            DetailLine("Move-in date", currentStay.optString("joining_date").ifBlank { "Not recorded" }, Icons.Filled.CalendarToday)
            DetailLine("Security deposit", if (currentStay.optInt("deposit_held") > 0) "${money(currentStay.optInt("deposit_held"))} · recorded" else "Not recorded", Icons.Filled.Lock)
            DetailLine("Stay status", status.ifBlank { "Unknown" }, Icons.Filled.VerifiedUser)
            val noticeDate = currentStay.optString("notice_date")
            if (noticeDate.isNotBlank()) DetailLine("Notice date", noticeDate, Icons.Filled.CalendarToday)
            val moveOutDate = currentStay.optString("move_out_date")
            if (moveOutDate.isNotBlank()) DetailLine("Move-out date", moveOutDate, Icons.Filled.CalendarToday)

            if (currentMoveOutRequest != null) {
                Spacer(Modifier.height(16.dp))
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Move-out request", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            StatusPill(
                                currentMoveOutRequest.optString("status", "pending").replace('_', ' ').uppercase(Locale.ROOT),
                                if (currentRequestStatus == "pending") SE.OrangeSoft else SE.BlueSoft,
                                if (currentRequestStatus == "pending") SE.Orange else SE.Blue
                            )
                        }
                        Text("Requested date: ${currentMoveOutRequest.optString("requested_move_out_date")}", color = SE.Muted, fontSize = 11.sp)
                        val reason = currentMoveOutRequest.optString("reason")
                        if (reason.isNotBlank()) Text("Your reason: $reason", color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp)
                        val ownerNote = currentMoveOutRequest.optString("owner_note")
                        if (ownerNote.isNotBlank()) Text("Owner response: $ownerNote", color = SE.Text, fontSize = 11.sp, lineHeight = 15.sp)
                        Text(
                            when (currentRequestStatus) {
                                "pending" -> "Waiting for the owner to review your request."
                                "approved" -> "Approved. The stay remains occupied until the owner confirms the actual move-out."
                                "rejected" -> "The owner requested a follow-up. Review the note above and submit a new request when you have agreed the details."
                                "cancelled" -> "This request was cancelled."
                                else -> "Request status is synced from StayEasy."
                            },
                            color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(17.dp))
            Text("Need to make a change?", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                "Submit a move-out request with your preferred date. The owner can approve or respond with a note; the room stays occupied until actual move-out is completed.",
                color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp
            )
            Spacer(Modifier.height(9.dp))
            OutlinedButton(
                onClick = {
                    moveOutDateText = earliestMoveOutDate
                    moveOutReasonText = ""
                    moveOutError = ""
                    moveOutDialogVisible = true
                },
                enabled = canRequestMoveOut && !loading && !moveOutSubmitting,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    when {
                        currentRequestStatus == "pending" -> "Request pending"
                        currentRequestStatus == "rejected" -> "Submit another move-out request"
                        status.equals("Notice given", ignoreCase = true) || currentRequestStatus == "approved" -> "Move-out in progress"
                        else -> "Request move-out"
                    },
                    color = SE.Ink
                )
            }
            if (canRequestMoveOut) {
                Text("Earliest permitted date: $earliestMoveOutDate · based on the property's notice period", color = SE.Muted, fontSize = 9.sp, lineHeight = 13.sp)
            }
        }

        if (stays.size > 1) {
            Spacer(Modifier.height(20.dp))
            Text("Previous stays", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SE.Ink)
            stays.filter { currentStay == null || it.optLong("id") != currentStay.optLong("id") }.forEach { row ->
                Spacer(Modifier.height(8.dp))
                Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(row.optString("property_name", "Property"), color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "${row.optString("joining_date")} · ${row.optString("status").replace('_', ' ')} · ${money(row.optInt("agreed_monthly_rent"))}/month",
                            color = SE.Muted, fontSize = 10.sp
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
    }

    if (moveOutDialogVisible && currentStay != null) {
        AlertDialog(
            onDismissRequest = { if (!moveOutSubmitting) moveOutDialogVisible = false },
            title = { Text("Request move-out", color = SE.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "The owner will review your requested date. The property notice period applies, and submitting this request does not release the room or bed.",
                        color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = moveOutDateText,
                        onValueChange = { moveOutDateText = it.trim().take(10); moveOutError = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Requested move-out date (YYYY-MM-DD)") },
                        placeholder = { Text("2026-12-15") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = moveOutReasonText,
                        onValueChange = { moveOutReasonText = it.take(1200); moveOutError = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Reason (optional)") },
                        placeholder = { Text("e.g. relocating for work") },
                        minLines = 2,
                        maxLines = 4
                    )
                    if (moveOutError.isNotBlank()) Text(moveOutError, color = SE.Red, fontSize = 11.sp, lineHeight = 15.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (moveOutSubmitting) return@Button
                        val requestedDate = runCatching { java.time.LocalDate.parse(moveOutDateText.trim()) }.getOrNull()
                        val earliestDate = runCatching { java.time.LocalDate.parse(earliestMoveOutDate) }.getOrNull()
                        when {
                            requestedDate == null -> moveOutError = "Enter a real date in YYYY-MM-DD format."
                            earliestDate != null && requestedDate.isBefore(earliestDate) ->
                                moveOutError = "The property's notice period means the earliest allowed date is $earliestMoveOutDate."
                            else -> {
                                val token = StayApiClient.currentSessionToken(baseUrl)
                                if (token == null) {
                                    moveOutError = "Your tenant session expired. Sign in again and retry."
                                } else {
                                    moveOutSubmitting = true
                                    moveOutError = ""
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                StayApiClient.requestMoveOut(
                                                    baseUrl,
                                                    token,
                                                    currentStay.optLong("id"),
                                                    requestedDate.toString(),
                                                    moveOutReasonText
                                                )
                                            }
                                            moveOutDialogVisible = false
                                            moveOutMessage = "Move-out request sent to the owner. You can track their response here."
                                            refreshKey++
                                        } catch (e: Exception) {
                                            moveOutError = e.message ?: "The request was not saved. Check the date and try again."
                                        } finally {
                                            moveOutSubmitting = false
                                        }
                                    }
                                }
                            }
                        }
                    },
                    enabled = !moveOutSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)
                ) { Text(if (moveOutSubmitting) "Submitting…" else "Submit request") }
            },
            dismissButton = {
                TextButton(onClick = { if (!moveOutSubmitting) moveOutDialogVisible = false }, enabled = !moveOutSubmitting) {
                    Text("Cancel", color = SE.Muted)
                }
            },
            containerColor = SE.Surface
        )
    }
}

@Composable
private fun TenantPaymentsScreen(onBack: () -> Unit, onReceipt: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        ScreenHeader("Payments & receipts", "A clear record of every charge", onBack = onBack)
        Spacer(Modifier.height(14.dp))
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SE.BlueSoft), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) { Text("TOTAL PAID · DEMO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SE.Blue); Text("₹37,000", color = SE.Ink, fontSize = 27.sp, fontWeight = FontWeight.ExtraBold); Text("Rent and deposit shown separately", color = SE.Muted, fontSize = 11.sp) } }
        Spacer(Modifier.height(18.dp)); Text("RECENT TRANSACTIONS", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        listOf(PaymentRowData("October rent", "1 Oct 2026 · UPI", 12500, "Paid"), PaymentRowData("Security deposit", "1 Oct 2026 · Bank transfer", 22000, "Recorded"), PaymentRowData("September rent", "5 Sep 2026 · UPI", 12500, "Paid")).forEach { payment ->
            Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(SE.Mint), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Receipt, null, tint = SE.MintText) }
                Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(payment.title, color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text(payment.subtitle, color = SE.Muted, fontSize = 10.sp); Text(payment.status, color = SE.MintText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }; Column(horizontalAlignment = Alignment.End) { Text(money(payment.amount), color = SE.Ink, fontWeight = FontWeight.Bold); TextButton(onClick = onReceipt, contentPadding = PaddingValues(0.dp)) { Text("Receipt", color = SE.Blue, fontSize = 11.sp) } }
            }; HorizontalDivider(color = SE.Border)
        }
        Spacer(Modifier.height(13.dp)); Text("Your owner may record cash or UPI receipts manually. A receipt here is demo data, not proof of a real transaction.", color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}
private data class PaymentRowData(val title: String, val subtitle: String, val amount: Int, val status: String)

@Composable
private fun IssueScreen(onBack: () -> Unit, onSubmit: (String, String, String) -> Unit) {
    var category by remember { mutableStateOf("Plumbing / water") }
    var description by remember { mutableStateOf("") }
    var urgency by remember { mutableStateOf("Normal") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        ScreenHeader("Raise a maintenance issue", "Help the owner understand what needs fixing", onBack = onBack)
        Spacer(Modifier.height(20.dp)); Text("WHAT'S THE ISSUE?", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SE.Muted, letterSpacing = 1.sp)
        Spacer(Modifier.height(8.dp)); FlowChips(listOf("Plumbing / water", "Electricity", "Wi-Fi", "Cleaning", "Appliance", "Other"), remember { mutableStateListOf(category) }, singleSelection = true) { category = it }
        Spacer(Modifier.height(17.dp)); Text("Priority", fontWeight = FontWeight.SemiBold, color = SE.Ink); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Low", "Normal", "Urgent").forEach { FilterChip(selected = urgency == it, onClick = { urgency = it }, label = { Text(it) }) } }
        Spacer(Modifier.height(14.dp)); Text("Describe the problem", fontWeight = FontWeight.SemiBold, color = SE.Ink)
        OutlinedTextField(value = description, onValueChange = { description = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp), placeholder = { Text("What happened? Where should the manager look?") }, shape = RoundedCornerShape(15.dp), colors = fieldColors())
        Spacer(Modifier.height(12.dp)); Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth().clickable { }) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.CameraAlt, null, tint = SE.Blue); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Add photos", color = SE.Ink, fontWeight = FontWeight.SemiBold); Text("Attach evidence if it helps explain the problem", color = SE.Muted, fontSize = 10.sp) }; Icon(Icons.Filled.Add, null, tint = SE.Blue) } }
        Spacer(Modifier.height(22.dp)); PrimaryButton("Submit issue", Icons.AutoMirrored.Filled.ArrowForward) { if (description.isBlank()) { description = "Please add a description before submitting." } else onSubmit(category, description, urgency) }
        Spacer(Modifier.height(10.dp)); Text("A shared ticket requires a connected customer session and an active server-backed stay. Otherwise, this report remains on this device.", color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun AccountTypeScreen(onTenant: () -> Unit, onOwner: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(SE.Background).verticalScroll(rememberScrollState()).padding(18.dp)) {
        ScreenHeader("Choose your workspace", "How would you like to use StayEasy?", onBack = onBack)
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick the experience that fits you. You can switch workspaces later.",
            color = SE.Muted, fontSize = 12.sp, lineHeight = 18.sp
        )
        Spacer(Modifier.height(18.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SE.Surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(SE.BlueSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Search, null, tint = SE.Blue, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("I'm looking for a place", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Tenant workspace", color = SE.Blue, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Explore PGs, compare rent and deposits, contact owners, arrange visits and manage your stay.",
                    color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp
                )
                Spacer(Modifier.height(14.dp))
                PrimaryButton("Continue as Tenant", Icons.AutoMirrored.Filled.ArrowForward, onTenant)
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SE.Ink),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Color.White.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Apartment, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("I have a property to rent", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Owner workspace", color = Color.White.copy(alpha = .76f), fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "Manage listings, rooms and beds, resident stays, rent charges, payments and deposits.",
                    color = Color.White.copy(alpha = .78f), fontSize = 11.sp, lineHeight = 16.sp
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onOwner,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)
                ) {
                    Text("Continue as Owner", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SE.OrangeSoft),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Filled.Lock, null, tint = SE.Orange, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Preview mode: these choices switch the local experience only. They do not authenticate an account or grant server-side Owner permissions.",
                    color = SE.Text, fontSize = 10.sp, lineHeight = 15.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun ProfileScreen(
    account: JSONObject?,
    onOwner: () -> Unit,
    onNotifications: () -> Unit,
    onSettings: () -> Unit,
    onAccount: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        ScreenHeader("Your profile", "Your StayEasy account")
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(SE.Surface)
                .clickable(onClick = onAccount).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(58.dp).clip(CircleShape).background(SE.BlueSoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Person, null, tint = SE.Blue, modifier = Modifier.size(29.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(account?.optString("display_name", "Guest explorer") ?: "Guest explorer", fontWeight = FontWeight.Bold, color = SE.Ink, fontSize = 17.sp)
                Text(account?.optString("email")?.takeIf { it.isNotBlank() } ?: "Sign in or create your StayEasy account", color = SE.Muted, fontSize = 11.sp)
            }
            if (account != null) StatusPill("SIGNED IN", SE.BlueSoft, SE.Blue) else Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = SE.Blue)
        }
        Spacer(Modifier.height(16.dp))
        MenuRow(if (account == null) "Sign in / Create account" else "Account & security", if (account == null) "Verify your email and sync your account" else "View account, refresh session or sign out", Icons.Filled.Lock, onAccount)
        MenuRow("Notifications", "Manage alerts and updates", Icons.Filled.Notifications, onNotifications)
        MenuRow("All tools & preferences", "Saved searches, compare, bookings, themes and privacy", Icons.Filled.Tune, onSettings)
        MenuRow("Privacy & security", "Controls and account data", Icons.Filled.Lock, onSettings)
        MenuRow("Help & support", "Get help with a stay", Icons.Filled.ReportProblem, onSettings)
        MenuRow("Share StayEasy", "Invite someone to explore", Icons.Filled.Share, onSettings)
        Spacer(Modifier.height(18.dp))
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SE.Ink),
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOwner)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Apartment, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Are you a PG owner?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Manage properties, beds and rent", color = Color.White.copy(alpha = 0.72f), fontSize = 11.sp)
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("StayEasy UI prototype · v0.1", color = SE.Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun AccountAuthScreen(
    baseUrl: String,
    account: JSONObject?,
    onBack: () -> Unit,
    onOpenConnection: () -> Unit,
    onSignedIn: (JSONObject) -> Unit,
    onSignedOut: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf("signin") }
    var email by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var workspace by remember { mutableStateOf("tenant") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun showMessage(text: String, error: Boolean = false) {
        message = text
        isError = error
    }
    fun runRequest(action: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            message = ""
            isError = false
            try {
                action()
            } catch (e: Exception) {
                showMessage(e.message ?: "StayEasy could not complete that request. Check the API connection.", true)
            } finally {
                busy = false
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)) {
        ScreenHeader("StayEasy account", "Secure access to your stay records", onBack = onBack)
        Spacer(Modifier.height(14.dp))
        if (account != null) {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(CircleShape).background(SE.BlueSoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.VerifiedUser, null, tint = SE.Blue, modifier = Modifier.size(27.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(account.optString("display_name", "StayEasy member"), color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(account.optString("email"), color = SE.Muted, fontSize = 12.sp)
                        }
                        StatusPill("VERIFIED", SE.BlueSoft, SE.Blue)
                    }
                    Spacer(Modifier.height(18.dp))
                    Text("Account role", color = SE.Muted, fontSize = 12.sp)
                    Text(account.optString("role", "customer").replaceFirstChar { it.uppercase() }, color = SE.Ink, fontWeight = FontWeight.SemiBold)
                    if (account.optString("owner_access_status") == "pending_approval") {
                        Spacer(Modifier.height(12.dp))
                        Text("Owner workspace requested", color = SE.Ink, fontWeight = FontWeight.Bold)
                        Text("Your account stays in Tenant mode until the StayEasy team approves Owner access.", color = SE.Muted, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.height(18.dp))
                    OutlinedButton(onClick = {
                        runRequest {
                            val fresh = withContext(Dispatchers.IO) { StayApiClient.currentAccount(baseUrl) }
                            onSignedIn(fresh)
                            showMessage("Account details refreshed.")
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Refresh account") }
                    Button(onClick = {
                        runRequest {
                            withContext(Dispatchers.IO) { StayApiClient.logoutAccount(context, baseUrl) }
                            onSignedOut()
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = SE.Ink)) {
                        Text(if (busy) "Please wait…" else "Sign out", color = Color.White)
                    }
                }
            }
        } else if (baseUrl.isBlank()) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Icon(Icons.Filled.Lock, null, tint = SE.Blue, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(9.dp))
                    Text("Connect your StayEasy API first", color = SE.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Account creation and email verification need a configured backend URL and a server with SMTP email delivery enabled.", color = SE.Muted, fontSize = 13.sp, lineHeight = 19.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = onOpenConnection, modifier = Modifier.fillMaxWidth()) { Text("Configure API connection") }
                }
            }
        } else {
            if (mode == "signin" || mode == "signup") {
                Row(Modifier.fillMaxWidth().background(SE.BlueSoft, RoundedCornerShape(14.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FilterChip(selected = mode == "signin", onClick = { mode = "signin"; showMessage("") }, label = { Text("Sign in") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = mode == "signup", onClick = { mode = "signup"; showMessage("") }, label = { Text("Create account") }, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                when (mode) {
                    "signup" -> "Create your account"
                    "verify" -> "Verify your email"
                    "forgot" -> "Reset your password"
                    "reset" -> "Choose a new password"
                    else -> "Welcome back"
                },
                color = SE.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold
            )
            Text(
                when (mode) {
                    "signup" -> "One account for enquiries, bookings and stay records."
                    "verify" -> "Enter the six-digit code sent to $email."
                    "forgot" -> "We’ll send a short-lived recovery code if the account exists."
                    "reset" -> "Enter the code from your email and a new password."
                    else -> "Sign in to sync account activity across devices."
                },
                color = SE.Muted, fontSize = 12.sp, lineHeight = 18.sp
            )
            Spacer(Modifier.height(13.dp))
            if (mode == "signup") {
                OutlinedTextField(displayName, { displayName = it }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (mode != "verify" && mode != "reset") {
                OutlinedTextField(email, { email = it }, label = { Text("Email address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (mode == "signin" || mode == "signup") {
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            }
            if (mode == "signup") {
                Spacer(Modifier.height(8.dp))
                Text("Choose your starting workspace", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = workspace == "tenant", onClick = { workspace = "tenant" }, label = { Text("Tenant") })
                    FilterChip(selected = workspace == "owner", onClick = { workspace = "owner" }, label = { Text("Owner") })
                }
                if (workspace == "owner") {
                    Text("Owner access requires approval. Choosing this option does not grant owner permissions.", color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp)
                }
            }
            if (mode == "verify" || mode == "reset") {
                OutlinedTextField(verificationCode, { verificationCode = it.filter(Char::isDigit).take(6) }, label = { Text("Six-digit email code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (mode == "reset") {
                OutlinedTextField(newPassword, { newPassword = it }, label = { Text("New password (12+ characters)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(12.dp))
            when (mode) {
                "signup" -> Button(onClick = {
                    if (displayName.trim().length < 2 || !email.contains("@") || password.length < 12) {
                        showMessage("Enter your name, a valid email, and a password of at least 12 characters.", true)
                    } else runRequest {
                        withContext(Dispatchers.IO) { StayApiClient.registerAccount(baseUrl, email, displayName, password, workspace) }
                        mode = "verify"
                        verificationCode = ""
                        showMessage("Verification email requested. Enter the code from your inbox.")
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Creating account…" else "Create account") }
                "verify" -> {
                    Button(onClick = {
                        if (verificationCode.length != 6) showMessage("Enter the six-digit code from your email.", true)
                        else runRequest {
                            withContext(Dispatchers.IO) { StayApiClient.verifyAccountEmail(baseUrl, email, verificationCode) }
                            mode = "signin"
                            verificationCode = ""
                            showMessage("Email verified. Sign in to continue.")
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Verifying…" else "Verify email") }
                    TextButton(onClick = {
                        if (password.isBlank()) showMessage("Enter your password to request another verification code.", true)
                        else runRequest {
                            withContext(Dispatchers.IO) { StayApiClient.resendAccountVerification(baseUrl, email, password) }
                            showMessage("If the account still needs verification, a new code has been sent.")
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Resend verification code") }
                }
                "forgot" -> Button(onClick = {
                    if (!email.contains("@")) showMessage("Enter your account email address.", true)
                    else runRequest {
                        withContext(Dispatchers.IO) { StayApiClient.requestPasswordReset(baseUrl, email) }
                        mode = "reset"
                        verificationCode = ""
                        showMessage("If a verified account exists, a recovery code has been sent.")
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Requesting code…" else "Send recovery code") }
                "reset" -> Button(onClick = {
                    if (verificationCode.length != 6 || newPassword.length < 12) showMessage("Enter the six-digit code and a new password of at least 12 characters.", true)
                    else runRequest {
                        withContext(Dispatchers.IO) { StayApiClient.resetAccountPassword(baseUrl, email, verificationCode, newPassword) }
                        mode = "signin"
                        password = ""
                        newPassword = ""
                        verificationCode = ""
                        showMessage("Password changed. Sign in with your new password.")
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Updating password…" else "Update password") }
                else -> Button(onClick = {
                    if (!email.contains("@") || password.isBlank()) showMessage("Enter your email and password.", true)
                    else runRequest {
                        val response = withContext(Dispatchers.IO) { StayApiClient.loginAccount(context, baseUrl, email, password) }
                        onSignedIn(response.getJSONObject("user"))
                    }
                }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "Signing in…" else "Sign in") }
            }
            if (mode == "signin") {
                TextButton(onClick = { mode = "forgot"; verificationCode = ""; showMessage("") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Forgot password?") }
            }
            if (mode == "verify" || mode == "reset") {
                TextButton(onClick = { mode = "signin"; showMessage("") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Back to sign in") }
            }
            if (message.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(message, color = if (isError) SE.Red else SE.Blue, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Account verification requires an active email delivery provider. StayEasy never treats selecting Owner as proof of ownership.", color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun NotificationsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        ScreenHeader("Updates", "Useful things, without the noise", onBack = onBack)
        Spacer(Modifier.height(12.dp))
        listOf(Triple("Visit reminder", "Your sample visit at Maple House is tomorrow at 11:00 AM.", "TODAY"), Triple("New reply", "The Courtyard Co-Living replied to your enquiry.", "2 HOURS AGO"), Triple("Payment reminder", "A sample rent reminder is scheduled before your due date.", "YESTERDAY")).forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.Top) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(if (index == 0) SE.OrangeSoft else SE.BlueSoft), contentAlignment = Alignment.Center) { Icon(if(index == 0) Icons.Filled.CalendarToday else if(index == 1) Icons.Filled.ChatBubbleOutline else Icons.Filled.Payments, null, tint = if(index == 0) SE.Orange else SE.Blue) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(row.first, fontWeight = FontWeight.Bold, color = SE.Ink); Text(row.second, color = SE.Muted, fontSize = 12.sp, lineHeight = 17.sp); Spacer(Modifier.height(3.dp)); Text(row.third, color = SE.Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold) } }; HorizontalDivider(color = SE.Border)
        }
    }
}

@Composable
private fun OwnerDashboardScreen(
    listings: List<PgListing>, onProperties: () -> Unit, onBeds: () -> Unit, onEnquiries: () -> Unit,
    onPayments: () -> Unit, onIssues: () -> Unit, onTenantView: () -> Unit, onAnalytics: () -> Unit, onResidents: () -> Unit
) {
    val totalBeds = listings.sumOf { it.totalBeds }
    val available = listings.sumOf { it.availableBeds }
    val occupied = max(0, totalBeds - available - 3)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 17.dp)) {
        Spacer(Modifier.height(14.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Good morning 👋", color = SE.Muted, fontSize = 12.sp); Text("Your business, at a glance", color = SE.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold) }; RoundIconButton(Icons.Filled.Notifications, {}, tint = SE.Blue); Spacer(Modifier.width(8.dp)); RoundIconButton(Icons.Filled.Person, onTenantView, tint = SE.Ink) }
        Spacer(Modifier.height(14.dp)); Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(SE.Surface).clickable(onClick = onProperties).padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Apartment, null, tint = SE.Blue); Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Text("Your properties", color = SE.Muted, fontSize = 10.sp); Text("${listings.size} properties · Pune", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp) }; Icon(Icons.Filled.ChevronRight, null, tint = SE.Muted) }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MetricCard("Total beds", "$totalBeds", "Across properties", SE.BlueSoft, SE.Blue, Modifier.weight(1f))
            MetricCard("Occupied", "$occupied", "Active stays", SE.Mint, SE.MintText, Modifier.weight(1f))
        }
        Spacer(Modifier.height(9.dp)); Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            MetricCard("Available", "$available", "Ready to assign", SE.OrangeSoft, SE.Orange, Modifier.weight(1f))
            MetricCard("Reserved", "3", "Expected arrivals", SE.Lavender, SE.Ink, Modifier.weight(1f))
        }
        Spacer(Modifier.height(19.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text("Today's priorities", color = SE.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("Live queue demo", color = SE.Muted, fontSize = 10.sp) }
        Spacer(Modifier.height(9.dp))
        TaskCard("Reply to new enquiries", "4 people are waiting for a response", Icons.Filled.ChatBubbleOutline, "4", SE.BlueSoft, SE.Blue, onEnquiries)
        TaskCard("Review rent due", "3 sample balances need attention", Icons.Filled.Payments, "3", SE.OrangeSoft, SE.Orange, onPayments)
        TaskCard("Resolve maintenance", "1 issue is marked urgent", Icons.Filled.ReportProblem, "1", SE.RedSoft, SE.Red, onIssues)
        TaskCard("Prepare arrivals", "2 move-ins are scheduled", Icons.Filled.CalendarToday, "2", SE.Lavender, SE.Ink, onResidents)
        Spacer(Modifier.height(18.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text("Occupancy pulse", color = SE.Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); TextButton(onClick = onAnalytics) { Text("Insights", color = SE.Blue) } }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(15.dp)) { Row(verticalAlignment = Alignment.Bottom) { Text("${if(totalBeds > 0) (occupied * 100 / totalBeds) else 0}%", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = SE.Ink); Spacer(Modifier.width(8.dp)); Text("estimated occupancy", color = SE.Muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 5.dp)); Spacer(Modifier.weight(1f)); Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = SE.MintText) }; Spacer(Modifier.height(10.dp)); Box(Modifier.fillMaxWidth().height(9.dp).clip(CircleShape).background(SE.Background)) { Box(Modifier.fillMaxWidth((if(totalBeds > 0) occupied.toFloat() / totalBeds else 0.65f).coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(SE.Blue)) }; Spacer(Modifier.height(7.dp)); Text("Keep availability current to avoid disappointing customers.", color = SE.Muted, fontSize = 11.sp) } }
        Spacer(Modifier.height(18.dp)); Text("Quick actions", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 17.sp); Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { QuickAction("Manage beds", Icons.Filled.Bed, SE.BlueSoft, SE.Blue, onBeds, Modifier.weight(1f)); QuickAction("Add property", Icons.Filled.Add, SE.OrangeSoft, SE.Orange, onProperties, Modifier.weight(1f)); QuickAction("Residents", Icons.Filled.Person, SE.Lavender, SE.Ink, onResidents, Modifier.weight(1f)) }
        Spacer(Modifier.height(16.dp)); Text("Numbers shown here are generated demo data.", color = SE.Muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally)); Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OwnerPropertiesScreen(listings: List<PgListing>, serverSynced: Boolean, onAdd: () -> Unit, onListing: (Long) -> Unit, onEdit: (Long) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 17.dp)) {
        Spacer(Modifier.height(14.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Property portfolio", color = SE.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Manage listings and occupancy", color = SE.Muted, fontSize = 12.sp) }; Button(onClick = onAdd, shape = RoundedCornerShape(13.dp), colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(5.dp)); Text("Add PG") } }
        Spacer(Modifier.height(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            StatusPill("${listings.size} properties", SE.BlueSoft, SE.Blue)
            StatusPill(if (serverSynced) "Server synced" else "Local / sample", if (serverSynced) SE.Mint else SE.OrangeSoft, if (serverSynced) SE.MintText else SE.Orange)
        }
        Spacer(Modifier.height(12.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(11.dp), contentPadding = PaddingValues(bottom = 16.dp)) { items(listings, key = { it.id }) { p ->
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth().clickable { onListing(p.id) }) {
                Column {
                    PropertyCover(p.photoUri, (p.id % 4).toInt(), Modifier.fillMaxWidth().height(145.dp))
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Text(p.name, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f)); StatusPill(if (p.serverPropertyId != null) "SERVER" else if (p.isDemo) "SAMPLE" else "LOCAL DRAFT", if (p.serverPropertyId != null) SE.BlueSoft else if (p.isDemo) SE.OrangeSoft else SE.Mint, if (p.serverPropertyId != null) SE.Blue else if (p.isDemo) SE.Orange else SE.MintText); if (!p.isDemo && p.serverPropertyId == null) { Spacer(Modifier.width(4.dp)); Icon(Icons.Filled.Settings, "Edit listing", tint = SE.Blue, modifier = Modifier.size(21.dp).clickable { onEdit(p.id) }) } }
                        Text(p.area, color = SE.Muted, fontSize = 11.sp); Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { SmallMetric("${p.totalRooms}", "Rooms"); SmallMetric("${p.totalBeds}", "Beds"); SmallMetric("${p.availableBeds}", "Available") }
                        Spacer(Modifier.height(11.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text("From ${money(p.monthlyRent)} / month", color = SE.Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("Manage", color = SE.Blue, fontWeight = FontWeight.Bold); Icon(Icons.Filled.ChevronRight, null, tint = SE.Blue) }
                    }
                }
            }
        } } 
    }
}

@Composable
private fun AddPropertyWizard(onPublish: (PgListing) -> Unit, onCancel: () -> Unit, initial: PgListing? = null) {
    var step by remember(initial?.id) { mutableIntStateOf(0) }
    var name by remember(initial?.id) { mutableStateOf(initial?.name ?: "") }
    var area by remember(initial?.id) { mutableStateOf(initial?.area ?: "Hinjawadi, Pune") }
    var description by remember(initial?.id) { mutableStateOf(initial?.description ?: "") }
    var policy by remember(initial?.id) { mutableStateOf(initial?.allowedFor ?: "Mixed") }
    var couples by remember(initial?.id) { mutableStateOf(initial?.couplesAllowed ?: false) }
    var sharing by remember(initial?.id) { mutableStateOf(initial?.sharing ?: "2 sharing") }
    var rentText by remember(initial?.id) { mutableStateOf((initial?.monthlyRent ?: 9500).toString()) }
    var depositText by remember(initial?.id) { mutableStateOf(initial?.deposit?.takeIf { it > 0 }?.toString() ?: "") }
    var foodText by remember(initial?.id) { mutableStateOf((initial?.foodMonthly ?: 0).toString()) }
    var roomsText by remember(initial?.id) { mutableStateOf((initial?.totalRooms ?: 6).toString()) }
    var bedsText by remember(initial?.id) { mutableStateOf((initial?.totalBeds ?: 18).toString()) }
    var photoUri by remember(initial?.id) { mutableStateOf(initial?.photoUri ?: "") }
    var validationMessage by remember(initial?.id) { mutableStateOf("") }
    var showAreaPicker by remember(initial?.id) { mutableStateOf(false) }
    var pinCode by remember(initial?.id) { mutableStateOf(initial?.pinCode ?: "") }
    var nearbyPlaces by remember(initial?.id) { mutableStateOf(initial?.nearbyPlaces ?: "") }
    var furnished by remember(initial?.id) { mutableStateOf(initial?.isFurnished ?: true) }
    var bathroomType by remember(initial?.id) { mutableStateOf(initial?.bathroomType ?: "Shared") }
    var cookingAllowed by remember(initial?.id) { mutableStateOf(initial?.cookingAllowed ?: false) }
    var curfewPolicy by remember(initial?.id) { mutableStateOf(initial?.curfewPolicy ?: "Ask owner") }
    var visitorPolicy by remember(initial?.id) { mutableStateOf(initial?.visitorPolicy ?: "Ask owner") }
    var minimumStayText by remember(initial?.id) { mutableStateOf((initial?.minimumStayMonths ?: 1).toString()) }
    var noticeDaysText by remember(initial?.id) { mutableStateOf((initial?.noticePeriodDays ?: 30).toString()) }
    var utilityText by remember(initial?.id) { mutableStateOf((initial?.utilityMonthlyEstimate ?: 0).toString()) }
    var expectedVacancy by remember(initial?.id) { mutableStateOf(initial?.expectedVacancy ?: "Available now") }
    var foodPolicy by remember(initial?.id) { mutableStateOf(initial?.foodPolicy ?: if ((initial?.foodMonthly ?: 0) > 0) "Optional" else "Not offered") }
    val amenities = remember(initial?.id) { mutableStateListOf<String>().apply { addAll(initial?.amenities ?: emptyList()) } }
    val context = androidx.compose.ui.platform.LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            photoUri = uri.toString()
        }
    }
    val steps = listOf("Property", "Stay rules", "Rooms & rent", "Review")
    BackHandler(enabled = step > 0) { step = (step - 1).coerceAtLeast(0) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(17.dp)) {
        ScreenHeader(if (initial == null) "Add a property" else "Edit property", "Step ${step + 1} of ${steps.size} · optional details can be added later", onBack = if (step > 0) ({ step -= 1 }) else onCancel, trailing = { TextButton(onClick = {
            StayRecordStore.add(context, StayRecord(category = "quality", title = name.ifBlank { "Untitled PG draft" },
                details = "Draft listing · Area: $area · Policy: $policy · Sharing: $sharing · Rent: ₹${rentText.ifBlank { "0" }} · Deposit: ${depositText.takeIf { it.isNotBlank() }?.let { "₹$it" } ?: "not specified"} · Amenities: ${amenities.joinToString()}",
                status = "Draft", property = area, amount = rentText.toDoubleOrNull()?.toInt() ?: 0))
            onCancel()
        }) { Text("Save draft later", fontSize = 10.sp, color = SE.Blue) } })
        Spacer(Modifier.height(13.dp)); Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth()) { steps.forEachIndexed { i, label -> Column(Modifier.weight(1f)) { Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(if (i <= step) SE.Blue else SE.Border)); Spacer(Modifier.height(6.dp)); Text(label, fontSize = 9.sp, color = if (i == step) SE.Blue else SE.Muted, fontWeight = if (i == step) FontWeight.Bold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis) } } }
        Spacer(Modifier.height(18.dp))
        when (step) {
            0 -> {
                Text("Tell residents about your property", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("A few clear details help the right residents find you.", color = SE.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth().clickable { photoPicker.launch(arrayOf("image/*")) }) {
                    Box(Modifier.fillMaxWidth().height(155.dp)) {
                        if (photoUri.isNotBlank()) AsyncImage(model = photoUri, contentDescription = "Selected property photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) else PropertyArt(0, Modifier.fillMaxSize())
                        Box(Modifier.align(Alignment.BottomEnd).padding(11.dp).clip(RoundedCornerShape(12.dp)).background(SE.Ink).padding(horizontal = 12.dp, vertical = 9.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.CameraAlt, null, tint = Color.White, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp)); Text(if (photoUri.isBlank()) "Add cover photo" else "Change cover", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) } }
                    }
                }
                Spacer(Modifier.height(13.dp)); FormLabel("PROPERTY NAME · REQUIRED"); OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("e.g. The Courtyard Co-Living") }, shape = RoundedCornerShape(14.dp), colors = fieldColors())
                Spacer(Modifier.height(11.dp)); FormLabel("LOCALITY / ADDRESS · REQUIRED"); OutlinedTextField(area, { area = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Filled.LocationOn, null) }, placeholder = { Text("Locality, city") }, shape = RoundedCornerShape(14.dp), colors = fieldColors())
                Spacer(Modifier.height(8.dp)); Text("Choose the nearest locality now; precise map pin selection will be connected with Google Maps later.", color = SE.Muted, fontSize = 10.sp)
                Spacer(Modifier.height(5.dp)); OutlinedButton(onClick = { showAreaPicker = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Filled.Map, null); Spacer(Modifier.width(7.dp)); Text("Choose locality / map area") }
                Spacer(Modifier.height(10.dp)); FormLabel("PIN CODE (OPTIONAL)"); OutlinedTextField(pinCode, { pinCode = it.filter(Char::isDigit).take(6) }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("e.g. 411057") }, shape = RoundedCornerShape(14.dp), colors = fieldColors())
                Spacer(Modifier.height(10.dp)); FormLabel("NEARBY OFFICES / COLLEGES / LANDMARKS (OWNER-ENTERED)"); OutlinedTextField(nearbyPlaces, { nearbyPlaces = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), placeholder = { Text("Name useful nearby places; distances will not be inferred") }, shape = RoundedCornerShape(14.dp), colors = fieldColors())
                Text("These are owner-entered notes until a configured Places/Maps service verifies place data and calculates distance.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                Spacer(Modifier.height(11.dp)); FormLabel("A short description (optional)"); OutlinedTextField(description, { description = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 85.dp), placeholder = { Text("Describe only confirmed property features and rules") }, shape = RoundedCornerShape(14.dp), colors = fieldColors())
            }
            1 -> {
                Text("Who can stay here?", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("Gender policy and couple eligibility are separate settings.", color = SE.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(18.dp)); FormLabel("GENDER / OCCUPANCY POLICY")
                listOf("Boys only", "Girls only", "Mixed").forEach { option ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(15.dp)).background(if (policy == option) SE.BlueSoft else Color.White).border(1.dp, if (policy == option) SE.Blue else SE.Border, RoundedCornerShape(15.dp)).clickable { policy = option }.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(option, color = SE.Ink, fontWeight = FontWeight.Bold); Text(when(option) { "Boys only" -> "For male residents only"; "Girls only" -> "For female residents only"; else -> "Accept residents of any gender, subject to room allocation rules" }, color = SE.Muted, fontSize = 11.sp) }
                        if (policy == option) Icon(Icons.Filled.CheckCircle, null, tint = SE.Blue)
                    }
                }
                HorizontalDivider(color = SE.Border); Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Couples welcome", color = SE.Ink, fontWeight = FontWeight.Bold); Text("Allow couples in the accommodation types you specify.", color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp) }; Switch(checked = couples, onCheckedChange = { enabled -> couples = enabled; if (enabled) policy = "Mixed" }) }
                Spacer(Modifier.height(12.dp)); Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = SE.OrangeSoft)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Filled.VerifiedUser, null, tint = SE.Orange); Spacer(Modifier.width(9.dp)); Text("Mixed accommodation and couple-friendly rooms are explicit listing attributes. Publishing them does not replace the owner's responsibility to follow applicable property rules and laws.", color = SE.Text, fontSize = 11.sp, lineHeight = 16.sp) } }
                Spacer(Modifier.height(15.dp)); Text("COOKING, VISITORS & CURFEW", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp)
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Cooking permitted", color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text("Only if the property rules allow it", color = SE.Muted, fontSize = 10.sp) }; Switch(checked = cookingAllowed, onCheckedChange = { cookingAllowed = it }) }
                FormLabel("VISITOR POLICY"); FlowChips(listOf("Ask owner", "Visitors allowed", "Daytime only", "No visitors"), remember(visitorPolicy) { mutableStateListOf(visitorPolicy) }, singleSelection = true) { visitorPolicy = it }
                FormLabel("CURFEW POLICY"); FlowChips(listOf("Ask owner", "No curfew", "10 PM", "11 PM", "Midnight"), remember(curfewPolicy) { mutableStateListOf(curfewPolicy) }, singleSelection = true) { curfewPolicy = it }
            }
            2 -> {
                Text("Set clear terms", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("Transparent details reduce unnecessary calls and disputes.", color = SE.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(17.dp)); FormLabel("ROOM TYPE")
                FlowChips(listOf("1 sharing", "2 sharing", "3 sharing", "4+ sharing", "Private room"), remember { mutableStateListOf(sharing) }, singleSelection = true) { sharing = it }
                Spacer(Modifier.height(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Column(Modifier.weight(1f)) { FormLabel("MONTHLY RENT PER PERSON · REQUIRED (₹100–₹1,00,000)"); OutlinedTextField(rentText, { rentText = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Text("₹") }, shape = RoundedCornerShape(13.dp), colors = fieldColors()) }; Column(Modifier.weight(1f)) { FormLabel("SECURITY DEPOSIT (OPTIONAL)"); OutlinedTextField(depositText, { depositText = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Text("₹") }, shape = RoundedCornerShape(13.dp), colors = fieldColors()) } }
                Spacer(Modifier.height(12.dp)); FormLabel("FOOD CHARGE PER MONTH (OPTIONAL)"); OutlinedTextField(foodText, { foodText = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Text("₹") }, shape = RoundedCornerShape(13.dp), colors = fieldColors())
                Spacer(Modifier.height(10.dp)); FormLabel("FOOD CHARGE POLICY"); FlowChips(listOf("Included", "Optional", "Extra charge", "Not offered"), remember(foodPolicy) { mutableStateListOf(foodPolicy) }, singleSelection = true) { foodPolicy = it }
                Spacer(Modifier.height(10.dp)); FormLabel("ESTIMATED MONTHLY UTILITIES / ELECTRICITY (₹, OPTIONAL)"); OutlinedTextField(utilityText, { utilityText = it.filter(Char::isDigit).take(8) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(13.dp), colors = fieldColors())
                Spacer(Modifier.height(10.dp)); Text("FURNISHING", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Row(verticalAlignment = Alignment.CenterVertically) { Text(if (furnished) "Furnished" else "Unfurnished", color = SE.Ink, fontSize = 12.sp, modifier = Modifier.weight(1f)); Switch(checked = furnished, onCheckedChange = { furnished = it }) }
                FormLabel("BATHROOM TYPE"); FlowChips(listOf("Attached", "Shared", "Common floor bathroom"), remember(bathroomType) { mutableStateListOf(bathroomType) }, singleSelection = true) { bathroomType = it }
                Spacer(Modifier.height(10.dp)); FormLabel("EXPECTED VACANCY"); FlowChips(listOf("Available now", "Next month", "Date to confirm"), remember(expectedVacancy) { mutableStateListOf(expectedVacancy) }, singleSelection = true) { expectedVacancy = it }
                if (expectedVacancy == "Date to confirm") OutlinedTextField(expectedVacancy, { expectedVacancy = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Expected date") }, singleLine = true)
                Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Column(Modifier.weight(1f)) { FormLabel("MINIMUM STAY (MONTHS)"); OutlinedTextField(minimumStayText, { minimumStayText = it.filter(Char::isDigit).take(2) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(13.dp), colors = fieldColors()) }; Column(Modifier.weight(1f)) { FormLabel("NOTICE PERIOD (DAYS)"); OutlinedTextField(noticeDaysText, { noticeDaysText = it.filter(Char::isDigit).take(3) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(13.dp), colors = fieldColors()) } }
                Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Column(Modifier.weight(1f)) { FormLabel("ROOMS · REQUIRED"); OutlinedTextField(roomsText, { roomsText = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Filled.Apartment, null) }, shape = RoundedCornerShape(13.dp), colors = fieldColors()) }; Column(Modifier.weight(1f)) { FormLabel("TOTAL BEDS · REQUIRED"); OutlinedTextField(bedsText, { bedsText = it.filter(Char::isDigit) }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Filled.Bed, null) }, shape = RoundedCornerShape(13.dp), colors = fieldColors()) } }
                Text("Total beds means every bed across all rooms. Example: 6 rooms × 3 beds = 18 beds.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
                 Spacer(Modifier.height(15.dp)); FormLabel("AMENITIES (OPTIONAL)")
                FlowChips(listOf("Wi-Fi", "AC", "Meals", "Attached bath", "Laundry", "Parking", "Power backup", "Housekeeping", "Study desk", "Lift", "Hot water", "Washing machine", "Refrigerator", "CCTV / security", "Water purifier", "Geyser", "Balcony", "Kitchen access", "Gym", "Work desk"), amenities)
                Spacer(Modifier.height(9.dp)); Text("Select the facilities you actually offer. With none selected, you can add amenities later.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp)
            }
            else -> {
                Text("Ready to publish?", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text("Review how your listing will appear in discovery.", color = SE.Muted, fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                val rent = rentText.toIntOrNull() ?: 0
                Card(shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        PropertyCover(photoUri, 1, Modifier.fillMaxWidth().height(165.dp))
                        Column(Modifier.padding(14.dp)) {
                            Text(name.ifBlank { "Your PG name" }, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                            Text(area.ifBlank { "Area, Pune" }, color = SE.Muted, fontSize = 12.sp); Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { StatusPill(policy, SE.BlueSoft, SE.Blue); if (couples) StatusPill("Couples welcome", SE.OrangeSoft, SE.Orange) }
                            Spacer(Modifier.height(11.dp)); Text("${money(rent)} / month", color = SE.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
                            Text("Deposit ${depositText.toIntOrNull()?.takeIf { it > 0 }?.let { money(it) } ?: "not specified"} · Food ${money(foodText.toIntOrNull() ?: 0)} / month", color = SE.Muted, fontSize = 11.sp)
                            Spacer(Modifier.height(10.dp)); Text("$sharing · ${bedsText.toIntOrNull() ?: 0} beds · ${(amenities.toList().take(4)).joinToString(" · ")}", color = SE.Muted, fontSize = 11.sp)
                            if (description.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(description, color = SE.Text, fontSize = 12.sp) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp)); Text("Publishing adds this property to this demo app's local discovery results. It does not publish to a public marketplace.", color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        if (showAreaPicker) {
            AlertDialog(
                onDismissRequest = { showAreaPicker = false },
                title = { Text("Choose a Pune locality") },
                text = { Column { listOf("Hinjawadi Phase 1, Pune", "Baner, Pune", "Wakad, Pune", "Kothrud, Pune", "Viman Nagar, Pune", "Kharadi, Pune", "Aundh, Pune").forEach { option -> Text(option, color = SE.Text, modifier = Modifier.fillMaxWidth().clickable { area = option; showAreaPicker = false }.padding(vertical = 10.dp)) } } },
                confirmButton = { TextButton(onClick = { showAreaPicker = false }) { Text("Done", color = SE.Blue) } }
            )
        }
        Spacer(Modifier.height(20.dp))
        if (step < 3) PrimaryButton(if (step == 2) "Review listing" else "Continue", Icons.AutoMirrored.Filled.ArrowForward) {
            validationMessage = when { step == 0 && name.isBlank() -> "Enter a property name before continuing."; step == 0 && area.isBlank() -> "Enter the property locality."; step == 2 && ((rentText.toIntOrNull() ?: 0) !in 100..100000) -> "Enter a monthly rent between ₹100 and ₹1,00,000."; step == 2 && (bedsText.toIntOrNull() ?: 0) <= 0 -> "Enter at least one bed."; else -> "" }
            if (validationMessage.isBlank()) step += 1
        } else PrimaryButton(if (initial == null) "Publish demo listing" else "Save listing changes", Icons.Filled.CheckCircle) {
            val id = initial?.id ?: System.currentTimeMillis()
            val beds = (bedsText.toIntOrNull() ?: 1).coerceAtLeast(1)
            val rent = (rentText.toIntOrNull() ?: 0).coerceAtLeast(0)
            val deposit = (depositText.toIntOrNull() ?: 0).coerceAtLeast(0)
            val food = (foodText.toIntOrNull() ?: 0).coerceAtLeast(0)
            val utility = (utilityText.toIntOrNull() ?: 0).coerceAtLeast(0)
            val facts = buildList {
                add("$sharing accommodation in ${area.ifBlank { "Pune" }}")
                add("Policy: $policy${if (couples) "; couples welcome" else ""}")
                add("Furnishing: ${if (furnished) "furnished" else "unfurnished"}")
                add("Bathroom: $bathroomType")
                if (amenities.isNotEmpty()) add("Facilities listed: ${amenities.joinToString(", ")}")
                add(if (deposit > 0) "Monthly rent: ₹$rent; security deposit: ₹$deposit" else "Monthly rent: ₹$rent; security deposit not specified")
                if (foodPolicy != "Not offered" || food > 0) add("Food policy: $foodPolicy; listed food charge: ₹$food/month")
                if (utility > 0) add("Indicative utilities: ₹$utility/month; confirm final billing")
                add("Cooking: ${if (cookingAllowed) "permitted" else "not listed as permitted"}; visitors: $visitorPolicy; curfew: $curfewPolicy")
                add("Minimum stay: ${(minimumStayText.toIntOrNull() ?: 1).coerceAtLeast(1)} months; notice: ${(noticeDaysText.toIntOrNull() ?: 30).coerceAtLeast(0)} days")
            }.joinToString(". ")
            onPublish(PgListing(
                id, name.ifBlank { "My new PG" }, area.ifBlank { "Pune" }, rent, deposit, sharing, policy, couples, amenities.toList(),
                0.0, 0.0, max(0, beds / 4), beds, description.ifBlank { facts }, food, photoUri, false,
                (roomsText.toIntOrNull() ?: 1).coerceAtLeast(1),
                isVerified = false, isPublished = true, isFurnished = furnished, bathroomType = bathroomType, cookingAllowed = cookingAllowed,
                curfewPolicy = curfewPolicy, visitorPolicy = visitorPolicy, minimumStayMonths = (minimumStayText.toIntOrNull() ?: 1).coerceAtLeast(1),
                noticePeriodDays = (noticeDaysText.toIntOrNull() ?: 30).coerceAtLeast(0), utilityMonthlyEstimate = utility, foodPolicy = foodPolicy,
                pinCode = pinCode, latitude = 0.0, longitude = 0.0, hasCoordinates = false, featuredListing = false,
                availabilityConfirmedAt = System.currentTimeMillis(), expectedVacancy = expectedVacancy, nearbyPlaces = nearbyPlaces
            ))
        }
        if (validationMessage.isNotBlank()) Text(validationMessage, color = SE.Orange, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun OwnerBedsScreen(
    listings: List<PgListing>,
    initialPropertyId: Long,
    onAddResident: (PgListing, List<String>) -> Unit,
    onServerUnitStatusChange: (PgListing, RemoteUnit, String) -> Unit,
    notify: (String) -> Unit,
    onProperties: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var propertyIndex by remember(initialPropertyId) { mutableIntStateOf(listings.indexOfFirst { it.id == initialPropertyId }.coerceAtLeast(0)) }
    var roomIndex by remember { mutableIntStateOf(0) }
    val listing = listings.getOrNull(propertyIndex) ?: demoListings.first()
    val serverUnits = listing.serverUnits
    val isServerListing = listing.serverPropertyId != null

    fun displayRoom(code: String): String {
        val number = code.removePrefix("ROOM-").removePrefix("R").toIntOrNull() ?: code.toIntOrNull()
        return number?.let { "Room $it" } ?: code
    }

    fun displayBed(unit: RemoteUnit, index: Int): String {
        if (unit.rentableType.equals("room", true)) return "Private room"
        val suffix = unit.unitCode.substringAfterLast("-", unit.unitCode).removePrefix("B")
        return "Bed ${suffix.ifBlank { (index + 1).toString() }}"
    }

    val roomLabels = if (isServerListing) {
        serverUnits.map { displayRoom(it.roomCode) }.distinct()
    } else {
        (1..listing.totalRooms.coerceIn(1, 30)).map { "Room " + (100 + it) }
    }
    val roomCount = roomLabels.size.coerceAtLeast(1)
    val declaredBedCount = if (isServerListing) serverUnits.size else listing.totalBeds.coerceAtLeast(0)
    val currentRoomIndex = roomIndex.coerceIn(0, roomLabels.lastIndex.coerceAtLeast(0))
    val roomLabel = roomLabels.getOrElse(currentRoomIndex) { "Room 101" }
    val currentServerRoomCode = serverUnits.firstOrNull { displayRoom(it.roomCode) == roomLabel }?.roomCode
    val bedsPerRoom = if (isServerListing) {
        serverUnits.count { it.roomCode == currentServerRoomCode }
    } else {
        (declaredBedCount / roomCount + if (currentRoomIndex < declaredBedCount % roomCount) 1 else 0).coerceIn(0, 6)
    }
    val bedLetters = listOf("A", "B", "C", "D", "E", "F")
    val bedRecords = remember(listing.id, roomLabel, listing.serverUnits) {
        mutableStateListOf<StayRecord>().apply {
            if (isServerListing) {
                serverUnits.filter { displayRoom(it.roomCode) == roomLabel }.forEachIndexed { index, unit ->
                    val status = unit.status.lowercase(Locale.ROOT).replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                    }
                    val bed = displayBed(unit, index)
                    val detail = when (unit.status.lowercase(Locale.ROOT)) {
                        "available" -> "Available on the StayEasy server; link a resident account before assignment."
                        "reserved" -> "Reserved in shared inventory."
                        "occupied" -> "Occupied in shared inventory; complete stay actions before releasing it."
                        "maintenance" -> "Blocked for maintenance in shared inventory."
                        "inactive" -> "Inactive on the StayEasy server."
                        else -> "Inventory status returned by the server."
                    }
                    add(StayRecord(
                        id = "server-unit-${unit.id}", category = "beds", title = bed, details = detail,
                        property = listing.name, unit = "$roomLabel · $bed", amount = unit.monthlyRent,
                        status = status, source = "Server"
                    ))
                }
            } else {
                val stored = StayRecordStore.load(context, "beds")
                val current = stored.filter { it.property == listing.name && it.unit.startsWith("$roomLabel ·", true) }
                    .associateBy { it.unit.substringAfter("·").trim() }
                bedLetters.take(bedsPerRoom).forEachIndexed { index, letter ->
                    val unit = "$roomLabel · Bed $letter"
                    val record = current["Bed $letter"] ?: StayRecord(
                        category = "beds", title = "Bed $letter",
                        details = if (listing.isDemo) "Sample inventory · no resident assignment verified" else "New bed unit",
                        property = listing.name, unit = unit,
                        status = listOf("Occupied", "Available", "Reserved", "Occupied", "Maintenance", "Available")[index % 6]
                    )
                    add(record)
                    if (current["Bed $letter"] == null) StayRecordStore.addIfAbsent(context, record)
                }
            }
        }
    }
    var activeBed by remember { mutableStateOf<StayRecord?>(null) }
    var showBedActions by remember { mutableStateOf(false) }

    fun changeStatus(record: StayRecord, status: String) {
        val serverUnit = if (record.source == "Server") {
            serverUnits.firstOrNull { record.id == "server-unit-${it.id}" }
        } else null
        if (serverUnit != null) {
            onServerUnitStatusChange(listing, serverUnit, status)
            return
        }
        val updated = record.copy(status = status, updatedAt = System.currentTimeMillis(), details = when (status) {
            "Available" -> "Ready for assignment; confirm cleaning and inspection."
            "Reserved" -> "Reserved locally; owner must confirm reservation terms."
            "Occupied" -> "Resident assigned; link the active stay record."
            "Maintenance" -> "Unavailable until repairs are completed."
            "Inactive" -> "Unit temporarily withdrawn from inventory."
            else -> record.details
        })
        StayRecordStore.update(context, "beds", updated)
        val idx = bedRecords.indexOfFirst { it.id == record.id }
        if (idx >= 0) bedRecords[idx] = updated
        StayRecordStore.notify(context, "Bed status changed", "${record.unit}: $status", listing.name)
        notify("${record.unit} set to $status in local inventory")
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(13.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Rooms & beds", color = SE.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("1. Choose a property  2. Choose a room  3. Update inventory or link a resident.", color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp)
            }
            RoundIconButton(Icons.Filled.Tune, onProperties, tint = SE.Blue)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listings.forEachIndexed { index, p ->
                FilterChip(selected = propertyIndex == index, onClick = { propertyIndex = index; roomIndex = 0 }, label = { Text(p.name, maxLines = 1) })
            }
        }
        Spacer(Modifier.height(13.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(listing.name, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(listing.area, color = SE.Muted, fontSize = 10.sp)
                    }
                    Text("${listing.totalRooms} rooms · ${declaredBedCount} units", color = SE.Blue, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                Text("Selected room · ${bedRecords.size} units", color = SE.Muted, fontSize = 10.sp)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusPill("${bedRecords.count { it.status == "Occupied" }} occupied", SE.BlueSoft, SE.Blue)
                    StatusPill("${bedRecords.count { it.status == "Available" }} available", SE.Mint, SE.MintText)
                    StatusPill("${bedRecords.count { it.status == "Reserved" }} reserved", SE.OrangeSoft, SE.Orange)
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            roomLabels.forEachIndexed { i, room ->
                val count = if (isServerListing) {
                    serverUnits.count { displayRoom(it.roomCode) == room }
                } else {
                    (declaredBedCount / roomCount + if (i < declaredBedCount % roomCount) 1 else 0).coerceIn(0, 6)
                }
                FilterChip(selected = roomIndex == i, onClick = { roomIndex = i }, label = { Text("$room · $count units") })
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            if (isServerListing && serverUnits.isNotEmpty())
                "Inventory is loaded from the StayEasy server. Status changes and resident assignments are confirmed by the server."
            else if (isServerListing)
                "This property exists on the server but has no room/bed units returned. Add inventory before attempting resident assignment."
            else
                "This is local/sample inventory. Status changes are saved on this device and are not shared with other accounts.",
            color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp
        )
        Spacer(Modifier.height(9.dp))
        LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(bedRecords, key = { it.id }) { record ->
                val status = record.status
                val colors = when (status) {
                    "Available" -> SE.Mint to SE.MintText
                    "Reserved" -> SE.OrangeSoft to SE.Orange
                    "Maintenance" -> SE.RedSoft to SE.Red
                    "Inactive" -> SE.Background to SE.Muted
                    else -> SE.BlueSoft to SE.Blue
                }
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(16.dp))
                        .background(SE.Surface).border(1.dp, SE.Border, RoundedCornerShape(16.dp))
                        .clickable { activeBed = record; showBedActions = true }.padding(11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(45.dp).clip(RoundedCornerShape(12.dp)).background(colors.first), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Bed, null, tint = colors.second)
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(record.unit, color = SE.Ink, fontWeight = FontWeight.Bold)
                        Text(record.details.ifBlank { "No additional details" }, color = SE.Muted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        StatusPill(status, colors.first, colors.second)
                        Text(money(record.amount.takeIf { it > 0 } ?: listing.monthlyRent), color = SE.Muted, fontSize = 10.sp)
                    }
                }
            }
        }
        PrimaryButton("Assign a resident", Icons.Filled.PersonAdd) {
            if (listing.serverPropertyId != null) {
                onAddResident(listing, emptyList())
            } else {
                val declaredOptions = roomLabels.flatMapIndexed { index, room ->
                    val roomBeds = (declaredBedCount / roomCount + if (index < declaredBedCount % roomCount) 1 else 0).coerceIn(0, 6)
                    bedLetters.take(roomBeds).map { letter -> "$room · Bed $letter" }
                }
                val existingStatus = StayRecordStore.load(context, "beds")
                    .filter { it.property == listing.name }
                    .associate { it.unit to it.status }
                val availableOptions = declaredOptions.filter { unit ->
                    existingStatus[unit] == null || existingStatus[unit].equals("Available", ignoreCase = true)
                }
                onAddResident(listing, availableOptions)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    if (showBedActions && activeBed != null) {
        val current = activeBed!!
        AlertDialog(
            onDismissRequest = { showBedActions = false },
            title = { Text(current.unit, color = SE.Ink, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Current status: ${current.status}", color = SE.Muted, fontSize = 12.sp)
                    val actions = when (current.status) {
                        "Available" -> listOf("Reserved", "Occupied", "Maintenance", "Inactive")
                        "Reserved" -> listOf("Occupied", "Available", "Maintenance")
                        "Occupied" -> listOf("Available", "Maintenance")
                        "Maintenance" -> listOf("Available", "Inactive")
                        else -> listOf("Available", "Maintenance")
                    }
                    actions.forEach { status ->
                        OutlinedButton(onClick = {
                            if (current.source == "Server" && status == "Occupied") {
                                notify("Link a verified resident account before marking this server unit occupied.")
                                showBedActions = false
                            } else {
                                if (current.source != "Server" && current.status == "Occupied" && status == "Available") {
                                    StayRecordStore.add(context, StayRecord(category = "stays", title = "Move-out recorded · ${current.unit}", details = "Bed moved to available. Complete notice-period validation, financial settlement and checklist in the production workflow.", property = listing.name, unit = current.unit, status = "Completed"))
                                }
                                if (current.source != "Server" && status == "Occupied") {
                                    StayRecordStore.add(context, StayRecord(category = "stays", title = "Stay assignment · ${current.unit}", details = "Set occupied in the bed board; link a resident, joining date, rent and deposit before production use.", property = listing.name, unit = current.unit, amount = listing.monthlyRent, status = "Active"))
                                }
                                changeStatus(current, status)
                                showBedActions = false
                            }
                        }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                            Text(when (status) {
                                "Reserved" -> "Reserve unit"
                                "Occupied" -> "Mark occupied / assign"
                                "Maintenance" -> "Block for maintenance"
                                "Available" -> "Mark available"
                                else -> "Deactivate unit"
                            })
                        }
                    }
                    if (current.status == "Occupied") {
                        Text("Releasing an occupied server unit may be rejected until the active stay and settlement are completed.", color = SE.Orange, fontSize = 10.sp, lineHeight = 14.sp)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBedActions = false }) { Text("Close") } },
            containerColor = SE.Surface
        )
    }
}


@Composable
private fun OwnerEnquiriesScreen(notify: (String) -> Unit) {
    var tab by remember { mutableStateOf("New") }
    var handled by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(13.dp)); Text("Enquiries", color = SE.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Move good leads forward without losing track", color = SE.Muted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("New", "Interested", "Visits", "All").forEach { FilterChip(selected = tab == it, onClick = { tab = it }, label = { Text(it) }) } }
        Spacer(Modifier.height(11.dp)); Text("${max(0, 4 - handled)} need a response · demo enquiries", color = SE.Muted, fontSize = 11.sp)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 15.dp)) {
            items(listOf(EnquiryData("Aman Verma", "2 sharing · from 15 Nov", "Is bed B still available?", "The Courtyard Co-Living"), EnquiryData("Priya Nair", "Private room · from 1 Dec", "Can I visit after work?", "Casa Duo"), EnquiryData("Rahul Patil", "3 sharing · immediate", "Does rent include meals?", "Maple House"), EnquiryData("Simran Kaur", "2 sharing · from 10 Nov", "Can you share room photos?", "CityNest Living"))) { enquiry ->
                Card(shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(13.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(CircleShape).background(SE.BlueSoft), contentAlignment = Alignment.Center) { Text(enquiry.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""), color = SE.Blue, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(enquiry.name, color = SE.Ink, fontWeight = FontWeight.Bold); Text(enquiry.stay, color = SE.Muted, fontSize = 10.sp) }; StatusPill("NEW", SE.OrangeSoft, SE.Orange) }
                        Spacer(Modifier.height(9.dp)); Text(enquiry.property, color = SE.Blue, fontSize = 11.sp, fontWeight = FontWeight.SemiBold); Text("“${enquiry.message}”", color = SE.Text, fontSize = 12.sp)
                        Spacer(Modifier.height(11.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { handled++; notify("Enquiry declined in demo") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Icon(Icons.Filled.Close, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Decline") }; Button(onClick = { handled++; notify("Enquiry accepted in demo; confirm availability before reserving") }, modifier = Modifier.weight(1.25f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)) { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Respond") } }
                    }
                }
            }
        }
    }
}
private data class EnquiryData(val name: String, val stay: String, val message: String, val property: String)

@Composable
private fun ResidentsScreen(onAdd: () -> Unit, notify: (String) -> Unit) {
    var tab by remember { mutableStateOf("Active") }
    var query by remember { mutableStateOf("") }
    val residents = listOf(ResidentData("Rohan Sharma", "Room 101 · Bed A", "₹9,000", "Paid"), ResidentData("Neha Singh", "Room 102 · Bed B", "₹9,000", "Due 1 Nov"), ResidentData("Amit Kumar", "Room 103 · Bed A", "₹8,500", "Paid"), ResidentData("Pooja Mehta", "Private Room 4", "₹14,000", "Due 5 Nov"))
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Residents", color = SE.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Current stays and upcoming moves", color = SE.Muted, fontSize = 11.sp) }; Button(onClick = onAdd, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = SE.Blue), contentPadding = PaddingValues(horizontal = 11.dp, vertical = 10.dp)) { Icon(Icons.Filled.Add, null); Text("Add") } }
        Spacer(Modifier.height(12.dp)); OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Search residents") }, shape = RoundedCornerShape(14.dp), colors = fieldColors())
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("Active", "Upcoming", "Moved out").forEach { FilterChip(selected = tab == it, onClick = { tab = it }, label = { Text(it) }) } }
        Spacer(Modifier.height(9.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 15.dp)) { items(residents.filter { it.name.contains(query, true) }) { resident ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(SE.Surface).clickable { notify("${resident.name}: sample resident record") }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(44.dp).clip(CircleShape).background(SE.Lavender), contentAlignment = Alignment.Center) { Text(resident.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""), color = SE.Ink, fontWeight = FontWeight.Bold) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(resident.name, color = SE.Ink, fontWeight = FontWeight.Bold); Text(resident.bed, color = SE.Muted, fontSize = 10.sp); Text(resident.status, color = if(resident.status == "Paid") SE.MintText else SE.Orange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }; Text(resident.rent, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp); Icon(Icons.Filled.ChevronRight, null, tint = SE.Muted) }
        } }
    }
}
private data class ResidentData(val name: String, val bed: String, val rent: String, val status: String)


@Composable
private fun AddServerResidentScreen(
    listing: PgListing,
    candidates: List<RemoteResidentCandidate>,
    candidatesLoading: Boolean,
    candidatesError: String,
    isSubmitting: Boolean,
    onRetryCandidates: () -> Unit,
    onCreateStay: (RemoteResidentCandidate, RemoteUnit, Int, Int, String) -> Unit,
    onCancel: () -> Unit
) {
    val availableUnits = remember(listing.serverUnits) {
        listing.serverUnits.filter { it.status.equals("available", ignoreCase = true) }
    }
    var selectedResidentId by remember(listing.serverPropertyId) {
        mutableStateOf(candidates.firstOrNull()?.id)
    }
    var selectedUnitId by remember(listing.serverPropertyId) {
        mutableStateOf(availableUnits.firstOrNull()?.id)
    }
    var rent by remember(listing.serverPropertyId) {
        mutableStateOf((availableUnits.firstOrNull()?.monthlyRent ?: listing.monthlyRent).toString())
    }
    var deposit by remember(listing.serverPropertyId) {
        mutableStateOf((availableUnits.firstOrNull()?.deposit ?: listing.deposit).toString())
    }
    var joiningDate by remember(listing.serverPropertyId) {
        mutableStateOf(java.time.LocalDate.now().toString())
    }
    var validationMessage by remember { mutableStateOf("") }
    val selectedResident = candidates.firstOrNull { it.id == selectedResidentId }
    val selectedUnit = availableUnits.firstOrNull { it.id == selectedUnitId }

    LaunchedEffect(candidates, listing.serverPropertyId) {
        if (candidates.none { it.id == selectedResidentId }) {
            selectedResidentId = candidates.firstOrNull()?.id
        }
    }
    LaunchedEffect(availableUnits, listing.serverPropertyId) {
        if (availableUnits.none { it.id == selectedUnitId }) {
            selectedUnitId = availableUnits.firstOrNull()?.id
            val first = availableUnits.firstOrNull()
            if (first != null) {
                rent = first.monthlyRent.toString()
                deposit = first.deposit.toString()
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader("Assign a resident", "Create a server-backed stay at ${listing.name}", onBack = onCancel)
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.BlueSoft)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(SE.Surface), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.VerifiedUser, null, tint = SE.Blue, modifier = Modifier.size(23.dp))
                }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(listing.name, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(listing.area, color = SE.Muted, fontSize = 11.sp)
                    Text("Connected to StayEasy server", color = SE.Blue, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Text("1. Verified resident account", color = SE.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Select the customer's existing StayEasy account. Contact details are not recreated locally.", color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(8.dp))
        if (candidatesLoading) {
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = SE.Blue)
                Spacer(Modifier.width(10.dp))
                Text("Loading eligible accounts…", color = SE.Muted, fontSize = 12.sp)
            }
        } else if (candidates.isEmpty()) {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
                Column(Modifier.fillMaxWidth().padding(13.dp)) {
                    Text("No eligible residents yet", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Only active customer accounts with verified email and an enquiry or reservation linked to this property can be assigned.", color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp)
                    Spacer(Modifier.height(9.dp))
                    OutlinedButton(onClick = onRetryCandidates, enabled = !isSubmitting, shape = RoundedCornerShape(11.dp)) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Refresh accounts")
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                candidates.forEach { resident ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            selectedResidentId = resident.id
                            validationMessage = ""
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = if (selectedResidentId == resident.id) SE.BlueSoft else SE.Surface)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(CircleShape).background(if (selectedResidentId == resident.id) SE.Surface else SE.Lavender), contentAlignment = Alignment.Center) {
                                Text(resident.displayName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase(Locale.ROOT), color = SE.Blue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(resident.displayName, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(resident.email, color = SE.Muted, fontSize = 10.sp)
                                Text(if (resident.emailVerified) "Email verified" else "Verification required", color = if (resident.emailVerified) SE.Blue else SE.Orange, fontWeight = FontWeight.SemiBold, fontSize = 10.sp)
                            }
                            if (selectedResidentId == resident.id) Icon(Icons.Filled.CheckCircle, null, tint = SE.Blue, modifier = Modifier.size(21.dp))
                        }
                    }
                }
            }
        }
        if (candidatesError.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(candidatesError, color = SE.Red, fontSize = 11.sp, lineHeight = 15.sp)
        }

        Spacer(Modifier.height(16.dp))
        Text("2. Available room or bed", color = SE.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Only units currently marked available on the server can be selected.", color = SE.Muted, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        if (availableUnits.isEmpty()) {
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
                Text("No server units are available. Return to Rooms & beds and refresh or update the inventory before assigning a resident.", color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(13.dp))
            }
        } else {
            availableUnits.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { unit ->
                        val roomNumber = unit.roomCode.removePrefix("ROOM-").removePrefix("R")
                        val label = if (unit.rentableType.equals("room", true)) {
                            "Room $roomNumber · Private"
                        } else {
                            "Room $roomNumber · Bed ${unit.unitCode.substringAfterLast("-", unit.unitCode).removePrefix("B")}"
                        }
                        FilterChip(
                            selected = selectedUnitId == unit.id,
                            onClick = {
                                selectedUnitId = unit.id
                                rent = unit.monthlyRent.toString()
                                deposit = unit.deposit.toString()
                                validationMessage = ""
                            },
                            label = { Text(label, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(15.dp))
        Text("3. Agreed terms", color = SE.Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Amounts are saved to the server with this stay. Deposit entries are manual records, not payment-provider verification.", color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                FormLabel("MONTHLY RENT (₹)")
                OutlinedTextField(rent, { rent = it.filter(Char::isDigit).take(7); validationMessage = "" }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Text("₹") }, shape = RoundedCornerShape(12.dp), colors = fieldColors())
            }
            Column(Modifier.weight(1f)) {
                FormLabel("DEPOSIT HELD (₹)")
                OutlinedTextField(deposit, { deposit = it.filter(Char::isDigit).take(8); validationMessage = "" }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Text("₹") }, shape = RoundedCornerShape(12.dp), colors = fieldColors())
            }
        }
        Spacer(Modifier.height(9.dp))
        FormLabel("JOINING DATE · YYYY-MM-DD")
        OutlinedTextField(joiningDate, { joiningDate = it.trim().take(10); validationMessage = "" }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("2026-11-01") }, shape = RoundedCornerShape(12.dp), colors = fieldColors())

        if (validationMessage.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(validationMessage, color = SE.Red, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                val rentValue = rent.toIntOrNull()
                val depositValue = deposit.toIntOrNull()
                val dateValid = runCatching { java.time.LocalDate.parse(joiningDate.trim()) }.isSuccess
                validationMessage = when {
                    selectedResident == null -> "Select a verified resident account."
                    selectedUnit == null -> "Select an available room or bed."
                    rentValue == null || rentValue !in 100..100000 -> "Enter monthly rent between ₹100 and ₹1,00,000."
                    depositValue == null || depositValue < 0 -> "Enter a valid deposit amount."
                    !dateValid -> "Enter the joining date as YYYY-MM-DD."
                    else -> ""
                }
                if (validationMessage.isBlank() && selectedResident != null && selectedUnit != null && rentValue != null && depositValue != null) {
                    onCreateStay(selectedResident, selectedUnit, rentValue, depositValue, joiningDate.trim())
                }
            },
            enabled = !isSubmitting && !candidatesLoading && candidates.isNotEmpty() && availableUnits.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(13.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Creating stay…")
            } else {
                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("Confirm resident assignment", fontWeight = FontWeight.Bold)
            }
        }
        Text("Inventory changes only after the server confirms the stay.", color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun AddResidentScreen(propertyName: String, bedOptions: List<String>, onSave: (String, String, Int, String, String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var rent by remember(propertyName) { mutableStateOf("9000") }
    var bed by remember(propertyName, bedOptions) { mutableStateOf(bedOptions.firstOrNull() ?: "") }
    var join by remember(propertyName) { mutableStateOf("1 Nov 2026") }
    var validationMessage by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        ScreenHeader("Add a resident", "Adding a resident to $propertyName", onBack = onCancel)
        Spacer(Modifier.height(14.dp))
        Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.BlueSoft)) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PersonAdd, null, tint = SE.Blue, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Start with the resident", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Add contact details first, then assign a room and enter the agreed rent.", color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
            Column(Modifier.padding(14.dp)) {
                Text("1. Contact information", color = SE.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(9.dp))
                FormLabel("FULL NAME · REQUIRED")
                OutlinedTextField(name, { name = it; validationMessage = "" }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Resident's full name") }, shape = RoundedCornerShape(13.dp), colors = fieldColors())
                Spacer(Modifier.height(10.dp))
                FormLabel("MOBILE NUMBER · REQUIRED")
                OutlinedTextField(phone, { phone = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15); validationMessage = "" }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("+91 98765 43210") }, shape = RoundedCornerShape(13.dp), colors = fieldColors())
            }
        }
        Spacer(Modifier.height(11.dp))
        Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
            Column(Modifier.padding(14.dp)) {
                Text("2. Room assignment", color = SE.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(if (bedOptions.isEmpty()) "There are no beds currently marked available. Update a bed's status in Rooms & beds before assigning a resident." else "Choose an available bed in $propertyName.", color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp)
                Spacer(Modifier.height(8.dp))
                bedOptions.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { option -> FilterChip(selected = bed == option, onClick = { bed = option }, label = { Text(option, fontSize = 10.sp) }) }
                    }
                }
            }
        }
        Spacer(Modifier.height(11.dp))
        Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface)) {
            Column(Modifier.padding(14.dp)) {
                Text("3. Rent & move-in", color = SE.Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Use the amount and date agreed with the resident.", color = SE.Muted, fontSize = 11.sp)
                Spacer(Modifier.height(9.dp))
                FormLabel("MONTHLY RENT (₹)")
                OutlinedTextField(rent, { rent = it.filter(Char::isDigit).take(7); validationMessage = "" }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Text("₹") }, placeholder = { Text("9000") }, shape = RoundedCornerShape(13.dp), colors = fieldColors())
                Spacer(Modifier.height(9.dp))
                FormLabel("JOINING DATE")
                OutlinedTextField(join, { join = it; validationMessage = "" }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("e.g. 1 Nov 2026") }, shape = RoundedCornerShape(13.dp), colors = fieldColors())
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("This prototype saves the resident locally. Confirm availability, agreement, deposit and identity before treating a move-in as final.", color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp)
        if (validationMessage.isNotBlank()) {
            Spacer(Modifier.height(7.dp))
            Text(validationMessage, color = SE.Red, fontSize = 11.sp)
        }
        Spacer(Modifier.height(15.dp))
        PrimaryButton("Save resident record", Icons.Filled.CheckCircle) {
            val digits = phone.count(Char::isDigit)
            validationMessage = when {
                name.isBlank() -> "Enter the resident's full name."
                bedOptions.isEmpty() || bed.isBlank() -> "No available bed is selected. Update a bed to Available in Rooms & beds first."
                digits !in 10..13 -> "Enter a valid mobile number with 10–13 digits."
                (rent.toIntOrNull() ?: 0) !in 100..100000 -> "Enter a monthly rent between ₹100 and ₹1,00,000."
                join.isBlank() -> "Enter the joining date, or a date to be confirmed."
                else -> ""
            }
            if (validationMessage.isBlank()) onSave(name.trim(), phone.trim(), rent.toIntOrNull() ?: 0, bed, join.trim())
        }
        Spacer(Modifier.height(18.dp))
    }
}
@Composable
private fun OwnerPaymentsScreen(notify: (String) -> Unit) {
    var tab by remember { mutableStateOf("Pending") }
    val paid = remember { mutableStateListOf<String>() }
    val rows = listOf(PaymentEntry("Rohan Sharma", "Room 101 · Bed A", 9000, "1 Nov"), PaymentEntry("Neha Singh", "Room 102 · Bed B", 9000, "1 Nov"), PaymentEntry("Amit Kumar", "Room 103 · Bed A", 8500, "3 Nov"), PaymentEntry("Pooja Mehta", "Private Room 4", 14000, "5 Nov"))
    val visible = rows.filter { if (tab == "Pending") !paid.contains(it.name) else if (tab == "Received") paid.contains(it.name) else true }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp)); Text("Rent & payments", color = SE.Ink, fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Keep charges, receipts and balances clear", color = SE.Muted, fontSize = 11.sp)
        Spacer(Modifier.height(13.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatMini("Expected", "₹40,500", SE.BlueSoft, SE.Blue, Modifier.weight(1f)); StatMini("Received", money(rows.filter { paid.contains(it.name) }.sumOf { it.amount }), SE.Mint, SE.MintText, Modifier.weight(1f)) }
        Spacer(Modifier.height(13.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("Pending", "Received", "All").forEach { FilterChip(selected = tab == it, onClick = { tab = it }, label = { Text(it) }) } }
        Spacer(Modifier.height(10.dp)); LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 14.dp)) { items(visible) { row ->
            Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(13.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(row.name, color = SE.Ink, fontWeight = FontWeight.Bold); Text(row.bed, color = SE.Muted, fontSize = 10.sp) }; Text(money(row.amount), color = SE.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }; Spacer(Modifier.height(9.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Text(if(paid.contains(row.name)) "Received · receipt recorded" else "Due ${row.due} · sample ledger", color = if(paid.contains(row.name)) SE.MintText else SE.Orange, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); if(!paid.contains(row.name)) Button(onClick = { paid.add(row.name); notify("Payment recorded for ${row.name}; demo ledger only") }, shape = RoundedCornerShape(11.dp), colors = ButtonDefaults.buttonColors(containerColor = SE.Blue), contentPadding = PaddingValues(horizontal = 11.dp, vertical = 7.dp)) { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(4.dp)); Text("Mark received", fontSize = 10.sp) } else TextButton(onClick = { notify("Receipt preview: demo only") }) { Text("Receipt", color = SE.Blue) } } } }
        } }
        Text("Manual payment entry is not bank verification. A production ledger must preserve corrections, references and audit history.", color = SE.Muted, fontSize = 10.sp, lineHeight = 15.sp)
    }
}
private data class PaymentEntry(val name: String, val bed: String, val amount: Int, val due: String)

@Composable
private fun OwnerIssuesScreen(notify: (String) -> Unit) {
    var tab by remember { mutableStateOf("Open") }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(14.dp)); Text("Maintenance & issues", color = SE.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("Track ownership, progress and resolution", color = SE.Muted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Open", "In progress", "Resolved").forEach { FilterChip(selected = tab == it, onClick = { tab = it }, label = { Text(it) }) } }
        Spacer(Modifier.height(11.dp)); val issues = listOf(Triple("Water pressure low", "Room 204 · reported 2 hrs ago", "Urgent"), Triple("Wi-Fi unstable", "Floor 2 · reported yesterday", "Normal"), Triple("Bathroom tap leaking", "Room 101 · reported yesterday", "Normal"))
        issues.forEach { issue ->
            Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth().padding(bottom = 9.dp)) { Column(Modifier.padding(13.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(if(issue.third == "Urgent") SE.RedSoft else SE.OrangeSoft), contentAlignment = Alignment.Center) { Icon(Icons.Filled.ReportProblem, null, tint = if(issue.third == "Urgent") SE.Red else SE.Orange) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(issue.first, color = SE.Ink, fontWeight = FontWeight.Bold); Text(issue.second, color = SE.Muted, fontSize = 10.sp) }; StatusPill(issue.third, if(issue.third == "Urgent") SE.RedSoft else SE.OrangeSoft, if(issue.third == "Urgent") SE.Red else SE.Orange) }; Spacer(Modifier.height(11.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = { notify("Issue details opened in demo") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(11.dp)) { Text("View details") }; Button(onClick = { notify(if(tab == "Resolved") "Issue reopened in demo" else "Issue moved to next status in demo") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(11.dp), colors = ButtonDefaults.buttonColors(containerColor = SE.Blue)) { Text(if(tab == "Resolved") "Reopen" else "Update status") } } } }
        }
        Spacer(Modifier.height(8.dp)); Text("Issue updates are local demo interactions only.", color = SE.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun OwnerAnalyticsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(17.dp)) {
        ScreenHeader("Business insights", "Understand occupancy and performance", onBack = onBack, trailing = { StatusPill("30 days", SE.BlueSoft, SE.Blue) })
        Spacer(Modifier.height(15.dp)); Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { MetricCard("Occupancy", "82%", "+6% this month", SE.BlueSoft, SE.Blue, Modifier.weight(1f)); MetricCard("Rent recorded", "₹1.6L", "Demo total", SE.Mint, SE.MintText, Modifier.weight(1f)) }
        Spacer(Modifier.height(13.dp)); Card(shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(15.dp)) { Text("Occupancy trend", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text("Illustrative monthly data", color = SE.Muted, fontSize = 10.sp); Spacer(Modifier.height(15.dp)); MiniBarChart(Modifier.fillMaxWidth().height(150.dp)); Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("Jun", "Jul", "Aug", "Sep", "Oct").forEach { Text(it, color = SE.Muted, fontSize = 10.sp) } } } }
        Spacer(Modifier.height(13.dp)); Text("What to look at next", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(8.dp)); TaskCard("Keep listings fresh", "Properties with older confirmations may lose enquiries", Icons.Filled.Refresh, "", SE.BlueSoft, SE.Blue, {}); TaskCard("Follow up visit outcomes", "Record whether a visit led to a move-in", Icons.Filled.CalendarToday, "", SE.OrangeSoft, SE.Orange, {}); TaskCard("Review outstanding balances", "Use the payments ledger before sending reminders", Icons.Filled.Payments, "", SE.Mint, SE.MintText, {})
        Spacer(Modifier.height(15.dp)); Text("Analytics here are mock numbers. Production reports must be computed from persisted, reconciled records.", color = SE.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun OwnerMoreScreen(
    onProperties: () -> Unit,
    onResidents: () -> Unit,
    onMoveOutRequests: () -> Unit,
    onIssues: () -> Unit,
    onAnalytics: () -> Unit,
    onTenant: () -> Unit,
    onProfile: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        ScreenHeader("Manage your business", "Everything beyond today's dashboard")
        Spacer(Modifier.height(17.dp))
        MenuRow("My properties", "Listing details, rooms and publishing", Icons.Filled.Apartment, onProperties)
        MenuRow("Residents", "Stay records and move-in / move-out", Icons.Filled.Person, onResidents)
        MenuRow("Move-out requests", "Review tenant dates, approve notice or respond with a note", Icons.Filled.CalendarToday, onMoveOutRequests)
        MenuRow("Maintenance", "Issue queue and resolution", Icons.Filled.ReportProblem, onIssues)
        MenuRow("Business insights", "Occupancy and collection trends", Icons.AutoMirrored.Filled.TrendingUp, onAnalytics)
        MenuRow("All owner workflows", "Rent, deposits, expenses, staff, food, reports and settings", Icons.Filled.Settings) { onProfile() }
        Spacer(Modifier.height(14.dp)); Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = SE.Ink), modifier = Modifier.fillMaxWidth().clickable(onClick = onTenant)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Home, null, tint = Color.White); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Switch to tenant view", color = Color.White, fontWeight = FontWeight.Bold); Text("Preview the customer experience", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp) }; Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White) } }
    }
}

@Composable
private fun OwnerSettingsScreen(onBack: () -> Unit) {
    var rentReminders by remember { mutableStateOf(true) }
    var inquiryAlerts by remember { mutableStateOf(true) }
    var issueAlerts by remember { mutableStateOf(true) }
    var staffAccess by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)) {
        ScreenHeader("Preferences & access", "Keep your operations the way you like", onBack = onBack)
        Spacer(Modifier.height(17.dp)); Text("NOTIFICATIONS", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        listOf("Rent reminders" to rentReminders, "New enquiry alerts" to inquiryAlerts, "Maintenance issue alerts" to issueAlerts).forEach { (label, current) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(label, color = SE.Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp); Text("Demo preference · no notification is sent", color = SE.Muted, fontSize = 10.sp) }; Switch(checked = current, onCheckedChange = { value -> when(label) { "Rent reminders" -> rentReminders = value; "New enquiry alerts" -> inquiryAlerts = value; else -> issueAlerts = value } }) }; HorizontalDivider(color = SE.Border)
        }
        Spacer(Modifier.height(17.dp)); Text("TEAM ACCESS", color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Card(shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SE.BlueSoft), contentAlignment = Alignment.Center) { Icon(Icons.Filled.PersonAdd, null, tint = SE.Blue) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("Invite a property manager", color = SE.Ink, fontWeight = FontWeight.Bold); Text("Assign selected buildings and tasks", color = SE.Muted, fontSize = 10.sp) }; Switch(checked = staffAccess, onCheckedChange = { staffAccess = it }) }; if (staffAccess) { Spacer(Modifier.height(10.dp)); Text("Manager role preview enabled. Real invitations and permissions require server-side identity and access control.", color = SE.Muted, fontSize = 11.sp, lineHeight = 16.sp) } } }
        Spacer(Modifier.height(18.dp)); Text("Other settings", color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 15.sp); MenuRow("Privacy and security", "Permissions and data access", Icons.Filled.Lock) {}; MenuRow("Business profile", "Owner contact and business details", Icons.Filled.Apartment) {}
        Spacer(Modifier.height(15.dp)); Text("Changes are stored in this screen's demo state only.", color = SE.Muted, fontSize = 10.sp)
    }
}

@Composable
private fun BrandMark() {
    Box(Modifier.size(45.dp).clip(RoundedCornerShape(15.dp)).background(SE.Ink), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(25.dp)) {
            val w = size.width; val h = size.height
            val roof = Path().apply { moveTo(w * 0.08f, h * 0.48f); lineTo(w * 0.5f, h * 0.08f); lineTo(w * 0.92f, h * 0.48f); lineTo(w * 0.84f, h * 0.55f); lineTo(w * 0.5f, h * 0.23f); lineTo(w * 0.16f, h * 0.55f); close() }
            drawPath(roof, Color.White)
            drawRoundRect(Color.White, topLeft = Offset(w * 0.2f, h * 0.47f), size = Size(w * 0.6f, h * 0.43f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.04f))
            drawRect(SE.Orange, topLeft = Offset(w * 0.43f, h * 0.65f), size = Size(w * 0.15f, h * 0.25f))
        }
    }
}

@Composable
private fun PropertyCover(photoUri: String, variant: Int, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(SE.Background)) {
        PropertyArt(variant, Modifier.fillMaxSize())
        if (photoUri.isNotBlank()) AsyncImage(model = photoUri, contentDescription = "Property photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun PropertyArt(variant: Int, modifier: Modifier = Modifier) {
    val wallColors = listOf(Color(0xFFF1E7D9), Color(0xFFE2E9E9), Color(0xFFEAE6F2), Color(0xFFF4E6DF), Color(0xFFE4EBDD))
    val floorColors = listOf(Color(0xFFD2B49A), Color(0xFFB8C6C2), Color(0xFFC4B8D2), Color(0xFFD8BBA9), Color(0xFFB8C7B1))
    val index = ((variant % wallColors.size) + wallColors.size) % wallColors.size
    Canvas(modifier.clip(RoundedCornerShape(16.dp))) {
        val w = size.width; val h = size.height
        drawRect(Brush.verticalGradient(listOf(wallColors[index], wallColors[index].copy(alpha = 0.88f))), size = size)
        drawRect(floorColors[index], topLeft = Offset(0f, h * 0.72f), size = Size(w, h * 0.28f))
        drawRect(Color.White.copy(alpha = 0.85f), topLeft = Offset(w * 0.10f, h * 0.10f), size = Size(w * 0.31f, h * 0.37f))
        drawRect(Color(0xFFBFD1D9), topLeft = Offset(w * 0.12f, h * 0.12f), size = Size(w * 0.27f, h * 0.33f))
        drawRect(Color.White.copy(alpha = 0.78f), topLeft = Offset(w * 0.245f, h * 0.12f), size = Size(w * 0.018f, h * 0.33f))
        drawRect(Color.White.copy(alpha = 0.78f), topLeft = Offset(w * 0.12f, h * 0.27f), size = Size(w * 0.27f, h * 0.018f))
        // Headboard and bed
        drawRoundRect(Color(0xFF8D7768), topLeft = Offset(w * 0.31f, h * 0.42f), size = Size(w * 0.56f, h * 0.27f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.025f))
        drawRoundRect(Color(0xFFF9F7F2), topLeft = Offset(w * 0.27f, h * 0.53f), size = Size(w * 0.62f, h * 0.24f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.035f))
        drawRoundRect(Color(0xFF8BA2B5), topLeft = Offset(w * 0.32f, h * 0.54f), size = Size(w * 0.22f, h * 0.10f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.025f))
        drawRoundRect(Color(0xFFD4A08A), topLeft = Offset(w * 0.55f, h * 0.54f), size = Size(w * 0.23f, h * 0.10f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.025f))
        drawRoundRect(Color(0xFFE0C3AE), topLeft = Offset(w * 0.29f, h * 0.66f), size = Size(w * 0.58f, h * 0.11f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.025f))
        // Side table and lamp
        drawRoundRect(Color(0xFF9A7861), topLeft = Offset(w * 0.05f, h * 0.67f), size = Size(w * 0.17f, h * 0.08f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.012f))
        drawRect(Color(0xFF6F5543), topLeft = Offset(w * 0.075f, h * 0.75f), size = Size(w * 0.025f, h * 0.14f))
        drawRect(Color(0xFF6F5543), topLeft = Offset(w * 0.18f, h * 0.75f), size = Size(w * 0.025f, h * 0.14f))
        drawLine(Color(0xFF8E6B4B), Offset(w * 0.14f, h * 0.47f), Offset(w * 0.14f, h * 0.65f), strokeWidth = w * 0.012f)
        drawCircle(Color(0xFFE9BC6F), radius = w * 0.065f, center = Offset(w * 0.14f, h * 0.47f))
        // Potted plant
        drawRoundRect(Color(0xFFBE8563), topLeft = Offset(w * 0.86f, h * 0.76f), size = Size(w * 0.09f, h * 0.12f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.015f))
        drawLine(Color(0xFF5E8467), Offset(w * 0.9f, h * 0.78f), Offset(w * 0.9f, h * 0.51f), strokeWidth = w * 0.012f, cap = StrokeCap.Round)
        drawLine(Color(0xFF5E8467), Offset(w * 0.9f, h * 0.64f), Offset(w * 0.84f, h * 0.57f), strokeWidth = w * 0.025f, cap = StrokeCap.Round)
        drawLine(Color(0xFF5E8467), Offset(w * 0.9f, h * 0.62f), Offset(w * 0.96f, h * 0.54f), strokeWidth = w * 0.025f, cap = StrokeCap.Round)
        drawLine(Color(0xFF5E8467), Offset(w * 0.9f, h * 0.56f), Offset(w * 0.86f, h * 0.48f), strokeWidth = w * 0.02f, cap = StrokeCap.Round)
    }
}

@Composable
private fun ListingCard(p: PgListing, isFavorite: Boolean, onClick: () -> Unit, onFavorite: () -> Unit, compact: Boolean, favoriteAction: Boolean = false) {
    Card(shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = SE.Surface), elevation = CardDefaults.cardElevation(0.dp), modifier = Modifier.fillMaxWidth().padding(bottom = if(compact) 0.dp else 8.dp).clickable(onClick = onClick)) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                PropertyCover(p.photoUri, (p.id % 4).toInt(), Modifier.size(if (compact) 118.dp else 126.dp, if (compact) 132.dp else 142.dp))
                if (p.isDemo) Box(Modifier.align(Alignment.TopStart).padding(5.dp).clip(RoundedCornerShape(6.dp)).background(Color.White.copy(alpha = 0.9f)).padding(horizontal = 6.dp, vertical = 3.dp)) { Text("DEMO", color = SE.Ink, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.Top) { Text(p.name, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)); Icon(if(isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null, tint = if(isFavorite) SE.Orange else SE.Muted, modifier = Modifier.size(18.dp).clickable(onClick = onFavorite)) }
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.LocationOn, null, tint = SE.Muted, modifier = Modifier.size(12.dp)); Text("${p.area} · ${if (p.distanceIsVerified) "${p.distanceKm} km" else "distance unverified"}", color = SE.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Row(verticalAlignment = Alignment.CenterVertically) { if (p.rating > 0) { Icon(Icons.Filled.Star, null, tint = Color(0xFFE4A128), modifier = Modifier.size(13.dp)); Text("${p.rating}", color = SE.Text, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(" · ", color = SE.Muted, fontSize = 10.sp) }; Text(p.sharing, color = SE.Muted, fontSize = 10.sp) }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { StatusPill(p.allowedFor, SE.BlueSoft, SE.Blue); if(p.couplesAllowed) StatusPill("Couples", SE.OrangeSoft, SE.Orange) }
                Spacer(Modifier.height(1.dp))
                Row(verticalAlignment = Alignment.Bottom) { Column(Modifier.weight(1f)) { Text(money(p.monthlyRent), color = SE.Ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp); Text("per month", color = SE.Muted, fontSize = 9.sp) }; Column(horizontalAlignment = Alignment.End) { Text("${p.availableBeds} available", color = SE.MintText, fontSize = 10.sp, fontWeight = FontWeight.Bold); if(favoriteAction) Text("Remove", color = SE.Orange, fontSize = 10.sp) } }
            }
        }
    }
}

@Composable
private fun DemoMapArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFFE8EDE7), size = size)
        val w = size.width; val h = size.height
        val park = Color(0xFFD5E8D1)
        listOf(Offset(w * 0.03f, h * 0.06f), Offset(w * 0.72f, h * 0.04f), Offset(w * 0.66f, h * 0.65f), Offset(w * 0.06f, h * 0.78f)).forEachIndexed { i, pos ->
            drawRoundRect(park, topLeft = pos, size = Size(if(i % 2 == 0) w*0.22f else w*0.19f, h*0.14f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w*0.025f))
        }
        val pathColor = Color(0xFFFCFCF7)
        val roads = listOf(
            listOf(Offset(-w*0.1f,h*0.26f), Offset(w*0.28f,h*0.28f), Offset(w*0.43f,h*0.22f), Offset(w*1.1f,h*0.3f)),
            listOf(Offset(-w*0.05f,h*0.59f), Offset(w*0.24f,h*0.53f), Offset(w*0.55f,h*0.58f), Offset(w*1.1f,h*0.48f)),
            listOf(Offset(w*0.31f,-h*0.1f), Offset(w*0.36f,h*0.27f), Offset(w*0.33f,h*0.66f), Offset(w*0.47f,h*1.1f)),
            listOf(Offset(w*0.78f,-h*0.1f), Offset(w*0.7f,h*0.3f), Offset(w*0.81f,h*0.65f), Offset(w*0.75f,h*1.1f))
        )
        roads.forEach { points -> val path = Path().apply { moveTo(points[0].x, points[0].y); for(i in 1 until points.size) lineTo(points[i].x, points[i].y) }; drawPath(path, pathColor, style = Stroke(width = w*0.045f, cap = StrokeCap.Round)); drawPath(path, Color(0xFFD4D9D2), style = Stroke(width = w*0.003f, cap = StrokeCap.Round)) }
        // Small blocks and green spaces give a map-like visual without implying live mapping data.
        for (r in 0..4) for (c in 0..3) {
            val x = w * (0.04f + c * 0.25f); val y = h * (0.04f + r * 0.19f)
            if ((r + c) % 3 != 0) drawRoundRect(Color(0xFFDDE2DE), topLeft = Offset(x, y), size = Size(w*0.1f,h*0.065f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(w*0.012f))
        }
    }
}

@Composable
private fun MapActionPill(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(SE.Surface).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = SE.Blue, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text(label, color = SE.Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun ChatBubble(text: String, owner: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if(owner) Arrangement.End else Arrangement.Start) {
        Box(Modifier.widthIn(max = 290.dp).clip(RoundedCornerShape(topStart = 17.dp, topEnd = 17.dp, bottomStart = if(owner) 17.dp else 4.dp, bottomEnd = if(owner) 4.dp else 17.dp)).background(if(owner) SE.BlueSoft else Color.White).padding(13.dp)) { Text(text, color = SE.Text, fontSize = 12.sp, lineHeight = 18.sp) }
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun RoomOption(title: String, rent: Int, beds: Int, selected: Boolean, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(15.dp)).background(if(selected) SE.BlueSoft else Color.White).border(1.dp, if(selected) SE.Blue else SE.Border, RoundedCornerShape(15.dp)).clickable(onClick = onSelect).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(SE.Surface), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Bed, null, tint = SE.Blue) }
        Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(if(beds > 0) "$beds beds currently shown available" else "Ask owner about availability", color = SE.Muted, fontSize = 10.sp) }; Column(horizontalAlignment = Alignment.End) { Text(money(rent), color = SE.Ink, fontWeight = FontWeight.Bold); Text("monthly", color = SE.Muted, fontSize = 9.sp) }; if(selected) { Spacer(Modifier.width(5.dp)); Icon(Icons.Filled.CheckCircle, null, tint = SE.Blue, modifier = Modifier.size(17.dp)) }
    }
}

@Composable
private fun FormLabel(text: String) { Text(text, color = SE.Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = .7.sp, modifier = Modifier.padding(bottom = 6.dp)) }

@Composable
private fun FlowChips(options: List<String>, selected: MutableList<String>, selectable: Boolean = true, singleSelection: Boolean = false, onPick: ((String) -> Unit)? = null) {
    Column {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    val isSelected = selected.contains(option)
                    FilterChip(selected = isSelected, onClick = { if (selectable) { if (singleSelection) { selected.clear(); selected.add(option) } else { if (isSelected) selected.remove(option) else selected.add(option) } }; onPick?.invoke(option) }, label = { Text(option, fontSize = 11.sp, maxLines = 1) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (onBack != null) { RoundIconButton(Icons.AutoMirrored.Filled.ArrowBack, onBack); Spacer(Modifier.width(10.dp)) }
        Column(Modifier.weight(1f)) { Text(title, color = SE.Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp); if (subtitle != null) Text(subtitle, color = SE.Muted, fontSize = 11.sp, lineHeight = 15.sp) }
        if (trailing != null) trailing()
    }
}

@Composable
private fun RoundIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, tint: Color = SE.Ink) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(SE.Surface).border(1.dp, SE.Border, CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp)) }
}

@Composable
private fun PrimaryButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = SE.Blue), contentPadding = PaddingValues(horizontal = 17.dp)) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp); if (icon != null) { Spacer(Modifier.width(8.dp)); Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
private fun StatusPill(text: String, background: Color, foreground: Color) {
    Box(Modifier.clip(RoundedCornerShape(9.dp)).background(background).padding(horizontal = 8.dp, vertical = 5.dp)) { Text(text, color = foreground, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
}

@Composable
private fun MetricCard(title: String, value: String, caption: String, background: Color, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(17.dp)).background(SE.Surface).padding(13.dp)) { Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(background), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = tint, modifier = Modifier.size(17.dp)) }; Spacer(Modifier.height(10.dp)); Text(value, color = SE.Ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold); Text(title, color = SE.Text, fontWeight = FontWeight.SemiBold, fontSize = 11.sp); Text(caption, color = SE.Muted, fontSize = 9.sp) }
}

@Composable
private fun SmallMetric(value: String, label: String) {
    Column(Modifier.clip(RoundedCornerShape(10.dp)).background(SE.Background).padding(horizontal = 11.dp, vertical = 8.dp)) { Text(value, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(label, color = SE.Muted, fontSize = 9.sp) }
}

@Composable
private fun TaskCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector, count: String, background: Color, tint: Color, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(16.dp)).background(SE.Surface).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(background), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp)) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(detail, color = SE.Muted, fontSize = 10.sp, lineHeight = 14.sp) }; if (count.isNotBlank()) Box(Modifier.clip(CircleShape).background(background).padding(horizontal = 9.dp, vertical = 6.dp)) { Text(count, color = tint, fontWeight = FontWeight.Bold, fontSize = 11.sp) }; Spacer(Modifier.width(3.dp)); Icon(Icons.Filled.ChevronRight, null, tint = SE.Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun QuickAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, background: Color, tint: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(SE.Surface).clickable(onClick = onClick).padding(vertical = 14.dp, horizontal = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(39.dp).clip(RoundedCornerShape(12.dp)).background(background), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }; Spacer(Modifier.height(8.dp)); Text(label, color = SE.Ink, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

@Composable
private fun DetailLine(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(35.dp).clip(RoundedCornerShape(11.dp)).background(SE.Surface), contentAlignment = Alignment.Center) { Icon(icon, null, tint = SE.Blue, modifier = Modifier.size(18.dp)) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = SE.Text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(detail, color = SE.Muted, fontSize = 10.sp) }; Icon(Icons.Filled.ChevronRight, null, tint = SE.Muted, modifier = Modifier.size(18.dp)) }
}

@Composable
private fun MenuRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(SE.BlueSoft), contentAlignment = Alignment.Center) { Icon(icon, null, tint = SE.Blue) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, color = SE.Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text(subtitle, color = SE.Muted, fontSize = 10.sp) }; Icon(Icons.Filled.ChevronRight, null, tint = SE.Muted) }; HorizontalDivider(color = SE.Border)
}

@Composable
private fun StatMini(title: String, value: String, background: Color, tint: Color, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(background).padding(13.dp)) { Text(title, color = tint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(5.dp)); Text(value, color = SE.Ink, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun MiniBarChart(modifier: Modifier = Modifier) {
    val values = listOf(0.54f, 0.66f, 0.62f, 0.78f, 0.84f, 0.82f)
    Canvas(modifier) {
        val gap = size.width * 0.055f; val barW = (size.width - gap * (values.size + 1)) / values.size
        for (i in values.indices) {
            val x = gap + i * (barW + gap); val barH = size.height * values[i]
            drawRoundRect(if(i == values.lastIndex) SE.Blue else Color(0xFFB7C7FF), topLeft = Offset(x, size.height - barH), size = Size(barW, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW * 0.22f))
        }
        for (i in 1..3) drawLine(SE.Border, Offset(0f, size.height * i / 4), Offset(size.width, size.height * i / 4), strokeWidth = 1.dp.toPx())
    }
}

@Composable
private fun EmptyState(title: String, detail: String, button: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 55.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(82.dp).clip(RoundedCornerShape(25.dp)).background(SE.BlueSoft), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Search, null, tint = SE.Blue, modifier = Modifier.size(37.dp)) }
        Spacer(Modifier.height(14.dp)); Text(title, color = SE.Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(detail, color = SE.Muted, fontSize = 12.sp, lineHeight = 18.sp); Spacer(Modifier.height(15.dp)); OutlinedButton(onClick = onClick, shape = RoundedCornerShape(13.dp)) { Text(button, color = SE.Blue) }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = SE.Blue, unfocusedBorderColor = SE.Border,
    focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
    focusedTextColor = SE.Text, unfocusedTextColor = SE.Text,
    focusedLabelColor = SE.Blue, cursorColor = SE.Blue
)
