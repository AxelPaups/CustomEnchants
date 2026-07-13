package com.neosoma.customenchants;

import com.neosoma.customenchants.enchant.CEnchant;
import com.neosoma.customenchants.enchant.Enchants;
import com.neosoma.customenchants.enchant.Rarity;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.EnchantmentRegistryEntry;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import io.papermc.paper.registry.keys.ItemTypeKeys;
import io.papermc.paper.registry.keys.tags.EnchantmentTagKeys;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.set.RegistryKeySet;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Bootstrapper : enregistre les enchantements custom dans le registre du jeu
 * au démarrage du serveur, puis les ajoute au tag de la table d'enchantement
 * (sauf les Mythiques, obtenables uniquement via loot/admin).
 */
@SuppressWarnings("UnstableApiUsage")
public class CEBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {

        // Clés typées de tous nos enchantements
        final Map<String, TypedKey<Enchantment>> keys = new HashMap<>();
        for (CEnchant ce : Enchants.ALL) {
            keys.put(ce.id(), EnchantmentKeys.create(Key.key("customenchants", ce.id())));
        }

        // Groupes d'exclusivité mutuelle
        final Map<String, List<TypedKey<Enchantment>>> groupes = new HashMap<>();
        for (CEnchant ce : Enchants.ALL) {
            if (ce.groupeExclusif() != null) {
                groupes.computeIfAbsent(ce.groupeExclusif(), g -> new ArrayList<>())
                       .add(keys.get(ce.id()));
            }
        }

        // ---- Enregistrement des enchantements ----
        context.getLifecycleManager().registerEventHandler(
                RegistryEvents.ENCHANTMENT.compose().newHandler(event -> {

            for (CEnchant ce : Enchants.ALL) {
                final TypedKey<Enchantment> key = keys.get(ce.id());
                final Rarity r = ce.rarete();

                // Ensemble exclusif : autres membres du groupe + conflits vanilla
                final List<TypedKey<Enchantment>> exclus = new ArrayList<>();
                if (ce.groupeExclusif() != null) {
                    for (TypedKey<Enchantment> k : groupes.get(ce.groupeExclusif())) {
                        if (!k.equals(key)) exclus.add(k);
                    }
                }
                exclus.addAll(ce.conflitsVanilla());

                // Items supportés selon la cible
                final RegistryKeySet<ItemType> items = switch (ce.cible()) {
                    case EPEE -> event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_SWORD);
                    case ARC -> RegistrySet.keySet(RegistryKey.ITEM,
                            List.of(ItemTypeKeys.BOW, ItemTypeKeys.CROSSBOW));
                    case TRIDENT -> RegistrySet.keySet(RegistryKey.ITEM, List.of(ItemTypeKeys.TRIDENT));
                    case MINAGE -> event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_MINING);
                    case HACHE -> RegistrySet.keySet(RegistryKey.ITEM, List.of(
                            ItemTypeKeys.WOODEN_AXE, ItemTypeKeys.STONE_AXE, ItemTypeKeys.IRON_AXE,
                            ItemTypeKeys.GOLDEN_AXE, ItemTypeKeys.DIAMOND_AXE, ItemTypeKeys.NETHERITE_AXE));
                    case HOUE -> RegistrySet.keySet(RegistryKey.ITEM, List.of(
                            ItemTypeKeys.WOODEN_HOE, ItemTypeKeys.STONE_HOE, ItemTypeKeys.IRON_HOE,
                            ItemTypeKeys.GOLDEN_HOE, ItemTypeKeys.DIAMOND_HOE, ItemTypeKeys.NETHERITE_HOE));
                    case ARMURE -> event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_ARMOR);
                    case CASQUE -> event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_HEAD_ARMOR);
                    case PLASTRON -> event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_CHEST_ARMOR);
                    case JAMBIERES -> event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_LEG_ARMOR);
                    case BOTTES -> event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_FOOT_ARMOR);
                    case DURABLE -> event.getOrCreateTag(ItemTypeTagKeys.ENCHANTABLE_DURABILITY);
                };

                event.registry().register(key, b -> b
                        .description(Component.text(ce.nom()))
                        .supportedItems(items)
                        .weight(r.poidsTable())
                        .maxLevel(ce.niveauMax())
                        .minimumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                r.coutMinBase(), r.coutMinParNiveau()))
                        .maximumCost(EnchantmentRegistryEntry.EnchantmentCost.of(
                                r.coutMaxBase(), r.coutMaxParNiveau()))
                        .anvilCost(r.coutEnclume())
                        .activeSlots(ce.cible().slot())
                        .exclusiveWith(RegistrySet.keySet(RegistryKey.ENCHANTMENT, exclus))
                );
            }
        }));

        // ---- Tag "in_enchanting_table" : tous sauf Mythique ----
        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.ENCHANTMENT), event -> {

            final Set<TypedKey<Enchantment>> enTable = Enchants.ALL.stream()
                    .filter(ce -> ce.rarete() != Rarity.MYTHIQUE)
                    .map(ce -> keys.get(ce.id()))
                    .collect(Collectors.toSet());

            event.registrar().addToTag(EnchantmentTagKeys.IN_ENCHANTING_TABLE, enTable);
        });
    }
}
