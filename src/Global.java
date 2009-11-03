abstract class Global {
	public static final String pieces[]= new String[] {"wRook","wKnight","wBishop","wQueen","wKing","wPawn",
													"bRook","bKnight","bBishop","bQueen","bKing","bPawn"};

	public static final int NO_CASTLE = 0;
    public static final int CASTLED = 1;
	public static final int BOTH_CASTLE = 7;
	public static final int ORDINARY_MOVE = 0;
	public static final int SHORT_CASTLE = 2;
	public static final int LONG_CASTLE = 3;
	public static final int EN_PASSANT_CAP = 4;
	public static final int PROMO_Q = 5;
	public static final int PROMO_R = 6;
	public static final int PROMO_B = 7;
	public static final int PROMO_N = 1;
	
	public static int REPSIZE = 16384;
	public static  int HASHSIZE = 262144;				//8 mb initial hashsize
	public static int PawnHASHSIZE = 174762;            //4mb initial pawn hash table
	public static int EvalHASHSIZE = 349524;              //4mb initial eval hash table
	public static long[] diag1Masks = new long[15];
	public static long[] diag2Masks = new long[15];
	public static long[] fileMasks = new long[8];					//used to isolate all pieces on a rank
	public static long[] rankMasks = new long[8];
	public static long[] plus9; //= new long[64];
	public static long[] plus7; //= new long[64];
	public static long[] plus8;// = new long[64];
	public static long[] plus1;// = new long[64];
	public static long[] minus9;
	public static long[] minus7;
	public static long[] minus8;
	public static long[] minus1;
	public static boolean[] diagSlides = new boolean[] {false,false,true,true,false,false,false,false,true,true,false,false};
	public static boolean[] rankSlides = new boolean[] {true,false,false,true,false,false,true,false,false,true,false,false};
	public static boolean[] slides = new boolean[] {true,false,true,true,false,false,true,false,true,true,false,false};
	public static final int Diag1Length[] = new int[]  		{1,2,3,4,5,6,7,8,
													 			2,3,4,5,6,7,8,7,
													 			3,4,5,6,7,8,7,6,
													 			4,5,6,7,8,7,6,5,
													 			5,6,7,8,7,6,5,4,
													 			6,7,8,7,6,5,4,3,
													 			7,8,7,6,5,4,3,2,
													 			8,7,6,5,4,3,2,1};

	public static final int Diag2Length[]= new int[] 			{8,7,6,5,4,3,2,1,
															 	7,8,7,6,5,4,3,2,
															 	6,7,8,7,6,5,4,3,
															 	5,6,7,8,7,6,5,4,
															 	4,5,6,7,8,7,6,5,
															 	3,4,5,6,7,8,7,6,
															 	2,3,4,5,6,7,8,7,
															 	1,2,3,4,5,6,7,8,};												 			
	
	public static final int Diag1Groups[] = new int[]  	   	   {0,1,2,3,4,5,6,7,
													 			1,2,3,4,5,6,7,8,
													 			2,3,4,5,6,7,8,9,
													 			3,4,5,6,7,8,9,10,
													 			4,5,6,7,8,9,10,11,
													 			5,6,7,8,9,10,11,12,
													 			6,7,8,9,10,11,12,13,
													 			7,8,9,10,11,12,13,14};
													 														 			
	public static final int Diag2Groups[] = new int[] 		{7,8,9,10,11,12,13,14,
															 6,7,8,9,10,11,12,13,
															 5,6,7,8,9,10,11,12,
															 4,5,6,7,8,9,10,11,
															 3,4,5,6,7,8,9,10,
															 2,3,4,5,6,7,8,9,
															 1,2,3,4,5,6,7,8,
															 0,1,2,3,4,5,6,7,};
	
	
	public static final int innerIndex[] = new int[]		{0,0,0,0,0,0,0,0,
															 0,1,1,1,1,1,1,0,
															 0,1,1,1,1,1,1,0,
															 0,1,1,1,1,1,1,0,
															 0,1,1,1,1,1,1,0,
															 0,1,1,1,1,1,1,0,
															 0,1,1,1,1,1,1,0,
															 0,0,0,0,0,0,0,0};
	
	public static final int bKnightVals[] = new int[]	{0,0,0,0,0,0,0,0,
														 0,3,3,3,3,3,3,0,
														 0,3,8,10,10,8,3,0,
														 0,3,8,10,10,8,3,0,
														 0,3,7,8,8,7,3,0,
														 0,3,6,6,6,6,3,0,
														 0,3,3,3,3,3,3,0,
														 0,0,0,0,0,0,0,0};
	
	public static final int wKnightVals[] = new int[]	{0,0,0,0,0,0,0,0,
														 0,3,3,3,3,3,3,0,
														 0,3,6,6,6,6,3,0,
														 0,3,7,8,8,7,3,0,
														 0,3,8,10,10,8,3,0,
														 0,3,8,10,10,8,3,0,
														 0,3,3,3,3,3,3,0,
														 0,0,0,0,0,0,0,0};	
	
	
	public static final int bishopVals[] = new int[] 	{8,8,8,8,8,8,8,8,
														 8,10,10,10,10,10,10,8,
														 8,10,12,12,12,12,10,8,
														 8,10,12,14,14,12,10,8,
														 8,10,12,14,14,12,10,8,
														 8,10,12,12,12,12,10,8,
														 8,10,10,10,10,10,10,8,
														 8,8,8,8,8,8,8,8};	
									
	public static long whiteTerritory;
	public static long blackTerritory;
	
	
	
	public static final int values[] = new int[] {500,325,350,900,2000,100,500,325,350,900,2000,100};
    public static final int mvvValues[] = new int[] {150,100,100,200,0,50,150,100,100,200,0,50};

	public static final int SEEvalues[] = new int[] {25,17,17,45,60,5,25,17,17,45,60,5};
	//public static final int SEEvalues[] = new int[] {500,300,330,950,2000,100,500,300,330,950,2000,100};	

	public static final int researchDiff[] = new int[] {0,5,15,40,100,250,600,1000,2000};
	
	public static final int oneAwayPassed = 150;		//pawn bonus for one away passed pawn
	public static final int twoAwayPassed = 40;			//pawn bonus for two away passed pawn
	
	public static long[] set_Mask = new long[64];
	
	public static long[] bKingMask;						//mask of squares around king
	public static long[] wKingMask;
	public static long[] wRookTrap = new long[] {0x303L, 0xC0C0L};
	public static long[] bRookTrap = new long[] {wRookTrap[0]<<48, wRookTrap[1]<<48};
	
	private  Global() {
		
		
		
		
	}
}		