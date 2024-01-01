package org.me.velocitybalancer.velocity.event;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.me.velocitybalancer.shared.ConfigHelper;
import org.me.velocitybalancer.velocity.VelocityBalancer;

public class KickedFromServerEventHandler {

    private final VelocityBalancer plugin;
    private final ConfigHelper configHelper;

    public KickedFromServerEventHandler(VelocityBalancer plugin, ConfigHelper configHelper) {
        this.plugin = plugin;
        this.configHelper = configHelper;
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        Player player = event.getPlayer();
        String lobbyGroup = configHelper.getLobbyGroup();

        if (lobbyGroup != null && !lobbyGroup.isEmpty()) {
            RegisteredServer lobbyServer = plugin.getBalancedServer(lobbyGroup, player);
            if (lobbyServer != null) {
                player.createConnectionRequest(lobbyServer).fireAndForget();
            }
        }
    }
}
