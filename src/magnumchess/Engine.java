package magnumchess;
import java.util.Random;
//import java.io.*;
/**
 * Engine.java
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

 * Engine.java
 * This class contains all the search functionality
 * -alpha beta - pvs search
 * -q search
 * -move generation functions - which uses chessBoard.java's individual piece move functions
 * -divide and perft functions for move generation testing and verification
 *
 * @version 	4.00 March 2013
 * @author 	Eric Stock
 */
public final class Engine {

   /** used similar to a C preprocessor define to instruct the move generation to properly generate all moves required for perft scoring */
    private static final boolean PERFT_ENABLED = false;
   
    /** used similar to a C preprocessor define to instruct perft to use the transposition table */
    /** useful for testing transposition table */
    private static final boolean PERFT_TRANSTABLE = true;

    /** chessBoard object from singleton class Board represents chess board and associated datastructures */
    private Board chessBoard;
    /** counter for positions evaluated */
    private static long nodes;			//counter for evaluated positions
    private static long hashHits;		//counter hash table hits
    private static long hashAttempts;	//counter for hash table attempts
    private static long nps;         //nodes per second

    /** 2D 64X64 arrays for history heuristic move ordering enhancement */
    private static final short[][][] Hist = new short[2][6][64];
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
    private static final int QUIES_CAPS = 2;
    private static final int QUIES_CHECKING = 5;
    private static final int QUIES_IN_CHECK = 3;
    private static final int QUIES_END = 4;
    
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
    private static final int ALPHA_START = -Global.MATE_SCORE;
    private static final int BETA_START = Global.MATE_SCORE;
    private static final int VALUE_START = -Global.MATE_SCORE;
    /** variable used to track how deep in the search we are */
    private static byte thisDepth;
    /** boolean which represents if we are using infinite time control */
    private static boolean infiniteTimeControl = false;
    /** used to track the age of hash table entries */
    private int ancient;
    /** count of moves generated when debugging move gen using perft command */
    private long perft = 0;
    /** "triangular array to store the PV **/
    private static final int[][] PV = new int[128][128];
    /** array to store the length of the PV **/
    private static final byte[] lengthPV = new byte[128];

    private static Random rand = new Random(9L);

