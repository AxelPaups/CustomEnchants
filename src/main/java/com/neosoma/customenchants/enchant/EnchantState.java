package com.neosoma.customenchants.enchant;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * État activé/désactivé de chaque enchantement, persisté dans states.yml.
 * Un enchantement désactivé reste enregistré (les items des joueurs ne cassent pas)
 * mais son effet est coupé et il n'est plus obtenable.
 */
public final class EnchantState {

    private static final Map<String, Boolean> ETATS = new ConcurrentHashMap<>();
    private static File fichier;

    private EnchantState() {}

    public static void charger(JavaPlugin plugin) {
        fichier = new File(plugin.getDataFolder(), "states.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        for (CEnchant ce : Enchants.ALL) {
            ETATS.put(ce.id(), yaml.getBoolean(ce.id(), true));
        }
    }

    public static void sauvegarder() {
        if (fichier == null) return;
        YamlConfiguration yaml = new YamlConfiguration();
        ETATS.forEach(yaml::set);
        try {
            yaml.save(fichier);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean actif(String id) {
        return ETATS.getOrDefault(id, true);
    }

    public static void definir(String id, boolean actif) {
        ETATS.put(id, actif);
        sauvegarder();
    }

    public static boolean basculer(String id) {
        boolean nouveau = !actif(id);
        definir(id, nouveau);
        return nouveau;
    }

    public static void toutDefinir(boolean actif) {
        for (CEnchant ce : Enchants.ALL) ETATS.put(ce.id(), actif);
        sauvegarder();
    }

    public static long nbActifs() {
        return ETATS.values().stream().filter(b -> b).count();
    }
}
