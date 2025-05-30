package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;
import java.util.Optional;

public class HitBoxTypes {
    public static final Key INTERACTION = Key.of("minecraft:interaction");
    public static final Key SHULKER = Key.of("minecraft:shulker");
    public static final Key HAPPY_GHAST = Key.of("minecraft:happy_ghast");
    public static final Key CUSTOM = Key.of("minecraft:custom");

    public static void register(Key key, HitBoxFactory factory) {
        Holder.Reference<HitBoxFactory> holder = ((WritableRegistry<HitBoxFactory>) BuiltInRegistries.HITBOX_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.HITBOX_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static HitBox fromMap(Map<String, Object> arguments) {
        Key type = Optional.ofNullable((String) arguments.get("type")).map(Key::of).orElse(HitBoxTypes.INTERACTION);
        HitBoxFactory factory = BuiltInRegistries.HITBOX_FACTORY.getValue(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown hitbox type: " + type);
        }
        return factory.create(arguments);
    }
}
