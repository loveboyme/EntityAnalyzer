package cn.panda.entityanalyzer.task;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
import cn.panda.entityanalyzer.kmeans.DistanceCalculator;
import cn.panda.entityanalyzer.kmeans.KMeans;
import cn.panda.entityanalyzer.kmeans.KMeansResult;
import cn.panda.entityanalyzer.kmeans.Point;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
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
    private final int k;
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
            Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.YELLOW + "§e当前世界中没有可分析的实体。"));
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(ChatColor.YELLOW + "§e正在执行 K-means 聚类算法..."));
        KMeans kMeans = new KMeans(entities, k);
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
            player.sendMessage(ChatColor.GREEN + "§a实体聚类分析完成！");

            Inventory gui = Bukkit.createInventory(null, Math.min(54, (assignmentsWithEntities.size() / 9 + (assignmentsWithEntities.size() % 9 == 0 ? 0 : 1)) * 9), ChatColor.DARK_AQUA + "§3实体密度分析结果");

            int slot = 0;
            for (Map.Entry<Point, List<Entity>> entry : assignmentsWithEntities.entrySet()) {
                Point centroid = entry.getKey();
                List<Entity> entitiesInCluster = entry.getValue();
                int entityCount = entitiesInCluster.size();

                if (entityCount > 0) {
                    ItemStack item = new ItemStack(Material.COMPASS);
                    ItemMeta meta = item.getItemMeta();
                    meta.setDisplayName(ChatColor.AQUA + "§b高密度区域 #" + (slot + 1));
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "§7包含实体数量: " + ChatColor.YELLOW + entityCount);
                    lore.add(ChatColor.GRAY + "§7中心坐标: " + ChatColor.YELLOW + String.format("%.0f, %.0f", centroid.getX(), centroid.getZ()));
                    lore.add(ChatColor.GREEN + "§a点击传送并查看");
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