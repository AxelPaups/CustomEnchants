package com.neosoma.customenchants.enchant;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Résolution runtime : id custom <-> instance Enchantment du registre.
 * Initialisé dans onEnable (le registre est figé et complet à ce moment-là).
 */
@SuppressWarnings("UnstableApiUsage")
public final class EnchantIndex {

    private static final Map<String, Enchantment> PAR_ID_RUNTIME = new LinkedHashMap<>();
    private static final Map<Enchantment, CEnchant> REVERSE = new HashMap<>();

    private EnchantIndex() {}

    public static void init() {
        final Registry<Enchantment> registre = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT);

        for (CEnchant ce : Enchants.ALL) {
            Enchantment ench = registre.getOrThrow(TypedKey.create(
                    RegistryKey.ENCHANTMENT, Key.key("customenchants", ce.id())));
            PAR_ID_RUNTIME.put(ce.id(), ench);
            REVERSE.put(ench, ce);
        }
    }

    /** L'instance Enchantment du registre pour un id custom. */
    public static Enchantment get(String id) {
        return PAR_ID_RUNTIME.get(id);
    }

    /** La définition custom correspondant à un Enchantment, ou null si vanilla. */
    public static CEnchant ceOf(Enchantment ench) {
        return REVERSE.get(ench);
    }

    public static boolean estCustom(Enchantment ench) {
        return REVERSE.containsKey(ench);
    }

    /** Niveau d'un enchantement custom sur un item (0 si absent ou item null). */
    public static int niveau(ItemStack item, String id) {
        if (item == null || item.getType().isAir()) return 0;
        Enchantment ench = PAR_ID_RUNTIME.get(id);
        if (ench == null) return 0;
        return item.getEnchantmentLevel(ench);
    }

    /** Tous les enchantements custom présents sur un item (y compris stockés dans un livre). */
    public static Map<CEnchant, Integer> customsSur(ItemStack item) {
        Map<CEnchant, Integer> resultat = new LinkedHashMap<>();
        if (item == null || item.getType().isAir()) return resultat;

        item.getEnchantments().forEach((ench, lvl) -> {
            CEnchant ce = REVERSE.get(ench);
            if (ce != null) resultat.put(ce, lvl);
        });

        if (item.getItemMeta() instanceof EnchantmentStorageMeta stockage) {
            stockage.getStoredEnchants().forEach((ench, lvl) -> {
                CEnchant ce = REVERSE.get(ench);
                if (ce != null) resultat.put(ce, lvl);
            });
        }
        return resultat;
    }
}
