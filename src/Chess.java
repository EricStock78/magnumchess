
public final class Chess{	
	
		
	private static final int size = 64;
	private static final int attacks = 256;
	private static final long[] KingMoveBoard 	= new long[size];
	private static final long[] WhitePawnMoveBoard		= new long[size];
	private static final long[] WhitePawnAttackBoard 	= new long[size];
	private static final long[] BlackPawnMoveBoard		= new long[size];
	private static final long[] BlackPawnAttackBoard 	= new long[size];
	private static final long[] KnightMoveBoard 		= new long[size];
	private static final long[] WKingBoard = new long[attacks];
	
	public Chess() {
		
		
		int length;
		
		initKingBoard(KingMoveBoard);
		initWhitePawnMoveBoard(WhitePawnMoveBoard);
		initWhitePawnAttackBoard(WhitePawnAttackBoard);
		initKnightMoveBoard(KnightMoveBoard);
		initBlackPawnMoveBoard(BlackPawnMoveBoard);
		initBlackPawnAttackBoard(BlackPawnAttackBoard);
	

	for(int i=0;i<256;i++) {
			setWKingCastle(i);
			//setBKingCastle(i);
	}
	}; // End constructor
	
	
	
	/***********************************************************************
		Name:		getWhitePawnMove
		Parameters:	int
		Returns:	BitBoard
		Description:This method returns 
	***********************************************************************/
	public static final long getWhitePawnMove(int square){
		return WhitePawnMoveBoard[square];
	}// End getWhitePawnMove
	
	/***********************************************************************
		Name:		getBlackPawnMove
		Parameters:	int
		Returns:	BitBoard
		Description:This method returns 
	***********************************************************************/
	public static final long getBlackPawnMove(int square){
		return BlackPawnMoveBoard[square];
	}// End getBlackPawnMove

	/***********************************************************************
		Name:		getKnightPosition
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet from KnightMoveBoard
					specified via the parameter
	***********************************************************************/
	public static final long getKnightPosition(int square){
		return KnightMoveBoard[square];
	}// End getKnightPostion
	
	/***********************************************************************
		Name:		getKingPosition
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet from KingMoveBoard
					specified via the parameter
	***********************************************************************/
	public static final long getKingPosition(int square){
		return KingMoveBoard[square];
	}// End getKingPosition
	
	/***********************************************************************
		Name:		getWhitePawnAttack
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet from WhitePawnAttackBoard
					specified via the parameter
	***********************************************************************/
	public static final long getWhitePawnAttack(int square){
		return WhitePawnAttackBoard[square];
	}// End getWhitePawnAttack
	
	/***********************************************************************
		Name:		getBlackPawnAttack
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet from BlackPawnAttackBoard
					specified via the parameter
	***********************************************************************/
	public static final long getBlackPawnAttack(int square){
		return BlackPawnAttackBoard[square];
	}// End getBlackPawnAttack
	
	/***********************************************************************
		Name:		initKnightMoveBoard
		Parameters:	BitSet
		Returns:	None
		Description:This method initializes the BitSet representing all 
					of the moves a Knight can make
	***********************************************************************/
	private static final void initKnightMoveBoard(long[] board){
		int square;
		int row;
		int x;
		int column;
		
		for(square=0;square<64;square++){
			// This next section on conditionals tests to 
			// see if the knight is near the center of the 
			// board where it can has a choice of eight
			// moves
				if(square-17>=0) 
					board[square] = (long)1<<(square-17);
				if(square-15>=0)
					board[square] |= (long)1<<(square-15);		
				if(square-10>=0)
					board[square] |= (long)1<<(square-10);	
				if(square-6>=0)
					board[square] |= (long)1<<(square-6);		
				if(square+6<=63)
					board[square] |= (long)1<<(square+6);	
				if(square+10<=63)
					board[square] |= (long)1<<(square+10);	
				if(square+15<=63)
					board[square] |= (long)1<<(square+15);		
				if(square+17<=63)
					board[square] |= (long)1<<(square+17);	
				
			if(square%8<=1) {
				if(square+6<=63)
					board[square] ^= (long)1<<(square+6);	
				if(square-10>=0)	
					board[square] ^= (long)1<<(square-10);	
			}
			// This next section of code tests to see if the knight 
			// is in one of the corners
			if(square%8==0) {
				if(square+15<=63)
					board[square] ^= (long)1<<(square+15);		
				if(square-17>=0)
					board[square] ^= (long)1<<(square-17);		
			}
			if(square%8>=6) {
				if(square-6>=0)
					board[square] ^= (long)1<<(square-6);			
				if(square+10<=63)
					board[square] ^= (long)1<<(square+10);		
			}
			if(square%8==7) {
				if(square+17<=63)
					board[square] ^= (long)1<<(square+17);			
				if(square-15>=0)
					board[square] ^= (long)1<<(square-15);			
			}	
		}		
	}		
	
