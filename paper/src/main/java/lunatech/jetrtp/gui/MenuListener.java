package lunatech.jetrtp.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class MenuListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder(false) instanceof ProfileMenu menu) {
            event.setCancelled(true);
            
            // Only process click if it's in the top inventory (the menu itself)
            if (event.getClickedInventory() == inv) {
                menu.handleClick((Player) event.getWhoClicked(), event.getSlot());
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder(false) instanceof ProfileMenu menu) {
            event.setCancelled(true);
        }
    }
}
