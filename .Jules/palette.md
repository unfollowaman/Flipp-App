## 2026-03-31 - Password Field Visibility Toggle Accessibility in Jetpack Compose
**Learning:** Icon-only action buttons inside Compose `OutlinedTextField` trailing icons need `IconButton` wrappers to meet minimum 48dp touch target guidelines and dynamic `contentDescription`s ("Show password" / "Hide password") reflecting current state for screen readers.
**Action:** Always wrap trailing icon actions in `IconButton` and dynamically update `contentDescription` based on state instead of using static descriptions like "Lock icon".

## 2026-03-31 - Navigation Icon Buttons Accessibility in Jetpack Compose
**Learning:** Custom styled icon buttons in Jetpack Compose built with `Box` + `Modifier.clickable` lack proper `Role.Button` accessibility semantics and minimum 48dp touch target expansion. Using Material 3 `IconButton` ensures standard button semantics for screen readers and automatic touch target handling while retaining custom brutalist border/background styling.
**Action:** Replace `Box` + `Modifier.clickable` on icon-only navigation buttons with `IconButton`.

## 2026-03-31 - Option Selection Grids Accessibility in Jetpack Compose
**Learning:** Custom selection option grids built with `Box` + `Modifier.clickable` fail to communicate selection state (`selected` / `not selected`) and control type to screen readers. Replacing `.clickable` with `Modifier.selectable(selected = isSelected, role = Role.RadioButton, onClick = ...)` exposes `selected` state and `Role.RadioButton` accessibility semantics.
**Action:** Use `Modifier.selectable` instead of `.clickable` on custom selection option grids and toggle items.

## 2026-03-31 - Header Logo Navigation Region Accessibility in Jetpack Compose
**Learning:** Clickable brand/logo header regions built with `Row` + `Modifier.clickable` lack default button accessibility roles and action context for screen readers. Explicitly supplying `role = Role.Button` and `onClickLabel = "..."` to `Modifier.clickable` ensures accessibility services announce the element as an interactive button with clear navigation feedback.
**Action:** Always provide `role = Role.Button` and descriptive `onClickLabel` parameters when using `Modifier.clickable` on interactive brand/logo layout containers.

## 2026-03-31 - Form Fields and Sliders Content Description Accessibility in Jetpack Compose
**Learning:** `OutlinedTextField` inputs and `Slider` components in custom-styled Jetpack Compose forms often rely on separate visual `Text` labels placed above or beside them. Without explicit `.semantics { contentDescription = "..." }`, screen readers like TalkBack announce generic elements ("edit box", "slider 50%") without providing field context. Applying `contentDescription` semantics connects the field purpose to screen readers without altering custom brutalist layout borders or typography.
**Action:** Always add `.semantics { contentDescription = "..." }` to `OutlinedTextField` and `Slider` components in custom-styled forms where visual labels are rendered outside the input bounds.
