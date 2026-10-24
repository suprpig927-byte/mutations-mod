package net.minecraft.server.jsonrpc.api;

import java.net.URI;
import java.util.List;
import net.minecraft.resources.Identifier;

public record SchemaComponent<T>(String name, URI ref, Schema<T> schema) {
   public SchemaComponent(Identifier name, Schema<T> schema) {
      this(name.toString(), ReferenceUtil.createLocalReference(name.toString()), schema);
   }

   public Schema<T> asRef() {
      return Schema.ofRef(this.ref, this.schema.codec());
   }

   public Schema<List<T>> asArray() {
      return Schema.arrayOf(this.asRef(), this.schema.codec());
   }
}
