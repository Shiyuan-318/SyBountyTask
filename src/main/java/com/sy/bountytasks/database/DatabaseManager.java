package com.sy.bountytasks.database;

import com.sy.bountytasks.SyBountyTask;
import com.sy.bountytasks.task.BountyTask;
import com.sy.bountytasks.task.TaskType;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private final SyBountyTask plugin;
    private Connection connection;
    private final String dbPath;

    public DatabaseManager(SyBountyTask plugin) {
        this.plugin = plugin;
        this.dbPath = plugin.getDataFolder() + File.separator + plugin.getConfig().getString("database-file", "tasks.db");
    }

    public void initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);

            createTables();
            plugin.getLogger().info("Database initialized successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() throws SQLException {
        String createTableSQL = """
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                publisher_uuid TEXT NOT NULL,
                publisher_name TEXT NOT NULL,
                type INTEGER NOT NULL,
                title TEXT NOT NULL,
                details TEXT NOT NULL,
                reward REAL NOT NULL,
                service_fee REAL NOT NULL,
                create_time INTEGER NOT NULL,
                claimer_uuid TEXT,
                claimer_name TEXT,
                status TEXT NOT NULL DEFAULT 'PENDING'
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
    }

    public int createTask(BountyTask task) {
        String sql = """
            INSERT INTO tasks (publisher_uuid, publisher_name, type, title, details, 
                             reward, service_fee, create_time, claimer_uuid, claimer_name, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, task.getPublisherUuid().toString());
            pstmt.setString(2, task.getPublisherName());
            pstmt.setInt(3, task.getType().getId());
            pstmt.setString(4, task.getTitle());
            pstmt.setString(5, task.getDetails());
            pstmt.setDouble(6, task.getReward());
            pstmt.setDouble(7, task.getServiceFee());
            pstmt.setLong(8, task.getCreateTime());
            pstmt.setString(9, task.getClaimerUuid() != null ? task.getClaimerUuid().toString() : null);
            pstmt.setString(10, task.getClaimerName());
            pstmt.setString(11, task.getStatus().name());

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create task: " + e.getMessage());
        }
        return -1;
    }

    public boolean updateTask(BountyTask task) {
        String sql = """
            UPDATE tasks SET claimer_uuid = ?, claimer_name = ?, status = ?
            WHERE id = ?
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, task.getClaimerUuid() != null ? task.getClaimerUuid().toString() : null);
            pstmt.setString(2, task.getClaimerName());
            pstmt.setString(3, task.getStatus().name());
            pstmt.setInt(4, task.getId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to update task: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteTask(int taskId) {
        String sql = "DELETE FROM tasks WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to delete task: " + e.getMessage());
        }
        return false;
    }

    public BountyTask getTask(int taskId) {
        String sql = "SELECT * FROM tasks WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return resultSetToTask(rs);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get task: " + e.getMessage());
        }
        return null;
    }

    public List<BountyTask> getAllTasks() {
        List<BountyTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks ORDER BY create_time DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tasks.add(resultSetToTask(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get all tasks: " + e.getMessage());
        }
        return tasks;
    }

    public List<BountyTask> getTasksByPublisher(UUID publisherUuid) {
        List<BountyTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE publisher_uuid = ? ORDER BY create_time DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, publisherUuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(resultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get tasks by publisher: " + e.getMessage());
        }
        return tasks;
    }

    public List<BountyTask> getTasksByClaimer(UUID claimerUuid) {
        List<BountyTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE claimer_uuid = ? ORDER BY create_time DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, claimerUuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(resultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get tasks by claimer: " + e.getMessage());
        }
        return tasks;
    }

    public List<BountyTask> getTasksByType(TaskType type) {
        List<BountyTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE type = ? AND status = 'PENDING' ORDER BY create_time DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, type.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(resultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get tasks by type: " + e.getMessage());
        }
        return tasks;
    }

    public List<BountyTask> getPendingTasks() {
        List<BountyTask> tasks = new ArrayList<>();
        String sql = "SELECT * FROM tasks WHERE status = 'PENDING' ORDER BY create_time DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tasks.add(resultSetToTask(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get pending tasks: " + e.getMessage());
        }
        return tasks;
    }

    public int getTodayTaskCount(UUID publisherUuid) {
        long startOfDay = getStartOfDayMillis();
        String sql = "SELECT COUNT(*) FROM tasks WHERE publisher_uuid = ? AND create_time >= ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, publisherUuid.toString());
            pstmt.setLong(2, startOfDay);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get today task count: " + e.getMessage());
        }
        return 0;
    }

    public void cleanupOldTasks(int daysOld) {
        long cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);
        String sql = "DELETE FROM tasks WHERE create_time < ? AND status IN ('PENDING', 'CLAIMED')";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, cutoffTime);
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                plugin.getLogger().info("Cleaned up " + deleted + " old tasks.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to cleanup old tasks: " + e.getMessage());
        }
    }

    private long getStartOfDayMillis() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private BountyTask resultSetToTask(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        UUID publisherUuid = UUID.fromString(rs.getString("publisher_uuid"));
        String publisherName = rs.getString("publisher_name");
        TaskType type = TaskType.fromId(rs.getInt("type"));
        String title = rs.getString("title");
        String details = rs.getString("details");
        double reward = rs.getDouble("reward");
        double serviceFee = rs.getDouble("service_fee");
        long createTime = rs.getLong("create_time");

        BountyTask task = new BountyTask(id, publisherUuid, publisherName, type, title, details, reward, serviceFee);
        task.setStatus(BountyTask.TaskStatus.valueOf(rs.getString("status")));

        String claimerUuidStr = rs.getString("claimer_uuid");
        if (claimerUuidStr != null) {
            task.setClaimerUuid(UUID.fromString(claimerUuidStr));
            task.setClaimerName(rs.getString("claimer_name"));
        }

        return task;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to close database: " + e.getMessage());
        }
    }
}
