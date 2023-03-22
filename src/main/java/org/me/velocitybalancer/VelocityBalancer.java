package org.me.velocitybalancer;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Plugin(id = "velocitybalancer", name = "VelocityBalancer", version = "1.0", description = "A plugin to balance player connections across server groups")
public class VelocityBalancer {

    private final ProxyServer proxy;
    private final Logger logger;
    private ConfigurationNode configRoot;
    private final Map<String, Boolean> serverStatus = new ConcurrentHashMap<>();


    @Inject
    public VelocityBalancer(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitalize(ProxyInitializeEvent event) {
        createConfigIfNotExists();

        // Register commands
        proxy.getCommandManager().register("hub", new LobbyCommand(), "lobby");
        proxy.getCommandManager().register("send", new SendCommand());
        proxy.getCommandManager().register("bsend", new BalanceSendCommand());

        // Offline server detection
        if (configRoot.node("offlinedetection").getBoolean()) {
            long detectionInterval = configRoot.node("detectioninterval").getInt();
            proxy.getScheduler().buildTask(this, this::checkOfflineServers)
                    .repeat(detectionInterval, TimeUnit.SECONDS).schedule();
        }
    }

    private void createConfigIfNotExists() {
        Path configPath = Paths.get("plugins", "velocitybalancer");
        Path configFile = configPath.resolve("config.yml");
        try {
            Files.createDirectories(configPath);
            if (Files.notExists(configFile)) {
                Files.createFile(configFile);
            }
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(configFile)
                    .nodeStyle(NodeStyle.BLOCK)
                    .build();
            configRoot = loader.load();
            if (configRoot.empty()) {
                configRoot.node("lobbygroup").set("authgroup");
                configRoot.node("offlinedetection").set(true);
                configRoot.node("detectioninterval").set(10);

                configRoot.node("balancing-groups", "authgroup", "servers").set(Arrays.asList("auth1", "auth2"));
                configRoot.node("balancing-groups", "authgroup", "balancing").set(true);
                configRoot.node("balancing-groups", "authgroup", "permission-redirect", "player.verifed").set("lobbygroup");
                configRoot.node("balancing-groups", "lobbygroup", "servers").set(Arrays.asList("lobby1", "lobby2"));
                configRoot.node("balancing-groups", "lobbygroup", "balancing").set(true);
                configRoot.node("balancing-groups", "lobbygroup", "permission-redirect").set(Collections.emptyMap());

                configRoot.node("messages", "no-permission").set("&cYou don't have permission to use this command");
                configRoot.node("messages", "send-usage").set("&e/send <user> <server>");
                configRoot.node("messages", "bsend-usage").set("&e/bsend <user> <server>");
                configRoot.node("messages", "server-not-found").set("&cServer not found");
                configRoot.node("messages", "player-not-found").set("&cPlayer not found");

                loader.save(configRoot);
            }
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.error("Error creating directories or config file", e);
        }
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        RegisteredServer targetServer = event.getOriginalServer();

        Map<String, Object> balancingGroups = configRoot.node("balancing-groups").childrenMap().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().raw()));

