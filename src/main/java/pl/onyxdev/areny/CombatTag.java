package pl.onyxdev.areny;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class CombatTag implements Listener {
    private final Commands commands;

    public CombatTag(Commands commands) {
        this.commands = commands;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        if (e.getDamager() instanceof Player p) attacker = p;
        else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player p) attacker = p;
        if (attacker == null || attacker.equals(victim)) return;
        commands.tagCombat(victim.getUniqueId());
        commands.tagCombat(attacker.getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        commands.forget(e.getPlayer().getUniqueId());
    }
}
