package pl.onyxdev.areny.gra;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import pl.onyxdev.areny.Lang;
import pl.onyxdev.areny.Mode;
import pl.onyxdev.areny.Msg;
import pl.onyxdev.areny.SunAreny;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class Game {
    public enum State { SIGNUP, COUNTDOWN, RUNNING, ENDING, CLOSED }

    protected final SunAreny plugin;
    public final Mode mode;
    protected final World world;
    protected final int ox, oy, oz;
    protected final ConfigurationSection cfg;

    protected State state = State.SIGNUP;

    protected final Set<UUID> players = new LinkedHashSet<>();
    protected final Set<UUID> alive = new LinkedHashSet<>();

    protected final List<UUID> out = new ArrayList<>();

    protected final Map<UUID, String> starters = new LinkedHashMap<>();
    protected final Map<UUID, Integer> slot = new HashMap<>();
    private final Map<UUID, BossBar> bars = new HashMap<>();

    protected final Set<UUID> quitters = new LinkedHashSet<>();

    private final Map<UUID, String> pendingRewards = new HashMap<>();

    private BukkitTask task;
    private int signupTicks;
    private int countdownTicks;
    protected int runTicks;

    protected Game(SunAreny plugin, Mode mode, World world) {
        this.plugin = plugin;
        this.mode = mode;
        this.world = world;
        this.cfg = plugin.modeConfig(mode);
        this.ox = cfg.getInt("x");
        this.oy = cfg.getInt("y", 100);
        this.oz = cfg.getInt("z");
    }

    public abstract int build();

    protected abstract Location spawn(int i, int n);

    protected abstract double fallY();

    protected abstract void giveKit(Player p);

    protected void onStart() {
    }

    protected void onRunTick() {
    }

    protected void onFall(Player p) {
        eliminate(p);
    }

    protected void onEnd() {
    }

    protected List<UUID> ranking() {
        List<UUID> r = new ArrayList<>(alive);
        List<UUID> rev = new ArrayList<>(out);
        Collections.reverse(rev);
        r.addAll(rev);
        return r;
    }

    protected String scoreSuffix(UUID id) {
        return "";
    }

    protected String barText(Player p) {
        return plugin.lang().raw("pasek.gra");
    }

    protected String[] barKv(Player p) {
        return new String[0];
    }

    public boolean allowBreak(Player p, Block b) {
        return false;
    }

    public void onBreak(BlockBreakEvent e) {
    }

    public boolean instaBreak(Player p, Block b) {
        return false;
    }

    public void onHit(Player attacker, Player victim) {
    }

    public void onProjectileHit(ProjectileHitEvent e) {
    }

    public Location center() {
        return new Location(world, ox + 0.5, oy, oz + 0.5);
    }

    protected Location spectatorPoint() {
        return new Location(world, ox + 0.5, oy + 14, oz - 22.5, 0, 30);
    }

    public boolean inBounds(Location l) {
        if (l.getWorld() != world) return false;
        double dx = l.getX() - (ox + 0.5), dz = l.getZ() - (oz + 0.5);
        return dx * dx + dz * dz < 45 * 45 && l.getY() > oy - 40 && l.getY() < oy + 30;
    }

    protected static Location facing(Location from, Location to) {
        Location l = from.clone();
        l.setDirection(to.toVector().subtract(from.toVector()));
        l.setPitch(0);
        return l;
    }

    protected void set(int x, int y, int z, Material m) {
        world.getBlockAt(x, y, z).setType(m, false);
    }

    public State state() {
        return state;
    }

    public boolean has(UUID id) {
        return players.contains(id);
    }

    public boolean isAlive(UUID id) {
        return alive.contains(id);
    }

    public int size() {
        return players.size();
    }

    public int signupSecondsLeft() {
        return (signupTicks + 19) / 20;
    }

    public void open() {
        signupTicks = plugin.getConfig().getInt("zapisy.sekundy", 60) * 20;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        Msg.broadcast(plugin.lang().withPrefix("ogloszenie.zapisy", kv(
                "sekundy", String.valueOf(signupSecondsLeft()), "liczba", "0", "max", String.valueOf(max()))));
    }

    protected int max() {
        return plugin.getConfig().getInt("zapisy.max-graczy", 12);
    }

    protected int min() {
        return Math.max(1, plugin.getConfig().getInt("zapisy.min-graczy", 2));
    }

    protected String[] kv(String... more) {
        return plugin.lang().modeKv(mode, more);
    }

    public enum JoinResult { OK, FULL, STARTED, FAILED, BROKEN_SNAPSHOT }

    public JoinResult join(Player p) {
        if (state != State.SIGNUP) return JoinResult.STARTED;
        if (players.size() >= max()) return JoinResult.FULL;
        if (plugin.schowek().has(p.getUniqueId()) && !plugin.schowek().restore(p, false)) return JoinResult.BROKEN_SNAPSHOT;
        if (!plugin.schowek().saveAndClear(p)) return JoinResult.FAILED;
        UUID id = p.getUniqueId();
        players.add(id);
        alive.add(id);
        int s = 0;
        while (slot.containsValue(s)) s++;
        slot.put(id, s);
        if (!p.teleport(spawn(s, max()))) {
            players.remove(id);
            alive.remove(id);
            slot.remove(id);
            plugin.schowek().restore(p, false);
            return JoinResult.FAILED;
        }
        BossBar bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
        if (Msg.bossBar(p, bar) != null) bars.put(id, bar);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        Msg.chat(p, Msg.INFO, plugin.lang().withPrefix("info.dolaczyles", kv()));
        toPlayers(plugin.lang().get("gra.dolaczyl", kv("gracz", p.getName(),
                "liczba", String.valueOf(players.size()), "max", String.valueOf(max()))));
        int full = plugin.getConfig().getInt("zapisy.pelne-sekundy", 10) * 20;
        if (players.size() >= max() && signupTicks > full) signupTicks = full;
        return JoinResult.OK;
    }

    public void leave(Player p, boolean quitting, boolean teleport) {
        UUID id = p.getUniqueId();
        if (state == State.CLOSED || !players.contains(id)) return;
        boolean wasAlive = alive.remove(id);
        if (wasAlive && (state == State.RUNNING) && starters.containsKey(id)) {
            out.add(id);
            quitters.add(id);
        }
        players.remove(id);
        slot.remove(id);
        BossBar bar = bars.remove(id);
        if (bar != null) p.hideBossBar(bar);
        if (!quitting) {
            plugin.schowek().restore(p, teleport);
            Msg.chat(p, Msg.INFO, plugin.lang().withPrefix("info.wyszedles"));

            String tier = pendingRewards.remove(id);
            if (tier != null) plugin.rewards().give(p, tier);
        }
        if (wasAlive) {
            toPlayers(plugin.lang().get("gra.wyszedl", kv("gracz", p.getName(),
                    "liczba", String.valueOf(alive.size()), "max", String.valueOf(max()))));
        }
        if (state == State.RUNNING) checkEnd();
    }

    public void forceStart() {
        if (state == State.SIGNUP) signupTicks = Math.min(signupTicks, 20);
    }

    private void tick() {
        switch (state) {
            case SIGNUP -> tickSignup();
            case COUNTDOWN -> tickCountdown();
            case RUNNING -> tickRunning();
            case ENDING -> {
                for (Player p : online(players)) {
                    if (p.getLocation().getY() < fallY()) {
                        p.teleport(spectatorPoint());
                        p.setFallDistance(0);
                    }
                }
            }
            default -> {
            }
        }
    }

    private void tickSignup() {
        guardFalls();
        signupTicks--;
        if (signupTicks % 20 == 0) {
            int sec = signupTicks / 20;
            for (int r : plugin.getConfig().getIntegerList("zapisy.przypomnienia")) {
                if (r == sec && players.size() < max()) {
                    Msg.broadcast(plugin.lang().withPrefix("ogloszenie.przypomnienie", kv("sekundy", String.valueOf(sec),
                            "liczba", String.valueOf(players.size()), "max", String.valueOf(max()))));
                }
            }
            updateBars();
        }
        if (signupTicks > 0) return;
        if (players.size() < min()) {
            Msg.broadcast(plugin.lang().withPrefix("ogloszenie.przepadla", kv(
                    "liczba", String.valueOf(players.size()), "min", String.valueOf(min()))));
            close();
            return;
        }
        state = State.COUNTDOWN;
        countdownTicks = plugin.getConfig().getInt("zapisy.odliczanie-sekundy", 5) * 20;

        int i = 0, n = alive.size();
        for (UUID id : alive) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                slot.put(id, i);
                p.teleport(spawn(i, n));
            }
            i++;
        }
    }

    private void tickCountdown() {
        guardFalls();
        if (countdownTicks % 20 == 0 && countdownTicks > 0) {
            String sec = String.valueOf(countdownTicks / 20);
            for (Player p : online(alive)) {
                Msg.title(p, plugin.lang().get("tytul.odliczanie", kv("sekundy", sec)),
                        plugin.lang().get("tytul.odliczanie-pod", kv()), 0, 22, 0);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
            }
            updateBars();
        }
        if (--countdownTicks > 0) return;
        if (alive.size() < min()) {
            Msg.broadcast(plugin.lang().withPrefix("ogloszenie.przepadla", kv(
                    "liczba", String.valueOf(alive.size()), "min", String.valueOf(min()))));
            close();
            return;
        }
        state = State.RUNNING;
        runTicks = 0;
        for (Player p : online(alive)) {
            starters.put(p.getUniqueId(), p.getName());
            giveKit(p);
            Msg.title(p, plugin.lang().get("tytul.start", kv()), plugin.lang().get("tytul.start-pod", kv()), 0, 30, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.4f);
        }
        toPlayers(plugin.lang().get("gra.start", kv()));
        onStart();
    }

    private void tickRunning() {
        runTicks++;
        for (Player p : online(new ArrayList<>(alive))) {
            if (state != State.RUNNING) return;
            if (p.getLocation().getY() < fallY()) onFall(p);
        }

        for (Player p : online(players)) {
            if (!alive.contains(p.getUniqueId()) && !inBounds(p.getLocation())) p.teleport(spectatorPoint());
        }
        if (state != State.RUNNING) return;
        onRunTick();
        if (state != State.RUNNING) return;
        if (runTicks % 20 == 0) updateBars();
        if (runTicks >= cfg.getInt("max-sekund", 300) * 20) {
            timeout();
            return;
        }
        checkEnd();
    }

    protected void timeout() {
        finish(null);
    }

    private void guardFalls() {
        for (Player p : online(alive)) {
            if (p.getLocation().getY() < fallY() || !inBounds(p.getLocation())) {
                p.teleport(spawn(slot.getOrDefault(p.getUniqueId(), 0), state == State.SIGNUP ? max() : alive.size()));
                p.setFallDistance(0);
            }
        }
    }

    protected void checkEnd() {
        if (state != State.RUNNING) return;
        boolean multi = starters.size() >= 2;
        if (alive.isEmpty() || (multi && alive.size() == 1)) {
            finish(alive.isEmpty() ? (out.isEmpty() ? null : out.getLast()) : alive.iterator().next());
        }
    }

    protected void eliminate(Player p) {
        UUID id = p.getUniqueId();

        if (state != State.RUNNING || !alive.remove(id)) return;
        out.add(id);
        int place = alive.size() + 1;
        p.getInventory().clear();
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(spectatorPoint());
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.8f);
        Msg.title(p, plugin.lang().get("tytul.odpadles"), plugin.lang().get("tytul.odpadles-pod", "miejsce", String.valueOf(place)), 5, 40, 10);
        toPlayers(plugin.lang().get("gra.odpadl", kv("gracz", p.getName(), "zywi", String.valueOf(alive.size()))));
        checkEnd();
    }

    protected void finish(UUID decided) {
        if (state != State.RUNNING) return;
        state = State.ENDING;
        onEnd();
        List<UUID> rank = ranking();
        if (decided != null) {
            rank.remove(decided);
            rank.addFirst(decided);
        }

        final UUID winner = starters.size() < 2 ? null : decided;
        String winnerName = winner == null ? null : starters.getOrDefault(winner, "?");

        boolean forfeit = winner != null && starters.keySet().stream().filter(id -> !id.equals(winner)).allMatch(quitters::contains);

        Lang lang = plugin.lang();
        toPlayers(lang.get("gra.podsumowanie-naglowek", kv()));
        for (int i = 0; i < rank.size() && i < 5; i++) {
            UUID id = rank.get(i);
            String place = winner == null && alive.contains(id) ? "–" : String.valueOf(i + 1);
            toPlayers(lang.get("gra.podsumowanie-miejsce", kv("miejsce", place, "gracz", starters.getOrDefault(id, "?"), "wynik", scoreSuffix(id))));
        }
        for (Player p : online(players)) {
            if (winner != null && p.getUniqueId().equals(winner)) {
                Msg.title(p, lang.get("tytul.wygrales"), lang.get("tytul.wygrales-pod", kv()), 5, 70, 20);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            } else if (winner != null) {
                Msg.title(p, lang.get("tytul.koniec", "gracz", winnerName), lang.get("tytul.koniec-pod", kv()), 5, 70, 20);
            } else {
                Msg.title(p, lang.get("tytul.koniec-remis"), Component.empty(), 5, 50, 20);
            }
        }
        if (winner != null) Msg.broadcast(lang.withPrefix("ogloszenie.wygrana", kv("gracz", winnerName)));
        else Msg.broadcast(lang.withPrefix("ogloszenie.remis", kv()));

        for (var e : starters.entrySet()) plugin.stats().record(e.getKey(), e.getValue(), mode, e.getKey().equals(winner));
        plugin.stats().saveAsync();

        Player w = winner == null ? null : Bukkit.getPlayer(winner);
        if (w != null) fireworks(w.getLocation());

        for (int i = 0; i < rank.size(); i++) {
            UUID id = rank.get(i);
            String tier;
            if (forfeit || starters.size() < 2) tier = "udzial";
            else if (winner == null) tier = alive.contains(id) ? "miejsce-2" : "udzial";
            else tier = i < 3 ? "miejsce-" + (i + 1) : "udzial";
            pendingRewards.put(id, tier);
        }
        Bukkit.getScheduler().runTaskLater(plugin, this::close, 140L);
    }

    private void fireworks(Location at) {
        Color[] colors = {Color.fromRGB(0xffc02e), Color.fromRGB(0xff7a1a), Color.WHITE};
        for (int i = 0; i < 3; i++) {
            int delay = i * 12;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Firework f = world.spawn(at.clone().add(Math.random() * 4 - 2, 1, Math.random() * 4 - 2), Firework.class, fw -> {
                    var meta = fw.getFireworkMeta();
                    meta.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(colors).withFade(Color.WHITE).trail(true).build());
                    meta.setPower(1);
                    fw.setFireworkMeta(meta);
                    fw.setPersistent(false);
                });
                Bukkit.getScheduler().runTaskLater(plugin, f::detonate, 20L);
            }, delay);
        }
    }

    public void close() {
        if (state == State.CLOSED) return;
        state = State.CLOSED;
        if (task != null) task.cancel();
        for (Player p : online(players)) {
            BossBar bar = bars.get(p.getUniqueId());
            if (bar != null) p.hideBossBar(bar);
            plugin.schowek().restore(p, true);
            String tier = pendingRewards.remove(p.getUniqueId());
            if (tier != null) plugin.rewards().give(p, tier);
        }
        pendingRewards.clear();
        players.clear();
        alive.clear();
        bars.clear();
        onEnd();
        build();
        plugin.manager().gameClosed(this);
    }

    protected void updateBars() {
        for (Player p : online(players)) {
            BossBar bar = bars.get(p.getUniqueId());
            if (bar == null) continue;
            String text;
            String[] extra;
            float progress;
            if (state == State.SIGNUP) {
                text = plugin.lang().raw("pasek.zapisy");
                extra = new String[]{"sekundy", String.valueOf(signupSecondsLeft()), "liczba", String.valueOf(players.size()), "max", String.valueOf(max())};
                progress = Math.clamp(signupTicks / (plugin.getConfig().getInt("zapisy.sekundy", 60) * 20f), 0f, 1f);
            } else if (!alive.contains(p.getUniqueId())) {
                text = plugin.lang().raw("pasek.ogladasz");
                extra = new String[0];
                progress = 1f;
            } else {
                text = barText(p);
                int left = Math.max(0, cfg.getInt("max-sekund", 300) - runTicks / 20);
                String[] base = {"zywi", String.valueOf(alive.size()), "czas", String.format("%d:%02d", left / 60, left % 60)};
                String[] own = barKv(p);
                extra = new String[base.length + own.length];
                System.arraycopy(base, 0, extra, 0, base.length);
                System.arraycopy(own, 0, extra, base.length, own.length);
                progress = Math.clamp(left / (float) cfg.getInt("max-sekund", 300), 0f, 1f);
            }
            bar.name(Lang.parse(text, kv(extra)));
            bar.progress(progress);
        }
    }

    protected void toPlayers(Component c) {
        for (Player p : online(players)) Msg.chat(p, Msg.GAME, c);
    }

    protected static List<Player> online(Iterable<UUID> ids) {
        List<Player> l = new ArrayList<>();
        for (UUID id : ids) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) l.add(p);
        }
        return l;
    }

    protected static ItemStack named(Material m, String mini) {
        ItemStack it = new ItemStack(m);
        it.editMeta(meta -> {
            meta.displayName(Lang.MM.deserialize("<!italic>" + mini));
            meta.setUnbreakable(true);
        });
        return it;
    }
}
