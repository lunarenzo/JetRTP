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
                .withArguments(new dev.jorel.commandapi.arguments.EntitySelectorArgument.OnePlayer("target"))
                .withOptionalArguments(
                    new StringArgument("profile").replaceSuggestions(ArgumentSuggestions.strings(info -> {
                        return plugin.getConfigHandler().getProfiles().keySet().toArray(new String[0]);
                    }))
                )
                .executes((sender, args) -> {
                    Player target = (Player) args.get("target");
                    if (target == null) {
                        sender.sendMessage("§cTarget player not found.");
                        return;
                    }
                    String profileName = (String) args.get("profile");
                    RtpProfile profile;
                    if (profileName != null) {
                        profile = plugin.getConfigHandler().getProfiles().get(profileName.toLowerCase());
                        if (profile == null) {
                            sender.sendMessage("§cUnknown RTP profile: " + profileName);
                            return;
                        }
                    } else {
                        profile = plugin.getConfigHandler().getProfiles().values().stream()
                            .filter(p -> p.enabled && p.commandEnabled)
                            .findFirst().orElse(null);
                    }
                    if (profile == null) {
                        sender.sendMessage("§cNo RTP profile could be resolved.");
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
