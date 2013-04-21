package magnumchess;

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
    
    public static int getTo(int move) {
    	return (move>>6)&63;	
    }
    
    public static int getFrom(int move) {
    	return (move)&63;
    }
    
    public static int moveType(int move) {
        return (move>>12)&15;
    }
	
    public static int getValue(int move) {
        return (move>>>24) & 1;
    }
    
    //used to mark the move as a mate killer
    public static int setValue(int move, int value) {          
	return move |= (value<<24);
    }
    
    public static int makeMove(int to,int from, int type) {
	return from | to<<6 | type<<12;
    }

    public static int makeKillerMove( int move, int piece) {
        return move | piece << 16;
    }
    
    public static int getKillerPiece( int move ) {
        return move >> 16 & 63;
    }
    
    public static int makeMove(int to,int from) {
        int piece = Board.getInstance().piece_in_square[from];
        int cP = Board.getInstance().piece_in_square[to];
        int type;
        if(cP != -1 ) {
            type = Global.ORDINARY_CAPTURE;
        }
        else {
            int side = piece % 6;
            type = Global.ORDINARY_MOVE;
            if(piece % 6 == 4)	{			
                if(from == 4 + (side * 56)) {
                    if(to == 2 + (side * 56)) {
                        type = Global.LONG_CASTLE;
                    } 
                    else if(to == 6 + (side * 56)) {
                        type = Global.SHORT_CASTLE;
                    }
                }
            } 
            else if(piece % 6 == 5) {			
                if(to - from == 16 - (32 * side)) {
                    type = Global.DOUBLE_PAWN;
                }         
                if( Global.RelativeRanks[side][to / 8] == 7) {
                    type = Global.PROMO_Q;
                }
                else if(to == Board.getInstance().getPassant(side^1)) {
                    type = Global.EN_PASSANT_CAP;
                }
            }
        }
        return from | to<<6 | type<<12;
    }
}