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

    public void updateServerOnlineStatus(String serverName, boolean isOnline) {
        serverOnlineStatus.put(serverName, isOnline);
    }

    public RegisteredServer getBalancedServer(String groupName, Player player) {
        Map<Object, ? extends ConfigurationNode> balancingGroups = configHelper.getBalancingGroups();

        ConfigurationNode group = balancingGroups.get(groupName);
        if (group == null) {
            return null;
        }

        // Process the permission-redirect configuration
        Map<Object, ? extends ConfigurationNode> permissionRedirects = group.node("permission-redirect").childrenMap();
        for (Map.Entry<Object, ? extends ConfigurationNode> entry : permissionRedirects.entrySet()) {
            if (player.hasPermission(entry.getKey().toString())) {
                String targetGroup = entry.getValue().getString();
                return getBalancedServer(targetGroup, player);
            }
        }

        List<String> servers;
        try {
            servers = group.node("servers").getList(String.class, new ArrayList<>());
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
        List<RegisteredServer> candidateServers = new ArrayList<>();

        for (String serverName : servers) {
            if (serverName.contains("*")) {
                String pattern = serverName.replace(".", "\\.").replace("*", ".*");
                proxy.getAllServers().stream()
                        .filter(server -> server.getServerInfo().getName().matches(pattern))
                        .forEach(candidateServers::add);
            } else {
                proxy.getServer(serverName).ifPresent(candidateServers::add);
            }
        }

        // Filter the list to only include online servers
        candidateServers.removeIf(server -> !serverOnlineStatus.getOrDefault(server.getServerInfo().getName(), false));

        // Find the server with the lowest player count
        return candidateServers.stream()
                .min(Comparator.comparingInt(server -> server.getPlayersConnected().size()))
                .orElse(null);
    }
}
