package pl.onyxdev.areny;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.List;

public final class Rewards {
    private final SunAreny plugin;

    public Rewards(SunAreny plugin) {
        this.plugin = plugin;
    }

    public void give(Player p, String tier) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("nagrody." + tier);
        if (s == null) return;
        int limit = plugin.getConfig().getInt("nagrody.gier-dziennie", 6);
        if (!plugin.stats().takeRewardSlot(p.getUniqueId(), limit)) {
            Msg.chat(p, Msg.GAME, plugin.lang().withPrefix("gra.bez-nagrody", "limit", String.valueOf(limit)));
            plugin.stats().saveAsync();
            return;
        }
        int xp = Math.max(0, s.getInt("xp"));
        if (xp > 0) p.giveExp(xp);
        List<String> cmds = s.getStringList("komendy");
        for (String c : cmds) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c.replace("{gracz}", p.getName()));
        }
        String extra = cmds.isEmpty() ? "" : " <gray>+ <gold>bonus</gold>";
        if (xp > 0 || !cmds.isEmpty()) {
            Msg.chat(p, Msg.GAME, plugin.lang().withPrefix("gra.nagroda", "xp", String.valueOf(xp), "dodatki", extra));
        }
        plugin.stats().saveAsync();
    }
}
