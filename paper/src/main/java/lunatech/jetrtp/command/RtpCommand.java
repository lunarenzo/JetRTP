package lunatech.jetrtp.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.RtpProfile;
import lunatech.jetrtp.service.RtpService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public final class RtpCommand extends Command {
    private final AbstractJetRTP plugin;
    private final RtpService rtpService;

    public RtpCommand(AbstractJetRTP plugin, RtpService rtpService) {
        this.plugin = plugin;
        this.rtpService = rtpService;
    }

    @Override
    public CommandAPICommand command() {
        return new CommandAPICommand("rtp")
            .withAliases("wild")
            .withHelp("Randomly teleport.", "Teleports you to a random safe location.")
            .withPermission("jakesrtp.use")
            .withOptionalArguments(
                new StringArgument("profile").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    CommandSender sender = info.sender();
                    if (!(sender instanceof Player player)) {
                        return new String[0];
                    }
                    List<String> suggestions = new ArrayList<>();
                    for (String key : plugin.getConfigHandler().getProfiles().keySet()) {
                        if (player.hasPermission("jakesrtp.usebyname") || player.hasPermission("jakesrtp.use." + key.toLowerCase())) {
                            suggestions.add(key);
                        }
                    }
                    return suggestions.toArray(new String[0]);
                }))
            )
            .executesPlayer(this::executeRtp)
            .withSubcommand(new CommandAPICommand("reload")
                .withPermission("jakesrtp.admin.reload")
                .executes((sender, args) -> {
                    if (plugin instanceof lunatech.jetrtp.JetRTP jetRtp) {
                        jetRtp.onReload();
                    }
                    sender.sendMessage("§aJetRTP profiles and configuration reloaded.");
                })
            )
            .withSubcommand(new CommandAPICommand("force")
                .withPermission("jakesrtp.admin.force")
                .withSubcommand(new CommandAPICommand("config")
                    .withArguments(new dev.jorel.commandapi.arguments.EntitySelectorArgument.OnePlayer("target"))
                    .withArguments(new StringArgument("profile").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                        return plugin.getConfigHandler().getProfiles().keySet().toArray(new String[0]);
                    })))
                    .executes((sender, args) -> {
                        Player target = (Player) args.get("target");
                        if (target == null) {
                            sender.sendMessage("§cTarget player not found.");
                            return;
                        }
                        String profileName = (String) args.get("profile");
                        RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                        if (profile == null) {
                            sender.sendMessage("§cUnknown RTP profile: " + profileName);
                            return;
                        }
                        sender.sendMessage("§aForcefully executing random teleport for " + target.getName() + " using profile " + profile.name);
                        rtpService.executeRtp(target, profile).thenAccept(success -> {
                            if (success) {
                                sender.sendMessage("§aRandomly teleported " + target.getName() + " successfully.");
                            } else {
                                sender.sendMessage("§cFailed to randomly teleport " + target.getName());
                            }
                        });
                    })
                )
                .withSubcommand(new CommandAPICommand("world")
                    .withArguments(new dev.jorel.commandapi.arguments.EntitySelectorArgument.OnePlayer("target"))
                    .withArguments(new StringArgument("world").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                        return plugin.getServer().getWorlds().stream().map(org.bukkit.World::getName).toArray(String[]::new);
                    })))
                    .executes((sender, args) -> {
                        Player target = (Player) args.get("target");
                        if (target == null) {
                            sender.sendMessage("§cTarget player not found.");
                            return;
                        }
                        String worldName = (String) args.get("world");
                        RtpProfile profile = plugin.getConfigHandler().getProfiles().values().stream()
                            .filter(p -> p.landingWorld.equalsIgnoreCase(worldName))
                            .findFirst().orElse(null);

                        if (profile == null) {
                            RtpProfile defaultProfile = plugin.getConfigHandler().getProfiles().get("default-settings");
                            if (defaultProfile == null) {
                                defaultProfile = plugin.getConfigHandler().getProfiles().values().stream().findFirst().orElse(null);
                            }
                            if (defaultProfile != null) {
                                try {
                                    profile = new RtpProfile();
                                    profile.name = defaultProfile.name;
                                    profile.bounds = defaultProfile.bounds;
                                    profile.checkRadius = defaultProfile.checkRadius;
                                    profile.distribution = defaultProfile.distribution;
                                    profile.landingWorld = worldName;
                                    profile.maxAttempts = defaultProfile.maxAttempts;
                                    profile.preparations = defaultProfile.preparations;
                                    profile.warmup = defaultProfile.warmup;
                                    profile.thenExecute = defaultProfile.thenExecute;
                                    profile.cost = 0;
                                } catch (Exception e) {
                                    profile = defaultProfile;
                                }
                            }
                        }

                        if (profile == null) {
                            sender.sendMessage("§cNo RTP profile could be resolved.");
                            return;
                        }

                        sender.sendMessage("§aForcefully executing random teleport for " + target.getName() + " landing in world " + worldName);
                        rtpService.executeRtp(target, profile).thenAccept(success -> {
                            if (success) {
                                sender.sendMessage("§aRandomly teleported " + target.getName() + " successfully.");
                            } else {
                                sender.sendMessage("§cFailed to randomly teleport " + target.getName());
                            }
                        });
                    })
                )
            )
            .withSubcommand(new CommandAPICommand("info")
                .withPermission("jakesrtp.admin.info")
                .withOptionalArguments(
                    new StringArgument("profile").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                        return plugin.getConfigHandler().getProfiles().keySet().toArray(new String[0]);
                    }))
                )
                .executes((sender, args) -> {
                    String profileName = (String) args.get("profile");
                    if (profileName != null) {
                        RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                        if (profile == null) {
                            sender.sendMessage("§cUnknown RTP profile: " + profileName);
                            return;
                        }
                        sender.sendMessage("§8=== §aJetRTP Profile Info: " + profile.name + " §8===");
                        sender.sendMessage("§7Enabled: §f" + profile.enabled);
                        sender.sendMessage("§7Landing World: §f" + profile.landingWorld);
                        sender.sendMessage("§7Cache Size: §f" + plugin.getCacheService().getCacheSize(profile) + " / " + profile.preparations.cacheLocations);
                        sender.sendMessage("§7Cooldown Time: §f" + profile.cooldown + "s");
                        sender.sendMessage("§7Cost: §f$" + profile.cost);
                        sender.sendMessage("§7Bounds: §fY " + profile.bounds.low + " to " + profile.bounds.high);
                        sender.sendMessage("§7Excluded Biomes: §f" + String.join(", ", profile.excludedBiomes));
                    } else {
                        sender.sendMessage("§8=== §aJetRTP General Info §8===");
                        sender.sendMessage("§7Loaded Profiles: §f" + plugin.getConfigHandler().getProfiles().size());
                        sender.sendMessage("§7Database Active: §f" + lunatech.jetrtp.utility.DB.isStarted());
                        for (RtpProfile profile : plugin.getConfigHandler().getProfiles().values()) {
                            int size = plugin.getCacheService().getCacheSize(profile);
                            sender.sendMessage("§8- §a" + profile.name + "§7: Cache = §f" + size + "/" + profile.preparations.cacheLocations);
                        }
                    }
                })
            )
            .withSubcommand(new CommandAPICommand("settings")
                .withPermission("jakesrtp.admin.settings")
                .withArguments(new StringArgument("profile").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                    return plugin.getConfigHandler().getProfiles().keySet().toArray(new String[0]);
                })))
                .withArguments(new StringArgument("key").replaceSuggestions(ArgumentSuggestions.strings(
                    "landingworld", "cost", "cooldown", "maxattempts", "bounds-low", "bounds-high"
                )))
                .withArguments(new StringArgument("value"))
                .executes((sender, args) -> {
                    String profileName = (String) args.get("profile");
                    String key = (String) args.get("key");
                    String value = (String) args.get("value");

                    RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                    if (profile == null) {
                        sender.sendMessage("§cUnknown RTP profile: " + profileName);
                        return;
                    }

                    try {
                        switch (key.toLowerCase()) {
                            case "landingworld":
                                profile.landingWorld = value;
                                break;
                            case "cost":
                                profile.cost = Double.parseDouble(value);
                                break;
                            case "cooldown":
                                profile.cooldown = Integer.parseInt(value);
                                break;
                            case "maxattempts":
                                profile.maxAttempts.value = Integer.parseInt(value);
                                break;
                            case "bounds-low":
                                profile.bounds.low = Integer.parseInt(value);
                                break;
                            case "bounds-high":
                                profile.bounds.high = Integer.parseInt(value);
                                break;
                            default:
                                sender.sendMessage("§cUnknown settings key: " + key);
                                return;
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cInvalid value for setting: " + value);
                        return;
                    }

                    plugin.getConfigHandler().saveProfile(profile);
                    sender.sendMessage("§aSuccessfully updated setting " + key + " to " + value + " in profile " + profile.name);
                })
            );
    }

    private void executeRtp(Player player, CommandArguments args) {
        String profileName = (String) args.get("profile");
        RtpProfile profile;
        if (profileName != null) {
            profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
            if (profile == null) {
                player.sendMessage("§cUnknown RTP profile: " + profileName);
                return;
            }
            if (!player.hasPermission("jakesrtp.usebyname") && !player.hasPermission("jakesrtp.use." + profileName.toLowerCase())) {
                player.sendMessage("§cYou do not have permission to use the profile: " + profileName);
                return;
            }
        } else {
            lunatech.jetrtp.gui.ProfileMenu.open(player, plugin);
            return;
        }

        if (profile == null) {
            player.sendMessage("§cNo RTP profile could be resolved for you.");
            return;
        }

        if (rtpService.isOnCooldown(player, profile)) {
            long remaining = rtpService.getRemainingCooldown(player, profile) / 1000L;
            player.sendMessage("§cYou must wait " + remaining + " seconds before using RTP again.");
            return;
        }

        rtpService.executeRtp(player, profile).thenAccept(success -> {
            if (!success) {
                player.sendMessage("§cTeleportation could not be completed at this time.");
            }
        });
    }
}
