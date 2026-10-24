package com.mojang.realmsclient.dto;

import com.google.gson.annotations.SerializedName;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record RealmsDescriptionDto(@Nullable @SerializedName("name") String name, @SerializedName("description") String description)
   implements ReflectionBasedSerialization {
}
