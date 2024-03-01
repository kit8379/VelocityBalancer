package org.me.velocitybalancer.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.me.velocitybalancer.shared.ConfigHelper;
import org.me.velocitybalancer.velocity.VelocityBalancer;

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

        Player player = (Player) source;

        if (!player.hasPermission("velocitybalancer.send")) {
            String noPermissionMessage = configHelper.getMessage("no-permission");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(noPermissionMessage));
            return;
        }

        if (args.length < 2) {
            String sendUsageMessage = configHelper.getMessage("send-usage");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(sendUsageMessage));
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
                p.createConnectionRequest(server).fireAndForget();
            }
            return;
        }

        Player target = proxy.getPlayer(args[0]).orElse(null);
        if (target == null) {
            String playerNotFoundMessage = configHelper.getMessage("player-not-found");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(playerNotFoundMessage));
            return;
        }

        RegisteredServer server = proxy.getServer(args[1]).orElse(null);
        if (server != null) {
            target.createConnectionRequest(server).fireAndForget();
        } else {
            String serverNotFoundMessage = configHelper.getMessage("server-not-found");
            source.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(serverNotFoundMessage));
        }
    }
}
