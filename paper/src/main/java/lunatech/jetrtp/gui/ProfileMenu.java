package lunatech.jetrtp.gui;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.RtpProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileMenu implements InventoryHolder {

    private final Inventory inventory;
    private final AbstractJetRTP plugin;
    private final Map<Integer, RtpProfile> slotProfiles = new HashMap<>();

    @SuppressWarnings("this-escape")
    public ProfileMenu(Player player, AbstractJetRTP plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("Random Teleport Destinations", NamedTextColor.DARK_GRAY));

        // Fill background
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.empty());
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

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

            ItemStack item = new ItemStack(iconMat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String formattedName = profile.name.substring(0, 1).toUpperCase() + profile.name.substring(1);
                meta.displayName(Component.text(formattedName, NamedTextColor.GREEN));
                
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Click to random teleport to this destination!", NamedTextColor.GRAY));
                lore.add(Component.empty());
                lore.add(Component.text("Cooldown: ", NamedTextColor.GRAY).append(Component.text(profile.cooldown + "s", NamedTextColor.YELLOW)));
                lore.add(Component.text("Cost: ", NamedTextColor.GRAY).append(Component.text("$" + profile.cost, NamedTextColor.YELLOW)));
                meta.lore(lore);
                item.setItemMeta(meta);
            }

            inventory.setItem(slot, item);
            slotProfiles.put(slot, profile);
            
            slot += 2;
            if (slot > 16) break;
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public void handleClick(Player player, int slot) {
        RtpProfile profile = slotProfiles.get(slot);
        if (profile == null) return;

        player.closeInventory();
        
        if (plugin.getRtpService().isOnCooldown(player, profile)) {
            long remaining = plugin.getRtpService().getRemainingCooldown(player, profile) / 1000L;
            player.sendMessage(io.github.milkdrinkers.colorparser.paper.ColorParser.of(io.github.milkdrinkers.wordweaver.Translation.of("rtp.cooldown")).with("remaining", String.valueOf(remaining)).build());
            return;
        }
        
        plugin.getRtpService().executeRtp(player, profile).thenAccept(success -> {
            if (!success) {
                player.sendMessage(io.github.milkdrinkers.colorparser.paper.ColorParser.of(io.github.milkdrinkers.wordweaver.Translation.of("rtp.teleport-failed")).build());
            }
        });
    }

    public static void open(Player player, AbstractJetRTP plugin) {
        ProfileMenu menu = new ProfileMenu(player, plugin);
        player.openInventory(menu.getInventory());
    }
}
