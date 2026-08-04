# PSA Legacy

A standalone Android app that talks directly to a PSA vehicle's head unit over
Bluetooth using the legacy **SMARTAPPS V1 ("Altran")** protocol — the same one the
old MyPeugeot app (1.3.1 / 1.16.6) used to read trips, car info, maintenance and
alert data.

No PSA cloud, no account, no network access of any kind. Your phone acts as the
server: the car connects to it, and everything stays on the device.

> **Why this exists:** MyPeugeot 1.33.0+ silently stopped working with older head
> units. Analysis of the decompiled APK shows the Bluetooth server layer was
> rewritten to accept connections on multiple UUIDs in parallel, and a failure to
> open the second (brand) UUID socket tears down *both* listeners — a regression
> for head units that only speak the legacy SMARTAPP UUID. PSA was also steering
> cars toward newer BTA2/CEA protocols that old head units can't handle. This app
> skips all of that by simply listening on the legacy UUID and speaking the
> Altran protocol itself, with no server in between.

## Features

- **Bluetooth server** (RFCOMM) — the car connects to the phone, exactly like the
  original app
- **Legacy SMARTAPP UUID** by default, plus optional **Peugeot brand** and **SPP**
  listeners (one independent accept socket per UUID — a failure on one never kills
  the others)
- **Trip history** — start/end time, mileage, GPS positions, fuel consumption,
  fuel level and autonomy, per trip
- **Maintenance info** — distance/days to next service, maintenance-passed flag
- **Alerts** — 256-bit vehicle alert mask decoded per trip, with a local
  IT/EN title catalog
- **VIN-based keying** — AES-128-ECB session keys derived from the vehicle VIN
- **Fully offline** — the manifest declares **no `INTERNET` permission**; trips are
  persisted as JSON in the app's private files directory
- **Raw protocol log** tab for debugging the wire traffic
- Three themes (dark brand, light Material 3, dark cluster)

## Compatible vehicles

Developed and verified on a **Peugeot 2008** (2016) with the SMARTAPPS V1
head unit.

At the protocol level the SMARTAPPS V1 ("Altran") protocol was shared across PSA
brands, so any **Peugeot / Citroën / DS** vehicle from the same head-unit era —
roughly the cars supported by **MyPeugeot ≤ 1.16.6** — should work, but **only
the 2008 is confirmed**. If your car's head unit can't connect, try the brand/SPP
UUID toggles and inspect the raw log.

**Not supported:** newer vehicles on BTA2/CEA protocol generations, vehicles that
require MyPeugeot 2.x, and anything needing PSA cloud services (remote lock,
location, etc.).

## How it works

```
phone (server)          car head unit (client)
    |  listen on UUIDs          |
    |<------ AuthRequest -------|   32-byte challenge
    |------- AuthResponse ----->|   encrypted challenge + 17-char VIN
    |--- ActivationRequest ---->|   sent 5 s after auth
    |<------ ActivationAck -----|   activation result + trip count
    |<------ TripData ×N -------|   one message per stored trip
    |------- TripDataAck ------>|   ack each trip
    |       disconnect           |
```

- The phone listens on the legacy SMARTAPP UUID `f7cc5d80-61eb-11e1-b86c-0800200c9a66`
  (plus optional brand/SPP sockets).
- Every wire frame is an AES-128-ECB encrypted payload with a length header and a
  checksum byte; session keys are derived from the VIN.
- Trips are decoded and **upserted by (VIN, trip number)** so re-syncs never
  duplicate entries.

## Getting started

1. **Pair** the phone and the car in Bluetooth settings.
2. Open the app → **Connect** tab → enter your **17-character VIN**
   (found on the windscreen or the registration document) → **Start listening**.
3. On the car's head unit, open the connected-apps entry
   (MyPeugeot / Connected Apps) to trigger the connection.
4. Trips show up in the **Trips** tab; fuel/maintenance/alerts on the **Car** tab.

> If nothing connects, toggle the **Peugeot brand** / **SPP** listener options and
> retry. The **Raw log** tab shows every frame sent and received, which helps
> diagnose which UUID your head unit expects.

## Building

Requirements: JDK 17+, Android SDK (platform 37).

```bash
# unit tests (protocol core)
./gradlew :app:testDebugUnitTest        # Windows: .\gradlew.bat ...

# debug APK
./gradlew :app:assembleDebug            # Windows: .\gradlew.bat ...
```

The protocol layer (`protocol/`) is pure Kotlin and covered by JVM unit tests:
framing, checksums, AES key derivation (known-answer vectors), message codecs,
trip decoding and the session state machine.

## Project layout

| Path | Responsibility |
|---|---|
| `protocol/` | Pure-Kotlin SMARTAPPS V1 core: frame/message codecs, AES-ECB VIN keying, trip/alert decoders, session state machine |
| `bluetooth/` | RFCOMM accept loops (one per UUID) + per-socket connection session |
| `service/` | Foreground service wiring server, sessions and storage together |
| `data/` | JSON persistence of trips, SharedPreferences settings |
| `ui/` | Compose screens: Connect, Trips, Car, Raw Log, Settings |

## Privacy

- No `INTERNET` permission — the app cannot send data anywhere.
- VIN and settings live in the app's private SharedPreferences; trips in the app's
  private files directory.
- The VIN is transmitted to the car over Bluetooth as part of the protocol's
  authentication exchange (this is how the original app worked too).

## Limitations

- Alert **titles** come from a local catalog extracted from the original app's
  string resources; unknown codes are shown as codes only.
- Trip data is pushed by the head unit after completed journeys — if the car has
  no stored trips it will report zero.
- Timestamps replicate the original app's timezone handling.

## Disclaimer

This project is **not affiliated with, endorsed by, or sponsored by Stellantis,
Peugeot or Citroën**. It is the result of reverse-engineering the SMARTAPPS V1
protocol from publicly available APKs for interoperability with the author's own
vehicle. All trademarks belong to their respective owners. Use at your own risk.

## License

MIT — see [LICENSE](LICENSE).
