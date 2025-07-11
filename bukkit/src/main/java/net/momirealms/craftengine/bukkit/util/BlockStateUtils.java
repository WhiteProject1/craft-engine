package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.ReflectionInitException;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBuiltInRegistries;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.NetworkReflections;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Instrument;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MapColor;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BlockStateUtils {
    public static final IdentityHashMap<Object, Object> CLIENT_SIDE_NOTE_BLOCKS = new IdentityHashMap<>();
    private static int vanillaStateSize;
    private static boolean hasInit;
    public static Map<Object, Integer> IGNITE_ODDS;

    @SuppressWarnings("unchecked")
    public static void init(int size) {
        if (hasInit) {
            throw new IllegalStateException("BlockStateUtils has already been initialized");
        }
        vanillaStateSize = size;
        try {
            IGNITE_ODDS = (Map<Object, Integer>) CoreReflections.field$FireBlock$igniteOdds.get(MBlocks.FIRE);
        } catch (ReflectiveOperationException e) {
            throw new ReflectionInitException("Failed to initialize instance$FireBlock$igniteOdds", e);
        }
        hasInit = true;
    }

    public static BlockStateWrapper toPackedBlockState(BlockData blockData) {
        Object state = blockDataToBlockState(blockData);
        int id = blockStateToId(state);
        return BlockStateWrapper.create(state, id, isVanillaBlock(id));
    }

    public static boolean isCorrectTool(@NotNull ImmutableBlockState state, @Nullable Item<ItemStack> itemInHand) {
        BlockSettings settings = state.settings();
        if (settings.requireCorrectTool()) {
            if (itemInHand == null) return false;
            if (!settings.isCorrectTool(itemInHand.id()) &&
                    (!settings.respectToolComponent() || !FastNMS.INSTANCE.method$ItemStack$isCorrectToolForDrops(itemInHand.getLiteralObject(), state.customBlockState().handle()))) {
                return false;
            }
        }
        return true;
    }

    public static List<Object> getAllBlockStates(String blockState) {
        int index = blockState.indexOf('[');
        if (index == -1) {
            return getAllBlockStates(Key.of(blockState));
        } else {
            String blockTypeString = blockState.substring(0, index);
            Key block = Key.of(blockTypeString);
            Optional<CustomBlock> optionalCustomBlock = BukkitBlockManager.instance().blockById(block);
            if (optionalCustomBlock.isPresent()) {
                ImmutableBlockState state = BlockStateParser.deserialize(blockState);
                if (state == null) {
                    return List.of();
                } else {
                    return List.of(state.customBlockState().handle());
                }
            } else {
                BlockData blockData = Bukkit.createBlockData(blockState);
                return List.of(blockDataToBlockState(blockData));
            }
        }
    }

    public static List<Object> getAllBlockStates(Key block) {
        Optional<CustomBlock> optionalCustomBlock = BukkitBlockManager.instance().blockById(block);
        return optionalCustomBlock.map(customBlock -> customBlock.variantProvider().states().stream().map(it -> it.customBlockState().handle()).toList())
                .orElseGet(() -> getAllVanillaBlockStates(block));
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getAllVanillaBlockStates(Key block) {
        try {
            Object blockIns = CoreReflections.method$Registry$get.invoke(MBuiltInRegistries.BLOCK, KeyUtils.toResourceLocation(block));
            Object definition = CoreReflections.field$Block$StateDefinition.get(blockIns);
            return (List<Object>) CoreReflections.field$StateDefinition$states.get(definition);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get all block states for " + block, e);
        }
    }

    public static Object createBlockUpdatePacket(BlockPos pos, ImmutableBlockState state) {
        try {
            return NetworkReflections.constructor$ClientboundBlockUpdatePacket.newInstance(LocationUtils.toBlockPos(pos), state.customBlockState().handle());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static BlockData fromBlockData(Object blockState) {
        return FastNMS.INSTANCE.method$CraftBlockData$fromData(blockState);
    }

    public static int blockDataToId(BlockData blockData) {
        return blockStateToId(blockDataToBlockState(blockData));
    }

    public static Key getBlockOwnerId(Block block) {
        return getBlockOwnerIdFromData(block.getBlockData());
    }

    public static Key getBlockOwnerIdFromData(BlockData block) {
        Object blockState = blockDataToBlockState(block);
        return getBlockOwnerIdFromState(blockState);
    }

    public static Key getBlockOwnerIdFromState(Object blockState) {
        return getBlockOwnerIdFromString(blockState.toString());
    }

    public static Key getBlockOwnerIdFromString(String id) {
        int first = id.indexOf('{');
        int last = id.indexOf('}');
        if (first != -1 && last != -1 && last > first) {
            String blockId = id.substring(first + 1, last);
            return Key.of(blockId);
        } else {
            throw new IllegalArgumentException("Invalid block ID format: " + id);
        }
    }

    public static Object blockDataToBlockState(BlockData blockData) {
        return FastNMS.INSTANCE.method$CraftBlockData$getState(blockData);
    }

    public static Object idToBlockState(int id) {
        return FastNMS.INSTANCE.method$IdMapper$byId(CoreReflections.instance$Block$BLOCK_STATE_REGISTRY, id);
    }

    public static int blockStateToId(Object blockState) {
        return FastNMS.INSTANCE.method$IdMapper$getId(CoreReflections.instance$Block$BLOCK_STATE_REGISTRY, blockState);
    }

    public static Object getBlockOwner(Object blockState) {
        return FastNMS.INSTANCE.method$BlockState$getBlock(blockState);
    }

    public static int physicsEventToId(BlockPhysicsEvent event) throws ReflectiveOperationException {
        Object blockData = CraftBukkitReflections.field$BlockPhysicsEvent$changed.get(event);
        Object blockState = CraftBukkitReflections.field$CraftBlockData$data.get(blockData);
        return FastNMS.INSTANCE.method$IdMapper$getId(CoreReflections.instance$Block$BLOCK_STATE_REGISTRY, blockState);
    }

    public static Object physicsEventToState(BlockPhysicsEvent event) throws ReflectiveOperationException {
        Object blockData = CraftBukkitReflections.field$BlockPhysicsEvent$changed.get(event);
        return CraftBukkitReflections.field$CraftBlockData$data.get(blockData);
    }

    public static void setLightEmission(Object state, int emission) throws ReflectiveOperationException {
        CoreReflections.field$BlockStateBase$lightEmission.set(state, emission);
    }

    public static int getLightEmission(Object state) {
        return FastNMS.INSTANCE.method$BlockStateBase$getLightEmission(state);
    }

    public static void setMapColor(Object state, MapColor color) throws ReflectiveOperationException {
        Object mcMapColor = CoreReflections.method$MapColor$byId.invoke(null, color.id);
        CoreReflections.field$BlockStateBase$mapColor.set(state, mcMapColor);
    }

    public static void setInstrument(Object state, Instrument instrument) throws ReflectiveOperationException {
        Object mcInstrument = ((Object[]) CoreReflections.method$NoteBlockInstrument$values.invoke(null))[instrument.ordinal()];
        CoreReflections.field$BlockStateBase$instrument.set(state, mcInstrument);
    }

    public static void setHardness(Object state, float hardness) throws ReflectiveOperationException {
        CoreReflections.field$BlockStateBase$hardness.set(state, hardness);
    }

    public static void setBurnable(Object state, boolean burnable) throws ReflectiveOperationException {
        CoreReflections.field$BlockStateBase$burnable.set(state, burnable);
    }

    public static void setUseShapeForLightOcclusion(Object state, boolean useShapeForLightOcclusion) throws ReflectiveOperationException {
        CoreReflections.field$BlockStateBase$useShapeForLightOcclusion.set(state, useShapeForLightOcclusion);
    }

    public static void setPushReaction(Object state, PushReaction reaction) throws ReflectiveOperationException {
        Object pushReaction = ((Object[])  CoreReflections.method$PushReaction$values.invoke(null))[reaction.ordinal()];
        CoreReflections.field$BlockStateBase$pushReaction.set(state, pushReaction);
    }

    public static void setIsRandomlyTicking(Object state, boolean randomlyTicking) throws ReflectiveOperationException {
        CoreReflections.field$BlockStateBase$isRandomlyTicking.set(state, randomlyTicking);
    }

    public static void setReplaceable(Object state, boolean replaceable) throws ReflectiveOperationException {
        CoreReflections.field$BlockStateBase$replaceable.set(state, replaceable);
    }

    public static boolean isReplaceable(Object state) {
        try {
            return (boolean) CoreReflections.field$BlockStateBase$replaceable.get(state);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to get replaceable property", e);
        }
    }

    public static void setCanOcclude(Object state, boolean canOcclude) throws ReflectiveOperationException {
        CoreReflections.field$BlockStateBase$canOcclude.set(state, canOcclude);
    }

    public static boolean isOcclude(Object state) {
        return FastNMS.INSTANCE.method$BlockStateBase$canOcclude(state);
    }

    public static void setIsRedstoneConductor(Object state, Object predicate) throws ReflectiveOperationException {
        CoreReflections.field$BlockStateBase$isRedstoneConductor.set(state, predicate);
    }

    public static void setIsSuffocating(Object state, Object predicate) throws ReflectiveOperationException {
        CoreReflections.field$BlockStateBase$isSuffocating.set(state, predicate);
    }

    public static void setIsViewBlocking(Object state, Object predicate) throws ReflectiveOperationException {
        CoreReflections.field$BlockStateBase$isViewBlocking.set(state, predicate);
    }

    public static boolean isClientSideNoteBlock(Object state) {
        return CLIENT_SIDE_NOTE_BLOCKS.containsKey(state);
    }

    public static boolean isVanillaBlock(Object state) {
        int id = blockStateToId(state);
        return id >= 0 && id < vanillaStateSize;
    }

    public static boolean isVanillaBlock(int id) {
        return id >= 0 && id < vanillaStateSize;
    }

    public static int vanillaStateSize() {
        return vanillaStateSize;
    }

    public static boolean isBurnable(Object state) {
        Object blockOwner = getBlockOwner(state);
        return IGNITE_ODDS.getOrDefault(blockOwner, 0) > 0;
    }
}
