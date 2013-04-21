package magnumchess;

/**
 * Main.java
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

import java.io.*;

/*
 * Main.java
 * Execution begins in this class as this class contains the main method
 * After initializing the engine, the engine waits for the user to input the "uci" command
 * Once this command is given, MagnumChess will go into uci mode where it responds to uci commands 
 *
 * 
 *
 * @version 	4.00 March 2012
 * @author 	Eric Stock
 */
public class Main
{
   
    
    /** time management variables */
    public static final int DEFAULT_WTIME = 1000;
    public static final int DEFAULT_BTIME = 1000;
    public static final int DEFAULT_WINC = 0;
    public static final int DEFAULT_BINC = 0;
    public static final int DEFAULT_TOGO = 40;
    
    public static BufferedReader reader;
    public static String cmd;
    public static Engine theSearch;
    public static Board Board;
    public static HistoryWriter writer;
    public static Evaluation2 eval;
    public static MoveFunctions moveFunctions;
    public static SEE see;
    
    //for debugging end of game conditions
    public static int iNumberCheckmates;
    public static int iNumberDrawRepetition;
    public static int iNumberDraw50Moves;
    public static int iNumberStalemates;
    public static int iNumberInsufficientMaterial;
    public static int iNumberRandomGames;

	 public static String latestMoves;

   
         
         /*
     * main method
     * Execution begins here
     *
     * @param String args[] - the arguments passed to the program
     * 
     */ 
    public static void main(String args[]) throws IOException 
	{	
        try {
            see = new SEE();
            
            moveFunctions = new MoveFunctions();
            theSearch = new Engine();
            writer = new HistoryWriter( theSearch );
            Board = Board.getInstance();
            eval = new Evaluation2();
            Board.newGame();
            reader = new BufferedReader(new InputStreamReader(System.in));
            latestMoves = "none";
				printGreeting();
				getCmd();
        } catch(Exception ex) {
            System.out.print("info string ");
            //File f = new File("errorLogs"+File.separator+"file"+Math.random()*500+".log");
				File f = new File("error");
            System.out.println(f.getCanonicalPath());
            f.createNewFile();
				FileWriter fileWriter;
				BufferedWriter buffWriter;
				try {
					fileWriter = new FileWriter(f);
					buffWriter = new BufferedWriter(fileWriter);
					buffWriter.write(latestMoves+"\n");
					buffWriter.write(ex.toString()+"\n");
					StackTraceElement[] arrTrace = ex.getStackTrace();
					int stackSize = arrTrace.length;
					for(int i=0; i<stackSize; i++)
					{
						buffWriter.write(arrTrace[i].toString()+"\n");
					}
					buffWriter.close();
				} catch(Exception ex2) {ex2.printStackTrace(System.out);}

				System.out.println(ex);
            ex.printStackTrace();
		}
	}
	
