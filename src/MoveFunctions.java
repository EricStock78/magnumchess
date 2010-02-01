/**
 * MovesFunctions.java
 *
 * Version 2.0   
 * 
 * Copyright (c) 2010 Eric Stock
 
Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:
 
The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.
 
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/**
 * MoveFunctions is a static class which packs information about a move into a 32 bit integer
 * MoveFunctions also is able to retrieve information from the packed 32 bit integer
 *
 * @version 	2.00 30 Jan 2010
 * @author 	Eric Stock
 */

public class MoveFunctions {
    
    /** chessBoard represents the instance of the singleton class Board - which 
     *contains all data structures and methods to make and unmake moves
     */
    private static Board chessBoard;
    
    public MoveFunctions() {
        chessBoard = Board.getInstance();
    }
    
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
		return ((move>>16)&15) - 1;                     //must subtract 1 since we store -1 as 0
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
		int move = from | to<<6 | piece <<12 | (capture+1)<<16 | type<<20 | value<<23;
		return move;
		
	}
	
    public static int makeMove(int to,int from) {
		int piece = chessBoard.piece_in_square[from];
		int cP = chessBoard.piece_in_square[to];
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
			else if(to == chessBoard.getPassantB())
				type = Global.EN_PASSANT_CAP;	
		}
		else if(piece == 11) {			//bPawn
			if(to/8 == 0)
				type = Global.PROMO_Q;
			else if(to == chessBoard.getPassantW())
				type = Global.EN_PASSANT_CAP;	
		}
		return from | to<<6 | piece <<12 | (cP+1)<<16 | type<<20 | 0;
	}
}