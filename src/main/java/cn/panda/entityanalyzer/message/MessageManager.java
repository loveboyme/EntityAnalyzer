package cn.panda.entityanalyzer.message;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final EntityAnalyzerPlugin plugin;
    private final Map<String, String> messages = new HashMap<>();

    public MessageManager(EntityAnalyzerPlugin plugin) {
        this.plugin = plugin;
        loadMessages(plugin.getConfig().getString("language", "zh_CN")); // 默认加载中文
    }

    public void loadMessages(String language) {
        messages.clear();
        String fileName = "message/" + language + ".yml";
        File languageFile = new File(plugin.getDataFolder(), fileName);

        if (!languageFile.exists()) {
            plugin.saveResource(fileName, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(languageFile);
        if (config.getKeys(true).isEmpty()) {
            plugin.getLogger().warning("找不到语言文件或语言文件为空: " + fileName + ". 使用默认英文。");
            loadDefaultMessages();
            return;
        }

        for (String key : config.getKeys(false)) {
            String message = config.getString(key);
            if (message != null) {
                messages.put(key, ChatColor.translateAlternateColorCodes('§', message));
            }
        }
    }

    private void loadDefaultMessages() {
        messages.clear();
        String fileName = "message/en_US.yml";
        try (InputStream inputStream = plugin.getResource(fileName);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            if (inputStream == null) {
                plugin.getLogger().severe("找不到默认英文语言文件!");
                return;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
            for (String key : config.getKeys(false)) {
                String message = config.getString(key);
                if (message != null) {
                    messages.put(key, ChatColor.translateAlternateColorCodes('§', message));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("加载默认英文语言文件失败: " + e.getMessage());
        }
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "§c[Missing Message: " + key + "]§r");
    }

    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }
}