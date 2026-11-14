package pl.onyxdev.areny;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import pl.onyxdev.areny.gra.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Commands implements TabExecutor {
    private static final long COMBAT_MS = 15_000;

    private final SunAreny plugin;

    private final Map<UUID, Long> combat = new HashMap<>();

    public Commands(SunAreny plugin) {
        this.plugin = plugin;
    }

    public void tagCombat(UUID id) {
        combat.put(id, System.currentTimeMillis());
    }

    public void forget(UUID id) {
        combat.remove(id);
    }

    private Lang lang() {
        return plugin.lang();
    }

    private void info(CommandSender s, String path, String... kv) {
        if (s instanceof Player p) Msg.chat(p, Msg.INFO, lang().withPrefix(path, kv));
        else s.sendMessage(lang().withPrefix(path, kv));
    }

    @Override
    public boolean onCommand(CommandSender s, Command command, String label, String[] a) {
        String sub = a.length == 0 ? "" : a[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "" -> {
                if (!(s instanceof Player p) || !perm(s, "sunareny.menu")) return true;
                p.openInventory(new Menu(plugin, p).getInventory());
            }
            case "dolacz", "join" -> {
                if (!(s instanceof Player p) || !perm(s, "sunareny.dolacz")) return true;
                Game g = plugin.manager().current();
                if (g == null || g.state() != Game.State.SIGNUP) {
                    if (g != null) info(p, "info.trwa-gra", "tryb", lang().modeName(g.mode));
                    else info(p, "info.brak-zapisow");
                    return true;
                }
                join(p, g);
            }
            case "opusc", "wyjdz", "leave" -> {
                if (!(s instanceof Player p) || !perm(s, "sunareny.opusc")) return true;
                leave(p);
            }
            case "statystyki", "staty" -> {
                if (!perm(s, "sunareny.statystyki")) return true;
                stats(s, a.length > 1 ? a[1] : null);
            }
            case "start" -> {
                if (!perm(s, "sunareny.admin.start")) return true;
                Mode m = a.length > 1 ? Mode.of(a[1]) : null;
                if (m == null) {
                    info(s, "admin.zly-tryb", "tryby", modesList());
                    return true;
                }
                Game cur = plugin.manager().current();
                if (cur != null) {
                    info(s, "info.trwa-gra", "tryb", lang().modeName(cur.mode));
                    return true;
                }
                if (!plugin.manager().open(m)) info(s, "info.tryb-wylaczony");
                else info(s, "admin.start", "tryb", lang().modeName(m));
            }
            case "stop" -> {
                if (!perm(s, "sunareny.admin.stop")) return true;
                Game cur = plugin.manager().current();
                if (cur == null) info(s, "admin.nic-nie-trwa");
                else {
                    cur.close();
                    info(s, "admin.stop");
                }
            }
            case "wymus" -> {
                if (!perm(s, "sunareny.admin.wymus")) return true;
                Game cur = plugin.manager().current();
                if (cur == null) info(s, "admin.nic-nie-trwa");
                else {
                    cur.forceStart();
                    info(s, "admin.wymus");
                }
            }
            case "odbuduj" -> {
                if (!perm(s, "sunareny.admin.odbuduj")) return true;
                if (plugin.manager().current() != null) {
                    info(s, "info.trwa-gra", "tryb", lang().modeName(plugin.manager().current().mode));
                    return true;
                }
                info(s, "admin.odbudowano", "bloki", String.valueOf(plugin.manager().buildAll()));
            }
            case "tp" -> {
                if (!(s instanceof Player p) || !perm(s, "sunareny.admin.tp")) return true;
                Mode m = a.length > 1 ? Mode.of(a[1]) : null;
                if (m == null) {
                    info(s, "admin.zly-tryb", "tryby", modesList());
                    return true;
                }
                if (plugin.manager().gameOf(p.getUniqueId()) != null) {
                    info(p, "info.juz-grasz");
                    return true;
                }
                p.teleport(plugin.manager().newGame(m).center().add(0, 6, 0));
            }
            case "reload" -> {
                if (!perm(s, "sunareny.admin.reload")) return true;
                plugin.reloadConfig();
                plugin.lang().load();
                info(s, "admin.przeladowano");
            }
            default -> {
                if (s.hasPermission("sunareny.admin")) info(s, "admin.pomoc");
                else if (s instanceof Player p) p.openInventory(new Menu(plugin, p).getInventory());
            }
        }
        return true;
    }

    private boolean perm(CommandSender s, String node) {
        if (s.hasPermission(node)) return true;
        s.sendMessage(Bukkit.permissionMessage());
        return false;
    }

    private String modesList() {
        return java.util.Arrays.stream(Mode.values()).map(m -> m.id).collect(Collectors.joining(", "));
    }

    public void joinOrOpen(Player p, Mode m) {
        Game cur = plugin.manager().current();
        if (cur != null) {
            if (cur.mode == m && cur.state() == Game.State.SIGNUP) join(p, cur);
            else info(p, "info.trwa-gra", "tryb", lang().modeName(cur.mode));
            return;
        }
        if (!plugin.manager().enabled(m)) {
            info(p, "info.tryb-wylaczony");
            return;
        }
        boolean admin = p.hasPermission("sunareny.admin.start");
        if (!admin) {
            if (!plugin.getConfig().getBoolean("gracze-otwieraja.wlaczone", true) || !p.hasPermission("sunareny.otworz")) {
                info(p, "info.brak-zapisow");
                return;
            }
            int cd = plugin.manager().playerCooldown();
            if (cd > 0) {
                info(p, "info.przerwa", "sekundy", String.valueOf(cd));
                return;
            }
        }
        if (!canJoinNow(p)) return;
        if (plugin.manager().open(m)) join(p, plugin.manager().current());
    }

    private boolean canJoinNow(Player p) {
        if (plugin.manager().gameOf(p.getUniqueId()) != null) {
            info(p, "info.juz-grasz");
            return false;
        }
        Long c = combat.get(p.getUniqueId());
        if (p.isInsideVehicle() || p.isGliding() || p.isDead() || p.getFallDistance() > 3
                || (c != null && System.currentTimeMillis() - c < COMBAT_MS)) {
            info(p, "info.nie-tutaj");
            return false;
        }
        return true;
    }

    private void join(Player p, Game g) {
        if (!canJoinNow(p)) return;
        switch (g.join(p)) {
            case FULL -> info(p, "info.pelne");
            case STARTED -> info(p, "info.trwa-gra", "tryb", lang().modeName(g.mode));
            case FAILED -> info(p, "info.nie-tutaj");
            case BROKEN_SNAPSHOT -> info(p, "info.schowek-blad");
            case OK -> {
            }
        }
    }

    public void leave(Player p) {
        if (plugin.manager().gameOf(p.getUniqueId()) == null) {
            info(p, "info.nie-grasz");
            return;
        }
        plugin.manager().leave(p, false, true);
    }

    private void stats(CommandSender s, String nick) {
        UUID id;
        String name;
        if (nick == null) {
            if (!(s instanceof Player p)) return;
            id = p.getUniqueId();
            name = p.getName();
        } else {
            Player online = Bukkit.getPlayerExact(nick);
            id = online != null ? online.getUniqueId() : plugin.stats().byNick(nick);
            if (id == null) {
                info(s, "info.brak-gracza");
                return;
            }
            name = online != null ? online.getName() : plugin.stats().nick(id);
        }
        Stats st = plugin.stats();
        send(s, lang().get("info.statystyki-naglowek", "gracz", name));
        send(s, lang().get("info.statystyki-linia", "wygrane", String.valueOf(st.get(id, "wygrane")),
                "rozegrane", String.valueOf(st.get(id, "rozegrane")), "seria", String.valueOf(st.get(id, "seria")),
                "rekord", String.valueOf(st.get(id, "rekord-serii"))));
        for (Mode m : Mode.values()) {
            send(s, lang().get("info.statystyki-tryb", lang().modeKv(m, "wygrane", String.valueOf(st.get(id, m, "wygrane")),
                    "rozegrane", String.valueOf(st.get(id, m, "rozegrane")))));
        }
    }

    private void send(CommandSender s, net.kyori.adventure.text.Component c) {
        if (s instanceof Player p) Msg.chat(p, Msg.INFO, c);
        else s.sendMessage(c);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command command, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            String[][] subs = {{"dolacz", "sunareny.dolacz"}, {"opusc", "sunareny.opusc"}, {"statystyki", "sunareny.statystyki"},
                    {"start", "sunareny.admin.start"}, {"stop", "sunareny.admin.stop"}, {"wymus", "sunareny.admin.wymus"},
                    {"odbuduj", "sunareny.admin.odbuduj"}, {"tp", "sunareny.admin.tp"}, {"reload", "sunareny.admin.reload"}};
            for (String[] x : subs) if (s.hasPermission(x[1]) && x[0].startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(x[0]);
        } else if (a.length == 2 && (a[0].equalsIgnoreCase("start") || a[0].equalsIgnoreCase("tp"))) {
            for (Mode m : Mode.values()) if (m.id.startsWith(a[1].toLowerCase(Locale.ROOT))) out.add(m.id);
        } else if (a.length == 2 && a[0].toLowerCase(Locale.ROOT).startsWith("staty")) {
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getName().toLowerCase(Locale.ROOT).startsWith(a[1].toLowerCase(Locale.ROOT))) out.add(p.getName());
        }
        return out;
    }
}
