package pl.onyxdev.areny.gra;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Vector;
import pl.onyxdev.areny.Lang;
import pl.onyxdev.areny.Mode;
import pl.onyxdev.areny.Msg;
import pl.onyxdev.areny.SunAreny;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class Krol extends Game {
    private static final int R = 13;
    private static final int TOP = 3;

    private final Map<UUID, Integer> points = new HashMap<>();
    private final Map<UUID, Long> lastHit = new HashMap<>();
    private UUID leader;
    private TextDisplay label;

    public Krol(SunAreny plugin, World world) {
        super(plugin, Mode.KROL, world);
    }

    @Override
    public int build() {
        int n = 0;

        for (int d = 0; d <= 6; d++) {
            int r = R - d * 2;
            if (r < 1) break;
            Material m = d == 0 ? Material.GRASS_BLOCK : d < 3 ? Material.DIRT : Material.STONE;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    if (Math.sqrt(x * x + z * z) <= r + 0.5) {
                        set(ox + x, oy - d, oz + z, m);
                        n++;
                    }
                }
            }
        }

        int[][] hill = {{1, 5}, {2, 3}};
        for (int[] h : hill) {
            for (int x = -h[1]; x <= h[1]; x++) {
                for (int z = -h[1]; z <= h[1]; z++) {
                    if (Math.sqrt(x * x + z * z) <= h[1] + 0.5) {
                        set(ox + x, oy + h[0], oz + z, h[0] == 1 ? Material.MOSS_BLOCK : Material.STONE_BRICKS);
                        n++;
                    }
                }
            }
        }
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) set(ox + x, oy + TOP, oz + z, Material.GOLD_BLOCK);
        set(ox, oy + TOP, oz, Material.BEACON);
        n += 9;
        return n;
    }

    @Override
    protected Location spawn(int i, int n) {
        double a = Math.PI * 2 * i / Math.max(1, n);
        Location l = new Location(world, ox + 0.5 + Math.cos(a) * 10.5, oy + 1, oz + 0.5 + Math.sin(a) * 10.5);
        return facing(l, center().add(0, 1, 0));
    }

    @Override
    protected double fallY() {
        return oy - 10;
    }

    @Override
    protected void giveKit(Player p) {
        p.getInventory().setItem(0, named(Material.STICK, "<gold><b>Kij Króla"));
    }

    @Override
    protected void onStart() {
        for (UUID id : alive) points.put(id, 0);
        label = world.spawn(new Location(world, ox + 0.5, oy + TOP + 4.2, oz + 0.5), TextDisplay.class, t -> {
            t.setBillboard(Display.Billboard.CENTER);
            t.setPersistent(false);
            t.setShadowed(true);
            t.setBackgroundColor(Color.fromARGB(90, 0, 0, 0));
            t.text(Lang.MM.deserialize("<gold><b>👑 Szczyt wolny"));
        });
    }

    private boolean onTop(Player p) {
        Location l = p.getLocation();
        double dx = l.getX() - (ox + 0.5), dz = l.getZ() - (oz + 0.5);
        return dx * dx + dz * dz <= 1.6 * 1.6 && l.getY() >= oy + TOP + 1 - 0.05 && l.getY() < oy + TOP + 3;
    }

    @Override
    protected void onRunTick() {
        if (runTicks % 20 != 0) return;
        int target = cfg.getInt("do-wygranej", 60);

        List<Player> top = new ArrayList<>();
        for (Player p : online(alive)) if (onTop(p)) top.add(p);
        if (top.size() == 1) {
            Player p = top.getFirst();
            int v = points.merge(p.getUniqueId(), 1, Integer::sum);
            Msg.actionBar(p, plugin.lang().get("pasek.na-szczycie"));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.5f, 1f + Math.min(1f, v / (float) target));
            world.spawnParticle(Particle.WAX_ON, p.getLocation().add(0, 1, 0), 6, 0.4, 0.5, 0.4, 0);
            if (v >= target) {
                finish(p.getUniqueId());
                return;
            }
        }
        UUID best = bestPlayer();
        if (best != null && !best.equals(leader) && points.getOrDefault(best, 0) > 0) {
            leader = best;
            toPlayers(plugin.lang().get("gra.lider-krol", kv("gracz", starters.getOrDefault(best, "?"))));
        }
        if (label != null && label.isValid()) {
            label.text(leader == null ? Lang.MM.deserialize("<gold><b>👑 Szczyt wolny")
                    : Lang.parse("<gold><b>👑 <white><gracz></white></b> <gray><pkt>/<cel>",
                    "gracz", starters.getOrDefault(leader, "?"), "pkt", String.valueOf(points.getOrDefault(leader, 0)), "cel", String.valueOf(target)));
        }
    }

    private UUID bestPlayer() {
        return alive.stream().max(Comparator.comparingInt(id -> points.getOrDefault(id, 0))).orElse(null);
    }

    @Override
    protected void onFall(Player p) {
        int kara = cfg.getInt("kara-za-upadek", 5);
        points.computeIfPresent(p.getUniqueId(), (k, v) -> Math.max(0, v - kara));
        Location s = spawn(ThreadLocalRandom.current().nextInt(12), 12);
        p.teleport(s);
        p.setFallDistance(0);
        p.setVelocity(new Vector());
        p.playSound(s, Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1.2f);
        toPlayers(plugin.lang().get("gra.upadek-krol", kv("gracz", p.getName(), "kara", String.valueOf(kara))));
    }

    @Override
    public void onHit(Player attacker, Player victim) {
        if (state != State.RUNNING || !isAlive(attacker.getUniqueId()) || !isAlive(victim.getUniqueId())) return;
        long now = System.currentTimeMillis();
        Long last = lastHit.get(attacker.getUniqueId());
        if (last != null && now - last < 450) return;
        lastHit.put(attacker.getUniqueId(), now);
        double power = cfg.getDouble("odrzut", 1.15);
        boolean stick = attacker.getInventory().getItemInMainHand().getType() == Material.STICK;
        Vector dir = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0);
        if (dir.lengthSquared() < 1e-4) dir = attacker.getLocation().getDirection().setY(0);
        dir.normalize().multiply(stick ? power : power * 0.5).setY(stick ? 0.42 : 0.3);
        victim.setVelocity(dir);
        victim.playHurtAnimation(attacker.getLocation().getYaw());
        world.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 1f);
    }

    @Override
    protected void timeout() {
        UUID best = bestPlayer();
        finish(best != null && points.getOrDefault(best, 0) > 0 ? best : null);
    }

    @Override
    protected void checkEnd() {
        if (state != State.RUNNING) return;
        if (alive.isEmpty()) finish(null);
        else if (starters.size() >= 2 && alive.size() == 1) finish(alive.iterator().next());
    }

    @Override
    protected List<UUID> ranking() {
        List<UUID> r = new ArrayList<>(starters.keySet());
        r.sort(Comparator.comparingInt((UUID id) -> points.getOrDefault(id, 0)).reversed());
        return r;
    }

    @Override
    protected String scoreSuffix(UUID id) {
        return " <dark_gray>· " + points.getOrDefault(id, 0) + " pkt";
    }

    @Override
    protected String barText(Player p) {
        return plugin.lang().raw("pasek.krol");
    }

    @Override
    protected String[] barKv(Player p) {
        return new String[]{"punkty", String.valueOf(points.getOrDefault(p.getUniqueId(), 0)),
                "cel", String.valueOf(cfg.getInt("do-wygranej", 60)),
                "lider", leader == null ? "—" : starters.getOrDefault(leader, "?")};
    }

    @Override
    protected void onEnd() {
        if (label != null) {
            label.remove();
            label = null;
        }
    }
}
