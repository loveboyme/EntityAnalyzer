package cn.panda.entityanalyzer.task;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
import cn.panda.entityanalyzer.kmeans.KMeans;
import cn.panda.entityanalyzer.kmeans.KMeansResult;
import cn.panda.entityanalyzer.kmeans.Point;
import org.bukkit.Bukkit;
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

    public KMeansAnalysisTask(EntityAnalyzerPlugin plugin, World world, int k, Player player) {
        this.plugin = plugin;
        this.world = world;
        this.k = k;
        this.player = player;
    }

    @Override
    public void run() {
        final int finalK = k;

        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Entity> entities = world.getLivingEntities().stream()
                    .filter(entity -> !(entity instanceof Player))
                    .collect(Collectors.toList());

            if (entities.isEmpty()) {
                player.sendMessage(plugin.getMessageManager().getMessage("no-entities-to-analyze"));
                return;
            }

            int effectiveK = finalK;
            if (effectiveK == -1) {
                effectiveK = Math.max(1, entities.size());
            }

            final int finalEffectiveK = effectiveK;
            final List<Entity> finalEntities = entities;

            player.sendMessage(plugin.getMessageManager().getMessage("analyzing-entities"));

            new BukkitRunnable() {
                @Override
                public void run() {
                    KMeans kMeans = new KMeans(finalEntities, finalEffectiveK);
                    KMeansResult result = kMeans.run(100);

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        plugin.getClusterSelectionListener().setClusterEntities(convertToEntityClusterMap(result, finalEntities));
                        displayAnalysisResults(result, finalEntities);
                        player.sendMessage(plugin.getMessageManager().getMessage("analysis-complete"));
                    });
                }
            }.runTaskAsynchronously(plugin);
        });
    }

    private Map<Point, List<Entity>> convertToEntityClusterMap(KMeansResult result, List<Entity> allEntities) {
        Map<Point, List<Entity>> assignmentsWithEntities = new HashMap<>();
        Map<Point, List<Point>> assignments = result.getAssignments();
        for (Map.Entry<Point, List<Point>> entry : assignments.entrySet()) {
            Point centroid = entry.getKey();
            List<Point> assignedPoints = entry.getValue();
            List<Entity> entitiesInCluster = new ArrayList<>();
            for (Entity entity : allEntities) {
                Point entityPoint = new Point(entity.getLocation().getX(), entity.getLocation().getZ());
                if (assignedPoints.contains(entityPoint)) {
                    entitiesInCluster.add(entity);
                }
            }
            assignmentsWithEntities.put(centroid, entitiesInCluster);
        }
        return assignmentsWithEntities;
    }

    private void displayAnalysisResults(KMeansResult result, List<Entity> entities) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            int numClusters = result.getCentroids().size();
            int inventorySize = Math.min(54, (numClusters / 9 + (numClusters % 9 == 0 ? 0 : 1)) * 9);
            Inventory gui = Bukkit.createInventory(null, inventorySize, plugin.getMessageManager().getMessage("analysis-result-title"));

            int slot = 0;
            Material clusterItemMaterial = plugin.getConfigManager().getClusterItemMaterial();
            Map<Point, List<Entity>> assignmentsWithEntities = convertToEntityClusterMap(result, entities);
            for (Map.Entry<Point, List<Entity>> entry : assignmentsWithEntities.entrySet()) {
                Point centroid = entry.getKey();
                List<Entity> entitiesInCluster = entry.getValue();
                int entityCount = entitiesInCluster.size();

                if (entityCount > 0 && slot < inventorySize) {
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
        });
    }
}