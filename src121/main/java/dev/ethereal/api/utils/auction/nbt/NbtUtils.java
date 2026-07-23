package dev.ethereal.api.utils.auction.nbt;

import dev.ethereal.api.system.interfaces.QuickImports;
import lombok.experimental.UtilityClass;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtSizeTracker;
import net.minecraft.registry.RegistryWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public class NbtUtils implements QuickImports {
    public <T extends ISerializable<?>> NbtList listToTag(Iterable<T> list) {
        NbtList tag = new NbtList();
        for (T item : list) tag.add(item.toTag());
        return tag;
    }

    public <T> List<T> listFromTag(NbtList tag, ToValue<T> toItem) {
        List<T> list = new ArrayList<>(tag.size());
        for (NbtElement itemTag : tag) {
            T value = toItem.toValue(itemTag);
            if (value != null) list.add(value);
        }
        return list;
    }

    public <K, V extends ISerializable<?>> NbtCompound mapToTag(Map<K, V> map) {
        NbtCompound tag = new NbtCompound();
        for (K key : map.keySet()) tag.put(key.toString(), map.get(key).toTag());
        return tag;
    }

    public <K, V> Map<K, V> mapFromTag(NbtCompound tag, ToKey<K> toKey, ToValue<V> toValue) {
        Map<K, V> map = new HashMap<>(tag.getSize());
        for (String key : tag.getKeys()) map.put(toKey.toKey(key), toValue.toValue(tag.get(key)));
        return map;
    }

    public boolean containsAllKeys(NbtCompound source, NbtCompound required) {
        for (String key : required.getKeys()) {
            if (!source.contains(key)) return false;
        }
        return true;
    }

    public boolean matchesNbtValues(NbtCompound source, NbtCompound required) {
        for (String key : required.getKeys()) {
            if (!source.contains(key)) return false;

            NbtElement sourceElement = source.get(key);
            NbtElement requiredElement = required.get(key);
            if (sourceElement == null || requiredElement == null || !sourceElement.equals(requiredElement)) {
                return false;
            }
        }
        return true;
    }

    public NbtCompound copyNbtKeys(NbtCompound source, String... keys) {
        NbtCompound result = new NbtCompound();
        for (String key : keys) {
            if (source.contains(key)) result.put(key, source.get(key).copy());
        }
        return result;
    }

    public boolean toClipboard(ISerializable<?> serializable) {
        return toClipboard(serializable.toTag());
    }

    public boolean toClipboard(NbtCompound tag) {
        String previousClipboard = mc.keyboard.getClipboard();
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            NbtIo.writeCompressed(tag, output);
            mc.keyboard.setClipboard(Base64.getEncoder().encodeToString(output.toByteArray()));
            return true;
        } catch (Exception e) {
            mc.keyboard.setClipboard(previousClipboard);
            return false;
        }
    }

    public boolean fromClipboard(ISerializable<?> serializable) {
        NbtCompound tag = fromClipboard();
        if (tag == null) return false;

        NbtCompound sourceTag = serializable.toTag();
        for (String key : sourceTag.getKeys()) {
            if (!tag.contains(key)) return false;
        }

        serializable.fromTag(tag);
        return true;
    }

    public NbtCompound fromClipboard() {
        try {
            byte[] data = Base64.getDecoder().decode(mc.keyboard.getClipboard().trim());
            return NbtIo.readCompressed(new DataInputStream(new ByteArrayInputStream(data)), NbtSizeTracker.ofUnlimitedBytes());
        } catch (Exception e) {
            return null;
        }
    }

    public boolean writeToFile(NbtCompound tag, File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) return false;

        try (FileOutputStream output = new FileOutputStream(file)) {
            NbtIo.writeCompressed(tag, output);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public NbtCompound readFromFile(File file) {
        if (file == null || !file.exists()) return null;
        try (FileInputStream input = new FileInputStream(file)) {
            return NbtIo.readCompressed(input, NbtSizeTracker.ofUnlimitedBytes());
        } catch (Exception e) {
            return null;
        }
    }

    public boolean saveItemStack(String name, ItemStack stack, File directory, RegistryWrapper.WrapperLookup registries) {
        if (!directory.exists() && !directory.mkdirs()) return false;
        File file = new File(directory, name + ".nbt");
        try {
            NbtElement nbt = ItemStack.CODEC.encodeStart(registries.getOps(NbtOps.INSTANCE), stack).getOrThrow();
            return nbt instanceof NbtCompound tag && writeToFile(tag, file);
        } catch (Exception e) {
            return false;
        }
    }

    public ItemStack loadItemStack(String name, File directory, RegistryWrapper.WrapperLookup registries) {
        try {
            NbtCompound tag = readFromFile(new File(directory, name + ".nbt"));
            if (tag == null) return ItemStack.EMPTY;

            Optional<ItemStack> stack = ItemStack.CODEC.parse(registries.getOps(NbtOps.INSTANCE), tag).result();
            return stack.orElse(ItemStack.EMPTY);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    public interface ToKey<T> {
        T toKey(String string);
    }

    public interface ToValue<T> {
        T toValue(NbtElement tag);
    }
}
