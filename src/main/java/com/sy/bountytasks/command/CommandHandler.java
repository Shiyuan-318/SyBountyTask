package com.sy.bountytasks.command;

import com.sy.bountytasks.SyBountyTask;
import com.sy.bountytasks.gui.GUIManager;
import com.sy.bountytasks.task.BountyTask;
import com.sy.bountytasks.task.TaskType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CommandHandler {
    private final SyBountyTask plugin;
    private final GUIManager guiManager;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final int BROADCAST_COOLDOWN = 60;

    public CommandHandler(SyBountyTask plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    public void handleOpen(Player player) {
        guiManager.openMainGUI(player);
    }

    public void handleCreateTask(Player player, int typeId, String title, String details, double reward) {
        TaskType type = TaskType.fromId(typeId);
        if (type == null) {
            player.sendMessage(ChatColor.RED + "无效的任务类型！类型: 1-物资 2-建造 3-打怪");
            return;
        }

        if (title.length() > 50) {
            player.sendMessage(ChatColor.RED + "任务标题过长！最多50个字符。");
            return;
        }

        if (details.length() > 200) {
            player.sendMessage(ChatColor.RED + "任务详情过长！最多200个字符。");
            return;
        }

        if (reward <= 0) {
            player.sendMessage(ChatColor.RED + "报酬必须大于0！");
            return;
        }

        int maxDailyTasks = plugin.getConfig().getInt("max-daily-tasks", 10);
        int todayCount = plugin.getDatabaseManager().getTodayTaskCount(player.getUniqueId());
        if (todayCount >= maxDailyTasks) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.max-tasks-reached", "")));
            return;
        }

        double serviceFeeRate = plugin.getConfig().getDouble("service-fee", 0.05);
        double serviceFee = reward * serviceFeeRate;
        double totalCost = reward + serviceFee;

        if (!plugin.getEconomyManager().hasEnough(player, totalCost)) {
            String msg = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.not-enough-money", ""));
            player.sendMessage(msg.replace("%amount%", plugin.getEconomyManager().format(totalCost)));
            return;
        }

        plugin.getEconomyManager().withdraw(player, totalCost);

        BountyTask task = new BountyTask(0, player.getUniqueId(), player.getName(),
                type, title, details, reward, serviceFee);

        int taskId = plugin.getDatabaseManager().createTask(task);
        if (taskId > 0) {
            String msg = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-created", ""));
            player.sendMessage(msg.replace("%id%", String.valueOf(taskId)));
            player.sendMessage(ChatColor.GRAY + "已扣除: " + ChatColor.GOLD + plugin.getEconomyManager().format(totalCost) +
                    ChatColor.GRAY + " (报酬: " + ChatColor.GOLD + plugin.getEconomyManager().format(reward) +
                    ChatColor.GRAY + " + 服务费: " + ChatColor.GOLD + plugin.getEconomyManager().format(serviceFee) + ")");
        } else {
            plugin.getEconomyManager().deposit(player, totalCost);
            player.sendMessage(ChatColor.RED + "任务创建失败！请联系管理员。");
        }
    }

    public void handleGetTask(Player player, int taskId) {
        BountyTask task = plugin.getDatabaseManager().getTask(taskId);
        if (task == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-not-found", "")));
            return;
        }

        if (task.getStatus() != BountyTask.TaskStatus.PENDING) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-already-claimed", "")));
            return;
        }

        if (task.getPublisherUuid().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你不能认领自己发布的任务！");
            return;
        }

        task.setStatus(BountyTask.TaskStatus.CLAIMED);
        task.setClaimerUuid(player.getUniqueId());
        task.setClaimerName(player.getName());

        if (plugin.getDatabaseManager().updateTask(task)) {
            String msg = ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-claimed", ""));
            player.sendMessage(msg.replace("%title%", task.getTitle()));

            Player publisher = Bukkit.getPlayer(task.getPublisherUuid());
            if (publisher != null && publisher.isOnline()) {
                publisher.sendMessage(ChatColor.GREEN + player.getName() + " 已认领了你的任务: " + ChatColor.YELLOW + task.getTitle());
            }
        } else {
            player.sendMessage(ChatColor.RED + "认领任务失败！请稍后再试。");
        }
    }

    public void handleBroadcast(Player player, int taskId) {
        long now = System.currentTimeMillis() / 1000;
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse != null && (now - lastUse) < BROADCAST_COOLDOWN) {
            int remaining = (int) (BROADCAST_COOLDOWN - (now - lastUse));
            player.sendMessage(ChatColor.RED + "广播冷却中，请等待 " + remaining + " 秒后再试。");
            return;
        }

        BountyTask task = plugin.getDatabaseManager().getTask(taskId);
        if (task == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-not-found", "")));
            return;
        }

        if (!task.getPublisherUuid().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你只能广播自己发布的任务！");
            return;
        }

        if (task.getStatus() != BountyTask.TaskStatus.PENDING) {
            player.sendMessage(ChatColor.RED + "只能广播未被认领的任务！");
            return;
        }

        cooldowns.put(player.getUniqueId(), now);

        String format = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("messages.broadcast-format", ""));
        String message = format
                .replace("%player%", player.getName())
                .replace("%title%", task.getTitle())
                .replace("%id%", String.valueOf(task.getId()));

        Component component = LegacyComponentSerializer.legacySection().deserialize(message)
                .clickEvent(ClickEvent.runCommand("/sybt get " + task.getId()))
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(
                        ChatColor.YELLOW + "点击认领此任务")));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.sendMessage(component);
        }
    }

    public void handleCompleteTask(Player player, int taskId) {
        BountyTask activeTask = plugin.getDatabaseManager().getTask(taskId);
        
        if (activeTask == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-not-found", "")));
            return;
        }

        if (!player.getUniqueId().equals(activeTask.getClaimerUuid())) {
            player.sendMessage(ChatColor.RED + "这不是你认领的任务！");
            return;
        }

        if (activeTask.getStatus() != BountyTask.TaskStatus.CLAIMED) {
            player.sendMessage(ChatColor.RED + "此任务当前状态无法提交完成！");
            return;
        }

        if (activeTask.getType() == TaskType.MATERIAL) {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand.getType() == Material.AIR) {
                player.sendMessage(ChatColor.RED + "请手持需要提交的物资！");
                return;
            }
            
            player.sendMessage(ChatColor.GOLD + "========== 确认提交物资 ==========");
            player.sendMessage(ChatColor.YELLOW + "任务: " + ChatColor.WHITE + activeTask.getTitle());
            player.sendMessage(ChatColor.YELLOW + "手持物品: " + ChatColor.AQUA + mainHand.getAmount() + "x " + mainHand.getType().name());
            player.sendMessage(ChatColor.GRAY + "请确认你要提交手中的物品");
            
            Component confirmButton = LegacyComponentSerializer.legacySection()
                    .deserialize(ChatColor.GREEN + "[提交手持物品]")
                    .clickEvent(ClickEvent.runCommand("/sybt submit " + activeTask.getId()))
                    .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection()
                            .deserialize(ChatColor.YELLOW + "点击确认提交手中物品")));
            player.sendMessage(confirmButton);
            player.sendMessage(ChatColor.GOLD + "==================================");
            return;
        }

        completeTask(player, activeTask);
    }

    public void handleSubmitMaterial(Player player, int taskId) {
        BountyTask activeTask = plugin.getDatabaseManager().getTask(taskId);
        
        if (activeTask == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-not-found", "")));
            return;
        }

        if (!player.getUniqueId().equals(activeTask.getClaimerUuid())) {
            player.sendMessage(ChatColor.RED + "这不是你认领的任务！");
            return;
        }

        if (activeTask.getStatus() != BountyTask.TaskStatus.CLAIMED) {
            player.sendMessage(ChatColor.RED + "此任务当前状态无法提交完成！");
            return;
        }

        if (activeTask.getType() != TaskType.MATERIAL) {
            player.sendMessage(ChatColor.RED + "此指令仅用于物资任务！");
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() == Material.AIR) {
            player.sendMessage(ChatColor.RED + "请手持需要提交的物资！");
            return;
        }

        completeTask(player, activeTask);
    }

    private void completeTask(Player player, BountyTask activeTask) {
        activeTask.setStatus(BountyTask.TaskStatus.COMPLETED);
        if (plugin.getDatabaseManager().updateTask(activeTask)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-completed", "")));

            Player publisher = Bukkit.getPlayer(activeTask.getPublisherUuid());
            if (publisher != null && publisher.isOnline()) {
                String msgTemplate = switch (activeTask.getType()) {
                    case MATERIAL -> plugin.getConfig().getString("messages.material-complete", "");
                    case BUILD -> plugin.getConfig().getString("messages.build-complete", "");
                    case MONSTER -> plugin.getConfig().getString("messages.monster-complete", "");
                };
                
                String msg = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.prefix", "") + msgTemplate);
                msg = msg.replace("%player%", player.getName()).replace("%title%", activeTask.getTitle());

                Component component = LegacyComponentSerializer.legacySection().deserialize(msg);
                
                Component confirmButton = LegacyComponentSerializer.legacySection()
                        .deserialize(ChatColor.GREEN + "[点击确认验收]")
                        .clickEvent(ClickEvent.runCommand("/sybt confirm " + activeTask.getId()))
                        .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection()
                                .deserialize(ChatColor.YELLOW + "点击确认任务完成并发放报酬")));

                publisher.sendMessage(component);
                publisher.sendMessage(confirmButton);
            }
        } else {
            player.sendMessage(ChatColor.RED + "提交任务失败！请稍后再试。");
        }
    }

    public void handleConfirmTask(Player player, int taskId) {
        BountyTask task = plugin.getDatabaseManager().getTask(taskId);
        if (task == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-not-found", "")));
            return;
        }

        if (!task.getPublisherUuid().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-not-yours", "")));
            return;
        }

        if (task.getStatus() != BountyTask.TaskStatus.COMPLETED) {
            player.sendMessage(ChatColor.RED + "此任务尚未提交验收！");
            return;
        }

        if (task.getType() == TaskType.MATERIAL) {
            Player claimer = Bukkit.getPlayer(task.getClaimerUuid());
            if (claimer != null && claimer.isOnline()) {
                ItemStack mainHand = claimer.getInventory().getItemInMainHand();
                if (mainHand.getType() != Material.AIR) {
                    player.getInventory().addItem(mainHand.clone());
                    mainHand.setAmount(0);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            plugin.getConfig().getString("messages.prefix", "") +
                                    plugin.getConfig().getString("messages.received-material", "")));
                }
            }
        }

        task.setStatus(BountyTask.TaskStatus.CONFIRMED);
        if (plugin.getDatabaseManager().updateTask(task)) {
            Player claimer = Bukkit.getPlayer(task.getClaimerUuid());
            if (claimer != null && claimer.isOnline()) {
                plugin.getEconomyManager().deposit(claimer, task.getReward());
                String msg = ChatColor.translateAlternateColorCodes('&',
                        plugin.getConfig().getString("messages.prefix", "") +
                                plugin.getConfig().getString("messages.received-reward", ""));
                claimer.sendMessage(msg.replace("%amount%", plugin.getEconomyManager().format(task.getReward())));
            }

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-confirmed", "")));
        } else {
            player.sendMessage(ChatColor.RED + "确认任务失败！请稍后再试。");
        }
    }

    public void handleDeleteTask(Player player, int taskId) {
        BountyTask task = plugin.getDatabaseManager().getTask(taskId);
        if (task == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-not-found", "")));
            return;
        }

        if (!task.getPublisherUuid().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-not-yours", "")));
            return;
        }

        if (task.getStatus() != BountyTask.TaskStatus.PENDING) {
            player.sendMessage(ChatColor.RED + "只能删除未被认领的任务！");
            return;
        }

        double refund = task.getReward() + task.getServiceFee();
        if (plugin.getDatabaseManager().deleteTask(taskId)) {
            plugin.getEconomyManager().deposit(player, refund);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("messages.prefix", "") +
                            plugin.getConfig().getString("messages.task-deleted", "")));
            player.sendMessage(ChatColor.GRAY + "已退还: " + ChatColor.GOLD + plugin.getEconomyManager().format(refund));
        } else {
            player.sendMessage(ChatColor.RED + "删除任务失败！请稍后再试。");
        }
    }
}
