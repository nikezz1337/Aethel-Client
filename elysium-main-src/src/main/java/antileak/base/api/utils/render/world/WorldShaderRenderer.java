package antileak.base.api.utils.render.world;

import antileak.base.api.QClient;

public class WorldShaderRenderer implements QClient {

    private static WorldShaderRenderer instance;

    public static WorldShaderRenderer getInstance() {
        if (instance == null) {
            instance = new WorldShaderRenderer();
        }
        return instance;
    }

    public void render() {
        // Legacy world-shader path is intentionally disabled.
    }
}
