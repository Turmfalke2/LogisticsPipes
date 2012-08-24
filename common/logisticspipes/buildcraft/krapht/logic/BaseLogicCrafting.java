package logisticspipes.buildcraft.krapht.logic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import logisticspipes.buildcraft.krapht.IRequireReliableTransport;
import logisticspipes.buildcraft.krapht.LogisticsManager;
import logisticspipes.buildcraft.krapht.LogisticsRequest;
import logisticspipes.buildcraft.krapht.RoutedPipe;
import logisticspipes.buildcraft.krapht.SimpleServiceLocator;
import logisticspipes.buildcraft.krapht.network.NetworkConstants;
import logisticspipes.buildcraft.krapht.network.PacketCoordinates;
import logisticspipes.buildcraft.krapht.network.PacketInventoryChange;
import logisticspipes.buildcraft.krapht.network.PacketPipeInteger;
import logisticspipes.buildcraft.krapht.proxy.MainProxy;
import logisticspipes.buildcraft.krapht.recipeproviders.ICraftingRecipeProvider;
import logisticspipes.buildcraft.krapht.routing.IRouter;
import logisticspipes.buildcraft.krapht.routing.Router;
import logisticspipes.krapht.AdjacentTile;
import logisticspipes.krapht.ItemIdentifier;
import logisticspipes.krapht.SimpleInventory;
import logisticspipes.krapht.WorldUtil;


import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IInventory;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import buildcraft.api.core.Orientations;
import buildcraft.core.network.TileNetworkData;
import buildcraft.transport.TileGenericPipe;
import cpw.mods.fml.common.network.PacketDispatcher;

public class BaseLogicCrafting extends BaseRoutingLogic implements IRequireReliableTransport {

	protected SimpleInventory _dummyInventory = new SimpleInventory(10, "Requested items", 127);
	//protected final InventoryUtilFactory _invUtilFactory;
	//protected final InventoryUtil _dummyInvUtil;

	@TileNetworkData
	public int signEntityX = 0;
	@TileNetworkData
	public int signEntityY = 0;
	@TileNetworkData
	public int signEntityZ = 0;
	//public LogisticsTileEntiy signEntity;

	protected final LinkedList<ItemIdentifier> _lostItems = new LinkedList<ItemIdentifier>();

	@TileNetworkData
	public int satelliteId = 0;

	public BaseLogicCrafting() {
	/*	this(new InventoryUtilFactory());
	}

	public BaseLogicCrafting(InventoryUtilFactory invUtilFactory) {
		_invUtilFactory = invUtilFactory;
		_dummyInvUtil = _invUtilFactory.getInventoryUtil(_dummyInventory);*/
		throttleTime = 40;
	}

	/* ** SATELLITE CODE ** */

	protected int getNextConnectSatelliteId(boolean prev) {
		final HashMap<Router, Orientations> routes = getRouter().getRouteTable();
		int closestIdFound = prev ? 0 : Integer.MAX_VALUE;
		for (final BaseLogicSatellite satellite : BaseLogicSatellite.AllSatellites) {
			if (routes.containsKey(satellite.getRouter())) {
				if (!prev && satellite.satelliteId > satelliteId && satellite.satelliteId < closestIdFound) {
					closestIdFound = satellite.satelliteId;
				} else if (prev && satellite.satelliteId < satelliteId && satellite.satelliteId > closestIdFound) {
					closestIdFound = satellite.satelliteId;
				}
			}
		}
		if (closestIdFound == Integer.MAX_VALUE) {
			return satelliteId;
		}

		return closestIdFound;

	}

	public void setNextSatellite(EntityPlayer player) {
		satelliteId = getNextConnectSatelliteId(false);
		if (MainProxy.isClient()) {
			final PacketCoordinates packet = new PacketCoordinates(NetworkConstants.CRAFTING_PIPE_NEXT_SATELLITE, xCoord, yCoord, zCoord);
			PacketDispatcher.sendPacketToServer(packet.getPacket());
		} else {
			final PacketPipeInteger packet = new PacketPipeInteger(NetworkConstants.CRAFTING_PIPE_SATELLITE_ID, xCoord, yCoord, zCoord, satelliteId);
			PacketDispatcher.sendPacketToPlayer(packet.getPacket(), player);
		}

	}
	
	// This is called by the packet PacketCraftingPipeSatelliteId
	public void setSatelliteId(int satelliteId) {
		this.satelliteId = satelliteId;
	}

	public void setPrevSatellite(EntityPlayer player) {
		satelliteId = getNextConnectSatelliteId(true);
		if (MainProxy.isClient()) {
			final PacketCoordinates packet = new PacketCoordinates(NetworkConstants.CRAFTING_PIPE_PREV_SATELLITE, xCoord, yCoord, zCoord);
			PacketDispatcher.sendPacketToServer(packet.getPacket());
		} else {
			final PacketPipeInteger packet = new PacketPipeInteger(NetworkConstants.CRAFTING_PIPE_SATELLITE_ID, xCoord, yCoord, zCoord, satelliteId);
			PacketDispatcher.sendPacketToPlayer(packet.getPacket(), player);
		}
	}

	public boolean isSatelliteConnected() {
		for (final BaseLogicSatellite satellite : BaseLogicSatellite.AllSatellites) {
			if (satellite.satelliteId == satelliteId) {
				if (getRouter().getRouteTable().containsKey(satellite.getRouter())) {
					return true;
				}
			}
		}
		return false;
	}

	public IRouter getSatelliteRouter() {
		for (final BaseLogicSatellite satellite : BaseLogicSatellite.AllSatellites) {
			if (satellite.satelliteId == satelliteId) {
				return satellite.getRouter();
			}
		}
		return null;
	}

