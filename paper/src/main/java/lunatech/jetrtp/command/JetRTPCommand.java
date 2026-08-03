package lunatech.jetrtp.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.translation.Translation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class JetRTPCommand {
    private final AbstractJetRTP plugin;

    public JetRTPCommand(AbstractJetRTP plugin) {
        this.plugin = plugin;
    }

    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("jetrtp")
            .requires(source -> source.getSender().hasPermission("jetrtp.command"))
            .executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                sender.sendMessage(
                    ColorParser.of("<white>JetRTP plugin is running. Group: <blue>lunatech<white>, Author: <blue>lunarenzo<white>.")
                        .legacy()
                        .build()
                );
                return com.mojang.brigadier.Command.SINGLE_SUCCESS;
            })
            .then(Commands.literal("translation")
                .requires(source -> source.getSender().hasPermission("jetrtp.command.translation"))
                .executes(ctx -> {
                    ctx.getSource().getSender().sendMessage(ColorParser.of(Translation.of("commands.translation.help")).build());
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("help")
                    .executes(ctx -> {
                        ctx.getSource().getSender().sendMessage(ColorParser.of(Translation.of("commands.translation.help")).build());
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                )
                .then(Commands.literal("reload")
                    .requires(source -> source.getSender().hasPermission("jetrtp.command.translation.reload"))
                    .executes(ctx -> {
                        Translation.load(plugin.getDataPath(), plugin.getConfigHandler().getConfig().language);
                        ctx.getSource().getSender().sendMessage(ColorParser.of(Translation.of("commands.translation.reloaded")).build());
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                )
                .then(Commands.literal("test")
                    .requires(source -> source.getSender().hasPermission("jetrtp.command.translation.test"))
                    .then(Commands.argument("key", StringArgumentType.word())
                        .executes(ctx -> {
                            CommandSender sender = ctx.getSource().getSender();
                            String node = StringArgumentType.getString(ctx, "key");

                            if (node.isBlank()) {
                                sender.sendMessage(ColorParser.of(Translation.of("commands.translation.test.not-empty")).with("node", node).build());
                                return 0;
                            }

                            if (node.startsWith(".") || node.endsWith(".")) {
                                sender.sendMessage(ColorParser.of(Translation.of("commands.translation.test.illegal")).with("node", node).build());
                                return 0;
                            }

                            String translation = Translation.of(node);

                            if (translation.isEmpty()) {
                                sender.sendMessage(ColorParser.of(Translation.of("commands.translation.test.not-found")).with("node", node).build());
                                return 0;
                            }

                            if (sender instanceof Player player) {
                                sender.sendMessage(
                                    ColorParser.of(Translation.of(node))
                                        .papi(player)
                                        .mini(player)
                                        .build()
                                );
                            } else {
                                sender.sendMessage(ColorParser.of(Translation.of(node)).build());
                            }
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        })
                    )
                )
            );
    }
}
