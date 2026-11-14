package pl.onyxdev.areny;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class SunAreny extends JavaPlugin {
    private Lang lang;
    private Schowek schowek;
    private Stats stats;
    private Rewards rewards;
    private Manager manager;
    private Commands commands;
    private World arenaWorld;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        lang = new Lang(this);
        lang.load();
        Msg.register(this);

        arenaWorld = createWorld();
        if (arenaWorld == null) {
            getLogger().severe("Nie udało się utworzyć świata aren — wyłączam SunAreny.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        schowek = new Schowek(this);
        stats = new Stats(this);
        rewards = new Rewards(this);
        manager = new Manager(this);
        commands = new Commands(this);

        var cmd = getCommand("arena");
        if (cmd == null) throw new IllegalStateException("Brak komendy arena w plugin.yml");
        cmd.setExecutor(commands);
        cmd.setTabCompleter(commands);
        getServer().getPluginManager().registerEvents(new Listeners(this), this);
        getServer().getPluginManager().registerEvents(new CombatTag(commands), this);

        int blocks = manager.buildAll();
        getLogger().info("Areny gotowe w świecie " + arenaWorld.getName() + " (" + blocks + " bloków).");
        manager.startAuto();

        Bukkit.getOnlinePlayers().forEach(schowek::restoreIfPending);
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.shutdown();
        if (stats != null) stats.saveNow();
    }

    private World createWorld() {
        String name = arenaWorldName();
        World w = Bukkit.getWorld(name);
        if (w == null) {
            Biome biome = null;
            NamespacedKey key = NamespacedKey.fromString(getConfig().getString("biom", "plains"));
            if (key != null) biome = Registry.BIOME.get(key);
            if (biome == null) biome = Biome.PLAINS;
            w = new WorldCreator(name)
                    .generator(new VoidGenerator(biome))
                    .generateStructures(false)
                    .createWorld();
        }
        if (w == null) return null;
        w.setGameRule(GameRules.ADVANCE_TIME, false);
        w.setGameRule(GameRules.ADVANCE_WEATHER, false);
        w.setGameRule(GameRules.SPAWN_MOBS, false);
        w.setGameRule(GameRules.SPAWN_MONSTERS, false);
        w.setGameRule(GameRules.SPAWN_PATROLS, false);
        w.setGameRule(GameRules.SPAWN_PHANTOMS, false);
        w.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false);
        w.setGameRule(GameRules.SPAWN_WARDENS, false);
        w.setGameRule(GameRules.FALL_DAMAGE, false);
        w.setGameRule(GameRules.FIRE_DAMAGE, false);
        w.setGameRule(GameRules.DROWNING_DAMAGE, false);
        w.setGameRule(GameRules.FREEZE_DAMAGE, false);
        w.setGameRule(GameRules.KEEP_INVENTORY, true);
        w.setGameRule(GameRules.BLOCK_DROPS, false);
        w.setGameRule(GameRules.MOB_GRIEFING, false);
        w.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRules.SPECTATORS_GENERATE_CHUNKS, false);
        w.setTime(getConfig().getLong("pora-dnia", 6000));
        w.setStorm(false);
        w.setThundering(false);
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setSpawnLocation(0, 110, 0);
        return w;
    }

    public String arenaWorldName() {
        return getConfig().getString("swiat", "areny");
    }

    public World arenaWorld() {
        return arenaWorld;
    }

    public ConfigurationSection modeConfig(Mode m) {
        ConfigurationSection s = getConfig().getConfigurationSection("tryby." + m.id);
        return s != null ? s : new YamlConfiguration();
    }

    public Lang lang() {
        return lang;
    }

    public Schowek schowek() {
        return schowek;
    }

    public Stats stats() {
        return stats;
    }

    public Rewards rewards() {
        return rewards;
    }

    public Manager manager() {
        return manager;
    }

    public Commands commands() {
        return commands;
    }
}
