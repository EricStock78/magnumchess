/**
 * Global.java
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

/*
 * Global.java
 * 
 * This abstract class contains only public static members
 * These variables are global variables used throughout the program
 * 
 *
 * @version 	3.00 25 Oct 2010
 * @author 	Eric Stock
 */

abstract class Global {

	public static final int COLOUR_WHITE = 0;
    public static final int COLOUR_BLACK = 1;

    public static final int PIECE_ROOK = 0;
    public static final int PIECE_KNIGHT = 1;
    public static final int PIECE_BISHOP = 2;
    public static final int PIECE_QUEEN = 3;
    public static final int PIECE_KING = 4;
    public static final int PIECE_PAWN = 5;

    /** string representation of all pieces */
    public static final String pieces[]= new String[] {"wRook","wKnight","wBishop","wQueen","wKing","wPawn",
													"bRook","bKnight","bBishop","bQueen","bKing","bPawn"};
    /** material values for each piece */
    public static final int values[] = new int[] {500,325,325,900,2000,100,500,325,325,900,2000,100};

    public static int totalValue;

	 public static final int[] materialOffset =/* new int[] {4, 324, 36, 1, 0, 2916,
                                                      12, 972, 108, 2, 0, 26244};*/
                                                 new int[] {1, 81, 9, 729, 0, 2916,
                                                            3, 243, 27, 1458, 0, 26244};

    public static final int materialDraw = 999999;

    /** inverse material values used for MLV/LVA ordering */
    public static final int invValues[] = new int[] {2,3,3,1,0,4,2,3,3,1,0,4};

    /** flags indicating if the piece slides */
    public static boolean[] slides = new boolean[] {true,false,true,true,false,false,true,false,true,true,false,false};

     /** castling status flags */
    public static final int NO_CASTLE = 0;
    public static final int CASTLED = 1;
	 public static final int SHORT_CASTLE = 2;
    public static final int LONG_CASTLE = 4;
    public static final int BOTH_CASTLE = 6;
  
   
    public static final int CHECKMATE = 1;
    public static final int DRAW_REPETITION = 2;
    public static final int DRAW_50MOVES = 3;
    public static final int STALEMATE = 4;
    public static final int INSUFICIENT_MATERIAL = 5;


	/** move types */
	public static final int ORDINARY_MOVE = 0;
	public static final int ORDINARY_CAPTURE = 1;
	public static final int DOUBLE_PAWN = 3;
	public static final int MOVE_KING_LOSE_CASTLE = 5;
	public static final int MOVE_ROOK_LOSE_CASTLE = 7;
	public static final int CAPTURE_ROOK_LOSE_CASTLE = 8;
	public static final int PROMO_Q = 9;
	public static final int PROMO_R = 10;
	public static final int PROMO_B = 11;
	public static final int PROMO_N = 12;
	public static final int EN_PASSANT_CAP = 13;

	/** repetition table size */
	public static int REPSIZE = 16384;
	/** hash table size */
	public static int HASHSIZE = 262144;				//8 mb initial hashsize
	/** pawn hash table size */
	public static int PawnHASHSIZE = 174762 * 4;
	/** eval hash table size */
	public static int EvalHASHSIZE = 349524 * 2;             

	/** various masks needed for evaluation and move generation */
	public static final long[][] mask_behind = new long[2][64];
	public static final long[][] mask_in_front = new long[2][64];
	public static final long[][] mask_forward = new long[2][64];

	public static final long[] neighbour_files = new long[8];
	public static final long[][] passed_masks = new long[2][64];

	public static final long[] set_Mask = new long[64];
	public static final long[] plus9 = new long[64];
	public static final long[] plus7 = new long[64];
	public static final long[] plus8 = new long[64];
	public static final long[] plus1 = new long[64];
	public static final long[] minus9 = new long[64];
	public static final long[] minus7 = new long[64];
	public static final long[] minus8 = new long[64];
	public static final long[] minus1 = new long[64];
	public static final long[] diag1Masks = new long[15];
	public static final long[] diag2Masks = new long[15];
	public static final long[] fileMasks = new long[8];		//used to isolate all pieces on a rank
	public static final long[] rankMasks = new long[8];
	public static final long[] whitePassedPawnMasks = new long[64];          //passed pawn masks for white
	public static final long[] blackPassedPawnMasks = new long[64];          //passed pawn masks for black

