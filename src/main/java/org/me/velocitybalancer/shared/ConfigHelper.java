package org.me.velocitybalancer.shared;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

public class ConfigHelper {

    private final Logger logger;
    private final Path dataFolder;
    private ConfigurationNode configData;

    public ConfigHelper(Logger logger) {
        this.logger = logger;
        this.dataFolder = Path.of("plugins/VelocityBalancer");
    }

    public void loadConfiguration() {
        try {
            if (!Files.exists(dataFolder)) {
                Files.createDirectories(dataFolder);
            }

            Path configFile = dataFolder.resolve("config.yml");
            YamlConfigurationLoader loader =
                    YamlConfigurationLoader.builder()
                            .path(configFile)
                            .nodeStyle(NodeStyle.BLOCK)
                            .build();

            if (!Files.exists(configFile)) {
                try (InputStream defaultConfigStream = this.getClass().getResourceAsStream("/config.yml")) {
                    if (defaultConfigStream != null) {
                        Files.copy(defaultConfigStream, configFile);
                    } else {
                        throw new IOException("Could not find default config in resources!");
                    }
                }
            }
            configData = loader.load();
        } catch (IOException e) {
            logger.warning("Failed to load config.yml: " + e.getMessage());
        }
    }

    // Methods to get specific settings from the config
    public String getLobbyGroup() {
        return configData.node("lobbygroup").getString();
    }

    public boolean isOfflineDetectionEnabled() {
        return configData.node("offlinedetection").getBoolean();
    }

    public int getDetectionInterval() {
        return configData.node("detectioninterval").getInt();
    }

    public Map<Object, ? extends ConfigurationNode> getBalancingGroups() {
        return configData.node("balancing-groups").childrenMap();
    }

    // Methods to get messages
    public String getNoPermissionMessage() {
        return Utils.colorize(configData.node("messages", "no-permission").getString("&cYou don't have permission to use this command"));
    }

    public String getSendUsageMessage() {
        return Utils.colorize(configData.node("messages", "send-usage").getString("&e/send <user> <server>"));
    }

    public String getBsendUsageMessage() {
        return Utils.colorize(configData.node("messages", "bsend-usage").getString("&e/bsend <user> <server>"));
    }

    public String getServerNotFoundMessage() {
        return Utils.colorize(configData.node("messages", "server-not-found").getString("&cServer not found"));
    }

    public String getPlayerNotFoundMessage() {
        return Utils.colorize(configData.node("messages", "player-not-found").getString("&cPlayer not found"));
    }

    public String getReloadMessage() {
        return Utils.colorize(configData.node("messages", "reload").getString("&aReloaded config"));
    }
}
