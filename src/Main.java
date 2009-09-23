import java.io.*;
public class Main
{
	public static BufferedReader reader;
	public static String cmd;
	public static Engine theSearch;
	public static Board Magnum;
	public static HistoryWriter writer;
	public static void main(String args[]) throws IOException 
	{	
		try {
		
		Magnum = new Board();
		Magnum.newGame();
		reader = new BufferedReader(new InputStreamReader(System.in));
		theSearch = new Engine(Magnum);
		HistoryWriter.setAlgebraicNotes();
		printGreeting();
		getCmd();
		} catch(Exception ex) {
			System.out.print("info string ");
			
			//throw(ex);
			System.out.println(ex);		
			ex.printStackTrace();
			//System.out.println(ex.);
			//System.out.println(ex.getStackTrace().toString());
		}
	}
	public static void printGreeting() {
		System.out.println("*****************MAGNUM CHESS***************");
		System.out.println("*****************version 1.00***************");
		System.out.println("to play in UCI mode type \"uci\"");
		System.out.println("to launch GUI type \"launch\"");
		
	}
	public static void uci() throws IOException{
		int movetime;
		int searchDepth;
		int wtime=0;
		int btime=0;
		int winc=0;
		int binc=0;
		
		boolean clock = false;					//playing using time controls
		boolean infinite = false;				//infinite time controls
		System.out.println("id name Magnum");
		System.out.println("id author Eric Stock");
		
		System.out.println("option name Hash type spin default 8 min 8 max 512");
		System.out.println("uciok");
		while(true) {
			cmd = reader.readLine();
			
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
						Magnum.undoAll();
						HistoryWriter.acceptMoves(moves);
					}
				} else {			//reading in a fen string
					int mstart = cmd.indexOf("moves");
					if(mstart> -1) {
                                                //String moves = cmd.substring(mstart+5);
						//Magnum.undoAll();
						//HistoryWriter.acceptMoves(moves);
                                            //System.out.println("Here1");
						Magnum.undoAll();
                        String fen = cmd.substring(cmd.indexOf("fen")+4,mstart-1);
						Board.acceptFen(fen);
                        String moves = cmd.substring(mstart+5);
						
						HistoryWriter.acceptMoves(moves);
                                                //HistoryWriter.acceptFen(fen);
					} else {
						//System.out.println("Here 2");
						String fen = cmd.substring(cmd.indexOf("fen")+4);
						Board.acceptFen(fen);
                                                //HistoryWriter.acceptFen(fen);
					}
					
				}
			}	
			if(cmd.startsWith("setoption")) {
				int index = cmd.indexOf("Hash");
				if(index == -1)
					System.out.println("info string option not recognized");
				else {
					
					index = cmd.indexOf("value");
					cmd = cmd.substring(index+5);
					cmd = cmd.trim();
					int hashSize = Integer.parseInt(cmd.substring(0));
					Global.HASHSIZE = hashSize*32768;
					theSearch.resetHash();
					System.out.println("info string hashsize is "+hashSize);
				}	
				//int index = cmd.indexOf("name");
				//cmd = cmd.substring(index+4);
				//cmd = cmd.trim();
					
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
						
						int index = cmd.indexOf("wtime");
						String temp = cmd.substring(index+5);
						temp = temp.trim();
						wtime = Integer.parseInt(temp.substring(0,temp.indexOf(" ")));
						
						index = cmd.indexOf("btime");
						temp = cmd.substring(index+5);
						temp = temp.trim();
						btime = Integer.parseInt(temp.substring(0,temp.indexOf(" ")));
						
						index = cmd.indexOf("winc");
						temp = cmd.substring(index+4);
						temp = temp.trim();
						winc = Integer.parseInt(temp.substring(0,temp.indexOf(" ")));
						
						index = cmd.indexOf("binc");
						temp = cmd.substring(index+4);
						temp = temp.trim();
						if(temp.indexOf(" ")!=-1)
							binc = Integer.parseInt(temp.substring(0,temp.indexOf(" ")));
						else	
							binc = Integer.parseInt(temp.substring(0));	
						
						
						if(Magnum.getTurn()==1)			//black moving
							movetime = btime/40+binc;
						else	
							movetime = wtime/40+winc;
					}
					catch(NumberFormatException ex) {}
				
				}
				
				String move = theSearch.search(movetime,searchDepth,infinite);
				
				System.out.println("bestmove "+move);
			}	
			if(cmd.equals("ucinewgame")) {
				Magnum.newGame();
				theSearch.resetHash();
				
			}
			
		
		}		
	}	
	
	public static void getCmd() throws IOException{
		
		while(true) {
			cmd = reader.readLine();
			if(cmd.equals("uci")) {
				System.out.println("plus 7 is "+Global.plus7[4]);
				uci();
				break;
			}
			if(cmd.equals("launch")) {
				System.out.println("plus 7 is "+Global.plus7[4]);
				GUI board = new GUI();	
				break;
			}
		}		
	}		


}