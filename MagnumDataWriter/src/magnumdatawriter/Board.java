
package magnumdatawriter;
/**
 * Board.java
 *
 * Version 4.0
 * 
 * Copyright (c) 2012 Eric Stock
 
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


import java.util.Random;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.IOException;


/**
 * Board.java - This class follows the singleton design pattern
 * Represents the chess board and all functionality needed to play chess excluding the search and evaluation
 * This includes gathering all possible moves for each piece
 * making and unmaking moves 
 * loading a fen position
 * starting a new game
 *
 * @version 	3.00 25 Oct 2010
 * @author 	Eric Stock
 */

public final class Board { 
    
   FileOutputStream fileOutputStream;
	DataOutputStream dataOutputStream;

	FileInputStream fileInputStream;
	DataInputStream dataInputStream;

    /** knight moves for each square */
    private static final long[] KnightMoveBoard = new long[64];
    /** white pawn moves for each square */
    private static final long[] WhitePawnMoveBoard	= new long[64];
	/** white pawn attack moves for each sqaure */
    private static final long[] WhitePawnAttackBoard = new long[64];
	/** black pawn moves for each square */
    private static final long[] BlackPawnMoveBoard	= new long[64];
	/** black pawn attack moves for each square */
    private static long[] BlackPawnAttackBoard = new long[64];

	/** these arrays of size 64 are used to generate moves using the rotated bitboard method */
	private static final int Convert[] = new int[]  {7,15,23,31,39,47,55,63,6,14,22,30,38,46,54,62,5,13,21,29,37,45,53,61,
										4,12,20,28,36,44,52,60,3,11,19,27,35,43,51,59,2,10,18,26,34,42,50,58,
										1,9,17,25,33,41,49,57,0,8,16,24,32,40,48,56};						
	
	
	
	private static final int L45Convert[] = new int[] {0,8,1,16,9,2,24,17,10,3,32,25,18,11,4,40,33,26,19,12,5,48,41,34,27,
                                          20,13,6,56,49,42,35,28,21,14,7,57,50,43,36,29,22,15,58,51,44,37,30,
										  23,59,52,45,38,31,60,53,46,39,61,54,47,62,55,63};					
	
	
	private static final int R45Convert[] = new int[] {7,6,15,5,14,23,4,13,22,31,3,12,21,30,39,2,11,20,29,38,47,1,10,19,28,
										  37,46,55,0,9,18,27,36,45,54,63,8,17,26,35,44,53,62,16,25,34,43,52,61,
										  24,33,42,51,60,32,41,50,59,40,49,58,48,57,56};					
														
	private  static final int L45Update[] = new int[64];
	private  static final int R45Update[] = new int[64];
	private  static final int R90Update[] = new int[64];
    
   private static final int ShiftL[] = new int[] {0,1,3,6,10,15,21,28,1,3,6,10,15,21,28,36,3,6,10,15,21,28,36,43,6,
                                      10,15,21,28,36,43,49,10,15,21,28,36,43,49,54,15,21,28,36,43,49,54,58,
                                      21,28,36,43,49,54,58,61,28,36,43,49,54,58,61,63};
	
	
	private static final int ShiftR[]= new int[]  {28,21,15,10,6,3,1,0,36,28,21,15,10,6,3,1,43,36,28,21,15,10,6,3,49,43,
									   36,28,21,15,10,6,54,49,43,36,28,21,15,10,58,54,49,43,36,28,21,15,61,
									   58,54,49,43,36,28,21,63,61,58,54,49,43,36,28};						
	
	
	private static final int ShiftRank[] = new int[] {0,0,0,0,0,0,0,0,8,8,8,8,8,8,8,8,16,16,16,16,16,16,16,16,24,24,24,24,
										 24,24,24,24,32,32,32,32,32,32,32,32,40,40,40,40,40,40,40,40,48,48,48,48,
										 48,48,48,48,56,56,56,56,56,56,56,56};				 	
	
	private static final int ShiftFile[] = new int[] {56,48,40,32,24,16,8,0,56,48,40,32,24,16,8,0,56,48,40,32,24,16,8,0,
										 56,48,40,32,24,16,8,0,56,48,40,32,24,16,8,0,56,48,40,32,24,16,8,0,
										 56,48,40,32,24,16,8,0,56,48,40,32,24,16,8,0};					
    
