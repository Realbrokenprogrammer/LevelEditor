package editor;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class LevelEditor extends Application {

	public static void main(String[] args) {
		launch();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.getIcons().add(new Image("file:res/icon.png"));
		StageManager sm = StageManager.getInstance();
		sm.showLevelEditor(primaryStage);
	}
}
