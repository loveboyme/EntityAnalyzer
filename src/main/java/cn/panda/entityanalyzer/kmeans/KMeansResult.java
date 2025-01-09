package cn.panda.entityanalyzer.kmeans;

import java.util.List;
import java.util.Map;

public class KMeansResult {
    private final List<Point> centroids; // 质心集合
    private final Map<Point, List<Point>> assignments; // 每个质心分配到的数据点

    public KMeansResult(List<Point> centroids, Map<Point, List<Point>> assignments) {
        this.centroids = centroids;
        this.assignments = assignments;
    }

    public List<Point> getCentroids() {
        return centroids;
    }

    public Map<Point, List<Point>> getAssignments() {
        return assignments;
    }
}