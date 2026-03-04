package com.ecocoins.hud;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SimpleHud extends CustomUIHud {

    private static final Logger LOGGER = Logger.getLogger(SimpleHud.class.getName());

    private final String uiPath;
    private final Map<String, String> values = new ConcurrentHashMap<>();

    protected SimpleHud(@Nonnull PlayerRef playerRef, @Nonnull String uiPath) {
        super(playerRef);
        this.uiPath = uiPath;
    }

    public SimpleHud setText(@Nonnull String elementId, @Nullable String value) {
        String normalizedId = normalizeId(elementId);
        if (!normalizedId.contains(".")) {
            normalizedId += ".Text";
        }
        values.put(normalizedId, value == null ? "" : value);
        return this;
    }

    public SimpleHud setVisible(@Nonnull String elementId, boolean visible) {
        return setProperty(elementId, "Visibility", visible ? "Visible" : "Collapsed");
    }

    public SimpleHud setProperty(@Nonnull String elementId, @Nonnull String property, @Nonnull String value) {
        values.put(normalizeId(elementId) + "." + property, value);
        return this;
    }

    public boolean pushUpdates() {
        try {
            this.show();
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to push HUD updates for path: " + uiPath + " - " + e.getMessage());
            return false;
        }
    }

    public boolean sendUpdate(@Nonnull UICommandBuilder builder) {
        try {
            this.update(false, builder);
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send incremental HUD update for path: " + uiPath + " - " + e.getMessage());
            return false;
        }
    }

    public UICommandBuilder createBuilder() {
        return new UICommandBuilder();
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append(uiPath);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            builder.set(entry.getKey(), entry.getValue());
        }
    }

    private String normalizeId(String id) {
        if (id == null || id.isBlank()) {
            return "#Unknown";
        }
        return id.startsWith("#") ? id : "#" + id;
    }
}