    /** these arrays are used to mask off and generate moves using magic bitboard move generation */
   private static final int bishopShift[] = new int[64];			//size of move database for each square
	private static final int rookShift[] = new int[64];			//size of move database for each square
   private static final long bMask[] = new long[64];			//bishop occupancy masks
	private static final long rMask[] = new long[64];			//rook occupancy masks
   private static final long bMagics[] = new long[64];			//64 9-bit bishop magic numbers
	private static final long rMagics[] = new long[64];			//64 rook magic numbers
	private static final long bishopTable[][] = new long[64][];	//9 bit bishop table of moves
	private static final long rookTable[][] = new long[64][];		//rook table of moves
   
     /** 64 bit represent the hash code for each position*/
    public long hashValue;

    /** Array of values representing the number of squares a queen would traverse
     * between any two squares on the board
     */
    private static final int[][] queenDist = new int[64][64];
	
	 /** instance of singleton MoveHelper Object used to store move info in a compact form */
    private MoveHelper Helper = MoveHelper.getInstance();

    /** call to private constructor - a la singleton pattern */
    private static final Board INSTANCE = new Board();

	 /**
     * Constructor Board
     * 
     * is private so only 1 instance is created
     * 
     * Variables necessary for move generation are initialized
     */

    private Board(){
		
		initQueenDist();
		
      for(int i=0;i<64;i++) {
			for(int j=0;j<64;j++) {
				if(L45Convert[j]==i) {
					L45Update[i]=j;
				}
			}	
		}
		
		for(int i=0;i<64;i++) {
			for(int j=0;j<64;j++) {
				if(R45Convert[j]==i) {
					R45Update[i]=j;
				}
			}
		}
		
		for(int i=0;i<64;i++) {
			for(int j=0;j<64;j++) {
				if(Convert[j]==i) {
					R90Update[i]=j;
				}
			}
		}	
		
      setBishopMasks();
		setBishopMagics();
		setRookMasks();
		setRookMagics();

		populateRookTables();
		populateBishopTables();
		
		try
		{
			fileOutputStream = new FileOutputStream("initialization.dat");
			dataOutputStream = new DataOutputStream(fileOutputStream);

			for(int i=0; i<64; i++)
			{
				dataOutputStream.writeLong( rMagics[i] );
				dataOutputStream.writeInt( rookShift[i] );
				dataOutputStream.writeLong( bMagics[i] );
				dataOutputStream.writeInt( bishopShift[i] );
				dataOutputStream.writeLong( rMask[i] );
				dataOutputStream.writeLong( bMask[i] );
			}
			for(int i=0; i<64; i++)
			{
				for(int j=0; j<(1<<rookShift[i]); j++ )
				{
					dataOutputStream.writeLong( rookTable[i][j] );
				}
			}
			for(int i=0; i<64; i++)
			{
				for(int j=0; j<(1<<bishopShift[i]); j++ )
				{
					dataOutputStream.writeLong( bishopTable[i][j] );
				}
			}

			for(int i=0; i<64; i++)
			{
				dataOutputStream.writeLong( Helper.getKingPosition(i) );
			}
			for(int i=0; i<256; i++)
			{
				dataOutputStream.writeLong( Helper.getKingCastle(i) );
			}
			InitKnightMoveBoard(KnightMoveBoard);
			InitWhitePawnMoveBoard(WhitePawnMoveBoard);
			InitWhitePawnAttackBoard(WhitePawnAttackBoard);
			InitBlackPawnMoveBoard(BlackPawnMoveBoard);
			InitBlackPawnAttackBoard(BlackPawnAttackBoard);

			for(int i=0; i<64; i++)
			{
				dataOutputStream.writeLong( KnightMoveBoard[i] );
				dataOutputStream.writeLong( WhitePawnMoveBoard[i] );
				dataOutputStream.writeLong( WhitePawnAttackBoard[i] );
				dataOutputStream.writeLong( BlackPawnMoveBoard[i] );
				dataOutputStream.writeLong( BlackPawnAttackBoard[i] );
			}
			
			setHash();

			dataOutputStream.close();
		}
		catch( IOException iox )
		{
			System.out.println("unable to open file stream");
		}
	 }

    /** 
     *  method getInstance
     * 
     * returns only instance of Board Class
     */
    public static Board getInstance(){
        return INSTANCE;
    }
   