    /*
     * method printGreeting()
     * 
     * prints a simple greeting message
     */ 
    public static void printGreeting() {
        System.out.println("*****************MAGNUM CHESS***************");
        System.out.println("*****************Version 4.00***************");
        System.out.println("to play in UCI mode type \"uci\"");
    }
	/*
     * method uci
     * 
     * enters a while loop and processes input from the user
     * 
     */ 
    public static void uci() throws IOException{
		int movetime;
		int maxMoveTime;
		int searchDepth;
		int wtime=0;
		int btime=0;
		int winc=0;
		int binc=0;
		int togo = 0;
        
		boolean infinite = false;				//infinite time controls
		System.out.println("id name Magnum");
		System.out.println("id author Eric Stock");
		
		System.out.println("option name Hash type spin default 64 min 8 max 512");
		System.out.println("option name Evaluation Table type spin default 8 min 1 max 64");
		System.out.println("option name Pawn Table type spin default 8 min 1 max 64");
		System.out.println("uciok");
		while(true) {
			cmd = reader.readLine();
			if(cmd.startsWith("quit"))
                System.exit(0);
		else if(cmd.equals("eval_dump_white")) {
            Evaluation2.getEval(Global.COLOUR_WHITE, -2000, 2000, 0);
            Evaluation2.printEvalTerms();
		}
		else if(cmd.equals("eval_dump_black"))
		{
		  Evaluation2.getEval(Global.COLOUR_BLACK, -2000, 2000, 0);
            Evaluation2.printEvalTerms();
		}
		else if(cmd.equals("see_test"))
		{
			theSearch.getCaptures(-1, new int[50]);
			theSearch.getCaptures(1, new int[50]);
		}
		else if ("isready".equals( cmd ))
				System.out.println("readyok");

            if(cmd.startsWith("perft")) {
                cmd = cmd.substring(5);
                cmd = cmd.trim();
                int depth = Integer.parseInt(cmd.substring(0));
                theSearch.PerftTest(depth);

            }
            if(cmd.startsWith("divide")) {
                cmd = cmd.substring(6);
                cmd = cmd.trim();
                int depth = Integer.parseInt(cmd.substring(0));
                theSearch.Divide(depth);

            }
            if(cmd.startsWith("position")) {
				if(cmd.indexOf(("startpos"))!= -1) {
					int mstart = cmd.indexOf("moves");
					if(mstart>-1) {
						String moves = cmd.substring(mstart+5);
						latestMoves = moves;
						//Board.undoAll();

                  HistoryWriter.acceptMoves(moves);
					}
				} else {			//reading in a fen string
					int mstart = cmd.indexOf("moves");
					if(mstart> -1) {
						Board.undoAll();
                  String fen = cmd.substring(cmd.indexOf("fen")+4,mstart-1);
						Board.acceptFen(fen);
                  String moves = cmd.substring(mstart+5);
						HistoryWriter.acceptMoves(moves);
					} else {
						String fen = cmd.substring(cmd.indexOf("fen")+4);
						Board.acceptFen(fen);
					}
				}
			}
         else if(cmd.startsWith("setoption")) {
				int index = cmd.indexOf("Hash");
				if(index != -1)  {
					index = cmd.indexOf("value");
					cmd = cmd.substring(index+5);
					cmd = cmd.trim();
					int hashSize = Integer.parseInt(cmd.substring(0));
					Global.HASHSIZE = hashSize * 2048;
					//ensure always a power of 2 for proper indexing
                                        Global.HASHSIZE = Integer.highestOneBit(Global.HASHSIZE);
                                        Engine.resetHash();
					System.out.println("info string hashsize is "+hashSize);
				} else if(cmd.indexOf("Evaluation Table")!= -1) {
               index = cmd.indexOf("value");
               cmd = cmd.substring(index+5);
					cmd = cmd.trim();
					int evalSize = Integer.parseInt(cmd.substring(0));
               Global.EvalHASHSIZE = evalSize * 131072;
               Evaluation2.reSizeEvalHash();
               System.out.println("info string evalHash is "+evalSize);
            } else if(cmd.indexOf("Pawn Table") != -1) {
               index = cmd.indexOf("value");
               cmd = cmd.substring(index+5);
					cmd = cmd.trim();
					int evalSize = Integer.parseInt(cmd.substring(0));
               Global.PawnHASHSIZE = evalSize * 43960;
               Evaluation2.reSizePawnHash();
               System.out.println("info string pawnHash is "+evalSize);
            }
				else
				{
                    System.out.println("info string command not recognized");
            }
			}
			
         else if(cmd.startsWith("go")) {
				movetime = 0;
            maxMoveTime = 0;
				searchDepth = 0;
				infinite = false;
				if(cmd.indexOf("depth")!=-1) {
					try
					{
						int index = cmd.indexOf("depth");
						cmd = cmd.substring(index+5);
						cmd = cmd.trim();
						searchDepth = Integer.parseInt(cmd.substring(0));
						movetime = 9999999;
                  maxMoveTime = movetime;
					}
					catch(NumberFormatException ex) {}
				}
				else if(cmd.indexOf("movetime")!=-1) {
					try
					{
						int index = cmd.indexOf("movetime");
						cmd = cmd.substring(index+8);
						cmd = cmd.trim();
						movetime = Integer.parseInt(cmd.substring(0));
                  maxMoveTime = movetime;
						searchDepth = 40;
					}
					catch(NumberFormatException ex) {}
				}
				else if(cmd.indexOf("infinite")!=-1) {
					infinite = true;	
					searchDepth = 40;
					movetime = 1000;
               maxMoveTime = movetime;
					
				}	
				else {				//extract the clock times and increments
					try {
						searchDepth = 40;
						
                  String temp;
						int index = cmd.indexOf("wtime");

                        if (index == -1)
                            wtime = DEFAULT_WTIME;
                        else {
                            temp = cmd.substring(index+5).trim();
                            wtime = Integer.parseInt(temp.substring(0,temp.indexOf(" ")));
                        }
						index = cmd.indexOf("btime");
                        if (index == -1)
                            btime = DEFAULT_BTIME;
                        else {
                            temp = cmd.substring(index+5).trim();
                            if(temp.indexOf(" ")!=-1)
                                btime = Integer.parseInt(temp.substring(0,temp.indexOf(" ")));
                            else
                                btime = Integer.parseInt(temp);
                        }
						index = cmd.indexOf("winc");
                        if (index == -1)
                            winc = DEFAULT_WINC;
                        else {
                            temp = cmd.substring(index+4).trim();      
                            if(temp.indexOf(" ")!=-1)
                                winc = Integer.parseInt(temp.substring(0,temp.indexOf(" ")));
                            else 
                                winc = Integer.parseInt(temp);
                        }
						index = cmd.indexOf("binc");
                        if (index == -1)
                            binc = DEFAULT_BINC;
                        else {
                            temp = cmd.substring(index+4);
                            temp = temp.trim();
                            if(temp.indexOf(" ")!=-1)
    							binc = Integer.parseInt(temp.substring(0,temp.indexOf(" ")));
    						else
    							binc = Integer.parseInt(temp);
                        }
                        index = cmd.indexOf("movestogo");
                        if(index == -1) 
                            togo = DEFAULT_TOGO;
                        else {
                            temp = cmd.substring(index+9).trim();
                            togo = Integer.parseInt(temp);
                        }
						if(Board.getTurn()==1)	{		//black moving
							movetime = Math.max(0,(btime/togo + binc));
                     //reduce the move time a little, as most of the time we will be extending this time to find the first move of the last iteration
                     movetime = (int)(((double)movetime)* 0.85);
                     int  maxTimeLimit = (int)(((double)btime + (double)binc)*0.40);
                     maxMoveTime = Math.min(movetime * 3, maxTimeLimit);
                  }
                  else {
							movetime = Math.max(0,(wtime/togo + winc));
                     //reduce the move time a little, as most of the time we will be extending this time to find the first move of the last iteration
                     movetime = (int)(((double)movetime)* 0.85);
                     int  maxTimeLimit = (int)(((double)wtime + (double)winc)*0.40);
                     maxMoveTime = Math.min(movetime * 3, maxTimeLimit);
                  }
                  // on the last move before the time is increased, the move time will be higher than the maxMoveTime,
                  // so we adjust the maxMoveTime to be equal to the movetime
                  if(movetime > maxMoveTime)  {
                     maxMoveTime = movetime;
                  }
               }
					catch(NumberFormatException ex) {
                   ex.printStackTrace(System.err);
               }
				}
            String move = theSearch.search(movetime, maxMoveTime, searchDepth,infinite);
				System.out.println("bestmove "+move);
			}	
         else if(cmd.equals("ucinewgame")) {
				Board.newGame();
				Engine.resetHash();	
			}
		}		
	}	
	/*
     * method getCmd()
     * 
     * gets users commands when program is first launched
     * 
     */ 
    public static void getCmd() throws IOException{
		
		while(true) {
			cmd = reader.readLine();
			if(cmd.equals("uci")) {
				uci();
				break;
			}
			else if(cmd.indexOf("setvalue") != -1)
			{
				SetClopParams(cmd);
			}
			else if(cmd.equals("RandomTest")) {
				iNumberRandomGames = 0;
				while(iNumberRandomGames < 1000)
				{
					Board.newGame();
					Engine.resetHash();

					while(true) {
						int result = theSearch.RandomSearch();

						if(result != 0) {
							switch(result)
							{
								case(Global.CHECKMATE):
								{
									
									iNumberCheckmates++;
								}
								break;
								case(Global.STALEMATE):
								{
									
									iNumberStalemates++;
								}
								break;
								case(Global.DRAW_50MOVES):
								{
									
									iNumberDraw50Moves++;
								}
								break;
								case(Global.DRAW_REPETITION):
								{
									
									iNumberDrawRepetition++;
								}
								break;
								case(Global.INSUFICIENT_MATERIAL):
								{
									
									iNumberInsufficientMaterial++;
								}
								break;
							}
						break;
						}
					}
					iNumberRandomGames++;
				}
				System.out.println("number of checkmates "+iNumberCheckmates);
				System.out.println("number of stalemates "+iNumberStalemates);
				System.out.println("number of draw 50 "+iNumberDraw50Moves);
				System.out.println("number of draw repetition "+iNumberDrawRepetition);
				System.out.println("number of insufficient material "+iNumberInsufficientMaterial);
			}	   

			if(cmd.startsWith("quit"))
				System.exit(0);

		}		
	}

