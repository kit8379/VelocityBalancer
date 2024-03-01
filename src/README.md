# VelocityBalancer

VelocityBalancer is a plugin for Velocity, a Minecraft proxy server, that provides load balancing functionality across different servers. It allows you to distribute player connections evenly among servers in a group, ensuring a smoother gameplay experience.

## Features

- **Server Balancing:** Automatically balance players across servers in a group based on player count.
- **Permission-Based Redirection:** Redirect players to specific servers based on their permissions.
- **Offline Server Detection:** Detect and exclude offline servers from the balancing algorithm.
- **Dynamic Configuration:** Easily configure balancing groups and settings through a configuration file.
- **Commands:** Provides commands for players to connect to the hub/lobby, send players to specific servers, and balance-send players to the least populated server.

## Installation

1. Download the VelocityBalancer.jar file.
2. Place the jar file in the `plugins` directory of your Velocity proxy server.
3. Restart the Velocity proxy server to load the plugin.

## Configuration

The plugin's configuration can be found in the `plugins/VelocityBalancer/config.yml` file. Here you can define balancing groups, set the offline detection interval, and configure permission-based redirects.

Example configuration:

```yaml
lobbygroup: "lobby"
offlinedetection: true
detectioninterval: 10

balancing-groups:
  lobby:
    servers:
      - "lobby1"
      - "lobby2"
    balancing: true
    permission-redirect:
      "player.lobby": "lobby"

  authgroup:
    servers:
      - "auth1"
      - "auth2"
    balancing: true
    permission-redirect:
      "player.auth": "authgroup"

messages:
  no-permission: "&cYou don't have permission to use this command."
  send-usage: "&e/send <user> <server>"
  bsend-usage: "&e/bsend <user> <server>"
  server-not-found: "&cServer not found."
  player-not-found: "&cPlayer not found."
  reload: "&aConfiguration reloaded."
  player-only: "&cOnly players can use this command."
```

## Commands

  - /hub or /lobby: Sends the player to the hub/lobby server.
  - /send <player> <server>: Sends a specific player to a server.
  - /bsend <player> <group>: Sends a player to the least populated server in a group.
  - /vbreload: Reloads the plugin's configuration.

## Permissions

- velocitybalancer.hub: Allows use of the /hub or /lobby command.
- velocitybalancer.send: Allows use of the /send command.
- velocitybalancer.bsend: Allows use of the /bsend command.
- velocitybalancer.reload: Allows use of the /vbalancerreload command.

## Support

For support or feature requests, please open an issue on the GitHub repository.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or suggest features.

## License

VelocityBalancer is licensed under the MIT License.

## Authors

- kit8379