	public static final long[] wRookTrap = new long[] {0x303L, 0xC0C0L};
	public static final long[] bRookTrap = new long[] {wRookTrap[0]<<48, wRookTrap[1]<<48};
    
    /** length of diagonals */
    public static final int Diag1Length[] = new int[]  {1,2,3,4,5,6,7,8,
                                                        2,3,4,5,6,7,8,7,
                                                        3,4,5,6,7,8,7,6,
                                                        4,5,6,7,8,7,6,5,
                                                        5,6,7,8,7,6,5,4,
                                                        6,7,8,7,6,5,4,3,
                                                        7,8,7,6,5,4,3,2,
                                                        8,7,6,5,4,3,2,1};
    
    /** length of opposite diagonal */                                                    
	public static final int Diag2Length[]= new int[]   {8,7,6,5,4,3,2,1,
                                                        7,8,7,6,5,4,3,2,
                                                        6,7,8,7,6,5,4,3,
                                                        5,6,7,8,7,6,5,4,
                                                        4,5,6,7,8,7,6,5,
                                                        3,4,5,6,7,8,7,6,
                                                        2,3,4,5,6,7,8,7,
                                                        1,2,3,4,5,6,7,8,};												 			
	
    /** array used for bishop move rotated bitboard generation */
	public static final int Diag1Groups[] = new int[]  {0,1,2,3,4,5,6,7,
                                                        1,2,3,4,5,6,7,8,
                                                        2,3,4,5,6,7,8,9,
                                                        3,4,5,6,7,8,9,10,
                                                        4,5,6,7,8,9,10,11,
                                                        5,6,7,8,9,10,11,12,
                                                        6,7,8,9,10,11,12,13,
                                                        7,8,9,10,11,12,13,14};
	
    /** array used for bishop move rotated bitboard generation */												 														 			
	public static final int Diag2Groups[] = new int[]   {7,8,9,10,11,12,13,14,
                                                         6,7,8,9,10,11,12,13,
                                                         5,6,7,8,9,10,11,12,
                                                         4,5,6,7,8,9,10,11,
                                                         3,4,5,6,7,8,9,10,
                                                         2,3,4,5,6,7,8,9,
                                                         1,2,3,4,5,6,7,8,
                                                         0,1,2,3,4,5,6,7,};
	
	/** mask used to seperate outer and inner squares */
	public static final int innerIndex[] = new int[]    {0,0,0,0,0,0,0,0,
                                                         0,1,1,1,1,1,1,0,
                                                         0,1,1,1,1,1,1,0,
                                                         0,1,1,1,1,1,1,0,
                                                         0,1,1,1,1,1,1,0,
                                                         0,1,1,1,1,1,1,0,
                                                         0,1,1,1,1,1,1,0,
                                                         0,0,0,0,0,0,0,0};
	
    /** piece sqaure tables for black knights */
	public static final int bKnightVals[] = new int[]	{0,0,0,0,0,0,0,0,
														 0,3,3,3,3,3,3,0,
														 0,3,8,10,10,8,3,0,
														 0,3,8,10,10,8,3,0,
														 0,3,7,8,8,7,3,0,
														 0,3,6,6,6,6,3,0,
														 0,3,3,3,3,3,3,0,
														 0,0,0,0,0,0,0,0};
	
    /** piece sqaure tables for white knights */
	public static final int wKnightVals[] = new int[]	{0,0,0,0,0,0,0,0,
														 0,3,3,3,3,3,3,0,
														 0,3,6,6,6,6,3,0,
														 0,3,7,8,8,7,3,0,
														 0,3,8,10,10,8,3,0,
														 0,3,8,10,10,8,3,0,
														 0,3,3,3,3,3,3,0,
														 0,0,0,0,0,0,0,0};	
	
	
    /** piece sqaure tables for bishops */
	public static final int bishopVals[] = new int[] 	{8,8,8,8,8,8,8,8,
														 8,10,10,10,10,10,10,8,
														 8,10,12,12,12,12,10,8,
														 8,10,12,14,14,12,10,8,
														 8,10,12,14,14,12,10,8,
														 8,10,12,12,12,12,10,8,
														 8,10,10,10,10,10,10,8,
														 8,8,8,8,8,8,8,8};	
				
}		