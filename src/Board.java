/**
 * Board.java
 *
 * Version 3.0   
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


import java.io.File;
import java.util.Random;

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

public final class Board {//extends java.util.Observable{
    
    /** count of all moves made over the board*/
    public int moveCount;					
    
	/** boardMoves is an array of all moves made in the game
     * -To do - switch this to a list as currently if the game goes over 256 moves, there is a crash
     */
    public static final int boardMoves[] = new int[512];
	
    /** array containing occupancy of pieces on the 64 board positions */
    public static final int piece_in_square[] = new int[64];
	
    public static int materialValues[] = new int[9 * 9 * 3 * 3 * 3 * 3 * 3 * 3 * 2 * 2];
    public static int materialKey = 0;
    /** number of white pieces on the board */
    public int wPieceNo;					
	
    /** number of black pieces on the board */
    public int bPieceNo;

    /** value used to adjust material table when 2 queens of a side or both are present */
    private int materialAdjust;

    private int[][] QueenMaterialAdjustArray = new int[8][8];

    /** variables representing the number of white and black queens
    *  used to correct material tables when more than one queen of a colour is present
    * Note - this will not correct the problem exactly, but it is expected that by showing the dominant queen material
    * the search will not suffer
    */
    private int numberWhiteQueens;
    private int numberBlackQueens;

    /** total material on the board */
    public int totalValue;
	
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

    /** The following 64 bit longs represent the bitboards used to
     * represent the occupancy for various pieces on the board
     */
    public long bitboard;
	 public long whitepieces;
	 public long blackpieces;
	 public long whitepawns;
	 public long blackpawns;
	 public long whiteknights;
	 public long blackknights;
	 public long whitebishops;
	 public long blackbishops;
	 public long whiterooks;
	 public long blackrooks;
	 public long whitequeen;
	 public long blackqueen;
	 public long whiteking;
	 public long blackking;
	 public long pawnsKings;
	 public long slidePieces;
	
	 /** castle status variables for each side*/
    public int bCastle, wCastle;
	
    /** this is the occupancy information for the initial chess position */
    private static final int[] init =     {0,1,2,3,4,2,1,0,
                                          5,5,5,5,5,5,5,5,
                                          -1,-1,-1,-1,-1,-1,-1,-1,
                                          -1,-1,-1,-1,-1,-1,-1,-1,
                                          -1,-1,-1,-1,-1,-1,-1,-1,
                                          -1,-1,-1,-1,-1,-1,-1,-1,
                                          11,11,11,11,11,11,11,11,
                                          6,7,8,9,10,8,7,6};
    
	
	
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
		
	private long Board45L ;
	private long Board45R;			
	private long Board90R;
	
    
    /** these arrays are used to mask off and generate moves using magic bitboard move generation */
   private static final int bishopShift[] = new int[64];			//size of move database for each square
	private static final int rookShift[] = new int[64];			//size of move database for each square
   private static final long bMask[] = new long[64];			//bishop occupancy masks
	private static final long rMask[] = new long[64];			//rook occupancy masks
   private static final long bMagics[] = new long[64];			//64 9-bit bishop magic numbers
	private static final long rMagics[] = new long[64];			//64 rook magic numbers
	private static final long bishopTable[][] = new long[64][];	//9 bit bishop table of moves
	private static final long rookTable[][] = new long[64][];		//rook table of moves
    
    private int flagHistory[] = new int[512];
	
	/** enPassant capture squares for each side */
    private int passantW, passantB;			
	
    /** 50 move draw count variable */
    private int drawCount;						
	
    /** -1 white to move, 1 black moves */
    private int turn;							
	
     /** 64 bit represent the hash code for each position*/
    public long hashValue;

    /** hash values for all 12 pieces on all 64 squares
     * note this is optimized for 32 bit computers
     */
    private static final long[][] pHash = new long[64][12];
	
    /** these are the hash values for the status of each sides castling */
    private static final long wCastleHash[] = new long[8];
	 private static final long bCastleHash[] = new long[8];
	
    /** side to move hash value */
    private long bHashMove;						//hash for when black is to move
	
    /** hash value for each passant square */
    private static final long[] passantHashW = new long[9];				//hash for passant squares
	 private static final long[] passantHashB = new long[9];
	
    /*
     * These two constants are the values for the passant squares when there is no actual passant square
     * There is a clever/idiot trick I thought of using the passant square % 9 to index the has for the passant square
     * Plus, these values will never cause a false attack...ex/  black pawns never attack sq 51 and white pawns never attack 3
     */
    private static final int NO_PASSANT_WHITE = 51;
    private static final int NO_PASSANT_BLACK = 3;

    /** hash history arrays stores the previous hash as each move is made
     * note - this should be changed to a list or vector as after 256 moves, crash
     */
    private long[] hashHistory = new long[512];
    
    /** hash value for the pawns on the board
     * used for seperate pawn hash table
     */
    private long pawnHash;
	
    /** HistoryWrite object is used to convert moves to and from algebraic notation and fen notation */
    private HistoryWriter writer;					//stores and writes the history of all moves made
	
    /** Array of values representing the number of squares a queen would traverse
     * between any two squares on the board
     */
    private static final int[][] queenDist = new int[64][64];
	
    /** variables used for extraction of set bit in a bitboard, using the debruijn technique */
    private static final int[] index32 = new int[32];
	 
    private static final int debruijn = 0x077CB531;
    
    /** instance of singleton MoveHelper Object used to store move info in a compact form */
    private MoveHelper Helper = MoveHelper.getInstance();
    
    /** zorbist key history array -- used for repetition detection */
    private static final long[] zorbistHistory = new long[202];
    
    /** array containing index of last reversable move -- used for repetition detection */
    private static final int[] lastReversableMove = new int[202];
    
    /** index into zorbistHistory array -- used for repetition detection */
    private int zorbistDepth;

    /** flag to indicate if the last move made was reversable or not */
    private boolean reversable = true;

    /** variables used to store the current line of moves from the root of the search **/
    private static final int[] arrCurrentMoves = new int[64];
    private int iCurrentMovesDepth;

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
		
      for(int i=0;i<32;i++) {
			index32[(debruijn<<i)>>>27] = i;
		}	
		writer = new HistoryWriter();
		initQueenDist();
		setHash();
		for(int i=0;i<15;i++) 
			Global.diag1Masks[i] = 0;
		for(int j=0;j<64;j++) {
			int temp = Global.Diag1Groups[j];
			Global.diag1Masks[temp] |=(long)1<<j;
		}
		for(int i=0;i<15;i++) 
			Global.diag2Masks[i] = 0;
		for(int j=0;j<64;j++) {
			int temp = Global.Diag2Groups[j];
			Global.diag2Masks[temp] |= (long)1<<j;
		}	
        for(int i=0;i<64;i++) {
			Global.set_Mask[i] = (long)1<<i;
		}	
		for(int i=0;i<8;i++) {
			Global.fileMasks[i] = 0;
			for(int j=0;j<64;j++) {
				if(j%8==i)
					Global.fileMasks[i] |= (long)1<<j;
			}
		}
		for(int i=0;i<8;i++) {
			Global.rankMasks[i]=0;
			for(int j=0;j<64;j++) {
				if(j/8==i) {
					Global.rankMasks[i] |= (long)1<<j;
                }
            }
		}
		for (int i = 0; i < 64; i++) {
			Global.plus9[i] = 0;
			for(int j = i + 9; j < 64; j += 9)	{
				if(j % 8 == 0) break;
				Global.plus9[i] |= Global.set_Mask[j];
			}
		}
		for(int i = 0; i < 64; i++) {
			Global.minus9[i] = 0;
			for(int j = i - 9; j >= 0; j -= 9) {
				if(j % 8 == 7) break;
				Global.minus9[i] |= Global.set_Mask[j];
			}
		}
		for( int i = 0; i < 64; i++) {
			Global.plus7[i] = 0;
			for(int j = i + 7; j < 64; j += 7) {
				if(j % 8 == 7) break;
				Global.plus7[i] |= Global.set_Mask[j];
			}	
		}
		for(int i = 0; i < 64; i++) {
			Global.minus7[i] = 0;
			for(int j = i - 7; j >= 0; j -= 7) {
				if(j % 8 == 0) break;
				Global.minus7[i] |= Global.set_Mask[j];
			}
		}
		for( int i = 0; i < 64; i++) {
			Global.plus8[i] = 0;
			for(int j = i + 8; j < 64; j += 8) {
				Global.plus8[i] |= Global.set_Mask[j];
			}
		}
		for (int i = 0; i < 64; i++) {
			Global.minus8[i] = 0;
			for(int j = i - 8; j >= 0; j -= 8) {
				Global.minus8[i] |= Global.set_Mask[j];
			}
		}
		for( int i = 0; i < 64; i++) {
			Global.plus1[i] = 0;
			for(int j = i+1; j < 64; j++) {
				if(j%8 == 0) break;
				Global.plus1[i] |= Global.set_Mask[j];
			}	
		}
		for( int i = 0; i < 64; i++) {
			Global.minus1[i] = 0;
			for(int j = i - 1; j >= 0; j -= 1) {
				if(j % 8 == 7) break;
				Global.minus1[i] |= Global.set_Mask[j];
			}
		}
     
       for(int i=8; i<64; i++) {
            int file = i%8;
            if(file == 0) 
                Global.whitePassedPawnMasks[i] = Global.plus8[i] | Global.plus8[i+1];
             else  if (file == 7) 
                Global.whitePassedPawnMasks[i] = Global.plus8[i] | Global.plus8[i-1];
             else 
                Global.whitePassedPawnMasks[i] = Global.plus8[i] | Global.plus8[i+1] | Global.plus8[i-1];
        }
        for(int i=55 ; i>=0; i--) {
            int rank = i%8;
            if(rank == 0)
                Global.blackPassedPawnMasks[i] = Global.minus8[i] | Global.minus8[i+1];
            else if(rank == 7)
                Global.blackPassedPawnMasks[i] = Global.minus8[i] | Global.minus8[i-1];
            else
                Global.blackPassedPawnMasks[i] = Global.minus8[i] | Global.minus8[i+1] | Global.minus8[i-1];
        }
        
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
		
      setBishopMasks();
		setBishopMagics();
		setRookMasks();
		setRookMagics();
		populateBishopTables();
		populateRookTables();
      zorbistDepth = 1;
      lastReversableMove[0] = 1;
      lastReversableMove[1] = 1;
      InitKnightMoveBoard(KnightMoveBoard);
      InitWhitePawnMoveBoard(WhitePawnMoveBoard);
		InitWhitePawnAttackBoard(WhitePawnAttackBoard);
		InitBlackPawnMoveBoard(BlackPawnMoveBoard);
		InitBlackPawnAttackBoard(BlackPawnAttackBoard);
      InitializeMaterialArray();
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
     *  method undoAll
     * 
     * un-does every move in game
     */
    
	public final void undoAll(){
		int noMoves = moveCount;
		for(int i=noMoves-1;i>=0;i--) {
			UnMake(boardMoves[i] ,true);
         writer.removeLastHistory();
		}
      zorbistDepth = 1;
      iCurrentMovesDepth = 0;
	}
    
    
    
    /** 
     *  method newGame
     * 
     * sets the board up for a new game
     */
    public final void newGame() {
		materialKey = 0;
      materialAdjust = 0;
      numberWhiteQueens = 0;
      numberBlackQueens = 0;
      zorbistDepth = 1;
      blackpieces = 0;
		whitepieces = 0;
		moveCount = 0;
		drawCount = 0;
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
		pawnsKings = 0;
		wPieceNo = 0;
		bPieceNo = 0;
      iCurrentMovesDepth = 0;
		for(int i=0;i<64;i++) {										//init empty squares to have -1 value
			piece_in_square[i] = -1;
		}
		bCastle = Global.BOTH_CASTLE;
		wCastle = Global.BOTH_CASTLE;
		passantW = NO_PASSANT_WHITE;
		passantB = NO_PASSANT_BLACK;
		hashValue = 0;
		pawnHash = 0;
		for(int i=0;i<64;i++) {
			if(init[i]!=-1) {
				setBoard(i,init[i]);
				hashValue ^= pHash[i][init[i]];
			}	
		}
		hashValue ^= bCastleHash[bCastle];
		hashValue ^= wCastleHash[wCastle];
		Engine.resetHash();
		Evaluation2.clearEvalHash();
		Evaluation2.clearPawnHash();
		writer.reset();	
	}	
    

    private final void InitializeMaterialArray() {

      int noReps = 0;

      QueenMaterialAdjustArray[2][0] = 2 * -Global.values[3] - Global.values[9];
      QueenMaterialAdjustArray[2][1] = -Global.values[3] + Global.values[5];
      QueenMaterialAdjustArray[2][2] = -Global.values[9] + Global.values[5];
      QueenMaterialAdjustArray[1][2] = 2 * Global.values[9] + Global.values[5];
      QueenMaterialAdjustArray[0][2] = 2 * Global.values[9] + Global.values[5];

      for(int wRook = 0; wRook < 3; wRook++) {

         for(int bRook = 0; bRook < 3; bRook++) {

            for(int wBishop = 0; wBishop < 3; wBishop++) {

               for(int bBishop = 0; bBishop < 3; bBishop++) {

                  for(int wKnight = 0; wKnight < 3; wKnight++) {

                     for(int bKnight = 0; bKnight < 3; bKnight++) {

                        for(int wQueen = 0; wQueen < 2; wQueen++) {

                           for(int bQueen = 0; bQueen < 2; bQueen++) {
 
                              for(int wPawn = 0; wPawn < 9; wPawn++) {

                                 for(int bPawn = 0; bPawn < 9; bPawn++) {

                                    noReps++;
                                    int noWhitePieces = wQueen + wRook + wBishop + wKnight + wPawn;
                                    int noBlackPieces = bQueen + bRook + bBishop + bKnight +  bPawn;

                                    int noWhiteMinors = wBishop + wKnight;
                                    int noBlackMinors = bBishop + bKnight;

                                    int index = wRook +
                                                   bRook   * 3 +
                                                   wBishop * 3 * 3 +
                                                   bBishop * 3 * 3 * 3 +
                                                   wKnight * 3 * 3 * 3 * 3 +
                                                   bKnight * 3 * 3 * 3 * 3 * 3 +
                                                   wQueen  * 3 * 3 * 3 * 3 * 3 * 3 +
                                                   bQueen  * 3 * 3 * 3 * 3 * 3 * 3 * 2 +
                                                   wPawn   * 3 * 3 * 3 * 3 * 3 * 3 * 2 * 2 +
                                                   bPawn   * 3 * 3 * 3 * 3 * 3 * 3 * 2 * 2 * 9;


                                       //+ values for back, -ve for white
                                        /** recognize some draw situations */
                                       int materialImbalence = 0;  
                                       
                                       
                  if(noWhitePieces <= 1 && noBlackPieces <= 1 && noWhiteMinors == noWhitePieces && noBlackMinors == noBlackPieces) //kings and 1 or less minors draw
                     materialImbalence = Global.materialDraw;
                  else if(noWhitePieces <= 2 && noBlackPieces == 2 && noBlackPieces == bKnight && noWhitePieces == noWhiteMinors && wBishop != 2)  //2 black knights against 2 or less minors
                     materialImbalence = Global.materialDraw;
                  else if(noBlackPieces <= 2 && noWhitePieces == 2 && noWhitePieces == bKnight && noBlackPieces == noBlackMinors && bBishop != 2)  //2 white knights against 2 or less minors
                     materialImbalence = Global.materialDraw;
                  else if(noWhitePieces == 1 && wPawn == 1 && noBlackPieces == 1 && noBlackPieces == noBlackMinors)  //1 white pawn against a minor
                     materialImbalence = Global.materialDraw;
                  else if(noBlackPieces == 1 && bPawn == 1 && noWhitePieces == 1 && noWhitePieces == noWhiteMinors)  //1 black pawn against a minor
                     materialImbalence = Global.materialDraw;
                  else if(noWhiteMinors == 2 && noWhitePieces == 2 && noWhitePieces != wBishop && noBlackPieces == 1 && noBlackMinors == 1)  //white has 2 minors, black has 1, white doesn't have bishop pair
                     materialImbalence = Global.materialDraw;
                  else if(noBlackMinors == 2 && noBlackPieces == 2 && noBlackPieces != bBishop && noWhitePieces == 1 && noWhiteMinors == 1)  //black has 2 minors, white has 1, black doesn't have bishop pair
                     materialImbalence = Global.materialDraw;
                  else if(noBlackPieces == 2 && noBlackPieces == noBlackMinors && noBlackPieces != bBishop && noWhitePieces == 1 && wRook == 1) //kbn vs KR
                     materialImbalence = Global.materialDraw;
                  else if(noWhitePieces == 2 && noWhitePieces == noWhiteMinors && noWhitePieces != wBishop && noBlackPieces == 1 && bRook == 1) //KBN vs kr
                     materialImbalence = Global.materialDraw;


                                       else {
                                          materialImbalence = -Global.values[3] * wQueen +
                                                               Global.values[9] * bQueen -
                                                               Global.values[0] * wRook +
                                                               Global.values[6] * bRook -
                                                               Global.values[2] * wBishop +
                                                               Global.values[8] * bBishop -
                                                               Global.values[1] * wKnight +
                                                               Global.values[7] * bKnight -
                                                               Global.values[5] * wPawn +
                                                               Global.values[11] * bPawn;
                                       

                                       if(wBishop == 2 && bBishop < 2)            //apply bishop pair bonus
                                          materialImbalence -= 50;
                                       else if(bBishop == 2 && wBishop < 2)
                                          materialImbalence += 50;

                                       //penalty for 3 pawns for a minor piece trade
                                       if(wPawn - bPawn >= 3 && noBlackMinors - noWhiteMinors > 0  )
                                          materialImbalence += 75;
                                       else if(bPawn - wPawn >= 3 && noWhiteMinors - noBlackMinors > 0  )
                                          materialImbalence -= 75;
                                      

                                       //penalty for rook and pawn for two minors
                                       if(wPawn - bPawn >= 1 && (wRook - bRook) == 1 && (noBlackMinors - noWhiteMinors) == 2  )
                                          materialImbalence += 25;
                                       else if(bPawn - wPawn >= 1 && (bRook - wRook) == 1 && (noWhiteMinors - noBlackMinors) == 2  )
                                          materialImbalence -= 25;

                                      //adjustment for queen vs rook, minor and pawn
                                      if(((wQueen - bQueen) == 1) && ((bRook - wRook) == 1) && ((noBlackMinors - noWhiteMinors) == 1) && ((bPawn - wPawn) == 1))
                                         materialImbalence -= 75;
                                      else if(((bQueen - wQueen) == 1) && ((wRook - bRook) == 1) && ((noWhiteMinors - noBlackMinors) == 1) && ((wPawn - bPawn) == 1))
                                         materialImbalence += 75;
                                     }

                                       materialValues[index] = materialImbalence;

                                    }

                                  }
                             }
                        }
                    }
                 }
               }
            }
          }
       }
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
     *  method getDistance
     * 
     * Returns the distance between two squres
     * @param to 
     * @param from 
     * 
     */
	public final  int getDistance(int to,int from) {
		return queenDist[to][from];
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
     *  method callFileWrite
     * 
     * writes all moves to a file
     * 
     *  @ param File f - the file to write to
     */
	public  final void callFileWrite(File f) {
		writer.historyToFile(f);
	}	
    
	/** 
     *  method callFileRead
     * 
     * reads in all moves from a file
     * 
     *  @ param File f - the file to read from
     */
    public  final void callFileRead(File f) {
		writer.readHistory(f);
	}	
    
    /** 
     *  method acceptFen
     * 
     * reads in a fen string which represents a chess position
     * this position is loaded 
     * 
     *  @ param String fen - fen position to load
     */
    public  void acceptFen(String fen) {
        String rank;
        
        /**reseting board variables  */
        materialKey = 0;
        materialAdjust = 0;
        numberWhiteQueens = 0;
        numberBlackQueens = 0;
        blackpieces = 0;
        whitepieces = 0;
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
        wPieceNo = 0;
        bPieceNo = 0;

        /**clear the board */
        for(int i=0;i<64;i++) {		
            piece_in_square[i] = -1;
        }
        bCastle = Global.NO_CASTLE;
        wCastle = Global.NO_CASTLE;
        passantW = NO_PASSANT_WHITE;
        passantB = NO_PASSANT_BLACK;
        hashValue = 0;
        pawnHash = 0;

        /** now process the fen string where it contains the placement of pieces */
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
                        count--;
                        break;
                    case('n'):
                        setBoard(count,7);
                        hashValue ^= pHash[count][7];
                        count--;
                        break;
                    case('b'):
                        setBoard(count,8);
                        hashValue ^= pHash[count][8];
                        count--;
                        break;
                    case('q'):
                        setBoard(count,9);
                        hashValue ^= pHash[count][9];
                        count--;
                        break;
                    case('k'):
                        setBoard(count,10);
                        hashValue ^= pHash[count][10];
                        count--;
                        break;
                    case('p'):
                        setBoard(count,11);
                        hashValue ^= pHash[count][11];
                        count--;
                        break;
                    case('R'):
                        setBoard(count,0);
                        hashValue ^= pHash[count][0];
                        count--;
                        break;
                    case('N'):
                        setBoard(count,1);
                        hashValue ^= pHash[count][1];
                        count--;
                        break;
                    case('B'):
                        setBoard(count,2);
                        hashValue ^= pHash[count][2];
                        count--;
                        break;
                    case('Q'):
                        setBoard(count,3);
                        hashValue ^= pHash[count][3];
                        count--;
                        break;
                    case('K'):
                        setBoard(count,4);
                        hashValue ^= pHash[count][4];
                        count--;
                        break;
                    case('P'):
                        setBoard(count,5);
                        hashValue ^= pHash[count][5];
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

        /** now process the side to move information */
        char c = fen.charAt(0);
        fen = fen.substring(fen.indexOf(" ")+1);
        if(c == 'w') {
            turn = -1;
        } else {
            turn = 1;
        }
        if(turn==1) {
			hashValue ^= bHashMove;
		 }
        /** now process the castling rights */
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

        /**process the passant square
        *get the first character - if it is a '-', then no passant square
        */
        c = fen.charAt(0);
        if(c != '-') {
            token = fen.substring(0,fen.indexOf(" "));
            int pSq = HistoryWriter.getNumericPosition(token);
            if(turn == -1)                  //white moving
                passantB = pSq;
            else
                passantW = pSq;
        }
        fen = fen.substring(fen.indexOf(" "));

        /** now process the drawCount */
        fen = fen.substring(fen.indexOf(" ")+1); 
        token = fen.substring(0,fen.indexOf(" "));    
        Integer noMoves = new Integer(token);
        drawCount = noMoves.intValue();

        /** now process the moveCount
        *   Note there are problems with this part of the fen reader
        *   To Do...change internal representation of number of moves
        * - fix unmaking moves based on this the no of moves
        * - currently will only work for no of moves at 1
        */
        
        fen = fen.substring(fen.indexOf(" ")+1); 
        token = fen;
        noMoves = new Integer(token);
        moveCount = noMoves.intValue()-1;

        /** set the has values for the recently set castling rights */
        hashValue ^= bCastleHash[bCastle];
        hashValue ^= wCastleHash[wCastle];
    }
	
    /** 
     *  method getPawnHash
     * 
     * returns the first 32 bits of the current pawn hash code
     * 
     */
	public  final long getPawnHash() {
		return pawnHash;
	}
	
    /** 
     *  method getHash
     * 
     * returns the first 32 bits of the current hash code
     * 
     */
    public  final long getHash() {
		return hashValue;
	}
	
	/** 
     *  method generateHash()
     * 
     * generates the first 32 bits of the hash code from scratch
     * 
     * can be used to debug and verify the hash is correct during the search
     * 
     */
    public final long generateHash() {
		long temp2;
		long temp = bitboard;
		long hash = 0;
		while(temp!=0) {
			temp2 = temp&-temp;
			int pos = Long.numberOfTrailingZeros(temp2);
			temp&=~temp2;
	
			hash ^=pHash[pos][piece_in_square[pos]];
		}			
		hash ^= passantHashW[passantW%9];
		hash ^= passantHashB[passantB%9];
		
		hash ^= bCastleHash[bCastle];
		hash ^= wCastleHash[wCastle];
	
		if(turn==1) {
			hash^=bHashMove; 
		}
	return hash;
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
		
		rand = new Random(80392848);
		for(i=0;i<64;i++) {
			for(j=0;j<12;j++) {
				pHash[i][j] = rand.nextLong() & Long.MAX_VALUE;
			}
		}
	
		/**set castle hashes and enPassant hashes */
		for(i=0;i<9;i++) {
			passantHashW[i] = rand.nextLong() & Long.MAX_VALUE;
			passantHashB[i] = rand.nextLong() & Long.MAX_VALUE;
		}				
					
		bHashMove = rand.nextLong() & Long.MAX_VALUE;
			
		for(i=0;i<8;i++) {
			bCastleHash[i] = rand.nextLong() & Long.MAX_VALUE;
			wCastleHash[i] = rand.nextLong() & Long.MAX_VALUE;
		}
				
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
     /** 
     *  method setBoard
     * 
     * This method places a piece on a square
     * 
     * @param int i - the square 
     * @param int piece - the piece to place on the board
     * 
     */
	public  final void setBoard(int i,int piece) {
		bitboard |= Global.set_Mask[i];
		piece_in_square[i] = piece;
		switch(piece) {
			case 0:
				wPieceNo++;
				whitepieces |= Global.set_Mask[i];
				whiterooks |= Global.set_Mask[i];
				materialKey += Global.materialOffset[piece];
            totalValue +=Global.values[piece];
				break;
			case 1:
				wPieceNo++;
				whitepieces |= Global.set_Mask[i];
				whiteknights |= Global.set_Mask[i];
				materialKey += Global.materialOffset[piece];
            totalValue +=Global.values[piece];
				break;
			case 2:
				wPieceNo++;
				whitepieces |= Global.set_Mask[i];
				whitebishops |= Global.set_Mask[i];
				materialKey += Global.materialOffset[piece];
            totalValue +=Global.values[piece];
				break;
			case 3:
				wPieceNo++;
				materialAdjust = QueenMaterialAdjustArray[++numberWhiteQueens][numberBlackQueens];
				whitepieces |= Global.set_Mask[i];
				whitequeen |= Global.set_Mask[i];
				materialKey += Global.materialOffset[piece];
            totalValue +=Global.values[piece];
				break;
			case 4:
				wPieceNo++;
				whitepieces |= Global.set_Mask[i];
				whiteking |= Global.set_Mask[i];
				pawnsKings |= Global.set_Mask[i];	
				break;
			case 5:
				wPieceNo++;
				whitepieces |= Global.set_Mask[i];
				whitepawns |= Global.set_Mask[i];
				pawnsKings |= Global.set_Mask[i];
				materialKey += Global.materialOffset[piece];
            totalValue +=Global.values[piece];
				pawnHash ^= pHash[i][piece];
				break;			
			case 6:
				bPieceNo++;
				blackpieces |= Global.set_Mask[i];
				blackrooks |= Global.set_Mask[i];
				materialKey += Global.materialOffset[piece];
            totalValue +=Global.values[piece];
				break;
			case 7:
				bPieceNo++;
				blackpieces |= Global.set_Mask[i];
				blackknights |= Global.set_Mask[i];
				materialKey += Global.materialOffset[piece];
            totalValue +=Global.values[piece];
				break;
			case 8:
				bPieceNo++;
				blackpieces |= Global.set_Mask[i];
				blackbishops |= Global.set_Mask[i];
				materialKey += Global.materialOffset[piece];
            totalValue +=Global.values[piece];
				break;
			case 9:
				bPieceNo++;
				materialAdjust = QueenMaterialAdjustArray[numberWhiteQueens][++numberBlackQueens];
				blackpieces |= Global.set_Mask[i];
				blackqueen |= Global.set_Mask[i];	
				materialKey += Global.materialOffset[piece];
            totalValue +=Global.values[piece];
				break;
			case 10:
				bPieceNo++;
				blackpieces |= Global.set_Mask[i];
				blackking |= Global.set_Mask[i];
				pawnsKings |= Global.set_Mask[i];
				break;
			case 11:
				bPieceNo++;
				blackpieces |= Global.set_Mask[i];
				blackpawns |= Global.set_Mask[i];
				pawnsKings |= Global.set_Mask[i];
				materialKey += Global.materialOffset[piece];
            totalValue +=Global.values[piece];
				pawnHash ^= pHash[i][piece];
				break;
		}
	}
	
	/***********************************************************************
		Name:		updateBoard
		Parameters:	int, String
		Returns:	None
		Description:This method updates all of the boards so that on index
					int there exists piece String
	***********************************************************************/	
	private  final void updateBoard(int i,int j) {
		//long bit = Global.set_Mask[i]|Global.set_Mask[j];
		long bit = (long)1 << i | (long)1 << j;
      bitboard ^= bit;
		int piece = piece_in_square[j];
		piece_in_square[i] = piece;
		piece_in_square[j] = -1;
		switch(piece) {
			case(0):		
				whitepieces ^= bit;
				whiterooks ^= bit;
				break;
			case(1):
				whitepieces ^= bit;
				whiteknights ^= bit;
				break;
			case(2):
				whitepieces ^= bit;
				whitebishops ^= bit;
				break;
			case(3):
				whitepieces ^= bit;
				whitequeen ^= bit;
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
				pawnHash ^= pHash[j][piece];
				pawnsKings ^= bit;
				break;
			case(6):
				blackpieces ^= bit;
				blackrooks ^= bit;
				break;
			case(7):
				blackpieces ^= bit;
				blackknights ^= bit;
				break;
			case(8):
				blackpieces ^= bit;
				blackbishops ^= bit;
				break;
			case(9):
				blackpieces ^= bit;
				blackqueen ^= bit;
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
				pawnHash ^= pHash[j][piece];
				pawnsKings ^= bit;
				break;
		}
	}
    
	/***********************************************************************
		Name:		clearBoard
		Parameters:	int, String
		Returns:	None
		Description:On index i String s is removed by this method
	***********************************************************************/
	public  final void clearBoard(int i) {
		bitboard ^= Global.set_Mask[i];
		int piece = piece_in_square[i];
		switch(piece) {
			case 0:
				wPieceNo--;
				whitepieces ^= Global.set_Mask[i];
				whiterooks ^= Global.set_Mask[i];
				materialKey -= Global.materialOffset[piece];
            totalValue -=Global.values[piece];
				break;
			case 1:
				wPieceNo--;
				whitepieces ^= Global.set_Mask[i];
				whiteknights ^= Global.set_Mask[i];
				materialKey -= Global.materialOffset[piece];
            totalValue -=Global.values[piece];
				break;
			case 2:
				wPieceNo--;
				whitepieces ^= Global.set_Mask[i];
				whitebishops ^= Global.set_Mask[i];
				materialKey -= Global.materialOffset[piece];
            totalValue -=Global.values[piece];
				break;
			case 3:
				wPieceNo--;
            materialAdjust = QueenMaterialAdjustArray[--numberWhiteQueens][numberBlackQueens];
				whitepieces ^= Global.set_Mask[i];
				whitequeen ^= Global.set_Mask[i];
				materialKey -= Global.materialOffset[piece];
            totalValue -=Global.values[piece];
				break;
			case 4:
				wPieceNo--;
				whitepieces ^= Global.set_Mask[i];
				whiteking ^= Global.set_Mask[i];
				pawnsKings ^= Global.set_Mask[i];
				break;
			case 5:
				wPieceNo--;
				whitepieces ^= Global.set_Mask[i];
				whitepawns ^= Global.set_Mask[i];
				pawnsKings ^= Global.set_Mask[i];
				materialKey -= Global.materialOffset[piece];
            totalValue -=Global.values[piece];
				pawnHash ^= pHash[i][piece];
				break;		
			case 6:
				bPieceNo--;
				blackpieces ^= Global.set_Mask[i];
				blackrooks ^= Global.set_Mask[i];
				materialKey -= Global.materialOffset[piece];
            totalValue -=Global.values[piece];
				break;
			case 7:
				bPieceNo--;
				blackpieces ^= Global.set_Mask[i];
				blackknights ^= Global.set_Mask[i];
				materialKey -= Global.materialOffset[piece];
            totalValue -=Global.values[piece];
				break;
			case 8:
				bPieceNo--;
				blackpieces ^= Global.set_Mask[i];
				blackbishops ^= Global.set_Mask[i];
				materialKey -= Global.materialOffset[piece];
            totalValue -=Global.values[piece];
				break;
			case 9:
				bPieceNo--;
            materialAdjust = QueenMaterialAdjustArray[numberWhiteQueens][--numberBlackQueens];
				blackpieces ^= Global.set_Mask[i];
				blackqueen ^= Global.set_Mask[i];
				materialKey -= Global.materialOffset[piece];
            totalValue -=Global.values[piece];
				break;	
			case 10:
				bPieceNo--;
				blackpieces ^= Global.set_Mask[i];
				blackking ^= Global.set_Mask[i];
				pawnsKings ^= Global.set_Mask[i];
				break;	
			case 11:
				bPieceNo--;
				blackpieces ^= Global.set_Mask[i];
				blackpawns ^= Global.set_Mask[i];
				pawnsKings ^= Global.set_Mask[i];
				materialKey -= Global.materialOffset[piece];
            totalValue -=Global.values[piece];
				pawnHash ^= pHash[i][piece];
				break;
		}	
		piece_in_square[i] = -1;
		
	}
	
    /** 
     *  method getMaterialScore
     * 
     * This method returns the material evaluation from the perspective of a certain side
     * The value Global.materialDraw is returned to flag draw scores, thus this method is only to be called during evaluation
     * 
     * @param int side - the side whose perspective the evaluation is for (-1 white, 1 black)
     * 
     */
    public  final int GetRawMaterialScore() {
         return materialValues[materialKey] + materialAdjust; 
	}

    /**
     *  method getMaterialScore
     *
     * This method returns the material evaluation from the perspective of a certain side
     * The value Global.materialDraw is returned to flag draw scores, thus this method is only to be called during evaluation
     *
     * @param int side - the side whose perspective the evaluation is for (-1 white, 1 black)
     *
     */
    public  final int GetMaterialScore(int side) {
		 if((materialValues[materialKey] == Global.materialDraw))
          return 0;
       else
          return side * (materialValues[materialKey] + materialAdjust);
	}
	
    /** 
     *  method getTotalValue
     * 
     * This method returns the total material value on the board
     *
     */
    public  final int getTotalValue() {
		return totalValue;
	}

    /** 
     *  method Long.numberOfTrailingZeros
     * 
     * This method will return a board position given a proper long using the debruijn technique
     * 
     * @param long pos - the position represented by 1 set bit in the 64 bit long variable
     *
     */
	public int getPos(long pos) {
      return Long.numberOfTrailingZeros(pos);
   }

    /** 
     *  method getAttackBoard
     * 
     * This method will return a bitset repesenting all squares attacked by the piece on a give square
     * 
     * @param int i - the position where the attacking piece is 
     * 
     * @return long - the bitset representing all attacked sqaures
     *
     */
    public  final long getAttackBoard(int i) {
        switch(piece_in_square[i]) {
            case 0:
                return getMagicRookMoves(i);
            case 1:
                return KnightMoveBoard[i];
            case 2:
                return getMagicBishopMoves(i);	
            case 3:
                return getQueenMoves(i);
            case 4:
                return Helper.getKingPosition(i);
            case 5:
                return WhitePawnAttackBoard[i];
            case 6:
                return getMagicRookMoves(i);
            case 7:
                return KnightMoveBoard[i];
            case 8:
                return getMagicBishopMoves(i);
            case 9:
                return getQueenMoves(i);
            case 10:
                return Helper.getKingPosition(i);
            case 11:
                return BlackPawnAttackBoard[i];
        } 
        return 0;
    }

    
	/** 
     *  method getAttack2(int i)
     * 
     * This method will a bitset representing all pieces which attack a given square
     * 
     * @param int i - the position of the square being attacked
     * 
     * @return long - the bitset representing pieces attacking the square
     *
     */	
    public  final long getAttack2(int i) {
        long attack = getMagicBishopMoves(i) & (whitebishops|blackbishops|whitequeen|blackqueen);
        attack |= getMagicRookMoves(i) & (whiterooks|blackrooks|whitequeen|blackqueen);
        attack |= KnightMoveBoard[i] & (whiteknights|blackknights);
        attack |= Helper.getKingPosition(i) & (whiteking|blackking);
        attack |= WhitePawnAttackBoard[i] & blackpawns;
        attack |= BlackPawnAttackBoard[i] & whitepawns;
        return attack;
    }	
    
    /** 
     *  method getMoves2(int i)
     * 
     * This method will a bitset representing all pieces which can move to a square
     * (similar to getAttack2, but no king moves generated here, and pawn pushes, not attacks generated here)
     * (used to generate moves to block an attack on a king)
     * 
     * @param int i - the position of the square to move to
     * 
     * @return long - the bitset representing pieces moving to the square
     *
     */	
    public  final long getMovesTo(int i) {
        long temp = getMagicBishopMoves(i);
        long movers = temp&(whitebishops|blackbishops|whitequeen|blackqueen);
        temp = getMagicRookMoves(i);
        movers |= temp&(whiterooks|blackrooks|whitequeen|blackqueen);
        temp = KnightMoveBoard[i];
        movers |= temp&(whiteknights|blackknights);
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

    /** 
     *  method isWhiteAttacked(int i)
     * 
     * This method determine if a given square occupied by a white piece is attacked
     * 
     * @param int i - the position of the square with the white piece
     * 
     * @return boolean - is the square attacked?
     *
     */	
    public  final boolean isWhiteAttacked(int i) {
      if((WhitePawnAttackBoard[i] & blackpawns) != 0L) return true;
      else if((getMagicBishopMoves(i) & (blackbishops | blackqueen)) != 0L) return true;
      else if((getMagicRookMoves(i) & (blackrooks | blackqueen)) != 0L) return true;
      else if((KnightMoveBoard[i] & blackknights) != 0L) return true;
      else if((Helper.getKingPosition(i) & blackking) != 0L) return true;
      else return false;
    }
 
    /** 
     *  method isBlackAttacked(int i)
     * 
     * This method determines if a given square occupied by a black piece is attacked
     * 
     * @param int i - the position of the square with the black piece
     * 
     * @return boolean - is the square attacked?
     *
     */	
    public  final boolean isBlackAttacked(int i) {
      if((BlackPawnAttackBoard[i] & whitepawns) != 0L) return true;
      else if((getMagicBishopMoves(i) & (whitebishops | whitequeen)) != 0L) return true;   
      else if((getMagicRookMoves(i) & (whiterooks | whitequeen)) != 0L) return true;
      else if((KnightMoveBoard[i] & whiteknights) != 0L) return true;
      else if((Helper.getKingPosition(i) & whiteking) != 0L) return true;
      else return false;
    }	

	/***********************************************************************
		Name:		getPiecesInSquare
		Parameters:	None
		Returns:	int[]
		Description:This method returns an integer array representing the 
					status of the chessboard
     * array entries contain no from 0 to 11 for pieces, -1 means no piece
	***********************************************************************/	
	public  final int[] getPiecesInSquare() {	
		return piece_in_square;
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
				bit = Global.set_Mask[index];
				bit &= board;
				
				if(((bit>>index)&1)==1) {
					rotate+=Global.set_Mask[i];
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
				bit = (long)1<<index;
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
				bit = Global.set_Mask[i];
				bit &=board;
				if(((bit>>i)&1)==1) {
					rotate+=Global.set_Mask[R90Update[i]];
				}
			}
	return rotate;
	}
	 
    /** 
     *  method getPassantW()
     * 
     * This accessor method returns white's passant square
     * 
     * @return int - passant sqaure position
     *
     */	
	public  final int getPassantW() {			//this method gets whtie passant square
		return passantW;
	}
	
    /** 
     *  method getPassantB()
     * 
     * This accessor method returns black's passant square
     * 
     * @return int - passant sqaure position
     *
     */	
    public  final int getPassantB() {			//this method gets black passant square
		return passantB;
	}	
	
    /** 
     *  method whiteHasCastled()
     * 
     * This method returns whether or not white has castled
     * 
     * @return boolean - has white castled?
     *
     */	
	public final boolean whiteHasCastled() {
       return(wCastle==Global.CASTLED);
	}
    
    /** 
     *  method blackHasCastled()
     * 
     * This method returns whether or not black has castled
     * 
     * @return boolean - has black castled?
     *
     */
	public final boolean blackHasCastled() {
       return(bCastle==Global.CASTLED);
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


   /**
     *  method getWPawnAttack(int index)
     * 
     * This method returns a bitset representing all the squares attacked by a white pawn
     * 
     * @param int - position of pawn
     * 
     * @return long - bitset of attacks for pawn
     *
     */
	public  final long getWPawnAttack(int index) {
		return WhitePawnAttackBoard[index];
	}
	
    /** 
     *  method getBPawnAttack(int index)
     * 
     * This method returns a bitset representing all the squares attacked by a black pawn
     * 
     * @param int - position of pawn
     * 
     * @return long - bitset of attacks for pawn
     *
     */
	public  final long getBPawnAttack(int index) {
		return BlackPawnAttackBoard[index];
	}
	
    
    /***********************************************************************		
		Name:		getWPawnMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					white pawn moves
	***********************************************************************/		
	public  final long getWPawnMoves(int index) {   //get White Pawn Moves Based on index of pawn
		long moves = WhitePawnMoveBoard[index];
      if((bitboard & Global.set_Mask[index + 8]) != 0)
         moves = 0;
      else if(index < 16 && (moves & bitboard) != 0)
         moves = Global.set_Mask[index + 8];
		return moves | (WhitePawnAttackBoard[index] & (blackpieces | Global.set_Mask[passantB]));
	}// End getWPawnMoves
  
	/***********************************************************************		
		Name:		getWBawnMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					black pawn moves
	***********************************************************************/		
	public  final long getBPawnMoves(int index) {	//get Black Pawn Moves Based on index of pawn
		long moves = BlackPawnMoveBoard[index];
		if((bitboard & Global.set_Mask[index - 8]) != 0)
         moves = 0;
      else if(index > 47 && (moves & bitboard) != 0)
         moves = Global.set_Mask[index - 8];
		return moves | (BlackPawnAttackBoard[index] & (whitepieces | Global.set_Mask[passantW]));
	}// End getBPawnMoves
	

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
		Name:		getWKnightMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					white knight moves
	***********************************************************************/			
	public  final long getWKnightMoves(int index) {
		return KnightMoveBoard[index] & ~whitepieces;
	}// End getWKnightMoves
	
	/***********************************************************************		
		Name:		getBKnightMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					black knight moves
	***********************************************************************/			
	public  final long getBKnightMoves(int index) {
		return KnightMoveBoard[index] & ~blackpieces;
	}// End getBKnightMoves

    /** 
     *  method getKnightMoves(int index)
     * 
     * This method returns a bitset representing all the squares attacked by knight
     * 
     * @param int - position of knight
     * 
     * @return long - bitset of attacks for knight
     *
     */
	public  final long getKnightMoves(int index) {
		return KnightMoveBoard[index];
	}
	
    /** 
     *  method getBKingCastle(int index)
     * 
     * This method returns a bitset representing all castle moves for a black king
     * 
     * @param int - position of king
     * 
     * @return long - bitset of castle moves for king
     *
     */
	public  final long getBKingCastle(int index) {
		long Temp = Helper.getKingCastle((int)(bitboard>>>56));
		long castle = 0;
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
	
	/** 
     *  method getWKingCastle(int index)
     * 
     * This method returns a bitset representing all castle moves for a white king
     * 
     * @param int - position of king
     * 
     * @return long - bitset of castle moves for king
     *
     */
	public  final long getWKingCastle(int index) {
		long Temp = Helper.getKingCastle((int)bitboard&255);
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
		Name:		getBKingMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					black king moves
	***********************************************************************/		
	public  final long getBKingMoves(int index) {
	 	long kingPos = Helper.getKingPosition(index);
	 	return kingPos&~blackpieces;
	}
	
    /***********************************************************************		
		Name:		getWKingMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					white king moves
	***********************************************************************/			
	public  final long getWKingMoves(int index) {
	 	long kingPos = Helper.getKingPosition(index);
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
	public  final long getWRookMoves(int index) {
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
	public  final long getBRookMoves(int index) {
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
	public  final long getWBishopMoves(int index) {
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
	public  final long getBBishopMoves(int index) {
	
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
	public  final long getWQueenMoves(int index) {	
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
	public  final long getBQueenMoves(int index) {
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
	private  final long getRookMoves(int index,int relation) {
			long temp=0;
		
			if(relation!=2) {
				long sRank = bitboard>>ShiftRank[index];
				int Decimal = (int)(sRank&255);	
				temp = Helper.getRooksRank2(index,Decimal);
			}
			if(relation!=1) {
				long sRank = Board90R>>ShiftFile[index];
				int Decimal = (int)(sRank&255);
				temp |=  Helper.getRooksFile2(index,Decimal);
			}	
			return temp;		
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
		Name:		getMagicRookMoves
		Parameters:	int
		Returns:	long
		Description:This method returns a BitSet representing all of the 
					RookMoves moves from square int
	***********************************************************************/
	public  final long getMagicRookMoves(int index) {
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
	private  final long getBishopMoves(int index,int relation) {
			
        if(relation==3) {
            int temp45 = (int)(Board45L>>ShiftL[index]);
            return Helper.getDiag1Attack(index,temp45&255);	

        }else if(relation==4)	{
            int temp45 = (int)(Board45R>>ShiftR[index]);
            return Helper.getDiag2Attack(index,temp45&255);	
        }
        else {
            int temp45 = (int)(Board45L>>ShiftL[index]);
            long tempD = Helper.getDiag1Attack(index,temp45&255);
            temp45 = (int)(Board45R>>ShiftR[index]);
            tempD |= Helper.getDiag2Attack(index,temp45&255);
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
	private  final long getConventionalBishopMoves(int index, long occ) {
		
		long right45 = rotate45R(occ);
		long left45 = rotate45L(occ);
		
		int temp45 = (int)(left45>>>ShiftL[index]);
		long tempD = Helper.getDiag1Attack(index,temp45&255);
		
		temp45 = (int)(right45>>>ShiftR[index]);
		tempD |= Helper.getDiag2Attack(index,temp45&255);
		
		return tempD;
		
	}
	/***********************************************************************		
		Name:		getMagicBishopMoves
		Parameters:	int
		Returns:	long
		Description:This method returns a BitSet representing all of the 
					BishopMoves moves from square int
	***********************************************************************/
	
	public  final long getMagicBishopMoves(int index) {
		
		long occ = bitboard & bMask[index];
		occ *= bMagics[index];
		occ >>>= (64-bishopShift[index]);
		return bishopTable[index][(int)(occ)];	
	}
	
	/***********************************************************************		
		Name:		getQueenMoves
		Parameters:	int
		Returns:	BitSet
		Description:This method returns a BitSet representing all of the 
					queen moves from square int
	***********************************************************************/			
	public  final long getQueenMoves(int index) {
		return getMagicBishopMoves(index) | getMagicRookMoves(index);
	}
  	
    /** 
     *  method switchTurn()
     * 
     * This method switches the player on move
     * 
     */
 	public  final void switchTurn() {
 		hashValue ^=bHashMove;
 		turn = -turn;	
 	
 	}
    
    /** 
     *  method getMaxNumberOfPieces()
     * 
     * This method returns the maximum number of pieces for either side
     * 
     * @return int - the max number of pieces
     *
     */
 	public  final int getMaxNumberOfPieces() {
 		if(wPieceNo>bPieceNo)
 			return wPieceNo;
 		else
 			return bPieceNo;
 	}
    
    /** 
     *  method getMinNumberOfPieces()
     * 
     * This method returns the minimum number of pieces for either side
     * 
     * @return int - the minimum number of pieces
     *
     */
 	public  final int getMinNumberOfPieces() {
 		if(wPieceNo<bPieceNo)
 			return wPieceNo;
 		else
 			return bPieceNo;
 	}
 	
    /** 
     *  method getNumberOfPieces()
     * 
     * This method returns the  number of pieces for a specific side
     * 
     * @param side - the side to get the number of pieces for
     * @return int - the number of pieces
     *
     */
 	public  final int getNumberOfPieces(int side) {
 		if(side==-1)
 			return wPieceNo;
 		else
 			return bPieceNo;
 	}		
   
   /**
     *  method AddRepetitionRoot()
     *
     * This method adds a position to the repetition table and returns the number of previous times this table has been added
     * This version of Add Repetition is to be used when the best move is being made after a search as
     * this version will reset the repetition table if the move is not reversable
     *
     * @return - the number of repetitions of the position added
     *
     */
   public int AddRepetitionRoot() {
      iCurrentMovesDepth = 0;
      if(reversable) {
         zorbistDepth++;
         zorbistHistory[zorbistDepth] = hashValue;
         lastReversableMove[zorbistDepth] = lastReversableMove[zorbistDepth - 1];
         
         if((zorbistDepth - lastReversableMove[zorbistDepth]) >= 100)               //draw by 50 move rule
            return 3;
         
         //check for repetitions
         int count = 1;
         for(int i = zorbistDepth - 2; i >= lastReversableMove[zorbistDepth]; i -= 2) {
            if(zorbistHistory[i] == hashValue)                                  //return true on first repetition
               count++;
         }
         return count;
      
      } else {
         zorbistDepth = 1;
         zorbistHistory[zorbistDepth] = hashValue;
         lastReversableMove[zorbistDepth] = lastReversableMove[zorbistDepth - 1];
         return 1;
      }
   }

   /**
     *  method AddRepetition()
     *
     * This method adds a position to the repetition table and returns the number of previous times this table has been added
     * This version of Add Repetition is to be used during the recursive search
     *
     * @return - the number of repetitions of the position added
     *
     */
   public int AddRepetition() {
      
      if(reversable) {
         zorbistDepth++;
         zorbistHistory[zorbistDepth] = hashValue;
         lastReversableMove[zorbistDepth] = lastReversableMove[zorbistDepth - 1];
         
         if((zorbistDepth - lastReversableMove[zorbistDepth]) >= 100)               //draw by 50 move rule
            return 3;
         
         //check for repetitions
         int count = 1;
         for(int i = zorbistDepth - 2; i >= lastReversableMove[zorbistDepth]; i -= 2) {
            if(zorbistHistory[i] == hashValue)                                  //return true on first repetition
               count++;
         }
         
         return count;
      
      } else {
         zorbistDepth++;
         zorbistHistory[zorbistDepth] = hashValue;
         lastReversableMove[zorbistDepth] = zorbistDepth;
         return 1;
      }
   }

   /**
     *  method AddMove()
     *
     * This method adds a move to the array of moves made in the game so far
     *
     * @param move - the move to be added
     */
   public final void AddMove(int move) {
      int to = (move >> 6) & 63;//MoveFunctions.getTo(move);
		int from = move & 63; //MoveFunctions.getFrom(move);
		boardMoves[moveCount] = move;
		writer.addHistory(to,from,moveCount);
   }

    /**
     *  method GetMoveAtDepth()
     *
     * This method returns the move made at the requested depth in the current line of the recursive search
     *
     * @param depth - the requested depth for the move
     *
     * @return int - the move made
     *
     */
   public int GetMoveAtDepth(int depth)   {
      return arrCurrentMoves[depth];
   }

   /**
     *  method MakeMove()
     * 
     * This method performs all necessary tasks to make a chess move during the search 
     * 
     * @param int move - 32 bits with move information stuffed in
     * @param boolean add - true if this move is being made on the board and not just the search
     * @param boolean board - indicates to store as a repetition
     * 
     * 
     * @return int - the number of repetitions for this position
     *
     */

   public final int MakeMove(int move, boolean board) {
		
		int to = (move >> 6) & 63;//MoveFunctions.getTo(move);
		int from = move & 63; //MoveFunctions.getFrom(move);
		int type = (move >> 20) & 15; //MoveFunctions.moveType(move);
		int thePiece = ((move>>12) & 15); //MoveFunctions.getPiece(move);
		int cP = ((move>>16) & 15) - 1; //MoveFunctions.getCapture(move);

      arrCurrentMoves[iCurrentMovesDepth] = move;
      iCurrentMovesDepth++;

      reversable = true;
		hashHistory[moveCount] = hashValue;
		
		flagHistory[moveCount] = (passantW) | (passantB) << 6 | wCastle << 12 | bCastle << 15;

      turn = -turn;
      hashValue ^= bHashMove;
      
      moveCount++;
		
      
      int oldPassantW = passantW;
      int oldPassantB = passantB;
      
      passantW = NO_PASSANT_WHITE;
		passantB = NO_PASSANT_BLACK;

      switch(type) {
			case(Global.ORDINARY_MOVE):
            if(cP != -1) {
               if(thePiece == 4) {
                  if(wCastle > Global.CASTLED) {
                     hashValue ^= wCastleHash[wCastle];
                     wCastle = Global.NO_CASTLE;
                     hashValue ^= wCastleHash[wCastle];
                  }
               } else if(thePiece == 10) {
                  if(bCastle > Global.CASTLED) {
                     hashValue ^= bCastleHash[bCastle];
                     bCastle = Global.NO_CASTLE;
                     hashValue ^= bCastleHash[bCastle];
                  }
               } else
                   UpdateCastleFlags(thePiece, to , from);
               reversable = false;
					clearBoard(to);
					hashValue ^= pHash[to][cP];
				} else {
               switch(thePiece) {
                  case 5:
                     reversable = false;
                     break;
                  case 11:
                     reversable = false;
                     break;
                  case 0:
                     if(wCastle > Global.CASTLED) {
                        if(from == 7) {
                           hashValue ^= wCastleHash[wCastle];
                           wCastle &= Global.LONG_CASTLE;
                           hashValue ^= wCastleHash[wCastle];
                        } else if(from == 0) {
                           hashValue ^= wCastleHash[wCastle];
                           wCastle &= Global.SHORT_CASTLE;
                           hashValue ^= wCastleHash[wCastle];
                        }
                     }
                     break;
                  case 6:
                     if(bCastle > Global.CASTLED) {
                        if(from == 63) {
                           hashValue ^= bCastleHash[bCastle];
                           bCastle &= Global.LONG_CASTLE;
                           hashValue ^= bCastleHash[bCastle];
                        } else if(from == 56) {
                           hashValue ^= bCastleHash[bCastle];
                           bCastle &= Global.SHORT_CASTLE;
                           hashValue ^= bCastleHash[bCastle];
                        }
                     }
                     break;
                  case 4:
                     if(wCastle > Global.CASTLED) {
                        hashValue ^= wCastleHash[wCastle];
                        wCastle = Global.NO_CASTLE;
                        hashValue ^= wCastleHash[wCastle];
                     }
                     break;
                  case 10:
                     if(bCastle > Global.CASTLED) {
                        hashValue ^= bCastleHash[bCastle];
                        bCastle &= Global.NO_CASTLE;
                        hashValue ^= bCastleHash[bCastle];
                     }
                     break;
               }
            }
			break;

         case(Global.EN_PASSANT_CAP):
            reversable = false;
				if(thePiece == 5) {
					clearBoard(to - 8);
					hashValue ^= pHash[to-8][11];
				} else {
					clearBoard(to+8);
					hashValue ^= pHash[to+8][5];
				}
				break;


			case(Global.SHORT_CASTLE):
				reversable = false;
            if(thePiece == 4) {
					hashValue ^= wCastleHash[wCastle];
               wCastle = Global.CASTLED;
					hashValue ^= wCastleHash[wCastle];
               updateBoard(5,7);
					hashValue ^= pHash[5][0];
					hashValue ^= pHash[7][0];
				} else {
					hashValue ^= bCastleHash[bCastle];
               bCastle = Global.CASTLED;
					hashValue ^= bCastleHash[bCastle];
					updateBoard(61,63);
					hashValue ^= pHash[61][6];
					hashValue ^= pHash[63][6];
				}
            break;

         case(Global.PROMO_Q):
				reversable = false;
				clearBoard(from);
				hashValue ^= pHash[from][thePiece];
				setBoard(from, thePiece-2);
				hashValue ^= pHash[from][thePiece-2];
				if(cP != -1) {
               UpdateCastleFlags(thePiece, to , from);
               clearBoard(to);
					hashValue ^= pHash[to][cP];
				}
				break;

			case(Global.LONG_CASTLE):
				reversable = false;
            if(thePiece == 4) {
					hashValue ^= wCastleHash[wCastle];
               wCastle = Global.CASTLED;
               hashValue ^= wCastleHash[wCastle];
					updateBoard(3,0);
					hashValue ^= pHash[3][0];
					hashValue ^= pHash[0][0];
				} else {
					hashValue ^= bCastleHash[bCastle];
               bCastle = Global.CASTLED;
					hashValue ^= bCastleHash[bCastle];
					updateBoard(59,56);
					hashValue ^= pHash[56][6];
					hashValue ^= pHash[59][6];
				}
				break;
			
			case(Global.PROMO_N):
				reversable = false;
				clearBoard(from);
				hashValue ^= pHash[from][thePiece];
				setBoard(from, thePiece-4);
				hashValue ^= pHash[from][thePiece-4];
				if(cP != -1) {
					UpdateCastleFlags(thePiece, to , from);
               clearBoard(to);
					hashValue ^= pHash[to][cP];
				}
				break;

         case(Global.PROMO_R):
				reversable = false;
				clearBoard(from);
				hashValue ^= pHash[from][thePiece];
				setBoard(from, thePiece-5);
				hashValue ^= pHash[from][thePiece-5];
				if(cP != -1) {
               UpdateCastleFlags(thePiece, to , from);
               clearBoard(to);
					hashValue ^= pHash[to][cP];
				}
				break;

			case(Global.PROMO_B):
				reversable = false;
				clearBoard(from);
				hashValue ^= pHash[from][thePiece];
				setBoard(from, thePiece-3);
				hashValue ^= pHash[from][thePiece-3];
				if(cP != -1) {
               UpdateCastleFlags(thePiece, to , from);
               clearBoard(to);
					hashValue ^= pHash[to][cP];
				}
				break;	
			
         case(Global.DOUBLE_PAWN_WHITE):
            reversable = false;
            passantW = to - 8;
            break;

         case(Global.DOUBLE_PAWN_BLACK):
            reversable = false;
            passantB = from - 8;
            break;
      }


		hashValue ^= pHash[from][piece_in_square[from]];
		hashValue ^= pHash[to][piece_in_square[from]];
		updateBoard(to,from);

     if(oldPassantW != passantW)
     {
        hashValue ^= passantHashW[oldPassantW%9];
        hashValue ^= passantHashW[passantW%9];
     }
     
     if(oldPassantB != passantB)
     {
        hashValue ^= passantHashB[oldPassantB%9];
        hashValue ^= passantHashB[passantB%9];
     }
     // if(hashValue != generateHash()) {
     //    System.out.println("info string generatehash is +"+generateHash());
    //  }
      
     if( board )
        return AddRepetition();
     else
        return 1;
	}
  
    

   public final void UpdateCastleFlags(int thePiece, int to, int from) {

      if(wCastle > Global.CASTLED) {
			if(to == 7 || from == 7) {
				hashValue ^= wCastleHash[wCastle];
            wCastle &= Global.LONG_CASTLE;
            hashValue ^= wCastleHash[wCastle];
         } else if(to == 0 || from == 0) {
            hashValue ^= wCastleHash[wCastle];
            wCastle &= Global.SHORT_CASTLE;
            hashValue ^= wCastleHash[wCastle];
         }
      }

      if(bCastle > Global.CASTLED) {
			if(to == 63 || from == 63) {
            hashValue ^= bCastleHash[bCastle];
            bCastle &= Global.LONG_CASTLE;
            hashValue ^= bCastleHash[bCastle];
         } else if(to == 56 || from == 56) {
            hashValue ^= bCastleHash[bCastle];
            bCastle &= Global.SHORT_CASTLE;
            hashValue ^= bCastleHash[bCastle];
         }
      }
   }

   /**
     *  method getDraw()
     * 
     * This accessor method returns the 50 move draw count variable
     * 
     * @return int - 50 move draw count var
     *
     */
	public  final int getDraw() {
		return drawCount;
	}
    
    /** 
     *  method getCount()
     * 
     * This accessor method returns the number of moves made
     * 
     * @return int - the move count
     *
     */
	public  final int getCount() {
		return moveCount;	
	}	
	
    /** 
     *  method getTurn()
     * 
     * This accessor method returns a var indicating whose turn it is to move
     * (-1 white, 1 black)
     * 
     * @return int - whose turn is it?
     *
     */
    public  final int getTurn() {
		return turn;
	}	
    
    /** 
     *  method unMake()
     * 
     * This method performs all necessary tasks to undo a chess move during the search 
     * 
     * @param int move - 32 bits with move information stuffed in
     * @param boolean add - true if this move is being made on the board and not just the search
     * @param boolean board - indicates to remove as a repetition
     * 
     * 
     * @return int - the number of repetitions for this position
     *
     */
	public final void UnMake(int move, boolean board) {
		if(board)
         zorbistDepth--;
		turn = -turn;	
		moveCount--;
		wCastle = (flagHistory[moveCount] >> 12) & 7;
		bCastle = (flagHistory[moveCount] >> 15) & 7;
		iCurrentMovesDepth--;
		int to = (move >> 6) & 63;//MoveFunctions.getTo(move);
		int from = move & 63; //MoveFunctions.getFrom(move);
		int type = (move >> 20) & 15; //MoveFunctions.moveType(move);
		int piece = ((move>>12) & 15); //MoveFunctions.getPiece(move);
		int capPiece = ((move>>16) & 15) - 1; //MoveFunctions.getCapture(move);
      
		switch(type) {
			case(Global.ORDINARY_MOVE):
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
					updateBoard(7,5);
				} else {
					updateBoard(63, 61);
				}
				break;

         case(Global.PROMO_Q):
				clearBoard(to);
				setBoard(to, piece);
				updateBoard(from, to);
				if(capPiece != -1)
					setBoard(to, capPiece);
				break;

         case(Global.LONG_CASTLE):
				updateBoard(from, to);
				if(piece == 4) {
					updateBoard(0, 3);
				} else {
					updateBoard(56, 59);
				}
				break;

         case(Global.PROMO_N):
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

         case(Global.PROMO_B):
				clearBoard(to);
				setBoard(to, piece);
				updateBoard(from, to);
				if(capPiece != -1)
					setBoard(to, capPiece);
				break;	
			
         case(Global.DOUBLE_PAWN_WHITE):
            updateBoard(from, to);
            break;

         case(Global.DOUBLE_PAWN_BLACK):
            updateBoard(from, to);
            break;

		}
		hashValue = hashHistory[moveCount];
		passantW = (flagHistory[moveCount] & 63);
		passantB = ((flagHistory[moveCount] >> 6) & 63);

      // if(hashValue != generateHash()) {
     //    System.out.println("info string generatehash is +"+generateHash());
     // }
	}	
		
}// End MagnumChess