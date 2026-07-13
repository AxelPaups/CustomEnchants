package com.neosoma.customenchants.enchant;

import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.EnchantmentKeys;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.neosoma.customenchants.enchant.Categorie.*;
import static com.neosoma.customenchants.enchant.Cible.*;
import static com.neosoma.customenchants.enchant.Rarity.*;

/** Catalogue central des enchantements custom du plugin. */
public final class Enchants {

    private Enchants() {}

    // Groupes d'exclusivité mutuelle (anti-cumul des combos trop puissants)
    public static final String GRP_GROS_DEGATS = "gros_degats";
    public static final String GRP_ELEMENTAIRE = "elementaire";
    public static final String GRP_FLECHE = "fleche";
    public static final String GRP_RECOLTE = "recolte";
    public static final String GRP_SAUVE_VIE = "sauve_vie";
    public static final String GRP_BOTTES = "bottes_mobilite";

    private static final List<TypedKey<Enchantment>> AUCUN = List.of();

    public static final List<CEnchant> ALL = List.of(

        // ================= COMBAT (épées) =================
        new CEnchant("vampirisme", "Vampirisme",
            "Vole des points de vie à la cible à chaque coup porté.",
            RARE, 3, COMBAT, EPEE, null, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("foudroiement", "Foudroiement",
            "Chance d'invoquer la foudre sur la cible frappée.",
            EPIQUE, 2, COMBAT, EPEE, GRP_ELEMENTAIRE, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("venin", "Venin",
            "Empoisonne la cible pendant quelques secondes.",
            COMMUN, 3, COMBAT, EPEE, GRP_ELEMENTAIRE, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("gel", "Gel",
            "Gèle et ralentit fortement la cible frappée.",
            RARE, 2, COMBAT, EPEE, GRP_ELEMENTAIRE, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("decapiteur", "Décapiteur",
            "Chance de faire tomber la tête de la victime.",
            RARE, 3, COMBAT, EPEE, null, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("rage", "Rage",
            "Inflige plus de dégâts quand vos PV sont bas.",
            EPIQUE, 3, COMBAT, EPEE, GRP_GROS_DEGATS, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("execution", "Exécution",
            "Inflige plus de dégâts aux cibles presque mortes.",
            LEGENDAIRE, 3, COMBAT, EPEE, GRP_GROS_DEGATS, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("vague", "Vague",
            "Vos coups blessent aussi les ennemis proches de la cible.",
            EPIQUE, 2, COMBAT, EPEE, null, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("affaiblissement", "Affaiblissement",
            "Affaiblit les coups de la cible frappée.",
            COMMUN, 2, COMBAT, EPEE, GRP_ELEMENTAIRE, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("embrasement", "Embrasement+",
            "Enflamme la cible plus longtemps qu'Aura de feu.",
            COMMUN, 3, COMBAT, EPEE, GRP_ELEMENTAIRE, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("chasseur", "Chasseur",
            "Inflige plus de dégâts aux monstres hostiles.",
            COMMUN, 3, COMBAT, EPEE, null, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("faucheuse", "Faucheuse",
            "Chance de coup critique dévastateur, accompagné d'âmes.",
            MYTHIQUE, 1, COMBAT, EPEE, GRP_GROS_DEGATS, AUCUN, Material.NETHERITE_SWORD),

        // ================= ARCS / ARBALÈTES =================
        new CEnchant("fleches_multiples", "Flèches multiples",
            "Tire plusieurs flèches en éventail à chaque tir.",
            EPIQUE, 2, Categorie.ARC, Cible.ARC, GRP_FLECHE, AUCUN, Material.BOW),
        new CEnchant("tracante", "Traçante",
            "La flèche corrige sa trajectoire vers la cible la plus proche.",
            RARE, 1, Categorie.ARC, Cible.ARC, GRP_FLECHE, AUCUN, Material.BOW),
        new CEnchant("explosive", "Explosive",
            "La flèche explose à l'impact, sans casser les blocs.",
            LEGENDAIRE, 2, Categorie.ARC, Cible.ARC, GRP_FLECHE, AUCUN, Material.BOW),
        new CEnchant("grappin", "Grappin",
            "La flèche vous tire vers son point d'impact.",
            RARE, 1, Categorie.ARC, Cible.ARC, null, AUCUN, Material.BOW),
        new CEnchant("glaciale", "Glaciale",
            "La flèche ralentit fortement la cible touchée.",
            COMMUN, 2, Categorie.ARC, Cible.ARC, null, AUCUN, Material.BOW),
        new CEnchant("pluie", "Pluie",
            "Fait pleuvoir une volée de flèches sur la zone d'impact.",
            EPIQUE, 1, Categorie.ARC, Cible.ARC, GRP_FLECHE, AUCUN, Material.BOW),

        // ================= TRIDENT =================
        new CEnchant("harpon", "Harpon",
            "Un trident lancé qui touche une cible la tire vers vous.",
            RARE, 2, Categorie.TRIDENT, Cible.TRIDENT, null, AUCUN, Material.TRIDENT),
        new CEnchant("tempete", "Tempête",
            "Chance d'invoquer la foudre sur la cible frappée, mêlée ou lancer, par tout temps.",
            EPIQUE, 2, Categorie.TRIDENT, Cible.TRIDENT, null, AUCUN, Material.TRIDENT),
        new CEnchant("leviathan", "Léviathan",
            "Le coup fait déferler une vague qui repousse les ennemis proches et vous porte.",
            MYTHIQUE, 1, Categorie.TRIDENT, Cible.TRIDENT, null, AUCUN, Material.TRIDENT),

        // ================= OUTILS =================
        new CEnchant("excavation", "Excavation",
            "Mine en zone 3x3 (5x5 au niveau 2).",
            EPIQUE, 2, OUTIL, MINAGE, GRP_RECOLTE,
            List.of(EnchantmentKeys.SILK_TOUCH), Material.NETHERITE_PICKAXE),
        new CEnchant("auto_fonte", "Auto-fonte",
            "Les minerais sont fondus directement à la récolte.",
            RARE, 1, OUTIL, MINAGE, null,
            List.of(EnchantmentKeys.FORTUNE, EnchantmentKeys.SILK_TOUCH), Material.NETHERITE_PICKAXE),
        new CEnchant("telekinesie", "Télékinésie",
            "Les blocs minés vont directement dans votre inventaire.",
            RARE, 1, OUTIL, MINAGE, null, AUCUN, Material.NETHERITE_PICKAXE),
        new CEnchant("experience_plus", "Expérience+",
            "Augmente l'expérience gagnée en minant.",
            COMMUN, 3, OUTIL, MINAGE, null, AUCUN, Material.NETHERITE_PICKAXE),
        new CEnchant("bucheron", "Bûcheron",
            "Abat l'arbre entier en cassant une seule bûche.",
            RARE, 1, OUTIL, HACHE, GRP_RECOLTE, AUCUN, Material.NETHERITE_AXE),
        new CEnchant("moissonneur", "Moissonneur",
            "Récolte les cultures mûres en zone et les replante.",
            COMMUN, 2, OUTIL, HOUE, null, AUCUN, Material.NETHERITE_HOE),
        new CEnchant("prospecteur", "Prospecteur",
            "Chance de doubler les minerais rares récoltés.",
            LEGENDAIRE, 3, OUTIL, MINAGE, null,
            List.of(EnchantmentKeys.SILK_TOUCH), Material.NETHERITE_PICKAXE),
        new CEnchant("marteau_piqueur", "Marteau-piqueur",
            "Confère Célérité en minant dans les profondeurs (sous Y=0).",
            COMMUN, 1, OUTIL, MINAGE, GRP_RECOLTE, AUCUN, Material.NETHERITE_PICKAXE),
        new CEnchant("sillage", "Sillage",
            "La houe laboure la terre en zone 3x3.",
            COMMUN, 1, OUTIL, HOUE, null, AUCUN, Material.NETHERITE_HOE),
        new CEnchant("mineur_de_lend", "Mineur de l'End",
            "Confère Célérité II dans le Nether et l'End.",
            EPIQUE, 1, OUTIL, MINAGE, null, AUCUN, Material.NETHERITE_PICKAXE),

        // ================= ARMURES =================
        new CEnchant("regenerescence", "Régénérescence",
            "Régénère vos PV si le set complet porte cet enchantement.",
            EPIQUE, 2, Categorie.ARMURE,Cible.ARMURE, null, AUCUN, Material.NETHERITE_CHESTPLATE),
        new CEnchant("velocite", "Vélocité",
            "Augmente votre vitesse de déplacement.",
            COMMUN, 3, Categorie.ARMURE,BOTTES, GRP_BOTTES, AUCUN, Material.NETHERITE_BOOTS),
        new CEnchant("poigne_du_golem", "Poigne du golem",
            "Réduit fortement le recul subi.",
            RARE, 2, Categorie.ARMURE,PLASTRON, null, AUCUN, Material.NETHERITE_CHESTPLATE),
        new CEnchant("epines_vengeresses", "Épines vengeresses",
            "Renvoie des dégâts et affaiblit vos attaquants.",
            RARE, 2, Categorie.ARMURE,Cible.ARMURE, null, AUCUN, Material.NETHERITE_CHESTPLATE),
        new CEnchant("sauteur", "Sauteur",
            "Augmente la hauteur de vos sauts.",
            COMMUN, 2, Categorie.ARMURE,BOTTES, GRP_BOTTES, AUCUN, Material.NETHERITE_BOOTS),
        new CEnchant("nyctalope", "Nyctalope",
            "Confère la vision nocturne permanente.",
            COMMUN, 1, Categorie.ARMURE,CASQUE, null, AUCUN, Material.NETHERITE_HELMET),
        new CEnchant("branchies", "Branchies",
            "Permet de respirer indéfiniment sous l'eau.",
            RARE, 1, Categorie.ARMURE,CASQUE, null, AUCUN, Material.NETHERITE_HELMET),
        new CEnchant("ancrage", "Ancrage",
            "Immunise contre le recul et les déplacements forcés.",
            EPIQUE, 1, Categorie.ARMURE,PLASTRON, null, AUCUN, Material.NETHERITE_CHESTPLATE),
        new CEnchant("dernier_souffle", "Dernier souffle",
            "À 10% de PV : absorption et résistance d'urgence (5 min de recharge).",
            LEGENDAIRE, 1, Categorie.ARMURE,PLASTRON, GRP_SAUVE_VIE, AUCUN, Material.NETHERITE_CHESTPLATE),
        new CEnchant("avatar", "Avatar",
            "Set complet : accorde un bonus adapté à votre environnement.",
            MYTHIQUE, 1, Categorie.ARMURE,Cible.ARMURE, GRP_SAUVE_VIE, AUCUN, Material.NETHERITE_CHESTPLATE),

        // ================= DIVERS =================
        new CEnchant("ame_liee", "Âme liée+",
            "L'item revient dans votre inventaire après la mort.",
            LEGENDAIRE, 1, DIVERS, DURABLE, null, AUCUN, Material.NETHERITE_SWORD),
        new CEnchant("reparation_dame", "Réparation d'âme",
            "L'item se répare à chaque créature tuée.",
            LEGENDAIRE, 1, DIVERS, DURABLE, null,
            List.of(EnchantmentKeys.MENDING), Material.NETHERITE_SWORD)
    );

    /** Index id -> définition. */
    public static final Map<String, CEnchant> PAR_ID =
            ALL.stream().collect(Collectors.toUnmodifiableMap(CEnchant::id, e -> e));

    public static CEnchant parId(String id) {
        return PAR_ID.get(id);
    }
}
