# DESIGN.md — Flipp Neobrutalist Design System & UI Guide

This document defines the visual identity, styling conventions, design tokens, component architecture, and accessibility standards for the **Flipp** application.

---

## 1. Visual Identity & Design Philosophy

Flipp adopts a **Neobrutalist design aesthetic**. Neobrutalism combines the raw, functional honesty of early web brutalism with modern, playful micro-interactions and high-accessibility design principles.

### Key Aesthetic Tenets:
- **High-Contrast Hard Borders:** Crisp, solid 2dp black outlines (`#0D0D0D`) frame every card, button, input container, and navigation header.
- **Solid Offset Drop Shadows:** Shadows are rendered as solid, unblurred black geometric planes offset by **4dp down and 4dp right**. No soft blur or ambient gradients are used.
- **Bold Pastel Accents:** Saturated, distinct pastel tones (Yellow, Sky Blue, Pink, Mint, Amber) demarcate tools, badges, and trust indicators against pure white (`#FFFFFF`) and warm cream (`#FAFAE8`) surfaces.
- **Physical Spring Press Feedback:** Interactive cards and buttons translate physically toward their shadow when pressed, simulating a mechanical switch.
- **Deliberate Typography:** Heavy sans-serif typefaces (Black 900 and Bold 700) ensure instant visual hierarchy and effortless scanning.

---

## 2. Color Palette & Tokens

All color tokens are centralized in `app/src/main/java/com/example/ui/theme/Color.kt`.

### Primary Palette

