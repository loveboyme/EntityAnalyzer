package cn.panda.entityanalyzer.util;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
import cn.panda.entityanalyzer.kmeans.Point;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ParticleDisplay {

    private final EntityAnalyzerPlugin plugin;
    private final Map<Point, BukkitTask> boundaryTaskIds = new WeakHashMap<>(); // 存储边界粒子任务
    private final int particleCount;
    private final long particleRefreshRate;
    private final long particleDuration;

    public ParticleDisplay(EntityAnalyzerPlugin plugin) {
        this.plugin = plugin;
        this.particleCount = plugin.getConfigManager().getParticleCount();
        this.particleRefreshRate = plugin.getConfigManager().getParticleRefreshRate();
        this.particleDuration = plugin.getConfigManager().getParticleDuration();
    }

    // 显示指定聚类区域的边界粒子效果
    public void displayClusterBoundary(Player player, Point centroid, List<Entity> entitiesInCluster) {
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ParticleDisplay] displayClusterBoundary 方法被调用，中心点：" + centroid);
        }

        // 清除之前可能存在的边界粒子效果
        if (boundaryTaskIds.containsKey(centroid)) {
            boundaryTaskIds.get(centroid).cancel();
            boundaryTaskIds.remove(centroid);
        }

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ParticleDisplay] displayClusterBoundary：聚类中包含 " + entitiesInCluster.size() + " 个实体");
        }
        if (entitiesInCluster.isEmpty()) {
            plugin.getLogger().warning("[ParticleDisplay] displayClusterBoundary：中心点 " + centroid + " 对应的聚类中没有实体");
            return;
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;

        for (Entity entity : entitiesInCluster) {
            if (entity.isValid()) {
                org.bukkit.Location loc = entity.getLocation();
                minX = Math.min(minX, loc.getX());
                minY = Math.min(minY, loc.getY());
                minZ = Math.min(minZ, loc.getZ());
                maxX = Math.max(maxX, loc.getX());
                maxY = Math.max(maxY, loc.getY());
                maxZ = Math.max(maxZ, loc.getZ());
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("[ParticleDisplay]   实体位置：x=" + loc.getX() + ", y=" + loc.getY() + ", z=" + loc.getZ());
                }
            }
        }

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ParticleDisplay] displayClusterBoundary：计算出的边界 - minX: " + minX + ", maxX: " + maxX + ", minY: " + minY + ", maxY: " + maxY + ", minZ: " + minZ + ", maxZ: " + maxZ);
        }

        World world = player.getWorld();
        final double finalMinX = minX;
        final double finalMaxX = maxX;
        final double finalMinY = minY;
        final double finalMaxY = maxY;
        final double finalMinZ = minZ;
        final double finalMaxZ = maxZ;
        final Particle finalParticleType = Particle.VILLAGER_HAPPY;
        final int finalParticleCount = particleCount;
        final double finalDelta = 0.6;
        final long finalParticleDuration = particleDuration;
        final Point finalCentroid = centroid;
        final Map<Point, BukkitTask> finalBoundaryTaskIds = boundaryTaskIds;
        final Player finalPlayer = player; // 将 player 传递到 lambda 中

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ParticleDisplay] displayClusterBoundary：开始绘制粒子效果，持续 " + finalParticleDuration / 20 + " 秒");
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // 绘制边框粒子
            for (double x = finalMinX - 0.2; x <= finalMaxX + 0.2; x += finalDelta) {
                displayParticle(finalPlayer, world, x, finalMinY, finalMinZ - 0.2, finalParticleType, finalParticleCount);
                displayParticle(finalPlayer, world, x, finalMaxY, finalMinZ - 0.2, finalParticleType, finalParticleCount);
                displayParticle(finalPlayer, world, x, finalMinY, finalMaxZ + 0.2, finalParticleType, finalParticleCount);
                displayParticle(finalPlayer, world, x, finalMaxY, finalMaxZ + 0.2, finalParticleType, finalParticleCount);
            }
            for (double y = finalMinY; y <= finalMaxY; y += finalDelta) {
                displayParticle(finalPlayer, world, finalMinX - 0.2, y, finalMinZ - 0.2, finalParticleType, finalParticleCount);
                displayParticle(finalPlayer, world, finalMaxX + 0.2, y, finalMinZ - 0.2, finalParticleType, finalParticleCount);
                displayParticle(finalPlayer, world, finalMinX - 0.2, y, finalMaxZ + 0.2, finalParticleType, finalParticleCount);
                displayParticle(finalPlayer, world, finalMaxX + 0.2, y, finalMaxZ + 0.2, finalParticleType, finalParticleCount);
            }
            for (double z = finalMinZ - 0.2; z <= finalMaxZ + 0.2; z += finalDelta) {
                displayParticle(finalPlayer, world, finalMinX - 0.2, finalMinY, z, finalParticleType, finalParticleCount);
                displayParticle(finalPlayer, world, finalMaxX + 0.2, finalMinY, z, finalParticleType, finalParticleCount);
                displayParticle(finalPlayer, world, finalMinX - 0.2, finalMaxY, z, finalParticleType, finalParticleCount);
                displayParticle(finalPlayer, world, finalMaxX + 0.2, finalMaxY, z, finalParticleType, finalParticleCount);
            }
        }, 0L, particleRefreshRate);

        // 存储任务，以便后续清除
        boundaryTaskIds.put(centroid, task);

        // 设置定时器，在指定时间后停止显示粒子效果
        final BukkitTask finalTask = task;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (finalBoundaryTaskIds.containsKey(finalCentroid) && finalBoundaryTaskIds.get(finalCentroid).equals(finalTask)) {
                finalTask.cancel();
                finalBoundaryTaskIds.remove(finalCentroid);
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("[ParticleDisplay] displayClusterBoundary：粒子效果已停止。");
                }
            }
        }, finalParticleDuration);
    }

    // 在指定位置显示粒子效果，并指定观察者
    private void displayParticle(Player player, World world, double x, double y, double z, Particle particle, int count) {
        Location location = new Location(world, x, y, z);
        Block block = location.getBlock();
        if (!block.getType().isSolid()) { // 检查目标位置是否不是固体方块
            player.spawnParticle(particle, x, y, z, count, 0.01, 0.01, 0.01, 0.01);
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("[ParticleDisplay]   生成粒子 " + particle + "，位置：x=" + x + ", y=" + y + ", z=" + z + " 给玩家 " + player.getName());
            }
        } else if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ParticleDisplay]   跳过生成粒子 " + particle + "，位置：x=" + x + ", y=" + y + ", z=" + z + "，因为方块是 " + block.getType());
        }
    }

    // 清除特定聚类的粒子效果
    public void clearBoundary(Point centroid) {
        if (boundaryTaskIds.containsKey(centroid)) {
            boundaryTaskIds.get(centroid).cancel();
            boundaryTaskIds.remove(centroid);
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("[ParticleDisplay] 清除聚类 " + centroid + " 的粒子效果");
            }
        }
    }

    // 清除所有显示的粒子效果
    public void clearAllBoundaries() {
        boundaryTaskIds.values().forEach(BukkitTask::cancel);
        boundaryTaskIds.clear();
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ParticleDisplay] 清除所有聚类的粒子效果");
        }
    }
}