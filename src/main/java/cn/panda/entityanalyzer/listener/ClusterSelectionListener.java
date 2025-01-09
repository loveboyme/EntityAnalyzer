package cn.panda.entityanalyzer.listener;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
import cn.panda.entityanalyzer.kmeans.Point;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class ClusterSelectionListener implements Listener {

    private final EntityAnalyzerPlugin plugin;
    private final Map<Point, List<Entity>> clusterEntities = new WeakHashMap<>();
    private final Map<Point, Integer> boundaryTaskIds = new WeakHashMap<>(); // 存储边界粒子任务的 ID
    private Point currentDisplayingCluster = null; // 用于跟踪当前正在显示的聚类

    public ClusterSelectionListener(EntityAnalyzerPlugin plugin) {
        this.plugin = plugin;
    }

    // 接收聚类结果
    public void setClusterEntities(Map<Point, List<Entity>> clusterEntities) {
        this.clusterEntities.clear();
        this.clusterEntities.putAll(clusterEntities);
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ClusterSelectionListener] 接收到聚类数据，聚类数量：" + clusterEntities.size());
            for (Map.Entry<Point, List<Entity>> entry : clusterEntities.entrySet()) {
                plugin.getLogger().info("[ClusterSelectionListener]   中心点：" + entry.getKey() + "，实体数量：" + entry.getValue().size());
            }
        }
        currentDisplayingCluster = null; // 清除当前显示的聚类
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        if (!event.getView().getTitle().equals(plugin.getMessageManager().getMessage("analysis-result-title"))) {
            plugin.getLogger().warning("[onInventoryClick] Inventory 标题不匹配: " + event.getView().getTitle());
            return;
        }

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        ItemMeta meta = clickedItem.getItemMeta();

        if (meta != null && meta.getDisplayName().startsWith(plugin.getMessageManager().getMessage("cluster-item-name-prefix"))) {
            String displayName = ChatColor.stripColor(meta.getDisplayName());
            String prefix = ChatColor.stripColor(plugin.getMessageManager().getMessage("cluster-item-name-prefix"));
            if (displayName.startsWith(prefix)) {
                String remaining = displayName.substring(prefix.length()).trim();
                StringBuilder indexBuilder = new StringBuilder();
                for (char c : remaining.toCharArray()) {
                    if (Character.isDigit(c)) {
                        indexBuilder.append(c);
                    } else {
                        break; // 遇到非数字字符停止提取
                    }
                }
                String indexStr = indexBuilder.toString();

                if (!indexStr.isEmpty()) {
                    try {
                        int clusterIndex = Integer.parseInt(indexStr) - 1;
                        plugin.getLogger().info("[onInventoryClick] 点击了分析结果物品: " + meta.getDisplayName() + ", 解析的聚类索引 (0-based): " + clusterIndex);

                        int i = 0;
                        Point targetCentroid = null;
                        for (Point centroid : clusterEntities.keySet()) {
                            plugin.getLogger().info("[onInventoryClick]   Checking centroid at index " + i + ": " + centroid);
                            if (i == clusterIndex) {
                                targetCentroid = centroid;
                                plugin.getLogger().info("[onInventoryClick] 找到目标中心点: " + targetCentroid);
                                break;
                            }
                            i++;
                        }

                        if (targetCentroid != null) {
                            plugin.getLogger().info("[onInventoryClick] 准备传送玩家 " + player.getName() + " 到 " + targetCentroid);
                            Location teleportLocation = new Location(player.getWorld(), targetCentroid.getX(), player.getLocation().getY(), targetCentroid.getZ());
                            player.teleport(teleportLocation);
                            player.sendMessage(plugin.getMessageManager().getMessage("teleport-to-area"));
                            highlightEntities(player, targetCentroid);
                            displayClusterBoundary(player, targetCentroid);
                        } else {
                            plugin.getLogger().warning("[onInventoryClick] 未找到对应的聚类中心点。");
                            player.sendMessage(plugin.getMessageManager().getMessage("cluster-info-not-found"));
                        }

                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("[onInventoryClick] 解析聚类索引失败: " + indexStr + ", 物品名称: " + meta.getDisplayName());
                        player.sendMessage(plugin.getMessageManager().getMessage("invalid-cluster-info-format"));
                    }
                } else {
                    plugin.getLogger().warning("[onInventoryClick] 未在物品名称中找到聚类索引: " + meta.getDisplayName());
                    player.sendMessage(plugin.getMessageManager().getMessage("invalid-cluster-info-format"));
                }
            } else {
                plugin.getLogger().warning("[onInventoryClick] 物品 Display Name 格式不符合预期: " + meta.getDisplayName());
            }
        } else if (meta != null) {
            plugin.getLogger().warning("[onInventoryClick] 点击的物品不是分析结果: " + meta.getDisplayName());
        }
    }
    // 清除之前显示的粒子效果和高亮
    private void clearPreviousEffects(Player player) {
        if (currentDisplayingCluster != null) {
            // 清除粒子效果
            if (boundaryTaskIds.containsKey(currentDisplayingCluster)) {
                Bukkit.getScheduler().cancelTask(boundaryTaskIds.get(currentDisplayingCluster));
                boundaryTaskIds.remove(currentDisplayingCluster);
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("[ClusterSelectionListener] 清除之前显示的聚类 " + currentDisplayingCluster + " 的粒子效果");
                }
            }

            // 清除高亮效果
            if (clusterEntities.containsKey(currentDisplayingCluster)) {
                List<Entity> entitiesToUnhighlight = clusterEntities.get(currentDisplayingCluster);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Entity entity : entitiesToUnhighlight) {
                        if (entity.isValid()) {
                            if (entity instanceof Item) {
                                ((Item) entity).setGlowing(false);
                            } else if (entity instanceof LivingEntity) {
                                ((LivingEntity) entity).removePotionEffect(PotionEffectType.GLOWING);
                            }
                        }
                    }
                });
            }
            currentDisplayingCluster = null;
        }
    }

    // 高亮显示指定聚类中的实体
    private void highlightEntities(Player player, Point centroid) {
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ClusterSelectionListener] highlightEntities 方法被调用，中心点：" + centroid);
        }
        if (!clusterEntities.containsKey(centroid)) {
            plugin.getLogger().warning("[ClusterSelectionListener] highlightEntities：找不到中心点 " + centroid + " 对应的聚类");
            return;
        }

        List<Entity> entitiesToHighlight = clusterEntities.get(centroid);
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ClusterSelectionListener] highlightEntities：找到 " + entitiesToHighlight.size() + " 个实体需要高亮显示");
        }
        if (entitiesToHighlight.isEmpty()) {
            plugin.getLogger().warning("[ClusterSelectionListener] highlightEntities：中心点 " + centroid + " 对应的聚类中没有实体");
            return;
        }

        for (Entity entity : entitiesToHighlight) {
            if (entity.isValid()) {
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("[ClusterSelectionListener]   尝试高亮实体 ID: " + entity.getEntityId() + ", 类型: " + entity.getType());
                }
                if (entity instanceof Item) {
                    ((Item) entity).setGlowing(true);
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("[ClusterSelectionListener]     物品设置为发光");
                    }
                } else if (entity instanceof LivingEntity) {
                    ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 10, 0));
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("[ClusterSelectionListener]     生物添加发光效果");
                    }
                }
            } else {
                plugin.getLogger().warning("[ClusterSelectionListener]   尝试高亮显示的实体 " + entity.getEntityId() + " 无效，已跳过");
            }
        }

        // 停止高亮
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("[ClusterSelectionListener] highlightEntities：停止高亮显示");
            }
            for (Entity entity : entitiesToHighlight) {
                if (entity.isValid()) {
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("[ClusterSelectionListener]   尝试移除高亮实体 ID: " + entity.getEntityId() + ", 类型: " + entity.getType());
                    }
                    if (entity instanceof Item) {
                        ((Item) entity).setGlowing(false);
                        if (plugin.getConfigManager().isDebugMode()) {
                            plugin.getLogger().info("[ClusterSelectionListener]     物品移除发光");
                        }
                    } else if (entity instanceof LivingEntity) {
                        ((LivingEntity) entity).removePotionEffect(PotionEffectType.GLOWING);
                        if (plugin.getConfigManager().isDebugMode()) {
                            plugin.getLogger().info("[ClusterSelectionListener]     生物移除发光效果");
                        }
                    }
                }
            }
        }, 20 * 10);
    }

    // 显示指定聚类区域的边界粒子效果
    private void displayClusterBoundary(Player player, Point centroid) {
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ClusterSelectionListener] displayClusterBoundary 方法被调用，中心点：" + centroid);
        }
        if (!clusterEntities.containsKey(centroid)) {
            plugin.getLogger().warning("[ClusterSelectionListener] displayClusterBoundary：找不到中心点 " + centroid + " 对应的聚类");
            return;
        }

        // 清除之前可能存在的边界粒子效果
        if (boundaryTaskIds.containsKey(centroid)) {
            Bukkit.getScheduler().cancelTask(boundaryTaskIds.get(centroid));
            boundaryTaskIds.remove(centroid);
        }

        List<Entity> entitiesInCluster = clusterEntities.get(centroid);
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ClusterSelectionListener] displayClusterBoundary：聚类中包含 " + entitiesInCluster.size() + " 个实体");
        }
        if (entitiesInCluster.isEmpty()) {
            plugin.getLogger().warning("[ClusterSelectionListener] displayClusterBoundary：中心点 " + centroid + " 对应的聚类中没有实体");
            return;
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE, maxZ = Double.MIN_VALUE;

        for (Entity entity : entitiesInCluster) {
            Location loc = entity.getLocation();
            minX = Math.min(minX, loc.getX());
            minY = Math.min(minY, loc.getY());
            minZ = Math.min(minZ, loc.getZ());
            maxX = Math.max(maxX, loc.getX());
            maxY = Math.max(maxY, loc.getY());
            maxZ = Math.max(maxZ, loc.getZ());
            if (plugin.getConfigManager().isDebugMode()) {
                plugin.getLogger().info("[ClusterSelectionListener]   实体位置：x=" + loc.getX() + ", y=" + loc.getY() + ", z=" + loc.getZ());
            }
        }

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ClusterSelectionListener] displayClusterBoundary：计算出的边界 - minX: " + minX + ", maxX: " + maxX + ", minY: " + minY + ", maxY: " + maxY + ", minZ: " + minZ + ", maxZ: " + maxZ);
        }

        World world = player.getWorld();
        final double finalMinX = minX; // 声明为 final
        final double finalMaxX = maxX;
        final double finalMinY = minY;
        final double finalMaxY = maxY;
        final double finalMinZ = minZ;
        final double finalMaxZ = maxZ;
        final Particle finalParticleType = Particle.VILLAGER_HAPPY; // 声明为 final
        final int finalParticleCount = 20; // 增加粒子数量
        final double finalDelta = 0.8; // 稍微增大粒子间隔，避免过于密集
        final int durationTicks = 20 * 30; // 持续时间设置为 30 秒
        final Point finalCentroid = centroid; // 声明为 final，用于 runTaskLater
        final Map<Point, Integer> finalBoundaryTaskIds = boundaryTaskIds; // 声明为 final，用于 runTaskLater

        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ClusterSelectionListener] displayClusterBoundary：开始绘制粒子效果，持续 " + durationTicks / 20 + " 秒");
        }

        // 使用 BukkitRunnable 来持续显示粒子效果
        int taskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // 绘制边框粒子
            for (double x = finalMinX - 0.2; x <= finalMaxX + 0.2; x += finalDelta) {
                displayParticle(world, x, finalMinY, finalMinZ - 0.2, finalParticleType, finalParticleCount);
                displayParticle(world, x, finalMaxY, finalMinZ - 0.2, finalParticleType, finalParticleCount);
                displayParticle(world, x, finalMinY, finalMaxZ + 0.2, finalParticleType, finalParticleCount);
                displayParticle(world, x, finalMaxY, finalMaxZ + 0.2, finalParticleType, finalParticleCount);
            }
            for (double y = finalMinY; y <= finalMaxY; y += finalDelta) {
                displayParticle(world, finalMinX - 0.2, y, finalMinZ - 0.2, finalParticleType, finalParticleCount);
                displayParticle(world, finalMaxX + 0.2, y, finalMinZ - 0.2, finalParticleType, finalParticleCount);
                displayParticle(world, finalMinX - 0.2, y, finalMaxZ + 0.2, finalParticleType, finalParticleCount);
                displayParticle(world, finalMaxX + 0.2, y, finalMaxZ + 0.2, finalParticleType, finalParticleCount);
            }
            for (double z = finalMinZ - 0.2; z <= finalMaxZ + 0.2; z += finalDelta) {
                displayParticle(world, finalMinX - 0.2, finalMinY, z, finalParticleType, finalParticleCount);
                displayParticle(world, finalMaxX + 0.2, finalMinY, z, finalParticleType, finalParticleCount);
                displayParticle(world, finalMinX - 0.2, finalMaxY, z, finalParticleType, finalParticleCount);
                displayParticle(world, finalMaxX + 0.2, finalMaxY, z, finalParticleType, finalParticleCount);
            }
        }, 0L, 5L).getTaskId(); // 每 5 个 ticks (0.25 秒) 刷新一次粒子效果

        // 存储任务 ID，以便后续清除
        boundaryTaskIds.put(centroid, taskId);

        // 设置定时器，在指定时间后停止显示粒子效果
        final int finalTaskId = taskId; // 声明为 final
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (finalBoundaryTaskIds.containsKey(finalCentroid) && finalBoundaryTaskIds.get(finalCentroid) == finalTaskId) {
                Bukkit.getScheduler().cancelTask(finalTaskId);
                finalBoundaryTaskIds.remove(finalCentroid);
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("[ClusterSelectionListener] displayClusterBoundary：粒子效果已停止。");
                }
            }
        }, durationTicks);
    }

    // 在指定位置显示粒子效果
    private void displayParticle(World world, double x, double y, double z, Particle particle, int count) {
        world.spawnParticle(particle, x, y, z, count, 0.1, 0.1, 0.1, 0.01);
        if (plugin.getConfigManager().isDebugMode()) {
            plugin.getLogger().info("[ClusterSelectionListener]   生成粒子 " + particle + "，位置：x=" + x + ", y=" + y + ", z=" + z);
        }
    }
}