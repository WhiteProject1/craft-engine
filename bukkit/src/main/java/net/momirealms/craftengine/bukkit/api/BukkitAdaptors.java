package net.momirealms.craftengine.bukkit.api;

import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.bukkit.world.BukkitWorldBlock;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class BukkitAdaptors {

    private BukkitAdaptors() {}

    public static BukkitServerPlayer adapt(final Player player) {
        return BukkitCraftEngine.instance().adapt(player);
    }

    public static BukkitWorld adapt(final World world) {
        return new BukkitWorld(world);
    }

    public static BukkitEntity adapt(final Entity entity) {
        return new BukkitEntity(entity);
    }

    public static BukkitWorldBlock adapt(final Block block) {
        return new BukkitWorldBlock(block);
    }
}
