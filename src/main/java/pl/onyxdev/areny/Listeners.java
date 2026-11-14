package pl.onyxdev.areny;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import pl.onyxdev.areny.gra.Game;

import java.util.Locale;

public final class Listeners implements Listener {
    private final SunAreny plugin;

    public Listeners(SunAreny plugin) {
        this.plugin = plugin;
    }

    private boolean inArenaWorld(Entity e) {
        return e.getWorld().getName().equals(plugin.arenaWorldName());
    }

    private Game game(Player p) {
        return plugin.manager().gameOf(p.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (plugin.schowek().restoreIfPending(p)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) Msg.chat(p, Msg.INFO, plugin.lang().withPrefix("info.przywrocono"));
            }, 40L);
        } else if (inArenaWorld(p) && !p.hasPermission("sunareny.admin")) {
            p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.manager().leave(e.getPlayer(), true, false);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        if (e.getFrom().getName().equals(plugin.arenaWorldName()) && game(p) != null) {
            plugin.manager().leave(p, false, false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        if (game(p) == null) return;
        var c = e.getCause();
        if (c == PlayerTeleportEvent.TeleportCause.ENDER_PEARL || c == PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT
                || c == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            e.setCancelled(true);
            return;
        }

        if (c != PlayerTeleportEvent.TeleportCause.PLUGIN && c != PlayerTeleportEvent.TeleportCause.UNKNOWN
                && e.getTo().getWorld() != p.getWorld()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        Player p = e.getPlayer();
        if (game(p) == null || p.hasPermission("sunareny.komendy")) return;
        String label = e.getMessage().substring(1).split(" ", 2)[0].toLowerCase(Locale.ROOT);
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);
        if (plugin.getConfig().getStringList("dozwolone-komendy").contains(label)) return;
        e.setCancelled(true);
        Msg.chat(p, Msg.INFO, plugin.lang().withPrefix("info.komenda-zablokowana"));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent e) {
        Game g = game(e.getPlayer());
        if (g != null && g.instaBreak(e.getPlayer(), e.getBlock())) e.setInstaBreak(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Game g = game(p);
        if (g != null) {
            if (g.allowBreak(p, e.getBlock())) g.onBreak(e);
            else e.setCancelled(true);
        } else if (inArenaWorld(p) && !p.hasPermission("sunareny.admin")) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (game(p) != null || (inArenaWorld(p) && !p.hasPermission("sunareny.admin"))) e.setCancelled(true);
    }

    @EventHandler
    public void onProjectile(ProjectileHitEvent e) {
        Projectile pr = e.getEntity();
        if (!inArenaWorld(pr) || !(pr.getShooter() instanceof Player p)) return;
        Game g = game(p);
        if (g != null) g.onProjectileHit(e);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!inArenaWorld(e.getEntity())) return;
        e.setCancelled(true);
        if (e.getEntity() instanceof Player victim && e.getDamager() instanceof Player attacker) {
            Game g = game(attacker);
            if (g != null && g == game(victim)) g.onHit(attacker, victim);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!inArenaWorld(e.getEntity())) return;
        if (!(e.getEntity() instanceof Player p)) {
            if (e.getCause() != EntityDamageEvent.DamageCause.VOID) e.setCancelled(true);
            return;
        }
        e.setCancelled(true);

        if (e.getCause() == EntityDamageEvent.DamageCause.VOID && game(p) == null) {
            p.setFallDistance(0);
            p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (inArenaWorld(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        if (game(p) == null) return;
        e.setKeepInventory(true);
        e.getDrops().clear();
        e.setKeepLevel(true);
        e.setDroppedExp(0);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (game(p) == null) return;
        Bukkit.getScheduler().runTask(plugin, () -> plugin.manager().leave(p, false, true));
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (inArenaWorld(e.getEntity()) && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (game(e.getPlayer()) != null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && game(p) != null) e.setCancelled(true);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Menu menu)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (e.getWhoClicked() instanceof Player p) menu.click(p, e.getSlot());
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Menu) e.setCancelled(true);
    }
}
