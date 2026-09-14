# Combat Monitor
**Combat Monitor** is a client side Minecraft PvP analysis mod that records your fights and turns them into detailed local reports. It is designed for players who want to review their own performance and understand how they fight over time.
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
When a recording ends, Combat Monitor saves an interactive HTML report and JSON data locally in your Minecraft Combat Tracker folder. The report opens on your computer and includes graphs, stat cards, and detailed combat data.

This sends the report to its website if synthetic input is detected, this does not need the player to record, but this not necessarily mean that that player is cheating as some mods like Librarian Trade finder, Snappy Tappy, Better place bind, Advance block placement or any mod like that can also trigger them (which are allowed on some servers).

[You can access the site by clicking here.](https://cheattracker.netlify.app/)

Combat Monitor does not change movement, aim, reach, or combat mechanics.

## Whats different?
This version track more metrics like Shield Breaking, Hit Types and Synthetic Input Detection.
