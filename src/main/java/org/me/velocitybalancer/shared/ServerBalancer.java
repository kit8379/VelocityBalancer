package org.me.velocitybalancer.shared;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerBalancer {

    private final ProxyServer proxy;
    private final ConfigHelper configHelper;
    private final Map<String, Boolean> serverOnlineStatus = new ConcurrentHashMap<>();

    public ServerBalancer(ProxyServer proxy, ConfigHelper configHelper) {
        this.proxy = proxy;
        this.configHelper = configHelper;
    }

    public RegisteredServer getBalancedServer(String groupNameOrServer, Player player) {
        ConfigurationNode group = configHelper.getGroup(groupNameOrServer);
        if (group != null) {
            // Handle group balancing
            return handleGroupBalancing(group, player);
        } else {
            // Direct server connection
            return proxy.getServer(groupNameOrServer).orElse(null);
        }
    }

    private RegisteredServer handleGroupBalancing(ConfigurationNode group, Player player) {
        // Process permission-redirects
        Map<Object, ? extends ConfigurationNode> permissionRedirects = group.node("permission-redirect").childrenMap();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : permissionRedirects.entrySet()) {
            if (player.hasPermission(entry.getKey().toString())) {
                String targetGroup = entry.getValue().getString();
                ConfigurationNode targetGroupNode = configHelper.getGroup(targetGroup);
                return handleGroupBalancing(targetGroupNode, player);
            }
        }

        // Perform load balancing
        List<String> servers;
        try {
            servers = group.node("servers").getList(String.class, Collections.emptyList());
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
        List<RegisteredServer> candidateServers = new ArrayList<>();
        for (String serverName : servers) {
            proxy.getServer(serverName).ifPresent(server -> {
                if (serverOnlineStatus.getOrDefault(server.getServerInfo().getName(), false)) {
                    candidateServers.add(server);
                }
            });
        }

        // Find server with lowest player count
        return candidateServers.stream()
                .min(Comparator.comparingInt(server -> server.getPlayersConnected().size()))
                .orElse(null);
    }

    public void updateServerOnlineStatus(String serverName, boolean isOnline) {
        serverOnlineStatus.put(serverName, isOnline);
    }
}
