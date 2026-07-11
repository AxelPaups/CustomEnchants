package com.neosoma.customenchants.listener;

import com.neosoma.customenchants.CustomEnchantsPlugin;
import com.neosoma.customenchants.enchant.CEnchant;
import com.neosoma.customenchants.enchant.EnchantState;
import com.neosoma.customenchants.enchant.Enchants;
import com.neosoma.customenchants.enchant.Rarity;
import com.neosoma.customenchants.util.Util;
import org.bukkit.Material;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bibliothécaires : une partie des trades de livre enchanté vanilla est remplacée
 * par un livre custom. Le coût grimpe avec la rareté (émeraudes -> diamants ->
 * netherite + blocs d'émeraude), et les livres Mythiques ne se vendent qu'une fois
 * (le poids de tirage de Rarity, déjà utilisé pour le loot, donne ~2% de chance).
 */
public class VillagerListener implements Listener {

    private final CustomEnchantsPlugin plugin;

    public VillagerListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTrade(VillagerAcquireTradeEvent event) {
        if (!(event.getEntity() instanceof Villager villageois)) return;
        if (villageois.getProfession() != Villager.Profession.LIBRARIAN) return;

        MerchantRecipe recette = event.getRecipe();
        if (recette.getResult().getType() != Material.ENCHANTED_BOOK) return;

        if (!plugin.getConfig().getBoolean("villageois.actif", true)) return;
        double chance = plugin.getConfig().getDouble("villageois.chance-remplacement", 0.20);
        var rand = ThreadLocalRandom.current();
        if (rand.nextDouble() >= chance) return;

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
        ItemStack livre = Util.livre(choisi, niveau);

        event.setRecipe(construireRecette(livre, choisi.rarete()));
    }

    private MerchantRecipe construireRecette(ItemStack livre, Rarity rarete) {
        var rand = ThreadLocalRandom.current();
        int maxUses = rarete == Rarity.MYTHIQUE ? 1 : 8 + rand.nextInt(5);
        MerchantRecipe recette = new MerchantRecipe(livre, 0, maxUses, true, 5, 0.05f);

        ItemStack ingredient1;
        ItemStack ingredient2 = null;

        switch (rarete) {
            case COMMUN -> ingredient1 = new ItemStack(Material.EMERALD, 6 + rand.nextInt(5));
            case RARE -> ingredient1 = new ItemStack(Material.EMERALD, 14 + rand.nextInt(7));
            case EPIQUE -> {
                ingredient1 = new ItemStack(Material.DIAMOND, 2 + rand.nextInt(3));
                ingredient2 = new ItemStack(Material.EMERALD, 10);
            }
            case LEGENDAIRE -> {
                ingredient1 = new ItemStack(Material.NETHERITE_SCRAP, 1);
                ingredient2 = new ItemStack(Material.EMERALD_BLOCK, 1 + rand.nextInt(2));
            }
            case MYTHIQUE -> {
                ingredient1 = new ItemStack(Material.NETHERITE_INGOT, 1);
                ingredient2 = new ItemStack(Material.EMERALD_BLOCK, 3);
            }
            default -> ingredient1 = new ItemStack(Material.EMERALD, 10);
        }

        recette.addIngredient(ingredient1);
        if (ingredient2 != null) recette.addIngredient(ingredient2);
        return recette;
    }
}
