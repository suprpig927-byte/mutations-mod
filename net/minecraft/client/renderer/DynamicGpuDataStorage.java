package net.minecraft.client.renderer;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import java.nio.ByteBuffer;
import java.util.List;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface DynamicGpuDataStorage<T extends DynamicGpuDataStorage.DynamicGpuData> extends AutoCloseable {
   void endFrame();

   GpuBufferSlice writeData(T var1);

   GpuBufferSlice[] writeData(T[] var1);

   GpuBufferSlice writeDataBatched(List<T> var1);

   GpuBufferSlice[] writeDataBatchedMultiple(List<List<T>> var1);

   @GpuBuffer.Usage
   int usage();

   @Override
   void close();

   public interface DynamicGpuData {
      void write(ByteBuffer var1);
   }
}
