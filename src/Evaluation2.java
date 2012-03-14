
import java.util.Arrays;

/**
 * Evaluation2.java
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
 * Evaluation2.java
 * This is the evaluation for Magnum
 * Includes penalties for isolated, doubled, backward pawns
 * Passed pawn bonus
 * king safety
 * position of major and minor pieces
 * mobility
 * center control
 *
 */

public class Evaluation2 {

    /** initialized instance of Board class */
    private static Board chessBoard;

    /** pawn hash table */
    private static TransTable PawnTable = new TransTable(Global.PawnHASHSIZE,1);

    /** evaluation hash table */
    private static TransTable EvalTable = new TransTable(Global.EvalHASHSIZE,2);

	 private static TransTable EvalTableLazy = new TransTable(Global.EvalHASHSIZE,2);

	 private static long[][] boardAttacks = new long[2][7];

	 private static int[][] RelativeRanks = { {0, 1, 2, 3, 4, 5, 6, 7} , {7, 6, 5, 4, 3, 2, 1, 0} };

	 /** indices of boards for specific pieces in boardAttacks */
    private static final int PAWN_BOARD = 0;
    private static final int KNIGHT_BOARD = 1;
	 private static final int BISHOP_BOARD = 2;
    private static final int ROOK_BOARD = 3;
    private static final int QUEEN_BOARD = 4;
    private static final int KING_BOARD = 5;
    private static final int ALL_BOARD = 6;

	 private static final int MIDDLE_GAME = 0;
	 private static final int END_GAME = 1;

	 /** white passed pawn bonus based on rank */
    private static final int[][] PassedPawnBonus = {{0,7,13,17,25,38,80,0},{0,15,23,35,47,90,200,0}};

    /** pawn bonus */
	 public static  int[][] CandidatePawnBonus = {{ 0, 5, 5, 10, 15, 30, 0, 0} , { 0, 10, 12, 17, 25, 40, 0, 0}};

	 public static final int[][] ChainPawn = {{ 7, 9, 10, 10, 10, 10, 9, 7} , { 0, 0, 0, 0, 0, 0, 0, 0}};

	 /** pawn penalties for weakness */
    public static final int[][] IsolatedPawn = {{ 6, 8, 10, 10, 10, 10, 8, 6} , { 8, 10, 10, 10, 10, 10, 10, 8}};
	
	 public static final int[][] WeakPawn = {{ 6, 8, 10, 10, 10, 10, 8, 6} , { 9, 10, 10, 10, 10, 10, 10, 9}};

	 public static final int[][] DoubledPawn = {{ 6, 9, 10, 10, 10, 10, 9, 6} , { 17, 20, 20, 20, 20, 20, 20, 17}};


	 /** mobility bonus multipliers */
    public static final int[][] KNIGHT_MOBILITY = {{-15, -10, -8, 0, 8, 12, 15, 16, 17}, {-15, -10, -8, 0, 8, 12, 15, 16, 17}};
	 public static final int[][] BISHOP_MOBILITY = {{ -15, -12, -3, 0, 5, 10, 12, 13, 14, 15, 16, 17, 17, 18, 18, 18}, { -15, -12, -3, 0, 5, 10, 12, 13, 14, 15, 16, 17, 17, 18, 18, 18}};
	 public static final int[][] ROOK_MOBILITY = {{ -15, -12, -3, 0, 5, 10, 12, 13, 14, 15, 15, 16, 16, 17, 17, 19}, { -15, -12, -3, 0, 5, 10, 12, 13, 14, 15, 15, 16, 16, 17, 17, 19}};
	 public static final int[][] QUEEN_MOBILITY = {{ -15, -12, -3, 0, 5, 10, 12, 13, 14, 15, 15, 16, 16, 16, 17, 17, 17, 17, 18, 18, 18, 18, 18, 19, 19, 19, 19, 19, 20, 20, 20, 20},
																	{ -15, -12, -3, 0, 5, 10, 12, 13, 14, 15, 15, 16, 16, 16, 17, 17, 17, 17, 18, 18, 18, 18, 18, 19, 19, 19, 19, 19, 20, 20, 20, 20}};

    private static final long CENTER_BITS = 0x0000001818000000L;
	 //private static final long EXTENDED_CENTER_BITS = 0x003C24243C00;

	 private static final int[] CenterScoreArray = {0, 1, 2, 4, 7, 12, 17, 26, 37, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 45, 45, 45, 45, 45, 45, 45, 45};
	 /** center bonus multiplier */
    private static final int CENTER_BONUS = 1;

    /** development penalty for backrank minor piece */
    private static final int BACKRANK_MINOR = 20;

    /** bonus for side to move called tempo */
    private static final int TEMPO_BONUS = 30;

    /** various positional bonuses */
    private static final int ROOK_OPEN = 20;
    private static final int ROOK_SEMI = 10;
    private static final int ROOK_DOUBLED = 20;
    private static final int DOUBLED_ROOKS_7TH = 60;
    private static final int ROOK_IN_FRONT_QUEEN = 25;
    private static final int ROOK_BEHIND_QUEEN = 12;
    private static final int ROOK_7TH_RANK = 35;
    private static final int QUEEN_ROOK_7TH_RANK = 100;
    private static final int HUNG_PENALTY = 20;

    /** king safety stuff here
     *  ideas from Ed Schroeder
     */

    private static final long kingZoneMask = 7 | 7 << 8 | 7 << 16;
	 private static final int[] kingFileShifts = { 0, 0, 1, 2, 3, 4, 5, 5 };

	 /** king safety penalty - formula based on attack pattern and protection patterns */
    private static final int[] kingSafetyEval = {   0,  2,  3,  6, 12, 18, 25, 37, 50, 75,
                                                100,125,150,175,200,225,250,275,300,325,
                                                350,375,400,425,450,475,500,525,550,575,
                                                600,600,600,600,600};


    /** table indexed by attack pattern on king, returns a value used in formula to calculate index to
     * above kingSafetyEval table
     */
    private static final byte TABLE [] = {
  //      . P N N R R R R Q Q Q Q Q Q Q Q 
  //            P   P N N   P N N R R R R 
  //                    P       P   P N N
  //												 P
          0,0,0,1,0,1,2,2,1,2,2,2,2,2,3,3 };

   /** values used to calculate indes into above kingSafetyEval table
    * these are based on the king position
    * notice if the king is flushed into the middle, these values go way up
    */
    private static final int KingAttackVal[] = new int[] {2,0,0,0,0,0,0,2,
                                                          2,1,1,1,1,1,1,2,
                                                          4,3,3,3,3,3,3,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4};

    /** king safety penalty */
    private static final int KING_PAWN_OPEN = 5;
    private static final int KING_PAWN_SEMI_OPEN = 5;

    /** bonus for pawns sheltering the king */
    private static final int[] PawnProtection = {0,13,5,3,1,0,0,0};

    /** squares of interest around white king */
    private static long whiteKingZone;
    /** squares of interest around black king */
    private static long blackKingZone;

	 private static int pawnShield;

    private static final int LAZY_MARGIN = 80;

	 private static final int[] PawnThreatValues = { 35, 20, 20, 55, 0, 0 };

	 /** knight piece square tables */
    private static final int KnightVals[] = new int[]	{-2,-2,-2,-2,-2,-2,-2,-2,
                                                        -2,1,1,1,1,1,1,-2,
                                                        -2,1,4,4,4,4,1,-2,
                                                        -2,1,5,6,6,5,1,-2,
                                                        -2,1,6,8,8,6,1,-2,
                                                        -2,1,6,8,8,6,1,-2,
                                                        -2,1,1,1,1,1,1,-2,
                                                        -2,-2,-2,-2,-2,-2,-2,-2};


    /** endgame king piece square tables */
    private static final int kingVals[] = new int[]     {-2,-2,-2,-2,-2,-2,-2,-2,
                                                        -2,1,1,1,1,1,1,-2,
                                                        -2,1,4,4,4,4,1,-2,
                                                        -2,1,4,8,8,4,1,-2,
                                                        -2,1,4,8,8,4,1,-2,
                                                        -2,1,4,4,4,4,1,-2,
                                                        -2,1,1,1,1,1,1,-2,
                                                        -2,-2,-2,-2,-2,-2,-2,-2};

