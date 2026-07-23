package antileak.base.client.modules.impl.render.base;

import lombok.RequiredArgsConstructor;
import antileak.base.api.QClient;
import antileak.base.api.events.implement.EventRender;
import antileak.base.api.events.implement.EventUpdate;
import antileak.base.api.utils.draggable.Draggable;

@RequiredArgsConstructor
public class InterfaceProcessing implements QClient {

    public final Draggable draggable;
    private boolean unusualRectType = true;

    public boolean isUnusualRectType() {
        return unusualRectType;
    }

    public void setUnusualRectType(boolean unusualRectType) {
        this.unusualRectType = unusualRectType;
    }

    public void onUpdate(EventUpdate eventUpdate) {
    }

    public void onRender(EventRender.Default eventRender) {

    }
}