    /**
     *  method initQueenDist
     * 
     * initializes arrays containing distance between two squares 
     */
	private  final void initQueenDist() {
		for(int i=0;i<64;i++) {
			for(int j=0;j<64;j++) {
				if((i%8<j%8)||(i%8==j%8)||(i/8==j/8))	{
					queenDist[i][j] = getQueenDistance(i,j);
					queenDist[j][i] = queenDist[i][j];
				}					
			}		
		}						 
	}
  
    /** 
     *  method getQueenDistance
     * 
     * Calculates and then returns the distance between two squres
     * @param qPos 
     * @param kPos 
     * 
     */
	private  final int getQueenDistance(int qPos,int kPos) {
		int qRank = qPos/8;
		int kRank = kPos/8;
		int qFile = qPos%8;
		int kFile = kPos%8;
		int fileDiff = Math.abs(qFile-kFile);
		int rankDiff = Math.abs(qRank-kRank);
		int Distance=0;
		if(qRank == kRank) 
			Distance = fileDiff;
		else if(qFile == kFile) 
			Distance = rankDiff;
		else if(kFile>qFile) {
			if(kRank>qRank)				{			//traverse D2
				if(fileDiff<rankDiff)   	{		//we go until we hit king's file
					int pos = qPos;
					while(pos%8!=kFile) {
						pos+=9;
						Distance++;
					}
					Distance+= Math.abs(pos/8-kRank);
				}
				else {							//we go until we hit the king's rank
					int pos = qPos;
					while(pos/8!=kRank) {
						pos+=9;
						Distance++;
					}
					Distance += Math.abs(pos-kPos);
				}
			} else {								//king Rank less than queen rank	//traverse D1
				if(fileDiff<rankDiff) {				//we go until we hit Kings file
					int pos = qPos;
					while(pos%8!=kFile) {
						pos-=7;
						Distance++;
					}
					Distance += Math.abs(pos/8-kRank);
				}
				else {							//we go until we hit king's rank
					int pos = qPos;		
					while(pos/8!=kRank) 	{
						pos-=7;
						Distance++;
					}
					Distance += Math.abs(pos-kPos);
				}
			}	
		}	
		return Distance;
	}
	
    /** 
     *  method setHash()
     * 
     * fills the various hash values for each square, castling and passant
     * 
     */
    private  final void setHash() {
		Random rand;
		int i, j;							//counters 

		try
		{
			rand = new Random(80392848);
			for(i=0;i<64;i++) {
				for(j=0;j<12;j++) {
					dataOutputStream.writeLong(rand.nextLong() & Long.MAX_VALUE);
				}
			}

			/**set castle hashes and enPassant hashes */
			for(i=0;i<9;i++) {
				dataOutputStream.writeLong(rand.nextLong() & Long.MAX_VALUE);
				dataOutputStream.writeLong(rand.nextLong() & Long.MAX_VALUE);
			}

			dataOutputStream.writeLong(rand.nextLong() & Long.MAX_VALUE);

			for(i=0;i<8;i++) {
				dataOutputStream.writeLong(rand.nextLong() & Long.MAX_VALUE);
				dataOutputStream.writeLong(rand.nextLong() & Long.MAX_VALUE);
			}
		}
		catch( Exception e) { System.out.println("exception caught and nothing done");};
	}				
	