  /** pawn piece square tables */
    private static final int PawnVals[][] =					{{0, 0, 0, 0, 0, 0, 0, 0,
                                                         0, 0, 0, -10, -10, 0, 0, 0,
                                                         1, 1, 1, 10, 10, 1, 1, 1,
                                                         3, 3, 3, 13, 13, 3, 3, 3,
                                                         6, 6, 6, 13, 13, 6, 6, 6,
                                                         10, 10, 10, 20, 20, 10, 10, 10,
                                                         10, 10, 10, 20, 20, 10, 10, 10,
                                                         0, 0, 0, 0, 0, 0, 0, 0} ,

																			{0, 0, 0, 0, 0, 0, 0, 0,
                                                         7, 7, 7, 8, 8, 7, 7, 7,
                                                         9, 9, 9, 10, 10, 9, 9, 9,
                                                         15, 15, 15, 22, 22, 15, 15, 15,
                                                         18, 18, 18, 26, 26, 18, 18, 18,
                                                         22, 22, 22, 30, 30, 22, 22, 22,
                                                         30, 30, 30, 35, 35, 30, 30, 30,
                                                         0, 0, 0, 0, 0, 0, 0, 0}};

/** outpost bonus table */
	 private static final int KNIGHT_OUTPOST = 0;
	 private static final int BISHOP_OUTPOST = 1;

	 private static final int OutpostBonus[][] =				{{0, 0, 0, 0, 0, 0, 0, 0,
                                                         0, 0, 0, 0, 0, 0, 0, 0,
                                                         0, 0, 2, 5, 5, 2, 0, 0,
                                                         0, 3, 10, 10, 10, 10, 3, 0,
                                                         0, 4, 12, 15, 15, 12, 4, 0,
                                                         0, 3, 10, 12, 12, 10, 3, 0,
                                                         0, 0, 0, 0, 0, 0, 0, 0,
																			0, 0, 0, 0, 0, 0, 0, 0} ,

																			{0, 0, 0, 0, 0, 0, 0, 0,
                                                         0, 0, 0, 0, 0, 0, 0, 0,
                                                         0, 0, 2, 5, 5, 2, 0, 0,
                                                         0, 3, 10, 10, 10, 10, 3, 0,
                                                         0, 4, 12, 15, 15, 12, 4, 0,
                                                         0, 3, 10, 12, 12, 10, 3, 0,
                                                         0, 0, 0, 0, 0, 0, 0, 0,
																			0, 0, 0, 0, 0, 0, 0, 0} };


    /* the follwoing are the components of the total evaluation score */

    /** total evaluated score */
    private static int finalScore;

    /** material eval term */
    private static int material;

    private static long passBits;

	 /** pawn eval term */
    private static int[] pawnScore = new int[2];

    /** passed pawn bonus */
    private static int[] passScore = new int[2];

    private static int threats;

	 /** weak pawn attack eval term */
    private static int weakAttackScore;

    /** hung piece eval term */
    private static int hungPenalty;

    /** centre eval term */
    private static int centre;

    /* mobility eval term */
    private static int[] mobility = new int[2];

    /** development bonus during opening of game */
    private static int develop;

    /** castline bonus */
    private static int castle;

    /** trapped eval penalty */
    private static int trapped;

    /** king safety eval terms */
	 private static int kingSafety;

    /** end game king safety eval term */
    private static int endKingSafety;

    /** tempo eval term */
    private static int tempo;

    /** bishop eval term */
    private static int bishopEval;

    /* knight eval term */
    private static int knightEval;

    /* queen eval term */
    private static int queenEval;

    /* rook eval term */
    private static int[] rookEval = new int[2];

    /** position of black king */
    private static int bKingPos;

    /** position of white king */
    private static int wKingPos;

    /** value between 0.0 and 1.0, 1.0 represents the end game, 0.0 the beginning */
    private static float endGameCoefficient;
	 private static float midGameCoefficient;

    private static long combinedOutposts;
	 private static long[] arrOutposts = new long[2];

	 /**
     * Constructor Evaluation2
     *
     * grabs a reference to the instantiated Board object
     *
     */
    public Evaluation2() {
        chessBoard = Board.getInstance();
    }

    /**
     * Method clearPawnHash
     *
     * calls the pawn hash table's method to clear its entries
     *
     */
    public static final void clearPawnHash() {
    	PawnTable.clearPawnHash();
    }

    /**
     * Method clearEvalHash
     *
     * calls the eval hash table's method to clear its entries
     *
     */
    public static final void clearEvalHash() {
    	EvalTable.clearEvalHash();
		EvalTableLazy.clearEvalHash();
    }

    /**
     * Method reSizeEvalHash
     *
     * creates a new eval hash - usually due to the size of the hash being adjusted
     * before the start of the game by a UCI command
     *
     */
    public static final void reSizeEvalHash() {
        EvalTable = new TransTable(Global.EvalHASHSIZE,2);
    }

    /**
     * Method reSizePawnHash
     *
     * creates a new pawn hash - usually due to the size of the hash being adjusted
     * before the start of the game by a UCI command
     *
     */
    public static final void reSizePawnHash() {
        PawnTable = new TransTable(Global.PawnHASHSIZE,1);
    }

