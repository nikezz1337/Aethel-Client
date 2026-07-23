package dev.ethereal.api.system.configs;

import lombok.Getter;
import dev.ethereal.api.system.files.AbstractFile;

public class FriendManager extends AbstractFile {
    @Getter private static final FriendManager instance = new FriendManager();

    @Override
    public String fileName() {
        return "friends";
    }
}
