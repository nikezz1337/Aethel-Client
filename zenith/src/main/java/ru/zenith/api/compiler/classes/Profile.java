package ru.zenith.api.compiler.classes;

import ru.zenith.api.compiler.annotations.VMProtect;
import ru.zenith.api.compiler.enums.VMProtectType;



public class Profile {
    private String name;
    
    private String version;
    
    public Profile(String name, String version) {
        this.name = name;
        this.version = version;
    }
    
    public String getName() {
        return name;
    }
    
    public String getVersion() {
        return version;
    }
}
