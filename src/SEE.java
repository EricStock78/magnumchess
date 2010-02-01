/**
 * SEE.java
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

/*
 * See.java
 * This class contains a method to statically evaluate the expected return before making a capture
 *
 * @version 	2.00 30 Jan 2010
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
    	
    	long enemies;
    	long king;							
    	long temp;
    	int nextPos;
    	if(side==1) {
			king = Board.blackking;
			enemies = Board.whitepieces;
		}
		else {
			king = Board.whiteking;
			enemies = Board.blackpieces;
		}

    	int kingPos = Board.getPos(king);
    	
    	if(kingPos == from) {
    	
    		return false;	
    	}
    	enemies &= Board.slidePieces;
    	
    	int difference = kingPos - from;
    	int rankDifference = kingPos/8 - from/8;
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
    	switch(relation) {
    		case(-9):	
    			if(Global.Diag2Groups[from] == Global.Diag2Groups[to]) return false;
    			
    			temp = Board.bitboard & Global.plus9[kingPos];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag2Masks[Global.Diag2Groups[from]]) == 0) return false;
    			return true;
    		case(9):
    			if(Global.Diag2Groups[from] == Global.Diag2Groups[to]) return false;
    			
    			temp = Board.bitboard & Global.plus9[from];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag2Masks[Global.Diag2Groups[from]]) == 0) return false;
    			return true;
    		case(-7):
    			if(Global.Diag1Groups[from] == Global.Diag1Groups[to]) return false;
    			
    			temp = Board.bitboard & Global.plus7[kingPos];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag1Masks[Global.Diag1Groups[from]]) == 0) return false;
    			return true;
    		case(7):
    			if(Global.Diag1Groups[from] == Global.Diag1Groups[to]) return false;
    			temp = Board.bitboard & Global.plus7[from];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag1Masks[Global.Diag1Groups[from]]) == 0) return false;
    			return true;	
    		case(-8):
    			if(from%8 == to%8) return false;
    			temp = Board.bitboard & Global.plus8[kingPos];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.fileMasks[from%8]) == 0) return false;
    			return true;			
    		case(8):
    			if(from%8 == to%8) return false;
    			temp = Board.bitboard & Global.plus8[from];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.fileMasks[from%8]) == 0) return false;
    			return true;
    		case(-99):
    			if(from/8 == to/8) return false;
    			temp = Board.bitboard & Global.plus1[kingPos];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.rankMasks[from/8]) == 0) return false;
    			return true;	
 			case(99):
 				if(from/8 == to/8) return false;
    			temp = Board.bitboard & Global.plus1[from];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.rankMasks[from/8]) == 0) return false;
    			return true;
 				
    	}
    	return false;
    	
    }
    
    /** 
     * Method sortCaptures
     * 
     *
     * bubble order of the  capturing pieces from least to greatest
     *
     * @param int start - position to start at in the array
     * @param int noMoves - number of positions to sort
     * @param int[] Moves - array to sort
     * 
     */
    private static final void SortCaptures(int start, int noMoves, int[] Pieces) {
		boolean done = false;
		for(int i=start ; i<noMoves; i++) {
			if(done)break;
			done = true;
			for(int j = noMoves-1 ; j>i; j--) {
				if(Pieces[j] < Pieces[j-1]) {		
					int temp = Pieces[j];
					Pieces[j] = Pieces[j-1];
					Pieces[j-1] = temp;
					done = false;
				}
			}
		}
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
     * @param passant - the passant square
     * 
     */
	public static int getSEE(int side, int to, int from, int passant) {
		
        long friends;
		long enemies;
		
		if (side == 1) {
			friends = Board.blackpieces;
			enemies = Board.whitepieces;
		} else {
			friends = Board.whitepieces;
			enemies = Board.blackpieces;
		}	
		
		int[] ePieces = new int[10];
		int[] fPieces = new int[10];
		
		int eCount = 0;
		int fCount = 0;
		
        if(to == passant)
			ePieces[0] = Global.values[5] << 6;
		else {
            int cp = Board.piece_in_square[to];
            if(cp != -1) {
                ePieces[0] = (Global.values[Board.piece_in_square[to]]<<6);    
            } else                                  //this is a pawn push promo move
                ePieces[0] = 0;
        } 
		eCount = 1; 	
		
        
        fPieces[0] = (Global.values[Board.piece_in_square[from]] << 6 | from);
		fCount = 1;
		
		long attack = Board.getAttack2(to);
        
        //add enemy defenders of piece being captured
        long enemyDefenders = attack & enemies;
        
        //if(enemyDefenders == 0) 
        //    return ePieces[0];
        
        while(enemyDefenders != 0) {       
            long temp2 = enemyDefenders & -enemyDefenders;
			enemyDefenders ^= temp2;
			int pos = Board.getPos(temp2);
			ePieces[eCount++] = Global.values[Board.piece_in_square[pos]] << 6 | pos;
        } 
        
        //sort enemy defenders if more than 3 total enemies
        if(eCount > 2) 
            SortCaptures(1,eCount,ePieces);
            //Arrays.sort(ePieces,1,eCount);      //leave index 0 alone as this is the pre determined piece to be captured
    
        
        //add additional friend attackers attacking piece being captured
        long friendAttackers = (attack & friends) & ~Global.set_Mask[from];
        
        //if(friendAttackers == 0)
        //    return ePieces[0] - fPieces[0];
        
        while(friendAttackers != 0) {       
            long temp2 = friendAttackers & -friendAttackers;
			friendAttackers ^= temp2;
			int pos = Board.getPos(temp2);
			fPieces[fCount++] = Global.values[Board.piece_in_square[pos]] << 6 | pos;
        } 
        //sort friend attackers if more than 3 total attackers
        if(fCount > 2)
            SortCaptures(1,fCount,fPieces);
            //Arrays.sort(fPieces,1,fCount);      //leave index 0 alone as this is the pre determined attacker
         
       
		int tempVal = 0;
		int alpha = -20000;
		int beta = 20000;
		int moveNumber = 0;
		long removedBits = 0;
		
        
        while (true) {
            if(fCount > moveNumber) {
                tempVal += ePieces[moveNumber] >> 6;
                beta = Math.min(beta,tempVal); 
            } else {
                Board.bitboard ^= removedBits;
                return alpha;
            }if(alpha >=beta) {
                Board.bitboard ^= removedBits;
                return alpha;
            }
                
            //add any hidden pieces after this capture
            
            int removedPosition = fPieces[moveNumber] & 63;
            Board.bitboard ^= Global.set_Mask[removedPosition];
            removedBits |= Global.set_Mask[removedPosition];
            long newAttack = Board.getAttack2(to);
            newAttack ^= attack;
            attack |= newAttack;
            
            if(newAttack != 0) {
                //attack ^= newAttack;
                if((newAttack & friends) != 0) {
                    long temp2 = newAttack & -newAttack;
                    newAttack ^= temp2;
                    int pos = Board.getPos(temp2);
                    fPieces[fCount++] = Global.values[Board.piece_in_square[pos]] << 6 | pos;
                    if(fCount - moveNumber > 2)
                        SortCaptures(moveNumber + 1,fCount,fPieces);
                } else if((newAttack & enemies) != 0) {
                    long temp2 = newAttack & -newAttack;
                    newAttack ^= temp2;
                    int pos = Board.getPos(temp2);
                    ePieces[eCount++] = Global.values[Board.piece_in_square[pos]] << 6 | pos;
                    if(eCount - moveNumber >= 2)
                        SortCaptures(moveNumber + 1,eCount,ePieces);
                } 
            }
            
            
            
            if(eCount > moveNumber+1) {
                tempVal -= fPieces[moveNumber] >> 6; 
                alpha = Math.max(alpha, tempVal);
            } else {
                //if enemy has no pieces left to defend, alpha can be updated
                alpha = Math.max(alpha, tempVal);
                if(alpha >= beta) {
                    Board.bitboard ^= removedBits;
                    return beta;
                }
                return alpha;
            }
            if(alpha >= beta) {
                Board.bitboard ^= removedBits;
                return beta;
            }
            
            //add any hidden pieces after this capture
            
            removedPosition = ePieces[moveNumber+1] & 63;
            Board.bitboard ^= Global.set_Mask[removedPosition];
            removedBits |= Global.set_Mask[removedPosition];
            newAttack = Board.getAttack2(to);
            newAttack ^= attack;
            attack |= newAttack;
            
            if(newAttack != 0) {
                if((newAttack & friends) != 0) {
                    long temp2 = newAttack & -newAttack;
                    newAttack ^= temp2;
                    int pos = Board.getPos(temp2);
                    fPieces[fCount++] = Global.values[Board.piece_in_square[pos]] << 6 | pos;
                    if(fCount - moveNumber > 2)
                        SortCaptures(moveNumber + 1,fCount,fPieces);
                } else if((newAttack & enemies) != 0) {
                    long temp2 = newAttack & -newAttack;
                    newAttack ^= temp2;
                    int pos = Board.getPos(temp2);
                    ePieces[eCount++] = Global.values[Board.piece_in_square[pos]] << 6 | pos;
                    if(eCount - moveNumber >= 3)
                        SortCaptures(moveNumber + 2,eCount,ePieces);
                }
            }
            moveNumber++;
        }     
    }       
    
}
