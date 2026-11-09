package com.overgrid.disastres.item;

import com.overgrid.disastres.DisastresNaturais;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModItems {
    private ModItems() {}

    public static final Item DISASTER_WAND = register("disaster_wand",
            key -> new DisasterWand(new Item.Settings().registryKey(key).maxCount(1)));

    private static Item register(String name, java.util.function.Function<RegistryKey<Item>, Item> factory) {
        Identifier id = DisastresNaturais.id(name);
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, id);
        return Registry.register(Registries.ITEM, id, factory.apply(key));
    }

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(DISASTER_WAND));
    }
}
