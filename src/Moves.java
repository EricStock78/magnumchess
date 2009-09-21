/**
 * @(#)Moves.java
 *
 *
 * @author 
 * @version 1.00 2008/5/25
 */


public class Moves {

    public static int getTo(int move) {
    	return move&63;	
    }
    public static int getFrom(int move) {
    	return (move>>6)&63;
    }
    public static int getPiece(int move) {
    	return (move>>12)&15;
    }
	public static int getCapture(int move) {
		return (move>>16)&15 - 1;		//must subtract 1 since we store -1 as 0
	}
	public static int moveType(int move) {
		return (move>>20)&7;
	}
	public static int getValue(int move) {
		return (move>>>23);
	}
	public static int makeMove(int to,int from,int piece,int capture,int type,int value) {
		return to | from<<6 | piece <<12 | (capture+1)<<16 | type<<20 | value<<23;
	}
}