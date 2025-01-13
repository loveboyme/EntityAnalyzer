package cn.panda.entityanalyzer.listener;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
import cn.panda.entityanalyzer.kmeans.Point;
import cn.panda.entityanalyzer.util.ParticleDisplay;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
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

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClusterSelectionListener implements Listener {

    private final EntityAnalyzerPlugin plugin;
    private final Map<Point, List<Entity>> clusterEntities = new WeakHashMap<>();
    private final ParticleDisplay particleDisplay;
    private Point currentDisplayingCluster = null; // 用于跟踪当前正在显示的聚类

    public ClusterSelectionListener(EntityAnalyzerPlugin plugin) {
        this.plugin = plugin;
        this.particleDisplay = plugin.getParticleDisplay();
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
        clearPreviousEffects(); // 清除之前显示的聚类效果
        currentDisplayingCluster = null;
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

        if (meta != null && meta.hasDisplayName()) {
            String displayName = ChatColor.stripColor(meta.getDisplayName());
            Pattern pattern = Pattern.compile("\\d+"); // 匹配一个或多个数字
            Matcher matcher = pattern.matcher(displayName);

            if (matcher.find()) {
                try {
                    int clusterIndex = Integer.parseInt(matcher.group()) - 1;

                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("[onInventoryClick] 点击了分析结果物品: " + meta.getDisplayName() + ", 解析的聚类索引 (0-based): " + clusterIndex);
                    }

                    int i = 0;
                    Point targetCentroid = null;
                    for (Point centroid : clusterEntities.keySet()) {
                        if (i == clusterIndex) {
                            targetCentroid = centroid;
                            break;
                        }
                        i++;
                    }

                    if (targetCentroid != null) {

                        if (plugin.getConfigManager().isDebugMode()) {
                            plugin.getLogger().info("[onInventoryClick] 准备传送玩家 " + player.getName() + " 到 " + targetCentroid);
                        }

                        Location teleportLocation = new Location(player.getWorld(), targetCentroid.getX(), player.getLocation().getY(), targetCentroid.getZ());
                        player.teleport(teleportLocation);
                        player.sendMessage(plugin.getMessageManager().getMessage("teleport-to-area"));
                        highlightEntities(player, targetCentroid);
                        displayClusterBoundary(player, targetCentroid);
                        currentDisplayingCluster = targetCentroid; // 更新当前显示的聚类
                    } else {
                        plugin.getLogger().warning("[onInventoryClick] 未找到对应的聚类中心点。");
                        player.sendMessage(plugin.getMessageManager().getMessage("cluster-info-not-found"));
                    }

                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("[onInventoryClick] 解析聚类索引失败，从 " + displayName + " 中提取的数字: " + matcher.group());
                    player.sendMessage(plugin.getMessageManager().getMessage("invalid-cluster-info-format"));
                }
            } else {
                plugin.getLogger().warning("[onInventoryClick] 未在物品名称中找到聚类索引: " + displayName);
                player.sendMessage(plugin.getMessageManager().getMessage("invalid-cluster-info-format"));
            }
        }
    }

    // 清除之前显示的粒子效果和高亮
    private void clearPreviousEffects() {
        if (currentDisplayingCluster != null) {
            // 清除粒子效果
            particleDisplay.clearBoundary(currentDisplayingCluster);

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
        } else {
            particleDisplay.clearAllBoundaries();
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

        if (plugin.getConfigManager().isGlowingEnabled()) {
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
                        ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) plugin.getConfigManager().getGlowingDuration(), 0));
                        if (plugin.getConfigManager().isDebugMode()) {
                            plugin.getLogger().info("[ClusterSelectionListener]     生物添加发光效果，持续 " + plugin.getConfigManager().getGlowingDuration() + " ticks");
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
            }, plugin.getConfigManager().getGlowingDuration());
        }
    }

    // 显示指定聚类区域的边界粒子效果
    private void displayClusterBoundary(Player player, Point centroid) {
        if (plugin.getConfigManager().isParticleEnabled()) {
            if (!clusterEntities.containsKey(centroid)) {
                plugin.getLogger().warning("[ClusterSelectionListener] displayClusterBoundary：找不到中心点 " + centroid + " 对应的聚类");
                return;
            }
            List<Entity> entitiesInCluster = clusterEntities.get(centroid);
            particleDisplay.displayClusterBoundary(player, centroid, entitiesInCluster);
        }
    }
}