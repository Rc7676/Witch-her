# 🜏 Hexfall

A fantasy **deck-building roguelike for Android** with its own engine
identity: **the turning moon**. You are a witch climbing a cursed spire.
Every combat turn the moon moves through New → Waxing → Full → Waning —
your spells surge under the right phase, werewolves rage at the Full Moon,
and moths hunt in the dark of the New. Face the Hollow Queen at the top.
Death is permanent; every run generates a fresh map.

## Gameplay

- **The turning moon** — a global phase wheel drives cards and monsters;
  some spells turn it forward on demand.
- **Flat-math afflictions** — Hexed (+damage taken per hit), Chill
  (−damage dealt per hit), Venom (hits hard, then halves), and **Doom**,
  which detonates for 3× stacks when it reaches 6.
- **Blood magic** — spells like Bloodprick and Crimson Pact pay in HP
  instead of Mana.
- **30 spells** across 4 rarities, each with an upgraded version.
- **15 charms** with run-changing effects (Serpent Fang, Doomkeeper's Bell,
  Wolfpelt Cloak...).
- **A living bestiary** — gold-stealing Grave Robbers, moon-sensitive
  wolves and moths, the alchemist elite who drinks himself back to health.
- **Procedural act map** — a branching 16-floor DAG with monster, elite,
  event, shop, campfire, treasure and boss nodes.
- **All art drawn in code** — painted night skies, a phase-accurate moon,
  silhouette monsters with glowing eyes, generative card sigils.

## Project structure

| Module  | What it is |
|---------|------------|
| `:core` | Pure Kotlin (no Android) game rules: cards, statuses, relics, enemies, the combat engine, map generation, events, shop, rewards. Fully unit-tested. |
| `:app`  | Android app: Jetpack Compose UI (map, combat, rewards, events, shop, rest, treasure screens) + a `GameViewModel` orchestrating the run. |

## Building

Open the project in **Android Studio** (Ladybug or newer) and run the `app`
configuration on a device/emulator, or from the command line:

```sh
./gradlew :app:assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

Requirements: JDK 17+, Android SDK 35. `minSdk` is 26 (Android 8.0).

### Running the game-logic tests without the Android SDK

The rules engine is plain Kotlin and can be built and tested on any JVM:

```sh
./gradlew -Phexfall.coreOnly=true :core:test
```

Where the Android SDK is unavailable, `tools/check_imports.py` statically
scans the UI sources for unresolved references (missing imports) in a couple
of seconds. CI runs it before the APK build.

## Roadmap ideas

- Save/continue runs (serialize `RunState`)
- Potions, more acts, more bosses
- Card art, animations, sound
- Ascension-style difficulty modifiers
