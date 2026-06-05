package com.jolly.hoverworth.listeners;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.jolly.hoverworth.HoverWorth;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WindowListener implements PacketListener {
    private final HoverWorth plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final PlainTextComponentSerializer plainText = PlainTextComponentSerializer.plainText();

    public WindowListener(HoverWorth plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            addLoreToWindowItems(event);
        } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            addLoreToSetSlot(event);
        }
    }

    private void addLoreToWindowItems(PacketSendEvent event) {
        WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
        if (!isVanillaInventory(wrapper.getWindowId(), event)) return;

        List<ItemStack> items = wrapper.getItems();
        if (items == null) return;

        List<ItemStack> displayedItems = new ArrayList<>(items.size());
        for (ItemStack item : items) {
            displayedItems.add(addLore(item));
        }
        wrapper.setItems(displayedItems);
    }

    private void addLoreToSetSlot(PacketSendEvent event) {
        WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
        if (!isVanillaInventory(wrapper.getWindowId(), event)) return;

        ItemStack item = wrapper.getItem();
        if (item == null || item.isEmpty()) return;

        wrapper.setItem(addLore(item));
    }

    private ItemStack addLore(ItemStack packetItem) {
        if (packetItem == null || packetItem.isEmpty()) return packetItem;

        org.bukkit.inventory.ItemStack displayItem = SpigotConversionUtil.toBukkitItemStack(packetItem).clone();
        ItemMeta meta = displayItem.getItemMeta();
        if (meta == null) return packetItem;

        String itemKey = displayItem.getType().name();
        if (plugin.getWorthFile() == null || !plugin.getWorthFile().get().contains(itemKey + ".worth")) return packetItem;

        double worth = plugin.getWorthFile().get().getDouble(itemKey + ".worth");
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        updateWorthLine(lore, worth);
        addDescriptionLine(lore, itemKey);
        meta.lore(lore);
        displayItem.setItemMeta(meta);

        return SpigotConversionUtil.fromBukkitItemStack(displayItem);
    }

    private void updateWorthLine(List<Component> lore, double worth) {
        String template = plugin.getConfig().getString(
                "settings.lore-message",
                "<white>Worth: <gold>{currency-symbol}{worth}<white>/pc"
        );
        String symbol = plugin.getConfig().getString("settings.currency-symbol", "$");
        Component worthLine = miniMessage.deserialize(
                template.replace("{currency-symbol}", symbol)
                        .replace("{worth}", formatWorth(worth))
        ).decoration(TextDecoration.ITALIC, false);

        String plainTemplate = plainText.serialize(miniMessage.deserialize(template
                .replace("{currency-symbol}", symbol)
                .replace("{worth}", "{worth}")));
        int worthPlaceholder = plainTemplate.indexOf("{worth}");
        if (worthPlaceholder < 0) {
            lore.removeIf(line -> plainText.serialize(line).equals(plainTemplate));
        } else {
            String prefix = plainTemplate.substring(0, worthPlaceholder);
            String suffix = plainTemplate.substring(worthPlaceholder + "{worth}".length());
            lore.removeIf(line -> isWorthLine(plainText.serialize(line), prefix, suffix));
        }
        lore.addFirst(worthLine);
    }

    private boolean isWorthLine(String line, String prefix, String suffix) {
        if (!line.startsWith(prefix) || !line.endsWith(suffix)) return false;

        String displayedWorth = line.substring(prefix.length(), line.length() - suffix.length());
        try {
            Double.parseDouble(displayedWorth);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private String formatWorth(double worth) {
        return BigDecimal.valueOf(worth).stripTrailingZeros().toPlainString();
    }

    private void addDescriptionLine(List<Component> lore, String itemKey) {
        String description = plugin.getWorthFile().get().getString(itemKey + ".description", "");
        if (description.isEmpty()) return;

        Component descriptionLine = miniMessage.deserialize(description);
        String plainDescription = plainText.serialize(descriptionLine).trim();
        boolean hasDescription = lore.stream()
                .map(line -> plainText.serialize(line).trim())
                .anyMatch(line -> line.equalsIgnoreCase(plainDescription));
        if (hasDescription) return;

        if (!description.contains("<italic>") && !description.contains("<i>")) {
            descriptionLine = descriptionLine.decoration(TextDecoration.ITALIC, false);
        }
        lore.addLast(descriptionLine);
    }

    private boolean isVanillaInventory(int windowId, PacketSendEvent event) {
        Player player = (Player) event.getPlayer();
        if (windowId == 0) return true;

        InventoryView view = player.getOpenInventory();
        if (view == null) return false;

        InventoryType type = view.getType();
        String title = ChatColor.stripColor(view.getTitle());
        if (title == null || isCustomTitle(title.toLowerCase(Locale.ROOT))) return false;

        return switch (type) {
            case CHEST, ENDER_CHEST, BARREL, HOPPER, FURNACE, BLAST_FURNACE, SMOKER,
                 ANVIL, BEACON, BREWING, DISPENSER, DROPPER, SHULKER_BOX -> true;
            default -> false;
        };
    }

    private boolean isCustomTitle(String title) {
        String[] vanillaTitles = {
                "chest", "large chest", "ender chest", "furnace", "blast furnace", "smoker",
                "anvil", "hopper", "beacon", "brewing stand", "dispenser", "dropper",
                "shulker box", "barrel", "crafting"
        };

        for (String vanillaTitle : vanillaTitles) {
            if (title.equals(vanillaTitle)) return false;
        }
        return true;
    }
}
