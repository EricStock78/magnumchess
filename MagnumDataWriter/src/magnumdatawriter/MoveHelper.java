package magnumdatawriter;

/**
 * MoveHelper.java
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

public final class MoveHelper{	
	
    /** king moves for each square */
	private static final long[] KingMoveBoard = new long[64];
   private static final long[] kingCastleBoard = new long[256];
	
   /** only instance of class due to singleton pattern */
   private static final MoveHelper INSTANCE = new MoveHelper();	
    
	/*
     * MoveHelper Constructor
     *
     * calls methods to fill arrays with needed values for move generation
     */
    private MoveHelper() {
		initKingBoard(KingMoveBoard);
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
		//index2 &= ((1<<Global.Diag1Length[index1])-1);
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
		//index2 &= ((1<<Global.Diag2Length[index1])-1);
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
		