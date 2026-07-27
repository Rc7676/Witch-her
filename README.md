# 🜏 Hexfall

A fantasy **deck-building roguelike for Android**, in the spirit of
*Slay the Spire* and *Roguebook*. You are a witch climbing a cursed spire:
fight monsters with a deck of spells, collect cards and relics, brave events,
shops and campfires — and face the Pale Lich at the top. Death is permanent;
every run generates a fresh map.

## Gameplay

- **Turn-based card combat** — 3 energy per turn, draw 5, attack/skill/power
  cards, enemy intents shown ahead of time.
- **Statuses** — Strength, Dexterity, Weak, Vulnerable, Frail, Poison, Regen,
  Thorns, and more.
- **30+ cards** across 4 rarities, each with an upgraded version.
- **14 relics** with passive run-changing effects.
- **Procedural act map** — a branching 16-floor DAG with monster, elite,
  event, shop, campfire, treasure and boss nodes.
- **Events, shops, campfires** — heal or upgrade at fires, buy cards/relics
  and remove cards at shops, gamble at story events.

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

## Roadmap ideas

- Save/continue runs (serialize `RunState`)
- Potions, more acts, more bosses
- Card art, animations, sound
- Ascension-style difficulty modifiers
