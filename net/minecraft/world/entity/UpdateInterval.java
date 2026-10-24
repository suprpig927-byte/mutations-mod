package net.minecraft.world.entity;

public interface UpdateInterval {
   UpdateInterval NEVER = new UpdateInterval() {
      @Override
      public boolean test(int tick) {
         return false;
      }

      @Override
      public int nextInterval(int tick) {
         return 0;
      }
   };

   boolean test(int var1);

   int nextInterval(int var1);

   static UpdateInterval periodic(final int period) {
      return new UpdateInterval() {
         @Override
         public boolean test(int tick) {
            return tick % period == 0;
         }

         @Override
         public int nextInterval(int tick) {
            return tick / period * period + period;
         }
      };
   }
}
