package pl.onyxdev.areny.gra;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import pl.onyxdev.areny.Mode;
import pl.onyxdev.areny.SunAreny;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Spleef extends Game {
    private static final int[][] LAYERS = {{0, 17}, {-9, 13}, {-18, 9}};
    private static final int MIN_RADIUS = 3;

    private final int[] radius = new int[LAYERS.length];
    private final List<Block> cracking = new ArrayList<>();
    private int crackAt = -1;

    public Spleef(SunAreny plugin, World world) {
        super(plugin, Mode.SPLEEF, world);
    }

    @Override
    public int build() {
        int n = 0;
        for (int i = 0; i < LAYERS.length; i++) {
            int dy = LAYERS[i][0], r = LAYERS[i][1];
            radius[i] = r;

            for (int x = -r - 3; x <= r + 3; x++) {
                for (int z = -r - 3; z <= r + 3; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    Material m = d <= r + 0.5 ? Material.SNOW_BLOCK : Material.AIR;
                    set(ox + x, oy + dy, oz + z, m);
                    n++;
                }
            }
        }

        for (int k = 0; k < 8; k++) {
            double a = Math.PI * 2 * k / 8;
            int x = ox + (int) Math.round(Math.cos(a) * 25), z = oz + (int) Math.round(Math.sin(a) * 25);
            for (int y = oy - 24; y <= oy + 3; y++) set(x, y, z, Material.QUARTZ_PILLAR);
            set(x, oy + 4, z, Material.SEA_LANTERN);
            set(x, oy + 5, z, Material.END_ROD);
            set(x, oy - 25, z, Material.SEA_LANTERN);
            n += 30;
        }
        cracking.clear();
        crackAt = -1;
        return n;
    }

    @Override
    protected Location spawn(int i, int n) {
        double a = Math.PI * 2 * i / Math.max(1, n);
        Location l = new Location(world, ox + 0.5 + Math.cos(a) * 11, oy + 1, oz + 0.5 + Math.sin(a) * 11);
        return facing(l, center().add(0, 1, 0));
    }

    @Override
    protected double fallY() {
        return oy + LAYERS[LAYERS.length - 1][0] - 6;
    }

    @Override
    protected void giveKit(Player p) {
        ItemStack shovel = named(Material.DIAMOND_SHOVEL, "<#9be7ff><b>Łopata Spleefa");
        shovel.addUnsafeEnchantment(Enchantment.EFFICIENCY, 5);
        p.getInventory().setItem(0, shovel);
    }

    private boolean isFloor(Block b) {
        if (b.getType() != Material.SNOW_BLOCK || b.getWorld() != world) return false;
        int dy = b.getY() - oy;
        for (int[] l : LAYERS) if (l[0] == dy) return Math.hypot(b.getX() - ox, b.getZ() - oz) <= l[1] + 0.5;
        return false;
    }

    @Override
    public boolean instaBreak(Player p, Block b) {
        return state == State.RUNNING && isAlive(p.getUniqueId()) && isFloor(b);
    }

    @Override
    public boolean allowBreak(Player p, Block b) {
        return instaBreak(p, b);
    }

    @Override
    public void onBreak(BlockBreakEvent e) {
        e.setDropItems(false);
        e.setExpToDrop(0);
        if (ThreadLocalRandom.current().nextDouble() < cfg.getDouble("sniezki-szansa", 0.35)) {
            Player p = e.getPlayer();
            if (p.getInventory().all(Material.SNOWBALL).values().stream().mapToInt(ItemStack::getAmount).sum() < 16) {
                p.getInventory().addItem(new ItemStack(Material.SNOWBALL));
            }
        }
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball) || state != State.RUNNING) return;
        Block b = e.getHitBlock();
        if (b != null && isFloor(b)) {
            world.spawnParticle(Particle.ITEM_SNOWBALL, b.getLocation().add(0.5, 1, 0.5), 8, 0.3, 0.2, 0.3, 0);
            b.setType(Material.AIR, false);
        }
        if (e.getHitEntity() instanceof Player victim && isAlive(victim.getUniqueId())) {
            Vector v = e.getEntity().getVelocity().setY(0);
            if (v.lengthSquared() > 0.001) victim.setVelocity(v.normalize().multiply(0.45).setY(0.25));
        }
    }

    @Override
    protected void onRunTick() {
        int every = Math.max(5, cfg.getInt("kurczenie-co", 25)) * 20;
        if (runTicks % every == 0) startCrack();
        if (crackAt > 0 && runTicks >= crackAt) {
            for (Block b : cracking) {
                if (b.getType() == Material.PACKED_ICE) b.setType(Material.AIR, false);
            }
            world.playSound(center(), Sound.BLOCK_GLASS_BREAK, 2f, 0.6f);
            cracking.clear();
            crackAt = -1;
        }
    }

    private void startCrack() {
        for (int i = 0; i < LAYERS.length; i++) {
            if (radius[i] <= MIN_RADIUS) continue;
            int from = radius[i] - 2, to = radius[i];
            int dy = LAYERS[i][0];
            for (int x = -to; x <= to; x++) {
                for (int z = -to; z <= to; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d <= from + 0.5 || d > to + 0.5) continue;
                    Block b = world.getBlockAt(ox + x, oy + dy, oz + z);
                    if (b.getType() == Material.SNOW_BLOCK) {
                        b.setType(Material.PACKED_ICE, false);
                        cracking.add(b);
                    }
                }
            }
            radius[i] = from;
            if (cracking.isEmpty()) continue;
            crackAt = runTicks + 50;
            world.playSound(center(), Sound.BLOCK_GLASS_HIT, 2f, 0.5f);
            toPlayers(plugin.lang().get("gra.kurczenie"));
            return;
        }
    }
}
