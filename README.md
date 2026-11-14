# SunAreny

**English** · [Polski](README.pl.md)

Sign-up minigames for Paper 26.2: Spleef, TNT Run and King of the Hill. The plugin creates its own empty world, builds the arenas and rebuilds them after every round.

## Modes

- **Spleef**: multi-level snow arena; the outer ring of the top level collapses over time, and broken snow can drop snowballs that destroy the block they hit
- **TNT Run**: blocks vanish shortly after you step on them, with a few seconds of protection at the start
- **Król Wyspy** (King of the Hill): stand on the top to score points, knock others off, lose points when you fall

## How it works

- One game at a time: 60-second sign-up with a clickable chat announcement, countdown, game, results screen, then everyone goes back where they were
- The server opens sign-ups on its own every 20 minutes when at least 3 players are online, and players can open them from the `/arena` menu
- Inventory, XP and position are saved to disk before the game and restored afterwards; a corrupted save never wipes a player
- Rewards for places 1 to 3 and for taking part, with a daily limit
- Only whitelisted commands work during a game
- Per-player stats in `statystyki.yml`: wins, games played and win streak

## Requirements

- Paper **26.2**
- Java **25**
- [SunCore](https://github.com/oonnyyx/SunCore)

## Installation

1. Download `SunAreny.jar` and `SunCore.jar` from [Releases](../../releases/latest).
2. Drop both into `plugins/` and restart the server. The `areny` world and all three arenas are created automatically.

## Commands

Aliases: `/areny`, `/minigry`.

| Command | Description | Permission |
|---|---|---|
| `/arena` | Menu: modes, sign-ups, stats | `sunareny.menu` |
| `/arena dolacz` | Join the current sign-up | `sunareny.dolacz` |
| `/arena opusc` | Leave the arena | `sunareny.opusc` |
| `/arena statystyki [player]` | Show stats | `sunareny.statystyki` |
| `/arena start <mode>` | Open sign-ups for a mode | `sunareny.admin.start` |
| `/arena wymus` | Start now without waiting for sign-ups to end | `sunareny.admin.wymus` |
| `/arena stop` | Stop the current game | `sunareny.admin.stop` |
| `/arena odbuduj` | Rebuild all arenas | `sunareny.admin.odbuduj` |
| `/arena tp <mode>` | Teleport to an arena | `sunareny.admin.tp` |
| `/arena reload` | Reload the config | `sunareny.admin.reload` |

Player permissions are granted to everyone by default, admin ones to ops. `sunareny.*` grants all of them.

## Configuration

`config.yml` covers the world, sign-up times, player limits, the automatic schedule, rewards (console commands) and settings for each mode. Messages are in `wiadomosci.yml`, in Polish.

## Building

```bash
git clone https://github.com/oonnyyx/SunCore && cd SunCore && mvn install && cd ..
git clone https://github.com/oonnyyx/SunAreny && cd SunAreny && mvn package
```

## License

[MIT](LICENSE)
