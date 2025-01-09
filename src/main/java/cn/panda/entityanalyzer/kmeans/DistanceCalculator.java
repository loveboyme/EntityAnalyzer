package cn.panda.entityanalyzer.kmeans;

public class DistanceCalculator {

    // 计算两个点之间的欧氏距离
    public static double euclideanDistance(Point p1, Point p2) {
        double dx = p1.getX() - p2.getX();
        double dz = p1.getZ() - p2.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }
}