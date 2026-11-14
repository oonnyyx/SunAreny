package pl.onyxdev.areny.gra;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.onyxdev.areny.Mode;
import pl.onyxdev.areny.SunAreny;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class TntRun extends Game {
    private static final int[] LAYERS = {0, -10};
    private static final int HALF = 15;
    private static final Set<Material> FLOOR = Set.of(Material.YELLOW_CONCRETE, Material.ORANGE_CONCRETE,
            Material.LIGHT_BLUE_CONCRETE, Material.WHITE_CONCRETE);

    private final Map<Block, Integer> doomed = new HashMap<>();

    public TntRun(SunAreny plugin, World world) {
        super(plugin, Mode.TNTRUN, world);
    }

    @Override
    public int build() {
        int n = 0;
        for (int i = 0; i < LAYERS.length; i++) {
            int y = oy + LAYERS[i];
            for (int x = -HALF; x <= HALF; x++) {
                for (int z = -HALF; z <= HALF; z++) {
                    boolean even = ((x + z) & 1) == 0;
                    Material top = i == 0 ? (even ? Material.YELLOW_CONCRETE : Material.ORANGE_CONCRETE)
                            : (even ? Material.LIGHT_BLUE_CONCRETE : Material.WHITE_CONCRETE);
                    set(ox + x, y, oz + z, top);
                    set(ox + x, y - 1, oz + z, Material.TNT);
                    n += 2;
                }
            }
        }

        for (int sx : new int[]{-HALF - 4, HALF + 4}) {
            for (int sz : new int[]{-HALF - 4, HALF + 4}) {
                for (int y = oy - 16; y <= oy + 2; y++) set(ox + sx, y, oz + sz, Material.CUT_SANDSTONE);
                set(ox + sx, oy + 3, oz + sz, Material.LANTERN);
                n += 20;
            }
        }
        doomed.clear();
        return n;
    }

    @Override
    protected Location spawn(int i, int n) {
        double a = Math.PI * 2 * i / Math.max(1, n);
        Location l = new Location(world, ox + 0.5 + Math.cos(a) * 10, oy + 1, oz + 0.5 + Math.sin(a) * 10);
        return facing(l, center().add(0, 1, 0));
    }

    @Override
    protected double fallY() {
        return oy + LAYERS[LAYERS.length - 1] - 6;
    }

    @Override
    protected void giveKit(Player p) {
    }

    @Override
    protected void onRunTick() {
        int delay = Math.max(2, cfg.getInt("opoznienie-tickow", 7));
        if (runTicks > cfg.getInt("ochrona-sekund", 3) * 20) {
            for (Player p : online(alive)) {
                Location l = p.getLocation();
                double y = l.getY() - 0.2;

                for (double dx : new double[]{-0.3, 0.3}) {
                    for (double dz : new double[]{-0.3, 0.3}) {
                        Block b = world.getBlockAt((int) Math.floor(l.getX() + dx), (int) Math.floor(y), (int) Math.floor(l.getZ() + dz));
                        if (FLOOR.contains(b.getType()) && !doomed.containsKey(b)) {
                            doomed.put(b, runTicks + delay);
                            b.setType(Material.RED_CONCRETE, false);
                        }
                    }
                }
            }
        }
        Iterator<Map.Entry<Block, Integer>> it = doomed.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (e.getValue() > runTicks) continue;
            Block b = e.getKey();
            b.setType(Material.AIR, false);
            b.getRelative(0, -1, 0).setType(Material.AIR, false);
            world.spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5), 6, 0.25, 0.25, 0.25, Material.RED_CONCRETE.createBlockData());
            if ((runTicks & 3) == 0) world.playSound(b.getLocation(), Sound.BLOCK_SAND_BREAK, 0.6f, 1.4f);
            it.remove();
        }
    }
}
