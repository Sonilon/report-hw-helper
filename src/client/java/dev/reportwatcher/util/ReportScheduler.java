package dev.reportwatcher.util;

import dev.reportwatcher.ReportWatcherClient;
import dev.reportwatcher.config.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Периодически отправляет команду /reportlist и ждёт открытия GUI.
 * Всё выполняется в game thread через tick-евент.
 */
public class ReportScheduler {

    private boolean running = false;
    private int tickCounter = 0;
    private int intervalTicks = 0; // в тиках (20 тиков = 1 сек)

    // Флаг: мы только что отправили команду, ждём открытия GUI
    public static final AtomicBoolean WAITING_FOR_SCREEN = new AtomicBoolean(false);

    // Флаг: GUI уже проверен в этом цикле
    private boolean checkedThisCycle = false;

    private ClientTickEvents.EndWorldTick tickListener;

    public void start(MinecraftClient client) {
        stop(); // на случай повторного вызова
        running = true;
        tickCounter = 0;
        checkedThisCycle = false;
        intervalTicks = ModConfig.CHECK_INTERVAL_SECONDS * 20;

        tickListener = (world) -> {
            if (!running || client.player == null) return;

            tickCounter++;

            // Пора отправить команду
            if (tickCounter >= intervalTicks) {
                tickCounter = 0;
                checkedThisCycle = false;
                WAITING_FOR_SCREEN.set(true);
                client.player.networkHandler.sendCommand(ModConfig.REPORT_COMMAND);
                ReportWatcherClient.LOGGER.debug("[ReportWatcher] Отправлена команда /" + ModConfig.REPORT_COMMAND);
            }
        };

        ClientTickEvents.END_WORLD_TICK.register(tickListener);
        ReportWatcherClient.LOGGER.info("[ReportWatcher] Планировщик запущен, интервал: "
                + ModConfig.CHECK_INTERVAL_SECONDS + "с");
    }

    public void stop() {
        running = false;
        WAITING_FOR_SCREEN.set(false);
        // Listener отписать нельзя в Fabric 1.20.1 стандартным способом,
        // поэтому просто ставим флаг running = false и проверяем его внутри
        ReportWatcherClient.LOGGER.info("[ReportWatcher] Планировщик остановлен");
    }

    /** Ручной триггер проверки прямо сейчас */
    public void triggerNow(MinecraftClient client) {
        if (client.player == null) return;
        tickCounter = 0;
        checkedThisCycle = false;
        WAITING_FOR_SCREEN.set(true);
        client.player.networkHandler.sendCommand(ModConfig.REPORT_COMMAND);
    }

    public boolean isRunning() {
        return running;
    }
}
