package org.me.velocitybalancer.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.me.velocitybalancer.shared.ConfigHelper;
import org.me.velocitybalancer.velocity.command.BalanceSendCommand;
import org.me.velocitybalancer.velocity.command.LobbyCommand;
import org.me.velocitybalancer.velocity.command.SendCommand;
import org.me.velocitybalancer.velocity.event.KickedFromServerEventHandler;
import org.me.velocitybalancer.velocity.event.ServerPreConnectEventHandler;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


@Plugin(id = "velocitybalancer", name = "VelocityBalancer", version = "1.0.0", description = "A plugin for balancing.",authors = {"kit8379"})
public class VelocityBalancer {

    private final ProxyServer proxy;
    private final Logger logger;
    private final ConfigHelper configHelper;
    private final Map<String, Boolean> serverStatus = new ConcurrentHashMap<>();

    @Inject
    public VelocityBalancer(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
        this.configHelper = new ConfigHelper(logger);
    }

    @Subscribe
    public void onProxyInitalize(ProxyInitializeEvent event) {
        logger.info("VelocityBalancer is starting up...");
        initialize();
        logger.info("VelocityBalancer has started successfully!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("VelocityBalancer is shutting down...");
        shutdown();
        logger.info("VelocityBalancer has shut down successfully!");
    }

    private void initialize() {
        configHelper.loadConfiguration();

        // Register commands
        proxy.getCommandManager().register("hub", new LobbyCommand(this, configHelper), "lobby");
        proxy.getCommandManager().register("send", new SendCommand(proxy, configHelper));
        proxy.getCommandManager().register("bsend", new BalanceSendCommand(this, proxy, configHelper));

        // Register events
        proxy.getEventManager().register(this, new KickedFromServerEventHandler(this, configHelper));
        proxy.getEventManager().register(this, new ServerPreConnectEventHandler(this, configHelper));

        // Offline server detection
        if (configHelper.isOfflineDetectionEnabled()) {
            long detectionInterval = configHelper.getDetectionInterval();
            proxy.getScheduler().buildTask(this, this::checkOfflineServers)
                    .repeat(detectionInterval, TimeUnit.SECONDS).schedule();
        }
    }

    private void shutdown() {
        // Nothing to do here
    }

    public void reload() {
        logger.info("VelocityBalancer is reloading...");
        shutdown();
        initialize();
        logger.info("VelocityBalancer has reloaded successfully!");
    }

    public RegisteredServer getBalancedServer(String groupName, Player player) {
        Map<Object, ? extends ConfigurationNode> balancingGroups = configHelper.getBalancingGroups();

        ConfigurationNode groupNode = balancingGroups.get(groupName);
        if (groupNode == null) {
            return null;
        }

        boolean isBalancing = groupNode.node("balancing").getBoolean();
        List<String> servers;
        try {
            servers = groupNode.node("servers").getList(String.class);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }

        if (!isBalancing) {
            return null;
        }

        // Process the permission-redirect configuration
        Map<String, String> permissionRedirects = configHelper.getPermissionRedirects(groupName);
        for (Map.Entry<String, String> entry : permissionRedirects.entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                return getBalancedServer(entry.getValue(), player);
            }
        }

        List<RegisteredServer> candidateServers = new ArrayList<>();
        if (servers != null) {
            for (String serverName : servers) {
                if (serverName.contains("*")) {
                    String pattern = serverName.replace(".", "\\.").replace("*", ".*");
                    for (RegisteredServer server : proxy.getAllServers()) {
                        if (server.getServerInfo().getName().matches(pattern)) {
                            candidateServers.add(server);
                        }
                    }
                } else {
                    proxy.getServer(serverName).ifPresent(candidateServers::add);
                }
            }
        }

        // Filter the list to only include online servers
        candidateServers.removeIf(server -> !serverStatus.getOrDefault(server.getServerInfo().getName(), false));

        // Find the server with the lowest player count
        return candidateServers.stream()
                .min(Comparator.comparingInt(server -> server.getPlayersConnected().size()))
                .orElse(null);
    }

    private void checkOfflineServers() {
        Map<Object, ? extends ConfigurationNode> balancingGroups = configHelper.getBalancingGroups();

        for (Map.Entry<Object, ? extends ConfigurationNode> groupEntry : balancingGroups.entrySet()) {
            List<String> servers;
            try {
                servers = groupEntry.getValue().node("servers").getList(String.class);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }

            if (servers != null) {
                for (String serverName : servers) {
                    Optional<RegisteredServer> serverOpt = proxy.getServer(serverName);
                    if (serverOpt.isPresent()) {
                        RegisteredServer server = serverOpt.get();
                        server.ping().whenComplete((ping, throwable) -> {
                            if (throwable == null) {
                                serverStatus.put(serverName, true);
                                logger.warning("Server " + serverName + " is online.");
                            } else {
                                serverStatus.put(serverName, false);
                                logger.warning("Server " + serverName + " is offline. Error: " + throwable.getMessage());
                            }
                        });
                    } else {
                        logger.warning("Server " + serverName + " not found.");
                    }
                }
            }
        }
    }
}