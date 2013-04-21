package magnumchess;

/**
 * Transtable.java
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
 * TransTable.java
 * This class can be instantiated as a 2 layer depth first and replace always
 * transposition table (64 bit hash key)
 * 
 * Can also become a pawn hash table (64 bit hash key)
 * Can also be a evaluation hash table (64 bit hash key)
 *
 *
 */

public class TransTable {
	
   private long[] Table;
   private int[] Table2;
   private long[] Table3;
   private int hashCount;
   private static final int AncientMask = ~(1 << 26 | 1 << 27 | 1 << 28);
   public static final long LAZY_BIT = ( (long)1 << 31);
   public static final int NUM_SLOTS = 4;
   public static final int SLOT_SIZE = 3;
   private static final int PAWN_TABLE_SIZE = 6;
   private static final int EVAL_TABLE_SIZE = 2;


   /**flag indicating what type of table...0 regular, 1 pawn, 2 eval **/
   private int type;						
   private int size;
	
	 /*
     * Constructor Transtable
     * 
     * @param int s - size of the trans table (in number of entries)
     * @param int t - type of trans table (0 regular, 1 pawn hash, 2 eval hash)
     *
     */ 
    public TransTable(int s,int t) {
        type = t;
        size = s;
        if(type == 0) {
            Table2 = new int[size * NUM_SLOTS * SLOT_SIZE];
        } else if (type == 1) {
            Table = new long[size * PAWN_TABLE_SIZE];
        } 
        else if (type == 2) {
            Table3 = new long[size * EVAL_TABLE_SIZE];
        } 
        hashCount = 0;
    }
    
     /*
     * Method addEvalHash
     * 
     * Stores an entry in the multi level hash table 
     * 
     * @param int key - index of hash entry
     * @param int lock - 1st half of 64 bit hash
     * @param int lock2 - 2nd half of 64 bit hash
     * @param int move - best move for the position
     * @param int value - the score for this pawn position
     * @param int depth - the depth searched for this entry
     * @param int type - the type of entry (exact, lower, upper, terminal)
     * @param int nullFail - flag to indicate if a null move should be tried at this position
     * @param int ancient - counter to represent "freshness" of entry
     *
     */ 
    public final void addHash(int move,int value,int depth,int type,int nullFail, long lock) {
        assert( value >= -Global.MATE_SCORE && value <= Global.MATE_SCORE);
        assert( type >= 0 && type < 8);
        assert( depth >= 0 && depth < 64 );
        assert( nullFail >= 0 && nullFail < 2);
        assert( Board.ancient >= 0 && Board.ancient < 8);
        assert( move >= 0 && move < ( 1<<16) );
        
        int key = (int)((lock >> 32) & (Global.HASHSIZE-1));
        int index = key * NUM_SLOTS * SLOT_SIZE;
	
        int replaceSlot = 0;
        
        for( int i=0; i < NUM_SLOTS; i++)
        {
            int slotIndex = index + i * SLOT_SIZE;
            if( Table2[slotIndex] == 0)
            {
                Table2[slotIndex] = (int)lock;  
                        
                Table2[slotIndex+1] = move
                                   | type << 16
                                   | depth << 19
                                   | nullFail << 25
                                   | Board.ancient << 26;
                Table2[slotIndex+2] = value + Global.MATE_SCORE;
                return;
            }
            else if( (Table2[slotIndex]) == (int)lock )
            {
                if( move == 0) {
                    move = (Table2[slotIndex+1] & 65535);
                }   
                
                Table2[slotIndex] = (int)lock;  

               Table2[slotIndex+1] = move
                                   | type << 16
                                   | depth << 19
                                   | nullFail << 25
                                   | Board.ancient << 26;
                Table2[slotIndex+2] = value + Global.MATE_SCORE;
                return;
            }
   
            int replaceSlotIndex = index + replaceSlot * SLOT_SIZE;
            
            int v1 = GetAncientVal(Table2[replaceSlotIndex + 1]) == Board.ancient ? 2 : 0;
            
            int v2 = ( GetAncientVal(Table2[slotIndex + 1]) == Board.ancient || GetResultType(Table2[slotIndex + 1]) == Global.SCORE_EXACT ) ? -2 : 0;
            
            int v3 = ( GetEntityDepth(Table2[slotIndex + 1]) <  GetEntityDepth(Table2[replaceSlotIndex + 1] ) ) ? 1 : 0;
            
            if( v1 + v2 + v3 > 0 ) {
                replaceSlot = i;
            }
           
       }
        
        Table2[index + replaceSlot * SLOT_SIZE] = (int)lock;  

        Table2[index + replaceSlot * SLOT_SIZE + 1] = move
                           | type << 16
                           | depth << 19
                           | nullFail << 25
                           | Board.ancient << 26;
        Table2[index + replaceSlot * SLOT_SIZE + 2] = value + Global.MATE_SCORE;
    }
    
