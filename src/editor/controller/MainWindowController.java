package editor.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import editor.entities.GameObject;
import editor.entities.Pixel;
import editor.ui.LevelPane;
import io.Level;
import io.LevelFileManager;
import io.LevelSettings;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainWindowController implements LevelEditorController {

	@FXML
	public VBox root;
	@FXML
	public MenuItem menuFileNew;
	@FXML
	public MenuItem menuFileOpen;
	@FXML
	public MenuItem menuFileSave;
	@FXML
	public MenuItem menuEditDelete;
	@FXML
	public MenuItem menuEditCopy;
	@FXML
	public MenuItem menuEditPaste;
	@FXML
	public MenuItem menuEditUndo;
	@FXML
	public MenuItem menuEditRedo;
	@FXML
	public MenuItem menuEditChangeSize;
	@FXML
	public AnchorPane anchorPane;
	@FXML
	public HBox layerBar;
	@FXML
	public VBox objectPanel;
	@FXML
	public VBox propertyPanel;
	@FXML
	public StackPane stackPane;
	@FXML
	public ScrollPane objectScroll;
	@FXML
	public ScrollPane propertyScroll;
	@FXML
	public HBox objectBar;
	@FXML
	public CheckBox layerCheckBox;
	@FXML
	public CheckBox overlapCheckBox;

	private LevelPane levelPane;

	private int currentLayer = 4;
	private int currentObjectType = 0;
	private GameObject[] allObjects;
	private LevelFileManager levelFileManager;
	public File openedFile;

	private int tileSize = 32;

	@FXML
	public void initialize() {
		levelFileManager = new LevelFileManager();
		AnchorPane.setTopAnchor(stackPane, 0.0);
		AnchorPane.setBottomAnchor(stackPane, 0.0);
		AnchorPane.setLeftAnchor(stackPane, 0.0);
		AnchorPane.setRightAnchor(stackPane, 0.0);

		initAllObjects();
		initObjectPanel();
		
		menuFileSave.setOnAction(e -> {
			levelPane.save(openedFile);
		});
		
		menuEditDelete.setOnAction(e -> {
			levelPane.deleteSelected();
		});
		
		menuEditCopy.setOnAction(e -> {
			levelPane.copyToClipboard();
		});
		
		menuEditPaste.setOnAction(e -> {
			levelPane.pasteClipboard();
		});
		
		menuEditUndo.setOnAction(e -> {
			levelPane.eventHandler.undo(levelPane.levelMap);
		});
		
		menuEditRedo.setOnAction(e -> {
			levelPane.eventHandler.redo(levelPane.levelMap);
		});
		
		menuEditChangeSize.setOnAction(e -> {
			Parent root;
			URI location = new File("res/" + LEVEL_SETTINGS_WINDOW).toURI();
			LevelSettingsWindowController levelSettingsWindowController;

			try {
				FXMLLoader loader = new FXMLLoader(location.toURL());
				root = loader.load();

				Stage stage = new Stage();
				stage.setTitle("Level Settings");
				stage.setScene(new Scene(root));

				levelSettingsWindowController = loader.<LevelSettingsWindowController>getController();
				levelSettingsWindowController.getUpdatedSettings(levelPane);

				stage.initModality(Modality.APPLICATION_MODAL);
				stage.show();
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		});
		
		menuFileOpen.setOnAction(e -> {
			FileChooser fc = new FileChooser();
			openedFile = fc.showOpenDialog(root.getScene().getWindow());
			Level level = levelFileManager.loadFile(openedFile);
			setNewLevel(level.levelSettings);
			levelPane.levelMap = level.levelMap;
			levelPane.draw();
		});

		menuFileNew.setOnAction(e -> {
			Parent root;
			URI location = new File("res/" + LEVEL_SETTINGS_WINDOW).toURI();
			LevelSettingsWindowController levelSettingsWindowController;

			try {
				FXMLLoader loader = new FXMLLoader(location.toURL());
				root = loader.load();

				Stage stage = new Stage();
				stage.setTitle("Level Settings");
				stage.setScene(new Scene(root));

				levelSettingsWindowController = loader.<LevelSettingsWindowController>getController();
				levelSettingsWindowController.getSettings(this);

				stage.initModality(Modality.APPLICATION_MODAL);
				stage.show();
			} catch (IOException exception) {
				exception.printStackTrace();
			}
		});
	}

	private void resetLevelPane() {
		this.stackPane.getChildren().remove(levelPane);
		this.levelPane = new LevelPane(this);
		this.levelPane.setAllObjects(allObjects);
		this.stackPane.getChildren().add(0, levelPane);
	}

	public void setNewLevel(LevelSettings levelSettings) {
		Stage stage = (Stage) this.root.getScene().getWindow();
		stage.setMaximized(true);
		resetLevelPane();
		int width = levelSettings.width * levelSettings.tileSize;
		int height = levelSettings.height * levelSettings.tileSize;
		this.levelPane.setMapSize(levelSettings, width, height);
		this.objectPanel.setMinHeight(stage.getHeight());
		this.propertyPanel.setMinHeight(stage.getHeight());

		layerCheckBox.setOnAction(e -> {
			levelPane.setShowOnlyCurrentLayer(layerCheckBox.isSelected());
			levelPane.draw();
		});

		overlapCheckBox.setOnAction(e -> {
			levelPane.setHighlightOverlaps(overlapCheckBox.isSelected());
			levelPane.draw();
		});

		ObservableList<Node> list = layerBar.getChildren();
		Pane firstPane = (Pane) list.get(currentLayer);
		firstPane.setStyle("-fx-background-color: #262626");
		for (int i = 1; i < list.size(); i++) {
			Pane p = (Pane) list.get(i);
			final int index = i;
			p.setOnMouseEntered(e -> {
				p.setStyle("-fx-background-color: #262626");
			});
			p.setOnMouseExited(e -> {
				if (index != currentLayer) {
					p.setStyle("-fx-background-color: #171717");
				}
			});
			p.setOnMouseClicked(e -> {
				if (index < 9) {
					Text t = (Text) p.getChildren().get(0);
					currentLayer = Integer.parseInt(t.getText());
					for (int j = 1; j < list.size(); j++) {
						Pane pane = (Pane) list.get(j);
						pane.setStyle("-fx-background-color: #171717");
					}
					p.setStyle("-fx-background-color: #262626");
				} else if (index == list.size() - 2) {
					if (objectScroll.isVisible()) {
						objectScroll.setVisible(false);
					} else {
						objectScroll.setVisible(true);
					}
				} else if (index == list.size() - 1) {
					levelPane.setCurrentObject(null);
				}
				levelPane.setCurrentLayer(currentLayer);
				levelPane.draw();
			});
		}
	}

	private void initAllObjects() {
		File[] types = new File("./res/sprites").listFiles();
		ArrayList<GameObject> temp = new ArrayList<GameObject>();

		for (int i = 0; i < types.length; i++) {
			File[] files = types[i].listFiles();
			for (int j = 0; j < files.length; j++) {
				GameObject o = new GameObject();
				o.type = types[i].getName();
				o.imageURL = files[j].getAbsolutePath();
				o.setObjectName(files[j].getName().replaceAll(".png", ""));
				Image img = new Image("file:" + o.imageURL);
				o.image = img;
				o.selectedPixels = sobel(o.image, Color.RED);
				o.highlightPixels = sobel(o.image, Color.BLUE);
				o.width = img.getWidth();
				o.height = img.getHeight();
				temp.add(o);
			}
		}

		allObjects = new GameObject[temp.size()];

		for (int i = 0; i < allObjects.length; i++) {
			allObjects[i] = temp.get(i);
		}
	}

	private void initObjectPanel() {
		objectPanel.setStyle("-fx-background-color: #CCCCCC");
		objectBar.setStyle("-fx-background-color: #FFFFFF");
		objectBar.setTranslateX(-10);

		ArrayList<String> tabs = new ArrayList<String>();
		ObservableList<Node> list = objectBar.getChildren();

		for (int i = 0; i < allObjects.length; i++) {
			if (!tabs.contains(allObjects[i].type)) {
				tabs.add(allObjects[i].type);
				int tabIndex = tabs.indexOf(allObjects[i].type);
				HBox p = new HBox();
				p.setPadding(new Insets(5, 5, 5, 5));
				Text t = new Text(allObjects[i].type);
				p.setPrefHeight(30);
				p.getChildren().add(t);
				list.add(p);

				if (tabIndex == currentObjectType) {
					p.setStyle("-fx-background-color: #CCCCCC");
					addGameObjectContent(tabIndex, tabs);
				}
				p.setOnMouseEntered(e -> {
					p.setStyle("-fx-background-color: #CCCCCC");
				});
				p.setOnMouseExited(e -> {
					if (tabIndex != currentObjectType) {
						p.setStyle("-fx-background-color: #FFFFFF");
					}
				});
				p.setOnMouseClicked(e -> {
					if (tabIndex != currentObjectType) {
						list.get(currentObjectType).setStyle("-fx-background-color: #FFFFFF");
						currentObjectType = tabIndex;
						p.setStyle("-fx-background-color: #CCCCCC");
						while (objectPanel.getChildren().size() > 1) {
							objectPanel.getChildren().remove(1);
						}
						addGameObjectContent(tabIndex, tabs);
					}
				});
			}
		}
	}

	private void addGameObjectContent(int tabIndex, ArrayList<String> tabs) {
		for (int i = 0; i < allObjects.length; i++) {
			if (allObjects[i].type.equals(tabs.get(tabIndex))) {
				final int objIndex = i;
				HBox hbox = new HBox();
				hbox.setSpacing(10);
				Text text = new Text(allObjects[i].getObjectName());
				ImageView iv = new ImageView();
				objectPanel.setSpacing(10);
				objectPanel.setPadding(new Insets(0, 10, 10, 10));
				Image img = new Image("file:" + allObjects[i].imageURL, 32, 32, false, false);
				iv.setImage(img);
				hbox.getChildren().addAll(iv, text);
				objectPanel.getChildren().add(hbox);
				hbox.setOnMouseClicked(e -> {
					GameObject t = new GameObject();
					t.type = tabs.get(tabIndex);
					t.setObjectName(allObjects[objIndex].getObjectName());
					t.imageURL = allObjects[objIndex].imageURL;
					t.width = allObjects[objIndex].width;
					t.height = allObjects[objIndex].height;
					levelPane.setCurrentObject(t);
				});
			}
		}
	}

	private WritableImage sobel(Image img, Color color) {
		PixelReader pr = img.getPixelReader();
		Pixel[][] grayscale = new Pixel[(int) img.getWidth() + 10][(int) img.getHeight() + 10];
		Pixel[][] result = new Pixel[(int) img.getWidth() + 10][(int) img.getHeight() + 10];

		for (int x = 0; x < img.getWidth(); x++) {
			for (int y = 0; y < img.getHeight(); y++) {
				if (pr.getColor(x, y).getOpacity() > 0.05) {
					grayscale[x + 5][y + 5] = new Pixel(x, y, Color.WHITE);
				} else {
					grayscale[x + 5][y + 5] = new Pixel(x, y, Color.BLACK);
				}
			}
		}

		for (int x = 0; x < grayscale.length; x++) {
			for (int y = 0; y < grayscale[0].length; y++) {
				if (grayscale[x][y] == null) {
					grayscale[x][y] = new Pixel(x, y, Color.BLACK);
				}
			}
		}

		for (int x = 1; x < grayscale.length - 1; x++) {
			for (int y = 1; y < grayscale[0].length - 1; y++) {
				double val1 = grayscale[x - 1][y - 1].color.getRed() * -1;
				val1 += grayscale[x - 1][y].color.getRed() * -2;
				val1 += grayscale[x - 1][y + 1].color.getRed() * -1;
				val1 += grayscale[x + 1][y - 1].color.getRed();
				val1 += grayscale[x + 1][y].color.getRed() * 2;
				val1 += grayscale[x + 1][y + 1].color.getRed();

				double val2 = grayscale[x - 1][y - 1].color.getRed() * -1;
				val2 += grayscale[x][y - 1].color.getRed() * -2;
				val2 += grayscale[x + 1][y - 1].color.getRed() * -1;
				val2 += grayscale[x - 1][y + 1].color.getRed() * 1;
				val2 += grayscale[x][y + 1].color.getRed() * 2;
				val2 += grayscale[x + 1][y + 1].color.getRed() * 1;

				double val = Math.sqrt(val1 * val1 + val2 * val2);
				if (val > 1) {
					val = 1;
				}
				result[x][y] = new Pixel(x, y, new Color(color.getRed(), color.getGreen(), color.getBlue(), val));
			}
		}

		WritableImage bfimg = new WritableImage(result.length, result[0].length);
		PixelWriter g = bfimg.getPixelWriter();
		for (int x = 0; x < result.length; x++) {
			for (int y = 0; y < result[0].length; y++) {
				Color c = null;
				if (result[x][y] == null) {
					c = new Color(0, 0, 0, 0);
				} else {
					double red = result[x][y].color.getRed();
					double green = result[x][y].color.getGreen();
					double blue = result[x][y].color.getBlue();
					double op = result[x][y].color.getOpacity();
					c = new Color(red, green, blue, op);
				}
				g.setColor(x, y, c);
			}
		}

		return bfimg;
	}
}
