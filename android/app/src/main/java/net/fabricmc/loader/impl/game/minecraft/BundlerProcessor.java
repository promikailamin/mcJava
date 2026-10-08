package net.fabricmc.loader.impl.game.minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.impl.game.LibClassifier;
import net.fabricmc.loader.impl.util.LoaderUtil;

final class BundlerProcessor {
   private static final String MAIN_CLASS_PROPERTY = "bundlerMainClass";

   static void process(LibClassifier<McLibrary> classifier) throws IOException {
      Path bundlerOrigin = classifier.getOrigin(McLibrary.MC_BUNDLER);
      String prevProperty = null;
      ClassLoader prevCl = null;
      boolean restorePrev = false;

      URL[] urls;
      label100: {
         try {
            URLClassLoader bundlerCl = new URLClassLoader(new URL[]{bundlerOrigin.toUri().toURL()}, MinecraftGameProvider.class.getClassLoader()) {
               @Override
               protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                  synchronized (this.getClassLoadingLock(name)) {
                     Class<?> c = this.findLoadedClass(name);
                     if (c == null) {
                        if (name.startsWith("net.minecraft.")) {
                           URL url = this.getResource(LoaderUtil.getClassFileName(name));
                           if (url != null) {
                              try {
                                 InputStream is = url.openConnection().getInputStream();

                                 try {
                                    byte[] data = new byte[Math.max(is.available() + 1, 1000)];
                                    int offset = 0;

                                    int len;
                                    while ((len = is.read(data, offset, data.length - offset)) >= 0) {
                                       offset += len;
                                       if (offset == data.length) {
                                          data = Arrays.copyOf(data, data.length * 2);
                                       }
                                    }

                                    c = this.defineClass(name, data, 0, offset);
                                 } catch (Throwable var12) {
                                    if (is != null) {
                                       try {
                                          is.close();
                                       } catch (Throwable var11) {
                                          var12.addSuppressed(var11);
                                       }
                                    }

                                    throw var12;
                                 }

                                 if (is != null) {
                                    is.close();
                                 }
                              } catch (IOException e) {
                                 throw new RuntimeException(e);
                              }
                           }
                        }

                        if (c == null) {
                           c = this.getParent().loadClass(name);
                        }
                     }

                     if (resolve) {
                        this.resolveClass(c);
                     }

                     return c;
                  }
               }
            };

            try {
               Class<?> cls = Class.forName(classifier.getClassName(McLibrary.MC_BUNDLER), true, bundlerCl);
               Method method = cls.getMethod("main", String[].class);
               prevProperty = System.getProperty("bundlerMainClass");
               prevCl = Thread.currentThread().getContextClassLoader();
               restorePrev = true;
               System.setProperty("bundlerMainClass", BundlerClassPathCapture.class.getName());
               Thread.currentThread().setContextClassLoader(bundlerCl);
               method.invoke(null, new String[0]);
               urls = BundlerClassPathCapture.FUTURE.get(10L, TimeUnit.SECONDS);
            } catch (Throwable var16) {
               try {
                  bundlerCl.close();
               } catch (Throwable var15) {
                  var16.addSuppressed(var15);
               }

               throw var16;
            }

            bundlerCl.close();
            break label100;
         } catch (ClassNotFoundException e) {
         } catch (Throwable t) {
            throw new RuntimeException("Error invoking MC server bundler: " + t, t);
         } finally {
            if (restorePrev) {
               Thread.currentThread().setContextClassLoader(prevCl);
               if (prevProperty != null) {
                  System.setProperty("bundlerMainClass", prevProperty);
               } else {
                  System.clearProperty("bundlerMainClass");
               }
            }
         }

         return;
      }

      classifier.remove(bundlerOrigin);

      for (URL url : urls) {
         classifier.process(url);
      }
   }
}
