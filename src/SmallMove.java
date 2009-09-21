public final class SmallMove implements Comparable<SmallMove>{
	
	
	private int from;
	private int oldFrom;
	private int value;
	
	public SmallMove(int f,int of,int v) {
		
		from = f;
		oldFrom = of;
		value = v;
	}
	
	public final int compareTo(SmallMove s) {
		int val = s.getValue();
		
		if(val>value)
			return -1;
		else
			return 1;
	}
	
	public final int getFrom() {
		return from;
	}
	public final int getOldFrom() {
		return oldFrom;
	}
	public final int getValue() {
		return value;
	}	
	public String toString() {
		return Integer.toString(value);
	}	
}