package net.fabricmc.loader.impl.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;

public final class DefaultLanguageAdapter implements LanguageAdapter {
   public static final DefaultLanguageAdapter INSTANCE = new DefaultLanguageAdapter();

   private DefaultLanguageAdapter() {
   }

   @Override
   public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
      String[] methodSplit = value.split("::");
      if (methodSplit.length >= 3) {
         throw new LanguageAdapterException("Invalid handle format: " + value);
      }

      Class<?> c;
      try {
         c = Class.forName(methodSplit[0], true, FabricLauncherBase.getLauncher().getTargetClassLoader());
      } catch (ClassNotFoundException e) {
         throw new LanguageAdapterException(e);
      }

      if (methodSplit.length == 1) {
         if (type.isAssignableFrom(c)) {
            try {
               return (T)c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
               throw new LanguageAdapterException(e);
            }
         } else {
            throw new LanguageAdapterException("Class " + c.getName() + " cannot be cast to " + type.getName() + "!");
         }
      } else {
         List<Executable> executableList = new ArrayList<>();

         for (Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(methodSplit[1])) {
               executableList.add(m);
            }
         }

         if (methodSplit[1].equals("<init>")) {
            executableList.addAll(Arrays.asList(c.getDeclaredConstructors()));
         }

         try {
            Field field = c.getDeclaredField(methodSplit[1]);
            Class<?> fType = field.getType();
            if ((field.getModifiers() & 8) == 0) {
               throw new LanguageAdapterException("Field " + value + " must be static!");
            } else if (!executableList.isEmpty()) {
               throw new LanguageAdapterException("Ambiguous " + value + " - refers to both field and method!");
            } else if (!type.isAssignableFrom(fType)) {
               throw new LanguageAdapterException("Field " + value + " cannot be cast to " + type.getName() + "!");
            } else {
               return (T)field.get(null);
            }
         } catch (NoSuchFieldException var16) {
            if (!type.isInterface()) {
               throw new LanguageAdapterException("Cannot proxy method " + value + " to non-interface type " + type.getName() + "!");
            }

            if (executableList.isEmpty()) {
               throw new LanguageAdapterException("Could not find " + value + "!");
            }

            if (executableList.size() >= 2) {
               throw new LanguageAdapterException("Found multiple method entries of name " + value + "!");
            }

            Executable targetExecutable = executableList.get(0);
            Object object = null;
            if (targetExecutable instanceof Method) {
               Method targetMethod = (Method)targetExecutable;
               if ((targetMethod.getModifiers() & 8) == 0) {
                  try {
                     object = c.getDeclaredConstructor().newInstance();
                  } catch (Exception e) {
                     throw new LanguageAdapterException(e);
                  }
               }
            }

            MethodHandle handle;
            try {
               if (targetExecutable instanceof Method) {
                  Method targetMethod = (Method)targetExecutable;
                  handle = MethodHandles.lookup().unreflect(targetMethod);
               } else {
                  Constructor<?> targetConstructor = (Constructor<?>)targetExecutable;
                  handle = MethodHandles.lookup().unreflectConstructor(targetConstructor);
               }
            } catch (Exception ex) {
               throw new LanguageAdapterException(ex);
            }

            if (object != null) {
               handle = handle.bindTo(object);
            }

            try {
               return MethodHandleProxies.asInterfaceInstance(type, handle);
            } catch (Exception ex) {
               throw new LanguageAdapterException(ex);
            }
         } catch (IllegalAccessException e) {
            throw new LanguageAdapterException("Field " + value + " cannot be accessed!", e);
         }
      }
   }
}
