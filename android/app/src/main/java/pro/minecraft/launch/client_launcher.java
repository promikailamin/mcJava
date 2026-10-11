package pro.minecraft.launch;

import pro.minecraft.main_activity;
import pro.minecraft.host.game_renderer;

public class client_launcher {
    private final main_activity activity;
    private final game_renderer renderer;
    
    public client_launcher(main_activity activity, game_renderer renderer) {
        this.activity = activity;
        this.renderer = renderer;
    }
    
    public void start() {}
    public void shutdown() {}
}