package org.me.velocitybalancer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Config {
    private final File configFile;
    private Map<String, Object> configData;
    private final ObjectMapper mapper;
    private final Logger logger;


    public Config(Logger logger) {
        this.logger = logger;
        // Create the plugin folder if it doesn't exist
        mapper = new ObjectMapper(new YAMLFactory());
        File pluginFolder = new File("plugins" + File.separator + "velocitybalancer");
        if (!pluginFolder.exists()) {
            boolean dirsCreated = pluginFolder.mkdirs();
            if (!dirsCreated) {
                logger.error("Failed to create plugin folder.");
            }
        }

        configFile = new File(pluginFolder, "config.yml");

        // Create the config.yml file if it doesn't exist
        if (!configFile.exists()) {
            try {
                boolean fileCreated = configFile.createNewFile();
                if (!fileCreated) {
                    logger.error("Failed to create config file.");
                } else {
                    configData = createDefaultConfig();
                    saveConfig();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            load();
        }
    }


    public Object get(String key) {
        return configData.get(key);
    }

    public void load() {
        try {
            configData = mapper.readValue(configFile, new TypeReference<HashMap<String, Object>>() {});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        try {
            mapper.writeValue(configFile, configData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Object> createDefaultConfig() {
        Map<String, Object> defaultConfig = new HashMap<>();

        defaultConfig.put("offlinedetection", true);
        defaultConfig.put("detectioninterval", 60);
        defaultConfig.put("lobbygroup", "lobby");
        defaultConfig.put("balancing-groups", new HashMap<String, Object>());

        return defaultConfig;
    }
}
