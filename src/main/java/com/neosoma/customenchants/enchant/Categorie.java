package com.neosoma.customenchants.enchant;

/** Catégories d'enchantements (utilisées pour le filtre du panel admin). */
public enum Categorie {
    COMBAT("Combat"),
    ARC("Arc"),
    TRIDENT("Trident"),
    OUTIL("Outil"),
    ARMURE("Armure"),
    DIVERS("Divers");

    private final String label;

    Categorie(String label) { this.label = label; }

    public String label() { return label; }
}
