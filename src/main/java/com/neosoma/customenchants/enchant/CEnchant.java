package com.neosoma.customenchants.enchant;

import io.papermc.paper.registry.TypedKey;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.List;

/**
 * Définition d'un enchantement custom (métadonnées uniquement, l'effet est codé dans les listeners).
 *
 * @param id               identifiant technique (namespace customenchants:id)
 * @param nom              nom affiché en jeu (français)
 * @param description      description française (panel admin, /ce info, lore des livres)
 * @param rarete           rareté (poids table/loot, couleur, coûts)
 * @param niveauMax        niveau maximum
 * @param categorie        catégorie (filtre du panel)
 * @param cible            items compatibles + slot d'activation
 * @param groupeExclusif   groupe d'exclusivité mutuelle (null = cumulable)
 * @param conflitsVanilla  enchantements vanilla incompatibles
 * @param itemExemple      item donné par le bouton "recevoir l'item enchanté" du panel
 */
public record CEnchant(
        String id,
        String nom,
        String description,
        Rarity rarete,
        int niveauMax,
        Categorie categorie,
        Cible cible,
        String groupeExclusif,
        List<TypedKey<Enchantment>> conflitsVanilla,
        Material itemExemple
) {
    public String cle() {
        return "customenchants:" + id;
    }
}
