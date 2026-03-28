package com.sy.bountytasks.command;

import com.sy.bountytasks.SyBountyTask;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SyBTCommand implements CommandExecutor, TabCompleter {
    private final SyBountyTask plugin;
    private final CommandHandler commandHandler;

    public SyBTCommand(SyBountyTask plugin, CommandHandler commandHandler) {
        this.plugin = plugin;
        this.commandHandler = commandHandler;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "此命令只能由玩家执行！");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "open" -> {
                if (!player.hasPermission("sybt.use")) {
                    player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                commandHandler.handleOpen(player);
            }
            case "task" -> {
                if (!player.hasPermission("sybt.use")) {
                    player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                if (args.length < 5) {
                    player.sendMessage(ChatColor.RED + "用法: /sybt task <类型> <标题> <详情> <报酬>");
                    player.sendMessage(ChatColor.GRAY + "类型: 1-物资 2-建造 3-打怪");
                    return true;
                }
                try {
                    int type = Integer.parseInt(args[1]);
                    String title = args[2];
                    String details = args[3];
                    double reward = Double.parseDouble(args[4]);
                    commandHandler.handleCreateTask(player, type, title, details, reward);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "类型和报酬必须是数字！");
                }
            }
            case "get" -> {
                if (!player.hasPermission("sybt.use")) {
                    player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "用法: /sybt get <任务ID>");
                    return true;
                }
                try {
                    int taskId = Integer.parseInt(args[1]);
                    commandHandler.handleGetTask(player, taskId);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "任务ID必须是数字！");
                }
            }
            case "br" -> {
                if (!player.hasPermission("sybt.use")) {
                    player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "用法: /sybt br <任务ID>");
                    return true;
                }
                try {
                    int taskId = Integer.parseInt(args[1]);
                    commandHandler.handleBroadcast(player, taskId);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "任务ID必须是数字！");
                }
            }
            case "complete" -> {
                if (!player.hasPermission("sybt.use")) {
                    player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "用法: /sybt complete <任务ID>");
                    return true;
                }
                try {
                    int taskId = Integer.parseInt(args[1]);
                    commandHandler.handleCompleteTask(player, taskId);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "任务ID必须是数字！");
                }
            }
            case "submit" -> {
                if (!player.hasPermission("sybt.use")) {
                    player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "用法: /sybt submit <任务ID>");
                    return true;
                }
                try {
                    int taskId = Integer.parseInt(args[1]);
                    commandHandler.handleSubmitMaterial(player, taskId);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "任务ID必须是数字！");
                }
            }
            case "confirm" -> {
                if (!player.hasPermission("sybt.use")) {
                    player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "用法: /sybt confirm <任务ID>");
                    return true;
                }
                try {
                    int taskId = Integer.parseInt(args[1]);
                    commandHandler.handleConfirmTask(player, taskId);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "任务ID必须是数字！");
                }
            }
            case "delete" -> {
                if (!player.hasPermission("sybt.use")) {
                    player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "用法: /sybt delete <任务ID>");
                    return true;
                }
                try {
                    int taskId = Integer.parseInt(args[1]);
                    commandHandler.handleDeleteTask(player, taskId);
                } catch (NumberFormatException e) {
                    player.sendMessage(ChatColor.RED + "任务ID必须是数字！");
                }
            }
            case "reload" -> {
                if (!player.hasPermission("sybt.admin")) {
                    player.sendMessage(ChatColor.RED + "你没有权限使用此命令！");
                    return true;
                }
                plugin.reloadConfig();
                player.sendMessage(ChatColor.GREEN + "配置文件已重新加载！");
            }
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "========== SyBountyTask 帮助 ==========");
        player.sendMessage(ChatColor.YELLOW + "/sybt open " + ChatColor.GRAY + "- 打开任务中心GUI");
        player.sendMessage(ChatColor.YELLOW + "/sybt task <类型> <标题> <详情> <报酬> " + ChatColor.GRAY + "- 发布任务");
        player.sendMessage(ChatColor.GRAY + "   类型: 1-物资 2-建造 3-打怪");
        player.sendMessage(ChatColor.YELLOW + "/sybt get <任务ID> " + ChatColor.GRAY + "- 认领任务");
        player.sendMessage(ChatColor.YELLOW + "/sybt br <任务ID> " + ChatColor.GRAY + "- 广播宣传任务");
        player.sendMessage(ChatColor.YELLOW + "/sybt complete <任务ID> " + ChatColor.GRAY + "- 提交任务完成");
        player.sendMessage(ChatColor.YELLOW + "/sybt submit <任务ID> " + ChatColor.GRAY + "- 确认提交物资");
        player.sendMessage(ChatColor.YELLOW + "/sybt confirm <任务ID> " + ChatColor.GRAY + "- 确认验收任务");
        player.sendMessage(ChatColor.YELLOW + "/sybt delete <任务ID> " + ChatColor.GRAY + "- 删除未认领的任务");
        if (player.hasPermission("sybt.admin")) {
            player.sendMessage(ChatColor.RED + "/sybt reload " + ChatColor.GRAY + "- 重载配置文件");
        }
        player.sendMessage(ChatColor.GOLD + "=====================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("open", "task", "get", "br", "complete", "submit", "confirm", "delete");
            if (sender.hasPermission("sybt.admin")) {
                subCommands = new ArrayList<>(subCommands);
                subCommands.add("reload");
            }
            for (String subCmd : subCommands) {
                if (subCmd.startsWith(args[0].toLowerCase())) {
                    completions.add(subCmd);
                }
            }
        } else if (args.length == 2) {
            String subCmd = args[0].toLowerCase();
            if (subCmd.equals("task")) {
                completions.addAll(Arrays.asList("1", "2", "3"));
            }
        }

        return completions;
    }
}
