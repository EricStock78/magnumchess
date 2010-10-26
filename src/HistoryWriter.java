/**
 * HistoryWriter.java
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

import java.io.*;
import java.util.ArrayList;

/*
 * HistoryWriter.java
 * 
 * To-do 
 * The code in this class is pretty messy.  There also may be extra functionality in this class related to the old Gui, which can now be removed
 * as the gui is no longer supported
 *
 *
 * This class handles the follwoing:
 * -conversion of chess formats (pgn,fen, etc)
 * -saving and loading games from file
 * 
 */

public final class HistoryWriter {
	
    /** algebraic notation names 64 board positions */
    private static String[] boardString;			
    
    /** abreviated names of pieces used for pgn notation */
	private static String[] pieceSt;			
	
    /** instance of singleton Board class */
    private static Board Magnum;
    
    /** use an array list to store all made moves */
	private static ArrayList<String> history;
	
	/*
     * Constructor HistoryWriter
     * 
     * Initializes some arrays and calls setAlgebraicNotes method
     * 
     */
	public HistoryWriter() {			//constructor
		Magnum = Board.getInstance();
		boardString = new String[64];
		pieceSt = new String[12];
		setAlgebraicNotes();		
		history = new ArrayList<String>(100);
	}
	
    
    /*
     * method acceptMoves
     * 
     * parses moves out of a string of moves in algebraic notation
     * makes these moves over the board
     * 
     * @param String moves - the moves to be parsed and made
     * 
     */ 
	public static void acceptMoves(String moves) {
		String move;
		int from=0;
		int to=0;
		int[] piece_in_square = Magnum.getPiecesInSquare();
		moves = removeFrontWhiteSpace(moves);
		int type = 0;
		
		while(moves.length()>=4)	{
			if(moves.indexOf(" ") == -1)	//last move to process
				move = moves;
			else
				move = moves.substring(0,moves.indexOf(" "));
			
			if(move.length() == 4) {
			
                move = moves.substring(0,4);
                String m1 = move.substring(0,2);
                String m2 = move.substring(2,4);
                for(int i=0;i<64;i++) {
                    if(boardString[i].equals(m1)) {
                        from = i;
                    }
                }	
                for(int i=0;i<64;i++) {
                    if(boardString[i].equals(m2))
                    to = i;
                }	
                int mv = MoveFunctions.makeMove(to,from);
                Magnum.AddMove(mv);
                Magnum.MakeMove(mv, false);
                Magnum.AddRepetitionRoot();
                if(moves.indexOf(" ")==-1) break;
                moves = moves.substring(moves.indexOf(" "));
                moves = removeFrontWhiteSpace(moves);
		
            } else if(move.length() == 5) {				//promo move
                move = moves.substring(0,5);
                String m1 = move.substring(0,2);
                String m2 = move.substring(2,4);
                String m3 = move.substring(4,5);
                for(int i=0;i<64;i++) {
                    if(boardString[i].equals(m1)) {
                        from = i;
                    }
                }	
                for(int i=0;i<64;i++) {
                    if(boardString[i].equals(m2))
                    to = i;
                }	
                int piece = piece_in_square[from];
                int cp = piece_in_square[to];
                if(m3.equals("q")) 
                    type = Global.PROMO_Q;
                else if(m3.equals("n"))
                    type = Global.PROMO_N;
                else if(m3.equals("b"))
                    type = Global.PROMO_B;
                else if(m3.equals("r"))
                    type = Global.PROMO_R;
                int mv = MoveFunctions.makeMove(to,from,piece,cp,type);
                Magnum.AddMove(mv);
                Magnum.MakeMove(mv, true);
                Magnum.AddRepetitionRoot();
                if(moves.indexOf(" ")==-1) break;
                moves = moves.substring(moves.indexOf(" "));
                moves = removeFrontWhiteSpace(moves);
            }	
        }	
    }
    
    /*
     * method getUCIMove
     * 
     * converts a move represented internally in the engine to algebraic notation 
     * to be used in UCI communication
     * 
     * @param int to - to position of move
     * @param int from - from position of move
     * @param int piece - piece type
     * 
     */ 
	public static String getUCIMove(int to,int from,int piece) {
		String st = "";
		st = st.concat(boardString[from]);
		st = st.concat(boardString[to]);
		if(piece%6==5 && (to/8==0 || to/8==7))
			st = st.concat("q");
		return st;
	}
    
