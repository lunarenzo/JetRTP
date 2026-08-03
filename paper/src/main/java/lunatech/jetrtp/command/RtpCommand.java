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
            .executesPlayer(this::executeRtp);
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
            profile = plugin.getConfigHandler().getProfiles().values().stream()
                .filter(p -> p.enabled && p.commandEnabled && (
                    !p.requireExplicitPermission || player.hasPermission("jakesrtp.use." + p.name.toLowerCase())
                ))
                .findFirst().orElse(null);

            if (profile == null) {
                profile = plugin.getConfigHandler().getProfiles().get("default-settings");
            }
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
