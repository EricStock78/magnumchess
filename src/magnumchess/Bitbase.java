package magnumchess;

import java.io.DataInputStream;
import java.io.IOException;
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
    
    public Bitbase() 
    {
    }
    
    public void LoadData( DataInputStream dataInputStream) throws IOException
    {
        for( int i=0; i< iMaxIndex / 32; i++) {
            KPKBitbase[i] = dataInputStream.readInt();
        }
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
}