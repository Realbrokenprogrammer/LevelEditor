package editor.controller;

import editor.StageManager;

public interface LevelEditorController {
	
	String LAYOUTS_PATH = "/";
	
	String MAIN_WINDOW = LAYOUTS_PATH + "MainWindow.fxml";
	
	StageManager stageManager = StageManager.getInstance();
}
