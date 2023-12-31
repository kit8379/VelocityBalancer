package org.me.velocitybalancer.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.me.velocitybalancer.shared.ConfigHelper;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.me.velocitybalancer.shared.ServerBalancer;
import org.me.velocitybalancer.velocity.command.BalanceSendCommand;
import org.me.velocitybalancer.velocity.command.LobbyCommand;
import org.me.velocitybalancer.velocity.command.ReloadCommand;
import org.me.velocitybalancer.velocity.command.SendCommand;



@Plugin(id = "velocitybalancer", name = "VelocityBalancer", version = "1.0.0", description = "A server balancing plugin", authors = {"kit8379"})
public class VelocityBalancer {
    private final Logger logger;
    private final ProxyServer proxy;
    private ConfigHelper configHelper;
    private ServerBalancer serverBalancer;

    @Inject
    public VelocityBalancer(Logger logger, ProxyServer proxy) {
        this.logger = logger;
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("VelocityBalancer is starting up...");
        initialize();
        logger.info("VelocityBalancer has started successfully!");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("VelocityBalancer is shutting down...");
        shutdown();
        logger.info("VelocityBalancer has shut down successfully!");
    }

    public void initialize() {
        configHelper = new ConfigHelper(logger);
        configHelper.loadConfiguration();
        serverBalancer = new ServerBalancer(proxy, configHelper);

        proxy.getCommandManager().register("hub", new LobbyCommand(configHelper, serverBalancer), "lobby");
        proxy.getCommandManager().register("send", new SendCommand(proxy, configHelper));
        proxy.getCommandManager().register("bsend", new BalanceSendCommand(proxy, configHelper, serverBalancer));
        proxy.getCommandManager().register("balancereload", new ReloadCommand(this, configHelper));

        if (configHelper.isOfflineDetectionEnabled()) {
            long detectionInterval = configHelper.getDetectionInterval();
            proxy.getScheduler().buildTask(this, this::checkOfflineServers)
                    .repeat(detectionInterval, TimeUnit.SECONDS).schedule();
        }
    }

    private void checkOfflineServers() {
        for (RegisteredServer server : proxy.getAllServers()) {
            server.ping().whenComplete((ping, throwable) -> {
                serverBalancer.updateServerOnlineStatus(server.getServerInfo().getName(), throwable == null);
            });
        }
    }

    public void shutdown() {
        // Any shutdown logic goes here
    }

    public void reload() {
        logger.info("VelocityBalancer is reloading...");
        shutdown();
        initialize();
        logger.info("VelocityBalancer has reloaded successfully!");
    }
}
