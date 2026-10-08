package net.fabricmc.loader.impl.lib.gson;

import java.io.IOException;

public final class MalformedJsonException extends IOException {
   public MalformedJsonException(String msg) {
      super(msg);
   }
}
