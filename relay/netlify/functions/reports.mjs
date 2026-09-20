import { getStore } from "@netlify/blobs";
import { isAdmin } from "../lib/auth.mjs";

export const config = { path: "/api/reports" };

export default async (req) => {
  const store = getStore({ name: "combat-tracker", consistency: "strong" });
  const index = (await store.get("index", { type: "json" })) || [];

  const q = (new URL(req.url).searchParams.get("q") || "").trim().toLowerCase();
  const list = Array.isArray(index) ? index.filter(Boolean) : [];
  const reports = q
    ? list.filter((e) => (e.player || "").toLowerCase().includes(q))
    : list;

  return new Response(JSON.stringify({ admin: isAdmin(req), reports }), {
    headers: {
      "content-type": "application/json",
      "cache-control": "no-store",
    },
  });
};