	/**
	* Method getEval
	*
	* this calls evaluation functions and sums the terms and returns the final evaluation
	* from the point of view of the side moving
	*
	* @param int side - the side to move
	*
	* @return int - the evaluation score
	*
	*/
	public static final int getEval(int side, int alpha, int beta)
	{
		/** see if there is a stored value in the eval hash table */
		int evalKey = (int)(chessBoard.hashValue % Global.EvalHASHSIZE);
		evalKey = Math.abs(evalKey);

		if(EvalTable.hasEvalHash(evalKey,chessBoard.hashValue))
		{
			int value = EvalTable.getEvalValue(evalKey);
			if((value & TransTable.LAZY_BIT) != 0)
			{
				value ^= TransTable.LAZY_BIT;
				value -= 21000;
				if( value < alpha )
				{
					return value;
				}
				else if( value >= beta )
				{
				  return value;
				}
			}
			else
			{
				return value - 21000;
			}
		}
		/** get the material score */
		material = chessBoard.GetRawMaterialScore();
		if( material > 100000)
			return 0;

		int lazyMargin = LAZY_MARGIN +  chessBoard.GetLazyPieceTotals() * 3;
		if( endGameCoefficient <=  0.70 && (chessBoard.GetPieceTotal(3) > 0 || chessBoard.GetPieceTotal(9) > 0))
			lazyMargin += 30;

		/** initialize evaluation terms */
		Arrays.fill(boardAttacks[0], 0);
		Arrays.fill(boardAttacks[1], 0);
		endGameCoefficient =  Math.max(0.0f, Math.min(1.0f,((float)Global.totalValue * 0.85f - (float)chessBoard.totalValue)/ ((float)Global.totalValue * 0.65f)) );
		midGameCoefficient = 1.0f - endGameCoefficient;
		tempo = 0;
		weakAttackScore = 0;
		centre = 0;
		mobility[MIDDLE_GAME] = 0;
		mobility[END_GAME] = 0;
		develop = 0;
		castle = 0;
		trapped = 0;
		kingSafety = 0;
		endKingSafety = 0;
		passBits = 0L;
		rookEval[MIDDLE_GAME] = 0;
		rookEval[END_GAME] = 0;
		pawnScore[MIDDLE_GAME] = 0;
		pawnScore[END_GAME] = 0;
		threats = 0;
		passScore[MIDDLE_GAME] = 0;
		passScore[END_GAME] = 0;
		whiteKingZone = 0;
		blackKingZone = 0;
		pawnShield = 0;
		wKingPos = chessBoard.pieceList[4][0];
		bKingPos = chessBoard.pieceList[10][0];
		combinedOutposts = 0L;
		arrOutposts[0] = 0L;
		arrOutposts[1] = 0L;

		/** set the development and castle scores */
		setDevelopmentBonus();

		/** see if there is a stored value in the pawn hash table */
		long hash = chessBoard.getPawnHash();
		boolean hasHash = false;
		if(hash > 0)
		{
			int key = (int)(hash % Global.PawnHASHSIZE);
			key = Math.abs(key);

			hasHash = PawnTable.hasPawnHash(key,chessBoard.getPawnHash());

			if(hasHash) {               /** extract pawn info from the hash */
				pawnScore[MIDDLE_GAME] = PawnTable.GetPawnValueMiddle(key);
				pawnScore[END_GAME] = PawnTable.GetPawnValueEnd(key);
				passBits = PawnTable.getPawnPassed(key);
				boardAttacks[Global.COLOUR_WHITE][PAWN_BOARD] = boardAttacks[Global.COLOUR_WHITE][ALL_BOARD] = PawnTable.getWhitePawnAttack(key);
				boardAttacks[Global.COLOUR_BLACK][PAWN_BOARD] = boardAttacks[Global.COLOUR_BLACK][ALL_BOARD] = PawnTable.getBlackPawnAttack(key);
				centre = PawnTable.GetPawnCenterScore(key);
				passScore[MIDDLE_GAME] = PawnTable.GetPassPhase1Mid(key);
				passScore[END_GAME] = PawnTable.GetPassPhase1End(key);
				pawnShield = PawnTable.GetPawnShield(key);
				combinedOutposts = PawnTable.GetPawnOutposts(key);
				arrOutposts[Global.COLOUR_WHITE] = (combinedOutposts >> 16) & 0x0000ffffffff0000L;
				arrOutposts[Global.COLOUR_BLACK] = (combinedOutposts << 16) & 0x0000ffffffff0000L;
			} else {               /** or calculate the pawn info */
				SetPawnAttack(Global.COLOUR_WHITE);
				SetPawnAttack(Global.COLOUR_BLACK);
				GetPawnsScore(Global.COLOUR_WHITE, chessBoard.whitepawns, chessBoard.blackpawns);
				pawnScore[MIDDLE_GAME] *= -1;
				pawnScore[END_GAME] *= -1;
				GetPawnsScore(Global.COLOUR_BLACK, chessBoard.blackpawns, chessBoard.whitepawns);

				/** set king attack pattern and calculate the king attack zone */
				SetKingEval(Global.COLOUR_WHITE);
				SetKingEval(Global.COLOUR_BLACK);

				//if(chessBoard.wCastle <= Global.CASTLED || chessBoard.bCastle <= Global.CASTLED)
				//{
				pawnShield = -GetKingPawnShield(Global.COLOUR_WHITE, whiteKingZone, chessBoard.whitepawns, chessBoard.blackpawns, wKingPos);
				pawnShield += GetKingPawnShield(Global.COLOUR_BLACK, blackKingZone, chessBoard.blackpawns, chessBoard.whitepawns, bKingPos);
				//}

				/** calculate passed pawn bonuses..which EXCLUDE attack square calculation...needed for lazy eval*/
				GetPassedPawnScorePhase1(passBits & chessBoard.whitepawns, Global.COLOUR_WHITE);
				GetPassedPawnScorePhase1(passBits & chessBoard.blackpawns, Global.COLOUR_BLACK);
				GenerateOutpostSquares();
				/** special case trap penalties for bishop and rook */
				
				PawnTable.addPawnHash(key, chessBoard.getPawnHash(), pawnScore[MIDDLE_GAME], pawnScore[END_GAME], centre,  passScore[MIDDLE_GAME], passScore[END_GAME], pawnShield, passBits
					  , boardAttacks[Global.COLOUR_WHITE][PAWN_BOARD], boardAttacks[Global.COLOUR_BLACK][PAWN_BOARD], combinedOutposts);
			  }
		  }

		  //threats = PawnThreats(Global.COLOUR_WHITE, chessBoard.blackpieces);
		  //threats -= PawnThreats(Global.COLOUR_BLACK, chessBoard.whitepieces);

		  setTrapPenalties();

		  /** tempo */
        tempo = TEMPO_BONUS * side;

		  if( endGameCoefficient <  1.00)
		  {
			  int midLazyScores = pawnScore[MIDDLE_GAME] + passScore[MIDDLE_GAME] + develop + castle + pawnShield;
			  int endLazyScores = pawnScore[END_GAME] + passScore[END_GAME] + endKingSafety ;
			  int allLazyScores = tempo + material + trapped + threats;

			  int finalLazyScore = side * (int)((float)midLazyScores * midGameCoefficient + (float)endLazyScores * (float)endGameCoefficient + (float)allLazyScores);

			  if( (finalLazyScore + lazyMargin) < alpha )
			  {
					EvalTable.AddEvalHashLazy(evalKey, chessBoard.hashValue, finalLazyScore + lazyMargin + 21000);
					return finalLazyScore + lazyMargin;
			  }
			  else if( finalLazyScore - lazyMargin >= beta )
			  {
				  EvalTable.AddEvalHashLazy(evalKey, chessBoard.hashValue, finalLazyScore - lazyMargin + 21000);
				  return  finalLazyScore - lazyMargin;
			  }
		  }

		  if( hasHash )
		  {
		     SetKingEval(Global.COLOUR_WHITE);
			  SetKingEval(Global.COLOUR_BLACK);
		  }

		  /** major and minor piece evaluations */
        long mobilityAreaWhite = ~(boardAttacks[Global.COLOUR_BLACK][PAWN_BOARD] | chessBoard.whitepieces);
		  long mobilityAreaBlack = ~(boardAttacks[Global.COLOUR_WHITE][PAWN_BOARD] | chessBoard.blackpieces);
		  
		  knightEval = -GetKnightEval(Global.COLOUR_WHITE, mobilityAreaWhite, bKingPos);
        knightEval += GetKnightEval(Global.COLOUR_BLACK, mobilityAreaBlack, wKingPos);
		  queenEval = -GetQueenEval(Global.COLOUR_WHITE, mobilityAreaWhite, wKingPos);
        queenEval += GetQueenEval(Global.COLOUR_BLACK, mobilityAreaBlack, bKingPos);
        bishopEval = -GetBishopEval(Global.COLOUR_WHITE, mobilityAreaWhite, bKingPos);
        bishopEval += GetBishopEval(Global.COLOUR_BLACK, mobilityAreaBlack, wKingPos);
		  GetRookEval(Global.COLOUR_WHITE, chessBoard.whitequeen, mobilityAreaWhite, chessBoard.whitepawns, chessBoard.blackpawns, bKingPos);
		  rookEval[MIDDLE_GAME] *= -1;
		  rookEval[END_GAME] *= -1;
		  GetRookEval(Global.COLOUR_BLACK, chessBoard.blackqueen, mobilityAreaBlack, chessBoard.blackpawns, chessBoard.whitepawns, wKingPos);
		  
		  //threats += Threats(Global.COLOUR_WHITE, chessBoard.blackpieces, chessBoard.blackpawns, side == Global.COLOUR_WHITE);
		  //threats -= Threats(Global.COLOUR_BLACK, chessBoard.whitepieces, chessBoard.whitepawns, side == Global.COLOUR_BLACK);
		  
		  endKingSafety = GetEndGameKing();

		  if( endGameCoefficient <=  0.70)
		  {
				if(chessBoard.GetPieceTotal(9) > 0)
					kingSafety = GetKingSafety(Global.COLOUR_WHITE, wKingPos, whiteKingZone);
			   if(chessBoard.GetPieceTotal(3) > 0)
					kingSafety -= GetKingSafety(Global.COLOUR_BLACK, bKingPos, blackKingZone);
		  }

		   /** calculate passed pawn bonuses..which INCLUDE attack square calculation...needed for lazy eval*/

		  //if( endGameCoefficient >=  0.50)
		  //{
			GetPassedPawnScorePhase2(passBits & chessBoard.whitepawns, Global.COLOUR_WHITE);
			GetPassedPawnScorePhase2(passBits & chessBoard.blackpawns, Global.COLOUR_BLACK);
		  //}
		  /** hung scores */
        if(side == 1) {
            hungPenalty = HungPieces(Global.COLOUR_BLACK, chessBoard.blackpieces ^ chessBoard.blackking);
		  } else {
           hungPenalty = -HungPieces(Global.COLOUR_WHITE, chessBoard.whitepieces ^ chessBoard.whiteking);
       }

		  if(centre < 0)
			  centre = CenterScoreArray[-centre] * -1;
		  else
			  centre = CenterScoreArray[centre];

		  int midScores = pawnScore[MIDDLE_GAME] + passScore[MIDDLE_GAME] + centre + mobility[MIDDLE_GAME] + develop + castle + kingSafety + pawnShield + rookEval[MIDDLE_GAME];
		  int endScores = pawnScore[END_GAME] + passScore[END_GAME] + endKingSafety + rookEval[END_GAME] + mobility[END_GAME];
		  int allScores = bishopEval + knightEval + queenEval + tempo + hungPenalty + material + trapped + threats;
		  
		  finalScore = side * (int)((float)midScores * midGameCoefficient + (float)endScores * (float)endGameCoefficient + (float)allScores);

        /** store the score in the eval hashtable */
        EvalTable.AddEvalHash(evalKey, chessBoard.hashValue, finalScore + 21000);

        return finalScore;
    }

