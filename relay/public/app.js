import { card } from "/report-card.js";

const grid = document.getElementById("grid");
const statusEl = document.getElementById("status");
const search = document.getElementById("search");
const moreWrap = document.querySelector(".more");
const loadMore = document.getElementById("loadMore");

const PAGE = 60;

let all = [];
let shown = [];
let page = 1;

async function load() {
  try {
    const res = await fetch("/api/reports");
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    all = Array.isArray(data.reports) ? data.reports : [];
    apply();
  } catch {
    statusEl.textContent = "Could not load reports.";
    statusEl.style.display = "";
  }
}

// A search shows every match at once so a player's full history is visible;
// browsing without a search pages through the list.
function apply() {
  const q = search.value.trim().toLowerCase();
  shown = q ? all.filter((r) => (r.player || "").toLowerCase().includes(q)) : all;
  page = 1;
  render();
}

function render() {
  const searching = search.value.trim() !== "";
  const limit = searching ? shown.length : page * PAGE;
  const slice = shown.slice(0, limit);

  grid.replaceChildren();
  if (!slice.length) {
    statusEl.textContent = all.length
      ? "No players match that search."
      : "No reports yet.";
    statusEl.style.display = "";
    moreWrap.hidden = true;
    return;
  }
  statusEl.style.display = "none";
  const frag = document.createDocumentFragment();
  for (const r of slice) frag.appendChild(card(r));
  grid.appendChild(frag);

  const remaining = shown.length - slice.length;
  moreWrap.hidden = remaining <= 0;
  if (remaining > 0) loadMore.textContent = `Load more (${remaining})`;
}

loadMore.addEventListener("click", () => {
  page += 1;
  render();
});

let debounce;
search.addEventListener("input", () => {
  clearTimeout(debounce);
  debounce = setTimeout(apply, 120);
});

load();
