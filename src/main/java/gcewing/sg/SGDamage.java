package gcewing.sg;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.Level;

/** Damage types are data-driven since 1.19.4; see data/sgcraft/damage_type. */
public final class SGDamage {

    public static final ResourceKey<DamageType> TRANSIENT = key("transient");
    public static final ResourceKey<DamageType> IRIS = key("iris");

    private SGDamage() {
    }

    static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(SGCraft.MODID, name));
    }

    public static DamageSource transientDamage(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TRANSIENT));
    }

    public static DamageSource iris(Level level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(IRIS));
    }
}
