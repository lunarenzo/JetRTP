package lunatech.jetrtp.translation;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class Translation {
    private static ConfigurationNode rootNode;

    public static void load(@NotNull Path directory, @NotNull String languageCode) {
        try {
            Path langDir = directory.resolve("lang");
            if (!Files.exists(langDir)) {
                Files.createDirectories(langDir);
            }
            Path langFile = langDir.resolve(languageCode + ".yml");
            
            if (!Files.exists(langFile)) {
                try (InputStream in = Translation.class.getResourceAsStream("/lang/" + languageCode + ".yml")) {
                    if (in != null) {
                        Files.copy(in, langFile, StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        // Fallback to en_US.yml if the specified language file is not found
                        try (InputStream enIn = Translation.class.getResourceAsStream("/lang/en_US.yml")) {
                            if (enIn != null) {
                                Files.copy(enIn, langFile, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .path(langFile)
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build();
            rootNode = loader.load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static @NotNull String of(@NotNull String path) {
        return of(path, "");
    }

    public static @NotNull String of(@NotNull String path, @NotNull String defaultValue) {
        if (rootNode == null) {
            return defaultValue;
        }
        Object[] keys = path.split("\\.");
        ConfigurationNode node = rootNode.node(keys);
        String val = node.getString();
        return val != null ? val : defaultValue;
    }
}
