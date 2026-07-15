package com.neosoma.customenchants.listener;

import com.neosoma.customenchants.CustomEnchantsPlugin;
import com.neosoma.customenchants.enchant.EnchantIndex;
import com.neosoma.customenchants.enchant.EnchantState;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Effets des enchantements d'arc et d'arbalète. */
public class BowListener implements Listener {

    private static final List<String> ENCHANTS_FLECHE =
            List.of("explosive", "grappin", "glaciale", "pluie");

    private final CustomEnchantsPlugin plugin;
    private final NamespacedKey clePrefixe;

    public BowListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
        this.clePrefixe = new NamespacedKey(plugin, "fleche");
    }

    private NamespacedKey cle(String id) {
        return new NamespacedKey(plugin, "fleche_" + id);
    }

    private int niv(ItemStack arc, String id) {
        int niveau = EnchantIndex.niveau(arc, id);
        return EnchantState.niveauActif(id, niveau) ? niveau : 0;
    }

    @EventHandler(ignoreCancelled = true)
    public void onTir(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player tireur)) return;
        ItemStack arc = event.getBow();
        if (arc == null) return;
        if (!(event.getProjectile() instanceof AbstractArrow fleche)) return;

        // Marque la flèche avec les enchantements du tir
        for (String id : ENCHANTS_FLECHE) {
            int lvl = niv(arc, id);
            if (lvl > 0) {
                fleche.getPersistentDataContainer().set(cle(id), PersistentDataType.INTEGER, lvl);
            }
        }

        // Flèches multiples : tirs supplémentaires en éventail
        int multi = niv(arc, "fleches_multiples");
        if (multi > 0) {
            Vector vitesse = fleche.getVelocity();
            for (int i = 1; i <= multi; i++) {
                for (int signe : new int[]{-1, 1}) {
                    Vector devie = vitesse.clone().rotateAroundY(Math.toRadians(7.0 * i * signe));
                    Arrow extra = tireur.launchProjectile(Arrow.class, devie);
                    extra.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
                    extra.setDamage(Math.max(1.0, extra.getDamage() * 0.6));
                    // Les flèches bonus héritent des effets d'impact
                    for (String id : ENCHANTS_FLECHE) {
                        Integer lvl = fleche.getPersistentDataContainer()
                                .get(cle(id), PersistentDataType.INTEGER);
                        if (lvl != null) {
                            extra.getPersistentDataContainer()
                                    .set(cle(id), PersistentDataType.INTEGER, lvl);
                        }
                    }
                }
            }
        }

        // Traçante : correction de trajectoire vers la cible la plus proche
        if (niv(arc, "tracante") > 0) {
            suivreCible(fleche, tireur);
        }
    }

    private void suivreCible(AbstractArrow fleche, Player tireur) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks += 3;
                if (ticks > 60 || !fleche.isValid() || fleche.isOnGround()) {
                    cancel();
                    return;
                }
                LivingEntity cible = null;
                double meilleure = 10 * 10;
                for (Entity proche : fleche.getNearbyEntities(10, 10, 10)) {
                    if (proche instanceof LivingEntity le && le != tireur) {
                        double d = le.getLocation().distanceSquared(fleche.getLocation());
                        if (d < meilleure) {
                            meilleure = d;
                            cible = le;
                        }
                    }
                }
                if (cible == null) return;
                Vector actuel = fleche.getVelocity();
                double vitesse = actuel.length();
                Vector versLaCible = cible.getEyeLocation().toVector()
                        .subtract(fleche.getLocation().toVector()).normalize();
                Vector nouveau = actuel.clone().normalize().multiply(0.75)
                        .add(versLaCible.multiply(0.25)).normalize().multiply(vitesse);
                fleche.setVelocity(nouveau);
                fleche.getWorld().spawnParticle(Particle.CRIT, fleche.getLocation(), 2, 0.05, 0.05, 0.05, 0);
            }
        }.runTaskTimer(plugin, 3L, 3L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onImpact(ProjectileHitEvent event) {
        Projectile proj = event.getEntity();
        if (!(proj instanceof AbstractArrow fleche)) return;

        var pdc = fleche.getPersistentDataContainer();
        Location impact = event.getHitEntity() != null
                ? event.getHitEntity().getLocation()
                : (event.getHitBlock() != null
                        ? event.getHitBlock().getLocation().add(0.5, 0.5, 0.5)
                        : fleche.getLocation());

        // Glaciale
        Integer glaciale = pdc.get(cle("glaciale"), PersistentDataType.INTEGER);
        if (glaciale != null && event.getHitEntity() instanceof LivingEntity touche) {
            touche.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS, 80, glaciale, true, true));
            touche.setFreezeTicks(Math.max(touche.getFreezeTicks(), 120));
        }

        // Explosive (sans casse de blocs)
        Integer explosive = pdc.get(cle("explosive"), PersistentDataType.INTEGER);
        if (explosive != null) {
            impact.getWorld().createExplosion(impact, 1.5f + 0.5f * explosive, false, false);
            fleche.remove();
        }

        // Grappin : tire le tireur vers l'impact
        Integer grappin = pdc.get(cle("grappin"), PersistentDataType.INTEGER);
        if (grappin != null && fleche.getShooter() instanceof Player tireur) {
            Vector direction = impact.toVector().subtract(tireur.getLocation().toVector());
            double distance = direction.length();
            if (distance > 2) {
                Vector traction = direction.normalize()
                        .multiply(Math.min(2.5, 0.6 + distance * 0.12));
                traction.setY(Math.min(1.2, traction.getY() + 0.35));
                tireur.setVelocity(traction);
                tireur.setFallDistance(0);
                tireur.getWorld().playSound(tireur.getLocation(),
                        Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.0f);
            }
        }

        // Pluie : volée de flèches sur la zone
        Integer pluie = pdc.get(cle("pluie"), PersistentDataType.INTEGER);
        if (pluie != null) {
            var rand = ThreadLocalRandom.current();
            int nombre = 8 + 4 * pluie;
            for (int i = 0; i < nombre; i++) {
                Location depart = impact.clone().add(
                        rand.nextDouble(-3, 3), 12 + rand.nextDouble(0, 3), rand.nextDouble(-3, 3));
                Arrow goutte = impact.getWorld().spawnArrow(
                        depart, new Vector(0, -1, 0), 1.6f, 6f);
                goutte.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
                goutte.setDamage(2.0);
                if (fleche.getShooter() instanceof Player tireur) {
                    goutte.setShooter(tireur);
                }
            }
        }
    }
}
