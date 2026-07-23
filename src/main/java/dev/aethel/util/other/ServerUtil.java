package dev.aethel.util.other;

import dev.aethel.util.IMinecraft;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ServerUtil implements IMinecraft {

    public boolean isHW() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return false;
        var serverInfo = mc.getNetworkHandler().getServerInfo();
        String brand = mc.getNetworkHandler().getBrand();
        String serverIp = serverInfo != null ? serverInfo.address.toLowerCase() : "";
        if (brand == null) return false;
        brand = brand.toLowerCase();
        return serverIp.contains("holyworld") && !serverIp.contains("funtime") || brand.contains("holyworld") && !serverIp.contains("funtime");
    }

    public boolean isFT() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return false;
        var serverInfo = mc.getNetworkHandler().getServerInfo();
        String brand = mc.getNetworkHandler().getBrand();
        if (brand == null) return false;
        brand = brand.toLowerCase();
        String serverIp = serverInfo != null ? serverInfo.address.toLowerCase() : "";
        return brand.contains("botfilter") || serverIp.contains("funtime");
    }

    public boolean isST() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return false;
        var serverInfo = mc.getNetworkHandler().getServerInfo();
        String serverIp = serverInfo != null ? serverInfo.address.toLowerCase() : "";
        return serverIp.contains("spookytime");
    }

    public boolean isRW() {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) return false;
        var serverInfo = mc.getNetworkHandler().getServerInfo();
        String serverIp = serverInfo != null ? serverInfo.address.toLowerCase() : "";
        return serverIp.contains("reallyworld");
    }
}
