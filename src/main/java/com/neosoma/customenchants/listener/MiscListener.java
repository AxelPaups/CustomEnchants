package com.neosoma.customenchants.listener;

import com.neosoma.customenchants.CustomEnchantsPlugin;
import com.neosoma.customenchants.enchant.EnchantIndex;
import com.neosoma.customenchants.enchant.EnchantState;
import com.neosoma.customenchants.util.Util;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Dernier souffle, ancrage, poigne du golem, âme liée+, réparation d'âme. */
public class MiscListener implements Listener {

    private final CustomEnchantsPlugin plugin;

    /** Items sauvés par Âme liée+ en attente de respawn. */
    private final Map<UUID, List<ItemStack>> amesLiees = new HashMap<>();

    public MiscListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    private int niv(ItemStack item, String id) {
        return EnchantState.actif(id) ? EnchantIndex.niveau(item, id) : 0;
    }

    // ---- Dernier souffle : sursaut de survie ----
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDegats(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player joueur)) return;

        ItemStack plastron = joueur.getInventory().getChestplate();
        if (niv(plastron, "dernier_souffle") <= 0) return;

        double pvApres = joueur.getHealth() - event.getFinalDamage();
        if (pvApres <= 0 || pvApres > joueur.getMaxHealth() * 0.20) return;

        long cd = (long) (plugin.getConfig().getDouble(
                "effets.dernier_souffle.cooldown-secondes", 300) * 1000);
        if (!Util.cooldownPret(joueur.getUniqueId(), "dernier_souffle", cd)) return;

        joueur.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION, 200, 1, true, true));
        joueur.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, 200, 1, true, true));
        plugin.msg(joueur, "<gold>Dernier souffle</gold> <gray>s'active !</gray>");
    }

    // ---- Ancrage & Poigne du golem : recul ----
    @EventHandler(ignoreCancelled = true)
    public void onRecul(EntityKnockbackEvent event) {
        if (!(event.getEntity() instanceof Player joueur)) return;

        ItemStack plastron = joueur.getInventory().getChestplate();

        if (niv(plastron, "ancrage") > 0) {
            event.setCancelled(true);
            return;
        }

        int poigne = niv(plastron, "poigne_du_golem");
        if (poigne > 0) {
            double facteur = Math.max(0, 1 - 0.35 * poigne);
            event.setKnockback(event.getKnockback().multiply(facteur));
        }
    }

    // ---- Âme liée+ : conserve les items à la mort ----
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMortJoueur(PlayerDeathEvent event) {
        if (event.getKeepInventory() || !EnchantState.actif("ame_liee")) return;

        List<ItemStack> sauves = new ArrayList<>();
        event.getDrops().removeIf(item -> {
            if (EnchantIndex.niveau(item, "ame_liee") > 0) {
                sauves.add(item);
                return true;
            }
            return false;
        });
        if (!sauves.isEmpty()) {
            amesLiees.merge(event.getPlayer().getUniqueId(), sauves, (anciens, nouveaux) -> {
                anciens.addAll(nouveaux);
                return anciens;
            });
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        List<ItemStack> sauves = amesLiees.remove(event.getPlayer().getUniqueId());
        if (sauves == null) return;
        for (ItemStack item : sauves) {
            var restes = event.getPlayer().getInventory().addItem(item);
            restes.values().forEach(reste -> event.getPlayer().getWorld()
                    .dropItemNaturally(event.getPlayer().getLocation(), reste));
        }
        plugin.msg(event.getPlayer(),
                "<light_purple>Âme liée+</light_purple> <gray>vous rend vos items.</gray>");
    }

    // ---- Réparation d'âme : répare l'arme à chaque kill ----
    @EventHandler(ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        Player tueur = event.getEntity().getKiller();
        if (tueur == null) return;

        ItemStack arme = tueur.getInventory().getItemInMainHand();
        if (niv(arme, "reparation_dame") <= 0) return;

        if (arme.getItemMeta() instanceof Damageable meta && meta.hasDamage()) {
            meta.setDamage(Math.max(0, meta.getDamage() - 20));
            arme.setItemMeta(meta);
        }
    }
}
