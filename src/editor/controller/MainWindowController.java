package editor.controller;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

import editor.entities.GameObject;
import editor.entities.ObjectType;
import editor.entities.Pixel;
import editor.entities.Property;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;

public class MainWindowController implements LevelEditorController {

	@FXML
	public VBox root;
	@FXML
	public MenuItem menuFileNew;
	@FXML
	public Canvas canvas;
	@FXML
	public ScrollPane scrollPane;
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

	private ArrayList<ArrayList<GameObject>> levelMap;
	private LevelSettings levelSettings;
	private int currentLayer = 4;
	private int currentObjectType = 0;
	private GameObject currentObject = null;
	private GameObject dragObject = null;
	private ArrayList<GameObject> selectedObjects = null;
	private ArrayList<GameObject> clipboard = null;
	private boolean ctrlIsDown = false;
	private boolean altIsDown = false;
	private boolean snapToGrid = true;
	private GameObject[] allObjects;
	private double scale = 1.0;
	private double objectScale = 1.0;
	private double mouseX;
	private double mouseY;

	private Rectangle selectRect;
	private boolean isSelecting = false;

	private final int TILE_SIZE = 32;

	@FXML
	public void initialize() {
		AnchorPane.setTopAnchor(stackPane, 0.0);
		AnchorPane.setBottomAnchor(stackPane, 0.0);
		AnchorPane.setLeftAnchor(stackPane, 0.0);
		AnchorPane.setRightAnchor(stackPane, 0.0);

		initAllObjects();
		initObjectPanel();
		
		for (int i = 0; i < allObjects.length; i++) {
			allObjects[i].selectedPixels = sobel(new Image("file:" + allObjects[i].imageURL, 
					allObjects[i].width, 
					allObjects[i].height, 
					false, false));
		}

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

	public void setNewLevel(LevelSettings levelSettings) {
		this.levelSettings = levelSettings;
		this.levelMap = new ArrayList<ArrayList<GameObject>>();
		for (int i = 0; i < 8; i++) {
			levelMap.add(new ArrayList<GameObject>());
		}
		this.selectedObjects = new ArrayList<GameObject>();
		this.clipboard = new ArrayList<GameObject>();
		this.canvas.setWidth(levelSettings.width * TILE_SIZE + 15);
		this.canvas.setHeight(levelSettings.height * TILE_SIZE + 15);
		Stage stage = (Stage) this.canvas.getScene().getWindow();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		double width = screenSize.getWidth() * 0.7;
		double height = screenSize.getHeight() * 0.7;
		stage.setWidth(width);
		stage.setHeight(height);

		this.objectPanel.setMinHeight(height);
		this.propertyPanel.setMinHeight(height);
		

		canvas.setOnMouseMoved(e -> {
			mouseX = e.getX();
			mouseY = e.getY();
			drawGrid();
		});

		canvas.setOnMousePressed(e -> {
			mouseX = e.getX();
			mouseY = e.getY();
			if (e.getButton() == MouseButton.PRIMARY) {
				if (currentObject != null) {
					placeObject();
				} else {
					selectObject();
				}
			} else if (e.getButton() == MouseButton.SECONDARY) {
				selectObject();
				removeObject();
				drawGrid();
			}
		});

		canvas.setOnMouseDragged(e -> {
			mouseX = e.getX();
			mouseY = e.getY();
			if (!isSelecting && selectedObjects.size() > 0) { // dragging elected objects
				updateSelectedPosition();
			} else if (currentObject == null) { // dragging select rect
				if (selectRect == null) {
					selectRect = new Rectangle(mouseX, mouseY, 0, 0);
					isSelecting = true;
				} else {
					selectRect.setWidth(mouseX - selectRect.getX());
					selectRect.setHeight(mouseY - selectRect.getY());
					setObjectsWithinRect();
				}
			} else if (e.getButton() == MouseButton.PRIMARY && snapToGrid) {
				placeObject();
			}
			drawGrid();
		});

		canvas.setOnMouseReleased(e -> {
			isSelecting = false;
			dragObject = null;
			if (selectRect != null) {
				selectRect = null;
			}
		});

		canvas.getScene().setOnKeyPressed(e -> {
			if (e.getCode() == KeyCode.CONTROL) {
				ctrlIsDown = true;
			}
			if (e.getCode() == KeyCode.ALT) {
				altIsDown = true;
			}
			// Copy
			if (e.getCode() == KeyCode.C && ctrlIsDown) {
				clipboard.clear();
				for (int i = 0; i < selectedObjects.size(); i++) {
					clipboard.add(selectedObjects.get(i));
				}
			}
			// Paste
			if (e.getCode() == KeyCode.V && ctrlIsDown) {
				selectedObjects.clear();
				for (int i = 0; i < clipboard.size(); i++) {
					GameObject o = copyGameObject(clipboard.get(i));
					levelMap.get(currentLayer - 1).add(o);
					selectedObjects.add(o);
				}
				drawGrid();
			}
			if (e.getCode() == KeyCode.DELETE) {
				removeAllSelectedObjects();
				drawGrid();
			}
		});

		canvas.getScene().setOnKeyReleased(e -> {
			if (e.getCode() == KeyCode.CONTROL) {
				ctrlIsDown = false;
			}
			if (e.getCode() == KeyCode.ALT) {
				altIsDown = false;
			}
			if (e.getCode() == KeyCode.S) {
				if (snapToGrid) {
					snapToGrid = false;
				} else {
					snapToGrid = true;
				}
			}
		});

		layerCheckBox.setOnAction(e -> {
			drawGrid();
		});

		canvas.getScene().addEventFilter(ScrollEvent.ANY, new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				if (ctrlIsDown) {
					if (event.getDeltaY() > 0) {
						// Zoom in
						if (scale < 3) {
							scale += 0.2;
						}
					} else {
						// Zoom out
						if (scale > 0.6) {
							scale -= 0.2;
						}
					}
					// Resize canvas
					canvas.setWidth(levelSettings.width * TILE_SIZE * scale);
					canvas.setHeight(levelSettings.height * TILE_SIZE * scale);
					drawGrid();
				} else if (altIsDown) {
					if (currentObject != null) {
						if (event.getDeltaY() > 0) {
							if (objectScale < 5) {
								objectScale += 0.2;
							}
						} else {
							if (objectScale > 0.6) {
								objectScale -= 0.2;
							}
						}
						drawGrid();
					}
				} else {
					double delta = event.getDeltaY() / canvas.getHeight();
					scrollPane.setVvalue(scrollPane.getVvalue() - delta * 1.5);
				}
				event.consume();
			}
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
					p.setStyle("-fx-background-color: #101010");
				}
			});
			p.setOnMouseClicked(e -> {
				if (index < 9) {
					Text t = (Text) p.getChildren().get(0);
					currentLayer = Integer.parseInt(t.getText());
					for (int j = 1; j < list.size(); j++) {
						Pane pane = (Pane) list.get(j);
						pane.setStyle("-fx-background-color: #101010");
					}
					p.setStyle("-fx-background-color: #262626");
					drawGrid();
				} else if (index == list.size() - 2) {
					if (objectScroll.isVisible()) {
						objectScroll.setVisible(false);
					} else {
						objectScroll.setVisible(true);
					}
				} else if (index == list.size() - 1) {
					currentObject = null;
				}
			});
		}
	}

	private GameObject copyGameObject(GameObject o) {
		GameObject result = new GameObject();
		result.height = o.height;
		result.width = o.width;
		result.scale = o.scale;
		result.x = o.x + 10;
		result.y = o.y + 10;
		result.imageURL = o.imageURL;
		result.type = o.type;
		result.setObjectName(o.getObjectName());
		return result;
	}

	private void initAllObjects() {
		GameObject grass = new GameObject();
		grass.type = ObjectType.TILE;
		grass.setObjectName("grass");
		grass.imageURL = "res/sprites/" + grass.getObjectName() + ".png";
		grass.image = new Image("file:" + grass.imageURL);
		grass.selectedPixels = sobel(new Image("file:" + grass.imageURL, grass.width * scale, grass.height * scale, false, false));
		grass.width = 32;
		grass.height = 32;
		
		GameObject suit = new GameObject();
		suit.type = ObjectType.ENEMY;
		suit.setObjectName("suit");
		suit.imageURL = "res/sprites/" + suit.getObjectName() + ".png";
		suit.image = new Image("file:" + suit.imageURL);
		suit.selectedPixels = sobel(suit.image);
		suit.width = 64;
		suit.height = 128;
		
		GameObject[] temp = { grass, suit };
		allObjects = temp;
	}

	private void initObjectPanel() {
		objectPanel.setStyle("-fx-background-color: #CCCCCC");
		objectBar.setStyle("-fx-background-color: #FFFFFF");
		objectBar.setTranslateX(-10);

		addGameObjectContent(0);

		ObservableList<Node> list = objectBar.getChildren();
		list.get(0).setStyle("-fx-background-color: #CCCCCC");
		for (int i = 0; i < list.size(); i++) {
			final int index = i;
			Pane p = (Pane) list.get(i);
			p.setOnMouseEntered(e -> {
				p.setStyle("-fx-background-color: #CCCCCC");
			});
			p.setOnMouseExited(e -> {
				if (index != currentObjectType) {
					p.setStyle("-fx-background-color: #FFFFFF");
				}
			});
			p.setOnMouseClicked(e -> {
				if (index != currentObjectType) {
					list.get(currentObjectType).setStyle("-fx-background-color: #FFFFFF");
					currentObjectType = index;
					p.setStyle("-fx-background-color: #CCCCCC");
					for (int j = 1; j < objectPanel.getChildren().size(); j++) {
						objectPanel.getChildren().remove(j);
					}
					addGameObjectContent(index);
				}
			});
		}
	}

	private void addGameObjectContent(int index) {
		if (index == 0) {
			for (int i = 0; i < this.allObjects.length; i++) {
				if (allObjects[i].type == ObjectType.TILE) {
					final int objIndex = i;
					ImageView iv = new ImageView();
					objectPanel.setSpacing(10);
					objectPanel.setPadding(new Insets(0, 10, 10, 10));
					Image img = new Image("file:res/sprites/" + allObjects[i].getObjectName() + ".png");
					iv.setImage(img);
					objectPanel.getChildren().add(iv);
					iv.setOnMouseClicked(e -> {
						GameObject t = new GameObject();
						t.type = ObjectType.TILE;
						t.setObjectName(allObjects[objIndex].getObjectName());
						t.imageURL = "res/sprites/" + t.getObjectName() + ".png";
						t.width = allObjects[objIndex].width;
						t.height = allObjects[objIndex].height;
						currentObject = t;
						selectedObjects.clear();
					});
				}
			}
		} else if (index == 1) {
			for (int i = 0; i < this.allObjects.length; i++) {
				if (allObjects[i].type == ObjectType.ENEMY) {
					final int objIndex = i;
					ImageView iv = new ImageView();
					objectPanel.setSpacing(10);
					objectPanel.setPadding(new Insets(0, 10, 10, 10));
					Image img = new Image("file:res/sprites/" + allObjects[i].getObjectName() + ".png");
					iv.setImage(img);
					objectPanel.getChildren().add(iv);
					iv.setOnMouseClicked(e -> {
						GameObject t = new GameObject();
						t.type = ObjectType.TILE;
						t.setObjectName(allObjects[objIndex].getObjectName());
						t.imageURL = "res/sprites/" + t.getObjectName() + ".png";
						t.width = allObjects[objIndex].width;
						t.height = allObjects[objIndex].height;
						currentObject = t;
						selectedObjects.clear();
					});
				}
			}
		}
	}

	private void setObjectsWithinRect() {
		selectedObjects.clear();
		Rectangle r = getAdjustedRect();
		for (int i = 0; i < levelMap.get(currentLayer - 1).size(); i++) {
			GameObject o = levelMap.get(currentLayer - 1).get(i);
			if (r.intersects(o.x * scale, o.y * scale, o.width * scale, o.height * scale)) {
				selectedObjects.add(o);
			}
		}
	}

	private void selectObject() {
		for (int i = levelMap.get(currentLayer - 1).size() - 1; i >= 0; i--) {
			GameObject o = levelMap.get(currentLayer - 1).get(i);
			if (o.contains(mouseX / scale, mouseY / scale)) {
				if (!selectedObjects.contains(o)) {
					hideProperties();
					selectedObjects.clear();
					selectedObjects.add(o);
					showProperties();
				}
				drawGrid();
				return;
			}
		}
		hideProperties();
		selectedObjects.clear();
		drawGrid();
	}
	
	private void showProperties() {
		propertyScroll.setVisible(true);
		while (propertyPanel.getChildren().size() > 1) {
			propertyPanel.getChildren().remove(1);
		}
		for (int i = 0; i < selectedObjects.get(0).properties.length; i++) {
			Property p = selectedObjects.get(0).properties[i];
			HBox hbox = new HBox();
			hbox.setSpacing(5);
			hbox.setPadding(new Insets(0, 0, 0, 5));
			Text t = new Text(p.name + ":");
			TextField tf = new TextField();
			tf.setText(p.value);
			hbox.getChildren().add(t);
			hbox.getChildren().add(tf);
			propertyPanel.getChildren().add(hbox);
		}
	}
	
	private void hideProperties() {
		propertyScroll.setVisible(false);
		if (selectedObjects.size() > 0) {
			for (int i = 1; i < propertyPanel.getChildren().size(); i++) {
				HBox hbox = (HBox) propertyPanel.getChildren().get(i);
				TextField tf = (TextField) hbox.getChildren().get(1);
				selectedObjects.get(0).properties[i - 1].value = tf.getText();
			}	
		}
		while (propertyPanel.getChildren().size() > 1) {
			propertyPanel.getChildren().remove(1);
		}
	}
	
	private void removeAllSelectedObjects() {
		for (int i = 0; i < selectedObjects.size(); i++) {
			levelMap.get(currentLayer - 1).remove(selectedObjects.get(i));
		}
		selectedObjects.clear();
	}

	private void removeObject() {
		for (int i = levelMap.get(currentLayer - 1).size() - 1; i >= 0; i--) {
			if (levelMap.get(currentLayer - 1).get(i).contains(mouseX / scale, mouseY / scale)) {
				if (selectedObjects.contains(levelMap.get(currentLayer - 1).get(i))) {
					selectedObjects.remove(levelMap.get(currentLayer - 1).get(i));
				}
				levelMap.get(currentLayer - 1).remove(i);
				break;
			}
		}
	}

	private void placeObject() {
		int x = (int) (mouseX / (TILE_SIZE * scale));
		int y = (int) (mouseY / (TILE_SIZE * scale));
		GraphicsContext g = this.canvas.getGraphicsContext2D();
		if (currentObject != null) {
			GameObject t = new GameObject();
			t.type = currentObject.type;
			t.setObjectName(currentObject.getObjectName());
			t.imageURL = "res/sprites/" + t.getObjectName() + ".png";
			t.width = currentObject.width * objectScale;
			t.height = currentObject.height * objectScale;
			t.scale = objectScale;

			if (snapToGrid) {
				t.x = x * TILE_SIZE;
				t.y = y * TILE_SIZE;
			} else {
				t.x = mouseX / scale - t.width / 2;
				t.y = mouseY / scale - t.height / 2;
			}

			if (isPositionFree(t.x, t.y)) {
				Image img = new Image("file:" + t.imageURL, t.width * scale, t.height * scale, false, false);
				g.drawImage(img, t.x * scale, t.y * scale);
				levelMap.get(currentLayer - 1).add(t);
			}
		}
	}

	private boolean isPositionFree(double x, double y) {
		for (int i = 0; i < levelMap.get(currentLayer - 1).size(); i++) {
			GameObject o = levelMap.get(currentLayer - 1).get(i);
			if (o.x == x && o.y == y) {
				return false;
			}
		}
		return true;
	}

	private void drawGrid() {
		int x = (int) (mouseX / (TILE_SIZE * scale));
		int y = (int) (mouseY / (TILE_SIZE * scale));

		GraphicsContext g = this.canvas.getGraphicsContext2D();
		g.clearRect(0, 0, this.canvas.getWidth(), this.canvas.getHeight());
		g.setStroke(Color.GRAY);
		g.scale(scale, scale);

		// Draw grid
		for (int i = 0; i < levelSettings.width; i++) {
			for (int j = 0; j < levelSettings.height; j++) {
				g.strokeRect(i * TILE_SIZE, j * TILE_SIZE, TILE_SIZE, TILE_SIZE);
			}
		}

		// Draw images for all objects
		if (layerCheckBox.isSelected()) {
			for (int i = 0; i < levelMap.get(currentLayer - 1).size(); i++) {
				GameObject t = levelMap.get(currentLayer - 1).get(i);
				for (int j = 0; j < allObjects.length; j++) {
					if (t.getObjectName() == allObjects[j].getObjectName()) {
						g.scale(t.scale, t.scale);
						g.drawImage(allObjects[j].image, t.x, t.y);
						g.scale(1 / t.scale, 1 / t.scale);
					}
				}
			}
		} else {
			for (int i = 0; i < levelMap.size(); i++) {
				for (int j = 0; j < levelMap.get(i).size(); j++) {
					GameObject t = levelMap.get(i).get(j);
					for (int k = 0; k < allObjects.length; k++) {
						if (t.getObjectName() == allObjects[k].getObjectName()) {
							g.scale(t.scale, t.scale);
							g.drawImage(allObjects[k].image, t.x / t.scale, t.y / t.scale);
							g.scale(1 / t.scale, 1 / t.scale);	
						}
					}
				}
			}
		}

		// Draw selected objects
		for (int i = 0; i < selectedObjects.size(); i++) {
			GameObject t = selectedObjects.get(i);
			WritableImage img = null;
			for (int j = 0; j < allObjects.length; j++) {
				if (t.getObjectName() == allObjects[j].getObjectName()) {
					img = allObjects[j].selectedPixels;
					break;
				}
			}
			double s = 10.0 * ((t.width) / (double) (img.getWidth() - 10.0));
			g.scale(t.scale, t.scale);
			g.drawImage(img, (t.x - s / 2) / t.scale, (t.y - s / 2) / t.scale);
			g.scale(1 / t.scale, 1 / t.scale);
		}

		// Draw select rectangle
		if (selectRect != null) {
			g.setFill(new Color(0, 0, 1, 0.2));
			Rectangle r = getAdjustedRect();
			g.fillRect(r.getX() / scale, r.getY() / scale, r.getWidth() / scale, r.getHeight() / scale);
		}

		// Draw current object at cursor
		if (currentObject != null) {
			GameObject t = currentObject;
			Image img = new Image("file:" + t.imageURL, t.width * objectScale, t.height * objectScale, false, false);
			if (snapToGrid) {
				g.drawImage(img, x * TILE_SIZE, y * TILE_SIZE);
			} else {
				g.drawImage(img, mouseX - (t.width) / 2, mouseY - (t.height) / 2);
			}
		}
		
		g.scale(1 / scale, 1 / scale);
	} 

	private Rectangle getAdjustedRect() {
		double rY = selectRect.getY();
		double rX = selectRect.getX();
		double width = selectRect.getWidth();
		double height = selectRect.getHeight();
		if (width < 0) {
			rX += width;
			width *= -1;
		}
		if (height < 0) {
			rY += height;
			height *= -1;
		}
		return new Rectangle(rX, rY, width, height);
	}

	private void updateSelectedPosition() {
		int x = (int) (mouseX / (TILE_SIZE * scale));
		int y = (int) (mouseY / (TILE_SIZE * scale));
		if (dragObject == null) {
			for (int i = 0; i < selectedObjects.size(); i++) {
				if (selectedObjects.get(i).contains(mouseX / scale, mouseY / scale)) {
					dragObject = selectedObjects.get(i);
					break;
				}
			}
		}

		double deltaX;
		double deltaY;
		if (snapToGrid) {
			deltaX = x * TILE_SIZE - dragObject.x;
			deltaY = y * TILE_SIZE - dragObject.y;
		} else {
			deltaX = (mouseX / scale - dragObject.width / 2) - dragObject.x;
			deltaY = (mouseY / scale - dragObject.height / 2) - dragObject.y;
		}

		for (int i = 0; i < selectedObjects.size(); i++) {
			selectedObjects.get(i).y += deltaY;
			selectedObjects.get(i).x += deltaX;
		}
	}

	private WritableImage sobel(Image img) {
		PixelReader pr = img.getPixelReader();
		Pixel[][] grayscale = new Pixel[(int) img.getWidth() + 10][(int) img.getHeight() + 10];
		Pixel[][] result = new Pixel[(int) img.getWidth() + 10][(int) img.getHeight() + 10];
		
		for (int x = 0; x < img.getWidth(); x++) {
			for (int y = 0; y < img.getHeight(); y++) {
				if (pr.getColor(x, y).getOpacity() > 0.05) {
					grayscale[x+5][y+5] = new Pixel(x, y, Color.WHITE);
				} else {
					grayscale[x+5][y+5] = new Pixel(x, y, Color.BLACK);
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
				Color topRight = grayscale[x + 1][y - 1].color;
				Color middleRight = grayscale[x + 1][y].color;
				Color bottomRight = grayscale[x + 1][y + 1].color;
				Color topLeft = grayscale[x - 1][y - 1].color;
				Color middleLeft = grayscale[x - 1][y].color;
				Color bottomLeft = grayscale[x - 1][y + 1].color;
				Color topCenter = grayscale[x][y - 1].color;
				Color bottomCenter = grayscale[x][y + 1].color;

				double val1 = topLeft.getRed() * -1;
				val1 += middleLeft.getRed() * -2;
				val1 += bottomLeft.getRed() * -1;
				val1 += topRight.getRed();
				val1 += middleRight.getRed() * 2;
				val1 += bottomRight.getRed();

				double val2 = topLeft.getRed() * -1;
				val2 += topCenter.getRed() * -2;
				val2 += topRight.getRed() * -1;
				val2 += bottomLeft.getRed() * 1;
				val2 += bottomCenter.getRed() * 2;
				val2 += bottomRight.getRed() * 1;

				double val = Math.sqrt(val1 * val1 + val2 * val2);
				if (val > 1) {
					val = 1;
				}
				result[x][y] = new Pixel(x, y, new Color(val, 0, 0, val));
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
