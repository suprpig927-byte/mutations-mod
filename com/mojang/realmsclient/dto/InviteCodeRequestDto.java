package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record InviteCodeRequestDto(
   @Nullable @SerializedName("linkId") String code,
   @SerializedName("realmId") long realmId,
   @SerializedName("enabled") boolean enabled,
   @Nullable @SerializedName("expirationDate") Long expirationDate
) implements ReflectionBasedSerialization {
}