    /*
     * method getUCIMove
     * 
     * converts a move represented internally in the engine to algebraic notation 
     * to be used in UCI communication
     * 
     * @param int move - move info packed into 32 bit int
     *
     */  
    public static String getUCIMove(int move) {
		int to = MoveFunctions.getTo(move);
        int from = MoveFunctions.getFrom(move);
        int piece = MoveFunctions.getPiece(move);

        String st = "";
		st = st.concat(boardString[from]);
		st = st.concat(boardString[to]);
		if(piece%6==5 && (to/8==0 || to/8==7)) {
			int type = MoveFunctions.moveType(move);
            if(type == Global.PROMO_Q)
               st = st.concat("q");
            else if(type == Global.PROMO_B)
               st = st.concat("b");
            else if(type == Global.PROMO_N)
               st = st.concat("n");
            else if(type == Global.PROMO_R)
               st = st.concat("r");
        }
        return st;
	}

    /*
     * method reset()
     * 
     * clears the array list containing all moves
     *
     */
	public void reset() {
		history.clear();
	}	
	
    /*
     * method getLastMove
     * 
     * returns last move made over the board in algebraic notation
     * 
     * @return String - the last move made
     *
     */
    public static String getLastMove() {
		return history.get(history.size()-1);
		
	}
	
    /*
     * method removeLastHistory
     * 
     * removes last move made over the board in algebraic notation
     *
     */
    public void removeLastHistory() {
		history.remove(history.size()-1);	
	}	
	
    /*
     * method historyToFile
     * 
     * writes a .pgn file containing a chess game
     * 
     * @param File f - the file to write to
     *
     */
    public void historyToFile(File f) {
		File gameFile;
		FileWriter fileWriter;
		BufferedWriter writer;
		try {
			gameFile = f;
			fileWriter = new FileWriter(gameFile);
			writer = new BufferedWriter(fileWriter);
            writer.write("[Event \"\"]\n[Site \"\"]\n[Date \"\"]\n[Round \"\"]\n[White \"\"]\n[Black \"\"]\n[TimeControl \"\"]\n[Result \"\"]\n");
            for(int i=0;i<history.size();i++) {
                writer.write(history.get(i));			
            }
            writer.close();
        } catch(Exception ex) {ex.printStackTrace(System.out);}
    }	
	
    /*
     * method historyToFile
     * 
     * reads a .pgn file containing a chess game
     * 
     * @param File f - the file to write to
     *
     */
    public void readHistory(File f) {
		File gameFile;
		FileReader fileReader;
		BufferedReader reader;
		String line;
		
		try {
			gameFile = f;
			fileReader = new FileReader(gameFile);
			reader = new BufferedReader(fileReader);
			line = null;
			while((line= reader.readLine()) != null&& line.indexOf("]")!=-1||isEmptyLine(line))			//skip game info
                processLine(line);
			while((line=reader.readLine()) !=null&&!isEmptyLine(line))  {
				processLine(line);
			}	
			reader.close();	
		}
		catch(Exception ex) {   
            ex.printStackTrace(System.out);
            System.out.println("File Initialization Failed");
            Magnum.newGame();
        }
	}	
	
    /*
     * method isEmptyLine
     * 
     * recognizes empty lines when parsing a .pgn file
     * These lines can then be skipped
     * 
     * @param String st - the line beign processed
     * 
     * @return boolean - is it blank?
     *
     */
    private boolean isEmptyLine(String st) {
		int index;
		index = 0;
		for(int i=0;i<st.length();i++) {	
			if(st.charAt(i)>=33&&st.charAt(i)<=122)
				index=i;
		}
		if(index==0) return true;
		else return false;
	}	
	
    /*
     * method removeFrontWhiteSpace
     * 
     * removes all white space from the start of the String being processed
     * 
     * @param String st - the line beign processed
     * 
     * @return String - the new string with the forward white space removed 
     *
     */
	private static String removeFrontWhiteSpace(String st) {
		int index;
		index = 0;
		for(int i=0;i<st.length();i++) {
			index=i;
			if(st.charAt(i)>=33&&st.charAt(i)<=122) break;
		}
		return st.substring(index);
	}
	
