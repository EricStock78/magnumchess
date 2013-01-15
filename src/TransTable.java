/**
 * Transtable.java
 *
 * Version 4.0   
 * 
 * Copyright (c) 2013 Eric Stock
 
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
   private long[] Table2;
   private long[] Table3;
   private int hashCount;
   private Board chessBoard;
   private static final long mask3 = ~((long)1 << 58 | (long)1 << 59 | (long)1 << 60);
	public static final long LAZY_BIT = ( (long)1 << 31);
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
		if(type == 0)
			Table2 = new long[size*4];
		else if (type == 1)
			Table = new long[size*PAWN_TABLE_SIZE];
		else if (type == 2)
			Table3 = new long[size*EVAL_TABLE_SIZE];
		hashCount = 0;
		chessBoard = Board.getInstance();
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
		int index = key*EVAL_TABLE_SIZE;
		Table3[index] = lock;
      Table3[index+1] = (long)value;
	}

	 public final void AddEvalHashLazy(int key, long lock, int value) {
		int index = key*EVAL_TABLE_SIZE;
		Table3[index] = lock;
      Table3[index+1] = (long)value | LAZY_BIT;
	}
	
    /*
     * Method addEvalHash
     * 
     * Stores an entry in the 2 level hash table - depth first and always replace schemes used
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
    public final void addHash(int key,int move,long value,int depth,int type,int nullFail,int ancient) {
                assert( value > -21000 && value < 21000);
                assert( type >= 0 && type < 8);
                assert( depth >= 0 && depth < 64 );
                assert( nullFail >= 0 && nullFail < 2);
                assert( ancient >= 0 && ancient < 8);
		int index = key*4;
		/** if empty slot, add entry */
		long word = (long)move
			| ((long)(value + 21000) << 32)
			| ((long)type << 48)
			| ((long)depth << 51)
			| ((long)nullFail << 57)
			| ((long)ancient << 58);

		if(Table2[index]==0)  {
			hashCount++;
			Table2[index] = chessBoard.hashValue;
			Table2[index+1] = word;
		}/** replace if depth greater */
		else if(depth >= (int)((Table2[index+1]>>51)&63L) || (int)((Table2[index+1] >> 58) & 7L) != ancient) {
			Table2[index] = chessBoard.hashValue;
			Table2[index+1] = word;
		}
		if(Table2[index+2]==0)
			hashCount++;
		Table2[index+2] = chessBoard.hashValue;
		Table2[index+3] = word;
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
     * Method getValue
     * 
     * returns the value stored position
     * 
     * @param int key - index of hash entry
     * @param int probe - indicates if this is the first or second entry in the 2 depth scheme hash table
     *
     * @return int - the value
     */     
    public final int getValue(int key,int probe) {
		return (int) (((Table2[key*4+1+probe] >>> 32)&65535L) - 21000);
    }
	
    /*
     * Method getDepth
     * 
     * returns the depth for the hash position
     * 
     * @param int key - index of hash entry
     * @param int probe - indicates if this is the first or second entry in the 2 depth scheme hash table
     *
     * @return int - the depth
     */ 
    public final int getDepth(int key,int probe ) {
		long temp = Table2[key*4+1+probe];
		return (int)((temp>>51)&63L);
	}
	
    /*
     * Method getType
     * 
     * returns the type of node stored
     * 
     * @param int key - index of hash entry
     * @param int probe - indicates if this is the first or second entry in the 2 depth scheme hash table
     *
     * @return int - the type (0 upper, 1 exact, 2 lower, 4) 
     */ 
    public final int getType(int key,int probe) {
		long temp =  Table2[key*4+1+probe];
		return (int)((temp>>48)&7L);
	}	
	
    /*
     * Method getNullFail
     * 
     * returns flag indicating if null move pruning shouldn't be used here
     * 
     * @param int key - index of hash entry
     * @param int probe - indicates if this is the first or second entry in the 2 depth scheme hash table
     *
     * @return int - 0 use null, 1 don't
     */ 
    public final int getNullFail(int key,int probe) {
		return (int)(Table2[key*4+1+probe]>>57)&1;
			
	}	
	
    /*
     * Method getNullFail
     * 
     * returns flag indicating if null move pruning shouldn't be used here
     * 
     * @param int key - index of hash entry
     * @param int probe - indicates if this is the first or second entry in the 2 depth scheme hash table
     *
     * @return int - 0 use null, 1 don't
     */ 
    public final int getMove(int key,int probe) {
       return (int)(Table2[key*4+1+probe]);
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
     * Method hasHash
     * 
     * returns flag indicating if a hash entry is stored for a position
     * 
     * @param int key - index of hash entry
     * @param int lock - 1st half of 64 bit hash signature
     * @param int lock2 - 2nd half of 64 bit hash signature
     *
     * @return boolean - is there a hash stored (0 hash at slot 1, 4 hash at slot 2, 1 none)
     */ 
	public final int hasHash(int key) {
		int index = key*4;
		if( chessBoard.hashValue == Table2[index])
			return 0;
		else if(chessBoard.hashValue == Table2[index+2])
			return 2;
		return 1;
	}
    /*
     * Method hasSecondHash
     * 
     * returns flag indicating if a hash entry at the second level in the table is
     * stored for the position
     * 
     * @param int key - index of hash entry
     * @param int lock - 1st half of 64 bit hash signature
     * @param int lock2 - 2nd half of 64 bit hash signature
     *
     * @return boolean - is there a hash stored (4 hash at slot 2, 1 none)
     */ 
	public final int hasSecondHash(int key) {
		if(chessBoard.hashValue == Table2[key*4+2])
			return 2;
		return 1;
	}
	
    /*
     * Method setNew
     * 
     * re-sets the "freshness" of a hash entry
     * 
     * @param int key - index of hash entry
     * @param int probe - the level in the hash table
     * @param int ancient - new "freshness" value
     */ 
	public void setNew(int key,int probe,int ancient) {
		if(probe == 0)   {
         Table2[key*4+1] &= mask3;
         Table2[key*4+1] |= ((long)ancient << 58);
      }
      else if(probe == 2 && ((Table2[key*4 + 3] >> 58)&7)!=ancient)  {
         Table2[key*4 + 3] &= mask3;
         Table2[key*4 + 3] |= ((long)ancient << 58);
         Table2[key*4] = Table2[key*4 + 2];
         Table2[key*4 + 1] = Table2[key*4 + 3];
      }
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