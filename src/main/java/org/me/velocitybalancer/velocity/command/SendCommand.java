package org.me.velocitybalancer.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.me.velocitybalancer.shared.ConfigHelper;

import java.util.Optional;

public class SendCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final ConfigHelper configHelper;

    public SendCommand(ProxyServer proxy, ConfigHelper configHelper) {
        this.proxy = proxy;
        this.configHelper = configHelper;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("velocitybalancer.send")) {
            source.sendMessage(Component.text(configHelper.getNoPermissionMessage()));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(Component.text(configHelper.getSendUsageMessage()));
            return;
        }

        if (args[0].equalsIgnoreCase("all")) {
            String targetServerName = args[1];
            Optional<RegisteredServer> targetServer = proxy.getServer(targetServerName);
            if (targetServer.isEmpty()) {
                source.sendMessage(Component.text(configHelper.getServerNotFoundMessage()));
                return;
            }

            for (Player player : proxy.getAllPlayers()) {
                player.createConnectionRequest(targetServer.get()).fireAndForget();
            }
            source.sendMessage(Component.text(configHelper.getSendAllSuccessMessage(targetServerName)));
            return;
        }

        String targetPlayerName = args[0];
        String targetServerName = args[1];

        Optional<Player> targetPlayer = proxy.getPlayer(targetPlayerName);
        if (targetPlayer.isEmpty()) {
            source.sendMessage(Component.text(configHelper.getPlayerNotFoundMessage()));
            return;
        }

        Optional<RegisteredServer> targetServer = proxy.getServer(targetServerName);
        if (targetServer.isEmpty()) {
            source.sendMessage(Component.text(configHelper.getServerNotFoundMessage()));
            return;
        }

        targetPlayer.get().createConnectionRequest(targetServer.get()).fireAndForget();
        source.sendMessage(Component.text(configHelper.getSendSuccessMessage(targetPlayerName, targetServerName)));
    }
}
