package com.neosoma.customenchants.listener;

import com.neosoma.customenchants.CustomEnchantsPlugin;
import com.neosoma.customenchants.enchant.CEnchant;
import com.neosoma.customenchants.enchant.EnchantIndex;
import com.neosoma.customenchants.enchant.EnchantState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.view.AnvilView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Enclume : limite du nombre d'enchantements custom par item,
 * blocage des enchantements désactivés, compatibilité item/enchant,
 * et coût XP progressif anti-cumul.
 */
public class AnvilListener implements Listener {

    private final CustomEnchantsPlugin plugin;

    public AnvilListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEnclume(PrepareAnvilEvent event) {
        ItemStack resultat = event.getResult();
        if (resultat == null || resultat.getType().isAir()) return;

        Map<CEnchant, Integer> customs = EnchantIndex.customsSur(resultat);
        if (customs.isEmpty()) return;

        // Filet de sécurité : un enchant custom ne peut pas finir sur un item incompatible
        // (les livres enchantés peuvent contenir n'importe quoi, comme en vanilla)
        if (resultat.getType() != org.bukkit.Material.ENCHANTED_BOOK) {
            for (CEnchant ce : customs.keySet()) {
                if (!EnchantIndex.get(ce.id()).canEnchantItem(resultat)) {
                    event.setResult(null);
                    return;
                }
            }
        }

        ItemStack base = event.getInventory().getFirstItem();
        Map<CEnchant, Integer> surBase = EnchantIndex.customsSur(base);

        // Un enchant désactivé ou un niveau bloqué ne peut pas être AJOUTÉ
        // (mais reste sur les items qui le portaient déjà à ce niveau)
        for (Map.Entry<CEnchant, Integer> entree : customs.entrySet()) {
            CEnchant ce = entree.getKey();
            int niveau = entree.getValue();
            boolean dejaPresentAuMemeNiveau = surBase.getOrDefault(ce, 0) == niveau;
            if (!EnchantState.niveauActif(ce.id(), niveau) && !dejaPresentAuMemeNiveau) {
                event.setResult(null);
                return;
            }
        }

        // Limite dure d'enchantements custom par item
        int limite = plugin.getConfig().getInt("enclume.max-enchants-custom", 3);
        if (customs.size() > limite) {
            event.setResult(null);
            return;
        }

        // Coût XP progressif : chaque enchant custom au-delà du premier coûte
        // sa base de rareté multipliée par mult^(position-1)
        if (customs.size() >= 2) {
            double mult = plugin.getConfig().getDouble("enclume.multiplicateur-cout", 2.0);
            List<Integer> bases = new ArrayList<>();
            customs.keySet().forEach(ce -> bases.add(ce.rarete().surchargeEnclume()));
            bases.sort(Integer::compareTo);

            double surcharge = 0;
            for (int i = 1; i < bases.size(); i++) {
                surcharge += bases.get(i) * Math.pow(mult, i - 1);
            }

            AnvilView vue = event.getView();
            int nouveauCout = (int) Math.min(39, vue.getRepairCost() + surcharge);
            vue.setRepairCost(nouveauCout);
        }
    }
}
