package org.me.velocitybalancer.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.me.velocitybalancer.shared.ConfigHelper;
import org.me.velocitybalancer.velocity.VelocityBalancer;

public class BalanceSendCommand implements SimpleCommand {

    private final VelocityBalancer plugin;
    private final ProxyServer proxy;
    private final ConfigHelper configHelper;

    public BalanceSendCommand(VelocityBalancer plugin, ProxyServer proxy, ConfigHelper configHelper) {
        this.plugin = plugin;
        this.proxy = proxy;
        this.configHelper = configHelper;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        Player player = (Player) source;

        if (!player.hasPermission("velocitybalancer.bsend")) {
            String noPermissionMessage = configHelper.getMessage("no-permission");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(noPermissionMessage));
            return;
        }

        if (args.length < 2) {
            String bsendUsageMessage = configHelper.getMessage("bsend-usage");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(bsendUsageMessage));
            return;
        }

        if (args[0].equalsIgnoreCase("all")) {
            RegisteredServer server = proxy.getServer(args[1]).orElse(null);
            if (server == null) {
                String serverNotFoundMessage = configHelper.getMessage("server-not-found");
                source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(serverNotFoundMessage));
                return;
            }
            for (Player p : proxy.getAllPlayers()) {
                RegisteredServer bestserver = plugin.getBalancedServer(args[1], p);
                p.createConnectionRequest(bestserver != null ? bestserver : server).fireAndForget();
            }
            return;
        }

        Player target = proxy.getPlayer(args[0]).orElse(null);
        if (target == null) {
            String playerNotFoundMessage = configHelper.getMessage("player-not-found");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(playerNotFoundMessage));
            return;
        }

        RegisteredServer bestServer = plugin.getBalancedServer(args[1], target);
        target.createConnectionRequest(bestServer != null ? bestServer : proxy.getServer(args[1]).orElse(null)).fireAndForget();
    }
}
