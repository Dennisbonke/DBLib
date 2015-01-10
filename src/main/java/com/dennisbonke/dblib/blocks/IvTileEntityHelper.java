package com.dennisbonke.dblib.blocks;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

public class IvTileEntityHelper {
    public static Packet getStandardDescriptionPacket(TileEntity tileEntity)
    {
        NBTTagCompound var1 = new NBTTagCompound();
        tileEntity.writeToNBT(var1);
        return new S35PacketUpdateTileEntity(tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, 1, var1);
    }
}
