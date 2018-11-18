package editor.entities;

import javafx.scene.paint.Color;

public class Pixel {	
	public int x;
	public int y;
	public Color color;
	
	public Pixel(int x, int y, Color color) {
		this.x = x;
		this.y = y;
		this.color = color;
	}
	
	public double angleTo(Pixel p) {
		double angle = Math.atan2(p.y - this.y, p.x - this.y);
		if(angle < 0) {
			angle += Math.PI * 2;
		}
		return angle;
	}
}
