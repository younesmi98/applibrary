// Proxies documented Steam Web API calls so STEAM_API_KEY never ships in the APK.
// Set with: supabase secrets set STEAM_API_KEY=...   Requires a signed-in Supabase user (verify_jwt = true).
const KEY = Deno.env.get("STEAM_API_KEY")!;
const API = "https://api.steampowered.com";

Deno.serve(async (req) => {
  const steamid = new URL(req.url).searchParams.get("steamid") ?? "";
  if (!/^\d{17}$/.test(steamid)) return Response.json({ error: "bad_steamid" }, { status: 400 });

  const owned = await get(`${API}/IPlayerService/GetOwnedGames/v1/?key=${KEY}&steamid=${steamid}&include_appinfo=1&include_played_free_games=1&format=json`);
  if (owned.status === 429 || owned.status >= 500) return Response.json({ error: "steam_unavailable" }, { status: 503 });
  const games = (await owned.json())?.response?.games;

  // Steam returns an empty `response` when game details are private. Report it; never invent data.
  if (!games) return Response.json({ state: "private_or_empty", games: [] });

  const profile = await get(`${API}/ISteamUser/GetPlayerSummaries/v2/?key=${KEY}&steamids=${steamid}`);
  const player = (await profile.json())?.response?.players?.[0] ?? null;
  return Response.json({ state: "ok", player, games });
});

async function get(url: string, tries = 3): Promise<Response> {
  for (let i = 0; ; i++) {
    const r = await fetch(url);
    if (r.status !== 429 && r.status < 500 || i >= tries - 1) return r;
    await new Promise((s) => setTimeout(s, 500 * 2 ** i)); // exponential backoff
  }
}
