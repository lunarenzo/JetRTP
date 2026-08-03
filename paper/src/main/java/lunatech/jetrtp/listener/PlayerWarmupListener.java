package lunatech.jetrtp.listener;

import lunatech.jetrtp.service.RtpService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerWarmupListener implements Listener {

    private final RtpService rtpService;

    public PlayerWarmupListener(RtpService rtpService) {
        this.rtpService = rtpService;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (rtpService.hasActiveWarmup(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())) {
                rtpService.cancelWarmup(player);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (rtpService.hasActiveWarmup(player)) {
                rtpService.cancelWarmup(player);
            }
        }
    }
}
