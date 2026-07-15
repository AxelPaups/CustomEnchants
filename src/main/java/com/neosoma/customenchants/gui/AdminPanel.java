package com.neosoma.customenchants.gui;

import com.neosoma.customenchants.enchant.CEnchant;
import com.neosoma.customenchants.enchant.EnchantState;
import com.neosoma.customenchants.enchant.Enchants;
import com.neosoma.customenchants.util.Util;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Construction et ouverture des inventaires du panel admin. */
public final class AdminPanel {

    public static final int TAILLE = 54;
    public static final int PAR_PAGE = 45;

    private AdminPanel() {}

    // =====================================================
    //  Panel principal
    // =====================================================
    public static void ouvrir(Player joueur, PanelHolder holder) {
        List<CEnchant> filtres = Enchants.ALL.stream()
                .filter(ce -> holder.filtreCategorie == null || ce.categorie() == holder.filtreCategorie)
                .filter(ce -> holder.filtreRarete == null || ce.rarete() == holder.filtreRarete)
                .toList();

        int pagesMax = Math.max(1, (int) Math.ceil(filtres.size() / (double) PAR_PAGE));
        if (holder.page >= pagesMax) holder.page = pagesMax - 1;
        if (holder.page < 0) holder.page = 0;

        String titrePanel = holder.admin ? "Panel CustomEnchants" : "Enchantements CustomEnchants";
        Inventory inv = Bukkit.createInventory(holder, TAILLE,
                titre(titrePanel + " — page " + (holder.page + 1) + "/" + pagesMax));
        holder.setInventory(inv);
        holder.idsAffiches.clear();

        int debut = holder.page * PAR_PAGE;
        for (int i = 0; i < PAR_PAGE && debut + i < filtres.size(); i++) {
            CEnchant ce = filtres.get(debut + i);
            holder.idsAffiches.add(ce.id());
            inv.setItem(i, itemEnchant(ce, holder.admin));
        }

        // Barre de navigation (commune aux deux panels)
        if (holder.page > 0) {
            inv.setItem(45, bouton(Material.ARROW, "Page précédente", NamedTextColor.YELLOW));
        }
        inv.setItem(46, bouton(Material.BOOKSHELF,
                "Filtre catégorie : " + (holder.filtreCategorie == null
                        ? "Toutes" : holder.filtreCategorie.label()),
                NamedTextColor.AQUA, "Clic : catégorie suivante"));
        inv.setItem(47, bouton(Material.DIAMOND,
                "Filtre rareté : " + (holder.filtreRarete == null
                        ? "Toutes" : holder.filtreRarete.label()),
                NamedTextColor.AQUA, "Clic : rareté suivante"));
        inv.setItem(49, bouton(Material.NETHER_STAR,
                EnchantState.nbActifs() + "/" + Enchants.ALL.size() + " enchantements actifs",
                NamedTextColor.WHITE));

        // Boutons globaux : réservés au panel admin
        if (holder.admin) {
            inv.setItem(50, bouton(Material.LIME_DYE, "Tout activer", NamedTextColor.GREEN));
            inv.setItem(51, bouton(Material.GRAY_DYE, "Tout désactiver", NamedTextColor.RED));
            inv.setItem(52, bouton(Material.COMPARATOR, "Recharger la config", NamedTextColor.GOLD));
        }
        if (debut + PAR_PAGE < filtres.size()) {
            inv.setItem(53, bouton(Material.ARROW, "Page suivante", NamedTextColor.YELLOW));
        }

        joueur.openInventory(inv);
    }

