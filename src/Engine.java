
/**
 * Engine.java
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
//import java.util.Arrays;
//import java.io.*;
/*
 * Engine.java
 * This class contains all the search functionality
 * -alpha beta - pvs search
 * -q search
 * -move generation functions - which uses chessBoard.java's individual piece move functions
 * -divide and perft functions for move generation testing and verification
 *
 * @version 	2.00 30 Jan 2010
 * @author 	Eric Stock
 */
public final class Engine {

   /** used similar to a C #define to instruct the move generation to properly generate all moves required for perft scoring */
   private static final boolean PERFT_ENABLED = false;
   /** chessBoard object from singleton class Board represents chess board and associated datastructures */
   private Board chessBoard;
   /** counter for positions evaluated */
   private static long nodes;			//counter for evaluated positions
   private static long hashnodes;		//counter for evaluated positions plus hash table exact hits
   /** 2D 64X64 arrays for history heuristic move ordering enhancement */
   private static final int[][] Hist = new int[64][64];
   private static final int[][] Hist2 = new int[64][64];
   private static TransTable HashTable = new TransTable(Global.HASHSIZE, 0);       //transposition table variable used in the search
   
   /** array for storing moves from the root position */
   private static final int[] moveOrder = new int[128];
   /** 2D array to store killer moves - 2 slots for each ply in the search */
   private static final int[][] killerMoves = new int[100][2];
   /** variables used to store time information - time in milliseconds*/
   private static long startTime, endTime;
   /** the maximum time we are allowed to extend the search when the 1st move drops at the root
    * ...also the max time we can extend by when we find a new best move, but make sure we verify this move
    * and collect its score
    */
   private static long maximumTime;
   /** the maximum time we are allowed to extend the search when we have started a depth iteration
    * and we do not have a score yet
    */
   private static long maximumRootGetAScoreTime;
   /** time used to complete the most recent iteration in the iteratively deepened search */
   private static long lastIterationTime;
   /** value which tracks the first root move's value at level 1 - used to determine if the search can stop when running low on time
    * If this variable indicates the root score has dropped, then the time will be extended
    */
   private static long levelOneValue;
   /** flag indicating a root move we scouted, has now failed high and must be researched...
    * we use this flag to prevent the time from running out until we can verify the move and
    * set it as the new best move if necessary
    */
   private static boolean failHighNewBestMoveFlag;
   /** this value represents the last score returned from the last ply of the iteratively deepened search
    * This will be used in time controls code to compare with the levelOneValue variable to see if we can stop the search
    */
   private static long lastRootIterationValue;
   /** global boolean flag to trigger then end of the search when out of time */
   private static boolean stop;
   /** stores node count at which a time check should occur */
   private static int nextTimeCheck;
   /** constants used to represent different steps of search */
   private static final int SEARCH_HASH = 1;
   private static final int KILLER_MOVES = 4;
   private static final int MATE_KILLER = 2;
   private static final int GOOD_CAPTURES = 3;
   private static final int NON_CAPS = 5;
   private static final int BAD_CAPS = 6;
   private static final int SEARCH_END = 7;
   private static final int TT_PV = 1 << 12;
   private static final int STANDPAT_PV = 2 << 12;
   private static final int ROOTDRAW_PV = 3 << 12;
   private static final int SEARCHDRAW_PV = 4 << 12;
   private static final int MATE_PV = 5 << 12;
   /** constants used to represent the 3 types of positions stored in the hash table */
   private static final int HASH_EXACT = 1;
   private static final int HASH_ALPHA = 0;
   private static final int HASH_BETA = 2;
   /** constants for starting values of search */
   private static final long ALPHA_START = -21000L;//Integer.MIN_VALUE+100;
   private static final long BETA_START = 21000L;//Integer.MAX_VALUE-100;
   private static final long VALUE_START = -21000L;//Integer.MIN_VALUE+100;
   /** variable used to track how deep in the search we are */
   private static int thisDepth;
   /** boolean which represents if we are using infinite time control */
   private static boolean infiniteTimeControl = false;
   /** used to track the age of hash table entries */
   private int ancient;
   /** count of moves generated when debugging move gen using perft command */
   private long perft = 0;
   /** instance of singleton MoveHelper Object used to store move info in a compact form */
   private MoveHelper Helper = MoveHelper.getInstance();
   /** "triangular array to store the PV **/
   private static final int[][] PV = new int[64][64];
  /** array to store the length of the PV **/
   private static final int[] lengthPV = new int[64];



   /**
    * Constructor Engine
    *
    * grabs a reference to the instantiated Board object
    *
    */
   public Engine() {
      chessBoard = Board.getInstance();
   }


   public void AdjustTime() {
      long currentTime = System.currentTimeMillis();
      long timeUsed = currentTime - startTime;
      lastIterationTime = timeUsed - lastIterationTime;
      long timeLeft = endTime - currentTime;
      if( timeLeft < lastIterationTime * 2 ) {
         endTime = Math.min( currentTime + (lastIterationTime * 2), startTime + maximumTime);
         System.out.println("info string time adjust");
      }
   }


   /**
    * Method timeLeft
    *
    * checks to see if we are out of time...if not, schedules next time check
    *
    * @return boolean - are we out of time?
    */
   public boolean timeLeft() {

      try {
         if (Main.reader.ready()) {
            if ("stop".equals(Main.reader.readLine())) {
               return false;
            }
         }
      } catch (Exception e) {System.out.println("Exception Caught and Swallowed !!!"); };

      long temp = System.currentTimeMillis();
      if (!infiniteTimeControl) {
         if (endTime > temp) {
            nextTimeCheck += ((endTime - temp) * 150);            //assumes a nodes per second of 150 000
            return true;
         } else {
            if(lastRootIterationValue < levelOneValue + 30 && !failHighNewBestMoveFlag)
               return false;
            else {
               if(((startTime + maximumRootGetAScoreTime) > temp) || (failHighNewBestMoveFlag && ((startTime + maximumTime) > temp)))
                   return true;
              else
                  return false;
            }
         }
      } else {
         nextTimeCheck += 1000;
         return true;
      }
   }

   /**
    * Method resetHash
    *
    * re-initializes the hash table
    * this would normally be called if the hash table is to be resized
    *
    */
   public static void resetHash() {
      HashTable = new TransTable(Global.HASHSIZE, 0);
      //add code to reset the eval table

      //add code to reset the pawn table
   }

   public int GetAllMoves(int side, int[] arrMoves)
   {
      int numberOfCaputres = getCaptures(side, arrMoves);
      int numberOfMoves = getMoves(side, arrMoves, numberOfCaputres);
      return numberOfMoves;
   }


