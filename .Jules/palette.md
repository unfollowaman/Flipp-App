## 2026-03-31 - Password Field Visibility Toggle Accessibility in Jetpack Compose
**Learning:** Icon-only action buttons inside Compose `OutlinedTextField` trailing icons need `IconButton` wrappers to meet minimum 48dp touch target guidelines and dynamic `contentDescription`s ("Show password" / "Hide password") reflecting current state for screen readers.
**Action:** Always wrap trailing icon actions in `IconButton` and dynamically update `contentDescription` based on state instead of using static descriptions like "Lock icon".

## 2026-03-31 - Navigation Icon Buttons Accessibility in Jetpack Compose
**Learning:** Custom styled icon buttons in Jetpack Compose built with `Box` + `Modifier.clickable` lack proper `Role.Button` accessibility semantics and minimum 48dp touch target expansion. Using Material 3 `IconButton` ensures standard button semantics for screen readers and automatic touch target handling while retaining custom brutalist border/background styling.
**Action:** Replace `Box` + `Modifier.clickable` on icon-only navigation buttons with `IconButton`.
