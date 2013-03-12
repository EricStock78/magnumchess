package magnumchess;


import magnumchess.Board;

/**
 * SEE.java
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

/*
 * See.java
 * This class contains a method to statically evaluate the expected return before making a capture
 *
 * @version 	4.00 March 2012
 * @author 	Eric Stock
 */

    
public class SEE {
    
    private static Board Board;
    
    /** 
     * Constructor SEE
     * 
     * grabs a reference to the instantiated Board object
     * 
     */
    public SEE() {
        Board = Board.getInstance();
    }
    
    /** 
     * Method isPinned
     * 
     * checks to see if a piece trying to move cannot because it is pinned to the king
     * (the king would be exposed to check should the piece move)
     * 
     * @param param side - the side on move
     * @param to - the square to move to
     * @param from - the square moved from
     */
    public static boolean isPinned(int side, int to, int from) {
    	int relation;
    	
    	long king = Board.pieceBits[side][Global.PIECE_KING];
		long enemies = Board.pieceBits[side ^ 1][Global.PIECE_ALL];

    	int kingPos = Long.numberOfTrailingZeros(king);
    	
    	if(kingPos == from) {
    	
    		return false;	
    	}
      enemies &= ( Board.pieceBits[side ^ 1][Global.PIECE_QUEEN] | Board.pieceBits[side ^ 1][Global.PIECE_BISHOP] | Board.pieceBits[side ^ 1][Global.PIECE_ROOK] );
	
    	int difference = kingPos - from;
    	int rankDifference = (kingPos >> 3) - (from >> 3);
    	if(difference < 0)
    		rankDifference *= -1;
    	if(rankDifference != 0) {
    		if((difference % rankDifference) != 0) return false;
    		relation = difference / rankDifference;
    	
    	} else  {
    		if(kingPos < from)
    			relation = -99;
    		else
    			relation = 99;		
    	}
      long temp;
    	int nextPos;
    	switch(relation) {
    		case(-9):	
    			if(Global.Diag2Groups[from] == Global.Diag2Groups[to]) return false;
    			
    			temp = Board.bitboard & Global.plus9[kingPos];
    			temp &= -temp;
    			nextPos = Long.numberOfTrailingZeros(temp);
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag2Masks[Global.Diag2Groups[from]]) == 0) return false;
    			return true;
    		case(9):
    			if(Global.Diag2Groups[from] == Global.Diag2Groups[to]) return false;
    			
