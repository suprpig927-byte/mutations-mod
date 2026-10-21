package com.mutationsmod.mutations.network;

import com.mutationsmod.mutations.MutationsMod;
import com.mutationsmod.mutations.ability.AbilityHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModPayloads {
    private ModPayloads() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MutationsMod.MODID, path);
    }

    /** Client -> server: the player pressed "Ability N" (slot is 0-based). */
    public record UseAbility(int slot) implements CustomPacketPayload {
        public static final Type<UseAbility> TYPE = new Type<>(id("use_ability"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UseAbility> CODEC =
                ByteBufCodecs.VAR_INT.map(UseAbility::new, UseAbility::slot).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: Phaser speed tier up (+1) or down (-1). */
    public record ChangeSpeed(int delta) implements CustomPacketPayload {
        public static final Type<ChangeSpeed> TYPE = new Type<>(id("change_speed"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ChangeSpeed> CODEC =
                ByteBufCodecs.VAR_INT.map(ChangeSpeed::new, ChangeSpeed::delta).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server -> client: add velocity to the local player. Player movement is controlled by the
     * client, so the server has to ask the client to apply dashes and knockback to players.
     */
    public record Impulse(double x, double y, double z, boolean replace) implements CustomPacketPayload {
        public static final Type<Impulse> TYPE = new Type<>(id("impulse"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Impulse> CODEC = StreamCodec.of(
                (RegistryFriendlyByteBuf buf, Impulse p) -> {
                    buf.writeDouble(p.x);
                    buf.writeDouble(p.y);
                    buf.writeDouble(p.z);
                    buf.writeBoolean(p.replace);
                },
                buf -> new Impulse(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(UseAbility.TYPE, UseAbility.CODEC, (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) AbilityHandler.useSlot(sp, payload.slot());
        });

        registrar.playToServer(ChangeSpeed.TYPE, ChangeSpeed.CODEC, (payload, ctx) -> {
            if (ctx.player() instanceof ServerPlayer sp) AbilityHandler.changeSpeedTier(sp, payload.delta());
        });

        registrar.playToClient(Impulse.TYPE, Impulse.CODEC, (payload, ctx) -> {
            var player = ctx.player();
            Vec3 v = new Vec3(payload.x(), payload.y(), payload.z());
            player.setDeltaMovement(payload.replace() ? v : player.getDeltaMovement().add(v));
        });
    }
}
