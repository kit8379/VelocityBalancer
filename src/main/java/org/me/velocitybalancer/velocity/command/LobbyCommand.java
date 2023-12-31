package org.me.velocitybalancer.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.me.velocitybalancer.shared.ConfigHelper;
import org.me.velocitybalancer.shared.ServerBalancer;

public class LobbyCommand implements SimpleCommand {

    private final ConfigHelper configHelper;
    private final ServerBalancer serverBalancer;

    public LobbyCommand(ConfigHelper configHelper, ServerBalancer serverBalancer) {
        this.configHelper = configHelper;
        this.serverBalancer = serverBalancer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            source.sendMessage(Component.text("Only players can use this command."));
            return;
        }

        Player player = (Player) source;
        String lobbyGroup = configHelper.getLobbyGroup();

        if (lobbyGroup != null) {
            RegisteredServer lobbyServer = serverBalancer.getBalancedServer(lobbyGroup, player);
            if (lobbyServer != null) {
                player.createConnectionRequest(lobbyServer).fireAndForget();
            } else {
                player.sendMessage(Component.text("No lobby server available."));
            }
        } else {
            player.sendMessage(Component.text("Lobby group is not configured."));
        }
    }
}
