package io.github.lucasstarsz.fastj.engine;

import io.github.lucasstarsz.fastj.math.Point;
import io.github.lucasstarsz.fastj.systems.behaviors.BehaviorManager;
import io.github.lucasstarsz.fastj.systems.control.LogicManager;
import io.github.lucasstarsz.fastj.systems.input.keyboard.Keyboard;
import io.github.lucasstarsz.fastj.systems.input.mouse.Mouse;
import io.github.lucasstarsz.fastj.systems.render.Display;
import io.github.lucasstarsz.fastj.systems.tags.TagManager;

import io.github.lucasstarsz.fastj.engine.internals.ThreadFixer;
import io.github.lucasstarsz.fastj.engine.internals.Timer;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The main control hub of the game engine.
 * <p>
 * This class contains the methods needed to initialize and run a game using the FastJ Game Engine. With this, you'll
 * have access to the engine's features in their full force.
 * <p>
 * <a href="https://github.com/lucasstarsz/FastJ-Engine">The FastJ Game Engine</a>
 *
 * @author Andrew Dey
 * @version 1.0.0
 */
public class FastJEngine {

    /** Default engine value for frames per second. */
    public static final int DefaultFPS = Math.max(Display.getDefaultMonitorRefreshRate(), 1);
    /** Default engine value for updates per second. */
    public static final int DefaultUPS = 60;
    /** Default engine value for the window resolution of the {@link Display}. */
    public static final Point DefaultWindowResolution = new Point(1280, 720);
    /** Default engine value for the internal resolution of the {@link Display}. */
    public static final Point DefaultInternalResolution = new Point(1280, 720);

    // engine speed variables
    private static int targetFPS;
    private static int targetUPS;

    // FPS counting
    private static Timer timer;
    private static int[] fpsLog;
    private static int drawFrames;
    private static int totalFPS;
    private static int fpsLogIndex;
    private static ScheduledExecutorService fpsLogger;

    // HW acceleration
    private static HWAccel hwAccel;

    // Display/Logic
    private static Display display;
    private static LogicManager gameManager;

    // Check values
    private static boolean isRunning;

    /**
     * Initializes the game engine with the specified title and logic manager.
     * <p>
     * Other values are set to their respective default values. These are as follows:
     * <ul>
     * 		<li>Default target FPS: The refresh rate of the default monitor (with a value of at least 1).</li>
     * 		<li>Default target UPS: 60 Updates Per Second.</li>
     * 		<li>Default window resolution: {@code 1280 * 720 (720p)} </li>
     * 		<li>Default internal game resolution: {@code 1280 * 720 (720p)}</li>
     * 		<li>Default hardware acceleration: {@code HWAccel.DEFAULT}</li>
     * </ul>
     *
     * @param gameTitle   The title to be used for the {@link Display} window.
     * @param gameManager Game Manager to be controlled by the engine.
     */
    public static void init(String gameTitle, LogicManager gameManager) {
        init(gameTitle, gameManager, DefaultFPS, DefaultUPS, DefaultWindowResolution, DefaultInternalResolution, HWAccel.DEFAULT);
    }

    /**
     * Initializes the game engine with the specified title, logic manager, and other options.
     *
     * @param gameTitle            The title to be used for the {@link Display} window.
     * @param gameManager          Game Manager to be controlled by the engine.
     * @param fps                  The FPS (frames per second) target for the engine to reach.
     * @param ups                  The UPS (updates per second) target for the engine to reach.
     * @param windowResolution     The game's window resolution.
     * @param internalResolution   The game's internal resolution. (This is the defined size of the game's canvas. As a
     *                             result, the content is scaled to fit the size of the {@code windowResolution}).
     * @param hardwareAcceleration Defines the type of hardware acceleration to use for the game.
     */
    public static void init(String gameTitle, LogicManager gameManager, int fps, int ups, Point windowResolution, Point internalResolution, HWAccel hardwareAcceleration) {
        runningCheck();

        FastJEngine.gameManager = gameManager;
        display = new Display(gameTitle, windowResolution, internalResolution);
        timer = new Timer();

        fpsLog = new int[100];
        Arrays.fill(fpsLog, -1);

        configure(fps, ups, windowResolution, internalResolution, hardwareAcceleration);
    }

