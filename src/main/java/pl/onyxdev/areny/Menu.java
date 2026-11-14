package pl.onyxdev.areny;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import pl.onyxdev.areny.gra.Game;

import java.util.ArrayList;
import java.util.List;

public final class Menu implements InventoryHolder {
    private static final int[] MODE_SLOTS = {11, 13, 15};
    private static final int STATS_SLOT = 22;
    private static final int LEAVE_SLOT = 26;

    private final SunAreny plugin;
    private final Inventory inv;

    public Menu(SunAreny plugin, Player viewer) {
        this.plugin = plugin;
        Lang lang = plugin.lang();
        this.inv = Bukkit.createInventory(this, 27, lang.get("menu.tytul"));
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, Component.text(" "), List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        Game cur = plugin.manager().current();
        Mode[] modes = Mode.values();
        for (int i = 0; i < modes.length && i < MODE_SLOTS.length; i++) {
            Mode m = modes[i];
            List<Component> lore = new ArrayList<>();
            lore.add(Lang.parse("<!italic><gray>" + lang.modeDesc(m)));
            lore.add(Component.empty());
            Stats st = plugin.stats();
            lore.add(lang.get("menu.twoje", "wygrane", String.valueOf(st.get(viewer.getUniqueId(), m, "wygrane")),
                    "rozegrane", String.valueOf(st.get(viewer.getUniqueId(), m, "rozegrane"))));
            String status;
            String[] kv = {};
            if (!plugin.manager().enabled(m)) status = "info.tryb-wylaczony";
            else if (cur != null && cur.mode == m && cur.state() == Game.State.SIGNUP) {
                status = "menu.tryb-zapisy";
                kv = new String[]{"liczba", String.valueOf(cur.size()), "max", String.valueOf(plugin.getConfig().getInt("zapisy.max-graczy", 12))};
            } else if (cur != null && cur.mode == m) status = "menu.tryb-gra";
            else if (cur != null) status = "menu.tryb-zajete";
            else if (plugin.manager().playerCooldown() > 0 && !viewer.hasPermission("sunareny.admin.start")) {
                status = "menu.tryb-przerwa";
                kv = new String[]{"sekundy", String.valueOf(plugin.manager().playerCooldown())};
            } else status = "menu.tryb-otworz";
            lore.add(lang.get(status, kv));
            Component name = Lang.parse("<!italic><color:<kolor>><b>" + lang.modeName(m), "kolor", lang.modeColor(m));
            ItemStack it = item(m.icon, name, lore);
            if (cur != null && cur.mode == m) it.editMeta(meta -> meta.setEnchantmentGlintOverride(true));
            inv.setItem(MODE_SLOTS[i], it);
        }
        inv.setItem(STATS_SLOT, item(Material.BOOK, lang.get("menu.statystyki"), List.of(
                lang.get("info.statystyki-linia",
                        "wygrane", String.valueOf(plugin.stats().get(viewer.getUniqueId(), "wygrane")),
                        "rozegrane", String.valueOf(plugin.stats().get(viewer.getUniqueId(), "rozegrane")),
                        "seria", String.valueOf(plugin.stats().get(viewer.getUniqueId(), "seria")),
                        "rekord", String.valueOf(plugin.stats().get(viewer.getUniqueId(), "rekord-serii"))))));
        if (plugin.manager().gameOf(viewer.getUniqueId()) != null) {
            inv.setItem(LEAVE_SLOT, item(Material.BARRIER, lang.get("menu.wyjdz"), List.of()));
        }
    }

    private static ItemStack item(Material m, Component name, List<Component> lore) {
        ItemStack it = new ItemStack(m);
        it.editMeta(meta -> {
            meta.displayName(name.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(c -> c.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false)).toList());
            meta.addItemFlags(ItemFlag.values());
        });
        return it;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void click(Player p, int slot) {
        if (slot == LEAVE_SLOT && plugin.manager().gameOf(p.getUniqueId()) != null) {
            p.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (p.isOnline()) plugin.commands().leave(p);
            });
            return;
        }
        for (int i = 0; i < MODE_SLOTS.length; i++) {
            if (MODE_SLOTS[i] != slot) continue;
            Mode m = Mode.values()[i];
            p.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (p.isOnline()) plugin.commands().joinOrOpen(p, m);
            });
            return;
        }
    }
}
