package com.bgsoftware.wildbuster.nms.v1_16_R3;

import net.minecraft.server.v1_16_R3.ChatMessage;
import net.minecraft.server.v1_16_R3.TileEntityHopper;
import org.bukkit.inventory.InventoryHolder;

public class CustomTileEntityHopper extends TileEntityHopper {

    private final InventoryHolder holder;

    public CustomTileEntityHopper(InventoryHolder holder, String title) {
        this.holder = holder;
        this.setCustomName(new ChatMessage(title));
    }

    @Override
    public InventoryHolder getOwner() {
        return holder;
    }
}