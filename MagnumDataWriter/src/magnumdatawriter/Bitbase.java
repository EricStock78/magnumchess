package magnumdatawriter;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Bitbase.java
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
public class Bitbase {
   
    private int iMaxIndex = 2 * 24 * 64 * 64;
    int KPKBitbase[] = new int[iMaxIndex / 32];
    int positions[] = new int[iMaxIndex];
    public final int RESULT_UNKNOWN = 1;
    public final int RESULT_DRAW = 2;
    public final int RESULT_WIN = 4;
    public final int RESULT_INVALID = 0;
    private final Board board;
    
    public Bitbase(Board p_board) 
    {
        board = p_board;
        
        Arrays.fill( positions, 0);
        Arrays.fill( KPKBitbase, 0);
    }
    
    void ToFile( DataOutputStream dataOutputStream ) {
        try
        {
            for( int i=0; i<iMaxIndex/32; i++) {
                dataOutputStream.writeInt(KPKBitbase[i]);
            }
        }
        catch( IOException ioEx )
        {
            System.out.println("Exception writing bibase to file");
        };
    }

    boolean Probe(int side, int blackKingSquare, int whiteKingSquare, int whitePawnSquare)
    {
        if( whitePawnSquare % 8 > 3)
        {
            whitePawnSquare ^= 7;
            whiteKingSquare ^= 7;
            blackKingSquare ^= 7;
        }
         
       int index = GetIndex( side, blackKingSquare, whiteKingSquare, whitePawnSquare );
      
       int bitbaseIndex = index / 32;
       int bitIndex = index % 32;
           
      
      if( (KPKBitbase[ bitbaseIndex ] & (1 << bitIndex)) != 0)
       {
           return true;
       }
       else
       {
           return false;
       }
    }
    
     public int GetIndex(int side, int blackKingSquare, int whiteKingSquare, int whitePawnSquare)
    {
        return side | blackKingSquare << 1 | whiteKingSquare << 7 | (whitePawnSquare & 3) << 13 | (whitePawnSquare / 8 -1) << 15;
    }
    void InitBitbase()
    {
        KPKPosition position = new KPKPosition();
        for(int i=0; i<iMaxIndex; i++)
        {
            positions[i] = position.ClassifyLeaf( i );
        }
        boolean done = false;
        while( !done )
        {
            done = true;
            for(int i=0; i<iMaxIndex; i++)
            {
                if( positions[i] == RESULT_UNKNOWN )
                {
                    positions[i] = position.Classify( i, positions );
                    if( positions[i] != RESULT_UNKNOWN )
                    {
                        done = false;
                    }
                } 
            }     
        }
        
        int iNumWins = 0;
        
        for(int i=0; i<iMaxIndex; i++)
        {
            int bitbaseIndex = i / 32;
            int bitIndex = i % 32;
            
            if( positions[i] == RESULT_WIN )
            {
                KPKBitbase[bitbaseIndex] |= (1 << bitIndex);
                iNumWins++;
            }
        }
        System.out.println("num wins is "+iNumWins);
    }
    
    public class KPKPosition {
        
        private int side;
        private int whiteKingSquare;
        private int blackKingSquare;
        private int whitePawnSquare;
       
        void KPKPosition() 
        {
            
        }
        
        void DecodeIndex(int index)
        {
            side = index & 1;
            blackKingSquare = (index >> 1 ) & 63;
            whiteKingSquare = (index >> 7 ) & 63;
            whitePawnSquare = (index >> 13) & 3 | (((index >> 15) + 1) << 3);
        }
        
       
         
