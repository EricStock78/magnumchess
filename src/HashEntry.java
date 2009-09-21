

public class HashEntry {
	private long hValue;
	private int to;
	private int from;
	private int thePiece;
	private int val;
	private int depth;
	private int type;
	private int nullFail;
	private int ancient;
	public HashEntry(long h,int t,int f,int p,int v,int d,int tp,int n,int a) {
		hValue = h;
		to = t;
		from = f;
		thePiece = p;
		val = v;
		depth = d;
		type = tp;
		nullFail = n;
		ancient = a;
	}
	public long getHash() {
		return hValue;
	}
	public int getTo() {
		return to;
	}
	public int getFrom() {
		return from;
	}
	public int getPiece() {
		return thePiece;
	}
	public int getVal() {
		return val;
	}
	public int getDepth() {
		return depth;
	}	
	public int getType() {
		return type;
	}
	public int getNullFail() {
		return nullFail;
	}	
	public int getAncient() {
		return ancient;
	}	
}		