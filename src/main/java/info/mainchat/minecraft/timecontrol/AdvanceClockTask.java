/*
 * ©2013 Omar Stefan Evans <omar@evansbros.info>
 *
 * Permission is hereby granted to all persons to use and redistribute this software
 * for any purpose, with or without modification, provided that this notice appears
 * in all copies or substantial portions of the software.
 */

package info.mainchat.minecraft.timecontrol;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public final class AdvanceClockTask extends BukkitRunnable {

    private final UUID worldUUID;

    AdvanceClockTask(UUID worldUUID) {
        this.worldUUID = worldUUID;
    }

    @Override
    public void run() {
        World world = Bukkit.getServer().getWorld(worldUUID);
        world.setTime((1 + world.getTime()) % 24000);
    }
}
