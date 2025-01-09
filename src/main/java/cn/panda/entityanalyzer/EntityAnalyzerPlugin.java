package cn.panda.entityanalyzer;

import cn.panda.entityanalyzer.command.AnalyzeEntitiesCommand;
import cn.panda.entityanalyzer.listener.ClusterSelectionListener;
import org.bukkit.plugin.java.JavaPlugin;

public class EntityAnalyzerPlugin extends JavaPlugin {

    private ClusterSelectionListener clusterSelectionListener;

    @Override
    public void onEnable() {
        getLogger().info("实体分析插件已启用!"); // 中文日志消息
        // 注册命令
        AnalyzeEntitiesCommand analyzeEntitiesCommand = new AnalyzeEntitiesCommand(this);
        getCommand("analyzeentities").setExecutor(analyzeEntitiesCommand);

        // 注册监听器
        clusterSelectionListener = new ClusterSelectionListener(this);
        getServer().getPluginManager().registerEvents(clusterSelectionListener, this);
    }

    public ClusterSelectionListener getClusterSelectionListener() {
        return clusterSelectionListener;
    }

    @Override
    public void onDisable() {
        getLogger().info("实体分析插件已禁用!"); // 中文日志消息
    }
}