    /**
     * Configures the game's FPS (Frames Per Second), UPS (Updates Per Second), viewer resolution, internal resolution,
     * and hardware acceleration.
     *
     * @param fps                  The FPS (frames per second) target for the engine to reach.
     * @param ups                  The UPS (updates per second) target for the engine to reach.
     * @param windowResolution     The game's window resolution.
     * @param internalResolution   The game's internal resolution. (This is the defined size of the game's canvas. As a
     *                             result, the content is scaled to fit the size of the {@code windowResolution}).
     * @param hardwareAcceleration Defines the type of hardware acceleration to use for the game.
     */
    public static void configure(int fps, int ups, Point windowResolution, Point internalResolution, HWAccel hardwareAcceleration) {
        runningCheck();

        configureViewerResolution(windowResolution);
        configureInternalResolution(internalResolution);
        configureHardwareAcceleration(hardwareAcceleration);
        setTargetFPS(fps);
        setTargetUPS(ups);
    }

    /**
     * Configures the game's window resolution.
     *
     * @param windowResolution The game's window resolution.
     */
    public static void configureViewerResolution(Point windowResolution) {
        runningCheck();

        if ((windowResolution.x | windowResolution.y) < 1) {
            error(CrashMessages.CONFIGURATION_ERROR.errorMessage, new IllegalArgumentException("Resolution values must be at least 1."));
        }

        display.setViewerResolution(windowResolution);
    }

    /**
     * Configures the game's internal resolution.
     * <p>
     * This sets the size of the game's drawing canvas. As a result, the content displayed on the canvas will be scaled
     * to fit the size of the {@code windowResolution}.
     *
     * @param internalResolution The game's internal resolution. (This is the defined size of the game's canvas. As a
     *                           result, the content is scaled to fit the size of the {@code windowResolution}).
     */
    public static void configureInternalResolution(Point internalResolution) {
        runningCheck();

        if ((internalResolution.x | internalResolution.y) < 1) {
            error(CrashMessages.CONFIGURATION_ERROR.errorMessage, new IllegalArgumentException("internal resolution values must be at least 1."));
        }

        display.setInternalResolution(internalResolution);
    }

    /**
     * Attempts to set the hardware acceleration type of this game engine to the specified parameter.
     * <p>
     * If the parameter specified is not supported by the user's computer, then the hardware acceleration will be set to
     * none, by default.
     *
     * @param hardwareAcceleration Defines the type of hardware acceleration to use for the game.
     */
    public static void configureHardwareAcceleration(HWAccel hardwareAcceleration) {
        runningCheck();

        if (hardwareAcceleration.equals(HWAccel.DIRECT3D)) {
            if (System.getProperty("os.name").startsWith("Win")) {
                HWAccel.setHardwareAcceleration(HWAccel.DIRECT3D);
                hwAccel = hardwareAcceleration;
            } else {
                warning("This OS doesn't support Direct3D hardware acceleration. Configuration will be left at default.");
                configureHardwareAcceleration(HWAccel.DEFAULT);
            }
        } else {
            HWAccel.setHardwareAcceleration(hardwareAcceleration);
            hwAccel = hardwareAcceleration;
        }
    }

    /**
     * Checks if the engine is currently running -- if it is, crash the game.
     * <p>
     * This method is usually for the purpose of ensuring certain methods aren't called while the game engine is
     * running.
     */
    public static void runningCheck() {
        if (isRunning) {
            error(CrashMessages.CALLED_AFTER_RUN_ERROR.errorMessage, new IllegalStateException("This method cannot be called after the game begins running."));
        }
    }

    /**
     * Gets the {@link Display} object associated with the game engine.
     *
     * @return The game engine's display instance.
     */
    public static Display getDisplay() {
        return display;
    }