    private int GetAncientVal( int bits ) {
        return (bits >> 26) & 7;
    }
    
    private int GetResultType( int bits ) {
        return (bits >> 16) & 7;
    }
    
    private int GetEntityDepth( int bits ) {
        return (bits >> 19) & 63;
    }
    
    public final int hasHash(long lock) {
        
        int key = (int)(( lock>>32) & (Global.HASHSIZE-1));
        int index = key * NUM_SLOTS * SLOT_SIZE;
        int iLock = (int)lock;
        if( iLock == Table2[index] ) {
            return index + 1;
        }
        else if( iLock == Table2[index + SLOT_SIZE] ) {
            return index + SLOT_SIZE + 1;
        }
        else if( iLock == Table2[index + SLOT_SIZE * 2] ) {
            return index + SLOT_SIZE * 2 + 1;
        }
        else if( iLock == Table2[index + SLOT_SIZE * 3] ) {
            return index + SLOT_SIZE * 3 + 1;
        }
        else {
            return -1;
        }
    }
    
    public int getValue(int index) {
        return Table2[index+1] - Global.MATE_SCORE;   
    }
    
    public final int getDepth(int index) {
        return (Table2[index] >> 19) & 63;
    }
	
    public final int getType(int index) {
        return (Table2[index] >> 16) & 7;
    }	
	
    public final int getNullFail(int index) {
        return (Table2[index] >> 25) & 1;
    }	
	
    public final int getMove(int index) {
        return Table2[index] & 65535;
    }

    public void setNew(int index) {
        Table2[index] &= AncientMask;
        Table2[index] |= Board.ancient << 26;
    }
    
    /*
     * Method addPawnHash
     * 
     * Stores an entry in the pawn hash table - always replace scheme
     * 
     * @param int key - index of entry in the pawn hash table
     * @param int lock - 32 bit hash 
     * @param int value - the score for this pawn position
     * @param int wFile - information about the white pawns on each of the 8 files packed into 32 bits
     * @param int bFile - information about the black pawns on each fo the 8 files packed into 32 bits
     * @param long passedBits - information about all the passed pawns packed into 64 bits
     *
     */ 
	public final void addPawnHash(int key, long lock, int value_mg, int value_eg, int center, int passPhase1Mid, int passPhase1End, int pawnShield, long passedBits, long whiteAttacks, long blackAttacks, long outposts) {
           
            assert(value_mg >= -4000 && value_mg <= 4000);
            assert(value_eg >= -4000 && value_eg <= 4000);
            assert(center >= -64 && center < 64);
            assert(passPhase1Mid >= -512 && passPhase1Mid < 512);
            assert(passPhase1End >= -1024 && passPhase1End < 1024);
            assert(pawnShield >= -128 && pawnShield < 128);
            int index = key * PAWN_TABLE_SIZE;
		Table[index] = lock;
		Table[index+1] = (long)(value_mg + 4000)
		| (long)(value_eg + 4000) << 13
		| (long)(center + 64) << 26
		| (long)(passPhase1Mid + 512) << 33
	   | (long)(passPhase1End + 1024) << 43
		| (long)(pawnShield + 128) << 54;
		Table[index+2] = passedBits;
		Table[index+3] = whiteAttacks;
		Table[index+4] = blackAttacks;
		Table[index+5] = outposts;
	}
	
    /*
     * Method addEvalHash
     * 
     * Stores an entry in the evaluation hash table - always replace scheme
     * 
     * @param int key - index of hash entry
     * @param long lock - 64 bit hash
     * @param int value - the score for this pawn position
     * @param long passedBits - information about all the passed pawns packed into 64 bits
     *
     */ 
    public final void AddEvalHash(int key, long lock, int value) {
		/*if(value < 0 || value > Global.MATE_SCORE * 2 ) {
            System.out.println("info string Here");
            int j = 5/0;
        }*/
        int index = key*EVAL_TABLE_SIZE;
		Table3[index] = lock;
      Table3[index+1] = (long)value;
	}

