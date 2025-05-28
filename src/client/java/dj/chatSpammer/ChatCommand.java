package dj.chatspammer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;
import net.minecraft.text.Text;


import java.util.concurrent.*;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;

public class ChatCommand {

    private static ScheduledExecutorService scheduler = null;
    private static ScheduledFuture<?> currentTask = null;

    private static void ensureScheduler() {
        if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        }
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                literal("spam")
                    .then(argument("amount", IntegerArgumentType.integer(1))
                        .then(argument("delay", IntegerArgumentType.integer(0))
                            .then(argument("message", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    int delay = IntegerArgumentType.getInteger(ctx, "delay");
                                    String message = StringArgumentType.getString(ctx, "message");

                                    MinecraftClient client = MinecraftClient.getInstance();

                                    ensureScheduler();

                                    // Cancel previous task if running
                                    if (currentTask != null && !currentTask.isDone()) {
                                        currentTask.cancel(true);
                                    }

                                    Runnable spamTask = new Runnable() {
                                        int sent = 0;

                                        @Override
                                        public void run() {
                                            if (sent >= amount) {
                                                currentTask.cancel(false);
                                                return;
                                            }

                                            client.execute(() -> {
                                                if (client.player != null && client.player.networkHandler != null) {
                                                    if (message.startsWith("/")) {
                                                        // Run as a command, drop the leading slash
                                                        client.player.networkHandler.sendCommand(message.substring(1));
                                                    } else {
                                                        // Send as chat message
                                                        client.player.networkHandler.sendChatMessage(message);
                                                    }

                                                    client.player.sendMessage(
                                                        Text.literal("Spamming (" + (sent + 1) + "/" + amount + ")"),
                                                        true // true means it's an ActionBar message
                                                    );

                                                }
                                            });

                                            sent++;
                                        }
                                    };

                                    currentTask = scheduler.scheduleAtFixedRate(spamTask, 0, delay, TimeUnit.MILLISECONDS);
                                    return 1;
                                })
                            )
                        )
                    )
            );

            dispatcher.register(
                literal("stopspam").executes(ctx -> {
                    if (currentTask != null && !currentTask.isDone()) {
                        currentTask.cancel(true);
                        currentTask = null;
                    }
                    if (scheduler != null && !scheduler.isShutdown()) {
                        scheduler.shutdownNow();
                        scheduler = null;
                    }
                    return 1;
                })
            );
        });
    }
}
