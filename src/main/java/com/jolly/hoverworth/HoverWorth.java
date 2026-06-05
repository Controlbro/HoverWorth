package com.jolly.hoverworth;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.jolly.hoverworth.commands.Reload;
import com.jolly.hoverworth.listeners.WindowListener;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import org.bukkit.plugin.java.JavaPlugin;

public final class HoverWorth extends JavaPlugin {
    private WorthFile worthFile;

    @Override
    public void onLoad() {
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().load();
        PacketEvents.getAPI().getEventManager().registerListener(new WindowListener(this), PacketListenerPriority.HIGHEST);
    }

    @Override
    public void onEnable() {
        new Metrics(this, 27893);
        saveDefaultConfig();
        worthFile = new WorthFile(this);
        PacketEvents.getAPI().init();
        getCommand("hwreload").setExecutor(new Reload(this));
        getLogger().info("HoverWorth enabled ✅");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    public WorthFile getWorthFile() {
        return worthFile;
    }
}