    /**
     * Gets the {@link LogicManager} associated with the game engine.
     *
     * @return The logic manager.
     */
    public static LogicManager getLogicManager() {
        return gameManager;
    }

    /**
     * Gets the hardware acceleration currently enabled for the game engine.
     *
     * @return Returns the HWAccelType that defines what hardware acceleration, or lack thereof, is currently being used
     * for the game engine.
     */
    public static HWAccel getHardwareAcceleration() {
        return hwAccel;
    }

    /**
     * Gets the engine's current target FPS.
     *
     * @return The target FPS.
     */
    public static int getTargetFPS() {
        return targetFPS;
    }

    /**
     * Sets the engine's target FPS.
     *
     * @param fps The target FPS to set to.
     */
    public static void setTargetFPS(int fps) {
        if (fps < 1) {
            error(CrashMessages.CONFIGURATION_ERROR.errorMessage, new IllegalArgumentException("FPS amount must be at least 1."));
        }
        targetFPS = fps;
    }

    /**
     * Gets the engine's current target UPS.
     *
     * @return The target UPS.
     */
    public static int getTargetUPS() {
        return targetUPS;
    }

    /**
     * Sets the engine's target UPS.
     *
     * @param ups The target UPS to set to.
     */
    public static void setTargetUPS(int ups) {
        if (ups < 1) {
            error(CrashMessages.CONFIGURATION_ERROR.errorMessage, new IllegalArgumentException("UPS amount must be at least 1."));
        }
        targetUPS = ups;
    }

    /**
     * Gets the value that defines whether the engine is running.
     *
     * @return The boolean that defines whether the engine is running.
     */
    public static boolean isRunning() {
        return isRunning;
    }

    /**
     * Gets the FPS-based value of the parameter specified.
     * <p>
     * The types of information are as follows:
     * <ul>
     * 		<li>{@link FPSValue#CURRENT} - gets the last recorded FPS value.</li>
     * 		<li>{@link FPSValue#AVERAGE} - gets the average FPS, based on the recorded FPS values.</li>
     * 		<li>{@link FPSValue#HIGHEST} - gets the highest recorded FPS value.</li>
     * 		<li>{@link FPSValue#LOWEST} - gets the lowest recorded FPS value.</li>
     * 		<li>{@link FPSValue#ONE_PERCENT_LOW} - gets the average FPS of the lowest 1% of all recorded FPS values.</li>
     * </ul>
     *
     * @param dataType {@link FPSValue} parameter that specifies the information being requested.
     * @return Double value, based on the information requested.
     */
    public static double getFPSData(FPSValue dataType) {
        int[] validFPSValues = Arrays.copyOfRange(fpsLog, 0, Math.min(fpsLog.length, fpsLogIndex));

        switch (dataType) {
            case CURRENT:
                return (fpsLog[fpsLogIndex % 100] != -1) ? fpsLog[fpsLogIndex % 100] : 0;
            case AVERAGE:
                return (double) totalFPS / (double) fpsLogIndex;
            case HIGHEST:
                return Arrays.stream(validFPSValues).reduce(Integer::max).orElse(-1);
            case LOWEST:
                return Arrays.stream(validFPSValues).reduce(Integer::min).orElse(-1);
            case ONE_PERCENT_LOW:
                return Arrays.stream(validFPSValues).sorted()
                        .limit(Math.max(1L, (long) (validFPSValues.length * 0.01)))
                        .average().orElse(-1d);
            default:
                throw new IllegalStateException("Unexpected value: " + dataType);
        }
    }

    /** Runs the game. */
    public static void run() {
        initEngine();
        gameLoop();
    }

    /** Closes the game, without closing the JVM instance. */
    public static void closeGame() {
        display.close();
    }

    /**
     * Logs the specified message, using {@code System.out.println}.
     *
     * @param <T>     This allows for any type of message.
     * @param message The message to log.
     */
    public static <T> void log(T message) {
        System.out.println("INFO: " + message);
    }

