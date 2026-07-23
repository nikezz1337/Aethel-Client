package antileak.base.api.events.implement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;
import antileak.base.api.events.Event;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class EventMove extends Event {
   private Vec3d movePos;
}