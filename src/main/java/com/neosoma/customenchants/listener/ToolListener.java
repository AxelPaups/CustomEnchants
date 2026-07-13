package com.neosoma.customenchants.listener;

import com.neosoma.customenchants.CustomEnchantsPlugin;
import com.neosoma.customenchants.enchant.EnchantIndex;
import com.neosoma.customenchants.enchant.EnchantState;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Effets des enchantements d'outils. */
public class ToolListener implements Listener {

    private final CustomEnchantsPlugin plugin;

    /** Garde anti-récursion : breakBlock() redéclenche BlockBreakEvent. */
    private boolean enTraitement = false;

    public ToolListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    private int niv(ItemStack outil, String id) {
        return EnchantState.actif(id) ? EnchantIndex.niveau(outil, id) : 0;
    }

    // =========================================================
    //  Casse de bloc : excavation, bûcheron, moissonneur, XP+, marteau-piqueur
    // =========================================================
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCasse(BlockBreakEvent event) {
        if (enTraitement) return;

        Player joueur = event.getPlayer();
        ItemStack outil = joueur.getInventory().getItemInMainHand();
        Block bloc = event.getBlock();
        int lvl;

        // Expérience+
        if ((lvl = niv(outil, "experience_plus")) > 0 && event.getExpToDrop() > 0) {
            double bonus = plugin.getConfig().getDouble(
                    "effets.experience_plus.bonus-par-niveau", 0.25) * lvl;
            event.setExpToDrop((int) Math.ceil(event.getExpToDrop() * (1 + bonus)));
        }

        // Marteau-piqueur : Célérité sous Y=0
        if (niv(outil, "marteau_piqueur") > 0 && bloc.getY() < 0) {
            joueur.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, 100, 0, true, false));
        }

        // Excavation : 3x3 (5x5 au niveau 2)
        if ((lvl = niv(outil, "excavation")) > 0 && !joueur.isSneaking()
                && bloc.isPreferredTool(outil)) {
            int rayon = lvl >= 2 ? 2 : 1;
            casserEnZone(joueur, bloc, outil, rayon);
        }

        // Bûcheron : abat l'arbre entier
        if (niv(outil, "bucheron") > 0 && !joueur.isSneaking()
                && Tag.LOGS.isTagged(bloc.getType())) {
            abattreArbre(joueur, bloc);
        }

        // Moissonneur : récolte en zone + replante
        if ((lvl = niv(outil, "moissonneur")) > 0) {
            moissonner(joueur, bloc, outil, lvl);
        }
    }

    private void casserEnZone(Player joueur, Block centre, ItemStack outil, int rayon) {
        float pitch = joueur.getLocation().getPitch();
        float yaw = joueur.getLocation().getYaw();

        enTraitement = true;
        try {
            for (int a = -rayon; a <= rayon; a++) {
                for (int b = -rayon; b <= rayon; b++) {
                    if (a == 0 && b == 0) continue;
                    Block cible;
                    if (Math.abs(pitch) > 45) {
                        // Regard vers le haut/bas : plan horizontal
                        cible = centre.getRelative(a, 0, b);
                    } else {
                        float y = (yaw % 360 + 360) % 360;
                        boolean axeX = (y >= 45 && y < 135) || (y >= 225 && y < 315);
                        // Face est/ouest : plan vertical Y-Z, sinon Y-X
                        cible = axeX ? centre.getRelative(0, a, b) : centre.getRelative(b, a, 0);
                    }
                    if (cassable(cible, outil)) {
                        joueur.breakBlock(cible); // déclenche les events : respecte les protections
                    }
                }
            }
        } finally {
            enTraitement = false;
        }
    }

    private boolean cassable(Block bloc, ItemStack outil) {
        Material type = bloc.getType();
        if (type.isAir() || !type.isSolid()) return false;
        if (type.getHardness() < 0) return false;          // bedrock & co
        if (bloc.getState() instanceof org.bukkit.block.Container) return false; // coffres etc.
        return bloc.isPreferredTool(outil);
    }

    private void abattreArbre(Player joueur, Block depart) {
        Set<Block> visites = new HashSet<>();
        Deque<Block> aTraiter = new ArrayDeque<>();
        aTraiter.add(depart);
        visites.add(depart);
        int casses = 0;

        enTraitement = true;
        try {
            while (!aTraiter.isEmpty() && casses < 80) {
                Block courant = aTraiter.poll();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = 0; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            Block voisin = courant.getRelative(dx, dy, dz);
                            if (!visites.contains(voisin)
                                    && Tag.LOGS.isTagged(voisin.getType())) {
                                visites.add(voisin);
                                aTraiter.add(voisin);
                                joueur.breakBlock(voisin);
                                casses++;
                            }
                        }
                    }
                }
            }
        } finally {
            enTraitement = false;
        }
    }

    private void moissonner(Player joueur, Block centre, ItemStack outil, int lvl) {
        boolean replanter = plugin.getConfig().getBoolean("effets.moissonneur.replanter", true);
        int rayon = lvl;
        Material typeCentre = centre.getType();

        // Replante le bloc cassé au tick suivant
        if (replanter && estCultureMure(centre)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (centre.getType().isAir()) centre.setType(typeCentre);
            });
        }

        enTraitement = true;
        try {
            for (int dx = -rayon; dx <= rayon; dx++) {
                for (int dz = -rayon; dz <= rayon; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Block voisin = centre.getRelative(dx, 0, dz);
                    if (estCultureMure(voisin)) {
                        Material type = voisin.getType();
                        joueur.breakBlock(voisin);
                        if (replanter && voisin.getType().isAir()) {
                            voisin.setType(type); // âge 0 par défaut
                        }
                    }
                }
            }
        } finally {
            enTraitement = false;
        }
    }

    private boolean estCultureMure(Block bloc) {
        return Tag.CROPS.isTagged(bloc.getType())
                && bloc.getBlockData() instanceof Ageable age
                && age.getAge() >= age.getMaximumAge();
    }

    // =========================================================
    //  Drops : auto-fonte, prospecteur, télékinésie
    // =========================================================
    private static final Map<Material, Material> FONTE = Map.of(
            Material.RAW_IRON, Material.IRON_INGOT,
            Material.RAW_GOLD, Material.GOLD_INGOT,
            Material.RAW_COPPER, Material.COPPER_INGOT,
            Material.COBBLESTONE, Material.STONE,
            Material.SAND, Material.GLASS
    );

    private static final Set<Material> MINERAIS_RARES = EnumSet.of(
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.NETHER_GOLD_ORE, Material.ANCIENT_DEBRIS
    );

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrops(BlockDropItemEvent event) {
        Player joueur = event.getPlayer();
        ItemStack outil = joueur.getInventory().getItemInMainHand();
        Material typeBloc = event.getBlockState().getType();

        // Auto-fonte
        if (niv(outil, "auto_fonte") > 0) {
            for (Item drop : event.getItems()) {
                ItemStack stack = drop.getItemStack();
                Material fondu = FONTE.get(stack.getType());
                if (fondu != null) {
                    drop.setItemStack(new ItemStack(fondu, stack.getAmount()));
                }
            }
        }

        // Prospecteur : chance de doubler les minerais rares
        int prospecteur = niv(outil, "prospecteur");
        if (prospecteur > 0 && MINERAIS_RARES.contains(typeBloc)) {
            double chance = plugin.getConfig().getDouble(
                    "effets.prospecteur.chance-par-niveau", 0.10) * prospecteur;
            if (ThreadLocalRandom.current().nextDouble() < chance) {
                for (Item drop : event.getItems()) {
                    ItemStack stack = drop.getItemStack();
                    stack.setAmount(Math.min(stack.getMaxStackSize(), stack.getAmount() * 2));
                    drop.setItemStack(stack);
                }
                joueur.getWorld().playSound(joueur.getLocation(),
                        Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                joueur.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                        joueur.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0);
            }
        }

        // Télékinésie : drops directement dans l'inventaire
        if (niv(outil, "telekinesie") > 0) {
            event.getItems().removeIf(drop -> {
                var restes = joueur.getInventory().addItem(drop.getItemStack());
                if (restes.isEmpty()) return true;
                drop.setItemStack(restes.values().iterator().next());
                return false;
            });
        }
    }

    // =========================================================
    //  Sillage : labourage 3x3
    // =========================================================
    private static final Set<Material> LABOURABLE = EnumSet.of(
            Material.GRASS_BLOCK, Material.DIRT, Material.DIRT_PATH);

    @EventHandler(ignoreCancelled = true)
    public void onLabour(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block clique = event.getClickedBlock();
        if (clique == null || !LABOURABLE.contains(clique.getType())) return;

        ItemStack outil = event.getItem();
        if (outil == null || niv(outil, "sillage") <= 0) return;

        // Au tick suivant, si le bloc cliqué est devenu de la terre labourée, on étend en 3x3
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (clique.getType() != Material.FARMLAND) return;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    Block voisin = clique.getRelative(dx, 0, dz);
                    if (LABOURABLE.contains(voisin.getType())
                            && voisin.getRelative(0, 1, 0).getType().isAir()) {
                        voisin.setType(Material.FARMLAND);
                    }
                }
            }
        });
    }
}