   //public BufferedWriter buffWriter;
   /**
    * Constructor Engine
    *
    * grabs a reference to the instantiated Board object
    *
    */
    public Engine() {
        chessBoard = Board.getInstance();
      
       /*File f = new File("debug");
        
        try {
            f.createNewFile();
            FileWriter fileWriter;
            fileWriter= new FileWriter(f);
            buffWriter = new BufferedWriter(fileWriter);  
        }
        catch (Exception ex2){};*/
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

    public int RandomSearch() 
    {
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

    public int SwitchSide(int side) 
    {
        return side ^ 1;
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
    public String search(int time, int maxTime, int searchDepth, boolean inf) 
    {
        // side moving
        int theSide = chessBoard.getTurn();

        // array of moves
        int[] moveArr = new int[128];

        int alpha, beta;

        // temporary varible to store the value returned from a search
        int value;

        // flag indicating if we have an exact score and thus can now scout all remaining moves
        boolean isExact = false;

        //flag indicating if the search has at least found a move which it can return if the search is terminated
        boolean hasValue = false;

        //the best move found in the search
        int bestMove = 0;

        //the number of moves to be searched
        int numberOfMoves;

        //the score of the best move searched
        int bestValue = 0;

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

        int partialDepthExtension = Global.HALF_EXTENSION;         //added to depth to ensure certain partial extensions will trigger the first time
        nextTimeCheck = Math.min(1000, time * nps);

        //System.out.println("info string time is "+time);
        //System.out.println("info string max root get a score time is "+maximumRootGetAScoreTime);
        //System.out.println("info string max time is "+maxTime);


        // ancient node value betweeen 0 and 7
        //ancient = (chessBoard.getCount() / 2) >> 3;
        ancient = chessBoard.getCount() % 8;
        infiniteTimeControl = inf;

        /** here we prepare the history arrays for the next search */
        if ((chessBoard.getCount() / 2 >> 3) == 7) {
           for (int i = 0; i < 6; i++) {
              for (int j = 0; j < 64; j++) {
                 Hist[Global.COLOUR_WHITE][i][j] = 0;
                 Hist[Global.COLOUR_BLACK][i][j] = 0;
              }
           }
        } else {
           for (int i = 0; i < 6; i++) {
              for (int j = 0; j < 64; j++) {
                 Hist[Global.COLOUR_WHITE][i][j] /= 10;
                 Hist[Global.COLOUR_BLACK][i][j] /= 10;
              }
           }
        }

        /** here the killer moves are cleared out before the next search */
        for (int j = 0; j < 25; j++) {
           killerMoves[j][0] = 0;
           killerMoves[j][1] = 0;
        }

        nodes = 0;
        hashAttempts = 0;
        hashHits = 0;
        /** collect the root moves */

        if (!inCheck(theSide)) {
            numberOfMoves = GetAllMoves(theSide, moveArr);
        } else {
            numberOfMoves = getCheckEscapes(theSide, moveArr); 
            if( numberOfMoves == 0)
                return "info string no legal move";
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
            } else if (isStaleMate( SwitchSide(theSide) )) {
                //System.out.println("stalemate move is " + HistoryWriter.getUCIMove((tempMove >> 6) & 63, tempMove & 63, (tempMove >> 12) & 15));
                compareArray[i] = 1;
            } else {
                compareArray[i] = (int) -Quies(SwitchSide(theSide), 1, -BETA_START, -ALPHA_START);
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
                        HashTable.addHash(key, mv, -Global.MATE_SCORE, insertDepth++, HASH_BETA, 0, ancient, chessBoard.hashValue);
                        chessBoard.MakeMove(mv, false);
                    }
                }
            }
            
            alpha = ALPHA_START;
            beta = BETA_START;

            int tempBestMove = Integer.MIN_VALUE;
            isExact = false;
            thisDepth = 0;

            for (int i = numberOfMoves - 1; i >= 0; i--) {
                int tempMove = moveArr[i];
                int nextDepth = depth * Global.PLY - Global.PLY + partialDepthExtension;
                if ((compareArray[i] & 1) != 0) {
                    //System.out.println("Draw move 2 is " + HistoryWriter.getUCIMove((tempMove >> 6) & 63, tempMove & 63, (tempMove >> 12) & 15));
                    value = 0;

                    if(!isExact && value < bestValue - 30 && !stop) {
                        GotoTimeState(TIME_STATE_FAIL_LOW);
                    }
                } else if (!isExact) {
                    chessBoard.MakeMove(tempMove, true);
                    value = -Max(SwitchSide(theSide), nextDepth, -beta, -alpha, false, inCheck(SwitchSide(theSide)), false, false);
                    chessBoard.UnMake(tempMove, true);
                    thisDepth--;
                    //check for a first move which goes LOW
                    if(value < bestValue - 30 && value < 10000L && !stop) {
                        GotoTimeState(TIME_STATE_FAIL_LOW);
                    }
                } else {
                    chessBoard.MakeMove(tempMove, true);
                    value = -Max(SwitchSide(theSide), nextDepth, -alpha - 1, -alpha, false, inCheck(SwitchSide(theSide)), false, false);
                    thisDepth--;

                    if (value > alpha && !stop) {
                        //check for a move which goes HIGH
                        GotoTimeState(TIME_STATE_FAIL_HIGH);
                        value = -Max(SwitchSide(theSide), nextDepth, -beta, -alpha, false, inCheck(SwitchSide(theSide)), false, false);
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
            String pv = HistoryWriter.getUCIMove(bestMove);
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
                    pv = pv.concat(HistoryWriter.getUCIMove(PV[0][i]));
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

            if (bestValue >= (Global.MATE_SCORE - Global.MAX_DEPTH)) {			//this is a winning mate score
                long mate = (Global.MATE_SCORE - bestValue) / 2;
                System.out.println("info depth " + depth + " score mate " + mate + " nodes " + nodes + " nps " + nps + " pv " + pv);
            } else if (bestValue <= -(Global.MATE_SCORE - Global.MAX_DEPTH)) {  //losing mate score
                long mate = (-Global.MATE_SCORE - bestValue) / 2;
                System.out.println("info depth " + depth + " score mate " + mate + " nodes " + nodes + " nps " + nps + " pv " + pv);
            } else {
                System.out.println("info depth " + depth + " score cp " + bestValue + " nodes " + nodes + " nps " + nps + " pv " + pv);
            }
            //System.out.println("info string hashHits are "+hashHits + "info string hashAttempts are "+hashAttempts+"info string hash hit percent is "+(double)hashHits/(double)hashAttempts * 100.0);
            //System.out.println("info string hash size is "+Global.HASHSIZE+" number of used entries is "+HashTable.getCount()+" percentage of used entries is "+(double)HashTable.getCount()/(double)Global.HASHSIZE * 100.0);

            sortMoves(0, numberOfMoves, moveArr, compareArray);
        }
        /*try {
           buffWriter.close();
        } catch (Exception e) {};*/
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
        return chessBoard.isAttacked(side, chessBoard.pieceList[Global.PIECE_KING + side*6][0]);  
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
     * Method getChecingMoves
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
    public int getCheckingMoves(int side, int[] Moves, int start) 
    {
        long moves, doubleMoves;
        int index = start;
        long pieces = chessBoard.pieceBits[side][Global.PIECE_PAWN];
        long enemyKing = chessBoard.pieceBits[side^1][Global.PIECE_KING];
        if( side == Global.COLOUR_WHITE)
        {
            moves = pieces << 8 & ~chessBoard.bitboard & ~Global.rankMasks[7];
            doubleMoves = moves << 8 & Global.rankMasks[3] & ~chessBoard.bitboard;
        }
        else
        {
            moves = pieces >> 8 & ~chessBoard.bitboard & ~Global.rankMasks[0];
            doubleMoves = moves >> 8 & Global.rankMasks[4] & ~chessBoard.bitboard;
        }

        while (moves != 0) 
        {
            long toBit = moves & -moves;
            moves ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            if( (chessBoard.getPawnAttack(side, to) & enemyKing ) != 0 )
            {
                 int from = to + Global.behindRank[side];
                 moveOrder[index] = Hist[side][5][to];
                 Moves[index++] = MoveFunctions.makeMove(to, from, Global.ORDINARY_MOVE);
            }
        }
        while (doubleMoves != 0) 
        {
            long toBit = doubleMoves & -doubleMoves;
            doubleMoves ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            if( (chessBoard.getPawnAttack(side, to) & enemyKing ) != 0 )
            {
                int from = to + Global.behindRank[side] * 2;
                moveOrder[index] = Hist[side][5][to];
                Moves[index++] = MoveFunctions.makeMove(to, from, Global.DOUBLE_PAWN);
            }
        }

        for(int j=0; j < chessBoard.pieceTotals[Global.pieceAdd[side]]; j++)
        {
            int from = chessBoard.pieceList[Global.pieceAdd[side]][j];
            long toSquares = chessBoard.getAttackBoard(from);
            toSquares &= ~chessBoard.bitboard;
            while (toSquares != 0) {
                long toBit = toSquares & -toSquares;
                toSquares ^= toBit;
                int to = Long.numberOfTrailingZeros(toBit);
                if( (chessBoard.getMagicRookMoves(to) & enemyKing ) != 0 )
                {
                    moveOrder[index] = Hist[side][0][to];
                    if((from == (side * 56)) && (chessBoard.castleFlag[side] & Global.LONG_CASTLE) != 0)
                        Moves[index++] = MoveFunctions.makeMove(to, from, Global.MOVE_ROOK_LOSE_CASTLE);
                    else if(from == (7 + (side * 56)) && (chessBoard.castleFlag[side] & Global.SHORT_CASTLE) != 0)
                        Moves[index++] = MoveFunctions.makeMove(to, from, Global.MOVE_ROOK_LOSE_CASTLE);
                    else
                        Moves[index++] = MoveFunctions.makeMove(to, from, Global.ORDINARY_MOVE);
                }
            }
        }

        for(int i=2; i<4; i++)
        {
            int pType = i + Global.pieceAdd[side];
            for(int j=0; j < chessBoard.pieceTotals[pType]; j++)
            {
                int from = chessBoard.pieceList[pType][j];
                long toSquares = chessBoard.getAttackBoard(from);
                toSquares &= ~chessBoard.bitboard;
                while (toSquares != 0) {
                    long toBit = toSquares & -toSquares;
                    toSquares ^= toBit;
                    int to = Long.numberOfTrailingZeros(toBit);
                    if( i == 2)
                    {
                        if( (chessBoard.getMagicBishopMoves(to) & enemyKing ) != 0 )
                        {
                            moveOrder[index] = Hist[side][i][to];
                            Moves[index++] = MoveFunctions.makeMove(to, from, Global.ORDINARY_MOVE);
                        }
                    }
                    else
                    {
                        if( (chessBoard.getQueenMoves(to) & enemyKing ) != 0 )
                        {
                            moveOrder[index] = Hist[side][i][to];
                            Moves[index++] = MoveFunctions.makeMove(to, from, Global.ORDINARY_MOVE);
                        }
                    }
                }
            }
        }
        sortMoves(start, index, Moves);
        return index;
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
    public int getMoves(int side, int[] Moves, int start) 
    {
        long moves, doubleMoves;
        int index = start;
        long pieces = chessBoard.pieceBits[side][Global.PIECE_PAWN];
        if( side == Global.COLOUR_WHITE)
        {
            moves = pieces << 8 & ~chessBoard.bitboard & ~Global.rankMasks[7];
            doubleMoves = moves << 8 & Global.rankMasks[3] & ~chessBoard.bitboard;
        }
        else
        {
            moves = pieces >> 8 & ~chessBoard.bitboard & ~Global.rankMasks[0];
            doubleMoves = moves >> 8 & Global.rankMasks[4] & ~chessBoard.bitboard;
        }

        while (moves != 0) 
        {
            long toBit = moves & -moves;
            moves ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            int from = to + Global.behindRank[side];
            moveOrder[index] = Hist[side][5][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, Global.ORDINARY_MOVE);
        }

        while (doubleMoves != 0) 
        {
            long toBit = doubleMoves & -doubleMoves;
            doubleMoves ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            int from = to + Global.behindRank[side] * 2;
            moveOrder[index] = Hist[side][5][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, Global.DOUBLE_PAWN);
        }

        int piece = 4 + Global.pieceAdd[side];
        int from = chessBoard.pieceList[piece][0];

        if (chessBoard.castleFlag[side] > Global.CASTLED) 
        {
            long Temp = chessBoard.getKingCastle(((int)(chessBoard.bitboard >>> (56 * side)) & 255));
            if(chessBoard.castleFlag[side] != Global.SHORT_CASTLE && ((Temp & Global.set_Mask[ 2 ]) != 0 ) ) {
                if( !chessBoard.isAttacked(side, 2 + (56 * side)) && !chessBoard.isAttacked(side, 3 + (56 * side)) ) {
                   moveOrder[index] = Hist[side][4][2 + (56 * side)];
                   Moves[index++] = MoveFunctions.makeMove(2 + (56 * side), from, Global.LONG_CASTLE);
                }
            }
            if(chessBoard.castleFlag[side] != Global.LONG_CASTLE && ((Temp & Global.set_Mask[ 6 ])!=0 ) ) {		
                if( !chessBoard.isAttacked(side, 5 + (56 * side)) && !chessBoard.isAttacked(side, 6 + (56 * side)) ) {
                   moveOrder[index] = Hist[side][4][6 + (56 * side)];
                   Moves[index++] = MoveFunctions.makeMove(6 + (56 * side), from, Global.SHORT_CASTLE);
                }
            }
        }

        int kingType = chessBoard.castleFlag[side] > Global.CASTLED ? Global.MOVE_KING_LOSE_CASTLE : Global.ORDINARY_MOVE;
        long toSquares = chessBoard.getKingMoves(from);
        toSquares &= ~chessBoard.bitboard;
        while (toSquares != 0) {
            long toBit = toSquares & -toSquares;
            toSquares ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            moveOrder[index] = Hist[side][4][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, kingType);
        }

        for(int j=0; j < chessBoard.pieceTotals[Global.pieceAdd[side]]; j++)
        {
            from = chessBoard.pieceList[Global.pieceAdd[side]][j];
            toSquares = chessBoard.getAttackBoard(from);
            toSquares &= ~chessBoard.bitboard;
            while (toSquares != 0) {
                long toBit = toSquares & -toSquares;
                toSquares ^= toBit;
                int to = Long.numberOfTrailingZeros(toBit);
                moveOrder[index] = Hist[side][0][to];
                if((from == (side * 56)) && (chessBoard.castleFlag[side] & Global.LONG_CASTLE) != 0)
                    Moves[index++] = MoveFunctions.makeMove(to, from, Global.MOVE_ROOK_LOSE_CASTLE);
                else if(from == (7 + (side * 56)) && (chessBoard.castleFlag[side] & Global.SHORT_CASTLE) != 0)
                    Moves[index++] = MoveFunctions.makeMove(to, from, Global.MOVE_ROOK_LOSE_CASTLE);
                else
                    Moves[index++] = MoveFunctions.makeMove(to, from, Global.ORDINARY_MOVE);
            }
        }

        for(int i=1; i<4; i++)
        {
            int pType = i + Global.pieceAdd[side];
            for(int j=0; j < chessBoard.pieceTotals[pType]; j++)
            {
                from = chessBoard.pieceList[pType][j];
                toSquares = chessBoard.getAttackBoard(from);
                toSquares &= ~chessBoard.bitboard;
                while (toSquares != 0) {
                    long toBit = toSquares & -toSquares;
                    toSquares ^= toBit;
                    int to = Long.numberOfTrailingZeros(toBit);
                    moveOrder[index] = Hist[side][i][to];
                    Moves[index++] = MoveFunctions.makeMove(to, from, Global.ORDINARY_MOVE);
                }
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

    private static final void sortMoves(int start, int noMoves, int[] Moves) 
    {
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
        int enemy = side ^ 1;
        int type;
        long enemies = chessBoard.pieceBits[enemy][Global.PIECE_ALL];
        boolean attackCastle = chessBoard.castleFlag[enemy] > Global.CASTLED ? true : false;

        for(int i=1; i<5; i++)
        {
            if(i == 4 && chessBoard.castleFlag[side] > Global.NO_CASTLE)
                type = Global.MOVE_KING_LOSE_CASTLE;
            else
                type = Global.ORDINARY_CAPTURE;
            int pType = i + Global.pieceAdd[side];
            for(int j=0; j < chessBoard.pieceTotals[pType]; j++)
            {
                int from = chessBoard.pieceList[pType][j];
                long toSquares = chessBoard.getAttackBoard(from);
                toSquares &= enemies;
                while (toSquares != 0) {
                    long toBit = toSquares & -toSquares;
                    toSquares ^= toBit;
                    int to = Long.numberOfTrailingZeros(toBit);
                    if(attackCastle && (to == 56 - (56 * side) || to == 63 - (56 * side)))
                        type = Global.CAPTURE_ROOK_LOSE_CASTLE;
                    moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + Global.invValues[i];
                    Captures[index++] = MoveFunctions.makeMove(to, from, type);
                }
            }
        }
        boolean ourCastle = chessBoard.castleFlag[side] > Global.CASTLED ? true : false;
        for(int j=0; j < chessBoard.pieceTotals[Global.pieceAdd[side]]; j++)
        {
            int pType = Global.pieceAdd[side];
            int from = chessBoard.pieceList[pType][j];
            long toSquares = chessBoard.getAttackBoard(from);
            toSquares &= enemies;
            while (toSquares != 0) {
                long toBit = toSquares & -toSquares;
                toSquares ^= toBit;
                int to = Long.numberOfTrailingZeros(toBit);
                if(attackCastle && (to == 56 - (56 * side) || to == 63 - (56 * side)))
                    type = Global.CAPTURE_ROOK_LOSE_CASTLE;
                else if(ourCastle)
                    type = Global.MOVE_ROOK_LOSE_CASTLE;
                else
                    type = Global.ORDINARY_CAPTURE;
                moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + Global.invValues[0];
                Captures[index++] = MoveFunctions.makeMove(to, from, type);
            }
        } 
        long pieces = chessBoard.pieceBits[side][Global.PIECE_PAWN];
      
        long lAttack, rAttack, promo;
        int passant;
        if(side == Global.COLOUR_WHITE)
        {
            passant = chessBoard.getPassant(Global.COLOUR_BLACK);
            enemies ^=  Global.set_Mask[passant];
            lAttack = (pieces << 7) & enemies & ~Global.fileMasks[7];
            rAttack = (pieces << 9) & enemies & ~Global.fileMasks[0];
            enemies ^=  Global.set_Mask[chessBoard.getPassant(Global.COLOUR_BLACK)];
            promo = pieces & Global.rankMasks[6];
            if (promo != 0) {
                promo <<= 8;
                promo &= ~chessBoard.bitboard;
            }
        }
        else
        {
            passant = chessBoard.getPassant(Global.COLOUR_WHITE);
            enemies ^=  Global.set_Mask[passant];
            lAttack = pieces >> 9 & enemies & ~Global.fileMasks[7];
            rAttack = pieces >> 7 & enemies & ~Global.fileMasks[0];
            enemies ^=  Global.set_Mask[chessBoard.getPassant(Global.COLOUR_WHITE)];
            promo = pieces & Global.rankMasks[1];
            if (promo != 0) {
                promo >>= 8;
                promo &= ~chessBoard.bitboard;
            }
        }
      
        while (lAttack != 0) {
            long toBit = lAttack & -lAttack;
            lAttack ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            int from = side == Global.COLOUR_WHITE ? to - 7 : to + 9;
            type = Global.ORDINARY_CAPTURE;
            if (to == passant) {
                moveOrder[index] = Global.values[5] + 4;
                type = Global.EN_PASSANT_CAP;
            } else if ((to >> 3) == 7 || (to >> 3) == 0) {
                if (PERFT_ENABLED) {
                    moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_B);
                    moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_N);
                    moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_R);
                }
                moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 700;
                type = Global.PROMO_Q;
            } else {
                moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 4;
            }
            Captures[index++] = MoveFunctions.makeMove(to, from, type);
        }

        while (rAttack != 0) {
            long toBit = rAttack & -rAttack;
            rAttack ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            int from = side == Global.COLOUR_WHITE ? to - 9 : to + 7;
            type = Global.ORDINARY_CAPTURE;
            if (to == passant) {
                moveOrder[index] = Global.values[5] + 4;
                type = Global.EN_PASSANT_CAP;
            } else if ((to >> 3) == 7 || (to >> 3) == 0) {
                if (PERFT_ENABLED) {
                    moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_B);
                    moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_N);
                    moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_R);
                }
                moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 700;
                type = Global.PROMO_Q;
            } else {
                moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + 4;
            }
            Captures[index++] = MoveFunctions.makeMove(to, from, type);
        }
      
        while (promo != 0) {
            int to = Long.numberOfTrailingZeros(promo);
            promo ^= Global.set_Mask[to];
            int from =  side == Global.COLOUR_WHITE ? to - 8 : to + 8; 
            if (PERFT_ENABLED) {
                moveOrder[index] = 0;
                Captures[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_B);
                moveOrder[index] = 0;
                Captures[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_N);
                moveOrder[index] = 0;
                Captures[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_R);
            }
            moveOrder[index] = 700;
            Captures[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_Q);
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
        
        int kingPos = chessBoard.pieceList[4 + side * 6][0];
        int index = 0;
        //get the king moves
        long toSquares = chessBoard.getAttackBoard(kingPos) & ~chessBoard.pieceBits[side][Global.PIECE_ALL];
        
        //find all slide diagonal attackers
        //use their position to mask off some of the kings moves as we know they would lead to check
        long attackers = chessBoard.getMagicBishopMoves(kingPos) & ( chessBoard.pieceBits[side^1][Global.PIECE_BISHOP] | chessBoard.pieceBits[side^1][Global.PIECE_QUEEN] );
        long attackBits = attackers;
        int noAttacks = 0;
        while (attackers != 0) {
            int attackFrom = Long.numberOfTrailingZeros(attackers);
            attackers ^= Global.set_Mask[attackFrom];
            toSquares &= ~Global.bishopMasks[attackFrom];
            noAttacks++;
        }
        
        //find all slide non diagonal attackers
        //use their position to mask off some of the kings moves as we know they would lead to check
        attackers = chessBoard.getMagicRookMoves(kingPos) & ( chessBoard.pieceBits[side^1][Global.PIECE_ROOK] | chessBoard.pieceBits[side^1][Global.PIECE_QUEEN] );
        attackBits |= attackers;
        while (attackers != 0) {
            int attackFrom = Long.numberOfTrailingZeros(attackers);
            attackers ^= Global.set_Mask[attackFrom];
            toSquares &= ~Global.rookMasks[attackFrom];
            noAttacks++;
        }

        //generate the king's moves
        while (toSquares != 0) {
            int attackTo = Long.numberOfTrailingZeros(toSquares);
            toSquares ^= Global.set_Mask[attackTo];
            
            int capture = chessBoard.piece_in_square[attackTo];
            int value = 100;
            int type = Global.ORDINARY_MOVE;
            if (capture != -1) {
                type = Global.ORDINARY_CAPTURE;
                value = 1000 + Global.values[chessBoard.piece_in_square[attackTo]];
            }
            moveOrder[index] = value;
            if( chessBoard.castleFlag[side] > Global.CASTLED ) {
                escapes[index++] = MoveFunctions.makeMove(attackTo, kingPos, Global.MOVE_KING_LOSE_CASTLE);
            }
            else {
                escapes[index++] = MoveFunctions.makeMove(attackTo, kingPos, type); 
            }     
        }
        
        if (noAttacks > 1) {
            sortCaps(0, index, escapes);
            return index;
        }
        
        //get the pawn and knight attackers
        attackBits |= chessBoard.getKnightMoves(kingPos) & chessBoard.pieceBits[side^1][Global.PIECE_KNIGHT];
        attackBits |= chessBoard.getPawnAttack(side, kingPos) & chessBoard.pieceBits[side^1][Global.PIECE_PAWN];
        
        if( Long.bitCount(attackBits) > 1 ) {
            sortCaps(0, index, escapes);
            return index;
        }
        
        //generate moves attacking the lone enemy attacker
        int attackTo = Long.numberOfTrailingZeros(attackBits);
        long friendAttackers = chessBoard.getAttack2(attackTo) & chessBoard.pieceBits[side][Global.PIECE_ALL] & ~chessBoard.pieceBits[side][Global.PIECE_KING];
        int capture = chessBoard.piece_in_square[attackTo];
        
        while (friendAttackers != 0) {
            int from = Long.numberOfTrailingZeros(friendAttackers);
            friendAttackers ^= Global.set_Mask[from];
            
            int type = Global.ORDINARY_CAPTURE;
            if (chessBoard.piece_in_square[from] % 6 == 5) {
                if ((attackTo >> 3) == 0 || (attackTo >> 3) == 7) {
                    if (PERFT_ENABLED) {
                        moveOrder[index] = 0;
                        escapes[index++] = MoveFunctions.makeMove(attackTo, from, Global.PROMO_N);
                        moveOrder[index] = 0;
                        escapes[index++] = MoveFunctions.makeMove(attackTo, from, Global.PROMO_R);
                        moveOrder[index] = 0;
                        escapes[index++] = MoveFunctions.makeMove(attackTo, from, Global.PROMO_B);
                    }
                    type = Global.PROMO_Q;
                     moveOrder[index] = 1000 + Global.values[Global.PIECE_QUEEN] + Global.values[chessBoard.piece_in_square[attackTo]];
                }
                else {
                     moveOrder[index] = 1000 + Global.values[chessBoard.piece_in_square[attackTo]] - Global.values[Global.PIECE_PAWN];
                }
            }
            else
            {
                moveOrder[index] = Global.values[chessBoard.piece_in_square[attackTo]] - Global.values[chessBoard.piece_in_square[from]];
                if(capture % 6 == 0 && chessBoard.castleFlag[side ^ 1] > Global.CASTLED ) {
                        type = Global.CAPTURE_ROOK_LOSE_CASTLE;
                }
                else if( chessBoard.piece_in_square[from] % 6 == 0 && chessBoard.castleFlag[side] > Global.CASTLED ) {
                        type = Global.MOVE_ROOK_LOSE_CASTLE;
                }
            }
            escapes[index++] = MoveFunctions.makeMove(attackTo, from, type);
        }

        if (chessBoard.piece_in_square[attackTo] % 6 == 5 && (attackTo + Global.forwardRank[side]) == chessBoard.getPassant(side^1)) 
        {
            long temp = chessBoard.getPawnAttack(side^1, attackTo + Global.forwardRank[side]) & chessBoard.pieceBits[side][Global.PIECE_PAWN];
            while (temp != 0) {
                int from = Long.numberOfTrailingZeros(temp);
                temp ^= Global.set_Mask[from];
                
                moveOrder[index] = 1000;
                escapes[index++] = MoveFunctions.makeMove(attackTo + Global.forwardRank[side], from, Global.EN_PASSANT_CAP);
            }
        }
        
        //if the attacker is a slide piece, generate moves to block the sliding attack
        long target = Global.mask_between[kingPos][attackTo];    
        
        if (!Global.slides[chessBoard.piece_in_square[attackTo]] && target != 0) {
            sortCaps(0, index, escapes);
            return index;
        }

        //lastly we need to generate moves to block a sliding attack
        while (target != 0) {
            int to = Long.numberOfTrailingZeros(target);
            target ^= Global.set_Mask[to];
            long friendMovers = chessBoard.getMovesTo(to) & chessBoard.pieceBits[side][Global.PIECE_ALL] & ~chessBoard.pieceBits[side][Global.PIECE_KING];
            while (friendMovers != 0) {
                int type = Global.ORDINARY_MOVE;
                int from = Long.numberOfTrailingZeros(friendMovers);
                friendMovers ^= Global.set_Mask[from];
                
                if( chessBoard.piece_in_square[from] % 6 == 0 && chessBoard.castleFlag[side] > Global.CASTLED ) {
                    type = Global.MOVE_ROOK_LOSE_CASTLE;
                }   
                moveOrder[index] =  -Global.values[chessBoard.piece_in_square[from]];
                
                if (chessBoard.piece_in_square[from] % 6 == 5) {
                    if (to < 8 || to > 55) {
                        if (PERFT_ENABLED) {
                            moveOrder[index] = -1000;
                            escapes[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_N);
                            moveOrder[index] = -1000;
                            escapes[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_R);
                            moveOrder[index] = -1000;
                            escapes[index++] = MoveFunctions.makeMove(to, from, Global.PROMO_B);
                        }
                        moveOrder[index] = -Global.values[Global.PIECE_QUEEN];
                        type = Global.PROMO_Q;
                    } 
                    else if(Math.abs(to - from) == 16)   {
                        type = Global.DOUBLE_PAWN;             
                    }
                } 
                escapes[index++] = MoveFunctions.makeMove(to, from, type);
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
        int piece = chessBoard.piece_in_square[from];
        
        if( piece == -1) {
            return false;
        }
        
        if (side == Global.COLOUR_WHITE && piece > 5) { //white moving
            System.out.println("info string bad hash move1 "+move+" hash is "+chessBoard.hashValue);
            System.out.println("info string piece is "+piece);
            System.out.println("info string to is "+to);
            System.out.println("info string from is "+from);
            System.out.println("info string type is "+MoveFunctions.moveType(move));

            return false;

        } else if (side == Global.COLOUR_BLACK && piece < 6) {
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
            temp = chessBoard.getPawnMoves(side, from);//, chessBoard.getPassantB());
        } else if (piece == 11) {
            temp = chessBoard.getPawnMoves(side, from);
        } else {
            temp = chessBoard.getAttackBoard(from);
        }
        if (piece < 6) {
            temp &= ~chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_ALL];
        } else {
            temp &= ~chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_ALL];
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
        int from = MoveFunctions.getFrom(move);
        int piece = move >> 16 & 15;

        if (side == Global.COLOUR_WHITE && piece > 5) //white moving
        {
            return false;
        } else if (side == Global.COLOUR_BLACK && piece < 6) {
            return false;
        }

        if (chessBoard.piece_in_square[from] != piece) {
            return false;
        }

        long temp;
        if (piece%6 == 5)
        { 
            temp = chessBoard.getPawnMovesNoAttack(side, from); 
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
    private int Max(int side, int depth, int alpha, int beta, boolean nMove, boolean isInCheck, boolean wasExtended, boolean iid) {
        /*try {
             buffWriter.write("max hash is"+chessBoard.hashValue+" "+"alpha "+alpha+" beta "+beta+" isInCheck "+isInCheck+" null is"+ nMove +" iid is "+iid+" extended is "+wasExtended+" \n");
        } catch (Exception ex2) {};
        */
        int effectiveDepth = depth / Global.PLY;
        
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
        
        //mate distance pruning
        int nalpha = Math.max( -Global.MATE_SCORE + thisDepth, alpha );
        int nbeta = Math.min( Global.MATE_SCORE - (thisDepth + 1), beta );
        if( nalpha >= nbeta ) {
            if (nalpha > alpha && nalpha < nbeta) {
                SavePV(TT_PV);
            }
            return nalpha;
        }
       
        boolean bNodePV = alpha < beta -1;
        
        /** score that a move must exceed to be searched during futility pruning */
        int scoreThreshold = chessBoard.GetMaterialScore(side);
       // int scoreThreshold = Evaluation2.getEval(side, -99999, 99999, thisDepth);
                        
        /** razoring code */
        boolean razored = false;
        if (!wasExtended && !isInCheck && effectiveDepth == 3) {
           if ((scoreThreshold + 900) <= alpha && chessBoard.getNumberOfPieces( SwitchSide(side) ) > 3) {
              razored = true;
              depth -= Global.PLY;
              --effectiveDepth;
           }
        }

        /** flag indicates it is worthless to try a null move search here */
        int nullFail = 0;
        int theMove = 0;


        /** hash table code - we are trying to cut the search based on what is stored */
        /** index represents the index of the move in the search and the index of a move in the hash table
         * this is an optimization to use 1 variable instead of 2
         */
        int singular_move = 0;
        int singular_value = 0;
        
        //long hashLockValue = excludedMove == 0 ? chessBoard.hashValue : chessBoard.hashValue ^ chessBoard.excludedHash;
        long hashLockValue = chessBoard.hashValue;
        
        int key = (int)(hashLockValue % Global.HASHSIZE);
        int hashSlot = 0;
        boolean hasHash = false;
        
        hashAttempts++;
        do { 
            hasHash = HashTable.hasHash(key, hashSlot, hashLockValue);
            if( hasHash) {
                HashTable.setNew(key, hashSlot, ancient);  
                if (!iid) {
                    int hDepth = HashTable.getDepth(key, hashSlot);
                    int hVal = HashTable.getValue(key, hashSlot);
                    hashHits++;
                    switch (HashTable.getType(key, hashSlot)) {
                        case (0):
                            if ( hDepth >= effectiveDepth) { 
                                if (hVal <= alpha) {
                                    nodes++;
                                    return hVal;
                                }
                            }
                        break;
                        case (1):
                            if ( hDepth >= effectiveDepth) {   
                                if (hVal > alpha && hVal < beta) {
                                    SavePV(TT_PV);
                                    nodes++;
                                    return hVal;
                                }

                            }    
                        break;
                        case (2):
                             if ( hDepth >= effectiveDepth) {    
                                if (hVal >= beta) {
                                    nodes++;
                                    return hVal;
                                }
                             }  
                        break;
                        case (4):
                            if (hVal == -Global.MATE_SCORE)
                                hVal += thisDepth;
                            if (hVal > alpha && hVal < beta) {
                                SavePV(TT_PV);
                            }
                            return hVal;    
                    }
                    nullFail |= HashTable.getNullFail(key, hashSlot);
                }
            } 
        } while( ++hashSlot < 2  );
        /** futility and extended futility pruning condition testing */
        boolean bFutilityPrune = false;
        int futilityMargin = 0;

        scoreThreshold += 500;
        if (!wasExtended && !isInCheck && effectiveDepth == 2 && scoreThreshold <= alpha) {
            bFutilityPrune = true;
            futilityMargin = 500;
        }
        scoreThreshold -= 300;
        if (!wasExtended && !isInCheck && effectiveDepth == 1 && scoreThreshold <= alpha) {
            bFutilityPrune = true;
            futilityMargin = 200;
        }

        int value;

        /** null move pruning code
         * we don't want to null move in a few situations
         * -when in check
         * -when performed a null move last ply
         * -when the hash table tells us not to (nullFail)
         * -when we are in the endgame as zugzwangs are more likely
         */
        if (!razored && !bFutilityPrune && !isInCheck && !nMove && nullFail != 1 
                && ( (chessBoard.pieceTotals[5] + chessBoard.pieceTotals[11]) != (chessBoard.noPieces[0] + chessBoard.noPieces[1] -2)) && chessBoard.getMinNumberOfPieces() > 1) {

            chessBoard.SwitchTurn();
            int reduce = 2 * Global.PLY;
            if (effectiveDepth > 6 && chessBoard.getMaxNumberOfPieces() > 3) {
                reduce = 3 * Global.PLY;
            }
            if (depth - reduce - Global.PLY >= Global.PLY) {
                value = -Max(SwitchSide(side), depth - reduce - Global.PLY, -beta, -beta + 1, true, false, false, false);
            } else {
                value = -Quies(SwitchSide(side), 0, -beta, -beta + 1);
            }
            thisDepth--;
            chessBoard.SwitchTurn();
            if (value >= beta) {
                if (!stop) {
                    HashTable.addHash(key, 0, value, effectiveDepth, HASH_BETA, 0, ancient, hashLockValue);
                }
                return value;
            }
            if (value <= (-Global.MATE_SCORE + Global.MAX_DEPTH )) {
                nullFail = 1;
            }
        }
        
        hashSlot = 0;
        boolean hasHashMove = (HashTable.hasHash(key, hashSlot, hashLockValue) && HashTable.getMove(key, hashSlot) != 0) 
                || (HashTable.hasHash(key, ++hashSlot, hashLockValue) && HashTable.getMove(key, hashSlot) != 0); 
       
        //internal iterative deepening
        if (!hasHashMove && effectiveDepth > 3 && !isInCheck && !razored && !bFutilityPrune ) {
            thisDepth--;
            Max(side, depth - 2 * Global.PLY, alpha, beta, true, false, false, true); 
        }  
        
        
        int state = SEARCH_HASH;
        
        int numberOfSkippedNodesFP = 0; /** the number of positions skipped due to futility pruning */

        int oldAlpha = alpha;

        boolean oneLegal = false;
        
        int bMove = VALUE_START;
        int hType = HASH_ALPHA;

        int endIndex = 0;
        int capIndex = 0;

        int bestFullMove = 0;
        int[] moveArr = new int[128];
        int[] hashArr = new int[4];
        int badCapIndex = 0;
        int moveCount = 0;
        
        int noHash = 0;
        boolean drawPV = false;
   
        boolean testCheck = false;
        int index = 0;
        
        Board.CheckInfo checkInfo = chessBoard.GetEmptyCheckInfo();
        while (state != SEARCH_END) {

            switch (state) {
                
                case (SEARCH_HASH):
                    hashSlot = 0;
                    //int singularDepth = 0;
                    do {
                        hasHash = HashTable.hasHash(key, hashSlot, hashLockValue);
                        if( hasHash) {
                            theMove = HashTable.getMove(key, hashSlot);
                            if (theMove != 0) {
                                bestFullMove = theMove;
                                /*int hDepth = HashTable.getDepth(key, hashSlot);
                                int hVal = HashTable.getValue(key, hashSlot);
                                if( excludedMove == 0 && hVal < Global.KNOWN_WIN 
                                        && effectiveDepth > 5 
                                        && hDepth >= effectiveDepth - 3 
                                        && (HashTable.getType(key, hashSlot) & 3) != 0 
                                        && hDepth > singularDepth )
                                {
                                     singular_move = HashTable.getMove(key, hashSlot);
                                     singular_value = hVal;
                                     singularDepth = hDepth;
                                }*/
                                moveArr[index++] = theMove;
                                if (index == 2) {
                                    if (moveArr[0] == moveArr[1]) {
                                        index--;
                                    }
                                    else {
                                        hashArr[noHash++] = theMove;
                                    }
                                }
                            }
                        }
                        ++hashSlot;
                    }
                    while( hashSlot < 2  );
                    
                    endIndex = 0; 
                    break;
                case (MATE_KILLER):
                    testCheck = true;
                    if (isInCheck) {
                        state = SEARCH_END - 1;
                        index = getCheckEscapes(side, moveArr);
                    } else {
                        index = 0;
                        endIndex = 0;

                        if (MoveFunctions.getValue(killerMoves[thisDepth][1]) >= 1 && verifyKiller(side, killerMoves[thisDepth][1])) {
                           if( hashArr[0] != (killerMoves[thisDepth][1] & 65535) && hashArr[1] != (killerMoves[thisDepth][1] & 65535)) {
                                moveArr[index++] = killerMoves[thisDepth][1] & 65535;
                                hashArr[noHash++] = killerMoves[thisDepth][1] & 65535;
                           }
                        }
                        if (MoveFunctions.getValue(killerMoves[thisDepth][0]) >= 1 && verifyKiller(side, killerMoves[thisDepth][0]) 
                                && (killerMoves[thisDepth][0] & 65535) != (killerMoves[thisDepth][1]  & 65535)) {
                            if (hashArr[0] != (killerMoves[thisDepth][0] & 65535) && hashArr[1] != (killerMoves[thisDepth][0] & 65535)) {
                                moveArr[index++] = killerMoves[thisDepth][0] & 65535;
                                hashArr[noHash++] = killerMoves[thisDepth][0] & 65535;
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
                    index = 126;
                    endIndex = 126;
                    
                    if (MoveFunctions.getValue(killerMoves[thisDepth][1]) == 0 && verifyKiller(side, killerMoves[thisDepth][1])) {
                       if( (hashArr[0] & 65535) != (killerMoves[thisDepth][1] & 65535) && (hashArr[1] & 65535) != (killerMoves[thisDepth][1] & 65535 )) {
                            moveArr[index++] = killerMoves[thisDepth][1] & 65535;
                            hashArr[noHash++] = killerMoves[thisDepth][1] & 65535;
                       }
                    }
                    if (MoveFunctions.getValue(killerMoves[thisDepth][0]) == 0 && verifyKiller(side, killerMoves[thisDepth][0]) 
                            && (killerMoves[thisDepth][0] & 65535) != (killerMoves[thisDepth][1] & 65535 )) {
                        if( (hashArr[0] & 65535) != (killerMoves[thisDepth][0] & 16777215) && (hashArr[1] & 65535) != (killerMoves[thisDepth][0] & 65535)) {
                            moveArr[index++] = killerMoves[thisDepth][0] & 65535;
                            hashArr[noHash++] = killerMoves[thisDepth][0] & 65535;
                       }
                    }
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
                
                /*if(state == SEARCH_HASH) {
                if(!verifyHash(side,moveArr[i])) {
                   System.out.println("info string crap move");
                                            continue;
                }
                }*/

                theMove = moveArr[i];
                
                if( testCheck && !chessBoard.CheckMove(side, theMove, checkInfo)) continue;
                
                //if( theMove == excludedMove ) continue;
                
                if( state == BAD_CAPS || state == NON_CAPS || state == GOOD_CAPTURES) {
                    boolean bDuplicate = false;
                    for( int m=0; m<noHash; m++)
                    {
                        if( hashArr[m]  == theMove ) {
                            bDuplicate = true;
                            break;
                        }
                    }
                    if( bDuplicate ) continue;
                } 
               
                int to = MoveFunctions.getTo(theMove);
                int from = MoveFunctions.getFrom(theMove);
                int piece = chessBoard.piece_in_square[from];

                /** any captures which the static exchnage evaluator finds lose material
                 * get placed in the bad capture array and are tried later
                 */
                if (state == GOOD_CAPTURES) {
                    int type = MoveFunctions.moveType(theMove);
                    int initialGain = 0;
                    if( type < Global.PROMO_Q) {
                        initialGain = Global.values[chessBoard.piece_in_square[to]]- Global.values[chessBoard.piece_in_square[from]];
                    } else if( type < Global.EN_PASSANT_CAP) {
                        initialGain = Global.values[3] - Global.values[5];
                    }
                    
                    /*int score1 = SEE.GetSEE2(side, to, from, type, 0);
                    int score2 = SEE.GetSEE(side, to, from, type, 0);
                    
                    if( (score1 >= 0 && score2 < 0) || (score2 >= 0 && score1 < 0 ) )
                    
                    {
                        SEE.GetSEE2(side, to, from, type, 0);
                        SEE.GetSEE(side, to, from, type, 0);
                        System.out.println("info string issue here");
                    }*/
                    
                    if( initialGain < 0 && (SEE.GetSEE2(side, to, from, type, 0)) < 0)
                    {
                        moveArr[badCapIndex--] = theMove;
                        //moveCount++;
                        continue;
                    }
                }  
                int extendAmount = 0;
                
                //extend if this is a checking move
                boolean checkingMove = false;
                if(chessBoard.MoveGivesCheck(side, theMove, checkInfo) )
                {
                    checkingMove = true;
                    if(bNodePV) {
                        extendAmount += Global.PLY;
                    }
                    else {
                        extendAmount += Global.PLY; 
                    } 
                }
                
                //recognize moves involving passed pawns
                //do not want to forward prune/lmr these moves
                boolean passedPawnMove = false;
                //boolean pawnExtension = false;
                if ((chessBoard.getTotalValue() < Global.totalValue * 0.4) && extendAmount == 0 && !isInCheck && ((piece % 6) == 5 && ((to >> 3) == 6 || (to >> 3) == 1))) {	//extention for close to promotion
                    passedPawnMove = true;
                    //pawnExtension = true;
                    if(bNodePV) {
                        extendAmount += Global.PLY;
                    }
                    else {
                       extendAmount += Global.PLY; 
                    } 
                }
                else if (piece % 6 == 5) {
                    passedPawnMove = Evaluation2.isPassedPawn(side, to); 
                }
                
                //singular extension
                if( theMove == singular_move ) {
                    int testVal = singular_value - depth * 3 / 4;
                    thisDepth--;
                    int singular_testVal = Max(side, depth / 2, testVal-1, testVal, true, isInCheck, false, true);
                    if( singular_testVal < testVal) {
                        extendAmount += Global.PLY;
                    }
                    //singular_testVal = Max(side, depth / 2, testVal-1, testVal, true, isInCheck, false, true, singular_move);
                }
                
                /** make the move */
                boolean draw = false;
                
                
                int reps = chessBoard.MakeMove(theMove, true);
              
                if( reps >= 2)
                {
                    draw = true;
                    oneLegal = true;
                    value = 0;
                    //moveCount++;
                }
                else
                {
                    /** we have a legal move */
                    oneLegal = true;

                    // futility pruning code
                    if (bFutilityPrune && extendAmount == 0 && !passedPawnMove) {
                        scoreThreshold = chessBoard.GetMaterialScore(side);
                        //int neededValue = alpha - futilityMargin;
                        //scoreThreshold = -Evaluation2.getEval(side^1, neededValue, neededValue+1, thisDepth);
         
                        if (scoreThreshold + futilityMargin <= alpha) {
                            numberOfSkippedNodesFP++;
                            chessBoard.UnMake(theMove, true);
                            //moveCount++;
                            continue;
                        }
                    }
          
                    //late move reduction code
                    boolean lmr = false;
                    if (state == NON_CAPS && ((!bNodePV && moveCount >= 4) || moveCount >= 15) && effectiveDepth >= 2 && !passedPawnMove
                           && !isInCheck && extendAmount == 0 ) {
                        extendAmount = -Global.PLY;
                        lmr = true;
                    }
                   
                    /*else if( state == BAD_CAPS && depth >= 2 && !passedPawnMove && !isInCheck && !checkingMove) {
                        if( depth > 5 )
                            extendAmount = -2;
                        else
                            extendAmount = -1;
                        lmr = true;
                    }*/
                    
                    int nextDepth = depth - Global.PLY + extendAmount;
                    
                    if (moveCount == 0) {
                        if (nextDepth >= Global.PLY) {
                            value = -Max(SwitchSide(side), nextDepth, -beta, -alpha, false, checkingMove, extendAmount > 0, false);
                        } else {
                            value = -Quies(SwitchSide(side), 0, -beta, -alpha);
                        }    
			thisDepth--;
                    } else {
                        if (nextDepth >= Global.PLY) {
                            value = -Max(SwitchSide(side), nextDepth, -alpha - 1, -alpha, false, checkingMove, extendAmount > 0, false);
                        } else {
                            value = -Quies(SwitchSide(side), 0, -alpha - 1, -alpha);
                        }
			thisDepth--;
                        if (value > alpha && value < beta) {
                            if (lmr) {
                                nextDepth = depth - Global.PLY;
                            }
                            if (nextDepth >= Global.PLY) {
                                value = -Max(SwitchSide(side), nextDepth, -beta, -alpha, false, checkingMove, extendAmount > 0, false);
                            } else {
                                value = -Quies(SwitchSide(side), 0, -beta, -alpha);
                            }
                            thisDepth--;
                        } else if (value > alpha && lmr == true) {       //use normal depth if lmr move looks interesting
                            nextDepth = depth - Global.PLY;  
                            if (nextDepth >= Global.PLY) {   
                                    value = -Max(SwitchSide(side), nextDepth, -beta, -alpha, false, false, false, false);
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
                    
                    if (value > alpha) {
                        if (value >= beta) {   
                                HashTable.addHash(key, theMove, value, effectiveDepth, HASH_BETA, nullFail, ancient, hashLockValue);
                            
                            Hist[side][piece%6][to] += effectiveDepth * effectiveDepth;
                            if (state == NON_CAPS && MoveFunctions.moveType(theMove) == Global.ORDINARY_MOVE) {								
                                int killerMove = MoveFunctions.makeKillerMove(to, from, piece);
                                if (killerMove != killerMoves[thisDepth][0]) {
                                    int temp1 = killerMoves[thisDepth][0];
                                    if (value >= (Global.MATE_SCORE - Global.MAX_DEPTH)) {  //mark this move as a mate killer
                                        killerMoves[thisDepth][0] = MoveFunctions.setValue(killerMove, 1);
                                    }
                                    else {
                                        killerMoves[thisDepth][0] = killerMove;
                                    }
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
      
        
        /*if( excludedMove != 0 && !oneLegal )
        {
            bMove = alpha;
            HashTable.addHash(key, bestFullMove, bMove, effectiveDepth, hType, nullFail, ancient, hashLockValue);
        }
        else*/ if (!isInCheck && !oneLegal) {				//stalemate detection
            bMove = 0;
            if (bMove > alpha && bMove < beta) {
                SavePV(SEARCHDRAW_PV);
            }
            hType = 4;
            HashTable.addHash(key, bestFullMove, bMove, effectiveDepth, hType, nullFail, ancient, hashLockValue);
        } else if (isInCheck && !oneLegal) {     //checkmate detection
            bMove = -(Global.MATE_SCORE - thisDepth);
            if (bMove > alpha && bMove < beta) {
                SavePV(MATE_PV);
            }
            hType = 4;
            HashTable.addHash(key, bestFullMove, -Global.MATE_SCORE, effectiveDepth, hType, nullFail, ancient, hashLockValue);
            return bMove;
        }
        else
        {
            //if we have skipped nodes due to futility pruning, then we adjust the transposition table entry
            // if we previously had an exact score, it is really a lower bound
            // if we have an upper bound, instead of storing the best score found, we store alpha
            if (numberOfSkippedNodesFP > 0) {
                if (hType == HASH_EXACT) {
                    hType = HASH_BETA;
                }
                else if (hType == HASH_ALPHA) {
                    bMove = alpha;
                }
            }
            HashTable.addHash(key, bestFullMove, bMove, effectiveDepth, hType, nullFail, ancient, hashLockValue);  
        }
        
        if (oneLegal && hType == HASH_EXACT) {             //update history tables
            Hist[side][chessBoard.piece_in_square[bestFullMove&63]%6][MoveFunctions.getTo(bestFullMove)]+= effectiveDepth * effectiveDepth;
        }
        if(alpha != oldAlpha && !drawPV)   {
            PV[thisDepth - 1][thisDepth - 1] = chessBoard.GetMoveAtDepth(thisDepth - 1);
            lengthPV[thisDepth-1] = lengthPV[thisDepth];
            System.arraycopy(PV[thisDepth], thisDepth, PV[thisDepth-1], thisDepth, lengthPV[thisDepth] - thisDepth + 1);
        }
        
        return bMove;
   }

    public boolean IsCheckDangerous( int move, int side )
    {
        return false;
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
     private int Quies(int side, int depth, int alpha, int beta) {
        /* try {
            buffWriter.write("quies "+chessBoard.hashValue+" "+"alpha "+alpha+" beta "+beta);
        } catch (Exception ex2) {};*/
 
        thisDepth++;
        nodes++;
        
        int[] capArr = new int[60];
        int value;
        int index = 0;
        int bValue = -Global.MATE_SCORE + thisDepth;
        int state = QUIES_CAPS;
        int testValue = 0;
        boolean oneLegal = false;
        //Board.CheckInfo checkinfo = chessBoard.GetEmptyCheckInfo();
        if( depth > 0 && inCheck(side)) {
            state = QUIES_IN_CHECK;
        }
        else
        {
            value = Evaluation2.getEval(side, alpha, beta, thisDepth);
            //value = Evaluation2.getEval(side, -Global.MATE_SCORE, Global.MATE_SCORE, thisDepth);

            if (value > alpha) {
                if (value >= beta) {
                    return value;
                }
                SavePV(STANDPAT_PV);
                alpha = value;
            }
            bValue = testValue = value;
        }
         
        int oldAlpha = alpha;
        
        while( state != QUIES_END )
        {
            switch( state )
            {
                case( QUIES_CAPS ):
                    index = getCaptures(side, capArr);
                 break;
                    
                case( QUIES_CHECKING ):
                    index = getCheckingMoves(side, capArr, 0);
                break;
                    
                case( QUIES_IN_CHECK ): 
                    index = getCheckEscapes(side, capArr);
                    bValue = -(Global.MATE_SCORE - thisDepth);
                    /*if (index == 0) {
                        if (bValue > alpha && bValue < beta) {
                            SavePV(MATE_PV);
                        }
                        return -Global.MATE_SCORE + thisDepth;
                    }*/
                break;
            }
            
            for (int i = index - 1; i >= 0; i--) {
                int to = MoveFunctions.getTo(capArr[i]);
                int from = MoveFunctions.getFrom(capArr[i]);
               
                //if( !chessBoard.CheckMove( side, capArr[i], checkinfo)) continue;
                
                if (state == QUIES_CAPS ) {
                    int type = MoveFunctions.moveType(capArr[i]);
                    int neededScore = (int)Math.max(0, (alpha - testValue));
                    int initialGain = 0;
                    if( type < Global.PROMO_Q) {
                        initialGain = Global.values[chessBoard.piece_in_square[to]]- Global.values[chessBoard.piece_in_square[from]];
                    } else if( type < Global.EN_PASSANT_CAP) {
                        initialGain = Global.values[3] - Global.values[5];
                    }
                    /*int score1 = SEE.GetSEE2(side, to, from, type, neededScore);
                    int score2 = SEE.GetSEE(side, to, from, type, neededScore);
                    
                    if( (score1 >= neededScore && score2 < neededScore) || (score2 >= neededScore && score1 < neededScore ) )
                    {
                    
                    
                        SEE.GetSEE2(side, to, from, type, neededScore);
                        SEE.GetSEE(side, to, from, type, neededScore);
                        System.out.println("info string issue here");
                    }*/
                    
                    if( initialGain < neededScore && (SEE.GetSEE2(side, to, from, type, neededScore)) < neededScore)
                    {
                        bValue = alpha;
                        continue;
                    }
                    
                }
                else if( state == QUIES_CHECKING )
                {
                    if( IsCheckDangerous( capArr[i], side )) continue;
                }
               // boolean bChecking = chessBoard.MoveGivesCheck(side, capArr[i], checkinfo);
                
                chessBoard.MakeMove(capArr[i], false);	
                
                if( inCheck(side)) {
                    chessBoard.UnMake(capArr[i], false);
                    continue;
                }
                oneLegal = true;
                
                value = -Quies(SwitchSide(side), depth + 1, -beta, -alpha);   
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
            
            if( state == QUIES_IN_CHECK ) {
                if( !oneLegal) {
                    if (bValue > alpha && bValue < beta) {
                            SavePV(MATE_PV);
                    }
                   return bValue;
                }
            }
            state++;
             if( state == QUIES_IN_CHECK ) break;
            //if( state == QUIES_CHECKING && depth >= 2) break;
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
            int piece = chessBoard.piece_in_square[from];

            if (inCheck(side)) {
                chessBoard.UnMake(moveArr[i], false);
                continue;
            }
            String output = HistoryWriter.getUCIMove(moveArr[i]);
            //System.out.print(output);
            System.out.print(" to is "+to+" from is "+from+" ");
            if (reps == 3) {
                System.out.println("1");
                    continue;
            } else {
                Perft(SwitchSide(side), depth - 1);
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

        int value = Evaluation2.getEval(side, -2000, 2000, 0);
        Board.getInstance().FlipPosition();
        int value2 = Evaluation2.getEval(SwitchSide(side), -2000, 2000, 0);

        if(value != value2)
        {
            System.out.println("value2 is "+value2);
            Evaluation2.printEvalTerms();
            System.out.println("value is "+value);
            Board.getInstance().FlipPosition();
            value2 = Evaluation2.getEval(side, -2000, 2000, 0);
            Evaluation2.printEvalTerms();
        }
        else
        {
            Board.getInstance().FlipPosition();
        }

        /*if(PERFT_TRANSTABLE) {
            int hashIndex = HashTable.hasHash(key, chessBoard.hashValue);
            if (hashIndex != 1 ) {
                if(HashTable.getDepth(key,  hashIndex) == depth) {
                    perft += (long)HashTable.getMove(key,  hashIndex);
                    return;
                }
                hashIndex = HashTable.hasSecondHash(key, chessBoard.hashValue);
                if (hashIndex != 1 && HashTable.getDepth(key,  hashIndex) == depth) {
                    perft += (long)HashTable.getMove(key,  hashIndex);
                    return;
                }
            }
        }*/
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
            PerftDebug(SwitchSide(side), depth - 1);
            chessBoard.UnMake(moveArr[i], true);
        }
        if(PERFT_TRANSTABLE)
            HashTable.addHash(key, (int)(perft - perftBefore) , 0, depth, 0, 0, 0, chessBoard.hashValue);
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

        boolean inCheck = inCheck(side);
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
            if (inCheck(side)) {
                chessBoard.UnMake(moveArr[i], true);
                continue;
            }
            Perft(SwitchSide(side), depth - 1);
            chessBoard.UnMake(moveArr[i], true);
        }
    }
}
