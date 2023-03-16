package org.me.velocitybalancer;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class Config {

    private Map<String, Object> data;

    public Config() {
        load();
    }

    void load() {
        Yaml yaml = new Yaml(new Constructor(Map.class));
        Path dataFolder = Paths.get("plugins", "VelocityBalancer");
        Path configFile = dataFolder.resolve("config.yml");

        if (!Files.exists(dataFolder)) {
            try {
                Files.createDirectories(dataFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!Files.exists(configFile)) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (is != null) {
                    Files.copy(is, configFile);
                } else {
                    throw new FileNotFoundException("Default config.yml not found in the plugin resources.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile.toFile()))) {
            data = yaml.load(reader);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Object get(String key) {
        return data.get(key);
    }
}
