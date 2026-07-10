package com.neosoma.customenchants.listener;

import com.neosoma.customenchants.CustomEnchantsPlugin;
import com.neosoma.customenchants.enchant.CEnchant;
import com.neosoma.customenchants.enchant.EnchantState;
import com.neosoma.customenchants.enchant.Enchants;
import com.neosoma.customenchants.util.Util;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.LootGenerateEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Injecte des livres enchantés custom dans les coffres générés
 * (donjons, mineshafts, temples, end cities...). Seule source de Mythique hors admin.
 */
public class LootListener implements Listener {

    private final CustomEnchantsPlugin plugin;

    public LootListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onLoot(LootGenerateEvent event) {
        if (!plugin.getConfig().getBoolean("loot.active", true)) return;

        double chance = plugin.getConfig().getDouble("loot.chance", 0.12);
        var rand = ThreadLocalRandom.current();
        if (rand.nextDouble() >= chance) return;

        // Tirage pondéré par la rareté, parmi les enchantements actifs
        List<CEnchant> actifs = Enchants.ALL.stream()
                .filter(ce -> EnchantState.actif(ce.id()))
                .toList();
        if (actifs.isEmpty()) return;

        int total = actifs.stream().mapToInt(ce -> ce.rarete().poidsLoot()).sum();
        int tirage = rand.nextInt(total);
        CEnchant choisi = actifs.get(0);
        for (CEnchant ce : actifs) {
            tirage -= ce.rarete().poidsLoot();
            if (tirage < 0) {
                choisi = ce;
                break;
            }
        }

        int niveau = 1 + rand.nextInt(choisi.niveauMax());
        event.getLoot().add(Util.livre(choisi, niveau));
    }
}