   public static final int PawnThreats(int side, long pieces)
	{
		int pawnThreatScore = 0;
		long attackedPieces = boardAttacks[side][PAWN_BOARD] & pieces;
		while(attackedPieces != 0)
		{
			long bit = attackedPieces & -attackedPieces;
			attackedPieces ^= bit;
			int position = Long.numberOfTrailingZeros(bit);
			pawnThreatScore += PawnThreatValues[chessBoard.piece_in_square[position] % 6];
		}
		return pawnThreatScore;
	}


	public static final int Threats(int side, long enemyPieces, long enemyPawns, boolean moving)
	{
		enemyPieces ^= enemyPawns;
		long loosePawns = enemyPawns & ~boardAttacks[(side+1)&1][ALL_BOARD];
		long loosePieces = enemyPieces & (~boardAttacks[(side+1)&1][PAWN_BOARD] | boardAttacks[side][PAWN_BOARD]);

		long hanging = (loosePawns | loosePieces) & boardAttacks[side][ALL_BOARD];
		int threatScore = 0;
		while(hanging != 0)
		{
			long bit = hanging & -hanging;
			hanging ^= bit;
			int pos = Long.numberOfTrailingZeros(bit);
			threatScore += 5 + Global.values[chessBoard.piece_in_square[pos]%6] / 60;
		}
		return threatScore;
	}
		//long weakPieces = (enemyPieces & ~enemyPawns) & ~boardAttacks[(side+1)&1][PAWN_BOARD] & boardAttacks[side][ALL_BOARD];
		//if( weakPieces == 0) return 0;

		/*int threatScore = 0;
		//int numberThreats = 0;
		for(int i=KNIGHT_BOARD; i <= QUEEN_BOARD; i++)
		{
			//if( weakPieces == 0) break;
			long attacked = weakPieces & boardAttacks[ side ][i];
			//weakPieces ^= attacked;
			while(attacked != 0)
			{
				long bit = attacked & -attacked;
				attacked ^= bit;
				int pos = Long.numberOfTrailingZeros(bit);
				threatScore += ThreatBonuses[i-1][chessBoard.piece_in_square[pos]%6];

			}
		}

		/*if(!moving)
		{
			if(numberThreats == 2)
				threatScore *= 2;
			else if(numberThreats > 2)
				threatScore *= 3;
		}*/
	


	/**
     * Method printEvalTerms()
     *
     * used to debug the evaluation routine by examining each term
     *
     */
    public static final void printEvalTerms() {
        System.out.println("score is "+finalScore);
        System.out.println("material is "+material);
        System.out.println("pawn score mid is "+pawnScore[MIDDLE_GAME]);
		  System.out.println("pawn score end is "+pawnScore[END_GAME]);
		  System.out.println("weak attack is "+weakAttackScore);
        System.out.println("mobility middle is "+mobility[MIDDLE_GAME]);
		  System.out.println("mobility end is "+mobility[END_GAME]);
        System.out.println("center is "+centre * CENTER_BONUS);
        System.out.println("develop is "+develop);
        System.out.println("castle is "+castle);
        System.out.println("trapped is "+trapped);
        System.out.println("king safety is "+kingSafety);
        System.out.println("tempo is "+tempo);
        System.out.println("hung is "+hungPenalty);
        System.out.println("passbits are "+ passBits);
		  System.out.println("pass score mid is "+passScore[MIDDLE_GAME]);
		  System.out.println("pass score end is "+passScore[END_GAME]);
        System.out.println("bishop eval is "+bishopEval);
        System.out.println("knight eval is "+knightEval);
        System.out.println("rook eval mid is "+ rookEval[MIDDLE_GAME]);
		  System.out.println("rook eval end is "+ rookEval[END_GAME]);
        System.out.println("queen eval is "+queenEval);
		  System.out.println("pawn shield is "+pawnShield);
        System.out.println("end game coefficient is "+endGameCoefficient);
		  System.out.println("end king safety is "+endKingSafety);
		  System.out.println("black king zone is "+ blackKingZone);
		  System.out.println("white king zone is "+ whiteKingZone);
		  System.out.println("threats are "+ threats);
        System.out.println("total value is "+chessBoard.totalValue);
    }

    /**
     * Method setTrapPenalties
     *
     * method recognizes several trapped piece patterns for bishops and rooks
     *
     */
    private static final void setTrapPenalties() {
       if((chessBoard.whitebishops & Global.set_Mask[48]) != 0) {
            if (((chessBoard.blackpawns & Global.set_Mask[41]) != 0) && ((chessBoard.blackpawns & Global.set_Mask[50]) != 0))
                trapped += 150;
        }
        if((chessBoard.whitebishops & Global.set_Mask[55]) != 0) {
            if (((chessBoard.blackpawns & Global.set_Mask[53]) != 0) && (chessBoard.blackpawns & Global.set_Mask[46]) != 0)
                trapped += 150;
        }
        if((chessBoard.blackbishops & Global.set_Mask[15]) != 0) {
            if(((chessBoard.whitepawns & Global.set_Mask[13])!= 0) && ((chessBoard.whitepawns & Global.set_Mask[22])!= 0))
                trapped -= 150;
        }
        if((chessBoard.blackbishops & Global.set_Mask[8]) != 0) {
            if((chessBoard.whitepawns & Global.set_Mask[10]) != 0 && ((chessBoard.whitepawns & Global.set_Mask[17])!= 0))
                trapped -= 150;
        }
        if(chessBoard.wCastle == Global.NO_CASTLE) {
            if((chessBoard.whiterooks & Global.wRookTrap[0]) != 0) {
                if(wKingPos < 4 && wKingPos > 0)
                    trapped += 50;
            }
            if((chessBoard.whiterooks & Global.wRookTrap[1]) != 0) {
                if(wKingPos > 4 && wKingPos < 7)
                    trapped += 50;
            }
        }
        if(chessBoard.bCastle == Global.NO_CASTLE) {
            if((chessBoard.blackrooks & Global.bRookTrap[0]) != 0) {
                if(bKingPos < 60 && bKingPos > 56)
                    trapped -= 50;
                }
            if((chessBoard.blackrooks & Global.bRookTrap[1]) != 0)  {
                if(bKingPos > 60 && bKingPos < 63)
                    trapped -= 50;
                }
            }
        }

    /**
     * Method setDevelopmentBonus
     *
     * his method sets development and castling bonus at the start of the game
     *
     */
    private static final void setDevelopmentBonus() {
       switch(chessBoard.wCastle) {
			case(Global.CASTLED):
				castle -= 40;
			break;
			case(Global.SHORT_CASTLE):
				if((chessBoard.whitequeen & Global.set_Mask[3])!= 0)
					develop-=20;
				castle -= 10;
			break;
			case(Global.LONG_CASTLE):
				if((chessBoard.whitequeen & Global.set_Mask[3])!= 0)
					develop-=20;
				castle -= 10;
			break;
			case(Global.BOTH_CASTLE):
				if((chessBoard.whitequeen & Global.set_Mask[3])!= 0)
					develop-=20;
				castle -= 20;
			break;
		}

		switch(chessBoard.bCastle) {
			case(Global.CASTLED):
				castle += 40;
			break;
			case(Global.SHORT_CASTLE):
				if((chessBoard.blackqueen & Global.set_Mask[59]) != 0)
					develop+=20;
				castle += 10;
			break;
			case(Global.LONG_CASTLE):
				if((chessBoard.blackqueen & Global.set_Mask[59]) != 0)
					develop+=20;
				castle += 10;
			break;
			case(Global.BOTH_CASTLE):
				if((chessBoard.blackqueen & Global.set_Mask[59]) != 0)
					develop+=20;
				castle += 20;
			break;
		}
	 }
    /**
     * Method setPawnAttack
     *
     * his method sets the WB and BB structures for pawns
     *
     */

