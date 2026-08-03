package lunatech.jetrtp.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import lunatech.jetrtp.AbstractJetRTP;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import org.bukkit.command.CommandSender;

import static lunatech.jetrtp.command.CommandHandler.BASE_PERM;

/**
 * Class containing the code for the main JetRTP command.
 */
final class JetRTPCommand extends Command {
    private final AbstractJetRTP plugin;

    /**
     * Instantiates and registers a new command.
     */
    JetRTPCommand(AbstractJetRTP plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandAPICommand command() {
        return new CommandAPICommand("jetrtp")
            .withHelp("Base command.", "Base command.")
            .withPermission(BASE_PERM)
            .withSubcommands(
                new TranslationCommand().command()
            )
            .executes(this::executorJetRTP);
    }

    private void executorJetRTP(CommandSender sender, CommandArguments args) {
        sender.sendMessage(
            ColorParser.of("<white>JetRTP plugin is running. Group: <blue>lunatech<white>, Author: <blue>lunarenzo<white>.")
                .legacy()
                .build()
        );
    }
}
