
import java.util.Arrays;
import java.io.*;

public final class Engine {
	
	
	private static Board Magnum;

	private static long prune;			//counter for evaluated positions
	private static long hashPrune;		//counter for evaluated positions plus hash table exact hits
	
	private static final int[][] Hist = new int[64][64];
	private static final int[][] Hist2 = new int[64][64];
	private static TransTable HashTable = new TransTable(Global.HASHSIZE,0);
	private static int[] moveOrder = new int[128];
	private static int theSide;
	private static final int[][] killerMoves = new int[100][2];
	
	long mills1, mills2;					//time in milleseconds
	private static boolean stop;
	private static int nextTimeCheck;
	private static final int HASH_EXACT = 1;
	private static final int HASH_ALPHA = 0;
	private static final int HASH_BETA  = 2;
	private static final int ALPHA_START = Integer.MIN_VALUE+100;
	private static final int BETA_START = Integer.MAX_VALUE-100;
	private static final int VALUE_START = Integer.MIN_VALUE+100;
	private static final int SEARCH_HASH = 1;
	private static final int KILLER_MOVES =4;
	private static final int MATE_KILLER =2;
	private static final int GOOD_CAPTURES = 3;
	private static final int NON_CAPS = 5;
	private static final int BAD_CAPS = 6;
	
	private static final int SEARCH_END = 7;	
	
        private static final int NO_ATTACK_BONUS = 10;
        private static final int DEFENCE_BONUS = 10;
        private static int fCount;
	private static int thisDepth;
	private static int thisDepth2;
	private static int[] index32 = new int[32];
	private static final int debruijn = 0x077CB531;
	private static boolean infinite = false;			//flag for infinite time controls
	private int histDiv = 50;
	private int ancient;
	private int perfit = 0;
	//private Evaluation eval;
	private int lastVal = 0;	
		
