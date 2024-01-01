package org.me.velocitybalancer.velocity.event;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.me.velocitybalancer.shared.ConfigHelper;
import org.me.velocitybalancer.velocity.VelocityBalancer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.List;
import java.util.Map;

public class ServerPreConnectEventHandler {

    private final VelocityBalancer plugin;
    private final ConfigHelper configHelper;

    public ServerPreConnectEventHandler(VelocityBalancer plugin, ConfigHelper configHelper) {
        this.plugin = plugin;
        this.configHelper = configHelper;
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) throws SerializationException {
        Player player = event.getPlayer();
        RegisteredServer targetServer = event.getOriginalServer();

        Map<Object, ? extends ConfigurationNode> balancingGroups = configHelper.getBalancingGroups();

        for (Object groupNameObj : balancingGroups.keySet()) {
            if (!(groupNameObj instanceof String)) continue;
            String groupName = (String) groupNameObj;

            ConfigurationNode groupNode = balancingGroups.get(groupName);
            if (groupNode == null) continue;

            boolean isBalancing = groupNode.node("balancing").getBoolean();
            List<String> servers;
            servers = groupNode.node("servers").getList(String.class);


            if (servers == null || !isBalancing) continue;

            if (servers.contains(targetServer.getServerInfo().getName())) {
                RegisteredServer balancedServer = plugin.getBalancedServer(groupName, player);
                if (balancedServer != null) {
                    event.setResult(ServerPreConnectEvent.ServerResult.allowed(balancedServer));
                    break;
                }
            }
        }
    }
}
