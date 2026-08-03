package lunatech.jetrtp.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
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
                    ctx.getSource().getSender().sendMessage(ColorParser.of("<red>Only players can run this command.").build());
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
                        ctx.getSource().getSender().sendMessage(ColorParser.of("<red>Only players can run this command.").build());
                        return 0;
                    }
                    String profileName = StringArgumentType.getString(ctx, "profile");
                    RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                    if (profile == null) {
                        player.sendMessage(ColorParser.of("<red>Unknown RTP profile: <yellow>" + profileName).build());
                        return 0;
                    }
                    if (!player.hasPermission("jakesrtp.usebyname") && !player.hasPermission("jakesrtp.use." + profileName.toLowerCase())) {
                        player.sendMessage(ColorParser.of("<red>You do not have permission to use the profile: <yellow>" + profileName).build());
                        return 0;
                    }

                    if (rtpService.isOnCooldown(player, profile)) {
                        long remaining = rtpService.getRemainingCooldown(player, profile) / 1000L;
                        player.sendMessage(ColorParser.of("<red>You must wait <yellow>" + remaining + "</yellow> seconds before using RTP again.").build());
                        return 0;
                    }

                    rtpService.executeRtp(player, profile).thenAccept(success -> {
                        if (!success) {
                            player.sendMessage(ColorParser.of("<red>Teleportation could not be completed at this time.").build());
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
                    ctx.getSource().getSender().sendMessage(ColorParser.of("<green>JetRTP profiles and configuration reloaded.").build());
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
                                    sender.sendMessage(ColorParser.of("<red>Target player not found.").build());
                                    return 0;
                                }
                                Player target = targets.getFirst();
                                String profileName = StringArgumentType.getString(ctx, "profile");
                                RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                                if (profile == null) {
                                    sender.sendMessage(ColorParser.of("<red>Unknown RTP profile: <yellow>" + profileName).build());
                                    return 0;
                                }
                                sender.sendMessage(ColorParser.of("<green>Forcefully executing random teleport for <white>" + target.getName() + "</white> using profile <white>" + profile.name).build());
                                rtpService.executeRtp(target, profile).thenAccept(success -> {
                                    if (success) {
                                        sender.sendMessage(ColorParser.of("<green>Randomly teleported <white>" + target.getName() + "</white> successfully.").build());
                                    } else {
                                        sender.sendMessage(ColorParser.of("<red>Failed to randomly teleport <white>" + target.getName()).build());
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
                                    sender.sendMessage(ColorParser.of("<red>Target player not found.").build());
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
                                    sender.sendMessage(ColorParser.of("<red>No RTP profile could be resolved.").build());
                                    return 0;
                                }

                                RtpProfile finalProfile = profile;
                                sender.sendMessage(ColorParser.of("<green>Forcefully executing random teleport for <white>" + target.getName() + "</white> landing in world <white>" + worldName).build());
                                rtpService.executeRtp(target, finalProfile).thenAccept(success -> {
                                    if (success) {
                                        sender.sendMessage(ColorParser.of("<green>Randomly teleported <white>" + target.getName() + "</white> successfully.").build());
                                    } else {
                                        sender.sendMessage(ColorParser.of("<red>Failed to randomly teleport <white>" + target.getName()).build());
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
                    sender.sendMessage(ColorParser.of("<dark_gray>=== <green>JetRTP General Info <dark_gray>===").build());
                    sender.sendMessage(ColorParser.of("<gray>Loaded Profiles: <white>" + plugin.getConfigHandler().getProfiles().size()).build());
                    sender.sendMessage(ColorParser.of("<gray>Database Active: <white>" + lunatech.jetrtp.utility.DB.isStarted()).build());
                    for (RtpProfile profile : plugin.getConfigHandler().getProfiles().values()) {
                        int size = plugin.getCacheService().getCacheSize(profile);
                        sender.sendMessage(ColorParser.of("<dark_gray>- <green>" + profile.name + "<gray>: Cache = <white>" + size + "/" + profile.preparations.cacheLocations).build());
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
                            sender.sendMessage(ColorParser.of("<red>Unknown RTP profile: <yellow>" + profileName).build());
                            return 0;
                        }
                        sender.sendMessage(ColorParser.of("<dark_gray>=== <green>JetRTP Profile Info: <white>" + profile.name + " <dark_gray>===").build());
                        sender.sendMessage(ColorParser.of("<gray>Enabled: <white>" + profile.enabled).build());
                        sender.sendMessage(ColorParser.of("<gray>Landing World: <white>" + profile.landingWorld).build());
                        sender.sendMessage(ColorParser.of("<gray>Cache Size: <white>" + plugin.getCacheService().getCacheSize(profile) + " / " + profile.preparations.cacheLocations).build());
                        sender.sendMessage(ColorParser.of("<gray>Cooldown Time: <white>" + profile.cooldown + "s").build());
                        sender.sendMessage(ColorParser.of("<gray>Cost: <white>$" + profile.cost).build());
                        sender.sendMessage(ColorParser.of("<gray>Bounds: <white>Y " + profile.bounds.low + " to " + profile.bounds.high).build());
                        sender.sendMessage(ColorParser.of("<gray>Excluded Biomes: <white>" + String.join(", ", profile.excludedBiomes)).build());
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
                                    sender.sendMessage(ColorParser.of("<red>Unknown RTP profile: <yellow>" + profileName).build());
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
                                            sender.sendMessage(ColorParser.of("<red>Unknown settings key: <yellow>" + key).build());
                                            return 0;
                                    }
                                } catch (NumberFormatException e) {
                                    sender.sendMessage(ColorParser.of("<red>Invalid value for setting: <yellow>" + value).build());
                                    return 0;
                                }

                                plugin.getConfigHandler().saveProfile(profile);
                                sender.sendMessage(ColorParser.of("<green>Successfully updated setting <white>" + key + "</white> to <white>" + value + "</white> in profile <white>" + profile.name).build());
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                        )
                    )
                )
            );
    }
}
