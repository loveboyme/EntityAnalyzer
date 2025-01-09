package cn.panda.entityanalyzer.config;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {

    private final EntityAnalyzerPlugin plugin;
    private int defaultClusterCount;
    private boolean debugMode;
    private Material clusterItemMaterial;

    public PluginConfig(EntityAnalyzerPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        FileConfiguration config = plugin.getConfig();

        defaultClusterCount = config.getInt("default-cluster-count", -1);
        debugMode = config.getBoolean("debug-mode", false);
        String materialName = config.getString("cluster-item-material", "COMPASS").toUpperCase();
        try {
            clusterItemMaterial = Material.valueOf(materialName);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的物品材质名称: " + materialName + ", 使用默认的指南针 (COMPASS)。");
            clusterItemMaterial = Material.COMPASS;
        }
    }

    public int getDefaultClusterCount() {
        return defaultClusterCount;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public Material getClusterItemMaterial() {
        return clusterItemMaterial;
    }
}