	/***********************************************************************
		Name:		initBlackPawnAttackBoard
		Parameters:	BitSet[]
		Returns:	None
		Description:This method initializes the BitSet representing all of 
					the possible attacks a Black pawn can dp
	***********************************************************************/
	private static final  void initBlackPawnAttackBoard(long[] board){
		int square;
		for(square=63;square>7;square--){
			if(square%8 == 0){
				board[square] = (long)1<<(square-7);			
			}
			else if(square % 8 == 7){
				board[square] = (long)1<<(square-9);			
			}
			else {
				board[square] = (long)1<<(square-7);			
				board[square] |= (long)1<<(square-9);			
			} 
		}
	}// End BlackPawnAttackBoard
	
	/***********************************************************************
		Name:
		Parameters:
		Returns:
		Description:
	***********************************************************************/
	private static final  void initBlackPawnMoveBoard(long[] board){
		int square;
		for(square=55;square>7;square--){
				if(square>=48)
					board[square] |= (long)1<<(square-16);
				board[square] |= (long)1<<(square-8);
		}// End for 
	}// End initBlackPawnAttackBoard
	
	/***********************************************************************
		Name:		initWhitePawnMoveBoard
		Parameters:	BitSet[]
		Returns:	None
		Description:This method initializes the WhitePawnBoard. It accounts
					for the first move principle for the pawns.
	***********************************************************************/
	private static final void initWhitePawnMoveBoard(long[] board){
		int square;
		for(square=0;square<=55;square++){
			if(square >= 8 && square <=15){
				board[square] |=(long)1<<(square+16);
			}
			board[square] |= (long)1<<(square+8);
		
		}// End for 
	}// End initWhitePawnMoveBoard
	
	/***********************************************************************
		Name:		initWhitePawnAttackBoard
		Parameters:	BitSet[]
		Returns:	None
		Description:This method initilaizes the BitSet representing all of
					the possible attacks a white pawn can do
	***********************************************************************/
	private static final void initWhitePawnAttackBoard(long[] board){
		int square;
		for(square=0;square<56;square++){
			if(square%8 == 0){
				board[square] = (long)1<<(square+9);	//powOf2[squareTopRight(square)];
			}
			else if(square%8 == 7){
				board[square] = (long)1<<(square+7);		//powOf2[squareTopLeft(square)];
			}
			else{
				board[square] = (long)1<<(square+7);		//powOf2[squareTopLeft(square)];
				board[square] |= (long)1<<(square+9);		//powOf2[squareTopRight(square)];
			}// End if
		}// End for
	}// End initWhitePawnAttackBoard
	
