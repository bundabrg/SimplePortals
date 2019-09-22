package xzot1k.plugins.sp.core;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import xzot1k.plugins.sp.SimplePortals;
import xzot1k.plugins.sp.api.enums.PointType;
import xzot1k.plugins.sp.api.events.PortalActionEvent;
import xzot1k.plugins.sp.api.events.PortalEnterEvent;
import xzot1k.plugins.sp.api.objects.Portal;
import xzot1k.plugins.sp.api.objects.SerializableLocation;

import java.util.List;
import java.util.Objects;

public class Listeners implements Listener {

	private SimplePortals pluginInstance;

	public Listeners(SimplePortals pluginInstance) {
		this.pluginInstance = pluginInstance;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent e) {
		Portal portalFrom = pluginInstance.getManager().getPortalAtLocation(e.getBlock().getLocation());
		if (portalFrom != null) {
			e.setCancelled(true);
			return;
		}

		Portal portalTo = pluginInstance.getManager().getPortalAtLocation(e.getToBlock().getLocation());
		if (portalTo != null)
			e.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onClick(PlayerInteractEvent e) {
		if (e.getAction() == Action.LEFT_CLICK_BLOCK && e.getClickedBlock() != null
				&& pluginInstance.getManager().isInSelectionMode(e.getPlayer())) {
			e.setCancelled(true);
			if (pluginInstance.getManager().updateCurrentSelection(e.getPlayer(), e.getClickedBlock().getLocation(),
					PointType.POINT_ONE)) {
				pluginInstance.getManager().highlightBlock(e.getClickedBlock(), e.getPlayer(), PointType.POINT_ONE);
				String message = pluginInstance.getConfig().getString("point-1-set-message");
				if (message != null && !message.equalsIgnoreCase(""))
					e.getPlayer().sendMessage(pluginInstance.getManager()
							.colorText(pluginInstance.getConfig().getString("prefix") + message));
			}
		}

		if (e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null
				&& pluginInstance.getManager().isInSelectionMode(e.getPlayer())) {
			e.setCancelled(true);

			if (pluginInstance.getServerVersion().toLowerCase().startsWith("v1_15")
					|| pluginInstance.getServerVersion().toLowerCase().startsWith("v1_14")
					|| pluginInstance.getServerVersion().toLowerCase().startsWith("v1_13")
					|| pluginInstance.getServerVersion().toLowerCase().startsWith("v1_12")
					|| pluginInstance.getServerVersion().toLowerCase().startsWith("v1_11")
					|| pluginInstance.getServerVersion().toLowerCase().startsWith("v1_10")
					|| pluginInstance.getServerVersion().toLowerCase().startsWith("v1_9"))
				if (e.getHand() != EquipmentSlot.HAND)
					return;

			if (pluginInstance.getManager().updateCurrentSelection(e.getPlayer(), e.getClickedBlock().getLocation(),
					PointType.POINT_TWO)) {
				pluginInstance.getManager().highlightBlock(e.getClickedBlock(), e.getPlayer(), PointType.POINT_TWO);

				String message = pluginInstance.getConfig().getString("point-2-set-message");
				if (message != null && !message.equalsIgnoreCase(""))
					e.getPlayer().sendMessage(pluginInstance.getManager()
							.colorText(pluginInstance.getConfig().getString("prefix") + message));
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onMove(PlayerMoveEvent e) {
		if (e.getFrom().getBlockX() != Objects.requireNonNull(e.getTo()).getBlockX()
				|| e.getFrom().getBlockY() != e.getTo().getBlockY()
				|| e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
			Portal portal = pluginInstance.getManager().getPortalAtLocation(e.getTo());
			if (portal != null) {
				PortalEnterEvent portalEnterEvent = new PortalEnterEvent(e.getPlayer(), portal);
				pluginInstance.getServer().getPluginManager().callEvent(portalEnterEvent);
				if (portalEnterEvent.isCancelled())
					return;

				if (pluginInstance.getConfig().getBoolean("use-portal-cooldown")
						&& pluginInstance.getManager().isPlayerOnCooldown(e.getPlayer())
						&& !e.getPlayer().hasPermission("simpleportals.cdbypass")) {
					double tv = pluginInstance.getConfig().getDouble("throw-velocity");
					if (!(tv <= -1))
						e.getPlayer().setVelocity(e.getPlayer().getLocation().getDirection()
								.setY(e.getPlayer().getLocation().getDirection().getY() / 2).multiply(-tv));

					String message = pluginInstance.getConfig().getString("enter-cooldown-message");
					if (message != null && !message.equalsIgnoreCase(""))
						e.getPlayer().sendMessage(pluginInstance.getManager().colorText(
								pluginInstance.getConfig().getString("prefix") + message.replace("{time}", String
										.valueOf(pluginInstance.getManager().getCooldownTimeLeft(e.getPlayer())))));
					return;
				}

				if (!pluginInstance.getConfig().getBoolean("bypass-portal-permissions")
						&& !e.getPlayer().hasPermission("simpleportals.portal." + portal.getPortalId())
						&& !e.getPlayer().hasPermission("simpleportals.portals." + portal.getPortalId())
						&& !e.getPlayer().hasPermission("simpleportals.portal.*")
						&& !e.getPlayer().hasPermission("simpleportals.portals.*")) {
					double tv = pluginInstance.getConfig().getDouble("throw-velocity");
					if (!(tv <= -1))
						e.getPlayer().setVelocity(e.getPlayer().getLocation().getDirection()
								.setY(e.getPlayer().getLocation().getDirection().getY() / 2).multiply(-tv));

					String message = pluginInstance.getConfig().getString("enter-no-permission-message");
					if (message != null && !message.equalsIgnoreCase(""))
						e.getPlayer().sendMessage(pluginInstance.getManager()
								.colorText(pluginInstance.getConfig().getString("prefix") + message));
					return;
				}

				PortalActionEvent portalActionEvent = new PortalActionEvent(e.getPlayer(), portal, e.getFrom(),
						portal.getTeleportLocation().asBukkitLocation());
				pluginInstance.getServer().getPluginManager().callEvent(portalActionEvent);
				if (portalActionEvent.isCancelled())
					return;

				for (int i = -1; ++i < portal.getCommands().size(); ) {
					String commandLine = portal.getCommands().get(i);
					if (commandLine.toUpperCase().endsWith(":PLAYER")) {
						commandLine = commandLine.replaceAll("(?i):PLAYER", "").replaceAll("(?i):CONSOLE", "")
								.replaceAll("(?i):CHAT", "");
						pluginInstance.getServer().dispatchCommand(e.getPlayer(),
								commandLine.replace("{player}", e.getPlayer().getName()));
					} else if (commandLine.toUpperCase().endsWith(":CONSOLE")) {
						commandLine = commandLine.replaceAll("(?i):PLAYER", "").replaceAll("(?i):CONSOLE", "")
								.replaceAll("(?i):CHAT", "");
						pluginInstance.getServer().dispatchCommand(pluginInstance.getServer().getConsoleSender(),
								commandLine.replace("{player}", e.getPlayer().getName()));
					} else if (commandLine.toUpperCase().endsWith(":CHAT")) {
						commandLine = commandLine.replaceAll("(?i):PLAYER", "").replaceAll("(?i):CONSOLE", "")
								.replaceAll("(?i):CHAT", "");
						e.getPlayer().chat(commandLine.replace("{player}", e.getPlayer().getName()));
					}
				}

				if (!portal.isCommandsOnly()) {
					String particleEffect = pluginInstance.getConfig().getString("teleport-visual-effect");
					if (particleEffect != null && !particleEffect.equalsIgnoreCase(""))
						pluginInstance.getManager().getParticleHandler().broadcastParticle(e.getPlayer().getLocation(),
								1, 2, 1, 0, particleEffect.toUpperCase().replace(" ", "_").replace("-", "_"), 50);

					String sound = pluginInstance.getConfig().getString("teleport-sound");
					if (sound != null && !sound.equalsIgnoreCase(""))
						e.getPlayer().getWorld().playSound(e.getPlayer().getLocation(),
								Sound.valueOf(sound.toUpperCase().replace(" ", "_").replace("-", "_")), 1, 1);

					if (pluginInstance.getConfig().getBoolean("use-portal-cooldown")
							&& !e.getPlayer().hasPermission("simpleportals.cdbypass"))
						pluginInstance.getManager().updatePlayerPortalCooldown(e.getPlayer());

					String message = pluginInstance.getConfig().getString("portal-message");
					if (message != null && !message.equalsIgnoreCase(""))
						e.getPlayer().sendMessage(pluginInstance.getManager()
								.colorText(pluginInstance.getConfig().getString("prefix") + message
										.replace("{name}", portal.getPortalId()).replace("{time}", String.valueOf(
												pluginInstance.getManager().getCooldownTimeLeft(e.getPlayer())))));

					portal.performAction(e.getPlayer());
				}
			} else {
				if (!pluginInstance.getManager().getSmartTransferMap().isEmpty()
						&& pluginInstance.getManager().getSmartTransferMap().containsKey(e.getPlayer().getUniqueId())) {
					SerializableLocation serializableLocation = pluginInstance.getManager().getSmartTransferMap()
							.get(e.getPlayer().getUniqueId());
					if (serializableLocation != null) {
						Location location = e.getPlayer().getLocation();
						serializableLocation.setWorldName(Objects.requireNonNull(location.getWorld()).getName());
						serializableLocation.setX(location.getX());
						serializableLocation.setY(location.getY());
						serializableLocation.setZ(location.getZ());
						serializableLocation.setYaw(location.getYaw());
						serializableLocation.setPitch(location.getPitch());
						return;
					}
				}

				pluginInstance.getManager().getSmartTransferMap().put(e.getPlayer().getUniqueId(),
						new SerializableLocation(pluginInstance, e.getFrom()));
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent e) {
		pluginInstance.getManager().getSmartTransferMap().remove(e.getPlayer().getUniqueId());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onTeleport(PlayerPortalEvent e) {
		if (e.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL
				|| e.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL
				|| e.getCause().name().equalsIgnoreCase("END_GATEWAY")) {
			Portal portal = pluginInstance.getManager().getPortalAtLocation(e.getFrom());
			if (portal != null) {
				e.setCancelled(true);
				return;
			}

			boolean foundPortal = false;
			for (int x = (e.getFrom().getBlockX() - 5) - 1; ++x <= e.getFrom().getBlockX() + 5; ) {
				if (foundPortal)
					break;
				for (int y = (e.getFrom().getBlockY() - 5) - 1; ++y <= e.getFrom().getBlockY() + 5; ) {
					if (foundPortal)
						break;
					for (int z = (e.getFrom().getBlockZ() - 5) - 1; ++z <= e.getFrom().getBlockZ() + 5; ) {
						Location location = new Location(e.getFrom().getWorld(), x, y, z);
						Portal p = pluginInstance.getManager().getPortalAtLocation(location);
						if (p != null) {
							e.setCancelled(true);
							foundPortal = true;
							break;
						}
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onSpawn(CreatureSpawnEvent e) {
		List<String> blockedMobs = pluginInstance.getConfig().getStringList("creature-spawning-blacklist");
		for (int i = -1; ++i < blockedMobs.size(); ) {
			String blockedMob = blockedMobs.get(i);
			if (blockedMob.replace(" ", "_").replace("-", "_").equalsIgnoreCase(e.getEntity().getType().name())) {
				if (pluginInstance.getManager().getPortalAtLocation(e.getLocation()) != null)
					e.setCancelled(true);
				break;
			}
		}
	}

}
