/**
 * Transtable.java
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
 * TransTable.java
 * This class can be instantiated as a 2 layer depth first and replace always
 * transposition table (64 bit hash key)
 * 
 * Can also become a pawn hash table (32 bit hash key)
 * Can also be a evaluation hash table (64 bit hash key)
 *
 * This trans table is optimized for 32 bit computers
 * 
 * To-do - investigate if optimizing for 64 bit is worthwhile
 *
 * @version 	2.00 30 Jan 2010
 * @author 	Eric Stock
 */

public class TransTable {
	
    private int[] Table; 
	private int hashCount;
	private static final int int31 = 1<<31;
	private static final int mask = 1<<25 | 1<<26 | 1<<27;
	private static final int mask2 = ~mask;
	
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
			Table = new int[size*8];
		else if (type == 1)
			Table = new int[size*6];
		else if (type == 2)
			Table = new int[size*3];
		hashCount = 0;
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
	public final void addPawnHash(int key, int lock, int value, int wFile, int bFile, long passedBits) {
		int index = key * 6;
		Table[index] = lock;
		Table[index+1] = value;
		Table[index+2] = wFile;
        Table[index+3] = bFile;
        Table[index+4] = (int)passedBits;
        Table[index+5] = (int)(passedBits>>>32);
	}
	
    /*
     * Method addEvalHash
     * 
     * Stores an entry in the evaluation hash table - always replace scheme
     * 
     * @param int key - index of hash entry
     * @param int lock - 1st half of 64 bit hash
     * @param int lock2 - 2nd half of 64 bit hash
     * @param int value - the score for this pawn position
     * @param int wFile - information about the white pawns on each of the 8 files packed into 32 bits
     * @param int bFile - information about the black pawns on each fo the 8 files packed into 32 bits
     * @param long passedBits - information about all the passed pawns packed into 64 bits
     *
     */ 
    public final void addEvalHash(int key, int lock, int lock2, int value) {
		int index = key*3;
		Table[index] = lock;
		Table[index+1] = lock2;
        Table[index+2] = value;    
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
    public final void addHash(int key,int lock, int lock2,int move,int value,int depth,int type,int nullFail,int ancient) {
		int index = key*8;
		move &= 8388607;
		
        /** if empty slot, add entry */
        if(Table[index]==0)  {
			hashCount++;
		Table[index] = lock;				
		Table[index+1] = lock2; 							
		Table[index+2] = move & 8388607;
		Table[index+3] = (value + 21000)
			| (type << 16)
			| (depth << 19)
			| (nullFail << 24)
			| (ancient << 25);
			return;
			
		}/** replace if depth greater */
		else if(depth>=((Table[index+3]>>19)&31) || ((Table[index+3] >> 25)&7)!=ancient) { 
			Table[index] = lock;				
			Table[index+1] = lock2;							
			Table[index+2] = move & 8388607;
			Table[index+3] = (value + 21000)
			| (type << 16)
			| (depth << 19)
			| (nullFail << 24)
			| (ancient << 25);
			return;
		
		}/** always replace/add into second level */
        if(Table[index+4]==0) 
            hashCount++;
        Table[index+4] = lock;			
        Table[index+5] = lock2; 							
        Table[index+6] = move & 8388607;
        Table[index+7] = (value + 21000)
        | (type << 16)
        | (depth << 19)
        | (nullFail << 24)
        | (ancient << 25);
        return;
		
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
		return Table[key*3+2];
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
	public final int getPawnValue(int key) {
		return Table[key*6+1];
	}	
    
    /*
     * Method getWPawnFile
     * 
     * returns information of white pawn placement on each file
     * 
     * @param int key - index of hash entry
     *
     * @return int - the file info
     */ 
    public final int getWPawnFile(int key) {
        return Table[key*6+2];  
    }
    
    /*
     * Method getBPawnFile
     * 
     * returns the value for the pawn position stored in the evaluation hash
     * 
     * @param int key - index of hash entry
     *
     * @return int - the file info
     */ 
	public final int getBPawnFile(int key) {
            return Table[key*6+3];
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
        int lowPass = Table[key*6+4];
        int highPass = Table[key*6+5];

        /** a litle bit twidling here since java doesn't support unsigned ints */
        if( (lowPass & int31) != 0) {           
            lowPass ^= int31;
            long passed = ((long)lowPass | ((long)highPass)<<32);
            passed |= (long)1<<31;  
            return passed;
        } else {
            return ((long)lowPass | ((long)highPass)<<32);
        }   
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
		return (Table[key*8+3+probe]&65535)-21000;
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
		int temp = Table[key*8+3+probe];
		return ((temp>>19)&31);
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
		int temp = Table[key*8+3+probe];
		return ((temp>>16)&7);
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
		return (Table[key*8+3+probe]>>24)&1;
			
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
		return Table[key*8+2+probe];
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
		if(Table[key*6] != (int)lock)
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
    public final boolean hasEvalHash(int key, int lock, int lock2) {
		int index = key*3;
		if(Table[index] != lock || Table[index+1] != lock2)
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
	public final int hasHash(int key, int lock, int lock2) {
		int index = key*8;
		if( lock == Table[index] && lock2 == Table[index+1])
			return 0;
		else if(lock == Table[index+4] && lock2 == Table[index+5])
			return 4;	
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
	public final int hasSecondHash(int key, int lock, int lock2) {
		if(lock == Table[key*8+4] && lock2 == Table[key*8+5])
			return 4;
		return 1;
	}
	
    /*
     * Method clearPosition
     * 
     * clears the position in the hash table
     * 
     * @param int key - index of hash entry
     */ 
	public final void clearPosition(int key) {
		Table[key*8] = 0;
		Table[key*8+4] = 0;
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
		Table[key*8+3+probe] &= mask2;
		Table[key*8+3+probe] |= (ancient<<25);
	}	
	
    /*
     * Method clearEvalHash
     * 
     * clears every entry in the eval hash table
     * 
     */ 
	public final void clearEvalHash() {
		for(int i=0;i<size*3;i+=3) {
			Table[i] = 0;	
		}	
	}
	
    /*
     * Method clearPawnHash
     * 
     * clears every entry in the pawn hash table
     * 
     */ 
	public final void clearPawnHash() {
		for(int i=0;i<size*6;i+=6) {
			Table[i] = 0;	
			
		}	
	}
	
    /*
     * Method clearHash
     * 
     * clears every entry in the hash table
     * 
     */
	public final void clearHash() {
		hashCount = 0;
		for(int i=0;i<size*8;i+=4) {
			Table[i] = 0;	
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