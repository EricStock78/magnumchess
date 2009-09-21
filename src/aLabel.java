import javax.swing.*;
import java.awt.LayoutManager;
public final class aLabel extends JLabel {
	private int piece;
	public aLabel(Icon image,int p) {
		super(image, SwingConstants.CENTER);
		piece = p;
	}
	public aLabel() {
		super();
		piece = -1;			//no piece on label
	}
	
	
	public void setPiece(int st) {
		piece = st;
	}
	public int getPiece() {
		return piece;
	}
}