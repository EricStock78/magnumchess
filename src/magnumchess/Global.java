package magnumchess;
/**
 * Global.java
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

/*
 * Global.java
 * 
 * This abstract class contains only public static members
 * These variables are global variables used throughout the program
 * 
 * Version 4.0
 *
 * Copyright (c) 2012 Eric Stock
 */

abstract class Global {

    public static final int COLOUR_WHITE = 0;
    public static final int COLOUR_BLACK = 1;

    public static final int LIGHT_SQUARE = 1;
    public static final int DARK_SQUARE = 0;
    
    public static final int MAX_DEPTH = 40;
    
    public static final int MATE_SCORE = 20000;
    public static final int KNOWN_WIN = 10000;
    public static final int KNOWN_WIN_KPK = 400;    //keep this small to still encourage promotion
    
    public static final int PIECE_ROOK = 0;
    public static final int PIECE_KNIGHT = 1;
    public static final int PIECE_BISHOP = 2;
    public static final int PIECE_QUEEN = 3;
    public static final int PIECE_KING = 4;
    public static final int PIECE_PAWN = 5;
    public static final int PIECE_ALL = 6;

    public static final int whiteKBNK = 90;
    public static final int blackKBNK = 270;
    public static final int whiteKRKB = 28;
    public static final int blackKRKB = 12;
    public static final int whiteKRKN = 244;
    public static final int blackKRKN = 84;
    public static final int whiteKRKP = 26245;
    public static final int blackKRKP = 2919;
    public static final int blackKPK = 26244;
    public static final int whiteKPK = 2916;
    
    public static int[][] RelativeRanks = { {0, 1, 2, 3, 4, 5, 6, 7} , {7, 6, 5, 4, 3, 2, 1, 0} };
    
    /** string representation of all pieces */
    public static final String pieces[]= new String[] {"wRook","wKnight","wBishop","wQueen","wKing","wPawn",
													"bRook","bKnight","bBishop","bQueen","bKing","bPawn"};
    /** material values for each piece */
	 public static final int values[] = new int[] {753,454,515,1421,20000,132,753,454,515,1421,20000,132};

    public static int totalValue;

	 public static final int[] materialOffset =				new int[] {1, 81, 9, 729, 0, 2916,
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
	
	/** hash table size */
	public static int HASHSIZE = 65536 * 64;				//64 mb initial hashsize
	/** pawn hash table size */
	public static int PawnHASHSIZE = 43960 * 8;		//8 mb initial pawn hash size
	/** eval hash table size */
	public static int EvalHASHSIZE = 131072 * 8;		//8 mb initial eval hash size

	/** various masks needed for evaluation and move generation */
	public static final long[][] mask_between = new long[64][64];
    
    public static final long[][] mask_behind = new long[2][64];
	public static final long[][] mask_in_front = new long[2][64];
	public static final long[][] mask_forward = new long[2][64];

	public static final long[] neighbour_files = new long[8];
	public static final long[][] passed_masks = new long[2][64];

	public static final int[] behindRank = {-8, 8};
        public static final int[] forwardRank = {8, -8};
   public static final int[] pieceAdd = {0, 6};
   
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
	
        public static final long[] bishopMasks = new long[64];
        public static final long[] rookMasks = new long[64];
        //public static final long[][] passedPawnMasks = new long[2][64];          //passed pawn masks for white
	//public static final long[] blackPassedPawnMasks = new long[64];          //passed pawn masks for black

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
	
}		