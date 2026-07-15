package com.neosoma.customenchants.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Holder du sous-menu "niveaux" : blocage individuel de chaque niveau d'un enchantement. */
public class NiveauHolder implements InventoryHolder {

    private Inventory inventory;
    public final String enchantId;

    // Pour revenir au panel avec les mêmes filtres
    public final PanelHolder retour;

    public NiveauHolder(String enchantId, PanelHolder retour) {
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
