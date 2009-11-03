


public final class TransTable {
	private int[] Table; 
	private int hashCount;
	private static final int int31 = 1<<31;
	private static final int int25 = 1<<25;
	private static final int mask = 1<<25 | 1<<26 | 1<<27;
	private static final int mask2 = ~mask;
	private int type;						//flag indicating what type of table...0 regular, 1 pawn, 2 eval
	private int size;
	
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
	public final void addPawnHash(int key, int lock, int value, int wFile, int bFile, long passedBits) {
		int index = key * 6;
		Table[index] = lock;
		Table[index+1] = value;
		Table[index+2] = wFile;
                Table[index+3] = bFile;
                Table[index+4] = (int)passedBits;
                Table[index+5] = (int)(passedBits>>>32);
	}
	public final void addEvalHash(int key, int lock, int lock2, int value) {
		int index = key*3;
		Table[index] = lock;
		Table[index+1] = lock2;
        Table[index+2] = value;    
	}
	
	//public final void addHash(int key,int lock, int lock2,int to,int from,int value,int depth,int type,int nullFail,int ancient) {
	public final void addHash(int key,int lock, int lock2,int move,int value,int depth,int type,int nullFail,int ancient) {
		
		//long temp,temp2;
		int index = key*8;
		move &= 8388607;
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
			
		}
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
		
		}// else {
		
		
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
		//}
	}
	public final int getEvalValue(int key) {
		return Table[key*3+2];
	}
       
	public final int getPawnValue(int key) {
		return Table[key*6+1];
	}	
        public final int getWPawnFile(int key) {
            return Table[key*6+2];  
        }
        
	public final int getBPawnFile(int key) {
            return Table[key*6+3];
        }
        
        public final long getPawnPassed(int key) {
            int lowPass = Table[key*6+4];
            int highPass = Table[key*6+5];
            
            if( (lowPass & int31) != 0) {
                lowPass ^= int31;
                long passed = ((long)lowPass | ((long)highPass)<<32);
                passed |= (long)1<<31;  
                return passed;
            } else {
                return ((long)lowPass | ((long)highPass)<<32);
            }   
        }  
        
        
        public final int getValue(int key,int probe) {
		return (Table[key*8+3+probe]&65535)-21000;
	}	
	public final int getDepth(int key,int probe ) {
		int temp = Table[key*8+3+probe];
		return ((temp>>19)&31);
	}
	public final int getType(int key,int probe) {
		int temp = Table[key*8+3+probe];
		return ((temp>>16)&7);
	}	
	public final int getNullFail(int key,int probe) {
		return (Table[key*8+3+probe]>>24)&1;
			
	}	
	public final int getMove(int key,int probe) {
		return Table[key*8+2+probe];
	}

	public final boolean hasPawnHash(int key, long lock) {
		//int index = key*6;
		if(Table[key*6] != (int)lock)
			return false;
		return true;
		
	}	
	public final boolean hasEvalHash(int key, int lock, int lock2) {
		int index = key*3;
		if(Table[index] != lock || Table[index+1] != lock2)
			return false;
		return true;
		
	}
	
	public final int hasHash(int key, int lock, int lock2) {
		
		int index = key*8;
		//if(Table[index]==0 && Table[index+4]==0)
		//	return 1;
		if( lock2 == Table[index+1])
			return 0;
		else if( lock2 == Table[index+5])
			return 4;	
		return 1;
	}
	public final int hasSecondHash(int key, int lock, int lock2) {
		
		//int index = key*8+4;
		//if(Table[index]==0)
		//	return 1;
		if( lock2 == Table[key*8+5])
			return 4;
		return 1;
	}
	
	public final void clearPosition(int key) {
		Table[key*8] = 0;
		Table[key*8+4] = 0;
	}
	/*
	public boolean isAncient(int key,int probe) {
		if(((Table[key*8+3+probe]>>>25)&1)==1)
			return true;
		else
			return false;
	}*/
	public void setNew(int key,int probe,int ancient) {
		Table[key*8+3+probe] &= mask2;
		Table[key*8+3+probe] |= (ancient<<25);
	}	
	
	
	public final void clearEvalHash() {
		//hashCount = 0;
		for(int i=0;i<size*3;i+=3) {
			Table[i] = 0;	
			
		}	
	}
	
	public final void clearPawnHash() {
		//hashCount = 0;
		for(int i=0;i<size*6;i+=6) {
			Table[i] = 0;	
			
		}	
	}
	
	public final void clearHash() {
		hashCount = 0;
		for(int i=0;i<size*8;i+=4) {
			Table[i] = 0;	
			
		}	
	}
	public  final int getCount() {
		return hashCount;
	}	
	
}	