/**
 * MoveHelper.java
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
 * MoveHelper.java
 *
 * follows singleton design pattern
 * this class initializes and stores in memory arrays of attacks for non sliding pieces
 * the pieces are king, pawns, and knight
 * There are also functions to assist in rook, bishop and queen moves  - These perhaps
 * don't belong here
 * 
 * To-do - some of the functions here should be placed in the Board.java class
 *
 * @version 	2.00 30 Jan 2010
 * @author 	Eric Stock
 */


public final class MoveHelper{	
	
    /** king moves for each square */
	private final long[] KingMoveBoard = new long[64];
	/** white pawn moves for each square */
    private final long[] WhitePawnMoveBoard	= new long[64];
	/** white pawn attack moves for each sqaure */
    private final long[] WhitePawnAttackBoard = new long[64];
	/** black pawn moves for each square */
    private final long[] BlackPawnMoveBoard	= new long[64];
	/** black pawn attack moves for each square */
    private final long[] BlackPawnAttackBoard = new long[64];
	/** knight moves for each square */
    private final long[] KnightMoveBoard = new long[64];
	/** castle moves based on rank occupancy */
    private final long[] kingCastleBoard = new long[256];
	
    /** only instance of class due to singleton pattern */
    private static final MoveHelper INSTANCE = new MoveHelper();	
    
	/*
     * MoveHelper Constructor
     *
     * calls methods to fill arrays with needed values for move generation
     */
    private MoveHelper() {
		initKingBoard(KingMoveBoard);
		initWhitePawnMoveBoard(WhitePawnMoveBoard);
		initWhitePawnAttackBoard(WhitePawnAttackBoard);
		initKnightMoveBoard(KnightMoveBoard);
		initBlackPawnMoveBoard(BlackPawnMoveBoard);
		initBlackPawnAttackBoard(BlackPawnAttackBoard);
        for(int i=0;i<256;i++) {
			setKingCastle(i);
        }
	};
	/*
     * Method getInstance()
     *
     * returns initialized instance of class
     * 
     * @return MoveHelper - the object
     * 
     */
	public static MoveHelper getInstance() {
        return INSTANCE;
    }    
	
	/***********************************************************************
		Name:		getWhitePawnMove
		Parameters:	int
		Returns:	BitBoard
		Description:This method returns 
	***********************************************************************/
	
    public final long getWhitePawnMove(int square){
		return WhitePawnMoveBoard[square];
	}
	
	/***********************************************************************
		Name:		getBlackPawnMove
		Parameters:	int
		Returns:	BitBoard
		Description:This method returns 
	***********************************************************************/
	public  final long getBlackPawnMove(int square){
		return BlackPawnMoveBoard[square];
	}

	/***********************************************************************
		Name:		getKnightPosition
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet from KnightMoveBoard
					specified via the parameter
	***********************************************************************/
    public final long getKnightPosition(int square){
		return KnightMoveBoard[square];
	}
	
	/***********************************************************************
		Name:		getKingPosition
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet from KingMoveBoard
					specified via the parameter
	***********************************************************************/
	public final long getKingPosition(int square){
		return KingMoveBoard[square];
	}
	
	/***********************************************************************
		Name:		getWhitePawnAttack
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet from WhitePawnAttackBoard
					specified via the parameter
	***********************************************************************/
	public final long getWhitePawnAttack(int square){
		return WhitePawnAttackBoard[square];
	}
	
	/***********************************************************************
		Name:		getBlackPawnAttack
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet from BlackPawnAttackBoard
					specified via the parameter
	***********************************************************************/
	public final long getBlackPawnAttack(int square){
		return BlackPawnAttackBoard[square];
	}
	