    /*
     * method removeEndWhiteSpace
     * 
     * removes all white space from the end of the String being processed
     * 
     * @param String st - the line beign processed
     * 
     * @return String - the new string with trailing white space removed 
     *
     */
	private String removeEndWhiteSpace(String st) {
		int index;
		index = 0;
		for(int i=0;i<st.length();i++) {
			if(st.charAt(i)>=33&&st.charAt(i)<=122)
				index = i;
		}
		return st.substring(0,index+1);	
	}
	
    /*
     * method removeEndWhiteSpace
     * 
     * removes all white space from the end of the String being processed
     * 
     * @param String st - the line beign processed
     * @param char s = the character to remove
     * 
     * @return String - the new string with the char removed
     *
     */
    public String removeChar(String st, char s) {
		int index;
		String temp;
		String temp2;
		index = -1;
		temp = "";
		temp2 = "";
		for(int i=0; i<st.length(); i++) {
			if(st.charAt(i) == s)
				index = i;
		}
		if(index>-1) {
		temp = st.substring(0,index);
		temp2 = st.substring(index+1,st.length());
		temp = temp.concat(temp2);
		return temp;
		} else
		return st;	
	}	
	
    /*
     * method processLine
     * 
     * removes white space from the line and checks if there is a move left to process
     * 
     * @param String st - the line beng processed
     *
     */
    public void processLine(String st) {
		String move1;
		String move2;
		int start;
		int end;
		start = st.indexOf(".")+1;
		st = st.substring(start);
		st = removeFrontWhiteSpace(st);
		end = st.indexOf(" ");
		move1 = st.substring(0,end);
		processMove(move1);
		start = st.indexOf(" ")+1;
		move2 = st.substring(start);	
		move2 = removeFrontWhiteSpace(move2);
		if(move2.length()>0) {
			move2 = removeEndWhiteSpace(move2);
			processMove(move2);
		}
	}		
	
    /*
     * method processMove
     * 
     * interprets the move in algebraic/pgn form and makes this move
     * 
     * @param String move1 - the move to process
     *
     */
    public void processMove(String move1) 	{
		String toSt;			//string rep of board position being moved to
		int from;
		int to;					//index of move to
		int pos;
		int[] piece_in_square;
		long attackers;
		long pieces;
		long bit;
		int rank;
		int file;
		
		to = -1;
		from = -1;
		bit = 0;
		move1 = removeChar(move1,'+');
		move1 = removeChar(move1,'=');
		if((int)move1.charAt(0)==79) {
			if(Magnum.getTurn()==1) {				//black moving
				if(move1.lastIndexOf(79)>3)	{
					int mv = MoveFunctions.makeMove(58,60);
					Magnum.AddMove(mv);
               Magnum.MakeMove(mv, true);
				}else{
					int mv = MoveFunctions.makeMove(62,60);
					Magnum.AddMove(mv);
               Magnum.MakeMove(mv, true);
				}
			}else {
				if(move1.lastIndexOf(79)>3){
					int mv = MoveFunctions.makeMove(2,4);
					Magnum.AddMove(mv);
               Magnum.MakeMove(mv, true);
				}else{
					int mv = MoveFunctions.makeMove(6,4);
					Magnum.AddMove(mv);
               Magnum.MakeMove(mv, true);
				}
			}	
		}	
		else {
			if(Magnum.getTurn()==-1)
				pieces = getPieces(move1,false);
			else
				pieces = getPieces(move1,true);
			if(move1.indexOf("Q")==move1.length()-1) 						//if promotion move, remove the "Q"
				move1 = move1.substring(0,move1.length()-1);
			toSt = move1.substring(move1.length()-2,move1.length());
			for(int i=0;i<64;i++) {
				if(boardString[i].equals(toSt))
					to = i;
			}
			piece_in_square = Magnum.getPiecesInSquare();
			if((int)move1.charAt(0)>96&&piece_in_square[to]==-1)		{	//pawn move
				file = to%8;
				pieces&=Global.fileMasks[file];
				if(Magnum.getTurn()==1)	{		//black moving
					bit = pieces&-pieces;
				}
				else {
					while(pieces!=0) {
						bit = pieces&(-pieces);
						pieces^=bit;
					}
				}	
				from = Magnum.getPos(bit);
				int mv = MoveFunctions.makeMove(to,from);
				Magnum.AddMove(mv);
            Magnum.MakeMove(mv, true);

			}else {
				int count = 0;
				rank=0;
				file=0;
				for(int i=0;i<move1.length();i++) {
					pos = move1.charAt(i);
					if(pos>=97&&pos<=104)
						file++;
					if(pos>=49&&pos<=56)
						rank++;	
				}			
				if(rank>1) {
					count=0;
					while(true) {
						pos=move1.charAt(count);
						if(pos>=49&&pos<=56) break;
						count++;
					}	
					pieces&=Global.rankMasks[pos-49];
				}
				if(file>1) {
					count = 0;
					while(true) {
						pos=move1.charAt(count);
						if(pos>=97&&pos<=104) break;
						count++;
					}
					pieces&=Global.fileMasks[pos-97];
				}
				attackers = Magnum.getAttack2(to);
				attackers&=pieces;
				attackers = attackers &-attackers;
				from = Magnum.getPos(attackers);
				int mv = MoveFunctions.makeMove(to,from);
				Magnum.AddMove(mv);
            Magnum.MakeMove(mv, true);
			}
		}
	}	
	
