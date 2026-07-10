package com.neosoma.customenchants.listener;

import com.neosoma.customenchants.enchant.CEnchant;
import com.neosoma.customenchants.enchant.EnchantIndex;
import com.neosoma.customenchants.enchant.EnchantState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;

/**
 * Le registre étant figé au démarrage, un enchantement désactivé ne peut pas être
 * retiré de la table à chaud : on le filtre donc du résultat au moment du tirage.
 */
public class TableFilterListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchant(EnchantItemEvent event) {
        event.getEnchantsToAdd().entrySet().removeIf(entree -> {
            CEnchant ce = EnchantIndex.ceOf(entree.getKey());
            return ce != null && !EnchantState.actif(ce.id());
        });
    }
}
