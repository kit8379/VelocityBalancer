package org.me.velocitybalancer.shared;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public String getLobbyGroup() {
        return configData.node("lobbygroup").getString();
    }

    public boolean isOfflineDetectionEnabled() {
        return configData.node("offlinedetection").getBoolean();
    }

    public int getDetectionInterval() {
        return configData.node("detectioninterval").getInt();
    }

    public ConfigurationNode getGroup(String groupName) {
        return configData.node("balancing-groups").node(groupName);
    }

    public boolean isGroupName(String name) {
        return configData.node("balancing-groups").hasChild(name);
    }

    // Message retrieval methods
    public String getNoPermissionMessage() {
        return Utils.colorize(configData.node("messages", "no-permission").getString("&cYou don't have permission to use this command."));
    }

    public String getSendUsageMessage() {
        return Utils.colorize(configData.node("messages", "send-usage").getString("&e/send <user> <server>"));
    }

    public String getServerNotFoundMessage() {
        return Utils.colorize(configData.node("messages", "server-not-found").getString("&cServer not found."));
    }

    public String getPlayerNotFoundMessage() {
        return Utils.colorize(configData.node("messages", "player-not-found").getString("&cPlayer not found."));
    }

    public String getReloadMessage() {
        return Utils.colorize(configData.node("messages", "reload").getString("&aConfiguration reloaded."));
    }

    public String getPlayerOnlyMessage() {
        return Utils.colorize(configData.node("messages", "player-only").getString("&cOnly players can use this command."));
    }

    public String getServerOfflineMessage() {
        return Utils.colorize(configData.node("messages", "server-offline").getString("&cServer is offline."));
    }

    public String getLobbyGroupNotFoundMessage() {
        return Utils.colorize(configData.node("messages", "lobby-group-not-found").getString("&cLobby group not found."));
    }

    public String getSendSuccessMessage(String playerName, String serverName) {
        String messageTemplate = configData.node("messages", "send-success").getString("&aSuccessfully sent %player% to %server%.");
        return Utils.colorize(messageTemplate.replace("%player%", playerName).replace("%server%", serverName));
    }

    public String getSendAllSuccessMessage(String serverName) {
        String messageTemplate = configData.node("messages", "send-all-success").getString("&aSuccessfully sent all players to %server%.");
        return Utils.colorize(messageTemplate.replace("%server%", serverName));
    }
}
