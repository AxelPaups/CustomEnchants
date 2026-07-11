package com.neosoma.customenchants.command;

import com.neosoma.customenchants.CustomEnchantsPlugin;
import com.neosoma.customenchants.enchant.CEnchant;
import com.neosoma.customenchants.enchant.EnchantIndex;
import com.neosoma.customenchants.enchant.EnchantState;
import com.neosoma.customenchants.enchant.Enchants;
import com.neosoma.customenchants.enchant.Rarity;
import com.neosoma.customenchants.gui.AdminPanel;
import com.neosoma.customenchants.gui.PanelHolder;
import com.neosoma.customenchants.util.Util;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Commande /ce :
 *   /ce menu                                 — menu public en lecture seule (GUI)
 *   /ce panel                                — panel admin (GUI)
 *   /ce toggle <enchant>                     — activer/désactiver
 *   /ce give <joueur> <enchant> <niv> [livre|item]
 *   /ce apply <enchant> <niv>                — enchante l'item en main
 *   /ce info <enchant>                       — détails
 *   /ce list [rarete]                        — liste
 *   /ce reload                               — recharge la config
 */
public class CeCommand implements BasicCommand {

    private static final String PERM_ADMIN = "customenchants.admin";

    private final CustomEnchantsPlugin plugin;

