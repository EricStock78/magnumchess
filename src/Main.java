/**
 * Main.java
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

import java.io.*;

/*
 * Main.java
 * Execution begins in this class as this class contains the main method
 * After initializing the engine, the engine waits for the user to input the "uci" command
 * Once this command is given, MagnumChess will go into uci mode where it only responds to uci commands 
 *
 *The debug GUI can also be started by typing launch
 * 
 *
 * @version 	2.00 30 Jan 2010
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
            eval = new Evaluation2();
            moveFunctions = new MoveFunctions();
            Board = Board.getInstance();
            writer = new HistoryWriter();
            Board.newGame();
            reader = new BufferedReader(new InputStreamReader(System.in));
            theSearch = new Engine();
            //HistoryWriter.setAlgebraicNotes();
            printGreeting();
            getCmd();
        } catch(Exception ex) {
			System.out.print("info string ");
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
		System.out.println("*****************version 2.00***************");
		System.out.println("to play in UCI mode type \"uci\"");
		//System.out.println("to launch GUI type \"launch\"");
		
	}
	/*
     * method uci
     * 
     * enters a while loop and processes input from the user
     * 
     */ 
    public static void uci() throws IOException{
		int movetime;
		int searchDepth;
		int wtime=0;
		int btime=0;
		int winc=0;
		int binc=0;
		int togo = 0;
        
		boolean infinite = false;				//infinite time controls
		System.out.println("id name Magnum");
		System.out.println("id author Eric Stock");
		
		System.out.println("option name Hash type spin default 8 min 8 max 512");
        System.out.println("option name Evaluation Table type spin default 4 min 1 max 64");
        System.out.println("option name Pawn Table type spin default 4 min 1 max 64");
        
		System.out.println("uciok");
		while(true) {
			cmd = reader.readLine();
			if(cmd.startsWith("quit"))
                System.exit(0);

			if ("isready".equals( cmd ))
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
						Board.undoAll();
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
			if(cmd.startsWith("setoption")) {
				int index = cmd.indexOf("Hash");
				if(index != -1)  {
					index = cmd.indexOf("value");
					cmd = cmd.substring(index+5);
					cmd = cmd.trim();
					int hashSize = Integer.parseInt(cmd.substring(0));
					Global.HASHSIZE = hashSize*32768;
					Engine.resetHash();
					System.out.println("info string hashsize is "+hashSize);
				} else if(cmd.indexOf("Evaluation Table")!= -1) {
                    index = cmd.indexOf("value");
                    cmd = cmd.substring(index+5);
					cmd = cmd.trim();
					int evalSize = Integer.parseInt(cmd.substring(0));
                    Global.EvalHASHSIZE = evalSize * 87381;
                    Evaluation2.reSizeEvalHash();
                    System.out.println("info string evalHash is "+evalSize);
                } else if(cmd.indexOf("Pawn Table") != -1) {
                    index = cmd.indexOf("value");
                    cmd = cmd.substring(index+5);
					cmd = cmd.trim();
					int evalSize = Integer.parseInt(cmd.substring(0));
                    Global.EvalHASHSIZE = evalSize * 43690;
                    Evaluation2.reSizeEvalHash();
                    System.out.println("info string pawnHash is "+evalSize);
                }  else {
                    System.out.println("info string command not recognized");
                }
			}
			
			if(cmd.startsWith("go")) {
				movetime = 0;
				searchDepth = 0;
				if(cmd.indexOf("depth")!=-1) {
					try
					{
						int index = cmd.indexOf("depth");
						cmd = cmd.substring(index+5);
						cmd = cmd.trim();
						searchDepth = Integer.parseInt(cmd.substring(0));
						movetime = 9999999;
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
						searchDepth = 49;
					}
					catch(NumberFormatException ex) {}
				}
				else if(cmd.indexOf("infinite")!=-1) {
					infinite = true;	
					searchDepth = 49;
					movetime = 1000;
					
				}	
				else {				//extract the clock times and increments
					try {
						searchDepth = 49;
						
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
						if(Board.getTurn()==1)			//black moving
							movetime = Math.max(0,(btime/togo + binc)-200);
						else	
							movetime = Math.max(0,(wtime/togo + winc)-200);
					}
					catch(NumberFormatException ex) {
                        ex.printStackTrace(System.err);
                    }
				}
				String move = theSearch.search(movetime,searchDepth,infinite);
				System.out.println("bestmove "+move);
			}	
			if(cmd.equals("ucinewgame")) {
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
            if(cmd.startsWith("quit"))
                System.exit(0);
		}		
	}		
}