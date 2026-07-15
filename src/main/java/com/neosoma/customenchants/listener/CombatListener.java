package com.neosoma.customenchants.listener;

import com.neosoma.customenchants.CustomEnchantsPlugin;
import com.neosoma.customenchants.enchant.EnchantIndex;
import com.neosoma.customenchants.enchant.EnchantState;
import com.neosoma.customenchants.util.Util;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Effets des enchantements de combat sur épées, plus épines vengeresses. */
public class CombatListener implements Listener {

    private final CustomEnchantsPlugin plugin;

    public CombatListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    private int niv(ItemStack arme, String id) {
        int niveau = EnchantIndex.niveau(arme, id);
        return EnchantState.niveauActif(id, niveau) ? niveau : 0;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCoup(EntityDamageByEntityEvent event) {

        // ---- Attaquant joueur : enchantements d'épée ----
        if (event.getDamager() instanceof Player attaquant
                && event.getEntity() instanceof LivingEntity victime) {

            ItemStack arme = attaquant.getInventory().getItemInMainHand();
            double degats = event.getDamage();
            var rand = ThreadLocalRandom.current();

            int lvl;

            // Vampirisme : vole des PV (cooldown)
            if ((lvl = niv(arme, "vampirisme")) > 0) {
                long cd = (long) (plugin.getConfig().getDouble(
                        "effets.vampirisme.cooldown-secondes", 2) * 1000);
                if (Util.cooldownPret(attaquant.getUniqueId(), "vampirisme", cd)) {
                    double vol = plugin.getConfig().getDouble(
                            "effets.vampirisme.pv-par-niveau", 1.0) * lvl;
                    double max = attaquant.getMaxHealth();
                    attaquant.setHealth(Math.min(max, attaquant.getHealth() + vol));
                    attaquant.getWorld().spawnParticle(Particle.HEART,
                            attaquant.getLocation().add(0, 1.8, 0), 2);
                }
            }

            // Foudroiement : chance d'éclair
            if ((lvl = niv(arme, "foudroiement")) > 0) {
                double chance = plugin.getConfig().getDouble(
                        "effets.foudroiement.chance-par-niveau", 0.10) * lvl;
                if (rand.nextDouble() < chance) {
                    victime.getWorld().strikeLightning(victime.getLocation());
                }
            }

            // Venin : poison
            if ((lvl = niv(arme, "venin")) > 0) {
                victime.addPotionEffect(new PotionEffect(
                        PotionEffectType.POISON, 60, lvl - 1, true, true));
            }

            // Gel : slowness + gel visuel
            if ((lvl = niv(arme, "gel")) > 0) {
                victime.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, 60, lvl, true, true));
                victime.setFreezeTicks(Math.max(victime.getFreezeTicks(), 160));
            }

            // Affaiblissement
            if ((lvl = niv(arme, "affaiblissement")) > 0) {
                victime.addPotionEffect(new PotionEffect(
                        PotionEffectType.WEAKNESS, 60, lvl - 1, true, true));
            }

            // Embrasement+ : feu prolongé
            if ((lvl = niv(arme, "embrasement")) > 0) {
                victime.setFireTicks(Math.max(victime.getFireTicks(), 60 + 40 * lvl));
            }

            // Chasseur : bonus contre les monstres
            if ((lvl = niv(arme, "chasseur")) > 0 && victime instanceof Monster) {
                degats *= 1 + plugin.getConfig().getDouble(
                        "effets.chasseur.bonus-par-niveau", 0.15) * lvl;
            }

            // Rage : bonus quand l'attaquant est bas en PV
            if ((lvl = niv(arme, "rage")) > 0) {
                double seuil = plugin.getConfig().getDouble("effets.rage.seuil-pv", 0.30);
                if (attaquant.getHealth() <= attaquant.getMaxHealth() * seuil) {
                    degats *= 1 + plugin.getConfig().getDouble(
                            "effets.rage.bonus-par-niveau", 0.20) * lvl;
                    attaquant.getWorld().spawnParticle(Particle.ANGRY_VILLAGER,
                            attaquant.getLocation().add(0, 2.2, 0), 3, 0.3, 0.2, 0.3, 0);
                }
            }

            // Exécution : bonus contre les cibles presque mortes
            if ((lvl = niv(arme, "execution")) > 0) {
                double seuil = plugin.getConfig().getDouble("effets.execution.seuil-pv", 0.20);
                if (victime.getHealth() <= victime.getMaxHealth() * seuil) {
                    degats *= 1 + plugin.getConfig().getDouble(
                            "effets.execution.bonus-par-niveau", 0.25) * lvl;
                    victime.getWorld().spawnParticle(Particle.CRIT,
                            victime.getLocation().add(0, 1, 0), 20, 0.3, 0.4, 0.3, 0.2);
                    victime.getWorld().playSound(victime.getLocation(),
                            Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                }
            }

            // Faucheuse : chance de critique dévastateur
            if (niv(arme, "faucheuse") > 0) {
                double chance = plugin.getConfig().getDouble("effets.faucheuse.chance", 0.05);
                if (rand.nextDouble() < chance) {
                    degats *= plugin.getConfig().getDouble("effets.faucheuse.multiplicateur", 3.0);
                    victime.getWorld().spawnParticle(Particle.SOUL,
                            victime.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.03);
                }
            }

            // Vague : dégâts de zone autour de la cible
            if ((lvl = niv(arme, "vague")) > 0) {
                double rayon = plugin.getConfig().getDouble("effets.vague.rayon", 3.0);
                double ratio = plugin.getConfig().getDouble(
                        "effets.vague.degats-ratio-par-niveau", 0.30) * lvl;
                double zone = degats * ratio;
                for (Entity proche : victime.getNearbyEntities(rayon, 2, rayon)) {
                    if (proche instanceof LivingEntity le
                            && le != attaquant && le != victime) {
                        le.damage(zone, attaquant);
                    }
                }
                victime.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                        victime.getLocation().add(0, 1, 0), 3, 1, 0.3, 1);
            }

            event.setDamage(degats);
        }

