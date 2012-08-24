/** 
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.buildcraft.krapht.routing;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import logisticspipes.buildcraft.krapht.CoreRoutedPipe;
import logisticspipes.buildcraft.krapht.RoutedPipe;
import logisticspipes.buildcraft.logisticspipes.IRoutedItem;
import logisticspipes.buildcraft.logisticspipes.modules.ILogisticsModule;


import net.minecraft.src.ItemStack;
import buildcraft.api.core.Orientations;
import buildcraft.api.core.Position;

public interface IRouter {
	public void destroy();
	public void update(boolean fullRefresh);
	public void sendRoutedItem(ItemStack item, Router destination, Position origin);
	public boolean isRoutedExit(Orientations connection);
	public boolean hasRoute(UUID id);
	public Orientations getExitFor(UUID id);
	
	@Deprecated
	public HashMap<Router, Orientations> getRouteTable();
	@Deprecated
	public LinkedList<Router> getRoutersByCost();
	public LinkedList<IRouter> getIRoutersByCost();
	@Deprecated
	public LinkedList<Orientations> GetNonRoutedExits();
	@Deprecated
	public CoreRoutedPipe getPipe();
	
	public UUID getId();
	@Deprecated
	public int getInboundItemsCount();
	@Deprecated
	public int getOutboundItemsCount();
	@Deprecated
	public void itemDropped(RoutedEntityItem routedEntityItem);
	@Deprecated
	public void startTrackingRoutedItem(RoutedEntityItem routedEntityItem);
	@Deprecated
	public void startTrackingInboundItem(RoutedEntityItem routedEntityItem);
	@Deprecated
	public void displayRoutes();
	@Deprecated
	public void displayRouteTo(IRouter r);
	@Deprecated
	public void outboundItemArrived(RoutedEntityItem routedEntityItem);
	@Deprecated
	public void inboundItemArrived(RoutedEntityItem routedEntityItem);
	
	public ILogisticsModule getLogisticsModule();
}