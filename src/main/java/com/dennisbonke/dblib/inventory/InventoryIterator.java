package com.dennisbonke.dblib.inventory;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

public class InventoryIterator {
    /**
     * Returns an Iterable object for the specified side of the inventory.
     *
     * @param inv
     * @param side
     * @return Iterable
     */
    public static Iterable<IInvSlot> getIterable(IInventory inv, ForgeDirection side) {
        if (inv instanceof ISidedInventory) {
            return new InventoryIteratorSided((ISidedInventory) inv, side);
        }

        return new InventoryIteratorSimple(inv);
    }

    public interface IInvSlot {

        int getIndex();

        boolean canPutStackInSlot(ItemStack stack);

        boolean canTakeStackFromSlot(ItemStack stack);

        ItemStack decreaseStackInSlot();

        ItemStack getStackInSlot();

        void setStackInSlot(ItemStack stack);
    }
}
