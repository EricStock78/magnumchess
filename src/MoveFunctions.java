/**
 * @(#)Moves.java
 *
 *
 * @author 
 * @version 1.00 2008/5/25
 */


public class MoveFunctions {

    public static int getTo(int move) {
    	return (move>>6)&63;	
    }
    public static int getFrom(int move) {
    	return (move)&63;
    }
    public static int getPiece(int move) {
    	return (move>>12)&15;
    }
	public static int getCapture(int move) {
		return ((move>>16)&15) - 1;		//must subtract 1 since we store -1 as 0
	}
	public static int moveType(int move) {
		return (move>>20)&7;
	}
	public static int getValue(int move) {
		return (move>>>23);
	}
	public static int setValue(int move, int value) {
		move &= 8388607;
		return move |= (value<<23);
	}
	public static int makeMove(int to,int from,int piece,int capture,int type,int value) {
		//if(piece == 0)
		//	System.out.println("probs");
		int move = from | to<<6 | piece <<12 | (capture+1)<<16 | type<<20 | value<<23;
		return move;
		
	}
	public static int makeMove(int to,int from) {
		int piece = Board.piece_in_square[from];
		int cP = Board.piece_in_square[to];
		int type = Global.ORDINARY_MOVE;
		if(piece == 4)	{			//wKing
			if(from == 4) {
				if(to == 2)
					type = Global.LONG_CASTLE;
				else if(to == 6)
					type = Global.SHORT_CASTLE;
			}
		} else if(piece == 10) {	//bKing
			if(from == 60) {
				if(to == 58)
					type = Global.LONG_CASTLE;
				else if(to == 62)
					type = Global.SHORT_CASTLE;
			}
		}
		else if(piece == 5)	{			//wPawn
			if(to/8 == 7)
				type = Global.PROMO_Q;
			else if(to == Board.getPassantB())
				type = Global.EN_PASSANT_CAP;	
		}
		else if(piece == 11) {			//bPawn
			if(to/8 == 0)
				type = Global.PROMO_Q;
			else if(to == Board.getPassantW())
				type = Global.EN_PASSANT_CAP;	
		}
		return from | to<<6 | piece <<12 | (cP+1)<<16 | type<<20 | 0;
	}
}