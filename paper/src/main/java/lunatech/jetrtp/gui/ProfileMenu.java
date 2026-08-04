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

    private static final Map<Material, ItemStack> FILLER_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private static ItemStack getFillerItem(Material material) {
        return FILLER_CACHE.computeIfAbsent(material, mat -> {
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.empty());
                item.setItemMeta(meta);
            }
            return item;
        });
    }

    private final Inventory inventory;
    private final AbstractJetRTP plugin;
    private final Map<Integer, RtpProfile> slotProfiles = new HashMap<>();

    @SuppressWarnings("this-escape")
    public ProfileMenu(Player player, AbstractJetRTP plugin) {
        this.plugin = plugin;
        var guiConfig = plugin.getConfigHandler().getConfig().gui;

        Component titleComponent = io.github.milkdrinkers.colorparser.paper.ColorParser.of(guiConfig.title).build();
        int invSize = guiConfig.size;
        if (invSize % 9 != 0 || invSize < 9 || invSize > 54) {
            invSize = 27;
        }

        this.inventory = Bukkit.createInventory(this, invSize, titleComponent);

        Material fillerMaterial = Material.GRAY_STAINED_GLASS_PANE;
        try {
            if (guiConfig.fillerMaterial != null) {
                fillerMaterial = Material.valueOf(guiConfig.fillerMaterial.toUpperCase());
            }
        } catch (Exception ignored) {}

        ItemStack filler = getFillerItem(fillerMaterial);
        for (int i = 0; i < invSize; i++) {
            inventory.setItem(i, filler);
        }

        if (guiConfig.layout != null) {
            for (Map.Entry<Integer, String> entry : guiConfig.layout.entrySet()) {
                int slot = entry.getKey();
                if (slot < 0 || slot >= invSize) continue;

                String profileName = entry.getValue();
                RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                if (profile == null || !profile.enabled || !profile.commandEnabled) continue;

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
            }
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
            player.sendMessage(io.github.milkdrinkers.colorparser.paper.ColorParser.of(lunatech.jetrtp.translation.Translation.of("rtp.cooldown")).with("remaining", String.valueOf(remaining)).build());
            return;
        }
        
        plugin.getRtpService().executeRtp(player, profile).thenAccept(success -> {
            if (!success) {
                player.sendMessage(io.github.milkdrinkers.colorparser.paper.ColorParser.of(lunatech.jetrtp.translation.Translation.of("rtp.teleport-failed")).build());
            }
        });
    }

    public static void open(Player player, AbstractJetRTP plugin) {
        ProfileMenu menu = new ProfileMenu(player, plugin);
        player.openInventory(menu.getInventory());
    }
}
