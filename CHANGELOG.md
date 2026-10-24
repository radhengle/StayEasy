# Changelog

User-facing and release-relevant changes are recorded here. Unreleased entries are merged functionality or work currently under review; an entry is not a release certification.

## Unreleased

### Added
- Android owner resident assignment for server-backed properties: select an eligible existing customer account with verified email, select an available server unit, provide joining date and agreed rent/deposit, and create the stay through the owner API.
- On successful server stay creation, refresh server inventory so the unit occupancy comes from the backend rather than optimistic local state.
- CI emulator screenshot capture is being added for Customer Home, Explore search and Owner workspace preview.

### Changed
- README now documents the server-backed resident assignment boundary and the Android screenshot artifact download path.
- Acceptance and release checklists distinguish passing builds from staging integration and production launch gates.

### Validation
- PR #12 passed backend contract/project validation, Android emulator UI smoke tests, debug APK packaging and unsigned release APK packaging.
- Authenticated Android-to-staging-API resident assignment and production signing remain open release checks.
