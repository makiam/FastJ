package io.github.lucasstarsz.fastj.game;

import io.github.lucasstarsz.fastj.engine.io.Display;
import io.github.lucasstarsz.fastj.engine.systems.game.LogicManager;
import io.github.lucasstarsz.fastj.engine.systems.game.Scene;
import io.github.lucasstarsz.fastj.game.scenes.GameScene;

public class GameManager extends LogicManager {

    @Override
    public void setup(Display display) {
        Scene k = new GameScene("Game");
        this.addScene(k);
        this.setCurrentScene(k);
        this.loadCurrentScene();
    }
}
