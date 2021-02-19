package io.github.lucasstarsz.fastj.ui;

import io.github.lucasstarsz.fastj.io.mouse.MouseActionListener;
import io.github.lucasstarsz.fastj.render.Camera;
import io.github.lucasstarsz.fastj.graphics.Drawable;
import io.github.lucasstarsz.fastj.systems.game.Scene;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A drawable to be used as UI.
 */
public abstract class UIElement extends Drawable implements MouseActionListener {

    private final List<Consumer<MouseEvent>> onActionEvents;
    private EventCondition onActionCondition;

    /**
     * Instantiates the UIElement's internals, and adds it to the origin scene as a ui element/mouse listener.
     *
     * @param origin The scene which this UIElement is tied to.
     */
    protected UIElement(Scene origin) {
        onActionEvents = new ArrayList<>();

        origin.addGUIObject(this);
        origin.addMouseActionListener(this);
    }

    /**
     * Sets the "onAction" condition, the determinant of whether the ui elements' {@code onAction} events will be
     * fired.
     *
     * @param condition The condition to set.
     */
    protected void setOnActionCondition(EventCondition condition) {
        onActionCondition = condition;
    }

    /**
     * Sets the UIElement's {@code onAction} event to the specified action.
     *
     * @param action The action to set.
     * @return The {@code UIElement}, for method chaining.
     */
    public UIElement setOnAction(Consumer<MouseEvent> action) {
        onActionEvents.clear();
        onActionEvents.add(action);
        return this;
    }

    /**
     * Adds the specified action to the UIElement's {@code onAction} events.
     *
     * @param action The action to add.
     * @return The {@code UIElement}, for method chaining.
     */
    public UIElement addOnAction(Consumer<MouseEvent> action) {
        onActionEvents.add(action);
        return this;
    }

    /**
     * Renders the {@code UIElement} to the parameter {@code Graphics2D} object, aligning with the window by rendering
     * at the inverse translation of the specified {@code Camera}.
     *
     * @param g      {@code Graphics2D} parameter that the {@code UIElement} will be rendered to.
     * @param camera {@code Camera} to help render at the correct position on the screen.
     */
    public abstract void renderAsGUIObject(Graphics2D g, Camera camera);

    /**
     * Removes the {@code UIElement}'s references in the specified scene as a GUI object and as a mouse listener.
     *
     * @param origin {@code Scene} parameter that will have all references to this {@code UIElement} removed.
     */
    @Override
    protected void destroyTheRest(Scene origin) {
        super.destroyTheRest(origin);

        origin.removeGUIObject(this);
        origin.removeMouseActionListener(this);
    }

    /**
     * Fires the ui element's {@code onAction} event(s), if its condition is met.
     *
     * @param mouseEvent The mouse event causing the {@code onAction} event(s) to be fired.
     */
    @Override
    public void onMousePressed(MouseEvent mouseEvent) {
        if (onActionCondition.condition(mouseEvent)) {
            onActionEvents.forEach(action -> action.accept(mouseEvent));
        }
    }
}