package net.minecraft.client.renderer.state;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.renderstate.BaseRenderState;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MapRenderState extends BaseRenderState {
   @Nullable
   public Identifier texture;
   public final List<MapRenderState.MapDecorationRenderState> decorations = new ArrayList<>();

   public static class MapDecorationRenderState extends BaseRenderState {
      public Holder<MapDecorationType> type;
      @Nullable
      public TextureAtlasSprite atlasSprite;
      public byte x;
      public byte y;
      public byte rot;
      public boolean renderOnFrame;
      @Nullable
      public Component name;
   }
}