	/** 
     *  method setBishopMagics
     * 
     * This method hard codes in the 64 magic numbers for bishops
     * 
     */
    private  final void setBishopMagics() {
		bMagics[0] = 0x0002020202020200L;
		bishopShift[0] = 6;
		bMagics[8] = 0x0000040404040400L;
		bishopShift[8] = 5;
		bMagics[16] = 0x0004000808080800L;
		bishopShift[16] = 5;
		bMagics[24] = 0x0002080010101000L;
		bishopShift[24] = 5;
		bMagics[32] = 0x0001041000202000L;
		bishopShift[32] = 5;
		bMagics[40] = 0x0000820820004000L;
		bishopShift[40] = 5;
		bMagics[48] = 0x0000410410400000L;
		bishopShift[48] = 5;
		bMagics[56] = 0x0000104104104000L;
		bishopShift[56] = 6;
		bMagics[1] = 0x0002020202020000L;
		bishopShift[1] = 5;
		bMagics[9] = 0x0000020202020200L;
		bishopShift[9] = 5;
		bMagics[17] = 0x0002000404040400L;
		bishopShift[17] = 5;
		bMagics[25] = 0x0001040008080800L;
		bishopShift[25] = 5;
		bMagics[33] = 0x0000820800101000L;
		bishopShift[33] = 5;
		bMagics[41] = 0x0000410410002000L;
		bishopShift[41] = 5;
		bMagics[49] = 0x0000208208200000L;
		bishopShift[49] = 5;
		bMagics[57] = 0x0000002082082000L;
		bishopShift[57] = 5;
		bMagics[2] = 0x0004010202000000L;
		bishopShift[2] = 5;
		bMagics[10] = 0x0000040102020000L;
		bishopShift[10] = 5;
		bMagics[18] = 0x0001000202020200L;
		bishopShift[18] = 7;
		bMagics[26] = 0x0000208004010400L;
		bishopShift[26] = 7;
		bMagics[34] = 0x0000104400080800L;
		bishopShift[34] = 7;
		bMagics[42] = 0x0000082088001000L;
		bishopShift[42] = 7;
		bMagics[50] = 0x0000002084100000L;
		bishopShift[50] = 5;
		bMagics[58] = 0x0000000020841000L;
		bishopShift[58] = 5;
		bMagics[3] = 0x0004040080000000L;
		bishopShift[3] = 5;
		bMagics[11] = 0x0000040400800000L;
		bishopShift[11] = 5;
		bMagics[19] = 0x0000800802004000L;
		bishopShift[19] = 7;
		bMagics[27] = 0x0000404004010200L;
		bishopShift[27] = 9;
		bMagics[35] = 0x0000020080080080L;
		bishopShift[35] = 9;
		bMagics[43] = 0x0000002011000800L;
		bishopShift[43] = 7;
		bMagics[51] = 0x0000000020880000L;
		bishopShift[51] = 5;
		bMagics[59] = 0x0000000000208800L;
		bishopShift[59] = 5;
		bMagics[4] = 0x0001104000000000L;
		bishopShift[4] = 5;
		bMagics[12] = 0x0000011040000000L;
		bishopShift[12] = 5;
		bMagics[20] = 0x0000800400A00000L;
		bishopShift[20] = 7;
		bMagics[28] = 0x0000840000802000L;
		bishopShift[28] = 9;
		bMagics[36] = 0x0000404040040100L;
		bishopShift[36] = 9;
		bMagics[44] = 0x0000080100400400L;
		bishopShift[44] = 7;
		bMagics[52] = 0x0000001002020000L;
		bishopShift[52] = 5;
		bMagics[60] = 0x0000000010020200L;
		bishopShift[60] = 5;
		bMagics[5] = 0x0000821040000000L;
		bishopShift[5] = 5;
		bMagics[13] = 0x0000008210400000L;
		bishopShift[13] = 5;
		bMagics[21] = 0x0000200100884000L;
		bishopShift[21] = 7;
		bMagics[29] = 0x0000404002011000L;
		bishopShift[29] = 7;
		bMagics[37] = 0x0000808100020100L;
		bishopShift[37] = 7;
		bMagics[45] = 0x0001010101000200L;
		bishopShift[45] = 7;
		bMagics[53] = 0x0000040408020000L;
		bishopShift[53] = 5;
		bMagics[61] = 0x0000000404080200L;
		bishopShift[61] = 5;
		bMagics[6] = 0x0000410410400000L;
		bishopShift[6] = 5;
		bMagics[14] = 0x0000004104104000L;
		bishopShift[14] = 5;
		bMagics[22] = 0x0000400082082000L;
		bishopShift[22] = 5;
		bMagics[30] = 0x0000808001041000L;
		bishopShift[30] = 5;
		bMagics[38] = 0x0001010100020800L;
		bishopShift[38] = 5;
		bMagics[46] = 0x0002020202000400L;
		bishopShift[46] = 5;
		bMagics[54] = 0x0004040404040000L;
		bishopShift[54] = 5;
		bMagics[62] = 0x0000040404040400L;
		bishopShift[62] = 5;
		bMagics[7] = 0x0000104104104000L;
		bishopShift[7] = 6;
		bMagics[15] = 0x0000002082082000L;
		bishopShift[15] = 5;
		bMagics[23] = 0x0000200041041000L;
		bishopShift[23] = 5;
		bMagics[31] = 0x0000404000820800L;
		bishopShift[31] = 5;
		bMagics[39] = 0x0000808080010400L;
		bishopShift[39] = 5;
		bMagics[47] = 0x0001010101000200L;
		bishopShift[47] = 5;
		bMagics[55] = 0x0002020202020000L;
		bishopShift[55] = 5;
		bMagics[63] = 0x0002020202020200L;
		bishopShift[63] = 6;
		
		for(int i=0; i<64; i++) {
			bishopTable[i] = new long[1<<bishopShift[i]];
		}
	};
	