	public static void SetClopParams(String command)
	{
		if(cmd.indexOf("pawnv") != -1)  {
			int index = cmd.indexOf("pawnv");
			cmd = cmd.substring(index+5);
			cmd = cmd.trim();
			int pawnValue = Integer.parseInt(cmd.substring(0));
			Global.values[Global.PIECE_PAWN] = pawnValue;
			Global.values[Global.PIECE_PAWN + 6] = pawnValue;
			System.out.println("info string pawn value is "+pawnValue);
			Board.InitializeMaterialArray();
			Board.newGame();
		} else if(cmd.indexOf("bishopv") != -1)  {
			int index = cmd.indexOf("bishopv");
			cmd = cmd.substring(index+7);
			cmd = cmd.trim();
			int bishopValue = Integer.parseInt(cmd.substring(0));
			Global.values[Global.PIECE_BISHOP] = bishopValue;
			Global.values[Global.PIECE_BISHOP + 6] = bishopValue;
			System.out.println("info string bishop value is "+bishopValue);
			Board.InitializeMaterialArray();
			Board.newGame();
		} else if(cmd.indexOf("knightv") != -1)  {
			int index = cmd.indexOf("knightv");
			cmd = cmd.substring(index+7);
			cmd = cmd.trim();
			int knightValue = Integer.parseInt(cmd.substring(0));
			Global.values[Global.PIECE_KNIGHT] = knightValue;
			Global.values[Global.PIECE_KNIGHT + 6] = knightValue;
			System.out.println("info string knight value is "+knightValue);
			Board.InitializeMaterialArray();
			Board.newGame();
		} else if(cmd.indexOf("rookv") != -1)  {
			int index = cmd.indexOf("rookv");
			cmd = cmd.substring(index+5);
			cmd = cmd.trim();
			int rookValue = Integer.parseInt(cmd.substring(0));
			Global.values[Global.PIECE_ROOK] = rookValue;
			Global.values[Global.PIECE_ROOK + 6] = rookValue;
			System.out.println("info string rook value is "+rookValue);
			Board.InitializeMaterialArray();
			Board.newGame();
		} else if(cmd.indexOf("queenv") != -1)  {
			int index = cmd.indexOf("queenv");
			cmd = cmd.substring(index+6);
			cmd = cmd.trim();
			int queenValue = Integer.parseInt(cmd.substring(0));
			Global.values[Global.PIECE_QUEEN] = queenValue;
			Global.values[Global.PIECE_QUEEN+ 6] = queenValue;
			System.out.println("info string queen value is "+queenValue);
			Board.InitializeMaterialArray();
			Board.newGame();
		}
		else if(cmd.indexOf("passerMGv1") != -1)	{
			int index = cmd.indexOf("passerMGv1");
			cmd = cmd.substring(index+10);
			cmd = cmd.trim();
			int iPasserMG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.PassedPawnBonus[0][i] += iPasserMG;
			}
		}
		else if(cmd.indexOf("passerEGv1") != -1)	{
			int index = cmd.indexOf("passerEGv1");
			cmd = cmd.substring(index+10);
			cmd = cmd.trim();
			int iPasserEG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.PassedPawnBonus[1][i] += iPasserEG;
			}
		}
		else if(cmd.indexOf("passerMG") != -1)	{

			int index = cmd.indexOf("passerMG");
			cmd = cmd.substring(index+8);
			cmd = cmd.trim();
			int iPasserMG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.PassedPawnBonus[0][i] = (int)((float)Evaluation2.PassedPawnBonus[0][i] * (((float)iPasserMG) / 10.0f));
			}
		}
		else if(cmd.indexOf("passerEG") != -1)	{
			int index = cmd.indexOf("passerEG");
			cmd = cmd.substring(index+8);
			cmd = cmd.trim();
			int iPasserEG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.PassedPawnBonus[1][i] = (int)((float)Evaluation2.PassedPawnBonus[1][i] * (((float)iPasserEG) / 10.0f));
			}
		}
		else if(cmd.indexOf("cPasserMGv1") != -1)	{
			int index = cmd.indexOf("cPasserMGv1");
			cmd = cmd.substring(index+11);
			cmd = cmd.trim();
			int iPasserMG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.CandidatePawnBonus[0][i] += iPasserMG;
			}
		}
		else if(cmd.indexOf("cPasserEGv1") != -1)	{
			int index = cmd.indexOf("cPasserEGv1");
			cmd = cmd.substring(index+11);
			cmd = cmd.trim();
			int iPasserEG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.CandidatePawnBonus[1][i] += iPasserEG;

			}
		}
		else if(cmd.indexOf("cPasserMG") != -1)	{
			int index = cmd.indexOf("cPasserMG");
			cmd = cmd.substring(index+9);
			cmd = cmd.trim();
			int iPasserMG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.CandidatePawnBonus[0][i] = (int)((float)Evaluation2.CandidatePawnBonus[0][i] * (((float)iPasserMG) / 10.0f));
			}
		}
		else if(cmd.indexOf("cPasserEG") != -1)	{
			int index = cmd.indexOf("cPasserEG");
			cmd = cmd.substring(index+9);
			cmd = cmd.trim();
			int iPasserEG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.CandidatePawnBonus[1][i] = (int)((float)Evaluation2.CandidatePawnBonus[1][i] * (((float)iPasserEG) / 10.0f));

			}
		}
		else if(cmd.indexOf("isolatedMG") != -1)	{
			int index = cmd.indexOf("isolatedMG");
			cmd = cmd.substring(index+10);
			cmd = cmd.trim();
			int iIsolatedMG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.IsolatedPawn[0][i] = (int)((float)Evaluation2.IsolatedPawn[0][i] * (((float)iIsolatedMG) / 10.0f));
			}
		}
		else if(cmd.indexOf("isolatedEG") != -1)	{
			int index = cmd.indexOf("isolatedEG");
			cmd = cmd.substring(index+10);
			cmd = cmd.trim();
			int iIsolatedEG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.IsolatedPawn[1][i] = (int)((float)Evaluation2.IsolatedPawn[1][i] * (((float)iIsolatedEG) / 10.0f));
			}
		}
		else if(cmd.indexOf("chainMG") != -1)	{
			int index = cmd.indexOf("chainMG");
			cmd = cmd.substring(index+7);
			cmd = cmd.trim();
			int iChainMG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.ChainPawn[0][i] = (int)((float)Evaluation2.ChainPawn[0][i] * (((float)iChainMG) / 10.0f));
			}
		}
		else if(cmd.indexOf("weakMG") != -1)	{
			int index = cmd.indexOf("weakMG");
			cmd = cmd.substring(index+6);
			cmd = cmd.trim();
			int iWeakMG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.WeakPawn[0][i] = (int)((float)Evaluation2.WeakPawn[0][i] * (((float)iWeakMG) / 10.0f));
			}
		}
		else if(cmd.indexOf("weakEG") != -1)	{
			int index = cmd.indexOf("weakEG");
			cmd = cmd.substring(index+6);
			cmd = cmd.trim();
			int iWeakEG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.WeakPawn[1][i] = (int)((float)Evaluation2.WeakPawn[1][i] * (((float)iWeakEG) / 10.0f));
			}
		}
		else if(cmd.indexOf("doubledMG") != -1)	{
			int index = cmd.indexOf("doubledMG");
			cmd = cmd.substring(index+9);
			cmd = cmd.trim();
			int iDoubledMG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.DoubledPawn[0][i] = (int)((float)Evaluation2.DoubledPawn[0][i] * (((float)iDoubledMG) / 10.0f));
			}
		}
		else if(cmd.indexOf("doubledEG") != -1)	{
			int index = cmd.indexOf("doubledEG");
			cmd = cmd.substring(index+9);
			cmd = cmd.trim();
			int iDoubledEG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.DoubledPawn[1][i] = (int)((float)Evaluation2.DoubledPawn[1][i] * (((float)iDoubledEG) / 10.0f));
			}
		}
		else if(cmd.indexOf("bishopMobilityMG") != -1)	{
			int index = cmd.indexOf("bishopMobilityMG");
			cmd = cmd.substring(index+16);
			cmd = cmd.trim();
			int iDoubledEG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<16; i++)
			{
				Evaluation2.BISHOP_MOBILITY[0][i] = (int)((float)Evaluation2.BISHOP_MOBILITY[0][i] * (((float)iDoubledEG) / 100.0f));
			}
		}
		else if(cmd.indexOf("bishopMobilityEG") != -1)	{
			int index = cmd.indexOf("bishopMobilityEG");
			cmd = cmd.substring(index+16);
			cmd = cmd.trim();
			int iDoubledEG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<16; i++)
			{
				Evaluation2.BISHOP_MOBILITY[1][i] = (int)((float)Evaluation2.BISHOP_MOBILITY[1][i] * (((float)iDoubledEG) / 100.0f));
			}
		}
		else if(cmd.indexOf("knightMobilityMG") != -1)	{
			int index = cmd.indexOf("knightMobilityMG");
			cmd = cmd.substring(index+16);
			cmd = cmd.trim();
			int iDoubledEG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.KNIGHT_MOBILITY[0][i] = (int)((float)Evaluation2.KNIGHT_MOBILITY[0][i] * (((float)iDoubledEG) / 100.0f));
			}
		}
		else if(cmd.indexOf("knightMobilityEG") != -1)	{
			int index = cmd.indexOf("knightMobilityEG");
			cmd = cmd.substring(index+16);
			cmd = cmd.trim();
			int iDoubledEG = Integer.parseInt(cmd.substring(0));
			for(int i=0; i<8; i++)
			{
				Evaluation2.KNIGHT_MOBILITY[1][i] = (int)((float)Evaluation2.KNIGHT_MOBILITY[1][i] * (((float)iDoubledEG) / 100.0f));
			}
		}
		else
		{
			System.out.println("info string command not recognized" + 6/0);
		}
	}
}