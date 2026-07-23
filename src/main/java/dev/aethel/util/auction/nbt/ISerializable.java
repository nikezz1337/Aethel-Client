package dev.aethel.util.auction.nbt;

import net.minecraft.nbt.NbtCompound;

public interface ISerializable<T extends NbtCompound> {
    T toTag();
    void fromTag(T tag);
}
