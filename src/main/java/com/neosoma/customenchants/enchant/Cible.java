package com.neosoma.customenchants.enchant;

import org.bukkit.inventory.EquipmentSlotGroup;

/** Items sur lesquels l'enchantement peut s'appliquer, et slot d'activation. */
public enum Cible {
    EPEE(EquipmentSlotGroup.MAINHAND, "Épées"),
    ARC(EquipmentSlotGroup.MAINHAND, "Arcs et arbalètes"),
    MINAGE(EquipmentSlotGroup.MAINHAND, "Outils de minage"),
    HACHE(EquipmentSlotGroup.MAINHAND, "Haches"),
    HOUE(EquipmentSlotGroup.MAINHAND, "Houes"),
    ARMURE(EquipmentSlotGroup.ARMOR, "Armure (toutes pièces)"),
    CASQUE(EquipmentSlotGroup.HEAD, "Casques"),
    PLASTRON(EquipmentSlotGroup.CHEST, "Plastrons"),
    JAMBIERES(EquipmentSlotGroup.LEGS, "Jambières"),
    BOTTES(EquipmentSlotGroup.FEET, "Bottes"),
    DURABLE(EquipmentSlotGroup.ANY, "Tout item avec durabilité");

    private final EquipmentSlotGroup slot;
    private final String label;

    Cible(EquipmentSlotGroup slot, String label) {
        this.slot = slot;
        this.label = label;
    }

    public EquipmentSlotGroup slot() { return slot; }
    public String label() { return label; }
}
