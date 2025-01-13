package cn.panda.entityanalyzer.config;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

public class PluginConfig {

    private final EntityAnalyzerPlugin plugin;
    private int defaultClusterCount;
    private boolean debugMode;
    private Material clusterItemMaterial;
    private boolean particleEnabled;
    private int particleCount;
    private long particleRefreshRate;
    private long particleDuration;
    private boolean glowingEnabled;
    private long glowingDuration;

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

        particleEnabled = config.getBoolean("particle.enabled", true);
        particleCount = config.getInt("particle.count", 50);
        particleRefreshRate = config.getLong("particle.refresh-rate", 3L);
        particleDuration = config.getLong("particle.duration", 600L);

        glowingEnabled = config.getBoolean("glowing.enabled", true);
        glowingDuration = config.getLong("glowing.duration", 200L);
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

    public boolean isParticleEnabled() {
        return particleEnabled;
    }

    public int getParticleCount() {
        return particleCount;
    }

    public long getParticleRefreshRate() {
        return particleRefreshRate;
    }

    public long getParticleDuration() {
        return particleDuration;
    }

    public boolean isGlowingEnabled() {
        return glowingEnabled;
    }

    public long getGlowingDuration() {
        return glowingDuration;
    }
}
