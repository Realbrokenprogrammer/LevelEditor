package editor.entities;

import java.awt.image.BufferedImage;

public class GameObject {
	public double x;
	public double y;
	public double width = 32;
	public double height = 32;
	public String imageURL = "";
	public BufferedImage selectedPixels;
	public ObjectType type = null;
	private String objectName = "";
	public Property[] properties;
	
	public boolean contains(double pointX, double pointY) {
		if (pointX > x && pointX < x + width && pointY > y && pointY < y + height) {
			return true;
		}
		return false;
	}
	
	public void setObjectName(String objectName) {
		this.objectName = objectName;
		if (objectName == "grass") {
			Property[] p = {new Property("friction", "normal"),
					new Property("length", "short")};
			properties = p;
		}
	}
	
	public String getObjectName() {
		return objectName;
	}
}