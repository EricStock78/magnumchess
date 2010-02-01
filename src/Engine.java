
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

import java.util.Arrays;
import java.io.*;
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
	
    /** chessBoard object from singleton class Board represents chess board and associated datastructures */
    private Board chessBoard;
	
    /** counter for positions evaluated */
    private static long nodes;			//counter for evaluated positions
	
    private static long hashnodes;		//counter for evaluated positions plus hash table exact hits
	
    /** 2D 64X64 arrays for history heuristic move ordering enhancement */
    private static final int[][] Hist = new int[64][64];
	private static final int[][] Hist2 = new int[64][64];
	
    /** initialization of Transposition Table */
    private static TransTable HashTable = new TransTable(Global.HASHSIZE,0);
	
    /** array for storing moves from the root position */
    private static int[] moveOrder = new int[128];
	
    /** 2D array to store killer moves - 2 slots for each ply in the search */
	private static final int[][] killerMoves = new int[100][2];
	
    /** variables used to store time information - time in milliseconds*/
    private static long mills1, mills2;					
	
    /** global boolean flag to trigger then end of the search when out of time */
    private static boolean stop;
	
    /** stores node count at which a time check should occur */
    private static int nextTimeCheck;
    
    /** constants used to represent different steps of search */
	private static final int SEARCH_HASH = 1;
	private static final int KILLER_MOVES =4;
	private static final int MATE_KILLER =2;
	private static final int GOOD_CAPTURES = 3;
	private static final int NON_CAPS = 5;
	private static final int BAD_CAPS = 6;
	private static final int SEARCH_END = 7;	
    
    /** constants used to represent the 3 types of positions stored in the hash table */
    private static final int HASH_EXACT = 1;
	private static final int HASH_ALPHA = 0;
	private static final int HASH_BETA  = 2;
    
    /** constants for starting values of search */
    private static final int ALPHA_START = Integer.MIN_VALUE+100;
	private static final int BETA_START = Integer.MAX_VALUE-100;
	private static final int VALUE_START = Integer.MIN_VALUE+100;
    
	/** variable used to track how deep in the search we are */
    private static int thisDepth;
	
	/** boolean which represents if we are using infinite time control */
    private static boolean infinite = false;			//flag for infinite time controls
	
    /** used to track the age of hash table entries */
    private int ancient;
    
    /** count of moves generated when debugging move gen using perft command */
	private long perft = 0;	
	
    
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
     * Method timeLeft
     * 
     * checks to see if we are out of time...if not, schedules next time check
     * 
     * @return boolean - are we out of time?
     */
    public boolean timeLeft() throws IOException {
		if(Main.reader.ready()) {	
			if("stop".equals(Main.reader.readLine())) 
				return false;
		}
		long temp = System.currentTimeMillis();
		if(!infinite) {
			if(mills2>temp) {
				nextTimeCheck += Math.min(25000,((mills2-temp)*15));
				return true;
			}else {
                return false;
			}
		}
		else {
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
		HashTable = new TransTable(Global.HASHSIZE,0);
        //add code to reset the eval table

        //add code to reset the pawn table
	}
	
    /** 
     * Method search
     *  
     * To-do - This method is due for a redesign and re-write 
     * currently very messy
     * furthermore, several optimizations exist for initally ordering the moves at the root
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
	public String search(int time,int searchDepth,boolean inf) {
        
		/** side moving */
        int theSide;
        
        /** array of moves */
        int[] moveArr = new int[128];
		
        /** alpha and beta vars */
        int alpha;							
		int beta;						
		
        /** temporary varible to store the value returned from a search */
        int value;													
		
        /**flag to see if legal move can be made */
        boolean ok = true;								
		boolean isExact;
		boolean hasValue;
		int noLegal;
		int best = 0;							
		int temp;								
		int index;
		int bMove;								
		int bestValue = 0;
		int reps;
		
        /** ancient node value betweeen 0 and 7; */
        ancient = (chessBoard.getCount()/2) % 8;			
		infinite = inf;

        /** here we prepare the history arrays for the next search */
        if((chessBoard.getCount()/2 % 8) == 7) {
			for(int i = 0; i<64; i++) {
				for(int j = 0; j < 64; j++) {
					Hist[i][j] = 0;
					Hist2[i][j] = 0;
				}
			}
		} else {
            for(int i = 0; i<64; i++) {
			   for(int j = 0; j < 64; j++) {
				 Hist[i][j] /= 10;
				 Hist2[i][j] /= 10;
			   }
		    } 
        }

        /** here the killer moves are cleared out before the next search */
        for (int j=0; j<25; j++) {
			killerMoves[j][0] = 0;
			killerMoves[j][1] = 0;
		}
        
        theSide = chessBoard.getTurn();
		mills1 = System.currentTimeMillis();
		mills2 = mills1 + time;
		stop = false;
        
        /** assumes program is searching 15000 moves per second, if not will take too long */
		nextTimeCheck = Math.min(1000, time * 15);			
		nodes = 0;
		hashnodes = 0;
		
		/** collect the root moves */
        if(!inCheck(theSide)) {
			int[] capArr = new int[60];
			index = getMoves(theSide, moveArr,0);	
			int index2 = getCaptures(theSide, capArr);
		
			for(int i = index; i<(index+index2);i++) {
				moveArr[i] = capArr[i-index];
			}
			index += index2;
			Arrays.sort(moveArr,0,index);
		} else 
			index = getCheckEscapes(theSide, moveArr);
		
		
		if (ok) {
			hasValue = false;
			
            /** iteratively deepened search starting at depth 2 */
            for (int depth = 2; depth <= searchDepth; depth++) {		
				if (stop && hasValue) break;
				value = VALUE_START;
				alpha = ALPHA_START;
				beta = BETA_START;
				bMove = Integer.MIN_VALUE;
				theSide = chessBoard.getTurn();
				isExact = false;
				noLegal = 0;
				thisDepth = 0;
				for (int i = index - 1; i >= 0; i--) {	
					temp = moveArr[i];
					reps = chessBoard.makeMove(temp,false,true);
					if(inCheck(theSide)) {
						moveArr[i] &= 8388607;
						chessBoard.unMake(temp,false,true);
						continue;
					}
					noLegal++;
					
                    /** to do 
                     *  handle these cases only during the first iteration
                     *  currently this is wasteful doing it every iteration
                     */
                    
					if (reps >= 2)
						value = 0;
					else if (chessBoard.getDraw() == 100)
						value = 0;
					else if (isStaleMate(-theSide)) 
						value = 0;
					else if (!isExact || depth < 3) {				
								
						value = -Max(-theSide, depth-1, -beta, -alpha, false,inCheck(-theSide), false, false);
						thisDepth--;
					} else {
						value = -Max(-theSide, depth-1, -alpha-1, -alpha, false,inCheck(-theSide), false, false);
						thisDepth--;
						if (value > alpha) {
							value = -Max(-theSide, depth-1, -beta, -alpha, false,inCheck(-theSide), false, false);		
							thisDepth--;
						}
					}	
					if ((((value > bMove)&& !stop) || !hasValue)  ) {
						alpha = value;
						isExact = true;
						hasValue = true;
						bestValue = value;
						bMove = value;
						best = temp;
						moveArr[i] &= 8388607;
						moveArr[i] |= ((200 - i) << 23);
					} else {
						moveArr[i] &= 8388607;
						moveArr[i] |= ((i ) << 23);
					}							
					chessBoard.unMake(temp,false,true);
				}
				long mills3 = System.currentTimeMillis();
				mills3 = mills3 - mills1;
				String pv = HistoryWriter.getUCIMove((best>>6) & 63, best & 63, (best >> 12) & 15);
				if (stop)
					depth--;
				if(bestValue > 10000) {			//this is a mate score
					int mate = (20000-bestValue)/2;
					System.out.println("info depth "+depth+" score mate "+mate+" nodes "+nodes+" pv "+pv);
				} else if(bestValue < -10000)	{
					int mate = (-20000-bestValue)/2;
					System.out.println("info depth "+depth+" score mate "+mate+" nodes "+nodes+" pv "+pv);
				
				} else {
					System.out.println("info depth "+depth+" score cp "+bestValue+" nodes "+nodes+" pv "+pv);
				}
				Arrays.sort(moveArr, 0, index);
				
			}
			
			reps = chessBoard.makeMove(best, true, true);
		
			//System.out.println("info string hash hits is "+hashnodes);
			//System.out.println("info string hashCount is "+HashTable.getCount());
			//System.out.println("info string total value is "+chessBoard.getTotalValue());
			//System.out.println("pawn hash hits is "+Global.pawnHashHits);
			//System.out.println("pawn hash miss is "+Global.pawnHashMiss);
            return HistoryWriter.getUCIMove(best);
		}
		return "";
	}

	/**********************************************************************
	This method tests whether a side is in check
	parameters - int side ....the side possibly in check
	**********************************************************************/	
	private boolean inCheck(int side) {  
		
		if(side==1) 					//black
			return chessBoard.isBlackAttacked(chessBoard.getPos(chessBoard.blackking));
		else 							//white
			return chessBoard.isWhiteAttacked(chessBoard.getPos(chessBoard.whiteking));
	}
	
	
	/************************************************************************
	This method will determine if the game is over due to a stalemate
	parameters int side - the side to move next
	************************************************************************/
	private boolean isStaleMate(int side) {
		
		int[] Moves;
		int temp;
		int index;
		if(!inCheck(side)) {				//side to move must not be in check...otherwise could be checkmate

			Moves = new int[128];
			index = getMoves(side,Moves,0);
			for(int i=0;i<index;i++) {
				temp = Moves[i];
				chessBoard.makeMove(temp,false,false);
				if(!inCheck(side)) {
					chessBoard.unMake(temp,false,false);
					return false;
				}
				else 
					chessBoard.unMake(temp,false,false);	
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
		long fromBit;
		int toSq;
		int toSq2;
		int bit;
		int to;
		int from;
		int index = start;
		int type;
		if (side == -1) {													//white moving
			pieces = chessBoard.whitepawns;
			long moves = pieces<<8 & ~chessBoard.bitboard & ~Global.rankMasks[7];
			long doubleMoves = moves<<8 & Global.rankMasks[3] & ~chessBoard.bitboard;
			toSq = (int)moves;
			toSq2 = (int)(moves>>>32);
			while(toSq != 0) {
				bit = toSq & -toSq;
				toSq ^= bit;
				to = chessBoard.getPos(bit);
				from = to-8;
				moveOrder[index] = Hist2[from][to];           
				Moves[index++] = MoveFunctions.makeMove(to,from,5,-1,Global.ORDINARY_MOVE,60);
			}
			while(toSq2 != 0) {
				bit = toSq2 & -toSq2;
				toSq2 ^= bit;
				to = chessBoard.getPos(bit)+32;
				from = to-8;
				moveOrder[index] = Hist2[from][to];
				Moves[index++] = MoveFunctions.makeMove(to,from,5,-1,Global.ORDINARY_MOVE,60);
			}
			toSq = (int)doubleMoves;
			while( toSq != 0) {
				bit = toSq & -toSq;
				toSq ^= bit;
				to = chessBoard.getPos(bit);
				from = to - 16;
				moveOrder[index] = Hist2[from][to];         
				Moves[index++] = MoveFunctions.makeMove(to,from,5,-1,Global.ORDINARY_MOVE,60);
			}	
			
			pieces = chessBoard.whiteknights;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= ~chessBoard.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = chessBoard.getPos(bit);	
					moveOrder[index] = Hist2[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,1,-1,Global.ORDINARY_MOVE,60);			
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = chessBoard.getPos(bit)+32;	
					moveOrder[index] = Hist2[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,1,-1,Global.ORDINARY_MOVE,60);			
				}
			}
			pieces = chessBoard.whitebishops;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= ~chessBoard.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = chessBoard.getPos(bit);
					moveOrder[index] = Hist2[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,2,-1,Global.ORDINARY_MOVE,60);		
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = chessBoard.getPos(bit)+32;
					moveOrder[index] = Hist2[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,2,-1,Global.ORDINARY_MOVE,60);		
				}
			}	
			pieces = chessBoard.whiterooks;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= ~chessBoard.bitboard;
				toSq = (int)(toSquares);
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = chessBoard.getPos(bit);
					moveOrder[index] = Hist2[from][to];// + value * valMult;
					Moves[index++] = MoveFunctions.makeMove(to,from,0,-1,Global.ORDINARY_MOVE,60);		
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = chessBoard.getPos(bit)+32;
					moveOrder[index] = Hist2[from][to];// + value * valMult;
					Moves[index++] = MoveFunctions.makeMove(to,from,0,-1,Global.ORDINARY_MOVE,60);		
				}
			}			
			pieces = chessBoard.whitequeen;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= ~chessBoard.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares >>> 32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = chessBoard.getPos(bit);
					moveOrder[index] = Hist2[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,3,-1,Global.ORDINARY_MOVE,60);				
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = chessBoard.getPos(bit) + 32;
					moveOrder[index] = Hist2[from][to];
                    Moves[index++] = MoveFunctions.makeMove(to,from,3,-1,Global.ORDINARY_MOVE,60);			
				}
			}			
			pieces = chessBoard.whiteking;
			if (pieces != 0) {
				from = chessBoard.getPos(pieces);		
				toSquares = chessBoard.getAttackBoard(from);
				if(chessBoard.wCastle > Global.CASTLED) {							
					long castle = chessBoard.getWKingCastle(from);
					if(chessBoard.wCastle == Global.LONG_CASTLE)
						castle &= Global.set_Mask[2];
                    else if(chessBoard.wCastle == Global.SHORT_CASTLE)
						castle &= Global.set_Mask[6];
					toSquares |=castle;
				}
				toSquares &= ~chessBoard.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>32);
				while (toSq != 0) {
					type = Global.ORDINARY_MOVE;
					bit = toSq & -toSq;
					toSq ^= bit;
					to = chessBoard.getPos(bit);
					if(from == 4 && to == 2)
						type = Global.LONG_CASTLE;
                    else if(from == 4 && to == 6)
						type = Global.SHORT_CASTLE;
					moveOrder[index] = Hist2[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,4,-1,type,60);		
				}
				while (toSq2 != 0) {
					type = Global.ORDINARY_MOVE;
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = chessBoard.getPos(bit)+32;
					moveOrder[index] = Hist2[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,4,-1,type,60);		
				}			
			}
		} else {								
			pieces = chessBoard.blackpawns;
			long moves = pieces>>8 & ~chessBoard.bitboard & ~Global.rankMasks[0];
			long doubleMoves = moves>>8 & Global.rankMasks[4] & ~chessBoard.bitboard;
			toSq = (int)moves;
			toSq2 = (int)(moves>>>32);
			while(toSq != 0) {
				bit = toSq & -toSq;
				toSq ^= bit;
				to = chessBoard.getPos(bit);
				from = to+8;
				moveOrder[index] = Hist[from][to];
				Moves[index++] = MoveFunctions.makeMove(to,from,11,-1,Global.ORDINARY_MOVE,60);	
			}
			while(toSq2 != 0) {
				bit = toSq2 & -toSq2;
				toSq2 ^= bit;
				to = chessBoard.getPos(bit)+32;
				from = to+8;
				moveOrder[index] = Hist[from][to];
				Moves[index++] = MoveFunctions.makeMove(to,from,11,-1,Global.ORDINARY_MOVE,60);	
			}
			toSq2 = (int)(doubleMoves>>>32);
			while(toSq2 != 0) {
				bit = toSq2 & -toSq2;
				toSq2 ^= bit;
				to = chessBoard.getPos(bit)+32;
				from = to+16;
				moveOrder[index] = Hist[from][to];
				Moves[index++] = MoveFunctions.makeMove(to,from,11,-1,Global.ORDINARY_MOVE,60);	
			}
			pieces = chessBoard.blackknights;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= ~chessBoard.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = chessBoard.getPos(bit);
					moveOrder[index] = Hist[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,7,-1,Global.ORDINARY_MOVE,60);		
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = chessBoard.getPos(bit)+32;
					moveOrder[index] = Hist[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,7,-1,Global.ORDINARY_MOVE,60);		
				}
			}
			pieces = chessBoard.blackbishops;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= ~chessBoard.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = chessBoard.getPos(bit);
					moveOrder[index] = Hist[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,8,-1,Global.ORDINARY_MOVE,60);		
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = chessBoard.getPos(bit)+32;
					moveOrder[index] = Hist[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,8,-1,Global.ORDINARY_MOVE,60);		
				}
			}
			pieces = chessBoard.blackrooks;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= ~chessBoard.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = chessBoard.getPos(bit);
					moveOrder[index] = Hist[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,6,-1,Global.ORDINARY_MOVE,60);			
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = chessBoard.getPos(bit)+32;
					moveOrder[index] = Hist[from][to];
					Moves[index++] = MoveFunctions.makeMove(to,from,6,-1,Global.ORDINARY_MOVE,60);			
				}
			}					
			pieces = chessBoard.blackqueen;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= ~chessBoard.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = chessBoard.getPos(bit);
					moveOrder[index] = Hist[from][to];
					Moves[index++] =  MoveFunctions.makeMove(to,from,9,-1,Global.ORDINARY_MOVE,60);			
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = chessBoard.getPos(bit) + 32;
					moveOrder[index] = Hist[from][to];
					Moves[index++] =  MoveFunctions.makeMove(to,from,9,-1,Global.ORDINARY_MOVE,60);			
				}
			}		
			pieces = chessBoard.blackking;
			if (pieces != 0) {
				from = chessBoard.getPos(pieces);		
				toSquares = chessBoard.getAttackBoard(from);
				
				if(chessBoard.bCastle > Global.CASTLED) {							
					long castle = chessBoard.getBKingCastle(from);
					if(chessBoard.bCastle == Global.LONG_CASTLE)
						castle &= Global.set_Mask[58];
                    else if(chessBoard.bCastle == Global.SHORT_CASTLE)
						castle &= Global.set_Mask[62];
					toSquares |=castle;
				}
				toSquares &= ~chessBoard.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = chessBoard.getPos(bit);
					moveOrder[index] = Hist[from][to];
					Moves[index++] =  MoveFunctions.makeMove(to,from,10,-1,Global.ORDINARY_MOVE,60);		
				}
				while (toSq2 != 0) {
					type = Global.ORDINARY_MOVE;
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = chessBoard.getPos(bit)+32;
					if(from == 60 && to == 58)
						type = Global.LONG_CASTLE;
                    else if(from == 60 && to == 62)
						type = Global.SHORT_CASTLE;
					moveOrder[index] = Hist[from][to];
					Moves[index++] =  MoveFunctions.makeMove(to,from,10,-1,type,60);		
				}
			}					
		}
		//Arrays.sort(Moves,start,index);
        sortMoves(start,index,Moves);
		return index;
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
     * @return int - the number of moves added
     * 
     */
    private static final void sortMoves(int start, int noMoves, int[] Moves) {
		boolean done = false;
		for(int i=start ; i<noMoves; i++) {
			if(done)break;
			done = true;
			for(int j = noMoves-1 ; j>i; j--) {
				if(moveOrder[j] < moveOrder[j-1]) {		//swap moves
					int temp = Moves[j];
					Moves[j] = Moves[j-1];
					Moves[j-1] = temp;
					temp = moveOrder[j];
					moveOrder[j] = moveOrder[j-1];
					moveOrder[j-1] = temp;
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
		long pMask;
		if (side == -1) {			//white moving
			passant = chessBoard.getPassantB();
			if(passant == -1)
				pMask = 0;
			else
				pMask = Global.set_Mask[passant];
			enemies = chessBoard.blackpieces;
			pieces = chessBoard.whitepawns;
			long lAttack = (pieces<<7) & (enemies | pMask) & ~Global.fileMasks[7];
			long rAttack = (pieces<<9) & (enemies | pMask) & ~Global.fileMasks[0];
			long promo = pieces & Global.rankMasks[6];
			if(promo != 0) {
				promo <<= 8;
				promo &= ~chessBoard.bitboard;
			}
			while (lAttack != 0) {
				toBit = lAttack & -lAttack;
				lAttack ^= toBit;
				to = chessBoard.getPos(toBit);
				from = to-7;
                type = Global.ORDINARY_MOVE;
                cP = chessBoard.piece_in_square[to];
                if(to == passant) {
                    moveOrder[index] = Global.values[5] + 4;
                    type = Global.EN_PASSANT_CAP;
                }else if(to/8 == 7) {
                    moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to,from,5,cP,Global.PROMO_B,1);
                    moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to,from,5,cP,Global.PROMO_N,1);
                    moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to,from,5,cP,Global.PROMO_R,1);
                    moveOrder[index] = Global.values[cP] + 700;
                    type = Global.PROMO_Q;
                } else
                    moveOrder[index] = Global.values[cP] + 4;
                Captures[index++] = MoveFunctions.makeMove(to,from,5,cP,type,0);
			}
			while (rAttack != 0) {
				toBit = rAttack & -rAttack;
				rAttack ^= toBit;
				to = chessBoard.getPos(toBit);
				from = to-9;
                type = Global.ORDINARY_MOVE;
                cP = chessBoard.piece_in_square[to];
                if(to == passant) {
                    moveOrder[index] = Global.values[5] + 4;
                    type = Global.EN_PASSANT_CAP;
                }else if(to/8 == 7) {
                    moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to,from,5,cP,Global.PROMO_B,1);
                     moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to,from,5,cP,Global.PROMO_N,1);
                    moveOrder[index] = 0;
                    Captures[index++] = MoveFunctions.makeMove(to,from,5,cP,Global.PROMO_R,1);
                    moveOrder[index] = Global.values[cP] + 700;
                    type = Global.PROMO_Q;
                } else
                    moveOrder[index] = Global.values[cP] + 4;
                Captures[index++] = MoveFunctions.makeMove(to,from,5,cP,type,0);
			}
			while(promo != 0) {
				toBit = promo & -promo;
				promo ^= toBit;
				to = chessBoard.getPos(toBit);
				from = to-8;                                                               
                moveOrder[index] = 0;
                Captures[index++] = MoveFunctions.makeMove(to,from,5,-1,Global.PROMO_B,1);
                moveOrder[index] = 0;
                Captures[index++] = MoveFunctions.makeMove(to,from,5,-1,Global.PROMO_N,1);
                moveOrder[index] = 0;
                Captures[index++] = MoveFunctions.makeMove(to,from,5,-1,Global.PROMO_R,1);
                moveOrder[index] = 700;
                Captures[index++] = MoveFunctions.makeMove(to,from,5,-1,Global.PROMO_Q,0);
			}
			pieces = chessBoard.whiteknights;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = chessBoard.getPos(toBit);
                    cP = chessBoard.piece_in_square[to];    
                    moveOrder[index] = Global.values[cP] + 3;
                    Captures[index++] = MoveFunctions.makeMove(to,from,1,cP,Global.ORDINARY_MOVE,0);
				}
			}
			pieces = chessBoard.whitebishops;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = chessBoard.getPos(toBit);
                    cP = chessBoard.piece_in_square[to];
                    moveOrder[index] = Global.values[cP] + 3;
                    Captures[index++] = MoveFunctions.makeMove(to,from,2,cP,Global.ORDINARY_MOVE,0);
				}
			}
			pieces = chessBoard.whitequeen;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = chessBoard.getPos(toBit);
                    cP = chessBoard.piece_in_square[to];
                    moveOrder[index] = Global.values[cP] + 1;   
                    Captures[index++] = MoveFunctions.makeMove(to,from,3,cP,Global.ORDINARY_MOVE,0);
				}
			}
			pieces = chessBoard.whiterooks;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = chessBoard.getPos(toBit);
                    cP = chessBoard.piece_in_square[to];
                    moveOrder[index] = Global.values[cP] + 2;  
                    Captures[index++] = MoveFunctions.makeMove(to,from,0,cP,Global.ORDINARY_MOVE,0);
				}
			}
			pieces = chessBoard.whiteking;
			if (pieces != 0) {
				fromBit = pieces & -pieces;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = chessBoard.getPos(toBit);     
                    cP = chessBoard.piece_in_square[to];
                    moveOrder[index] = Global.values[cP];  
                    Captures[index++] = MoveFunctions.makeMove(to,from,4,cP,Global.ORDINARY_MOVE,0);
				}
			}	
		} else {					//black moving
			passant = chessBoard.getPassantW();
			if(passant == -1)
				pMask = 0;
			else
				pMask = Global.set_Mask[passant];
			enemies = chessBoard.whitepieces;
			pieces = chessBoard.blackpawns;
			long lAttack = pieces>>9 & (enemies | pMask) & ~Global.fileMasks[7];
			long rAttack = pieces>>7 & (enemies | pMask) & ~Global.fileMasks[0];
			long promo = pieces & Global.rankMasks[1];
			if(promo != 0) {
				promo >>=8;
				promo &= ~chessBoard.bitboard;
			}
			while(lAttack != 0) {
				toBit = lAttack & -lAttack;
				lAttack ^= toBit;
				to = chessBoard.getPos(toBit);
				from = to + 9;
                cP = chessBoard.piece_in_square[to];
                type = Global.ORDINARY_MOVE;
                if(to == passant) {
                    moveOrder[index] = Global.values[5] + 4;
                    type = Global.EN_PASSANT_CAP;
                }else if(to/8 == 0) {
                    moveOrder[index] = 0;  
                    Captures[index++] = MoveFunctions.makeMove(to,from,11,cP,Global.PROMO_B,1);
                    moveOrder[index] = 0;  
                    Captures[index++] = MoveFunctions.makeMove(to,from,11,cP,Global.PROMO_N,1);
                    moveOrder[index] = 0;  
                    Captures[index++] = MoveFunctions.makeMove(to,from,11,cP,Global.PROMO_R,1);
                    moveOrder[index] = Global.values[cP] + 700;
                    type = Global.PROMO_Q;
                } else
                    moveOrder[index] = Global.values[cP] + 4;
				Captures[index++] = MoveFunctions.makeMove(to,from,11,cP,type,0);	
			}
			while(rAttack != 0) {
				toBit = rAttack & -rAttack;
				rAttack ^= toBit;
				to = chessBoard.getPos(toBit);
				from = to + 7;
                cP = chessBoard.piece_in_square[to];
                type = Global.ORDINARY_MOVE;
                if(to == passant) {
                    type = Global.EN_PASSANT_CAP;
                    moveOrder[index] = Global.values[5] + 4;   
                }else if(to/8 == 0)   {
                    moveOrder[index] = 0;  
                    Captures[index++] = MoveFunctions.makeMove(to,from,11,cP,Global.PROMO_B,1);
                    moveOrder[index] = 0;  
                    Captures[index++] = MoveFunctions.makeMove(to,from,11,cP,Global.PROMO_N,1);
                    moveOrder[index] = 0;  
                    Captures[index++] = MoveFunctions.makeMove(to,from,11,cP,Global.PROMO_R,1);
                    moveOrder[index] = Global.values[cP] + 700; 
                    type = Global.PROMO_Q;
                } else
                    moveOrder[index] = Global.values[cP] + 4;
                Captures[index++] = MoveFunctions.makeMove(to,from,11,cP,type,0);
			}
			while(promo != 0) {
				toBit = promo & -promo;
				promo ^= toBit;
				to = chessBoard.getPos(toBit);
				from = to + 8;
                moveOrder[index] = 0;
                Captures[index++] = MoveFunctions.makeMove(to,from,11,-1,Global.PROMO_B,1);
                moveOrder[index] = 0;
                Captures[index++] = MoveFunctions.makeMove(to,from,11,-1,Global.PROMO_N,1);
                moveOrder[index] = 0;
                Captures[index++] = MoveFunctions.makeMove(to,from,11,-1,Global.PROMO_R,1);
                moveOrder[index] = 700;
                Captures[index++] = MoveFunctions.makeMove(to,from,11,-1,Global.PROMO_Q,0);
			}
			pieces = chessBoard.blackknights;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = chessBoard.getPos(toBit);
                    cP = chessBoard.piece_in_square[to];
                    moveOrder[index] = Global.values[cP] + 3;
                    Captures[index++] = MoveFunctions.makeMove(to,from,7,cP,Global.ORDINARY_MOVE,0);
				}
			}
			pieces = chessBoard.blackbishops;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = chessBoard.getPos(toBit);
                    cP = chessBoard.piece_in_square[to];
                    moveOrder[index] = Global.values[cP] + 3;
                    Captures[index++] = MoveFunctions.makeMove(to,from,8,cP,Global.ORDINARY_MOVE,0);
				}
			}
			pieces = chessBoard.blackqueen;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = chessBoard.getPos(toBit);
                    cP = chessBoard.piece_in_square[to];
                    moveOrder[index] = Global.values[cP] + 1;
                    Captures[index++] = MoveFunctions.makeMove(to,from,9,cP,Global.ORDINARY_MOVE,0);
				}
			}
			pieces = chessBoard.blackrooks;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = chessBoard.getPos(toBit);
                    cP = chessBoard.piece_in_square[to];
                    moveOrder[index] = Global.values[cP] + 2;
                    Captures[index++] = MoveFunctions.makeMove(to,from,6,cP,Global.ORDINARY_MOVE,0);
				}
			}
			pieces = chessBoard.blackking;
			if (pieces != 0) {
				fromBit = pieces & -pieces;
				from = chessBoard.getPos(fromBit);		
				toSquares = chessBoard.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = chessBoard.getPos(toBit);
                    cP = chessBoard.piece_in_square[to];
                    moveOrder[index] = Global.values[cP];
                    Captures[index++] = MoveFunctions.makeMove(to,from,10,cP,Global.ORDINARY_MOVE,0);
				}
			}	
		}
		sortMoves(0,index,Captures);
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
		
		if(side == -1) {			//white moving
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
		kingPos = chessBoard.getPos(king);
		long toSquares = chessBoard.getAttackBoard(kingPos) & ~friends;
		long temp = chessBoard.getMagicBishopMoves(kingPos);
		temp &= (bishops | queen);
		while( temp != 0) {
			fromBit = temp & -temp;
			attackFrom = chessBoard.getPos(fromBit);
			if(Global.Diag1Groups[kingPos] == Global.Diag1Groups[attackFrom])
				toSquares &= ((~Global.diag1Masks[Global.Diag1Groups[kingPos]])^Global.set_Mask[attackFrom]);
			else 
				toSquares &= ((~Global.diag2Masks[Global.Diag2Groups[kingPos]])^Global.set_Mask[attackFrom]);	
			temp ^= fromBit;
			attacks++;
		}
		
		
		temp = chessBoard.getMagicRookMoves(kingPos);
		temp &= (rooks | queen);
		while( temp != 0) {
			fromBit = temp & -temp;
			attackFrom = chessBoard.getPos(fromBit);
			if((attackFrom/8) == (kingPos/8))
				toSquares &= ((~Global.rankMasks[kingPos/8])^Global.set_Mask[attackFrom]);
			else 
				toSquares &= ((~Global.fileMasks[kingPos%8])^Global.set_Mask[attackFrom]);	
			temp ^= fromBit;
			attacks++;
		}
		
		while( toSquares != 0) {
			long bit = toSquares & -toSquares;
			toSquares ^= bit;
			int to = chessBoard.getPos(bit);
			if(side == -1) {		//white moving'
				if(chessBoard.isWhiteAttacked(to))
					continue;
			} else {
				if(chessBoard.isBlackAttacked(to))
					continue;
			}
			int cP = chessBoard.piece_in_square[to];
			int	value = 10;
			if(cP != -1) {
				value = SEE.getSEE(side, to,kingPos, -1);
				if(value >= 0)
					value += 10000;
				else
					value = 0;	
			} else {
				if(side == -1)
					value = Hist2[kingPos][to];
				else
					value = Hist[kingPos][to];
			}			
			moveOrder[index] = value;
			escapes[index++] = MoveFunctions.makeMove(to,kingPos,chessBoard.piece_in_square[kingPos], cP, Global.ORDINARY_MOVE,0);
		}
		if(attacks == 2) {
			sortMoves(0,index,escapes);
			return index;
		}	
		temp = chessBoard.getKnightMoves(kingPos);
		temp &= knights;
		while( temp != 0) {
			fromBit = temp & -temp;
			attackFrom = chessBoard.getPos(fromBit);
			temp ^= fromBit;
			attacks++;
		}
		
		if(side == -1) {			//white moving
			temp = chessBoard.getWPawnAttack(kingPos);
			temp &= chessBoard.blackpawns;
            while(temp != 0) {
				fromBit = temp & -temp;
				attackFrom = chessBoard.getPos(fromBit);
				temp ^= fromBit;
				attacks++;				
			}
		} else {					//black moving
			temp = chessBoard.getBPawnAttack(kingPos);
			temp &= chessBoard.whitepawns;
            while(temp != 0) {
				fromBit = temp & -temp;
				attackFrom = chessBoard.getPos(fromBit);
				temp ^= fromBit;
				attacks++;				
			}
		}
		if(attacks == 2) {
			sortMoves(0,index,escapes);
			return index;
        }
		temp = chessBoard.getAttack2(attackFrom);
		temp &= (friends & ~king);
		int type;
		int value;
		int cP = chessBoard.piece_in_square[attackFrom];
		
        while( temp != 0) {
            type = Global.ORDINARY_MOVE;
            fromBit = temp & -temp;
            temp ^= fromBit;
            int from = chessBoard.getPos(fromBit);
            if(SEE.isPinned(side,attackFrom,from))
                continue;
            value = SEE.getSEE(side,attackFrom, from, -1);
            if(value >= 0)
                value += 10000;
            else
                value = 0;
            if(chessBoard.piece_in_square[from] % 6==5) {
                if(attackFrom/8 == 0 || attackFrom/8 == 7) {
                    moveOrder[index] = value;
                    escapes[index++] = MoveFunctions.makeMove(attackFrom,from,chessBoard.piece_in_square[from],cP,Global.PROMO_N,0);
                    moveOrder[index] = value;
                    escapes[index++] = MoveFunctions.makeMove(attackFrom,from,chessBoard.piece_in_square[from],cP,Global.PROMO_R,0);
                    moveOrder[index] = value;
                    escapes[index++] = MoveFunctions.makeMove(attackFrom,from,chessBoard.piece_in_square[from],cP,Global.PROMO_B,0);
                    type = Global.PROMO_Q;
                }
            }
            moveOrder[index] = value;
            escapes[index++] = MoveFunctions.makeMove(attackFrom,from,chessBoard.piece_in_square[from],cP,type,0);
		}
		
                
        if(chessBoard.piece_in_square[attackFrom] == 5) {
            if( (attackFrom-8) == chessBoard.getPassantW()) {
                temp = chessBoard.getWPawnAttack(attackFrom-8) & chessBoard.blackpawns;
                while(temp != 0) {
                    fromBit = temp & -temp;
                    temp ^= fromBit;
                    int from = chessBoard.getPos(fromBit);
                    if(SEE.isPinned(side,attackFrom-8,from))
                        continue;
                    value = SEE.getSEE(side,attackFrom-8, from, chessBoard.getPassantW());
                    if(value >= 0)
                        value += 10000;
                    else 
                        value = 0;
                    moveOrder[index] = value;
                    escapes[index++] = MoveFunctions.makeMove(attackFrom-8,from,chessBoard.piece_in_square[from],-1,Global.EN_PASSANT_CAP,0); 
                }
            }
        }
        //if attacking piece is a black pawn ( white moving )    
        if(chessBoard.piece_in_square[attackFrom] == 11) {
            if( (attackFrom+8) == chessBoard.getPassantB()) {
                temp = chessBoard.getBPawnAttack(attackFrom+8) & chessBoard.whitepawns;
                while(temp != 0) {
                    fromBit = temp & -temp;
                    temp ^= fromBit;
                    int from = chessBoard.getPos(fromBit);
                    if(SEE.isPinned(side,attackFrom+8,from))
                        continue;
                    value = SEE.getSEE(side,attackFrom+8, from, chessBoard.getPassantB());
                    if(value >= 0)
                        value += 10000;
                    else 
                        value = 0;
                    moveOrder[index] = value;
                    escapes[index++] = MoveFunctions.makeMove(attackFrom+8,from,chessBoard.piece_in_square[from],-1,Global.EN_PASSANT_CAP,0); 
                }
            }
        }
		//if one attacker is a slide piece, generate moves to block the sliding attack
		if(!Global.slides[chessBoard.piece_in_square[attackFrom]]) return index;
	
		long squares = 0;                                                           //intermediate squares between attacker and king
		//need to get bitset of squares between attacker and king
		int difference = kingPos - attackFrom;
		int rankDiff = kingPos/8 - attackFrom/8;
		if(difference < 0)
			rankDiff *= -1;
		int relation = -1;
		if(rankDiff != 0) {
			relation = difference / rankDiff;
		} else {
			if(kingPos < attackFrom)
				relation = -99;
			else
				relation = 99;
		}
		switch(relation) {
			case(-9):
				squares = Global.plus9[kingPos] ^ Global.plus9[attackFrom-9];// ^ Global.set_Mask[attackFrom];
				break;	
			case(9):
				squares = Global.plus9[attackFrom] ^ Global.plus9[kingPos-9];// ^ Global.set_Mask[kingPos];
				break;
			case(-7):
				squares = Global.plus7[kingPos] ^ Global.plus7[attackFrom-7];// ^ Global.set_Mask[attackFrom];
				break;
			case(7):
				squares = Global.plus7[attackFrom] ^ Global.plus7[kingPos-7];// ^ Global.set_Mask[kingPos];
				break;
			case(-8):
				squares = Global.plus8[kingPos] ^ Global.plus8[attackFrom-8];// ^ Global.set_Mask[attackFrom];
				break;
			case(8):
				squares = Global.plus8[attackFrom] ^ Global.plus8[kingPos-8];//] ^ Global.set_Mask[kingPos];
				break;
			case(-99):
				squares = Global.plus1[kingPos] ^ Global.plus1[attackFrom-1];// ^ Global.set_Mask[attackFrom];
				break;
			case(99):
				squares = Global.plus1[attackFrom] ^ Global.plus1[kingPos-1];// ^ Global.set_Mask[kingPos];
				break;
		}
		
        //now generate all moves to the squares bit set to block the sliding check
		while(squares != 0) {
			long toBit = squares & -squares;
			squares ^= toBit;
			int to = chessBoard.getPos(toBit);
			long attackers = chessBoard.getMovesTo(to);
			attackers &= (friends & ~king);
			while(attackers != 0) {
				type = Global.ORDINARY_MOVE;
				fromBit = attackers & -attackers;
				attackers ^= fromBit;
				int from = chessBoard.getPos(fromBit);
				if(SEE.isPinned(side,to,from))
					continue;
				if(chessBoard.piece_in_square[from] %6==5) {
					if(to/8 == 0 || to/8 == 7)
						type = Global.PROMO_Q;
				}
				if(side == -1)
					value = Hist2[from][to];
				else
					value = Hist[from][to];
				moveOrder[index] = value;	
				escapes[index++] = MoveFunctions.makeMove(to,from,chessBoard.piece_in_square[from],-1,type,0);
			}	
		}
		sortMoves(0,index,escapes);
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
    private boolean verifyHash(int side,int move) {
		
		if(move == 0)
			return false;
		int div;
		int to = MoveFunctions.getTo(move);
		int from = MoveFunctions.getFrom(move);
		int piece = MoveFunctions.getPiece(move);
		
		if(side==1)
			div =1;
		else
			div = 0;
		
		if(chessBoard.piece_in_square[from]!=piece)
			return false;
		if(chessBoard.piece_in_square[from]==-1)
			return false;
		if(chessBoard.piece_in_square[from]/6==div) {		
			long temp;
			if(piece==5)	//wPawn
				temp = chessBoard.getWPawnMoves(from,chessBoard.getPassantB());
			else if(piece==11)
				temp = chessBoard.getBPawnMoves(from,chessBoard.getPassantW());
			else
				temp = chessBoard.getAttackBoard(from);
			if((temp&Global.set_Mask[to])!=0)
				return true;
		}		
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
    private boolean verifyKiller(int side,int move) {
		
		if(move == 0)
			return false;
		int div;
		int to = MoveFunctions.getTo(move);
		int from = MoveFunctions.getFrom(move);
		int piece = MoveFunctions.getPiece(move);
		if(to==from)
			return false;
		
		if(side==1)
			div =1;
		else
			div = 0;
		if(piece%6==4) {
			if(Math.abs(to-from)==2)
				return false;
		}
		if(chessBoard.piece_in_square[from]!=piece)
			return false;
		if(chessBoard.piece_in_square[to]!=-1||chessBoard.piece_in_square[from]==-1)
			return false;
		if(chessBoard.piece_in_square[from]/6==div) {		
			long temp;
			if(piece==5)	//wPawn
				temp = chessBoard.getWPawnMoves(from,chessBoard.getPassantB());
			else if(piece==11)
				temp = chessBoard.getBPawnMoves(from,chessBoard.getPassantW());
			else
				temp = chessBoard.getAttackBoard(from);
			if((temp&Global.set_Mask[to])!=0)
				return true;
		}		
		return false;
	}

    /** 
     * Method markPreviousMoves
     *  
     * To-do - This looks pretty inneficient
     * There must be a faster way
     * 
     * marks the hash and killer moves in the regular move lists
     * this ensures the hash and killer moves are not searched twice
     *
     * @param int[] moves - the regular moves array
     * @param int noMoves - number of regular moves
     * @param int start - index of regular moves to start at
     * @param int[] prevMoves - killer and hash moves
     * @param int noPrevious - number of previous moves
     * 
     * @return int - the number of hash and killer moves which still need to be removed
     * 
     */
	private int markPreviousMoves(int[] moves, int noMoves, int start, int[] prevMoves, int noPrevious) {
		for(int i = start; i < noMoves+start; i++) {		//look through all moves
			for(int j = 0; j < noPrevious; j++) {				//loop through moves to remove
				if((moves[i]& 8388607) == (prevMoves[j]& 8388607))   {
					moves[i] = -200000;
					for(int k=j ; k<7; k++) {
						prevMoves[k] = prevMoves[k+1];
					}
					noPrevious--;
				}	
			}
		}	
		return noPrevious;
	}
	
    /** 
     * Method getHashMoves
     * 
     * probes the has table and collects any stored moves
     *
     * @param int key - computed hash key to lookup has entries
     * @param int hashArr[] - array to store hash moves in
     * 
     * @return int - number of hash moves retrieved
     * 
     */
    private int getHashMoves( int key, int[] hashArr) {
		int hashCount = 0;
		int hashIndex = HashTable.hasHash(key, chessBoard.hashValue, chessBoard.hashValue2);
		if(hashIndex != 1) {
			int hashMove = HashTable.getMove(key, hashIndex);
			if(hashMove != 0)
				hashArr[hashCount++] = hashMove;
			if(hashIndex == 0) {
				hashIndex = HashTable.hasSecondHash(key, chessBoard.hashValue, chessBoard.hashValue2);
				if(hashIndex != 1) {
					hashMove = HashTable.getMove(key, hashIndex);
					if(hashMove != 0) {
						hashArr[hashCount++] = hashMove;
						if(hashCount == 2) {
							if(hashArr[0] == hashArr[1])
								hashCount--;
						}
					}
				}
			}	
		}
		return hashCount;
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
    private int Max(int side,int depth,int alpha,int beta,boolean nMove,boolean isInCheck, boolean wasExtended,boolean iid) {
        
        if(!iid)
            thisDepth++;
        /** time management code */
        if(++hashnodes >= nextTimeCheck) {	
            try {
                if (!timeLeft()) {
                    stop = true;		
                }
            }
            catch(IOException e) {e.printStackTrace();};
        }

        if (stop) 
            return 0;	
        
        if(depth < 0)
            depth = 0;

        int key = (chessBoard.hashValue % Global.HASHSIZE);

        int fscore = chessBoard.value*side;

        boolean fnodes=false;
        boolean razored = false;
        int fMargin = 0;
        int nonodes = 0;

        /** razoring code */
        if(!wasExtended && !isInCheck && depth==3 ) {
            if((fscore+900)<=alpha && chessBoard.getNumberOfPieces(-side) > 3) {
                razored = true;
                depth--;
            }
        }

        /** flag indicates it is worthless to try a null move search here */
        int nullFail = 0;

        /** hash table code - we are trying to cut the search based on what is stored */
        int hashIndex = HashTable.hasHash(key,chessBoard.hashValue,chessBoard.hashValue2);

        if (hashIndex != 1 ) {
            HashTable.setNew(key,hashIndex,ancient);
            int type = HashTable.getType(key,hashIndex);

            switch(type) {
                case(0):
                    if(!iid && HashTable.getDepth(key,hashIndex)>=depth) {
                        int hVal = HashTable.getValue(key,hashIndex);
                        if(hVal <= alpha) {
                            return hVal;
                        }    
                    }
                    break;
                case(1):
                    if(!iid && HashTable.getDepth(key,hashIndex)>=depth) {
                        hashnodes++;
                        return HashTable.getValue(key,hashIndex);
                    }
                    break;
                case(2):
                    if(!iid && HashTable.getDepth(key,hashIndex)>=depth) {
                        int hVal = HashTable.getValue(key,hashIndex);
                        if(hVal >= beta) {                   
                            return hVal;
                        }
                    }
                    break;
                case(4):
                    int hVal = HashTable.getValue(key,hashIndex);
                    if(hVal == -20000);
                        hVal += thisDepth;
                    return hVal;
            }			
            nullFail = HashTable.getNullFail(key,hashIndex);	
        }	
        
        /** futility and extended futility pruning condition testing */
        fscore +=500;
        if(!wasExtended && !isInCheck && depth ==2 && fscore<=alpha) {	
            fnodes = true;
            fMargin = 500;
        }
        fscore -= 300;
        if(!wasExtended && !isInCheck && depth ==1 && fscore<=alpha) {
            fnodes = true;
            fMargin = 200;
        }

        int hashMove = 0;


        int value;
        
        /** if at a leaf node, call the quiescence search and store the value in the trans table */
        if (depth == 0) {			//if at leaf node return value
            nodes++;
            value = Quies(side,0,alpha,beta);

            if(value>=beta) 
                HashTable.addHash(key,chessBoard.hashValue,chessBoard.hashValue2,hashMove,value,depth,HASH_BETA,0,ancient);
            else if(value<=alpha)
                HashTable.addHash(key,chessBoard.hashValue,chessBoard.hashValue2,hashMove,value,depth,HASH_ALPHA,0,ancient);
            else 
                HashTable.addHash(key,chessBoard.hashValue,chessBoard.hashValue2,hashMove,value,depth,HASH_EXACT,0,ancient);
            return value;

        }

        /** null move pruning code
         * we don't want to null move in a few situations
         * -when in check
         * -when performed a null move last ply
         * -when the hash table tells us not to (nullFail)
         * -when we are in the endgame as zugzwangs are more likely
         */
        if(!razored && !fnodes && !isInCheck && !nMove && nullFail != 1 && (chessBoard.pawnsKings != chessBoard.bitboard) && chessBoard.totalValue > 1200 && chessBoard.getMinNumberOfPieces() >1  ) {
            chessBoard.switchTurn();
            int reduce = 2;
            if(depth>6 && chessBoard.getMaxNumberOfPieces() >3)
                reduce = 3;
            value = -Max(-side,depth-reduce-1,-beta,-beta+1,true,false,false,false);
            thisDepth--;
            chessBoard.switchTurn();
            if(value>=beta) {		
                if(!stop) 	
                    HashTable.addHash(key,chessBoard.hashValue,chessBoard.hashValue2,hashMove,value,depth,HASH_BETA,0,ancient);
                return value;
            }
            if(value <= -19000) {
                nullFail = 1;
            }           
        }
        
        int killerDepth = thisDepth;
        boolean oneLegal = false;
        int piece;
        int to, from;
        int bestFrom = 0;
        int bestTo = 0;
        int bMove =  VALUE_START;
        int hType = HASH_ALPHA;
        int index=0;
        int theMove;
        int endIndex = 0;
        int capIndex = 0;
        
        /** state of search we are in..etc Hash move, killers, or regular */
        int state= SEARCH_HASH;			
        
        /** pExtend stores extensions/reduction amount for each move */
        int pExtend = 0;
        
        int reps;
        int noKillers = 0;
        int totalHash = 0;
        int hashCount = 0;
        int bestFullMove = 0;
        int[] moveArr = new int[128];
        int[] hashArr = new int[8];
        int[] badCapArr = new int[50];
        int noBadCap = 0;
        int moveCount = 0;
        int passant;
        
        if(side == -1)
            passant = chessBoard.getPassantB();
        else
            passant = chessBoard.getPassantW();

        while(state != SEARCH_END) {

            switch(state) {
                case(SEARCH_HASH):
                    hashCount = getHashMoves(key, hashArr);

                    if(hashCount == 0 && depth > 3  && !isInCheck ) {
                        Max(side,depth-2,alpha,beta,true,false,false,true);
                        hashIndex = HashTable.hasHash(key,chessBoard.hashValue,chessBoard.hashValue2);
                        if(hashIndex != 1) {
                            hashArr[0] = HashTable.getMove(key,hashIndex);
                            if(hashArr[0] != 0) 
                                hashCount++;
                        }
                        if(hashCount == 0 && hashIndex == 0) {
                            hashIndex = HashTable.hasSecondHash(key,chessBoard.hashValue,chessBoard.hashValue2);
                            if(hashIndex != 1) {
                                hashArr[0] = HashTable.getMove(key,hashIndex);
                                if(hashArr[0] != 0) 
                                    hashCount++;
                            }
                        }
                    }

                    nMove = false;
                    for(int i=0;i<hashCount;i++) {
                        moveArr[i] = hashArr[i];
                    }
                    index = hashCount;
                    totalHash = hashCount;
                    endIndex = 0;
                    break;
                case(MATE_KILLER):
                    if(isInCheck) {
                        state = SEARCH_END-1;			
                        index = getCheckEscapes(side,moveArr);
                        if(hashCount > 0)
                            hashCount = markPreviousMoves(moveArr,index,0,hashArr,hashCount);
                        endIndex = 0;
                    }else {
                        index = 0;
                        if(MoveFunctions.getValue(killerMoves[killerDepth][1]) >= 200) {

                            if(verifyKiller(side,killerMoves[killerDepth][1]))	{

                                moveArr[index] = killerMoves[killerDepth][1] & 8388607;
                                hashArr[totalHash] = moveArr[index];
                                for(int i=0;i<totalHash;i++) {
                                    if(moveArr[index] == hashArr[i]) {
                                        index--;
                                        totalHash--;
                                    }
                                }		
                                index++;
                                totalHash++;
                            }

                        }

                        if(MoveFunctions.getValue(killerMoves[killerDepth][0]) >= 200) {

                            if(verifyKiller(side,killerMoves[killerDepth][0]))	{

                                moveArr[index] = killerMoves[killerDepth][0] & 8388607;
                                hashArr[totalHash] = moveArr[index];
                                for(int i=0;i<totalHash;i++) {
                                   if(moveArr[index] == hashArr[i]) {
                                        index--;
                                        totalHash--;
                                    }
                                }		
                                index++;
                                totalHash++;
                            }
                        }

                        noKillers += index;
                        endIndex = 0;	
                    }
                    break;
                case(GOOD_CAPTURES):
                    index = getCaptures(side,moveArr);
                    if(totalHash > 0)
                        hashCount = markPreviousMoves(moveArr,index,0,hashArr,hashCount);
                    capIndex = index;
                    endIndex = 0;

                    break;
                case(KILLER_MOVES):
                    index = 0;
                    totalHash = hashCount+noKillers;
                    totalHash = hashCount;

                    if(verifyKiller(side,killerMoves[killerDepth][1]))	{			
                        moveArr[127] = killerMoves[killerDepth][1]& 8388607;
                        hashArr[totalHash++] = moveArr[127];
                        index++;
                        for(int i=0;i<totalHash-1;i++) {
                            if(moveArr[127] == hashArr[i]) {
                                index--;
                                totalHash--;
                            }
                        }		
                    }
                    if(verifyKiller(side,killerMoves[killerDepth][0]))	{		
                        int position = 127 - index;
                        moveArr[position] = killerMoves[killerDepth][0] & 8388607;
                        hashArr[totalHash++] = moveArr[position];
                        index++;
                        for(int i=0;i<totalHash-1;i++) {
                            if(moveArr[position] == hashArr[i]) {
                                index--;
                                totalHash--;
                            }
                        }		
                    }
                    endIndex = 128-index;
                    index = 128;
                    break;
                case(NON_CAPS):
                    index = getMoves(side,moveArr,capIndex);
                    endIndex = capIndex;
                    if(totalHash > 0)
                        totalHash = markPreviousMoves(moveArr,index,capIndex,hashArr,totalHash);
                    break;
                case(BAD_CAPS):	
                    index = noBadCap;
                    moveArr = badCapArr;
                    endIndex = 0;
                    break;
            }	
            for (int i = index - 1; i >= endIndex; i--) {

                if(state == SEARCH_HASH) {
                    if(!verifyHash(side,moveArr[i])) continue;
                }

                theMove = moveArr[i];
                if (moveArr[i] == -200000)  continue;
                to = MoveFunctions.getTo(theMove);
                from = MoveFunctions.getFrom(theMove);
                int type = MoveFunctions.moveType(theMove);
                piece = MoveFunctions.getPiece(theMove);
                
                /** any captures which the static exchnage evaluator finds lose material
                 * get placed in the bad capture array and are tried later
                 */
                if(state == GOOD_CAPTURES) {            
                    if(MoveFunctions.getValue(theMove) == 1) continue;	
                    if(chessBoard.piece_in_square[to] != -1) {
                        if(Global.values[chessBoard.piece_in_square[from]] >= Global.values[chessBoard.piece_in_square[to]] 
                        && SEE.getSEE(side,to,from,passant) < 0)  { 
                           badCapArr[noBadCap++] = theMove;
                           continue;    
                        } 
                    } else {
                        if(SEE.getSEE(side,to,from,passant) < 0) {
                           badCapArr[noBadCap++] = theMove;
                           continue;    
                        }
                    }   
                }
                pExtend = 0;
                
                /** make the move */
                reps = chessBoard.makeMove(theMove,false,true);		

                if (!isInCheck && inCheck(side)) {
                    chessBoard.unMake(theMove,false,true);			//avoid leaving king in check
                    continue;
                }
                /** repetition detection */  
                if (reps == 2) {
                    oneLegal = true;
                    value = 0;
                }else if (chessBoard.getDraw() >= 100) {    /** 50 move draw detection */
                    oneLegal = true;
                    value = 0;
                }else {
                    /** we have a legal move */
                    oneLegal = true;

                    //extend if this is a checking move
                    boolean checkingMove = false;
                    if(inCheck(-side)) {                           
                        checkingMove = true;
                        pExtend++;   
                    }    

                    //passed pawn push extension
                    boolean pawnExtension = false;
                    if((chessBoard.getTotalValue() < 5000) && !checkingMove && !isInCheck && (piece%6==5 && (to/8==6 || to/8==1)) )		{	//extention for close to promotion
                        pawnExtension = true;
                        pExtend++;
                    }		

                    //recognize moves involving passed pawns
                    //do not want to forward nodes these moves
                    boolean passedPawnMove = false; 
                    if(piece%6 == 5) {
                        passedPawnMove = Evaluation2.isPassedPawn(side, to);    
                    }

                    //late move reduction code
                    boolean lmr = false;
                    if(state == NON_CAPS && ((alpha == beta - 1 && moveCount >= 15) || moveCount >= 4) && depth >= 2 && !passedPawnMove
                       && !isInCheck && !checkingMove && type != Global.SHORT_CASTLE 
                       && type != Global.LONG_CASTLE) {
                            pExtend = -1;    
                            lmr = true;
                    }

                    // futility pruning code 
                    if(fnodes && pExtend == 0 && !checkingMove && !passedPawnMove) {
                        fscore = chessBoard.getEval2(side);
                        if(fscore + fMargin <= alpha) {	
                            nonodes++;
                            chessBoard.unMake(theMove,false,true);
                            continue;
                        }
                    }		

                    if(moveCount == 0) {
                        value = -Max(-side, depth-1+pExtend, -beta, -alpha, nMove,checkingMove, isInCheck | pawnExtension, false);
                        thisDepth--;
                    }else {
                        value = -Max(-side,depth-1+pExtend,-alpha-1,-alpha,nMove,checkingMove,isInCheck | pawnExtension,false);
                        thisDepth--; 
                        if(value>alpha && value<beta) {
                            if(lmr == true)
                                pExtend = 0;
                            value = -Max(-side,depth-1+pExtend,-beta,-alpha,nMove,checkingMove,isInCheck | pawnExtension,false);
                            thisDepth--;
                       }
                       else if(value>alpha && lmr == true) {       //use normal depth if lmr move looks interesting
                            pExtend = 0;
                            value = -Max(-side,depth-1+pExtend,-beta,-alpha,nMove,checkingMove,isInCheck | pawnExtension,false);
                            thisDepth--;
                       } 
                    }
                }	

                chessBoard.unMake(theMove,false,true);

                if(value > bMove) {	
                    if(value > alpha) {
                        hType = HASH_EXACT;
                        alpha = value;
                    }
                    if (value >= beta) {
                        if (!stop) {
                            HashTable.addHash(key,chessBoard.hashValue,chessBoard.hashValue2,theMove,value,depth,HASH_BETA,nullFail,ancient); 	
                            if (side == 1)
                                Hist[from][to]++;
                            else
                                Hist2[from][to]++;
                        }
                        if (state == NON_CAPS) {									//if state is a non capture or a bad capture
                            if(value >= 19000) 
                                theMove = MoveFunctions.setValue(theMove,200);
                            if(theMove != killerMoves[killerDepth][0]) {
                                int temp1 = killerMoves[killerDepth][0];
                                killerMoves[killerDepth][0] = theMove;	
                                killerMoves[killerDepth][1] = temp1;	
                            }
                        }
                        return value;
                    }
                    bestFullMove = theMove;
                    bMove = value;
                    bestTo = to;
                    bestFrom = from;
                }
                moveCount++;
            }                                                       //end for
            state++;
        }                                                           // end while	
        if(!isInCheck&& !oneLegal && nonodes==0)	{				//stalemate detection
            bMove = 0;
            hType = 4;	
        }else if(isInCheck && !oneLegal)	{		
            bMove = -20000+thisDepth;
            hType = 4;
            if(!stop)
                HashTable.addHash(key,chessBoard.hashValue,chessBoard.hashValue2,bestFullMove,-20000,depth,hType,nullFail,ancient);   		
            return bMove;

        }
        if(hType == HASH_ALPHA)                         //if all moves are poor, then don't want a move stored in TT
            bestFullMove = 0;

        if(fnodes && nonodes>0) {
            if(hType == HASH_EXACT)
                hType = HASH_BETA;
            if(hType == HASH_ALPHA)	
                bMove = alpha;
        }

        if(!stop) 
            HashTable.addHash(key,chessBoard.hashValue,chessBoard.hashValue2,bestFullMove,bMove,depth,hType,nullFail,ancient);   		

        if(oneLegal && hType == HASH_EXACT) {
            if(side==1)                         //black moving
                Hist[bestFrom][bestTo]++;
            else
                Hist2[bestFrom][bestTo]++;
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
	private int Quies(int side,int depth,int alpha,int beta) {
		
        int[] capArr = new int[60];
		int value;
		int index=0;
		int bValue;
        boolean isInCheck = false;
		int testValue = 0;
        if(depth > 0 && inCheck(side)) {
			isInCheck = true;
			index = getCheckEscapes(side,capArr);
			if(index == 0)
				return -20000+thisDepth;
			bValue = -20000+thisDepth;
		}else {
            value = Evaluation2.getEval(side);
            if (value > alpha) {
				alpha = value;
			}	
			if (alpha >= beta)
				return alpha;
			index = getCaptures(side, capArr);

			if (index == 0 ) 
				return value;
			bValue = value;
          testValue = value;
		}
		
		for(int i = index-1; i >=0; i--) {
			 
            if(MoveFunctions.getValue(capArr[i]) == 1) continue;	
            
            int to = MoveFunctions.getTo(capArr[i]);
            int from = MoveFunctions.getFrom(capArr[i]);
            
            if(!isInCheck && chessBoard.piece_in_square[to] != -1) {
                if((Global.values[chessBoard.piece_in_square[to]] - Global.values[chessBoard.piece_in_square[from]]) < Math.max(0,alpha - testValue)
               && (SEE.getSEE(side,to,from,-1) < Math.max(0,(alpha - testValue))))  { 
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
            
            chessBoard.makeMove(capArr[i],false,false);		//make the move;
          
			if(!isInCheck && inCheck(side)) {
				chessBoard.unMake(capArr[i],false,false);
				continue;
			}

			value = -Quies(-side,depth+1,-beta,-alpha);
			chessBoard.unMake(capArr[i],false,false);
			if(value>bValue) {
				if(value>=beta) 
					return value;
				bValue = value;
				if(value>alpha) {
					alpha = value;
				}
			}	
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
        Perft(chessBoard.getTurn(),depth);
        System.out.println("Perft value is "+perft);

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
        if(inCheck(side))
			inCheck = true;

        int[] moveArr = new int[128];
		if(!inCheck) {
			int index2 = getCaptures(side, moveArr);
			index = getMoves(side, moveArr,index2);
		} else {
			index = getCheckEscapes(side,moveArr);
		}
	    for(int i = index-1; i >=0; i--) {
			perft = 0;
            int reps = chessBoard.makeMove(moveArr[i],false,false);		//make the move;
			//print out the to and from algebraic positions
			int to = MoveFunctions.getTo(moveArr[i]);
            int from = MoveFunctions.getFrom(moveArr[i]);
            int piece = MoveFunctions.getPiece(moveArr[i]);

            String output = HistoryWriter.getUCIMove(to, from, piece);
            System.out.print(output);
            if(!inCheck && inCheck(side)) {
				chessBoard.unMake(moveArr[i],false,false);
				continue;
			}
			if(reps==3)
				return;
			else
				Perft(-side,depth-1);
			chessBoard.unMake(moveArr[i],false,false);
			System.out.println(" "+perft);
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
	private void Perft(int side,int depth) {
		
        int reps;
		boolean inCheck = false;
		if(inCheck(side))
			inCheck = true;
		if(depth<=0) {
			perft++;
			return;
		}
		int index;
		int[] moveArr = new int[128];
		if(!inCheck) {
			int index2 = getCaptures(side, moveArr);
			index = getMoves(side, moveArr,index2);	
		} else {
			index = getCheckEscapes(side,moveArr);
		}
		for(int i = index-1; i >=0; i--) {
            reps = chessBoard.makeMove(moveArr[i],false,false);		//make the move
			if(!inCheck && inCheck(side)) {
				chessBoard.unMake(moveArr[i],false,false);
				continue;
			}
			if(reps==3)
				return;
			else 	
				Perft(-side,depth-1);
			chessBoard.unMake(moveArr[i],false,false);
		}	
	}		


}		
	
	