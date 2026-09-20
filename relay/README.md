# AegisAC — Reports dashboard

A tiny Netlify site that receives AegisAC session reports. The mod uploads a report
when a session ends — every session, so the dashboard has full combat stats — and each
card flags whether **synthetic input** was detected. Nothing is shown to the player
in-game.

**Browsing is public; deleting is not.** Anyone with the URL can open `/`, read the
cards and open any report — there is no login, no sign-out button and nothing that
says who you are. Deleting lives at **`/admin`**, which nothing links to: you type the
URL, and it asks for the admin password.

- **Dashboard** (`/`) — recent sessions, each with an avatar, server, an
  **account-type** badge (premium / cracked, detected client-side by the mod), any
  detected input types with per-session counts, and **where each flag came from** —
  the mod that did it, or that it came from outside the game. A **search box in the
  top-right** filters by player name.
- **Report view** (`/report/:id`) — the exact self-contained HTML report the mod
  generated (charts and all).
- **Admin panel** (`/admin`) — password gate, then one flat grid of every report,
  newest first — the same cards as the dashboard plus a checkbox and a **Delete**
  button. A toolbar above the grid holds **Select all**, the selection count and
  **Delete selected (n)**. Every delete opens an in-page confirmation naming exactly
  what goes; deleting a single card from a player who has several offers **All N
  reports from &lt;player&gt;** in the same dialog, which is how one player's whole
  history goes in a single call. Sign-out returns to `/`.

## How it fits together

```
mod  ──POST /api/report──▶  report.mjs  ──▶  Netlify Blobs   (open; no login)
                                              ├─ report/<id>   (HTML)
                                              ├─ canonical/<id>(hashed JSON)
                                              └─ index         (metadata, newest first)

browser ──GET /api/reports──────▶ reports.mjs     ──▶ index → cards        (public)
browser ──GET /report/:id ──────▶ report-view.mjs ──▶ report/<id>          (public)

browser ──POST /api/login ──────▶ login.mjs       ──▶ Set-Cookie: ct_admin (signed, HttpOnly)
browser ──POST /api/delete ─────▶ delete.mjs      ──▶ one report           (admin only)
browser ──POST /api/delete-bulk─▶ delete-bulk.mjs ──▶ many, one index write(admin only)
browser ──POST /api/delete-user─▶ delete-user.mjs ──▶ everything for a player (admin only)
```

