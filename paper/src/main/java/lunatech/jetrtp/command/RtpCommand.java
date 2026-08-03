package lunatech.jetrtp.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.config.RtpProfile;
import lunatech.jetrtp.service.RtpService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public final class RtpCommand {
    private final AbstractJetRTP plugin;
    private final RtpService rtpService;

    public RtpCommand(AbstractJetRTP plugin, RtpService rtpService) {
        this.plugin = plugin;
        this.rtpService = rtpService;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("rtp")
            .requires(source -> source.getSender().hasPermission("jakesrtp.use"))
            .executes(ctx -> {
                if (ctx.getSource().getExecutor() instanceof Player player) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        lunatech.jetrtp.gui.ProfileMenu.open(player, plugin);
                    });
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                } else {
                    ctx.getSource().getSender().sendMessage("§cOnly players can run this command.");
                    return 0;
                }
            })
            .then(Commands.argument("profile", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    if (ctx.getSource().getSender() instanceof Player player) {
                        plugin.getConfigHandler().getProfiles().keySet().stream()
                            .filter(key -> player.hasPermission("jakesrtp.usebyname") || player.hasPermission("jakesrtp.use." + key.toLowerCase()))
                            .filter(key -> key.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                            .forEach(builder::suggest);
                    } else {
                        plugin.getConfigHandler().getProfiles().keySet().stream()
                            .filter(key -> key.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                            .forEach(builder::suggest);
                    }
                    return builder.buildFuture();
                })
                .executes(ctx -> {
                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                        ctx.getSource().getSender().sendMessage("§cOnly players can run this command.");
                        return 0;
                    }
                    String profileName = StringArgumentType.getString(ctx, "profile");
                    RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                    if (profile == null) {
                        player.sendMessage("§cUnknown RTP profile: " + profileName);
                        return 0;
                    }
                    if (!player.hasPermission("jakesrtp.usebyname") && !player.hasPermission("jakesrtp.use." + profileName.toLowerCase())) {
                        player.sendMessage("§cYou do not have permission to use the profile: " + profileName);
                        return 0;
                    }

                    if (rtpService.isOnCooldown(player, profile)) {
                        long remaining = rtpService.getRemainingCooldown(player, profile) / 1000L;
                        player.sendMessage("§cYou must wait " + remaining + " seconds before using RTP again.");
                        return 0;
                    }

                    rtpService.executeRtp(player, profile).thenAccept(success -> {
                        if (!success) {
                            player.sendMessage("§cTeleportation could not be completed at this time.");
                        }
                    });
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("jakesrtp.admin.reload"))
                .executes(ctx -> {
                    if (plugin instanceof lunatech.jetrtp.JetRTP jetRtp) {
                        jetRtp.onReload();
                    }
                    ctx.getSource().getSender().sendMessage("§aJetRTP profiles and configuration reloaded.");
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("force")
                .requires(source -> source.getSender().hasPermission("jakesrtp.admin.force"))
                .then(Commands.literal("config")
                    .then(Commands.argument("target", io.papermc.paper.command.brigadier.argument.ArgumentTypes.player())
                        .then(Commands.argument("profile", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                plugin.getConfigHandler().getProfiles().keySet().stream()
                                    .filter(key -> key.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                var targetResolver = ctx.getArgument("target", io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver.class);
                                var targets = targetResolver.resolve(ctx.getSource());
                                if (targets.isEmpty()) {
                                    sender.sendMessage("§cTarget player not found.");
                                    return 0;
                                }
                                Player target = targets.getFirst();
                                String profileName = StringArgumentType.getString(ctx, "profile");
                                RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                                if (profile == null) {
                                    sender.sendMessage("§cUnknown RTP profile: " + profileName);
                                    return 0;
                                }
                                sender.sendMessage("§aForcefully executing random teleport for " + target.getName() + " using profile " + profile.name);
                                rtpService.executeRtp(target, profile).thenAccept(success -> {
                                    if (success) {
                                        sender.sendMessage("§aRandomly teleported " + target.getName() + " successfully.");
                                    } else {
                                        sender.sendMessage("§cFailed to randomly teleport " + target.getName());
                                    }
                                });
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                        )
                    )
                )
                .then(Commands.literal("world")
                    .then(Commands.argument("target", io.papermc.paper.command.brigadier.argument.ArgumentTypes.player())
                        .then(Commands.argument("world", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                plugin.getServer().getWorlds().stream()
                                    .map(org.bukkit.World::getName)
                                    .filter(name -> name.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                    .forEach(builder::suggest);
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                var targetResolver = ctx.getArgument("target", io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver.class);
                                var targets = targetResolver.resolve(ctx.getSource());
                                if (targets.isEmpty()) {
                                    sender.sendMessage("§cTarget player not found.");
                                    return 0;
                                }
                                Player target = targets.getFirst();
                                String worldName = StringArgumentType.getString(ctx, "world");
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
                                    return 0;
                                }

                                RtpProfile finalProfile = profile;
                                sender.sendMessage("§aForcefully executing random teleport for " + target.getName() + " landing in world " + worldName);
                                rtpService.executeRtp(target, finalProfile).thenAccept(success -> {
                                    if (success) {
                                        sender.sendMessage("§aRandomly teleported " + target.getName() + " successfully.");
                                    } else {
                                        sender.sendMessage("§cFailed to randomly teleport " + target.getName());
                                    }
                                });
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                        )
                    )
                )
            )
            .then(Commands.literal("info")
                .requires(source -> source.getSender().hasPermission("jakesrtp.admin.info"))
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage("§8=== §aJetRTP General Info §8===");
                    sender.sendMessage("§7Loaded Profiles: §f" + plugin.getConfigHandler().getProfiles().size());
                    sender.sendMessage("§7Database Active: §f" + lunatech.jetrtp.utility.DB.isStarted());
                    for (RtpProfile profile : plugin.getConfigHandler().getProfiles().values()) {
                        int size = plugin.getCacheService().getCacheSize(profile);
                        sender.sendMessage("§8- §a" + profile.name + "§7: Cache = §f" + size + "/" + profile.preparations.cacheLocations);
                    }
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("profile", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        plugin.getConfigHandler().getProfiles().keySet().stream()
                            .filter(key -> key.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                            .forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        CommandSender sender = ctx.getSource().getSender();
                        String profileName = StringArgumentType.getString(ctx, "profile");
                        RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                        if (profile == null) {
                            sender.sendMessage("§cUnknown RTP profile: " + profileName);
                            return 0;
                        }
                        sender.sendMessage("§8=== §aJetRTP Profile Info: " + profile.name + " §8===");
                        sender.sendMessage("§7Enabled: §f" + profile.enabled);
                        sender.sendMessage("§7Landing World: §f" + profile.landingWorld);
                        sender.sendMessage("§7Cache Size: §f" + plugin.getCacheService().getCacheSize(profile) + " / " + profile.preparations.cacheLocations);
                        sender.sendMessage("§7Cooldown Time: §f" + profile.cooldown + "s");
                        sender.sendMessage("§7Cost: §f$" + profile.cost);
                        sender.sendMessage("§7Bounds: §fY " + profile.bounds.low + " to " + profile.bounds.high);
                        sender.sendMessage("§7Excluded Biomes: §f" + String.join(", ", profile.excludedBiomes));
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("settings")
                .requires(source -> source.getSender().hasPermission("jakesrtp.admin.settings"))
                .then(Commands.argument("profile", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        plugin.getConfigHandler().getProfiles().keySet().stream()
                            .filter(key -> key.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                            .forEach(builder::suggest);
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("key", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            List.of("landingworld", "cost", "cooldown", "maxattempts", "bounds-low", "bounds-high").stream()
                                .filter(k -> k.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("value", StringArgumentType.string())
                            .executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                String profileName = StringArgumentType.getString(ctx, "profile");
                                String key = StringArgumentType.getString(ctx, "key");
                                String value = StringArgumentType.getString(ctx, "value");

                                RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                                if (profile == null) {
                                    sender.sendMessage("§cUnknown RTP profile: " + profileName);
                                    return 0;
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
                                            return 0;
                                    }
                                } catch (NumberFormatException e) {
                                    sender.sendMessage("§cInvalid value for setting: " + value);
                                    return 0;
                                }

                                plugin.getConfigHandler().saveProfile(profile);
                                sender.sendMessage("§aSuccessfully updated setting " + key + " to " + value + " in profile " + profile.name);
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                        )
                    )
                )
            );
    }
}