| Token Name | Hex Code | Swatch Preview | Usage & Semantic Purpose |
| :--- | :--- | :--- | :--- |
| `WhiteColor` / `BackgroundColor` | `#FFFFFF` | ![](https://via.placeholder.com/15/FFFFFF/000000?text=+) | Primary screen background, card surfaces, top app bar |
| `CreamColor` | `#FAFAE8` | ![](https://via.placeholder.com/15/FAFAE8/000000?text=+) | Secondary surface background, trust disclaimer banner |
| `BlackColor` | `#0D0D0D` | ![](https://via.placeholder.com/15/0D0D0D/FFFFFF?text=+) | Borders (2dp), solid drop shadows, primary text, icons |
| `YellowColor` | `#F5E642` | ![](https://via.placeholder.com/15/F5E642/000000?text=+) | "PDF → PNG" tool accent, highlighted badges, secondary brand |
| `SkyBlueColor` | `#6DC8F3` | ![](https://via.placeholder.com/15/6DC8F3/000000?text=+) | "Images → PDF" accent, security shield icon container |
| `PinkColor` | `#F075C2` | ![](https://via.placeholder.com/15/F075C2/000000?text=+) | "Merge PDFs" accent, tertiary highlights |
| `MintColor` | `#3DD68C` | ![](https://via.placeholder.com/15/3DD68C/000000?text=+) | Progress bar indicator, "LOCAL ONLY" tag, success status |
| `RedColor` | `#E8302A` | ![](https://via.placeholder.com/15/E8302A/FFFFFF?text=+) | "Split PDF" accent, error messages, validation warnings |
| `AmberColor` | `#F5A623` | ![](https://via.placeholder.com/15/F5A623/000000?text=+) | "Protect PDF" accent, warning states |

### Neutral & Utility Shades
- **Drop Zone Surface:** `#FAFAFA`
- **Subdued Text / Hints:** `#888888`
- **Secondary Body Text:** `#333333`
- **Muted Subtitles:** `BlackColor.copy(alpha = 0.5f)`

---

## 3. Typography System

Defined in `app/src/main/java/com/example/ui/theme/Type.kt` using standard Android `FontFamily.SansSerif`.

| Style Token | Weight | Size | Line Height | Letter Spacing | Context of Use |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `displayLarge` | Black (900) | 32sp | 38sp | -0.5sp | Hero headers, major screen titles |
| `headlineMedium` | Bold (700) | 24sp | 30sp | 0.0sp | Top navbar brand logo ("flipp") |
| `titleLarge` | Bold (700) | 20sp | 26sp | 0.0sp | Tool screen card titles, section headers |
| `titleMedium` | SemiBold (600) | 16sp | 22sp | 0.0sp | Card subtitles, input labels |
| `bodyLarge` | Normal (400) | 16sp | 24sp | +0.5sp | Paragraph body text, descriptions |
| `bodyMedium` | Normal (400) | 14sp | 20sp | +0.25sp | Secondary descriptions, preview text |
| `labelLarge` | Bold (700) | 15sp | 20sp | +0.5sp | Button labels, interactive triggers |
| `labelMedium` | Medium (500) | 12sp | 16sp | +0.5sp | Small labels, tool badges, progress percentages |
| `bodySmall` | Light (300) | 12sp | 16sp | 0.0sp | Auxiliary hints, legal disclaimer text |

---

## 4. Spacing, Borders & Shadows

- **Border Width:**
  - Standard structural borders: `2.dp` solid `BlackColor` (`#0D0D0D`).
  - Badges and inner chips: `1.dp` solid `BlackColor`.
  - DropZone boundary: `2.dp` solid `BlackColor` with dashed path effect `[15f, 15f]`.
- **Corner Radii:**
  - Standard cards and buttons: `10.dp`.
  - Tool hero banners and DropZones: `16.dp`.
  - Icon buttons and small avatars: `6.dp` or `12.dp`.
  - Badges and Progress bars: Capsule pills (`50` or `99.dp`).
- **Shadow Offset Convention:**
  - Solid shadow layer placed at `offset(4.dp, 4.dp)` directly beneath the foreground container.
  - No blur or Gaussian dispersion.
- **Screen Margins:**
  - Horizontal screen padding: `16.dp`.
  - Vertical screen padding: `20.dp`.
  - Inter-component vertical spacing: `16.dp` (`Arrangement.spacedBy(16.dp)`).

---

## 5. Core Reusable Components (`UiComponents.kt`)

### 5.1. `BrutalistShadowBox`
The foundation container for interactive cards and static containers.
```kotlin
BrutalistShadowBox(
    modifier = Modifier.fillMaxWidth(),
    backgroundColor = WhiteColor,
    shadowColor = BlackColor,
    cornerRadius = 10.dp,
    borderWidth = 2.dp,
    onClick = { /* optional */ },
    onClickLabel = "Open Tool"
) { /* Content */ }
```
- **Physics Behavior:** When `onClick != null`, the component listens to `collectIsPressedAsState()`. On press, `animateDpAsState` animates translation from `(0dp, 0dp)` to `(4dp, 4dp)` using a high-stiffness spring (`Spring.StiffnessHigh`), collapsing the foreground into the shadow.

### 5.2. `BrutalistButton`
Interactive full-width or inline button wrapping `BrutalistShadowBox`.
- Features 14dp vertical / 28dp horizontal padding, Bold 16sp text, and centered alignment.
- Meets accessibility standards with automatic touch-target handling.

### 5.3. `TopNavbar` & `BottomTrustBar`
- **`TopNavbar`:** 64dp height, White surface with a 2dp bottom border. Contains the bold "flipp" logotype, subtle category subtitle ("| file converter"), and a quick-action circular button navigating to the Privacy Commitment screen.
- **`BottomTrustBar`:** High-contrast footer combining a solid black section ("LOCAL ONLY • 100% Privacy-First" with mint accent and sky-blue lock badge) and a cream disclaimer bar ("NO UPLOADS • NO ACCOUNTS • WORKS OFFLINE").

### 5.4. `DropZone` & `SingleFileDropZone`
- 200dp height container with `#FAFAFA` background and dashed 2dp black border.
- Features prominent plus icon, drag/browse prompt text, accepted format subtitle ("Accepts .pdf or images"), and an embedded `BrutalistButton` triggering the system file picker.

### 5.5. `StageProgressBar`
- Encapsulates progress reporting during conversions.
- Displays stage label and percentage in bold 12sp text.
- Progress track: White capsule container with 2dp black border and `MintColor` animated progress fill.
- Employs `Modifier.progressSemantics(progress)` for screen-reader accessibility.

---

## 6. Accessibility (a11y) Standards & Rules

Flipp adheres to WCAG 2.1 AA accessibility guidelines, specifically tailored for Jetpack Compose as documented in `.Jules/palette.md`:

1. **48dp Minimum Touch Target:**
   - All icon-only actions (such as password visibility toggles, navigation arrows, or deletion icons) must be wrapped inside Material 3 `IconButton` rather than naked `Box.clickable`.
2. **Accessible Radio & Option Grids:**
   - Multi-option chips and radio selectors must use `Modifier.selectable(selected = isSelected, role = Role.RadioButton, onClick = ...)` instead of bare `.clickable` so TalkBack announces state changes ("selected" / "not selected").
3. **Container Accessibility Semantics:**
   - When large layout rows or cards act as navigation links (such as the header logo row or tool banner cards), attach `role = Role.Button` and a clear `onClickLabel` (e.g. `onClickLabel = "Go to home screen"`).
4. **Form Labels & Slider Semantics:**
   - Whenever custom visual labels appear outside of `OutlinedTextField` or `Slider` components, add `.semantics { contentDescription = "Field Name" }` so accessibility tools announce the purpose of the input.
5. **Dynamic Descriptions on Stateful Toggles:**
   - Trailing icons must reflect their current action in `contentDescription` (e.g. "Show password" vs. "Hide password", not a static "Lock icon").

---

## 7. Responsive Behavior & Screen Adaptability

- **Vertical Scrolling:** All tool screens wrap their main column with `.verticalScroll(rememberScrollState())` to prevent layout clipping on smaller screens or when virtual keyboards appear.
- **Dynamic Text Scaling:** Typography sizes are defined in `sp` and layout dimensions in `dp`, respecting user system font scaling settings up to 200%.
- **Edge-to-Edge Support:** The application calls `enableEdgeToEdge()` in `MainActivity.onCreate()` and handles system window insets via Compose's `Scaffold(innerPadding)`.