	 public final void AddEvalHashLazy(int key, long lock, int value) {
		/*if(value < 0 || value > Global.MATE_SCORE * 2 ) {
            System.out.println("info string Here");
            int j = 5/0;
        }*/
        
         int index = key*EVAL_TABLE_SIZE;
		Table3[index] = lock;
      Table3[index+1] = (long)value | LAZY_BIT;
	}
	
   
    /*
     * Method getEvalValue
     * 
     * returns the evaluation value for an entry stored in the evaluation hash
     * 
     * @param int key - index of hash entry
     *
     */ 
    public final int getEvalValue(int key) {
		return (int)Table3[key*EVAL_TABLE_SIZE+1];
	}
     
    /*
     * Method getPawnValue
     * 
     * returns the value for the pawn position stored in the evaluation hash
     * 
     * @param int key - index of hash entry
     *
     * @return int - the evaluation stored
     */ 
	public final int GetPawnValueMiddle(int key) {
		return (int)(Table[key*PAWN_TABLE_SIZE + 1] & ((1 << 13)-1) ) - 4000;
	}

	public final int GetPawnValueEnd(int key) {
		return (int)(Table[key*PAWN_TABLE_SIZE + 1] >> 13 & ((1 << 13)-1) ) - 4000;
	}

   public final int GetPawnCenterScore(int key) {
		return (int)((Table[key*PAWN_TABLE_SIZE + 1] >> 26 & 127L) - 64);
	}

	public final int GetPassPhase1Mid(int key) {
		return (int)((Table[key*PAWN_TABLE_SIZE + 1] >> 33 & 1023L) - 512);
	}

	public final int GetPassPhase1End(int key) {
		return (int)((Table[key*PAWN_TABLE_SIZE + 1] >> 43 & 2047L) - 1024);
	}

	public final int GetPawnShield(int key) {
		return (int)((Table[key*PAWN_TABLE_SIZE + 1] >> 54 & 255L) - 128);
	}
	public final long GetPawnOutposts(int key) {
		return Table[key*PAWN_TABLE_SIZE + 5];
	}

	/*
     * Method getPawnPassed
     * 
     * returns the value for the pawn position stored in the evaluation hash
     * 
     * @param int key - index of hash entry
     *
     * @return long - passed pawn information 
     */ 
    public final long getPawnPassed(int key) {
        return Table[key*PAWN_TABLE_SIZE + 2];
    }

	 public final long getWhitePawnAttack(int key) {
		 return Table[key*PAWN_TABLE_SIZE + 3];
	 }

	 public final long getBlackPawnAttack(int key) {
		 return Table[key*PAWN_TABLE_SIZE + 4];
	 }
   
    
    /*
     * Method hasPawnHash
     * 
     * returns flag indicating if a pawn hash is stored for a position
     * 
     * @param int key - index of hash entry
     * @param int lock - 32 bit hash signiture to verify this is the correct pawn position
     *
     * @return boolean - is there a pawn hash stored
     */ 
	public final boolean hasPawnHash(int key, long lock) {
		if(Table[key*PAWN_TABLE_SIZE] != lock)
			return false;
		return true;
		
	}	
	
    /*
     * Method hasEvalHash
     * 
     * returns flag indicating if an eval hash is stored for a position
     * 
     * @param int key - index of hash entry
     * @param int lock - 1st half of 64 bit hash signature
     * @param int lock2 - 2nd half of 64 bit hash signature
     *
     * @return boolean - is there an eval hash stored
     */ 
    public final boolean hasEvalHash(int key, long lock) {
		int index = key*EVAL_TABLE_SIZE;
		if(Table3[index] != lock)
			return false;
		return true;
		
	}
	
   
    /*
     * Method clearEvalHash
     * 
     * clears every entry in the eval hash table
     * 
     */ 
	public final void clearEvalHash() {
		for(int i=0;i<size*EVAL_TABLE_SIZE;i+=EVAL_TABLE_SIZE) {
			Table3[i] = 0L;
		}	
	}
	
    /*
     * Method clearPawnHash
     * 
     * clears every entry in the pawn hash table
     * 
     */ 
	public final void clearPawnHash() {
		for(int i=0;i<size*PAWN_TABLE_SIZE;i+=PAWN_TABLE_SIZE) {
			Table[i] = 0L;
			
		}	
	}
	
    /*
     * Method getCount
     * 
     * returns the nubmer of entries stored in the hash table
     * 
     * @return int - the number of entries stored
     */
	public  final int getCount() {
		return hashCount;
	}	
}	