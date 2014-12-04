package com.dennisbonke.dblib.tools;

import com.dennisbonke.dblib.blocks.BlockArea;
import com.dennisbonke.dblib.blocks.BlockCoord;
import com.dennisbonke.dblib.blocks.IvBlockCollection;
import com.dennisbonke.dblib.math.AxisAlignedTransform2D;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IvWorldData {
    public static final String ID_FIX_TAG_KEY = "SG_ID_FIX_TAG";

    public IvBlockCollection blockCollection;
    public List<TileEntity> tileEntities;
    public List<Entity> entities;

    public IvWorldData(IvBlockCollection blockCollection, List<TileEntity> tileEntities, List<Entity> entities)
    {
        this.blockCollection = blockCollection;
        this.tileEntities = tileEntities;
        this.entities = entities;
    }

    public IvWorldData(World world, BlockArea blockArea, boolean captureEntities)
    {
        int[] size = blockArea.areaSize();
        blockCollection = new IvBlockCollection(size[0], size[1], size[2]);

        tileEntities = new ArrayList<>();
        for (BlockCoord worldCoord : blockArea)
        {
            BlockCoord dataCoord = worldCoord.subtract(blockArea.getLowerCorner());

            blockCollection.setBlock(dataCoord, world.getBlock(worldCoord.x, worldCoord.y, worldCoord.z));
            blockCollection.setMetadata(dataCoord, (byte) world.getBlockMetadata(worldCoord.x, worldCoord.y, worldCoord.z));

            TileEntity tileEntity = world.getTileEntity(worldCoord.x, worldCoord.y, worldCoord.z);
            if (tileEntity != null)
            {
                tileEntities.add(tileEntity);
            }
        }

        if (captureEntities)
        {
            entities = world.getEntitiesWithinAABBExcludingEntity(null, blockArea.asAxisAlignedBB(), new EntitySelectorSaveable());
        }
        else
        {
            entities = Collections.emptyList();
        }
    }

    public IvWorldData(NBTTagCompound compound, World world, MCRegistry registry)
    {
        compound = (NBTTagCompound) compound.copy(); // Copy since ID fix tags are being removed when being applied

        blockCollection = new IvBlockCollection(compound.getCompoundTag("blockCollection"), registry);

        NBTTagList teList = compound.getTagList("tileEntities", Constants.NBT.TAG_COMPOUND);
        tileEntities = new ArrayList<>(teList.tagCount());
        for (int i = 0; i < teList.tagCount(); i++)
        {
            NBTTagCompound teCompound = teList.getCompoundTagAt(i);
            recursivelyApplyIDFixTags(teCompound, registry);
            TileEntity tileEntity = TileEntity.createAndLoadEntity(teCompound);

            tileEntities.add(tileEntity);
        }

        if (world != null)
        {
            NBTTagList entityList = compound.getTagList("entities", Constants.NBT.TAG_COMPOUND);
            entities = new ArrayList<>(entityList.tagCount());
            for (int i = 0; i < entityList.tagCount(); i++)
            {
                NBTTagCompound entityCompound = entityList.getCompoundTagAt(i);
                recursivelyApplyIDFixTags(entityCompound, registry);
                Entity entity = EntityList.createEntityFromNBT(entityCompound, world);

                entities.add(entity);
            }
        }
    }

    public NBTTagCompound createTagCompound(BlockCoord referenceCoord)
    {
        NBTTagCompound compound = new NBTTagCompound();

        compound.setTag("blockCollection", blockCollection.createTagCompound());

        NBTTagList teList = new NBTTagList();
        for (TileEntity tileEntity : tileEntities)
        {
            NBTTagCompound teCompound = new NBTTagCompound();

            moveTileEntityForGeneration(tileEntity, referenceCoord.invert());
            tileEntity.writeToNBT(teCompound);
            moveTileEntityForGeneration(tileEntity, referenceCoord);

            recursivelyInjectIDFixTags(teCompound);
            teList.appendTag(teCompound);
        }
        compound.setTag("tileEntities", teList);

        NBTTagList entityList = new NBTTagList();
        for (Entity entity : entities)
        {
            NBTTagCompound entityCompound = new NBTTagCompound();

            moveEntityForGeneration(entity, referenceCoord.invert());
            entity.writeToNBTOptional(entityCompound);
            moveEntityForGeneration(entity, referenceCoord);

            recursivelyInjectIDFixTags(entityCompound);
            entityList.appendTag(entityCompound);
        }
        compound.setTag("entities", entityList);

        return compound;
    }

    public static void moveTileEntityForGeneration(TileEntity tileEntity, BlockCoord coord)
    {
        if (tileEntity instanceof Movable)
            ((Movable) tileEntity).move(coord.x, coord.y, coord.z);
        else
        {
            tileEntity.xCoord += coord.x;
            tileEntity.yCoord += coord.y;
            tileEntity.zCoord += coord.z;
        }
    }

    public static void setTileEntityPosForGeneration(TileEntity tileEntity, BlockCoord coord)
    {
        moveTileEntityForGeneration(tileEntity, coord.subtract(new BlockCoord(tileEntity)));
    }

    public static void transformTileEntityPosForGeneration(TileEntity tileEntity, AxisAlignedTransform2D transform, int[] size)
    {
        if (tileEntity instanceof Transformable)
            ((Transformable) tileEntity).transform(transform.getRotation(), transform.isMirrorX(), size);
        else
            setTileEntityPosForGeneration(tileEntity, transform.apply(new BlockCoord(tileEntity), size));
    }

    public static void moveEntityForGeneration(Entity entity, BlockCoord coord)
    {
        if (entity instanceof Movable)
            ((Movable) entity).move(coord.x, coord.y, coord.z);
        else
        {
            entity.setPosition(entity.posX + coord.x, entity.posY + coord.y, entity.posZ + coord.z);

            if (entity instanceof EntityHanging)
            {
                EntityHanging entityHanging = (EntityHanging) entity;
                entityHanging.field_146063_b += coord.x;
                entityHanging.field_146064_c += coord.y;
                entityHanging.field_146062_d += coord.z;
                entityHanging.setDirection(entityHanging.hangingDirection);
            }
        }
    }

    public static void transformEntityPosForGeneration(Entity entity, AxisAlignedTransform2D transform, int[] size)
    {
        if (entity instanceof Transformable)
            ((Transformable) entity).transform(transform.getRotation(), transform.isMirrorX(), size);
        else
        {
            double[] newEntityPos = transform.apply(new double[]{entity.posX, entity.posY, entity.posZ}, size);
            entity.setPosition(newEntityPos[0], newEntityPos[1], newEntityPos[2]);

            if (entity instanceof EntityHanging)
            {
                EntityHanging entityHanging = (EntityHanging) entity;
                BlockCoord hangingCoord = new BlockCoord(entityHanging.field_146063_b, entityHanging.field_146064_c, entityHanging.field_146062_d);
                BlockCoord newHangingCoord = transform.apply(hangingCoord, size);
                entityHanging.field_146063_b = newHangingCoord.x;
                entityHanging.field_146064_c = newHangingCoord.y;
                entityHanging.field_146062_d = newHangingCoord.z;
                entityHanging.setDirection(entityHanging.hangingDirection);
            }
        }
    }

    public static void recursivelyInjectIDFixTags(NBTTagCompound compound)
    {
        injectIDFixTags(compound);

        for (Object key : compound.func_150296_c())
        {
            String keyString = (String) key;
            NBTBase innerCompound = compound.getTag(keyString);

            if (innerCompound instanceof NBTTagCompound)
            {
                recursivelyInjectIDFixTags((NBTTagCompound) innerCompound);
            }
            else if (innerCompound instanceof NBTTagList)
            {
                recursivelyInjectIDFixTags((NBTTagList) innerCompound);
            }
        }
    }

    public static void recursivelyInjectIDFixTags(NBTTagList list)
    {
        int tagType = list.func_150303_d();
        switch (tagType)
        {
            case Constants.NBT.TAG_COMPOUND:
                for (int i = 0; i < list.tagCount(); i++)
                {
                    recursivelyInjectIDFixTags(list.getCompoundTagAt(i));
                }
                break;
            case Constants.NBT.TAG_LIST:
//                for (int i = 0; i < list.tagCount(); i++)
//                {
//                    recursivelyInjectIDFixTags(list.getCompoundTagAt(i));
//                }
                break;
        }
    }

    public static void injectIDFixTags(NBTTagCompound compound)
    {
        NBTTagList list = null;

        if (compound.hasKey("id") && compound.hasKey("Count") && compound.hasKey("Damage"))
        {
            list = new NBTTagList();
            addItemTag(compound.getInteger("id"), list, "id");
        }

        if (list != null)
        {
            compound.setTag(ID_FIX_TAG_KEY, list);
        }
    }

    public static void addItemTag(int itemID, NBTTagList tagList, String tagDest)
    {
        Item item = Item.getItemById(itemID);
        if (item != null)
        {
            String stringID = Item.itemRegistry.getNameForObject(item);

            NBTTagCompound idCompound = new NBTTagCompound();
            idCompound.setString("type", "item");
            idCompound.setString("tagDest", tagDest);
            idCompound.setString("itemID", stringID);

            tagList.appendTag(idCompound);
        }
        else
        {
            System.out.println("Failed to apply item tag for structure with ID '" + itemID + "'");
        }
    }

    public static void recursivelyApplyIDFixTags(NBTTagCompound compound, MCRegistry registry)
    {
        applyIDFixTags(compound, registry);
        compound.removeTag(ID_FIX_TAG_KEY);

        for (Object key : compound.func_150296_c())
        {
            String keyString = (String) key;
            NBTBase innerCompound = compound.getTag(keyString);

            if (innerCompound instanceof NBTTagCompound)
            {
                recursivelyApplyIDFixTags((NBTTagCompound) innerCompound, registry);
            }
            else if (innerCompound instanceof NBTTagList)
            {
                recursivelyApplyIDFixTags((NBTTagList) innerCompound, registry);
            }
        }
    }

    public static void recursivelyApplyIDFixTags(NBTTagList list, MCRegistry registry)
    {
        int tagType = list.func_150303_d();
        switch (tagType)
        {
            case Constants.NBT.TAG_COMPOUND:
                for (int i = 0; i < list.tagCount(); i++)
                {
                    recursivelyApplyIDFixTags(list.getCompoundTagAt(i), registry);
                }
                break;
            case Constants.NBT.TAG_LIST:
//                for (int i = 0; i < list.tagCount(); i++)
//                {
//                    recursivelyApplyIDFixTags(list.getCompoundTagAt(i));
//                }
                break;
        }
    }

    public static void applyIDFixTags(NBTTagCompound compound, MCRegistry registry)
    {
        if (compound.hasKey(ID_FIX_TAG_KEY))
        {
            NBTTagList list = compound.getTagList(ID_FIX_TAG_KEY, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++)
            {
                NBTTagCompound fixTag = list.getCompoundTagAt(i);
                String type = fixTag.getString("type");

                if ("item".equals(type))
                {
                    String dest = fixTag.getString("tagDest");
                    String stringID = fixTag.getString("itemID");
                    Item item = registry.itemFromID(stringID);
                    if (item != null)
                    {
                        int itemID = Item.getIdFromItem(item);
                        compound.setInteger(dest, itemID);
                    }
                    else
                    {
                        System.out.println("Failed to fix item tag from structure with ID '" + stringID + "'");
                    }
                }
            }
        }
    }

    public static class EntitySelectorSaveable implements IEntitySelector
    {
        @Override
        public boolean isEntityApplicable(Entity entity)
        {
            return !(entity instanceof EntityPlayer);
        }
    }
}
