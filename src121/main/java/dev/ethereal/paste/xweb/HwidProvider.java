package dev.ethereal.paste.xweb;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public final class HwidProvider {

    private static volatile String cached;

    private HwidProvider() {
    }

    public static void main(String[] args) {
        System.out.println(current());
    }

    public static String current() {
        if (cached != null) return cached;

        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            Future<String> boardFuture = executor.submit(() -> firstNonEmpty(
                    () -> reg("HKLM\\HARDWARE\\DESCRIPTION\\System\\BIOS", "BaseBoardManufacturer"),
                    () -> wmic("baseboard get manufacturer", "manufacturer"),
                    () -> powershell("(Get-CimInstance Win32_BaseBoard | Select-Object -First 1 -ExpandProperty Manufacturer)")));
            Future<String> cpuFuture = executor.submit(() -> firstNonEmpty(
                    () -> reg("HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "ProcessorNameString"),
                    () -> wmic("cpu get name", "name"),
                    () -> powershell("(Get-CimInstance Win32_Processor | Select-Object -First 1 -ExpandProperty Name)")));
            Future<String> gpuFuture = executor.submit(() -> firstNonEmpty(
                    () -> wmic("path win32_videocontroller get name", "name"),
                    () -> powershell("(Get-CimInstance Win32_VideoController | Select-Object -First 1 -ExpandProperty Name)")));

            String board = await(boardFuture);
            String cpu = await(cpuFuture);
            String gpu = await(gpuFuture);
            String raw = board + "::" + cpu + "::" + gpu;
            cached = sha256(raw);
            return cached;
        } finally {
            executor.shutdownNow();
        }
    }

    private static String await(Future<String> future) {
        try {
            return future.get(20, TimeUnit.SECONDS);
        } catch (Exception exception) {
            return "UNKNOWN";
        }
    }

    @SafeVarargs
    private static String firstNonEmpty(Supplier<String>... sources) {
        for (Supplier<String> source : sources) {
            String value = source.get();
            if (value != null && !value.isBlank() && !value.equalsIgnoreCase("UNKNOWN")) {
                return value.trim();
            }
        }
        return "UNKNOWN";
    }

    private static String powershell(String command) {
        return readLine(new String[]{"powershell", "-NoProfile", "-NonInteractive", "-Command", command}, null, 8000);
    }

    private static String wmic(String args, String header) {
        return readLine(new String[]{"cmd", "/c", "wmic " + args}, header, 5000);
    }

    private static String reg(String key, String name) {
        String line = readLineContaining(new String[]{"reg", "query", key, "/v", name}, "REG_", 3000);
        if (line == null) return null;
        int idx = line.indexOf("REG_");
        if (idx < 0) return null;
        int valueStart = line.indexOf("    ", idx);
        return valueStart < 0 ? null : line.substring(valueStart).trim();
    }

    private static String readLine(String[] command, String headerToSkip, long timeoutMs) {
        for (String trimmed : run(command, timeoutMs)) {
            if (headerToSkip != null && trimmed.equalsIgnoreCase(headerToSkip)) continue;
            return trimmed;
        }
        return null;
    }

    private static String readLineContaining(String[] command, String marker, long timeoutMs) {
        for (String trimmed : run(command, timeoutMs)) {
            if (trimmed.contains(marker)) return trimmed;
        }
        return null;
    }

    private static java.util.List<String> run(String[] command, long timeoutMs) {
        final java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        Process process = null;
        try {
            process = new ProcessBuilder(command).redirectErrorStream(true).start();
            final Process current = process;
            Thread reader = new Thread(() -> {
                try (BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(current.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty()) lines.add(trimmed);
                    }
                } catch (Exception ignored) {
                }
            });
            reader.setDaemon(true);
            reader.start();
            if (!process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
            }
            reader.join(500);
        } catch (Exception ignored) {
            if (process != null) process.destroyForcibly();
        }
        return new java.util.ArrayList<>(lines);
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
