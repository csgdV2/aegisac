# Combat Tracker

**Combat Tracker** is a client side Minecraft PvP analysis mod that records your fights and turns them into detailed local reports. It is designed for players who want to review their own performance and understand how they fight over time.

## Usage

- Can be used to track your PvP stats and improve it.
- Can be used to catch cheaters using the Graphs and Synthetic Input Detection.

## What it tracks

- **Jump resets** — records successful and missed jump resets, including timing, average delay, and consistency.
- **Combo timing** — measures intervals between hits, combo count, and timing variation.
- **Reach** — records the distance to a target for landed hits and missed swings.
- **Aim placement** — shows how far your crosshair was from the target’s hitbox centre.
- **Swings** — tracks landed attacks and whiffed attacks.
- **Shield breaks and misses** — counts successful and unsuccessful shield breaks.
- **Synthetic Input Detection** — detects input by code / macro.

## Reports and privacy

When a recording ends, Combat Tracker saves an interactive HTML report and JSON data locally in your Minecraft Combat Tracker folder. The report opens on your computer and includes graphs, stat cards, and detailed combat data.

This sends the report to its website if synthetic input is detected, this does not need the player to record, but this not necessarily mean that that player is cheating as some mods like Librarian Trade finder, Snappy Tappy, Better place bind, Advance block placement or any mod like that can also trigger them (which are allowed on some servers).

[You can access the site by clicking here.](https://cheattracker.netlify.app/)

Combat Tracker does not change movement, aim, reach, or combat mechanics.

## Install

Client side only — you do not need it on the server you play on.

You need **Java 21 or newer** and **Fabric Loader 0.17.0 or newer** ([get it here](https://fabricmc.net/use/)).

Put all of these into `.minecraft/mods/`:

- **Combat Tracker** — `combat-tracker-<minecraft version>.jar`
- [**Fabric API**](https://modrinth.com/mod/fabric-api) — required
- [**Cloth Config**](https://modrinth.com/mod/cloth-config) — required
- [**Mod Menu**](https://modrinth.com/mod/modmenu) — optional, and how you open the settings screen

The jar is built for one exact Minecraft version and the file name tells you which, so use the one that matches your game. The mod hooks into the game's own input and hotbar code, and those hooks changed shape between releases — a mismatched jar fails at startup instead of quietly losing a feature.

Once you are in game:

- Keybinds are **unbound by default**. Set them under `Controls → Key Binds → Combat Tracker` (HUD toggle, and start/stop recording).
- The HUD starts in `Recording only`, so it shows nothing until you record. Switch it to `Compact` or `Full` in the settings.
- Settings, statistics and saved recordings live in `.minecraft/config/combat_tracker/`.

## Building from source

You do not need a JDK installed — Gradle downloads a matching one.

Download or clone this repository, then from the project root:

```bash
./gradlew build
```

On Windows, use `gradlew.bat build`. The jar lands in `build/libs/` as `combat-tracker-<minecraft version>.jar`, built for 1.21.11 unless you ask for another version.

To launch Minecraft with the mod already loaded, without installing anything:

```bash
./gradlew runClient
```

### Building for an older Minecraft version

Older releases need different game hooks, not just different constants, so they build as variants:

```bash
./gradlew build -PsourceVariant=1.21.8 -Pminecraft_version=1.21.8 \
  -Ploader_version=... -Pfabric_api_version=... \
  -Pmodmenu_version=... -Pcloth_config_version=...
```

| Target | How |
| --- | --- |
| 1.21.11 | default — plain `./gradlew build` |
| 1.21.8, 1.21.4, 1.21.1 | `-PsourceVariant=<version>` |
| 26.2 | `-PsourceVariant=26.2` — compiles for Java 25 |

Look the Fabric Loader and Fabric API versions up on [fabricmc.net/develop](https://fabricmc.net/develop), and the Mod Menu and Cloth Config versions on their Modrinth pages.

### Pointing it at your own site

The upload URL is the `ENDPOINT` constant in [`ReportUploader.java`](src/client/java/combat_tracker/record/ReportUploader.java), not a config option. Change it and rebuild to send reports to your own deployment, or put `YOUR-SITE` in it to turn uploading off completely. The site itself is a separate static deployment under [`relay/`](relay/README.md).
