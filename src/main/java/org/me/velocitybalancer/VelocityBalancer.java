package org.me.velocitybalancer;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Plugin(id = "velocitybalancer", name = "VelocityBalancer", version = "1.0", description = "A plugin to balance player connections across server groups")
public class VelocityBalancer {

    private final ProxyServer proxy;
    private final Logger logger;
    private Config config;
    private final Map<String, Boolean> serverStatus = new ConcurrentHashMap<>();


    @Inject
    public VelocityBalancer(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        config = new Config(logger);
        logger.info("Loaded config data: " + config.get("balancing-groups"));

        // Register commands
        proxy.getCommandManager().register("vbreload", new ReloadCommand());
        proxy.getCommandManager().register("hub", new LobbyCommand(), "lobby");

        // Offline server detection
        if ((Boolean) config.get("offlinedetection")) {
            long detectionInterval = (Integer) config.get("detectioninterval");
            proxy.getScheduler().buildTask(this, this::checkOfflineServers)
                    .repeat(detectionInterval, TimeUnit.SECONDS).schedule();
        }
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        String lobbyGroup = (String) config.get("lobbygroup");
        boolean forceLobbyGroup = (Boolean) config.get("force-lobby-group");

        if (forceLobbyGroup && lobbyGroup != null) {
            RegisteredServer lobbyServer = getBalancedServer(lobbyGroup, player);
            if (lobbyServer != null) {
                player.createConnectionRequest(lobbyServer).fireAndForget();
                return;
            }
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> balancingGroups = (Map<String, Object>) config.get("balancing-groups");
        for (String groupName : balancingGroups.keySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> group = (Map<String, Object>) balancingGroups.get(groupName);
            boolean isBalancing = (Boolean) group.get("balancing");
            if (isBalancing) {
                RegisteredServer balancedServer = getBalancedServer(groupName, player);
                if (balancedServer != null) {
                    player.createConnectionRequest(balancedServer).fireAndForget();
                    break;
                }
            }
        }
    }


    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        String lobbyGroup = (String) config.get("lobbygroup");

        if (lobbyGroup != null) {
            RegisteredServer lobbyServer = getBalancedServer(lobbyGroup, player);
            if (lobbyServer != null) {
                player.createConnectionRequest(lobbyServer).fireAndForget();
            }
        }
    }

    private RegisteredServer getBalancedServer(String groupName, Player player) {
        @SuppressWarnings("unchecked")
        Map<String, Object> balancingGroups = (Map<String, Object>) config.get("balancing-groups");
        @SuppressWarnings("unchecked")
        Map<String, Object> group = (Map<String, Object>) balancingGroups.get(groupName);

        if (group == null) {
            return null;
        }

        // Process the permission-redirect configuration
        @SuppressWarnings("unchecked")
        Map<String, String> permissionRedirects = (Map<String, String>) group.get("permission-redirect");
        if (permissionRedirects != null) {
            for (String permission : permissionRedirects.keySet()) {
                if (player.hasPermission(permission)) {
                    String targetGroup = permissionRedirects.get(permission);
                    return getBalancedServer(targetGroup, player);
                }
            }
        }

        @SuppressWarnings("unchecked")
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
        @SuppressWarnings("unchecked")
        Map<String, Object> balancingGroups = (Map<String, Object>) config.get("balancing-groups");

        for (Map.Entry<String, Object> groupEntry : balancingGroups.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> group = (Map<String, Object>) groupEntry.getValue();
            @SuppressWarnings("unchecked")
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


    private class ReloadCommand implements SimpleCommand {
        @Override
        public void execute(@NonNull Invocation invocation) {
            config.load();
            invocation.source().sendMessage(Component.text("VelocityBalancer config reloaded."));
        }
    }


    private class LobbyCommand implements SimpleCommand {
        @Override
        public void execute(@NonNull Invocation invocation) {
            CommandSource source = invocation.source();
            if (!(source instanceof Player)) {
                source.sendMessage(Component.text("This command can only be used by a player."));
                return;
            }

            Player player = (Player) source;
            String lobbyGroup = (String) config.get("lobbygroup");

            if (lobbyGroup != null) {
                RegisteredServer lobbyServer = getBalancedServer(lobbyGroup, player);
                if (lobbyServer != null) {
                    player.createConnectionRequest(lobbyServer).fireAndForget();
                }
            }
        }
    }
}