	/***********************************************************************
		Name:		initKingBoard
		Parameters:	BitSet[]
		Returns:	None
		Description:This method initializes the BitSet representing the 
					king move board
	***********************************************************************/
	private static final void initKingBoard(long[] board){
		int square;
		
		for(square=0;square<64;square++){
			int rank = square/8;
			int file = square%8;
			board[square]=0;
			if(rank>0) {
				board[square] |= (long)1<<(square-8);
				if(file>0)
					board[square] |= (long)1<<(square-9);
				if(file<7)
					board[square] |= (long)1<<(square-7);
			}
			if(rank<7) {
				board[square] |= (long)1<<(square+8);
				if(file>0) 
					board[square] |= (long)1<<(square+7);
				if(file<7)
					board[square] |= (long)1<<(square+9);
			}
			if(file>0)
				board[square] |= (long)1<<(square-1);
			if(file<7)
				board[square] |= (long)1<<(square+1);
		}
	}
	
	
	
	
	public static final long getKingCastle(int file) {
		return WKingBoard[file];
	}

	private static final void setWKingCastle(int file) {
		if(file>=144 && file < 160) {
			WKingBoard[file] = (long)1<<6;
		}
		long temp = file&31;
		if(temp==17)
			WKingBoard[file] |= (long)1<<2;
	}
	
	
	public static final long getDiag1Attack(int index1, int index2) {
		
		int DistToLeft = 0;						//used to find spot in index2
		int oldIndex2 = index2;
		int temp = index1;	
		index2 &= ((1<<Global.Diag1Length[index1])-1);
		long diag = 0;
		while(temp%8!=0 && temp<56) {			//while not at most left square
			temp+=7;
			DistToLeft++;
		}
		
		if(index1%8!=0) {
			int ind2 = DistToLeft;
			for(int i=index1+7;i<64&&i%8!=7;i+=7) {			//move up and left along diagonal
				ind2--;
				diag |= (long)1<<i;
				if((index2&(long)1<<ind2)!=0) break; 
			}
		}
		
		if(index1%8!=7) {
			int ind2=DistToLeft;
			for(int i=index1-7;i>=0&&i%8!=0;i-=7) {
				ind2++;
				diag |= (long)1<<i;
				if((index2&(long)1<<(ind2))!=0) break;
		
			}
		}
		return diag;
	}
	
	public static final long getDiag2Attack(int index1, int index2) {
		
		int DistToLeft = 0;						//used to find spot in index2
		int oldIndex2 = index2;
		int temp = index1;	
		index2 &= ((1<<Global.Diag2Length[index1])-1);
		long diag = 0;
		
		while(temp%8!=0 && temp>7) {			//while not at most left square
			temp-=9;
			DistToLeft++;
		}
		
		if(index1%8!=0) {
			int ind2 = DistToLeft;
			for(int i=index1-9;i>=0&&i%8!=7;i-=9) {			//move down and left along diagonal
				ind2--;
				diag |= (long)1<<i;
				if((index2&(long)1<<ind2)!=0) break; 
			}
		}
		
		if(index1%8!=7) {
			int ind2=DistToLeft;
			for(int i=index1+9;i<64&&i%8!=0;i+=9) {
				ind2++;
				diag |= (long)1<<i;
				if((index2&(long)1<<(ind2))!=0) break;
		
			}
		}
		return diag;
	}


	
	public static final long getRooksRank2(int index1,int index2) {
	
		long rank = 0; 
		for(int i=index1+1;i<index1/8*8+8;i++)   {		//move up file and look for pieces in the way
			rank |= (long)1<<(i);
			if((index2&(long)1<<i%8)!=0) break;
		}
		for(int i=index1-1;i>=index1/8*8;i--) {				//move down file and look for pieces in the way
			rank |= (long)1<<(i);
			if((index2&(long)1<<i%8)!=0) break;
		}
		return rank;
}
		
	
	public static final long getRooksFile2(int index1,int index2) {
	
		long file = 0;
		 
		for(int i=index1+8;i<64;i+=8)   {		//move up file and look for pieces in the way
			file |= (long)1<<(i);
			if((index2&(long)1<<i/8)!=0) break;
		}
		for(int i=index1-8;i>=0;i-=8) {				//move down file and look for pieces in the way
			file |= (long)1<<(i);
			if((index2&(long)1<<i/8)!=0) break;
		}
		return file;
}
	
	
}	
		