   private static final void SetPawnAttack(int side)
	{
		int leftOffset = side == Global.COLOUR_WHITE ? 7 : -9;
		int rightOffset = side == Global.COLOUR_WHITE ? 9 : -7;
		int piece = side == Global.COLOUR_WHITE ? 5 : 11;
		
		for(int i=0; i < chessBoard.pieceTotals[piece]; i++)
		{
			int position = chessBoard.pieceList[piece][i];
			if(position % 8 > 0) {
            boardAttacks[side][PAWN_BOARD] |= (long)1 << (position + leftOffset);
				boardAttacks[side][ALL_BOARD] |= (long)1 << (position + leftOffset);   
         }
         if(position % 8 < 7) {
            boardAttacks[side][PAWN_BOARD] |= (long)1 << (position + rightOffset);
				boardAttacks[side][ALL_BOARD] |= (long)1 << (position + rightOffset);
         }
      }
		centre += Long.bitCount(CENTER_BITS & boardAttacks[side][PAWN_BOARD]) * (-1 + side * 2);
   }

	/**
	* Method getPawnsScore
	*
	* this method calculates penalties for doubled pawns, isolated pawns, weak pawns and the positional score
	* for white pawns
	*
	* @return int - the pawn positional eval score
	*
	*/
	private static final void GetPawnsScore(int side, long pawns, long enemyPawns) {
		boolean isolated, backward, chain, opposed, passed;
		long temp = pawns;
		int behindValue;
		int forwardValue;
		if(side == Global.COLOUR_WHITE )
		{
			behindValue =  -1;
			forwardValue = 1;
		}
		else
		{
			behindValue =  1;
			forwardValue = -1;
		}

		while(temp != 0)
		{
			long piece = temp & -temp;
			temp ^= piece;
			int position = Long.numberOfTrailingZeros(piece);
			int rank = position >> 3;
			int file = position & 7;
			int relativeRank = RelativeRanks[side][rank];//Board.GetRelativeRank(side, position);
			int relativePosition = (relativeRank << 3) + file;// Board.GetRelativePosition(side, position);
			int rankBehind = rank + behindValue;
			int rankForward = rank + forwardValue;
			
			//positional score
			pawnScore[MIDDLE_GAME] += PawnVals[MIDDLE_GAME][relativePosition];
			pawnScore[END_GAME] += PawnVals[END_GAME][relativePosition];

			if((Global.passed_masks[side][position] & enemyPawns) == 0)
			{
				passBits |= piece;
				passed = true;
			}
			else
			{
				passed = false;
			}

			//doubled
			long friendPawnsInFront =  Global.mask_in_front[side][position] & pawns;
			int numberPawns = Long.bitCount(friendPawnsInFront);

			if(numberPawns >= 1)
			{
				if(numberPawns == 1)
				{
					pawnScore[MIDDLE_GAME] -= DoubledPawn[MIDDLE_GAME][file];
					pawnScore[END_GAME] -= DoubledPawn[END_GAME][file];
				}
				else
				{
					pawnScore[MIDDLE_GAME] -= DoubledPawn[MIDDLE_GAME][file] * 2;
					pawnScore[END_GAME] -= DoubledPawn[END_GAME][file] * 2;
				}
			}

			//chain
			if((pawns & Global.neighbour_files[file] & (Global.rankMasks[rankBehind] | Global.rankMasks[rank] )) != 0)
			{
				chain = true;
				pawnScore[MIDDLE_GAME] += ChainPawn[MIDDLE_GAME][file];
				pawnScore[END_GAME] += ChainPawn[END_GAME][file];
			}
			else
			{
				chain = false;
			}

			//opposed
			if((enemyPawns & Global.mask_in_front[side][position]) != 0)
			{
				opposed = true;
			}
			else
			{
				opposed = false;
			}

			//isolated
			if((Global.neighbour_files[file] & pawns) == 0)
			{
				isolated = true;
				pawnScore[MIDDLE_GAME] -= IsolatedPawn[MIDDLE_GAME][file];
				pawnScore[END_GAME] -= IsolatedPawn[END_GAME][file];
				if(!opposed)
				{
					pawnScore[MIDDLE_GAME] -= IsolatedPawn[MIDDLE_GAME][file] / 2;
					pawnScore[END_GAME] -= IsolatedPawn[END_GAME][file] / 2;
				}
			}
			else
			{
				isolated = false;
			}

			if ( !( passed | isolated | chain)
				&& ((boardAttacks[(side+1)%2][PAWN_BOARD] & ((long)1 << position)) == 0)
				&& ((pawns & Global.neighbour_files[file] & Global.mask_forward[(side+1) & 1][position]) == 0))
			{
				long forwardAttacks = Global.rankMasks[rankForward] & Global.neighbour_files[file];// & Global.mask_forward[side][position];
				while ((forwardAttacks & (enemyPawns | pawns)) == 0)
				{
					if(side == Global.COLOUR_WHITE)
					{
						forwardAttacks <<= 8;
					}
					else
					{
						forwardAttacks >>= 8;
					}
				}
				long nextRankForward = side == Global.COLOUR_WHITE ? forwardAttacks << 8 : forwardAttacks >> 8;
				if(((forwardAttacks | nextRankForward) & enemyPawns ) != 0)
				{
					backward = true;
				}
				else
				{
					backward = false;
				}
			}
			else
			{
				backward = false;
			}

			if(backward)
			{
				pawnScore[MIDDLE_GAME] -= WeakPawn[MIDDLE_GAME][file];
				pawnScore[END_GAME] -= WeakPawn[END_GAME][file];

				if(!opposed)
				{
					pawnScore[MIDDLE_GAME] -= WeakPawn[MIDDLE_GAME][file] / 2;
					pawnScore[END_GAME] -= WeakPawn[END_GAME][file] / 2;
				}
			}

			//candidate passer
			if(!opposed && !passed)
			{
				int posForward = side == Global.COLOUR_WHITE  ? position + 8 : position - 8;

				long helperPawns = Global.neighbour_files[file] & Global.mask_forward[(side+1) & 1][posForward] & pawns;
				if(helperPawns != 0)
				{
					long opponentPawns = Global.neighbour_files[file] & Global.mask_forward[side][position] & enemyPawns;
					if(Long.bitCount(helperPawns) >= Long.bitCount(opponentPawns))
					{
						pawnScore[MIDDLE_GAME] += CandidatePawnBonus[MIDDLE_GAME][relativeRank];
						pawnScore[END_GAME] += CandidatePawnBonus[END_GAME][relativeRank];
					}
				}
			}
		}
	}


