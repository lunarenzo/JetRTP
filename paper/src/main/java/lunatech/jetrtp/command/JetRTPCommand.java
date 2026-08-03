package lunatech.jetrtp.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import io.github.milkdrinkers.wordweaver.Translation;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lunatech.jetrtp.AbstractJetRTP;
import lunatech.jetrtp.utility.Cfg;
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
                    ctx.getSource().getSender().sendMessage(Translation.as("commands.translation.help"));
                    return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                })
                .then(Commands.literal("help")
                    .executes(ctx -> {
                        ctx.getSource().getSender().sendMessage(Translation.as("commands.translation.help"));
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                )
                .then(Commands.literal("reload")
                    .requires(source -> source.getSender().hasPermission("jetrtp.command.translation.reload"))
                    .executes(ctx -> {
                        Translation.setLanguage(Cfg.get().language);
                        Translation.reload();
                        ctx.getSource().getSender().sendMessage(Translation.as("commands.translation.reloaded"));
                        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                    })
                )
                .then(Commands.literal("test")
                    .requires(source -> source.getSender().hasPermission("jetrtp.command.translation.test"))
                    .then(Commands.argument("key", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            Translation.getKeys().stream()
                                .filter(key -> key.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
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

                            if (translation == null) {
                                sender.sendMessage(ColorParser.of(Translation.of("commands.translation.test.not-found")).with("node", node).build());
                                return 0;
                            }

                            if (translation.isBlank()) {
                                sender.sendMessage(ColorParser.of(Translation.of("commands.translation.test.not-empty2")).with("node", node).build());
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
                                sender.sendMessage(Translation.as(node));
                            }
                            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
                        })
                    )
                )
            );
    }
}