    public CeCommand(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String permission() {
        return "customenchants.use";
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();
        Player joueur = source.getExecutor() instanceof Player p ? p
                : (sender instanceof Player p2 ? p2 : null);

        if (args.length == 0) {
            aide(sender);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {

            case "menu" -> {
                if (joueur == null) {
                    plugin.msg(sender, "<red>Cette commande s'utilise en jeu.</red>");
                    return;
                }
                AdminPanel.ouvrir(joueur, new PanelHolder(false));
            }

            case "panel" -> {
                if (!admin(sender)) return;
                if (joueur == null) {
                    plugin.msg(sender, "<red>Cette commande s'utilise en jeu.</red>");
                    return;
                }
                AdminPanel.ouvrir(joueur, new PanelHolder());
            }

            case "toggle" -> {
                if (!admin(sender)) return;
                CEnchant ce = trouver(sender, args, 1);
                if (ce == null) return;
                boolean actif = EnchantState.basculer(ce.id());
                plugin.msg(sender, actif
                        ? "<green>" + ce.nom() + " activé.</green>"
                        : "<red>" + ce.nom() + " désactivé.</red>");
            }

            case "give" -> {
                if (!admin(sender)) return;
                if (args.length < 4) {
                    plugin.msg(sender, "<red>Usage : /ce give (joueur) (enchant) (niveau) [livre|item]</red>");
                    return;
                }
                Player cible = Bukkit.getPlayerExact(args[1]);
                if (cible == null) {
                    plugin.msg(sender, "<red>Joueur introuvable : " + args[1] + "</red>");
                    return;
                }
                CEnchant ce = trouver(sender, args, 2);
                if (ce == null) return;
                int niveau = lireNiveau(sender, args[3], ce);
                if (niveau <= 0) return;

                boolean item = args.length >= 5 && args[4].equalsIgnoreCase("item");
                ItemStack don = item ? Util.itemEnchante(ce, niveau) : Util.livre(ce, niveau);
                var restes = cible.getInventory().addItem(don);
                restes.values().forEach(reste ->
                        cible.getWorld().dropItemNaturally(cible.getLocation(), reste));

                plugin.msg(sender, "<green>" + (item ? "Item" : "Livre") + " " + ce.nom() + " "
                        + Util.romain(niveau) + " donné à " + cible.getName() + ".</green>");
                plugin.msg(cible, "<light_purple>Vous avez reçu " + ce.nom() + " "
                        + Util.romain(niveau) + ".</light_purple>");
            }

            case "apply" -> {
                if (!admin(sender)) return;
                if (joueur == null) {
                    plugin.msg(sender, "<red>Cette commande s'utilise en jeu.</red>");
                    return;
                }
                if (args.length < 3) {
                    plugin.msg(sender, "<red>Usage : /ce apply (enchant) (niveau)</red>");
                    return;
                }
                CEnchant ce = trouver(sender, args, 1);
                if (ce == null) return;
                int niveau = lireNiveau(sender, args[2], ce);
                if (niveau <= 0) return;

                ItemStack enMain = joueur.getInventory().getItemInMainHand();
                if (enMain.getType().isAir()) {
                    plugin.msg(sender, "<red>Prenez un item en main.</red>");
                    return;
                }
                if (!EnchantIndex.get(ce.id()).canEnchantItem(enMain)) {
                    plugin.msg(sender, "<red>" + ce.nom() + " ne s'applique pas sur cet item."
                            + " Items valides : " + ce.cible().label() + ".</red>");
                    return;
                }
                enMain.addUnsafeEnchantment(EnchantIndex.get(ce.id()), niveau);
                plugin.msg(sender, "<green>" + ce.nom() + " " + Util.romain(niveau)
                        + " appliqué sur votre item.</green>");
            }

            case "info" -> {
                CEnchant ce = trouver(sender, args, 1);
                if (ce == null) return;
                plugin.msg(sender, "<bold>" + ce.nom() + "</bold> <dark_gray>(" + ce.id() + ")</dark_gray>");
                sender.sendMessage(net.kyori.adventure.text.Component.text("  " + ce.description()));
                plugin.msg(sender, "<gray>Rareté :</gray> " + ce.rarete().label()
                        + " <gray>| Niveau max :</gray> " + Util.romain(ce.niveauMax())
                        + " <gray>| Catégorie :</gray> " + ce.categorie().label()
                        + " <gray>| État :</gray> "
                        + (EnchantState.actif(ce.id()) ? "<green>actif</green>" : "<red>désactivé</red>"));
            }

            case "list" -> {
                Rarity filtre = null;
                if (args.length >= 2) {
                    for (Rarity r : Rarity.values()) {
                        if (r.name().equalsIgnoreCase(args[1])
                                || r.label().equalsIgnoreCase(args[1])) {
                            filtre = r;
                        }
                    }
                }
                plugin.msg(sender, "<bold>Enchantements custom :</bold>");
                for (CEnchant ce : Enchants.ALL) {
                    if (filtre != null && ce.rarete() != filtre) continue;
                    String etat = EnchantState.actif(ce.id()) ? "<green>✔</green>" : "<red>✘</red>";
                    plugin.msg(sender, etat + " <white>" + ce.nom() + "</white> <dark_gray>("
                            + ce.id() + ", " + ce.rarete().label() + ", max "
                            + Util.romain(ce.niveauMax()) + ")</dark_gray>");
                }
            }

            case "reload" -> {
                if (!admin(sender)) return;
                plugin.reloadConfig();
                plugin.msg(sender, "<gold>Configuration rechargée.</gold>");
            }

            default -> aide(sender);
        }
    }

    private void aide(CommandSender sender) {
        plugin.msg(sender, "<bold>Commandes CustomEnchants :</bold>");
        plugin.msg(sender, "<yellow>/ce menu</yellow> <gray>— menu public (lecture seule)</gray>");
        plugin.msg(sender, "<yellow>/ce panel</yellow> <gray>— panel admin</gray>");
        plugin.msg(sender, "<yellow>/ce toggle (enchant)</yellow> <gray>— activer/désactiver</gray>");
        plugin.msg(sender, "<yellow>/ce give (joueur) (enchant) (niveau) [livre|item]</yellow>");
        plugin.msg(sender, "<yellow>/ce apply (enchant) (niveau)</yellow> <gray>— enchante l'item en main</gray>");
        plugin.msg(sender, "<yellow>/ce info (enchant)</yellow> <gray>— détails</gray>");
        plugin.msg(sender, "<yellow>/ce list [rarete]</yellow> <gray>— liste</gray>");
        plugin.msg(sender, "<yellow>/ce reload</yellow> <gray>— recharge la config</gray>");
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission(PERM_ADMIN)) return true;
        plugin.msg(sender, "<red>Permission requise : " + PERM_ADMIN + "</red>");
        return false;
    }

    private CEnchant trouver(CommandSender sender, String[] args, int index) {
        if (args.length <= index) {
            plugin.msg(sender, "<red>Précisez un enchantement (ex : vampirisme).</red>");
            return null;
        }
        CEnchant ce = Enchants.parId(args[index].toLowerCase(Locale.ROOT));
        if (ce == null) {
            plugin.msg(sender, "<red>Enchantement inconnu : " + args[index]
                    + ". Voir /ce list</red>");
        }
        return ce;
    }

    private int lireNiveau(CommandSender sender, String brut, CEnchant ce) {
        try {
            int niveau = Integer.parseInt(brut);
            if (niveau < 1 || niveau > ce.niveauMax()) {
                plugin.msg(sender, "<red>Niveau invalide (1 à " + ce.niveauMax() + ").</red>");
                return -1;
            }
            return niveau;
        } catch (NumberFormatException e) {
            plugin.msg(sender, "<red>Niveau invalide : " + brut + "</red>");
            return -1;
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length <= 1) {
            suggestions.addAll(List.of("menu", "panel", "toggle", "give", "apply", "info", "list", "reload"));
        } else {
            String sub = args[0].toLowerCase(Locale.ROOT);
            switch (sub) {
                case "toggle", "info", "apply" -> {
                    if (args.length == 2) suggestions.addAll(Enchants.PAR_ID.keySet());
                }
                case "give" -> {
                    if (args.length == 2) {
                        Bukkit.getOnlinePlayers().forEach(p -> suggestions.add(p.getName()));
                    } else if (args.length == 3) {
                        suggestions.addAll(Enchants.PAR_ID.keySet());
                    } else if (args.length == 4) {
                        suggestions.addAll(List.of("1", "2", "3"));
                    } else if (args.length == 5) {
                        suggestions.addAll(List.of("livre", "item"));
                    }
                }
                case "list" -> {
                    if (args.length == 2) {
                        for (Rarity r : Rarity.values()) {
                            suggestions.add(r.name().toLowerCase(Locale.ROOT));
                        }
                    }
                }
                default -> { }
            }
        }

        String saisie = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return suggestions.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(saisie))
                .toList();
    }
}
