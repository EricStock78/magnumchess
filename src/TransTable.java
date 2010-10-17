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
	
   private long[] Table;
	private long[] Table2;
   private long[] Table3;
   private int hashCount;
	//private static final int int31 = 1<<31;
	//private static final int mask = 1<<25 | 1<<26 | 1<<27;
	//private static final int mask2 = ~mask;
   private static final long mask3 = ~((long)1 << 57 | (long)1 << 58 | (long)1 << 59);
	
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
			Table = new long[size*4];
		else if (type == 2)
			Table3 = new long[size*2];
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
	public final void addPawnHash(int key, long lock, int value, int wFile, int bFile, long passedBits) {
		int index = key * 4;
		Table[index] = lock;
		Table[index+1] = (long)value;
		Table[index+2] = (long)wFile<<32 | (long)bFile;
      Table[index+3] = passedBits;
     
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
    public final void addEvalHash(int key, long lock, int value) {
		int index = key*2;
		Table3[index] = lock;
      Table3[index+1] = (long)value;
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
    public final void addHash(int key,long lock,int move,long value,int depth,int type,int nullFail,int ancient) {

      int index = key*4;
      /** if empty slot, add entry */
      if(Table2[index]==0)  {
      hashCount++;
      Table2[index] = lock;
      Table2[index+1] = (long)move
           | ((value + 21000) << 32)
           | ((long)type << 48)
           | ((long)depth << 51)
           | ((long)nullFail << 56)
           | ((long)ancient << 57);

		}/** replace if depth greater */
		else if(depth>=((Table2[index+1]>>51)&31) || ((Table2[index+1] >> 57)&7)!=ancient) {
			Table2[index] = lock;
         Table2[index+1] = (long)move
           |(((value + 21000)) << 32)
           | ((long)type << 48)
           | ((long)depth << 51)
           | ((long)nullFail << 56)
           | ((long)ancient << 57);
          
			
		}/** always replace/add into second level */
      if(Table2[index+2]==0)
            hashCount++;
         Table2[index+2] = lock;
         Table2[index+3] = (long)move
           |(((value + 21000)) << 32)
           | ((long)type << 48)
           | ((long)depth << 51)
           | ((long)nullFail << 56)
           | ((long)ancient << 57);
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
		return (int)Table3[key*2+1];
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
		return (int)Table[key*4+1];
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
        return(int)(Table[key*4+2] >> 32);
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
            return (int)Table[key*4+2];
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
        return Table[key*4+3];
      /*
        int lowPass = Table[key*6+4];
        int highPass = Table[key*6+5];

        
        if( (lowPass & int31) != 0) {           
            lowPass ^= int31;
            long passed = ((long)lowPass | ((long)highPass)<<32);
            passed |= (long)1<<31;  
            return passed;
        } else {
            return ((long)lowPass | ((long)highPass)<<32);
        }   */
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
    public final long getValue(int key,int probe) {
		return ((Table2[key*4+1+probe] >>> 32)&65535L) - 21000;
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
		return (int)((temp>>51)&31L);
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
		return (int)(Table2[key*4+1+probe]>>56)&1;
			
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
		if(Table[key*4] != lock)
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
		int index = key*2;
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
	public final int hasHash(int key, long lock) {
      int index = key*4;
		if( lock == Table2[index])
			return 0;
		else if(lock == Table2[index+2])
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
	public final int hasSecondHash(int key, long lock) {
      if(lock == Table2[key*4+2])
			return 2;
		return 1;
	}
	
    /*
     * Method clearPosition
     * 
     * clears the position in the hash table
     * 
     * @param int key - index of hash entry
     
	public final void clearPosition(int key) {
		Table2[key*4] = 0L;
		Table2[key*4+2] = 0L;
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
         Table2[key*4+1] |= ((long)ancient << 57);
      }
      else if(probe == 2 && ((Table2[key*4 + 3] >> 57)&7)!=ancient)  {
         Table2[key*4 + 3] &= mask3;
         Table2[key*4 + 3] |= ((long)ancient << 57);
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
		for(int i=0;i<size*2;i+=2) {
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
		for(int i=0;i<size*4;i+=4) {
			Table[i] = 0L;
			
		}	
	}
	
    /*
     * Method clearHash
     * 
     * clears every entry in the hash table
     * 
    
	public final void clearHash() {
		hashCount = 0;
		for(int i=0;i<size*4;i+=2) {
			Table2[i] = 0L;
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