    /** 
     *  method setRookMagics
     * 
     * This method hard codes in the 64 magic numbers for rooks
     * 
     */
    private  final void setRookMagics() {
		rMagics[0] = 0x0080001020400080L;	
		rookShift[0] = 12;
		rMagics[8] = 0x0000800020400080L;
		rookShift[8] = 11;
		rMagics[16] = 0x0000208000400080L;
		rookShift[16] = 11;
		rMagics[24] = 0x0000208080004000L;
		rookShift[24] = 11;
		rMagics[32] = 0x0000204000800080L;
		rookShift[32] = 11;
		rMagics[40] = 0x0000204000808000L;
		rookShift[40] = 11;
		rMagics[48] = 0x0000204000800080L;
		rookShift[48] = 11;
		rMagics[56] = 0x0000102040800101L;
		rookShift[56] = 12;
		rMagics[1] = 0x0040001000200040L;
		rookShift[1] = 11;
		rMagics[9] = 0x0000400020005000L;
		rookShift[9] = 10;
		rMagics[17] = 0x0000404000201000L;
		rookShift[17] = 10;
		rMagics[25] = 0x0000200040005000L;
		rookShift[25] = 10;
		rMagics[33] = 0x0000200040401000L;
		rookShift[33] = 10;
		rMagics[41] = 0x0000200040008080L;
		rookShift[41] = 10;
		rMagics[49] = 0x0000200040008080L;
		rookShift[49] = 10;
		rMagics[57] = 0x0000102040008101L;
		rookShift[57] = 11;
		rMagics[2] = 0x0080081000200080L;
		rookShift[2] = 11;
		rMagics[10] = 0x0000801000200080L;
		rookShift[10] = 10;
		rMagics[18] = 0x0000808010002000L;
		rookShift[18] = 10;
		rMagics[26] = 0x0000100080200080L;
		rookShift[26] = 10;
		rMagics[34] = 0x0000100080802000L;
		rookShift[34] = 10;
		rMagics[42] = 0x0000100020008080L;
		rookShift[42] = 10;
		rMagics[50] = 0x0000100020008080L;
		rookShift[50] = 10;
		rMagics[58] = 0x0000081020004101L;
		rookShift[58] = 11;
		rMagics[3] = 0x0080040800100080L;
		rookShift[3] = 11;
		rMagics[11] = 0x0000800800100080L;
		rookShift[11] = 10;
		rMagics[19] = 0x0000808008001000L;
		rookShift[19] = 10;
		rMagics[27] = 0x0000080080100080L;
		rookShift[27] = 10;
		rMagics[35] = 0x0000080080801000L;
		rookShift[35] = 10;
		rMagics[43] = 0x0000080010008080L;
		rookShift[43] = 10;
		rMagics[51] = 0x0000080010008080L;
		rookShift[51] = 10;
		rMagics[59] = 0x0000040810002101L;
		rookShift[59] = 11;
		rMagics[4] = 0x0080020400080080L;
		rookShift[4] = 11;
		rMagics[12] = 0x0000800400080080L;
		rookShift[12] = 10;
		rMagics[20] = 0x0000808004000800L;
		rookShift[20] = 10;
		rMagics[28] = 0x0000040080080080L;
		rookShift[28] = 10;
		rMagics[36] = 0x0000040080800800L;
		rookShift[36] = 10;
		rMagics[44] = 0x0000040008008080L;
		rookShift[44] = 10;
		rMagics[52] = 0x0000040008008080L;
		rookShift[52] = 10;
		rMagics[60] = 0x0001000204080011L;
		rookShift[60] = 11;
		rMagics[5] = 0x0080010200040080L;
		rookShift[5] = 11;
		rMagics[13] = 0x0000800200040080L;
		rookShift[13] = 10;
		rMagics[21] = 0x0000808002000400L;
		rookShift[21] = 10;
		rMagics[29] = 0x0000020080040080L;
		rookShift[29] = 10;
		rMagics[37] = 0x0000020080800400L;
		rookShift[37] = 10;
		rMagics[45] = 0x0000020004008080L;
		rookShift[45] = 10;
		rMagics[53] = 0x0000020004008080L;
		rookShift[53] = 10;
		rMagics[61] = 0x0001000204000801L;
		rookShift[61] = 11;
		rMagics[6] = 0x0080008001000200L;
		rookShift[6] = 11;
		rMagics[14] = 0x0000800100020080L;
		rookShift[14] = 10;
		rMagics[22] = 0x0000010100020004L;
		rookShift[22] = 10;
		rMagics[30] = 0x0000010080800200L;
		rookShift[30] = 10;
		rMagics[38] = 0x0000020001010004L;
		rookShift[38] = 10;
		rMagics[46] = 0x0000010002008080L;
		rookShift[46] = 10;
		rMagics[54] = 0x0000800100020080L;
		rookShift[54] = 10;
		rMagics[62] = 0x0001000082000401L;
		rookShift[62] = 11;
		rMagics[7] = 0x0080002040800100L;
		rookShift[7] = 12;
		rMagics[15] = 0x0000800040800100L;
		rookShift[15] = 11;
		rMagics[23] = 0x0000020000408104L;
		rookShift[23] = 11;
		rMagics[31] = 0x0000800080004100L;
		rookShift[31] = 11;
		rMagics[39] = 0x0000800040800100L;
		rookShift[39] = 11;
		rMagics[47] = 0x0000004081020004L;
		rookShift[47] = 11;
		rMagics[55] = 0x0000800041000080L;
		rookShift[55] = 11;
		rMagics[63] = 0x0000002040810402L;
		rookShift[63] = 12;
	
		for(int i=0; i<64; i++) {
			rookTable[i] = new long[1<<rookShift[i]];
		}
	}
		
