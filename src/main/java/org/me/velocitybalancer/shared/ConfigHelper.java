package org.me.velocitybalancer.shared;

import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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
                        throw new IOException("Default config not found in resources!");
                    }
                }
            }
            configData = loader.load();
        } catch (IOException e) {
            logger.warning("Failed to load config.yml: " + e.getMessage());
        }
    }

    public String getLobbyGroup() {
        return configData.node("lobbygroup").getString("lobbygroup");
    }

    public boolean isOfflineDetectionEnabled() {
        return configData.node("offlinedetection").getBoolean(true);
    }

    public int getDetectionInterval() {
        return configData.node("detectioninterval").getInt(10);
    }

    public Map<Object, ? extends ConfigurationNode> getBalancingGroups() {
        return configData.node("balancing-groups").childrenMap();
    }

    public String getMessage(String key) {
        return Utils.colorize(configData.node("messages", key).getString());
    }

    public Map<String, String> getPermissionRedirects(String groupName) {
        Map<String, String> permissionRedirects = new HashMap<>();
        try {
            ConfigurationNode redirectNode = configData.node("balancing-groups", groupName, "permission-redirect");
            if (!redirectNode.virtual()) {
                for (Map.Entry<Object, ? extends ConfigurationNode> entry : redirectNode.childrenMap().entrySet()) {
                    String key = entry.getKey().toString();
                    String value = entry.getValue().getString();
                    permissionRedirects.put(key, value);
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to get permission redirects from config: " + e.getMessage());
        }
        return permissionRedirects;
    }
}
