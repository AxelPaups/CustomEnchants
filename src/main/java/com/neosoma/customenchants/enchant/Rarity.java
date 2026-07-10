package com.neosoma.customenchants.enchant;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Raretés des enchantements custom.
 * Détermine le poids dans la table d'enchantement, la couleur d'affichage,
 * le poids de tirage dans le loot et les coûts (table + enclume).
 */
public enum Rarity {

    COMMUN("Commun", NamedTextColor.WHITE, 10, 40, 2, 5, 3, 25, 5, 5),
    RARE("Rare", NamedTextColor.BLUE, 5, 30, 4, 8, 4, 30, 6, 10),
    EPIQUE("Épique", NamedTextColor.DARK_PURPLE, 2, 18, 6, 12, 5, 35, 7, 18),
    LEGENDAIRE("Légendaire", NamedTextColor.GOLD, 1, 10, 8, 20, 6, 45, 8, 25),
    MYTHIQUE("Mythique", NamedTextColor.RED, 1, 2, 10, 25, 8, 50, 10, 35);

    private final String label;
    private final TextColor couleur;
    private final int poidsTable;      // poids dans la table d'enchantement (comme vanilla)
    private final int poidsLoot;       // poids de tirage dans les coffres
    private final int coutEnclume;     // anvilCost vanilla
    private final int coutMinBase;
    private final int coutMinParNiveau;
    private final int coutMaxBase;
    private final int coutMaxParNiveau;
    private final int surchargeEnclume; // base du coût XP progressif (anti-cumul)

    Rarity(String label, TextColor couleur, int poidsTable, int poidsLoot, int coutEnclume,
           int coutMinBase, int coutMinParNiveau, int coutMaxBase, int coutMaxParNiveau,
           int surchargeEnclume) {
        this.label = label;
        this.couleur = couleur;
        this.poidsTable = poidsTable;
        this.poidsLoot = poidsLoot;
        this.coutEnclume = coutEnclume;
        this.coutMinBase = coutMinBase;
        this.coutMinParNiveau = coutMinParNiveau;
        this.coutMaxBase = coutMaxBase;
        this.coutMaxParNiveau = coutMaxParNiveau;
        this.surchargeEnclume = surchargeEnclume;
    }

    public String label() { return label; }
    public TextColor couleur() { return couleur; }
    public int poidsTable() { return poidsTable; }
    public int poidsLoot() { return poidsLoot; }
    public int coutEnclume() { return coutEnclume; }
    public int coutMinBase() { return coutMinBase; }
    public int coutMinParNiveau() { return coutMinParNiveau; }
    public int coutMaxBase() { return coutMaxBase; }
    public int coutMaxParNiveau() { return coutMaxParNiveau; }
    public int surchargeEnclume() { return surchargeEnclume; }
}
