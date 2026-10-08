package net.fabricmc.loader.impl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Arguments {
   public static final String GAME_VERSION = "fabric.gameVersion";
   public static final String ADD_MODS = "fabric.addMods";
   private final Map<String, String> values = new LinkedHashMap<>();
   private final List<String> extraArgs = new ArrayList<>();

   public Collection<String> keys() {
      return this.values.keySet();
   }

   public List<String> getExtraArgs() {
      return Collections.unmodifiableList(this.extraArgs);
   }

   public boolean containsKey(String key) {
      return this.values.containsKey(key);
   }

   public String get(String key) {
      return this.values.get(key);
   }

   public String getOrDefault(String key, String value) {
      return this.values.getOrDefault(key, value);
   }

   public void put(String key, String value) {
      this.values.put(key, value);
   }

   public void addExtraArg(String value) {
      this.extraArgs.add(value);
   }

   public void parse(String[] args) {
      this.parse(Arrays.asList(args));
   }

   public void parse(List<String> args) {
      for (int i = 0; i < args.size(); i++) {
         String arg = args.get(i);
         if (arg.startsWith("--") && i < args.size() - 1) {
            String value = args.get(i + 1);
            if (value.startsWith("--")) {
               value = "";
            } else {
               i++;
            }

            this.values.put(arg.substring(2), value);
         } else {
            this.extraArgs.add(arg);
         }
      }
   }

   public String[] toArray() {
      String[] newArgs = new String[this.values.size() * 2 + this.extraArgs.size()];
      int i = 0;

      for (String s : this.values.keySet()) {
         newArgs[i++] = "--" + s;
         newArgs[i++] = this.values.get(s);
      }

      for (String s : this.extraArgs) {
         newArgs[i++] = s;
      }

      return newArgs;
   }

   public String remove(String s) {
      return this.values.remove(s);
   }
}
