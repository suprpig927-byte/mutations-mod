package com.mutationsmod.mutations;

import com.mutationsmod.mutations.data.MutationData;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MutationsMod.MODID);

    /** Saved with the player, kept through death, and synced to clients that can see the player. */
    public static final Supplier<AttachmentType<MutationData>> DATA = ATTACHMENT_TYPES.register("data",
            () -> AttachmentType.builder(() -> MutationData.EMPTY)
                    .serialize(MutationData.CODEC)
                    .copyOnDeath()
                    .sync(MutationData.STREAM_CODEC)
                    .build());

    private ModAttachments() {}

    public static MutationData get(Player player) {
        return player.getData(DATA);
    }

    public static void set(Player player, MutationData data) {
        player.setData(DATA, data);
    }
}
