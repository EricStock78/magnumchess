/**
 * Global.java
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
 * Global.java
 * 
 * This abstract class contains only public static members
 * These variables are global variables used throughout the program
 * 
 *
 * @version 	2.00 30 Jan 2010
 * @author 	Eric Stock
 */

abstract class Global {
	/** string representation of all pieces */
    public static final String pieces[]= new String[] {"wRook","wKnight","wBishop","wQueen","wKing","wPawn",
													"bRook","bKnight","bBishop","bQueen","bKing","bPawn"};
    /** material values for each piece */
    public static final int values[] = new int[] {500,325,350,900,2000,100,500,325,350,900,2000,100};
    
    /** flags indicating if the piece slides */
    public static boolean[] slides = new boolean[] {true,false,true,true,false,false,true,false,true,true,false,false};

    
    /** castling status flags */
    public static final int NO_CASTLE = 0;
    public static final int CASTLED = 1;
	public static final int BOTH_CASTLE = 7;
    public static final int SHORT_CASTLE = 2;   /** also doubles as a move type */
	public static final int LONG_CASTLE = 3;    /** also doubles as a move type */
    
    /** move types */
    public static final int ORDINARY_MOVE = 0;
	public static final int EN_PASSANT_CAP = 4;
	public static final int PROMO_Q = 5;
	public static final int PROMO_R = 6;
	public static final int PROMO_B = 7;
	public static final int PROMO_N = 1;
	
	/** repetition table size */
    public static int REPSIZE = 16384;
	/** hash table size */
    public static  int HASHSIZE = 262144;				//8 mb initial hashsize
	/** pawn hash table size */
    public static int PawnHASHSIZE = 174762;            //4mb initial pawn hash table
	/** eval hash table size */
    public static int EvalHASHSIZE = 349524;              //4mb initial eval hash table
	
    /** various masks needed for evaluation and move generation */
    public static long[] set_Mask;
    public static long[] plus9; 
	public static long[] plus7;
	public static long[] plus8;
	public static long[] plus1;
	public static long[] minus9;
	public static long[] minus7;
	public static long[] minus8;
	public static long[] minus1;
    public static long[] diag1Masks = new long[15];
	public static long[] diag2Masks = new long[15];
	public static long[] fileMasks = new long[8];		//used to isolate all pieces on a rank
	public static long[] rankMasks = new long[8];
	public static long[] whitePassedPawnMasks;          //passed pawn masks for white
    public static long[] blackPassedPawnMasks;          //passed pawn masks for black
    public static long[] bKingMask;						//mask of squares around king
	public static long[] wKingMask;
	public static long[] wRookTrap = new long[] {0x303L, 0xC0C0L};
	public static long[] bRookTrap = new long[] {wRookTrap[0]<<48, wRookTrap[1]<<48};
    
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