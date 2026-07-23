package dev.aethel.config;

public class ClientConfig {
    private static final String CLIENT_NAME = "Aethel Client";
    private static String user = "xuesos";

    public static String getUser() {
        return user;
    }

    public static void setUser(String newUser) {
        user = newUser;
    }

    public static String getWindowTitle() {
        return CLIENT_NAME + "(" + user + ")";
    }
}