	/* ** OTHER CODE ** */

	/*public int RequestsItem(ItemIdentifier item) {
		if (item == null) {
			return 0;
		}
		return _dummyInvUtil.getItemCount(item);
	}*/

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		_dummyInventory.readFromNBT(nbttagcompound, "");
		satelliteId = nbttagcompound.getInteger("satelliteid");
		signEntityX = nbttagcompound.getInteger("CraftingSignEntityX");
		signEntityY = nbttagcompound.getInteger("CraftingSignEntityY");
		signEntityZ = nbttagcompound.getInteger("CraftingSignEntityZ");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		_dummyInventory.writeToNBT(nbttagcompound, "");
		nbttagcompound.setInteger("satelliteid", satelliteId);
		
		nbttagcompound.setInteger("CraftingSignEntityX", signEntityX);
		nbttagcompound.setInteger("CraftingSignEntityY", signEntityY);
		nbttagcompound.setInteger("CraftingSignEntityZ", signEntityZ);
	}

	@Override
	public void destroy() {
		if(signEntityX != 0 && signEntityY != 0 && signEntityZ != 0) {
			worldObj.setBlockWithNotify(signEntityX, signEntityY, signEntityZ, 0);
			signEntityX = 0;
			signEntityY = 0;
			signEntityZ = 0;
		}
	}

	@Override
	public void onWrenchClicked(EntityPlayer entityplayer) {
	}

	@Override
	public void throttledUpdateEntity() {
		super.throttledUpdateEntity();
		if (_lostItems.isEmpty()) {
			return;
		}
		System.out.println("Item lost");
		final Iterator<ItemIdentifier> iterator = _lostItems.iterator();
		while (iterator.hasNext()) {
			final LogisticsRequest request = new LogisticsRequest(iterator.next(), 1, getRoutedPipe());
			if (LogisticsManager.Request(request, ((RoutedPipe) container.pipe).getRouter().getRoutersByCost(), null)) {
				iterator.remove();
			}
		}
	}

	@Override
	public void itemArrived(ItemIdentifier item) {
	}

	@Override
	public void itemLost(ItemIdentifier item) {
		_lostItems.add(item);
	}

	public void openAttachedGui(EntityPlayer player) {
		if (MainProxy.isServer()) {
			final PacketCoordinates packet = new PacketCoordinates(NetworkConstants.CRAFTING_PIPE_OPEN_CONNECTED_GUI, xCoord, yCoord, zCoord);
			PacketDispatcher.sendPacketToServer(packet.getPacket());
		}
		final WorldUtil worldUtil = new WorldUtil(worldObj, xCoord, yCoord, zCoord);
		boolean found = false;
		for (final AdjacentTile tile : worldUtil.getAdjacentTileEntities()) {
			for (ICraftingRecipeProvider provider : SimpleServiceLocator.craftingRecipeProviders) {
				if (provider.canOpenGui(tile.tile)) {
					found = true;
					break;
				}
			}

			if (!found)
				found = (tile.tile instanceof IInventory && !(tile.tile instanceof TileGenericPipe));

			if (found) {
				Block block = worldObj.getBlockId(tile.tile.xCoord, tile.tile.yCoord, tile.tile.zCoord) < Block.blocksList.length ? Block.blocksList[worldObj.getBlockId(tile.tile.xCoord, tile.tile.yCoord, tile.tile.zCoord)] : null;
				if(block != null) {
					if(block.onBlockActivated(worldObj, tile.tile.xCoord, tile.tile.yCoord, tile.tile.zCoord, player, 0, 0, 0, 0)){
						break;
					}
				}
			}
		}
	}

	public void importFromCraftingTable(EntityPlayer player) {
		final WorldUtil worldUtil = new WorldUtil(worldObj, xCoord, yCoord, zCoord);
		for (final AdjacentTile tile : worldUtil.getAdjacentTileEntities()) {
			for (ICraftingRecipeProvider provider : SimpleServiceLocator.craftingRecipeProviders) {
				if (provider.importRecipe(tile.tile, _dummyInventory))
					return;
			}
		}
		
		if (MainProxy.isClient()) {
			// Send packet asking for import
			final PacketCoordinates packet = new PacketCoordinates(NetworkConstants.CRAFTING_PIPE_IMPORT, xCoord, yCoord, zCoord);
			PacketDispatcher.sendPacketToServer(packet.getPacket());
		} else{
			// Send inventory as packet
			final PacketInventoryChange packet = new PacketInventoryChange(NetworkConstants.CRAFTING_PIPE_IMPORT_BACK, xCoord, yCoord, zCoord, _dummyInventory);
			PacketDispatcher.sendPacketToPlayer(packet.getPacket(), player);

		}
	}

	/* ** INTERFACE TO PIPE ** */
	public ItemStack getCraftedItem() {
		return _dummyInventory.getStackInSlot(9);
	}

	public ItemStack getMaterials(int slotnr) {
		return _dummyInventory.getStackInSlot(slotnr);
	}

	/**
	 * Simply get the dummy inventory
	 * 
	 * @return the dummy inventory
	 */
	public SimpleInventory getDummyInventory() {
		return _dummyInventory;
	}
	
	public void setDummyInventorySlot(int slot, ItemStack itemstack) {
		_dummyInventory.setInventorySlotContents(slot, itemstack);
	}

	/* ** NON NETWORKING ** */
	public void paintPathToSatellite() {
		final IRouter satelliteRouter = getSatelliteRouter();
		if (satelliteRouter == null) {
			return;
		}

		getRouter().displayRouteTo(satelliteRouter);
	}


}