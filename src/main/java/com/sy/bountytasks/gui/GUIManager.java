package com.sy.bountytasks.gui;

import com.sy.bountytasks.SyBountyTask;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIManager {
    private final SyBountyTask plugin;
    private final Map<String, Integer> playerPageMap = new HashMap<>();
    private final Map<String, TaskType> playerFilterMap = new HashMap<>();

    public static final String MAIN_GUI_TITLE = "悬赏任务中心";
    public static final String TASK_LIST_GUI_TITLE = "任务列表 - ";
    public static final int GUI_SIZE = 54;

    public GUIManager(SyBountyTask plugin) {
        this.plugin = plugin;
    }

    public void openMainGUI(Player player) {
        Inventory gui = Bukkit.createInventory(new GUIHolder("main"), GUI_SIZE, MAIN_GUI_TITLE);

        ItemStack materialItem = createMenuItem(Material.DIAMOND, 
                ChatColor.AQUA + "物资任务",
                ChatColor.GRAY + "点击查看物资类任务");
        gui.setItem(11, materialItem);

        ItemStack buildItem = createMenuItem(Material.BRICKS,
                ChatColor.GOLD + "建造任务",
                ChatColor.GRAY + "点击查看建造类任务");
        gui.setItem(13, buildItem);

        ItemStack monsterItem = createMenuItem(Material.IRON_SWORD,
                ChatColor.RED + "打怪任务",
                ChatColor.GRAY + "点击查看打怪类任务");
        gui.setItem(15, monsterItem);

        ItemStack myTasksItem = createMenuItem(Material.BOOK,
                ChatColor.GREEN + "我的任务",
                ChatColor.GRAY + "查看你发布和认领的任务");
        gui.setItem(29, myTasksItem);

        ItemStack allTasksItem = createMenuItem(Material.CHEST,
                ChatColor.YELLOW + "全部任务",
                ChatColor.GRAY + "查看所有待领取的任务");
        gui.setItem(33, allTasksItem);

        ItemStack createTaskItem = createMenuItem(Material.WRITABLE_BOOK,
                ChatColor.LIGHT_PURPLE + "发布任务",
                ChatColor.GRAY + "点击了解如何发布任务",
                ChatColor.YELLOW + "指令: /sybt task <类型> <标题> <详情> <报酬>");
        gui.setItem(49, createTaskItem);

        player.openInventory(gui);
    }

    public void openTaskListGUI(Player player, TaskType type, int page) {
        String title = TASK_LIST_GUI_TITLE + (type != null ? type.getDisplayName() : "全部");
        Inventory gui = Bukkit.createInventory(new GUIHolder("task_list", type), GUI_SIZE, title);

        List<BountyTask> tasks;
        if (type != null) {
            tasks = plugin.getDatabaseManager().getTasksByType(type);
        } else {
            tasks = plugin.getDatabaseManager().getPendingTasks();
        }

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, tasks.size());

        for (int i = startIndex; i < endIndex; i++) {
            BountyTask task = tasks.get(i);
            ItemStack taskItem = createTaskItem(task);
            gui.setItem(i - startIndex, taskItem);
        }

        if (page > 0) {
            ItemStack prevPage = createMenuItem(Material.ARROW,
                    ChatColor.YELLOW + "上一页",
                    ChatColor.GRAY + "当前页: " + (page + 1));
            gui.setItem(45, prevPage);
        }

        if (endIndex < tasks.size()) {
            ItemStack nextPage = createMenuItem(Material.ARROW,
                    ChatColor.YELLOW + "下一页",
                    ChatColor.GRAY + "当前页: " + (page + 1));
            gui.setItem(53, nextPage);
        }

        ItemStack backItem = createMenuItem(Material.BARRIER,
                ChatColor.RED + "返回",
                ChatColor.GRAY + "返回主菜单");
        gui.setItem(49, backItem);

        playerPageMap.put(player.getName(), page);
        player.openInventory(gui);
    }

    public void openMyTasksGUI(Player player, int page) {
        Inventory gui = Bukkit.createInventory(new GUIHolder("my_tasks"), GUI_SIZE, "我的任务");

        List<BountyTask> publishedTasks = plugin.getDatabaseManager().getTasksByPublisher(player.getUniqueId());
        List<BountyTask> claimedTasks = plugin.getDatabaseManager().getTasksByClaimer(player.getUniqueId());

        List<BountyTask> allTasks = new ArrayList<>();
        allTasks.addAll(publishedTasks);
        allTasks.addAll(claimedTasks);

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, allTasks.size());

        for (int i = startIndex; i < endIndex; i++) {
            BountyTask task = allTasks.get(i);
            ItemStack taskItem = createTaskItem(task, true);
            gui.setItem(i - startIndex, taskItem);
        }

        if (page > 0) {
            ItemStack prevPage = createMenuItem(Material.ARROW,
                    ChatColor.YELLOW + "上一页",
                    ChatColor.GRAY + "当前页: " + (page + 1));
            gui.setItem(45, prevPage);
        }

        if (endIndex < allTasks.size()) {
            ItemStack nextPage = createMenuItem(Material.ARROW,
                    ChatColor.YELLOW + "下一页",
                    ChatColor.GRAY + "当前页: " + (page + 1));
            gui.setItem(53, nextPage);
        }

        ItemStack backItem = createMenuItem(Material.BARRIER,
                ChatColor.RED + "返回",
                ChatColor.GRAY + "返回主菜单");
        gui.setItem(49, backItem);

        player.openInventory(gui);
    }

    private ItemStack createMenuItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(name));
        
        List<Component> loreList = new ArrayList<>();
        for (String line : lore) {
            loreList.add(LegacyComponentSerializer.legacySection().deserialize(line));
        }
        meta.lore(loreList);
        
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTaskItem(BountyTask task) {
        return createTaskItem(task, false);
    }

    private ItemStack createTaskItem(BountyTask task, boolean showStatus) {
        Material material = Material.valueOf(task.getType().getIconMaterial());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String statusPrefix = "";
        if (showStatus) {
            switch (task.getStatus()) {
                case PENDING -> statusPrefix = ChatColor.GRAY + "[待领取] ";
                case CLAIMED -> statusPrefix = ChatColor.YELLOW + "[已认领] ";
                case COMPLETED -> statusPrefix = ChatColor.GREEN + "[待验收] ";
                case CONFIRMED -> statusPrefix = ChatColor.DARK_GREEN + "[已完成] ";
            }
        }

        meta.displayName(LegacyComponentSerializer.legacySection().deserialize(
                statusPrefix + ChatColor.WHITE + task.getTitle()));

        List<Component> lore = new ArrayList<>();
        lore.add(LegacyComponentSerializer.legacySection().deserialize(ChatColor.GRAY + "ID: " + ChatColor.YELLOW + task.getId()));
        lore.add(LegacyComponentSerializer.legacySection().deserialize(ChatColor.GRAY + "类型: " + ChatColor.AQUA + task.getType().getDisplayName()));
        lore.add(LegacyComponentSerializer.legacySection().deserialize(ChatColor.GRAY + "详情: " + ChatColor.WHITE + task.getDetails()));
        lore.add(LegacyComponentSerializer.legacySection().deserialize(ChatColor.GRAY + "报酬: " + ChatColor.GOLD + plugin.getEconomyManager().format(task.getReward())));
        lore.add(LegacyComponentSerializer.legacySection().deserialize(ChatColor.GRAY + "发布者: " + ChatColor.GREEN + task.getPublisherName()));
        
        if (task.getClaimerName() != null) {
            lore.add(LegacyComponentSerializer.legacySection().deserialize(ChatColor.GRAY + "认领者: " + ChatColor.GREEN + task.getClaimerName()));
        }

        if (task.getStatus() == BountyTask.TaskStatus.PENDING) {
            lore.add(LegacyComponentSerializer.legacySection().deserialize(""));
            lore.add(LegacyComponentSerializer.legacySection().deserialize(ChatColor.YELLOW + "点击认领此任务"));
            lore.add(LegacyComponentSerializer.legacySection().deserialize(ChatColor.GRAY + "或使用 /sybt get " + task.getId()));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void sendTaskInfo(Player player, BountyTask task) {
        player.sendMessage(ChatColor.GOLD + "========== 任务信息 ==========");
        player.sendMessage(ChatColor.YELLOW + "ID: " + ChatColor.WHITE + task.getId());
        player.sendMessage(ChatColor.YELLOW + "标题: " + ChatColor.WHITE + task.getTitle());
        player.sendMessage(ChatColor.YELLOW + "类型: " + ChatColor.AQUA + task.getType().getDisplayName());
        player.sendMessage(ChatColor.YELLOW + "详情: " + ChatColor.WHITE + task.getDetails());
        player.sendMessage(ChatColor.YELLOW + "报酬: " + ChatColor.GOLD + plugin.getEconomyManager().format(task.getReward()));
        player.sendMessage(ChatColor.YELLOW + "发布者: " + ChatColor.GREEN + task.getPublisherName());
        player.sendMessage(ChatColor.YELLOW + "状态: " + getStatusColor(task.getStatus()) + task.getStatus().name());
        player.sendMessage(ChatColor.GOLD + "==============================");
    }

    private ChatColor getStatusColor(BountyTask.TaskStatus status) {
        return switch (status) {
            case PENDING -> ChatColor.GRAY;
            case CLAIMED -> ChatColor.YELLOW;
            case COMPLETED -> ChatColor.GREEN;
            case CONFIRMED -> ChatColor.DARK_GREEN;
        };
    }

    public void sendClickableMessage(Player player, String message, String hoverText, String command) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(message)
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(hoverText)))
                .clickEvent(ClickEvent.runCommand(command));
        player.sendMessage(component);
    }

    public void sendClickableMessageWithUrl(Player player, String message, String hoverText, String url) {
        Component component = LegacyComponentSerializer.legacySection().deserialize(message)
                .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacySection().deserialize(hoverText)))
                .clickEvent(ClickEvent.openUrl(url));
        player.sendMessage(component);
    }

    public int getPlayerPage(String playerName) {
        return playerPageMap.getOrDefault(playerName, 0);
    }

    public void setPlayerPage(String playerName, int page) {
        playerPageMap.put(playerName, page);
    }

    public TaskType getPlayerFilter(String playerName) {
        return playerFilterMap.get(playerName);
    }

    public void setPlayerFilter(String playerName, TaskType type) {
        playerFilterMap.put(playerName, type);
    }

    public static class GUIHolder implements InventoryHolder {
        private final String type;
        private final TaskType taskType;

        public GUIHolder(String type) {
            this.type = type;
            this.taskType = null;
        }

        public GUIHolder(String type, TaskType taskType) {
            this.type = type;
            this.taskType = taskType;
        }

        public String getType() {
            return type;
        }

        public TaskType getTaskType() {
            return taskType;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
