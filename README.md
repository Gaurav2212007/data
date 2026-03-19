# 💧 Data Bottle — Android Overlay App

A floating animated water bottle that sits in the **top-right corner of your screen**
and shows your real mobile data usage in real time. Water level syncs with your actual
remaining mobile data. Color shifts green → yellow → red as data runs low.

---

## Features

- 🍶 **Animated glass bottle** with sloshing waves and rising bubbles
- 📶 **Real mobile data sync** using Android's NetworkStatsManager
- 🎨 **Color-coded**: Green (plenty) → Yellow (getting low) → Red (almost gone)
- 👆 **Tap** the bottle to see full breakdown (used / remaining / total)
- ↔️ **Drag** to reposition anywhere on screen
- 🔄 **Auto-refreshes** every 30 seconds
- 🔁 **Restarts after reboot** automatically
- 🌙 Works on top of all apps (games, videos, browser — everything)

---

## How to Build & Install

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- A physical Android device (API 26+, Android 8.0+)
  - Overlay widgets don't work in emulators properly

### Steps

1. **Open in Android Studio**
   ```
   File → Open → select the DataBottleApp folder
   ```

2. **Sync Gradle**
   Android Studio will prompt you — click "Sync Now"

3. **Connect your phone**
   - Enable Developer Options → USB Debugging on your device
   - Connect via USB

4. **Run the app**
   - Click the green ▶ Run button
   - Select your device

---

## First-Time Setup (on your phone)

After installing, the app shows a setup screen. Follow these steps:

### Step 1 — Grant "Draw Over Apps"
Tap **"1. Grant Draw Over Apps"** → Find "Data Bottle" in the list → Enable it → Go back

### Step 2 — Grant "Usage Access"
Tap **"2. Grant Usage Access"** → Find "Data Bottle" → Enable it → Go back

### Step 3 — Enter your data plan
Type your monthly data plan size in GB (e.g. `5` for a 5 GB plan)

### Step 4 — Start!
Tap **"▶ Start Bottle Overlay"**

The bottle will appear in the top-right corner of your screen immediately! 🎉

---

## Permissions Explained

| Permission | Why it's needed |
|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the floating bottle on top of other apps |
| `PACKAGE_USAGE_STATS` | Read your real mobile data usage from Android system |
| `FOREGROUND_SERVICE` | Keep the bottle running as a persistent service |
| `RECEIVE_BOOT_COMPLETED` | Auto-start bottle when phone restarts |
| `READ_PHONE_STATE` | Read subscriber ID for accurate per-SIM data stats |

---

## Project Structure

```
DataBottleApp/
├── app/src/main/
│   ├── java/com/databottle/
│   │   ├── MainActivity.java        ← Setup screen & permission flow
│   │   ├── OverlayService.java      ← Foreground service, draws the overlay
│   │   ├── BottleView.java          ← Custom animated bottle canvas view
│   │   ├── DataUsageHelper.java     ← Reads real mobile data from system
│   │   └── BootReceiver.java        ← Auto-start on device boot
│   ├── res/
│   │   ├── layout/
│   │   │   ├── activity_main.xml    ← Setup UI
│   │   │   └── overlay_bottle.xml  ← Floating widget layout
│   │   ├── drawable/                ← Backgrounds, icons
│   │   └── values/                  ← Colors, strings, themes
│   └── AndroidManifest.xml
└── README.md
```

---

## Customisation

### Change refresh interval
In `OverlayService.java`, change:
```java
private static final long REFRESH_MS = 30_000L; // 30 seconds
```
e.g. `10_000L` for every 10 seconds (uses more battery).

### Change billing cycle start date
In `DataUsageHelper.java`, find:
```java
cal.set(Calendar.DAY_OF_MONTH, 1);
```
Change `1` to your actual billing cycle start date (e.g. `15` if cycle starts on the 15th).

### Change bottle position
In `OverlayService.java`:
```java
params.gravity = Gravity.TOP | Gravity.END;  // top-right
params.x = dpToPx(8);   // right margin
params.y = dpToPx(60);  // top margin (below status bar)
```

---

## Troubleshooting

**Bottle shows "??" for percentage**
→ Usage Access permission not granted. Open the app and tap Step 2.

**Bottle disappears after reboot**
→ Make sure the app is not in your phone's battery saver "restricted" list.
→ Go to Settings → Battery → Data Bottle → Set to "Unrestricted".

**Data percentage seems wrong**
→ Make sure the plan GB you entered matches your actual plan.
→ Some carriers reset data mid-month — adjust the billing cycle start date in `DataUsageHelper.java`.

**Build fails with "Manifest merger failed"**
→ Make sure you have the latest version of Android Studio and all SDK tools installed.

---

## Minimum Requirements

- Android 8.0 (API 26) or higher
- Any Android phone with mobile data

---

*Built with ❤️ — a living data monitor right on your screen.*
