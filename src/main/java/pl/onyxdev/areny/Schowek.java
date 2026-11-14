package pl.onyxdev.areny;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class Schowek {
    private final SunAreny plugin;
    private final File dir;

    private final Set<UUID> done = new HashSet<>();

    public Schowek(SunAreny plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "schowek");
        dir.mkdirs();
    }

    private File file(UUID id) {
        return new File(dir, id + ".yml");
    }

    public boolean has(UUID id) {
        return !done.contains(id) && file(id).isFile();
    }

    public boolean saveAndClear(Player p) {
        if (has(p.getUniqueId()) && !restore(p, false)) return false;
        p.closeInventory();
        done.remove(p.getUniqueId());
        YamlConfiguration y = new YamlConfiguration();
        y.set("nick", p.getName());
        y.set("ekwipunek", Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(p.getInventory().getContents())));
        y.set("poziom", p.getLevel());
        y.set("postep", (double) p.getExp());
        y.set("zycie", p.getHealth());
        y.set("glod", p.getFoodLevel());
        y.set("nasycenie", (double) p.getSaturation());
        y.set("tryb", p.getGameMode().name());
        y.set("latanie", p.getAllowFlight());
        Location l = p.getLocation();
        y.set("pozycja.swiat", l.getWorld().getName());
        y.set("pozycja.x", l.getX());
        y.set("pozycja.y", l.getY());
        y.set("pozycja.z", l.getZ());
        y.set("pozycja.yaw", (double) l.getYaw());
        y.set("pozycja.pitch", (double) l.getPitch());
        int i = 0;
        for (PotionEffect e : p.getActivePotionEffects()) {
            String b = "efekty." + i++;
            y.set(b + ".typ", e.getType().getKey().toString());
            y.set(b + ".czas", e.getDuration());
            y.set(b + ".moc", e.getAmplifier());
            y.set(b + ".ikona", e.hasIcon());
        }
        File target = file(p.getUniqueId());
        try {
            File tmp = new File(dir, target.getName() + ".tmp");
            Files.writeString(tmp.toPath(), y.saveToString(), StandardCharsets.UTF_8);
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Nie zapisano schowka " + p.getName(), ex);
            return false;
        }
        clear(p);
        return true;
    }

    public static void clear(Player p) {
        p.getInventory().clear();
        p.setItemOnCursor(null);
        p.setLevel(0);
        p.setExp(0);
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setGameMode(GameMode.SURVIVAL);
        p.setFlying(false);
        p.setAllowFlight(false);
    }

    public boolean restore(Player p, boolean teleport) {
        UUID id = p.getUniqueId();
        File f = file(id);
        if (!has(id)) return false;

        YamlConfiguration y = new YamlConfiguration();
        ItemStack[] items;
        try {
            y.load(f);
            if (!y.isString("nick") || !y.isString("ekwipunek") || !y.isString("tryb")) {
                throw new InvalidConfigurationException("brak wymaganych pól");
            }
            String inv = y.getString("ekwipunek", "");
            items = inv.isEmpty() ? new ItemStack[0] : ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(inv));
        } catch (IOException | InvalidConfigurationException | RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "Uszkodzony schowek " + f.getName() + " (" + p.getName()
                    + ") — gracz NIE został wyczyszczony, plik zostaje do ręcznego odzyskania", ex);
            return false;
        }
        GameMode gm;
        try {
            gm = GameMode.valueOf(y.getString("tryb", "SURVIVAL"));
        } catch (IllegalArgumentException ex) {
            gm = GameMode.SURVIVAL;
        }
        List<PotionEffect> effects = new ArrayList<>();
        ConfigurationSection ef = y.getConfigurationSection("efekty");
        if (ef != null) {
            for (String k : ef.getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString(ef.getString(k + ".typ", ""));
                PotionEffectType t = key == null ? null : Registry.EFFECT.get(key);
                if (t != null) effects.add(new PotionEffect(t, ef.getInt(k + ".czas"), ef.getInt(k + ".moc"), false, true, ef.getBoolean(k + ".ikona", true)));
            }
        }
        Location back = location(y);

        clear(p);
        p.getInventory().setContents(items);
        p.setLevel(y.getInt("poziom"));
        p.setExp((float) Math.clamp(y.getDouble("postep"), 0, 0.9999));
        var max = p.getAttribute(Attribute.MAX_HEALTH);
        double maxHp = max == null ? 20 : max.getValue();
        p.setHealth(Math.clamp(y.getDouble("zycie", maxHp), 1, maxHp));
        p.setFoodLevel(y.getInt("glod", 20));
        p.setSaturation((float) y.getDouble("nasycenie", 5));
        p.setGameMode(gm);
        p.setAllowFlight(y.getBoolean("latanie") || gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR);
        effects.forEach(p::addPotionEffect);

        p.saveData();

        if (!f.delete()) {
            File moved = new File(dir, id + ".przywrocony-" + System.currentTimeMillis());
            if (!f.renameTo(moved)) {
                plugin.getLogger().warning("Nie usunięto schowka " + f.getName() + " — blokuję ponowne oddanie do restartu");
                done.add(id);
            }
        }
        if (teleport) teleportBack(p, back);
        p.setFallDistance(0);
        return true;
    }

    private void teleportBack(Player p, Location to) {
        if (p.teleport(to)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline() || p.getWorld() != plugin.arenaWorld()) return;
            if (!p.teleport(to)) p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
        });
    }

    private Location location(YamlConfiguration y) {
        World w = Bukkit.getWorld(y.getString("pozycja.swiat", ""));
        if (w == null || w.getName().equals(plugin.arenaWorldName())) {
            return Bukkit.getWorlds().getFirst().getSpawnLocation();
        }
        return new Location(w, y.getDouble("pozycja.x"), y.getDouble("pozycja.y"), y.getDouble("pozycja.z"),
                (float) y.getDouble("pozycja.yaw"), (float) y.getDouble("pozycja.pitch"));
    }

    public boolean restoreIfPending(Player p) {
        return has(p.getUniqueId()) && restore(p, true);
    }
}
