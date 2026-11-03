# Mira-FP

Administrator-controlled simulated actors for the Mira Paper server suite.

Mira-FP is intended to provide configurable fake player profiles, in-world models, tab-list representation, equipment, ranks, tags, virtual inventories, chat, movement and approved integrations. This is a **hybrid** design: presentation and scripted behaviours are separated from server-side plugin interactions.

## Current Release

**v0.1.0-dev** — separate development builds for **Paper 26.2 (Java 25)** and **Paper 1.18.2 (Java 17)**. **Development previews, not feature-complete.**

[View releases](https://github.com/FiveSOCE/Mira-FP/releases)

## JAR Downloads

| Minecraft server | Java | JAR download | Source branch |
| --- | --- | --- | --- |
| Paper 26.2 | 25 | [Download Mira-FP-26.2-0.1.0-dev.jar](https://github.com/FiveSOCE/Mira-FP/releases/download/dev-26.2/Mira-FP-26.2-0.1.0-dev.jar) | [feature/hybrid-26.2](https://github.com/FiveSOCE/Mira-FP/tree/feature/hybrid-26.2) |
| Paper 1.18.2 | 17 | [Download Mira-FP-1.18.2-0.1.0-dev.jar](https://github.com/FiveSOCE/Mira-FP/releases/download/dev-1.18.2/Mira-FP-1.18.2-0.1.0-dev.jar) | [compat/1.18.2](https://github.com/FiveSOCE/Mira-FP/tree/compat/1.18.2) |

Both development JARs passed GitHub Actions compilation and have verified direct GitHub release asset links. The 1.18.2 JAR was validated as Java 17 bytecode; in-game runtime testing remains outstanding. Use the JAR matching your server version. Both are early development builds with armor-stand stand-ins, not genuine player-model/tab-list bots.

## Requirements

- Paper 26.2 with Java 25, or Paper 1.18.2 with Java 17
- Use the branch-specific JAR
- No MiraCore dependency
- No required third-party NPC plugin

## Currently Implemented

- Permission-gated `/fp` administration GUI and commands
- Persisted simulated actor profiles and spawn positions in `actors.yml`
- Visible **armor-stand stand-in entities**, displayed names, and basic equipment
- Idle/wander behaviour with rectangular block safety checks and a configurable radius
- Configurable rank/tag **display text** (not LuckPerms or MiraTags accounts)
- Admin-triggered chat messages explicitly marked **[Simulated]**
- Console actions referenced by allowlisted IDs in `config.yml` (admin only)
- Recovery of actor profiles on normal server restart

## Not Yet Implemented

- Genuine player-shaped character models, player skins and tab-list entries
- Full pathfinding, realistic player animation and advanced region selection
- ItemStack-rich persistent inventory and custom-item metadata
- LuckPerms, MiraTags, MiraTab, MiraChats or MiraFactions adapters
- Automated scripted behaviours or a general plugin action API
- Full GUI profile editor and runtime/integration tests

These items must not be advertised as working.

## Commands

| Command | Permission | Purpose |
| --- | --- | --- |
| `/fp` | `mirafp.admin` | Open admin GUI |
| `/fp help` | `mirafp.admin` | Display commands |
| `/fp create <name>` | `mirafp.admin` | Create actor at admin location |
| `/fp list` | `mirafp.admin` | List actor profiles |
| `/fp remove <name>` | `mirafp.admin` | Delete actor profile |
| `/fp spawn <name>` | `mirafp.admin` | Spawn stand-in |
| `/fp despawn <name>` | `mirafp.admin` | Remove world stand-in without deleting profile |
| `/fp tp <name>` | `mirafp.admin` | Move actor anchor to admin |
| `/fp mode <name> idle\|wander` | `mirafp.admin` | Change movement mode |
| `/fp radius <name> <blocks>` | `mirafp.admin` | Change movement radius (0-128) |
| `/fp gear <name> <slot> <material>` | `mirafp.admin` | Set equipment material |
| `/fp rank <name> <text>` | `mirafp.admin` | Set display prefix |
| `/fp tag <name> <text>` | `mirafp.admin` | Set display suffix |
| `/fp chat <name> <message>` | `mirafp.admin` | Broadcast clearly simulated chat |
| `/fp run <name> <action-id>` | `mirafp.admin` | Invoke administrator-configured console command |
| `/fp reload` | `mirafp.admin` | Reload config |

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `mirafp.admin` | Operators | All management and scripting commands |

## Data Files

```text
config.yml - movement, chat and allowlisted actions
actors.yml - stored profiles, positions, display text and equipment
```

**Security:** `actions` values execute with console permissions. Only trusted server owners should edit `config.yml`; do not configure arbitrary player-provided commands. This is not equivalent to a simulated `Player` executing another plugin's commands.

## MiraCore Integration

Standalone by default. Optional integrations are planned, not yet implemented.

## Building

```bash
gradle clean build
```

JAR output: `build/libs/Mira-FP-26.2-0.1.0-dev.jar` or `build/libs/Mira-FP-1.18.2-0.1.0-dev.jar`, depending on branch.

## License

Copyright © 2026 FiveSOCE. **All Rights Reserved.** See [LICENSE](LICENSE).