    /*
     * method getPieces
     * 
     * returns the bitset containing the piece being moved
     * 
     * @param String st - the move being processed
     * @param boolean side - true - black, false - white
     *
     * @return long - the bitset
     */
	public long getPieces(String st, boolean side) {
		int p;
		long temp;
		p = (int)st.charAt(0);
		switch(p) {
			case(66):			//bishop 
				if(side)
					temp = Magnum.blackbishops;
				else
					temp = Magnum.whitebishops;
					break;
			case(75):			//king
				if(side)
					temp = Magnum.blackking;
				else
					temp = Magnum.whiteking;
					break;
			case(78):			//knight
				if(side)
					temp = Magnum.blackknights;
				else
					temp = Magnum.whiteknights;
					break;
			case(82):			//rook
				if(side)
					temp = Magnum.blackrooks;
				else
					temp = Magnum.whiterooks;
					break;
			case(81):			//queen
				if(side)
					temp = Magnum.blackqueen;
				else
					temp = Magnum.whitequeen;
					break;
			default:				//pawn move
				if(side)
					temp = Magnum.blackpawns;
				else
					temp = Magnum.whitepawns;
					break;
        }
        return temp;	
    }	
	
    /*
     * method addHistory
     * 
     * converts a move to algebraic notation and adds it to the history arraylist
     * 
     * @param int to - position moved to
     * @param int from - position moved from
     * @param int c - full move count of move
     *
     */
	public void addHistory(int to,int from,int c) {
		int count;
		String moveSt;
		
		long samePieces;			//long representing same type of pieces as the piece capturing
		long attackers;				//pieces attacking to square
		int rank;
		int file;
		boolean sameFile;
		boolean sameRank;
		
		count = c;
		moveSt = "";
		if(Magnum.piece_in_square[from]==4 && Math.abs(to-from)==2) {		//white castle move
			if(to>from)				// kingside caslte
				moveSt = moveSt.concat("O-O");
			else					//queen side
				moveSt = moveSt.concat("O-O-O");
		}	
		else if(Magnum.piece_in_square[from]==10 && Math.abs(to-from)==2) {		//black castle move
			if(to>from)				// kingside caslte
				moveSt = moveSt.concat("O-O");
			else					//queen side
				moveSt = moveSt.concat("O-O-O");	
		}
		else {	
			moveSt = moveSt.concat(pieceSt[Magnum.piece_in_square[from]]);
			int pType = Magnum.piece_in_square[from]%6;
				if(pType==3||pType==1||pType==0) {			//if queen,knight or rook, could have ambiguity
					rank = from/8;
					file = from%8;
					samePieces = getSame(Magnum.piece_in_square[from]);
					attackers = Magnum.getAttack2(to);
					samePieces &= attackers;
					samePieces ^= (long)1<<from;
					sameFile = false;
					sameRank = false;
					if(samePieces != 0) {		//we have ambiguity
						while(samePieces!=0) {	
							long temp = samePieces&-samePieces;		//get right most bit (an attacker of same piece type)
							int pos = Magnum.getPos(temp);
							if(pos%8==file)
							sameFile=true;
							if(pos/8==rank)
								sameRank=true;
							samePieces ^= temp;
						}	
						if(sameFile)
							moveSt = moveSt.concat(boardString[from].substring(1,2));
						else if(sameRank)
							moveSt = moveSt.concat(boardString[from].substring(0,1));
						else
							moveSt = moveSt.concat(boardString[from].substring(0,1));
					}
				}	
			if(Magnum.piece_in_square[to]!=-1)      {				//capture move
				if(Magnum.piece_in_square[from]%6==5)				//pawn is moving
					moveSt = moveSt.concat(boardString[from].substring(0,1));
				moveSt = moveSt.concat("x");
			}
			moveSt = moveSt.concat(boardString[to]);
			if(Magnum.piece_in_square[from]%6==5) {			//pawn moving
				if(to/8==0||to/8==7)					//promotion
					moveSt = moveSt.concat("Q");
			}
		}
		moveSt = moveSt.concat(" ");
		if(count%2==1)
			moveSt = moveSt.concat("\n");
		history.add(moveSt);
	}
	
