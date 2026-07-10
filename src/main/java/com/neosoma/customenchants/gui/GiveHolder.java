package com.neosoma.customenchants.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Holder du sous-menu "se donner" : enchantement sélectionné + niveau choisi. */
public class GiveHolder implements InventoryHolder {

    private Inventory inventory;
    public final String enchantId;
    public int niveau = 1;

    // Pour revenir au panel avec les mêmes filtres
    public final PanelHolder retour;

    public GiveHolder(String enchantId, PanelHolder retour) {
        this.enchantId = enchantId;
        this.retour = retour;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
