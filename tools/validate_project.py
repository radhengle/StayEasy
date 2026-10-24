"""Static source/package checks. This is not a replacement for a Gradle build or emulator test."""
from pathlib import Path
import ast
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
main = root / "app/src/main/java/com/stayeasy/app/MainActivity.kt"
features = root / "app/src/main/java/com/stayeasy/app/FeatureScreens.kt"
store = root / "app/src/main/java/com/stayeasy/app/FeatureStore.kt"
api_client = root / "app/src/main/java/com/stayeasy/app/StayApiClient.kt"
manifest = root / "app/src/main/AndroidManifest.xml"
api_source = root / "backend/app/main.py"
auth_source = root / "backend/app/auth.py"
security_source = root / "backend/app/security.py"
models = root / "backend/app/models.py"
admin = root / "admin/index.html"

for path in [main, features, store, api_client, manifest, api_source, auth_source, security_source, models, admin]:
    assert path.is_file(), f"Required file missing: {path.relative_to(root)}"
main_text = main.read_text(encoding="utf-8")
feature_text = features.read_text(encoding="utf-8")
store_text = store.read_text(encoding="utf-8")
client_text = api_client.read_text(encoding="utf-8")
api_text = api_source.read_text(encoding="utf-8")
auth_text = auth_source.read_text(encoding="utf-8")
security_text = security_source.read_text(encoding="utf-8")
model_text = models.read_text(encoding="utf-8")
admin_text = admin.read_text(encoding="utf-8")

required_routes = [
    '"home" ->', '"search" ->', '"map" ->', '"filters" ->', '"details" ->',
    '"request_visit" ->', '"saved" ->', '"enquiries" ->', '"chat" ->',
    '"stay" ->', '"payments" ->', '"issue" ->', '"profile" ->',
    '"owner_dashboard" ->', '"owner_properties" ->', '"add_pg" ->', '"edit_pg" ->',
    '"owner_beds" ->', '"owner_enquiries" ->', '"owner_residents" ->',
    '"add_resident" ->', '"owner_payments" ->', '"owner_issues" ->',
    '"owner_analytics" ->', '"owner_settings" ->', '"feature_hub" ->',
    '"theme_settings" ->', '"api_connection" ->', '"compare" ->', '"account_auth" ->'
]
for route in required_routes:
    assert route in main_text, f"Missing Android route: {route}"
for marker in [
    '"Boys only"', '"Girls only"', '"Mixed"', 'couplesAllowed',
    'ListingStore.save(context, customListings.toList())', 'takePersistableUriPermission',
    'DemoMapArt', 'distanceIsVerified', 'StayUserDataStore.saveFavorites',
    'remoteListings', 'LocalDensity', 'AccountAuthScreen', 'restoreAccountSession',
    'Sign in / Create account', 'Verify email', 'Forgot password?'
]:
    assert marker in main_text, f"Missing Android behaviour marker: {marker}"
for marker in [
    'FeatureRecordsScreen', 'ComparePropertiesScreen', 'ThemeSettingsScreen',
    'RequestBookingScreen', 'ApiConnectionScreen',
    'maintenance', 'deposit', 'visit', 'reservation'
]:
    assert marker.lower() in feature_text.lower(), f"Missing feature screen marker: {marker}"
for marker in ['SharedPreferences', 'StayRecord', 'apiBaseUrl', 'favorites', 'recentLocations']:
    assert marker.lower() in store_text.lower(), f"Missing local store marker: {marker}"
assert 'putString("apiBaseUrl", value.apiBaseUrl)' in store_text, "API base URL preference is not persisted"
for marker in ['/health', '/properties?city=$encodedCity', 'distanceIsVerified = false', 'https', '/auth/register', '/auth/verify-email', '/auth/login', '/auth/forgot-password', '/auth/logout', 'AndroidKeyStore', 'restoreAccountSession']:
    assert marker.lower() in client_text.lower(), f"Missing API client marker: {marker}"
for route in [
    '"/register"', '"/verify-email"', '"/resend-verification"', '"/login"',
    '"/forgot-password"', '"/reset-password"', '"/me"', '"/logout"'
]:
    assert route in auth_text, f"Missing authentication route: {route}"
for marker in ["hash_password", "verify_password", "hash_one_time_code", "auth_version"]:
    assert marker in security_text or marker in auth_text, f"Missing authentication security contract: {marker}"
for route in [
    '"/health"', '"/properties"', '"/owner/properties"', '"/owner/units"',
    '"/enquiries"', '"/visits"', '"/reservations"', '"/owner/stays"',
    '"/owner/ledger"', '"/owner/billing/generate"', '"/issues"',
    '"/owner/notices"', '"/owner/staff"', '"/admin/listing-review"',
    '"/admin/reports"', '"/privacy/requests"', '"/amenities/catalog"'
]:
    assert route in api_text, f"Missing backend endpoint marker: {route}"
for model in [
    'class Property', 'class PropertyUnit', 'class Reservation', 'class ResidentStay',
    'class LedgerEntry', 'class MaintenanceIssue', 'class AuditLog', 'class PrivacyRequest',
    'class PlatformReport', 'class ResidentDocument'
]:
    assert model in model_text, f"Missing domain model: {model}"
for marker in ['/admin/listing-review', 'Approve', 'Request changes', 'Pause', 'Reject']:
    assert marker.lower() in admin_text.lower(), f"Missing moderation console marker: {marker}"

ET.parse(manifest)
ET.parse(root / "app/src/main/res/xml/network_security_config.xml")
assert (root / "settings.gradle.kts").exists()
assert (root / "app/build.gradle.kts").exists()
assert (root / "gradlew").exists() and (root / "gradlew.bat").exists(), "Gradle launchers missing"
assert "GRADLE_VERSION=8.9" in (root / "gradlew").read_text(encoding="utf-8")
assert "GRADLE_VERSION=8.9" in (root / "gradlew.bat").read_text(encoding="utf-8")
for doc in [
    "docs/MAP_INTEGRATION.md", "docs/GOOGLE_MAPS_INTEGRATION.md", "docs/TEST_PLAN.md",
    "docs/IMPLEMENTATION_MATRIX.md", "docs/PRODUCTION_READINESS.md"
]:
    assert (root / doc).is_file(), f"Missing documentation: {doc}"
assert "0.2.0-comprehensive-foundation" in (root / "app/build.gradle.kts").read_text(encoding="utf-8")
assert (root / "backend/alembic.ini").is_file()
assert (root / "backend/alembic/versions/20261024_01_initial_schema.py").is_file()
for py in (root / "backend").rglob("*.py"):
    ast.parse(py.read_text(encoding="utf-8"), filename=str(py))

print(f"PASS: Android navigation and feature markers ({len(required_routes)} routes), local persistence and theme hooks.")
print("PASS: API/client entry points, core domain models, moderation console, XML and project docs.")
print("NOTE: Static validation is not a Kotlin/Gradle build or emulator test.")