	 /**
     * Method GePassedPawnScorePhase2
     *
     * calculates the passed pawn bonus for passed pawns
     * this is based on many dynamic features...
	  * phase2 is done after our lazy eval check as we
	  * need the square attack information computed when evaluating pieces
     *
     * @param long passers - the 64 bits packed with passed pawns
     *
     * @return int - the passed pawn bonus score
     *
     */
	private static final void GetPassedPawnScorePhase2(long passers, int side)
	{
		long piece;
		long friendPawns;
		long friendPieces;
		long enemyPieces;
		int position;
		int nextSquareIncrement;
		int enemyKingPos;
		int friendKingPos;

		if(side == Global.COLOUR_WHITE)
		{
			friendPawns = chessBoard.whitepawns;
			friendPieces = chessBoard.whitepieces;
			enemyPieces = chessBoard.blackpieces;
			nextSquareIncrement = 8;
			enemyKingPos = bKingPos;
			friendKingPos = wKingPos;
		}
		else
		{
			friendPawns = chessBoard.blackpawns;
			friendPieces = chessBoard.blackpieces;
			enemyPieces = chessBoard.whitepieces;
			nextSquareIncrement = -8;
			enemyKingPos = wKingPos;
			friendKingPos = bKingPos;
		}

		while(passers != 0)
		{
			piece = passers & -passers;
			passers ^= piece;
			position = Long.numberOfTrailingZeros(piece);
			int blockPos = position + nextSquareIncrement;
			int rank = position>>3;
			int relativeRank = RelativeRanks[side][rank];
			int endScore = PassedPawnBonus[END_GAME][relativeRank];
			int endBonus = 0;
			int kingEndBonus = 0;

			//apply score based on path to queen
			long forwardPath = Global.mask_in_front[side][position];
			long unsafeSquares = forwardPath & (boardAttacks[(side+1) & 1][ALL_BOARD] | enemyPieces);
			long defendedSquares = forwardPath & boardAttacks[side][ALL_BOARD];

			if(unsafeSquares == 0)
			{
				endBonus += endScore / 3;
			}
			else if((unsafeSquares & defendedSquares) == unsafeSquares)
			{
				endBonus += endScore / 5;
			}
			else
			{
				if( (forwardPath & friendPieces) == 0)
				{
					endBonus += endScore / 7;
				}
			}

			//doubled pawn - reduce bonus
			if((Global.mask_in_front[side][position] & friendPawns) != 0)
				endBonus /= 3;

			kingEndBonus = -chessBoard.getDistance(friendKingPos, blockPos) * endBonus / 20;
			kingEndBonus += chessBoard.getDistance(enemyKingPos, blockPos) * endBonus / 20;

			passScore[END_GAME] += (kingEndBonus + endBonus) * (-1 + side * 2);
		}
	}




	 /**
     * Method GePassedPawnScorePhase1
     *
     * calculates the passed pawn bonus for passed pawns
     * this is based on many dynamic features...
	  * phase1 is necessary for lazy eval as we calculate everything which doens't
	  * need the square attack information
     *
     * @param long passers - the 64 bits packed with passed pawns
     *
     * @return int - the passed pawn bonus score
     *
     */
	private static final void GetPassedPawnScorePhase1(long passers, int side)
	{
		long piece;
		long friendPawns;
		int position;
		int nextSquareIncrement;
		int enemyKingPos;
		int friendKingPos;

		if(side == Global.COLOUR_WHITE)
		{
			friendPawns = chessBoard.whitepawns;
			nextSquareIncrement = 8;
			enemyKingPos = bKingPos;
			friendKingPos = wKingPos;
		}
		else
		{
			friendPawns = chessBoard.blackpawns;
			nextSquareIncrement = -8;
			enemyKingPos = wKingPos;
			friendKingPos = bKingPos;
		}

		while(passers != 0)
		{
			piece = passers & -passers;
			passers ^= piece;
			position = Long.numberOfTrailingZeros(piece);
			int blockPos = position + nextSquareIncrement;
			int rank = position>>3;
			int file = position&7;
			int relativeRank = RelativeRanks[side][rank];//Board.GetRelativeRank(side, position);
			int middleScore = PassedPawnBonus[MIDDLE_GAME][relativeRank];
			int endScore = PassedPawnBonus[END_GAME][relativeRank];
			int kingEndBonus = 0;

			//bonus for supporting pawns
			long neighbourPawns = friendPawns & Global.neighbour_files[file] & (Global.rankMasks[rank] | Global.rankMasks[Board.GetPreviousRank(side, rank)]);
			int neighbourBonusMid = 0;
			int neighbourBonusEnd = 0;
			while(neighbourPawns != 0)
			{
				long neighbour = neighbourPawns & -neighbourPawns;
				neighbourPawns ^= neighbour;
				int neighbourPos = Long.numberOfTrailingZeros(neighbour);
				if(neighbourPos / 8 == rank)
				{
					neighbourBonusMid += middleScore / 4;
					neighbourBonusEnd += endScore / 4;
				}
				else
				{
					neighbourBonusMid += middleScore / 6;
					neighbourBonusEnd += endScore / 6;
				}
			}
			middleScore += neighbourBonusMid;
			endScore += neighbourBonusEnd;

			//doubled pawn - reduce bonus
			if((Global.mask_in_front[side][position] & friendPawns) != 0)
				endScore /= 3;

			kingEndBonus = -chessBoard.getDistance(friendKingPos, blockPos) * endScore / 20;
			kingEndBonus += chessBoard.getDistance(enemyKingPos, blockPos) * endScore / 20;

			passScore[MIDDLE_GAME] += middleScore * (-1 + side * 2);
			passScore[END_GAME] += (endScore + kingEndBonus) * (-1 + side * 2);
		}
	}


	/**
     * Method isPassedPawn
     *
     * this method determines using a bitmask whether a pawn is passed
     *
     * @param int side - the side to move
     * @param int position - the position of the pawn
     *
     * @return boolean - true if pawn is passed
     *
     */
    public static final boolean isPassedPawn(int side, int position) {
        if(side == -1) {
            return ((Global.whitePassedPawnMasks[position] & chessBoard.blackpawns) == 0);
        } else
            return ((Global.blackPassedPawnMasks[position] & chessBoard.whitepawns) == 0);
    }

     /**
     * Method getWKnightEval
     *
     * calculates positional score for white knight
     * adds knight attack to WB array
     *
     * @return int - knight positional score
     *
     */
	private static int GetKnightEval(int side, long mobilityArea, int enemyKingPos)
	{
		int score = 0;
		for(int i=0; i < chessBoard.pieceTotals[1 + side*6]; i++)
		{
			int position = chessBoard.pieceList[1 + side*6][i];
			int relativeRank = RelativeRanks[side][position>>3] ;
			int relativePosition = (relativeRank << 3) + (position & 7);
			if(relativeRank == 0)
				develop -= BACKRANK_MINOR *  (-1 + side * 2);
			long attacks = chessBoard.getKnightMoves(position);
			boardAttacks[side][KNIGHT_BOARD] |= attacks;
			boardAttacks[side][ALL_BOARD] |= attacks;
			long mobilitySquares = mobilityArea & attacks;
			int mobilityNumber = Long.bitCount(mobilitySquares);
			mobility[MIDDLE_GAME] += KNIGHT_MOBILITY[MIDDLE_GAME][mobilityNumber] * (-1 + side * 2);
			mobility[END_GAME] += KNIGHT_MOBILITY[END_GAME][mobilityNumber] * (-1 + side * 2);
			centre += Long.bitCount(CENTER_BITS & attacks) * (-1 + side * 2);
			score -= chessBoard.getDistance(position, enemyKingPos);
			score += KnightVals[ relativePosition ];//Board.GetRelativePosition(side, position)];
			score += GetOutpostScore(position, relativePosition, side, KNIGHT_OUTPOST);
		}
		return score;
    }

	public static void GenerateOutpostSquares()
	{
		for(int i=16; i<=47; i++)
		{
			if( (Global.mask_in_front[Global.COLOUR_WHITE][i] & Global.neighbour_files[i & 7] & chessBoard.blackpawns) == 0)
				arrOutposts[Global.COLOUR_WHITE] |= (long)1 << (i);


			if( (Global.mask_in_front[Global.COLOUR_BLACK][i] & Global.neighbour_files[i & 7] & chessBoard.whitepawns) == 0)
				arrOutposts[Global.COLOUR_BLACK] |= (long)1 << (i);
		}
		combinedOutposts = (arrOutposts[Global.COLOUR_BLACK] >> 16) | (arrOutposts[Global.COLOUR_WHITE] << 16);
	}

	public static int GetOutpostScore(int position, int relativePosition, int side, int outpostType)
	{
		if( (arrOutposts[side] & (long)1 << (position)) != 0)
		{
			int bonus = OutpostBonus[outpostType][relativePosition];
			long friendPawns = side == Global.COLOUR_WHITE ? chessBoard.whitepawns : chessBoard.blackpawns;
			if((Global.mask_in_front[side][position] & Global.neighbour_files[position & 7] & friendPawns) != 0  )
			{
				bonus += bonus/2;
			}
			return bonus;
		}
		else
			return 0;
	}

