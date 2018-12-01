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
	public String objectName = "";
	
	public boolean contains(double pointX, double pointY) {
		if (pointX > x && pointX < x + width && pointY > y && pointY < y + height) {
			return true;
		}
		return false;
	}
}
