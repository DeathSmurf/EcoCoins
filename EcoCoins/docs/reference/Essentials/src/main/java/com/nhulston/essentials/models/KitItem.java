package com.nhulston.essentials.models;

import javax.annotation.Nonnull;

/**
 * Represents a single item in a kit.
 *
 * @param section hotbar, storage, armor, utility, tools
 */
public record KitItem(String itemId, int quantity, String section, int slot) {
    public KitItem(@Nonnull String itemId, int quantity, @Nonnull String section, int slot) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.section = section;
        this.slot = slot;
    }

    @Override
    @Nonnull
    public String itemId() {
        return itemId;
    }

    @Override
    @Nonnull
    public String section() {
        return section;
    }
}
