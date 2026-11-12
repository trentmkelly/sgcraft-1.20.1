package gcewing.sg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import gcewing.sg.block.entity.SGBaseBlockEntity;
import gcewing.sg.util.GateTransform;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;

/**
 * Moves entities between stargates. The original rebuilt entities by hand and sent its own
 * respawn packets; on 1.20.1 this goes through ServerPlayer.teleportTo and
 * Entity.changeDimension with a custom ITeleporter instead.
 */
public final class SGTeleport {

    private SGTeleport() {
    }

    public static Entity teleportEntityAndRiders(Entity entity, Vec3 motion, GateTransform t1, GateTransform t2,
                                                 ServerLevel destLevel, boolean destBlocked) {
        List<Entity> riders = new ArrayList<>(entity.getPassengers());
        for (int i = 0; i < riders.size(); i++) {
            Entity rider = riders.get(i);
            Vec3 riderMotion = rider.getDeltaMovement();
            rider.stopRiding();
            riders.set(i, teleportEntityAndRiders(rider, riderMotion, t1, t2, destLevel, destBlocked));
        }
        unleashEntity(entity);
        Entity newEntity = teleportEntity(entity, motion, t1, t2, destLevel, destBlocked);
        if (newEntity != null && newEntity.isAlive()) {
            for (Entity rider : riders)
                if (rider != null && rider.isAlive())
                    rider.startRiding(newEntity, true);
        }
        return newEntity;
    }

    /** Break leashes to or from the entity; teleporting would otherwise drop an extra lead. */
    static void unleashEntity(Entity entity) {
        if (entity instanceof Mob mob && mob.isLeashed())
            mob.dropLeash(true, false);
        AABB box = entity.getBoundingBox().inflate(7.0);
        for (Mob other : entity.level().getEntitiesOfClass(Mob.class, box))
            if (other.isLeashed() && other.getLeashHolder() == entity)
                other.dropLeash(true, false);
    }

    static Entity teleportEntity(Entity entity, Vec3 motion, GateTransform t1, GateTransform t2,
                                 ServerLevel destLevel, boolean destBlocked) {
        Vec3 p = t1.ip(entity.position());            // local position
        Vec3 v = t1.iv(motion);                       // local velocity
        Vec3 r = t1.iv(yawVector(entity));            // local facing
        Vec3 q = t2.p(-p.x, p.y, -p.z);               // new global position
        Vec3 u = t2.v(-v.x, v.y, -v.z);               // new global velocity
        Vec3 s = t2.v(r.scale(-1));                   // new global facing
        float a = (float) yawAngle(s, entity);        // new global yaw angle
        if (destBlocked) {
            terminateEntityByIrisImpact(entity);
            playIrisHitSound(destLevel, q, entity);
            return null;
        }
        if (entity instanceof ServerPlayer player) {
            if (player.level() != destLevel)
                q = q.add(yawVector(a)); // as in the original, step clear of the gate after changing dimension
            // Like the original, players keep their position and facing but not their velocity.
            player.teleportTo(destLevel, q.x, q.y, q.z, a, player.getXRot());
            return player;
        }
        if (entity.level() == destLevel) {
            entity.moveTo(q.x, q.y, q.z, a, entity.getXRot());
            entity.setYHeadRot(a);
            entity.setDeltaMovement(u);
            entity.hurtMarked = true;
            return entity;
        }
        final Vec3 dest = q;
        return entity.changeDimension(destLevel, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(Entity e, ServerLevel destWorld, Function<ServerLevel, PortalInfo> defaultPortalInfo) {
                return new PortalInfo(dest, u, a, e.getXRot());
            }

            @Override
            public Entity placeEntity(Entity e, ServerLevel currentWorld, ServerLevel destWorld, float yaw,
                                      Function<Boolean, Entity> repositionEntity) {
                Entity moved = repositionEntity.apply(false);
                if (moved != null) {
                    moved.setDeltaMovement(u);
                    moved.setYHeadRot(a);
                }
                return moved;
            }

            @Override
            public boolean playTeleportSound(ServerPlayer player, ServerLevel sourceWorld, ServerLevel destWorld) {
                return false;
            }
        });
    }

    static void terminateEntityByIrisImpact(Entity entity) {
        if (entity instanceof Player player)
            terminatePlayerByIrisImpact(player);
        else
            entity.discard();
    }

    static void terminatePlayerByIrisImpact(Player player) {
        if (player.getAbilities().instabuild)
            SGBaseBlockEntity.sendChatMessage(player, Component.literal("Destination blocked by iris"));
        else {
            if (!(SGConfig.preserveInventory.get() || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)))
                player.getInventory().clearContent();
            player.hurt(SGDamage.iris(player.level()), SGBaseBlockEntity.irisDamageAmount);
        }
    }

    static void playIrisHitSound(ServerLevel world, Vec3 pos, Entity entity) {
        double volume = Math.min(entity.getBbWidth() * entity.getBbHeight(), 1.0);
        double pitch = 2.0 - volume;
        world.playSound(null, pos.x, pos.y, pos.z, ModRegistry.SOUND_IRIS_HIT.get(), SoundSource.NEUTRAL,
            (float) volume, (float) pitch);
    }

    /** Projectiles store their yaw with the opposite sign to living entities. */
    static int yawSign(Entity entity) {
        return entity instanceof Projectile ? -1 : 1;
    }

    static Vec3 yawVector(Entity entity) {
        return yawVector(yawSign(entity) * entity.getYRot());
    }

    static Vec3 yawVector(double yaw) {
        double a = Math.toRadians(yaw);
        return new Vec3(-Math.sin(a), 0, Math.cos(a));
    }

    static double yawAngle(Vec3 v, Entity entity) {
        double a = Math.atan2(-v.x, v.z);
        return yawSign(entity) * Math.toDegrees(a);
    }
}
