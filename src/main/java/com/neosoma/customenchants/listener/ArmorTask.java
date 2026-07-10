package com.neosoma.customenchants.listener;

import com.neosoma.customenchants.CustomEnchantsPlugin;
import com.neosoma.customenchants.enchant.EnchantIndex;
import com.neosoma.customenchants.enchant.EnchantState;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Tâche périodique (toutes les 3 s) appliquant les effets passifs d'armure.
 * Volontairement hors tick pour rester léger sur un serveur peuplé.
 */
public class ArmorTask extends BukkitRunnable {

    private final CustomEnchantsPlugin plugin;

    public ArmorTask(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    private int niv(ItemStack piece, String id) {
        return EnchantState.actif(id) ? EnchantIndex.niveau(piece, id) : 0;
    }

    @Override
    public void run() {
        for (Player joueur : plugin.getServer().getOnlinePlayers()) {
            PlayerInventory inv = joueur.getInventory();
            ItemStack casque = inv.getHelmet();
            ItemStack plastron = inv.getChestplate();
            ItemStack jambieres = inv.getLeggings();
            ItemStack bottes = inv.getBoots();
            int lvl;

            // Vélocité (bottes)
            if ((lvl = niv(bottes, "velocite")) > 0) {
                effet(joueur, PotionEffectType.SPEED, lvl - 1);
            }
            // Sauteur (bottes)
            if ((lvl = niv(bottes, "sauteur")) > 0) {
                effet(joueur, PotionEffectType.JUMP_BOOST, lvl - 1);
            }
            // Nyctalope (casque) : durée longue pour éviter le clignotement
            if (niv(casque, "nyctalope") > 0) {
                joueur.addPotionEffect(new PotionEffect(
                        PotionEffectType.NIGHT_VISION, 400, 0, true, false));
            }
            // Branchies (casque)
            if (niv(casque, "branchies") > 0) {
                effet(joueur, PotionEffectType.WATER_BREATHING, 0);
            }

            // Régénérescence : set complet requis
            int regen = nivSetComplet(casque, plastron, jambieres, bottes, "regenerescence");
            if (regen > 0) {
                effet(joueur, PotionEffectType.REGENERATION, regen - 1);
            }

            // Avatar : set complet, bonus selon l'environnement
            if (nivSetComplet(casque, plastron, jambieres, bottes, "avatar") > 0) {
                appliquerAvatar(joueur);
            }

            // Mineur de l'End (pioche en main, Nether/End)
            ItemStack enMain = inv.getItemInMainHand();
            if (niv(enMain, "mineur_de_lend") > 0) {
                World.Environment env = joueur.getWorld().getEnvironment();
                if (env == World.Environment.NETHER || env == World.Environment.THE_END) {
                    effet(joueur, PotionEffectType.HASTE, 1);
                }
            }
        }
    }

    /** Niveau minimum de l'enchantement sur les 4 pièces (0 si une pièce ne l'a pas). */
    private int nivSetComplet(ItemStack casque, ItemStack plastron,
                              ItemStack jambieres, ItemStack bottes, String id) {
        int a = niv(casque, id), b = niv(plastron, id), c = niv(jambieres, id), d = niv(bottes, id);
        if (a <= 0 || b <= 0 || c <= 0 || d <= 0) return 0;
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private void appliquerAvatar(Player joueur) {
        World.Environment env = joueur.getWorld().getEnvironment();
        if (env == World.Environment.NETHER) {
            effet(joueur, PotionEffectType.FIRE_RESISTANCE, 0);
        } else if (joueur.isInWater()) {
            effet(joueur, PotionEffectType.DOLPHINS_GRACE, 0);
            effet(joueur, PotionEffectType.WATER_BREATHING, 0);
        } else if (joueur.getLocation().getY() < 0) {
            effet(joueur, PotionEffectType.NIGHT_VISION, 0);
            effet(joueur, PotionEffectType.HASTE, 0);
        } else {
            effet(joueur, PotionEffectType.SPEED, 0);
        }
    }

    private void effet(Player joueur, PotionEffectType type, int amplificateur) {
        joueur.addPotionEffect(new PotionEffect(type, 90, amplificateur, true, false));
    }
}
