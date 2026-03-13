package com.ecocoins.core;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class InventoryUtil {

    private static final short HOTBAR_START_SLOT = 0;
    private static final short HOTBAR_END_SLOT = 8;

    private InventoryUtil() {}

    public static int countItemId(Inventory inv, String itemId) {
        ItemContainer all = inv.getCombinedEverything();
        AtomicInteger total = new AtomicInteger(0);
        all.forEach((slot, stack) -> {
            if (stack == null || stack.isEmpty()) return;
            if (matchesItemId(resolveItemId(stack), itemId)) total.addAndGet(stack.getQuantity());
        });
        return total.get();
    }

    /**
     * Remueve hasta 'amount' del itemId del inventario. Retorna true si removió todo.
     */
    public static boolean removeItemId(Inventory inv, String itemId, int amount) {
        if (amount <= 0) return true;
        ItemContainer all = inv.getCombinedEverything();

        // Camino rápido con el id exacto
        ItemStack need = new ItemStack(itemId, amount);
        if (all.canRemoveItemStack(need)) {
            all.removeItemStack(need);
            return true;
        }

        // Fallback: remover por id normalizado (con/sin namespace)
        int available = countItemId(inv, itemId);
        if (available < amount) return false;

        Map<String, Integer> removeByExactId = new LinkedHashMap<>();
        AtomicInteger remaining = new AtomicInteger(amount);
        all.forEach((slot, stack) -> {
            if (remaining.get() <= 0 || stack == null || stack.isEmpty()) return;
            String stackId = resolveItemId(stack);
            if (!matchesItemId(stackId, itemId)) return;

            int take = Math.min(stack.getQuantity(), remaining.get());
            if (take <= 0) return;

            removeByExactId.merge(stackId, take, Integer::sum);
            remaining.addAndGet(-take);
        });

        if (remaining.get() > 0) return false;

        for (Map.Entry<String, Integer> entry : removeByExactId.entrySet()) {
            ItemStack chunk = new ItemStack(entry.getKey(), entry.getValue());
            if (!all.canRemoveItemStack(chunk)) return false;
            all.removeItemStack(chunk);
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

    public static int countItemIdInHotbar(Inventory inv, String itemId) {
        if (inv == null || itemId == null || itemId.isBlank()) return 0;

        ItemContainer hotbar = inv.getHotbar();
        AtomicInteger total = new AtomicInteger(0);
        for (short slot = HOTBAR_START_SLOT; slot <= HOTBAR_END_SLOT; slot++) {
            ItemStack stack = hotbar.getItemStack(slot);
            if (stack == null || stack.isEmpty()) continue;
            if (matchesItemId(resolveItemId(stack), itemId)) {
                total.addAndGet(stack.getQuantity());
            }
        }
        return total.get();
    }

    /**
     * Remueve únicamente desde hotbar (slots 1..9 para el jugador, 0..8 interno).
     */
    public static boolean removeItemIdFromHotbar(Inventory inv, String itemId, int amount) {
        if (inv == null || itemId == null || itemId.isBlank()) return false;
        if (amount <= 0) return true;

        ItemContainer hotbar = inv.getHotbar();
        int available = countItemIdInHotbar(inv, itemId);
        if (available < amount) return false;

        int remaining = amount;
        for (short slot = HOTBAR_START_SLOT; slot <= HOTBAR_END_SLOT && remaining > 0; slot++) {
            ItemStack stack = hotbar.getItemStack(slot);
            if (stack == null || stack.isEmpty()) continue;

            String stackId = resolveItemId(stack);
            if (!matchesItemId(stackId, itemId)) continue;

            int qty = stack.getQuantity();
            int take = Math.min(qty, remaining);

            if (take >= qty) {
                hotbar.setItemStackForSlot(slot, ItemStack.EMPTY);
            } else {
                String resolvedId = stackId != null ? stackId : itemId;
                hotbar.setItemStackForSlot(slot, new ItemStack(resolvedId, qty - take));
            }
            remaining -= take;
        }

        return remaining <= 0;
    }


    private static String resolveItemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        String direct = stack.getItemId();
        if (direct != null && !direct.isBlank()) return direct;

        if (stack.getItem() != null && stack.getItem().getId() != null && !stack.getItem().getId().isBlank()) {
            return stack.getItem().getId();
        }

        return null;
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
