package net.minecraft.server.dialog;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.dialog.body.DialogBody;

public record CommonDialogData(
   Component title,
   Optional<Component> externalTitle,
   boolean canCloseWithEscape,
   boolean pause,
   DialogAction afterAction,
   List<DialogBody> body,
   List<Input> inputs
) {
   public static final MapCodec<CommonDialogData> MAP_CODEC = RecordCodecBuilder.mapCodec(
         i -> i.group(
               ComponentSerialization.CODEC.fieldOf("title").forGetter((CommonDialogData d) -> d.title()),
               ComponentSerialization.CODEC.optionalFieldOf("external_title").forGetter((CommonDialogData d) -> d.externalTitle()),
               Codec.BOOL.optionalFieldOf("can_close_with_escape", true).forGetter((CommonDialogData d) -> d.canCloseWithEscape()),
               Codec.BOOL.optionalFieldOf("pause", true).forGetter((CommonDialogData d) -> d.pause()),
               DialogAction.CODEC.optionalFieldOf("after_action", DialogAction.CLOSE).forGetter((CommonDialogData d) -> d.afterAction()),
               DialogBody.COMPACT_LIST_CODEC.optionalFieldOf("body", List.of()).forGetter((CommonDialogData d) -> d.body()),
               Input.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter((CommonDialogData d) -> d.inputs())
            )
            .apply(i, CommonDialogData::new)
      )
      .validate(
         data -> data.pause && !data.afterAction.willUnpause()
            ? DataResult.error(() -> "Dialogs that pause the game must use after_action values that unpause it after user action!")
            : DataResult.success(data)
      );

   public Component computeExternalTitle() {
      return this.externalTitle.orElse(this.title);
   }
}
