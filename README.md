# Ingame

Plugin to used to manage competitive matches on bolt.rip.

Used alongside [Events](https://github.com/PGMDev/Events) which handles teams and matches.

### Permissions

 - `ingame.staff.*` - allows users to run `/ingame` staff commands
    - Sub permission nodes exist for individual commands for example `ingame.staff.poll`.

## Commands
### Staff Commands

| Command | Description |
| ------- | ----------- |
| `ingame match` | View info about the current Bolt match |
| `ingame poll` | Poll the API once for a new Bolt match |
| `ingame poll -r` | Poll the API until a Bolt match found |
| `ingame status` | View the status of the API polling |
| `ingame clear` | Clear the currently stored Bolt match |
| `ingame cancel` | Report the current Bolt match as cancelled |
| `ingame ban <player>` | Manually queue bans a player |

### Player Commands

| Command | Description |
| ------- | ----------- |
| `requeue` | Will call the requeue endpoint for the player |

### License
> AGPL-3.0
