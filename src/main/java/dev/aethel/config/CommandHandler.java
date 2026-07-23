package dev.aethel.config;

import dev.aethel.Aethel;
import dev.aethel.module.list.render.BlockESP;
import dev.aethel.util.world.ServerUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.File;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static boolean handle(String msg) {
        if (!msg.startsWith(".")) return false;

        if (tryAutoCommand(msg)) return true;

        msg = msg.substring(1);
        String[] args = msg.split(" ");
        if (args.length == 0 || args[0].isEmpty()) return true;

        switch (args[0]) {
            case "cfg" -> handleCfg(args);
            case "friend" -> handleFriend(args);
            case "bind" -> handleBind(args);
            case "vclip" -> handleVClip(args);
            case "staff" -> handleStaff(args);
            case "macro" -> handleMacro(args);
            case "blockesp" -> handleBlockEsp(args);
            case "gps" -> handleGps(args);
            case "rct" -> handleRct(args);
        }
        return true;
    }

    private static final java.util.LinkedHashMap<String, String> AUTO_COMMANDS = new java.util.LinkedHashMap<>() {{
        put("фр", "/ah");
        put("ус", "/ec");
        put("зфн", "/pay");
        put("ещз", "/top");
        put("ифдфтсу", "/balance");
        put("ьщтун", "/money");
        put("дшые", "/list");
        put("рги", "/hub");
        put("дщиин", "/lobby");
    }};

    private static boolean tryAutoCommand(String msg) {
        String raw = msg.substring(1);
        String lower = raw.toLowerCase();

        for (var entry : AUTO_COMMANDS.entrySet()) {
            String shortcut = entry.getKey();
            String fullCommand = entry.getValue();

            if (lower.equals(shortcut)) {
                mc.player.networkHandler.sendCommand(fullCommand.substring(1));
                ChatUtils.send("Написал правильно: " + fullCommand);
                return true;
            }

            if (lower.startsWith(shortcut + " ")) {
                String args = raw.substring(shortcut.length() + 1);
                String finalCmd = fullCommand + " " + args;
                mc.player.networkHandler.sendCommand(finalCmd.substring(1));
                ChatUtils.send("Написал правильно: " + finalCmd);
                return true;
            }
        }
        return false;
    }

    private static void handleCfg(String[] args) {
        if (args.length < 2) return;
        switch (args[1]) {
            case "save" -> {
                if (args.length < 3) return;
                ConfigManager.save(args[2]);
                ChatUtils.send("Конфиг " + args[2] + " сохранён!");
            }
            case "load" -> {
                if (args.length < 3) return;
                ConfigManager.load(args[2]);
                ChatUtils.send("Конфиг " + args[2] + " загружен!");
            }
            case "delete" -> {
                if (args.length < 3) return;
                ConfigManager.delete(args[2]);
                ChatUtils.send("Конфиг " + args[2] + " удалён!");
            }
            case "list" -> {
                File dir = new File("Aethel\\configs");
                File[] files = dir.listFiles((d, name) -> name.endsWith(".json") && !name.equals("autocfg.json"));
                if (files == null || files.length == 0) {
                    ChatUtils.send("Конфиги: нет");
                } else {
                    String list = java.util.Arrays.stream(files)
                            .map(f -> f.getName().replace(".json", ""))
                            .collect(Collectors.joining(", "));
                    ChatUtils.send("Конфиги: " + list);
                }
            }
            case "dir" -> {
                try {
                    new ProcessBuilder("explorer.exe", new File("Aethel\\configs").getAbsolutePath()).start();
                } catch (Exception ignored) {}
            }
        }
    }

    private static void handleFriend(String[] args) {
        if (args.length < 2) return;
        switch (args[1]) {
            case "add" -> {
                if (args.length < 3) return;
                if (FriendManager.add(args[2])) {
                    ChatUtils.send("Игрок " + args[2] + " добавлен в друзья!");
                } else {
                    ChatUtils.send("Игрок " + args[2] + " уже в списке друзей!");
                }
            }
            case "remove" -> {
                if (args.length < 3) return;
                FriendManager.remove(args[2]);
                ChatUtils.send("Игрок " + args[2] + " удалён из друзей!");
            }
            case "list" -> {
                List<String> friends = FriendManager.getFriends();
                if (friends.isEmpty()) {
                    ChatUtils.send("Друзья: нет");
                } else {
                    ChatUtils.send("Друзья: " + String.join(", ", friends));
                }
            }
            case "clear" -> {
                FriendManager.clear();
                ChatUtils.send("Список друзей очищен!");
            }
        }
    }

    private static void handleBind(String[] args) {
        if (args.length < 2) return;
        switch (args[1]) {
            case "add" -> {
                if (args.length < 4) return;
                var module = Aethel.getInstance().getModuleStorage().get(args[2]);
                if (module == null) {
                    ChatUtils.send("Модуль " + args[2] + " не найден!");
                    return;
                }
                int key = parseKey(args[3]);
                if (key == -1) {
                    ChatUtils.send("Клавиша " + args[3] + " не найдена!");
                    return;
                }
                module.setKey(key);
                ChatUtils.send("Модуль " + module.getName() + " привязан к " + args[3]);
            }
            case "remove" -> {
                if (args.length < 3) return;
                var module = Aethel.getInstance().getModuleStorage().get(args[2]);
                if (module == null) {
                    ChatUtils.send("Модуль " + args[2] + " не найден!");
                    return;
                }
                module.setKey(-1);
                ChatUtils.send("Бинд снят с модуля " + module.getName());
            }
            case "clear" -> {
                if (args.length >= 3) {
                    var module = Aethel.getInstance().getModuleStorage().get(args[2]);
                    if (module == null) {
                        ChatUtils.send("Модуль " + args[2] + " не найден!");
                        return;
                    }
                    module.setKey(-1);
                    ChatUtils.send("Бинд снят с модуля " + module.getName());
                } else {
                    for (var mod : Aethel.getInstance().getModuleStorage().getModules()) {
                        mod.setKey(-1);
                    }
                    ChatUtils.send("Все бинды очищены!");
                }
            }
            case "list" -> {
                var bound = Aethel.getInstance().getModuleStorage().getModules().stream()
                        .filter(m -> m.getKey() != -1)
                        .collect(Collectors.toList());
                if (bound.isEmpty()) {
                    ChatUtils.send("Бинды: нет");
                } else {
                    for (var m : bound) {
                        String keyName = getKeyName(m.getKey());
                        ChatUtils.send("" + m.getName() + " -> " + keyName);
                    }
                }
            }
        }
    }

    private static void handleVClip(String[] args) {
        if (args.length < 2) return;
        if (mc.player == null) return;
        switch (args[1]) {
            case "up" -> {
                double y = findSafeY(true);
                if (y != Double.MAX_VALUE) {
                    mc.player.setPosition(mc.player.getX(), y, mc.player.getZ());
                    ChatUtils.send("Телепорт вверх выполнен!");
                } else {
                    ChatUtils.send("Не найдено безопасное место!");
                }
            }
            case "down" -> {
                double y = findSafeY(false);
                if (y != Double.MAX_VALUE) {
                    mc.player.setPosition(mc.player.getX(), y, mc.player.getZ());
                    ChatUtils.send("Телепорт вниз выполнен!");
                } else {
                    ChatUtils.send("Не найдено безопасное место!");
                }
            }
            default -> {
                try {
                    double y = Double.parseDouble(args[1]);
                    mc.player.setPosition(mc.player.getX(), mc.player.getY() + y, mc.player.getZ());
                    ChatUtils.send("Телепорт на " + y + " блоков!");
                } catch (NumberFormatException e) {
                    ChatUtils.send("Использование: .vclip <число/up/down>");
                }
            }
        }
    }

    private static void handleStaff(String[] args) {
        if (args.length < 2) return;
        switch (args[1]) {
            case "add" -> {
                if (args.length < 3) return;
                if (StaffManager.add(args[2])) {
                    ChatUtils.send("Игрок " + args[2] + " добавлен в персонал!");
                } else {
                    ChatUtils.send("Игрок " + args[2] + " уже в списке персонала!");
                }
            }
            case "remove" -> {
                if (args.length < 3) return;
                StaffManager.remove(args[2]);
                ChatUtils.send("Игрок " + args[2] + " удалён из персонала!");
            }
            case "list" -> {
                List<String> staff = StaffManager.getStaff();
                if (staff.isEmpty()) {
                    ChatUtils.send("Персонал: нет");
                } else {
                    ChatUtils.send("Персонал: " + String.join(", ", staff));
                }
            }
            case "clear" -> {
                StaffManager.clear();
                ChatUtils.send("Список персонала очищен!");
            }
        }
    }

    private static void handleMacro(String[] args) {
        if (args.length < 2) return;
        switch (args[1]) {
            case "add" -> {
                if (args.length < 4) return;
                int key = parseKey(args[2]);
                if (key == -1) {
                    ChatUtils.send("Клавиша " + args[2] + " не найдена!");
                    return;
                }
                StringBuilder command = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (command.length() > 0) command.append(" ");
                    command.append(args[i]);
                }
                String name = "Macro-" + args[2].toUpperCase() + "-" + (System.currentTimeMillis() % 10000);
                MacroManager.add(name, key, command.toString());
                ChatUtils.send("Макрос привязан к " + args[2]);
            }
            case "remove" -> {
                if (args.length < 3) return;
                if (MacroManager.remove(args[2]) != null) {
                    ChatUtils.send("Макрос " + args[2] + " удалён!");
                } else {
                    ChatUtils.send("Макрос " + args[2] + " не найден!");
                }
            }
            case "list" -> {
                var all = MacroManager.getAll();
                if (all.isEmpty()) {
                    ChatUtils.send("Макросы: нет");
                } else {
                    var names = all.stream().map(m -> m.name).collect(Collectors.joining(", "));
                    ChatUtils.send("Макросы: " + names);
                }
            }
            case "clear" -> {
                if (args.length >= 3) {
                    if (MacroManager.remove(args[2]) != null) {
                        ChatUtils.send("Макрос " + args[2] + " удалён!");
                    } else {
                        ChatUtils.send("Макрос " + args[2] + " не найден!");
                    }
                } else {
                    MacroManager.clear();
                    ChatUtils.send("Все макросы очищены!");
                }
            }
        }
    }

    private static void handleBlockEsp(String[] args) {
        if (args.length < 2) return;
        var blockEsp = Aethel.getInstance().getModuleStorage().get(BlockESP.class);
        if (blockEsp == null) return;
        switch (args[1]) {
            case "add" -> {
                if (args.length < 3) return;
                if (blockEsp.isTracking(args[2])) {
                    ChatUtils.send("Блок " + args[2] + " уже отслеживается!");
                    return;
                }
                blockEsp.addBlock(args[2]);
                ChatUtils.send("Блок " + args[2] + " добавлен в отслеживание!");
            }
            case "remove" -> {
                if (args.length < 3) return;
                if (!blockEsp.isTracking(args[2])) {
                    ChatUtils.send("Блок " + args[2] + " не отслеживается!");
                    return;
                }
                blockEsp.removeBlock(args[2]);
                ChatUtils.send("Блок " + args[2] + " удалён из отслеживания!");
            }
            case "list" -> {
                var blocks = blockEsp.getTrackedBlocks();
                if (blocks.isEmpty()) {
                    ChatUtils.send("Список отслеживаемых блоков пуст!");
                } else {
                    String blockList = String.join(", ", blocks);
                    ChatUtils.send("Отслеживаемые блоки (" + blocks.size() + "): " + blockList);
                }
            }
            case "clear" -> {
                if (blockEsp.getTrackedBlocks().isEmpty()) {
                    ChatUtils.send("Список отслеживаемых блоков уже пуст!");
                    return;
                }
                blockEsp.clearBlocks();
                ChatUtils.send("Список отслеживаемых блоков очищен!");
            }
        }
    }

    private static void handleGps(String[] args) {
        if (args.length < 2) return;
        if (args[1].equals("clear") || args[1].equals("remove")) {
            WaypointStorage.clear();
            ChatUtils.send("Метка удалена!");
            return;
        }
        if (args.length < 3) return;
        try {
            int x = Integer.parseInt(args[1]);
            int z = Integer.parseInt(args[2]);
            WaypointStorage.set(x, z);
            ChatUtils.send("Установлен маршрут: " + x + " " + z);
        } catch (NumberFormatException e) {
            ChatUtils.send("Использование: .gps <x> <z>");
        }
    }

    private static int parseKey(String name) {
        if (name.equalsIgnoreCase("NONE")) return -1;
        try {
            var fields = GLFW.class.getDeclaredFields();
            for (var f : fields) {
                if (Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())
                        && f.getName().startsWith("GLFW_KEY_")
                        && f.getName().substring(9).equalsIgnoreCase(name)) {
                    return f.getInt(null);
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static String getKeyName(int key) {
        try {
            var fields = GLFW.class.getDeclaredFields();
            for (var f : fields) {
                if (Modifier.isStatic(f.getModifiers()) && Modifier.isPublic(f.getModifiers())
                        && f.getType() == int.class && f.getName().startsWith("GLFW_KEY_")
                        && f.getInt(null) == key) {
                    return f.getName().substring(9);
                }
            }
        } catch (Exception ignored) {}
        return "UNKNOWN";
    }

    private static double findSafeY(boolean up) {
        if (mc.world == null || mc.player == null) return Double.MAX_VALUE;
        int x = (int) Math.floor(mc.player.getX());
        int z = (int) Math.floor(mc.player.getZ());
        int startY = (int) Math.floor(mc.player.getY());
        int limit = up ? startY + 256 : startY - 1;
        int step = up ? 1 : -1;
        for (int y = startY + step; up ? y <= limit : y >= limit; y += step) {
            var floor = mc.world.getBlockState(new net.minecraft.util.math.BlockPos(x, y - 1, z));
            var feet = mc.world.getBlockState(new net.minecraft.util.math.BlockPos(x, y, z));
            var head = mc.world.getBlockState(new net.minecraft.util.math.BlockPos(x, y + 1, z));
            if (!floor.isAir() && feet.isAir() && head.isAir()) {
                return y;
            }
        }
        return Double.MAX_VALUE;
    }

    private static void handleRct(String[] args) {
        if (mc.player == null || mc.world == null) {
            ChatUtils.send("Вы не в игре!");
            return;
        }

//        if (!ServerUtil.isHolyWorld()) {
//            ChatUtils.send("[RCT] Не работает на этом сервере!");
//            return;
//        }

        if (ServerUtil.isPvp()) {
            ChatUtils.send("[RCT] Вы находитесь в режиме PvP!");
            return;
        }

        int anarchy;
        if (args.length >= 2) {
            try {
                anarchy = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                ChatUtils.send("Использование: .rct <номер анархии>");
                return;
            }
        } else {
            anarchy = ServerUtil.getAnarchy();
        }

        if (anarchy <= 0 || anarchy >= 64) {
            ChatUtils.send("[RCT] Неверный номер анархии!");
            return;
        }

        RCTHandler.getInstance().reconnect(anarchy);
    }
}
