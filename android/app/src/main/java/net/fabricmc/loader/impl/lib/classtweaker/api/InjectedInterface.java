package net.fabricmc.loader.impl.lib.classtweaker.api;

import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public interface InjectedInterface {
   String getInterfaceName();

   String getInterfaceSignature();

   boolean hasGenerics();
}
