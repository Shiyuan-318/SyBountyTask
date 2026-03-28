package com.sy.bountytasks.gui;

import com.sy.bountytasks.SyBountyTask;
import com.sy.bountytasks.task.BountyTask;
import com.sy.bountytasks.task.TaskType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {
    private final SyBountyTask plugin;
    private final GUIManager guiManager;

    public GUIListener(SyBountyTask plugin, GUIManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GUIManager.GUIHolder holder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        int slot = event.getSlot();
        String type = holder.getType();

        switch (type) {
            case "main" -> handleMainGUIClick(player, slot);
            case "task_list" -> handleTaskListGUIClick(player, slot, holder.getTaskType());
            case "my_tasks" -> handleMyTasksGUIClick(player, slot);
        }
    }

    private void handleMainGUIClick(Player player, int slot) {
        switch (slot) {
            case 11 -> guiManager.openTaskListGUI(player, TaskType.MATERIAL, 0);
            case 13 -> guiManager.openTaskListGUI(player, TaskType.BUILD, 0);
            case 15 -> guiManager.openTaskListGUI(player, TaskType.MONSTER, 0);
            case 29 -> guiManager.openMyTasksGUI(player, 0);
            case 33 -> guiManager.openTaskListGUI(player, null, 0);
            case 49 -> player.sendMessage(ChatColor.YELLOW + "使用指令发布任务: /sybt task <类型> <标题> <详情> <报酬>");
        }
    }

    private void handleTaskListGUIClick(Player player, int slot, TaskType filterType) {
        int currentPage = guiManager.getPlayerPage(player.getName());

        if (slot == 45 && currentPage > 0) {
            guiManager.openTaskListGUI(player, filterType, currentPage - 1);
            return;
        }

        if (slot == 53) {
            guiManager.openTaskListGUI(player, filterType, currentPage + 1);
            return;
        }

        if (slot == 49) {
            guiManager.openMainGUI(player);
            return;
        }

        if (slot < 45) {
            java.util.List<BountyTask> tasks;
            if (filterType != null) {
                tasks = plugin.getDatabaseManager().getTasksByType(filterType);
            } else {
                tasks = plugin.getDatabaseManager().getPendingTasks();
            }

            int taskIndex = currentPage * 45 + slot;
            if (taskIndex < tasks.size()) {
                BountyTask task = tasks.get(taskIndex);
                if (task.getStatus() == BountyTask.TaskStatus.PENDING) {
                    plugin.getCommandHandler().handleGetTask(player, task.getId());
                } else {
                    guiManager.sendTaskInfo(player, task);
                }
            }
        }
    }

    private void handleMyTasksGUIClick(Player player, int slot) {
        int currentPage = guiManager.getPlayerPage(player.getName());

        if (slot == 45 && currentPage > 0) {
            guiManager.openMyTasksGUI(player, currentPage - 1);
            return;
        }

        if (slot == 53) {
            guiManager.openMyTasksGUI(player, currentPage + 1);
            return;
        }

        if (slot == 49) {
            guiManager.openMainGUI(player);
            return;
        }

        if (slot < 45) {
            java.util.List<BountyTask> publishedTasks = plugin.getDatabaseManager().getTasksByPublisher(player.getUniqueId());
            java.util.List<BountyTask> claimedTasks = plugin.getDatabaseManager().getTasksByClaimer(player.getUniqueId());

            java.util.List<BountyTask> allTasks = new java.util.ArrayList<>();
            allTasks.addAll(publishedTasks);
            allTasks.addAll(claimedTasks);

            int taskIndex = currentPage * 45 + slot;
            if (taskIndex < allTasks.size()) {
                BountyTask task = allTasks.get(taskIndex);
                guiManager.sendTaskInfo(player, task);
            }
        }
    }
}
