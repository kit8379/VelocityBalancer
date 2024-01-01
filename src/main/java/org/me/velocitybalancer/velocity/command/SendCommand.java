package org.me.velocitybalancer.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.me.velocitybalancer.shared.ConfigHelper;
import org.me.velocitybalancer.shared.ServerBalancer;

import java.util.Optional;

public class SendCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final ConfigHelper configHelper;
    private final ServerBalancer serverBalancer;

    public SendCommand(ProxyServer proxy, ConfigHelper configHelper, ServerBalancer serverBalancer) {
        this.proxy = proxy;
        this.configHelper = configHelper;
        this.serverBalancer = serverBalancer;
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

        String sourceArg = args[0];
        String targetArg = args[1];

        if ("all".equalsIgnoreCase(sourceArg)) {
            handleSendAllPlayers(source, targetArg);
        } else {
            handleSendSinglePlayer(source, sourceArg, targetArg);
        }
    }

    private void handleSendAllPlayers(CommandSource source, String targetArg) {
        if (configHelper.isGroupName(targetArg)) {
            // Handle group balancing send
            for (Player player : proxy.getAllPlayers()) {
                RegisteredServer balancedServer = serverBalancer.getBalancedServer(targetArg, player);
                if (balancedServer != null) {
                    player.createConnectionRequest(balancedServer).fireAndForget();
                }
            }
            source.sendMessage(Component.text(configHelper.getSendAllSuccessMessage(targetArg)));
        } else {
            // Handle send to a specific server
            Optional<RegisteredServer> targetServer = proxy.getServer(targetArg);

            if (targetServer.isEmpty()) {
                source.sendMessage(Component.text(configHelper.getServerNotFoundMessage()));
                return;
            }

            for (Player player : proxy.getAllPlayers()) {
                player.createConnectionRequest(targetServer.get()).fireAndForget();
            }
            source.sendMessage(Component.text(configHelper.getSendAllSuccessMessage(targetArg)));
        }
    }

    private void handleSendSinglePlayer(CommandSource source, String playerName, String targetArg) {
        Optional<Player> targetPlayer = proxy.getPlayer(playerName);
        if (targetPlayer.isEmpty()) {
            source.sendMessage(Component.text(configHelper.getPlayerNotFoundMessage()));
            return;
        }

        RegisteredServer targetServer = serverBalancer.getBalancedServer(targetArg, targetPlayer.get());
        if (targetServer != null) {
            targetPlayer.get().createConnectionRequest(targetServer).fireAndForget();
            source.sendMessage(Component.text(configHelper.getSendSuccessMessage(playerName, targetServer.getServerInfo().getName())));
        } else {
            source.sendMessage(Component.text(configHelper.getServerNotFoundMessage()));
        }
    }
}
