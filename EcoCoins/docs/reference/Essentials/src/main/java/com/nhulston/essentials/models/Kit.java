package com.nhulston.essentials.models;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a kit configuration.
 */
public class Kit {
    private final String id;
    private final String displayName;
    private final int cooldown;  // in seconds, 0 = no cooldown
    private final String type;   // "add" or "replace"
    private final List<KitItem> items;

    public Kit(@Nonnull String id, @Nonnull String displayName, int cooldown,
               @Nonnull String type, @Nonnull List<KitItem> items) {
        this.id = id;
        this.displayName = displayName;
        this.cooldown = cooldown;
        this.type = type;
        this.items = new ArrayList<>(items);
    }

    @Nonnull
    public String getId() {
        return id;
    }

    @Nonnull
    public String getDisplayName() {
        return displayName;
    }

    public int getCooldown() {
        return cooldown;
    }

    @Nonnull
    public String getType() {
        return type;
    }

    public boolean isReplaceMode() {
        return "replace".equalsIgnoreCase(type);
    }

    @Nonnull
    public List<KitItem> getItems() {
        return items;
    }
}
