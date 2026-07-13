package com.neosoma.customenchants.listener;

import com.neosoma.customenchants.CustomEnchantsPlugin;
import com.neosoma.customenchants.enchant.EnchantIndex;
import com.neosoma.customenchants.enchant.EnchantState;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/** Effets des enchantements de trident : Harpon, Tempête, Léviathan. */
public class TridentListener implements Listener {

    private final CustomEnchantsPlugin plugin;

    public TridentListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    private int niv(ItemStack trident, String id) {
        return EnchantState.actif(id) ? EnchantIndex.niveau(trident, id) : 0;
    }

    /** L'item représentant le trident, en mêlée (dans la main) ou lancé (porté par le projectile). */
    private ItemStack armeDe(Entity damager) {
        if (damager instanceof Player joueur) return joueur.getInventory().getItemInMainHand();
        if (damager instanceof Trident trident) return trident.getItemStack();
        return null;
    }

    private Player lanceurDe(Entity damager) {
        if (damager instanceof Player joueur) return joueur;
        if (damager instanceof Trident trident && trident.getShooter() instanceof Player joueur) return joueur;
        return null;
    }

    // =========================================================
    //  Tempête & Léviathan : coup porté (mêlée ou trident lancé)
    // =========================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCoup(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity victime)) return;

        ItemStack arme = armeDe(event.getDamager());
        Player lanceur = lanceurDe(event.getDamager());
        if (arme == null || lanceur == null) return;

        var rand = ThreadLocalRandom.current();
        int lvl;

        // Tempête : chance de foudre sur la cible, par tout temps
        if ((lvl = niv(arme, "tempete")) > 0) {
            double chance = plugin.getConfig().getDouble(
                    "effets.tempete.chance-par-niveau", 0.15) * lvl;
            if (rand.nextDouble() < chance) {
                victime.getWorld().strikeLightning(victime.getLocation());
            }
        }

        // Léviathan : vague qui repousse les ennemis proches + sursaut aquatique pour le lanceur
        if (niv(arme, "leviathan") > 0) {
            double rayon = plugin.getConfig().getDouble("effets.leviathan.rayon", 4.0);
            for (Entity proche : victime.getNearbyEntities(rayon, 2, rayon)) {
                if (proche instanceof LivingEntity le && le != lanceur && le != victime) {
                    Vector poussee = le.getLocation().toVector()
                            .subtract(victime.getLocation().toVector());
                    if (poussee.lengthSquared() < 0.01) poussee = new Vector(0, 0, 0.1);
                    poussee = poussee.normalize().multiply(1.4);
                    poussee.setY(0.35);
                    le.setVelocity(le.getVelocity().add(poussee));
                }
            }
            lanceur.addPotionEffect(new PotionEffect(
                    PotionEffectType.DOLPHINS_GRACE, 100, 0, true, true));
            lanceur.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, 100, 1, true, true));
            victime.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP,
                    victime.getLocation().add(0, 1, 0), 40, 1.5, 0.5, 1.5, 0.05);
        }
    }

    // =========================================================
    //  Harpon : impact du trident lancé, tire la cible vers le lanceur
    // =========================================================
    @EventHandler(ignoreCancelled = true)
    public void onImpact(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return;
        if (!(trident.getShooter() instanceof Player lanceur)) return;
        if (!(event.getHitEntity() instanceof LivingEntity cible)) return;

        int lvl = niv(trident.getItemStack(), "harpon");
        if (lvl <= 0) return;

        Vector direction = lanceur.getLocation().toVector().subtract(cible.getLocation().toVector());
        double distance = direction.length();
        if (distance < 1) return;

        Vector traction = direction.normalize().multiply(Math.min(2.2, 0.5 + distance * 0.10 * lvl));
        traction.setY(Math.min(0.8, traction.getY() + 0.25));
        cible.setVelocity(traction);
        cible.setFallDistance(0);
        cible.getWorld().playSound(cible.getLocation(),
                Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 0.8f);
    }
}