    /**
     * Logs the specified warning message, using {@code System.err.println}.
     *
     * @param <T>            This allows for any type of warning message.
     * @param warningMessage The warning to log.
     */
    public static <T> void warning(T warningMessage) {
        System.err.println("WARNING: " + warningMessage);
    }

    /**
     * Closes the game, then throws the error specified with the error message.
     *
     * @param <T>          This allows for any type of error message.
     * @param errorMessage The error message to log.
     * @param exception    The exception that caused a need for this method call.
     */
    public static <T> void error(T errorMessage, Exception exception) {
        FastJEngine.closeGame();
        throw new IllegalStateException("ERROR: " + errorMessage, exception);
    }

    /** Initializes the game engine's components. */
    private static void initEngine() {
        runningCheck();
        isRunning = true;

        ThreadFixer.start();
        display.init();
        gameManager.setup(display);

        timer.init();
        fpsLogger = Executors.newSingleThreadScheduledExecutor();
        fpsLogger.scheduleWithFixedDelay(() -> {
            FastJEngine.logFPS(drawFrames);
            drawFrames = 0;
        }, 1, 1, TimeUnit.SECONDS);

        System.gc(); // yes, I really gc before starting.
        display.open();
    }

    /** Runs the game loop -- the heart of the engine. */
    private static void gameLoop() {
        float elapsedTime;
        float accumulator = 0f;
        float interval = 1f / targetUPS;

        while (!display.isClosed()) {
            elapsedTime = timer.getElapsedTime();
            accumulator += elapsedTime;

            gameManager.getCurrentScene().inputManager.processEvents(gameManager.getCurrentScene());

            while (accumulator >= interval) {
                gameManager.update(display);
                accumulator -= interval;
            }

            gameManager.render(display);
            drawFrames++;

            if (!display.isFullscreen()) {
                sync();
            }
        }

        exit();
    }

    /**
     * Syncs the game engine frame rate.
     * <p>
     * This provides, for a lack of better terms, "jank" way of emulating V-Sync when the game engine is not running in
     * fullscreen mode.
     */
    private static void sync() {
        final float loopSlot = 1f / targetFPS;
        final double endTime = timer.getLastLoopTime() + loopSlot;
        final double currentTime = timer.getTime();
        if (currentTime < endTime) {
            try {
                TimeUnit.MILLISECONDS.sleep((long) ((endTime - currentTime) * 1000L));
            } catch (InterruptedException ignored) {
            }
        }
    }

    /** Gracefully removes all resources created by the game engine. */
    private static void exit() {
        isRunning = false;
        fpsLogger.shutdownNow();
        gameManager.reset();

        Mouse.stop();
        Keyboard.stop();
        BehaviorManager.reset();
        TagManager.reset();

        // engine speed variables
        targetFPS = 0;
        targetUPS = 0;

        // FPS counting
        timer = null;
        fpsLog = null;
        drawFrames = 0;
        totalFPS = 0;
        fpsLogIndex = 0;
        fpsLogger = null;

        // HW acceleration
        hwAccel = null;

        // Display/Logic
        display = null;
        gameManager = null;

        // Check values
        isRunning = false;

        // Helpful? Debatable. Do I care? Not yet....
        System.gc();
    }

    /**
     * Logs the current frames rendered, displaying it on the {@code Display} if necessary.
     *
     * @param frames The count of frames rendered.
     */
    private static void logFPS(int frames) {
        if (display.isShowingFPSInTitle()) {
            display.setDisplayedTitle(String.format("%s | FPS: %d", display.getTitle(), frames));
        }

        storeFPS(frames);
    }

    /**
     * Stores the specified frames value in the engine's FPS log.
     *
     * @param frames The count of frames rendered.
     */
    private static void storeFPS(int frames) {
        fpsLog[fpsLogIndex % 100] = frames;
        fpsLogIndex++;
        totalFPS += frames;
    }
}