    private static ItemStack itemEnchant(CEnchant ce, boolean admin) {
        boolean actif = EnchantState.actif(ce.id());
        ItemStack item = new ItemStack(actif ? Material.ENCHANTED_BOOK : Material.BOOK);

        Util.editerMeta(item, meta -> {
            meta.displayName(Component.text(ce.nom(), ce.rarete().couleur())
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(actif ? "  [ACTIF]" : "  [DÉSACTIVÉ]",
                            actif ? NamedTextColor.GREEN : NamedTextColor.RED)));

            List<Component> lore = new ArrayList<>(Util.loreDescription(ce.description()));
            lore.add(Component.empty());
            lore.add(ligne("Rareté : ", ce.rarete().label(), ce.rarete().couleur()));
            lore.add(ligne("Catégorie : ", ce.categorie().label(), NamedTextColor.WHITE));
            lore.add(ligne("Niveau max : ", Util.romain(ce.niveauMax()), NamedTextColor.WHITE));
            lore.add(ligne("S'applique sur : ", ce.cible().label(), NamedTextColor.WHITE));
            if (ce.groupeExclusif() != null) {
                lore.add(Util.ligneGrise("Non cumulable avec son groupe."));
            }
            var bloques = EnchantState.niveauxBloques(ce.id());
            if (!bloques.isEmpty()) {
                String niveaux = bloques.stream().sorted()
                        .map(Util::romain).reduce((a, b) -> a + ", " + b).orElse("");
                lore.add(ligne("Niveaux bloqués : ", niveaux, NamedTextColor.RED));
            }
            if (admin) {
                lore.add(Component.empty());
                lore.add(Component.text("Clic gauche : activer / désactiver", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Clic droit : se donner (livre ou item)", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Shift + clic gauche : bloquer des niveaux", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
        });
        return item;
    }

    // =====================================================
    //  Sous-menu "se donner"
    // =====================================================
    public static void ouvrirGive(Player joueur, GiveHolder holder) {
        CEnchant ce = Enchants.parId(holder.enchantId);
        if (ce == null) return;
        holder.niveau = Math.min(Math.max(1, holder.niveau), ce.niveauMax());

        Inventory inv = Bukkit.createInventory(holder, 27, titre("Se donner : " + ce.nom()));
        holder.setInventory(inv);

        inv.setItem(4, itemEnchant(ce, true));
        inv.setItem(10, bouton(Material.REDSTONE, "Niveau −1", NamedTextColor.RED));
        inv.setItem(13, bouton(Material.EXPERIENCE_BOTTLE,
                "Niveau choisi : " + Util.romain(holder.niveau) + " (max "
                        + Util.romain(ce.niveauMax()) + ")",
                NamedTextColor.WHITE));
        inv.setItem(16, bouton(Material.EMERALD, "Niveau +1", NamedTextColor.GREEN));
        inv.setItem(20, bouton(Material.ENCHANTED_BOOK,
                "Recevoir le livre enchanté", NamedTextColor.LIGHT_PURPLE));
        ItemStack exemple = bouton(ce.itemExemple(),
                "Recevoir l'item déjà enchanté", NamedTextColor.LIGHT_PURPLE);
        inv.setItem(22, exemple);
        inv.setItem(26, bouton(Material.ARROW, "Retour au panel", NamedTextColor.YELLOW));

        joueur.openInventory(inv);
    }

    // =====================================================
    //  Sous-menu "niveaux" : blocage individuel
    // =====================================================
    public static void ouvrirNiveaux(Player joueur, NiveauHolder holder) {
        CEnchant ce = Enchants.parId(holder.enchantId);
        if (ce == null) return;

        int taille = Math.max(9, (int) (Math.ceil((ce.niveauMax() + 1) / 9.0) * 9));
        Inventory inv = Bukkit.createInventory(holder, taille, titre("Niveaux : " + ce.nom()));
        holder.setInventory(inv);

        for (int i = 1; i <= ce.niveauMax(); i++) {
            int niveau = i;
            boolean bloque = EnchantState.niveauBloque(ce.id(), niveau);
            ItemStack item = new ItemStack(bloque ? Material.GRAY_DYE : Material.LIME_DYE);
            Util.editerMeta(item, meta -> {
                meta.displayName(Component.text(ce.nom() + " " + Util.romain(niveau),
                                ce.rarete().couleur())
                        .decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(bloque ? "  [BLOQUÉ]" : "  [ACTIF]",
                                bloque ? NamedTextColor.RED : NamedTextColor.GREEN)));
                meta.lore(List.of(Util.ligneGrise("Clic : activer / bloquer ce niveau")));
            });
            inv.setItem(i - 1, item);
        }
        inv.setItem(taille - 1, bouton(Material.ARROW, "Retour au panel", NamedTextColor.YELLOW));

        joueur.openInventory(inv);
    }

    // =====================================================
    //  Helpers
    // =====================================================
    private static Component titre(String texte) {
        return Component.text(texte, NamedTextColor.DARK_PURPLE);
    }

    private static Component ligne(String cle, String valeur,
                                   net.kyori.adventure.text.format.TextColor couleur) {
        return Component.text(cle, NamedTextColor.DARK_GRAY)
                .append(Component.text(valeur, couleur))
                .decoration(TextDecoration.ITALIC, false);
    }

    private static ItemStack bouton(Material materiau, String nom,
                                    net.kyori.adventure.text.format.TextColor couleur,
                                    String... lore) {
        ItemStack item = new ItemStack(materiau);
        Util.editerMeta(item, meta -> {
            meta.displayName(Component.text(nom, couleur)
                    .decoration(TextDecoration.ITALIC, false));
            if (lore.length > 0) {
                List<Component> lignes = new ArrayList<>();
                for (String l : lore) lignes.add(Util.ligneGrise(l));
                meta.lore(lignes);
            }
        });
        return item;
    }
}
