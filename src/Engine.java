import java.util.Random;

/**
 * Engine.java
 *
 * Version 4.0
 * 
 * Copyright (c) 2012 Eric Stock

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

 * Engine.java
 * This class contains all the search functionality
 * -alpha beta - pvs search
 * -q search
 * -move generation functions - which uses chessBoard.java's individual piece move functions
 * -divide and perft functions for move generation testing and verification
 *
 * @version 	4.00 March 2012
 * @author 	Eric Stock
 */
public final class Engine {

   /** used similar to a C preprocessor define to instruct the move generation to properly generate all moves required for perft scoring */
   private static final boolean PERFT_ENABLED = false;
   
    /** used similar to a C preprocessor define to instruct perft to use the transposition table */
    /** useful for testing transposition table */
    private static final boolean PERFT_TRANSTABLE = false;

   /** chessBoard object from singleton class Board represents chess board and associated datastructures */
   private Board chessBoard;
   /** counter for positions evaluated */
   private static long nodes;			//counter for evaluated positions
   private static long hashHits;		//counter hash table hits
   private static long hashAttempts;	//counter for hash table attempts
   private static long nps;         //nodes per second

   /** 2D 64X64 arrays for history heuristic move ordering enhancement */
   private static final short[][] Hist = new short[64][64];
   private static final short[][] Hist2 = new short[64][64];
   private static TransTable HashTable = new TransTable(Global.HASHSIZE, 0);       //transposition table variable used in the search
   
   /** array for storing moves from the root position */
   private static final int[] moveOrder = new int[128];
   /** 2D array to store killer moves - 2 slots for each ply in the search */
   private static final int[][] killerMoves = new int[100][2];
   
   /** global boolean flag to trigger then end of the search when out of time */
   private static boolean stop;
   
   /** stores node count at which a time check should occur */
   private static long nextTimeCheck;
   
   /** used to track the end time and start time of a search */
   private static long startTime, endTime;

   /** the amount of time currently allowed in the search
    * - modified by the time state machine contained within this class
    */
   private static long searchTime;

   /** the maximum time we are allowed to extend the search when the 1st move drops at the root
    * ...also the max time we can extend by when we find a new best move, but make sure we verify this move
    * and collect its score
    */
   private static long maximumTime;
   
   /** the maximum time we are allowed to extend the search when we have started a depth iteration
    * and we do not have a score yet
    */
   
   private static long maximumRootGetAScoreTime;
   /** time used to complete the most recent itTIME_STATE_HAVE_1ST_eration in the iteratively deepened search */
   
   private static final int TIME_STATE_HAVE_1ST_MOVE = 0;
   private static final int TIME_STATE_FAIL_LOW = 1;
   private static final int TIME_STATE_FAIL_HIGH = 2;
   private static final int TIME_STATE_ITERATION_START = 3;
   
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
   private static byte thisDepth;
   /** boolean which represents if we are using infinite time control */
   private static boolean infiniteTimeControl = false;
   /** used to track the age of hash table entries */
   private int ancient;
   /** count of moves generated when debugging move gen using perft command */
   private long perft = 0;
   /** "triangular array to store the PV **/
   private static final int[][] PV = new int[64][64];
  /** array to store the length of the PV **/
   private static final byte[] lengthPV = new byte[64];

   private static Random rand = new Random(9L);

   /**
    * Constructor Engine
    *
    * grabs a reference to the instantiated Board object
    *
    */
   public Engine() {
      chessBoard = Board.getInstance();
   }

    /**
    * Method GotoTimeState
    *
    * handles state transitions related to changing the amount of time we use during the search
    *
    * @param timeState - the time control state we are switching to
    */
   private void GotoTimeState(int timeState) {

      switch(timeState) {
         case(TIME_STATE_HAVE_1ST_MOVE):
            endTime = startTime + searchTime;
            break;

         case(TIME_STATE_FAIL_LOW):
            //need to increase the searchTime and maximumRootGetAScoreTime
            //must make sure we don't exceed the maximumTime for the search
            //only do this if the current iteration is likely to be the last without a time extension
            long currentTime = System.currentTimeMillis();
            long last60Percent = (long)(((double)maximumRootGetAScoreTime)*0.60f);

            //if the move fails low in the last 70 percent or greater of the time allocated, we must extend to try to find a better move
            if(currentTime + last60Percent > endTime) {
               searchTime = maximumRootGetAScoreTime + last60Percent * 2;
               searchTime =  Math.min(searchTime, maximumTime);
               endTime = startTime + searchTime;

               //extend the time to find a root score also
               maximumRootGetAScoreTime += last60Percent * 3;
               maximumRootGetAScoreTime = Math.min(maximumRootGetAScoreTime, maximumTime);
            }
            break;

         case(TIME_STATE_FAIL_HIGH):
            //we use the maximumRootGetAScoreTime to try to make sure we give this move a chance to prove itself as good, or fail low after all
            endTime = startTime + maximumRootGetAScoreTime;
            break;

         case(TIME_STATE_ITERATION_START):
            endTime = startTime + maximumRootGetAScoreTime;
            break;
      }
      nextTimeCheck = nodes + Math.max(nps / 100L , ((endTime - System.currentTimeMillis())/2 * (nps / 1000L)));
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
            nextTimeCheck = nodes + Math.max(nps / 100L , ((endTime - temp)/2 * (nps / 1000L)));
            return true;
         } else {
             return false;
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



	public int RandomSearch() {

		int theSide = chessBoard.getTurn();
		int[] moveArr = new int[128];
		boolean bInCheck = inCheck(theSide);
		int numberOfMoves = GetAllMoves(theSide, moveArr);
		
		for (int i = numberOfMoves - 1; i >= 0; i--) {
			int tempMove = moveArr[i];
			chessBoard.MakeMove(tempMove, false);
			if (inCheck(theSide)) {
				for (int j = i; j <= numberOfMoves - 1; j++) {
					moveArr[j] = moveArr[j + 1];
				}
				numberOfMoves--;
				chessBoard.UnMake(tempMove, false);
				continue;
			}
			chessBoard.UnMake(tempMove, false);
		}

		if(numberOfMoves == 0)
		{
			return bInCheck ? Global.CHECKMATE : Global.STALEMATE;
		}

		int iRandomIndex = rand.nextInt(numberOfMoves);

		chessBoard.AddMove( moveArr[iRandomIndex]);
		chessBoard.MakeMove( moveArr[iRandomIndex], false);
		int reps =  chessBoard.AddRepetitionRoot();

		if(chessBoard.GetRawMaterialScore() == Global.materialDraw) {
			return Global.INSUFICIENT_MATERIAL;
		}
		else if (reps == 3) {
			return Global.DRAW_REPETITION;
		}
		else if (reps == 5) {
			return Global.DRAW_50MOVES;
		}
		return 0;
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

      if(maxTime != time) {
         maximumRootGetAScoreTime = maxTime -  (int)((double)maxTime * 0.57);
      } else {
         maximumRootGetAScoreTime = maxTime;
      }
      maximumRootGetAScoreTime = Math.max(time, maximumRootGetAScoreTime);
      
      maximumTime = maxTime;
      startTime = System.currentTimeMillis();
      endTime = startTime + time;
      searchTime = time;

      stop = false;
      nps = 15;        //start the nps very low, so we can search a few moves, calculate the nps and then set our time checks accordingly
      
      nextTimeCheck = Math.min(1000, time * nps);

      //System.out.println("info string time is "+time);
      //System.out.println("info string max root get a score time is "+maximumRootGetAScoreTime);
      //System.out.println("info string max time is "+maxTime);


      // ancient node value betweeen 0 and 7
      ancient = (chessBoard.getCount() / 2) >> 3;

      infiniteTimeControl = inf;

      /** here we prepare the history arrays for the next search */
      if ((chessBoard.getCount() / 2 >> 3) == 7) {
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
      hashAttempts = 0;
	 hashHits = 0;
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
            //System.out.println("Draw move is " + HistoryWriter.getUCIMove((tempMove >> 6) & 63, tempMove & 63, (tempMove >> 12) & 15));
            compareArray[i] = 1;
         } else if (chessBoard.getDraw() == 100) {
            compareArray[i] = 1;
         } else if (isStaleMate(-theSide)) {
            //System.out.println("stalemate move is " + HistoryWriter.getUCIMove((tempMove >> 6) & 63, tempMove & 63, (tempMove >> 12) & 15));
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
         hashAttempts = 0;
	    hashHits = 0;
	    GotoTimeState(TIME_STATE_ITERATION_START);

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
                  HashTable.addHash(key, mv, -20000, insertDepth++, HASH_BETA, 0, ancient);
                  chessBoard.MakeMove(mv, false);
               }
            }
         }
         value = VALUE_START;
         alpha = ALPHA_START;
         beta = BETA_START;
        
