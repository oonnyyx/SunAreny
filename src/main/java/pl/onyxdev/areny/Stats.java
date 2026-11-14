package pl.onyxdev.areny;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class Stats {
    private static final ZoneId PL = ZoneId.of("Europe/Warsaw");

    private final SunAreny plugin;
    private final File file;
    private final YamlConfiguration y;

    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SunAreny-zapis");
        t.setDaemon(true);
        return t;
    });

    public Stats(SunAreny plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "statystyki.yml");
        this.y = YamlConfiguration.loadConfiguration(file);
    }

    private String base(UUID id) {
        return "gracze." + id + ".";
    }

    public int get(UUID id, String key) {
        return y.getInt(base(id) + key);
    }

    public int get(UUID id, Mode m, String key) {
        return y.getInt(base(id) + "tryby." + m.id + "." + key);
    }

    public void record(UUID id, String nick, Mode m, boolean won) {
        String b = base(id);
        y.set(b + "nick", nick);
        y.set(b + "rozegrane", y.getInt(b + "rozegrane") + 1);
        y.set(b + "tryby." + m.id + ".rozegrane", y.getInt(b + "tryby." + m.id + ".rozegrane") + 1);
        if (won) {
            y.set(b + "wygrane", y.getInt(b + "wygrane") + 1);
            y.set(b + "tryby." + m.id + ".wygrane", y.getInt(b + "tryby." + m.id + ".wygrane") + 1);
            int s = y.getInt(b + "seria") + 1;
            y.set(b + "seria", s);
            if (s > y.getInt(b + "rekord-serii")) y.set(b + "rekord-serii", s);
        } else {
            y.set(b + "seria", 0);
        }
    }

    public boolean takeRewardSlot(UUID id, int perDay) {
        if (perDay <= 0) return true;
        String b = base(id) + "nagrody.";
        String today = LocalDate.now(PL).toString();
        if (!today.equals(y.getString(b + "dzien"))) {
            y.set(b + "dzien", today);
            y.set(b + "gier", 0);
        }
        int n = y.getInt(b + "gier");
        if (n >= perDay) return false;
        y.set(b + "gier", n + 1);
        return true;
    }

    public UUID byNick(String nick) {
        ConfigurationSection g = y.getConfigurationSection("gracze");
        if (g == null) return null;
        for (String k : g.getKeys(false)) {
            if (nick.equalsIgnoreCase(g.getString(k + ".nick"))) {
                try {
                    return UUID.fromString(k);
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    public String nick(UUID id) {
        return y.getString(base(id) + "nick", "?");
    }

    public void saveAsync() {
        String data = y.saveToString();
        io.execute(() -> write(data));
    }

    public void saveNow() {
        io.shutdown();
        try {
            io.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        write(y.saveToString());
    }

    private void write(String data) {
        try {
            File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
            Files.writeString(tmp.toPath(), data, StandardCharsets.UTF_8);
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.WARNING, "Nie zapisano statystyki.yml", ex);
        }
    }
}
