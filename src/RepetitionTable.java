/**
 * RepetitionTable.java
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
 * RepetitionTable.java
 * This class stores all searched positions at each point in the search
 * This is used to see if the position has previously occured
 * Thus draw by repetition situations can be detected
 *
 * A hash table using double hashing is used for this
 * 
 *
 * @version 	2.00 30 Jan 2010
 * @author 	Eric Stock
 */



public class RepetitionTable {
	private int[] table;
   
    /*
     * RepetitionTable constructor
     * 
     * Initializes the array which contains the hash entries
     *
     */
    public RepetitionTable() {
    	table = new int[Global.REPSIZE];
    }
    
    /*
     * Method addPosition
     * 
     * Adds a position to the rep table
     * 
     * @param key - 32bit hash signature for the position
     * 
     * @param int - number of times this position has occured ( >=1 as it has occured at least once now)
     *
     */
    public final int addPosition(int key) {
    	int reps = 1;
        /** index is calculated using the primary hash function */
    	int index = key % Global.REPSIZE;
    	/** decrement uses the secondary hash function */
    	int decrement = (key/Global.REPSIZE) % Global.REPSIZE;
    	decrement |= 1;
    	while(table[index] != 0) {
    		if(table[index] == key) reps++;
    		index -= decrement;
    		if(index < 0)
    			index += Global.REPSIZE;
    			
    	}
    	table[index] = key;
    	return reps;
    }
    
    /*
     * Method removePosition
     * 
     * Removes a position from the rep table
     * 
     * @param key - 32bit hash signature for the position
     * 
     */
	public final void removePosition(int key) {
		int index = key % Global.REPSIZE;
		int decrement = (key/Global.REPSIZE) % Global.REPSIZE;
		int lastIndex = 0;
		decrement |= 1;
		while(table[index] != 0) {
			if(table[index] == key)
				lastIndex = index;
			index -= decrement;
			if(index < 0)
				index += Global.REPSIZE;
				
		}
		table[lastIndex] = 0;
	}

}