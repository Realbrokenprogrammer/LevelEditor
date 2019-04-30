package editor.entities;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;

public class GameObject {
	public double x;
	public double y;
	public double width = 32;
	public double height = 32;
	public double scale = 1.0;
	public String imageURL = "";
	public Image image = null;
	public WritableImage selectedPixels;
	public WritableImage highlightPixels;
	public String type = null;
	private String objectName = "";
	public Property[] properties;

	public boolean contains(double pointX, double pointY) {
		if (pointX > x - 1 && pointX < x + width + 2 && pointY > y - 1 && pointY < y + height + 2) {
			return true;
		}
		return false;
	}

	public boolean overlaps(GameObject o) {
		double xmin = Math.max(x, o.x);
		double xmax1 = x + width;
		double xmax2 = o.x + o.width;
		double xmax = Math.min(xmax1, xmax2);
		if (xmax > xmin) {
			double ymin = Math.max(y, o.y);
			double ymax1 = y + height;
			double ymax2 = o.y + o.height;
			double ymax = Math.min(ymax1, ymax2);
			if (ymax > ymin) {
				return true;
			}
		}
		return false;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
		if (objectName == "grass") {
			Property[] p = { new Property("friction", "normal"), new Property("length", "short") };
			properties = p;
		} else if (objectName == "suit") {
			Property[] p = { new Property("color", "black") };
			properties = p;
		} else {
			properties = new Property[0];
		}
	}

	public String getObjectName() {
		return objectName;
	}
}