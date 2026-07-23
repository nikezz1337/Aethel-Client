package antileak.base.api.utils.rpc.callbacks;

import com.sun.jna.Callback;
import antileak.base.api.utils.rpc.utils.DiscordUser;

public interface JoinRequestCallback extends Callback {
    void apply(DiscordUser var1);
}
