package net.fabricmc.loader.impl.lib.tinyremapper;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrClass;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrField;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrLocal;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMember;
import net.fabricmc.loader.impl.lib.tinyremapper.api.TrMethod;

public final class MemberInstance implements TrField, TrMethod {
   private static final AtomicReferenceFieldUpdater<MemberInstance, String> newNameUpdater = AtomicReferenceFieldUpdater.newUpdater(
      MemberInstance.class, String.class, "newName"
   );
   private static final AtomicReferenceFieldUpdater<MemberInstance, String> newBridgedNameUpdater = AtomicReferenceFieldUpdater.newUpdater(
      MemberInstance.class, String.class, "newBridgedName"
   );
   final TrMember.MemberType type;
   final ClassInstance cls;
   final String name;
   final String desc;
   final int access;
   final int index;
   TrLocal[] locals;
   private volatile String newName;
   private volatile String newBridgedName;
   String newNameOriginatingCls;
   MemberInstance bridgeTarget;

   MemberInstance(TrMember.MemberType type, ClassInstance cls, String name, String desc, int access, int index) {
      this.type = type;
      this.cls = cls;
      this.name = name;
      this.desc = desc;
      this.access = access;
      this.index = index;
   }

   @Override
   public TrMember.MemberType getType() {
      return this.type;
   }

   @Override
   public TrClass getOwner() {
      return this.cls;
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public String getDesc() {
      return this.desc;
   }

   @Override
   public int getAccess() {
      return this.access;
   }

   @Override
   public int getIndex() {
      return this.index;
   }

   @Override
   public TrLocal[] getLocals() {
      return (TrLocal[])this.locals.clone();
   }

   public TinyRemapper.MrjState getContext() {
      return this.cls.getContext();
   }

   public String getId() {
      return getId(this.type, this.name, this.desc, this.cls.tr.ignoreFieldDesc);
   }

   public String getNewName() {
      String ret = this.newBridgedName;
      return ret != null ? ret : this.newName;
   }

   public String getNewMappedName() {
      return this.newName;
   }

   public String getNewBridgedName() {
      return this.newBridgedName;
   }

   public boolean setNewName(String name, boolean fromBridge) {
      if (name == null) {
         throw new NullPointerException("null name");
      } else if (fromBridge) {
         boolean ret = newBridgedNameUpdater.compareAndSet(this, null, name);
         return ret || name.equals(this.newBridgedName);
      } else {
         boolean ret = newNameUpdater.compareAndSet(this, null, name);
         return ret || name.equals(this.newName);
      }
   }

   public void forceSetNewName(String name) {
      this.newName = name;
   }

   public void setLocals(TrLocal[] locals) {
      this.locals = (TrLocal[])locals.clone();
   }

   @Override
   public String toString() {
      return String.format("%s/%s%s", this.cls.getName(), this.name, this.desc);
   }

   public static String getId(TrMember.MemberType type, String name, String desc, boolean ignoreFieldDesc) {
      return type == TrMember.MemberType.METHOD ? getMethodId(name, desc) : getFieldId(name, desc, ignoreFieldDesc);
   }

   public static String getMethodId(String name, String desc) {
      return name.concat(desc);
   }

   public static String getFieldId(String name, String desc, boolean ignoreDesc) {
      return ignoreDesc ? name : name + ";;" + desc;
   }

   public static String getNameFromId(TrMember.MemberType type, String id, boolean ignoreFieldDesc) {
      if (ignoreFieldDesc && type == TrMember.MemberType.FIELD) {
         return id;
      } else {
         String separator = type == TrMember.MemberType.METHOD ? "(" : ";;";
         int pos = id.lastIndexOf(separator);
         if (pos < 0) {
            throw new IllegalArgumentException(String.format("invalid %s id: %s", type.name(), id));
         } else {
            return id.substring(0, pos);
         }
      }
   }
}