         long tempBestMove = Integer.MIN_VALUE;
         isExact = false;
         thisDepth = 0;

         

         for (int i = numberOfMoves - 1; i >= 0; i--) {
            int tempMove = moveArr[i];
            if ((compareArray[i] & 1) != 0) {
               //System.out.println("Draw move 2 is " + HistoryWriter.getUCIMove((tempMove >> 6) & 63, tempMove & 63, (tempMove >> 12) & 15));
               value = 0;

               if(!isExact && value < bestValue - 30 && !stop) {
                  GotoTimeState(TIME_STATE_FAIL_LOW);
               }
               
            } else if (!isExact) {
               chessBoard.MakeMove(tempMove, true);
               value = -Max(-theSide, depth - 1, -beta, -alpha, false, inCheck(-theSide), false, false);
               chessBoard.UnMake(tempMove, true);
               thisDepth--;
               //check for a first move which goes LOW
               if(value < bestValue - 30 && value < 10000L && !stop) {
                  GotoTimeState(TIME_STATE_FAIL_LOW);
               }
            } else {
               chessBoard.MakeMove(tempMove, true);
               value = -Max(-theSide, depth - 1, -alpha - 1, -alpha, false, inCheck(-theSide), false, false);
               thisDepth--;

               if (value > alpha && !stop) {
                  //check for a move which goes HIGH
                  GotoTimeState(TIME_STATE_FAIL_HIGH);

                  value = -Max(-theSide, depth - 1, -beta, -alpha, false, inCheck(-theSide), false, false);
                  
                  thisDepth--;
               }
               chessBoard.UnMake(tempMove, true);
            }

            if ((((value > tempBestMove) && !stop) || !hasValue)) {      //have a new best move..update alpha..etc
               GotoTimeState(TIME_STATE_HAVE_1ST_MOVE);
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
                  compareArray[i] = 10000 - (i << 1 | 1);
               } else {
                  compareArray[i] = 10000 - (i << 1);
               }
            } else {                                                 //move is no good
               GotoTimeState(TIME_STATE_HAVE_1ST_MOVE);
               if (value == 0 && (compareArray[i] & 1) != 0)
                  compareArray[i] = 2000 + (i << 1 | 1);
               else
                  compareArray[i] = 2000 + (i << 1);                  //if this is a root draw or stalemate, set the lsb flag
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
         long elapsedTime = (System.currentTimeMillis() - startTime);

         if(elapsedTime == 0) {
            nps = 100000;
         } else  {
            nps = (int)(((double)nodes) / (((double)(elapsedTime))/(1000.0f)));
         }
       
         if (bestValue > 10000L) {			//this is a winning mate score
            long mate = (20000L - bestValue) / 2;
            System.out.println("info depth " + depth + " score mate " + mate + " nodes " + nodes + " nps " + nps + " pv " + pv);
         } else if (bestValue < -10000) {  //losing mate score
            long mate = (-20000L - bestValue) / 2;
            System.out.println("info depth " + depth + " score mate " + mate + " nodes " + nodes + " nps " + nps + " pv " + pv);
         } else {
            System.out.println("info depth " + depth + " score cp " + bestValue + " nodes " + nodes + " nps " + nps + " pv " + pv);
         }
	    //System.out.println("info string hashHits are "+hashHits + "info string hashAttempts are "+hashAttempts+"info string hash hit percent is "+(double)hashHits/(double)hashAttempts * 100.0);
         //System.out.println("info string hash size is "+Global.HASHSIZE+" number of used entries is "+HashTable.getCount()+" percentage of used entries is "+(double)HashTable.getCount()/(double)Global.HASHSIZE * 100.0);
	   
	    sortMoves(0, numberOfMoves, moveArr, compareArray);
      }
      
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
            Moves[index++] = MoveFunctions.makeMove(to, from, 5, -1, Global.DOUBLE_PAWN);
         }

         from = chessBoard.pieceList[4][0];
         if (chessBoard.wCastle > Global.CASTLED) {
				long Temp = chessBoard.getKingCastle((int)chessBoard.bitboard&255);
				if(chessBoard.wCastle != Global.SHORT_CASTLE && ((Temp & Global.set_Mask[2])!=0) ) {
					if( !chessBoard.isWhiteAttacked(2) && !chessBoard.isWhiteAttacked(3) ) {
						moveOrder[index] = Hist2[from][2];
						Moves[index++] = MoveFunctions.makeMove(2, from, 4, -1, Global.LONG_CASTLE);
					}
				}
				if(chessBoard.wCastle != Global.LONG_CASTLE && ((Temp & Global.set_Mask[6])!=0) ) {		
					if( !chessBoard.isWhiteAttacked(5) && !chessBoard.isWhiteAttacked(6) ) {
						moveOrder[index] = Hist2[from][6];
						Moves[index++] = MoveFunctions.makeMove(6, from, 4, -1, Global.SHORT_CASTLE);
					}
				}
         }
			int kingType = chessBoard.wCastle > Global.CASTLED ? Global.MOVE_KING_LOSE_CASTLE : Global.ORDINARY_MOVE;
			toSquares = chessBoard.getKingMoves(from);
         toSquares &= ~chessBoard.bitboard;
         while (toSquares != 0) {
            toBit = toSquares & -toSquares;
            toSquares ^= toBit;
            to = Long.numberOfTrailingZeros(toBit);
            moveOrder[index] = Hist2[from][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, 4, -1, kingType);
         }

			for(int j=0; j < chessBoard.pieceTotals[0]; j++)
			{
				from = chessBoard.pieceList[0][j];
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= ~chessBoard.bitboard;
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = Long.numberOfTrailingZeros(toBit);
					moveOrder[index] = Hist2[from][to];
					if(from == 0 && (chessBoard.wCastle & Global.LONG_CASTLE) != 0)
						Moves[index++] = MoveFunctions.makeMove(to, from,0, -1, Global.MOVE_ROOK_LOSE_CASTLE);
					else if(from == 7 && (chessBoard.wCastle & Global.SHORT_CASTLE) != 0)
						Moves[index++] = MoveFunctions.makeMove(to, from, 0, -1, Global.MOVE_ROOK_LOSE_CASTLE);
					else
						Moves[index++] = MoveFunctions.makeMove(to, from, 0, -1, Global.ORDINARY_MOVE);
				}
			}

			for(int i=1; i<4; i++)
			{
				for(int j=0; j < chessBoard.pieceTotals[i]; j++)
				{
					from = chessBoard.pieceList[i][j];
					toSquares = chessBoard.getAttackBoard(from);
					toSquares &= ~chessBoard.bitboard;
					while (toSquares != 0) {
						toBit = toSquares & -toSquares;
						toSquares ^= toBit;
						to = Long.numberOfTrailingZeros(toBit);
						moveOrder[index] = Hist2[from][to];
						Moves[index++] = MoveFunctions.makeMove(to, from, i, -1, Global.ORDINARY_MOVE);
					}
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
            Moves[index++] = MoveFunctions.makeMove(to, from, 11, -1, Global.DOUBLE_PAWN);
         }

			for(int j=0; j < chessBoard.pieceTotals[6]; j++)
			{
				from = chessBoard.pieceList[6][j];
				pieces ^= Global.set_Mask[from];
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= ~chessBoard.bitboard;
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = Long.numberOfTrailingZeros(toBit);
					moveOrder[index] = Hist[from][to];
					if(from == 56 && (chessBoard.bCastle & Global.LONG_CASTLE) != 0)
						Moves[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], -1, Global.MOVE_ROOK_LOSE_CASTLE);
					else if(from == 63 && (chessBoard.bCastle & Global.SHORT_CASTLE) != 0)
						Moves[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], -1, Global.MOVE_ROOK_LOSE_CASTLE);
					else
						Moves[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], -1, Global.ORDINARY_MOVE);
				}
			}
			
			for(int i=7; i<10; i++)
			{
				for(int j=0; j < chessBoard.pieceTotals[i]; j++)
				{
					from = chessBoard.pieceList[i][j];
					pieces ^= Global.set_Mask[from];
					toSquares = chessBoard.getAttackBoard(from);
					toSquares &= ~chessBoard.bitboard;
					while (toSquares != 0) {
						toBit = toSquares & -toSquares;
						toSquares ^= toBit;
						to = Long.numberOfTrailingZeros(toBit);
						moveOrder[index] = Hist[from][to];
						Moves[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], -1, Global.ORDINARY_MOVE);
					}
				}
         }

         from = chessBoard.pieceList[10][0];
			if (chessBoard.bCastle > Global.CASTLED) {
				long Temp = chessBoard.getKingCastle((int)(chessBoard.bitboard>>>56));
				if(chessBoard.bCastle != Global.SHORT_CASTLE && ((Temp & Global.set_Mask[2])!=0) ) {
					if( !chessBoard.isBlackAttacked(58) && !chessBoard.isBlackAttacked(59) ) {
						moveOrder[index] = Hist2[from][58];
						Moves[index++] = MoveFunctions.makeMove(58, from, 10, -1, Global.LONG_CASTLE);
					}
				}
				if(chessBoard.bCastle != Global.LONG_CASTLE && ((Temp & Global.set_Mask[6])!=0) ) {		
					if( !chessBoard.isBlackAttacked(61) && !chessBoard.isBlackAttacked(62) ) {
						moveOrder[index] = Hist2[from][62];
						Moves[index++] = MoveFunctions.makeMove(62, from, 10, -1, Global.SHORT_CASTLE);
					}
				}
         }

			toSquares = chessBoard.getKingMoves(from);
         toSquares &= ~chessBoard.bitboard;
         int kingType = chessBoard.bCastle > Global.CASTLED ? Global.MOVE_KING_LOSE_CASTLE : Global.ORDINARY_MOVE;
			while (toSquares != 0) {
           toBit = toSquares & -toSquares;
           toSquares ^= toBit;
           to = Long.numberOfTrailingZeros(toBit);
            moveOrder[index] = Hist[from][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, 10, -1, kingType);
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
		int index = 0;
		int type;
		if (side == -1) {			//white moving
			long enemies = chessBoard.blackpieces;
			boolean attackCastle = chessBoard.bCastle > Global.CASTLED ? true : false;

			for(int i=1; i<5; i++)
			{
				if(i == 4 && chessBoard.wCastle > Global.NO_CASTLE)
					type = Global.MOVE_KING_LOSE_CASTLE;
				else
					 type = Global.ORDINARY_CAPTURE;

				for(int j=0; j < chessBoard.pieceTotals[i]; j++)
				{
					int from = chessBoard.pieceList[i][j];
					long toSquares = chessBoard.getAttackBoard(from);
					toSquares &= enemies;
					while (toSquares != 0) {
						long toBit = toSquares & -toSquares;
						toSquares ^= toBit;
						int to = Long.numberOfTrailingZeros(toBit);
						if(attackCastle && (to == 56 || to == 63) && chessBoard.piece_in_square[to] == 6)
							type = Global.CAPTURE_ROOK_LOSE_CASTLE;
						//SEE.GetSEE2(side, to, from, type, 0);
						moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + Global.invValues[i];
						Captures[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], chessBoard.piece_in_square[to], type);
					}
				}
			}


			boolean ourCastle = chessBoard.wCastle > Global.CASTLED ? true : false;
			for(int j=0; j < chessBoard.pieceTotals[0]; j++)
			{
				int from = chessBoard.pieceList[0][j];
				long toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;
				while (toSquares != 0) {
					long toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					int to = Long.numberOfTrailingZeros(toBit);
					if(attackCastle && (to == 56 || to == 63) && chessBoard.piece_in_square[to] == 6)
						type = Global.CAPTURE_ROOK_LOSE_CASTLE;
					else if(ourCastle)
						type = Global.MOVE_ROOK_LOSE_CASTLE;
					else
						type = Global.ORDINARY_CAPTURE;
					//SEE.GetSEE2(side, to, from, type, 0);
					moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + Global.invValues[0];
					Captures[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], chessBoard.piece_in_square[to], type);
				}
			}

			long pieces = chessBoard.whitepawns;
			enemies ^=  Global.set_Mask[chessBoard.getPassantB()];
			long lAttack = (pieces << 7) & enemies & ~Global.fileMasks[7];
			long rAttack = (pieces << 9) & enemies & ~Global.fileMasks[0];
			enemies ^=  Global.set_Mask[chessBoard.getPassantB()];
			long promo = pieces & Global.rankMasks[6];
			if (promo != 0) {
				promo <<= 8;
				promo &= ~chessBoard.bitboard;
			}

			while (lAttack != 0) {
				long toBit = lAttack & -lAttack;
				lAttack ^= toBit;
				int to = Long.numberOfTrailingZeros(toBit);
				int from = to - 7;
				type = Global.ORDINARY_CAPTURE;
				if (to == chessBoard.getPassantB()) {
					moveOrder[index] = Global.values[5] + 4;
					type = Global.EN_PASSANT_CAP;
				} else if ((to >> 3) == 7) {
					if (PERFT_ENABLED) {
						moveOrder[index] = 0;
						Captures[index++] = MoveFunctions.makeMove(to, from, 5, chessBoard.piece_in_square[to], Global.PROMO_B);
						moveOrder[index] = 0;
						Captures[index++] = MoveFunctions.makeMove(to, from, 5, chessBoard.piece_in_square[to], Global.PROMO_N);
						moveOrder[index] = 0;
						Captures[index++] = MoveFunctions.makeMove(to, from, 5, chessBoard.piece_in_square[to], Global.PROMO_R);
					}
					moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 700;
					type = Global.PROMO_Q;
				} else {
					moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 4;
				}
				//SEE.GetSEE2(side, to, from, type, 0);
				Captures[index++] = MoveFunctions.makeMove(to, from, 5, chessBoard.piece_in_square[to], type);
			}

			while (rAttack != 0) {
				long toBit = rAttack & -rAttack;
				rAttack ^= toBit;
				int to = Long.numberOfTrailingZeros(toBit);
				int from = to - 9;
				type = Global.ORDINARY_CAPTURE;
				if (to == chessBoard.getPassantB()) {
					moveOrder[index] = Global.values[5] + 4;
					type = Global.EN_PASSANT_CAP;
				} else if ((to >> 3) == 7) {
					if (PERFT_ENABLED) {
						moveOrder[index] = 0;
						Captures[index++] = MoveFunctions.makeMove(to, from, 5, chessBoard.piece_in_square[to], Global.PROMO_B);
						moveOrder[index] = 0;
						Captures[index++] = MoveFunctions.makeMove(to, from, 5, chessBoard.piece_in_square[to], Global.PROMO_N);
						moveOrder[index] = 0;
						Captures[index++] = MoveFunctions.makeMove(to, from, 5, chessBoard.piece_in_square[to], Global.PROMO_R);
					}
					moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 700;
					type = Global.PROMO_Q;
				} else {
					moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 4;
				}
				//SEE.GetSEE2(side, to, from, type, 0);
				Captures[index++] = MoveFunctions.makeMove(to, from, 5, chessBoard.piece_in_square[to], type);
			}

			while (promo != 0) {
				int to = Long.numberOfTrailingZeros(promo);
				promo ^= Global.set_Mask[to];
				if (PERFT_ENABLED) {
					moveOrder[index] = 0;
					Captures[index++] = MoveFunctions.makeMove(to, to-8, 5, -1, Global.PROMO_B);
					moveOrder[index] = 0;
					Captures[index++] = MoveFunctions.makeMove(to, to-8, 5, -1, Global.PROMO_N);
					moveOrder[index] = 0;
					Captures[index++] = MoveFunctions.makeMove(to, to-8, 5, -1, Global.PROMO_R);
				}
				moveOrder[index] = 700;
				//SEE.GetSEE2(side, to, to-8, Global.PROMO_Q, 0);
				Captures[index++] = MoveFunctions.makeMove(to, to-8, 5, -1, Global.PROMO_Q);
			}
		} else {					//black moving
			long enemies = chessBoard.whitepieces;
			boolean attackCastle = chessBoard.wCastle > Global.CASTLED ? true : false;

			for(int i=7; i<11; i++)
			{
				if(i == 10 && chessBoard.bCastle > Global.NO_CASTLE)
					type = Global.MOVE_KING_LOSE_CASTLE;
				else
					 type = Global.ORDINARY_CAPTURE;

				for(int j=0; j < chessBoard.pieceTotals[i]; j++)
				{
					int from = chessBoard.pieceList[i][j];
					long toSquares = chessBoard.getAttackBoard(from);
					toSquares &= enemies;
					while (toSquares != 0) {
						long toBit = toSquares & -toSquares;
						toSquares ^= toBit;
						int to = Long.numberOfTrailingZeros(toBit);
						if(attackCastle && (to == 0 || to == 7) && chessBoard.piece_in_square[to] == 0)
							type = Global.CAPTURE_ROOK_LOSE_CASTLE;
						//SEE.GetSEE2(side, to, from, type, 0);
						moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + Global.invValues[i];
						Captures[index++] = MoveFunctions.makeMove(to, from, i, chessBoard.piece_in_square[to], type);
					}
				}
			}


			boolean ourCastle = chessBoard.bCastle > Global.CASTLED ? true : false;
			for(int j=0; j < chessBoard.pieceTotals[6]; j++)
			{
				int from = chessBoard.pieceList[6][j];
				long toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;
				while (toSquares != 0) {
					long toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					int to = Long.numberOfTrailingZeros(toBit);
					if(attackCastle && (to == 0 || to == 7) && chessBoard.piece_in_square[to] == 0)
						type = Global.CAPTURE_ROOK_LOSE_CASTLE;
					else if(ourCastle)
						type = Global.MOVE_ROOK_LOSE_CASTLE;
					else
						type =  Global.ORDINARY_CAPTURE;
					//SEE.GetSEE2(side, to, from, type, 0);
					moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + Global.invValues[6];
					Captures[index++] = MoveFunctions.makeMove(to, from, 6, chessBoard.piece_in_square[to], type);
				}
			}

			long pieces = chessBoard.blackpawns;
			enemies ^= Global.set_Mask[chessBoard.getPassantW()];
			long lAttack = pieces >> 9 & enemies & ~Global.fileMasks[7];
			long rAttack = pieces >> 7 & enemies & ~Global.fileMasks[0];
			enemies ^= Global.set_Mask[chessBoard.getPassantW()];
			long promo = pieces & Global.rankMasks[1];
			if (promo != 0) {
				promo >>= 8;
				promo &= ~chessBoard.bitboard;
				}
			while (lAttack != 0) {
				long toBit = lAttack & -lAttack;
				lAttack ^= toBit;
				int to = Long.numberOfTrailingZeros(toBit);
				int from = to + 9;
				type = Global.ORDINARY_CAPTURE;
				if (to == chessBoard.getPassantW()) {
					moveOrder[index] = Global.values[5] + 4;
					type = Global.EN_PASSANT_CAP;
				} else if ((to >> 3) == 0) {
					if (PERFT_ENABLED) {
						moveOrder[index] = 0;
						Captures[index++] = MoveFunctions.makeMove(to, from, 11, chessBoard.piece_in_square[to], Global.PROMO_B);
						moveOrder[index] = 0;
						Captures[index++] = MoveFunctions.makeMove(to, from, 11, chessBoard.piece_in_square[to], Global.PROMO_N);
						moveOrder[index] = 0;
						Captures[index++] = MoveFunctions.makeMove(to, from, 11, chessBoard.piece_in_square[to], Global.PROMO_R);
					}
					moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 700;
					type = Global.PROMO_Q;
				} else {
					moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 4;
				}
				//SEE.GetSEE2(side, to, from, type, 0);
				Captures[index++] = MoveFunctions.makeMove(to, from, 11, chessBoard.piece_in_square[to], type);
			}

			while (rAttack != 0) {
				long toBit = rAttack & -rAttack;
				rAttack ^= toBit;
				int to = Long.numberOfTrailingZeros(toBit);
				int from = to + 7;
				chessBoard.piece_in_square[to] = chessBoard.piece_in_square[to];
				type = Global.ORDINARY_CAPTURE;
				if (to == chessBoard.getPassantW()) {
					type = Global.EN_PASSANT_CAP;
					moveOrder[index] = Global.values[5] + 4;
				} else if ((to >> 3) == 0) {
					if (PERFT_ENABLED) {
					moveOrder[index] = 0;
					Captures[index++] = MoveFunctions.makeMove(to, from, 11, chessBoard.piece_in_square[to], Global.PROMO_B);
					moveOrder[index] = 0;
					Captures[index++] = MoveFunctions.makeMove(to, from, 11, chessBoard.piece_in_square[to], Global.PROMO_N);
					moveOrder[index] = 0;
					Captures[index++] = MoveFunctions.makeMove(to, from, 11, chessBoard.piece_in_square[to], Global.PROMO_R);
				}
					moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 700;
					type = Global.PROMO_Q;
				} else {
					moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 4;
				}
				//SEE.GetSEE2(side, to, from, type, 0);
				Captures[index++] = MoveFunctions.makeMove(to, from, 11, chessBoard.piece_in_square[to], type);
			}

			while (promo != 0) {
				long toBit = promo & -promo;
				promo ^= toBit;
				int to = Long.numberOfTrailingZeros(toBit);
				int from = to + 8;
				if (PERFT_ENABLED) {
					moveOrder[index] = 0;
					Captures[index++] = MoveFunctions.makeMove(to, from, 11, -1, Global.PROMO_B);
					moveOrder[index] = 0;
					Captures[index++] = MoveFunctions.makeMove(to, from, 11, -1, Global.PROMO_N);
					moveOrder[index] = 0;
					Captures[index++] = MoveFunctions.makeMove(to, from, 11, -1, Global.PROMO_R);
				}
				//SEE.GetSEE2(side, to, from, Global.PROMO_Q, 0);
				moveOrder[index] = 700;
				Captures[index++] = MoveFunctions.makeMove(to, from, 11, -1, Global.PROMO_Q);
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
		boolean kingLoseCastle;
		boolean rookLoseCastle;
		boolean attackRookCastle;

      if (side == -1) {			//white moving
         friends = chessBoard.whitepieces;
         king = chessBoard.whiteking;
         bishops = chessBoard.blackbishops;
         queen = chessBoard.blackqueen;
         rooks = chessBoard.blackrooks;
         knights = chessBoard.blackknights;
			kingLoseCastle = chessBoard.wCastle > Global.CASTLED ? true : false;
			rookLoseCastle = kingLoseCastle;
			attackRookCastle = chessBoard.bCastle > Global.CASTLED ? true : false;
      } else {					//black moving
         friends = chessBoard.blackpieces;
         king = chessBoard.blackking;
         bishops = chessBoard.whitebishops;
         queen = chessBoard.whitequeen;
         rooks = chessBoard.whiterooks;
         knights = chessBoard.whiteknights;
			kingLoseCastle = chessBoard.bCastle > Global.CASTLED ? true : false;
			rookLoseCastle = kingLoseCastle;
			attackRookCastle = chessBoard.wCastle > Global.CASTLED ? true : false;
      }
      kingPos = Long.numberOfTrailingZeros(king);
      long toSquares = chessBoard.getAttackBoard(kingPos) & ~friends;


		long temp = chessBoard.getMagicBishopMoves(kingPos);
      temp &= (bishops | queen);
		long attackBits = temp;
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
		attackBits |= temp;
		while (temp != 0) {
         fromBit = temp & -temp;
         attackFrom = Long.numberOfTrailingZeros(fromBit);
         if ((attackFrom >> 3) == (kingPos >> 3)) {
            toSquares &= ((~Global.rankMasks[kingPos >> 3]) ^ Global.set_Mask[attackFrom]);
         } else {
            toSquares &= ((~Global.fileMasks[kingPos & 7]) ^ Global.set_Mask[attackFrom]);
         }
         temp ^= fromBit;
         attacks++;
      }

      while (toSquares != 0) {
         int type = Global.ORDINARY_MOVE;
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
         int cP = chessBoard.piece_in_square[to];
         int value;
         if (cP != -1) {
            type = Global.ORDINARY_CAPTURE;
				value = 1000;
         } else {
            value = 100;
         }
         moveOrder[index] = value;
         if( kingLoseCastle )
				type = Global.MOVE_KING_LOSE_CASTLE;
			escapes[index++] = MoveFunctions.makeMove(to, kingPos, chessBoard.piece_in_square[kingPos], cP, type);    //generate king escape moves
      }
      if (attacks > 2) {
         sortCaps(0, index, escapes);
         return index;
      }
		temp = chessBoard.getKnightMoves(kingPos);
		temp &= knights;
		attacks += Long.bitCount(temp);
		attackBits |= temp;
      if (side == -1) {			//white moving
         temp = chessBoard.getWPawnAttack(kingPos);
         temp &= chessBoard.blackpawns;
         attacks += Long.bitCount(temp);
			attackBits |= temp;
      } else {					//black moving
         temp = chessBoard.getBPawnAttack(kingPos);
         temp &= chessBoard.whitepawns;
         attacks += Long.bitCount(temp);
			attackBits |= temp;
      }
      if (attacks == 2) {
         sortCaps(0, index, escapes);
         return index;
      }
      attackFrom = Long.numberOfTrailingZeros(attackBits);
		temp = chessBoard.getAttack2(attackFrom);
      temp &= (friends & ~king);
      int type;
      int value;
      int cP = chessBoard.piece_in_square[attackFrom];

      while (temp != 0) {
         type = Global.ORDINARY_CAPTURE;
         fromBit = temp & -temp;
         temp ^= fromBit;
         int from = Long.numberOfTrailingZeros(fromBit);
         if (SEE.isPinned(side, attackFrom, from)) {
            continue;
         }
         
         if (chessBoard.piece_in_square[from] % 6 == 5) {
            if ((attackFrom >> 3) == 0 || (attackFrom >> 3) == 7) {
					if (PERFT_ENABLED) {
                  moveOrder[index] = 0;
                  escapes[index++] = MoveFunctions.makeMove(attackFrom, from, chessBoard.piece_in_square[from], cP, Global.PROMO_N);
                  moveOrder[index] = 0;
                  escapes[index++] = MoveFunctions.makeMove(attackFrom, from, chessBoard.piece_in_square[from], cP, Global.PROMO_R);
                  moveOrder[index] = 0;
                  escapes[index++] = MoveFunctions.makeMove(attackFrom, from, chessBoard.piece_in_square[from], cP, Global.PROMO_B);
               }
               type = Global.PROMO_Q;
					value = 800 + Global.values[chessBoard.piece_in_square[attackFrom]];
				}
				else
					value = Global.values[chessBoard.piece_in_square[attackFrom]] - Global.values[Global.PIECE_PAWN];
         }
			else
			{
				value = Global.values[chessBoard.piece_in_square[attackFrom]] - Global.values[chessBoard.piece_in_square[from]];
				if(attackRookCastle && (cP % 6) == 0)
					type = Global.CAPTURE_ROOK_LOSE_CASTLE;
				else if(rookLoseCastle && chessBoard.piece_in_square[from] % 6 == 0)
					type = Global.MOVE_ROOK_LOSE_CASTLE;
			}
			moveOrder[index] = value;
			escapes[index++] = MoveFunctions.makeMove(attackFrom, from, chessBoard.piece_in_square[from], cP, type);            //these are attacks on the king's attacker
      }

      if (chessBoard.piece_in_square[attackFrom] == 5) {
         if ((attackFrom - 8) == chessBoard.getPassantW()) {
            temp = chessBoard.getWPawnAttack(attackFrom - 8) & chessBoard.blackpawns;
            while (temp != 0) {
               fromBit = temp & -temp;
               temp ^= fromBit;
               int from = Long.numberOfTrailingZeros(fromBit);
               if (SEE.isPinned(side, attackFrom - 8, from)) {
						continue;
               }
               value = 100;
               moveOrder[index] = value;
               escapes[index++] = MoveFunctions.makeMove(attackFrom - 8, from, chessBoard.piece_in_square[from], -1, Global.EN_PASSANT_CAP);
            }
         }
      }
      //if attacking piece is a black pawn ( white moving )
      if (chessBoard.piece_in_square[attackFrom] == 11) {
         if ((attackFrom + 8) == chessBoard.getPassantB()) {
            temp = chessBoard.getBPawnAttack(attackFrom + 8) & chessBoard.whitepawns;
            while (temp != 0) {
               fromBit = temp & -temp;
               temp ^= fromBit;
               int from = Long.numberOfTrailingZeros(fromBit);
               if (SEE.isPinned(side, attackFrom + 8, from)) {
						continue;
               }
               value = 100;
               moveOrder[index] = value;
               escapes[index++] = MoveFunctions.makeMove(attackFrom + 8, from, chessBoard.piece_in_square[from], -1, Global.EN_PASSANT_CAP);
            }
         }
      }
      //if one attacker is a slide piece, generate moves to block the sliding attack
      if (!Global.slides[chessBoard.piece_in_square[attackFrom]]) {
         return index;
      }

      long squares = 0;                                                           //intermediate squares between attacker and king
      //need to get bitset of squares between attacker and king
      int difference = kingPos - attackFrom;
      int rankDiff = (kingPos >> 3) - (attackFrom >> 3);
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
            if( rookLoseCastle && chessBoard.piece_in_square[from] % 6 == 0 )
					type = Global.MOVE_ROOK_LOSE_CASTLE;
				
				if (SEE.isPinned(side, to, from)) {
               continue;
            }

				value =  -Global.values[chessBoard.piece_in_square[from]];
            
            if (chessBoard.piece_in_square[from] == 5) {
               if (to > 55) {
                  if (PERFT_ENABLED) {
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], -1, Global.PROMO_N);
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], -1, Global.PROMO_R);
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], -1, Global.PROMO_B);
                  }
                  value = -900;
						type = Global.PROMO_Q;
               } else if((to - from) == 16)   {
                  type = Global.DOUBLE_PAWN;             //handle case where a double pawn move blocks the attack
               }
            } else if (chessBoard.piece_in_square[from] == 11) {
               if (to < 8) {
                  if (PERFT_ENABLED) {
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], -1, Global.PROMO_N);
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], -1, Global.PROMO_R);
                     moveOrder[index] = value;
                     escapes[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], -1, Global.PROMO_B);
                  }
                  value = -900;
						type = Global.PROMO_Q;
               } else if((from - to) == 16)   {
                  type = Global.DOUBLE_PAWN;             //handle case where a double pawn move blocks the attack
               }
            }
            moveOrder[index] = value;
				escapes[index++] = MoveFunctions.makeMove(to, from, chessBoard.piece_in_square[from], -1, type);
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
   private boolean verifyHash(int side, int move) {
      
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
      if (chessBoard.piece_in_square[from] != piece) {
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
   }

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

      int piece = MoveFunctions.getPiece(move);

      if (side == -1 && piece > 5) //white moving
      {
         return false;
      } else if (side == 1 && piece < 6) {
         return false;
      }

      
      int from = MoveFunctions.getFrom(move);

      if (chessBoard.piece_in_square[from] != piece) {
         return false;
      }

      long temp;
      if (piece == 5) //wPawn
      { 
         temp = chessBoard.getWPawnMoves(from);
      } else if (piece == 11) {  //bpawn
         temp = chessBoard.getBPawnMoves(from);
      } else {
         temp = chessBoard.getAttackBoard(from);
      }

      int to = MoveFunctions.getTo(move);
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
		//long oldHash = chessBoard.hashValue;
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
      int index = HashTable.hasHash(key);
	   hashAttempts++;
      if (index != 1) {
         HashTable.setNew(key, index, ancient);
	    hashHits++;
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
                  /*if(hVal == 0 )
						{
							 int mv = HashTable.getMove(key, index);
							 int reps = chessBoard.MakeMove(mv, true);
							 chessBoard.UnMake(mv, true);
							 if(reps >= 2)
							 {
								 SavePV(TT_PV);
								 return 0;
							 }
						}
						else*/ if (hVal > alpha && hVal < beta) {
                     SavePV(TT_PV);
                     nodes++;
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
               if (hVal == -20000)
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
       if (!razored && !bFutilityPrune && !isInCheck && !nMove && nullFail != 1 
			&& ( (chessBoard.pieceTotals[5] + chessBoard.pieceTotals[11]) != (chessBoard.wPieceNo + chessBoard.bPieceNo -2)) && chessBoard.getMinNumberOfPieces() > 1) {

			chessBoard.SwitchTurn();
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
         chessBoard.SwitchTurn();
         if (value >= beta) {
            if (!stop) {
               HashTable.addHash(key, 0, value, depth, HASH_BETA, 0, ancient);
            }
            return value;
         }
         if (value <= -19000) {
            nullFail = 1;
         }
      }
      //boolean isRootFirstMove = false;
      //if(alpha == -BETA_START) {
      //   isRootFirstMove = true;
     // }


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

      boolean drawPV = false;
   
		boolean testCheck = !isInCheck;
		while (state != SEARCH_END) {

         switch (state) {
            case (SEARCH_HASH):
               testCheck = false;
					index = HashTable.hasHash(key);
               if (index != 1) {
				  theMove = HashTable.getMove(key, index);
                  bestFullMove = theMove;
                  if (theMove != 0) {// && verifyHash(side, theMove)) {
                     hashArr[hashCount++] = theMove;
                  }
                  if (index == 0) {
                     index = HashTable.hasSecondHash(key);
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
               if (hashCount == 0 && depth > 3 && !isInCheck) {
                  thisDepth--;
                  Max(side, depth - 2, alpha, beta, true, false, false, true);
                  index = HashTable.hasHash(key);
                  if (index != 1) {
                     hashArr[0] = HashTable.getMove(key, index);
                     if (hashArr[0] != 0 ) { // && verifyHash(side, hashArr[0])) {
                        bestFullMove = hashArr[0];
                        hashCount++;
                     }
                  }
                  if (hashCount == 0 && index == 0) {
                     index = HashTable.hasSecondHash(key);
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
               testCheck = !isInCheck;
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
               testCheck = !isInCheck;
					index = getCaptures(side, moveArr);
               capIndex = index;
               badCapIndex = index - 1; 
               break;
            case (KILLER_MOVES):
               testCheck = !isInCheck;
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
               testCheck = !isInCheck;
					index = getMoves(side, moveArr, capIndex);
               endIndex = capIndex;
               break;
            case (BAD_CAPS):
               testCheck = !isInCheck;
					index = capIndex;
               endIndex = badCapIndex + 1;
               break;
         }
         for (int i = index - 1; i >= endIndex; i--) {

            /*if(state == SEARCH_HASH) {
            if(!verifyHash(side,moveArr[i])) {
               System.out.println("info string crap move");
					continue;
            }
            }*/

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
					int type = MoveFunctions.moveType(theMove);

					if( type < Global.PROMO_Q)
					{
						if (Global.values[chessBoard.piece_in_square[to]] < Global.values[chessBoard.piece_in_square[from]]
						&& (SEE.GetSEE2(side, to, from, type, 0)) < 0) {
								moveArr[badCapIndex--] = theMove;
								continue;
						}
					}
					else if(type != Global.EN_PASSANT_CAP && (SEE.GetSEE2(side, to, from, type, 0)) < 0)
					{
						moveArr[badCapIndex--] = theMove;
						continue;
					}
            }
            extendAmount = 0;

            /** make the move */
            boolean draw = false;

            int reps = chessBoard.MakeMove(theMove, true);

				if(testCheck && inCheck(side))
				{
					chessBoard.UnMake(theMove, true);			//avoid leaving king in check
					continue;
				}
				else if( reps >= 2)
				{
					draw = true;
					oneLegal = true;
					value = 0;
				}
				else
				{
					/*if(!testCheck && inCheck(side))
					{
						System.out.println("probs");
						for(int j=0; j<64; j++)
						{
							System.out.print(chessBoard.piece_in_square[j]+" ");
							if((j & 7) == 7)
								System.out.println();
						}
					}*/

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
               if ((chessBoard.getTotalValue() < Global.totalValue * 0.4) && !checkingMove && !isInCheck && ((piece % 6) == 5 && ((to >> 3) == 6 || (to >> 3) == 1))) {	//extention for close to promotion
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
                       && !isInCheck && !checkingMove) {// (MoveFunctions.moveType(theMove) == Global.ORDINARY_MOVE || MoveFunctions.moveType(theMove) > Global.DOUBLE_PAWN_WHITE)) {
                  extendAmount = -1;
                  lmr = true;
               }
               int nextDepth = depth - 1 + extendAmount;
               if (moveCount == 0) {
                  if (nextDepth > 0) {
                     value = -Max(-side, depth - 1 + extendAmount, -beta, -alpha, nMove, checkingMove, isInCheck | pawnExtension, false);
                  } else {
                     value = -Quies(-side, 0, -beta, -alpha);
                  }
                  thisDepth--;
               } else {
                  if (nextDepth > 0) {
                     value = -Max(-side, depth - 1 + extendAmount, -alpha - 1, -alpha, nMove, checkingMove, isInCheck | pawnExtension, false);
                  } else {
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
              // if(isRootFirstMove && moveCount > 4) {
              //    levelOneValue = -value;
              // }

               if (value > alpha) {
                  if (value >= beta) {
                     HashTable.addHash(key, theMove, value, depth, HASH_BETA, nullFail, ancient);
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
                     lengthPV[thisDepth-1] = (byte)(thisDepth+1);
                  } else {
                     drawPV = false;
                  }
                  hType = HASH_EXACT;
                  alpha = value;
               }
               bMove = value;
				}
            moveCount++;
         }  //end for
         state++;
      }  // end while
      
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
         HashTable.addHash(key, bestFullMove, -20000, depth, hType, nullFail, ancient);
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
      HashTable.addHash(key, bestFullMove, bMove, depth, hType, nullFail, ancient);

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
      long testValue = 0;
      boolean isInCheck = false;
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
         //bValue = -20000 + thisDepth;
      } else {
			/*value = Evaluation2.getEval(side, -1000, 1000);
			int material = chessBoard.GetRawMaterialScore();
			Board.getInstance().FlipPosition();
			int value2 = Evaluation2.getEval(-side, -1000, 1000);
			int material2 = chessBoard.GetRawMaterialScore();
			if(material == -material2 && value != value2)
			{
				
				System.out.println("info string here");
				//System.out.println("value2 is "+value2);
				//System.out.println("material2 is "+material2);
				//Evaluation2.printEvalTerms();
				//System.out.println("value is "+value);
				//System.out.println("material is "+material);
				value2 = Evaluation2.getEval(-side, -1000, 1000);
				Evaluation2.printEvalTerms();

				Board.getInstance().FlipPosition();
				value2 = Evaluation2.getEval(side, -1000, 1000);
				Evaluation2.printEvalTerms();
			}
			else
			{
				Board.getInstance().FlipPosition();
			}*/
			value = Evaluation2.getEval(side, (int)alpha, (int)beta);
			
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

         int to = MoveFunctions.getTo(capArr[i]);
         int from = MoveFunctions.getFrom(capArr[i]);
			int neededScore = (int)Math.max(0, (alpha - testValue));
			if (!isInCheck) {
				int type = MoveFunctions.moveType(capArr[i]);
				
				if( type < Global.PROMO_Q)
				{
					if ((chessBoard.piece_in_square[to] - Global.values[chessBoard.piece_in_square[from]]) < neededScore
					&& (SEE.GetSEE2(side, to, from, MoveFunctions.moveType(capArr[i]), neededScore)) < neededScore) {

						bValue = alpha;
						continue;
					}
				}
				else if( (SEE.GetSEE2(side, to, from, MoveFunctions.moveType(capArr[i]), neededScore)) < neededScore)
				{
					bValue = alpha;
					continue;
				}
			}
			
         chessBoard.MakeMove(capArr[i], false);	

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

         if (!inCheck && inCheck(side)) {
            chessBoard.UnMake(moveArr[i], false);
            continue;
         }
         String output = HistoryWriter.getUCIMove(to, from, piece);
         System.out.print(output);

	    if (reps == 3) {
             System.out.println("1");
		   continue;
         } else {
            Perft(-side, depth - 1);
         }
         chessBoard.UnMake(moveArr[i], false);
         //String output = HistoryWriter.getUCIMove(to, from, piece);
         //System.out.print(output);
	   
	    System.out.println(" " + perft);
      }
   }

   /**
    * Method PerftDebug
    *
    * recursive function to generate and return the number of moves searched
	 * -checks symmetry of evaluation and may test trans table
    *
    * @param int side - the side moving
    * @param int depth - the depth for the perft test
    *
    */
   private void PerftDebug(int side, int depth) {
      if (depth == 0) {
         perft++;
         return;
      }
	 int key = (int) (chessBoard.hashValue % Global.HASHSIZE);

	 int value = Evaluation2.getEval(side, -2000, 2000);
	 Board.getInstance().FlipPosition();
	 int value2 = Evaluation2.getEval(-side, -2000, 2000);

	 if(value != value2)
	 {
		 System.out.println("value2 is "+value2);
		 Evaluation2.printEvalTerms();
		 System.out.println("value is "+value);
		 Board.getInstance().FlipPosition();
		 value2 = Evaluation2.getEval(side, -2000, 2000);
		 Evaluation2.printEvalTerms();
	 }
	 else
	 {
		Board.getInstance().FlipPosition();
	 }

	 if(PERFT_TRANSTABLE) {
		 int hashIndex = HashTable.hasHash(key);
		 if (hashIndex != 1 ) {
			  if(HashTable.getDepth(key,  hashIndex) == depth) {
				   perft += (long)HashTable.getMove(key,  hashIndex);
				   return;
			  }
			  hashIndex = HashTable.hasSecondHash(key);
			  if (hashIndex != 1 && HashTable.getDepth(key,  hashIndex) == depth) {
				   perft += (long)HashTable.getMove(key,  hashIndex);
				   return;
			  }
		 }
	 }
	 long perftBefore = perft;
	 boolean inCheck = inCheck(side) ? true : false;
      int index;
      int[] moveArr = new int[128];
      if (!inCheck) {
         index = getCaptures(side, moveArr);
         index = getMoves(side, moveArr, index);
      } else {
         index = getCheckEscapes(side, moveArr);
      }
      for (int i = index - 1; i >= 0; i--) {
         chessBoard.MakeMove(moveArr[i], true);
         if (!inCheck && inCheck(side)) {
            chessBoard.UnMake(moveArr[i], true);
            continue;
         }
         PerftDebug(-side, depth - 1);
         chessBoard.UnMake(moveArr[i], true);
	 }
	 if(PERFT_TRANSTABLE)
		HashTable.addHash(key, (int)(perft - perftBefore) , 0, depth, 0, 0, 0);
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
		if (depth == 0) {
			perft++;
			return;
		}

		boolean inCheck = inCheck(side) ? true : false;
		int index;
		int[] moveArr = new int[128];
		if (!inCheck) {
			index = getCaptures(side, moveArr);
			index = getMoves(side, moveArr, index);
		} else {
			index = getCheckEscapes(side, moveArr);
		}
		for (int i = index - 1; i >= 0; i--) {
			chessBoard.MakeMove(moveArr[i], true);
			if (!inCheck && inCheck(side)) {
				chessBoard.UnMake(moveArr[i], true);
				continue;
			}
			Perft(-side, depth - 1);
			chessBoard.UnMake(moveArr[i], true);
		}
	}

}
