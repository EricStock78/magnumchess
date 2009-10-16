/**********************************************************************************
	Cosc 3P71:		Final Project
	Authored by: 	Eric Stock
					Joseph Troop
	Name:			Board.java
**********************************************************************************/
//import java.io.*;
import java.lang.Integer;
import java.io.File;
import java.io.IOException;
import java.util.Stack;
import java.util.Vector;
import java.util.Random;
/***********************************************************************
	Name:		Board
	
	Parameters:	None
	Returns:	None
	Description:This is the main storage and processing class for the 
				entire project. 
***********************************************************************/
public final class Board extends java.util.Observable{
	private int size = 64;	
	private static final Chess Position = new Chess();
	private static long Board45L;				//rotated 45L board
	private static long Board45R;				//rotated 45R boardw
	private static long Board90R;
	public static long bitboard;
	public static long whitepieces;	//BitSets for all chess pieces
	public static long blackpieces;
	public static long whitepawns;
	public static long blackpawns;
	public static long whiteknights;
	public static long blackknights;
	public static long whitebishops;
	public static long blackbishops;
	public static long whiterooks;
	public static long blackrooks;
	public static long whitequeen;
	public static long blackqueen;
	public static long whiteking;
	public static long blackking;
	public static long pawnsKings;
	public static long slidePieces;				
	public static boolean wleftC, wrightC;			//castle flags
	public static boolean bleftC,brightC;				//castle flags
	public static int bCastle, wCastle;
	//private static boolean wHasCastled;
	//private static boolean bHasCastled;
	
	private static long bMask[] = new long[64];			//bishop occupancy masks
	private static long rMask[] = new long[64];			//rook occupancy masks 
	private static long bMagics[] = new long[64];			//64 9-bit bishop magic numbers
	private static long rMagics[] = new long[64];			//64 rook magic numbers					
	private static long bishopTable[][] = new long[64][];	//9 bit bishop table of moves
	private static long rookTable[][] = new long[64][];		//rook table of moves
	private static int bishopShift[] = new int[64];			//size of move database for each square
	private static int rookShift[] = new int[64];			//size of move database for each square
	
	private static int flagHistory[];
	public static int boardMoves[] = new int[512];
	private static final int binLookup[] = new int[] 	{1,2,4,8,16,32,64,128};			//array to lookup bit to decimal vals
	
	public static int piece_in_square[];
	private static final int Convert[] = new int[]  {7,15,23,31,39,47,55,63,6,14,22,30,38,46,54,62,5,13,21,29,37,45,53,61,
													4,12,20,28,36,44,52,60,3,11,19,27,35,43,51,59,2,10,18,26,34,42,50,58,
													1,9,17,25,33,41,49,57,0,8,16,24,32,40,48,56};						
	
	//array used to rotate 90R
	
	
	
	private static final int LConvert[] = new int[]  {56,48,40,32,24,16,8,0,57,49,41,33,25,17,9,1,58,50,42,34,26,18,10,2,
								   					59,51,43,35,27,19,11,3,60,52,44,36,28,20,12,4,61,53,45,37,29,21,13,
								   					5,62,54,46,38,30,22,14,6,63,55,47,39,31,23,15,7};
	
	
	private static final int L45Convert[] = new int[] {0,8,1,16,9,2,24,17,10,3,32,25,18,11,4,40,33,26,19,12,5,48,41,34,27,
														20,13,6,56,49,42,35,28,21,14,7,57,50,43,36,29,22,15,58,51,44,37,30,
														23,59,52,45,38,31,60,53,46,39,61,54,47,62,55,63};					
	
	
	private static final int R45Convert[] = new int[] {7,6,15,5,14,23,4,13,22,31,3,12,21,30,39,2,11,20,29,38,47,1,10,19,28,
														37,46,55,0,9,18,27,36,45,54,63,8,17,26,35,44,53,62,16,25,34,43,52,61,
														24,33,42,51,60,32,41,50,59,40,49,58,48,57,56};					
														//array used to rotate 45R
	
	
	
	
	private static int L45Update[];					//arrays indicating which index to change
	
	
	
	
	
	private static int R45Update[];
	private static int R90Update[];				
	
	
	private static final int ShiftL[] = new int[] 		{0,1,3,6,10,15,21,28,1,3,6,10,15,21,28,36,3,6,10,15,21,28,36,43,6,
														10,15,21,28,36,43,49,10,15,21,28,36,43,49,54,15,21,28,36,43,49,54,58,
														21,28,36,43,49,54,58,61,28,36,43,49,54,58,61,63};
	
	
	private static final int ShiftR[]= new int[]		{28,21,15,10,6,3,1,0,36,28,21,15,10,6,3,1,43,36,28,21,15,10,6,3,49,43,
														 36,28,21,15,10,6,54,49,43,36,28,21,15,10,58,54,49,43,36,28,21,15,61,
														 58,54,49,43,36,28,21,63,61,58,54,49,43,36,28};						
	
	
	private static final int ShiftRank[] = new int[] {0,0,0,0,0,0,0,0,8,8,8,8,8,8,8,8,16,16,16,16,16,16,16,16,24,24,24,24,
													24,24,24,24,32,32,32,32,32,32,32,32,40,40,40,40,40,40,40,40,48,48,48,48,
													48,48,48,48,56,56,56,56,56,56,56,56};				 	
	//array used to shift rook rank board;
	
	
	
	private static final int ShiftFile[] = new int[] {56,48,40,32,24,16,8,0,56,48,40,32,24,16,8,0,56,48,40,32,24,16,8,0,
													56,48,40,32,24,16,8,0,56,48,40,32,24,16,8,0,56,48,40,32,24,16,8,0,
													56,48,40,32,24,16,8,0,56,48,40,32,24,16,8,0};					
		//array used to shift rook file board;
	
	
	
	private static final int Diag1Index[] = new int[] 		{0,8,16,24,32,40,48,56,8,16,24,32,40,48,56,57
															,16,24,32,40,48,56,57,58,
															24,32,40,48,56,57,58,59,
															32,40,48,56,57,58,59,60,
															40,48,56,57,58,59,60,61,
															48,56,57,58,59,60,61,62,
															56,57,58,59,60,61,62,63};
	
	
	private static final int Diag1Index2[] = new int[]		{0,1,2,3,4,5,6,7,
															1,2,3,4,5,6,7,15,
															2,3,4,5,6,7,15,23,
															3,4,5,6,7,15,23,31,
															4,5,6,7,15,23,31,39,
															5,6,7,15,23,31,39,47,
															6,7,15,23,31,39,47,55,
															7,15,23,31,39,47,55,63};
															
	private static final int Diag2Index[] = new int[]		{63,55,47,39,31,23,15,7,
															62,63,55,47,39,31,23,15,
															61,62,63,55,47,39,31,23,
															60,61,62,63,55,47,39,31,
															59,60,61,62,63,55,47,39,
															58,59,60,61,62,63,55,47,
															57,58,59,60,61,62,63,55,
															56,57,58,59,60,61,62,63};
	
	
	private static final int Diag2Index2[] = new int[]		{0,1,2,3,4,5,6,7,
															8,0,1,2,3,4,5,6,
															16,8,0,1,2,3,4,5,
															24,16,8,0,1,2,3,4,
															32,24,16,8,0,1,2,3,
															40,32,24,16,8,0,1,2,
															48,40,32,24,16,8,0,1,
															56,48,40,32,24,16,8,0};
	
	
	
	
	
	private static final int outerIndex[] = new int[]			{1,1,1,1,1,1,1,1,
															 1,0,0,0,0,0,0,1,
															 1,0,0,0,0,0,0,1,
															 1,0,0,0,0,0,0,1,
															 1,0,0,0,0,0,0,1,
															 1,0,0,0,0,0,0,1,
															 1,0,0,0,0,0,0,1,
															 1,1,1,1,1,1,1,1};
	
	private static final int knightVals[] = new int[]	{-2,-2,-2,-2,-2,-2,-2,-2,
														 -2,1,1,1,1,1,1,-2,
														 -2,1,4,4,4,4,1,-2,
														 -2,1,4,8,8,4,1,-2,
														 -2,1,4,8,8,4,1,-2,
														 -2,1,4,4,4,4,1,-2,
														 -2,1,1,1,1,1,1,-2,
														 -2,-2,-2,-2,-2,-2,-2,-2};
														  	
		
		
	private static final int[] init = 	{0,1,2,3,4,2,1,0,
									   5,5,5,5,5,5,5,5,
									   -1,-1,-1,-1,-1,-1,-1,-1,
									   -1,-1,-1,-1,-1,-1,-1,-1,
									   -1,-1,-1,-1,-1,-1,-1,-1,
									   -1,-1,-1,-1,-1,-1,-1,-1,
									   11,11,11,11,11,11,11,11,
									   6,7,8,9,10,8,7,6};	
		
							
	//public static final long[] Global.Global.Global.set_Mask = new long[64]; 
	
	
	
	private static final int[] attackVals =  {0,15,30,50,80,110,150,150,150,150,200,250,250,250,250,250,250};		//values for number of attackers of king
		
	private static long powOf2[];
	public static int value;							//value of board
	
	public static long attack1[];
	public static long attack2[];
	private static int passantW, passantB;			//passant squares
	
	
	public static int moveCount;						//count of all moves made on board
	private static boolean gameOn;						//flag to see if game is over
	private static int drawCount;						//50 move count variable
	private static int turn;							//-1 white moves, 1 black moves
	private static int[][] pHash, pHash2;					//hash values for all 12 pieces on all 64 squares
	private static int wCastleHash[], wCastleHash2[]; 
	private static int bCastleHash[], bCastleHash2[];
	private static int bHashMove, bHashMove2;						//hash for when black is to move
	//private static long bHashLeft,bHashRight,wHashLeft,wHashRight;		//castling rights hashes
	private static int[] passantHashW, passantHashW2;				//hash for passant squares
	private static int[] passantHashB, passantHashB2;
	private static int[] hashHistory, hashHistory2;
	public static int hashValue, hashValue2;					//the hash value of the board
	private static int pawnHash, pawnHash2;	
	private static RepetitionTable repTable;
	//private static Position[] repetition;
	
