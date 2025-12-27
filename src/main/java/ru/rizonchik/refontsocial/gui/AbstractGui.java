package ru.rizonchik.refontsocial.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class AbstractGui {

    protected Inventory inventory;

    public Inventory getInventory() {
        return inventory;
    }

    public abstract void open(Player player);

    public abstract void onClick(Player player, int rawSlot, ItemStack clicked);

    public void onClose(Player player) {
    }
}