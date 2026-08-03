package lunatech.jetrtp.gui;

import dev.triumphteam.gui.builder.item.ItemBuilder;
import dev.triumphteam.gui.guis.Gui;
import dev.triumphteam.gui.guis.GuiItem;
import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.RtpProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import java.util.Collection;

public class ProfileMenu {

    public static void open(Player player, AbstractJetRTP plugin) {
        Gui gui = Gui.gui()
            .title(Component.text("Random Teleport Destinations", NamedTextColor.DARK_GRAY))
            .rows(3)
            .create();

        GuiItem filler = ItemBuilder.from(Material.GRAY_STAINED_GLASS_PANE)
            .name(Component.empty())
            .asGuiItem();
        gui.getFiller().fill(filler);

        Collection<RtpProfile> profiles = plugin.getConfigHandler().getProfiles().values();
        int slot = 10;

        for (RtpProfile profile : profiles) {
            if (!profile.enabled || !profile.commandEnabled) continue;
            
            if (!player.hasPermission("jakesrtp.usebyname") && !player.hasPermission("jakesrtp.use." + profile.name.toLowerCase())) {
                continue;
            }

            Material iconMat = Material.COMPASS;
            if (profile.name.equalsIgnoreCase("nether-rtp") || profile.name.contains("nether")) {
                iconMat = Material.NETHERRACK;
            } else if (profile.name.equalsIgnoreCase("end-rtp") || profile.name.contains("end")) {
                iconMat = Material.ENDER_PEARL;
            }

            GuiItem item = ItemBuilder.from(iconMat)
                .name(Component.text(profile.name.substring(0, 1).toUpperCase() + profile.name.substring(1), NamedTextColor.GREEN))
                .lore(
                    Component.text("Click to random teleport to this destination!", NamedTextColor.GRAY),
                    Component.empty(),
                    Component.text("Cooldown: ", NamedTextColor.GRAY).append(Component.text(profile.cooldown + "s", NamedTextColor.YELLOW)),
                    Component.text("Cost: ", NamedTextColor.GRAY).append(Component.text("$" + profile.cost, NamedTextColor.YELLOW))
                )
                .asGuiItem(event -> {
                    gui.close(player);
                    if (plugin.getRtpService().isOnCooldown(player, profile)) {
                        long remaining = plugin.getRtpService().getRemainingCooldown(player, profile) / 1000L;
                        player.sendMessage("§cYou must wait " + remaining + " seconds before using RTP again.");
                        return;
                    }
                    plugin.getRtpService().executeRtp(player, profile).thenAccept(success -> {
                        if (!success) {
                            player.sendMessage("§cTeleportation could not be completed at this time.");
                        }
                    });
                });

            gui.setItem(slot, item);
            slot += 2;
            if (slot > 16) break;
        }

        gui.open(player);
    }
}
