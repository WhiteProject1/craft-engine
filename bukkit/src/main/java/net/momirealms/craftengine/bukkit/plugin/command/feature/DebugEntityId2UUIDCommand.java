package net.momirealms.craftengine.bukkit.plugin.command.feature;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.bukkit.parser.WorldParser;
import org.incendo.cloud.parser.standard.IntegerParser;

public class DebugEntityId2UUIDCommand extends BukkitCommandFeature<CommandSender> {

    public DebugEntityId2UUIDCommand(CraftEngineCommandManager<CommandSender> commandManager, CraftEngine plugin) {
        super(commandManager, plugin);
    }

    @Override
    public Command.Builder<? extends CommandSender> assembleCommand(CommandManager<CommandSender> manager, Command.Builder<CommandSender> builder) {
        return builder
                .required("world", WorldParser.worldParser())
                .required("entityId", IntegerParser.integerParser())
                .handler(context -> {
                    World world = context.get("world");
                    int entityId = context.get("entityId");
                    Entity entity = FastNMS.INSTANCE.getBukkitEntityById(world, entityId);
                    if (entity == null) {
                        context.sender().sendMessage("entity not found");
                        return;
                    }
                    Location location = entity.getLocation();
                    context.sender().sendMessage(
                            String.format(
                                    """
                                    ===========================
                                    uuid: %s
                                    name: %s
                                    location: %s,%s,%s
                                    type: %s
                                    ===========================
                                    """,
                                    entity.getUniqueId(),
                                    entity.getName(),
                                    location.x(), location.y(), location.z(),
                                    entity.getType()
                            )
                    );
                });
    }

    @Override
    public String getFeatureID() {
        return "debug_entity_id_to_uuid";
    }
}
