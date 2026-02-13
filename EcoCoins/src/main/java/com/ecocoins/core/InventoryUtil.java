package com.ecocoins.core;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class InventoryUtil {

    private InventoryUtil() {}

    public static int countItemId(Inventory inv, String itemId) {
        ItemContainer all = inv.getCombinedEverything();
        AtomicInteger total = new AtomicInteger(0);
        all.forEach((slot, stack) -> {
            if (stack == null || stack.isEmpty()) return;
            if (matchesItemId(stack.getItemId(), itemId)) total.addAndGet(stack.getQuantity());
        });
        return total.get();
    }

    /**
     * Remueve hasta 'amount' del itemId del inventario. Retorna true si removió todo.
     */
    public static boolean removeItemId(Inventory inv, String itemId, int amount) {
        if (amount <= 0) return true;
        ItemContainer all = inv.getCombinedEverything();

        AtomicInteger remaining = new AtomicInteger(amount);
        List<SlotRemoval> removals = new ArrayList<>();

        all.forEach((slot, stack) -> {
            if (remaining.get() <= 0 || stack == null || stack.isEmpty()) return;
            if (!matchesItemId(stack.getItemId(), itemId)) return;

            int take = Math.min(stack.getQuantity(), remaining.get());
            if (take <= 0) return;

            removals.add(new SlotRemoval(slot, take));
            remaining.addAndGet(-take);
        });

        if (remaining.get() > 0) return false;

        for (SlotRemoval removal : removals) {
            var tx = all.removeItemStackFromSlot(removal.slot, removal.amount);
            if (tx == null || !tx.succeeded()) return false;
        }

        return true;
    }

    /**
     * Intenta agregar 'amount' del itemId al inventario. Retorna true si entró completo.
     */
    public static boolean addItemId(Inventory inv, String itemId, int amount) {
        if (amount <= 0) return true;
        ItemContainer all = inv.getCombinedEverything();
        ItemStack give = new ItemStack(itemId, amount);
        if (!all.canAddItemStack(give)) return false;
        return all.addItemStack(give).succeeded();
    }

    public static boolean canAddItemId(Inventory inv, String itemId, int amount) {
        if (amount <= 0) return true;
        ItemContainer all = inv.getCombinedEverything();
        return all.canAddItemStack(new ItemStack(itemId, amount));
    }

    private static final class SlotRemoval {
        private final short slot;
        private final int amount;

        private SlotRemoval(short slot, int amount) {
            this.slot = slot;
            this.amount = amount;
        }
    }

    private static boolean matchesItemId(String actual, String expected) {
        if (actual == null || expected == null) return false;
        String a = actual.trim();
        String e = expected.trim();
        if (a.equals(e)) return true;
        return normalizeItemId(a).equals(normalizeItemId(e));
    }

    private static String normalizeItemId(String itemId) {
        int namespaceSeparator = itemId.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < itemId.length()) {
            return itemId.substring(namespaceSeparator + 1).trim();
        }
        return itemId;
    }
}
