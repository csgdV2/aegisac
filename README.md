# AegisAC

AegisAC is a client-side Minecraft anticheat that watches your combat for
**synthetic input** like clicks, hotbar switches, item use and keybind presses that came
from a hacked client or a mod instead of your hands.

## What it looks for

Whenever something suspicious happens, AegisAC traces where it came from a loaded mod or an unattributable direct write and notes it:

- **Synthetic attacks** — swings triggered by code rather than a real click.
- **Synthetic use-item** — right-click actions fired by code.
- **Synthetic hotbar switches** — slot changes with no real input behind them.
- **Synthetic keybind presses** — bound actions fired programmatically.

Around those flags it also keeps the combat context — jump-reset timing, combo
intervals, reach, aim placement, swings, shield breaks.

## How reporting works

AegisAC starts recording the moment you join a server. When the session ends, it sends a
report **only if it caught synthetic input during that session**.
Reports show up on the dashboard:

[You can access it by clicking here.](https://aegisac.netlify.app/)

Keep in mind a flag isn't proof on its own. Plenty of allowed mods (Librarian trade
finders, Snappy Tappy, better/advanced block placement and the like) fire programmatic
input that trips the same detectors, which is why every flag on the dashboard says which
mod or tool it came from so a reviewer can make the call in context.

AegisAC never touches movement, aim, reach, or combat mechanics. It only watches.

## Requiring it on your server

If you run a Paper server and want to *force* everyone to have AegisAC, there's a
companion plugin: **ForceAegisAC**, [you can download it here](https://modrinth.com/plugin/force-aegisac).
Drop it in your `plugins/` folder and anyone who joins without the mod gets kicked.

## Install

It's client side only, so you don't need it on the server you're playing on.

You'll want **Java 21 or newer** and **Fabric Loader 0.17.0 or newer**
([grab it here](https://fabricmc.net/use/)).

Drop both of these into `.minecraft/mods/`:

- **AegisAC** — `aegisac-<minecraft version>.jar`
- [**Fabric API**](https://modrinth.com/mod/fabric-api) — required

## Building from source

No JDK needed — Gradle pulls down a matching one for you.

Clone (or download) the repo, then from the project root:

```bash
./gradlew build
```

On Windows that's `gradlew.bat build`. The jar lands in `build/libs/` as
`aegisac-<minecraft version>.jar`, built for 1.21.11 unless you ask for another version.

Want to try it without installing anything? Launch Minecraft with the mod already
loaded:

```bash
./gradlew runClient
```

### Building for an older Minecraft version

Older releases need different game hooks, not just tweaked constants, so they build as
separate variants:

```bash
./gradlew build -PsourceVariant=1.21.8 -Pminecraft_version=1.21.8 \
  -Ploader_version=... -Pfabric_api_version=...
```

| Target | How |
| --- | --- |
| 1.21.11 | default — just `./gradlew build` |
| 1.21.8, 1.21.4, 1.21.1 | `-PsourceVariant=<version>` |
| 26.2 | `-PsourceVariant=26.2` — compiles for Java 25 |

One caveat: on the pre-1.21.5 variants (1.21.1, 1.21.4) the hotbar-slot drift check is
turned off, because the game API it leans on doesn't exist yet. Synthetic attack, use and
keybind detection still work everywhere. You can look up the matching Fabric Loader and
Fabric API versions on [fabricmc.net/develop](https://fabricmc.net/develop).

### Pointing it at your own site

The upload URL lives in the `ENDPOINT` constant in
[`ReportUploader.java`](src/client/java/combat_tracker/record/ReportUploader.java) — it's
not a config option. Change it and rebuild to send reports to your own deployment, or
leave `YOUR-SITE` in it to turn uploading off entirely. The dashboard itself is a
separate static deployment over in [`relay/`](relay/README.md).