	/***********************************************************************
		Name:		initKnightMoveBoard
		Parameters:	BitSet
		Returns:	None
		Description:This method initializes the BitSet representing all 
					of the moves a Knight can make
	***********************************************************************/
	private final void initKnightMoveBoard(long[] board){
		int square;
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
	private final  void initBlackPawnAttackBoard(long[] board){
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
	}
	
	/*
     * Method initBlackPawnMoveBoard
     *
     * initializes all black pawn moves for each sqare
     * 
     */
	private final  void initBlackPawnMoveBoard(long[] board){
		int square;
		for(square=55;square>7;square--){
				if(square>=48)
					board[square] |= (long)1<<(square-16);
				board[square] |= (long)1<<(square-8);
		}
	}
	
	/***********************************************************************
		Name:		initWhitePawnMoveBoard
		Parameters:	BitSet[]
		Returns:	None
		Description:This method initializes the WhitePawnBoard. It accounts
					for the first move principle for the pawns.
	***********************************************************************/
	private final void initWhitePawnMoveBoard(long[] board){
		int square;
		for(square=0;square<=55;square++){
			if(square >= 8 && square <=15){
				board[square] |=(long)1<<(square+16);
			}
			board[square] |= (long)1<<(square+8);
		
		}
	}
	
	/***********************************************************************
		Name:		initWhitePawnAttackBoard
		Parameters:	BitSet[]
		Returns:	None
		Description:This method initilaizes the BitSet representing all of
					the possible attacks a white pawn can do
	***********************************************************************/
	private final void initWhitePawnAttackBoard(long[] board){
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
			}
		}
	}
	
	/***********************************************************************
		Name:		initKingBoard
		Parameters:	BitSet[]
		Returns:	None
		Description:This method initializes the BitSet representing the 
					king move board
	***********************************************************************/
	private final void initKingBoard(long[] board){
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
	
	/*
     * Method getKingCastle
     *
     * returns the potential castle moves 
     * 
     * @param int rank - the occupancy of the rank the king is on
     * 
     */
	public final long getKingCastle(int rank) {
		return kingCastleBoard[rank];
	}

    /*
     * Method setKingCastle
     *
     * initializes array of all potential castle moves
     * 
     * @param int rank - the occupancy of the rank the king is on
     * ex// if the file is full (255) there are no castle moves
     * 
     */
	private final void setKingCastle(int file) {
		if(file>=144 && file < 160) {
			kingCastleBoard[file] = (long)1<<6;
		}
		long temp = file&31;
		if(temp==17)
			kingCastleBoard[file] |= (long)1<<2;
	}
	
	/*
     * Method getDiag1Attack
     *
     * calculates all sliding moves along diagonal pointing NW
     * 
     * @param int index1 - the index the diagonal sliding piece is on
     * @param int index2 - the occupancy of the diagonal
     * 
     * @return long - a bitset representing the moves along this diagonal
     * 
     */
	public final long getDiag1Attack(int index1, int index2) {
		
		int DistToLeft = 0;						//used to find spot in index2
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
	
    /*
     * Method getDiag1Attack
     *
     * calculates all sliding moves along diagonal pointing NE
     * 
     * @param int index1 - the index the diagonal sliding piece is on
     * @param int index2 - the occupancy of the diagonal
     * 
     * @return long - a bitset representing the moves along this diagonal
     * 
     */
	public final long getDiag2Attack(int index1, int index2) {
		
		int DistToLeft = 0;						//used to find spot in index2
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

	/*
     * Method getRooksRank2
     *
     * calculates sliding moves along a rank
     * 
     * @param int index1 - the index the rank sliding piece is on
     * @param int index2 - the occupancy of the diagonal
     * 
     * @return long - a bitset representing the moves along the rank
     * 
     */
	public final long getRooksRank2(int index1,int index2) {
	
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
		
	/*
     * Method getRooksFile2
     *
     * calculates sliding moves along a file
     * 
     * @param int index1 - the index the rank sliding piece is on
     * @param int index2 - the occupancy of the diagonal
     * 
     * @return long - a bitset representing the moves along the file
     * 
     */
	public final long getRooksFile2(int index1,int index2) {
	
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
		