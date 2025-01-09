package cn.panda.entityanalyzer.command;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
import cn.panda.entityanalyzer.listener.ClusterSelectionListener;
import cn.panda.entityanalyzer.task.KMeansAnalysisTask;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AnalyzeEntitiesCommand implements CommandExecutor {

    private final EntityAnalyzerPlugin plugin;

    public AnalyzeEntitiesCommand(EntityAnalyzerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "§c此命令只能由玩家执行。");
            return true;
        }

        Player player = (Player) sender;

        int k = 5; // 默认聚类数量
        if (args.length > 0) {
            try {
                k = Integer.parseInt(args[0]);
                if (k <= 0) {
                    player.sendMessage(ChatColor.RED + "§c聚类数量必须大于 0。");
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "§c无效的聚类数量。请输入一个数字。");
                return true;
            }
        }

        player.sendMessage(ChatColor.YELLOW + "§e正在进行实体聚类分析，请稍候...");
        new KMeansAnalysisTask(plugin, player.getWorld(), k, player).runTaskAsynchronously(plugin);

        return true;
    }
}