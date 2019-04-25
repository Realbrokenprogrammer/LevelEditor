package editor.entities;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.shape.Rectangle;

public class GameObject {
	public double x;
	public double y;
	public double width = 32;
	public double height = 32;
	public double scale = 1.0;
	public String imageURL = "";
	public Image image = null;
	public WritableImage selectedPixels;
	public ObjectType type = null;
	private String objectName = "";
	public Property[] properties;
	
	public boolean contains(double pointX, double pointY) {
		if (pointX > x && pointX < x + width && pointY > y && pointY < y + height) {
			return true;
		}
		return false;
	}
	
	public boolean overlaps(GameObject o) {
		Rectangle r1 = new Rectangle(x, y, width, height);
		return r1.intersects(o.x + 1, o.y + 1, o.width - 2, o.height - 2);
	}
	
	public void setObjectName(String objectName) {
		this.objectName = objectName;
		if (objectName == "grass") {
			Property[] p = {new Property("friction", "normal"),
					new Property("length", "short")};
			properties = p;
		} else if (objectName == "suit") {
			Property[] p = {new Property("color", "black")};
			properties = p;
		}
	}
	
	public String getObjectName() {
		return objectName;
	}
}