	public static int totalValue;
	private static HistoryWriter writer;					//stores and writes the history of all moves made
	public static int wPieceNo;					//number of white pieces
	public static int bPieceNo;
	
	private final static int[][] queenDist = new int[64][64];
	
	private static int[] index32 = new int[32];
	private static final int debruijn = 0x077CB531;
	
	public Board() throws IOException {
		for(int i=0;i<32;i++) {
			index32[(debruijn<<i)>>>27] = i;
		}	
		//bMagics = new long[64];
		
		piece_in_square = new int[64];
		attack1 = new long[64];
		attack2 = new long[64];
		powOf2 = new long[64];
		//prevMoves = new Stack<Move>();
		writer = new HistoryWriter(this);
		initQueenDist();
		pHash = new int[64][12];
		pHash2 = new int[64][12];
		bCastleHash = new int[8];
		bCastleHash2 = new int[8];
		wCastleHash = new int[8];
		wCastleHash2 = new int[8];
		passantHashW = new int[8];
		passantHashW2 = new int[8];
		passantHashB = new int[8];
		passantHashB2 = new int[8];
		
		setHash();
		for(int x=0;x<63;x++) {
			powOf2[x] = (long)Math.pow(2,x);
		}
		powOf2[63] = (long)Math.pow(-2,63);
		Global.diag1Masks = new long[15];
		for(int i=0;i<15;i++) 
			Global.diag1Masks[i] = 0;
		for(int j=0;j<64;j++) {
			int temp = Global.Diag1Groups[j];
			Global.diag1Masks[temp] |=(long)1<<j;
		}
	
		Global.diag2Masks = new long[15];
		for(int i=0;i<15;i++) 
			Global.diag2Masks[i] = 0;
		for(int j=0;j<64;j++) {
			int temp = Global.Diag2Groups[j];
			Global.diag2Masks[temp] |= (long)1<<j;
		}	
		//for(int i=0;i<15;i++)
		//	System.out.println("diag 2 at "+i+" is "+Global.diag2Masks[i]);
		for(int i=0;i<64;i++) {
			Global.set_Mask[i] = (long)1<<i;
		}	
		
		//Global.fileMasks = new long[8];
		for(int i=0;i<8;i++) {
			Global.fileMasks[i] = 0;
			for(int j=0;j<64;j++) {
				if(j%8==i)
					Global.fileMasks[i] |= (long)1<<j;
			}
			
		
		}
		//Global.rankMasks = new long[8];
		for(int i=0;i<8;i++) {
			Global.rankMasks[i]=0;
			for(int j=0;j<64;j++) {
				if(j/8==i) {
					Global.rankMasks[i] |= (long)1<<j;
			}
			}
			
		
		}
		Global.plus9 = new long[64];
		for (int i = 0; i < 64; i++) {
			Global.plus9[i] = 0;
			for(int j = i + 9; j < 64; j += 9)	{
				if(j % 8 == 0) break;
				Global.plus9[i] |= Global.set_Mask[j];
			}
		}
		Global.minus9 = new long[64];
		for(int i = 0; i < 64; i++) {
			Global.minus9[i] = 0;
			for(int j = i - 9; j >= 0; j -= 9) {
				if(j % 8 == 7) break;
				Global.minus9[i] |= Global.set_Mask[j];
			}
		}
		
		Global.plus7 = new long[64];
		for( int i = 0; i < 64; i++) {
			Global.plus7[i] = 0;
			for(int j = i + 7; j < 64; j += 7) {
				if(j % 8 == 7) break;
				Global.plus7[i] |= Global.set_Mask[j];
			}	
		}
		
		Global.minus7 = new long[64];
		for(int i = 0; i < 64; i++) {
			Global.minus7[i] = 0;
			for(int j = i - 7; j >= 0; j -= 7) {
				if(j % 8 == 0) break;
				Global.minus7[i] |= Global.set_Mask[j];
			}
		}
		
		Global.plus8 = new long[64];
		for( int i = 0; i < 64; i++) {
			Global.plus8[i] = 0;
			for(int j = i + 8; j < 64; j += 8) {
				Global.plus8[i] |= Global.set_Mask[j];
			}
		}
		Global.minus8 = new long[64];
		for (int i = 0; i < 64; i++) {
			Global.minus8[i] = 0;
			for(int j = i - 8; j >= 0; j -= 8) {
				Global.minus8[i] |= Global.set_Mask[j];
			}
		}
		
		Global.plus1 = new long[64];
		for( int i = 0; i < 64; i++) {
			Global.plus1[i] = 0;
			for(int j = i+1; j < 64; j++) {
				if(j%8 == 0) break;
				Global.plus1[i] |= Global.set_Mask[j];
			}	
		}
		
		Global.minus1 = new long[64];
		for( int i = 0; i < 64; i++) {
			Global.minus1[i] = 0;
			for(int j = i - 1; j >= 0; j -= 1) {
				if(j % 8 == 7) break;
				Global.minus1[i] |= Global.set_Mask[j];
			}
		}
		
		L45Update = new int[64];
		for(int i=0;i<64;i++) {
			for(int j=0;j<64;j++) {
				if(L45Convert[j]==i) {
					L45Update[i]=j;
				}
			}	
		}
		R45Update = new int[64];
		for(int i=0;i<64;i++) {
			for(int j=0;j<64;j++) {
				if(R45Convert[j]==i) {
					R45Update[i]=j;
				}
			}
		}
		R90Update = new int[64];
		for(int i=0;i<64;i++) {
			for(int j=0;j<64;j++) {
				if(Convert[j]==i) {
					R90Update[i]=j;
				}
			}
		}	
		Global.wKingMask = new long[8];	
		Global.bKingMask = new long[8];
		for(int i = 0; i < 8; i++) {
			if(i%8 == 0) {
				Global.wKingMask[i] = Global.set_Mask[i+8] | Global.set_Mask[i+9] | Global.set_Mask[i+10] | Global.set_Mask[i+16] |
					Global.set_Mask[i+17] | Global.set_Mask[i+18] | Global.set_Mask[i+1] | Global.set_Mask[i+2] | Global.set_Mask[i];
				Global.bKingMask[i] = Global.set_Mask[56+i-6] | Global.set_Mask[56+i-7] | Global.set_Mask[56+i-8] | Global.set_Mask[56+i-14] |
					Global.set_Mask[56+i-15] | Global.set_Mask[56+i-16] | Global.set_Mask[56+i+1] | Global.set_Mask[56+i+2] | Global.set_Mask[56+i];
		
			} else if(i % 8 == 7) {
				Global.wKingMask[i] = Global.set_Mask[i+6] | Global.set_Mask[i+7] | Global.set_Mask[i+8] | Global.set_Mask[i+14] |
					Global.set_Mask[i+15] | Global.set_Mask[i+16] | Global.set_Mask[i-1] | Global.set_Mask[i-2] | Global.set_Mask[i];
				Global.bKingMask[i] = Global.set_Mask[56+i-8] | Global.set_Mask[56+i-9] | Global.set_Mask[56+i-10] | Global.set_Mask[56+i-16] |
					Global.set_Mask[56+i-17] | Global.set_Mask[56+i-18] | Global.set_Mask[56+i-1] | Global.set_Mask[56+i-2] | Global.set_Mask[56+i];	
				
			} else {
				Global.wKingMask[i] = Global.set_Mask[i+7] | Global.set_Mask[i+8] | Global.set_Mask[i+9] | Global.set_Mask[i+15] |
					Global.set_Mask[i+16] | Global.set_Mask[i+17] | Global.set_Mask[i-1] | Global.set_Mask[i+1] | Global.set_Mask[i];
				Global.bKingMask[i] = Global.set_Mask[56+i-7] | Global.set_Mask[56+i-8] | Global.set_Mask[56+i-9] | Global.set_Mask[56+i-15] |
					Global.set_Mask[56+i-16] | Global.set_Mask[56+i-17] | Global.set_Mask[56+i+1] | Global.set_Mask[56+i-1] | Global.set_Mask[56+i];
				
			}
		}
		
			
		Global.whiteTerritory = 0;	
		for(int i=0;i<32;i++) {
			Global.whiteTerritory |= (long)1<<i;
		}
		Global.blackTerritory = ~Global.whiteTerritory;
		setBishopMasks();
		setBishopMagics();
		setRookMasks();
		setRookMagics();
		populateBishopTables();
		populateRookTables();
	}
	public static final void callWriter() {
		writer.writeHistory();
	}	
	public static final void callFileWrite(File f) {
		writer.historyToFile(f);
	}	
	public static final void callFileRead(File f) {
		writer.readHistory(f);
	}	
	private static final void initQueenDist() {
		for(int i=0;i<64;i++) {
			for(int j=0;j<64;j++) {
				if((i%8<j%8)||(i%8==j%8)||(i/8==j/8))	{
					queenDist[i][j] = getQueenDistance(i,j);
					queenDist[j][i] = queenDist[i][j];
				}					
			}		
		}						 
	}
	public final static int getDistance(int q,int k) {
		return queenDist[q][k];
	}		
		
		
	
