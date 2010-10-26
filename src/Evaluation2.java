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

    /** white passed pawn bonus based on rank */
    private static final int[] wPassedPawnBonus = {0,7,13,17,25,38,80,0};

    /** white passed pawn bonus based on rank for endgame*/
    private static final int[] wPassedPawnBonusEndGame = {0,15,23,35,47,90,200,0};

    /** black passed pawn bonus based on rank */
    private static final int[] bPassedPawnBonus = {0,80,38,25,17,13,7,0};

    /** black passed pawn bonus based on rank for endgame*/
    private static final int[] bPassedPawnBonusEndGame = {0,200,90,47,35,23,15,0};

    /** specific passed pawn bonuses */
    private static final int CONNECTED_PASSED_BONUS = 20;
    private static final int PROTECTED_PASSED_BONUS = 8;

    /** pawn penalties for weakness */
    private static final int ISOLATED_PAWN = 10;
    private static final int WEAK_PAWN = 10;
    private static final int WEAK_OPEN_PAWN = 4;
    private static final int DOUBLED_PAWN = 20;
    private static final int TRIPLE_PAWN = 50;

    /** mobility bonus multipliers */
    private static final int MOBILITY_BONUS = 1;
    private static final int BISHOP_MOBILITY = 4;
    private static final int ROOK_MOBILITY = 1;
    private static final int KNIGHT_MOBILITY = 4;
    private static final int QUEEN_MOBILITY = 1;
    private static final int PAWN_MOBILITY = 2;

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
    private static final int HUNG_PENALTY = 30;

    /** king safety stuff here
     *  ideas from Ed Schroeder
     */

    /** king safety penalty - formula based on attack pattern and protection patterns */
    private static final int[] kingSafetyEval = {   0,  2,  3,  6, 12, 18, 25, 37, 50, 75,
                                                100,125,150,175,200,225,250,275,300,325,
                                                350,375,400,425,450,475,500,525,550,575,
                                                600,600,600,600,600};


    /** table indexed by attack pattern on king, returns a value used in formula to calculate index to
     * above kingSafetyEval table
     */
    private static final byte TABLE [] = {
  //      . P N N R R R R Q Q Q Q Q Q Q Q K K K K K K K K K K K K K K K K
  //            P   P N N   P N N R R R R   P N N R R R R Q Q Q Q Q Q Q Q
  //                    P       P   N N N       P   P N N   P N N R R R R
          0,0,0,0,0,0,1,1,0,1,2,2,2,3,3,3,0,0,0,0,1,1,2,2,2,3,3,3,3,3,3,3 };

   /** values used to calculate indes into above kingSafetyEval table
    * these are based on the king position
    * notice if the king is flushed into the middle, these values go way up
    */
    private static final int wKingAttackVal[] = new int[] {2,0,0,0,0,0,0,2,
                                                          2,1,1,1,1,1,1,2,
                                                          4,3,3,3,3,3,3,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4};

    private static final int bKingAttackVal[] = new int[] {4,4,4,4,4,4,4,4,
                                                           4,4,4,4,4,4,4,4,
                                                           4,4,4,4,4,4,4,4,
                                                           4,4,4,4,4,4,4,4,
                                                           4,4,4,4,4,4,4,4,
                                                           4,3,3,3,3,3,3,4,
                                                           2,1,1,1,1,1,1,2,
                                                           2,0,0,0,0,0,0,2,};

    /** king safety penalty */
    private static final int KING_PAWN_OPEN_FILE = 5;
    private static final int ENEMY_KING_PAWN_OPEN_FILE = 20;
    private static final int ENEMY_KING_PAWN_SEMI_OPEN = 8;

    /** bonus for pawns sheltering the king */
    private static final int[] whitePawnProtection = {0,15,8,2,1,0,0,0};
    private static final int[] blackPawnProtection = {0,0,0,1,2,8,15,0};

    /** squares of interest around white king */
    private static long whiteKingZone;
    /** squares of interest around black king */
    private static long blackKingZone;

    /** knight piece square tables */
    private static final int wKnightVals[] = new int[]	{-2,-2,-2,-2,-2,-2,-2,-2,
                                                        -2,1,1,1,1,1,1,-2,
                                                        -2,1,4,4,4,4,1,-2,
                                                        -2,1,5,6,6,5,1,-2,
                                                        -2,1,6,8,8,6,1,-2,
                                                        -2,1,6,8,8,6,1,-2,
                                                        -2,1,1,1,1,1,1,-2,
                                                        -2,-2,-2,-2,-2,-2,-2,-2};

    private static final int bKnightVals[] = new int[]	{-2,-2,-2,-2,-2,-2,-2,-2,
                                                        -2,1,1,1,1,1,1,-2,
                                                        -2,1,6,8,8,6,1,-2,
                                                        -2,1,6,8,8,6,1,-2,
                                                        -2,1,5,6,6,5,1,-2,
                                                        -2,1,4,4,4,4,1,-2,
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
    private static final int wPawnVals[] = new int[]    {0, 0, 0, 0, 0, 0, 0, 0,
                                                         0, 0, 0, -10, -10, 0, 0, 0,
                                                         1, 1, 1, 10, 10, 1, 1, 1,
                                                         3, 3, 3, 13, 13, 3, 3, 3,
                                                         6, 6, 6, 13, 13, 6, 6, 6,
                                                         10, 10, 10, 20, 20, 10, 10, 10,
                                                         10, 10, 10, 20, 20, 10, 10, 10,
                                                         0, 0, 0, 0, 0, 0, 0, 0};

    private static final int bPawnVals[] = new int[]     {0, 0, 0, 0, 0, 0, 0, 0,
                                                         10, 10, 10, 20, 20, 10, 10, 10,
                                                         10, 10, 10, 20, 20, 10, 10, 10,
                                                         6, 6, 6, 13, 13, 6, 6, 6,
                                                         3, 3, 3, 13, 13, 3, 3, 3,
                                                         1, 1, 1, 10, 10, 1, 1, 1,
                                                         0, 0, 0, -10, -10, 0, 0, 0,
                                                         0, 0, 0, 0, 0, 0, 0, 0};

    /** penalties for attacking enemy king on a passed pawn in the endgame */
    private static final int wPawnEnemyKingTropism[] = new int[] {0,0,0,0,0,0,0,0,
                                                                  1,1,1,1,1,1,1,1,
                                                                  3,3,3,3,3,3,3,3,
                                                                  5,5,5,5,5,5,5,5,
                                                                  8,8,8,8,8,8,8,8,
                                                                  15,15,15,15,15,15,15,15,
                                                                  30,30,30,30,30,30,30,30,
                                                                  0,0,0,0,0,0,0,0,};

    private static final int bPawnEnemyKingTropism[] = new int[] {0,0,0,0,0,0,0,0,
                                                                  30,30,30,30,30,30,30,30,
                                                                  15,15,15,15,15,15,15,15,
                                                                  8,8,8,8,8,8,8,8,
                                                                  5,5,5,5,5,5,5,5,
                                                                  3,3,3,3,3,3,3,3,
                                                                  1,1,1,1,1,1,1,1,
                                                                  0,0,0,0,0,0,0,0,};

    /** center bonus multipliers for attacks to these squares */
    private static final int centreArr[] = new int[]    {0,0,0,0,0,0,0,0,
                                                        0,0,0,0,0,0,0,0,
                                                        0,0,1,1,1,1,0,0,
                                                        0,0,1,5,5,1,0,0,
                                                        0,0,1,5,5,1,0,0,
                                                        0,0,1,1,1,1,0,0,
                                                        0,0,0,0,0,0,0,0,
                                                        0,0,0,0,0,0,0,0,};

    /** array to be filled with attack patterns for each square */
    private static byte[] WB;
    private static byte[] BB;

    /** positions of specific pieces in attack patterns for WB and BB */
    private static final int PAWN_BIT = 8;
    private static final int MINOR_BIT = 16;
    private static final int ROOK_BIT = 32;
    private static final int QUEEN_BIT = 64;
    private static final int KING_BIT = -128;
    private static final int COUNT_MASK = 7;     //used to mask off number of attacks in WB and BB

    /** stores lowest rank for white pawns across each file */
    private static byte[] wPawnFileVals;

    /** stores highest rank for black pawns across each file */
    private static byte[] bPawnFileVals;


    /* the follwoing are the components of the total evaluation score */


    /** total evaluated score */
    private static int finalScore;

    /** material eval term */
    private static int material;

    /** pawn eval term */
    private static int pawnScore;

    /** passed pawn bonus */
    private static int passScore;

    /** weak pawn attack eval term */
    private static int weakAttackScore;

    /** hung piece eval term */
    private static int hungPenalty;

    /** centre eval term */
    private static int centre;

    /* mobility eval term */
    private static int mobility;

    /** development bonus during opening of game */
    private static int develop;

    /** castline bonus */
    private static int castle;

    /** trapped eval penalty */
    private static int trapped;

    /** king safety eval terms */
    private static int wkingSafety;
    private static int bkingSafety;

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
    private static int rookEval;

    /* end of evaluation components*/


    /** bit mask for white weak pawns */
    private static long wWeakPawns;

    /** bit mask for black weak pawns */
    private static long bWeakPawns;

    /** position of black king */
    private static int bKingPos;

    /** position of white king */
    private static int wKingPos;

    /** counter for white pawns */
    private static int noWhitePawns;

    /** counter for black pawns */
    private static int noBlackPawns;

    /** value between 0.0 and 1.0, 1.0 represents the end game, 0.0 the beginning */
    private static double endGameCoefficient;

     /** instance of singleton MoveHelper Object used to store move info in a compact form */
    private static MoveHelper Helper = MoveHelper.getInstance();

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
    public static final int getEval(int side) {

        /** see if there is a stored value in the eval hash table */
        int evalKey = (int)(chessBoard.hashValue % Global.EvalHASHSIZE);
        evalKey = Math.abs(evalKey);

        if(EvalTable.hasEvalHash(evalKey,chessBoard.hashValue)) {
            return (EvalTable.getEvalValue(evalKey));
        }

         /** get the material score */
        material = chessBoard.GetRawMaterialScore();
        if( material > 100000)
           return 0;

        /** initialize evaluation terms */

        endGameCoefficient =  Math.min(1.0,(8000.0 - (double)chessBoard.totalValue)/ 6600.0);
        //endGameCoefficient *= endGameCoefficient;
        tempo = 0;
        noWhitePawns = 0;
        noBlackPawns = 0;
        weakAttackScore = 0;
        centre = 0;
        mobility = 0;
        develop = 0;
        castle = 0;
        trapped = 0;
        wkingSafety = 0;
        bkingSafety = 0;
        endKingSafety = 0;

        /** set the development and castle scores */
        setDevelopmentBonus();

        WB = new byte[64];
        BB = new byte[64];

        /** set the BB and WB for pawns */
        setPawnAttack();


        /** see if there is a stored value in the pawn hash table */
        long hash = chessBoard.getPawnHash();
        int key = (int)(hash % Global.PawnHASHSIZE);
        key = Math.abs(key);
        boolean hasHash = PawnTable.hasPawnHash(key,chessBoard.getPawnHash());

        int wfileBit;
        int bfileBit;
        long passBits;

        if(hasHash) {               /** extract pawn info from the hash */
            wfileBit = PawnTable.getWPawnFile(key);
            bfileBit = PawnTable.getBPawnFile(key);
            bitToArrayWhitePawnFile(wfileBit);
            bitToArrayBlackPawnFile(bfileBit);
            pawnScore = PawnTable.getPawnValue(key);
            passBits = PawnTable.getPawnPassed(key);
         } else {               /** or calculate the pawn info */
            wfileBit = getWhitePawnFile();
            bfileBit = getBlackPawnFile();
            bitToArrayWhitePawnFile(wfileBit);
            bitToArrayBlackPawnFile(bfileBit);
            pawnScore = getWPawnsScore2();
            pawnScore += getBPawnsScore2();
            passBits = getPassedPawnsBits();

            /** add the pawn info to the pawn hash */
            PawnTable.addPawnHash(key, chessBoard.getPawnHash(), pawnScore, wfileBit, bfileBit, passBits);
        }
        /** set king attack pattern in WB and BB, and calculate the king attack zone */
        setWKingEval();
        setBKingEval();

        /** special case trap penalties for bishop and rook */
        setTrapPenalties();

        /** major and minor piece evaluations */
        knightEval = getWKnightEval();
        knightEval += getBKnightEval();
        queenEval = getWQueenEval();
        queenEval += getBQueenEval();
        bishopEval = getWBishopEval();
        bishopEval += getBBishopEval();
        rookEval = getWRookEval();
        rookEval += getBRookEval();

        /** calculate passed pawn bonuses */
        passScore = GetWhitePassedPawnScore(passBits);
        passScore += GetBlackPassedPawnScore(passBits);

        /** tempo and hung scores */
        if(side == 1) {
            tempo = TEMPO_BONUS;
            hungPenalty = blackHungPieces();
        }else {
           tempo = -TEMPO_BONUS;
           hungPenalty = whiteHungPieces();
       }

        /** if not near end game, then calculate the king safety */
        if(chessBoard.totalValue > 2500) {
            wkingSafety = getKingSafetyWhite();
            bkingSafety = getKingSafetyBlack();
        }else {     /** if in the end game calculate the endgame safety */
            endKingSafety = getEndGameKing();
            //passScore += GetEndPawnRaceScore(side, passBits);
        }

        /** sum up the final evaluation score */
        finalScore = side * (material + pawnScore + (mobility * MOBILITY_BONUS) + (centre * CENTER_BONUS) + develop + castle + trapped + wkingSafety + bkingSafety + endKingSafety +tempo + hungPenalty + passScore + bishopEval + knightEval + rookEval + queenEval);

        /** store the score in the eval hashtable */
        EvalTable.addEvalHash(evalKey, chessBoard.hashValue, finalScore);

        return finalScore;
    }

    /**
     * Method printEvalTerms()
     *
     * used to debug the evaluation routine by examining each term
     *
     */
    public static final void printEvalTerms() {
        System.out.println("score is "+finalScore);
        System.out.println("material is "+material);
        System.out.println("pawn score is "+pawnScore);
        System.out.println("weak attack is "+weakAttackScore);
        System.out.println("mobility is "+mobility * MOBILITY_BONUS);
        System.out.println("center is "+centre * CENTER_BONUS);
        System.out.println("develop is "+develop);
        System.out.println("castle is "+castle);
        System.out.println("trapped is "+trapped);
        System.out.println("whtie king safety is "+wkingSafety);
        System.out.println("black king safety is "+bkingSafety);
        System.out.println("tempo is "+tempo);
        System.out.println("hung is "+hungPenalty);
        System.out.println("pass score is "+passScore);
        System.out.println("bishop eval is "+bishopEval);
        System.out.println("knight eval is "+knightEval);
        System.out.println("rook eval is "+rookEval);
        System.out.println("queen eval is "+queenEval);
        System.out.println("end game coefficient is "+endGameCoefficient);
        System.out.println("total value is "+chessBoard.totalValue);
    }

    /*
     * Returns the max of the file and rank distances
     */
    private static final int GetDistance(int square1, int square2) {
       int rankDist = Math.abs(square1/8 - square2/8);
       int fileDist = Math.abs(square1%8 - square2%8);

       return Math.max(rankDist, fileDist);
    }

    private static final boolean CanKingStopWPawn(int pawn, int king, boolean pMoves) {
      if(pawn/8 == 1)
         pawn += 8;

      int pdist = 7 - pawn/8;
      int kdist = GetDistance(king, pawn % 8 + 56);

      if(!pMoves)
         kdist--;

      return (pdist >= kdist);
    }

    private static final boolean CanKingStopBPawn(int pawn, int king, boolean pMoves) {
      if(pawn/8 == 6)
         pawn -= 8;

      int pdist = pawn/8;
      int kdist = GetDistance(king, pawn % 8);

      if(!pMoves)
         kdist--;

      return (pdist >= kdist);
    }

    private static final int GetEndPawnRaceScore(int side, long passBits)   {
       if(chessBoard.totalValue == Global.values[5] * noWhitePawns ) {           //if we only have white pawns on the board (plus kings of course)
          int kingPos = Long.numberOfTrailingZeros(chessBoard.whiteking);
          int enemyKingPos = Long.numberOfTrailingZeros(chessBoard.blackking);

          long pawns = chessBoard.whitepawns;
          while(pawns != 0) {
            long piece = pawns & -pawns;
            pawns ^= piece;
            int position = Long.numberOfTrailingZeros(piece);
            if(kingPos/8 <= position/8) continue;          //if the king isn't in front of the pawn, then no good

            //special cases of A and H pawns
            if(position % 8 == 0) {
               if(kingPos %8 == 1 && GetDistance(kingPos,56) < GetDistance(enemyKingPos,56))
                  return -500;
               continue;
            }
            else if(position % 8 == 7 ) {
               if(kingPos % 8 == 6 && GetDistance(kingPos,63) < GetDistance(enemyKingPos,63))
                  return -500;
               continue;
            }


            if(GetDistance(kingPos,position) < GetDistance(enemyKingPos, position)) {
               if(kingPos/8 >= (position/8 + 2))      //if king is two ranks in front of pawn rank and closer than the enemy king
                  return -500;
               else if(kingPos/8 == 5 )               //if king is on 6th rank and closer
                  return -500;
            }

           int moving = 0;
           if(side == -1)
              moving = 1;
           if(kingPos/8 == position/8 + 1) {                        //if king is one ranks in front of pawn rank and same distance as the enemy king
               if((GetDistance(kingPos,position) - moving <= GetDistance(enemyKingPos, position)))  {
                  if(side == -1 && (enemyKingPos == position + 16 || enemyKingPos == kingPos + 16))            //here is a position where this doesn't win
                     return 0;
                  else if(side == 1) {
                     if(enemyKingPos/8 == kingPos/8 + 2) {                                //here is another position where it is not a win
                        int kingRank = kingPos % 8;
                        int enemyKingRank = enemyKingPos % 8;
                        if(enemyKingRank == kingRank + 1 || enemyKingRank == kingRank - 1)
                        return 0;
                     }
                  }

                  if((GetDistance(kingPos,position) == 2) && (enemyKingPos == position + 16 || enemyKingPos == position + 8))      //and another
                     return 0;

                  return -500;
               }
            }
         }
      }

       if(chessBoard.totalValue == Global.values[5] * noBlackPawns ) {           //if we only have white pawns on the board (plus kings of course)
          int kingPos = Long.numberOfTrailingZeros(chessBoard.blackking);
          int enemyKingPos = Long.numberOfTrailingZeros(chessBoard.whiteking);

          long pawns = chessBoard.blackpawns;
          while(pawns != 0) {
            long piece = pawns & -pawns;
            pawns ^= piece;
            int position = Long.numberOfTrailingZeros(piece);
            if(kingPos/8 >= position/8) continue;          //if the king isn't in front of the pawn, then no good

            //special cases of A and H pawns
            if(position % 8 == 0) {
               if(kingPos %8 == 1 && GetDistance(kingPos,0) < GetDistance(enemyKingPos,0))
                  return 500;
               continue;
            }
            else if(position % 8 == 7) {
               if(kingPos % 8 == 6 && GetDistance(kingPos,7) < GetDistance(enemyKingPos,7))
                  return 500;
               continue;
            }
            if(GetDistance(kingPos,position) < GetDistance(enemyKingPos, position)) {
               if(kingPos/8 <= position/8 - 2)
                  return 500;
               else if(kingPos/8 == 2 )
                  return 500;
            }

           int moving = 0;
           if(side == 1)
              moving = 1;

            if(kingPos/8 == position/8 - 1) {                        //if king is one ranks in front of pawn rank and same distance as the enemy king
               if((GetDistance(kingPos,position) - moving <= GetDistance(enemyKingPos, position)))  {
                  if(side == 1 && (enemyKingPos == position - 16 || enemyKingPos == kingPos - 16))            //here is a position where this doesn't win
                     return 0;
                  else if(side == -1) {
                     if(enemyKingPos/8 == kingPos/8 - 2) {                                //here is another position where it is not a win
                        int kingRank = kingPos % 8;
                        int enemyKingRank = enemyKingPos % 8;
                        if(enemyKingRank == kingRank + 1 || enemyKingRank == kingRank - 1)
                        return 0;
                     }
                  }

                  if((GetDistance(kingPos,position) == 2) && (enemyKingPos == position - 16 || enemyKingPos == position - 8))      //and another
                     return 0;

                  return -500;
               }
            }
         }
      }
      return GetEndPawnRace(passBits, side);
    }

    private static final int GetEndPawnRace(long passBits, int side)   {
       long whitepass = passBits & chessBoard.whitepawns;
       long blackpass = passBits & chessBoard.blackpawns;
       int wQueenDist = 8;
       int bQueenDist = 8;

       boolean forcedKingMoveW = false;
       boolean forcedKingMoveB = false;

       //if all black has is king and pawn and white has passed pawns
       if(whitepass != 0 && chessBoard.blackpieces == (chessBoard.blackking | chessBoard.blackpawns) ) {
          boolean pMoves = false;
          if(side == -1)
             pMoves = true;         //flag indicates white is on move

          while(whitepass != 0) {
               long piece = whitepass & -whitepass;
               whitepass ^= piece;
               int position = Long.numberOfTrailingZeros(piece);
               int nextPosition = position+8;
               if(position / 8 == 1)
                  nextPosition += 8;

               //if the black king can stop the pawn, but to do so, it must move immediately
               if(side == -1 && CanKingStopWPawn(position, bKingPos, true) &&  !CanKingStopWPawn(nextPosition, bKingPos, true))  {  //white pawn gets to move first
                  forcedKingMoveB = true;
                  continue;
               } else if(side == 1 && CanKingStopWPawn(position, bKingPos, false) &&  !CanKingStopWPawn(position, bKingPos, true)) {  //black king gets to move first
                  forcedKingMoveB = true;
                  continue;
               }

               if(!CanKingStopWPawn(position, bKingPos, pMoves)) {
                  int queen_dist = 7 - position / 8;
                  if( (Global.plus8[position] & chessBoard.whiteking) != 0) {
                     if(position % 8 == 0 || position % 8 == 7) continue;
                     queen_dist++;
                  }
                  if(position / 8 == 1)
                     queen_dist--;
                  if(queen_dist < wQueenDist)
                     wQueenDist = queen_dist;
               }
          }
       }

       if(blackpass != 0 && chessBoard.whitepieces == (chessBoard.whiteking | chessBoard.whitepawns) ) {
          boolean pMoves = false;
          if(side == 1)
             pMoves = true;

          while(blackpass != 0) {
               long piece = blackpass & -blackpass;
               blackpass ^= piece;
               int position = Long.numberOfTrailingZeros(piece);
               int nextPosition = position-8;
               if(position / 8 == 6)
                  nextPosition -= 8;

               //if the white king can stop the pawn, but to do so, it must move immediately
               if(side == 1 && CanKingStopBPawn(position, wKingPos, true) &&  !CanKingStopBPawn(nextPosition, wKingPos, true))  {  //white pawn gets to move first
                  forcedKingMoveW = true;
                  continue;
               } else if(side == -1 && CanKingStopBPawn(position, wKingPos, false) &&  !CanKingStopBPawn(position, wKingPos, true)) {  //black king gets to move first
                  forcedKingMoveW = true;
                  continue;
               }

               if(!CanKingStopBPawn(position, wKingPos, pMoves)) {
                  int queen_dist = position / 8;
                  if( (Global.minus8[position] & chessBoard.blackking) != 0) {
                     if(position % 8 == 0 || position % 8 == 7) continue;
                     queen_dist++;
                  }
                  if(position / 8 == 6)
                     queen_dist--;
                  if(queen_dist < bQueenDist)
                     bQueenDist = queen_dist;
               }
          }
       }

       if(forcedKingMoveW && forcedKingMoveB)   return 0;

       if( wQueenDist  < 8 && bQueenDist == 8 )
          return -500;
       else if( bQueenDist < 8 &&  wQueenDist == 8 )
          return 500;
       else if( wQueenDist < bQueenDist && forcedKingMoveW && !forcedKingMoveB)
          return -500;
       else if( bQueenDist < wQueenDist && forcedKingMoveB && !forcedKingMoveW)
          return 500;
       else
          return 0;
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
        if(chessBoard.wCastle > Global.CASTLED && chessBoard.bCastle > Global.CASTLED) {
            if((chessBoard.whitequeen & Global.set_Mask[3])!= 0)
                develop-=20;
            if((chessBoard.blackqueen & Global.set_Mask[59]) != 0)
                develop+=20;
       }

       switch(chessBoard.wCastle) {
            case(Global.CASTLED):
                    castle -= 40;
                    break;
            case(Global.SHORT_CASTLE):
                    castle -= 10;
                    break;
            case(Global.LONG_CASTLE):
                    castle -= 10;
                    break;
            case(Global.BOTH_CASTLE):
                    castle -= 20;
                    break;
            }

        switch(chessBoard.bCastle) {
            case(Global.CASTLED):
                    castle += 40;
                    break;
            case(Global.SHORT_CASTLE):
                    castle += 10;
                    break;
            case(Global.LONG_CASTLE):
                    castle += 10;
                    break;
            case(Global.BOTH_CASTLE):
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

   private static final void setPawnAttack() {
      long pawns = chessBoard.whitepawns;
      while(pawns != 0) {
         noWhitePawns++;
         long piece = pawns & -pawns;
         pawns ^= piece;
         int position = Long.numberOfTrailingZeros(piece);
         if(position % 8 > 0) {
            WB[position + 7]++;
            WB[position + 7] |= PAWN_BIT;
            centre -= centreArr[position + 7];
         }
         if(position % 8 < 7) {
            WB[position + 9]++;
            WB[position + 9] |= PAWN_BIT;
            centre -= centreArr[position + 9];
         }
      }
      long forwardMove = chessBoard.whitepawns << 8;
      forwardMove &= ~chessBoard.bitboard;
      int count;

      for(count = 0; forwardMove != 0; count++) {
         forwardMove &= forwardMove-1;
      }
      mobility -= count * PAWN_MOBILITY;

      pawns = chessBoard.blackpawns;
      while(pawns != 0) {
         noBlackPawns++;
         long piece = pawns & -pawns;
         pawns ^= piece;
         int position = Long.numberOfTrailingZeros(piece);
         if(position % 8 > 0) {
            BB[position - 9]++;
            BB[position - 9] |= PAWN_BIT;
            centre += centreArr[position - 9];
         }
         if(position % 8 < 7) {
            BB[position - 7]++;
            BB[position - 7] |= PAWN_BIT;
            centre += centreArr[position - 7];
         }
      }
      forwardMove = chessBoard.blackpawns >> 8;
      forwardMove &= ~chessBoard.bitboard;

      for(count = 0; forwardMove != 0; count++) {
         forwardMove &= forwardMove-1;
      }
      mobility += count * PAWN_MOBILITY;

   }

    /**
     * Method getWhitePawnFile
     *
     * calculates an integer bit string based on the state of the white pawn file
     * value is 7 if no pawn exists on the file
     *
     * @return int - 32bits packed with the file info
     */
    private static final int getWhitePawnFile() {

        int wPawnFile = 0;
        for(int i=0; i<=7; i++) {       //compute the lowest rank of white pawns in each file

            long temp = chessBoard.whitepawns & Global.fileMasks[i];
            if(temp != 0) {
               long temp2 = temp & -temp;
               temp ^= temp2;
               wPawnFile |= (Long.numberOfTrailingZeros(temp2)/8)<<(3*i);         //shift to position for file
            } else
               wPawnFile |= 7<<(3*i);
        }
        return wPawnFile;
    }

    /**
     * Method getBlackPawnFile
     *
     * calculates an integer bit string based on the state of the black pawn file
     * value is 0 if no pawn exists on the file
     *
     * @return int - 32bits packed with the file info
     */
    private static final int getBlackPawnFile() {

        int bPawnFile = 0;
        for(int i=0; i<=7; i++) {       //compute the highest rank of enemy pawns in each file

            long temp = chessBoard.blackpawns & Global.fileMasks[i];
            while(temp != 0) {
               long temp2 = temp & -temp;
               temp ^= temp2;
               if(temp == 0)
                   bPawnFile |= (Long.numberOfTrailingZeros(temp2)/8)<<(3*i);
            }
        }
        return bPawnFile;
    }


    /**
     * Method bitToAttayWhitePawnFile
     *
     * takes the bit string representing the most forward pawn position on the file
     * and sets up an array that will be used in the rest of the evaluation
     *
     * @param int fileBit - the 32 bits of packed file info
     *
     */
    private static final void bitToArrayWhitePawnFile(int fileBit) {
        wPawnFileVals = new byte[8];
        for(int i=0; i<=7; i++) {
            wPawnFileVals[i] = (byte)((fileBit >>> (3*i)) & 7);
        }
    }

    /**
     * Method bitToAttayBlackPawnFile
     *
     * takes the bit string representing the most forward pawn position on the file
     * and sets up an array that will be used in the rest of the evaluation
     *
     * @param int fileBit - the 32 bits of packed file info
     *
     */
    private static final void bitToArrayBlackPawnFile(int fileBit) {
        bPawnFileVals = new byte[8];
        for(int i=0; i<=7; i++) {
            bPawnFileVals[i] = (byte)((fileBit >>> (3*i)) & 7);
        }
    }

    /**
     * Method getWPawnsScore2
     *
     * this method calculates penalties for doubled pawns, isolated pawns, weak pawns and the positional score
     * for white pawns
     *
     * @return int - the white pawn positional eval score
     *
     */
    private static final int getWPawnsScore2() {

        long piece;
        int position;

        int isolatedScore = 0;          //isolated pawn score
        int doubledScore = 0;           //doubled pawn score
        int weakScore = 0;              //backward pawn score
        int positionScore = 0;          //positional piece square table score

        int doubleCount = 0;
        boolean tempWeak;
        boolean isolated;
        long file;

        for(int i = 0 ; i <= 7 ; i++ ) {
            file = chessBoard.whitepawns & Global.fileMasks[i];
            isolated = true;
            doubleCount = 0;
            if( i > 0 && wPawnFileVals[i-1] < 7 )
                isolated = false;
            else if( i < 7 && wPawnFileVals[i+1] < 7 )
                isolated = false;

            while(file != 0) {
                piece = file & -file;
                file ^= piece;
                position = Long.numberOfTrailingZeros(piece);
                positionScore -= wPawnVals[position];
                if(doubleCount == 1)
                    doubledScore += DOUBLED_PAWN;
                else if(doubleCount > 1)
                    doubledScore += TRIPLE_PAWN;
                doubleCount++;
                if(isolated)
                    isolatedScore += ISOLATED_PAWN;
                if(isolated) continue;

                if(WB[position] == 0 && BB[position] == 0) {           //if pawn is not isolated, check for weakness if pawn not defended or attacked
                    tempWeak = true;

                    if(Board.piece_in_square[position+8] != 5 && (WB[position+8] >= BB[position+8]))   //if pawn can advance not weak
                        tempWeak = false;
                    else if(tempWeak & position/8 >= 3 ) {  //if a pawn behind can advance to help not weak
                        if(((i - 1) >= 0) && Board.piece_in_square[position-17] == 5) {     //check for friend to left
                            //check to see if this friend pawn can advance to support the pawn
                            if((Board.piece_in_square[position - 9] != 11) && (WB[position-9] >= BB[position-9]))
                                tempWeak = false;
                        }
                        if(tempWeak == true && (i + 1 <= 7) && Board.piece_in_square[position-15] == 5) {//check for friend to right
                             //check to see if this friend pawn can advance to support the pawn
                            if((Board.piece_in_square[position - 7] != 11) && (WB[position-7] >= BB[position-7]))
                                tempWeak = false;
                        }
                    }
                    if(tempWeak) {
                        if(bPawnFileVals[i] == 0)           //if weak pawn is on an open file - bad
                            weakScore += WEAK_OPEN_PAWN;
                        weakScore += WEAK_PAWN;
                    }

                }
            }
        }
        return isolatedScore + weakScore + positionScore + doubledScore;
    }

    /**
     * Method getBPawnsScore2
     *
     * this method calculates penalties for doubled pawns, isolated pawns, weak pawns and the positional score
     * for black pawns
     *
     * @return int - the black pawn positional eval score
     *
     */
    private static final int getBPawnsScore2() {

        long piece;
        int position;

        int isolatedScore = 0;          //isolated pawn score
        int doubledScore = 0;           //doubled pawn score
        int weakScore = 0;              //backward pawn score
        int positionScore = 0;          //positional piece square table score

        int doubleCount = 0;
        boolean tempWeak;
        boolean isolated;
        long file;

        for(int i = 0 ; i <= 7 ; i++ ) {
            file = chessBoard.blackpawns & Global.fileMasks[i];
            isolated = true;
            doubleCount = 0;
            if( i > 0 && bPawnFileVals[i-1] > 0 )
                isolated = false;
            else if( i < 7 && bPawnFileVals[i+1] > 0 )
                isolated = false;

            while(file != 0) {
                piece = file & -file;
                file ^= piece;
                position = Long.numberOfTrailingZeros(piece);
                positionScore += bPawnVals[position];
                if(doubleCount == 1)
                    doubledScore -= DOUBLED_PAWN;
                else if(doubleCount > 1)
                    doubledScore -= TRIPLE_PAWN;
                doubleCount++;
                if(isolated)
                    isolatedScore -= ISOLATED_PAWN;
                if(isolated) continue;

                if(BB[position] == 0 && WB[position] == 0) {           //if pawn is not isolated, check for weakness if pawn not defended or attacked
                    tempWeak = true;

                    if(Board.piece_in_square[position-8] != 11 && (BB[position-8] >= WB[position-8]))   //if pawn can advance not weak
                        tempWeak = false;
                    else if(tempWeak & position/8 <= 4 ) {  //if a pawn behind can advance to help not weak
                        if(((i - 1) >= 0) && Board.piece_in_square[position+15] == 11) {     //check for friend to left
                            //check to see if this friend pawn can advance to support the pawn
                            if((Board.piece_in_square[position + 7] != 5) && (BB[position+7] >= WB[position+7]))
                                tempWeak = false;
                        }
                        if(tempWeak == true && (i + 1 <= 7) && Board.piece_in_square[position+17] == 11) {//check for friend to right
                             //check to see if this friend pawn can advance to support the pawn
                            if((Board.piece_in_square[position + 9] != 5) && (BB[position+9] >= WB[position+9]))
                                tempWeak = false;
                        }
                    }
                    if(tempWeak) {
                        if(wPawnFileVals[i] == 7)           //if weak pawn is on an open file - bad
                            weakScore -= WEAK_OPEN_PAWN;
                        weakScore -= WEAK_PAWN;
                    }

                }
            }
        }
        return isolatedScore + weakScore + positionScore + doubledScore;
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
     * Method getPassedPawnsBits
     *
     * returns a bit string representing all passed pawns on the chessBoard
     *
     * @return long - 64 bits of passed pawn info packed in
     *
     */
    private static final long getPassedPawnsBits() {

        long passers = 0;

        long pawns = chessBoard.whitepawns;
        long piece;
        int position;
        while (pawns != 0) {

            piece = pawns&(-pawns);
            pawns ^= piece;
            position = Long.numberOfTrailingZeros(piece);

            if(isPassedPawn(-1,position))
                passers |= piece;
        }
        pawns = chessBoard.blackpawns;

        while (pawns != 0) {

            piece = pawns&(-pawns);
            pawns ^= piece;
            position = Long.numberOfTrailingZeros(piece);
            if(isPassedPawn(1,position))
                passers |= piece;
        }
        return passers;
    }

   
    /**
     * Method GetWhitePassedPawnScore
     *
     * calculates the passed pawn bonus for white passed pawns
     * this is based on many dynamic features
     *
     * @param long passers - the 64 bits packed with passed pawns
     *
     * @return int - the passed pawn bonus score
     *
     */
    private static final int GetWhitePassedPawnScore(long passers) {
        long piece;
        int position;
        int rank;
        int file;
        int currentScore = 0;
        int totalPass = 0;
        passers &= chessBoard.whitepawns;            //get only the white pawns
        long whitePassers = passers;            //whitePassers keeps original white passed pawns
        while(passers != 0) {
            piece = passers & -passers;
            passers ^= piece;
            position = Long.numberOfTrailingZeros(piece);
            long rookAttacks = chessBoard.getMagicRookMoves(position);
            rank = position / 8;                //get rank of pawn we are testing
            file = position % 8;

            // interpolate bonus between middle and endgame
            currentScore = (int)(-wPassedPawnBonus[rank] * (1.0 - endGameCoefficient));
            currentScore -=  (int)(wPassedPawnBonusEndGame[rank] * (endGameCoefficient));

            if(( file > 0) && ((whitePassers & Global.fileMasks[file-1]) != 0)) {  //if passed pawn beside current pawn
                currentScore -= (10 + CONNECTED_PASSED_BONUS * endGameCoefficient);
            } else {
                if((file > 0) && (Board.piece_in_square[position-9] == 5 || Board.piece_in_square[position-1] == 5)) //pawn beside current pawn
                    currentScore -= (5 + PROTECTED_PASSED_BONUS * endGameCoefficient);
            }
            if(( file < 7) && ((whitePassers & Global.fileMasks[file+1]) != 0)) {   //if passed pawn beside current pawn
                currentScore -= ( 10 + CONNECTED_PASSED_BONUS * endGameCoefficient);
            } else {
                if(( file < 7) && (Board.piece_in_square[position-7] == 5 || Board.piece_in_square[position+1] == 5))   //pawn beside current pawn
                   currentScore -= ( 5 + PROTECTED_PASSED_BONUS * endGameCoefficient);
            }

            if((rookAttacks & Global.minus8[position] & chessBoard.whiterooks) != 0)   //bonus for rook behind the pawn
                currentScore -=  (5 + currentScore * 0.2 * endGameCoefficient);

            if(Board.piece_in_square[position+8] >= 0) {        //if the pawn is blocked
                if(Board.piece_in_square[position+8] > 5)               //blocked by an enemy piece
                    currentScore *= 0.6;
                else                                                    //blocked by a friend
                    currentScore *= 0.8;
            }

            if(BB[position+8] != 0)                   //position in front of pawn attacked by enemy
                currentScore *= 0.85;
            if(WB[position+8] >= MINOR_BIT)           //position in front of pawn defended by a bishop, knight, rook or queen
                currentScore *= 1.15;

            if(endGameCoefficient >= 0.99) {             //if this is the endgame and we have the king's support
                if((WB[position] & KING_BIT) != 0)
                    currentScore *= 1.15;
                else if((WB[position+8] & KING_BIT) != 0)
                    currentScore *= 1.15;
            }

            if(chessBoard.totalValue < 1200) {           //late end game..bonus for keeping enemy king away from passed pawn
                int x = Math.abs(bKingPos%8 - file);
                int y = Math.abs(bKingPos/8 - rank);
                if(y > x)
                    x = y;
                currentScore -= x * wPawnEnemyKingTropism[position];
            }

            if((Global.plus8[position] & chessBoard.whitepawns) != 0)            //doubled pawn - reduce bonus
                currentScore /= 3;

            totalPass += currentScore;
        }
        return totalPass;
    }

    /**
     * Method GetBlackPassedPawnScore
     *
     * calculates the passed pawn bonus for white passed pawns
     * this is based on many dynamic features
     *
     * @param long passers - the 64 bits packed with passed pawns
     *
     * @return int - the passed pawn bonus score
     *
     */
    private static final int GetBlackPassedPawnScore(long passers) {
        long piece;
        int position;
        int rank;
        int file;
        int currentScore = 0;
        int totalPass = 0;
        passers &= chessBoard.blackpawns;            //get only the white pawns
        long blackPassers = passers;            //whitePassers keeps original white passed pawns
        while(passers != 0) {
            piece = passers & -passers;
            passers ^= piece;
            position = Long.numberOfTrailingZeros(piece);
            long rookAttacks = chessBoard.getMagicRookMoves(position);
            rank = position / 8;                //get rank of pawn we are testing
            file = position % 8;

            // interpolate bonus between middle and endgame
            currentScore = (int)(bPassedPawnBonus[rank] * (1.0 - endGameCoefficient));
            currentScore +=  (int)(bPassedPawnBonusEndGame[rank] * (endGameCoefficient));

            if(( file > 0) && ((blackPassers & Global.fileMasks[file-1]) != 0)) {  //if passed pawn beside current pawn
                currentScore += 10 + CONNECTED_PASSED_BONUS * endGameCoefficient;
            } else {
                if((file > 0) && (Board.piece_in_square[position-9] == 11 || Board.piece_in_square[position-1] == 11)) //pawn beside current pawn
                    currentScore += 5 + PROTECTED_PASSED_BONUS * endGameCoefficient;
            }
            if(( file < 7) && ((blackPassers & Global.fileMasks[file+1]) != 0)) {   //if passed pawn beside current pawn
                currentScore += 10 + CONNECTED_PASSED_BONUS * endGameCoefficient;
            } else {
                if(( file < 7) && (Board.piece_in_square[position-7] == 11 || Board.piece_in_square[position+1] == 11))   //pawn beside current pawn
                   currentScore += 5 + PROTECTED_PASSED_BONUS * endGameCoefficient;
            }

            if((rookAttacks & Global.plus8[position] & chessBoard.blackrooks) != 0)   //bonus for rook behind the pawn
                currentScore +=  (5 + currentScore * 0.2 * endGameCoefficient);

            if(Board.piece_in_square[position-8] >= 0) {        //if the pawn is blocked
                if(Board.piece_in_square[position+8] < 6)               //blocked by an enemy piece
                    currentScore *= 0.6;
                else                                                    //blocked by a friend
                    currentScore *= 0.8;
            }

            if(WB[position+8] != 0)                   //position in front of pawn attacked by enemy
                currentScore *= 0.85;
            if(BB[position+8] >= MINOR_BIT)           //position in front of pawn defended by a bishop, knight, rook or queen
                currentScore *= 1.15;

            if(endGameCoefficient >= 0.99) {             //if this is the endgame and we have the king's support
                if((BB[position] & KING_BIT) != 0)
                    currentScore *= 1.15;
                else if((BB[position+8] & KING_BIT) != 0)
                    currentScore *= 1.15;
            }

            if(chessBoard.totalValue < 1200) {           //late end game..bonus for keeping enemy king away from passed pawn
                int x = Math.abs(wKingPos%8 - file);
                int y = Math.abs(wKingPos/8 - rank);
                if(y > x)
                    x = y;
                currentScore += x * bPawnEnemyKingTropism[position];

            }

            if((Global.minus8[position] & chessBoard.blackpawns) != 0)            //doubled pawn - reduce bonus
                currentScore /= 3;

            totalPass += currentScore;
        }
        return totalPass;
    }


    /**
     * To-do test to see if this code improves play (currently not used)
     *
     * Method getWeakAttackScore
     *
     * calculates penalties for weak penalties
     * this is based on many dynamic features
     *
     * @return int - combined penalties for both sides
     *
     */
    private static final int getWeakAttackScore() {

        long pawns = wWeakPawns;
        long pawn;
        int position;
        int weakAttack = 0;

        while(pawns != 0) {
            pawn = pawns & -pawns;
            pawns ^= pawn;
            position = Long.numberOfTrailingZeros(pawn);
            if((BB[position] & PAWN_BIT) != 0) continue;       //if white pawn can capture a black pawn no penalty
            if(BB[position] == 0) continue;                    //if no black attackers then no penalty
            int defenders = WB[position]>>3;
            int attackers = BB[position]>>3;
            defenders = defenders & -defenders;             //get smallest defender
            attackers = attackers & -attackers;             //get smallest attacker
            if(defenders < attackers) continue;          //dont care if bishop defendes pawn attacked by rook etc
            weakAttack += 5 * (BB[position] & 7);       //award a penalty of 10 for each attacker on the weak pawn
        }

        pawns = bWeakPawns;
        while(pawns != 0) {
            pawn = pawns & -pawns;
            pawns ^= pawn;
            position = Long.numberOfTrailingZeros(pawn);
            if((WB[position] & PAWN_BIT) != 0) continue;       //if black pawn can capture a black pawn no penalty
            if(WB[position] == 0) continue;                    //if no white attackers then no penalty
            int defenders = BB[position]>>3;
            int attackers = WB[position]>>3;
            defenders = defenders & -defenders;             //get smallest defender
            attackers = attackers & -attackers;             //get smallest attacker
            if(defenders < attackers) continue;          //dont care if bishop defendes pawn attacked by rook etc
            weakAttack -= 5 * (WB[position] & 7);       //award a penalty of 10 for each attacker on the weak pawn
        }
        return weakAttack;
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
    private static int getWKnightEval() {
        long knights = chessBoard.whiteknights;
        int score = 0;

        while(knights != 0) {
            int position = Long.numberOfTrailingZeros(knights);
            knights ^= Global.set_Mask[position];
            if(position < 8)                          //knight on 1st rank
                develop += BACKRANK_MINOR;            //penalize
            long attacks = chessBoard.getKnightMoves(position);
            while(attacks != 0) {                       //update the WB table
               int pos = Long.numberOfTrailingZeros(attacks);
               attacks ^= Global.set_Mask[pos];
               centre -= centreArr[pos];
               if(Board.piece_in_square[pos] == -1 || Board.piece_in_square[pos] > 5 )  {     //if not attacking own piece add mobility
                    if((BB[pos] & PAWN_BIT) == 0)
                        mobility-= KNIGHT_MOBILITY;
               }
               WB[pos]++;
               WB[pos] |= MINOR_BIT;
            }
            score -= wKnightVals[position];
            score += chessBoard.getDistance(position, bKingPos);
        }
        return score;
    }
    /**
     * Method getBKnightEval
     *
     * calculates positional score for black knight
     * adds knight attack to BB array
     *
     * @return int - knight positional score
     *
     */
    private static int getBKnightEval() {
        long knights = chessBoard.blackknights;
        int score = 0;
        while(knights != 0) {
            long piece = knights & -knights;
            knights ^= piece;
            int position = Long.numberOfTrailingZeros(piece);
            if(position/8 == 7)                         //knight on 8th rank
                develop -= BACKRANK_MINOR;              //penalize
            long attacks = chessBoard.getKnightMoves(position);
            while(attacks != 0) {                       //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Long.numberOfTrailingZeros(attack);
               centre += centreArr[pos];
               if(Board.piece_in_square[pos] < 6)  {     //if not attacking own piece add mobility
                    if((WB[pos] & PAWN_BIT) == 0)
                        mobility+= KNIGHT_MOBILITY;
               }
               BB[pos]++;
               BB[pos] |= MINOR_BIT;
            }
            score += bKnightVals[position];
            score -= chessBoard.getDistance(position, wKingPos);
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
    private static int getWQueenEval() {
        long queens = chessBoard.whitequeen;
        int score = 0;
        while(queens != 0) {
            long piece = queens & -queens;
            queens ^= piece;
            int position = Long.numberOfTrailingZeros(piece);
            long attacks = chessBoard.getQueenMoves(position);           //get attacks for queen
            while(attacks != 0) {                                   //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Long.numberOfTrailingZeros(attack);
               centre -= centreArr[pos];
               if(Board.piece_in_square[pos] < 0 || Board.piece_in_square[pos] >5)
                   mobility -= QUEEN_MOBILITY;

               WB[pos]++;
               WB[pos] |= QUEEN_BIT;
            }
            score += 3 * chessBoard.getDistance(position, bKingPos);        //tropism
        }
        return score;
    }

    /**
     * Method getBQueenEval
     *
     * calculates positional score for black queen
     * adds queen attack to BB array
     *
     * @return int - queen positional score
     *
     */
    private static int getBQueenEval() {
        long queens = chessBoard.blackqueen;
        int score = 0;
        while(queens != 0) {
            long piece = queens & -queens;
            queens ^= piece;
            int position = Long.numberOfTrailingZeros(piece);
            long attacks = chessBoard.getQueenMoves(position);            //get queen moves
            while(attacks != 0) {                       //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Long.numberOfTrailingZeros(attack);
               centre += centreArr[pos];
               if(Board.piece_in_square[pos] < 6)
                   mobility += QUEEN_MOBILITY;
               BB[pos]++;
               BB[pos] |= QUEEN_BIT;
            }
            score -= 3 * chessBoard.getDistance(position, wKingPos);        //tropism
        }
        return score;
    }

    /**
     * Method setWKingEval
     *
     * sets the white king position, attack zone, and updates the WB table
     *
     */
    private static void setWKingEval() {
        long king = chessBoard.whiteking;
        wKingPos = Long.numberOfTrailingZeros(king);                          //get position of white king
        int kingFile = wKingPos%8;
        if(kingFile < 3)                                        //if king in corner, use corner king zone
            kingFile = 0;
        else if(kingFile > 4)
            kingFile = 7;
        if((wKingPos/8) < 6)                                    //set the white king zone
            whiteKingZone = Global.wKingMask[kingFile]<<((wKingPos/8)*8);
        else
            whiteKingZone = 0;
        long attacks = Helper.getKingPosition(wKingPos);
        while(attacks != 0) {                       //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Long.numberOfTrailingZeros(attack);
               WB[pos]++;
               WB[pos] |= KING_BIT;
        }
    }

    /**
     * Method setBKingEval
     *
     * sets the black king position, attack zone, and updates the BB table
     *
     */
    private static void setBKingEval() {
        long king = chessBoard.blackking;
        bKingPos = Long.numberOfTrailingZeros(king);                          //get position of white king
        int kingFile = bKingPos%8;
        if (kingFile < 3)
            kingFile = 0;
        else if(kingFile > 4)
            kingFile = 7;
        if((bKingPos/8) > 1)                                    //set the black king zone
            blackKingZone = Global.bKingMask[kingFile]>>>((7-(bKingPos/8))*8);
        else
            blackKingZone = 0;
        long attacks = Helper.getKingPosition(bKingPos);
        while(attacks != 0) {                       //update the BB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Long.numberOfTrailingZeros(attack);
               BB[pos]++;
               BB[pos] |= KING_BIT;
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
    private static int getEndGameKing() {
        int score = 0;
        score -= kingVals[wKingPos];
        score += kingVals[bKingPos];
        return score;
    }

    /**
     * Method getKingSafetyWhite
     *
     * calculates the white king safety - considers attacks, defenders and pawn shield
     *
     * @return int - king safety score
     *
     */
    private static int getKingSafetyWhite() {
        int score = 0;
        int attackScore = 0;
        int openFilePenalty = 0;
        int position;

        int file = wKingPos%8;
        int rank = wKingPos/8;
        if(chessBoard.wCastle <= Global.CASTLED || chessBoard.bCastle <= Global.CASTLED) {
           int pawnShieldScore = 0;
           long pawnProtect = whiteKingZone & chessBoard.whitepawns;

            while(pawnProtect != 0) {
                long pawn = pawnProtect & -pawnProtect;
                pawnProtect ^= pawn;
                position = Long.numberOfTrailingZeros(pawn);
                pawnShieldScore += whitePawnProtection[position/8];
            }
            score -= pawnShieldScore;

            if(chessBoard.blackqueen != 0 && endGameCoefficient <=  0.7) {
               int counter = 0;
               int mask = 0;

               //collect the scores of the squares two positions in front of the king

               if(rank < 6) {
                  if(file - 1 >= 0)
                    mask |= BB[wKingPos+15];
                  if(file + 1 <= 7)
                    mask |= BB[wKingPos+17];
                  mask |= BB[wKingPos+16];
               }

                //collect the scores of the squares beside and behind the white king
               if(file - 1 >= 0) {
                  if(BB[wKingPos-1] > 0) {        //if square beside king attacked
                    mask |= BB[wKingPos-1];
                    counter++;                  //increment counter
                    if(WB[wKingPos-1] == -127)   //if square beside king attacked and not defended
                        counter++;              //increment counter
                  }
                  if(rank > 0) {
                    if(BB[wKingPos-9] > 0) {        //if square behind  king attacked
                        mask |= BB[wKingPos-9];
                        counter++;                  //increment counter
                        if(WB[wKingPos-9] == -127)   //if square behind king attacked and not defended
                            counter++;              //increment counter
                    }
                  }
               }
               if(file + 1 <= 7) {
                  if(BB[wKingPos+1] > 0) {        //if square beside king attacked
                     mask |= BB[wKingPos+1];
                     counter++;                  //increment counter
                     if(WB[wKingPos+1] == -127)   //if square beside king attacked and not defended
                        counter++;              //increment counter
                  }
                  if(rank > 0) {
                     if(BB[wKingPos-7] > 0) {        //if square behind king attacked
                        mask |= BB[wKingPos-7];
                        counter++;                  //increment counter
                        if(WB[wKingPos-7] == -127)   //if square behind king attacked and not defended
                            counter++;              //increment counter
                     }
                  }
               }

               if(rank > 0) {
                  if(BB[wKingPos-8] > 0) {        //if square behind king attacked
                     mask |= BB[wKingPos-8];
                      counter++;                  //increment counter
                     if(WB[wKingPos-8] == -127)   //if square behind king attacked and not defended
                        counter++;              //increment counter
                  }
               }

               // collect the scores of the squares directly in front of the enemy king

               if( rank < 7) {

                  if(BB[wKingPos + 8] > 0) {        //if square in front of king attacked
                     mask |= BB[wKingPos+8];
                     counter++;                  //increment counter
                     if(WB[wKingPos+ 8] == -127)   //if square in front of king attacked and not defended
                        counter++;              //increment counter
                     if(Board.piece_in_square[wKingPos+8] > 5)           //if there is an enemy black piece at this square
                        counter++;
                  }

                  if(file - 1 >= 0) {
                    if(BB[wKingPos + 7] > 0) {        //if square beside king attacked
                        mask |= BB[wKingPos+7];
                        counter++;                  //increment counter
                        if(WB[wKingPos+ 7] == -127)   //if square beside king attacked and not defended
                            counter++;              //increment counter
                        if(Board.piece_in_square[wKingPos+7] > 5)           //if there is an enemy black piece at this square
                            counter++;
                     }
                  }

                  if(file + 1 <= 7) {
                    if(BB[wKingPos+9] > 0) {        //if square beside king attacked
                        mask |= BB[wKingPos+9];
                        counter++;                  //increment counter
                        if(WB[wKingPos+9] == -127)   //if square beside king attacked and not defended
                            counter++;              //increment counter
                        if(Board.piece_in_square[wKingPos+9] > 5)
                            counter++;
                     }
                  }

               }

               mask>>=3;
               mask &= 31;
               counter += TABLE[mask];
               attackScore += kingSafetyEval[counter + wKingAttackVal[wKingPos]];

            }

           if(file == 0) {
                 if(wPawnFileVals[0] == 7) {
                   openFilePenalty += KING_PAWN_OPEN_FILE;
                   if(bPawnFileVals[0] == 0)
                       openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
               } else if(bPawnFileVals[0] == 0)
                   openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;

               if(wPawnFileVals[1] == 7) {
                   openFilePenalty += KING_PAWN_OPEN_FILE;
                   if(bPawnFileVals[1] == 0)
                       openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
               } else if(bPawnFileVals[1] == 0)
                   openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;

               if(wPawnFileVals[2] == 7) {
                   openFilePenalty += KING_PAWN_OPEN_FILE;
                   if(bPawnFileVals[2] == 0)
                       openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
               } else if(bPawnFileVals[2] == 0)
                   openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;

           } else if(file == 7) {
               if(wPawnFileVals[5] == 7) {
                   openFilePenalty += KING_PAWN_OPEN_FILE;
                   if(bPawnFileVals[5] == 0)
                       openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
               } else if(bPawnFileVals[5] == 0)
                   openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;

               if(wPawnFileVals[6] == 7) {
                   openFilePenalty += KING_PAWN_OPEN_FILE;
                   if(bPawnFileVals[6] == 0)
                       openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
               } else if(bPawnFileVals[6] == 0)
                   openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;

               if(wPawnFileVals[7] == 7) {
                   openFilePenalty += KING_PAWN_OPEN_FILE;
                   if(bPawnFileVals[7] == 0)
                       openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
               } else if(bPawnFileVals[7] == 0)
                   openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;
           } else {
               if(wPawnFileVals[file] == 7) {
                   openFilePenalty += KING_PAWN_OPEN_FILE;
                   if(bPawnFileVals[file] == 0)
                       openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
               } else if(bPawnFileVals[file] == 0)
                   openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;

               if(wPawnFileVals[file-1] == 7) {
                   openFilePenalty += KING_PAWN_OPEN_FILE;
                   if(bPawnFileVals[file-1] == 0)
                       openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
               } else if(bPawnFileVals[file-1] == 0)
                   openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;

               if(wPawnFileVals[file+1] == 7) {
                   openFilePenalty += KING_PAWN_OPEN_FILE;
                   if(bPawnFileVals[file+1] == 0)
                       openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
               } else if(bPawnFileVals[file+1] == 0)
                   openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;
            }

        }

    return (int)(((double)score + (double)openFilePenalty) * (1.0-endGameCoefficient)) + attackScore;
    }

    /**
     * Method getKingSafetyBlack
     *
     * calculates the black king safety - considers attacks, defenders and pawn shield
     *
     * @return int - king safety score
     *
     */
    private static int getKingSafetyBlack() {
        int score = 0;
        int attackScore = 0;
        int openFilePenalty = 0;
        int position;
        int file = bKingPos%8;
        int rank = bKingPos/8;
        if(chessBoard.bCastle <= Global.CASTLED || chessBoard.wCastle <= Global.CASTLED ) {
            int pawnShieldScore = 0;
            long pawnProtect = blackKingZone & chessBoard.blackpawns;

            while(pawnProtect != 0) {
                long pawn = pawnProtect & -pawnProtect;
                pawnProtect ^= pawn;
                position = Long.numberOfTrailingZeros(pawn);
                pawnShieldScore += blackPawnProtection[position/8];
            }
            score += pawnShieldScore;

         if(chessBoard.whitequeen != 0 && endGameCoefficient <=  0.7) {
             byte mask = 0;
             int counter = 0;
             //collect the scores of the squares two places in front of the black king
             if(rank > 1) {
                 if(file - 1 >= 0)
                     mask |= WB[bKingPos-17];
                 if(file + 1 <= 7 )
                     mask |= WB[bKingPos-15];
                 mask |= WB[bKingPos-16];
             }
             //collect the scores of the squares beside and behind the black king
             if(file - 1 >= 0) {
                if(WB[bKingPos-1] > 0) {        //if square beside king attacked
                    mask |= WB[bKingPos-1];
                    counter++;                  //increment counter
                    if(BB[bKingPos-1] == -127)   //if square beside king attacked and not defended
                        counter++;              //increment counter
                }
                if(rank < 7) {
                    if(WB[bKingPos+7] > 0) {        //if square behind  king attacked
                        mask |= WB[bKingPos+7];
                        counter++;                  //increment counter
                        if(BB[bKingPos+7] == -127)   //if square behind king attacked and not defended
                            counter++;              //increment counter
                    }
                }
            }
            if(file + 1 <= 7) {
                if(WB[bKingPos+1] > 0) {        //if square beside king attacked
                    mask |= WB[bKingPos+1];
                    counter++;                  //increment counter
                    if(BB[bKingPos+1] == -127)   //if square beside king attacked and not defended
                        counter++;              //increment counter
                }
                if(rank < 7) {
                   if(WB[bKingPos+9] > 0) {        //if square behind king attacked
                       mask |= WB[bKingPos+9];
                       counter++;                  //increment counter
                        if(BB[bKingPos+9] == -127)   //if square behind king attacked and not defended
                            counter++;              //increment counter
                   }
                }
            }

           if(rank < 7) {
               if(WB[bKingPos+8] > 0) {        //if square behind king attacked
                   mask |= WB[bKingPos+8];
                   counter++;                  //increment counter
                   if(BB[bKingPos+8] == -127)   //if square behind king attacked and not defended
                        counter++;              //increment counter
               }
           }


            // collect the scores of the squares directly in front of the enemy king

            if( rank > 0) {

                if(WB[bKingPos - 8] > 0) {        //if square in front of king attacked
                    mask |= WB[bKingPos-8];
                    counter++;                  //increment counter
                    if(BB[bKingPos - 8] == -127)   //if square in front of king attacked and not defended
                        counter++;              //increment counter
                    if(Board.piece_in_square[bKingPos - 8] < 6 && Board.piece_in_square[bKingPos - 8] >= 0)    //if there is an enemy white piece at this square
                        counter++;
                }



                if(file - 1 >= 0) {
                    if(WB[bKingPos - 9] > 0) {        //if square beside king attacked
                        mask |= WB[bKingPos-9];
                        counter++;                  //increment counter
                        if(BB[bKingPos - 9] == -127)   //if square beside king attacked and not defended
                            counter++;              //increment counter
                        if(Board.piece_in_square[bKingPos - 9] < 6 && Board.piece_in_square[bKingPos - 9] >= 0)    //if there is an enemy white piece at this square
                            counter++;
                    }
                }

                if(file + 1 <= 7) {
                    if(WB[bKingPos - 7] > 0) {        //if square beside king attacked
                        mask |= WB[bKingPos - 7];
                        counter++;                  //increment counter
                        if(BB[bKingPos - 7] == -127)   //if square beside king attacked and not defended
                            counter++;              //increment counter
                         if(Board.piece_in_square[bKingPos - 7] < 6 && Board.piece_in_square[bKingPos - 7] >= 0)    //if there is an enemy white piece at this square
                            counter++;
                    }
                }

            }
            mask>>=3;
            mask &=31;
            counter += TABLE[mask];
            attackScore -= kingSafetyEval[counter + bKingAttackVal[bKingPos]];
         }

        if(file == 0) {
            if(bPawnFileVals[0] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[0] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[0] == 7)
                openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;

            if(bPawnFileVals[1] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[1] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[1] == 7)
                 openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;

            if(bPawnFileVals[2] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[2] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[2] == 7)
                 openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;

        } else if(file == 7) {
            if(bPawnFileVals[5] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[5] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[5] == 7)
                 openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;

            if(bPawnFileVals[6] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[6] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[6] == 7)
                openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;

            if(bPawnFileVals[7] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[7] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[7] == 7)
                 openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;

        } else {
            if(bPawnFileVals[file] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[file] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[file] == 7)
                openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;

            if(bPawnFileVals[file-1] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[file-1] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[file-1] == 7)
                openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;

            if(bPawnFileVals[file+1] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[file+1] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[file+1] == 7)
                openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;
        }
        }
        return (int)(((double)score + (double)openFilePenalty) * (1.0-endGameCoefficient)) + attackScore;
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
    private static int getWBishopEval() {
        long bishops = chessBoard.whitebishops; 
        int score = 0;
        while(bishops != 0) {
            long piece = bishops & -bishops;
            bishops ^= piece;
            int position = Long.numberOfTrailingZeros(piece);
            if(position/8 == 0)                         //knight on 8th rank
                develop += BACKRANK_MINOR;              //penalize
            long attacks = chessBoard.getMagicBishopMoves(position);
            //attacks = chessBoard.getAttackBoard(position);
            while(attacks != 0) {                                   //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Long.numberOfTrailingZeros(attack);
               centre -= centreArr[pos];
               if(Board.piece_in_square[pos] < 0 || Board.piece_in_square[pos] >5)
                   mobility -= BISHOP_MOBILITY;
               WB[pos]++;
               WB[pos] |= MINOR_BIT;
            }
            score += chessBoard.getDistance(position, bKingPos);        //tropism
        }
        return score;
    }

    /**
     * Method getBBishopEval
     *
     * calculates positional score for black bishop
     * adds bishop attack to BB array
     *
     * @return int - bishop positional score
     *
     */
    private static int getBBishopEval() {
        long bishops = chessBoard.blackbishops;
        int score = 0;
        while(bishops != 0) {
            long piece = bishops & -bishops;
            bishops ^= piece;
            int position = Long.numberOfTrailingZeros(piece);
            if(position/8 == 7)                         //knight on 8th rank
                develop -= BACKRANK_MINOR;              //penalize
            long attacks = chessBoard.getMagicBishopMoves(position);
            //attacks = chessBoard.getAttackBoard(position);
            while(attacks != 0) {                                   //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Long.numberOfTrailingZeros(attack);
               centre += centreArr[pos];
               if(Board.piece_in_square[pos] < 6)
                   mobility += BISHOP_MOBILITY;
               BB[pos]++;
               BB[pos] |= MINOR_BIT;
            }
            score -= chessBoard.getDistance(position, wKingPos);        //tropism
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
    private static int getWRookEval() {
        long rooks = chessBoard.whiterooks;
        int rookScore = 0;
        int oldFile = -1;
        int oldRank = -1;

        while(rooks != 0) {
            int nearEnemyKing = 1;
            long piece = rooks & -rooks;
            rooks ^= piece;
            int position = Long.numberOfTrailingZeros(piece);
            int file = position % 8;
            int rank = position / 8;
            if(bKingPos/8 == 7 && rank == 6 )   {          //7th rank bonus
                rookScore -= ROOK_7TH_RANK;
                if(rank == oldRank)                     //doubled rook bonus;
                    rookScore -= DOUBLED_ROOKS_7TH;
                    if((Global.rankMasks[rank] & chessBoard.whitequeen) != 0)
                        rookScore -= QUEEN_ROOK_7TH_RANK;
            }
            if(wPawnFileVals[position%8] == 7) {           // semi open file
                if(Math.abs(position%8 - bKingPos%8) < 2)
                    nearEnemyKing = 2;
                long rookQueen = Global.fileMasks[file] & chessBoard.whitequeen ;
                if((rookQueen) != 0)   {     //rook queen connectivity
                    if(position > Long.numberOfTrailingZeros(rookQueen &  -rookQueen))
                        rookScore -= ROOK_IN_FRONT_QUEEN;
                    else
                        rookScore -= ROOK_BEHIND_QUEEN;
                }
                if(file == oldFile)
                    rookScore -= ROOK_DOUBLED * nearEnemyKing;
                if(bPawnFileVals[position%8] == 0)          // open file
                    rookScore -= ROOK_OPEN * nearEnemyKing;
                else
                    rookScore -= ROOK_SEMI * nearEnemyKing;
            }
            oldFile = file;
            oldRank = rank;
            long attacks = chessBoard.getMagicRookMoves(position);
            //attacks = chessBoard.getAttackBoard(position);
            while(attacks != 0) {                                   //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Long.numberOfTrailingZeros(attack);
               centre -= centreArr[pos];
               if(Board.piece_in_square[pos] < 0 || Board.piece_in_square[pos] >5)
                   mobility -= ROOK_MOBILITY;
               WB[pos]++;
               WB[pos] |= ROOK_BIT;
            }
            rookScore += chessBoard.getDistance(position, bKingPos);        //tropism
        }
        return rookScore;
    }

    /**
     * Method getBRookEval
     *
     * calculates positional score for black rook
     * adds rook attack to BB array
     *
     * @return int - rook positional score
     *
     */
    private static int getBRookEval() {
        long rooks = chessBoard.blackrooks;
        int rookScore = 0;
        int oldFile = -1;
        int oldRank = -1;

        while(rooks != 0) {
            int nearEnemyKing = 1;
            long piece = rooks & -rooks;
            rooks ^= piece;
            int position = Long.numberOfTrailingZeros(piece);
            int file = position % 8;
            int rank = position / 8;
            if(wKingPos/8 == 0  && rank == 1)   {          //7th rank bonus
                rookScore += ROOK_7TH_RANK;
                if(rank == oldRank)                     //doubled rook bonus;
                    rookScore += DOUBLED_ROOKS_7TH;
                if((Global.rankMasks[rank] & chessBoard.blackqueen) != 0)
                    rookScore += QUEEN_ROOK_7TH_RANK;
            }
            if(bPawnFileVals[position%8] == 0) {           // semi open file
                if(Math.abs(position%8 - wKingPos%8) < 2)
                    nearEnemyKing = 2;
                long rookQueen = Global.fileMasks[file] & chessBoard.blackqueen ;
                if((rookQueen) != 0)   {     //rook queen connectivity
                    if(position < Long.numberOfTrailingZeros(rookQueen &  -rookQueen))
                        rookScore += ROOK_IN_FRONT_QUEEN;
                    else
                        rookScore += ROOK_BEHIND_QUEEN;
                }
                if(file == oldFile)
                    rookScore += ROOK_DOUBLED * nearEnemyKing;
                if(wPawnFileVals[position%8] == 7)          // open file
                    rookScore += ROOK_OPEN * nearEnemyKing;
                else
                    rookScore += ROOK_SEMI * nearEnemyKing;
            }
            oldFile = file;
            oldRank = rank;
            long attacks = chessBoard.getMagicRookMoves(position);
            while(attacks != 0) {                                   //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Long.numberOfTrailingZeros(attack);
               centre += centreArr[pos];
               if(Board.piece_in_square[pos] < 6)
                   mobility += ROOK_MOBILITY;
               BB[pos]++;
               BB[pos] |= ROOK_BIT;
            }
            rookScore -= chessBoard.getDistance(position, wKingPos);        //tropism
        }
        return rookScore;
    }

    /**
     * Method whiteHungPieces
     *
     * calculates penalty for situation where white has hanging pieces

     * @return int - hanging piece penalty
     *
     */
    private static int whiteHungPieces() {
        long side = chessBoard.whitepieces & ~chessBoard.whiteking;
        int hung = 0;
        int score = 0;
        long piece;
        int position;
        while(side != 0) {
            piece = side & -side;
            side ^= piece;
            position = Long.numberOfTrailingZeros(piece);
            if(BB[position] > 0 &&  WB[position] == 0)
                hung++;
            else if(BB[position] > 0) {

                switch(Board.piece_in_square[position]) {

                    case 0:
                        if((BB[position] & (MINOR_BIT | PAWN_BIT)) != 0)
                            hung++;
                        break;
                    case 1:
                        if((BB[position] & PAWN_BIT) != 0)
                            hung++;
                        break;
                    case 2:
                        if((BB[position] & PAWN_BIT) != 0)
                            hung++;
                        break;
                    case 3:
                        if((BB[position] & (MINOR_BIT | PAWN_BIT | ROOK_BIT)) != 0)
                            hung++;
                        break;
                }
            }
        }
        if(hung == 2)
            score = HUNG_PENALTY;
        else if(hung > 2)
            score = 2*HUNG_PENALTY;
        return score;
    }

    /**
     * Method blackHungPieces
     *
     * calculates penalty for situation where black has hanging pieces

     * @return int - hanging piece penalty
     *
     */
    public static int blackHungPieces() {
        long side = chessBoard.blackpieces & ~chessBoard.blackking;
        int hung = 0;
        int score = 0;
        long piece;
        int position;
        while(side != 0) {
            piece = side & -side;
            side ^= piece;
            position = Long.numberOfTrailingZeros(piece);
            if(WB[position] > 0 &&  BB[position] == 0)
                hung++;
            else if(WB[position] > 0) {

                switch(Board.piece_in_square[position]) {

                    case 6:
                        if((WB[position] & (MINOR_BIT | PAWN_BIT)) != 0)
                            hung++;
                        break;
                    case 7:
                        if((WB[position] & PAWN_BIT) != 0)
                            hung++;
                        break;
                    case 8:
                        if((WB[position] & PAWN_BIT) != 0)
                            hung++;
                        break;
                    case 9:
                        if((WB[position] & (MINOR_BIT | PAWN_BIT | ROOK_BIT)) != 0)
                            hung++;
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

