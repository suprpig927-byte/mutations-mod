package net.minecraft.server.network;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ClientInformation;
import net.neoforged.neoforge.network.connection.ConnectionType;

public record CommonListenerCookie(GameProfile gameProfile, int latency, ClientInformation clientInformation, boolean transferred, ConnectionType connectionType) {
   @Deprecated
   public CommonListenerCookie(GameProfile gameProfile, int latency, ClientInformation clientInformation, boolean transferred) {
      this(gameProfile, latency, clientInformation, transferred, ConnectionType.OTHER);
   }

   public static CommonListenerCookie createInitial(GameProfile gameProfile, boolean transferred) {
      return new CommonListenerCookie(gameProfile, 0, ClientInformation.createDefault(), transferred);
   }
}
