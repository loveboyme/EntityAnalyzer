package cn.panda.entityanalyzer.config;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {

    private final EntityAnalyzerPlugin plugin;
    private int defaultClusterCount;
    private boolean debugMode;
    private Material clusterItemMaterial;
    private int particleCount; // 新增：粒子数量
    private long particleRefreshRate; // 新增：粒子刷新频率 (单位：ticks)

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
        particleCount = config.getInt("particle.count", 50); // 默认值可以根据需要调整
        particleRefreshRate = config.getLong("particle.refresh-rate", 3L); // 默认值可以根据需要调整
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

    public int getParticleCount() {
        return particleCount;
    }

    public long getParticleRefreshRate() {
        return particleRefreshRate;
    }
}