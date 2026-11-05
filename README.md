# Aura Launcher

A calm, colourful, minimalist Android launcher built with Kotlin + Jetpack Compose.

## Features
- **Home**: serif clock + greeting, swipeable pages, auto-flowing app grid, folders, real Android widgets, glass dock
- **App drawer**: swipe up, A–Z section headers with fast-scroll rail, instant search, web-search fallback, hide apps
- **Long-press menus**: app shortcuts (when default launcher), add to home / dock, hide, app info, uninstall
- **Widgets**: any widget on the device (bind + configure flow), resize S/M/L/XL, reorder, remove
- **Icons**: default **Puffy** style: every icon becomes a glossy, inflated candy bubble tinted from its own colours (greys get a colour from the app name). Also Circle / Squircle / Rounded / Square / Original, size slider, ADW/Nova icon packs
- **Themes**: 8 colour palettes (3 pastel candy ones first: Bubblegum, Sorbet, Mint Cream), system / dark / light, animated Aurora wallpaper or your own system wallpaper
- **Notification dots** on icons (opt-in, uses notification access)
- **Gestures**: swipe up = drawer, swipe down = notification shade, long-press = home menu, Home button = back to page 1
- Work-profile apps, package install/uninstall live updates

## Build the APK

### Option A: Android Studio
Open this folder, let Gradle sync, press Run (or *Build > Build APK*).

### Option B: GitHub (no PC needed)
1. Create a GitHub repo and upload this whole folder (keep `.github/workflows/build.yml`).
2. Open the **Actions** tab, run **Build APK** (it also runs on every push).
3. Download `AuraLauncher-apk` from the finished run, unzip, install the `.apk`.

## First launch
Install the APK, press Home, choose **Aura** and **Always**. Or in Aura: Settings > Set Aura as default launcher.
For notification dots, switch them on in Settings and allow notification access.

Min Android 8.0 (API 26). Target/compile SDK 35.
