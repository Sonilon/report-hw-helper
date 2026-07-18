package dev.reportwatcher;

import dev.reportwatcher.config.ModConfig;
import dev.reportwatcher.util.ReportScheduler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportWatcherClient implements ClientModInitializer {

    public static final String MOD_ID = "reportwatcher";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final ReportScheduler SCHEDULER = new ReportScheduler();

    private boolean wasConnected = false;

    @Override
    public void onInitializeClient() {
        ModConfig.load();

        // Определяем подключение/отключение через тик
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean connected = client.player != null && client.world != null;

            if (connected && !wasConnected) {
                // Только что подключились
                SCHEDULER.start(client);
                sendModMessage(client, "§a✔ §fReportWatcher §7активирован. Проверка каждые §e"
                        + ModConfig.CHECK_INTERVAL_SECONDS + " §7секунд.");
            } else if (!connected && wasConnected) {
                // Только что отключились
                SCHEDULER.stop();
            }

            wasConnected = connected;
        });

        // Команды /rw check и /rw status
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("rw")
                    .then(ClientCommandManager.literal("check")
                        .executes(ctx -> {
                            MinecraftClient client = ctx.getSource().getClient();
                            client.player.sendMessage(
                                Text.literal("§e[RW] §7Запускаю ручную проверку..."), false);
                            SCHEDULER.triggerNow(client);
                            return 1;
                        })
                    )
                    .then(ClientCommandManager.literal("status")
                        .executes(ctx -> {
                            MinecraftClient client = ctx.getSource().getClient();
                            String status = SCHEDULER.isRunning()
                                ? "§aАктивен §7| Интервал: §e" + ModConfig.CHECK_INTERVAL_SECONDS + "с"
                                : "§cОстановлен";
                            client.player.sendMessage(
                                Text.literal("§e[RW] §7Статус: " + status), false);
                            return 1;
                        })
                    )
            );
        });

        LOGGER.info("[ReportWatcher] Мод загружен!");
    }

    public static void sendModMessage(MinecraftClient client, String message) {
        if (client.player != null) {
            client.execute(() -> client.player.sendMessage(Text.literal(message), false));
        }
    }
}
