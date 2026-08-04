package lunatech.jetrtp.gui;

import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.PluginConfig;
import lunatech.jetrtp.config.RtpProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileMenu implements InventoryHolder {

    private static final Map<String, ItemStack> itemCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static ItemStack fillerCache = null;
    private static Component titleCache = null;

    public static void clearCache() {
        itemCache.clear();
        fillerCache = null;
        titleCache = null;
    }

    private final Inventory inventory;
    private final AbstractJetRTP plugin;
    private final Map<Integer, String> slotActions = new HashMap<>();

    @SuppressWarnings("this-escape")
    public ProfileMenu(Player player, AbstractJetRTP plugin) {
        this.plugin = plugin;
        
        PluginConfig.RtpGuiConfig guiConfig = plugin.getConfigHandler().getConfig().gui;
        
        int rows = Math.clamp(guiConfig.rows, 1, 6);
        int size = rows * 9;
        
        Component titleComponent;
        if (titleCache != null) {
            titleComponent = titleCache;
        } else {
            titleComponent = io.github.milkdrinkers.colorparser.paper.ColorParser.of(guiConfig.title).legacy().build();
            titleCache = titleComponent;
        }
        this.inventory = Bukkit.createInventory(this, size, titleComponent);

        // Fill background
        ItemStack filler;
        if (fillerCache != null) {
            filler = fillerCache.clone();
        } else {
            Material fillMat = Material.GRAY_STAINED_GLASS_PANE;
            try {
                if (guiConfig.fillItem != null) {
                    fillMat = Material.valueOf(guiConfig.fillItem.toUpperCase());
                }
            } catch (Exception ignored) {}
            
            filler = new ItemStack(fillMat);
            ItemMeta fillerMeta = filler.getItemMeta();
            if (fillerMeta != null) {
                fillerMeta.displayName(Component.empty());
                filler.setItemMeta(fillerMeta);
            }
            fillerCache = filler.clone();
        }
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, filler.clone());
        }

        // Add custom items
        if (guiConfig.items != null) {
            for (Map.Entry<String, PluginConfig.RtpGuiItemConfig> entry : guiConfig.items.entrySet()) {
                int slot;
                try {
                    slot = Integer.parseInt(entry.getKey());
                } catch (NumberFormatException e) {
                    continue;
                }
                
                if (slot < 0 || slot >= size) {
                    continue;
                }
                
                PluginConfig.RtpGuiItemConfig itemConfig = entry.getValue();
                if (itemConfig == null) {
                    continue;
                }
                
                String action = itemConfig.action;
                if (action != null && action.startsWith("rtp:")) {
                    String profileName = action.substring(4).trim().toLowerCase();
                    if (!player.hasPermission("jakesrtp.usebyname") && !player.hasPermission("jakesrtp.use." + profileName)) {
                        continue; // Hide profile if player has no permission
                    }
                }
                
                ItemStack item;
                String cacheKey = entry.getKey();
                if (itemCache.containsKey(cacheKey)) {
                    item = itemCache.get(cacheKey).clone();
                } else {
                    Material itemMat = Material.BARRIER;
                    try {
                        if (itemConfig.material != null) {
                            itemMat = Material.valueOf(itemConfig.material.toUpperCase());
                        }
                    } catch (Exception ignored) {}
                    
                    item = new ItemStack(itemMat);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        if (itemConfig.name != null) {
                            meta.displayName(io.github.milkdrinkers.colorparser.paper.ColorParser.of(itemConfig.name).legacy().build());
                        }
                        if (itemConfig.lore != null) {
                            List<Component> loreList = new ArrayList<>();
                            for (String line : itemConfig.lore) {
                                loreList.add(io.github.milkdrinkers.colorparser.paper.ColorParser.of(line).legacy().build());
                            }
                            meta.lore(loreList);
                        }
                        item.setItemMeta(meta);
                    }
                    itemCache.put(cacheKey, item.clone());
                }
                
                inventory.setItem(slot, item);
                if (action != null) {
                    slotActions.put(slot, action);
                }
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return this.inventory;
    }

    public void handleClick(Player player, int slot) {
        String action = slotActions.get(slot);
        if (action == null) return;

        if (action.equalsIgnoreCase("close")) {
            player.closeInventory();
            return;
        }

        if (action.startsWith("rtp:")) {
            String profileName = action.substring(4).trim();
            RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
            if (profile == null) {
                player.closeInventory();
                player.sendMessage(io.github.milkdrinkers.colorparser.paper.ColorParser.of(lunatech.jetrtp.translation.Translation.of("rtp.unknown-profile")).with("profile", profileName).build());
                return;
            }

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
    }

    public static void open(Player player, AbstractJetRTP plugin) {
        ProfileMenu menu = new ProfileMenu(player, plugin);
        player.openInventory(menu.getInventory());
    }
}