        // ---- Victime joueur : épines vengeresses ----
        if (event.getEntity() instanceof Player victime
                && event.getDamager() instanceof LivingEntity agresseur
                && EnchantState.actif("epines_vengeresses")) {

            int total = 0;
            for (ItemStack piece : victime.getInventory().getArmorContents()) {
                total += EnchantIndex.niveau(piece, "epines_vengeresses");
            }
            if (total > 0 && Util.cooldownPret(victime.getUniqueId(), "epines", 500)) {
                agresseur.damage(0.75 * total); // dégâts sans attribution : pas de boucle
                agresseur.addPotionEffect(new PotionEffect(
                        PotionEffectType.WEAKNESS, 40, 0, true, true));
                agresseur.getWorld().playSound(agresseur.getLocation(),
                        Sound.ENCHANT_THORNS_HIT, 1.0f, 1.0f);
                agresseur.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                        agresseur.getLocation().add(0, 1, 0), 6, 0.2, 0.3, 0.2, 0.1);
            }
        }
    }

    // ---- Décapiteur : drop de tête à la mort ----
    private static final Map<EntityType, Material> TETES = Map.of(
            EntityType.ZOMBIE, Material.ZOMBIE_HEAD,
            EntityType.SKELETON, Material.SKELETON_SKULL,
            EntityType.CREEPER, Material.CREEPER_HEAD,
            EntityType.WITHER_SKELETON, Material.WITHER_SKELETON_SKULL,
            EntityType.PIGLIN, Material.PIGLIN_HEAD
    );

    @EventHandler(ignoreCancelled = true)
    public void onMort(EntityDeathEvent event) {
        Player tueur = event.getEntity().getKiller();
        if (tueur == null || !EnchantState.actif("decapiteur")) return;

        int lvl = EnchantIndex.niveau(tueur.getInventory().getItemInMainHand(), "decapiteur");
        if (lvl <= 0) return;

        double chance = plugin.getConfig().getDouble(
                "effets.decapiteur.chance-par-niveau", 0.05) * lvl;
        if (ThreadLocalRandom.current().nextDouble() >= chance) return;

        if (event.getEntity() instanceof Player victime) {
            ItemStack tete = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) tete.getItemMeta();
            meta.setOwningPlayer(victime);
            tete.setItemMeta(meta);
            event.getDrops().add(tete);
        } else {
            Material tete = TETES.get(event.getEntityType());
            if (tete != null) event.getDrops().add(new ItemStack(tete));
        }

        event.getEntity().getWorld().playSound(event.getEntity().getLocation(),
                Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, 0.8f);
        event.getEntity().getWorld().spawnParticle(Particle.CRIT,
                event.getEntity().getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
    }
}
