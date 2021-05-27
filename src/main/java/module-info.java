/**
 * The FastJ Library in its entirety.
 * <p>
 * FastJ does not use any external dependencies -- it relies entirely on Java 11's {@code java.desktop} module, using
 * AWT and Swing to display output.
 * <p>
 * This game library is split into two main parts:
 * <ul>
 *     <li>the framework with which to build games (every package except {@link io.github.lucasstarsz.fastj.engine}), and</li>
 *     <li>the engine running it (just package {@code engine}).</li>
 * </ul>
 * <p>
 * For more information, check out
 * <a href="https://github.com/lucasstarsz/FastJ-Engine" target="_blank">the github repository.</a>
 */
module fastj.library {
    requires transitive java.desktop;

    exports tech.fastj.engine;
    exports tech.fastj.math;

    exports tech.fastj.graphics;
    exports tech.fastj.graphics.game;
    exports tech.fastj.graphics.ui;
    exports tech.fastj.graphics.ui.elements;

    exports tech.fastj.systems.input;
    exports tech.fastj.systems.input.keyboard;
    exports tech.fastj.systems.input.mouse;

    exports tech.fastj.systems.behaviors;
    exports tech.fastj.systems.control;
    exports tech.fastj.systems.tags;
}
