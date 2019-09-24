package xzot1k.plugins.sp.api;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BlockIterator;
import xzot1k.plugins.sp.SimplePortals;
import xzot1k.plugins.sp.api.enums.PointType;
import xzot1k.plugins.sp.api.objects.Portal;
import xzot1k.plugins.sp.api.objects.Region;
import xzot1k.plugins.sp.api.objects.SerializableLocation;
import xzot1k.plugins.sp.core.objects.TaskHolder;
import xzot1k.plugins.sp.core.packets.jsonmsgs.JSONHandler;
import xzot1k.plugins.sp.core.packets.jsonmsgs.versions.*;
import xzot1k.plugins.sp.core.packets.particles.ParticleHandler;
import xzot1k.plugins.sp.core.packets.particles.versions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class Manager {
	private SimplePortals pluginInstance;
	private HashMap<UUID, Region> currentSelections;
	private HashMap<UUID, Boolean> selectionMode;
	private HashMap<UUID, Long> playerPortalCooldowns;
	private List<Portal> portals;
	private HashMap<UUID, TaskHolder> visualTasks;
	private HashMap<UUID, SerializableLocation> smartTransferMap;
	private HashMap<UUID, Location> quitLocations;

	private ParticleHandler particleHandler;
	private JSONHandler jsonHandler;

	public Manager(SimplePortals pluginInstance) {
		this.pluginInstance = pluginInstance;
		currentSelections = new HashMap<>();
		selectionMode = new HashMap<>();
		playerPortalCooldowns = new HashMap<>();
		visualTasks = new HashMap<>();
		portals = new ArrayList<>();
		smartTransferMap = new HashMap<>();
		quitLocations = new HashMap<>();

		setupPackets();
	}

	private void setupPackets() {
		boolean success = false;
		switch (pluginInstance.getServerVersion()) {
			case "v1_14_R1":
				particleHandler = new PH_Latest();
				setJSONHandler(new JSONHandler1_14R1());
				success = true;
				break;
			case "v1_13_R2":
				particleHandler = new PH_Latest();
				setJSONHandler(new JSONHandler1_13R2());
				success = true;
				break;
			case "v1_13_R1":
				particleHandler = new PH_Latest();
				setJSONHandler(new JSONHandler1_13R1());
				success = true;
				break;
			case "v1_12_R1":
				particleHandler = new PH1_12R1(pluginInstance);
				setJSONHandler(new JSONHandler1_12R1());
				success = true;
				break;
			case "v1_11_R1":
				particleHandler = new PH1_11R1(pluginInstance);
				setJSONHandler(new JSONHandler1_11R1());
				success = true;
				break;
			case "v1_10_R1":
				particleHandler = new PH1_10R1(pluginInstance);
				setJSONHandler(new JSONHandler1_10R1());
				success = true;
				break;
			case "v1_9_R2":
				particleHandler = new PH1_9R2(pluginInstance);
				setJSONHandler(new JSONHandler1_9R2());
				success = true;
				break;
			case "v1_9_R1":
				particleHandler = new PH1_9R1(pluginInstance);
				setJSONHandler(new JSONHandler1_9R1());
				success = true;
				break;
			case "v1_8_R3":
				particleHandler = new PH1_8R3(pluginInstance);
				setJSONHandler(new JSONHandler1_8R3());
				success = true;
				break;
			case "v1_8_R2":
				particleHandler = new PH1_8R2(pluginInstance);
				setJSONHandler(new JSONHandler1_8R2());
				success = true;
				break;
			case "v1_8_R1":
				particleHandler = new PH1_8R1(pluginInstance);
				setJSONHandler(new JSONHandler1_8R1());
				success = true;
				break;
			default:
				break;
		}

		if (success)
			pluginInstance.log(Level.INFO,
					"All packets have been successfully setup for " + pluginInstance.getServerVersion() + "!");
		else
			pluginInstance.log(Level.WARNING, "Your server version (" + pluginInstance.getServerVersion()
					+ ") is not supported. Most packet features will be disabled.");

	}

	public boolean isNumeric(String string) {
		return string.matches("-?\\d+(\\.\\d+)?");
	}

	public String colorText(String text) {
		return ChatColor.translateAlternateColorCodes('&', text);
	}

	public boolean updateCurrentSelection(Player player, Location location, PointType pointType) {
		if (!getCurrentSelections().isEmpty() && getCurrentSelections().containsKey(player.getUniqueId())) {
			Region region = getCurrentSelections().get(player.getUniqueId());
			if (region != null) {
				switch (pointType) {
					case POINT_ONE:
						region.setPoint1(location);
						break;
					case POINT_TWO:
						region.setPoint2(location);
						break;
					default:
						break;
				}

				if (!region.getPoint1().getWorldName().equalsIgnoreCase(region.getPoint2().getWorldName())) {
					player.sendMessage(
							pluginInstance.getManager().colorText(pluginInstance.getConfig().getString("prefix")
									+ pluginInstance.getConfig().getString("not-same-world-message")));
					return false;
				}

				getCurrentSelections().put(player.getUniqueId(), region);
				return true;
			}
		}

		Region region = null;
		switch (pointType) {
			case POINT_ONE:
			case POINT_TWO:
				region = new Region(pluginInstance, location, location);
				break;
			default:
				break;
		}

		if (!region.getPoint1().getWorldName().equalsIgnoreCase(region.getPoint2().getWorldName())) {
			player.sendMessage(pluginInstance.getManager().colorText(pluginInstance.getConfig().getString("prefix")
					+ pluginInstance.getConfig().getString("not-same-world-message")));
			return false;
		}

		getCurrentSelections().put(player.getUniqueId(), region);
		return true;
	}

	public Region getCurrentSelection(Player player) {
		if (!getCurrentSelections().isEmpty() && getCurrentSelections().containsKey(player.getUniqueId()))
			return getCurrentSelections().get(player.getUniqueId());
		return null;
	}

	public void clearCurrentSelection(Player player) {
		if (!getCurrentSelections().isEmpty())
			getCurrentSelections().remove(player.getUniqueId());
	}

	public void setSelectionMode(Player player, boolean selectionMode) {
		getSelectionMode().put(player.getUniqueId(), selectionMode);
	}

	public boolean isInSelectionMode(Player player) {
		if (!getSelectionMode().isEmpty() && getSelectionMode().containsKey(player.getUniqueId()))
			return getSelectionMode().get(player.getUniqueId());
		return false;
	}

	public void updatePlayerPortalCooldown(Player player) {
		getPlayerPortalCooldowns().put(player.getUniqueId(), System.currentTimeMillis());
	}

	public boolean isPlayerOnCooldown(Player player) {
		if (!getPlayerPortalCooldowns().isEmpty() && getPlayerPortalCooldowns().containsKey(player.getUniqueId()))
			return getCooldownTimeLeft(player) > 0;
		return false;
	}

	public long getCooldownTimeLeft(Player player) {
		int cooldown = pluginInstance.getConfig().getInt("portal-cooldown-duration");
		if (cooldown >= 0)
			if (!getPlayerPortalCooldowns().isEmpty() && getPlayerPortalCooldowns().containsKey(player.getUniqueId()))
				return ((getPlayerPortalCooldowns().get(player.getUniqueId()) / 1000) + cooldown)
						- (System.currentTimeMillis() / 1000);
		return 0;
	}

	public Portal getPortalAtLocation(Location location) {
		for (int i = -1; ++i < getPortals().size(); ) {
			Portal portal = getPortals().get(i);
			if (portal.getRegion().isInRegion(location))
				return portal;
		}

		return null;
	}

	public Portal getPortalById(String portalName) {
		for (int i = -1; ++i < getPortals().size(); ) {
			Portal portal = getPortals().get(i);
			if (portal.getPortalId().equalsIgnoreCase(portalName))
				return portal;
		}

		return null;
	}

	public boolean doesPortalExist(String portalName) {
		for (int i = -1; ++i < getPortals().size(); ) {
			Portal portal = getPortals().get(i);
			if (portal.getPortalId().equalsIgnoreCase(portalName))
				return true;
		}

		return false;
	}

	public void teleportPlayerWithEntity(Player player, Location location) {
		teleportPlayerWithEntity(player, location, false);
	}

	@SuppressWarnings("deprecation")
	public void teleportPlayerWithEntity(Player player, Location location, boolean onQuit) {
		// Teleport Immediately
		if (!onQuit) {
			if (player.getVehicle() != null && pluginInstance.getConfig().getBoolean("vehicle-teleportation")) {
				Entity entity = player.getVehicle();
				if (pluginInstance.getServerVersion().startsWith("v1_11")
						|| pluginInstance.getServerVersion().startsWith("v1_12")
						|| pluginInstance.getServerVersion().startsWith("v1_13")
						|| pluginInstance.getServerVersion().startsWith("v1_14"))
					entity.removePassenger(player);
				else
					entity.setPassenger(null);
				if (entity.getPassengers().contains(player))
					entity.eject();

				player.teleport(location);
				new BukkitRunnable() {
					@Override
					public void run() {
						entity.teleport(player.getLocation());
						entity.addPassenger(player);
					}
				}.runTaskLater(pluginInstance, 1);
			} else
				player.teleport(location);
			return;
		}

		// Set teleport on quit
		quitLocations.put(player.getUniqueId(), location);
	}

	public boolean isFacingPortal(Player player, Portal portal, int range) {
		BlockIterator blockIterator = new BlockIterator(player, range);
		Block lastBlock;

		boolean foundPortal = false;
		while (blockIterator.hasNext()) {
			lastBlock = blockIterator.next();
			if (!portal.getRegion().isInRegion(lastBlock.getLocation()))
				continue;

			foundPortal = true;
			break;
		}

		return foundPortal;
	}

	public String getDirection(double yaw) {
		if (yaw < 0)
			yaw += 360;
		if (yaw >= 315 || yaw < 45)
			return "SOUTH";
		else if (yaw < 135)
			return "WEST";
		else if (yaw < 225)
			return "NORTH";
		else if (yaw < 315)
			return "EAST";
		return "NORTH";
	}

	public void highlightBlock(Block block, Player player, PointType pointType) {
		if (particleHandler == null)
			return;

		String particleEffect = Objects.requireNonNull(pluginInstance.getConfig().getString("selection-visual-effect"))
				.toUpperCase().replace(" ", "_").replace("-", "_");

		BukkitTask bukkitTask = new BukkitRunnable() {
			int duration = pluginInstance.getConfig().getInt("selection-visual-duration");
			double lifetime = 0;
			Location blockLocation = block.getLocation().clone();

			@Override
			public void run() {
				if (lifetime >= duration) {
					cancel();
					return;
				}

				for (double y = blockLocation.getBlockY() - 0.2; (y += 0.2) < (blockLocation.getBlockY() + 1.1); )
					for (double x = blockLocation.getBlockX() - 0.2; (x += 0.2) < (blockLocation.getBlockX() + 1.1); )
						for (double z = blockLocation.getBlockZ() - 0.2; (z += 0.2) < (blockLocation.getBlockZ()
								+ 1.1); ) {
							Location location = new Location(blockLocation.getWorld(), x, y, z);

							if ((y < (blockLocation.getBlockY() + 0.2) || y > (blockLocation.getBlockY() + 0.9))
									&& (z < (blockLocation.getBlockZ() + 0.2) || z > (blockLocation.getBlockZ() + 0.9)))
								particleHandler.displayParticle(player, location, 0, 0, 0, 0, particleEffect, 1);

							if ((x < (blockLocation.getBlockX() + 0.2) || x > (blockLocation.getBlockX() + 0.9))
									&& (z < (blockLocation.getBlockZ() + 0.2) || z > (blockLocation.getBlockZ() + 0.9)))
								particleHandler.displayParticle(player, location, 0, 0, 0, 0, particleEffect, 1);

							if ((y < (blockLocation.getBlockY() + 0.2) || y > (blockLocation.getBlockY() + 0.9))
									&& (x < (blockLocation.getBlockX() + 0.2) || x > (blockLocation.getBlockX() + 0.9)))
								particleHandler.displayParticle(player, location, 0, 0, 0, 0, particleEffect, 1);
						}

				lifetime += 0.25;
			}
		}.runTaskTimer(pluginInstance, 0, 5);

		if (!getVisualTasks().isEmpty() && getVisualTasks().containsKey(player.getUniqueId())) {
			TaskHolder taskHolder = getVisualTasks().get(player.getUniqueId());
			if (taskHolder != null) {
				if (taskHolder.getRegionDisplay() != null)
					taskHolder.getRegionDisplay().cancel();
				if (pointType == PointType.POINT_ONE)
					taskHolder.setSelectionPointOne(bukkitTask);
				else
					taskHolder.setSelectionPointTwo(bukkitTask);
				return;
			}
		}

		TaskHolder taskHolder = new TaskHolder();
		if (pointType == PointType.POINT_ONE)
			taskHolder.setSelectionPointOne(bukkitTask);
		else
			taskHolder.setSelectionPointTwo(bukkitTask);
		getVisualTasks().put(player.getUniqueId(), taskHolder);
	}

	public void loadPortals() {
		getPortals().clear();
		List<String> portalIds = new ArrayList<>(
				Objects.requireNonNull(pluginInstance.getPortalsConfig().getConfigurationSection("")).getKeys(false));
		if (!portalIds.isEmpty())
			for (int i = -1; ++i < portalIds.size(); ) {
				String portalId = portalIds.get(i);
				if (doesPortalExist(portalId))
					return;

				try {
					String pointOneWorld = pluginInstance.getPortalsConfig().getString(portalId + ".point-1.world"),
							pointTwoWorld = pluginInstance.getPortalsConfig().getString(portalId + ".point-2.world"),
							teleportWorld = pluginInstance.getPortalsConfig()
									.getString(portalId + ".teleport-location.world");

					if (pluginInstance.getServer().getWorld(pointOneWorld) == null
							|| pluginInstance.getServer().getWorld(pointTwoWorld) == null
							|| pluginInstance.getServer().getWorld(teleportWorld) == null) {
						pluginInstance.log(Level.WARNING, "The portal '" + portalId
								+ "' was skipped and not loaded due to a invalid or missing world.");
						continue;
					}

					SerializableLocation teleportPoint1 = new SerializableLocation(pluginInstance, pointOneWorld,
							pluginInstance.getPortalsConfig().getDouble(portalId + ".point-1.x"),
							pluginInstance.getPortalsConfig().getDouble(portalId + ".point-1.y"),
							pluginInstance.getPortalsConfig().getDouble(portalId + ".point-1.z"),
							pluginInstance.getPortalsConfig().getDouble(portalId + ".point-1.yaw"),
							pluginInstance.getPortalsConfig().getDouble(portalId + ".point-1.pitch")),

							teleportPoint2 = new SerializableLocation(pluginInstance, pointTwoWorld,
									pluginInstance.getPortalsConfig().getDouble(portalId + ".point-2.x"),
									pluginInstance.getPortalsConfig().getDouble(portalId + ".point-2.y"),
									pluginInstance.getPortalsConfig().getDouble(portalId + ".point-2.z"),
									pluginInstance.getPortalsConfig().getDouble(portalId + ".point-2.yaw"),
									pluginInstance.getPortalsConfig().getDouble(portalId + ".point-2.pitch"));
					Region region = new Region(pluginInstance, teleportPoint1, teleportPoint2);
					Portal portal = new Portal(pluginInstance, portalId, region);

					SerializableLocation tpLocation = new SerializableLocation(pluginInstance, teleportWorld,
							pluginInstance.getPortalsConfig().getDouble(portalId + ".teleport-location.x"),
							pluginInstance.getPortalsConfig().getDouble(portalId + ".teleport-location.y"),
							pluginInstance.getPortalsConfig().getDouble(portalId + ".teleport-location.z"),
							pluginInstance.getPortalsConfig().getDouble(portalId + ".teleport-location.yaw"),
							pluginInstance.getPortalsConfig().getDouble(portalId + ".teleport-location.pitch"));
					portal.setTeleportLocation(tpLocation);
					portal.setServerSwitchName(
							pluginInstance.getPortalsConfig().getString(portalId + ".portal-server"));
					portal.setCommandsOnly(pluginInstance.getPortalsConfig().getBoolean(portalId + ".commands-only"));
					portal.setCommands(pluginInstance.getPortalsConfig().getStringList(portalId + ".commands"));

					String materialName = pluginInstance.getPortalsConfig().getString(portalId + ".last-fill-material");
					if (materialName != null && !materialName.equalsIgnoreCase("")) {
						Material material = Material
								.getMaterial(materialName.toUpperCase().replace(" ", "_").replace("-", "_"));
						portal.setLastFillMaterial(material == null ? Material.AIR : material);
					} else
						portal.setLastFillMaterial(Material.AIR);

					portal.register();
				} catch (Exception ignored) {
					pluginInstance.log(Level.WARNING,
							"The portal " + portalId
									+ " was unable to be loaded. Please check its information in the portals.yml. "
									+ "This could be something as simple as a missing or invalid world.");
				}
			}

		File dir = new File(pluginInstance.getDataFolder(), "/portals");
		if (!dir.exists())
			return;
		File[] files = dir.listFiles();
		if (files == null || files.length <= 0)
			return;

		for (int i = -1; ++i < files.length; ) {
			File file = files[i];
			if (file != null && file.getName().toLowerCase().endsWith(".yml")) {
				YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
				if (doesPortalExist(yamlConfiguration.getString("portal-id")))
					return;

				try {
					SerializableLocation teleportPoint1 = new SerializableLocation(pluginInstance,
							yamlConfiguration.getString("point-1.world"), yamlConfiguration.getDouble("point-1.x"),
							yamlConfiguration.getDouble("point-1.y"), yamlConfiguration.getDouble("point-1.z"),
							yamlConfiguration.getDouble("point-1.yaw"), yamlConfiguration.getDouble("point-1.pitch")),
							teleportPoint2 = new SerializableLocation(pluginInstance,
									yamlConfiguration.getString("point-2.world"),
									yamlConfiguration.getDouble("point-2.x"), yamlConfiguration.getDouble("point-2.y"),
									yamlConfiguration.getDouble("point-2.z"),
									yamlConfiguration.getDouble("point-2.yaw"),
									yamlConfiguration.getDouble("point-2.pitch"));
					Region region = new Region(pluginInstance, teleportPoint1, teleportPoint2);
					Portal portal = new Portal(pluginInstance, yamlConfiguration.getString("portal-id"), region);

					SerializableLocation tpLocation = new SerializableLocation(pluginInstance,
							yamlConfiguration.getString("teleport-location.world"),
							yamlConfiguration.getDouble("teleport-location.x"),
							yamlConfiguration.getDouble("teleport-location.y"),
							yamlConfiguration.getDouble("teleport-location.z"),
							yamlConfiguration.getDouble("point-1.yaw"), yamlConfiguration.getDouble("point-1.pitch"));
					portal.setTeleportLocation(tpLocation);
					portal.setServerSwitchName(yamlConfiguration.getString("portal-server"));
					portal.setCommandsOnly(yamlConfiguration.getBoolean("commands-only"));
					portal.setCommands(yamlConfiguration.getStringList("commands"));

					String materialName = yamlConfiguration.getString("last-fill-material");
					if (materialName != null && !materialName.equalsIgnoreCase("")) {
						Material material = Material
								.getMaterial(materialName.toUpperCase().replace(" ", "_").replace("-", "_"));
						portal.setLastFillMaterial(material == null ? Material.AIR : material);
					} else
						portal.setLastFillMaterial(Material.AIR);

					portal.register();
					file.delete();
					pluginInstance.log(Level.INFO,
							"The portal " + portal.getPortalId() + " has been converted over to a v1.2.x portal.");
				} catch (Exception ignored) {
					pluginInstance.log(Level.WARNING, "The file " + file.getName()
							+ " was unable to be converted. Please make sure this is a SimplePortals portal.");
				}
			}
		}

		dir.delete();
		pluginInstance.log(Level.INFO,
				"All old portal files have been removed (All portals are located in the portals.yml).");
		savePortals();
	}

	public void savePortals() {
		for (int i = -1; ++i < getPortals().size(); ) {
			Portal portal = getPortals().get(i);
			portal.save(false);
		}

		pluginInstance.savePortalsConfig();
	}

	public void clearAllVisuals(Player player) {
		if (!getVisualTasks().isEmpty() && getVisualTasks().containsKey(player.getUniqueId())) {
			TaskHolder taskHolder = getVisualTasks().get(player.getUniqueId());
			if (taskHolder.getRegionDisplay() != null)
				taskHolder.getRegionDisplay().cancel();
			if (taskHolder.getSelectionPointOne() != null)
				taskHolder.getSelectionPointOne().cancel();
			if (taskHolder.getSelectionPointTwo() != null)
				taskHolder.getSelectionPointTwo().cancel();
		}
	}

	public boolean switchServer(Player player, String serverName) {
		try {
			Bukkit.getMessenger().registerOutgoingPluginChannel(pluginInstance, "BungeeCord");
			ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(byteArray);
			out.writeUTF("Connect");
			out.writeUTF(serverName);
			player.sendPluginMessage(pluginInstance, "BungeeCord", byteArray.toByteArray());
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			pluginInstance.log(Level.WARNING,
					"There seems to have been a issue when switching the player to the " + serverName + " server.");
			return false;
		}
	}

	private HashMap<UUID, Region> getCurrentSelections() {
		return currentSelections;
	}

	public List<Portal> getPortals() {
		return portals;
	}

	private HashMap<UUID, Boolean> getSelectionMode() {
		return selectionMode;
	}

	private HashMap<UUID, Long> getPlayerPortalCooldowns() {
		return playerPortalCooldowns;
	}

	public ParticleHandler getParticleHandler() {
		return particleHandler;
	}

	public HashMap<UUID, TaskHolder> getVisualTasks() {
		return visualTasks;
	}

	public JSONHandler getJSONHandler() {
		return jsonHandler;
	}

	private void setJSONHandler(JSONHandler jsonHandler) {
		this.jsonHandler = jsonHandler;
	}

	public HashMap<UUID, SerializableLocation> getSmartTransferMap() {
		return smartTransferMap;
	}

	public HashMap<UUID, Location> getQuitLocations() {
		return quitLocations;
	}

}