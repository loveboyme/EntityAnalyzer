package cn.panda.entityanalyzer.command;

import cn.panda.entityanalyzer.EntityAnalyzerPlugin;
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
            sender.sendMessage(plugin.getMessageManager().getMessage("only-players-execute"));
            return true;
        }

        Player player = (Player) sender;
        int k = plugin.getConfigManager().getDefaultClusterCount();

        if (args.length > 0) {
            try {
                k = Integer.parseInt(args[0]);
                if (k <= 0) {
                    player.sendMessage(plugin.getMessageManager().getMessage("invalid-cluster-count"));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessageManager().getMessage("invalid-cluster-count-format"));
                return true;
            }
        }

        player.sendMessage(plugin.getMessageManager().getMessage("analyzing-entities"));
        new cn.panda.entityanalyzer.task.KMeansAnalysisTask(plugin, player.getWorld(), k, player).runTaskAsynchronously(plugin);

        return true;
    }
}