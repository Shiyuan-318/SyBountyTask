package com.sy.bountytasks.task;

public enum TaskType {
    MATERIAL(1, "物资", "DIAMOND"),
    BUILD(2, "建造", "BRICKS"),
    MONSTER(3, "打怪", "IRON_SWORD");

    private final int id;
    private final String displayName;
    private final String iconMaterial;

    TaskType(int id, String displayName, String iconMaterial) {
        this.id = id;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
    }

    public int getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconMaterial() {
        return iconMaterial;
    }

    public static TaskType fromId(int id) {
        for (TaskType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }
}
