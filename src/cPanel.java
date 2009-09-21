import javax.swing.*;
import java.awt.LayoutManager;
import java.awt.*;
public final class cPanel extends JPanel {
	private int index;
	public cPanel(LayoutManager layout,int i) {
		
		super(layout);
		int row, col;
		Color lightB;
		Color superB;
		float r,g,b;
	  
		r = (float)0.7;
		g = (float)0.8;
		b = (float)1.0;
		lightB = new Color(r,g,b);
		r = (float)0.95;
		g = (float)0.95;
		b = (float)1.0;
		
		superB = new Color(r,g,b);

		index = i;
		row = i/8;
		col = i%8;
		if(row%2==0) {
			if(col%2==0) {
				setBackground(lightB);
			}
			else {
				setBackground(superB);
			}
		}	
		else {
			if(col%2==0) {
				setBackground(superB);
			}
			else {
				setBackground(lightB);
			}
		}
	}			
	public int getIndex() {
		return index;
}
}