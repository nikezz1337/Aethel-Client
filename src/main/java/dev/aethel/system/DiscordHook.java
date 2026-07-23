package dev.aethel.system;

import eu.donyka.discord.RPCHandler;
import eu.donyka.discord.discord.RichPresence;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DiscordHook {
    @SneakyThrows
    public void startRPC() {
        RPCHandler.setOnReady(user -> {
            RichPresence presence = RichPresence.builder()
                    .details("t.me/aethelclient")
                    .largeImageKey("https://i.imgur.com/m3K8KID.jpeg")
                    .largeImageText("https://t.me/aethelclient")
                    .build();

            RPCHandler.updatePresence(presence);
        });

        RPCHandler.setOnDisconnected(error -> {
            System.out.println("RPC Disconnected: " + error);
        });

        RPCHandler.setOnErrored(error -> {
            System.out.println("RPC Errored: " + error);
        });

        RPCHandler.startup("1522355608370675923", false);
    }

    public void stopRPC() {
        RPCHandler.shutdown();
    }
}
