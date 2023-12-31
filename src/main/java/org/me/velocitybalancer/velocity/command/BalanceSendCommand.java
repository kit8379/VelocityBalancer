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

public class BalanceSendCommand implements SimpleCommand {

    private final ProxyServer proxy;
    private final ConfigHelper configHelper;
    private final ServerBalancer serverBalancer;

    public BalanceSendCommand(ProxyServer proxy, ConfigHelper configHelper, ServerBalancer serverBalancer) {
        this.proxy = proxy;
        this.configHelper = configHelper;
        this.serverBalancer = serverBalancer;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("velocitybalancer.balancesend")) {
            source.sendMessage(Component.text(configHelper.getNoPermissionMessage()));
            return;
        }

        if (args.length < 2) {
            source.sendMessage(Component.text(configHelper.getBsendUsageMessage()));
            return;
        }

        String targetGroupName = args[1];
        if (args[0].equalsIgnoreCase("all")) {
            for (Player player : proxy.getAllPlayers()) {
                RegisteredServer balancedServer = serverBalancer.getBalancedServer(targetGroupName, player);
                if (balancedServer != null) {
                    player.createConnectionRequest(balancedServer).fireAndForget();
                }
            }
            source.sendMessage(Component.text(configHelper.getBsendAllSuccessMessage(targetGroupName)));
        } else {
            Optional<Player> targetPlayer = proxy.getPlayer(args[0]);
            if (targetPlayer.isEmpty()) {
                source.sendMessage(Component.text(configHelper.getPlayerNotFoundMessage()));
                return;
            }

            RegisteredServer balancedServer = serverBalancer.getBalancedServer(targetGroupName, targetPlayer.get());
            if (balancedServer != null) {
                targetPlayer.get().createConnectionRequest(balancedServer).fireAndForget();
                source.sendMessage(Component.text(configHelper.getBsendSuccessMessage(targetPlayer.get().getUsername(), targetGroupName)));
            } else {
                source.sendMessage(Component.text(configHelper.getServerOfflineMessage()));
            }
        }
    }
}
