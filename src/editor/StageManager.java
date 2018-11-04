package editor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import editor.controller.LevelEditorController;
import editor.controller.MainWindowController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class StageManager {
	
	private static StageManager instance;
	private Stage mainStage;
	
	private StageManager() {
		
	}
	
	public static StageManager getInstance() {
		if (instance == null) {
			instance = new StageManager();
		}
		return instance;
	}
	
	private Map<String, LevelEditorController> controllers = new HashMap<>();
	
	public MainWindowController getMainWindowController() {
		return (MainWindowController) controllers.get(LevelEditorController.MAIN_WINDOW);
	}
	
	public void showLevelEditor(Stage primaryStage) throws IOException {
		mainStage = primaryStage;
		mainStage.setTitle("Level Editor");
		
		VBox mainWindow = (VBox) loadLayout(LevelEditorController.MAIN_WINDOW);
		Scene mainScene = new Scene(mainWindow);
		
		mainStage.setScene(mainScene);
		mainStage.show();
	}
	
	private Parent loadLayout(String layout) throws IOException {
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getResource(layout));
		Parent nodeLayout = loader.load();
		
		controllers.put(layout, loader.getController());
		
		return nodeLayout;
	}
}
