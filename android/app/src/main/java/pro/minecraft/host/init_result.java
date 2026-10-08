package pro.minecraft.host;

/**
 * Output of native {@code game_renderer} context creation, handed to the launcher.
 */
public record init_result(
        int width,
        int height,
        String version,
        String vendor,
        String renderer) {
}