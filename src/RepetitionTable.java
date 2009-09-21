/**
 * @(#)RepetitionTable.java
 *
 *
 * @author 
 * @version 1.00 2008/5/30
 */


public class RepetitionTable {
	private int[] table;
   
    public RepetitionTable() {
    	table = new int[Global.REPSIZE];
    }
    public final int addPosition(int key) {
    	int reps = 1;
    	int index = key % Global.REPSIZE;
    	
    	int decrement = (key/Global.REPSIZE) % Global.REPSIZE;
    	decrement |= 1;
    	//if(decrement == Global.REPSIZE ) decrement = 1;
    	
    	while(table[index] != 0) {
    		if(table[index] == key) reps++;
    		index -= decrement;
    		if(index < 0)
    			index += Global.REPSIZE;
    			
    	}
    	table[index] = key;
    	return reps;
    }
    
	public final void removePosition(int key) {
		int index = key % Global.REPSIZE;
		int decrement = (key/Global.REPSIZE) % Global.REPSIZE;
		int lastIndex = 0;
		decrement |= 1;
		//if(decrement == 0) decrement = 1;
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