        public int ClassifyLeaf(int index)
        {
            DecodeIndex( index );
            
            //check for invalid positions
            if( blackKingSquare == whiteKingSquare || blackKingSquare == whitePawnSquare || whiteKingSquare == whitePawnSquare)
            {
                return RESULT_INVALID;
            }
            else if( (board.getKingMoves( blackKingSquare ) & ((long)1 << whiteKingSquare)) != 0 )
            {
                return RESULT_INVALID;
            }
            else if( ( side == Global.COLOUR_WHITE) 
                    && ( (board.getPawnAttack( side, whitePawnSquare) & ((long)1 << blackKingSquare)) != 0 ))
            {   
                return RESULT_INVALID;
            }
            //check for win if we can safely promote
            else if( side == Global.COLOUR_WHITE && whitePawnSquare / 8 == 6 && 
                    whiteKingSquare !=  whitePawnSquare + 8 &&
                    ( board.getQueenDistance(blackKingSquare, whitePawnSquare + 8) > 1 || 
                    board.getQueenDistance(whiteKingSquare, whitePawnSquare + 8) == 1 ) )
            {
                return RESULT_WIN;
            }
            
            //check for draws
            
            //stalemate
            else if( side == Global.COLOUR_BLACK && 
                    ( ( board.getKingMoves( blackKingSquare ) & 
                    ~(board.getKingMoves( whiteKingSquare ) | board.getPawnAttack(Global.COLOUR_WHITE, whitePawnSquare))) == 0))
            {
                return RESULT_DRAW;
            }
            
            else if( side == Global.COLOUR_BLACK && ( (board.getKingMoves(blackKingSquare) & ((long)1 << whitePawnSquare) & ~board.getKingMoves(whiteKingSquare)) != 0))
            {
                return RESULT_DRAW;
            }
            
            else if( ( blackKingSquare == whitePawnSquare + 8 ) && whitePawnSquare / 8 < 6)
            {
                return RESULT_DRAW;
            }
            
            else if( side == Global.COLOUR_WHITE  && whiteKingSquare == (whitePawnSquare + 8) && 
                    blackKingSquare == whiteKingSquare + 16 && whitePawnSquare / 8 < 4 )
            {
                return RESULT_DRAW;
            }
            
            else if( blackKingSquare == 56 && whitePawnSquare % 8 == 0 )
            {
                return RESULT_DRAW;
            }
            
            else if( whitePawnSquare % 8 == 0 && whiteKingSquare % 8 == 0 &&
                    whiteKingSquare > whitePawnSquare && blackKingSquare == whiteKingSquare + 2 )
            {
                return RESULT_DRAW;
            }
            
            else
            {
                return RESULT_UNKNOWN;
            }
        }
        
        int Classify( int index, int positions[])
        {
            int result = RESULT_INVALID;
            
            DecodeIndex( index );
            
            long kingMoves = 0;
            if( side == Global.COLOUR_WHITE )
            {
                kingMoves = board.getKingMoves(whiteKingSquare);
            }
            else 
            {
                kingMoves = board.getKingMoves(blackKingSquare);
            }
            
            while( kingMoves != 0 )
            {
                long lsb = kingMoves & -kingMoves;
                kingMoves ^= lsb;
                int kingPos = Long.numberOfTrailingZeros( lsb );
                if( side == Global.COLOUR_WHITE )
                {
                    result |= positions[ GetIndex( Global.COLOUR_BLACK, blackKingSquare, kingPos, whitePawnSquare)];
                    if(  (result & RESULT_WIN) != 0 )
                    {
                        return RESULT_WIN;
                    }
                }
                else
                {
                    result |= positions[ GetIndex( Global.COLOUR_WHITE, kingPos, whiteKingSquare, whitePawnSquare)];
                    if(  (result & RESULT_DRAW) != 0 )
                    {
                        return RESULT_DRAW;
                    }
                } 
            }
            
            if( side == Global.COLOUR_WHITE )
            {
                if( whitePawnSquare / 8 < 6)
                {
                    result |= positions[ GetIndex( Global.COLOUR_BLACK, blackKingSquare, whiteKingSquare, whitePawnSquare + 8)];
                    if(  (result & RESULT_WIN) != 0 )
                    {
                        return RESULT_WIN;
                    }
                    
                    if( whitePawnSquare / 8 == 1)
                    {
                        result |= positions[ GetIndex( Global.COLOUR_BLACK, blackKingSquare, whiteKingSquare, whitePawnSquare + 16)];
                        if(  (result & RESULT_WIN) != 0 )
                        {
                            return RESULT_WIN;
                        }
                    }
                    
                }
            }
            
            if( (result & RESULT_UNKNOWN) == 0 )
            {
                if( side == Global.COLOUR_WHITE )
                {
                    return RESULT_DRAW;
                }
                else
                {
                    return RESULT_WIN;
                }
            }
            else
            {
                return RESULT_UNKNOWN;
            } 
        }
    } 
}
