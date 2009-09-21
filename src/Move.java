

public class Move implements Comparable<Move>{
	private boolean capture;
	private boolean promo;				//promotion flag
	private int capPiece;
	private int thePiece;
	private int to;
	private int from;
	private int value;
	private int passantB;				//passant squares for the move
	private int passantW;
	private int drawCount;			//50 move draw variable
	private boolean lwCastle;
	private boolean rwCastle;
	private boolean lbCastle;
	private boolean rbCastle;
	
	public Move(boolean c,int cP,int tP,int o,int f,boolean lw,boolean rw,boolean lb,boolean rb,int pB,int pW,boolean p) {
		capture = c;
		capPiece = cP;
		thePiece = tP;
		to = o;
		from = f;
		lwCastle = lw;
		rwCastle = rw;
		lbCastle = lb;
		rbCastle = rb;
		passantB = pB;
		passantW = pW;
		promo = p;
		drawCount=0;
	}
	public Move(boolean c,int cP,int tP,int o,int f,boolean lw,boolean rw,boolean lb,boolean rb,int pB,int pW,boolean p,int d) {
		capture = c;
		capPiece = cP;
		thePiece = tP;
		to = o;
		from = f;
		lwCastle = lw;
		rwCastle = rw;
		lbCastle = lb;
		rbCastle = rb;
		passantB = pB;
		passantW = pW;
		promo = p;
		drawCount = d;
	}
	public Move(boolean c,int cP,int tP,int o,int f,int v) {
		capture = c;
		capPiece = cP;
		thePiece = tP;
		to = o;
		from = f;
		value = v;
		lwCastle = false;
		rwCastle = false;
		lbCastle = false;
		rbCastle = false;
		passantB = 0;
		passantW = 0;
		promo = false;
		drawCount=0;
	}
	public boolean equals(Object O) {
		Move temp = (Move)O;
		if(temp.getTo()==to && temp.getFrom()==from&& temp.getThePiece()==thePiece)
			return true;
		else
			return false;
	}		
	
	
	public int compareTo(Move m) {
		int val = m.getValue();
		/*
		if(val<value) 
			return -1;
		if(val>value)
			return 1;
		else
			return -1;
		*/
		if(val>value)
			return 1;
		else
			return -1;
	}
	public void setValue(int v) {
		value = v;
	}
	public boolean isCapture() {			//return is this was capture move
		return capture;
	}
	public int getCapPiece() {
		return capPiece;
	}
	public int getThePiece() {
		return thePiece;
	}
	public int getTo() {
		return to;
	}
	public int getFrom() {
		return from;
	}
	public int getValue() {
		return value;
	}
	public boolean getlW() {
		return lwCastle;
	}
	public boolean getrW() {
		return rwCastle;
	}
	public boolean getlB() {
		return lbCastle;
	}
	public boolean getrB() {
		return rbCastle;
	}
	public int getPb() {
		return passantB;
	}
	public int getPw() {
		return passantW;
	}		
	public boolean getPromo() {
		return promo;
	}	
	public int getDraw() {
		return drawCount;
	}	
		
}