	/**
     * Method getWBishopEval
     *
     * calculates positional score for white bishop
     * adds bishop attack to WB array
     *
     * @return int - bishop positional score
     *
     */
	private static int GetBishopEval(int side, long mobilityArea, int enemyKingPos)
	{
		int score = 0;
		for(int i=0; i < chessBoard.pieceTotals[2 + side*6]; i++)
		{
			int position = chessBoard.pieceList[2 + side*6][i];
			int relativeRank = RelativeRanks[side][position>>3] ;
			int relativePosition = (relativeRank << 3) + (position & 7);
			if(RelativeRanks[side][position>>3] == 0) //if(Board.GetRelativeRank(side, position) == 0)
				develop -= BACKRANK_MINOR *  (-1 + side * 2);
			long attacks = chessBoard.getMagicBishopMoves(position);
			boardAttacks[side][BISHOP_BOARD] |= attacks;
			boardAttacks[side][ALL_BOARD] |= attacks;
			long mobilitySquares = mobilityArea & attacks;
			int mobilityNumber = Long.bitCount(mobilitySquares);
			mobility[MIDDLE_GAME] += BISHOP_MOBILITY[MIDDLE_GAME][mobilityNumber] * (-1 + side * 2);
			mobility[END_GAME] += BISHOP_MOBILITY[END_GAME][mobilityNumber] * (-1 + side * 2);
			centre += Long.bitCount(CENTER_BITS & attacks) * (-1 + side * 2);
			score -= chessBoard.getDistance(position, enemyKingPos);
			score += GetOutpostScore(position, relativePosition, side, BISHOP_OUTPOST);
		}
	return score;
	}
    /**
     * Method getWQueenEval
     *
     * calculates positional score for white queen
     * adds queen attack to WB array
     *
     * @return int - queen positional score
     *
     */
	private static int GetQueenEval(int side, long mobilityArea, int enemyKingPos)
	{
		int score = 0;
		for(int i=0; i<chessBoard.pieceTotals[3 + side*6]; i++)
		{
			int position = chessBoard.pieceList[3 + side*6][i];
			long attacks = chessBoard.getQueenMoves(position);        
			boardAttacks[side][QUEEN_BOARD] |= attacks;
			boardAttacks[side][ALL_BOARD] |= attacks;
			long mobilitySquares = mobilityArea & attacks;
			int mobilityNumber = Long.bitCount(mobilitySquares);
			mobility[MIDDLE_GAME] += QUEEN_MOBILITY[MIDDLE_GAME][mobilityNumber] * (-1 + side * 2);
			mobility[END_GAME] += QUEEN_MOBILITY[END_GAME][mobilityNumber] * (-1 + side * 2);
			centre += Long.bitCount(CENTER_BITS & attacks) * (-1 + side * 2);
			score -= 3 * chessBoard.getDistance(position, enemyKingPos);
		}
		return score;
	}


	/**
	* Method getWRookEval
	*
	* calculates positional score for white rook
	* adds rook attack to WB array
	*
	* @return int - rook positional score
	*
	*/
	private static void GetRookEval(int side, long queens, long mobilityArea, long pawns, long enemyPawns, int enemyKingPos)
	{
		int enemyKingRelativeRank = RelativeRanks[side][enemyKingPos>>3];//Board.GetRelativeRank(side, enemyKingPos);
		int oldFile = -1;
		int oldRank = -1;

		for(int i=0; i<chessBoard.pieceTotals[side*6]; i++)
		{
			int position = chessBoard.pieceList[side*6][i];
			int file = position & 7;
			int rank = position >> 3;
			int relativeRank = RelativeRanks[side][rank];//Board.GetRelativeRank(side, position);
			int nearEnemyKing = 1;
			if(enemyKingRelativeRank == 7 && relativeRank == 6 )
			{
				rookEval[MIDDLE_GAME] += ROOK_7TH_RANK;
				rookEval[END_GAME] += ROOK_7TH_RANK;

				if(rank == oldRank)
				{
					rookEval[MIDDLE_GAME] += DOUBLED_ROOKS_7TH;
					rookEval[END_GAME] += DOUBLED_ROOKS_7TH;
				}
				if((Global.rankMasks[rank] & queens) != 0)
				{
					rookEval[MIDDLE_GAME] += QUEEN_ROOK_7TH_RANK;
					rookEval[END_GAME] += QUEEN_ROOK_7TH_RANK;
				}
			}
			if((pawns & Global.fileMasks[file]) == 0 )
			{
				if(Math.abs(file - (enemyKingPos & 7)) < 2)
				{
					nearEnemyKing = 2;
				}
				if((Global.mask_behind[side][position] & queens) != 0)
				{
					rookEval[MIDDLE_GAME] += ROOK_IN_FRONT_QUEEN;
				}
				else if((Global.mask_in_front[side][position] & queens) != 0)
				{
					rookEval[MIDDLE_GAME] += ROOK_BEHIND_QUEEN;
				}
				if(file == oldFile)
				{
					rookEval[MIDDLE_GAME] += ROOK_DOUBLED * nearEnemyKing;
					rookEval[END_GAME] += ROOK_DOUBLED * nearEnemyKing;
				}
				if((enemyPawns & Global.fileMasks[file]) == 0 )          // open file
				{
					rookEval[END_GAME] += ROOK_OPEN * nearEnemyKing;
					rookEval[MIDDLE_GAME] += ROOK_OPEN * nearEnemyKing;
				}
				else
				{
					rookEval[END_GAME] += ROOK_OPEN * nearEnemyKing;
					rookEval[MIDDLE_GAME] += ROOK_SEMI * nearEnemyKing;
				}
			}
			
			long attacks = chessBoard.getMagicRookMoves(position);
			boardAttacks[side][ROOK_BOARD] |= attacks;
			boardAttacks[side][ALL_BOARD] |= attacks;
			long mobilitySquares = mobilityArea & attacks;
			int mobilityNumber = Long.bitCount(mobilitySquares);
			mobility[MIDDLE_GAME] += ROOK_MOBILITY[MIDDLE_GAME][mobilityNumber] * (-1 + side * 2);
			mobility[END_GAME] += ROOK_MOBILITY[END_GAME][mobilityNumber] * (-1 + side * 2);
			centre += Long.bitCount(CENTER_BITS & attacks) * (-1 + side * 2);
			rookEval[MIDDLE_GAME] -= chessBoard.getDistance(position, enemyKingPos);
			rookEval[END_GAME] -= chessBoard.getDistance(position, enemyKingPos);

			oldFile = file;
			oldRank = rank;
		}
	}

	/**
     * Method SetKingEval
     *
     * sets the king position, attack zone, and updates the WB table
     *
     */
    private static void SetKingEval(int side) {
		if(side == Global.COLOUR_WHITE)
		{
			int kingFile = wKingPos & 7;
			int kingRank = wKingPos >> 3;
			int fileShift = kingFileShifts[kingFile];
			int rankShift = Math.min(5, kingRank);
			whiteKingZone = kingZoneMask << ((rankShift << 3) + fileShift);
			long attacks = chessBoard.getKingMoves(wKingPos);
			boardAttacks[side][KING_BOARD] |= attacks;
			boardAttacks[side][ALL_BOARD] |= attacks;
		}
		else
		{
			int kingFile = bKingPos & 7;
			int kingRank = bKingPos >> 3;
			int fileShift = kingFileShifts[kingFile];
			int rankShift = Math.max(0, kingRank-2);
			blackKingZone = kingZoneMask << ((rankShift << 3) + fileShift);
			long attacks = chessBoard.getKingMoves(bKingPos);
			boardAttacks[side][KING_BOARD] |= attacks;
			boardAttacks[side][ALL_BOARD] |= attacks;
		}
	}

    /**
     * Method getEndGameKing
     *
     * calculates the engame score for the kings
     *
     * @return int - endgame king position score
     *
     */
	private static int GetEndGameKing() {
		int score = 0;
		score -= kingVals[wKingPos];
		score += kingVals[bKingPos];
		return score;
	}

 
	
