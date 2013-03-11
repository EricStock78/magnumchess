package magnumchess;


import magnumchess.Board;

/**
 * MovesFunctions.java
 *
 * Version 4.0
 * 
 * Copyright (c) 2013 Eric Stock
 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * MoveFunctions is a static class which packs information about a move into a 32 bit integer
 * MoveFunctions also is able to retrieve information from the packed 32 bit integer
 *
 * @version 	4.00 March 2012
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
		return (move>>20)&15;
	}
	
    public static int getValue(int move) {
		return (move>>>24) & 1;
	}
	
    public static int setValue(int move, int value) {          //used to mark the move as a mate killer
		return move |= (value<<24);
	}
    
    public static int makeMove(int to,int from,int piece,int capture,int type) {
		int move = from | to<<6 | piece <<12 | (capture+1)<<16 | type<<20;
		return move;
	}

	
    public static int makeMove(int to,int from) {
		int piece = Board.getInstance().piece_in_square[from];
		int cP = Board.getInstance().piece_in_square[to];
		int type;
		if(cP != -1 )
		{
			type = Global.ORDINARY_CAPTURE;
			
			if(piece % 6 == 4)
				type = Global.MOVE_KING_LOSE_CASTLE;
			else if(piece % 6 == 0)
				type = Global.MOVE_ROOK_LOSE_CASTLE;
			else if(cP % 6 == 0)
				type = Global.CAPTURE_ROOK_LOSE_CASTLE;
		}
		else
		{
			type = Global.ORDINARY_MOVE;
			if(piece == 4)	{			//wKing
				if(from == 4) {
					if(to == 2)
						type = Global.LONG_CASTLE;
					else if(to == 6)
						type = Global.SHORT_CASTLE;
					else
						type = Global.MOVE_KING_LOSE_CASTLE;
				}
			} else if(piece == 10) {	//bKing
				if(from == 60) {
					if(to == 58)
						type = Global.LONG_CASTLE;
					else if(to == 62)
						type = Global.SHORT_CASTLE;
					else
						type = Global.MOVE_KING_LOSE_CASTLE;
				}
			}
			else if(piece == 5)	{			//wPawn
				if(to - from == 16)
					type = Global.DOUBLE_PAWN;

				if(to/8 == 7)
					type = Global.PROMO_Q;
				else if(to == chessBoard.getPassant(Global.COLOUR_BLACK))
					type = Global.EN_PASSANT_CAP;
			}
			else if(piece == 11) {			//bPawn
				if(from - to == 16)
					type = Global.DOUBLE_PAWN;

				if(to/8 == 0)
					type = Global.PROMO_Q;
				else if(to == chessBoard.getPassant(Global.COLOUR_WHITE))
					type = Global.EN_PASSANT_CAP;
			}
			else if(piece % 6 == 0) {
					type = Global.MOVE_ROOK_LOSE_CASTLE;
			}
		}

		return from | to<<6 | piece <<12 | (cP+1)<<16 | type<<20;
	}
}