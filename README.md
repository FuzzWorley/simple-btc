# Bitcoin Portfolio

A self-contained Android Bitcoin portfolio tracker with a security deception model.

## Overview

Shows real portfolio data only after correct PIN or biometric authentication. An incorrect PIN silently shows a randomly generated spoof amount indistinguishable in appearance from real data. No backend server — all data is fetched directly from free APIs on the user's device.

## Architecture

MVVM + Clean Architecture with Hilt DI.

```
UI Layer        → Jetpack Compose screens + ViewModels
Domain Layer    → UseCases + Repository interfaces + Domain models
Data Layer      → Repository implementations + Room cache + Retrofit APIs + EncryptedSharedPreferences
```

Single source of truth: Room is the only source data flows from. API results write to Room; UI observes Room via Flow.

## Security Model

- Real BTC amount stored in Android Keystore-backed EncryptedSharedPreferences
- PIN stored as salted SHA-256 hash
- Correct PIN or biometric → real BTC amount shown
- Wrong PIN → random 0.01–10.0 BTC shown (re-randomized each failure)
- No visual difference between authenticated and spoof modes
- `FLAG_SECURE` prevents screenshots and app-switcher previews
- No PIN lockout, no attempt counter (by design)

## API Sources

| Data | Source | Notes |
|---|---|---|
| BTC current price | CoinGecko (free) | 30 req/min limit, refresh every 60s |
| BTC price history | CoinGecko (free) | 1D–10Y ranges |
| Gold spot price (XAU) | metals.live | USD per troy oz |
| Gold price history | Stooq (CSV) | Daily close prices |

Stale threshold: current price > 5 min, history > 24 hr. On failure, serves cached Room data with amber stale banner.

## Building

**Prerequisites:** Android Studio Hedgehog or later, JDK 17

1. Clone the repo
2. Open the project root in Android Studio
3. Let Android Studio sync (downloads Gradle 8.7 + all dependencies)
4. Run on device or emulator (minSdk 26 / Android 8.0)

**From terminal** (requires Gradle wrapper jar — generated on first Android Studio sync):
```bash
./gradlew assembleDebug
```

## Running Tests

```bash
# Unit tests (JVM)
./gradlew test

# Instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Full check
./gradlew check
```

## Dependencies

| Library | License |
|---|---|
| Jetpack Compose | Apache 2.0 |
| Hilt | Apache 2.0 |
| Room | Apache 2.0 |
| Retrofit + OkHttp | Apache 2.0 |
| Vico Charts | Apache 2.0 |
| Kotlin Coroutines | Apache 2.0 |
| MockK | Apache 2.0 |
| Turbine | Apache 2.0 |
| JUnit 5 | EPL 2.0 |

## Known Limitations

- CoinGecko free tier: 30 requests/min (app respects this with 60s refresh intervals)
- Stooq may occasionally return empty data — app falls back to cache
- metals.live availability not guaranteed — app falls back to last cached XAU price
- Notifications: out of scope (v1)
- Multi-currency fiat (EUR, GBP, etc.): out of scope (v1) — USD + XAU only
