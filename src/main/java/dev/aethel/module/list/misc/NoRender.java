package dev.aethel.module.list.misc;

import dev.aethel.module.Module;
import dev.aethel.module.ModuleCategory;
import dev.aethel.module.ModuleInformation;

@ModuleInformation(
    moduleName = "NoRender",
    moduleCategory = ModuleCategory.MISC,
    moduleDesc = "Убирает элементы рендера"
)
public class NoRender extends Module {
    public static final NoRender INSTANCE = new NoRender();

    public NoRender() {}
}
