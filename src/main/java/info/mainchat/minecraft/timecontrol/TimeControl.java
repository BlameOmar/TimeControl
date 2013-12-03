/*
 * ©2013 Omar Stefan Evans <omar@evansbros.info>
 *
 * Permission is hereby granted to all persons to use and redistribute this software
 * for any purpose, with or without modification, provided that this notice appears
 * in all copies or substantial portions of the software.
 */

package info.mainchat.minecraft.timecontrol;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.UUID;

public final class TimeControl extends JavaPlugin implements Listener {

    private HashMap<UUID, String> doDaylightCycleOriginalValue;
    private HashMap<UUID, Integer> taskIDs;

    @Override
    public void onEnable() {
        if (doDaylightCycleOriginalValue == null) {
            doDaylightCycleOriginalValue = new HashMap<>();
        }

        if (taskIDs == null) {
            taskIDs = new HashMap<>();
        }

        for (World world : getServer().getWorlds()) {
            enableTimeControlForWorld(world);
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (World world : getServer().getWorlds()) {
            disableTimeControlForWorld(world);
        }

        getServer().getScheduler().cancelTasks(this);
        HandlerList.unregisterAll((JavaPlugin) this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerLeavesBed(PlayerBedLeaveEvent event) {
        World world = event.getPlayer().getWorld();
        ConfigurationSection worldConfiguration = getConfig().getConfigurationSection("worlds." + world.getName());
        if (worldConfiguration.getBoolean("scale time") && !worldConfiguration.getBoolean("lock time")) {
            getLogger().info("Checking sleeping players");
            for (Player player : world.getPlayers()) {
                if (player == event.getPlayer()) {
                    continue;
                }
                if (!player.isSleeping() && !player.isSleepingIgnored()) {
                    getLogger().info(event.getPlayer().getDisplayName() + "is not sleeping.");
                    return;
                }
            }
            getLogger().info("All players are sleeping");
            world.setTime(0);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldLoadEvent(WorldLoadEvent event) {
        enableTimeControlForWorld(event.getWorld());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onWorldUnloadEvent(WorldUnloadEvent event) {
        disableTimeControlForWorld(event.getWorld());
    }

    public void enableTimeControlForWorld(World world) {
        ConfigurationSection worldConfiguration = getConfig().getConfigurationSection("worlds." + world.getName());
        if (worldConfiguration == null) {
            worldConfiguration = getConfig().createSection("worlds." + world.getName());
            worldConfiguration.set("scale time", false);
            worldConfiguration.set("scale time by", 3);
            worldConfiguration.set("lock time", false);
            worldConfiguration.set("lock time to", 6000);
        }
        worldConfiguration.addDefault("scale time", false);
        worldConfiguration.addDefault("scale time by", 3);
        worldConfiguration.addDefault("lock time", false);
        worldConfiguration.addDefault("lock time to", 6000);

        if (worldConfiguration.getBoolean("scale time") && worldConfiguration.getBoolean("lock time")) {
            getLogger().severe(world.getName() + ": Scale time and lock time cannot be both set.");
        } else if (worldConfiguration.getBoolean("scale time")) {
            getLogger().info("Scaling time for " + world.getName());
            doDaylightCycleOriginalValue.put(world.getUID(), world.getGameRuleValue("doDaylightCycle"));
            world.setGameRuleValue("doDaylightCycle", "false");
            Integer taskID = getServer().getScheduler().scheduleSyncRepeatingTask(this, new AdvanceClockTask(world.getUID()), 0, worldConfiguration.getInt("scale time by"));
            taskIDs.put(world.getUID(), taskID);
        } else if (worldConfiguration.getBoolean("lock time")) {
            getLogger().info("Locking time for " + world.getName());
            doDaylightCycleOriginalValue.put(world.getUID(), world.getGameRuleValue("doDaylightCycle"));
            world.setGameRuleValue("doDaylightCycle", "false");
            world.setTime(worldConfiguration.getLong("lock time to"));
        }

        saveConfig();
    }

    public void disableTimeControlForWorld(World world) {
        Integer taskID = taskIDs.get(world.getUID());
        if (taskID != null) {
            getServer().getScheduler().cancelTask(taskID);
        }

        world.setGameRuleValue("doDaylightCycle", doDaylightCycleOriginalValue.get(world.getUID()));
    }
}
