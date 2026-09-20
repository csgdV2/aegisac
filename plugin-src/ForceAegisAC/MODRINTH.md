# Force AegisAC

**Require the [AegisAC](https://modrinth.com/mod/aegisanticheat) client mod to play on your server.**
Players who join without it get kicked.

## Features

- **Hard requirement** — joining without AegisAC gets you kicked, cleanly.
- **Configurable** — set the grace period, customise the kick message (colour codes + line breaks), toggle console logging.
- **Bypass permission** — `forceaegisac.bypass` lets staff or trusted clients skip the check.
- **Lightweight** — no dependencies, no database, tiny jar. Stable APIs only.

## Compatibility

- **Minecraft 1.21.1 → latest** — one jar for all of it, no per-version downloads.
- **Paper** and its forks (Purpur, Pufferfish, Folia-family, etc.). Works on Spigot too, though kick-message formatting is nicer on Paper.

## Commands & permissions

| Command | Permission | Description |
| --- | --- | --- |
| `/forceaegisac reload` (`/faac`) | `forceaegisac.admin` (op) | Reload the config. |

| Permission | Default | Effect |
| --- | --- | --- |
| `forceaegisac.bypass` | `false` | Never kicked for missing the mod. |
| `forceaegisac.admin` | op | Allows `/forceaegisac reload`. |
