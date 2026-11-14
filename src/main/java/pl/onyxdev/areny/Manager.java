package pl.onyxdev.areny;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import pl.onyxdev.areny.gra.Game;
import pl.onyxdev.areny.gra.Krol;
import pl.onyxdev.areny.gra.Spleef;
import pl.onyxdev.areny.gra.TntRun;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class Manager {
    private final SunAreny plugin;
    private Game current;
    private long lastEnd = 0;
    private long lastAuto = System.currentTimeMillis();
    private BukkitTask autoTask;

    public Manager(SunAreny plugin) {
        this.plugin = plugin;
    }

    public Game newGame(Mode m) {
        World w = plugin.arenaWorld();
        return switch (m) {
            case SPLEEF -> new Spleef(plugin, w);
            case TNTRUN -> new TntRun(plugin, w);
            case KROL -> new Krol(plugin, w);
        };
    }

    public int buildAll() {
        int n = 0;
        for (Mode m : Mode.values()) n += newGame(m).build();
        return n;
    }

    public Game current() {
        return current;
    }

    public Game gameOf(UUID id) {
        return current != null && current.has(id) ? current : null;
    }

    public boolean enabled(Mode m) {
        return plugin.modeConfig(m).getBoolean("wlaczony", true);
    }

    public List<Mode> enabledModes() {
        List<Mode> l = new ArrayList<>();
        for (Mode m : Mode.values()) if (enabled(m)) l.add(m);
        return l;
    }

    public boolean open(Mode m) {
        if (current != null || !enabled(m)) return false;
        current = newGame(m);
        current.build();
        current.open();
        lastAuto = System.currentTimeMillis();
        return true;
    }

    public int playerCooldown() {
        long ms = plugin.getConfig().getInt("gracze-otwieraja.przerwa-sekundy", 120) * 1000L - (System.currentTimeMillis() - lastEnd);
        return (int) Math.max(0, (ms + 999) / 1000);
    }

    public void gameClosed(Game g) {
        if (current == g) {
            current = null;
            lastEnd = System.currentTimeMillis();
        }
    }

    public void startAuto() {
        if (autoTask != null) autoTask.cancel();
        autoTask = Bukkit.getScheduler().runTaskTimer(plugin, this::autoTick, 20L * 30, 20L * 30);
    }

    private void autoTick() {
        int every = plugin.getConfig().getInt("automat.co-minut", 20);
        if (every <= 0 || current != null) return;
        if (Bukkit.getOnlinePlayers().size() < plugin.getConfig().getInt("automat.min-online", 3)) return;
        long since = System.currentTimeMillis() - Math.max(lastAuto, lastEnd);
        if (since < every * 60_000L) return;
        List<Mode> modes = enabledModes();
        if (modes.isEmpty()) return;
        open(modes.get(ThreadLocalRandom.current().nextInt(modes.size())));
    }

    public void shutdown() {
        if (autoTask != null) autoTask.cancel();
        if (current != null) current.close();
    }

    public void leave(Player p, boolean quitting, boolean teleport) {
        Game g = gameOf(p.getUniqueId());
        if (g != null) g.leave(p, quitting, teleport);
    }
}
