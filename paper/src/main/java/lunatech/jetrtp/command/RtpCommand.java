package lunatech.jetrtp.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import lunatech.jetrtp.translation.Translation;
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
                    ctx.getSource().getSender().sendMessage(ColorParser.of(Translation.of("rtp.only-players")).build());
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
                        ctx.getSource().getSender().sendMessage(ColorParser.of(Translation.of("rtp.only-players")).build());
                        return 0;
                    }
                    String profileName = StringArgumentType.getString(ctx, "profile");
                    RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                    if (profile == null) {
                        player.sendMessage(ColorParser.of(Translation.of("rtp.unknown-profile")).with("profile", profileName).build());
                        return 0;
                    }
                    if (!player.hasPermission("jakesrtp.usebyname") && !player.hasPermission("jakesrtp.use." + profileName.toLowerCase())) {
                        player.sendMessage(ColorParser.of(Translation.of("rtp.no-permission")).with("profile", profileName).build());
                        return 0;
                    }

                    if (rtpService.isOnCooldown(player, profile)) {
                        long remaining = rtpService.getRemainingCooldown(player, profile) / 1000L;
                        player.sendMessage(ColorParser.of(Translation.of("rtp.cooldown")).with("remaining", String.valueOf(remaining)).build());
                        return 0;
                    }

                    rtpService.executeRtp(player, profile).thenAccept(success -> {
                        if (!success) {
                            player.sendMessage(ColorParser.of(Translation.of("rtp.teleport-failed")).build());
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
                    ctx.getSource().getSender().sendMessage(ColorParser.of(Translation.of("rtp.reloaded")).build());
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
                                    sender.sendMessage(ColorParser.of(Translation.of("rtp.target-not-found")).build());
                                    return 0;
                                }
                                Player target = targets.getFirst();
                                String profileName = StringArgumentType.getString(ctx, "profile");
                                RtpProfile profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                                if (profile == null) {
                                    sender.sendMessage(ColorParser.of(Translation.of("rtp.unknown-profile")).with("profile", profileName).build());
                                    return 0;
                                }
                                sender.sendMessage(ColorParser.of(Translation.of("rtp.force-config")).with("target", target.getName()).with("profile", profile.name).build());
                                rtpService.executeRtp(target, profile).thenAccept(success -> {
                                    if (success) {
                                        sender.sendMessage(ColorParser.of(Translation.of("rtp.force-success")).with("target", target.getName()).build());
                                    } else {
                                        sender.sendMessage(ColorParser.of(Translation.of("rtp.force-failed")).with("target", target.getName()).build());
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
                                    sender.sendMessage(ColorParser.of(Translation.of("rtp.target-not-found")).build());
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
                                    sender.sendMessage(ColorParser.of(Translation.of("rtp.no-profile-resolved")).build());
                                    return 0;
                                }

                                RtpProfile finalProfile = profile;
                                sender.sendMessage(ColorParser.of(Translation.of("rtp.force-world")).with("target", target.getName()).with("world", worldName).build());
                                rtpService.executeRtp(target, finalProfile).thenAccept(success -> {
                                    if (success) {
                                        sender.sendMessage(ColorParser.of(Translation.of("rtp.force-success")).with("target", target.getName()).build());
                                    } else {
                                        sender.sendMessage(ColorParser.of(Translation.of("rtp.force-failed")).with("target", target.getName()).build());
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
                    sender.sendMessage(ColorParser.of(Translation.of("info.general-header")).build());
                    sender.sendMessage(ColorParser.of(Translation.of("info.loaded-profiles")).with("count", String.valueOf(plugin.getConfigHandler().getProfiles().size())).build());
                    sender.sendMessage(ColorParser.of(Translation.of("info.database-active")).with("status", String.valueOf(lunatech.jetrtp.utility.DB.isStarted())).build());
                    for (RtpProfile profile : plugin.getConfigHandler().getProfiles().values()) {
                        int size = plugin.getCacheService().getCacheSize(profile);
                        sender.sendMessage(ColorParser.of(Translation.of("info.profile-entry"))
                            .with("profile", profile.name)
                            .with("cache", String.valueOf(size))
                            .with("max", String.valueOf(profile.preparations.cacheLocations))
                            .build());
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
                            sender.sendMessage(ColorParser.of(Translation.of("rtp.unknown-profile")).with("profile", profileName).build());
                            return 0;
                        }
                        sender.sendMessage(ColorParser.of(Translation.of("info.profile-header")).with("profile", profile.name).build());
                        sender.sendMessage(ColorParser.of(Translation.of("info.profile-enabled")).with("status", String.valueOf(profile.enabled)).build());
                        sender.sendMessage(ColorParser.of(Translation.of("info.profile-world")).with("world", profile.landingWorld).build());
                        sender.sendMessage(ColorParser.of(Translation.of("info.profile-cache"))
                            .with("cache", String.valueOf(plugin.getCacheService().getCacheSize(profile)))
                            .with("max", String.valueOf(profile.preparations.cacheLocations))
                            .build());
                        sender.sendMessage(ColorParser.of(Translation.of("info.profile-cooldown")).with("cooldown", String.valueOf(profile.cooldown)).build());
                        sender.sendMessage(ColorParser.of(Translation.of("info.profile-cost")).with("cost", String.valueOf(profile.cost)).build());
                        sender.sendMessage(ColorParser.of(Translation.of("info.profile-bounds"))
                            .with("low", String.valueOf(profile.bounds.low))
                            .with("high", String.valueOf(profile.bounds.high))
                            .build());
                        sender.sendMessage(ColorParser.of(Translation.of("info.profile-biomes")).with("biomes", String.join(", ", profile.excludedBiomes)).build());
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
                                    sender.sendMessage(ColorParser.of(Translation.of("rtp.unknown-profile")).with("profile", profileName).build());
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
                                            sender.sendMessage(ColorParser.of(Translation.of("settings.unknown-key")).with("key", key).build());
                                            return 0;
                                    }
                                } catch (NumberFormatException e) {
                                    sender.sendMessage(ColorParser.of(Translation.of("settings.invalid-value")).with("value", value).build());
                                    return 0;
                                }

                                plugin.getConfigHandler().saveProfile(profile);
                                sender.sendMessage(ColorParser.of(Translation.of("settings.success"))
                                    .with("key", key)
                                    .with("value", value)
                                    .with("profile", profile.name)
                                    .build());
                                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                            })
                        )
                    )
                )
            );
    }
}
