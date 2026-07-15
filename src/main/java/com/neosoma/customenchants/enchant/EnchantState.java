package com.neosoma.customenchants.enchant;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * État activé/désactivé de chaque enchantement (et de chaque niveau individuellement),
 * persisté dans states.yml. Un enchantement ou un niveau désactivé reste enregistré
 * (les items des joueurs ne cassent pas) mais son effet est coupé et il n'est plus obtenable.
 */
public final class EnchantState {

    private static final Map<String, Boolean> ETATS = new ConcurrentHashMap<>();
    private static final Map<String, Set<Integer>> NIVEAUX_BLOQUES = new ConcurrentHashMap<>();
    private static File fichier;

    private EnchantState() {}

    public static void charger(JavaPlugin plugin) {
        fichier = new File(plugin.getDataFolder(), "states.yml");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(fichier);
        for (CEnchant ce : Enchants.ALL) {
            ETATS.put(ce.id(), yaml.getBoolean(ce.id(), true));
            Set<Integer> bloques = NIVEAUX_BLOQUES.computeIfAbsent(
                    ce.id(), k -> ConcurrentHashMap.newKeySet());
            bloques.addAll(yaml.getIntegerList("niveaux-bloques." + ce.id()));
        }
    }

    public static void sauvegarder() {
        if (fichier == null) return;
        YamlConfiguration yaml = new YamlConfiguration();
        ETATS.forEach(yaml::set);
        NIVEAUX_BLOQUES.forEach((id, niveaux) -> {
            if (!niveaux.isEmpty()) {
                yaml.set("niveaux-bloques." + id, new ArrayList<>(niveaux));
            }
        });
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

    // ---- Blocage par niveau ----

    /** true si ce niveau précis de l'enchantement est bloqué (indépendamment de l'état global). */
    public static boolean niveauBloque(String id, int niveau) {
        Set<Integer> bloques = NIVEAUX_BLOQUES.get(id);
        return bloques != null && bloques.contains(niveau);
    }

    /** true si l'enchantement est actif ET que ce niveau précis n'est pas bloqué. */
    public static boolean niveauActif(String id, int niveau) {
        return actif(id) && !niveauBloque(id, niveau);
    }

    /** Bascule le blocage d'un niveau précis. Renvoie true si le niveau est désormais actif. */
    public static boolean basculerNiveau(String id, int niveau) {
        Set<Integer> bloques = NIVEAUX_BLOQUES.computeIfAbsent(id, k -> ConcurrentHashMap.newKeySet());
        boolean etaitBloque = !bloques.add(niveau);
        if (etaitBloque) bloques.remove(niveau);
        sauvegarder();
        return etaitBloque;
    }

    public static Set<Integer> niveauxBloques(String id) {
        return Set.copyOf(NIVEAUX_BLOQUES.getOrDefault(id, Set.of()));
    }

    /** Tire un niveau au hasard parmi ceux non bloqués (0 si aucun n'est disponible). */
    public static int niveauAleatoireAutorise(CEnchant ce) {
        List<Integer> autorises = new ArrayList<>();
        for (int n = 1; n <= ce.niveauMax(); n++) {
            if (niveauActif(ce.id(), n)) autorises.add(n);
        }
        if (autorises.isEmpty()) return 0;
        return autorises.get(ThreadLocalRandom.current().nextInt(autorises.size()));
    }
}
