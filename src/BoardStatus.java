public final class BoardStatus {
	
	
	private int[] pieces;
	private int[] pieces2;
	private int side;
	private int side2;
	
	public BoardStatus(int[] p, int s) {
		pieces = new int[64];
		System.arraycopy(p,0,pieces,0,64);
		//pieces = p;
		side = s;
	}
	public final int compareTo(Object o) {
		boolean same;
		same = true;
		side2 = ((BoardStatus)o).getSide();
		pieces2 = ((BoardStatus)o).getPieces();
		//System.arraycopy(((BoardStatus)o).getPieces(),0,pieces2,0,64);
		if(side != side2)
			same = false;
		for(int i=0;i<64;i++) {
			if(pieces2[i] != pieces[i]) {
				same = false;
			}
		}		
		if(same)
			return 1;
		else
			return -1;
	}			
	public final int[] getPieces() {
		return pieces;
	}
	public final int getSide() {
		return side;
	}		
}