	private static final int getQueenDistance(int qPos,int kPos) {
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
	public final void undoAll(){
		int noMoves = moveCount;
		for(int i=noMoves-1;i>=0;i--) {
			unMake(boardMoves[i],true,true);
		}
		//moveCount = 0;
		//setChanged();
		//notifyObservers();
		//writer.reset();
	}
	
        
        public static void acceptFen(String fen) {
            String rank;
            
            
          //reseting board variables  
            
            blackpieces = 0;
            whitepieces = 0;
            //bHasCastled = false;
            //wHasCastled = false;
            bitboard = 0;
            whiteknights = 0;
            blackknights = 0;
            whitebishops = 0;
            blackbishops = 0;
            whiterooks = 0;
            blackrooks = 0;
            whiteking = 0;
            blackking = 0;
            whitequeen = 0;
            blackqueen = 0;
            whitepawns = 0;
            blackpawns = 0;
            slidePieces = 0;
            totalValue = 0;
            value = 0;
            pawnsKings = 0;
            wPieceNo = 0;
            bPieceNo = 0;
            
            //clear the board
            
            for(int i=0;i<64;i++) {		
		piece_in_square[i] = -1;
            }
		
        
            bCastle = Global.NO_CASTLE;
            wCastle = Global.NO_CASTLE;
		
            passantW = -1;
            passantB = -1;
            
            
            hashValue = 0;
            hashValue2 = 0;
            pawnHash = 0;
            pawnHash2 = 0;
            
            
           
            //now process the fen string where it contains the placement of pieces
            
            int count = 63;
            for(int i=0; i<8; i++) {
             
                int endOfRank = fen.indexOf("/");
                if(endOfRank == -1) {
                    endOfRank = fen.indexOf(" ");
                    rank = fen.substring(0,endOfRank);
                    fen = fen.substring(endOfRank+1);
                }else {
                    rank = fen.substring(0,endOfRank);
                    fen = fen.substring(endOfRank+1);
                }
                
                for(int j=0; j<endOfRank; j++) {
                    char c = rank.charAt(endOfRank-1-j);
                
                    switch(c) {
                        case('r'):
                            setBoard(count,6);
                            hashValue ^= pHash[count][6];
                            hashValue2 ^= pHash2[count][6];
                            count--;
                            break;
                        case('n'):
                            setBoard(count,7);
                            hashValue ^= pHash[count][7];
                            hashValue2 ^= pHash2[count][7];
                            count--;
                            break;
                        case('b'):
                            setBoard(count,8);
                            hashValue ^= pHash[count][8];
                            hashValue2 ^= pHash2[count][8];
                            count--;
                            break;
                        case('q'):
                            setBoard(count,9);
                            hashValue ^= pHash[count][9];
                            hashValue2 ^= pHash2[count][9];
                            count--;
                            break;
                        case('k'):
                            setBoard(count,10);
                            hashValue ^= pHash[count][10];
                            hashValue2 ^= pHash2[count][10];
                            count--;
                            break;
                        case('p'):
                            setBoard(count,11);
                            hashValue ^= pHash[count][11];
                            hashValue2 ^= pHash2[count][11];
                            count--;
                            break;
                        case('R'):
                            setBoard(count,0);
                            hashValue ^= pHash[count][0];
                            hashValue2 ^= pHash2[count][0];
                            count--;
                            break;
                        case('N'):
                            setBoard(count,1);
                            hashValue ^= pHash[count][1];
                            hashValue2 ^= pHash2[count][1];
                            count--;
                            break;
                        case('B'):
                            setBoard(count,2);
                            hashValue ^= pHash[count][2];
                            hashValue2 ^= pHash2[count][2];
                            count--;
                            break;
                        case('Q'):
                            setBoard(count,3);
                            hashValue ^= pHash[count][3];
                            hashValue2 ^= pHash2[count][3];
                            count--;
                            break;
                        case('K'):
                            setBoard(count,4);
                            hashValue ^= pHash[count][4];
                            hashValue2 ^= pHash2[count][4];
                            count--;
                            break;
                        case('P'):
                            setBoard(count,5);
                            hashValue ^= pHash[count][5];
                            hashValue2 ^= pHash2[count][5];
                            count--;
                            break;
                        case('1'):
                            count--;
                            break;
                        case('2'):
                            count-=2;
                            break;
                        case('3'):
                            count-=3;
                            break;
                        case('4'):
                            count-=4;
                            break;
                        case('5'):
                            count-=5;
                            break;
                        case('6'):
                            count-=6;
                            break;
                        case('7'):
                            count-=7;
                            break;
                        case('8'):
                            count-=8;
                            break;
                    }
                }
            }
            
            //now process the side to move information
         
            char c = fen.charAt(0);
            fen = fen.substring(fen.indexOf(" ")+1);
            if(c == 'w') {
                turn = -1;
            } else {
                turn = 1;
            }
            
            
            //now process the castling rights
            
            String token = fen.substring(0,fen.indexOf(" "));
            fen = fen.substring(fen.indexOf(" ")+1);
            int tokenSize = token.length();
            for(int i=0;i<tokenSize;i++) {
                c = token.charAt(i);
                switch(c) {
                    case('K'):
                        wCastle = Global.SHORT_CASTLE;
                        break;
                    case('Q'):
                        if(wCastle == Global.NO_CASTLE)
                            wCastle = Global.LONG_CASTLE;
                        else
                            wCastle = Global.BOTH_CASTLE;
                        break;
                    case('k'):
                        bCastle = Global.SHORT_CASTLE;
                        break;
                    case('q'):
                        if(bCastle == Global.NO_CASTLE)
                            bCastle = Global.LONG_CASTLE;
                        else
                            bCastle = Global.BOTH_CASTLE;
                        break;
                        
                }
                
            }

            //process the passant square
            //get the first character - if it is a '-', then no passant square
            c = fen.charAt(0);
            if(c != '-') {
                token = fen.substring(0,fen.indexOf(" "));
                //System.out.println("passant square is "+token);
                int pSq = HistoryWriter.getNumericPosition(token);
                //System.out.println("passant int is "+pSq);
                if(turn == -1)                  //white moving
                    passantB = pSq;
                else
                    passantW = pSq;
            }
            fen = fen.substring(fen.indexOf(" "));


            //now process the drawCount
            
            fen = fen.substring(fen.indexOf(" ")+1); 
            token = fen.substring(0,fen.indexOf(" "));    
            Integer noMoves = new Integer(token);
            drawCount = noMoves.intValue();
            
            
            //now process the moveCount
            //Note there are problems with this part of the fen reader
            //To Do...change internal representation of number of moves
            // - fix unmaking moves based on this the no of moves
            // - currently will only work for no of moves at 1
            
            fen = fen.substring(fen.indexOf(" ")+1); 
            token = fen;
            noMoves = new Integer(token);
            moveCount = noMoves.intValue()-1;
        
            // set the has values for the recently set castling rights
            
            hashValue ^= bCastleHash[bCastle];
            hashValue2 ^= bCastleHash2[bCastle];
            hashValue ^= wCastleHash[wCastle];
            hashValue2 ^= wCastleHash2[wCastle];
            
        
        
        }
	
        
        public final void newGame() {
		blackpieces = 0;
		whitepieces = 0;
		//prevMoves.clear();
		moveCount = 0;
		drawCount = 0;
		//bHasCastled = false;
		//wHasCastled = false;
		bitboard = 0;
		Board45R = 0;
		Board45L = 0;
	 	Board90R = 0;
		whiteknights = 0;
		blackknights = 0;
		whitebishops = 0;
		blackbishops = 0;
		whiterooks = 0;
		blackrooks = 0;
		whiteking = 0;
		blackking = 0;
		whitequeen = 0;
		blackqueen = 0;
		whitepawns = 0;
		blackpawns = 0;
		slidePieces = 0;
		totalValue = 0;
		turn = -1;						//white moves first
		value = 0;
		pawnsKings = 0;
		wPieceNo = 0;
		bPieceNo = 0;
		for(int i=0;i<64;i++) {										//init empty squares to have -1 value
			attack1[i] = 0;
			attack2[i] = 0;
			piece_in_square[i] = -1;
		}
		
		bCastle = Global.BOTH_CASTLE;
		wCastle = Global.BOTH_CASTLE;
		
		passantW = -1;
		passantB = -1;
		repTable = new RepetitionTable();
		hashValue = 0;
		hashValue2 = 0;
		pawnHash = 1;
		pawnHash2 = 1;
		hashHistory = new int[512];
		hashHistory2 = new int[512];
		flagHistory = new int[512];
		for(int i=0;i<64;i++) {
			if(init[i]!=-1) {
				setBoard(i,init[i]);
				hashValue ^= pHash[i][init[i]];
				hashValue2 ^= pHash2[i][init[i]];
				//setBoards();
			}	
		}
		hashValue ^= bCastleHash[bCastle];
		hashValue2 ^= bCastleHash2[bCastle];
		hashValue ^= wCastleHash[wCastle];
		hashValue2 ^= wCastleHash2[wCastle];
		Engine.resetHash();
		Evaluation2.clearEvalHash();
		Evaluation2.clearPawnHash();
		setChanged();
		notifyObservers();
		writer.reset();
		
	}	
	public static final long getPawnHash() {
		return pawnHash;
	}
	public static final long getPawnHash2() {
            return pawnHash2;
        }	
	public static final long getHash() {
		return hashValue;
	}
	
	public static final long getHash2() {
		return hashValue2;
	}	
		
	public static final long generateHash() {
		long temp2;
		long temp = bitboard;
		int hash = 0;
		while(temp!=0) {
			temp2 = temp&-temp;
			int pos = getPos(temp2);
			temp&=~temp2;
	
			hash ^=pHash[pos][piece_in_square[pos]];
		}			
		if(passantW!=-1)
			hash ^= passantHashW[passantW%8];
		if(passantB!=-1)
			hash ^= passantHashB[passantB%8];
		
		hash ^= bCastleHash[bCastle];
		hash ^= wCastleHash[wCastle];
	
		if(turn==1) {
			hash^=bHashMove; 
		}
	
	return hash;
}	
	public static final long generateHash2() {
		long temp2;
		long temp = bitboard;
		int hash = 0;
		while(temp!=0) {
			temp2 = temp&-temp;
			int pos = getPos(temp2);
			temp&=~temp2;
	
			hash ^=pHash2[pos][piece_in_square[pos]];
		}			
		if(passantW!=-1)
			hash ^= passantHashW2[passantW%8];
		if(passantB!=-1)
			hash ^= passantHashB2[passantB%8];
		
		hash ^= bCastleHash2[bCastle];
		hash ^= wCastleHash2[wCastle];
	
		if(turn==1) {
			hash^=bHashMove2; 
		}
	
	return hash;
}	

	private static final void setHash() {
		Random rand;
		int i, j;							//counters 
		
		rand = new Random(80392848);
		for(i=0;i<64;i++) {
			
			for(j=0;j<12;j++) {
				pHash[i][j]=rand.nextInt() & Integer.MAX_VALUE;
				
                                pHash2[i][j]=rand.nextInt();
			}
		}
	
		//set castle hashes and enPassant hashes
		for(i=0;i<8;i++) {
			passantHashW[i] = rand.nextInt() & Integer.MAX_VALUE;
			passantHashW2[i] = rand.nextInt();
			passantHashB[i] = rand.nextInt() & Integer.MAX_VALUE;
			passantHashB2[i] = rand.nextInt();
		}				
					
		bHashMove = rand.nextInt() & Integer.MAX_VALUE;
		bHashMove2 = rand.nextInt();
			
		for(i=0;i<8;i++) {
			bCastleHash[i] = rand.nextInt() & Integer.MAX_VALUE;
			bCastleHash2[i] = rand.nextInt();
			wCastleHash[i] = rand.nextInt() & Integer.MAX_VALUE;
			wCastleHash2[i] = rand.nextInt();
		}
				
	}				
	
	//public static final boolean isEmpty() {
	//	return prevMoves.empty();
	//}
		
	/*
	public Move getMove() {
		return (Move)prevMoves.pop();
	}


	*/
	
	/***********************************************************************
		Name:		setBishopMagics
		Parameters:	None
		Returns:	None
		Description:This method hard codes in the 64 magic numbers for bishops
	***********************************************************************/	
	private static final void setBishopMagics() {
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
	private static final void setRookMagics() {
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
	private static final void setBishopMasks() {
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
	
	private static final void setRookMasks() {
		for(int i=0; i<64; i++) {
			rMask[i] = getConventionalRookMoves(i,0); //& 0x007e7e7e7e7e7e00L;
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
	private static final void populateBishopTables() {
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
	public static final void populateRookTables() {
		int index;
		long occ;
		long attacks;
		long oset;
		
		for(int sq = 0; sq < 64; sq++) {
			occ = 0;
			//oset = getConventionalRookMoves(sq,occ); //& 0x007e7e7e7e7e7e00L;
			/*
			if(sq%8 > 0)
				oset &= 0x7f7f7f7f7f7f7f7fL;
			if(sq%8 < 7)
				oset &= 0xfefefefefefefefeL;
			if(sq/8 > 0)
				oset &= 0x00ffffffffffffffL;
			if(sq/8 < 7)
				oset &= 0xffffffffffffff00L;	
			*/
			oset = rMask[sq];
			do {
			
				index = (int)(((occ*rMagics[sq])>>>(64-rookShift[sq])));
				//System.out.println("index is "+index);
				//System.out.println("sq is "+sq);
				attacks = getConventionalRookMoves(sq,occ);
				rookTable[sq][index] = attacks;
				occ = (occ - oset) & oset;
			} while(occ != 0);
		}
	}		

	/***********************************************************************
		Name:		setBoards
		Parameters:	None
		Returns:	None
		Description:This method populates the rotated BitBoards
	***********************************************************************/
	private static final void setBoards() {			//sets the Rotated Static Bitboards
		Board45L = rotate45L(bitboard);
		Board45R = rotate45R(bitboard);
		Board90R = rotate90R(bitboard);
	}// setBoards
	
	
	
	
	
	/***********************************************************************
		Name:		setBoard
		Parameters:	int, String
		Returns:	None
		Description:This method sets where a piece will go on the board. The 
					int paramater defines where the piece will go and the 
					String parameter defines what type of piece it is.
	***********************************************************************/
	private static final void setAttack() {	//method sets the two arrays of 64 Bitsets
		
		int i,j;
		long moves;
		long bit;
		for(i=0;i<64;i++) {		//initialize arrays
			attack1[i] = 0;
			attack2[i] = 0;
		}
		
		for(i=0;i<64;i++) {
			if(piece_in_square[i]>=0&&piece_in_square[i]<12) {
				moves = getAttackBoard(i);
			
				attack1[i] = moves;
				
				for(j=0;j<64;j++) {
					bit = (long)1<<i;
					bit &= moves;
					if(((moves>>j)&1)==1) {
					//if(AttackBoards1[i].get(j))
						attack2[j] += (long)1<<i;
					}	
				}
			}
	
		}
}
	
	
	
	
	
	
	public static final void setBoard(int i,int piece) {
		
		
		bitboard |= Global.set_Mask[i];
		
		piece_in_square[i] = piece;
		
		switch(piece) {
			case 0:
				wPieceNo++;
				slidePieces |= Global.set_Mask[i];
				whitepieces |= Global.set_Mask[i];
				whiterooks |= Global.set_Mask[i];
				value -= Global.values[piece];
				totalValue +=Global.values[piece];
				break;
			case 1:
				wPieceNo++;
				whitepieces |= Global.set_Mask[i];
				whiteknights |= Global.set_Mask[i];
				value -= Global.values[piece];
				totalValue +=Global.values[piece];
				break;
			case 2:
				wPieceNo++;
				slidePieces |= Global.set_Mask[i];
				whitepieces |= Global.set_Mask[i];
				whitebishops |= Global.set_Mask[i];
				value -= Global.values[piece];
				totalValue +=Global.values[piece];
				break;
			case 3:
				wPieceNo++;
				slidePieces |= Global.set_Mask[i];
				whitepieces |= Global.set_Mask[i];
				whitequeen |= Global.set_Mask[i];
				value -= Global.values[piece];
				totalValue +=Global.values[piece];
				break;
			case 4:
				wPieceNo++;
				whitepieces |= Global.set_Mask[i];
				whiteking |= Global.set_Mask[i];
				pawnsKings |= Global.set_Mask[i];
				value -= Global.values[piece];
				break;
			case 5:
				wPieceNo++;
				whitepieces |= Global.set_Mask[i];
				whitepawns |= Global.set_Mask[i];
				pawnsKings |= Global.set_Mask[i];
				value -= Global.values[piece];
				totalValue +=Global.values[piece];
				pawnHash ^= pHash[i][piece];
				pawnHash2 ^= pHash2[i][piece];
				break;			
			case 6:
				bPieceNo++;
				slidePieces |= Global.set_Mask[i];
				blackpieces |= Global.set_Mask[i];
				blackrooks |= Global.set_Mask[i];
				value += Global.values[piece];
				totalValue +=Global.values[piece];
				break;
			case 7:
				bPieceNo++;
				blackpieces |= Global.set_Mask[i];
				blackknights |= Global.set_Mask[i];
				value += Global.values[piece];
				totalValue +=Global.values[piece];
				break;
			case 8:
				bPieceNo++;
				slidePieces |= Global.set_Mask[i];
				blackpieces |= Global.set_Mask[i];
				blackbishops |= Global.set_Mask[i];
				value += Global.values[piece];
				totalValue +=Global.values[piece];
				break;
			case 9:
				bPieceNo++;
				slidePieces |= Global.set_Mask[i];
				blackpieces |= Global.set_Mask[i];
				blackqueen |= Global.set_Mask[i];
				value += Global.values[piece];
				totalValue +=Global.values[piece];
				break;
			case 10:
				bPieceNo++;
				blackpieces |= Global.set_Mask[i];
				blackking |= Global.set_Mask[i];
				pawnsKings |= Global.set_Mask[i];
				value += Global.values[piece];
				break;
			case 11:
				bPieceNo++;
				blackpieces |= Global.set_Mask[i];
				blackpawns |= Global.set_Mask[i];
				pawnsKings |= Global.set_Mask[i];
				value += Global.values[piece];
				totalValue +=Global.values[piece];
				pawnHash ^= pHash[i][piece];
				pawnHash2 ^= pHash2[i][piece];
				break;
		}		//end 
		//if(!equal)
		//setAttack1(i,false,0,0);
	
		
	}// End setBoard
	
	/***********************************************************************
		Name:		updateBoard
		Parameters:	int, String
		Returns:	None
		Description:This method updates all of the boards so that on index
					int there exists piece String
	***********************************************************************/	
	private static final void updateBoard(int i,int j) {
		
		
		long bit = Global.set_Mask[i]|Global.set_Mask[j];
	
		bitboard ^= bit;
		
		int piece = piece_in_square[j];
		piece_in_square[i] = piece;
		piece_in_square[j] = -1;
		passantW=-1;
		passantB=-1;
		switch(piece) {
			case(0):		//wRook
				whitepieces ^= bit;
				whiterooks ^= bit;
				slidePieces ^= bit;
				break;
			case(1):
				whitepieces ^= bit;
				whiteknights ^= bit;
				break;
			case(2):
				whitepieces ^= bit;
				whitebishops ^= bit;
				slidePieces ^= bit;
				break;
			case(3):
				whitepieces ^= bit;
				whitequeen ^= bit;
				slidePieces ^= bit;
				break;
			case(4):
				whitepieces ^= bit;
				whiteking ^= bit;
				pawnsKings ^= bit;
				break;
			case(5):
				whitepieces ^= bit;
				whitepawns ^= bit;
				pawnHash ^= pHash[i][piece];
				pawnHash2 ^= pHash2[i][piece];
				pawnHash ^= pHash[j][piece];
				pawnHash2 ^= pHash2[j][piece];
				pawnsKings ^= bit;
				
				if((i-j) == 16)	{
					hashValue ^= passantHashW[i%8];
					hashValue2 ^= passantHashW2[i%8];
					passantW = i-8;
				} 
				
				break;
			case(6):
				blackpieces ^= bit;
				blackrooks ^= bit;
				slidePieces ^= bit;
				break;
			case(7):
				blackpieces ^= bit;
				blackknights ^= bit;
				break;
			case(8):
				blackpieces ^= bit;
				blackbishops ^= bit;
				slidePieces ^= bit;
				break;
			case(9):
				blackpieces ^= bit;
				blackqueen ^= bit;
				slidePieces ^= bit;
				break;
			case(10):
				blackpieces ^= bit;
				blackking ^= bit;
				pawnsKings ^= bit;
				break;
			case(11):
				blackpieces ^= bit;
				blackpawns ^= bit;
				pawnHash ^= pHash[i][piece];
				pawnHash2 ^= pHash2[i][piece];
				pawnHash ^= pHash[j][piece];
				pawnHash2 ^= pHash2[j][piece];
				pawnsKings ^= bit;
				
				if((i-j) == -16) {
					hashValue ^= passantHashB[i%8];
					hashValue2 ^= passantHashB2[i%8];
					passantB = i+8;
				}
				
				break;
		}
		
		
	}// End updateBoard
	/***********************************************************************
		Name:		clearBoard
		Parameters:	int, String
		Returns:	None
		Description:On index i String s is removed by this method
	***********************************************************************/
	public static final void clearBoard(int i) {
		
		bitboard ^= Global.set_Mask[i];
		int piece = piece_in_square[i];
		
		
		
		switch(piece) {
			case 0:
				wPieceNo--;
				slidePieces ^= Global.set_Mask[i];
				whitepieces ^= Global.set_Mask[i];
				whiterooks ^= Global.set_Mask[i];
				value += Global.values[piece];
				totalValue -=Global.values[piece];
				break;
			case 1:
				wPieceNo--;
				whitepieces ^= Global.set_Mask[i];
				whiteknights ^= Global.set_Mask[i];
				value += Global.values[piece];
				totalValue -=Global.values[piece];
				break;
			case 2:
				wPieceNo--;
				slidePieces ^= Global.set_Mask[i];
				whitepieces ^= Global.set_Mask[i];
				whitebishops ^= Global.set_Mask[i];
				value += Global.values[piece];
				totalValue -=Global.values[piece];
				break;
			case 3:
				wPieceNo--;
				slidePieces ^= Global.set_Mask[i];
				whitepieces ^= Global.set_Mask[i];
				whitequeen ^= Global.set_Mask[i];
				value += Global.values[piece];
				totalValue -=Global.values[piece];
				break;
			case 4:
				wPieceNo--;
				whitepieces ^= Global.set_Mask[i];
				whiteking ^= Global.set_Mask[i];
				pawnsKings ^= Global.set_Mask[i];
				value += Global.values[piece];
				break;
			case 5:
				wPieceNo--;
				whitepieces ^= Global.set_Mask[i];
				whitepawns ^= Global.set_Mask[i];
				pawnsKings ^= Global.set_Mask[i];
				value += Global.values[piece];
				totalValue -=Global.values[piece];
				pawnHash ^= pHash[i][piece];
				pawnHash2 ^= pHash2[i][piece];
				break;		
			case 6:
				bPieceNo--;
				slidePieces ^= Global.set_Mask[i];
				blackpieces ^= Global.set_Mask[i];
				blackrooks ^= Global.set_Mask[i];
				value -= Global.values[piece];
				totalValue -=Global.values[piece];
				break;
			case 7:
				bPieceNo--;
				blackpieces ^= Global.set_Mask[i];
				blackknights ^= Global.set_Mask[i];
				value -= Global.values[piece];
				totalValue -=Global.values[piece];
				break;
			case 8:
				bPieceNo--;
				slidePieces ^= Global.set_Mask[i];
				blackpieces ^= Global.set_Mask[i];
				blackbishops ^= Global.set_Mask[i];
				value -= Global.values[piece];
				totalValue -=Global.values[piece];
				break;
			case 9:
				bPieceNo--;
				slidePieces ^= Global.set_Mask[i];
				blackpieces ^= Global.set_Mask[i];
				blackqueen ^= Global.set_Mask[i];
				value -= Global.values[piece];
				totalValue -=Global.values[piece];
				break;	
			case 10:
				bPieceNo--;
				blackpieces ^= Global.set_Mask[i];
				blackking ^= Global.set_Mask[i];
				pawnsKings ^= Global.set_Mask[i];
				value -= Global.values[piece];
				break;	
			case 11:
				bPieceNo--;
				blackpieces ^= Global.set_Mask[i];
				blackpawns ^= Global.set_Mask[i];
				pawnsKings ^= Global.set_Mask[i];
				value -= Global.values[piece];
				totalValue -=Global.values[piece];
				pawnHash ^= pHash[i][piece];
				pawnHash2 ^= pHash2[i][piece];
				break;
		}		//end switch	
		piece_in_square[i] = -1;
		//if(!overLap)
		//	setAttack2(i,cap);
		
	}// clearBoard
	public static final int getEval2(int side) {
		return side*value;
	}	
	public static final int getTotalValue() {
		return totalValue;
	}


	public static int getPos(long pos) { // this method will return a board position given a proper long
			int first = (int)pos;
			int second=0;
			int sq=0;
			if(first!=0) {
				second = first;
				sq = 0;
			}else {
				second = (int)(pos>>>32);
				sq+=32;	
			}
			second*=debruijn;
			second>>>=27;
			return sq+index32[second];
	}	

		public static final long getAttackBoard(int i) {
		
			switch(piece_in_square[i]) {
					case 0:
						return getMagicRookMoves(i);
					case 1:
						return Position.getKnightPosition(i);
					case 2:
						return getMagicBishopMoves(i);	
					case 3:
						return getQueenMoves(i);
					case 4:
						return Position.getKingPosition(i);
					case 5:
						return Position.getWhitePawnAttack(i);
					case 6:
						return getMagicRookMoves(i);
					case 7:
						return Position.getKnightPosition(i);
					case 8:
						return getMagicBishopMoves(i);
					case 9:
						return getQueenMoves(i);
					case 10:
						return Position.getKingPosition(i);
					case 11:
						return Position.getBlackPawnAttack(i);
				} 
			return 0;
		}
	
		public static final long getAttack1(int i) {
			return attack1[i];
		}		
		
		
		public static final long getAttack2(int i) {
			long temp = getMagicBishopMoves(i);
			long attack = temp&(whitebishops|blackbishops|whitequeen|blackqueen);
			
			temp = getMagicRookMoves(i);
			attack |= temp&(whiterooks|blackrooks|whitequeen|blackqueen);
			
			temp = Position.getKnightPosition(i);
			attack |= temp&(whiteknights|blackknights);
			
			temp = Position.getKingPosition(i);
			attack |= temp&(whiteking|blackking);
			
			temp = Position.getWhitePawnAttack(i);
			attack |= temp&blackpawns;
		
			temp = Position.getBlackPawnAttack(i);
			attack |= temp&whitepawns;
			
			return attack;
		}	
		
		public static final long getMovesTo(int i) {
			long temp = getMagicBishopMoves(i);
			long movers = temp&(whitebishops|blackbishops|whitequeen|blackqueen);
			
			temp = getMagicRookMoves(i);
			movers |= temp&(whiterooks|blackrooks|whitequeen|blackqueen);
			
			temp = Position.getKnightPosition(i);
			movers |= temp&(whiteknights|blackknights);
			
			//temp = Position.getKingPosition(i);
			//movers |= temp&(whiteking|blackking);
			
			//temp = Position.getWhitePawnAttack(i);
			//movers |= temp&blackpawns&whitepieces;
		
			//temp = Position.getBlackPawnAttack(i);
			//movers |= temp&whitepawns&blackpieces;
			
			if(piece_in_square[i] == -1) {
				if(i/8 >0) {
				
					temp = Global.set_Mask[i-8];
					movers |= (temp & whitepawns);
					if((i/8 == 3) && (piece_in_square[i-8] == -1)) {
						temp = Global.set_Mask[i-16];
						movers |= (temp & whitepawns);
					}
				}
				if(i/8<7) {
				
					temp = Global.set_Mask[i+8];
					movers |= (temp & blackpawns);
					if((i/8 == 4)&& (piece_in_square[i+8] == -1)) {
						temp = Global.set_Mask[i+16];
						movers |= (temp & blackpawns);
					}
				}
			}
			return movers;
		}	
		
		
		
		public static final boolean isWhiteAttacked(int i) {
			long temp = getMagicBishopMoves(i);
			temp &= (blackbishops | blackqueen);
			if(temp != 0) return true;
			
			temp = getMagicRookMoves(i);
			temp &= (blackrooks | blackqueen);
			if(temp != 0) return true;
			
			temp = Position.getKnightPosition(i);
			temp &= blackknights;
			if(temp != 0) return true;
			
			temp = Position.getKingPosition(i);
			temp &= blackking;
			if(temp != 0) return true;
			
			temp = Position.getWhitePawnAttack(i);
			temp &= blackpawns;
			if(temp != 0) return true;
			
			return false;
		}	
		public static final long getWhiteAttacker(int i) {
			long temp = getMagicBishopMoves(i);
			temp &= (blackbishops | blackqueen);
			if(temp != 0) return temp & -temp;
			
			temp = getMagicRookMoves(i);
			temp &= (blackrooks | blackqueen);
			if(temp != 0) return temp & -temp;
			
			temp = Position.getKnightPosition(i);
			temp &= blackknights;
			if(temp != 0) return temp & -temp;
			
			temp = Position.getKingPosition(i);
			temp &= blackking;
			if(temp != 0) return temp & -temp;
			
			temp = Position.getWhitePawnAttack(i);
			temp &= blackpawns;
			if(temp != 0) return temp & -temp;
			
			return 0;
		}	
		
		
		
		
		public static final boolean isBlackAttacked(int i) {
			long temp = getMagicBishopMoves(i);
			temp &= (whitebishops | whitequeen);
			if(temp != 0) return true;
			
			temp = getMagicRookMoves(i);
			temp &= (whiterooks | whitequeen);
			if(temp != 0) return true;
			
			temp = Position.getKnightPosition(i);
			temp &= whiteknights;
			if(temp != 0) return true;
			
			temp = Position.getKingPosition(i);
			temp &= whiteking;
			if(temp != 0) return true;
			
			temp = Position.getBlackPawnAttack(i);
			temp &= whitepawns;
			if(temp != 0) return true;
			
			return false;
		}	
		
		public static final long getSliders() {
			return slidePieces;
		}	
	/***********************************************************************
		Name:		getPiecesInSquare
		Parameters:	None
		Returns:	int[]
		Description:This method returns an integer array representing the 
					status of the chessboard
	***********************************************************************/	
	//array entries contain no from 0 to 11 for pieces, -1 means no piece
	public static final int[] getPiecesInSquare() {	
		return piece_in_square;
	}// End getPiecesInSquare
	
	/***********************************************************************
		Name:		getAllPieces
		Parameters:	None
		Returns:	BitSet
		Description:This method returns a BitSet representing where all of 
					the pieces are on the board. There is no distinction 
					between any of the pieces
	***********************************************************************/
	public static final long getAllPieces() {		//returns bitboard BitSet
		return bitboard;
	}// getAllPieces

	/***********************************************************************
		Name:		getWhitePieces
		Parameters:	None
		Returns:	BitSet
		Description:This method returns a BitSet representing the position 
					of all the white pieces
	***********************************************************************/
	public static final long getWhitePieces() {	//all the white pieces
		return whitepieces;	
	}// End getWhitePieces
	
	/***********************************************************************
		Name:		getBlackPieces
		Parameters:	None
		Returns:	None
		Description:This method returns  BitSet representing the position 
					of all the black pieces
	***********************************************************************/
	public static final long getBlackPieces() {	//all the black pieces
		return blackpieces;
	}// End getBlackPieces
		
	/***********************************************************************
		Name:		getWhiteRooks
		Parameters:	None
		Returns:	BitSet
		Description:This method returns a BitSet representing the position
					of all the white rooks
	***********************************************************************/
	public static final long getWhiteRooks() {		//all the white rooks
		return whiterooks;
	}// End getWhiteRooks
	
	/***********************************************************************
		Name:		getBlackRooks
		Parameters:	None
		Returns:	None
		Description:This method returns a BitSet representing the position 
					of all the black rooks
	***********************************************************************/	
	public static final long getBlackRooks() {		
		return blackrooks;
	}// End getBlackRooks

	/***********************************************************************		
		Name:		getWhiteKnights
		Parameters:	None
		Returns:	None
		Description:This method returns a BitSet representing the position 
					of all the white knights
	***********************************************************************/
	public static final long getWhiteKnights() {
		return whiteknights;
	}// End getWhiteKnights

	/***********************************************************************		
		Name:		getBlackKnights
		Parameters:	None
		Returns:	None
		Description:This method returns a BitSet representing the position 
					of all the black knights
	***********************************************************************/
	public static final long getBlackKnights() {
		return blackknights;
	}// End getBlackKnights
	
	/***********************************************************************		
		Name:		getWhiteBishops
		Parameters:	None
		Returns:	None
		Description:This method returns a BitSet representing the position 
					of all the white bishops
	***********************************************************************/
	public static final long getWhiteBishops() {
		return whitebishops;
	}// End getWhiteBishops
	
	/***********************************************************************		
		Name:		getBlackBishops
		Parameters:	None
		Returns:	None
		Description:This method returns a BitSet representing the position 
					of all the black bishops
	***********************************************************************/	
	public static final long getBlackBishops() {
		return blackbishops;
	}// End getBlackBishops

	/***********************************************************************		
		Name:		getWhiteQueen
		Parameters:	None
		Returns:	None
		Description:This method returns a BitSet representing the position 
					of the white queen
	***********************************************************************/
	public static final long getWhiteQueen() {
		return whitequeen;
	}// End getWhiteQueen
	
	/***********************************************************************		
		Name:		getBlackQueen
		Parameters:	None
		Returns:	None
		Description:This method returns a BitSet representing the position 
					of the Black queen
	***********************************************************************/
	public static final long getBlackQueen() {
		return blackqueen;
	}// End getBlackQueen

	/***********************************************************************		
		Name:		getWhiteKing
		Parameters:	None
		Returns:	None
		Description:This method returns a BitSet representing the position 
					of the white king
	***********************************************************************/
	public static final long getWhiteKing() {
		return whiteking;
	}// End getWhiteKing
	//public BitSet getWKing() {
	//	return convertToBSet(whiteking);
	//}	
	/***********************************************************************		
		Name:		getBlackKing
		Parameters:	None
		Returns:	None
		Description:This method returns a BitSet representing the position 
					of the black king
	***********************************************************************/
	public static final long getBlackKing() {
		return blackking;
	}// End getBlackKing

	/***********************************************************************		
		Name:		getWhitePawns
		Parameters:	None
		Returns:	None
		Description:This method returns a BitSet representing the position 
					of all the white pawns
	***********************************************************************/
	public static final long getWhitePawns() {
		return whitepawns;
	}// End getWhitePawns
	
	/***********************************************************************		
		Name:		getBlackPawns
		Parameters:	None
		Returns:	None
		Description:This method returns a BitSet representing the position 
					of all the black pawns
	***********************************************************************/	
	public static final long getBlackPawns() {
		return blackpawns;
	}// End getBlackPawns
	
	public static final long getPawnsAndKings() {
		return pawnsKings;
	}
	
	
	/***********************************************************************		
		Name:		rotate45R
		Parameters:	BitSet
		Returns:	BitSet
		Description:This method returns an BitSet representing the board 
					parameter rotated 45 degrees to the right
	***********************************************************************/	
	private static final long rotate45R(long board) {
			int index;
			long bit;
			long rotate = 0;
			for(int i=0;i<64;i++) {
				
				index = R45Convert[i];
				bit = Global.set_Mask[index];
				bit &= board;
				
				if(((bit>>index)&1)==1) {
					rotate+=Global.set_Mask[i];
				}
			}	
		return rotate;
	}// End rotate45R

	/***********************************************************************		
		Name:		rotate45L
		Parameters:	BitSet
		Returns:	BitSet
		Description:This method returns an BitSet representing the board 
					parameter rotated 45 degrees to the left
	***********************************************************************/	
	private static final long rotate45L(long board) {
			int index;
			long rotate;
			long bit;
			//BitSet rotate = new BitSet(64);
			rotate = 0;
			for(int i=0;i<64;i++) {
				index = L45Convert[i];
				bit = (long)1<<index;
				bit &= board;
				
				if(((bit>>index)&1)==1) {
					rotate+=(long)1<<i;
				}
			}
		return rotate;
	}// End rotate45L
		
	
	/***********************************************************************		
		Name:		rotate90R
		Parameters:	BitSet
		Returns:	BitSet
		Description:This method returns an BitSet representing the board 
					parameter rotated 90 degrees to the right
	***********************************************************************/	
	private static final long rotate90R(long board) {
			int index;
			long bit;
			long rotate = 0;
			for(int i=0;i<64;i++) {
				//index = Convert[i];
				bit = Global.set_Mask[i];
				bit &=board;
				if(((bit>>i)&1)==1) {
					rotate+=Global.set_Mask[R90Update[i]];
				}
			}
	//Board90R |= Global.set_Mask[R90Update[i]];
	return rotate;
	}// End rotate90R
	

	public static final int getPassantW() {			//this method gets whtie passant square
		return passantW;
	}
	public static final int getPassantB() {			//this method gets black passant square
		return passantB;
	}	
		
	public static final boolean whiteHasCastled() {
            return(wCastle==Global.CASTLED);
            //return wHasCastled;
	}
	public static final boolean blackHasCastled() {
            return(bCastle==Global.CASTLED);
            //return bHasCastled;
	}

	
	public static final long getWPawnAttack(int index) {
		return Position.getWhitePawnAttack(index);
	}
	
	/***********************************************************************		
		Name:		getWPawnMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					white pawn moves
	***********************************************************************/		
	public static final long getWPawnMoves(int index,int pB) {   //get White Pawn Moves Based on index of pawn
		long moves = Position.getWhitePawnMove(index);
		long pMask;
		if(pB == -1)
			pMask = 0;
		else 
			pMask = Global.set_Mask[pB];
		moves &=~(whitepieces | blackpieces);
		if(index<16&&(bitboard&Global.set_Mask[index+8])!=0)
				moves &=~Global.set_Mask[index+16];
		long moves2 = Position.getWhitePawnAttack(index);
		moves2 &=(blackpieces|pMask);
		return moves|=moves2;
	}// End getWPawnMoves

	
	public static final long getBPawnAttack(int index) {
		return Position.getBlackPawnAttack(index);
	}
	
	/***********************************************************************		
		Name:		getWBawnMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					black pawn moves
	***********************************************************************/		
	public static final long getBPawnMoves(int index,int pW) {	//get Black Pawn Moves Based on index of pawn
		long moves = Position.getBlackPawnMove(index);
		long pMask;
		if(pW == -1)
			pMask = 0;
		else
			pMask = Global.set_Mask[pW];
		moves &=~(blackpieces | whitepieces);			//can't move over either pieces with pawn
		if(index>47&&(bitboard&Global.set_Mask[index-8])!=0)
				moves &=~Global.set_Mask[index-16];
		long moves2 = Position.getBlackPawnAttack(index);
		moves2 &= (whitepieces|pMask);
		return moves |= moves2;
	}// End getBPawnMoves
	
	/***********************************************************************		
		Name:		getWKnightMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					white knight moves
	***********************************************************************/			
	public static final long getWKnightMoves(int index) {
		long knight = Position.getKnightPosition(index);
		return knight&~whitepieces;
	}// End getWKnightMoves
	
	/***********************************************************************		
		Name:		getBKnightMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					black knight moves
	***********************************************************************/			
	public static final long getBKnightMoves(int index) {
		long knight = Position.getKnightPosition(index);
		return knight&~blackpieces;
	}// End getBKnightMoves

	
	public static final long getKnightMoves(int index) {
		long knight = Position.getKnightPosition(index);
		return knight;
	}
	
	
	
	
	public static final long getBKingCastle(int index) {
		long Temp = bitboard>>>56;
		int Decimal = (int)Temp;
		Temp = Position.getKingCastle(Decimal);
		long castle = 0;
		//Temp = Temp<<56;
		if((Temp & Global.set_Mask[2])!=0) {		//if left castle available test for checks
			if( !isBlackAttacked(58) && !isBlackAttacked(59) )
				castle |= Global.set_Mask[58];
			
		}
		if((Temp & Global.set_Mask[6])!=0) {		//if right castle available test for checks
			if( !isBlackAttacked(61) && !isBlackAttacked(62) )
				castle |= Global.set_Mask[62];
		}
		return castle;
	}
	
	
	/***********************************************************************		
		Name:		getBKingMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					black king moves
	***********************************************************************/			
	
	
	
	
	public static final long getBKingMoves(int index) {
	 	long kingPos = Position.getKingPosition(index);
	 	return kingPos&~blackpieces;
	}//End getBKingMoves
	
	
	
	public static final long getWKingCastle(int index) {
		int Decimal = (int)bitboard&255;
		long Temp = Position.getKingCastle(Decimal);
		if((Temp & Global.set_Mask[2])!=0) {		//if left castle available test for checks
			if(isWhiteAttacked(2) || isWhiteAttacked(3))
				Temp &= Global.set_Mask[6];
		}
		if((Temp & Global.set_Mask[6])!=0) {		//if right castle available test for checks
			if(isWhiteAttacked(5) || isWhiteAttacked(6))
				Temp &= Global.set_Mask[2];
		}
		return Temp;
	}
	
	
	
	/***********************************************************************		
		Name:		getWKingMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					white king moves
	***********************************************************************/			
	public static final long getWKingMoves(int index) {
	 	long kingPos = Position.getKingPosition(index);
	 	kingPos &= ~whitepieces;
		return kingPos;
	}// getWKingMoves

	/***********************************************************************		
		Name:		getWRookMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					white rook moves
	***********************************************************************/				
	public static final long getWRookMoves(int index) {
		long moves = getRookMoves(index,0);
		return moves&~whitepieces;
	}// End getWRookMoves

	/***********************************************************************		
		Name:		getBRookMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					black rook moves
	***********************************************************************/			
	public static final long getBRookMoves(int index) {
		long moves = getRookMoves(index,0);
		return moves&~blackpieces;
	}// End getBRookMoves

	/***********************************************************************		
		Name:		getWBishopMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					white bishop moves
	***********************************************************************/				
	public static final long getWBishopMoves(int index) {
		long moves = getBishopMoves(index,0);
		return moves&~whitepieces;
	}//  End getWBishopMoves

	/***********************************************************************		
		Name:		getBBishopMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					black bishop moves
	***********************************************************************/			
	public static final long getBBishopMoves(int index) {
	
		long moves = getBishopMoves(index,0);
		return moves&~blackpieces;
	}// End getBBishopMoves

	/***********************************************************************		
		Name:		getWQueenMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					white queen moves
	***********************************************************************/			
	public static final long getWQueenMoves(int index) {	
		long moves = getQueenMoves(index);
		return moves&~whitepieces;
	}// End getWQueenMoves

	/***********************************************************************		
		Name:		getBQueenMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					black moves moves
	***********************************************************************/			
	public static final long getBQueenMoves(int index) {
		long moves = getQueenMoves(index);
		return moves&~blackpieces;
	}// End getBQueenMoves
	
	/***********************************************************************		
		Name:		getRookMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					rook moves
	***********************************************************************/			
	
	
	private static final long getRookMoves(int index,int relation) {
			long temp=0;
		
			if(relation!=2) {
				long sRank = bitboard>>ShiftRank[index];
				int Decimal = (int)(sRank&255);	
				temp = Position.getRooksRank2(index,Decimal);
			}
			if(relation!=1) {
				long sRank = Board90R>>ShiftFile[index];
				int Decimal = (int)(sRank&255);
				temp |=  Position.getRooksFile2(index,Decimal);
			}
			/*
			if(getConventionalRookMoves(index,bitboard) != getMagicRookMoves(index)) {
				System.out.println("old moves are "+getConventionalRookMoves(index,bitboard));
				System.out.println("new moves are "+getMagicRookMoves(index));
				System.out.println("index is "+index);
			}
			*/	
			return temp;		

	}// End getRookMoves
	
	
	
	/***********************************************************************		
		Name:		getConventionalRookMoves
		Parameters:	int,long
		Returns:	BitSet
		Description:This method returns a bitset representing the attackable rook squares 
		given the occupancy
	***********************************************************************/	
	
	
	private static final long getConventionalRookMoves(int index, long occ) {
		long temp = 0;
		long sRank = occ>>ShiftRank[index];
		int Decimal = (int)(sRank&255);
		temp = Position.getRooksRank2(index,Decimal);
		sRank = rotate90R(occ)>>ShiftFile[index];
		Decimal = (int)(sRank&255);
		temp |= Position.getRooksFile2(index,Decimal);
		return temp;
		
	}
	/***********************************************************************		
		Name:		getMagicRookMoves
		Parameters:	int
		Returns:	long
		Description:This method returns a BitSet representing all of the 
					RookMoves moves from square int
	***********************************************************************/
	
	public static final long getMagicRookMoves(int index) {
		
		long occ = bitboard & rMask[index];
		occ *= rMagics[index];
		occ >>>= (64-rookShift[index]);
		return rookTable[index][(int)(occ)];	
	}
	
	
	/***********************************************************************		
		Name:		getBishopMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					BishopMoves moves from square int
	***********************************************************************/			
	private static final long getBishopMoves(int index,int relation) {
			
			if(relation==3) {
				int temp45 = (int)(Board45L>>ShiftL[index]);
				return Position.getDiag1Attack(index,temp45&255);	
			
			}else if(relation==4)	{
				int temp45 = (int)(Board45R>>ShiftR[index]);
				return Position.getDiag2Attack(index,temp45&255);
				
				
			}
			else {
				int temp45 = (int)(Board45L>>ShiftL[index]);
				long tempD = Position.getDiag1Attack(index,temp45&255);
				temp45 = (int)(Board45R>>ShiftR[index]);
				tempD |= Position.getDiag2Attack(index,temp45&255);
				return tempD;
			}		
	
	}
	
	/***********************************************************************		
		Name:		getConventionalBishopMoves
		Parameters:	int, long
		Returns:	long
		Description:This method returns a BitSet representing all of the 
					BishopMoves moves from square int, given a specified occupancy
	***********************************************************************/
	private static final long getConventionalBishopMoves(int index, long occ) {
		
		long right45 = rotate45R(occ);
		long left45 = rotate45L(occ);
		
		int temp45 = (int)(left45>>>ShiftL[index]);
		long tempD = Position.getDiag1Attack(index,temp45&255);
		
		temp45 = (int)(right45>>>ShiftR[index]);
		tempD |= Position.getDiag2Attack(index,temp45&255);
		
		return tempD;
		
	}
	/***********************************************************************		
		Name:		getMagicBishopMoves
		Parameters:	int
		Returns:	long
		Description:This method returns a BitSet representing all of the 
					BishopMoves moves from square int
	***********************************************************************/
	
	public static final long getMagicBishopMoves(int index) {
		
		long occ = bitboard & bMask[index];
		occ *= bMagics[index];
		occ >>>= (64-bishopShift[index]);
		return bishopTable[index][(int)(occ)];	
	}
	
	/*
	
	private static final long getBishopMoves(int index) {
			int length1, length2;
			int Decimal1, Decimal2;			
			long diag1;
			long diag2;
			long t45L;
			long t45R;
			int shift;
			
			t45L = Board45L>>ShiftL[index];
			length1 = Diag1Length[index];						
			Decimal1 = (int)t45L&((1<<length1)-1);
			diag1 = rPosition.getDiag1Attack(index,Decimal1);
			t45R = Board45R>>ShiftR[index];
			length2 = Diag2Length[index];							//same for next diagonal
			Decimal2 = (int)t45R&((1<<length2)-1);
			diag2 = rPosition.getDiag2Attack(index,Decimal2);
			
			diag1 |= diag2;
			return diag1;
	}// End getBishopMoves
*/
	/***********************************************************************		
		Name:		getQueenMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					queen moves from square int
	***********************************************************************/			
	public static final long getQueenMoves(int index) {
		//long Temp;	
		//Temp = getBishopMoves(index);
		//Temp |= getRookMoves(index);
		//return Temp;
		//return getBishopMoves(index,relation) | getRookMoves(index,relation);
		return getMagicBishopMoves(index) | getMagicRookMoves(index);
	}// End getQueenMoves
  	
 	public static final void switchTurn() {
 		hashValue ^=bHashMove;
 		hashValue2 ^= bHashMove2;
 		turn = -turn;	
 	
 	}
 	public static final int getMaxNumberOfPieces() {
 		if(wPieceNo>bPieceNo)
 			return wPieceNo;
 		else
 			return bPieceNo;
 	}
 	public static final int getMinNumberOfPieces() {
 		if(wPieceNo<bPieceNo)
 			return wPieceNo;
 		else
 			return bPieceNo;
 	}
 	
 	public static final int getNumberOfPieces(int side) {
 		if(side==-1)
 			return wPieceNo;
 		else
 			return bPieceNo;
 	}		
  	
	public final int makeMove(int move,boolean add,boolean board) {  
		
		int reps = 0;
		int to = MoveFunctions.getTo(move);
		int from = MoveFunctions.getFrom(move);
		int type = MoveFunctions.moveType(move);
		int thePiece = MoveFunctions.getPiece(move);
		int cP = MoveFunctions.getCapture(move);
		
		hashHistory[moveCount] = hashValue;
		hashHistory2[moveCount] = hashValue2;
		
		flagHistory[moveCount] = passantW+1 | (passantB+1)<<6 | wCastle<<12 | bCastle<<15 | drawCount << 21;
		
		if(add) {
			boardMoves[moveCount] = move;
			writer.addHistory(to,from,piece_in_square,moveCount);	
			
		}	
		turn = -turn;
		
		hashValue ^= bHashMove;	
		hashValue2 ^= bHashMove2;
		
		hashValue ^= bCastleHash[bCastle];
		hashValue2 ^= bCastleHash2[bCastle];
		hashValue ^= wCastleHash[wCastle];
		hashValue2 ^= wCastleHash2[wCastle];
		
		moveCount++;
		
		if(passantB!= -1) {
			hashValue ^= passantHashB[passantB%8];
			hashValue2 ^= passantHashB2[passantB%8];
		} else if(passantW != -1) {
			hashValue ^= passantHashW[passantW%8];
			hashValue2 ^= passantHashW2[passantW%8];
		}	
		
		switch(type) {
			case(Global.ORDINARY_MOVE):
				if(cP != -1) {
					drawCount = 0;
					clearBoard(to);
					hashValue ^= pHash[to][cP];
					hashValue2 ^= pHash2[to][cP];
				} else {
					if((thePiece % 6) != 5)
						drawCount++;
					else
						drawCount = 0;
				}
				break;
			case(Global.SHORT_CASTLE):
				drawCount++;
				if(thePiece == 4) {
					wCastle = Global.CASTLED;
					//wHasCastled = true;
					updateBoard(5,7);
					hashValue ^= pHash[5][0];
					hashValue2 ^= pHash2[5][0];
					hashValue ^= pHash[7][0];
					hashValue2 ^= pHash2[7][0];
				} else {
					bCastle = Global.CASTLED;
					//bHasCastled = true;
					updateBoard(61,63);
					hashValue ^= pHash[61][6];
					hashValue2 ^= pHash2[61][6];
					hashValue ^= pHash[63][6];
					hashValue2 ^= pHash2[63][6];
				}
				break;
			case(Global.LONG_CASTLE):
				drawCount++;
				if(thePiece == 4) {
					wCastle = Global.CASTLED;
					//wHasCastled = true;
					updateBoard(3,0);
					hashValue ^= pHash[3][0];
					hashValue2 ^= pHash2[3][0];
					hashValue ^= pHash[0][0];
					hashValue2 ^= pHash2[0][0];
				} else {
					bCastle = Global.CASTLED;
					//bHasCastled = true;
					updateBoard(59,56);
					hashValue ^= pHash[56][6];
					hashValue2 ^= pHash2[56][6];
					hashValue ^= pHash[59][6];
					hashValue2 ^= pHash2[59][6];
				}
				break;
			case(Global.EN_PASSANT_CAP):
				drawCount++;
				if(thePiece == 5) {
					clearBoard(to - 8);
					hashValue ^= pHash[to-8][11];
					hashValue2 ^= pHash2[to-8][11];
				} else {
					clearBoard(to+8);
					hashValue ^= pHash[to+8][5];
					hashValue2 ^= pHash2[to+8][5];
				}
				break;
		
			case(Global.PROMO_Q):
				drawCount = 0;
				clearBoard(from);
				hashValue ^= pHash[from][thePiece];
				hashValue2 ^= pHash2[from][thePiece];
				setBoard(from, thePiece-2);
				hashValue ^= pHash[from][thePiece-2];
				hashValue2 ^= pHash2[from][thePiece-2];
				
				if(cP != -1) {
					clearBoard(to);
					hashValue ^= pHash[to][cP];
					hashValue2 ^= pHash2[to][cP];
				}
				break;
			case(Global.PROMO_B):
				drawCount = 0;
				clearBoard(from);
				hashValue ^= pHash[from][thePiece];
				hashValue2 ^= pHash2[from][thePiece];
				setBoard(from, thePiece-3);
				hashValue ^= pHash[from][thePiece-3];
				hashValue2 ^= pHash2[from][thePiece-3];
				
				if(cP != -1) {
					clearBoard(to);
					hashValue ^= pHash[to][cP];
					hashValue2 ^= pHash2[to][cP];
				}
				break;	
			case(Global.PROMO_R):
				drawCount = 0;
				clearBoard(from);
				hashValue ^= pHash[from][thePiece];
				hashValue2 ^= pHash2[from][thePiece];
				setBoard(from, thePiece-5);
				hashValue ^= pHash[from][thePiece-5];
				hashValue2 ^= pHash2[from][thePiece-5];
				
				if(cP != -1) {
					clearBoard(to);
					hashValue ^= pHash[to][cP];
					hashValue2 ^= pHash2[to][cP];
				}
				break;	
			case(Global.PROMO_N):
				drawCount = 0;
				clearBoard(from);
				hashValue ^= pHash[from][thePiece];
				hashValue2 ^= pHash2[from][thePiece];
				setBoard(from, thePiece-4);
				hashValue ^= pHash[from][thePiece-4];
				hashValue2 ^= pHash2[from][thePiece-4];
				
				if(cP != -1) {
					clearBoard(to);
					hashValue ^= pHash[to][cP];
					hashValue2 ^= pHash2[to][cP];
				}
				break;
			
				
			
		}
		
		if(wCastle > Global.CASTLED) {
			if(thePiece == 4)
				wCastle = Global.NO_CASTLE;
			else if(to == 7 || from == 7) {
				if(wCastle == Global.SHORT_CASTLE)
                    wCastle = Global.NO_CASTLE;
                else
                    wCastle = Global.LONG_CASTLE;
			}else if(to == 0 || from == 0) {
                if(wCastle == Global.LONG_CASTLE)
                    wCastle = Global.NO_CASTLE;
                else
                    wCastle = Global.SHORT_CASTLE;
			}		
		}
		if(bCastle > Global.CASTLED) {
			if(thePiece == 10)
				bCastle = Global.NO_CASTLE;
			else if(to == 63 || from == 63) {
                if(bCastle == Global.SHORT_CASTLE)
                    bCastle = Global.NO_CASTLE;
                else
                    bCastle = Global.LONG_CASTLE;
                
                //bCastle &= Global.LONG_CASTLE;
			}else if(to == 56 || from == 56) {
                if(bCastle == Global.LONG_CASTLE)
                    bCastle = Global.NO_CASTLE;
                else
                    bCastle = Global.SHORT_CASTLE;

                //bCastle &= Global.SHORT_CASTLE;
			}		
		}				
		hashValue ^= bCastleHash[bCastle];
		hashValue2 ^= bCastleHash2[bCastle];
		hashValue ^= wCastleHash[wCastle];
		hashValue2 ^= wCastleHash2[wCastle];
	
		
		hashValue ^= pHash[from][piece_in_square[from]];
		hashValue ^= pHash[to][piece_in_square[from]];
		hashValue2 ^= pHash2[from][piece_in_square[from]];
		hashValue2 ^= pHash2[to][piece_in_square[from]];
		updateBoard(to,from);
		
		
		if(board) { 
			reps = repTable.addPosition(hashValue);
		}
		
		if(add) {
			setChanged();
			notifyObservers();
		}
		return reps;
	
	}
	public static final int getDraw() {
		return drawCount;
	}
	public static final int getCount() {
		return moveCount;	
	}	
	public static final int getTurn() {
		return turn;
	}	
	public static final int getValue() {
		return value;
	}
	
	public final void unMake(int move, boolean sub,boolean board) {
		
		if(board)	
			repTable.removePosition(hashValue);
		
		turn = -turn;	
		moveCount--;
		wCastle = (flagHistory[moveCount] >> 12) & 7;
		bCastle = (flagHistory[moveCount] >> 15) & 7;
		drawCount = flagHistory[moveCount] >> 21;
		int to = MoveFunctions.getTo(move);
		int from = MoveFunctions.getFrom(move);
		int piece = MoveFunctions.getPiece(move);
		int capPiece = MoveFunctions.getCapture(move);
		int type = MoveFunctions.moveType(move);	
		
		switch(type) {
			case(Global.ORDINARY_MOVE):
				updateBoard(from, to);
				if(capPiece != -1)
					setBoard(to, capPiece);
				break;
			
			case(Global.PROMO_Q):
				clearBoard(to);
				setBoard(to, piece);
				updateBoard(from, to);
				if(capPiece != -1)
					setBoard(to, capPiece);
				break;
			case(Global.PROMO_B):
				clearBoard(to);
				setBoard(to, piece);
				updateBoard(from, to);
				if(capPiece != -1)
					setBoard(to, capPiece);
				break;	
			case(Global.PROMO_R):
				clearBoard(to);
				setBoard(to, piece);
				updateBoard(from, to);
				if(capPiece != -1)
					setBoard(to, capPiece);
				break;
			case(Global.PROMO_N):
				clearBoard(to);
				setBoard(to, piece);
				updateBoard(from, to);
				if(capPiece != -1)
					setBoard(to, capPiece);
				break;
			case(Global.EN_PASSANT_CAP):
				updateBoard(from, to);
				if(piece == 5) 
					setBoard(to-8, 11);
				else
					setBoard(to+8, 5);
				break;
				
			case(Global.SHORT_CASTLE):
				updateBoard(from, to);
				if(piece == 4) {
					//wHasCastled = false;
					updateBoard(7,5);
				} else {
					//bHasCastled = false;
					updateBoard(63, 61);
				}
				break;
				
			case(Global.LONG_CASTLE):
				updateBoard(from, to);
				if(piece == 4) {
					//wHasCastled = false;
					updateBoard(0, 3);
				} else {
					//bHasCastled = false;
					updateBoard(56, 59);
				}
				break;
		}
	
		hashValue = hashHistory[moveCount];
		hashValue2 = hashHistory2[moveCount];
		passantW = (flagHistory[moveCount] & 63) - 1;
		passantB = ((flagHistory[moveCount] >> 6) & 63) - 1;
		
		if(sub) {
			writer.removeLastHistory();
			setChanged();
			notifyObservers();
		}	
	}	
		
}// End MagnumChess