	public Engine(Board mag) {
		Magnum = mag;	
		for(int i=0;i<32;i++) {
			index32[(debruijn<<i)>>>27] = i;
		}	
		//eval = new Evaluation(Magnum);
	}
	
	
	public boolean timeLeft() throws IOException {
		if(Main.reader.ready()) {	
			if("stop".equals(Main.reader.readLine())) 
				return false;
		}
		
		long temp = System.currentTimeMillis();
		
		
		if(!infinite) {
			if(mills2>temp) {
				nextTimeCheck += Math.min(25000,((mills2-temp)*50));
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
	public static void resetHash() {
		HashTable = new TransTable(Global.HASHSIZE,0);
	}
	
	
	public String search(int time,int searchDepth,boolean inf) {

		int[] moveArr = new int[128];
		double secs;
		double rate;	
		double branch;
		double exp;
		int thePiece;
		int alpha;							
		int beta;						
		int value;								
		int to;									
		boolean ok = true;								//flag to see if legal move can be made
		boolean isExact;
		boolean hasValue;
		int noLegal;
		int best = 0;							
		int temp;								
		int index;
		int bMove;								
		int bestValue = 0;
		int from;
		int reps;
		ancient = (Magnum.getCount()/2) % 8;			//ancient node value betweeen 0 and 7;
		infinite = inf;
		//Evaluation2.getEval(t);
                if((Board.getCount()/2 % 8) == 7) {
			HashTable.clearHash();
			Evaluation2.clearPawnHash();
			Evaluation2.clearEvalHash();
			for(int i = 0; i<64; i++) {
				for(int j = 0; j < 64; j++) {
					Hist[i][j] = 0;
					Hist2[i][j] = 0;
				}
			}
		}
		theSide = Board.getTurn();
		mills1 = System.currentTimeMillis();
		mills2 = mills1 + time;
		stop = false;
		
		nextTimeCheck = Math.min(1000, time * 15);				//assumes program is searching 15000 moves per second.   if not will take too long
		//resetHash();
		//HashTable.clearHash();
		prune = 0;
		hashPrune = 0;
		
		fCount = 0;
		
		
                for(int i = 0; i<64; i++) {
			for(int j = 0; j < 64; j++) {
				Hist[i][j] /= 10;
				Hist2[i][j] /= 10;
				//if(Hist[i][j] > 50)
				//	System.out.print("hist[i][j] is "+Hist[i][j]);
				//Hist[i][j] = Math.min(15,Hist[i][j]/2);
				//Hist2[i][j] = Math.min(15,Hist2[i][j]/2);
				//System.out.print("hist[i][j] is "+Hist[i][j]);
			}
		}
		if(!inCheck(theSide)) {
			int[] capArr = new int[60];
			index = getMoves(theSide, moveArr,0);	
			int index2 = getCaptures(theSide, capArr, Integer.MIN_VALUE);
		
			for(int i = index; i<(index+index2);i++) {
				moveArr[i] = capArr[i-index];
			}
			index += index2;
			Arrays.sort(moveArr,0,index);
		} else {
			index = getCheckEscapes(theSide, moveArr);
		}
		
		//index = getMoves(theSide, moveArr);
		//System.out.println("index is "+index);
		
		
		
		
		
		
		for (int l=0; l<15; l++) {
			killerMoves[l][0] = 0;
			killerMoves[l][1] = 0;
			
		}
			
		/*
		if(inCheck (-theSide)) {
			System.out.println("info string In Check");
			ok = false;
		}
		
		else if (isCheckMate(theSide)) {
			System.out.println("info string Checkmate");
			ok = false;
		}
		else if (isStaleMate(theSide))  {
			System.out.println("info string Stalemate");
			ok = false;
		}	
		*/
		//System.out.println("no pos is "+getPos(0));
		//Perfit(theSide,6,Integer.MIN_VALUE+50,Integer.MAX_VALUE-50);
		//System.out.println("perfit is "+perfit );
		//System.out.println("mask is "+Global.diag2Masks[Global.Diag2Groups[13]]);
		if (ok) {
			hasValue = false;
			for (int depth = 2; depth <= searchDepth; depth++) {		
				if (stop && hasValue) break;
				value = VALUE_START;
				alpha = ALPHA_START;
				beta = BETA_START;
				bMove = Integer.MIN_VALUE;
				theSide = Board.getTurn();
				isExact = false;
				
				noLegal = 0;
				thisDepth = 0;

				for (int i = index - 1; i >= 0; i--) {
					
					temp = moveArr[i];
					reps = Magnum.makeMove(temp,false,true);
					if(inCheck(theSide)) {
						moveArr[i] &= 8388607;
						Magnum.unMake(temp,false,true);
						continue;
					}
					noLegal++;
					
					if (reps >= 2)
						value = -25;
					else if (Board.getDraw() == 100)
						value = -25;
					else if (isStaleMate(-theSide)) 
						value = -25;
					else if (!isExact || depth < 3) {				
								
						value = -Max(-theSide, depth-1, -beta, -alpha, false, 0, false);
						thisDepth--;
					} else {
						value = -Max(-theSide, depth-1, -alpha-1, -alpha, false, 0, false);
						thisDepth--;
						if (value > alpha) {
							value = -Max(-theSide, depth-1, -beta, -alpha, false, 0, false);		
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
						
					Magnum.unMake(temp,false,true);
				}
		
				long mills3 = System.currentTimeMillis();
				mills3 = mills3 - mills1;
				secs = (double)mills3 / 1000.0;
				rate = (double)prune / secs;
				String pv = HistoryWriter.getUCIMove((best>>6) & 63, best & 63, (best >> 12) & 15);
				if (stop)
					depth--;
				//System.out.println("info depth "+depth+" score cp "+bestValue+" nodes "+prune+" nps "+(int)rate+" pv "+pv);
				if(bestValue > 10000) {			//this is a mate score
					int mate = (20000-bestValue)/2;
					System.out.println("info depth "+depth+" score mate "+mate+" nodes "+prune+" pv "+pv);
				} else if(bestValue < -10000)	{
					int mate = (-20000-bestValue)/2;
					System.out.println("info depth "+depth+" score mate "+mate+" nodes "+prune+" pv "+pv);
				
				} else {
					System.out.println("info depth "+depth+" score cp "+bestValue+" nodes "+prune+" pv "+pv);
				}
				Arrays.sort(moveArr, 0, index);
				
				//if(noLegal == 0 )
				//	break;
		
				//if(bMove >= 20000 ||bMove <= -20000)
				//	break; 
			}
			
			reps = Magnum.makeMove(best, true, true);
		
			//System.out.println("info string hash hits is "+hashPrune);
			//System.out.println("info string hashCount is "+HashTable.getCount());
			//System.out.println("info string total value is "+Board.getTotalValue());
			//System.out.println("pawn hash hits is "+Global.pawnHashHits);
			//System.out.println("pawn hash miss is "+Global.pawnHashMiss);
			//Global.pawnHashMiss = 0;
			//Global.pawnHashHits = 0;
			return HistoryWriter.getUCIMove((best >> 6) & 63, best & 63, (best >> 12) & 15);
		}
		return "";
	}

	
	/**********************************************************************
	This method tests whether the game is a draw due to 50 move rule
	
	**********************************************************************/
	private boolean is48Draw(int to,int piece,int tempPiece) {
		//int[] thePieces;
		
		if(tempPiece!=-1)
			return false;
		else if(piece==11 || piece==5)
			return false;
		else if(Board.getDraw()>=96)
			return true;
		else 
			return false;
		
		
	}		
	
	
	/**********************************************************************
	This method tests whether the game is a draw due to 50 move rule
	
	**********************************************************************/
	private boolean isFiftyDraw(int to,int piece,int tempPiece) {
		//int[] thePieces;
		
		//thePieces = Magnum.getPiecesInSquare();
		if(tempPiece!=-1)
			return false;
		else if(piece==11 || piece==5)
			return false;
		else if(Magnum.getDraw()>=99)
			return true;
		else 
			return false;
		
		
	}		
	
	/**********************************************************************
	This method tests whether a side is in check
	parameters - int side ....the side possibly in check
	**********************************************************************/
		
	private boolean inCheck(int side) {  
		
		if(side==1) 					//black
			return Board.isBlackAttacked(getPos(Board.blackking));
		else 							//white
			return Board.isWhiteAttacked(getPos(Board.whiteking));
		
	
	}
	
	
	
	/**********************************************************************
	This method will determine if the game is over due to a checkmate
	parameters int side - the side to move next
	**********************************************************************/
	private boolean isCheckMate(int side) {

		int[] Moves;
		int temp;
		int index;
		int to;
		int from;

		if (inCheck(side)) {				//side to move must be in check...otherwise could be stalemate

			Moves = new int[128];
			index = getMoves(side,Moves,0);
			for (int i=0;i<index;i++) {
				temp = Moves[i];
				to = (temp>>6)&63;
				from = temp&63;
				Magnum.makeMove(temp,false,false);
				if (!inCheck(side)) {
					Magnum.unMake(temp,false,false);
					return false;
				}
				Magnum.unMake(temp,false,false);
			}
			return true;
		}			
		return false;
	}		
	
	/************************************************************************
	This method will determine if the game is over due to a stalemate
	parameters int side - the side to move next
	************************************************************************/
	private boolean isStaleMate(int side) {
		
		int[] Moves;
		int temp;
		int index;
		int to;
		int from;
		
		if(!inCheck(side)) {				//side to move must not be in check...otherwise could be checkmate

			Moves = new int[128];
			index = getMoves(side,Moves,0);
			for(int i=0;i<index;i++) {
				temp = Moves[i];
				to = (temp>>6)&63;
				from = temp&63;
				Magnum.makeMove(temp,false,false);
				if(!inCheck(side)) {
					Magnum.unMake(temp,false,false);
					return false;
				}
				else 
					Magnum.unMake(temp,false,false);	
			}
			return true;
		}			
		return false;
	}	
	
	
	private int getPos(long pos) { // this method will return a board position given a proper long
			int first = (int)pos;
			int second=0;
			int sq=0;
			if(first!=0) {
				second = first;
				sq = 0;
			}else {
				second = (int)(pos>>>32);
				sq+=32;	
			}
			second*=debruijn;
			second>>>=27;
			return sq+index32[second];
	}		
	
	private int getPos(int pos) { // this method will return a board position given a proper long
			
			
			pos*=debruijn;
			pos>>>=27;
			return index32[pos];
	}	
	
	public int getMoves(int side, int[] Moves, int start) {
		//Evaluation2.getEval(99);
		long pieces;
		long toSquares;
		long fromBit;
		//long toBit;
		int toSq;
		int toSq2;
		int bit;
		//long passant;
		int to;
		int from;
		int value = 0;
		int index = start;
		//boolean lCastle;
		//boolean rCastle;
		int type;
		int valMult = 5;
		if (side == -1) {													//white moving
			//passant = Board.getPassantB();
			
			pieces = Board.getWhitePawns();
			
			long moves = pieces<<8 & ~Board.bitboard & ~Global.rankMasks[7];
			long doubleMoves = moves<<8 & Global.rankMasks[3] & ~Board.bitboard;
			toSq = (int)moves;
			toSq2 = (int)(moves>>>32);
			while(toSq != 0) {
				bit = toSq & -toSq;
				toSq ^= bit;
				to = getPos(bit);
				from = to-8;
				moveOrder[index] = Hist2[from][to];
                                //if(Evaluation2.BB[to] == 0)
                                //    moveOrder[index] += NO_ATTACK_BONUS;
                                //if(Evaluation2.WB[to] > 0)
                                //    moveOrder[index] += DEFENCE_BONUS;
                                
				//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
				//moveOrder[index] += 2;
				Moves[index++] = MoveFunctions.makeMove(to,from,5,-1,Global.ORDINARY_MOVE,60);
			}
			while(toSq2 != 0) {
				bit = toSq2 & -toSq2;
				toSq2 ^= bit;
				to = getPos(bit)+32;
				from = to-8;
				moveOrder[index] = Hist2[from][to];
				//if(Evaluation2.BB[to] == 0)
                                //    moveOrder[index] += NO_ATTACK_BONUS;
                                //if(Evaluation2.WB[to] > 0)
                                //    moveOrder[index] += DEFENCE_BONUS;
                                
                                
                                //moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
				//moveOrder[index] += 2;
				Moves[index++] = MoveFunctions.makeMove(to,from,5,-1,Global.ORDINARY_MOVE,60);
			}
			toSq = (int)doubleMoves;
			while( toSq != 0) {
				bit = toSq & -toSq;
				toSq ^= bit;
				to = getPos(bit);
				from = to - 16;
				moveOrder[index] = Hist2[from][to];
                                //if(Evaluation2.BB[to] == 0)
                                //    moveOrder[index] += NO_ATTACK_BONUS;
                                //if(Evaluation2.WB[to] > 0)
                                //    moveOrder[index] += DEFENCE_BONUS;
                                //moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
				//moveOrder[index] += 2;
				Moves[index++] = MoveFunctions.makeMove(to,from,5,-1,Global.ORDINARY_MOVE,60);
			}	
			
			pieces = Board.getWhiteKnights();
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= ~Board.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = getPos(bit);	
					value = 0;
					
					value += Global.wKnightVals[to];
					value -= Global.wKnightVals[from];
					
					moveOrder[index] = Hist2[from][to]+value*valMult;
					
                                        //if(Evaluation2.BB[to] == 0)
                                         //   moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.WB[to] > 17)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//moveOrder[index] += 4;
					Moves[index++] = MoveFunctions.makeMove(to,from,1,-1,Global.ORDINARY_MOVE,60);
								
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = getPos(bit)+32;	
					value = 0;
					
					value += Global.wKnightVals[to];
					value -= Global.wKnightVals[from];
					
					moveOrder[index] = Hist2[from][to]+value*valMult;
					//if(Evaluation2.BB[to] == 0)
                                       //     moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.WB[to] > 17)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//moveOrder[index] += 4;
					Moves[index++] = MoveFunctions.makeMove(to,from,1,-1,Global.ORDINARY_MOVE,60);
								
				}
			}
			pieces = Board.getWhiteBishops();
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= ~Board.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = getPos(bit);
					value = 0;
					
					value += Global.bishopVals[to];
					value -= Global.bishopVals[from];	
					
					moveOrder[index] = Hist2[from][to] + value * valMult;
					
                                        //if(Evaluation2.BB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.WB[to] > 17)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//moveOrder[index] += 4;
					Moves[index++] = MoveFunctions.makeMove(to,from,2,-1,Global.ORDINARY_MOVE,60);		
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = getPos(bit)+32;
					value = 0;
					
					value += Global.bishopVals[to];
					value -= Global.bishopVals[from];	
			
					moveOrder[index] = Hist2[from][to] + value * valMult;
					
                                        //if(Evaluation2.BB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.WB[to] > 17)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//moveOrder[index] += 4;
					Moves[index++] = MoveFunctions.makeMove(to,from,2,-1,Global.ORDINARY_MOVE,60);		
				}
			}	
			pieces = Board.getWhiteRooks();
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= ~Board.bitboard;
				toSq = (int)(toSquares);
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = getPos(bit);
					value = 0;
					
					moveOrder[index] = Hist2[from][to] + value * valMult;
					
                                        //if(Evaluation2.BB[to] == 0)
                                         //   moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.WB[to] > 33)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//moveOrder[index] += 5;
					Moves[index++] = MoveFunctions.makeMove(to,from,0,-1,Global.ORDINARY_MOVE,60);		
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = getPos(bit)+32;
					value = 0;
					
					moveOrder[index] = Hist2[from][to] + value * valMult;
					
                                        //if(Evaluation2.BB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.WB[to] > 33)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					
                                        //moveOrder[index] += 5;
					Moves[index++] = MoveFunctions.makeMove(to,from,0,-1,Global.ORDINARY_MOVE,60);		
				}
			}			
			pieces = Board.getWhiteQueen();
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= ~Board.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares >>> 32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = getPos(bit);
					value = 0;
					
					moveOrder[index] = Hist2[from][to] + value*valMult;
					
                                        //if(Evaluation2.BB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.WB[to] > 65)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//moveOrder[index] += 7;
					Moves[index++] = MoveFunctions.makeMove(to,from,3,-1,Global.ORDINARY_MOVE,60);	
							
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = getPos(bit) + 32;
					value = 0;
					
					moveOrder[index] = Hist2[from][to] + value*valMult;
					
                                        //if(Evaluation2.BB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.WB[to] > 65)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//moveOrder[index] += 7;
					
                                        Moves[index++] = MoveFunctions.makeMove(to,from,3,-1,Global.ORDINARY_MOVE,60);	
							
				}
			}			
			pieces = Board.getWhiteKing();
			if (pieces != 0) {
				from = getPos(pieces);		
				toSquares = Board.getAttackBoard(from);
				
				if(Board.wCastle != Global.NO_CASTLE) {							//are castle moves possible?
					
					long castle = Board.getWKingCastle(from);
					if(Board.wCastle == Global.LONG_CASTLE)
						castle &= Global.set_Mask[2];
					if(Board.wCastle == Global.SHORT_CASTLE)
						castle &= Global.set_Mask[6];
					toSquares |=castle;
				}
				toSquares &= ~Board.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>32);
				while (toSq != 0) {
					type = Global.ORDINARY_MOVE;
					bit = toSq & -toSq;
					toSq ^= bit;
					to = getPos(bit);
					if(from == 4 && to == 2)
						type = Global.LONG_CASTLE;
					if(from == 4 && to == 6)
						type = Global.SHORT_CASTLE;
					moveOrder[index] = Hist2[from][to];
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					Moves[index++] = MoveFunctions.makeMove(to,from,4,-1,type,60);		
				}
				while (toSq2 != 0) {
					type = Global.ORDINARY_MOVE;
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = getPos(bit)+32;
					moveOrder[index] = Hist2[from][to];
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					Moves[index++] = MoveFunctions.makeMove(to,from,4,-1,type,60);		
				}			
		
			}
			
		} else {								//black moving
			//passant = Board.getPassantW();
			pieces = Board.getBlackPawns();
			
			long moves = pieces>>8 & ~Board.bitboard & ~Global.rankMasks[0];
			long doubleMoves = moves>>8 & Global.rankMasks[4] & ~Board.bitboard;
			toSq = (int)moves;
			toSq2 = (int)(moves>>>32);
			
			while(toSq != 0) {
				bit = toSq & -toSq;
				toSq ^= bit;
				to = getPos(bit);
				from = to+8;
				moveOrder[index] = Hist[from][to];
				//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
				//if(Evaluation2.WB[to] == 0)
                                //    moveOrder[index] += NO_ATTACK_BONUS;
                                //if(Evaluation2.BB[to] > 0)
                                //    moveOrder[index] += DEFENCE_BONUS;
                                //moveOrder[index] += 2;
				Moves[index++] = MoveFunctions.makeMove(to,from,11,-1,Global.ORDINARY_MOVE,60);	
			}
			while(toSq2 != 0) {
				bit = toSq2 & -toSq2;
				toSq2 ^= bit;
				to = getPos(bit)+32;
				from = to+8;
				moveOrder[index] = Hist[from][to];
				//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
				//if(Evaluation2.WB[to] == 0)
                                //    moveOrder[index] += NO_ATTACK_BONUS;
                                //if(Evaluation2.BB[to] > 0)
                                //    moveOrder[index] += DEFENCE_BONUS;
                                //moveOrder[index] += 2;
				Moves[index++] = MoveFunctions.makeMove(to,from,11,-1,Global.ORDINARY_MOVE,60);	
			}
			toSq2 = (int)(doubleMoves>>>32);
			while(toSq2 != 0) {
				bit = toSq2 & -toSq2;
				toSq2 ^= bit;
				to = getPos(bit)+32;
				from = to+16;
				moveOrder[index] = Hist[from][to];
				//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
				//if(Evaluation2.WB[to] == 0)
                                //    moveOrder[index] += NO_ATTACK_BONUS;
                                //if(Evaluation2.BB[to] > 0)
                                 //   moveOrder[index] += DEFENCE_BONUS;
                                //moveOrder[index] += 2;
				Moves[index++] = MoveFunctions.makeMove(to,from,11,-1,Global.ORDINARY_MOVE,60);	
			}

			pieces = Board.getBlackKnights();
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= ~Board.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = getPos(bit);
					value = 0;
					
					value += Global.bKnightVals[to];
					value -= Global.bKnightVals[from];	
					
					moveOrder[index] = Hist[from][to] + value * valMult;
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//if(Evaluation2.WB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.BB[to] > 17)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += 4;
					Moves[index++] = MoveFunctions.makeMove(to,from,7,-1,Global.ORDINARY_MOVE,60);		
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = getPos(bit)+32;
					value = 0;
				
					value += Global.bKnightVals[to];
					value -= Global.bKnightVals[from];	
					
					moveOrder[index] = Hist[from][to] + value * valMult;
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//if(Evaluation2.WB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.BB[to] > 17)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += 4;
					Moves[index++] = MoveFunctions.makeMove(to,from,7,-1,Global.ORDINARY_MOVE,60);		
				}
			}
			pieces = Board.getBlackBishops();
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= ~Board.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = getPos(bit);
					value = 0;
	
					value += Global.bishopVals[to];
					value -= Global.bishopVals[from];
					
					moveOrder[index] = Hist[from][to] + value * valMult;
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//if(Evaluation2.WB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                       // if(Evaluation2.BB[to] > 17)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += 4;
					Moves[index++] = MoveFunctions.makeMove(to,from,8,-1,Global.ORDINARY_MOVE,60);		
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = getPos(bit)+32;
					value = 0;
					
					value += Global.bishopVals[to];
					value -= Global.bishopVals[from];
					
					moveOrder[index] = Hist[from][to] + value * valMult;
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//if(Evaluation2.WB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.BB[to] > 17)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += 4;
					Moves[index++] = MoveFunctions.makeMove(to,from,8,-1,Global.ORDINARY_MOVE,60);		
				}
			}
			pieces = Board.getBlackRooks();
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= ~Board.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = getPos(bit);
					value = 0;
					
					moveOrder[index] = Hist[from][to];
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//if(Evaluation2.WB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.BB[to] > 33)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += 5;
					Moves[index++] = MoveFunctions.makeMove(to,from,6,-1,Global.ORDINARY_MOVE,60);			
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = getPos(bit)+32;
					value = 0;
		
					moveOrder[index] = Hist[from][to];
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//if(Evaluation2.WB[to] == 0)
                                       //     moveOrder[index] += NO_ATTACK_BONUS;
                                       // if(Evaluation2.BB[to] > 33)
                                       //     moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += 5;
					Moves[index++] = MoveFunctions.makeMove(to,from,6,-1,Global.ORDINARY_MOVE,60);			
				}
			}	
							
			pieces = Board.getBlackQueen();
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= ~Board.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = getPos(bit);
					value = 0;
				
					moveOrder[index] = Hist[from][to];
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//if(Evaluation2.WB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.BB[to] > 65)
                                       //     moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += 7;
					Moves[index++] =  MoveFunctions.makeMove(to,from,9,-1,Global.ORDINARY_MOVE,60);			
				}
				while (toSq2 != 0) {
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = getPos(bit) + 32;
					value = 0;
					//if((Board.isBlackAttacked(to)))
					//	value -= 5;
					moveOrder[index] = Hist[from][to];
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					//if(Evaluation2.WB[to] == 0)
                                        //    moveOrder[index] += NO_ATTACK_BONUS;
                                        //if(Evaluation2.BB[to] > 65)
                                        //    moveOrder[index] += DEFENCE_BONUS;
                                        //moveOrder[index] += 7;
					Moves[index++] =  MoveFunctions.makeMove(to,from,9,-1,Global.ORDINARY_MOVE,60);			
				}
			}		
			pieces = Board.getBlackKing();
			if (pieces != 0) {
				from = getPos(pieces);		
				toSquares = Board.getAttackBoard(from);
				
				if(Board.bCastle != Global.NO_CASTLE) {							//are castle moves possible?
					long castle = Board.getBKingCastle(from);
					if(Board.bCastle == Global.LONG_CASTLE)
						castle &= Global.set_Mask[58];
					if(Board.bCastle == Global.SHORT_CASTLE)
						castle &= Global.set_Mask[62];
					toSquares |=castle;
				}
				toSquares &= ~Board.bitboard;
				toSq = (int)toSquares;
				toSq2 = (int)(toSquares>>>32);
				while (toSq != 0) {
					bit = toSq & -toSq;
					toSq ^= bit;
					to = getPos(bit);
					moveOrder[index] = Hist[from][to];
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					Moves[index++] =  MoveFunctions.makeMove(to,from,10,-1,Global.ORDINARY_MOVE,60);		
				}
				while (toSq2 != 0) {
					type = Global.ORDINARY_MOVE;
					bit = toSq2 & -toSq2;
					toSq2 ^= bit;
					to = getPos(bit)+32;
					if(from == 60 && to == 58)
						type = Global.LONG_CASTLE;
					if(from == 60 && to == 62)
						type = Global.SHORT_CASTLE;
					moveOrder[index] = Hist[from][to];
					//moveOrder[index] += SEE.getSEE2(side,to,from) * 10000;
					Moves[index++] =  MoveFunctions.makeMove(to,from,10,-1,type,60);		
				}
			}				
			
		}
		sortNonCaps(start,index,Moves);
		return index;
	}
	private static final void sortNonCaps(int start, int noMoves, int[] Moves) {
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
	
	public int getCaptures(int side, int[] Captures, int cutoff) {
		long pieces;
		long toSquares;
		long fromBit;
		long toBit;
		int passant;
		//long friends;
		long enemies;
		int to;
		int from;
		int value;
		int index = 0;
		int cP;
		int type;
		long pMask;
		//int toSq;
		//int toSq2;
		//int bit;
		if (side == -1) {			//white moving
			passant = Board.getPassantB();
			if(passant == -1)
				pMask = 0;
			else
				pMask = Global.set_Mask[passant];
			
			//friends = Board.whitepieces;
			enemies = Board.blackpieces;
		
			pieces = Board.whitepawns;
			
			long lAttack = (pieces<<7) & (enemies | pMask) & ~Global.fileMasks[7];
			long rAttack = (pieces<<9) & (enemies | pMask) & ~Global.fileMasks[0];
			long promo = pieces & Global.rankMasks[6];
			if(promo != 0) {
				promo <<= 8;
				promo &= ~Board.bitboard;
			}
			
			
			while (lAttack != 0) {
				toBit = lAttack & -lAttack;
				lAttack ^= toBit;
				to = getPos(toBit);
				from = to-7;
				value = SEE.getSEE(side,to,from,passant);
				if(value > cutoff) {
					type = Global.ORDINARY_MOVE;
					if(to == passant)
						type = Global.EN_PASSANT_CAP;
					if(to/8 == 7)
						type = Global.PROMO_Q;
					cP = Board.piece_in_square[to];
					value += 60;
					//value *=
                                        Captures[index++] = MoveFunctions.makeMove(to,from,5,cP,type,value);	
				}
			}
			while (rAttack != 0) {
				toBit = rAttack & -rAttack;
				rAttack ^= toBit;
				to = getPos(toBit);
				from = to-9;
				value = SEE.getSEE(side,to,from,passant);
				if(value > cutoff) {
					type = Global.ORDINARY_MOVE;
					if(to == passant)
						type = Global.EN_PASSANT_CAP;
					if(to/8 == 7)
						type = Global.PROMO_Q;
					cP = Board.piece_in_square[to];
					value += 60;
					Captures[index++] = MoveFunctions.makeMove(to,from,5,cP,type,value);	
				}
			}
			
			while(promo != 0) {
				toBit = promo & -promo;
				promo ^= toBit;
				to = getPos(toBit);
				from = to-8;
				value = 35;
				if(value > cutoff) {
					type = Global.PROMO_Q;
					value += 60;
				    Captures[index++] = MoveFunctions.makeMove(to,from,5,-1,type,value);	
				}
			}
				
			
			pieces = Board.whiteknights;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = getPos(toBit);
					value = SEE.getSEE(side,to,from,-1);
					if (value >= cutoff) {
						cP = Board.piece_in_square[to];
						value += 60;
						Captures[index++] = MoveFunctions.makeMove(to,from,1,cP,Global.ORDINARY_MOVE,value);		
					}
				}
			}
			pieces = Board.whitebishops;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = getPos(toBit);
					value = SEE.getSEE(side,to,from,-1);
					if (value >= cutoff) {
						cP = Board.piece_in_square[to];
						value += 60;
						Captures[index++] = MoveFunctions.makeMove(to,from,2,cP,Global.ORDINARY_MOVE,value);		
					}
				}
			}
			pieces = Board.whitequeen;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = getPos(toBit);
					value = SEE.getSEE(side,to,from,-1);
					if (value >= cutoff) {
						cP = Board.piece_in_square[to];
						value += 60;
						Captures[index++] = MoveFunctions.makeMove(to,from,3,cP,Global.ORDINARY_MOVE,value);			
					}
				}
			}
			pieces = Board.whiterooks;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = getPos(toBit);
					value = SEE.getSEE(side,to,from,-1);
					if (value >= cutoff) {
						cP = Board.piece_in_square[to];
						value += 60;
						Captures[index++] = MoveFunctions.makeMove(to,from,0,cP,Global.ORDINARY_MOVE,value);			
					}
				}
			}
			pieces = Board.whiteking;
			if (pieces != 0) {
				fromBit = pieces & -pieces;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = getPos(toBit);
					value = SEE.getSEE(side,to,from,-1);
					if (value >= cutoff) {
						cP = Board.piece_in_square[to];
						value += 60;
						Captures[index++] = MoveFunctions.makeMove(to,from,4,cP,Global.ORDINARY_MOVE,value);		
					}
				}
			}	
		} else {					//black moving
			
			passant = Board.getPassantW();
			if(passant == -1)
				pMask = 0;
			else
				pMask = Global.set_Mask[passant];
			
			//friends = Board.blackpieces;
			enemies = Board.whitepieces;
			
			pieces = Board.blackpawns;
			long lAttack = pieces>>9 & (enemies | pMask) & ~Global.fileMasks[7];
			long rAttack = pieces>>7 & (enemies | pMask) & ~Global.fileMasks[0];
			
			long promo = pieces & Global.rankMasks[1];
			if(promo != 0) {
				promo >>=8;
				promo &= ~Board.bitboard;
			}
			while(lAttack != 0) {
				toBit = lAttack & -lAttack;
				lAttack ^= toBit;
				to = getPos(toBit);
				from = to + 9;
				value = SEE.getSEE(side,to,from,passant);
				if(value > cutoff)  {
					type = Global.ORDINARY_MOVE;
					if(to == passant)
						type = Global.EN_PASSANT_CAP;
					if(to/8 == 0)
						type = Global.PROMO_Q;
					cP = Board.piece_in_square[to];
					value += 60;
					Captures[index++] = MoveFunctions.makeMove(to,from,11,cP,type,value);	
				}
			}
			while(rAttack != 0) {
				toBit = rAttack & -rAttack;
				rAttack ^= toBit;
				to = getPos(toBit);
				from = to + 7;
				value = SEE.getSEE(side,to,from,passant);
				if(value > cutoff) {
					type = Global.ORDINARY_MOVE;
					if(to == passant)
						type = Global.EN_PASSANT_CAP;
					if(to/8 == 0)
						type = Global.PROMO_Q;
					cP = Board.piece_in_square[to];
					value += 60;
					Captures[index++] = MoveFunctions.makeMove(to,from,11,cP,type,value);	
				}
			}
			while(promo != 0) {
				toBit = promo & -promo;
				promo ^= toBit;
				to = getPos(toBit);
				from = to + 8;
				value = 35;
				if(value > cutoff) {
					value += 60;
					Captures[index++] = MoveFunctions.makeMove(to,from,11,-1,Global.PROMO_Q,value);	
				}
			}
			
			pieces = Board.blackknights;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = getPos(toBit);
					value = SEE.getSEE(side,to,from,-1);
					if (value >= cutoff) {
						cP = Board.piece_in_square[to];
						value += 60;
						Captures[index++] = MoveFunctions.makeMove(to,from,7,cP,Global.ORDINARY_MOVE,value);		
					}
				}
			}
			pieces = Board.blackbishops;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = getPos(toBit);
					value = SEE.getSEE(side,to,from,-1);
					if (value >= cutoff) {
						cP = Board.piece_in_square[to];
						value += 60;
						Captures[index++] = MoveFunctions.makeMove(to,from,8,cP,Global.ORDINARY_MOVE,value);	
					}
				}
			}
			pieces = Board.blackqueen;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = getPos(toBit);
					value = SEE.getSEE(side,to,from,-1);
					if (value >= cutoff) {
						cP = Board.piece_in_square[to];
						value += 60;
						Captures[index++] = MoveFunctions.makeMove(to,from,9,cP,Global.ORDINARY_MOVE,value);	
						
					}
				}
			}
			pieces = Board.blackrooks;
			while (pieces != 0) {
				fromBit = pieces & -pieces;
				pieces ^= fromBit;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = getPos(toBit);
					value = SEE.getSEE(side,to,from,-1);
					if (value >= cutoff) {
						cP = Board.piece_in_square[to];
						value += 60;
						Captures[index++] = MoveFunctions.makeMove(to,from,6,cP,Global.ORDINARY_MOVE,value);	
					}
				}
			}
			pieces = Board.blackking;
			if (pieces != 0) {
				fromBit = pieces & -pieces;
				from = getPos(fromBit);		
				toSquares = Board.getAttackBoard(from);
				toSquares &= enemies;									
				while (toSquares != 0) {
					toBit = toSquares & -toSquares;
					toSquares ^= toBit;
					to = getPos(toBit);
					value = SEE.getSEE(side,to,from,-1);
					if (value >= cutoff) {
						cP = Board.piece_in_square[to];
						value += 60;
						Captures[index++] = MoveFunctions.makeMove(to,from,10,cP,Global.ORDINARY_MOVE,value);		
					}
				}
			}	
		}
		Arrays.sort(Captures, 0, index);
		return index;	
		
	}
	private int getCheckEscapes(int side, int[] escapes) {
		int attacks = 0;
		long friends;
		long bishops;
		long queen;
		long rooks;
		long pawns;
		long knights;
		long king;
		int kingPos;
		long fromBit;
		int attackFrom = -1;
		int index = 0;
		
		if(side == -1) {			//white moving
			friends = Board.whitepieces;
			king = Board.whiteking;
			
			bishops = Board.blackbishops;
			queen = Board.blackqueen;
			rooks = Board.blackrooks;
			pawns = Board.blackpawns;
			knights = Board.blackknights;
		} else {					//black moving
			friends = Board.blackpieces;
			king = Board.blackking;
			
			bishops = Board.whitebishops;
			queen = Board.whitequeen;
			rooks = Board.whiterooks;
			pawns = Board.whitepawns;
			knights = Board.whiteknights;
		}
		kingPos = getPos(king);
		long toSquares = Board.getAttackBoard(kingPos) & ~friends;
		long temp = Board.getMagicBishopMoves(kingPos);
		temp &= (bishops | queen);
		while( temp != 0) {
			fromBit = temp & -temp;
			attackFrom = getPos(fromBit);
			if(Global.Diag1Groups[kingPos] == Global.Diag1Groups[attackFrom])
				toSquares &= ((~Global.diag1Masks[Global.Diag1Groups[kingPos]])^Global.set_Mask[attackFrom]);
			else 
				toSquares &= ((~Global.diag2Masks[Global.Diag2Groups[kingPos]])^Global.set_Mask[attackFrom]);	
			temp ^= fromBit;
			attacks++;
		}
		
		
		temp = Board.getMagicRookMoves(kingPos);
		temp &= (rooks | queen);
		while( temp != 0) {
			fromBit = temp & -temp;
			attackFrom = getPos(fromBit);
			int diff = Math.abs(kingPos - attackFrom);
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
			int to = getPos(bit);
			if(side == -1) {		//white moving'
				if(Board.isWhiteAttacked(to))
					continue;
			} else {
				if(Board.isBlackAttacked(to))
					continue;
			}
			int cP = Board.piece_in_square[to];
			
			int	value = 10;
			if(cP != -1) {
				value = SEE.getSEE(side, to,kingPos, -1);
				if(value >= 0)
					value += 10000;
				else
					value = 0;	
			} else {
				//value = SEE.getSEE2(side,to,kingPos) * 100;
				if(side == -1)
					value = Hist2[kingPos][to];
				else
					value = Hist[kingPos][to];
			}			
			moveOrder[index] = value;
			escapes[index++] = MoveFunctions.makeMove(to,kingPos,Board.piece_in_square[kingPos], cP, Global.ORDINARY_MOVE,value);
		}
		
		
		if(attacks == 2) {
			sortNonCaps(0,index,escapes);
			return index;
		}	
		temp = Board.getKnightMoves(kingPos);
		temp &= knights;
		while( temp != 0) {
			fromBit = temp & -temp;
			attackFrom = getPos(fromBit);
			temp ^= fromBit;
			attacks++;
		}
		
	
		
		if(side == -1) {			//white moving
			temp = Board.getWPawnAttack(kingPos);
			temp &= Board.blackpawns;
			
                        while(temp != 0) {
				fromBit = temp & -temp;
				attackFrom = getPos(fromBit);
				temp ^= fromBit;
				attacks++;				
			}
		} else {					//black moving
			temp = Board.getBPawnAttack(kingPos);
			temp &= Board.whitepawns;
			
                        while(temp != 0) {
				fromBit = temp & -temp;
				attackFrom = getPos(fromBit);
				temp ^= fromBit;
				attacks++;				
			}
		}
		
		
		if(attacks == 2) {
			sortNonCaps(0,index,escapes);
			return index;
		}
		
	
		temp = Board.getAttack2(attackFrom);
		temp &= (friends & ~king);
		int type;
		int value;
		int cP = Board.piece_in_square[attackFrom];
		
                //int piece = getPos(attackFrom);
                //if attacking piece is a white pawn ( black moving )
                //System.out.println("piece pos is "+piece);
                
                while( temp != 0) {
			type = Global.ORDINARY_MOVE;
			fromBit = temp & -temp;
			temp ^= fromBit;
			int from = getPos(fromBit);
			if(SEE.isPinned(side,attackFrom,from))
				continue;
			value = SEE.getSEE(side,attackFrom, from, -1);
			if(value >= 0)
				value += 10000;
			else 
				value = 0;
			if(Board.piece_in_square[from] % 6==5) {
				if(attackFrom/8 == 0 || attackFrom/8 == 7)
					type = Global.PROMO_Q;
			}
			moveOrder[index] = value;
			escapes[index++] = MoveFunctions.makeMove(attackFrom,from,Board.piece_in_square[from],cP,type,value);
		}
		//piece = getPos(attackFrom);
                //if attacking piece is a white pawn ( black moving )
                //System.out.println("piece pos is "+piece);
                
                if(Board.piece_in_square[attackFrom] == 5) {
                    if( (attackFrom-8) == Board.getPassantW()) {
                        temp = Board.getWPawnAttack(attackFrom-8) & Board.blackpawns;
                        while(temp != 0) {
                            fromBit = temp & -temp;
                            temp ^= fromBit;
                            int from = getPos(fromBit);
                            if(SEE.isPinned(side,attackFrom-8,from))
				continue;
                            value = SEE.getSEE(side,attackFrom-8, from, Board.getPassantW());
                            if(value >= 0)
				value += 10000;
                            else 
			    value = 0;
                            moveOrder[index] = value;
                            escapes[index++] = MoveFunctions.makeMove(attackFrom-8,from,Board.piece_in_square[from],-1,Global.EN_PASSANT_CAP,value); 
                        }
                    }
                }
                //if attacking piece is a black pawn ( white moving )    
                if(Board.piece_in_square[attackFrom] == 11) {
                    if( (attackFrom+8) == Board.getPassantB()) {
                        temp = Board.getBPawnAttack(attackFrom+8) & Board.whitepawns;
                        while(temp != 0) {
                            fromBit = temp & -temp;
                            temp ^= fromBit;
                            int from = getPos(fromBit);
                            if(SEE.isPinned(side,attackFrom+8,from))
                                continue;
                            value = SEE.getSEE(side,attackFrom+8, from, Board.getPassantB());
                            if(value >= 0)
                                value += 10000;
                            else 
                                value = 0;
                            moveOrder[index] = value;
                            escapes[index++] = MoveFunctions.makeMove(attackFrom+8,from,Board.piece_in_square[from],-1,Global.EN_PASSANT_CAP,value); 
                    }
                }
            }
		//if one attacker is a slide piece, generate moves to block the sliding attack
		
		if(!Global.slides[Board.piece_in_square[attackFrom]]) return index;
	
		long squares = 0;						//intermediate squares between attacker and king
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
			int to = getPos(toBit);
			
			long attackers = Board.getMovesTo(to);
			attackers &= (friends & ~king);
			while(attackers != 0) {
				type = Global.ORDINARY_MOVE;
				fromBit = attackers & -attackers;
				attackers ^= fromBit;
				int from = getPos(fromBit);
				if(SEE.isPinned(side,to,from))
					continue;
				if(Board.piece_in_square[from] %6==5) {
					if(to/8 == 0 || to/8 == 7)
						type = Global.PROMO_Q;
				}
				//value = SEE.getSEE2(side,to,from) * 100;
				if(side == -1)
					value = Hist2[from][to];
				else
					value = Hist[from][to];
				moveOrder[index] = value;	
				escapes[index++] = MoveFunctions.makeMove(to,from,Board.piece_in_square[from],-1,type,0);
			}	
		}
		sortNonCaps(0,index,escapes);
		//Arrays.sort(escapes,0,index);
		return index;
	
	
	}
	
	

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
		
		
		if(Board.piece_in_square[from]!=piece)
			return false;
		
		if(Board.piece_in_square[to]!=-1||Board.piece_in_square[from]==-1)
			return false;
		
		if(Board.piece_in_square[from]/6==div) {		
			long temp;
			if(piece==5)	//wPawn
				temp = Board.getWPawnMoves(from,Board.getPassantB());
			else if(piece==11)
				temp = Board.getBPawnMoves(from,Board.getPassantW());
			else
				temp = Board.getAttackBoard(from);
			if((temp&Global.set_Mask[to])!=0)
				return true;
		}		
		return false;
		
	}

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
	private int getHashMoves( int key, int[] hashArr) {
		int hashCount = 0;
		int hashIndex = HashTable.hasHash(key, Board.hashValue, Board.hashValue2);
		
		if(hashIndex != 1) {
			int hashMove = HashTable.getMove(key, hashIndex);
			if(hashMove != 0)
				hashArr[hashCount++] = hashMove;
			
			if(hashIndex == 0) {
				hashIndex = HashTable.hasSecondHash(key, Board.hashValue, Board.hashValue2);
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


private int Max(int side,int depth,int alpha,int beta,boolean nMove,int wasExtend,boolean iid) {
		if(!iid)
			thisDepth++;
		if(++hashPrune >= nextTimeCheck) {	
				try {
					if (!timeLeft()) {
						stop = true;		
					}
				}
				catch(IOException e) {e.printStackTrace();};
			}
		
		if (stop) 
			return 0;	
		
		boolean isInCheck = false;
		int key = (Board.hashValue % Global.HASHSIZE);
		int extend = 0;
		
		if (!nMove && !iid && inCheck(side)){
		//if (inCheck(side)){
			isInCheck = true;			
			if(!iid) {
				extend++;
				depth++;
			}
		}
		
		int fscore = Board.value*side;
		//int fscore = Evaluation2.getEval(side);
                boolean fprune=false;
		boolean razored = false;
		
		int fMargin = 0;
		int noPrune = 0;
		
		if(wasExtend==0 && !isInCheck && depth==3 ) {
			if((fscore+900)<=alpha && Board.getNumberOfPieces(-side) > 3) {
				razored = true;
				depth--;
			}
		}
		
		int nullFail = 0;
		


		//int key = (Board.hashValue % Global.HASHSIZE);
		int hashIndex = HashTable.hasHash(key,Board.hashValue,Board.hashValue2);
		
			if (hashIndex != 1 ) {
				HashTable.setNew(key,hashIndex,ancient);
				int type = HashTable.getType(key,hashIndex);
				
				switch(type) {
					case(0):
						if(!iid && HashTable.getDepth(key,hashIndex)>=depth) {
							int hVal = HashTable.getValue(key,hashIndex);
							if(hVal <= alpha)
								return hVal;
						}
						break;
					case(1):
						if(!iid && HashTable.getDepth(key,hashIndex)>=depth) {
							hashPrune++;
							return HashTable.getValue(key,hashIndex);
						}
						break;
					case(2):
						if(!iid && HashTable.getDepth(key,hashIndex)>=depth) {
							int hVal = HashTable.getValue(key,hashIndex);
							if(hVal >= beta)
								return hVal;
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
	
		
		fscore +=500;
		if(wasExtend==0 && !isInCheck && depth ==2 && fscore<=alpha) {	
			fprune = true;
			fMargin = 500;
		}
			
		fscore -= 300;
		if(!isInCheck && depth ==1 && fscore<=alpha) {
			fprune = true;
			fMargin = 200;
		}
		
		int hashMove = 0;
		
		
		int value;
		if (depth <= 0) {			//if at leaf node return value
			prune++;
			value = Quies(side,0,alpha,beta);
			//value = Evaluation.getEval(side,0);
			depth = 0;
			
			if(value>=beta) 
				HashTable.addHash(key,Board.hashValue,Board.hashValue2,hashMove,value,depth,HASH_BETA,0,ancient);
			if(value<=alpha) 
				HashTable.addHash(key,Board.hashValue,Board.hashValue2,hashMove,value,depth,HASH_ALPHA,0,ancient);
			else 
				HashTable.addHash(key,Board.hashValue,Board.hashValue2,hashMove,value,depth,HASH_EXACT,0,ancient);
				
			
			return value;
			
		}
		//nullFail = 1;
		//if(Board.getMinNumberOfPieces() > 1)
		//	System.out.println("info string problem min number of pieces is "+Board.getMinNumberOfPieces());
		if(!razored && !fprune && !isInCheck && !nMove && (Board.pawnsKings != Board.bitboard) && Board.totalValue > 1200 && Board.getMinNumberOfPieces() >1  ) {
			Board.switchTurn();
			int reduce = 2;
			if(depth>6 && Board.getMaxNumberOfPieces() >3)
				reduce = 3;
			value = -Max(-side,depth-reduce-1,-beta,-beta+1,true,0,false);
			thisDepth--;
		
			Board.switchTurn();
			if(value>=beta) {		
				if(!stop) 	
					HashTable.addHash(key,Board.hashValue,Board.hashValue2,hashMove,value,depth,HASH_BETA,0,ancient);
				return value;
			}
			if(value <= -19000) {
			//if(value <= -19000 && wasExtend==0) {
                            //extend++;
                            //depth++;
                            nullFail = 1;
                        }           
                }
		
		//nMove = false;
		//int wasNull = nMove;
		int killerDepth = thisDepth;
		boolean oneLegal = false;
		int piece;
		int to, from;
		int bestFrom = 0;
		int bestTo = 0;
		int bMove =  VALUE_START;
		int hType = HASH_ALPHA;
		int index=0;
		int[] moveArr = new int[128];
		int theMove;
		
		int endIndex = 0;
		int state= SEARCH_HASH;			//state of search we are in..etc Hash move, killers, or regular
		
		int badCapStart = 0;
		int capIndex = 0;
		
		int pExtend = 0;
		boolean hasVal = false;
		int reps;
		int noKillers = 0;
		int totalHash = 0;
		int hashCount = 0;
		int bestFullMove = 0;
		int[] hashArr = new int[8];
		//hashCount = 0;
                //Evaluation2.getEval(1);
	while(state != SEARCH_END) {
			
		switch(state) {
			case(SEARCH_HASH):
				hashCount = getHashMoves(key, hashArr);
				
				if(hashCount == 0 && depth > 3 && !nMove  && !isInCheck ) {
					//HashTable.clearPosition(key);
					Max(side,depth-2,alpha,beta,true,0,true);
					//thisDepth--;
					hashIndex = HashTable.hasHash(key,Board.hashValue,Board.hashValue2);
			
					if(hashIndex != 1) {
				
						hashArr[0] = HashTable.getMove(key,hashIndex);
						if(hashArr[0] != 0) 
							hashCount++;
					 
					}
					if(hashCount == 0 && hashIndex == 0) {
						hashIndex = HashTable.hasSecondHash(key,Board.hashValue,Board.hashValue2);
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
				//totalHash = hashCount;
	
			
				
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
				index = getCaptures(side,moveArr,Integer.MIN_VALUE);
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
				/*
				if(totalHash > 0){
					System.out.println("info string total hash is "+totalHash);
					System.out.println("piece is "+MoveFunctions.getPiece(hashArr[0]));
					System.out.println("to is "+MoveFunctions.getTo(hashArr[0]));
					System.out.println("from is "+MoveFunctions.getFrom(hashArr[0]));
				
				}
				*/
				break;
			case(BAD_CAPS):	
				index = badCapStart; 
				endIndex = 0;
				break;
		}	
		
		
		for (int i = index - 1; i >= endIndex; i--) {
		
			theMove = moveArr[i];
			
			if (moveArr[i] == -200000)  continue;
			
			if(state == GOOD_CAPTURES && ((MoveFunctions.getValue(theMove)-60) < 0))	{
				badCapStart = i+1;
				break;
			}
				
			pExtend = 0;
		
			reps = Magnum.makeMove(theMove,false,true);		//make the move;
			
			if (!isInCheck && inCheck(side)) {
				Magnum.unMake(theMove,false,true);								//cant leave king in check
				continue;
			}
			
			oneLegal = true;
			to = MoveFunctions.getTo(theMove);
			from = MoveFunctions.getFrom(theMove);
			piece = MoveFunctions.getPiece(theMove);
			/*
			if(isInCheck && inCheck(side)) {
		
				System.out.println("info string in check error "+i+" from is "+from+" to is "+to+" piece is "+piece);
				if(side == -1) {
	
					System.out.println("info string is white attacked at to is "+Board.isWhiteAttacked(to));
					System.out.println("info string white attacker pos is "+getPos(Board.getWhiteAttacker(to)));
					System.out.println("info string white attacker is "+Board.piece_in_square[getPos(Board.getWhiteAttacker(to))]);
				}
				Magnum.unMake(theMove,false,true);								//cant leave king in check
				if(side == -1)
					System.out.println("info string Now is white attacked at to is "+Board.isWhiteAttacked(to));
				
				continue;
			}
			*/
			if((Board.getTotalValue() < 5000) && extend == 0 && !isInCheck && (piece%6==5 && (to/8==6 || to/8==1)) )		{	//extention for close to promotion
				pExtend++;
			}		
			extend+=pExtend;
			
			if(fprune && extend == 0 && !inCheck(-side)) {
				fscore = Board.getEval2(side);
				if(fscore + fMargin <= alpha) {	
					if(fscore > bMove|| !hasVal) {
						hasVal = true;
						bMove = fscore;
						bestFrom = from;
						bestTo = to;
						bestFullMove = theMove;
					}
					fCount++;
					noPrune++;
					
					Magnum.unMake(theMove,false,true);
					continue;
				}
				
			}		
			
			
			if (reps == 2)
				value = 0;
			else { 	
					if(hType==HASH_ALPHA) {
						value = -Max(-side, depth-1+pExtend, -beta, -alpha, nMove, extend, false);
						thisDepth--;
					}else {
						value = -Max(-side,depth-1+pExtend,-alpha-1,-alpha,nMove,extend,false);
						thisDepth--;
						if(value>alpha&&value<beta) {
							value = -Max(-side,depth-1+pExtend,-beta,-alpha,nMove,extend,false);
							thisDepth--;
						}
					}		
			}	
			extend -= pExtend;
			if (Board.getDraw() >= 100)
				value = 0;
			Magnum.unMake(theMove,false,true);
			if(value > bMove) {
				
				
				hasVal = true;
			if(value > alpha) {
				hType = HASH_EXACT;
				alpha = value;
			}
			
			if (value >= beta) {
				if (!stop) {
					HashTable.addHash(key,Board.hashValue,Board.hashValue2,theMove,value,depth,HASH_BETA,nullFail,ancient); 	
					if (side == 1)
						Hist[from][to]+=2;//(9-killerDepth);
					else
						Hist2[from][to]+=2;//(9-killerDepth);
				}
				if (state == NON_CAPS) {									//if state is a non capture or a bad capture
					if(value >= 19000) {
							theMove = MoveFunctions.setValue(theMove,200);
					}
					
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
	
		}		//end for
	
	state++;
	}			// end while	
		if(!isInCheck&& !oneLegal && noPrune==0)	{				//stalemate detection
			bMove = 0;
			hType = 4;	
		}else if(isInCheck && !oneLegal)	{		
			bMove = -20000+thisDepth;
			hType = 4;
			if(!stop)
				HashTable.addHash(key,Board.hashValue,Board.hashValue2,bestFullMove,-20000,depth,hType,nullFail,ancient);   		
			return bMove;
		
		}
		if(fprune && noPrune>0) {
			if(hType == HASH_EXACT)
				hType = HASH_BETA;
			if(hType == HASH_ALPHA)	
				bMove = alpha;
		}
		
		if(!stop) 
			HashTable.addHash(key,Board.hashValue,Board.hashValue2,bestFullMove,bMove,depth,hType,nullFail,ancient);   		
		
		if(oneLegal) {
		if(side==1)	//black moving
			Hist[bestFrom][bestTo]++;//(7-killerDepth);
		else
			Hist2[bestFrom][bestTo]++;//(7-killerDepth);
		
		}
		
		return bMove;
	
	
	}	

	
		
	

	private void Perfit(int side,int depth,int alpha,int beta) {
	
		int reps;
		
		boolean inCheck = false;
		if(inCheck(side))
			inCheck = true;
		if(depth<=0) {
			perfit++;
			return;
		}
		int index;
		int[] moveArr = new int[128];
		if(!inCheck) {
			int index2 = getCaptures(side, moveArr, Integer.MIN_VALUE);
			index = getMoves(side, moveArr,index2);	
		} else {
			index = getCheckEscapes(side,moveArr);
		}
		for(int i = index-1; i >=0; i--) {
			reps = Magnum.makeMove(moveArr[i],false,false);		//make the move;
			
			if(!inCheck && inCheck(side)) {
				Magnum.unMake(moveArr[i],false,false);
				continue;
			}
			
			if(reps==3)
				return;
			else 	
				Perfit(-side,depth-1,-beta,-alpha);
			Magnum.unMake(moveArr[i],false,false);
			
			
		}	
	}			
		
		
		
	private int Quies(int side,int depth,int alpha,int beta) {
		int[] capArr = new int[60];
		int value;
		int index=0;
		//int thePiece;
		int bValue;
		boolean isInCheck = false;
		if(depth > 0 && inCheck(side)) {
			isInCheck = true;
			index = getCheckEscapes(side,capArr);
			if(index == 0)
				return -20000+thisDepth;
			bValue = -20000+thisDepth;
		}else {
                        //value = Board.getValue()*side;
			
                    
                    value = Evaluation2.getEval(side);
			
                       //value = Evaluation.getEval(side,alpha);
                        if (value > alpha) {
				alpha = value;
			}	
			if (alpha >= beta)
				return alpha;
			index = getCaptures(side, capArr, Math.max(0, (alpha - value)/20));
		
			if (index == 0 ) 
				return value;
			bValue = value;
		}
		
		//boolean oneLegal = false;
		for(int i = index-1; i >=0; i--) {
			Magnum.makeMove(capArr[i],false,false);		//make the move;
			if(!isInCheck && inCheck(side)) {
				Magnum.unMake(capArr[i],false,false);
				continue;
			}
			//oneLegal = true;
			value = -Quies(-side,depth+1,-beta,-alpha);
			Magnum.unMake(capArr[i],false,false);
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
}		
	
	