        for (String groupName : balancingGroups.keySet()) {
            Map<String, Object> group = (Map<String, Object>) balancingGroups.get(groupName);
            boolean isBalancing = (Boolean) group.get("balancing");

            List<String> servers = (List<String>) group.get("servers");

            if (isBalancing && servers.contains(targetServer.getServerInfo().getName())) {
                RegisteredServer balancedServer = getBalancedServer(groupName, player);
                if (balancedServer != null) {
                    event.setResult(ServerPreConnectEvent.ServerResult.allowed(balancedServer));
                    break;
                }
            }
        }
    }


    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        String lobbyGroup = configRoot.node("lobbygroup").getString();

        if (lobbyGroup != null) {
            RegisteredServer lobbyServer = getBalancedServer(lobbyGroup, player);
            if (lobbyServer != null) {
                player.createConnectionRequest(lobbyServer).fireAndForget();
            }
        }
    }

    private RegisteredServer getBalancedServer(String groupName, Player player) {

        Map<String, Object> balancingGroups = configRoot.node("balancing-groups").childrenMap().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().raw()));

        Map<String, Object> group = (Map<String, Object>) balancingGroups.get(groupName);

        if (group == null) {
            return null;
        }

        // Process the permission-redirect configuration
        Map<String, String> permissionRedirects = (Map<String, String>) group.get("permission-redirect");
        if (permissionRedirects != null) {
            for (String permission : permissionRedirects.keySet()) {
                if (player.hasPermission(permission)) {
                    String targetGroup = permissionRedirects.get(permission);
                    return getBalancedServer(targetGroup, player);
                }
            }
        }

        List<String> servers = (List<String>) group.get("servers");
        List<RegisteredServer> candidateServers = new ArrayList<>();

        for (String serverName : servers) {
            if (serverName.contains("*")) {
                String pattern = serverName.replace(".", "\\.").replace("*", ".*");
                for (RegisteredServer server : proxy.getAllServers()) {
                    if (server.getServerInfo().getName().matches(pattern)) {
                        candidateServers.add(server);
                    }
                }
            } else {
                Optional<RegisteredServer> server = proxy.getServer(serverName);
                server.ifPresent(candidateServers::add);
            }
        }

        // Filter the list to only include online servers
        candidateServers.removeIf(server -> !serverStatus.getOrDefault(server.getServerInfo().getName(), false));

        // Find the server with the lowest player count
        RegisteredServer bestServer = null;
        int lowestPlayerCount = Integer.MAX_VALUE;

        for (RegisteredServer server : candidateServers) {
            int currentPlayerCount = server.getPlayersConnected().size();
            if (currentPlayerCount < lowestPlayerCount) {
                lowestPlayerCount = currentPlayerCount;
                bestServer = server;
            }
        }

        return bestServer;
    }

    private void checkOfflineServers() {

        Map<String, Object> balancingGroups = configRoot.node("balancing-groups").childrenMap().entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().raw()));

        for (Map.Entry<String, Object> groupEntry : balancingGroups.entrySet()) {
            Map<String, Object> group = (Map<String, Object>) groupEntry.getValue();
            List<String> servers = (List<String>) group.get("servers");

            for (String serverName : servers) {
                Optional<RegisteredServer> serverOpt = proxy.getServer(serverName);
                if (serverOpt.isPresent()) {
                    RegisteredServer server = serverOpt.get();
                    server.ping().whenComplete((ping, throwable) -> {
                        if (throwable == null) {
                            serverStatus.put(serverName, true);
                            logger.debug("Server " + serverName + " is online.");
                        } else {
                            serverStatus.put(serverName, false);
                            logger.debug("Server " + serverName + " is offline. Error: " + throwable.getMessage());
                        }
                    });
                } else {
                    logger.debug("Server " + serverName + " not found.");
                }
            }
        }
    }

    private class LobbyCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            if (!(source instanceof Player)) {
                source.sendMessage(Component.text("This command can only be used by a player."));
                return;
            }

            Player player = (Player) source;
            String lobbyGroup = configRoot.node("lobbygroup").getString();

            if (lobbyGroup != null) {
                RegisteredServer lobbyServer = getBalancedServer(lobbyGroup, player);
                if (lobbyServer != null) {
                    player.createConnectionRequest(lobbyServer).fireAndForget();
                }
            }
        }
    }

    private class BalanceSendCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();

            Player player = (Player) source;

            if (!player.hasPermission("velocitybalancer.send")) {
                String noPermissionMessage = configRoot.node("messages", "no-permission").getString();
                assert noPermissionMessage != null;
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(noPermissionMessage));
                return;
            }

            if (args.length < 2) {
                String bsendUsageMessage = configRoot.node("messages", "bsend-usage").getString();
                assert bsendUsageMessage != null;
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(bsendUsageMessage));
                return;
            }

            if (args[0].equalsIgnoreCase("all")) {
                RegisteredServer server = proxy.getServer(args[1]).orElse(null);
                if (server == null) {
                    String serverNotFoundMessage = configRoot.node("messages", "server-not-found").getString();
                    assert serverNotFoundMessage != null;
                    source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(serverNotFoundMessage));
                    return;
                }
                for (Player p : proxy.getAllPlayers()) {
                    RegisteredServer bestserver = getBalancedServer(args[1], p);
                    if (bestserver != null) {
                        p.createConnectionRequest(bestserver).fireAndForget();
                    } else {
                        p.createConnectionRequest(server).fireAndForget();
                    }
                }
                return;
            }

            Player target = proxy.getPlayer(args[0]).orElse(null);
            if (target == null) {
                String playerNotFoundMessage = configRoot.node("messages", "player-not-found").getString();
                assert playerNotFoundMessage != null;
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(playerNotFoundMessage));
                return;
            }

            RegisteredServer bestServer = getBalancedServer(args[1], target);
            if (bestServer != null) {
                target.createConnectionRequest(bestServer).fireAndForget();
                return;
            }

            RegisteredServer server = proxy.getServer(args[1]).orElse(null);
            if (server != null) {
                target.createConnectionRequest(server).fireAndForget();
                return;
            }

            String serverNotFoundMessage = configRoot.node("messages", "server-not-found").getString();
            assert serverNotFoundMessage != null;
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(serverNotFoundMessage));
        }
    }

    private class SendCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments();

            Player player = (Player) source;

            if (!player.hasPermission("velocitybalancer.forcesend")) {
                String noPermissionMessage = configRoot.node("messages", "no-permission").getString();
                assert noPermissionMessage != null;
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(noPermissionMessage));
                return;
            }

            if (args.length < 2) {
                String sendUsageMessage = configRoot.node("messages", "send-usage").getString();
                assert sendUsageMessage != null;
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(sendUsageMessage));
                return;
            }

            if (args[0].equalsIgnoreCase("all")) {
                RegisteredServer server = proxy.getServer(args[1]).orElse(null);
                if (server == null) {
                    String serverNotFoundMessage = configRoot.node("messages", "server-not-found").getString();
                    assert serverNotFoundMessage != null;
                    source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(serverNotFoundMessage));
                    return;
                }
                for (Player p : proxy.getAllPlayers()) {
                    p.createConnectionRequest(server).fireAndForget();
                }
                return;
            }

            Player target = proxy.getPlayer(args[0]).orElse(null);
            if (target == null) {
                String playerNotFoundMessage = configRoot.node("messages", "player-not-found").getString();
                assert playerNotFoundMessage != null;
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(playerNotFoundMessage));
                return;
            }

            RegisteredServer server = proxy.getServer(args[1]).orElse(null);
            if (server != null) {
                target.createConnectionRequest(server).fireAndForget();
                return;
            }

            String serverNotFoundMessage = configRoot.node("messages", "server-not-found").getString();
            assert serverNotFoundMessage != null;
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(serverNotFoundMessage));
        }
    }
}