   /**
    * Method search
    *
    * search handles the iteratively deepened alpha-beta search from the root
    *
    * @param int time - the maximum time for the search
    * @param int searchDepth - the maximum depth for the search
    * @param boolean inf - is this an inifinite search?
    *
    * @return String - the best move found
    *
    */
   public String search(int time, int maxTime, int searchDepth, boolean inf) {

      // side moving
      int theSide = chessBoard.getTurn();

      // array of moves
      int[] moveArr = new int[128];

      // alpha and beta vars
      long alpha, beta;

      // temporary varible to store the value returned from a search
      long value;

      // flag indicating if we have an exact score and thus can now scout all remaining moves
      boolean isExact = false;

      //flag indicating if the search has at least found a move which it can return if the search is terminated
      boolean hasValue = false;

      //the best move found in the search
      int bestMove = 0;

      //the number of moves to be searched
      int numberOfMoves;

      //the score of the best move searched
      long bestValue = 0;

      //time control initialization code
      levelOneValue = 0;
      lastRootIterationValue = 0;
      failHighNewBestMoveFlag = false;
      lastIterationTime = 0L;
      maximumRootGetAScoreTime = maxTime -  (int)((double)maxTime * 0.50);
      maximumTime = maxTime;
      startTime = System.currentTimeMillis();
      endTime = startTime + time;
      stop = false;

      /** assumes program is searching 15000 moves per second, if not will take too long */
      nextTimeCheck = Math.min(1000, time * 15);

      // ancient node value betweeen 0 and 7;
      ancient = (chessBoard.getCount() / 2) % 8;

      infiniteTimeControl = inf;

      /** here we prepare the history arrays for the next search */
      if ((chessBoard.getCount() / 2 % 8) == 7) {
         for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 64; j++) {
               Hist[i][j] = 0;
               Hist2[i][j] = 0;
            }
         }
      } else {
         for (int i = 0; i < 64; i++) {
            for (int j = 0; j < 64; j++) {
               Hist[i][j] /= 10;
               Hist2[i][j] /= 10;
            }
         }
      }

      /** here the killer moves are cleared out before the next search */
      for (int j = 0; j < 25; j++) {
         killerMoves[j][0] = 0;
         killerMoves[j][1] = 0;
      }

      theSide = chessBoard.getTurn();
      
      nodes = 0;
      hashnodes = 0;

      /** collect the root moves */

     if (!inCheck(theSide)) {
        numberOfMoves = GetAllMoves(theSide, moveArr);
      } else {
         numberOfMoves = getCheckEscapes(theSide, moveArr);
      }

      thisDepth = 0;

      int[] compareArray = new int[128];           //array used to store values of moves for sorting the move list


      //remove illegal moves
      //mark moves which result in a draw
      //perform an initial sort of the moves list based on their Q sort scores
      for (int i = numberOfMoves - 1; i >= 0; i--) {
         int tempMove = moveArr[i];
         int reps = chessBoard.MakeMove(tempMove, true);

         if (inCheck(theSide)) {                    //eliminate illegal moves from later ply
            for (int j = i; j <= numberOfMoves - 1; j++) {
               moveArr[j] = moveArr[j + 1];
               compareArray[j] = compareArray[j + 1];
            }
            numberOfMoves--;
            chessBoard.UnMake(tempMove, true);
            continue;
         }

         if (reps == 3) {
            System.out.println("Draw move is " + HistoryWriter.getUCIMove((tempMove >> 6) & 63, tempMove & 63, (tempMove >> 12) & 15));
            compareArray[i] = 1;
         } else if (chessBoard.getDraw() == 100) {
            compareArray[i] = 1;
         } else if (isStaleMate(-theSide)) {
            System.out.println("stalemate move is " + HistoryWriter.getUCIMove((tempMove >> 6) & 63, tempMove & 63, (tempMove >> 12) & 15));
            compareArray[i] = 1;
         } else {
            compareArray[i] = (int) -Quies(-theSide, 1, -BETA_START, -ALPHA_START);
            compareArray[i] &= ~1;        //mask off the lowest bit
            thisDepth--;
         }
         chessBoard.UnMake(tempMove, true);
      }
      sortMoves(0, numberOfMoves, moveArr, compareArray);

      //iteratively deepened search starting at depth 2
      for (int depth = 2; depth <= searchDepth; depth++) {
         if (!timeLeft()) {
            stop = true;
         }

         if (stop && hasValue) {
            break;
         }

         // insert pv from last iteration into the transposition table
         if (depth > 2) {
            for (int i = 0; i < 64; i++) {
               int insertDepth = 0;
               int mv = PV[0][i];
               if (mv == TT_PV) {

                  for (int j = i - 1; j >= 0; j--) {
                     //System.out.println("unmaking move to is "+MoveFunctions.getTo(PV[0][j])+" from is "+MoveFunctions.getFrom(PV[0][j]));
                     chessBoard.UnMake(PV[0][j], false);
                  }
                  break;
               } else if (mv == STANDPAT_PV) {
                  for(int j = i - 1; j >= 0; j--) {
                     //System.out.println("unmaking move to is "+MoveFunctions.getTo(PV[0][j])+" from is "+MoveFunctions.getFrom(PV[0][j]));
                     chessBoard.UnMake(PV[0][j], false);
                  }
                  break;
               } else if (mv == ROOTDRAW_PV) {
                  for (int j = i - 1; j >= 0; j--) {
                     //System.out.println("unmaking move to is "+MoveFunctions.getTo(PV[0][j])+" from is "+MoveFunctions.getFrom(PV[0][j]));
                     chessBoard.UnMake(PV[0][j], false);
                  }
                  break;
               } else if (mv == SEARCHDRAW_PV) {
                  for (int j = i - 1; j >= 0; j--) {
                     //System.out.println("unmaking move to is "+MoveFunctions.getTo(PV[0][j])+" from is "+MoveFunctions.getFrom(PV[0][j]));
                     chessBoard.UnMake(PV[0][j], false);
                  }
                  break;
               } else if (mv == MATE_PV) {
                  for (int j = i - 1; j >= 0; j--) {
                     //System.out.println("unmaking move to is "+MoveFunctions.getTo(PV[0][j])+" from is "+MoveFunctions.getFrom(PV[0][j]));
                     chessBoard.UnMake(PV[0][j], false);
                  }
                  break;
               } else {
                  //System.out.println("inserting move to is "+MoveFunctions.getTo(mv)+" from is "+MoveFunctions.getFrom(mv));
                  int key = (int) (chessBoard.hashValue % Global.HASHSIZE);
                  HashTable.addHash(key, chessBoard.hashValue, mv, -20000, insertDepth++, HASH_BETA, 0, ancient);
                  chessBoard.MakeMove(mv, false);
               }
            }
         }
         value = VALUE_START;
         alpha = ALPHA_START;
         beta = BETA_START;
         levelOneValue = -beta;
         lastRootIterationValue = bestValue;
         long tempBestMove = Integer.MIN_VALUE;
         isExact = false;
         thisDepth = 0;

         long currentTime = System.currentTimeMillis();
         long timeUsed = currentTime - startTime;
         lastIterationTime = timeUsed - lastIterationTime;

         for (int i = numberOfMoves - 1; i >= 0; i--) {
            int tempMove = moveArr[i];
            if ((compareArray[i] & 1) != 0) {
               System.out.println("Draw move 2 is " + HistoryWriter.getUCIMove((tempMove >> 6) & 63, tempMove & 63, (tempMove >> 12) & 15));
               value = 0;
               levelOneValue = 0;
            } else if (!isExact) {
               chessBoard.MakeMove(tempMove, true);
               value = -Max(-theSide, depth - 1, -beta, -alpha, false, inCheck(-theSide), false, false);
               chessBoard.UnMake(tempMove, true);
               thisDepth--;
               levelOneValue = value;
               //check for a first move which goes LOW
               if(value < bestValue - 30 && !stop) {
                  lastRootIterationValue = value;
                  AdjustTime();
                
               }
            } else {
               chessBoard.MakeMove(tempMove, true);
               value = -Max(-theSide, depth - 1, -alpha - 1, -alpha, false, inCheck(-theSide), false, false);
               thisDepth--;

               if (value > alpha && !stop) {
                  //check for a move which goes HIGH
                  failHighNewBestMoveFlag = true;
                  value = -Max(-theSide, depth - 1, -beta, -alpha, false, inCheck(-theSide), false, false);
                  thisDepth--;
               }
               chessBoard.UnMake(tempMove, true);
            }

            if ((((value > tempBestMove) && !stop) || !hasValue)) {      //have a new best move..update alpha..etc
               failHighNewBestMoveFlag = false;
               alpha = value;
               isExact = true;
               hasValue = true;
               bestValue = value;
               tempBestMove = value;
               bestMove = tempMove;
               if (value == 0 && (compareArray[i] & 1) != 0) //if this is a root draw or stalemate, set the lsb flag
               {
                  PV[0][0] = tempMove;                   //update trianglular PV array for a root draw move
                  PV[0][1] = ROOTDRAW_PV;
                  compareArray[i] = 10000 - i << 1 | 1;
               } else {
                  compareArray[i] = 10000 - i << 1;
               }
            } else {                                                 //move is no good
               failHighNewBestMoveFlag = false;
               if (value == 0 && (compareArray[i] & 1) != 0)
                  compareArray[i] = 2000 + i << 1 | 1;
               else
                  compareArray[i] = 2000 + i << 1;                  //if this is a root draw or stalemate, set the lsb flag
            }
         }

         //prepare the principal variation for display
         String pv = HistoryWriter.getUCIMove((bestMove >> 6) & 63, bestMove & 63, (bestMove >> 12) & 15);
         for (int i = 1; i < 64; i++) {
            pv = pv.concat(" ");
            int mv = PV[0][i];
            if (mv == TT_PV) {
               pv = pv.concat("tt");
               break;
            } else if (mv == STANDPAT_PV) {
               pv = pv.concat("standPat");
               break;
            } else if (mv == ROOTDRAW_PV) {
               pv = pv.concat("rootDraw");
               break;
            } else if (mv == SEARCHDRAW_PV) {
               pv = pv.concat("searchDraw");
               break;
            } else if (mv == MATE_PV) {
               pv = pv.concat("mate");
               break;
            } else {
               pv = pv.concat(HistoryWriter.getUCIMove((PV[0][i] >> 6) & 63, PV[0][i] & 63, (PV[0][i] >> 12) & 15));
            }
         }

         //want to print the last completed ply
         
         if (stop && !isExact) {       //if search was stopped, the last completed ply was not actually complete
            depth--;
         }
         //print UCI move information
         if (bestValue > 10000L) {			//this is a winning mate score
            long mate = (20000L - bestValue) / 2;
            System.out.println("info depth " + depth + " score mate " + mate + " nodes " + nodes + " pv " + pv);
         } else if (bestValue < -10000) {  //losing mate score
            long mate = (-20000L - bestValue) / 2;
            System.out.println("info depth " + depth + " score mate " + mate + " nodes " + nodes + " pv " + pv);
         } else {
            System.out.println("info depth " + depth + " score cp " + bestValue + " nodes " + nodes + " pv " + pv);
         }
         sortMoves(0, numberOfMoves, moveArr, compareArray);

      }
      //System.out.println("info string hasnodes are "+hashnodes);
      //make the best move
      chessBoard.AddMove(bestMove);
      chessBoard.MakeMove(bestMove, false);
      chessBoard.AddRepetitionRoot();

      return HistoryWriter.getUCIMove(bestMove);

   }

   /**********************************************************************
   This method tests whether a side is in check
   parameters - int side ....the side possibly in check
    **********************************************************************/
   private boolean inCheck(int side) {

      if (side == 1) //black
      {
         return chessBoard.isBlackAttacked(Long.numberOfTrailingZeros(chessBoard.blackking));
      } else //white
      {
         return chessBoard.isWhiteAttacked(Long.numberOfTrailingZeros(chessBoard.whiteking));
      }
   }

   /************************************************************************
   This method will determine if the game is over due to a stalemate
   parameters int side - the side to move next
    ************************************************************************/
   private boolean isStaleMate(int side) {
      if (!inCheck(side)) {				//side to move must not be in check...otherwise could be checkmate
         int[] Moves = new int[128];
         int numberOfMoves = GetAllMoves(side, Moves);
         for (int i = 0; i < numberOfMoves; i++) {
            int temp = Moves[i];
            chessBoard.MakeMove(temp, false);
            if (!inCheck(side)) {
               chessBoard.UnMake(temp, false);
               return false;
            } else {
               chessBoard.UnMake(temp, false);
            }
         }
         return true;
      }
      return false;
   }

   /**
    * Method getMoves
    *
    * collects all of the non capture, non pawn promotion moves
    *
    * @param int side - the side to move
    * @param int[] Moves - the array to fill with moves
    * @param int start - index to start placing move into as capture moves may
    * already occupy first spaces in array
    *
    * @return int - the number of moves added
    *
    */
   public int getMoves(int side, int[] Moves, int start) {
      long pieces;
      long toSquares;
      long toBit;
      int to;
      int from;
      int index = start;
      if (side == -1) {													//white moving
         pieces = chessBoard.whitepawns;
         long moves = pieces << 8 & ~chessBoard.bitboard & ~Global.rankMasks[7];
         long doubleMoves = moves << 8 & Global.rankMasks[3] & ~chessBoard.bitboard;

         while (moves != 0) {
            toBit = moves & -moves;
            moves ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);
            from = to - 8;
            moveOrder[index] = Hist2[from][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, 5, -1, Global.ORDINARY_MOVE);
         }

         while (doubleMoves != 0) {
            toBit = doubleMoves & -doubleMoves;
            doubleMoves ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);
            from = to - 16;
            moveOrder[index] = Hist2[from][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, 5, -1, Global.DOUBLE_PAWN_WHITE);
         }

         from = Long.numberOfTrailingZeros(chessBoard.whiteking);
         toSquares = Helper.getKingPosition(from);  //chessBoard.getAttackBoard(from);
         if (chessBoard.wCastle > Global.CASTLED) {
            long castle = chessBoard.getWKingCastle(from);
            if (chessBoard.wCastle == Global.LONG_CASTLE) {
               castle &= Global.set_Mask[2];
            } else if (chessBoard.wCastle == Global.SHORT_CASTLE) {
               castle &= Global.set_Mask[6];
            }
            toSquares |= castle;
         }
         toSquares &= ~chessBoard.bitboard;
         while (toSquares != 0) {
            toBit = toSquares & -toSquares;
            toSquares ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);

            int type = Global.ORDINARY_MOVE;
            if (from == 4 && to == 2) {
               type = Global.LONG_CASTLE;
            } else if (from == 4 && to == 6) {
               type = Global.SHORT_CASTLE;
            }
            moveOrder[index] = Hist2[from][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, 4, -1, type);
         }

         pieces = chessBoard.whitepieces & ~(chessBoard.whiteking | chessBoard.whitepawns);
         while (pieces != 0) {
            from = Long.numberOfTrailingZeros(pieces);
            pieces ^= Global.set_Mask[from];
            toSquares = chessBoard.getAttackBoard(from);
            toSquares &= ~chessBoard.bitboard;
            while (toSquares != 0) {
               toBit = toSquares & -toSquares;
               toSquares ^= toBit;
               to = Long.numberOfTrailingZeros(toBit);

               moveOrder[index] = Hist2[from][to];
               Moves[index++] = MoveFunctions.makeMove(to, from, Board.piece_in_square[from], -1, Global.ORDINARY_MOVE);
            }
         }
      } else {
         pieces = chessBoard.blackpawns;
         long moves = pieces >> 8 & ~chessBoard.bitboard & ~Global.rankMasks[0];
         long doubleMoves = moves >> 8 & Global.rankMasks[4] & ~chessBoard.bitboard;

         while (moves != 0) {
            toBit = moves & -moves;
            moves ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);
            from = to + 8;
            moveOrder[index] = Hist[from][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, 11, -1, Global.ORDINARY_MOVE);
         }

         while (doubleMoves != 0) {
            toBit = doubleMoves & -doubleMoves;
            doubleMoves ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);
            from = to + 16;
            moveOrder[index] = Hist[from][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, 11, -1, Global.DOUBLE_PAWN_BLACK);
         }
         pieces = chessBoard.blackpieces & ~(chessBoard.blackking | chessBoard.blackpawns);
         while (pieces != 0) {
            from = Long.numberOfTrailingZeros(pieces);
            pieces ^= Global.set_Mask[from];
            toSquares = chessBoard.getAttackBoard(from);
            toSquares &= ~chessBoard.bitboard;
            while (toSquares != 0) {
               toBit = toSquares & -toSquares;
               toSquares ^= toBit;
               to = Long.numberOfTrailingZeros(toBit);

               moveOrder[index] = Hist[from][to];
               Moves[index++] = MoveFunctions.makeMove(to, from, Board.piece_in_square[from], -1, Global.ORDINARY_MOVE);
            }
         }

         from = Long.numberOfTrailingZeros(chessBoard.blackking);
         toSquares = Helper.getKingPosition(from);//chessBoard.getAttackBoard(from);

         if (chessBoard.bCastle > Global.CASTLED) {
            long castle = chessBoard.getBKingCastle(from);
            if (chessBoard.bCastle == Global.LONG_CASTLE) {
               castle &= Global.set_Mask[58];
            } else if (chessBoard.bCastle == Global.SHORT_CASTLE) {
               castle &= Global.set_Mask[62];
            }
            toSquares |= castle;
         }
         toSquares &= ~chessBoard.bitboard;
         while (toSquares != 0) {
           toBit = toSquares & -toSquares;
           toSquares ^= toBit;
           to = Long.numberOfTrailingZeros(toBit);

            int type = Global.ORDINARY_MOVE;
            if (from == 60 && to == 58) {
               type = Global.LONG_CASTLE;
            } else if (from == 60 && to == 62) {
               type = Global.SHORT_CASTLE;
            }
            moveOrder[index] = Hist[from][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, 10, -1, type);
         }
      }
      sortMoves(start, index, Moves);
      return index;
   }

   /**
    * Method sortMoves
    *
    * Insertion sort
    *
    *
    * @param int start - position to start at in the array
    * @param int noMoves - number of positions to sort
    * @param int[] Moves - array to sort
    *
    *
    */

   private static final void sortMoves(int start, int noMoves, int[] Moves) {
     
      for (int i = start ; i < noMoves; i++) {
         int element = Moves[i];
         int score = moveOrder[i];
         int j = i;
         
         while(j > start && moveOrder[j-1] > score) {
          
            Moves[j] = Moves[j-1];
            moveOrder[j] = moveOrder[j-1];
            j--; 
         }
         Moves[j] = element;
         moveOrder[j] = score;
       }
   }

   /**
    * Method sortMoves
    *
    * Bubble sort 
    *
    *
    * @param int start - position to start at in the array
    * @param int noMoves - number of positions to sort
    * @param int[] Moves - array to sort
    *
    *
    */
    private static final void sortCaps(int start, int noMoves, int[] Moves) {
      boolean done = false;
      for (int i = start; i < noMoves; i++) {
         if (done) {
            break;
         }
         done = true;
         for (int j = noMoves - 1; j > i; j--) {
            if (moveOrder[j] < moveOrder[j - 1]) {		//swap moves
               int temp = Moves[j];
               Moves[j] = Moves[j - 1];
               Moves[j - 1] = temp;
               temp = moveOrder[j];
               moveOrder[j] = moveOrder[j - 1];
               moveOrder[j - 1] = temp;
               done = false;
            }
         }
      }
   }
   /**
    * Method sortMoves
    *
    * To-do
    * -bubble sorting just a few captures is ok, but this is being used to sort
    * the regular moves - NEED TO WRITE A FASTER SORT ROUTINE FOR NON CAPS
    *
    * bubble sorts the moves
    *
    * @param int start - position to start at in the array
    * @param int noMoves - number of positions to sort
    * @param int[] Moves - array to sort
    *
    *
    */
   private static final void sortMoves(int start, int noMoves, int[] Moves, int[] compareArray) {
      boolean done = false;
      for (int i = start; i < noMoves; i++) {
         if (done) {
            break;
         }
         done = true;
         for (int j = noMoves - 1; j > i; j--) {
            if (compareArray[j] < compareArray[j - 1]) {		//swap moves
               int temp = Moves[j];
               Moves[j] = Moves[j - 1];
               Moves[j - 1] = temp;
               temp = compareArray[j];
               compareArray[j] = compareArray[j - 1];
               compareArray[j - 1] = temp;
               done = false;
            }
         }
      }
   }

   /**
    * Method getCaptures
    *
    * collects all capture moves and pawn promotion moves
    *
    * @param int side - the side to move
    * @param int[] Captures - the array to fill with moves
    *
    *
    * @return int - the number of moves added
    *
    */
   public int getCaptures(int side, int[] Captures) {
      long pieces;
      long toSquares;
      long fromBit;
      long toBit;
      int passant;
      long enemies;
      int to;
      int from;
      int index = 0;
      int cP;
      int type;
      if (side == -1) {			//white moving
         passant = chessBoard.getPassantB();
         long pMask = Global.set_Mask[passant];
         enemies = chessBoard.blackpieces;
         pieces = chessBoard.whitepawns;
         long lAttack = (pieces << 7) & (enemies | pMask) & ~Global.fileMasks[7];
         long rAttack = (pieces << 9) & (enemies | pMask) & ~Global.fileMasks[0];
         long promo = pieces & Global.rankMasks[6];
         if (promo != 0) {
            promo <<= 8;
            promo &= ~chessBoard.bitboard;
         }
         while (lAttack != 0) {
            toBit = lAttack & -lAttack;
            lAttack ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);
            from = to - 7;
            type = Global.ORDINARY_MOVE;
            cP = Board.piece_in_square[to];
            if (to == passant) {
               moveOrder[index] = Global.values[5] + 4;
               type = Global.EN_PASSANT_CAP;
            } else if (to / 8 == 7) {
               if (PERFT_ENABLED) {
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 5, cP, Global.PROMO_B);
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 5, cP, Global.PROMO_N);
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 5, cP, Global.PROMO_R);
               }
               moveOrder[index] = Global.values[cP] + 700;
               type = Global.PROMO_Q;

            } else {
               moveOrder[index] = Global.values[cP] + 4;
            }
            Captures[index++] = MoveFunctions.makeMove(to, from, 5, cP, type);
         }

         while (rAttack != 0) {
            toBit = rAttack & -rAttack;
            rAttack ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);
            from = to - 9;
            type = Global.ORDINARY_MOVE;
            cP = Board.piece_in_square[to];
            if (to == passant) {
               moveOrder[index] = Global.values[5] + 4;
               type = Global.EN_PASSANT_CAP;
            } else if (to / 8 == 7) {
               if (PERFT_ENABLED) {
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 5, cP, Global.PROMO_B);
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 5, cP, Global.PROMO_N);
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 5, cP, Global.PROMO_R);
               }
               moveOrder[index] = Global.values[cP] + 700;
               type = Global.PROMO_Q;
            } else {
               moveOrder[index] = Global.values[cP] + 4;
            }
            Captures[index++] = MoveFunctions.makeMove(to, from, 5, cP, type);
         }
         while (promo != 0) {
            toBit = promo & -promo;
            promo ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);
            from = to - 8;
            if (PERFT_ENABLED) {
               moveOrder[index] = 0;
               Captures[index++] = MoveFunctions.makeMove(to, from, 5, -1, Global.PROMO_B);
               moveOrder[index] = 0;
               Captures[index++] = MoveFunctions.makeMove(to, from, 5, -1, Global.PROMO_N);
               moveOrder[index] = 0;
               Captures[index++] = MoveFunctions.makeMove(to, from, 5, -1, Global.PROMO_R);
            }
            moveOrder[index] = 700;
            Captures[index++] = MoveFunctions.makeMove(to, from, 5, -1, Global.PROMO_Q);
         }

         pieces = chessBoard.whitepieces & ~chessBoard.whitepawns;
         while (pieces != 0) {
            fromBit = pieces & -pieces;
            pieces ^= fromBit;
            from = Long.numberOfTrailingZeros(fromBit);
            int fromPiece = Board.piece_in_square[from];
            int fromVal = Global.invValues[fromPiece];
            toSquares = chessBoard.getAttackBoard(from);
            toSquares &= enemies;
            while (toSquares != 0) {
               toBit = toSquares & -toSquares;
               toSquares ^= toBit;
               to = Long.numberOfTrailingZeros(toBit);
               cP = Board.piece_in_square[to];
               moveOrder[index] = Global.values[cP] + fromVal;
               Captures[index++] = MoveFunctions.makeMove(to, from, fromPiece, cP, Global.ORDINARY_MOVE);
            }
         }
      } else {					//black moving
         passant = chessBoard.getPassantW();
         long pMask = Global.set_Mask[passant];
         enemies = chessBoard.whitepieces;
         pieces = chessBoard.blackpawns;
         long lAttack = pieces >> 9 & (enemies | pMask) & ~Global.fileMasks[7];
         long rAttack = pieces >> 7 & (enemies | pMask) & ~Global.fileMasks[0];
         long promo = pieces & Global.rankMasks[1];
         if (promo != 0) {
            promo >>= 8;
            promo &= ~chessBoard.bitboard;
         }
         while (lAttack != 0) {
            toBit = lAttack & -lAttack;
            lAttack ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);
            from = to + 9;
            cP = Board.piece_in_square[to];
            type = Global.ORDINARY_MOVE;
            if (to == passant) {
               moveOrder[index] = Global.values[5] + 4;
               type = Global.EN_PASSANT_CAP;
            } else if (to / 8 == 0) {
               if (PERFT_ENABLED) {
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 11, cP, Global.PROMO_B);
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 11, cP, Global.PROMO_N);
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 11, cP, Global.PROMO_R);
               }
               moveOrder[index] = Global.values[cP] + 700;
               type = Global.PROMO_Q;
            } else {  
               moveOrder[index] = Global.values[cP] + 4;
            }
            Captures[index++] = MoveFunctions.makeMove(to, from, 11, cP, type);
         }
         while (rAttack != 0) {
            toBit = rAttack & -rAttack;
            rAttack ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);
            from = to + 7;
            cP = Board.piece_in_square[to];
            type = Global.ORDINARY_MOVE;
            if (to == passant) {
               type = Global.EN_PASSANT_CAP;
               moveOrder[index] = Global.values[5] + 4;
            } else if (to / 8 == 0) {
               if (PERFT_ENABLED) {
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 11, cP, Global.PROMO_B);
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 11, cP, Global.PROMO_N);
                  moveOrder[index] = 0;
                  Captures[index++] = MoveFunctions.makeMove(to, from, 11, cP, Global.PROMO_R);
               }
               moveOrder[index] = Global.values[cP] + 700;
               type = Global.PROMO_Q;
            } else {
               moveOrder[index] = Global.values[cP] + 4;
            }
            Captures[index++] = MoveFunctions.makeMove(to, from, 11, cP, type);
         }
         while (promo != 0) {
            toBit = promo & -promo;
            promo ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);
            from = to + 8;
            if (PERFT_ENABLED) {
               moveOrder[index] = 0;
               Captures[index++] = MoveFunctions.makeMove(to, from, 11, -1, Global.PROMO_B);
               moveOrder[index] = 0;
               Captures[index++] = MoveFunctions.makeMove(to, from, 11, -1, Global.PROMO_N);
               moveOrder[index] = 0;
               Captures[index++] = MoveFunctions.makeMove(to, from, 11, -1, Global.PROMO_R);
            }
            moveOrder[index] = 700;
            Captures[index++] = MoveFunctions.makeMove(to, from, 11, -1, Global.PROMO_Q);
         }

         pieces = chessBoard.blackpieces & ~chessBoard.blackpawns;
         while (pieces != 0) {
            fromBit = pieces & -pieces;
            pieces ^= fromBit;
            from = Long.numberOfTrailingZeros(fromBit);
            int fromPiece = Board.piece_in_square[from];
            int fromVal = Global.invValues[fromPiece];
            toSquares = chessBoard.getAttackBoard(from);
            toSquares &= enemies;
            while (toSquares != 0) {
               toBit = toSquares & -toSquares;
               toSquares ^= toBit;
               to = Long.numberOfTrailingZeros(toBit);
               cP = Board.piece_in_square[to];
               moveOrder[index] = Global.values[cP] + fromVal;
               Captures[index++] = MoveFunctions.makeMove(to, from, fromPiece, cP, Global.ORDINARY_MOVE);
            }
         }
      }
      sortCaps(0, index, Captures);
      return index;
   }

   /**
    * Method getCheckEscapes
    *
    * collects moves to escape from check
    *
    * @param int side - the side to move
    * @param int[] escapes - the array to fill with moves
    *
    *
    * @return int - the number of moves added
    *
    */
   private int getCheckEscapes(int side, int[] escapes) {
      int attacks = 0;
      long friends;
      long bishops;
      long queen;
      long rooks;
      long knights;
      long king;
      int kingPos;
      long fromBit;
      int attackFrom = -1;
      int index = 0;

      if (side == -1) {			//white moving
         friends = chessBoard.whitepieces;
         king = chessBoard.whiteking;
         bishops = chessBoard.blackbishops;
         queen = chessBoard.blackqueen;
         rooks = chessBoard.blackrooks;
         knights = chessBoard.blackknights;
      } else {					//black moving
         friends = chessBoard.blackpieces;
         king = chessBoard.blackking;
         bishops = chessBoard.whitebishops;
         queen = chessBoard.whitequeen;
         rooks = chessBoard.whiterooks;
         knights = chessBoard.whiteknights;
      }
      kingPos = Long.numberOfTrailingZeros(king);
      long toSquares = chessBoard.getAttackBoard(kingPos) & ~friends;
      long temp = chessBoard.getMagicBishopMoves(kingPos);
      temp &= (bishops | queen);
      while (temp != 0) {
         fromBit = temp & -temp;
         attackFrom = Long.numberOfTrailingZeros(fromBit);
         if (Global.Diag1Groups[kingPos] == Global.Diag1Groups[attackFrom]) {
            toSquares &= ((~Global.diag1Masks[Global.Diag1Groups[kingPos]]) ^ Global.set_Mask[attackFrom]);
         } else {
            toSquares &= ((~Global.diag2Masks[Global.Diag2Groups[kingPos]]) ^ Global.set_Mask[attackFrom]);
         }
         temp ^= fromBit;
         attacks++;
      }


      temp = chessBoard.getMagicRookMoves(kingPos);
      temp &= (rooks | queen);
      while (temp != 0) {
         fromBit = temp & -temp;
         attackFrom = Long.numberOfTrailingZeros(fromBit);
         if ((attackFrom / 8) == (kingPos / 8)) {
            toSquares &= ((~Global.rankMasks[kingPos / 8]) ^ Global.set_Mask[attackFrom]);
         } else {
            toSquares &= ((~Global.fileMasks[kingPos % 8]) ^ Global.set_Mask[attackFrom]);
         }
         temp ^= fromBit;
         attacks++;
      }

      while (toSquares != 0) {
         long bit = toSquares & -toSquares;
         toSquares ^= bit;
         int to = Long.numberOfTrailingZeros(bit);
         if (side == -1) {		//white moving
            if (chessBoard.isWhiteAttacked(to)) {
               continue;
            }
         } else {
            if (chessBoard.isBlackAttacked(to)) {
               continue;
            }
         }
         int cP = Board.piece_in_square[to];
         int value = 10;
         if (cP != -1) {
            value = SEE.getSEE(side, to, kingPos, -1);
            if (value >= 0) {
               value += 10000;
            } else {
               value = 0;
            }
         } else {
            if (side == -1) {
               value = Hist2[kingPos][to];
            } else {
               value = Hist[kingPos][to];
            }
         }
         moveOrder[index] = value;
         escapes[index++] = MoveFunctions.makeMove(to, kingPos, Board.piece_in_square[kingPos], cP, Global.ORDINARY_MOVE);    //generate king escape moves
      }
      if (attacks == 2) {
         sortCaps(0, index, escapes);
         return index;
      }
      temp = chessBoard.getKnightMoves(kingPos);
      temp &= knights;
      while (temp != 0) {
         fromBit = temp & -temp;
         attackFrom = Long.numberOfTrailingZeros(fromBit);
         temp ^= fromBit;
         attacks++;
      }

      if (side == -1) {			//white moving
         temp = chessBoard.getWPawnAttack(kingPos);
         temp &= chessBoard.blackpawns;
         while (temp != 0) {
            fromBit = temp & -temp;
            attackFrom = Long.numberOfTrailingZeros(fromBit);
            temp ^= fromBit;
            attacks++;
         }
      } else {					//black moving
         temp = chessBoard.getBPawnAttack(kingPos);
         temp &= chessBoard.whitepawns;
         while (temp != 0) {
            fromBit = temp & -temp;
            attackFrom = Long.numberOfTrailingZeros(fromBit);
            temp ^= fromBit;
            attacks++;
         }
      }
      if (attacks == 2) {
         sortCaps(0, index, escapes);
         return index;
      }
      temp = chessBoard.getAttack2(attackFrom);
      temp &= (friends & ~king);
      int type;
      int value;
      int cP = Board.piece_in_square[attackFrom];

      while (temp != 0) {
         type = Global.ORDINARY_MOVE;
         fromBit = temp & -temp;
         temp ^= fromBit;
         int from = Long.numberOfTrailingZeros(fromBit);
         if (SEE.isPinned(side, attackFrom, from)) {
            continue;
         }
         value = SEE.getSEE(side, attackFrom, from, -1);
         if (value >= 0) {
            value += 10000;
         } else {
            value = 0;
         }
         if (Board.piece_in_square[from] % 6 == 5) {
            if (attackFrom / 8 == 0 || attackFrom / 8 == 7) {
               if (PERFT_ENABLED) {
                  moveOrder[index] = value;
                  escapes[index++] = MoveFunctions.makeMove(attackFrom, from, Board.piece_in_square[from], cP, Global.PROMO_N);
                  moveOrder[index] = value;
                  escapes[index++] = MoveFunctions.makeMove(attackFrom, from, Board.piece_in_square[from], cP, Global.PROMO_R);
                  moveOrder[index] = value;
                  escapes[index++] = MoveFunctions.makeMove(attackFrom, from, Board.piece_in_square[from], cP, Global.PROMO_B);
               }
               type = Global.PROMO_Q;
            }
         }
         moveOrder[index] = value;
         escapes[index++] = MoveFunctions.makeMove(attackFrom, from, Board.piece_in_square[from], cP, type);            //these are attacks on the king's attacker
      }


      if (Board.piece_in_square[attackFrom] == 5) {
         if ((attackFrom - 8) == chessBoard.getPassantW()) {
            temp = chessBoard.getWPawnAttack(attackFrom - 8) & chessBoard.blackpawns;
            while (temp != 0) {
               fromBit = temp & -temp;
               temp ^= fromBit;
               int from = Long.numberOfTrailingZeros(fromBit);
               if (SEE.isPinned(side, attackFrom - 8, from)) {
                  continue;
               }
               value = SEE.getSEE(side, attackFrom - 8, from, chessBoard.getPassantW());
               if (value >= 0) {
                  value += 10000;
               } else {
                  value = 0;
               }
               moveOrder[index] = value;
               escapes[index++] = MoveFunctions.makeMove(attackFrom - 8, from, Board.piece_in_square[from], -1, Global.EN_PASSANT_CAP);
            }
         }
      }
      //if attacking piece is a black pawn ( white moving )
      if (Board.piece_in_square[attackFrom] == 11) {
         if ((attackFrom + 8) == chessBoard.getPassantB()) {
            temp = chessBoard.getBPawnAttack(attackFrom + 8) & chessBoard.whitepawns;
            while (temp != 0) {
               fromBit = temp & -temp;
               temp ^= fromBit;
               int from = Long.numberOfTrailingZeros(fromBit);
               if (SEE.isPinned(side, attackFrom + 8, from)) {
                  continue;
               }
               value = SEE.getSEE(side, attackFrom + 8, from, chessBoard.getPassantB());
               if (value >= 0) {
                  value += 10000;
               } else {
                  value = 0;
               }
               moveOrder[index] = value;
               escapes[index++] = MoveFunctions.makeMove(attackFrom + 8, from, Board.piece_in_square[from], -1, Global.EN_PASSANT_CAP);
            }
         }
      }
      //if one attacker is a slide piece, generate moves to block the sliding attack
      if (!Global.slides[Board.piece_in_square[attackFrom]]) {
         return index;
      }

      long squares = 0;                                                           //intermediate squares between attacker and king
      //need to get bitset of squares between attacker and king
      int difference = kingPos - attackFrom;
      int rankDiff = kingPos / 8 - attackFrom / 8;
      if (difference < 0) {
         rankDiff *= -1;
      }
      int relation = -1;
      if (rankDiff != 0) {
         relation = difference / rankDiff;
      } else {
         if (kingPos < attackFrom) {
            relation = -99;
         } else {
            relation = 99;
         }
      }
      switch (relation) {
         case (-9):
            squares = Global.plus9[kingPos] ^ Global.plus9[attackFrom - 9];// ^ Global.set_Mask[attackFrom];
            break;
         case (9):
            squares = Global.plus9[attackFrom] ^ Global.plus9[kingPos - 9];// ^ Global.set_Mask[kingPos];
            break;
         case (-7):
            squares = Global.plus7[kingPos] ^ Global.plus7[attackFrom - 7];// ^ Global.set_Mask[attackFrom];
            break;
         case (7):
            squares = Global.plus7[attackFrom] ^ Global.plus7[kingPos - 7];// ^ Global.set_Mask[kingPos];
            break;
         case (-8):
            squares = Global.plus8[kingPos] ^ Global.plus8[attackFrom - 8];// ^ Global.set_Mask[attackFrom];
            break;
         case (8):
            squares = Global.plus8[attackFrom] ^ Global.plus8[kingPos - 8];//] ^ Global.set_Mask[kingPos];
            break;
         case (-99):
            squares = Global.plus1[kingPos] ^ Global.plus1[attackFrom - 1];// ^ Global.set_Mask[attackFrom];
            break;
         case (99):
            squares = Global.plus1[attackFrom] ^ Global.plus1[kingPos - 1];// ^ Global.set_Mask[kingPos];
            break;
      }

      //now generate all moves to the squares bit set to block the sliding check
      while (squares != 0) {
         long toBit = squares & -squares;
         squares ^= toBit;
         int to = Long.numberOfTrailingZeros(toBit);
         long attackers = chessBoard.getMovesTo(to);
         attackers &= (friends & ~king);
         while (attackers != 0) {
            type = Global.ORDINARY_MOVE;
            fromBit = attackers & -attackers;
            attackers ^= fromBit;
            int from = Long.numberOfTrailingZeros(fromBit);
            if (SEE.isPinned(side, to, from)) {
               continue;
            }
            if (side == -1) {
               value = Hist2[from][to];
            } else {
               value = Hist[from][to];
            }
            
            if (Board.piece_in_square[from] == 5) {
               if (to > 55) {
                  if (PERFT_ENABLED) {
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, Board.piece_in_square[from], -1, Global.PROMO_N);
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, Board.piece_in_square[from], -1, Global.PROMO_R);
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, Board.piece_in_square[from], -1, Global.PROMO_B);
                  }
                  type = Global.PROMO_Q;
               } else if((to - from) == 16)   {
                  type = Global.DOUBLE_PAWN_WHITE;             //handle case where a double pawn move blocks the attack
               }
            } else if (Board.piece_in_square[from] == 11) {
               if (to < 8) {
                  if (PERFT_ENABLED) {
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, Board.piece_in_square[from], -1, Global.PROMO_N);
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, Board.piece_in_square[from], -1, Global.PROMO_R);
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, Board.piece_in_square[from], -1, Global.PROMO_B);
                  }
                  type = Global.PROMO_Q;
               } else if((from - to) == 16)   {
                  type = Global.DOUBLE_PAWN_BLACK;             //handle case where a double pawn move blocks the attack
               }
            }
            moveOrder[index] = value;
            escapes[index++] = MoveFunctions.makeMove(to, from, Board.piece_in_square[from], -1, type);
         }
      }
      sortMoves(0, index, escapes);
      return index;
   }


   /**
    * Method verifyHash
    *
    * ensures that the hash move is legal
    * Since I use a 64 bit hash, it should be legal, but if it is not
    * due to a hash collision, the program could crash
    *
    * @param int side - the side to move
    * @param int move - the hash move to be verified
    *
    * @return boolean - was the hash move legal
    *
    */
   /*private boolean verifyHash(int side, int move) {
      
      if (move == 0) {
         return false;
     }

      int to = MoveFunctions.getTo(move);
      int from = MoveFunctions.getFrom(move);
      int piece = MoveFunctions.getPiece(move);

      if (side == -1 && piece > 5) { //white moving
         System.out.println("info string bad hash move1 "+move+" hash is "+chessBoard.hashValue);
         System.out.println("info string piece is "+piece);
         System.out.println("info string to is "+to);
         System.out.println("info string from is "+from);
         System.out.println("info string type is "+MoveFunctions.moveType(move));

         return false;

      } else if (side == 1 && piece < 6) {
         System.out.println("info string bad hash move2 "+move+" hash is "+chessBoard.hashValue);
         return false;
      }
      if (Board.piece_in_square[from] != piece) {
         System.out.println("info string bad hash move3 "+move);
         return false;
      }

      long temp;
      if (piece == 5) //wPawn
      {
         temp = chessBoard.getWPawnMoves(from);//, chessBoard.getPassantB());
      } else if (piece == 11) {
         temp = chessBoard.getBPawnMoves(from);
      } else {
         temp = chessBoard.getAttackBoard(from);
      }
      if (piece < 6) {
         temp &= ~chessBoard.whitepieces;
      } else {
         temp &= ~chessBoard.blackpieces;
      }

      if ((temp & Global.set_Mask[to]) != 0) {
         return true;
      }

      System.out.println("info string bad hash move4 piece is " + piece);
      //System.out.println("passantW is " + chessBoard.getPassantW());
      //temp = chessBoard.getBPawnMoves(from, chessBoard.getPassantW());
      return false;
   }*/

   /**
    * Method verifyKiller
    *
    * ensures that the killer move is legal
    *
    * @param int side - the side to move
    * @param int move - the hash move to be verified
    *
    * @return boolean - was the hash move legal
    *
    */
   private boolean verifyKiller(int side, int move) {

      if (move == 0) {  
         return false;
      }

      int to = MoveFunctions.getTo(move);
      int from = MoveFunctions.getFrom(move);
      int piece = MoveFunctions.getPiece(move);

      if (side == -1 && piece > 5) //white moving
      {
         return false;
      } else if (side == 1 && piece < 6) {
         return false;
      }

      if (Board.piece_in_square[from] != piece) {
         return false;
      }

      long temp;
      if (piece == 5) //wPawn
      { 
         temp = chessBoard.getWPawnMoves(from);//, chessBoard.getPassantB());
      } else if (piece == 11) {
         temp = chessBoard.getBPawnMoves(from);
      } else {
         temp = chessBoard.getAttackBoard(from);
      }
      temp &= ~chessBoard.bitboard;
      if ((temp & Global.set_Mask[to]) != 0) {
         return true;
      }
      return false;
   }

   /**
    * Method SavePV
    *
    * saves the PV in a situation where the pv line stops
    *
    * @param type - the type of pv ending
    *
    */
   private void SavePV(int type) {
      PV[thisDepth-1][thisDepth-1] = Board.getInstance().GetMoveAtDepth(thisDepth-1);
      PV[thisDepth-1][thisDepth] = type;
      lengthPV[thisDepth-1] = thisDepth;
   }

   /**
    * Method Max
    *
    * this is the negamax search function for non root nodes
    *
    * @param int side - the side to move
    * @param int depth - the depth remaining in the search
    * @param int alpha - alpha value
    * @param int beta - beta value
    * @param boolean nMove - flag to indicate whether or not a null move can be used
    * @param boolean isInCheck - flag indicates whether the side to move is in check
    * @param boolean was extended - indicates that previous move was extended
    * @param boolean iid - indicates if this is part of internal iterative deepening
    *
    * @return int - the score from searching the position
    *
    */
   private long Max(int side, int depth, long alpha, long beta, boolean nMove, boolean isInCheck, boolean wasExtended, boolean iid) {

      thisDepth++;


      nodes++;

      /** time management code */
      if (++nodes >= nextTimeCheck) {
         if (!timeLeft()) {
            stop = true;
         }
      }

      if (stop) {
         return 0;
      }
  
      int key = (int) (chessBoard.hashValue % Global.HASHSIZE);

      /** score that a move must exceed to be searched during futility pruning */
      int scoreThreshold = chessBoard.GetMaterialScore(side);

      /** razoring code */
      boolean razored = false;
      if (!wasExtended && !isInCheck && depth == 3) {
         if ((scoreThreshold + 900) <= alpha && chessBoard.getNumberOfPieces(-side) > 3) {
            razored = true;
            depth--;
         }
      }

      /** flag indicates it is worthless to try a null move search here */
      int nullFail = 0;

      int hashCount = 0;
      int[] hashArr = new int[8];
      int theMove = 0;


      /** hash table code - we are trying to cut the search based on what is stored */
      /** index represents the index of the move in the search and the index of a move in the hash table
       * this is an optimization to use 1 variable instead of 2
       */
      int index = HashTable.hasHash(key, chessBoard.hashValue);

      if (index != 1) {
         HashTable.setNew(key, index, ancient);

         switch (HashTable.getType(key, index)) {
            case (0):
               if (!iid && HashTable.getDepth(key, index) >= depth) {
                  long hVal = HashTable.getValue(key, index);
                  if (hVal <= alpha) {
                     nodes++;
                     return hVal;
                  }
               }
               break;
            case (1):
               if (!iid && HashTable.getDepth(key, index) >= depth) {
                  long hVal = HashTable.getValue(key, index);
                  if (hVal > alpha && hVal < beta) {
                     SavePV(TT_PV);
                     nodes++;
                     hashnodes++;
                     return HashTable.getValue(key, index);
                  }
               }
               break;
            case (2):
               if (!iid && HashTable.getDepth(key, index) >= depth) {
                  long hVal = HashTable.getValue(key, index);
                  if (hVal >= beta) {
                     nodes++;
                     return hVal;
                  }
               }
               break;
            case (4):
               long hVal = HashTable.getValue(key, index);
               if (hVal == -20000);
               hVal += thisDepth;
               if (hVal > alpha && hVal < beta) {
                  SavePV(TT_PV);
               }
               return hVal;
         }
         nullFail = HashTable.getNullFail(key, index);
      }

      /** futility and extended futility pruning condition testing */
      boolean bFutilityPrune = false;
      int futilityMargin = 0;

      scoreThreshold += 500;
      if (!wasExtended && !isInCheck && depth == 2 && scoreThreshold <= alpha) {
         bFutilityPrune = true;
         futilityMargin = 500;
      }
      scoreThreshold -= 300;
      if (!wasExtended && !isInCheck && depth == 1 && scoreThreshold <= alpha) {
         bFutilityPrune = true;
         futilityMargin = 200;
      }

      long value;


      /** null move pruning code
       * we don't want to null move in a few situations
       * -when in check
       * -when performed a null move last ply
       * -when the hash table tells us not to (nullFail)
       * -when we are in the endgame as zugzwangs are more likely
       */
       if (!razored && !bFutilityPrune && !isInCheck && !nMove && nullFail != 1 && (chessBoard.pawnsKings != chessBoard.bitboard) && chessBoard.totalValue > 1200 && chessBoard.getMinNumberOfPieces() > 1) {

          chessBoard.switchTurn();
         int reduce = 2;
         if (depth > 6 && chessBoard.getMaxNumberOfPieces() > 3) {
            reduce = 3;
         }
         if (depth - reduce - 1 > 0) {
            value = -Max(-side, depth - reduce - 1, -beta, -beta + 1, true, false, false, false);
         } else {
            value = -Quies(-side, 0, -beta, -beta + 1);
         }
         thisDepth--;
         chessBoard.switchTurn();
         if (value >= beta) {
            if (!stop) {
               HashTable.addHash(key, chessBoard.hashValue, 0, value, depth, HASH_BETA, 0, ancient);
            }
            return value;
         }
         if (value <= -19000) {
            nullFail = 1;
         }
      }
      boolean isRootFirstMove = false;
      if(alpha == -BETA_START) {
         isRootFirstMove = true;
      }


      /** the number of positions skipped due to futility pruning */
      int numberOfSkippedNodesFP = 0;

      long oldAlpha = alpha;

      boolean oneLegal = false;
      int piece;
      int to, from;

      long bMove = VALUE_START;
      int hType = HASH_ALPHA;

      int endIndex = 0;
      int capIndex = 0;

      /** state of search we are in..etc Hash move, killers, or regular */
      int state = SEARCH_HASH;

      /** extendAmount stores extensions/reduction amount for each move */
      int extendAmount = 0;

      
      int bestFullMove = 0;
      int[] moveArr = new int[128];
     

      int badCapIndex = 0;
      int moveCount = 0;
      int passant;

      boolean drawPV = false;
      if (side == -1) {
         passant = chessBoard.getPassantB();
      } else {
         passant = chessBoard.getPassantW();
      }
      while (state != SEARCH_END) {

         switch (state) {
            case (SEARCH_HASH):
               index = HashTable.hasHash(key, chessBoard.hashValue);
               if (index != 1) {
                  theMove = HashTable.getMove(key, index);
                  bestFullMove = theMove;
                  if (theMove != 0) {// && verifyHash(side, theMove)) {
                     hashArr[hashCount++] = theMove;
                  }
                  if (index == 0) {
                     index = HashTable.hasSecondHash(key, chessBoard.hashValue);
                     if (index != 1) {
                        theMove = HashTable.getMove(key, index);
                        if (theMove != 0) { // && verifyHash(side, theMove)) {
                           hashArr[hashCount++] = theMove;
                           if (hashCount == 2) {
                              if (hashArr[0] == hashArr[1]) {
                                 hashCount--;
                              }
                           }
                        }
                     }
                  }
               }

               /** internal iterative deepening **/
               if (hashCount == 0 && beta - alpha > 1 && depth > 3 && !isInCheck) {
                  thisDepth--;
                  Max(side, depth - 2, alpha, beta, true, false, false, true);
                  index = HashTable.hasHash(key, chessBoard.hashValue);
                  if (index != 1) {
                     hashArr[0] = HashTable.getMove(key, index);
                     if (hashArr[0] != 0 ) { // && verifyHash(side, hashArr[0])) {
                        bestFullMove = hashArr[0];
                        hashCount++;
                     }
                  }
                  if (hashCount == 0 && index == 0) {
                     index = HashTable.hasSecondHash(key, chessBoard.hashValue);
                     if (index != 1) {
                        hashArr[0] = HashTable.getMove(key, index);
                        if (hashArr[0] != 0 ) { //&& verifyHash(side, hashArr[0])) {
                           bestFullMove = hashArr[0];
                           hashCount++;
                        }
                     }
                  }
               }
               nMove = false;       
               System.arraycopy(hashArr, 0, moveArr, 0, hashCount);
               index = hashCount;
               break;
            case (MATE_KILLER):
               if (isInCheck) {
                  state = SEARCH_END - 1;
                  index = getCheckEscapes(side, moveArr);
               } else {
                  index = 0;
                  if (MoveFunctions.getValue(killerMoves[thisDepth][1]) >= 200) {
                     if (verifyKiller(side, killerMoves[thisDepth][1])) {
                        moveArr[index++] = killerMoves[thisDepth][1] & 16777215;
                        hashArr[hashCount++] = moveArr[index];
                        for (int i = 0; i < hashCount - 1; i++) {
                           if (moveArr[index] == hashArr[i]) {
                              index--;
                              hashCount--;
                           }
                        }
                     }
                  }

                  if (MoveFunctions.getValue(killerMoves[thisDepth][0]) >= 200) {
                     if (verifyKiller(side, killerMoves[thisDepth][0])) {
                        moveArr[index++] = killerMoves[thisDepth][0] & 16777215;
                        hashArr[hashCount++] = moveArr[index];
                        for (int i = 0; i < hashCount - 1; i++) {
                           if (moveArr[index] == hashArr[i]) {
                              index--;
                              hashCount--;
                           }
                        }
                     }
                  }
               }
               break;
            case (GOOD_CAPTURES):
               index = getCaptures(side, moveArr);
               capIndex = index;
               badCapIndex = index - 1; 
               break;
            case (KILLER_MOVES):
               index = 0;

               if (verifyKiller(side, killerMoves[thisDepth][1])) {
                  moveArr[127] = killerMoves[thisDepth][1] & 16777215;
                  hashArr[hashCount++] = moveArr[127];
                  index++;
                  for (int i = 0; i < hashCount - 1; i++) {
                     if (moveArr[127] == hashArr[i]) {
                        index--;
                        hashCount--;
                     }
                  }
               }
               if (verifyKiller(side, killerMoves[thisDepth][0])) {
                  int position = 127 - index;
                  moveArr[position] = killerMoves[thisDepth][0] & 16777215;
                  hashArr[hashCount++] = moveArr[position];
                  index++;
                  for (int i = 0; i < hashCount - 1; i++) {
                     if (moveArr[position] == hashArr[i]) {
                        index--;
                        hashCount--;
                     }
                  }
               }
               endIndex = 128 - index;
               index = 128;
               break;
            case (NON_CAPS):
               index = getMoves(side, moveArr, capIndex);
               endIndex = capIndex;
               break;
            case (BAD_CAPS):
               index = capIndex;
               endIndex = badCapIndex + 1;
               break;
         }
         for (int i = index - 1; i >= endIndex; i--) {

            //if(state == SEARCH_HASH) {
            //if(!verifyHash(side,moveArr[i])) {
            //   System.out.println("info string crap move");
            //   continue;
            //}
            //}

            theMove = moveArr[i];

            if (state == GOOD_CAPTURES || state == NON_CAPS || state == BAD_CAPS) {
               boolean duplicate = false;
               for (int j = 0; j < hashCount; j++) {
                  if (theMove == hashArr[j]) {
                     duplicate = true;
                     break;
                  }
               }
               if (duplicate) {
                  continue;
               }
            }

            to = MoveFunctions.getTo(theMove);
            from = MoveFunctions.getFrom(theMove);
            piece = MoveFunctions.getPiece(theMove);

            /** any captures which the static exchnage evaluator finds lose material
             * get placed in the bad capture array and are tried later
             */
            if (state == GOOD_CAPTURES) {
               if (MoveFunctions.getValue(theMove) == 1) {
                  continue;
               }
               if (Board.piece_in_square[to] != -1) {
                  if (Global.values[Board.piece_in_square[from]] >= Global.values[Board.piece_in_square[to]]
                          && SEE.getSEE(side, to, from, passant) < 0) {
                     moveArr[badCapIndex--] = theMove;
                     continue;
                  }
               } else {
                  if (SEE.getSEE(side, to, from, passant) < 0) {
                     moveArr[badCapIndex--] = theMove;
                     continue;
                  }
               }
            }
            extendAmount = 0;

            /** make the move */
            boolean draw = false;

            if (chessBoard.MakeMove(theMove, true) >= 2) {
               draw = true;
               oneLegal = true;
               value = 0;
            } else if (!isInCheck && inCheck(side)) {
               chessBoard.UnMake(theMove, true);			//avoid leaving king in check
               continue;
            } else {
               /** we have a legal move */
               oneLegal = true;

               //extend if this is a checking move
               boolean checkingMove = false;
               if (inCheck(-side)) {
                  checkingMove = true;
                  extendAmount++;
               }

               //passed pawn push extension
               boolean pawnExtension = false;
               if ((chessBoard.getTotalValue() < 5000) && !checkingMove && !isInCheck && (piece % 6 == 5 && (to / 8 == 6 || to / 8 == 1))) {	//extention for close to promotion
                  pawnExtension = true;
                  extendAmount++;
               }

               //recognize moves involving passed pawns
               //do not want to forward prune/lmr these moves
               boolean passedPawnMove = false;
               if (piece % 6 == 5) {
                  passedPawnMove = Evaluation2.isPassedPawn(side, to);
               }

                // futility pruning code
               if (bFutilityPrune && extendAmount == 0 && !checkingMove && !passedPawnMove) {
                  scoreThreshold = chessBoard.GetMaterialScore(side);
                  if (scoreThreshold + futilityMargin <= alpha) {
                     numberOfSkippedNodesFP++;
                     chessBoard.UnMake(theMove, true);
                     continue;
                  }
               }
          
               //late move reduction code
               boolean lmr = false;
                if (state == NON_CAPS && ((alpha == beta - 1 && moveCount >= 4) || moveCount >= 15) && depth >= 2 && !passedPawnMove
                       && !isInCheck && !checkingMove && (MoveFunctions.moveType(theMove) == Global.ORDINARY_MOVE || MoveFunctions.moveType(theMove) > Global.DOUBLE_PAWN_WHITE)) {
                  extendAmount = -1;
                  lmr = true;
               }

               int nextDepth = depth - 1 + extendAmount;
               if (moveCount == 0) {
                  if (nextDepth > 0) {
                     value = -Max(-side, depth - 1 + extendAmount, -beta, -alpha, nMove, checkingMove, isInCheck | pawnExtension, false);
                  } else {
                     //nodes++;
                     value = -Quies(-side, 0, -beta, -alpha);
                  }
                  thisDepth--;
               } else {
                  if (nextDepth > 0) {
                     value = -Max(-side, depth - 1 + extendAmount, -alpha - 1, -alpha, nMove, checkingMove, isInCheck | pawnExtension, false);
                  } else {
                     //nodes++;
                     value = -Quies(-side, 0, -alpha - 1, -alpha);
                  }
                  thisDepth--;
                  if (value > alpha && value < beta) {
                     if (lmr == true) {
                        extendAmount = 0;
                     }
                     if (depth - 1 > 0) {
                        value = -Max(-side, depth - 1 + extendAmount, -beta, -alpha, nMove, checkingMove, isInCheck | pawnExtension, false);
                     } else {
                        //nodes++;
                        value = -Quies(-side, 0, -beta, -alpha);
                     }
                     thisDepth--;
                  } else if (value > alpha && lmr == true) {       //use normal depth if lmr move looks interesting
                     if (depth - 1 > 0) {   
                        value = -Max(-side, depth - 1, -beta, -alpha, nMove, false, false, false);
                        thisDepth--;
                     } 
                  }
               }
            }

            chessBoard.UnMake(theMove, true);

            if (stop) {
               return 0;
            }

            if (value > bMove) {
               if(isRootFirstMove) {
                  levelOneValue = -value;
               }

               if (value > alpha) {
                  if (value >= beta) {
                     HashTable.addHash(key, chessBoard.hashValue, theMove, value, depth, HASH_BETA, nullFail, ancient);
                     if (side == 1) {
                        Hist[from][to]++;
                     } else {
                        Hist2[from][to]++;
                     }
                     if (state == NON_CAPS) {									//if state is a non capture or a bad capture
                        if (value >= 19000) {
                           theMove = MoveFunctions.setValue(theMove, 200);          //mark this move as a mate killer
                        }
                        if (theMove != killerMoves[thisDepth][0]) {
                           int temp1 = killerMoves[thisDepth][0];
                           killerMoves[thisDepth][0] = theMove;
                           killerMoves[thisDepth][1] = temp1;
                        }
                     }
                     return value;
                  }

                  bestFullMove = theMove;
                  if (draw) {  
                     drawPV = true;
                     PV[thisDepth - 1][thisDepth - 1] = chessBoard.GetMoveAtDepth(thisDepth - 1);
                     PV[thisDepth-1][thisDepth] = theMove;
                     PV[thisDepth-1][thisDepth + 1] = SEARCHDRAW_PV;
                     lengthPV[thisDepth-1] = thisDepth+1;
                  } else {
                     drawPV = false;
                  }
                  hType = HASH_EXACT;
                  alpha = value;
               }
               bMove = value;
            }
            moveCount++;
         }                                                        //end for
         state++;
      }                                                           // end while
      
      if (!isInCheck && !oneLegal) {				//stalemate detection
         bMove = 0;
         if (bMove > alpha && bMove < beta) {
            SavePV(SEARCHDRAW_PV);
         }
         hType = 4;
      } else if (isInCheck && !oneLegal) {     //checkmate detection
         bMove = -20000 + thisDepth;
         if (bMove > alpha && bMove < beta) {
            SavePV(MATE_PV);
         }
         hType = 4;
         HashTable.addHash(key, chessBoard.hashValue, bestFullMove, -20000, depth, hType, nullFail, ancient);
         return bMove;
      }

      //if we have skipped nodes due to futility pruning, then we adjust the transposition table entry
      // if we previously had an exact score, it is really a lower bound
      // if we have an upper bound, instead of storing the best score found, we store alpha
      if (numberOfSkippedNodesFP > 0) {
         if (hType == HASH_EXACT) {
            hType = HASH_BETA;
         }
         if (hType == HASH_ALPHA) {
            bMove = alpha;
         }
      }

      HashTable.addHash(key, chessBoard.hashValue, bestFullMove, bMove, depth, hType, nullFail, ancient);

      if (oneLegal && hType == HASH_EXACT) {             //update history tables
         if (side == 1) //black moving
         {
            Hist[MoveFunctions.getFrom(bestFullMove)][MoveFunctions.getTo(bestFullMove)]++;
         } else {
            Hist2[MoveFunctions.getFrom(bestFullMove)][MoveFunctions.getTo(bestFullMove)]++;
         }
      }
      if(alpha != oldAlpha && !drawPV)   {
         PV[thisDepth - 1][thisDepth - 1] = chessBoard.GetMoveAtDepth(thisDepth - 1);
         lengthPV[thisDepth-1] = lengthPV[thisDepth];
         System.arraycopy(PV[thisDepth], thisDepth, PV[thisDepth-1], thisDepth, lengthPV[thisDepth] - thisDepth + 1);
      }
      return bMove;
   }

   /**
    * Method Quies
    *
    * To-do - add ability to search checking moves at first and perhaps second plies of q search
    *
    * this is the negamax quiescense search function
    * used to reduce horizon effect by searching all captures until no more on the board
    *
    * @param int side - the side to move
    * @param int depth - the depth in the search from the leaf
    * @param int alpha - alpha value
    * @param int beta - beta value
    *
    * @return int - the score from searching the position
    *
    */
   private long Quies(int side, int depth, long alpha, long beta) {
      thisDepth++;
      nodes++;
      int[] capArr = new int[60];
      long value;
      int index = 0;
      long bValue;
      boolean isInCheck = false;
      long testValue = 0;
      if (depth > 0 && inCheck(side)) {
         isInCheck = true;
         index = getCheckEscapes(side, capArr);
         bValue = -20000 + thisDepth;
         if (index == 0) {
            if (bValue > alpha && bValue < beta) {
               SavePV(MATE_PV);
            }
            return -20000 + thisDepth;
         }
         bValue = -20000 + thisDepth;
      } else {
         value = Evaluation2.getEval(side);
         if (value > alpha) {
            if (value >= beta) {
               return value;
            }
            SavePV(STANDPAT_PV);
            alpha = value;
         }
        
         index = getCaptures(side, capArr);

         if (index == 0) {
            return value;
         }
         bValue = value;
         testValue = value;
      }
      long oldAlpha = alpha;
      for (int i = index - 1; i >= 0; i--) {

         if (MoveFunctions.getValue(capArr[i]) == 1) {
            continue;
         }

         int to = MoveFunctions.getTo(capArr[i]);
         int from = MoveFunctions.getFrom(capArr[i]);

         if (!isInCheck && Board.piece_in_square[to] != -1) {
            if ((Global.values[Board.piece_in_square[to]] - Global.values[Board.piece_in_square[from]]) < Math.max(0, alpha - testValue)
                    && (SEE.getSEE(side, to, from, -1) < Math.max(0, (alpha - testValue)))) {
               bValue = alpha;
               continue;
            }
         } /*else {          //SEE doesn't properly handle passant capture removal yet
         int passant;
         if(side == -1)
         passant = chessBoard.getPassantB();
         else
         passant = chessBoard.getPassantW();

         if(SEE.getSEE3(side,to,from,passant) < Math.max(0,(alpha - testValue)/20)) {
         bValue = alpha;
         continue;
         }
         }  */

         chessBoard.MakeMove(capArr[i], false);		//make the move;

         if (!isInCheck && inCheck(side)) {
            chessBoard.UnMake(capArr[i], false);
            continue;
         }
       
         value = -Quies(-side, depth + 1, -beta, -alpha);
         thisDepth--;
         chessBoard.UnMake(capArr[i], false);
         if (value > bValue) {
            if (value >= beta) {
               return value;
            }
            bValue = value;
            if (value > alpha) {
               alpha = value;
            }
         }
      }
      if(alpha != oldAlpha)   {
          PV[thisDepth - 1][thisDepth - 1] = chessBoard.GetMoveAtDepth(thisDepth - 1);
          lengthPV[thisDepth-1] = lengthPV[thisDepth];
          System.arraycopy(PV[thisDepth], thisDepth, PV[thisDepth-1], thisDepth, lengthPV[thisDepth] - thisDepth + 1);
     }
      return bValue;
   }

   /**
    * Method PerftTest
    *
    * test move generation by generating all legal moves for a given depth
    * these values can be compared against known correct values
    *
    * @param int depth - the depth for the perft test
    *
    */
   public void PerftTest(int depth) {
      perft = 0;
      long temp = System.currentTimeMillis();
      Perft(chessBoard.getTurn(), depth);
      long temp2 = System.currentTimeMillis();
      long time = temp2 - temp;
      System.out.println("Perft value is " + perft);
      System.out.println("millis seconds taken is " + (time));

   }

   /**
    * Method Divide
    *
    * if a bug is found from a perft test, divide will show all moves from a position
    * and their corresponding number of moves for a given depth
    * This can identify which moves are producing incorrect results
    *
    * @param int depth - the depth for the divide test
    *
    */
   public void Divide(int depth) {
      int index;
      boolean inCheck = false;
      int side = chessBoard.getTurn();
      if (inCheck(side)) {
         inCheck = true;
      }

      int[] moveArr = new int[128];
      if (!inCheck) {
         int index2 = getCaptures(side, moveArr);
         index = getMoves(side, moveArr, index2);
      } else {
         index = getCheckEscapes(side, moveArr);
      }
      for (int i = index - 1; i >= 0; i--) {
         perft = 0;
         int reps = chessBoard.MakeMove(moveArr[i], false);		//make the move;
         //print out the to and from algebraic positions
         int to = MoveFunctions.getTo(moveArr[i]);
         int from = MoveFunctions.getFrom(moveArr[i]);
         int piece = MoveFunctions.getPiece(moveArr[i]);

         String output = HistoryWriter.getUCIMove(to, from, piece);
         System.out.print(output);
         if (!inCheck && inCheck(side)) {
            chessBoard.UnMake(moveArr[i], false);
            continue;
         }
         if (reps == 3) {
            return;
         } else {
            Perft(-side, depth - 1);
         }
         chessBoard.UnMake(moveArr[i], false);
         System.out.println(" " + perft);
      }
   }

   /**
    * Method Perft
    *
    * recursive function to generate and return the number of moves searched
    *
    * @param int side - the side moving
    * @param int depth - the depth for the perft test
    *
    */
   private void Perft(int side, int depth) {

      int reps;
      boolean inCheck = false;
      if (inCheck(side)) {
         inCheck = true;
      }
      if (depth <= 0) {
         perft++;
         return;
      }
      int index;
      int[] moveArr = new int[128];
      if (!inCheck) {
         int index2 = getCaptures(side, moveArr);
         index = getMoves(side, moveArr, index2);
      } else {
         index = getCheckEscapes(side, moveArr);
      }
      for (int i = index - 1; i >= 0; i--) {
         reps = chessBoard.MakeMove(moveArr[i], true);		//make the move
         if (!inCheck && inCheck(side)) {
            chessBoard.UnMake(moveArr[i], true);
            continue;
         }
         if (reps == 3) {
            return;
         } else {
            Perft(-side, depth - 1);
         }
         chessBoard.UnMake(moveArr[i], true);
      }
   }
}
