package com.neosoma.customenchants.gui;

import com.neosoma.customenchants.enchant.Categorie;
import com.neosoma.customenchants.enchant.Rarity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;

/** Holder du panel principal : page + filtres + ids affichés (dans l'ordre des slots). */
public class PanelHolder implements InventoryHolder {

    private Inventory inventory;
    public int page = 0;
    public Categorie filtreCategorie = null;
    public Rarity filtreRarete = null;
    public final List<String> idsAffiches = new ArrayList<>();

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
