package cn.panda.entityanalyzer.task;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
import cn.panda.entityanalyzer.kmeans.KMeans;
import cn.panda.entityanalyzer.kmeans.KMeansResult;
import cn.panda.entityanalyzer.kmeans.Point;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KMeansAnalysisTask extends BukkitRunnable {

    private final EntityAnalyzerPlugin plugin;
    private final World world;
    private int k;
    private final Player player;
    private Map<Point, List<Entity>> assignmentsWithEntities; // 存储包含实体的聚类结果

    public KMeansAnalysisTask(EntityAnalyzerPlugin plugin, World world, int k, Player player) {
        this.plugin = plugin;
        this.world = world;
        this.k = k;
        this.player = player;
    }

    @Override
    public void run() {
        // 获取世界中所有的非玩家生物实体
        List<Entity> entities = world.getLivingEntities().stream() // 使用 getLivingEntities() 包括更多类型的生物
                .filter(entity -> !(entity instanceof Player))
                .collect(Collectors.toList());

        if (entities.isEmpty()) {
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(plugin.getMessageManager().getMessage("no-entities-to-analyze")));
            return;
        }

        // 如果 k 为 -1，则设置为实体数量，但不小于 1
        if (k == -1) {
            k = Math.max(1, entities.size());
        }

        final int finalK = k; // 确保 k 在 lambda 表达式中是 final 的

        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(plugin.getMessageManager().getMessage("analyzing-entities")));
        KMeans kMeans = new KMeans(entities, finalK);
        KMeansResult result = kMeans.run(100);
        Map<Point, List<Point>> assignments = result.getAssignments();

        // 将实体关联到聚类中心
        assignmentsWithEntities = new HashMap<>();
        for (Map.Entry<Point, List<Point>> entry : assignments.entrySet()) {
            Point centroid = entry.getKey();
            List<Point> assignedPoints = entry.getValue();
            List<Entity> entitiesInCluster = new ArrayList<>();
            for (Entity entity : entities) {
                Point entityPoint = new Point(entity.getLocation().getX(), entity.getLocation().getZ());
                // 判断实体是否属于当前聚类 (之前被分配到该聚类)
                if (assignedPoints.contains(entityPoint)) {
                    entitiesInCluster.add(entity);
                }
            }
            assignmentsWithEntities.put(centroid, entitiesInCluster);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            player.sendMessage(plugin.getMessageManager().getMessage("analysis-complete"));

            int numClusters = assignmentsWithEntities.size();
            int inventorySize = Math.min(54, (numClusters / 9 + (numClusters % 9 == 0 ? 0 : 1)) * 9);
            Inventory gui = Bukkit.createInventory(null, inventorySize, plugin.getMessageManager().getMessage("analysis-result-title"));

            int slot = 0;
            Material clusterItemMaterial = plugin.getConfigManager().getClusterItemMaterial();
            for (Map.Entry<Point, List<Entity>> entry : assignmentsWithEntities.entrySet()) {
                Point centroid = entry.getKey();
                List<Entity> entitiesInCluster = entry.getValue();
                int entityCount = entitiesInCluster.size();

                if (entityCount > 0 && slot < inventorySize) { // 关键修改：添加边界检查
                    ItemStack item = new ItemStack(clusterItemMaterial);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(plugin.getMessageManager().getMessage("cluster-item-name", "%index%", String.valueOf(slot + 1)));
                    List<String> lore = new ArrayList<>();
                    lore.add(plugin.getMessageManager().getMessage("cluster-item-lore-count", "%count%", String.valueOf(entityCount)));
                    lore.add(plugin.getMessageManager().getMessage("cluster-item-lore-coords",
                            "%x%", String.format("%.0f", centroid.getX()),
                            "%z%", String.format("%.0f", centroid.getZ())));
                    lore.add(plugin.getMessageManager().getMessage("cluster-item-lore-action"));
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                    gui.setItem(slot++, item);
                }
            }
            player.openInventory(gui);
            // 将包含实体的聚类结果传递给监听器
            plugin.getClusterSelectionListener().setClusterEntities(assignmentsWithEntities);
        });
    }

    public Map<Point, List<Entity>> getAssignmentsWithEntities() {
        return assignmentsWithEntities;
    }
}