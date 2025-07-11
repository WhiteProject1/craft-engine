package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.block.LeavesDecayEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class LeavesBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private static final Object LOG_TAG = BlockTags.getOrCreate(Key.of("minecraft", "logs"));
    private final int maxDistance;
    private final Property<Integer> distanceProperty;
    private final Property<Boolean> persistentProperty;

    public LeavesBlockBehavior(CustomBlock block,
                               int maxDistance,
                               Property<Integer> distanceProperty,
                               Property<Boolean> persistentProperty) {
        super(block);
        this.maxDistance = maxDistance;
        this.distanceProperty = distanceProperty;
        this.persistentProperty = persistentProperty;
    }

    public int getDistance(ImmutableBlockState state) {
        return state.get(this.distanceProperty);
    }

    public boolean isPersistent(ImmutableBlockState state) {
        return state.get(this.persistentProperty);
    }

    public boolean isWaterLogged(ImmutableBlockState state) {
        if (this.waterloggedProperty == null) return false;
        return state.get(this.waterloggedProperty);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object world;
        Object blockPos;
        Object neighborState;
        Object blockState = args[0];
        if (VersionHelper.isOrAbove1_21_2()) {
            world = args[1];
            neighborState = args[6];
            blockPos = args[3];
        } else {
            world = args[3];
            blockPos = args[4];
            neighborState = args[2];
        }
        ImmutableBlockState thisState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
        if (thisState != null) {
            Optional<LeavesBlockBehavior> optionalBehavior = thisState.behavior().getAs(LeavesBlockBehavior.class);
            if (optionalBehavior.isPresent()) {
                LeavesBlockBehavior behavior = optionalBehavior.get();
                int distance = behavior.getDistanceAt(neighborState) + 1;
                if (distance != 1 || behavior.getDistance(thisState) != distance) {
                    FastNMS.INSTANCE.method$LevelAccessor$scheduleBlockTick(world, blockPos, thisBlock, 1);
                }
            }
        }
        return blockState;
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        ImmutableBlockState currentState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
        if (currentState != null && !currentState.isEmpty()) {
            Optional<LeavesBlockBehavior> optionalBehavior = currentState.behavior().getAs(LeavesBlockBehavior.class);
            if (optionalBehavior.isPresent()) {
                LeavesBlockBehavior behavior = optionalBehavior.get();
                ImmutableBlockState newState = behavior.updateDistance(currentState, level, blockPos);
                if (newState != currentState) {
                    if (blockState == newState.customBlockState().handle()) {
                        CoreReflections.method$BlockStateBase$updateNeighbourShapes.invoke(blockState, level, blockPos, UpdateOption.UPDATE_ALL.flags(), 512);
                    } else {
                        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, newState.customBlockState().handle(), UpdateOption.UPDATE_ALL.flags());
                    }
                }
            }
        }
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object level = args[1];
        Object blockPos = args[2];
        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(args[0]));
        if (immutableBlockState != null) {
            Optional<LeavesBlockBehavior> optionalBehavior = immutableBlockState.behavior().getAs(LeavesBlockBehavior.class);
            if (optionalBehavior.isPresent()) {
                LeavesBlockBehavior behavior = optionalBehavior.get();
                if (behavior.isDecaying(immutableBlockState)) {
                    World bukkitWorld = FastNMS.INSTANCE.method$Level$getCraftWorld(level);
                    BlockPos pos = LocationUtils.fromBlockPos(blockPos);
                    // call bukkit event
                    LeavesDecayEvent event = new LeavesDecayEvent(bukkitWorld.getBlockAt(pos.x(), pos.y(), pos.z()));
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        return;
                    }
                    FastNMS.INSTANCE.method$Level$removeBlock(level, blockPos, false);
                    if (isWaterLogged(immutableBlockState)) {
                        bukkitWorld.setBlockData(pos.x(), pos.y(), pos.z(), Material.WATER.createBlockData());
                    }
                    net.momirealms.craftengine.core.world.World world = new BukkitWorld(bukkitWorld);
                    WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(pos));
                    ContextHolder.Builder builder = ContextHolder.builder()
                            .withParameter(DirectContextParameters.POSITION, position);
                    for (Item<Object> item : immutableBlockState.getDrops(builder, world, null)) {
                        world.dropItemNaturally(position, item);
                    }
                }
            }
        }
    }

    private boolean isDecaying(ImmutableBlockState blockState) {
        return !isPersistent(blockState) && getDistance(blockState) == this.maxDistance;
    }

    private ImmutableBlockState updateDistance(ImmutableBlockState state, Object world, Object blockPos) throws ReflectiveOperationException {
        int i = this.maxDistance;
        Object mutablePos = CoreReflections.constructor$MutableBlockPos.newInstance();
        int j = Direction.values().length;
        for (int k = 0; k < j; ++k) {
            Object direction = CoreReflections.instance$Directions[k];
            CoreReflections.method$MutableBlockPos$setWithOffset.invoke(mutablePos, blockPos, direction);
            Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, mutablePos);
            i = Math.min(i, getDistanceAt(blockState) + 1);
            if (i == 1) {
                break;
            }
        }
        return state.with(this.distanceProperty, i);
    }

    private int getDistanceAt(Object blockState) throws ReflectiveOperationException {
        boolean isLog = (boolean) CoreReflections.method$BlockStateBase$hasTag.invoke(blockState, LOG_TAG);
        if (isLog) return 0;
        int id = BlockStateUtils.blockStateToId(blockState);
        if (BlockStateUtils.isVanillaBlock(id)) {
            Object distanceProperty = CoreReflections.field$LeavesBlock$DISTANCE.get(null);
            boolean hasDistanceProperty = (boolean) CoreReflections.method$StateHolder$hasProperty.invoke(blockState, distanceProperty);
            if (!hasDistanceProperty) return this.maxDistance;
            return (int) CoreReflections.method$StateHolder$getValue.invoke(blockState, distanceProperty);
        } else {
            ImmutableBlockState anotherBlockState = BukkitBlockManager.instance().getImmutableBlockStateUnsafe(id);
            Optional<LeavesBlockBehavior> optionalAnotherBehavior = anotherBlockState.behavior().getAs(LeavesBlockBehavior.class);
            return optionalAnotherBehavior.map(leavesBlockBehavior -> leavesBlockBehavior.getDistance(anotherBlockState)).orElse(this.maxDistance);
        }
    }

    @SuppressWarnings("unchecked")
    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<Boolean> persistent = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("persistent"), "warning.config.block.behavior.leaves.missing_persistent");
            Property<Integer> distance = (Property<Integer>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("distance"), "warning.config.block.behavior.leaves.missing_distance");
            int actual = distance.possibleValues().get(distance.possibleValues().size() - 1);
            return new LeavesBlockBehavior(block, actual, distance, persistent);
        }
    }
}
