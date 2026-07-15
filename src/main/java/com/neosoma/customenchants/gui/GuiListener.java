package com.neosoma.customenchants.gui;

import com.neosoma.customenchants.CustomEnchantsPlugin;
import com.neosoma.customenchants.enchant.CEnchant;
import com.neosoma.customenchants.enchant.EnchantState;
import com.neosoma.customenchants.enchant.Enchants;
import com.neosoma.customenchants.enchant.Categorie;
import com.neosoma.customenchants.enchant.Rarity;
import com.neosoma.customenchants.util.Util;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/** Gestion des clics dans le panel admin et le sous-menu give. */
public class GuiListener implements Listener {

    private final CustomEnchantsPlugin plugin;

    public GuiListener(CustomEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PanelHolder
                || event.getInventory().getHolder() instanceof GiveHolder
                || event.getInventory().getHolder() instanceof NiveauHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClic(InventoryClickEvent event) {
        var holder = event.getInventory().getHolder();
        if (!(holder instanceof PanelHolder) && !(holder instanceof GiveHolder)
                && !(holder instanceof NiveauHolder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player joueur)) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        if (holder instanceof PanelHolder panel) {
            clicPanel(joueur, panel, slot, event.isRightClick(), event.isShiftClick());
        } else if (holder instanceof GiveHolder give) {
            clicGive(joueur, give, slot);
        } else if (holder instanceof NiveauHolder niveaux) {
            clicNiveaux(joueur, niveaux, slot);
        }
    }

    // =====================================================
    //  Panel principal
    // =====================================================
    private void clicPanel(Player joueur, PanelHolder panel, int slot, boolean clicDroit, boolean shiftClic) {

        // Zone des enchantements
        if (slot < AdminPanel.PAR_PAGE) {
            if (!panel.admin) return; // menu public : lecture seule, aucune action au clic
            if (slot >= panel.idsAffiches.size()) return;
            String id = panel.idsAffiches.get(slot);
            CEnchant ce = Enchants.parId(id);
            if (ce == null) return;

            if (clicDroit) {
                AdminPanel.ouvrirGive(joueur, new GiveHolder(id, panel));
            } else if (shiftClic) {
                AdminPanel.ouvrirNiveaux(joueur, new NiveauHolder(id, panel));
            } else {
                boolean actif = EnchantState.basculer(id);
                plugin.msg(joueur, actif
                        ? "<green>" + ce.nom() + " activé.</green>"
                        : "<red>" + ce.nom() + " désactivé.</red>");
                AdminPanel.ouvrir(joueur, panel);
            }
            return;
        }

        // Boutons globaux : réservés au panel admin
        if (!panel.admin && (slot == 50 || slot == 51 || slot == 52)) return;

        // Barre de navigation
        switch (slot) {
            case 45 -> {
                panel.page--;
                AdminPanel.ouvrir(joueur, panel);
            }
            case 46 -> {
                panel.filtreCategorie = suivantCategorie(panel.filtreCategorie);
                panel.page = 0;
                AdminPanel.ouvrir(joueur, panel);
            }
            case 47 -> {
                panel.filtreRarete = suivantRarete(panel.filtreRarete);
                panel.page = 0;
                AdminPanel.ouvrir(joueur, panel);
            }
            case 50 -> {
                EnchantState.toutDefinir(true);
                plugin.msg(joueur, "<green>Tous les enchantements sont activés.</green>");
                AdminPanel.ouvrir(joueur, panel);
            }
            case 51 -> {
                EnchantState.toutDefinir(false);
                plugin.msg(joueur, "<red>Tous les enchantements sont désactivés.</red>");
                AdminPanel.ouvrir(joueur, panel);
            }
            case 52 -> {
                plugin.reloadConfig();
                plugin.msg(joueur, "<gold>Configuration rechargée.</gold>");
                AdminPanel.ouvrir(joueur, panel);
            }
            case 53 -> {
                panel.page++;
                AdminPanel.ouvrir(joueur, panel);
            }
            default -> { }
        }
    }

    private Categorie suivantCategorie(Categorie actuel) {
        Categorie[] valeurs = Categorie.values();
        if (actuel == null) return valeurs[0];
        int index = actuel.ordinal() + 1;
        return index >= valeurs.length ? null : valeurs[index];
    }

    private Rarity suivantRarete(Rarity actuel) {
        Rarity[] valeurs = Rarity.values();
        if (actuel == null) return valeurs[0];
        int index = actuel.ordinal() + 1;
        return index >= valeurs.length ? null : valeurs[index];
    }

    // =====================================================
    //  Sous-menu "se donner"
    // =====================================================
    private void clicGive(Player joueur, GiveHolder give, int slot) {
        CEnchant ce = Enchants.parId(give.enchantId);
        if (ce == null) return;

        switch (slot) {
            case 10 -> {
                give.niveau = Math.max(1, give.niveau - 1);
                AdminPanel.ouvrirGive(joueur, give);
            }
            case 16 -> {
                give.niveau = Math.min(ce.niveauMax(), give.niveau + 1);
                AdminPanel.ouvrirGive(joueur, give);
            }
            case 20 -> {
                donner(joueur, Util.livre(ce, give.niveau));
                plugin.msg(joueur, "<light_purple>Livre " + ce.nom() + " "
                        + Util.romain(give.niveau) + " reçu.</light_purple>");
            }
            case 22 -> {
                donner(joueur, Util.itemEnchante(ce, give.niveau));
                plugin.msg(joueur, "<light_purple>Item enchanté " + ce.nom() + " "
                        + Util.romain(give.niveau) + " reçu.</light_purple>");
            }
            case 26 -> AdminPanel.ouvrir(joueur, give.retour);
            default -> { }
        }
    }

    private void donner(Player joueur, ItemStack item) {
        var restes = joueur.getInventory().addItem(item);
        restes.values().forEach(reste ->
                joueur.getWorld().dropItemNaturally(joueur.getLocation(), reste));
    }

    // =====================================================
    //  Sous-menu "niveaux" : blocage individuel
    // =====================================================
    private void clicNiveaux(Player joueur, NiveauHolder niveaux, int slot) {
        CEnchant ce = Enchants.parId(niveaux.enchantId);
        if (ce == null) return;

        if (slot < ce.niveauMax()) {
            int niveau = slot + 1;
            boolean actif = EnchantState.basculerNiveau(ce.id(), niveau);
            plugin.msg(joueur, actif
                    ? "<green>" + ce.nom() + " " + Util.romain(niveau) + " débloqué.</green>"
                    : "<red>" + ce.nom() + " " + Util.romain(niveau) + " bloqué.</red>");
            AdminPanel.ouvrirNiveaux(joueur, niveaux);
            return;
        }

        if (slot == niveaux.getInventory().getSize() - 1) {
            AdminPanel.ouvrir(joueur, niveaux.retour);
        }
    }
}