	/***********************************************************************
		Name:		setBishopMasks
		Parameters:	None
		Returns:	None
		Description:This method sets the masks of all possible squares to consider
		when calculating the bishop moves
	***********************************************************************/	
    private  final void setBishopMasks() {
		for(int i=0; i<64; i++) {
			bMask[i] = getConventionalBishopMoves(i,0) & 0x007e7e7e7e7e7e00L;
		}
	}	
	/***********************************************************************
		Name:		setRookMasks
		Parameters:	None
		Returns:	None
		Description:This method sets the masks of all possible squares to consider
		when calculating the rook moves
	***********************************************************************/
	private  final void setRookMasks() {
		for(int i=0; i<64; i++) {
			rMask[i] = getConventionalRookMoves(i,0); 
			if(i%8 < 7)
				rMask[i] &= 0x7f7f7f7f7f7f7f7fL;
			if(i%8 > 0)
				rMask[i] &= 0xfefefefefefefefeL;
			if(i/8 > 0)
				rMask[i] &= 0xffffffffffffff00L;
			if(i/8 < 7)
				rMask[i] &= 0x00ffffffffffffffL;	
		}
	}
	
	/***********************************************************************
		Name:		populateBishopTables
		Parameters:	None
		Returns:	None
		Description:This method calculates the magic hash table index and places the 
		corresponding attack long in the array at the calculated index
	***********************************************************************/
	private  final void populateBishopTables() {
		int index;
		long occ;
		long attacks;
		long oset;

		for(int sq = 0; sq < 64; sq++) {
			occ = 0;
			oset = getConventionalBishopMoves(sq,occ)& 0x007e7e7e7e7e7e00L;
			do {
				index = (int)(((occ*bMagics[sq])>>>(64-bishopShift[sq])));
				attacks = getConventionalBishopMoves(sq,occ);
				bishopTable[sq][index] = attacks;
				occ = (occ - oset) & oset;				//move to next subset
			} while(occ != 0);
		}
	}		
	/***********************************************************************
		Name:		populateRookTables
		Parameters:	None
		Returns:	None
		Description:This method calculates the magic hash table index and places the 
		corresponding attack long in the array at the calculated index
	***********************************************************************/
	public  final void populateRookTables() {
		int index;
		long occ;
		long attacks;
		long oset;
		
		for(int sq = 0; sq < 64; sq++) {
			occ = 0;
			oset = rMask[sq];
			do {
				index = (int)(((occ*rMagics[sq])>>>(64-rookShift[sq])));
				attacks = getConventionalRookMoves(sq,occ);
				rookTable[sq][index] = attacks;
				occ = (occ - oset) & oset;
			} while(occ != 0);
		}
	}		

	
	/***********************************************************************		
		Name:		rotate45R
		Parameters:	BitSet
		Returns:	BitSet
		Description:This method returns an BitSet representing the board 
					parameter rotated 45 degrees to the right
	***********************************************************************/	
	private  final long rotate45R(long board) {
			int index;
			long bit;
			long rotate = 0;
			for(int i=0;i<64;i++) {
				
				index = R45Convert[i];
				bit = (long)1 << index;
				bit &= board;
				
				if(((bit>>index)&1)==1) {
					rotate += (long)1 << i;
				}
			}	
		return rotate;
	}