	/*
     * method getSame
     * 
     * returns the bitset containing the specified piece type
     * 
     * @param int pType - type of piece
     * 
     * @return long - the bitset of requested piece type
     *
     */
	private long getSame(int pType) {
		long pieces;
		switch(pType) {
			case 0:				//white rook
				pieces = Magnum.whiterooks;
				break;
			case 1:				//white knight
				pieces = Magnum.whiteknights;
				break;
			case 3:
				pieces = Magnum.whitequeen;
				break;
			case 6:				//white rook
				pieces = Magnum.blackrooks;
				break;
			case 7:				//white knight
				pieces = Magnum.blackknights;
				break;
			case 9:
				pieces = Magnum.blackqueen;
				break;
			default:
				//this shouldn't happen
				pieces = 0;
		}
		return pieces;
	}

    /*
     * method getNumericPosition
     * 
     * converts a 2 character string representing an algebraic chess position
     * to an integer representing a square
     * 
     * @param String st - string representation of square
     * 
     * @return int - the numberic square representation (0 to 64)
     *
     */
    public static int getNumericPosition(String st) {

        for(int i=0; i<64; i++) {
            if(st.equals(boardString[i]))
                return i;
        }
        return -1;
    }

    /*
     * method setAlgebraicNotes
     * 
     * initializes arrays used to convert between internal numeric and algebraic notation
     * 
     */
	private static void setAlgebraicNotes() {
		int file;
		int rank;
		for(int i=0;i<12;i++) {
			switch(i) {
				case(0):	
					pieceSt[i] = "R";
					break;
				case(1):
					pieceSt[i] = "N";
					break;
				case(2):
					pieceSt[i] = "B";
					break;
				case(3):
					pieceSt[i] = "Q";
					break;
				case(4):
					pieceSt[i] = "K";
					break;
				case(5):
					pieceSt[i] = "";
					break;
				case(6):
					pieceSt[i] = "R";
					break;
				case(7):
					pieceSt[i] = "N";
					break;
				case(8):
					pieceSt[i] = "B";
					break;
				case(9):
					pieceSt[i] = "Q";
					break;
				case(10):
					pieceSt[i] = "K";
					break;
				case(11):
					pieceSt[i] = "";
					break;
			
			}	
		}
		
		for(int i=0;i<64;i++) {
			rank = i/8;
			file = i%8;
			switch(file) {
				case(0):
					boardString[i] = "a";
					break;
				case(1):
					boardString[i] = "b";
					break;
				case(2):
					boardString[i] = "c";	
					break;
				case(3):
					boardString[i] = "d";	
					break;
				case(4):
					boardString[i] = "e";
					break;
				case(5):
					boardString[i] = "f";
					break;
				case(6):
					boardString[i] = "g";
					break;
				case(7):
					boardString[i] = "h";
					break;
			}		
			switch(rank) {
				case(0):
					boardString[i]+= "1";
					break;
				case(1):
					boardString[i] += "2";
					break;
				case(2):
					boardString[i] += "3";	
					break;
				case(3):
					boardString[i] += "4";	
					break;
				case(4):
					boardString[i] += "5";
					break;
				case(5):
					boardString[i] += "6";
					break;
				case(6):
					boardString[i] += "7";
					break;
				case(7):
					boardString[i] += "8";
					break;
			}
		}
	}					
}		
