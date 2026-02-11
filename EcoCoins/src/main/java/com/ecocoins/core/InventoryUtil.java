package com.ecocoins.core;

import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.concurrent.atomic.AtomicInteger;

public final class InventoryUtil {

    private InventoryUtil() {}

    public static int countItemId(Inventory inv, String itemId) {
        ItemContainer all = inv.getCombinedEverything();
        AtomicInteger total = new AtomicInteger(0);
        all.forEach((slot, stack) -> {
            if (stack == null || stack.isEmpty()) return;
            if (itemId.equals(stack.getItemId())) total.addAndGet(stack.getQuantity());
        });
        return total.get();
    }

    /**
     * Remueve hasta 'amount' del itemId del inventario. Retorna true si removió todo.
     */
    public static boolean removeItemId(Inventory inv, String itemId, int amount) {
        if (amount <= 0) return true;
        ItemContainer all = inv.getCombinedEverything();
        ItemStack need = new ItemStack(itemId, amount);
        // removeItemStack intenta remover del contenedor; si no puede, la transacción quedará parcial.
        // Usamos canRemoveItemStack primero.
        if (!all.canRemoveItemStack(need)) return false;
        all.removeItemStack(need);
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
}
