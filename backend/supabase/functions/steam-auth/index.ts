// Steam OpenID callback. Validates the assertion with Steam itself, then hands the SteamID64 back to the app.
// Secrets: none needed here. The user's Steam password is never involved.
const STEAM = "https://steamcommunity.com/openid/login";
const CLAIMED = /^https:\/\/steamcommunity\.com\/openid\/id\/(\d{17})$/;

Deno.serve(async (req) => {
  const url = new URL(req.url);
  const p = url.searchParams;
  const state = p.get("state") ?? "";

  // 1. return_to must be this very function (stops assertions made for another site)
  const returnTo = p.get("openid.return_to") ?? "";
  if (!returnTo.startsWith(`${url.origin}${url.pathname}`) || p.get("openid.mode") !== "id_res") {
    return new Response("Invalid request", { status: 400 });
  }

  // 2. ask Steam whether it issued this assertion
  const body = new URLSearchParams();
  for (const [k, v] of p) if (k.startsWith("openid.")) body.set(k, v);
  body.set("openid.mode", "check_authentication");
  const res = await fetch(STEAM, { method: "POST", body });
  const text = await res.text();
  if (!res.ok || !/is_valid\s*:\s*true/.test(text)) return new Response("Steam rejected the sign-in", { status: 401 });

  // 3. extract SteamID64 from the claimed identity
  const m = CLAIMED.exec(p.get("openid.claimed_id") ?? "");
  if (!m) return new Response("Unexpected identity", { status: 400 });

  // 4. back to the app; the app checks `state` against the value it created
  return Response.redirect(`nexus://steam-connected?steamid=${m[1]}&state=${encodeURIComponent(state)}`, 302);
});
