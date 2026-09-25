## 2026-03-31 - Password Field Visibility Toggle Accessibility in Jetpack Compose
**Learning:** Icon-only action buttons inside Compose `OutlinedTextField` trailing icons need `IconButton` wrappers to meet minimum 48dp touch target guidelines and dynamic `contentDescription`s ("Show password" / "Hide password") reflecting current state for screen readers.
**Action:** Always wrap trailing icon actions in `IconButton` and dynamically update `contentDescription` based on state instead of using static descriptions like "Lock icon".
