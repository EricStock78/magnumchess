package magnumchess;

/*import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;*/
import java.util.Arrays;
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
    private static final boolean PERFT_TRANSTABLE = false;

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
    private static final int[][] killerMoves = new int[2][100];

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
    private static final int QUIES_HASH = 1;
    private static final int QUIES_MOVES = 2;
    private static final int QUIES_CHECKING = 3;
    private static final int QUIES_END = 4;
    
    /** constants for starting values of search */
    private static final int ALPHA_START = -Global.MATE_SCORE;
    private static final int BETA_START = Global.MATE_SCORE;
    private static final int VALUE_START = -Global.MATE_SCORE;
    /** variable used to track how deep in the search we are */
    private static byte thisDepth;
    /** boolean which represents if we are using infinite time control */
    private static boolean infiniteTimeControl = false;
    /** count of moves generated when debugging move gen using perft command */
    private long perft = 0;
    
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
        int side = chessBoard.getTurn();
        int[] moveArr = new int[128];
        boolean bInCheck = inCheck(side);
        int numberOfMoves = inCheck(side) ? getCheckEscapes(side, moveArr) : GetAllMoves(side, moveArr);
        Board.CheckInfo checkInfo = chessBoard.GetCheckInfo();
        for (int i = numberOfMoves - 1; i >= 0; i--) {
            if( !chessBoard.CheckMove(side, moveArr[i], checkInfo) ) {
                for (int j = i; j <= numberOfMoves - 1; j++) {
                    moveArr[j] = moveArr[j + 1];
                }
                numberOfMoves--;
                continue;
            }
            boolean bGivesCheck = chessBoard.MoveGivesCheck(side, moveArr[i], checkInfo);
            chessBoard.MakeMove(moveArr[i], false, bGivesCheck, checkInfo);
            chessBoard.UnMake(moveArr[i], false);
        }

        if(numberOfMoves == 0)
        {
            return bInCheck ? Global.CHECKMATE : Global.STALEMATE;
        }

        int iRandomIndex = rand.nextInt(numberOfMoves);
        boolean bGivesCheck = chessBoard.MoveGivesCheck(side, moveArr[iRandomIndex], checkInfo);
        chessBoard.AddMove( moveArr[iRandomIndex]);
        chessBoard.MakeMove( moveArr[iRandomIndex], false, bGivesCheck, checkInfo );
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

        // temporary varible to store the value returned from a search
        int value;

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

        Board.ancient = chessBoard.getCount() % 8;
        
        infiniteTimeControl = inf;

        /** here we prepare the history arrays for the next search */
        for (int i = 0; i < 6; i++) {
            Arrays.fill(Hist[Global.COLOUR_WHITE][i], (short)0 );
            Arrays.fill(Hist[Global.COLOUR_BLACK][i], (short)0 );
        }
        
        /** here the killer moves are cleared out before the next search */
        Arrays.fill(killerMoves[0], 0 );
        Arrays.fill(killerMoves[1], 0 );
       
        nodes = 0;
        //hashAttempts = 0;
        //hashHits = 0;
       
        /** collect the root moves */
        if (!inCheck(theSide)) {
            numberOfMoves = GetAllMoves(theSide, moveArr);
        } else {
            numberOfMoves = getCheckEscapes(theSide, moveArr); 
            if( numberOfMoves == 0) {
                return "info string no legal move";
            }
        }

        thisDepth = 0;

        int[] compareArray = new int[128];           //array used to store values of moves for sorting the move list
        
        //remove illegal moves
        //mark moves which result in a draw
        //perform an initial sort of the moves list based on their Q sort scores
        Board.CheckInfo checkInfo = chessBoard.GetCheckInfo();
        for (int i = numberOfMoves - 1; i >= 0; i--) {
            int tempmove = moveArr[i];
            if( !chessBoard.CheckMove(theSide, tempmove, checkInfo) ) {
                for (int j = i; j <= numberOfMoves - 1; j++) {
                    moveArr[j] = moveArr[j + 1];
                    compareArray[j] = compareArray[j + 1];
                }
                numberOfMoves--;
                continue;
            }
            boolean bGivesCheck = chessBoard.MoveGivesCheck(theSide, tempmove, checkInfo);
            
            int reps = chessBoard.MakeMove(tempmove, true, bGivesCheck, checkInfo);
            
            if (reps == 3) {
                moveArr[i] = MoveFunctions.setMoveRootDraw( moveArr[i] );
                compareArray[i] = 0;
            } else if (chessBoard.getDraw() == 100) {
                moveArr[i] = MoveFunctions.setMoveRootDraw( moveArr[i] );
                compareArray[i] = 0;
            } else if (isStaleMate( SwitchSide(theSide) )) {
                moveArr[i] = MoveFunctions.setMoveRootDraw( moveArr[i] );
                compareArray[i] = 0;
            } else {
                compareArray[i] = -Quies(SwitchSide(theSide), 1, -BETA_START, -ALPHA_START, bGivesCheck);
                thisDepth--;
            }
            chessBoard.UnMake(moveArr[i], true);
        }
        sortMoves(0, numberOfMoves, moveArr, compareArray);

        //iteratively deepened search starting at depth 2
        for (int depth = 2; depth <= searchDepth; depth++) {
            //hashAttempts = 0;
            //hashHits = 0;
            GotoTimeState(TIME_STATE_ITERATION_START);

            if (!timeLeft()) {
                stop = true;
            }

            if (stop && hasValue) {
                break;
            }

            int alpha = ALPHA_START;
            int beta = BETA_START;

            int tempBestMove = Integer.MIN_VALUE;
            boolean isExact = false;
            thisDepth = 0;

            for (int i = numberOfMoves - 1; i >= 0; i--) {
                int tempMove = moveArr[i];
                int nextDepth = depth * Global.PLY - Global.PLY + partialDepthExtension;
               
                boolean bGivesCheck = chessBoard.MoveGivesCheck(theSide, tempMove, checkInfo);
                 
                if (MoveFunctions.isMoveRootDraw( moveArr[i] )) {
                    value = 0;
                }
                else if (!isExact) {
                    chessBoard.MakeMove(tempMove, true, bGivesCheck, checkInfo);
                    value = -Max(SwitchSide(theSide), nextDepth, -beta, -alpha, false, bGivesCheck, false, false, 0);
                    chessBoard.UnMake(tempMove, true);
                    thisDepth--;
                    //check for a first move which goes LOW
                    if(value < bestValue - 30 && value < 10000L && !stop) {
                        GotoTimeState(TIME_STATE_FAIL_LOW);
                    }
                    
                } else {
                    
                    chessBoard.MakeMove(tempMove, true, bGivesCheck, checkInfo);
                    value = -Max(SwitchSide(theSide), nextDepth, -alpha - 1, -alpha, false, bGivesCheck, false, false, 0);
                    thisDepth--;

                    if (value > alpha && !stop) {
                        //check for a move which goes HIGH
                        GotoTimeState(TIME_STATE_FAIL_HIGH);
                        value = -Max(SwitchSide(theSide), nextDepth, -beta, -alpha, false, bGivesCheck, false, false, 0);
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
                    compareArray[i] = 10000 - i;
                } 
                else {                                                 //move is no good
                    compareArray[i] = 2000 + i;                 
                }
            }

            //try to get the pv from the TT
            String pv = HistoryWriter.getUCIMove(bestMove);
            int move = bestMove;
            int moveDepth = 0;
            int[] pvMoves = new int[64];
            do {
                Board.CheckInfo info = chessBoard.GetCheckInfo();
                boolean bGivesCheck = chessBoard.MoveGivesCheck(chessBoard.getTurn(), move, info);
                int reps = chessBoard.MakeMove( move, true, bGivesCheck, info );
                
                pvMoves[moveDepth++] = move;
                int hashIndex = HashTable.hasHash(chessBoard.hashValue);
                move = 0;
                if( hashIndex != -1)
                {
                   move = HashTable.getMove( hashIndex );       
                }
                
                if( move != 0 ) {
                    pv = pv.concat(" ");
                    pv = pv.concat(HistoryWriter.getUCIMove(move));
                }
                if( reps == 3) break;
                    
            } while(move != 0 && moveDepth < 64);
             
            while( moveDepth > 0) {
                moveDepth--;
                chessBoard.UnMake(pvMoves[moveDepth], true);
            }
            
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
        boolean bGivesCheck = chessBoard.MoveGivesCheck(chessBoard.getTurn(), bestMove, checkInfo);
        chessBoard.MakeMove( bestMove, false, bGivesCheck, checkInfo );
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
            Board.CheckInfo checkInfo = chessBoard.GetCheckInfo();
            for (int i = 0; i < numberOfMoves; i++) {
                if( !chessBoard.CheckMove(side, Moves[i], checkInfo)) {
                    continue;
                }
                else {
                    return false;
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
    public int getCheckingMoves(int side, int[] Moves, int start, Board.CheckInfo checkInfo) 
    {
        long moves, doubleMoves, dcMoves, dcDoubleMoves;
        int index = start;
        long pieces = chessBoard.pieceBits[side][Global.PIECE_PAWN];
        long dcPieces = chessBoard.pieceBits[side][Global.PIECE_PAWN] & checkInfo.dcCandidates;
        if( side == Global.COLOUR_WHITE)
        {
            moves = pieces << 8 & ~chessBoard.bitboard & ~Global.rankMasks[7];
            dcMoves = dcPieces << 8 & ~chessBoard.bitboard & ~Global.rankMasks[7];
            doubleMoves = moves << 8 & Global.rankMasks[3] & ~chessBoard.bitboard;
        }
        else
        {
            moves = pieces >> 8 & ~chessBoard.bitboard & ~Global.rankMasks[0];
            dcMoves = dcPieces >> 8 & ~chessBoard.bitboard & ~Global.rankMasks[0];
            doubleMoves = moves >> 8 & Global.rankMasks[4] & ~chessBoard.bitboard;
        }
        moves &= checkInfo.checkSquares[Global.PIECE_PAWN];
        while (moves != 0) 
        {
            long toBit = moves & -moves;
            moves ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            int from = to + Global.behindRank[side];
            moveOrder[index] = Hist[side][5][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, Global.ORDINARY_MOVE);
        }
        
        dcDoubleMoves = dcMoves;
        while (dcMoves != 0) 
        {
            int enemyKing = chessBoard.pieceList[4 + (side^1) * 6][0];
            long toBit = dcMoves & -dcMoves;
            dcMoves ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            int from = to + Global.behindRank[side];
            if(((Global.mask_between[enemyKing][from] & (1L << to)) == 0 )
                && ((Global.mask_between[enemyKing][to] & (1L << from)) == 0) ) {
                moveOrder[index] = Hist[side][5][to];
                Moves[index++] = MoveFunctions.makeMove(to, from, Global.ORDINARY_MOVE);
            }
            else {
                dcDoubleMoves ^= toBit;
            }
        }
        
        doubleMoves &= checkInfo.checkSquares[Global.PIECE_PAWN];
        while (doubleMoves != 0) 
        {
            long toBit = doubleMoves & -doubleMoves;
            doubleMoves ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            int from = to + Global.behindRank[side] * 2;
            moveOrder[index] = Hist[side][5][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, Global.DOUBLE_PAWN);
        }
        
        if( side == Global.COLOUR_WHITE) {
            dcDoubleMoves = dcDoubleMoves << 8 & Global.rankMasks[3] & ~chessBoard.bitboard; 
        }
        else {
            dcDoubleMoves = dcDoubleMoves >> 8 & Global.rankMasks[4] & ~chessBoard.bitboard;
        }
        
        while (dcDoubleMoves != 0) 
        {
            int enemyKing = chessBoard.pieceList[4 + (side^1) * 6][0];
            long toBit = dcDoubleMoves & -dcDoubleMoves;
            dcDoubleMoves ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            int from = to + Global.behindRank[side] * 2;
            if(((Global.mask_between[enemyKing][from] & (1L << to)) == 0 )
                && ((Global.mask_between[enemyKing][to] & (1L << from)) == 0) ) {
                moveOrder[index] = Hist[side][5][to];
                Moves[index++] = MoveFunctions.makeMove(to, from, Global.DOUBLE_PAWN);
            }
        }

        for(int i=0; i<4; i++)
        {
            int pType = i + Global.pieceAdd[side];
            for(int j=0; j < chessBoard.pieceTotals[pType]; j++)
            {
                int from = chessBoard.pieceList[pType][j];
                long toSquares = chessBoard.getAttackBoard(from);
                toSquares &= ~chessBoard.bitboard;
                if( (checkInfo.dcCandidates & (1L << from)) == 0 ) {
                    toSquares &= checkInfo.checkSquares[i];
                }
                while (toSquares != 0) {
                    long toBit = toSquares & -toSquares;
                    toSquares ^= toBit;
                    int to = Long.numberOfTrailingZeros(toBit);
                    moveOrder[index] = Hist[side][i][to];
                    Moves[index++] = MoveFunctions.makeMove(to, from, Global.ORDINARY_MOVE);
                }
            }
        }
        
        int kingPos = chessBoard.pieceList[4 + side * 6][0];
        if( (checkInfo.dcCandidates & (1L << kingPos)) != 0 ) {
            int enemyKing = chessBoard.pieceList[4 + (side^1) * 6][0];
            long toSquares = chessBoard.getAttackBoard(kingPos);
            toSquares &= ~chessBoard.bitboard;
            while (toSquares != 0) {
                long toBit = toSquares & -toSquares;
                toSquares ^= toBit;
                int to = Long.numberOfTrailingZeros(toBit);
                if(((Global.mask_between[enemyKing][kingPos] & (1L << to)) == 0 )
                    && ((Global.mask_between[enemyKing][to] & (1L << kingPos)) == 0) ) {
                    moveOrder[index] = Hist[side][Global.PIECE_KING][to];
                    Moves[index++] = MoveFunctions.makeMove(to, kingPos, Global.ORDINARY_MOVE);
                }
            }
        }
        
        if (chessBoard.castleFlag[side] > Global.CASTLED)  {
            long Temp = chessBoard.getKingCastle(((int)(chessBoard.bitboard >>> (56 * side)) & 255));
            if(chessBoard.castleFlag[side] != Global.SHORT_CASTLE && ((Temp & Global.set_Mask[ 2 ]) != 0 ) ) {
                if( !chessBoard.isAttacked(side, 2 + (56 * side)) && !chessBoard.isAttacked(side, 3 + (56 * side)) ) {
                    if( chessBoard.MoveGivesCheck(side, MoveFunctions.makeMove(2 + (56 * side), 4 + 56 * side, Global.LONG_CASTLE), checkInfo)) { 
                        moveOrder[index] = Hist[side][4][2 + (56 * side)];
                        Moves[index++] = MoveFunctions.makeMove(2 + (56 * side), 4 + 56 * side, Global.LONG_CASTLE);
                    }
                }
            }
            if(chessBoard.castleFlag[side] != Global.LONG_CASTLE && ((Temp & Global.set_Mask[ 6 ])!=0 ) ) {		
                if( !chessBoard.isAttacked(side, 5 + (56 * side)) && !chessBoard.isAttacked(side, 6 + (56 * side)) ) {
                    if( chessBoard.MoveGivesCheck(side, MoveFunctions.makeMove(6 + (56 * side), 4 + 56 * side, Global.SHORT_CASTLE), checkInfo)) { 
                        moveOrder[index] = Hist[side][4][6 + (56 * side)];
                        Moves[index++] = MoveFunctions.makeMove(6 + (56 * side), 4 + 56 * side, Global.SHORT_CASTLE);
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
                if( !chessBoard.isAttacked(side, 2 + (56 * side)) && !chessBoard.isAttacked(side, 3 + (56 * side)) && !chessBoard.isAttacked(side, 4 + (56 * side)) ) {
                   moveOrder[index] = Hist[side][4][2 + (56 * side)];
                   Moves[index++] = MoveFunctions.makeMove(2 + (56 * side), from, Global.LONG_CASTLE);
                }
            }
            if(chessBoard.castleFlag[side] != Global.LONG_CASTLE && ((Temp & Global.set_Mask[ 6 ])!=0 ) ) {		
                if( !chessBoard.isAttacked(side, 5 + (56 * side)) && !chessBoard.isAttacked(side, 6 + (56 * side)) && !chessBoard.isAttacked(side, 4 + (56 * side)) ) {
                   moveOrder[index] = Hist[side][4][6 + (56 * side)];
                   Moves[index++] = MoveFunctions.makeMove(6 + (56 * side), from, Global.SHORT_CASTLE);
                }
            }
        }

        long toSquares = chessBoard.getKingMoves(from);
        toSquares &= ~chessBoard.bitboard;
        while (toSquares != 0) {
            long toBit = toSquares & -toSquares;
            toSquares ^= toBit;
            int to = Long.numberOfTrailingZeros(toBit);
            moveOrder[index] = Hist[side][4][to];
            Moves[index++] = MoveFunctions.makeMove(to, from, Global.ORDINARY_MOVE);
        }

        for(int i=0; i<4; i++)
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
        
        for(int i=0; i<5; i++)
        {
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
                    moveOrder[index] = Global.values[chessBoard.piece_in_square[to]] + Global.invValues[i];
                    Captures[index++] = MoveFunctions.makeMove(to, from, type);
                }
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
        
        assert( chessBoard.checkers == (chessBoard.getAttack2(kingPos)& chessBoard.pieceBits[side^1][Global.PIECE_ALL]) );
        
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
            escapes[index++] = MoveFunctions.makeMove(attackTo, kingPos, type);      
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
     * Method verifyMove
     *
     * ensures that the hash move or killer move is legal
     *
     * @param int side - the side to move
     * @param int move - the hash move to be verified
     *
     * @return boolean - was the hash move legal
     *
     */
   
    private boolean verifyMove(int side, int move, boolean bKillerMove, Board.CheckInfo checkInfo) 
    {  
        
        if ( move == 0) {
            return false;
        }
        
        int from = MoveFunctions.getFrom( move );
        int to = MoveFunctions.getTo( move );
        int piece = chessBoard.piece_in_square[ from ];
        
        if( bKillerMove ) {
            if( piece != MoveFunctions.getKillerPiece( move ) ) {
                return false;
            }
        }
        
        if( piece == -1 || piece /6 != side ) {
            return false;
        }
       
        switch( MoveFunctions.moveType(move) )
        {
            case( Global.ORDINARY_MOVE ):
                if( chessBoard.piece_in_square[ to ] != -1 ) {    
                    return false;
                }
                
                if( piece % 6 != 5 ) {
                    if ( (chessBoard.getAttackBoard(from) & (1L << to)) == 0 ) {
                        return false;
                    }
                }
                else {
                    if( to != from + Global.forwardRank[side] ) {
                        return false;
                    }
                     
                    if( Global.RelativeRanks[side][from/8] > 5 || Global.RelativeRanks[side][from/8] < 1) {
                        return false;
                    }
                }
            break;
                
            case( Global.ORDINARY_CAPTURE ):
                int capPiece = chessBoard.piece_in_square[ to ];
                if(  capPiece == -1 || capPiece /6 != (side^1) ) {
                    return false;
                }
                if ( (chessBoard.getAttackBoard(from) & (1L << to)) == 0 ) {
                    return false;
                }
                if( piece % 6 == 5 && ( Global.RelativeRanks[side][from/8] > 5 || Global.RelativeRanks[side][from/8] < 1) ) {
                    return false;
                }
                
            break;
                
            case( Global.DOUBLE_PAWN ):
                int rank = from / 8;
                if( piece % 6 != 5 ) {
                    return false;
                }
                if( Global.RelativeRanks[side][rank] != 1) {
                    return false;
                }
                if( chessBoard.piece_in_square[ from +Global.forwardRank[side]] != -1 || chessBoard.piece_in_square[ from + Global.forwardRank[side] * 2] != -1) {
                    return false;
                }  
                
            break;
            
            case( Global.PROMO_Q ):
                rank = from / 8;
                if( piece % 6 != 5 ) {
                    return false;
                }
                if( Global.RelativeRanks[side][rank] != 6) {
                    return false;
                }
                boolean bOk = false;
                if( to == from + Global.forwardRank[side] ) {   //non capture
                    if( chessBoard.piece_in_square[ to ] == -1 ) {
                        bOk = true;
                    }
                }
                else {                                          //capture
                    if( (chessBoard.getPawnAttack(side, from) & (1L << to)) != 0 ) {
                        bOk = true;
                    }
                }
                if( !bOk ) {
                    return false;
                }   
                
            break;
            case( Global.EN_PASSANT_CAP ):
                if( piece % 6 != 5 ) {
                    return false;
                }
                else if( Global.RelativeRanks[side][to/8] != 5 ) {
                    return false;
                }
                else if( (chessBoard.getPawnAttack(side, from) & (1L << to)) == 0 ) {
                    return false;
                }
                else if( to != chessBoard.getPassant( side ^1)) {
                    return false;
                }
            break;
                
            case( Global.SHORT_CASTLE ):
                if( piece % 6 != 4 ) {
                    return false;
                }
                if( chessBoard.checkers != 0) {
                    return false;
                }
                if( from != chessBoard.pieceList[piece][0] || from != (4 + 56 * side) ) {
                    return false;
                }
                if( to == 6 + 56 * side && (chessBoard.castleFlag[side] & Global.SHORT_CASTLE) != 0 ) {
                    long castleBits = chessBoard.getKingCastle(((int)(chessBoard.bitboard >>> (56 * side)) & 255));
                    if(  (castleBits & (1L << 6)) != 0 ) {
                        if( chessBoard.isAttacked(side, 5 + (56 * side)) || chessBoard.isAttacked(side, 6 + (56 * side)) || chessBoard.isAttacked(side, 4 + (56 * side)) ) {
                            return false;
                        }
                    }
                    else {
                        return false;
                    }
                }
                else {
                    return false;
                }
            break;    
                
            case( Global.LONG_CASTLE ):   
                if( piece % 6 != 4 ) {
                    return false;
                }
                if( chessBoard.checkers != 0) {
                    return false;
                }
                
                if( from != chessBoard.pieceList[piece][0] || from != (4 + 56 * side) ) {
                    return false;
                }
                if( to == 2 + 56 * side && (chessBoard.castleFlag[side] & Global.LONG_CASTLE) != 0) {
                    long castleBits = chessBoard.getKingCastle(((int)(chessBoard.bitboard >>> (56 * side)) & 255));
                    if(  (castleBits & (1L << 2)) != 0 ) {
                        if( chessBoard.isAttacked(side, 2 + (56 * side)) || chessBoard.isAttacked(side, 3 + (56 * side)) || chessBoard.isAttacked(side, 4 + (56 * side)) ) {
                            return false;
                        }
                    }
                    else {
                        return false;
                    }
                }
                else {
                    return false;
                }
                
            break;
            
        }
        
        if( chessBoard.checkers != 0 ) {
            
            if( chessBoard.MoreThanOne( chessBoard.checkers )) {
                //only a escape move is acceptable
                if( piece != Global.PIECE_KING ) {
                    return false;
                }
                else if( chessBoard.isAttacked(side, to, chessBoard.bitboard ^ ((1L << from) | (1L << to)))) {
                    return false;
                }
            }
            else {
                //either a move to block or capture the checker, or a king escape move
                if( piece == Global.PIECE_KING ) {               
                    if( chessBoard.isAttacked(side, to, chessBoard.bitboard ^ ((1L << from) | (1L << to)))) {
                        return false;
                    }
                }
                else {
                    int checker = Long.numberOfTrailingZeros(chessBoard.checkers);
                    int king = chessBoard.pieceList[side][Global.PIECE_KING];
                    if(to != checker && (Global.mask_between[king][checker] & (1L << to)) == 0 ) {
                        return false;
                    }
                } 
            }
        }
        return true;
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
    private int Max(int side, int depth, int alpha, int beta, boolean nMove, boolean isInCheck, boolean wasExtended, boolean iid, int excludedMove) {
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

        if (stop || thisDepth > 99) {
           return 0;
        }
        
        //mate distance pruning
        int nalpha = Math.max( -Global.MATE_SCORE + thisDepth, alpha );
        int nbeta = Math.min( Global.MATE_SCORE - (thisDepth + 1), beta );
        if( nalpha >= nbeta ) {
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
        //int singular_move = 0;
        //int singular_value = 0;
        
        //long hashLockValue = excludedMove == 0 ? chessBoard.hashValue : chessBoard.hashValue ^ chessBoard.excludedHash;
        long hashLockValue = chessBoard.hashValue;
        
        if( !iid )
            hashAttempts++;
        
        int hashIndex = iid ? -1 : HashTable.hasHash(hashLockValue);
        /*{
            HashTable.addHash((1 << 16) - 1, Global.MATE_SCORE, 45, Global.SCORE_LOWER, 1, chessBoard.hashValue);
            //HashTable.addHash(0, -Global.MATE_SCORE, 63, Global.SCORE_TERMINAL, 0, chessBoard.hashValue);
            //HashTable.addHash((1 << 16) - 1, Global.MATE_SCORE, 45, Global.SCORE_LOWER, 1, chessBoard.hashValue);

            int hIndex = HashTable.hasHash(hashLockValue);

            int hDepth = HashTable.getDepth(hIndex);
            int hVal = HashTable.getValue(hIndex);
            int type = HashTable.getType(hIndex);
            int move = HashTable.getMove(hIndex);
            int nf = HashTable.getNullFail(hIndex);
        } */          
       
        if( hashIndex != -1) {
            hashHits++;
            HashTable.setNew(hashIndex);
            
                
            int hDepth = HashTable.getDepth(hashIndex);
            int hVal = HashTable.getValue(hashIndex);

            switch (HashTable.getType(hashIndex)) {
                case (Global.SCORE_UPPER):
                    if ( hDepth >= effectiveDepth ) { 
                        if (hVal <= alpha) {
                            nodes++;
                            return hVal;
                        }
                    }
                break;
                case (Global.SCORE_EXACT):
                    if ( hDepth >= effectiveDepth) {   
                        if (hVal > alpha && hVal < beta) {
                            nodes++;
                            return hVal;
                        }

                    }
                break;
                case (Global.SCORE_LOWER):
                     if ( hDepth >= effectiveDepth) {    
                        if (hVal >= beta) {
                            nodes++;
                            return hVal;
                        }
                     } 
                break;
                case (Global.SCORE_TERMINAL):
                    if (hVal == -Global.MATE_SCORE) {
                        hVal += thisDepth;
                    }
                    return hVal;    
            }
            nullFail = HashTable.getNullFail(hashIndex);
        }
        
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
                value = -Max(SwitchSide(side), depth - reduce - Global.PLY, -beta, -beta + 1, true, false, false, false, 0);
            } else {
                value = -Quies(SwitchSide(side), 0, -beta, -beta + 1, false);
            }
            thisDepth--;
            chessBoard.SwitchTurn();
            if (value >= beta) {
                if (!stop) {
                    HashTable.addHash(0, value, effectiveDepth, Global.SCORE_LOWER, 0, hashLockValue);
                }
                return value;
            }
            if (value <= (-Global.MATE_SCORE + Global.MAX_DEPTH )) {
                nullFail = 1;
            }
        }
        
        if ( hashIndex == -1 && effectiveDepth > 3 && !isInCheck && !razored && !bFutilityPrune ) {
            thisDepth--;
            Max(side, depth - 2 * Global.PLY, alpha, beta, true, false, false, true, 0); 
            hashIndex = HashTable.hasHash(hashLockValue);
        }  
        
        int state = SEARCH_HASH;
        int numberOfSkippedNodesFP = 0; /** the number of positions skipped due to futility pruning */
        
        boolean oneLegal = false;
        
        int bMove = VALUE_START;
        int hType = Global.SCORE_UPPER;

        int endIndex = 0;
        int capIndex = 0;

        int[] moveArr = new int[128];
        int[] hashArr = new int[4];
        int badCapIndex = 0;
        int moveCount = 0;
        
        int noHash = 0;
        
        int index = 0;
        
        int ttMove = 0;
        Board.CheckInfo checkInfo = chessBoard.GetCheckInfo();
        while (state != SEARCH_END) {

            switch (state) {
                
                case (SEARCH_HASH):
                    if( hashIndex != -1 ) {
                        theMove = HashTable.getMove(hashIndex);
                        if (verifyMove(side,theMove, false, checkInfo)) {  
                            //int hDepth = MoveFunctions.getTTMoveDepth( ttMove );
                            /*int hDepth = HashTable.getDepth(hashIndex, false);
                            int hVal = HashTable.getValue(hashIndex, false);
                            if( excludedMove == 0 && hVal < Global.KNOWN_WIN 
                                    && effectiveDepth > 5 
                                    && hDepth >= effectiveDepth - 3 
                                    && (HashTable.getType(hashIndex, false) & 1) == 1)
                            {
                                 singular_move = theMove;
                                 singular_value = hVal;
                            }*/
                            moveArr[index++] = theMove;
                            hashArr[noHash++] = theMove;
                         }
                        /*else {
                            if( theMove != 0) {
                                System.out.println("info string hash fail");
                                verifyMove(side,theMove, false);
                            }
                       }*/
                    }

                    endIndex = 0; 
                    break;
                case (MATE_KILLER): 
                    if (isInCheck) {
                        state = SEARCH_END - 1;
                        index = getCheckEscapes(side, moveArr);
                    } else {
                        index = 0;
                        endIndex = 0;

                        if (MoveFunctions.getValue(killerMoves[1][thisDepth]) >= 1 && verifyMove(side, killerMoves[1][thisDepth], true, checkInfo)) {
                           if( hashArr[0] != (killerMoves[1][thisDepth] & 65535) && hashArr[1] != (killerMoves[1][thisDepth] & 65535)) {
                                moveArr[index++] = killerMoves[1][thisDepth] & 65535;
                                hashArr[noHash++] = killerMoves[1][thisDepth] & 65535;
                           }
                        }
                        if (MoveFunctions.getValue(killerMoves[0][thisDepth]) >= 1 && verifyMove(side, killerMoves[0][thisDepth], true, checkInfo) 
                                && (killerMoves[0][thisDepth] & 65535) != (killerMoves[1][thisDepth]  & 65535)) {
                            if (hashArr[0] != (killerMoves[0][thisDepth] & 65535) && hashArr[1] != (killerMoves[0][thisDepth] & 65535)) {
                                moveArr[index++] = killerMoves[0][thisDepth] & 65535;
                                hashArr[noHash++] = killerMoves[0][thisDepth] & 65535;
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
                    
                    if (MoveFunctions.getValue(killerMoves[1][thisDepth]) == 0 && verifyMove(side, killerMoves[1][thisDepth], true,checkInfo)) {
                       if( (hashArr[0] & 65535) != (killerMoves[1][thisDepth] & 65535) && (hashArr[1] & 65535) != (killerMoves[1][thisDepth] & 65535 )) {
                            moveArr[index++] = killerMoves[1][thisDepth] & 65535;
                            hashArr[noHash++] = killerMoves[1][thisDepth] & 65535;
                       }
                    }
                    if (MoveFunctions.getValue(killerMoves[0][thisDepth]) == 0 && verifyMove(side, killerMoves[0][thisDepth], true, checkInfo) 
                            && (killerMoves[0][thisDepth] & 65535) != (killerMoves[1][thisDepth] & 65535 )) {
                        if( (hashArr[0] & 65535) != (killerMoves[0][thisDepth] & 16777215) && (hashArr[1] & 65535) != (killerMoves[0][thisDepth] & 65535)) {
                            moveArr[index++] = killerMoves[0][thisDepth] & 65535;
                            hashArr[noHash++] = killerMoves[0][thisDepth] & 65535;
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
               
                theMove = moveArr[i];
                
                if( theMove == excludedMove ) {
                    continue;
                }
                
                if( state == BAD_CAPS || state == NON_CAPS || state == GOOD_CAPTURES ) {
                    boolean bDuplicate = false;
                    for( int m=0; m<noHash; m++)
                    {
                        if( hashArr[m]  == theMove ) {
                            bDuplicate = true;
                            break;
                        }
                    }
                    if( bDuplicate ) {
                        continue;
                    }
                } 
               
                if( !chessBoard.CheckMove(side, theMove, checkInfo) ) {
                    continue;
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
                    
                    if( initialGain < 0 && (SEE.GetSEE_NoPins(side, to, from, type, 0)) < 0)
                    {
                        moveArr[badCapIndex--] = theMove;
                        continue;
                    }
                }
                
                boolean checkingMove = chessBoard.MoveGivesCheck(side, theMove, checkInfo);
                int extendAmount = checkingMove ? Global.PLY : 0;
                
                //singular extension
                /*if( extendAmount == 0 && theMove == singular_move ) {
                    int testVal = singular_value - depth / 2;
                    thisDepth--;
                    int singular_testVal = Max(side, depth / 2, testVal-1, testVal, true, isInCheck, false, true, singular_move);
                    if( singular_testVal < testVal) {
                        extendAmount += Global.PLY;
                    }
                    //singular_testVal = Max(side, depth / 2, testVal-1, testVal, true, isInCheck, false, true, singular_move);
                }*/
                
                int reps = chessBoard.MakeMove(theMove, true, checkingMove, checkInfo);
              
                assert( !inCheck( side ));
                assert( checkingMove == inCheck( side^1));
                
                if( reps >= 2)
                {
                    oneLegal = true;
                    value = 0;
                }
                else
                {
                    //recognize moves involving passed pawns
                    //do not want to forward prune/lmr these moves
                    boolean passedPawnMove = false;
                    if ((chessBoard.getTotalValue() < Global.totalValue * 0.4) && extendAmount == 0 && !isInCheck && ((piece % 6) == 5 && ((to >> 3) == 6 || (to >> 3) == 1))) {	//extention for close to promotion
                        passedPawnMove = true;
                        extendAmount += Global.PLY;
                    }
                    else if (piece % 6 == 5) {
                        passedPawnMove = Evaluation2.isPassedPawn(side, to); 
                    }
                    
                    /** we have a legal move */
                    oneLegal = true;

                    // futility pruning code
                    if (bFutilityPrune && extendAmount == 0 && !isInCheck && !passedPawnMove) {
                        scoreThreshold = chessBoard.GetMaterialScore(side);
                        //int neededValue = alpha - futilityMargin;
                        //scoreThreshold = -Evaluation2.getEval(side^1, neededValue, neededValue+1, thisDepth);
         
                        if (scoreThreshold + futilityMargin <= alpha) {
                            numberOfSkippedNodesFP++;
                            chessBoard.UnMake(theMove, true);
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
                    
                    int nextDepth = depth - Global.PLY + extendAmount;
                   
                    if (moveCount == 0) {
                        if (nextDepth >= Global.PLY) {
                            value = -Max(SwitchSide(side), nextDepth, -beta, -alpha, false, checkingMove, extendAmount > 0, false, 0);
                        } else {
                            value = -Quies(SwitchSide(side), 0, -beta, -alpha, false);
                        }    
			thisDepth--;
                    } else {
                        if (nextDepth >= Global.PLY) {
                            value = -Max(SwitchSide(side), nextDepth, -alpha - 1, -alpha, false, checkingMove, extendAmount > 0, false, 0);
                        } else {
                            value = -Quies(SwitchSide(side), 0, -alpha - 1, -alpha, false);
                        }
			thisDepth--;
                        if (value > alpha && value < beta) {
                            if (lmr) {
                                nextDepth = depth - Global.PLY;
                            }
                            if (nextDepth >= Global.PLY) {
                                value = -Max(SwitchSide(side), nextDepth, -beta, -alpha, false, checkingMove, extendAmount > 0, false, 0);
                            } else {
                                value = -Quies(SwitchSide(side), 0, -beta, -alpha, false);
                            }
                            thisDepth--;
                        } else if (value > alpha && (lmr)) {       //use normal depth if lmr move looks interesting
                            nextDepth = depth - Global.PLY;  
                            if (nextDepth >= Global.PLY) {   
                                    value = -Max(SwitchSide(side), nextDepth, -beta, -alpha, false, false, false, false, 0);
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
                        ttMove = theMove;
                        if (value >= beta) {     
                            
                            HashTable.addHash(ttMove, value, effectiveDepth, Global.SCORE_LOWER, nullFail, hashLockValue);
                            
                            Hist[side][piece%6][to] += effectiveDepth * effectiveDepth;
                            if (state == NON_CAPS) {								
                                int killerMove = MoveFunctions.makeKillerMove(theMove, piece);
                                if (killerMove != killerMoves[0][thisDepth]) {
                                    int temp1 = killerMoves[0][thisDepth];
                                    if (value >= (Global.MATE_SCORE - Global.MAX_DEPTH)) {  //mark this move as a mate killer
                                        killerMoves[0][thisDepth] = MoveFunctions.setValue(killerMove, 1);
                                    }
                                    else {
                                        killerMoves[0][thisDepth] = killerMove;
                                    }
                                    killerMoves[1][thisDepth] = temp1;
                                }
                            }
                            return value;
                        }
       
                        hType = Global.SCORE_EXACT;
                        alpha = value;
                    }
                    bMove = value;
                }
                moveCount++;
            }  
            state++;
        }  
      
        
        if( excludedMove != 0 && !oneLegal )
        {
            bMove = alpha;
            HashTable.addHash(ttMove, bMove, effectiveDepth, hType, nullFail, hashLockValue);
        }
        else if (!isInCheck && !oneLegal) {				//stalemate detection
            bMove = 0;
            HashTable.addHash(ttMove, bMove, effectiveDepth, Global.SCORE_TERMINAL, nullFail, hashLockValue);
        } else if (isInCheck && !oneLegal) {     //checkmate detection
            bMove = -(Global.MATE_SCORE - thisDepth);
            HashTable.addHash(ttMove, -Global.MATE_SCORE, effectiveDepth, Global.SCORE_TERMINAL, nullFail, hashLockValue);
            return bMove;
        }
        else
        {
            //if we have skipped nodes due to futility pruning, then we adjust the transposition table entry
            // if we previously had an exact score, it is really a lower bound
            // if we have an upper bound, instead of storing the best score found, we store alpha
            if (numberOfSkippedNodesFP > 0) {
                if (hType == Global.SCORE_EXACT) {
                    hType = Global.SCORE_LOWER;
                }
                else if (hType == Global.SCORE_UPPER) {
                    bMove = alpha;
                }
            }
            HashTable.addHash(ttMove, bMove, effectiveDepth, hType, nullFail, hashLockValue);  
        }
        
        if (oneLegal && hType == Global.SCORE_EXACT) {             //update history tables
            Hist[side][chessBoard.piece_in_square[ttMove&63]%6][MoveFunctions.getTo(ttMove)]+= effectiveDepth * effectiveDepth;
        }
        
        return bMove;
   }

    public boolean IsCheckDangerous( int side, int move  )
    {
        int to = MoveFunctions.getTo(move);
        int from = MoveFunctions.getFrom(move);
        long occupancy = chessBoard.bitboard ^ chessBoard.pieceBits[side^1][Global.PIECE_KING] ^ (1L << from);
        long kingMoves = chessBoard.getAttackBoard(chessBoard.pieceList[Global.PIECE_KING + 6 * (side^1)][0]);
        long newAttack = chessBoard.getAttackBoard(to, occupancy);
        long oldAttack = chessBoard.getAttackBoard(to);
        if( !chessBoard.MoreThanOne(kingMoves & ~(chessBoard.pieceBits[side][Global.PIECE_ALL] | newAttack | (1L << to) ) ) ) {
            return true;
        }
        
        if( chessBoard.piece_in_square[from] % 6 == 3 && ( (kingMoves & (1L << to)) != 0 ) ) {
            return true;
        }
        
        long threats = (chessBoard.pieceBits[side^1][Global.PIECE_ALL] ^ chessBoard.pieceBits[side^1][Global.PIECE_KING]) & newAttack & ~oldAttack;
        if( threats != 0 ) {
            return true;
        }
        
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
     private int Quies(int side, int depth, int alpha, int beta, boolean bInCheck) {
        /* try {
            buffWriter.write("quies "+chessBoard.hashValue+" "+"alpha "+alpha+" beta "+beta);
        } catch (Exception ex2) {};*/
 
        assert( bInCheck == inCheck( side ));
         
        thisDepth++;
        
        nodes++;
        
        int hashIndex = HashTable.hasHash(chessBoard.hashValue);
        int hDepth = (bInCheck || depth > Global.QUIES_CHECK_MAX_DEPTH) ? Global.HASH_QUIES : Global.HASH_QUIES_CHECK;
        if( hashIndex != -1) {
            hashHits++;
            HashTable.setNew(hashIndex);
            
            int hVal = HashTable.getValue(hashIndex);
            int entryDepth = HashTable.getDepth(hashIndex);
            switch (HashTable.getType(hashIndex)) {
                case (Global.SCORE_UPPER):
                    if (entryDepth >= hDepth && hVal <= alpha) {
                        nodes++;
                        return hVal;
                    }   
                break;
                case (Global.SCORE_EXACT):
                    if (entryDepth >= hDepth && hVal > alpha && hVal < beta) {
                        nodes++;
                        return hVal;
                    }
                break;
                case (Global.SCORE_LOWER):
                    if (entryDepth >= hDepth && hVal >= beta) {
                        nodes++;
                        return hVal;
                    }
                break;
                case (Global.SCORE_TERMINAL):
                    if (hVal == -Global.MATE_SCORE) {
                        nodes++;
                        hVal += thisDepth;
                    }
                    return hVal;   
            }  
        }
        
        int bValue = -Global.MATE_SCORE + thisDepth;
        int testValue = 0;
        int value = 0;
       
        if( !bInCheck ) {
            value = Evaluation2.getEval(side, alpha, beta, thisDepth); 
            if (value > alpha) {
                if (value >= beta) {
                    return value;
                }
                alpha = value;
            }
            bValue = testValue = value;
        }

        int index = 0;
        
        boolean oneLegal = false;
        
        int state = hashIndex == -1 ? QUIES_MOVES : QUIES_HASH;
        
        int[] capArr = new int[60];
        int hType = Global.SCORE_UPPER;
        int bestMove = 0;
        int hashMove = 0;
        Board.CheckInfo checkInfo = chessBoard.GetCheckInfo();
        while( state < QUIES_END )
        {
            switch( state )
            {
                case( QUIES_HASH ):
                    capArr[0] = HashTable.getMove(hashIndex);
                    int type = MoveFunctions.moveType(capArr[0]);
                    if( !bInCheck && !(type == Global.ORDINARY_CAPTURE || type == Global.PROMO_Q || type == Global.EN_PASSANT_CAP)) {
                        break;
                    }
                    else if( verifyMove(side, capArr[0], false, checkInfo) ) {                             
                        hashMove = capArr[0];
                        index = 1;
                    }
                break;
                case( QUIES_MOVES ):
                    if( bInCheck) {
                        index = getCheckEscapes(side, capArr);
                    }
                    else {
                        index = getCaptures(side, capArr);
                    }      
                 break;
                    
                case( QUIES_CHECKING ):
                     /*{
                       int testArr1[] = new int[128];
                        int testIndex1 = getMoves( side, testArr1, 0 );
                        int iNumChecks = 0;
                        for (int i = testIndex1 - 1; i >= 0; i--) {
                            if( chessBoard.MoveGivesCheck( side, testArr1[i], checkInfo)) {
                                iNumChecks++;
                            }
                        }
                        
                        int testArr2[] = new int[128];
                        int testIndexChecks = getCheckingMoves(side, testArr2, 0, checkInfo);
                        
                        if( iNumChecks != testIndexChecks ) {
                           
                            for (int i = testIndex1 - 1; i >= 0; i--) {
                                if( chessBoard.MoveGivesCheck( side, testArr1[i], checkInfo)) {
                                    int from = MoveFunctions.getFrom(testArr1[i]);
                                    int to = MoveFunctions.getTo(testArr1[i]);
                                    int TestType = MoveFunctions.moveType(testArr1[i]);
                                    System.out.println( "piece is "+chessBoard.piece_in_square[from]+" type is "+TestType+" from is "+from+" to is "+to );
                                    iNumChecks++;
                                }
                            }
                            System.out.println();
                            
                            for (int i = testIndexChecks - 1; i >= 0; i--) {
                                if( chessBoard.MoveGivesCheck( side, testArr2[i], checkInfo)) {
                                    int from = MoveFunctions.getFrom(testArr2[i]);
                                    int to = MoveFunctions.getTo(testArr2[i]);
                                    int TestType = MoveFunctions.moveType(testArr2[i]);
                                    System.out.println( "piece is "+chessBoard.piece_in_square[from]+" type is "+TestType+" from is "+from+" to is "+to );
                                    iNumChecks++;
                                }
                            }
                        }
                    }*/
                    
                    index = getCheckingMoves(side, capArr, 0, checkInfo);
                break; 
            }
            
            for (int i = index - 1; i >= 0; i--) {
                
                int to = MoveFunctions.getTo(capArr[i]);
                int from = MoveFunctions.getFrom(capArr[i]);
                
                boolean bGivesCheck = ( state == QUIES_CHECKING || chessBoard.MoveGivesCheck(side, capArr[i], checkInfo) );
                
                if (state != QUIES_HASH ) {
                    
                    if( capArr[i] == hashMove ) {  
                        continue;
                    }
                    
                    int type = MoveFunctions.moveType(capArr[i]);
                    
                    //if( !bNodePV && !bInCheck && bGivesCheck && type == Global.ORDINARY_MOVE && !IsCheckDangerous( side, capArr[i] ) ) {
                    //    continue;
                    //}
                    
                    /*int enemyKing = chessBoard.pieceList[4 + (side^1) * 6][0];
                    boolean bDiscoveredCheck = false;
                    if( (chessBoard.piece_in_square[from] % 6) < 4 ) {
                        if((checkInfo.dcCandidates & (1L << from)) != 0 ) {
                            bDiscoveredCheck = true;
                        }
                    }else if( (checkInfo.dcCandidates & (1L << from)) != 0 && ((Global.mask_between[enemyKing][from] & (1L << to)) == 0) && ((Global.mask_between[enemyKing][to] & (1L << from)) == 0) ) {
                        bDiscoveredCheck = true;  
                    }*/
                    
                    if (!bInCheck && state == QUIES_MOVES  ) {
                    
                        if( (type == Global.ORDINARY_CAPTURE || type == Global.PROMO_Q || type == Global.EN_PASSANT_CAP) ) {

                            int neededScore = bGivesCheck ? 0 : (int)Math.max(0, (alpha - testValue));
                            int initialGain = 0;
                            if( type < Global.PROMO_Q) {
                                initialGain = Global.values[chessBoard.piece_in_square[to]]- Global.values[chessBoard.piece_in_square[from]];
                            } else if( type < Global.EN_PASSANT_CAP) {
                                initialGain = Global.values[3] - Global.values[5];
                            }

                            if( initialGain < neededScore && (SEE.GetSEE_PinsPlus(side, to, from, type, neededScore)) < neededScore)
                            {
                                bValue = alpha;
                                continue;
                            }   
                        }
                    } 
                    else if( state == QUIES_CHECKING ) {
                        
                        if( SEE.GetSEE_PinsPlus(side, to, from, type, 0) < 0 ) {
                            bValue = alpha;
                            continue;
                        }
                    }
                }
                
                if( !chessBoard.CheckMove(side, capArr[i], checkInfo) ) {
                    continue;
                }
                
                int reps = chessBoard.MakeMove(capArr[i], true, bGivesCheck, checkInfo);	
                
                assert ( bGivesCheck == inCheck(side^1) );
                
                if( reps >= 2) {
                    oneLegal = true;
                    value = 0;
                    chessBoard.UnMake(capArr[i], true);
                }
                else { 
                    oneLegal = true;

                    value = -Quies(SwitchSide(side), depth + 1, -beta, -alpha, bGivesCheck);   
                    thisDepth--;
                    chessBoard.UnMake(capArr[i], true);
                }
                if (value > bValue) {
                    if (value >= beta) {
                        HashTable.addHash(capArr[i], value, hDepth, Global.SCORE_LOWER, 0, chessBoard.hashValue);
                        return value;
                    }
                    bValue = value;
                    if (value > alpha) {
                        bestMove = capArr[i];
                        hType = Global.SCORE_EXACT;
                        alpha = value;
                    }
                }
            }
            
            if( state == QUIES_MOVES) {
                if( bInCheck || depth > Global.QUIES_CHECK_MAX_DEPTH ) {
                    state = QUIES_END;
                } else {
                    state++;
                }
            }
            else {
                state++;
            }
        }
        if( bInCheck ) {
            if( !oneLegal) {
               HashTable.addHash(0, -Global.MATE_SCORE, hDepth, Global.SCORE_TERMINAL, 0, chessBoard.hashValue);
               return bValue;
            }
        }
        
        HashTable.addHash(bestMove, bValue, hDepth, hType, 0, chessBoard.hashValue);
        
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
        //Perft(chessBoard.getTurn(), depth, inCheck(chessBoard.getTurn()));
        PerftDebug(chessBoard.getTurn(), depth, inCheck(chessBoard.getTurn()));
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
        
        Board.CheckInfo checkInfo = chessBoard.GetCheckInfo();
        for (int i = index - 1; i >= 0; i--) {
            perft = 0;
            //int reps = chessBoard.MakeMove(moveArr[i], false);		
            int to = MoveFunctions.getTo(moveArr[i]);
            int from = MoveFunctions.getFrom(moveArr[i]);
            
            if( !chessBoard.CheckMove(side, moveArr[i], checkInfo) ) {
                continue;
            }
            boolean bGivesCheck = chessBoard.MoveGivesCheck(side, moveArr[i], checkInfo);
            chessBoard.MakeMove(moveArr[i], true, bGivesCheck, checkInfo);
            Perft(SwitchSide(side), depth - 1, bGivesCheck);
            
            System.out.print(" to is "+to+" from is "+from+" ");
            
            chessBoard.UnMake(moveArr[i], false);
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
    private void PerftDebug(int side, int depth, boolean inCheck) {
        if (depth == 0) {
            perft++;
            return;
        }
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

        if(PERFT_TRANSTABLE) {
            int hashIndex = HashTable.hasHash(chessBoard.hashValue);
            if (hashIndex != -1 ) {
                if(HashTable.getDepth(hashIndex) == depth) {
                    perft += HashTable.getValue(hashIndex);
                    return;
                }
            }
        }
        long perftBefore = perft;
        
        int index;
        int[] moveArr = new int[128];
        
        if (!inCheck) {
            index = getCaptures(side, moveArr);
            index = getMoves(side, moveArr, index);
        } else {
            index = getCheckEscapes(side, moveArr);
        }
        Board.CheckInfo checkInfo = chessBoard.GetCheckInfo();
        for (int i = index - 1; i >= 0; i--) {
            if( !chessBoard.CheckMove(side, moveArr[i], checkInfo) ) {
                continue;
            }
            boolean bGivesCheck = chessBoard.MoveGivesCheck(side, moveArr[i], checkInfo);
            chessBoard.MakeMove(moveArr[i], true, bGivesCheck, checkInfo);
            PerftDebug(SwitchSide(side), depth - 1, bGivesCheck);
            chessBoard.UnMake(moveArr[i], true);
        }
        if(PERFT_TRANSTABLE) {
            HashTable.addHash(0 , (int)(perft - perftBefore), depth, 0, 0, chessBoard.hashValue);
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
    private void Perft(int side, int depth, boolean inCheck) {
        int index;
        int[] moveArr = new int[128];
        if (!inCheck) {
            index = getCaptures(side, moveArr);
            index = getMoves(side, moveArr, index);
        } else {
            index = getCheckEscapes(side, moveArr);
        }
        Board.CheckInfo checkInfo = chessBoard.GetCheckInfo();
        for (int i = index - 1; i >= 0; i--) {
            
            if( !chessBoard.CheckMove(side, moveArr[i], checkInfo) ) {
                continue;
            }
            boolean bGivesCheck = chessBoard.MoveGivesCheck(side, moveArr[i], checkInfo);
            
            chessBoard.MakeMove(moveArr[i], true, bGivesCheck, checkInfo);
            
            if( depth > 1) {
                Perft(SwitchSide(side), depth - 1, bGivesCheck);
            } 
            else {
                perft++;
            }
            chessBoard.UnMake(moveArr[i], true);
        }
    }
}
