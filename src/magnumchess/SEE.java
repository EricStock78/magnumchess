package magnumchess;

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
        int king = Board.pieceList[Global.PIECE_KING + side * 6][0];
        long queenMoves = Board.getMagicRookMoves(king) | Board.getMagicBishopMoves(king);
        if( (queenMoves & (1L << from)) != 0 ) {
            long queenMovesUncovered = Board.getMagicRookMoves(king, Board.bitboard ^ (1L << from) | (1L << to)) | Board.getMagicBishopMoves(king, Board.bitboard ^ (1L << from) | (1L << to));
            long uncovered = queenMovesUncovered & ~queenMoves;
            uncovered &= (Board.pieceBits[side^1][Global.PIECE_ALL] & Board.bitboard);
            //uncovered &= (Board.pieceBits[side^1][Global.PIECE_ALL]);
            if( uncovered != 0)
            {
                int pos = Long.numberOfTrailingZeros(uncovered);
                int piece = Board.piece_in_square[pos]%6;
                if( pos/8 == king/8 || pos%8 == king%8) {
                    if(  piece == 0 || piece == 3 ) {
                        return true;
                    }
                }
                else if(  piece == 2 || piece == 3 ) {
                    return true;
                }
                return false;
            }
            else {
                return false;
            }
        }
        else
        {
            return false;
        }
    }   

    /** 
     * Method getSEE
     * 
     *
     * performs the static exchange evaluation of proposed capture
     * Takes into consideration hidden pieces behind pieces making a capture
     * Performs the test algorithm 
     *
     * @param param side - the side on move
     * @param to - the square to move to
     * @param from - the square moved from
     * 
     */

    public static int GetSEE_PinsPlus(int side, int to, int from, int moveType, int test)
    {
        long origPieces = Board.bitboard;   //used to restore board
        int capPiece = Board.piece_in_square[to];
        int captured = Board.piece_in_square[from];
        int gain = capPiece == -1 ? 0 : Global.values[capPiece];
        switch( moveType ) {
            case(Global.EN_PASSANT_CAP):
                Board.bitboard ^= side == Global.COLOUR_WHITE ? Global.set_Mask[to-8] : Global.set_Mask[to+8];
                gain = Global.values[Global.PIECE_PAWN];
            break;
            case(Global.LONG_CASTLE):
                return 0;
            case(Global.SHORT_CASTLE):
                return 0;
            case( Global.PROMO_Q ):
                captured = Global.PIECE_QUEEN;
                gain += Global.values[Global.PIECE_QUEEN] - Global.values[Global.PIECE_PAWN];
            break;
        }
        
        if(gain < test)
        {
            Board.bitboard = origPieces;
            return gain;
        }
        
        Board.bitboard ^= Global.set_Mask[from];
        long attack = Board.getAttack2(to) & Board.bitboard;
        long stmAttacks = attack & Board.pieceBits[side^1][Global.PIECE_ALL];
        if( stmAttacks == 0 )
        {
            Board.bitboard = origPieces;
            return gain;
        }
        
        int[] swapList = new int[32];
        int swapIndex = 1;
        int stm = side ^ 1;
        swapList[0] = gain;
        //long[] pins = new long[2];
        //int ourBest = -20000;
        //int theirBest = -gain;
        do
        {
            int oldCaptured = captured;
            captured = -1;
            do{
                
                long bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_PAWN];             
                while( bits != 0 )
                { 
                    if( !isPinned( stm, to, Long.numberOfTrailingZeros(bits))) {
                        captured = Global.PIECE_PAWN;
                        Board.bitboard ^= (bits & -bits);
                        attack |= Board.getMagicBishopMoves(to) & (Board.pieceBits[0][Global.PIECE_BISHOP] | Board.pieceBits[1][Global.PIECE_BISHOP] |
                            Board.pieceBits[0][Global.PIECE_QUEEN] | Board.pieceBits[1][Global.PIECE_QUEEN] );
                        break;
                    }
                    
                    bits ^= (bits & -bits);
                }
                if( captured != -1) break;
                
                bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_KNIGHT];
                while( bits != 0 )
                { 
                    if( !isPinned( stm, to, Long.numberOfTrailingZeros(bits))) {
                        captured = Global.PIECE_KNIGHT;
                        Board.bitboard ^= (bits & -bits);
                        break;
                    }
                    bits ^= (bits & -bits);
                }
                
                if( captured != -1) break;
                
                bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_BISHOP];
                if( bits != 0 )
                {  
                     if( !isPinned( stm, to, Long.numberOfTrailingZeros(bits))) {
                         captured = Global.PIECE_BISHOP;
                         Board.bitboard ^= (bits & -bits);
                         attack |= Board.getMagicBishopMoves(to) & (Board.pieceBits[0][Global.PIECE_BISHOP] | Board.pieceBits[1][Global.PIECE_BISHOP] |
                             Board.pieceBits[0][Global.PIECE_QUEEN] | Board.pieceBits[1][Global.PIECE_QUEEN] );
                         break;
                     }
                }
               
                bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_ROOK];
                while( bits != 0 )
                { 
                    if( !isPinned( stm, to, Long.numberOfTrailingZeros(bits))) {
                        captured = Global.PIECE_ROOK;
                        Board.bitboard ^= (bits & -bits);
                        attack |= Board.getMagicRookMoves(to) & (Board.pieceBits[0][Global.PIECE_ROOK] | Board.pieceBits[1][Global.PIECE_ROOK] |
                           Board.pieceBits[0][Global.PIECE_QUEEN] | Board.pieceBits[1][Global.PIECE_QUEEN] );
                        break;
                    }
                    bits ^= (bits & -bits);
                }
                
                if( captured != -1) break;
                
                bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_QUEEN];
                while( bits != 0 )
                {  
                    if( !isPinned( stm, to, Long.numberOfTrailingZeros(bits))) {
                        captured = Global.PIECE_QUEEN; 
                        Board.bitboard ^= (bits & -bits);
                         attack |= Board.getMagicBishopMoves(to) & (Board.pieceBits[0][Global.PIECE_BISHOP] | Board.pieceBits[1][Global.PIECE_BISHOP] |
                                Board.pieceBits[0][Global.PIECE_QUEEN] | Board.pieceBits[1][Global.PIECE_QUEEN] );
                         attack |= Board.getMagicRookMoves(to) & (Board.pieceBits[0][Global.PIECE_ROOK] | Board.pieceBits[1][Global.PIECE_ROOK] |
                                Board.pieceBits[0][Global.PIECE_QUEEN] | Board.pieceBits[1][Global.PIECE_QUEEN] );
                         break;
                    }
                    bits ^= (bits & -bits);
                }
                
                if( captured != -1) break;
                
                bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_KING];
                if( bits != 0 )
                { 
                   // no need to find attackers behind for king as it might mean king was in check or we can recapture after losing king...both don't make sense
                   captured = Global.PIECE_KING;
                   Board.bitboard ^= (bits & -bits);
                   break;
                }
            } while(false);
            
            if( captured == -1) {
                break;
            }
            
            swapList[swapIndex] = -swapList[swapIndex-1] + Global.values[oldCaptured];
            
            // see if we can exit out due to test algorithm 
            if( side == stm ) {
                //theirBest = Math.max( theirBest, -swapList[swapIndex] );
                
                if(swapList[swapIndex] < test ) {
                    Board.bitboard = origPieces;
                    return swapList[swapIndex];
                }
            }
            else if( side != stm ) {
                //ourBest = Math.max( ourBest, -swapList[swapIndex] );
                if( -swapList[swapIndex] >= test ) {
                    Board.bitboard = origPieces;
                    return -swapList[swapIndex];
                }
            }
            
            swapIndex++;
            
            stm = stm ^ 1;
            stmAttacks = attack & Board.pieceBits[stm][Global.PIECE_ALL] & Board.bitboard;// & ~pins[stm];
            
        } while( stmAttacks != 0 );
        
        /*if( stm == side) {
           theirBest = Math.max( theirBest, swapList[swapIndex-1] ); 
        }
        else {
           ourBest = Math.max( ourBest, -swapList[swapIndex-1] );
        }*/
        
        while (--swapIndex > 0) {
            swapList[swapIndex-1] = Math.min(-swapList[swapIndex], swapList[swapIndex-1]);
        }
        
        Board.bitboard = origPieces;
        //return Math.max(ourBest, -theirBest);
        
        return swapList[0];
    }
    
    
     /** 
     * Method getSEE
     * 
     *
     * performs the static exchange evaluation of proposed capture
     * Takes into consideration hidden pieces behind pieces making a capture
     * Performs the test algorithm 
     *
     * @param param side - the side on move
     * @param to - the square to move to
     * @param from - the square moved from
     * 
     */

    public static int GetSEE_NoPins(int side, int to, int from, int moveType, int test)
    {
        long origPieces = Board.bitboard;   //used to restore board
        int capPiece = Board.piece_in_square[to];
        int captured = Board.piece_in_square[from];
        int gain = capPiece == -1 ? 0 : Global.values[capPiece];
        switch( moveType ) {
            case(Global.EN_PASSANT_CAP):
                Board.bitboard ^= side == Global.COLOUR_WHITE ? Global.set_Mask[to-8] : Global.set_Mask[to+8];
                gain = Global.values[Global.PIECE_PAWN];
            break;
            case(Global.LONG_CASTLE):
                return 0;
            case(Global.SHORT_CASTLE):
                return 0;
            case( Global.PROMO_Q ):
                captured = Global.PIECE_QUEEN;
                gain += Global.values[Global.PIECE_QUEEN] - Global.values[Global.PIECE_PAWN];
            break;
        }
        
        if(gain < test)
        {
            Board.bitboard = origPieces;
            return gain;
        }
        
        Board.bitboard ^= Global.set_Mask[from];
        long attack = Board.getAttack2(to) & Board.bitboard;
        long stmAttacks = attack & Board.pieceBits[side^1][Global.PIECE_ALL];
        if( stmAttacks == 0 )
        {
            Board.bitboard = origPieces;
            return gain;
        }
        
        int[] swapList = new int[32];
        int swapIndex = 1;
        int stm = side ^ 1;
        swapList[0] = gain;
        //int ourBest = -20000;
        //int theirBest = -gain;
        do
        {
            swapList[swapIndex] = -swapList[swapIndex-1] + Global.values[captured];
            
            // see if we can exit out due to test algorithm 
            if( side == stm ) {
                //theirBest = Math.max( theirBest, -swapList[swapIndex] );
                if(swapList[swapIndex] < test ) {
                    Board.bitboard = origPieces;
                    return swapList[swapIndex];
                }
            }
            else if( side != stm ) {
                //ourBest = Math.max( ourBest, -swapList[swapIndex] );
                if( -swapList[swapIndex] >= test ) {
                    Board.bitboard = origPieces;
                    return -swapList[swapIndex];
                }
            }
            
            if( (stmAttacks & Board.pieceBits[stm][Global.PIECE_PAWN]) != 0 )
            {
                long bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_PAWN];
                captured = Global.PIECE_PAWN;
                Board.bitboard ^= (bits & -bits);
                attack |= Board.getMagicBishopMoves(to) & (Board.pieceBits[0][Global.PIECE_BISHOP] | Board.pieceBits[1][Global.PIECE_BISHOP] |
                    Board.pieceBits[0][Global.PIECE_QUEEN] | Board.pieceBits[1][Global.PIECE_QUEEN] );
            }
            else if( (stmAttacks & Board.pieceBits[stm][Global.PIECE_KNIGHT]) != 0 )
            {
                long bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_KNIGHT];
                captured = Global.PIECE_KNIGHT;
                Board.bitboard ^= (bits & -bits);
            }
            else if( (stmAttacks & Board.pieceBits[stm][Global.PIECE_BISHOP]) != 0 )
            {
                long bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_BISHOP]; 
                captured = Global.PIECE_BISHOP;
                Board.bitboard ^= (bits & -bits);
                attack |= Board.getMagicBishopMoves(to) & (Board.pieceBits[0][Global.PIECE_BISHOP] | Board.pieceBits[1][Global.PIECE_BISHOP] |
                    Board.pieceBits[0][Global.PIECE_QUEEN] | Board.pieceBits[1][Global.PIECE_QUEEN] );
            }
            else if( (stmAttacks & Board.pieceBits[stm][Global.PIECE_ROOK]) != 0 )
            {
                long bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_ROOK];
                captured = Global.PIECE_ROOK;
                Board.bitboard ^= (bits & -bits);
                attack |= Board.getMagicRookMoves(to) & (Board.pieceBits[0][Global.PIECE_ROOK] | Board.pieceBits[1][Global.PIECE_ROOK] |
                   Board.pieceBits[0][Global.PIECE_QUEEN] | Board.pieceBits[1][Global.PIECE_QUEEN] );
            }
            else if( (stmAttacks & Board.pieceBits[stm][Global.PIECE_QUEEN]) != 0 )
            {
                long bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_QUEEN];
                captured = Global.PIECE_QUEEN; 
                Board.bitboard ^= (bits & -bits);
                 attack |= Board.getMagicBishopMoves(to) & (Board.pieceBits[0][Global.PIECE_BISHOP] | Board.pieceBits[1][Global.PIECE_BISHOP] |
                        Board.pieceBits[0][Global.PIECE_QUEEN] | Board.pieceBits[1][Global.PIECE_QUEEN] );
                 attack |= Board.getMagicRookMoves(to) & (Board.pieceBits[0][Global.PIECE_ROOK] | Board.pieceBits[1][Global.PIECE_ROOK] |
                        Board.pieceBits[0][Global.PIECE_QUEEN] | Board.pieceBits[1][Global.PIECE_QUEEN] );
            }
            else if( (stmAttacks & Board.pieceBits[stm][Global.PIECE_KING]) != 0 )
            {
               // no need to find attackers behind for king as it might mean king was in check or we can recapture after losing king...both don't make sense
               captured = Global.PIECE_KING;
               long bits = stmAttacks & Board.pieceBits[stm][Global.PIECE_KING];
               Board.bitboard ^= (bits & -bits);
            }
            
            swapIndex++;
            
            stm = stm ^ 1;
            stmAttacks = attack & Board.pieceBits[stm][Global.PIECE_ALL] & Board.bitboard;
            
        } while( stmAttacks != 0 );
        
        /*if( stm == side) {
           theirBest = Math.max( theirBest, swapList[swapIndex-1] ); 
        }
        else {
           ourBest = Math.max( ourBest, -swapList[swapIndex-1] );
        }*/
        
        while (--swapIndex > 0) {
            swapList[swapIndex-1] = Math.min(-swapList[swapIndex], swapList[swapIndex-1]);
        }
        
        Board.bitboard = origPieces;
        //return Math.max(ourBest, -theirBest);
        
        return swapList[0];
    }
}
