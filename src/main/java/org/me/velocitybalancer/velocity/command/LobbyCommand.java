package org.me.velocitybalancer.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.me.velocitybalancer.shared.ConfigHelper;
import net.kyori.adventure.text.Component;
import org.me.velocitybalancer.velocity.VelocityBalancer;

public class LobbyCommand implements SimpleCommand {

    private final VelocityBalancer plugin;
    private final ConfigHelper configHelper;

    public LobbyCommand(VelocityBalancer plugin, ConfigHelper configHelper) {
        this.plugin = plugin;
        this.configHelper = configHelper;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player)) {
            String onlyPlayerMessage = configHelper.getMessage("player-only");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(onlyPlayerMessage));
            return;
        }

        Player player = (Player) source;

        if (!player.hasPermission("velocitybalancer.hub")) {
            String noPermissionMessage = configHelper.getMessage("no-permission");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(noPermissionMessage));
            return;
        }

        String lobbyGroup = configHelper.getLobbyGroup();

        if (lobbyGroup != null && !lobbyGroup.isEmpty()) {
            RegisteredServer lobbyServer = plugin.getBalancedServer(lobbyGroup, player);
            if (lobbyServer != null) {
                player.createConnectionRequest(lobbyServer).fireAndForget();
            }
        }
    }
}
