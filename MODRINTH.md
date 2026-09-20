# AegisAC

AegisAC is a client-side Minecraft anticheat that quietly watches your own combat for
**synthetic input** — clicks, hotbar switches, item use and keybind presses that came
from a hacked client or a mod instead of your hands.

There's nothing to look at and nothing to click. No HUD, no settings screen, no
keybinds, no chat spam. You drop it in your mods folder and forget it's there — it just
runs in the background.

## What it looks for

Whenever something suspicious happens, AegisAC traces where it came from — a loaded mod
on the call stack, an external tool, or a direct write it can't pin down — and notes it:

- **Synthetic attacks** — swings triggered by code rather than a real click.
- **Synthetic use-item** — right-click actions fired by code.
- **Synthetic hotbar switches** — slot changes with no real input behind them.
- **Synthetic keybind presses** — bound actions fired programmatically.

Around those flags it also keeps the combat context — jump-reset timing, combo
intervals, reach, aim placement, swings, shield breaks — so whoever reviews a report can
actually see the fight the flags happened in, not just a number.

## How reporting works

AegisAC starts recording the moment you join a server. When the session ends, it sends a
report **only if it caught synthetic input during that session** — clean sessions stay
on your machine and nothing is ever written to your disk. Reports show up on the
dashboard:

[https://aegisac.netlify.app/](https://aegisac.netlify.app/)

Keep in mind a flag isn't proof on its own. Plenty of allowed mods (Librarian trade
finders, Snappy Tappy, better/advanced block placement and the like) fire programmatic
input that trips the same detectors, which is why every flag on the dashboard says which
mod or tool it came from — so a reviewer can make the call in context.

And to be clear: AegisAC never touches movement, aim, reach, or combat mechanics. It
only watches.

## Install

It's client side only, so you don't need it on the server you're playing on.

You'll want **Java 21 or newer** and **Fabric Loader 0.17.0 or newer**
([grab it here](https://fabricmc.net/use/)).

Drop both of these into `.minecraft/mods/`:

- **AegisAC** — download the file that matches your Minecraft version
- [**Fabric API**](https://modrinth.com/mod/fabric-api) — required

Each build targets one exact Minecraft version, so pick the download that matches your
game. AegisAC hooks straight into the game's input and hotbar code, and those hooks
change shape between releases — so a mismatched build just fails at startup rather than
silently dropping a feature.

There's nothing to configure and nothing to switch on. The detection tuning is baked in,
so every copy measures the same way.

> On the pre-1.21.5 builds (1.21.1, 1.21.4) the hotbar-slot drift check is turned off,
> because the game API it leans on doesn't exist yet. Synthetic attack, use and keybind
> detection still work everywhere.
