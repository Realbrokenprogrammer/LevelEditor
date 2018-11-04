package editor;

import javafx.application.Application;
import javafx.stage.Stage;

public class LevelEditor extends Application {

	public static void main(String[] args) {
		launch();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		StageManager sm = StageManager.getInstance();
		sm.showLevelEditor(primaryStage);
	}
}