	private static int GetKingPawnShield(int side, long kingZone, long pawns, long enemyPawns, int kingPos)
	{
		int score = 0;
		//mark king's critical files as semi open if no friend pawn is on the file
		int kingFile = kingPos & 7;
		int fileShift = kingFileShifts[kingFile];

		for(int i=0; i<3; i++)
		{
			long pawnFile = pawns & Global.fileMasks[fileShift + i];
			if( (pawnFile) != 0)
			{
				long zonePawns = pawnFile & kingZone;
				if(zonePawns != 0)
				{
					int position = side == Global.COLOUR_WHITE ? Long.numberOfTrailingZeros(zonePawns) : (63 - Long.numberOfLeadingZeros(zonePawns));
					score += PawnProtection[RelativeRanks[side][position>>3]];
				}
			}
			else
			{
				score -= KING_PAWN_SEMI_OPEN;
				if( (enemyPawns & Global.fileMasks[fileShift + i]) == 0 )
				{
					score -= KING_PAWN_OPEN;
				}
			}
		}
		return score;
	}

	 private static int GetKingSafety(int side, int kingPos, long kingZone)
	 {
		 //get all king squares attacked
		 long attacks = boardAttacks[(side+1) & 1][ALL_BOARD] & kingZone;
		 
		 /*if(side == Global.COLOUR_WHITE)
		 {
			 kingZone |= kingZone >> 8;
		 }
		 else
		 {
			 kingZone |= kingZone << 8;
		 }*/

		 long undefendedAttacks = attacks & ~(boardAttacks[side][ALL_BOARD] ^ boardAttacks[side][KING_BOARD]);
		 
		 int count = Long.bitCount(attacks) + Long.bitCount(undefendedAttacks);

		 if(count > 0)
		 {
			 int mask = 0;

			 if((boardAttacks[(side+1) & 1][PAWN_BOARD] & kingZone) != 0)
				mask |= 1;

			 if( ((boardAttacks[(side+1) & 1][KNIGHT_BOARD] | boardAttacks[( side + 1) %2][BISHOP_BOARD]) & kingZone) != 0)
				mask |= 2;

			 if((boardAttacks[(side+1) & 1][ROOK_BOARD] & kingZone) != 0)
				mask |= 4;

			 if((boardAttacks[(side+1) & 1][QUEEN_BOARD] & kingZone) != 0)
				mask |= 8;

			 count += TABLE[mask];
		 }

		 return kingSafetyEval[count + KingAttackVal[ (RelativeRanks[side][kingPos>>3] << 3 ) + ( kingPos & 7 ) ]];
	 }

	
	private static int HungPieces(int side, long piecesNoKing)
	{
		int enemySide = (side+1) & 1;
		piecesNoKing  &= boardAttacks[enemySide][ALL_BOARD];

		int hung=0;
		//long hungBits = 0;
		while(piecesNoKing != 0)
		{
			long bit = piecesNoKing & -piecesNoKing;
			piecesNoKing ^= bit;

			if( (boardAttacks[side][ALL_BOARD] & bit) == 0 )
			{
				//hungBits |= bit;
				hung++;
			}
			else
			{
				int position = Long.numberOfTrailingZeros( bit );
				switch(chessBoard.piece_in_square[position] % 6)
				{
					case 0:
						//if(( (boardAttacks[enemySide][PAWN_BOARD] | boardAttacks[enemySide][BISHOP_BOARD] | boardAttacks[enemySide][KNIGHT_BOARD] | boardAttacks[enemySide][KING_BOARD]) & bit) != 0 )
						if(( (boardAttacks[enemySide][PAWN_BOARD] | boardAttacks[enemySide][BISHOP_BOARD] | boardAttacks[enemySide][KNIGHT_BOARD] | boardAttacks[enemySide][KING_BOARD]) & bit) != 0 )
						{
							//hungBits |= bit;
							hung++;
						}
					break;

					case 1:
						if(( (boardAttacks[enemySide][PAWN_BOARD] | boardAttacks[enemySide][BISHOP_BOARD] | boardAttacks[enemySide][KING_BOARD]) & bit) != 0 )
						{
							//hungBits |= bit;
							hung++;
						}
					case 2:
						if(( (boardAttacks[enemySide][PAWN_BOARD] | boardAttacks[enemySide][KNIGHT_BOARD] | boardAttacks[enemySide][KING_BOARD]) & bit) != 0 )
						{
							//hungBits |= bit;
							hung++;
						}
					break;

					case 3:
							if(( (boardAttacks[enemySide][PAWN_BOARD] | boardAttacks[enemySide][BISHOP_BOARD] | boardAttacks[enemySide][KNIGHT_BOARD] | boardAttacks[enemySide][ROOK_BOARD]) & bit) != 0 )
							{
								//hungBits |= bit;
								hung++;
							}
					break;

				}
			}
		}

		if(hung >= 2)
		{
			if(hung == 2)
				return -HUNG_PENALTY;
			else
			 return -HUNG_PENALTY * 2;
		}
		else
			return 0;
	}
}
		/*if(hung >= 2)
		{
		//loop through the bits and find the 2nd largest piece value
			int bestValue = -2000;
			int secondBest = -2000;
			while(hungBits != 0)
			{
				long bit = hungBits & -hungBits;
				hungBits ^= bit;
				int value = Global.values[chessBoard.piece_in_square[Long.numberOfTrailingZeros(bit)]];
				if(value >= bestValue) {
					secondBest = bestValue;
					bestValue = value;
				}
				else if( value > secondBest)
					secondBest = value;
			}

			return - (HUNG_PENALTY + hung * secondBest / 20);
		}
		else
			return 0;
	}
}
		/*
		if(hung >= 2)
		{
			if(hung == 2)
				return -HUNG_PENALTY;
			else
			 return -2*HUNG_PENALTY;
		}
		else
			return 0;
	}
}
			//loop through the bits and find the 2nd largest piece value
			/*int bestValue = -2000;
			int secondBest = -2000;
			while(hungBits != 0)
			{
				long bit = hungBits & -hungBits;
				hungBits ^= bit;
				int value = Global.values[chessBoard.piece_in_square[Long.numberOfTrailingZeros(bit)]];
				if(value >= bestValue) {
					secondBest = bestValue;
					bestValue = value;
				}
				else if( value > secondBest)
					secondBest = value;
			}

			return - hung * secondBest / 12;
		}
		else
			return 0;
	}
}

	/* private static int HungPieces(int side, long bla)
	{
		long pieces;
		int enemySide = (side+1)%2;
		if(side == Global.COLOUR_WHITE)
		{
			pieces = chessBoard.whitepieces & ~chessBoard.whiteking;
			pieces &= boardAttacks[enemySide][ALL_BOARD];
		}
		else
		{
			pieces = chessBoard.blackpieces & ~chessBoard.blackking;
			pieces &= boardAttacks[enemySide][ALL_BOARD];
		}

		int hung = 0;
		int score = 0;
		long piece;
		while(pieces != 0)
		{
			piece = pieces & -pieces;
			pieces ^= piece;

			if( (boardAttacks[side][ALL_BOARD] & piece) == 0 )
			{
				hung++;
			}
			else
			{
				int position = Long.numberOfTrailingZeros(piece);
				switch(chessBoard.piece_in_square[position])
				{
					case 6:
					case 0:
					if(( (boardAttacks[enemySide][PAWN_BOARD] | boardAttacks[enemySide][BISHOP_BOARD] | boardAttacks[enemySide][KNIGHT_BOARD]) & piece) != 0 )
					{
						hung++;
					}
					break;

					case 1:
					case 2:
					case 7:
					case 8:
					if(( (boardAttacks[enemySide][PAWN_BOARD] ) & piece) != 0 )
					{
						hung++;
					}
					break;

					case 3:
					case 9:
					if(( (boardAttacks[enemySide][PAWN_BOARD] | boardAttacks[enemySide][BISHOP_BOARD] | boardAttacks[enemySide][KNIGHT_BOARD] | boardAttacks[enemySide][ROOK_BOARD]) & piece) != 0 )
					{
						hung++;
					}
					break;
				}
			}
		}
		if(hung == 2)
			score = -HUNG_PENALTY;
		else if(hung > 2)
			score = -2*HUNG_PENALTY;

		return score;
	}
}
*/