Storage is [Netlify Blobs](https://docs.netlify.com/blobs/overview/) — no database
and no persistent server to run. The `index` blob is a read-modify-write list capped
at 500 entries; for a low-volume personal dashboard that is plenty. Reports upsert by
`sessionId`, so an idempotent re-send updates the same entry instead of duplicating.

## Where a flag came from

Each flag the mod records carries an origin, and the card shows one chip per source:

| Chip                          | Means                                                     |
| ----------------------------- | --------------------------------------------------------- |
| `Turbo Clicker ×12` (blue)    | a loaded mod was on the call stack when the action happened |
| `External · AutoHotkey ×7`    | neither a mod nor the game's own code was responsible      |
| `Unattributed ×6` (grey)      | flagged, but nothing identifiable to attribute it to       |

Hovering a chip shows which detections it accounts for, the mod version, and the exact
call site. Reports uploaded before this existed simply have no chips.

Two things this is **not**. A mod named on a chip is a mod that was on the stack —
an auto-tool, a keybind helper and a killaura are indistinguishable from there. And
the software named next to `External` is a guess from the process list (macro tools and
peripheral suites the mod knows by name), so it says what was *running*, not what did
anything. Only names the mod recognises are ever sent; the process list itself is not.

`report.mjs` treats every field as untrusted text and clamps it: 24 rows per report,
`modId`/`modName`/suspect names 64 characters, `modVersion` 32, `detail` 160, 6
suspects. Rows arrive as:

```json
{ "kind": "attack", "type": "MOD", "modId": "turboclicker",
  "modName": "Turbo Clicker", "modVersion": "2.4.1",
  "detail": "mixin handler in net.minecraft.client.Minecraft.handler$…",
  "count": 9, "suspects": [] }
```

`kind` is one of `hotbar` / `use` / `attack` / `keybind` and `type` one of `MOD` /
`EXTERNAL` / `UNKNOWN`; anything else is dropped or falls back to `UNKNOWN`. The
mod-side half is `FlagOrigin`, `SourceAttribution` and `ExternalSuspects` in
`src/client/java/combat_tracker/detection/`.

## Access & the admin password

There is **one** password and it guards deleting only. `/admin` shows a gate; a correct
password is exchanged at `/api/login` for a signed, `HttpOnly` cookie (`ct_admin`), and
each of `/api/delete`, `/api/delete-bulk` and `/api/delete-user` verifies that cookie
itself. Hiding a button is not the gate — a hand-rolled `POST` gets a `401` all the
same. The panel's "am I admin" flag from `/api/reports` only decides what to draw.

Default password, to override in production: `thispasswordisverysecure`

Set these in the Netlify UI (Site configuration → Environment variables):

```
ADMIN_PASSWORD    the delete password
AUTH_SECRET       HMAC key that signs the admin cookie (any long random string)
```

If `AUTH_SECRET` is unset, the signing key is derived from `ADMIN_PASSWORD`, so cookies
stay unforgeable as long as the password is secret — but if this repo is public, anyone
can read the default, so change it before relying on it. Changing either value
invalidates existing sessions. A session lasts **one day**, then the gate applies
again. (An earlier version had a second "viewer" password for browsing; that role is
gone, and `VIEWER_PASSWORD` is now ignored.)

**Making the dashboard public makes the reports public.** Player names, servers,
account-type badges and the full HTML reports are readable by anyone who has the URL,
including search engines unless you tell them otherwise; `/admin` sends
`noindex, nofollow`, but `/` does not. Only deleting is protected. If that is not what
you want, put Netlify's own access control in front of the site.

## Deploy

1. Install the Netlify CLI and log in:
   ```
   npm i -g netlify-cli
   netlify login
   ```
2. From this `relay/` folder, create the site and install deps:
   ```
   npm install
   netlify init      # or: netlify sites:create, then link
   ```
3. Deploy:
   ```
   netlify deploy --build --prod
   ```
   Blobs are enabled automatically for the site — nothing else to configure.
4. Copy the site URL (e.g. `https://your-site.netlify.app`).

## Point the mod at it

The dashboard URL is baked into the mod's source (not `config.json`), so it never
appears in the player's config file. It ships pointing at `https://aegisac.netlify.app`.
To send reports to your own deployment, edit the `ENDPOINT` constant in
`src/client/java/combat_tracker/record/ReportUploader.java`:

```java
private static final String ENDPOINT = "https://your-site.netlify.app/api/report";
```

then rebuild the mod (`./gradlew build`). While the constant still contains
`YOUR-SITE` the upload is skipped, so set it to your real site before expecting
reports to arrive.

## Run locally

```
npm install
netlify dev
```

`netlify dev` emulates the Functions runtime **and** Blobs locally. Then, in another
terminal:

```
curl -X POST http://localhost:8888/api/report \
     -H "content-type: application/json" \
     --data @sample.json
```

Load `http://localhost:8888/` — it opens straight into the dashboard. Confirm the
**SampleUser** card shows `Attack ×3` / `Keybind ×5` chips and a second row naming
`Turbo Clicker` and `External`, try the top-right search, and click **Open report**.
Then go to `http://localhost:8888/admin`, enter `thispasswordisverysecure`, and check
that a card can be deleted from its own **Delete** button, from **Delete selected (n)**
after ticking it, and — for a player with more than one report — through the **All N
reports from &lt;player&gt;** option in the confirmation dialog.

## Notes

- Netlify's synchronous request limit is ~6 MB, so `report.mjs` rejects an HTML body
  over 5 MB (`413`). Session reports are normally short and well under this.
- Player-supplied text (name, server, mod names, call sites) is only ever inserted with
  `textContent`, and `id`/`uuid` are constrained to `[A-Za-z0-9._-]` before they touch a
  blob key or an avatar URL.
- `POST /api/report` is intentionally open so the mod can upload without logging in.
  `sessionId` is derived from the player UUID + start time, so the worst an open ingest
  allows is spoofed or duplicate entries — which an admin can delete.
- `/report/:id` is served with `public, max-age=60`, so a report can still be fetched
  from a CDN edge for up to a minute after it is deleted. `/api/reports` is `no-store`,
  so the card list itself is always current.
- The admin cookie is `HttpOnly` + `SameSite=Lax` + `Secure` and `Max-Age`-capped to one
  day. Failed logins get a fixed delay to blunt brute-forcing; for a public deployment
  you may still want Netlify's own access control or rate limiting in front.
- `/api/delete-bulk` accepts up to 500 ids and rewrites the index once, and
  `/api/delete-user` matches on UUID when the reports carry one and on the exact name
  when they do not — the same identity the panel uses to decide which other reports
  belong to a player.