	/***********************************************************************		
		Name:		rotate45L
		Parameters:	BitSet
		Returns:	BitSet
		Description:This method returns an BitSet representing the board 
					parameter rotated 45 degrees to the left
	***********************************************************************/	
	private  final long rotate45L(long board) {
			int index;
			long rotate;
			long bit;
			rotate = 0;
			for(int i=0;i<64;i++) {
				index = L45Convert[i];
				bit = (long)1 << index;
				bit &= board;
				
				if(((bit>>index)&1)==1) {
					rotate+=(long)1<<i;
				}
			}
		return rotate;
	}
		
	
	/***********************************************************************		
		Name:		rotate90R
		Parameters:	BitSet
		Returns:	BitSet
		Description:This method returns an BitSet representing the board 
					parameter rotated 90 degrees to the right
	***********************************************************************/	
	private  final long rotate90R(long board) {
			long bit;
			long rotate = 0;
			for(int i=0;i<64;i++) {
				bit = (long)1 << i;
				bit &=board;
				if(((bit>>i)&1)==1) {
					rotate += (long)1 << R90Update[i];
				}
			}
	return rotate;
	}
	 
   
   /***********************************************************************
		Name:		initBlackPawnAttackBoard
		Parameters:	BitSet[]
		Returns:	None
		Description:This method initializes the BitSet representing all of
					the possible attacks a Black pawn can dp
	***********************************************************************/
	private final void InitBlackPawnAttackBoard(long[] board){
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
	private final void InitBlackPawnMoveBoard(long[] board){
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
	private final void InitWhitePawnMoveBoard(long[] board){
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
	private final void InitWhitePawnAttackBoard(long[] board){
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
		Name:		initKnightMoveBoard
		Parameters:	BitSet
		Returns:	None
		Description:This method initializes the BitSet representing all
					of the moves a Knight can make
	***********************************************************************/
	private final void InitKnightMoveBoard(long[] board){
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
		Name:		getConventionalRookMoves
		Parameters:	int,long
		Returns:	BitSet
		Description:This method returns a bitset representing the attackable rook squares 
		given the occupancy
	***********************************************************************/	
	private  final long getConventionalRookMoves(int index, long occ) {
		long temp = 0;
		long sRank = occ>>ShiftRank[index];
		int Decimal = (int)(sRank&255);
		temp = Helper.getRooksRank2(index,Decimal);
		sRank = rotate90R(occ)>>ShiftFile[index];
		Decimal = (int)(sRank&255);
		temp |= Helper.getRooksFile2(index,Decimal);
		return temp;
		
	}

	/***********************************************************************		
		Name:		getConventionalBishopMoves
		Parameters:	int, long
		Returns:	long
		Description:This method returns a BitSet representing all of the 
					BishopMoves moves from square int, given a specified occupancy
	***********************************************************************/
	private  final long getConventionalBishopMoves(int index, long occ) {
		
		long right45 = rotate45R(occ);
		long left45 = rotate45L(occ);
		
		int temp45 = (int)(left45>>>ShiftL[index]);
		long tempD = Helper.getDiag1Attack(index,temp45&255);
		
		temp45 = (int)(right45>>>ShiftR[index]);
		tempD |= Helper.getDiag2Attack(index,temp45&255);
		
		return tempD;
		
	}
}