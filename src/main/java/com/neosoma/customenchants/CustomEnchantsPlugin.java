package com.neosoma.customenchants;

import com.neosoma.customenchants.command.CeCommand;
import com.neosoma.customenchants.enchant.EnchantIndex;
import com.neosoma.customenchants.enchant.EnchantState;
import com.neosoma.customenchants.enchant.Enchants;
import com.neosoma.customenchants.gui.GuiListener;
import com.neosoma.customenchants.listener.AnvilListener;
import com.neosoma.customenchants.listener.ArmorTask;
import com.neosoma.customenchants.listener.BowListener;
import com.neosoma.customenchants.listener.CombatListener;
import com.neosoma.customenchants.listener.LootListener;
import com.neosoma.customenchants.listener.MiscListener;
import com.neosoma.customenchants.listener.TableFilterListener;
import com.neosoma.customenchants.listener.ToolListener;
import com.neosoma.customenchants.listener.TridentListener;
import com.neosoma.customenchants.listener.VillagerListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class CustomEnchantsPlugin extends JavaPlugin {

    private static CustomEnchantsPlugin instance;

    public static CustomEnchantsPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Résolution des enchantements enregistrés au bootstrap
        EnchantIndex.init();
        EnchantState.charger(this);

        // Listeners d'effets
        var pm = getServer().getPluginManager();
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new BowListener(this), this);
        pm.registerEvents(new ToolListener(this), this);
        pm.registerEvents(new TridentListener(this), this);
        pm.registerEvents(new MiscListener(this), this);
        pm.registerEvents(new TableFilterListener(), this);
        pm.registerEvents(new AnvilListener(this), this);
        pm.registerEvents(new LootListener(this), this);
        pm.registerEvents(new VillagerListener(this), this);
        pm.registerEvents(new GuiListener(this), this);

        // Effets d'armure périodiques (toutes les 3 secondes, léger)
        new ArmorTask(this).runTaskTimer(this, 60L, 60L);

        // Commande /ce
        registerCommand("ce", new CeCommand(this));

        getLogger().info(Enchants.ALL.size() + " enchantements custom chargés ("
                + EnchantState.nbActifs() + " actifs).");
    }

    @Override
    public void onDisable() {
        EnchantState.sauvegarder();
    }

    /** Envoie un message préfixé (MiniMessage) à un joueur ou à la console. */
    public void msg(CommandSender cible, String miniMessage) {
        String prefixe = getConfig().getString("messages.prefixe",
                "<dark_purple><bold>CE</bold></dark_purple> <dark_gray>»</dark_gray> ");
        Component composant = MiniMessage.miniMessage().deserialize(prefixe + miniMessage);
        cible.sendMessage(composant);
    }
}