    			temp = Board.bitboard & Global.plus9[from];
    			temp &= -temp;
    			nextPos = Long.numberOfTrailingZeros(temp);
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag2Masks[Global.Diag2Groups[from]]) == 0) return false;
    			return true;
    		case(-7):
    			if(Global.Diag1Groups[from] == Global.Diag1Groups[to]) return false;
    			
    			temp = Board.bitboard & Global.plus7[kingPos];
    			temp &= -temp;
    			nextPos = Long.numberOfTrailingZeros(temp);
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag1Masks[Global.Diag1Groups[from]]) == 0) return false;
    			return true;
    		case(7):
    			if(Global.Diag1Groups[from] == Global.Diag1Groups[to]) return false;
    			temp = Board.bitboard & Global.plus7[from];
    			temp &= -temp;
    			nextPos = Long.numberOfTrailingZeros(temp);
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag1Masks[Global.Diag1Groups[from]]) == 0) return false;
    			return true;	
    		case(-8):
    			if((from & 7) == (to & 7)) return false;
    			temp = Board.bitboard & Global.plus8[kingPos];
    			temp &= -temp;
    			nextPos = Long.numberOfTrailingZeros(temp);
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.fileMasks[from&7]) == 0) return false;
    			return true;			
    		case(8):
    			if((from & 7) == (to & 7)) return false;
    			temp = Board.bitboard & Global.plus8[from];
    			temp &= -temp;
    			nextPos = Long.numberOfTrailingZeros(temp);
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.fileMasks[from&7]) == 0) return false;
    			return true;
    		case(-99):
    			if((from >> 3) == (to >> 3)) return false;
    			temp = Board.bitboard & Global.plus1[kingPos];
    			temp &= -temp;
    			nextPos = Long.numberOfTrailingZeros(temp);
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.rankMasks[from>>3]) == 0) return false;
    			return true;	
 			case(99):
 				if((from >> 3) == (to >> 3)) return false;
    			temp = Board.bitboard & Global.plus1[from];
    			temp &= -temp;
    			nextPos = Long.numberOfTrailingZeros(temp);
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.rankMasks[from>>3]) == 0) return false;
    			return true;
 				
    	}
    	return false;
    	
    }
    
    /** 
     * Method getSEE
     * 
     *
     * performs the static exchange evaluation of proposed capture
     * Takes into consideration hidden pieces behind pieces making a capture
     * Performs the alpha-beta algorithm 
     *
     * @param param side - the side on move
     * @param to - the square to move to
     * @param from - the square moved from
     * 
     */

	 public static int GetSEE2(int side, int to, int from, int moveType, int test)
	 {
		//System.out.println("to is "+to);
 		//System.out.println("from is "+from);

		//int friendSide = Board.piece_in_square[from] /6;
		int enemySide = side^1;
		int[][] arrPieces = new int[2][10];
 		int arrPieceCount[] = new int[2];
		int tempVal;
		long removedBits = 1L << from;

		switch(moveType)
		{
			case(Global.EN_PASSANT_CAP):
				tempVal = Global.values[Global.PIECE_PAWN];
				removedBits |= side == Global.COLOUR_WHITE ? Global.set_Mask[to-8] : Global.set_Mask[to+8];
				arrPieces[side][arrPieceCount[side]++] = (Global.values[Global.PIECE_PAWN] << 6 | from );
			break;

			case(Global.PROMO_Q):
				if(Board.piece_in_square[to] != -1)
					tempVal = (Global.values[Board.piece_in_square[to]] + Global.values[Global.PIECE_QUEEN] - Global.values[Global.PIECE_PAWN]);
				else
					tempVal = Global.values[Global.PIECE_QUEEN] - Global.values[Global.PIECE_PAWN];
				arrPieces[side][arrPieceCount[side]++] = (Global.values[Global.PIECE_QUEEN] << 6 | from );
			break;

			default:
				arrPieces[side][arrPieceCount[side]++] = (Global.values[Board.piece_in_square[from]] << 6 | from );
				tempVal = Global.values[Board.piece_in_square[to]];
			break;
		}

		if(tempVal < test)
		{
			return tempVal;
		}

		Board.bitboard ^= removedBits;
		long attack = Board.getAttack2(to) & Board.bitboard;
		
		long originalAttack = attack;
		while(attack != 0)
		{
			 long bit = attack & -attack;
			 attack ^= bit;
			 int position = Long.numberOfTrailingZeros( bit );
			 int piece = Board.piece_in_square[position];
			 arrPieces[piece/6][arrPieceCount[piece/6]++] = (Global.values[piece] << 6 | position);
		}

		int moveNumber = 0;

		while (true) {
			
			if(arrPieceCount[enemySide] > moveNumber ) {
				tempVal -= arrPieces[side][moveNumber] >> 6;
				if(tempVal >= test) {
					//System.out.println("value is "+tempVal);
					Board.bitboard ^= removedBits;
					return tempVal;
				}
			} else {
				/*for(int i=0; i<arrPieceCount[friendSide]; i++)
				{
					System.out.print(" friend is "+(arrPieces[friendSide][i] & 63) );
				}
				System.out.println();
				for(int i=0; i<arrPieceCount[enemySide]; i++)
				{
					System.out.print(" enemy is "+(arrPieces[enemySide][i] & 63) );
				}
				System.out.println();
				System.out.println("value is "+tempVal);
				*/
				Board.bitboard ^= removedBits;
				return tempVal;
			}

			//place lowest piece at correct pos in array
			int lowestValue = arrPieces[enemySide][moveNumber];
			for(int i = moveNumber+1; i < arrPieceCount[enemySide]; i++)
			{
				if( arrPieces[enemySide][i] < lowestValue)
				{
					lowestValue = arrPieces[enemySide][i];
					arrPieces[enemySide][i] = arrPieces[enemySide][moveNumber];
					arrPieces[enemySide][moveNumber] = lowestValue;
				}
			}

			int removedPosition = arrPieces[enemySide][moveNumber] & 63;
			Board.bitboard ^= Global.set_Mask[removedPosition];
			removedBits |= Global.set_Mask[removedPosition];
			long newAttack = Board.GetSlideAttacks2SEE(to) & Board.bitboard;
			newAttack &= ~originalAttack;
			
			if(newAttack != 0)
			{
				originalAttack |= newAttack;
				int position = Long.numberOfTrailingZeros( newAttack );
				int piece = Board.piece_in_square[position];
				arrPieces[piece/6][arrPieceCount[piece/6]++] = (Global.values[piece] << 6 | position);
			}

			if(arrPieceCount[side] > moveNumber + 1) {
				tempVal += arrPieces[enemySide][moveNumber] >> 6;
				if(tempVal < test) {
					//System.out.println("value is "+tempVal);
					Board.bitboard ^= removedBits;
				   return tempVal;
				}
			} 
			else
			{
				/*for(int i=0; i<arrPieceCount[friendSide]; i++)
				{
					System.out.print(" friend is "+(arrPieces[friendSide][i] & 63) );
				}
				System.out.println();
				for(int i=0; i<arrPieceCount[enemySide]; i++)
				{
					System.out.print(" enemy is "+(arrPieces[enemySide][i] & 63) );
				}
				System.out.println();
				System.out.println("value is "+tempVal);
				*/
				Board.bitboard ^= removedBits;
				return tempVal;
			}

			//place lowest piece at correct pos in array
			lowestValue = arrPieces[side][moveNumber+1];
			for(int i = moveNumber+2; i < arrPieceCount[side]; i++)
			{
				if( arrPieces[side][i] < lowestValue)
				{
					lowestValue = arrPieces[side][i];
					arrPieces[side][i] = arrPieces[side][moveNumber+1];
					arrPieces[side][moveNumber+1] = lowestValue;
				}
			}

			removedPosition = arrPieces[side][moveNumber+1] & 63;
			Board.bitboard ^= Global.set_Mask[removedPosition];
			removedBits |= Global.set_Mask[removedPosition];
			newAttack = Board.GetSlideAttacks2SEE(to) & Board.bitboard;
			newAttack &= ~originalAttack;
			if(newAttack != 0)
			{
				 originalAttack |= newAttack;
				 int position = Long.numberOfTrailingZeros( newAttack );
				 int piece = Board.piece_in_square[position];
				 arrPieces[piece/6][arrPieceCount[piece/6]++] = (Global.values[piece] << 6 | position);
			}

			moveNumber++;
		}
	}
}