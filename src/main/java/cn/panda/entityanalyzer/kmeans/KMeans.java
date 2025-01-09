package cn.panda.entityanalyzer.kmeans;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.*;
import java.util.stream.Collectors;

public class KMeans {

    private final List<Point> points; // 数据点集合
    private final int k; // 聚类数量
    private List<Point> centroids; // 质心集合

    public KMeans(List<Entity> entities, int k) {
        this.points = entities.stream()
                .map(entity -> new Point(entity.getLocation().getX(), entity.getLocation().getZ())) // 将实体位置转换为二维点
                .collect(Collectors.toList());
        this.k = k;
        this.centroids = initializeCentroids(); // 初始化质心
    }

    // 初始化质心：随机选择 k 个数据点作为初始质心
    private List<Point> initializeCentroids() {
        List<Point> initialCentroids = new ArrayList<>();
        Random random = new Random();
        Set<Integer> chosenIndices = new HashSet<>();
        for (int i = 0; i < k; i++) {
            int randomIndex;
            do {
                randomIndex = random.nextInt(points.size());
            } while (chosenIndices.contains(randomIndex)); // 确保选择不重复的点
            chosenIndices.add(randomIndex);
            initialCentroids.add(points.get(randomIndex));
        }
        return initialCentroids;
    }

    // 运行 K-means 算法
    public KMeansResult run(int maxIterations) {
        Map<Point, List<Point>> assignments = new HashMap<>(); // 存储每个质心分配到的数据点
        for (int i = 0; i < maxIterations; i++) {
            assignments.clear(); // 清空上一次的分配结果

            // 分配点到最近的质心
            for (Point point : points) {
                Point closestCentroid = null;
                double minDistance = Double.MAX_VALUE;
                for (Point centroid : centroids) {
                    double distance = DistanceCalculator.euclideanDistance(point, centroid); // 计算欧氏距离
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestCentroid = centroid;
                    }
                }
                assignments.computeIfAbsent(closestCentroid, k -> new ArrayList<>()).add(point); // 将点分配给最近的质心
            }

            // 更新质心
            List<Point> newCentroids = new ArrayList<>();
            for (Point centroid : centroids) {
                if (assignments.containsKey(centroid)) {
                    List<Point> assignedPoints = assignments.get(centroid);
                    if (!assignedPoints.isEmpty()) {
                        double avgX = assignedPoints.stream().mapToDouble(Point::getX).average().orElse(centroid.getX());
                        double avgZ = assignedPoints.stream().mapToDouble(Point::getZ).average().orElse(centroid.getZ());
                        newCentroids.add(new Point(avgX, avgZ)); // 计算新的质心位置
                    } else {
                        // 如果没有分配任何点，则质心保持不变
                        newCentroids.add(centroid);
                    }
                } else {
                    // 如果一个质心没有分配到任何点，则质心保持不变
                    newCentroids.add(centroid);
                }
            }

            // 检查质心是否发生变化，如果没有变化则认为收敛
            if (centroids.equals(newCentroids)) {
                break;
            }
            centroids = newCentroids; // 更新质心
        }
        return new KMeansResult(centroids, assignments); // 返回聚类结果
    }
}