package magnumchess;

import java.util.Arrays;

/**
 * Evaluation2.java
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



public class Evaluation2 {
    
    /** initialized instance of Board class */
    private static Board chessBoard;

    /** pawn hash table */
    private static TransTable PawnTable = new TransTable(Global.PawnHASHSIZE,1);

    /** evaluation hash table */
    private static TransTable EvalTable = new TransTable(Global.EvalHASHSIZE,2);

    private static TransTable EvalTableLazy = new TransTable(Global.EvalHASHSIZE,2);

    private static long[][] boardAttacks = new long[2][7];

    /** indices of boards for specific pieces in boardAttacks */
    private static final int PAWN_BOARD = 5;
    private static final int KNIGHT_BOARD = 1;
    private static final int BISHOP_BOARD = 2;
    private static final int ROOK_BOARD = 0;
    private static final int QUEEN_BOARD = 3;
    private static final int KING_BOARD = 4;
    private static final int ALL_BOARD = 6;

    private static final int MIDDLE_GAME = 0;
    private static final int END_GAME = 1;

    /** white passed pawn bonus based on rank */
    public static final int[][] PassedPawnBonus = {{0,7,13,17,25,38,80,0},{0,15,23,35,47,90,200,0}};

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

    /** squares of interest around kings */
    private static long[] kingZone = new long[2];

    private static int pawnShield;

    private static final int LAZY_MARGIN = 80;

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
    private static final int kingVals[] = new int[]     {-4,-2, 0, 2, 2, 0,-2,-4,
                                                         -2, 0, 2, 4, 4, 2, 0,-2,
                                                          0, 2, 4, 6, 6, 4, 2, 0,
                                                          2, 4, 6, 8, 8, 6, 4, 2,
                                                          2, 4, 6, 8, 8, 6, 4, 2,
                                                         -2, 2, 4, 6, 6, 4, 2, 0,
                                                         -2, 0, 2, 4, 4, 2, 0,-2,
                                                         -4,-2, 0, 2, 2, 0,-2,-4};

    /** pawn piece square tables */
    private static final int PawnVals[][] =         {{0, 0, 0, 0, 0, 0, 0, 0,
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

    private static final int OutpostBonus[][] =		{{0, 0, 0, 0, 0, 0, 0, 0,
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

    private static int[] kingPos = new int[2];
    private static int[] enemyKingPos = new int[2];

    /** value between 0.0 and 1.0, 1.0 represents the end game, 0.0 the beginning */
    private static float endGameCoefficient;
    private static float midGameCoefficient;

    private static long combinedOutposts;
    private static long[] arrOutposts = new long[2];

    private static final short boardCorners[][] = {{0, 63} , {56, 7}};
    
    private static int scaleFactor;
    private static final int scaleFactorNormal = 64;
    
    private static Bitbase bitbase;
    /**
     * Constructor Evaluation2
     *
     * grabs a reference to the instantiated Board object
     *
     */
    public Evaluation2() {
        chessBoard = Board.getInstance();
        bitbase = chessBoard.GetBitbase();
    }

    /**
     * Method clearPawnHash
     *
     * calls the pawn hash table's method to clear its entries
     *
     */
    public static void clearPawnHash() {
    	PawnTable.clearPawnHash();
    }

    /**
     * Method clearEvalHash
     *
     * calls the eval hash table's method to clear its entries
     *
     */
    public static void clearEvalHash() {
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
    public static void reSizeEvalHash() {
        EvalTable = new TransTable(Global.EvalHASHSIZE,2);
    }

    /**
     * Method reSizePawnHash
     *
     * creates a new pawn hash - usually due to the size of the hash being adjusted
     * before the start of the game by a UCI command
     *
     */
    public static void reSizePawnHash() {
        PawnTable = new TransTable(Global.PawnHASHSIZE,1);
    }

    public static int EvaluateKPK( int side, int toMove, int depth )
    {
        int enemyKingSquare = chessBoard.pieceList[4 + (side^1)*6][0];
        int friendKingSquare = chessBoard.pieceList[4 + side*6][0];
        int pawnSquare = chessBoard.pieceList[5 + side*6][0];
       
        //need to mirror vertically to convert from a black to white position
        if( side == Global.COLOUR_BLACK )
        {
            pawnSquare ^= 56;
            friendKingSquare ^= 56;
            enemyKingSquare ^= 56;
        }
        
        boolean bWin = bitbase.Probe( toMove, enemyKingSquare, friendKingSquare, pawnSquare);
        
        if( bWin )
        {
            return Global.KNOWN_WIN_KPK + (pawnSquare / 8) * 10 - depth;
        }
        else
        {
            return 0;
        }
    }
    
    public static int EvaluateKBNK( int side )
    {
        int bishopSquare = chessBoard.pieceList[2 + side*6][0];
        int enemyKingSquare = chessBoard.pieceList[4 + (side^1)*6][0];
        int friendKingSquare = chessBoard.pieceList[4 + side*6][0];
        int distToDarkSquare = bishopSquare/8 + bishopSquare%8;
        int bishopSquareColour = (distToDarkSquare % 2) == 0 ? Global.DARK_SQUARE : Global.LIGHT_SQUARE;
        
        int iShortestEnemyKingDist = 8;
        for( int i=0; i<2; i++)
        {
            int iDist = chessBoard.getRookDistance( enemyKingSquare, boardCorners[bishopSquareColour][i] );
            iShortestEnemyKingDist = Math.min( iShortestEnemyKingDist, iDist);
        }
        
        return Global.KNOWN_WIN - ( iShortestEnemyKingDist * 100 ) - chessBoard.getQueenDistance(enemyKingSquare, friendKingSquare) * 10;
    }
    
    public static int EvaluateKRKB( int side )
    {
        int enemyKingSquare = chessBoard.pieceList[4 + (side^1)*6][0];
        int score = 10 - kingVals[enemyKingSquare];
        
        return score;
    }
    
    public static int EvaluateKRKN( int side )
    {
        int enemyKingSquare = chessBoard.pieceList[4 + (side^1)*6][0];
        int enemyKnight = chessBoard.pieceList[1 + (side^1)*6][0];
        
        int weakSidePenalty = chessBoard.getRookDistance(enemyKingSquare, enemyKnight) * 15;
        
        int score = 10 - kingVals[enemyKingSquare] + weakSidePenalty;
        
        return score;
    }
    
    public static int EvaluateKRKP( int side, int toMove )
    {
        int sideTempo = side == toMove ? 1 : 0;
        
        int strongKing = chessBoard.pieceList[4 + (side)*6][0];
        int weakKing = chessBoard.pieceList[4 + (side^1)*6][0];
        int strongRook = chessBoard.pieceList[(side)*6][0];
        int weakPawn = chessBoard.pieceList[5 + (side^1)*6][0];
        int weakQueenSquare = (weakPawn % 8);
        
        if( side == Global.COLOUR_BLACK )
        {
            strongKing ^= 56;
            weakKing ^= 56;
            strongRook ^= 56;
            weakPawn ^= 56;
        }
        
        if( strongKing < weakPawn && strongKing%8 == weakPawn%8)
        {
            return Global.values[0] - chessBoard.getQueenDistance(weakPawn, strongKing);
        }
        
        else if( chessBoard.getQueenDistance(weakKing, weakPawn) - (sideTempo ^ 1) >= 3 &&
                chessBoard.getQueenDistance(weakKing, strongRook) >= 3)
        {
            return Global.values[0] - chessBoard.getQueenDistance(weakPawn, strongKing);
        }
        
        else if( weakKing / 8 <= 2 && chessBoard.getQueenDistance(weakKing, weakPawn) == 1 &&
            strongKing / 8 >= 3 && chessBoard.getQueenDistance( strongKing, weakPawn) - tempo > 2)
        {
            return 100 - chessBoard.getQueenDistance( strongKing, weakPawn) * 5;
        }
        
        else
        {
            return 200 - (chessBoard.getQueenDistance(strongKing, weakPawn - 8) * 8) 
            + (chessBoard.getQueenDistance(weakKing, weakPawn - 8) * 8)
            + (chessBoard.getQueenDistance(weakPawn, weakQueenSquare) * 8);
        }
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
    public static int getEval(int side, int alpha, int beta, int depth)
    {
        /** see if there is a stored value in the eval hash table */
        int evalKey = (int)(chessBoard.hashValue % Global.EvalHASHSIZE);
        evalKey = Math.abs(evalKey);

        if(EvalTable.hasEvalHash(evalKey,chessBoard.hashValue))
        {
            int value = EvalTable.getEvalValue(evalKey);
            if((value & TransTable.LAZY_BIT) != 0)
            {
                int lazyMargin = LAZY_MARGIN +  chessBoard.GetLazyPieceTotals() * 3;

                endGameCoefficient =  Math.max(0.0f, Math.min(1.0f,((float)Global.totalValue * 0.85f - (float)chessBoard.totalValue)/ ((float)Global.totalValue * 0.65f)) );
                if( endGameCoefficient <=  0.70 && (chessBoard.GetPieceTotal(3) > 0 || chessBoard.GetPieceTotal(9) > 0)) {
                    lazyMargin += 30;
                }
                
                value ^= TransTable.LAZY_BIT;
                value -= Global.MATE_SCORE;
                if( (value + lazyMargin) <= alpha )
                {
                    return value + lazyMargin;
                }
                else if( (value - lazyMargin) >= beta )
                {
                    return value - lazyMargin;
                }
            }
            else
            {
                return value - Global.MATE_SCORE;
            }
        }
        /** get the material score */
        material = chessBoard.GetRawMaterialScore();
        if( material > 100000) {
            return 0;
        }
        
        int sideMult = side == Global.COLOUR_WHITE ? -1 : 1;
        switch( Board.materialKey )
        {
            case( Global.whiteKBNK ):
            {
                finalScore = sideMult * -EvaluateKBNK( Global.COLOUR_WHITE);
                return finalScore;
            }
            
            case( Global.blackKBNK ):
            {
                finalScore =  sideMult * EvaluateKBNK( Global.COLOUR_BLACK);
                return finalScore;
            }
                
            case( Global.whiteKRKB ):
            {
                finalScore =  sideMult * -EvaluateKRKB( Global.COLOUR_WHITE );
                return finalScore;
            }   
            
            case( Global.blackKRKB ):
            {
                finalScore =  sideMult * EvaluateKRKB( Global.COLOUR_BLACK );
                return finalScore;
            }
                
             case( Global.whiteKRKN ):
            {
                finalScore =  sideMult * -EvaluateKRKN( Global.COLOUR_WHITE );
                return finalScore;
            }   
            
            case( Global.blackKRKN ):
            {
                finalScore =  sideMult * EvaluateKRKN( Global.COLOUR_BLACK );
                return finalScore;
            }
            
            case( Global.whiteKRKP ):
            {
                finalScore =  sideMult * -EvaluateKRKP( Global.COLOUR_WHITE, side );
                return finalScore;
            }   
            
            case( Global.blackKRKP ):
            {
                finalScore =  sideMult * EvaluateKRKP( Global.COLOUR_BLACK, side );
                return finalScore;
            }
            
            case( Global.whiteKPK ):
            {
               finalScore = sideMult * -EvaluateKPK( Global.COLOUR_WHITE, side, depth );
               return finalScore;
            }
                
            case( Global.blackKPK ):
            {
                finalScore = sideMult * EvaluateKPK( Global.COLOUR_BLACK, side^1, depth );
                return finalScore;
            }
        }
        
        
        int lazyMargin = LAZY_MARGIN +  chessBoard.GetLazyPieceTotals() * 3;

        /** initialize evaluation terms */
        Arrays.fill(boardAttacks[0], 0);
        Arrays.fill(boardAttacks[1], 0);
        endGameCoefficient =  Math.max(0.0f, Math.min(1.0f,((float)Global.totalValue * 0.85f - (float)chessBoard.totalValue)/ ((float)Global.totalValue * 0.65f)) );
        midGameCoefficient = 1.0f - endGameCoefficient;
        if( endGameCoefficient <=  0.70 && (chessBoard.GetPieceTotal(3) > 0 || chessBoard.GetPieceTotal(9) > 0)) {
                lazyMargin += 30;
        }
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
        passScore[MIDDLE_GAME] = 0;
        passScore[END_GAME] = 0;
        pawnShield = 0;
        kingPos[Global.COLOUR_WHITE] = chessBoard.pieceList[4][0];
        kingPos[Global.COLOUR_BLACK] = chessBoard.pieceList[10][0];
        enemyKingPos[Global.COLOUR_WHITE] = chessBoard.pieceList[10][0];
        enemyKingPos[Global.COLOUR_BLACK] = chessBoard.pieceList[4][0];  
        combinedOutposts = 0L;
        arrOutposts[0] = 0L;
        arrOutposts[1] = 0L;
        
        scaleFactor = scaleFactorNormal;

       
        
        
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
                GetPawnsScore(Global.COLOUR_WHITE);
                pawnScore[MIDDLE_GAME] *= -1;
                pawnScore[END_GAME] *= -1;
                GetPawnsScore(Global.COLOUR_BLACK);

                /** set king attack pattern and calculate the king attack zone */
                SetKingEval();

                pawnShield = -GetKingPawnShield(Global.COLOUR_WHITE);
                pawnShield += GetKingPawnShield(Global.COLOUR_BLACK);

                /** calculate passed pawn bonuses..which EXCLUDE attack square calculation...needed for lazy eval*/
                GetPassedPawnScorePhase1(passBits & chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_PAWN], Global.COLOUR_WHITE);
                GetPassedPawnScorePhase1(passBits & chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_PAWN], Global.COLOUR_BLACK);
                GenerateOutpostSquares();
                /** special case trap penalties for bishop and rook */

                PawnTable.addPawnHash(key, chessBoard.getPawnHash(), pawnScore[MIDDLE_GAME], pawnScore[END_GAME], centre,  passScore[MIDDLE_GAME], passScore[END_GAME], pawnShield, passBits
                          , boardAttacks[Global.COLOUR_WHITE][PAWN_BOARD], boardAttacks[Global.COLOUR_BLACK][PAWN_BOARD], combinedOutposts);
            }
        }

        setTrapPenalties();

        /** tempo */
        
        tempo = TEMPO_BONUS * sideMult;

        if( endGameCoefficient <  1.00)
        {
            int midLazyScores = pawnScore[MIDDLE_GAME] + passScore[MIDDLE_GAME] + develop + castle + pawnShield;
            int endLazyScores = pawnScore[END_GAME] + passScore[END_GAME] + endKingSafety ;
            int allLazyScores = tempo + material + trapped;

            int finalLazyScore = sideMult * (int)((float)midLazyScores * midGameCoefficient + (float)endLazyScores * (float)endGameCoefficient + (float)allLazyScores);

            if( (finalLazyScore + lazyMargin) <= alpha )
            {
                EvalTable.AddEvalHashLazy(evalKey, chessBoard.hashValue, finalLazyScore + Global.MATE_SCORE);
                return finalLazyScore + lazyMargin;
            }
            else if( finalLazyScore - lazyMargin >= beta )
            {
                EvalTable.AddEvalHashLazy(evalKey, chessBoard.hashValue, finalLazyScore + Global.MATE_SCORE);
                return  finalLazyScore - lazyMargin;
            }
        }

        if( hasHash )
        {
           SetKingEval();
        }

        /** major and minor piece evaluations */
        long mobilityAreaWhite = ~(boardAttacks[Global.COLOUR_BLACK][PAWN_BOARD] | chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_ALL]);
        long mobilityAreaBlack = ~(boardAttacks[Global.COLOUR_WHITE][PAWN_BOARD] | chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_ALL]);
		  
        
        /*knightEval = -GetEvalPieceType(knightType, Global.COLOUR_WHITE, mobilityAreaWhite);
        bishopEval = -GetEvalPieceType(bishopType, Global.COLOUR_WHITE, mobilityAreaWhite);
        queenEval = -GetEvalPieceType(queenType, Global.COLOUR_WHITE, mobilityAreaWhite);
        int rookScore = -GetEvalPieceType(rookType, Global.COLOUR_WHITE, mobilityAreaWhite);
        
        rookEval[MIDDLE_GAME] *= -1;
        rookEval[END_GAME] *= -1;
        mobility[MIDDLE_GAME] *= -1;
        mobility[END_GAME] *= -1;
        centre *= -1;
        develop *= -1;
        knightEval += GetEvalPieceType(knightType, Global.COLOUR_BLACK, mobilityAreaBlack);
        bishopEval += GetEvalPieceType(bishopType, Global.COLOUR_BLACK, mobilityAreaBlack);
        queenEval += GetEvalPieceType(queenType, Global.COLOUR_BLACK, mobilityAreaBlack);
        rookScore += GetEvalPieceType(rookType, Global.COLOUR_BLACK, mobilityAreaBlack);
        */
        knightEval = -GetKnightEval(Global.COLOUR_WHITE, mobilityAreaWhite);
        queenEval = -GetQueenEval(Global.COLOUR_WHITE, mobilityAreaWhite);
        bishopEval = -GetBishopEval(Global.COLOUR_WHITE, mobilityAreaWhite);
        GetRookEval(Global.COLOUR_WHITE, mobilityAreaWhite);
        
        rookEval[MIDDLE_GAME] *= -1;
        rookEval[END_GAME] *= -1;
        mobility[MIDDLE_GAME] *= -1;
        mobility[END_GAME] *= -1;
        centre *= -1;
        develop *= -1;
        
        knightEval += GetKnightEval(Global.COLOUR_BLACK, mobilityAreaBlack);
        queenEval += GetQueenEval(Global.COLOUR_BLACK, mobilityAreaBlack);
        bishopEval += GetBishopEval(Global.COLOUR_BLACK, mobilityAreaBlack);
        GetRookEval(Global.COLOUR_BLACK, mobilityAreaBlack);
        
        endKingSafety = GetEndGameKing();

        if( endGameCoefficient <=  0.70)
        {
            if(chessBoard.GetPieceTotal(9) > 0) {
                kingSafety = GetKingSafety(Global.COLOUR_WHITE);
            }
            if(chessBoard.GetPieceTotal(3) > 0) {
                kingSafety -= GetKingSafety(Global.COLOUR_BLACK);
            }
        }

        /** calculate passed pawn bonuses..which INCLUDE attack square calculation...needed for lazy eval*/
        GetPassedPawnScorePhase2(passBits & chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_PAWN], Global.COLOUR_WHITE);
        GetPassedPawnScorePhase2(passBits & chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_PAWN], Global.COLOUR_BLACK);

        /** hung scores */
        hungPenalty = sideMult * HungPieces(side, chessBoard.pieceBits[side][Global.PIECE_ALL] ^ chessBoard.pieceBits[side][Global.PIECE_KING]);

        if(centre < 0) {
            centre = CenterScoreArray[-centre] * -1;
        } else {
            centre = CenterScoreArray[centre];
        }
        int midScores = pawnScore[MIDDLE_GAME] + passScore[MIDDLE_GAME] + centre + mobility[MIDDLE_GAME] + develop + castle + kingSafety + pawnShield + rookEval[MIDDLE_GAME];
        int endScores = pawnScore[END_GAME] + passScore[END_GAME] + endKingSafety + rookEval[END_GAME] + mobility[END_GAME];
        int allScores = bishopEval + knightEval + + queenEval + tempo + hungPenalty + material + trapped;

        finalScore = sideMult * (int)((float)midScores * midGameCoefficient + (float)endScores * (float)endGameCoefficient + (float)allScores);

        /** store the score in the eval hashtable */
        EvalTable.AddEvalHash(evalKey, chessBoard.hashValue, finalScore + Global.MATE_SCORE);

        return finalScore;
    }

    /**
     * Method printEvalTerms()
     *
     * used to debug the evaluation routine by examining each term
     *
     */
    public static void printEvalTerms() {
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
        System.out.println("black king zone is "+ kingZone[Global.COLOUR_BLACK]);
        System.out.println("white king zone is "+ kingZone[Global.COLOUR_WHITE]);
        System.out.println("total value is "+chessBoard.totalValue);
    }

    /**
     * Method setTrapPenalties
     *
     * method recognizes several trapped piece patterns for bishops and rooks
     *
     */
    private static void setTrapPenalties() {
        if((chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_BISHOP] & Global.set_Mask[48]) != 0) {
            if (((chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_PAWN] & Global.set_Mask[41]) != 0) && ((chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_PAWN] & Global.set_Mask[50]) != 0))
                trapped += 150;
        }
        if((chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_BISHOP] & Global.set_Mask[55]) != 0) {
            if (((chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_PAWN] & Global.set_Mask[53]) != 0) && (chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_PAWN] & Global.set_Mask[46]) != 0)
                trapped += 150;
        }
        if((chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_BISHOP] & Global.set_Mask[15]) != 0) {
            if(((chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_PAWN] & Global.set_Mask[13])!= 0) && ((chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_PAWN] & Global.set_Mask[22])!= 0))
                trapped -= 150;
        }
        if((chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_BISHOP] & Global.set_Mask[8]) != 0) {
            if((chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_PAWN] & Global.set_Mask[10]) != 0 && ((chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_PAWN] & Global.set_Mask[17])!= 0))
                trapped -= 150;
        }
        if(chessBoard.castleFlag[Global.COLOUR_WHITE] == Global.NO_CASTLE) {
            if((chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_ROOK] & Global.wRookTrap[0]) != 0) {
                if(kingPos[Global.COLOUR_WHITE] < 4 && kingPos[Global.COLOUR_WHITE] > 0)
                    trapped += 50;
            }
            if((chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_ROOK] & Global.wRookTrap[1]) != 0) {
                if(kingPos[Global.COLOUR_WHITE] > 4 && kingPos[Global.COLOUR_WHITE] < 7)
                    trapped += 50;
            }
        }
        if(chessBoard.castleFlag[Global.COLOUR_BLACK] == Global.NO_CASTLE) {
            if((chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_ROOK] & Global.bRookTrap[0]) != 0) {
                if(kingPos[Global.COLOUR_BLACK] < 60 && kingPos[Global.COLOUR_BLACK] > 56)
                    trapped -= 50;
                }
            if((chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_ROOK] & Global.bRookTrap[1]) != 0)  {
                if(kingPos[Global.COLOUR_BLACK] > 60 && kingPos[Global.COLOUR_BLACK] < 63)
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
    private static void setDevelopmentBonus() {
        switch(chessBoard.castleFlag[Global.COLOUR_WHITE]) {
            case(Global.CASTLED):
                    castle -= 40;
            break;
            case(Global.SHORT_CASTLE):
                    if((chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_QUEEN] & Global.set_Mask[3])!= 0)
                        develop-=20;
                    castle -= 10;
            break;
            case(Global.LONG_CASTLE):
                    if((chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_QUEEN] & Global.set_Mask[3])!= 0)
                        develop-=20;
                    castle -= 10;
            break;
            case(Global.BOTH_CASTLE):
                    if((chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_QUEEN] & Global.set_Mask[3])!= 0)
                        develop-=20;
                    castle -= 20;
            break;
        }   

        switch(chessBoard.castleFlag[Global.COLOUR_BLACK]) {
            case(Global.CASTLED):
                castle += 40;
            break;
            case(Global.SHORT_CASTLE):
                if((chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_QUEEN] & Global.set_Mask[59]) != 0)
                    develop+=20;
                castle += 10;
            break;
            case(Global.LONG_CASTLE):
                if((chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_QUEEN] & Global.set_Mask[59]) != 0)
                    develop+=20;
                castle += 10;
            break;
            case(Global.BOTH_CASTLE):
                if((chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_QUEEN] & Global.set_Mask[59]) != 0)
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

    private static void SetPawnAttack(int side)
    {
        int leftOffset = side == Global.COLOUR_WHITE ? 7 : -9;
        int rightOffset = side == Global.COLOUR_WHITE ? 9 : -7;
        int piece = side == Global.COLOUR_WHITE ? 5 : 11;

        for(int i=0; i < chessBoard.pieceTotals[piece]; i++)
        {
            int position = chessBoard.pieceList[piece][i];
            if(position % 8 > 0) {
                boardAttacks[side][PAWN_BOARD] |= 1L << (position + leftOffset);
                boardAttacks[side][ALL_BOARD] |= 1L << (position + leftOffset);   
            }
            if(position % 8 < 7) {
                boardAttacks[side][PAWN_BOARD] |= 1L << (position + rightOffset);
                boardAttacks[side][ALL_BOARD] |= 1L << (position + rightOffset);
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
    private static void GetPawnsScore(int side) {
        boolean isolated, backward, chain, opposed, passed;
        long temp = chessBoard.pieceBits[side][Global.PIECE_PAWN];
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
            int relativeRank = Global.RelativeRanks[side][rank];//Board.GetRelativeRank(side, position);
            int relativePosition = (relativeRank << 3) + file;// Board.GetRelativePosition(side, position);
            int rankBehind = rank + behindValue;
            int rankForward = rank + forwardValue;

            //positional score
            pawnScore[MIDDLE_GAME] += PawnVals[MIDDLE_GAME][relativePosition];
            pawnScore[END_GAME] += PawnVals[END_GAME][relativePosition];

            if((Global.passed_masks[side][position] & chessBoard.pieceBits[(side+1)&1][Global.PIECE_PAWN]) == 0)
            {
                    passBits |= piece;
                    passed = true;
            }
            else
            {
                    passed = false;
            }

            //doubled
            long friendPawnsInFront =  Global.mask_in_front[side][position] & chessBoard.pieceBits[side][Global.PIECE_PAWN];
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
            if((chessBoard.pieceBits[side][Global.PIECE_PAWN] & Global.neighbour_files[file] & (Global.rankMasks[rankBehind] | Global.rankMasks[rank] )) != 0)
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
            if((chessBoard.pieceBits[(side+1)&1][Global.PIECE_PAWN] & Global.mask_in_front[side][position]) != 0)
            {
                opposed = true;
            }
            else
            {
                opposed = false;
            }

            //isolated
            if((Global.neighbour_files[file] & chessBoard.pieceBits[side][Global.PIECE_PAWN]) == 0)
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
                    && ((boardAttacks[(side+1)%2][PAWN_BOARD] & (1L << position)) == 0)
                    && ((chessBoard.pieceBits[side][Global.PIECE_PAWN] & Global.neighbour_files[file] & Global.mask_forward[(side+1) & 1][position]) == 0))
            {
                long forwardAttacks = Global.rankMasks[rankForward] & Global.neighbour_files[file];// & Global.mask_forward[side][position];
                while ((forwardAttacks & (chessBoard.pieceBits[(side+1)&1][Global.PIECE_PAWN] | chessBoard.pieceBits[side][Global.PIECE_PAWN])) == 0)
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
                if(((forwardAttacks | nextRankForward) & chessBoard.pieceBits[(side+1)&1][Global.PIECE_PAWN] ) != 0)
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

                long helperPawns = Global.neighbour_files[file] & Global.mask_forward[(side+1) & 1][posForward] & chessBoard.pieceBits[side][Global.PIECE_PAWN];
                if(helperPawns != 0)
                {
                    long opponentPawns = Global.neighbour_files[file] & Global.mask_forward[side][position] & chessBoard.pieceBits[(side+1)&1][Global.PIECE_PAWN];
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
    private static void GetPassedPawnScorePhase2(long passers, int side)
    {
        while(passers != 0)
        {
            long piece = passers & -passers;
            passers ^= piece;
            int position = Long.numberOfTrailingZeros(piece);
            int blockPos = position + Global.forwardRank[side];
            int rank = position>>3;
            int relativeRank = Global.RelativeRanks[side][rank];
            int endScore = PassedPawnBonus[END_GAME][relativeRank];
            int endBonus = 0;
            int kingEndBonus = 0;

            //apply score based on path to queen
            long forwardPath = Global.mask_in_front[side][position];
            long unsafeSquares = forwardPath & (boardAttacks[(side+1) & 1][ALL_BOARD] | chessBoard.pieceBits[side ^ 1][Global.PIECE_ALL]);
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
                if( (forwardPath & chessBoard.pieceBits[side][Global.PIECE_ALL]) == 0)
                {
                    endBonus += endScore / 7;
                }
            }

            //doubled pawn - reduce bonus
            if((Global.mask_in_front[side][position] & chessBoard.pieceBits[side][Global.PIECE_PAWN]) != 0) {
                endBonus /= 3;
            }

            kingEndBonus = -chessBoard.getQueenDistance(kingPos[side], blockPos) * endBonus / 20;
            kingEndBonus += chessBoard.getQueenDistance(enemyKingPos[side], blockPos) * endBonus / 20;

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
    private static void GetPassedPawnScorePhase1(long passers, int side)
    {
        while(passers != 0)
        {
            long piece = passers & -passers;
            passers ^= piece;
            int position = Long.numberOfTrailingZeros(piece);
            int blockPos = position + Global.forwardRank[side];
            int rank = position>>3;
            int file = position&7;
            int relativeRank = Global.RelativeRanks[side][rank];//Board.GetRelativeRank(side, position);
            int middleScore = PassedPawnBonus[MIDDLE_GAME][relativeRank];
            int endScore = PassedPawnBonus[END_GAME][relativeRank];
            int kingEndBonus = 0;

            //bonus for supporting pawns
            long neighbourPawns = chessBoard.pieceBits[side][Global.PIECE_PAWN] & Global.neighbour_files[file] & (Global.rankMasks[rank] 
                    | Global.rankMasks[Board.GetPreviousRank(side, rank)]);
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
            if((Global.mask_in_front[side][position] & chessBoard.pieceBits[side][Global.PIECE_PAWN]) != 0) {
                endScore /= 3;
            }
            
            kingEndBonus = -chessBoard.getQueenDistance(kingPos[side ], blockPos) * endScore / 20;
            kingEndBonus += chessBoard.getQueenDistance(kingPos[side ^ 1], blockPos) * endScore / 20;

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
    public static boolean isPassedPawn(int side, int position) {
       return ((Global.passed_masks[side][position] & chessBoard.pieceBits[side^1][Global.PIECE_PAWN]) == 0);
    }

    /**
     * Method getWKnightEval
     *
     * calculates positional score for white knight
     * adds knight attack to WB array
     *
     * @return int - knight positional score
     */
     
    private static int GetKnightEval(int side, long mobilityArea)
    {
        int score = 0;
        for(int i=0; i < chessBoard.pieceTotals[1 + side*6]; i++)
        {
            int position = chessBoard.pieceList[1 + side*6][i];
            int relativeRank = Global.RelativeRanks[side][position>>3] ;
            int relativePosition = (relativeRank << 3) + (position & 7);
            if(relativeRank == 0) {
                develop -= BACKRANK_MINOR;
            }
            long attacks = chessBoard.getKnightMoves(position);
            boardAttacks[side][KNIGHT_BOARD] |= attacks;
            boardAttacks[side][ALL_BOARD] |= attacks;
            long mobilitySquares = mobilityArea & attacks;
            int mobilityNumber = Long.bitCount(mobilitySquares);
            mobility[MIDDLE_GAME] += KNIGHT_MOBILITY[MIDDLE_GAME][mobilityNumber];
            mobility[END_GAME] += KNIGHT_MOBILITY[END_GAME][mobilityNumber];
            centre += Long.bitCount(CENTER_BITS & attacks);
            score -= chessBoard.getQueenDistance(position, enemyKingPos[side]);
            score += KnightVals[ relativePosition ];
            score += GetOutpostScore(position, relativePosition, side, KNIGHT_OUTPOST);
        }
        return score;
    }

    public static void GenerateOutpostSquares()
    {
        for(int i=16; i<=47; i++)
        {
            if( (Global.mask_in_front[Global.COLOUR_WHITE][i] & Global.neighbour_files[i & 7] & chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_PAWN]) == 0) {
                arrOutposts[Global.COLOUR_WHITE] |= 1L << (i);
            }    

            if( (Global.mask_in_front[Global.COLOUR_BLACK][i] & Global.neighbour_files[i & 7] & chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_PAWN]) == 0) {
                arrOutposts[Global.COLOUR_BLACK] |= 1L << (i);
            }    
        }
        combinedOutposts = (arrOutposts[Global.COLOUR_BLACK] >> 16) | (arrOutposts[Global.COLOUR_WHITE] << 16);
    }

    public static int GetOutpostScore(int position, int relativePosition, int side, int outpostType)
    {
        if( (arrOutposts[side] & 1L << (position)) != 0)
        {
            int bonus = OutpostBonus[outpostType][relativePosition];
            long friendPawns = chessBoard.pieceBits[side][Global.PIECE_PAWN];
            if((Global.mask_in_front[side][position] & Global.neighbour_files[position & 7] & friendPawns) != 0  )
            {
                bonus += bonus/2;
            }
            return bonus;
        }
        else {
            return 0;
        }
     }

    /**
     * Method getWBishopEval
     *
     * calculates positional score for white bishop
     * adds bishop attack to WB array
     *
     * @return int - bishop positional score
     */
     
    private static int GetBishopEval(int side, long mobilityArea)
    {
        int score = 0;
        for(int i=0; i < chessBoard.pieceTotals[2 + side*6]; i++)
        {
            int position = chessBoard.pieceList[2 + side*6][i];
            int relativeRank = Global.RelativeRanks[side][position>>3] ;
            int relativePosition = (relativeRank << 3) + (position & 7);
            if(Global.RelativeRanks[side][position>>3] == 0) {
                develop -= BACKRANK_MINOR;
            }
            long attacks = chessBoard.getMagicBishopMoves(position);
            boardAttacks[side][BISHOP_BOARD] |= attacks;
            boardAttacks[side][ALL_BOARD] |= attacks;
            long mobilitySquares = mobilityArea & attacks;
            int mobilityNumber = Long.bitCount(mobilitySquares);
            mobility[MIDDLE_GAME] += BISHOP_MOBILITY[MIDDLE_GAME][mobilityNumber];
            mobility[END_GAME] += BISHOP_MOBILITY[END_GAME][mobilityNumber];
            centre += Long.bitCount(CENTER_BITS & attacks);
            score -= chessBoard.getQueenDistance(position, enemyKingPos[side]);
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
     */
     
    private static int GetQueenEval(int side, long mobilityArea)
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
            mobility[MIDDLE_GAME] += QUEEN_MOBILITY[MIDDLE_GAME][mobilityNumber];
            mobility[END_GAME] += QUEEN_MOBILITY[END_GAME][mobilityNumber];
            centre += Long.bitCount(CENTER_BITS & attacks);
            score -= 3 * chessBoard.getQueenDistance(position, enemyKingPos[side]);
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
    */
   
    private static void GetRookEval(int side, long mobilityArea)
    {
        int enemyKingRelativeRank = Global.RelativeRanks[side][enemyKingPos[side]>>3];//Board.GetRelativeRank(side, enemyKingPos);
        int oldFile = -1;
        int oldRank = -1;

        for(int i=0; i<chessBoard.pieceTotals[side*6]; i++)
        {
            int position = chessBoard.pieceList[side*6][i];
            int file = position & 7;
            int rank = position >> 3;
            int relativeRank = Global.RelativeRanks[side][rank];
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
                if((Global.rankMasks[rank] & chessBoard.pieceBits[side][Global.PIECE_QUEEN]) != 0)
                {
                    rookEval[MIDDLE_GAME] += QUEEN_ROOK_7TH_RANK;
                    rookEval[END_GAME] += QUEEN_ROOK_7TH_RANK;
                }
            }
            if((chessBoard.pieceBits[side][Global.PIECE_PAWN] & Global.fileMasks[file]) == 0 )
            {
                if(Math.abs(file - (enemyKingPos[side] & 7)) < 2)
                {
                    nearEnemyKing = 2;
                }
                if((Global.mask_behind[side][position] & chessBoard.pieceBits[side][Global.PIECE_QUEEN]) != 0)
                {
                    rookEval[MIDDLE_GAME] += ROOK_IN_FRONT_QUEEN;
                }
                else if((Global.mask_in_front[side][position] & chessBoard.pieceBits[side][Global.PIECE_QUEEN]) != 0)
                {
                    rookEval[MIDDLE_GAME] += ROOK_BEHIND_QUEEN;
                }
                if(file == oldFile)
                {
                    rookEval[MIDDLE_GAME] += ROOK_DOUBLED * nearEnemyKing;
                    rookEval[END_GAME] += ROOK_DOUBLED * nearEnemyKing;
                }
                if((chessBoard.pieceBits[(side+1)&1][Global.PIECE_PAWN] & Global.fileMasks[file]) == 0 )          // open file
                {
                    rookEval[END_GAME] += ROOK_OPEN * nearEnemyKing;
                    rookEval[MIDDLE_GAME] += ROOK_OPEN * nearEnemyKing;
                }
                else
                {
                    rookEval[END_GAME] += ROOK_SEMI * nearEnemyKing;
                    rookEval[MIDDLE_GAME] += ROOK_SEMI * nearEnemyKing;
                }
            }

            long attacks = chessBoard.getMagicRookMoves(position);
            boardAttacks[side][ROOK_BOARD] |= attacks;
            boardAttacks[side][ALL_BOARD] |= attacks;
            long mobilitySquares = mobilityArea & attacks;
            int mobilityNumber = Long.bitCount(mobilitySquares);
            mobility[MIDDLE_GAME] += ROOK_MOBILITY[MIDDLE_GAME][mobilityNumber];
            mobility[END_GAME] += ROOK_MOBILITY[END_GAME][mobilityNumber];
            centre += Long.bitCount(CENTER_BITS & attacks);
            rookEval[MIDDLE_GAME] -= chessBoard.getQueenDistance(position, enemyKingPos[side]);
            rookEval[END_GAME] -= chessBoard.getQueenDistance(position, enemyKingPos[side]);
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
    private static void SetKingEval() 
    {
        int side = Global.COLOUR_WHITE;
        int kingFile = kingPos[side] & 7;
        int kingRank = kingPos[side] >> 3;
        int fileShift = kingFileShifts[kingFile];
        int rankShift = Math.min(5, kingRank);
        kingZone[side] = kingZoneMask << ((rankShift << 3) + fileShift);
        long attacks = chessBoard.getKingMoves(kingPos[side]);
        boardAttacks[side][KING_BOARD] |= attacks;
        boardAttacks[side][ALL_BOARD] |= attacks;
        side = Global.COLOUR_BLACK;
        kingFile = kingPos[side] & 7;
        kingRank = kingPos[side] >> 3;
        fileShift = kingFileShifts[kingFile];
        rankShift = Math.max(0, kingRank-2);
        kingZone[side] = kingZoneMask << ((rankShift << 3) + fileShift);
        attacks = chessBoard.getKingMoves(kingPos[side]);
        boardAttacks[side][KING_BOARD] |= attacks;
        boardAttacks[side][ALL_BOARD] |= attacks;      
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
        score -= kingVals[kingPos[Global.COLOUR_WHITE]];
        score += kingVals[kingPos[Global.COLOUR_BLACK]];
        return score;
    }

    private static int GetKingPawnShield(int side)
    {
        int score = 0;
        //mark king's critical files as semi open if no friend pawn is on the file
        int kingFile = kingPos[side] & 7;
        int fileShift = kingFileShifts[kingFile];

        for(int i=0; i<3; i++)
        {
            long pawnFile = chessBoard.pieceBits[side][Global.PIECE_PAWN] & Global.fileMasks[fileShift + i];
            if( (pawnFile) != 0)
            {
                long zonePawns = pawnFile & kingZone[side];
                if(zonePawns != 0)
                {
                    int position = side == Global.COLOUR_WHITE ? Long.numberOfTrailingZeros(zonePawns) : (63 - Long.numberOfLeadingZeros(zonePawns));
                    score += PawnProtection[Global.RelativeRanks[side][position>>3]];
                }
            }
            else
            {
                score -= KING_PAWN_SEMI_OPEN;
                if( (chessBoard.pieceBits[(side+1) & 1][Global.PIECE_PAWN] & Global.fileMasks[fileShift + i]) == 0 )
                {
                    score -= KING_PAWN_OPEN;
                }
            }
        }
        return score;
    }

    private static int GetKingSafety(int side)
    {
        //get all king squares attacked
        long attacks = boardAttacks[(side+1) & 1][ALL_BOARD] & kingZone[side];

        long undefendedAttacks = attacks & ~(boardAttacks[side][ALL_BOARD] ^ boardAttacks[side][KING_BOARD]);

        int count = Long.bitCount(attacks) + Long.bitCount(undefendedAttacks);

        if(count > 0)
        {
            int mask = 0;

            if((boardAttacks[(side+1) & 1][PAWN_BOARD] & kingZone[side]) != 0)
                mask |= 1;

            if( ((boardAttacks[(side+1) & 1][KNIGHT_BOARD] | boardAttacks[( side + 1) %2][BISHOP_BOARD]) & kingZone[side]) != 0)
                mask |= 2;

            if((boardAttacks[(side+1) & 1][ROOK_BOARD] & kingZone[side]) != 0)
                mask |= 4;

            if((boardAttacks[(side+1) & 1][QUEEN_BOARD] & kingZone[side]) != 0)
                mask |= 8;

            count += TABLE[mask];
        }

        return kingSafetyEval[count + KingAttackVal[ (Global.RelativeRanks[side][kingPos[side]>>3] << 3 ) + ( kingPos[side] & 7 ) ]];
    }

    private static int HungPieces(int side, long piecesNoKing)
    {
        int enemySide = side ^ 1;
        piecesNoKing  &= boardAttacks[enemySide][ALL_BOARD];

        int hung=0; 
        while(piecesNoKing != 0)
        {
            long bit = piecesNoKing & -piecesNoKing;
            piecesNoKing ^= bit;

            if( (boardAttacks[side][ALL_BOARD] & bit) == 0 )
            {
                hung++;
            }
            else
            {
                int position = Long.numberOfTrailingZeros( bit );
                switch(chessBoard.piece_in_square[position] % 6)
                {
                    case 0:
                        if(( (boardAttacks[enemySide][PAWN_BOARD] | boardAttacks[enemySide][BISHOP_BOARD] | boardAttacks[enemySide][KNIGHT_BOARD] | boardAttacks[enemySide][KING_BOARD]) & bit) != 0 )
                        {
                            hung++;
                        }
                    break;

                    case 1:
                        if(( (boardAttacks[enemySide][PAWN_BOARD] | boardAttacks[enemySide][BISHOP_BOARD] | boardAttacks[enemySide][KING_BOARD]) & bit) != 0 )
                        {
                            hung++;
                        }
                    case 2:
                        if(( (boardAttacks[enemySide][PAWN_BOARD] | boardAttacks[enemySide][KNIGHT_BOARD] | boardAttacks[enemySide][KING_BOARD]) & bit) != 0 )
                        {
                            hung++;
                        }
                    break;

                    case 3:
                        if(( (boardAttacks[enemySide][PAWN_BOARD] | boardAttacks[enemySide][BISHOP_BOARD] | boardAttacks[enemySide][KNIGHT_BOARD] | boardAttacks[enemySide][ROOK_BOARD]) & bit) != 0 )
                        {
                            hung++;
                        }
                    break;
                }
            }
        }

        if(hung >= 2)
        {
            if(hung == 2) {
                return -HUNG_PENALTY;
            } else {
                return -HUNG_PENALTY * 2;
            }
        } else { 
            return 0;
        }
    }
}

