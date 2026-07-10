package com.neosoma.customenchants.util;

import com.neosoma.customenchants.enchant.CEnchant;
import com.neosoma.customenchants.enchant.EnchantIndex;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Petites fonctions utilitaires partagées. */
public final class Util {

    private static final String[] ROMAINS =
            {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private Util() {}

    public static String romain(int n) {
        return (n >= 1 && n < ROMAINS.length) ? ROMAINS[n] : String.valueOf(n);
    }

    /** Découpe une description en lignes de lore grises (~38 caractères). */
    public static List<Component> loreDescription(String texte) {
        List<Component> lignes = new ArrayList<>();
        StringBuilder ligne = new StringBuilder();
        for (String mot : texte.split(" ")) {
            if (ligne.length() + mot.length() + 1 > 38) {
                lignes.add(ligneGrise(ligne.toString()));
                ligne = new StringBuilder();
            }
            if (!ligne.isEmpty()) ligne.append(' ');
            ligne.append(mot);
        }
        if (!ligne.isEmpty()) lignes.add(ligneGrise(ligne.toString()));
        return lignes;
    }

    public static Component ligneGrise(String texte) {
        return Component.text(texte, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }

    /** Crée un livre enchanté custom. */
    public static ItemStack livre(CEnchant ce, int niveau) {
        ItemStack livre = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) livre.getItemMeta();
        meta.addStoredEnchant(EnchantIndex.get(ce.id()), niveau, true);
        List<Component> lore = new ArrayList<>(loreDescription(ce.description()));
        lore.add(Component.text("Rareté : ", NamedTextColor.DARK_GRAY)
                .append(Component.text(ce.rarete().label(), ce.rarete().couleur()))
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        livre.setItemMeta(meta);
        return livre;
    }

    /** Crée un item déjà enchanté (bouton "recevoir l'item" du panel). */
    public static ItemStack itemEnchante(CEnchant ce, int niveau) {
        ItemStack item = new ItemStack(ce.itemExemple());
        item.addUnsafeEnchantment(EnchantIndex.get(ce.id()), niveau);
        return item;
    }

    // ---- Cooldowns génériques (joueur + clé) ----
    private static final Map<UUID, Map<String, Long>> COOLDOWNS = new HashMap<>();

    /** true si le cooldown est écoulé (et le réarme). */
    public static boolean cooldownPret(UUID joueur, String cle, long millis) {
        long maintenant = System.currentTimeMillis();
        Map<String, Long> parCle = COOLDOWNS.computeIfAbsent(joueur, u -> new HashMap<>());
        Long dernier = parCle.get(cle);
        if (dernier != null && maintenant - dernier < millis) return false;
        parCle.put(cle, maintenant);
        return true;
    }

    /** Force la métadonnée d'un item via un consommateur (raccourci). */
    public static void editerMeta(ItemStack item, java.util.function.Consumer<ItemMeta> editeur) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        editeur.accept(meta);
        item.setItemMeta(meta);
    }
}
