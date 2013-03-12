package magnumchess;

/**
 * HistoryWriter.java
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

import magnumchess.Engine;
import magnumchess.Board;
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
    private static Board chessBoard;

	 private static Engine search;
    
    /** use an array list to store all made moves */
	private static ArrayList<String> history;

	/*
     * Constructor HistoryWriter
     * 
     * Initializes some arrays and calls setAlgebraicNotes method
     * 
     */
	public HistoryWriter(Engine pSearch) {			//constructor
		chessBoard = Board.getInstance();
		boardString = new String[64];
		pieceSt = new String[12];
		setAlgebraicNotes();		
		history = new ArrayList<String>(100);
		search = pSearch;
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
		moves = removeFrontWhiteSpace(moves);
		int type = 0;
		
		//calculate total number of moves
		String tempMoves = moves;
		int movesProcessed = 0;
		
		//prune all moves which we already have from the moves list
		int numberBoardMoves = Board.getInstance().getCount();
		
		while(tempMoves.length() >= 4)	{
			
			if(tempMoves.indexOf(" ") == -1)	//last move to process
				move = tempMoves;
			else
				move = tempMoves.substring(0, tempMoves.indexOf(" "));
			
			if(move.length() == 4) 
			{
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
				
				if(movesProcessed < numberBoardMoves)
				{
					int boardMove = Board.getInstance().GetBoardMove(movesProcessed);
					if(to != MoveFunctions.getTo(boardMove) || from != MoveFunctions.getFrom(boardMove))
					{
						if(tempMoves.indexOf(" ")== -1 )
						{
							tempMoves = "";
							break;
						}
						else
						{
							tempMoves = tempMoves.substring(tempMoves.indexOf(" "));
							tempMoves = removeFrontWhiteSpace(tempMoves);
						}
						break;
					}
				}
				else
				{
					break;
				}
				movesProcessed++;
			}
			else if(move.length() == 5)
			{
				//move = moves.substring(0,5);
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
				
				if(m3.equals("q")) 
				  type = Global.PROMO_Q;
				else if(m3.equals("n"))
				  type = Global.PROMO_N;
				else if(m3.equals("b"))
				  type = Global.PROMO_B;
				else if(m3.equals("r"))
				  type = Global.PROMO_R;

				if(movesProcessed < numberBoardMoves)
				{
					int boardMove = Board.getInstance().GetBoardMove(movesProcessed);
					if(to != MoveFunctions.getTo(boardMove) || from != MoveFunctions.getFrom(boardMove) || type != MoveFunctions.moveType(boardMove))
					{
						if(tempMoves.indexOf(" ")== -1 )
						{
							tempMoves = "";
							break;
						}
						else
						{
							tempMoves = tempMoves.substring(tempMoves.indexOf(" "));
							tempMoves = removeFrontWhiteSpace(tempMoves);
						}
					}
				}
				else
				{
					break;
				}
				movesProcessed++;
			}

			
			if(tempMoves.indexOf(" ")== -1 )
			{
				tempMoves = "";
				break;
			}
			else
			{
				tempMoves = tempMoves.substring(tempMoves.indexOf(" "));
				tempMoves = removeFrontWhiteSpace(tempMoves);
			}
		}

		if(movesProcessed < numberBoardMoves)
		{
			//if we are accepting less moves than we have played, we must re-made all of them from scratch
			chessBoard.undoAll();
			acceptMoves(moves);
			return;
		}
		chessBoard.ResetMovesDepth();

		while(tempMoves.length() >= 4)	
		{	
			if(tempMoves.indexOf(" ") == -1)	//last move to process
				move = tempMoves;
			else
				move = tempMoves.substring(0, tempMoves.indexOf(" "));

			int moveArr[] = new int[128];

			int numberOfMoves = search.GetAllMoves(chessBoard.getTurn(), moveArr);

			if(move.length() == 4)
			{
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

				for(int i=0; i < numberOfMoves; i++)
				{
					int generatedTo = MoveFunctions.getTo( moveArr[i] );
					int generatedFrom = MoveFunctions.getFrom( moveArr[i] );

					if( generatedTo == to && generatedFrom == from)
					{
						chessBoard.AddMove( moveArr[i] );
						chessBoard.MakeMove( moveArr[i], false);
						chessBoard.AddRepetitionRoot();
						break;
					}
				}
			}
			else if(move.length() == 5)
			{
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
				if(m3.equals("q"))
				  type = Global.PROMO_Q;
				else if(m3.equals("n"))
				  type = Global.PROMO_N;
				else if(m3.equals("b"))
				  type = Global.PROMO_B;
				else if(m3.equals("r"))
				  type = Global.PROMO_R;

				for(int i=0; i < numberOfMoves; i++)
				{
					int generatedTo = MoveFunctions.getTo( moveArr[i] );
					int generatedFrom = MoveFunctions.getFrom( moveArr[i] );
					int generatedType = MoveFunctions.moveType( moveArr[i] );

					if( generatedTo == to && generatedFrom == from && generatedType == type)
					{
						chessBoard.AddMove( moveArr[i] );
						chessBoard.MakeMove( moveArr[i], false);
						chessBoard.AddRepetitionRoot();
						break;
					}
					else if( generatedTo == to && generatedFrom == from && type >= Global.PROMO_R )
					{
						int mv = MoveFunctions.makeMove(to, from, 5, chessBoard.piece_in_square[to], type);
						chessBoard.AddMove( mv );
						chessBoard.MakeMove( mv, false);
						chessBoard.AddRepetitionRoot();
						break;

					}
				}

			}


			//numberOfMoves = GetAllMoves(chessBoard.getTurn(), moveArr);

			/*chessBoard.AddMove(mv);
			chessBoard.MakeMove(mv, false);
			chessBoard.AddRepetitionRoot();
			*/
			if(tempMoves.indexOf(" ")==-1) break;
			tempMoves = tempMoves.substring(tempMoves.indexOf(" "));
			tempMoves = removeFrontWhiteSpace(tempMoves);
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
            chessBoard.newGame();
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
			if(chessBoard.getTurn()==1) {				//black moving
				if(move1.lastIndexOf(79)>3)	{
					int mv = MoveFunctions.makeMove(58,60);
					chessBoard.AddMove(mv);
               chessBoard.MakeMove(mv, true);
				}else{
					int mv = MoveFunctions.makeMove(62,60);
					chessBoard.AddMove(mv);
               chessBoard.MakeMove(mv, true);
				}
			}else {
				if(move1.lastIndexOf(79)>3){
					int mv = MoveFunctions.makeMove(2,4);
					chessBoard.AddMove(mv);
               chessBoard.MakeMove(mv, true);
				}else{
					int mv = MoveFunctions.makeMove(6,4);
					chessBoard.AddMove(mv);
               chessBoard.MakeMove(mv, true);
				}
			}	
		}	
		else {
			if(chessBoard.getTurn()==Global.COLOUR_WHITE)
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
			piece_in_square = chessBoard.getPiecesInSquare();
			if((int)move1.charAt(0)>96&&piece_in_square[to]==-1)		{	//pawn move
				file = to%8;
				pieces&=Global.fileMasks[file];
				if(chessBoard.getTurn()==1)	{		//black moving
					bit = pieces&-pieces;
				}
				else {
					while(pieces!=0) {
						bit = pieces&(-pieces);
						pieces^=bit;
					}
				}	
				from = chessBoard.getPos(bit);
				int mv = MoveFunctions.makeMove(to,from);
				chessBoard.AddMove(mv);
            chessBoard.MakeMove(mv, true);

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
				attackers = chessBoard.getAttack2(to);
				attackers&=pieces;
				attackers = attackers &-attackers;
				from = chessBoard.getPos(attackers);
				int mv = MoveFunctions.makeMove(to,from);
				chessBoard.AddMove(mv);
            chessBoard.MakeMove(mv, true);
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
					temp = chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_BISHOP];
				else
					temp = chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_BISHOP];
					break;
			case(75):			//king
				if(side)
					temp = chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_KING];
				else
					temp = chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_KING];
					break;
			case(78):			//knight
				if(side)
					temp = chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_KNIGHT];
				else
					temp = chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_KNIGHT];
					break;
			case(82):			//rook
				if(side)
					temp = chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_ROOK];
				else
					temp = chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_ROOK];
					break;
			case(81):			//queen
				if(side)
					temp = chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_QUEEN];
				else
					temp = chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_QUEEN];
					break;
			default:				//pawn move
				if(side)
					temp = chessBoard.pieceBits[Global.COLOUR_WHITE][Global.PIECE_PAWN];
				else
					temp = chessBoard.pieceBits[Global.COLOUR_BLACK][Global.PIECE_PAWN];
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
		if(chessBoard.piece_in_square[from]==4 && Math.abs(to-from)==2) {		//white castle move
			if(to>from)				// kingside caslte
				moveSt = moveSt.concat("O-O");
			else					//queen side
				moveSt = moveSt.concat("O-O-O");
		}	
		else if(chessBoard.piece_in_square[from]==10 && Math.abs(to-from)==2) {		//black castle move
			if(to>from)				// kingside caslte
				moveSt = moveSt.concat("O-O");
			else					//queen side
				moveSt = moveSt.concat("O-O-O");	
		}
		else {	
			moveSt = moveSt.concat(pieceSt[chessBoard.piece_in_square[from]]);
			int pType = chessBoard.piece_in_square[from]%6;
				if(pType==3||pType==1||pType==0) {			//if queen,knight or rook, could have ambiguity
					rank = from/8;
					file = from%8;
					samePieces = getSame(chessBoard.piece_in_square[from]);
					attackers = chessBoard.getAttack2(to);
					samePieces &= attackers;
					samePieces ^= (long)1<<from;
					sameFile = false;
					sameRank = false;
					if(samePieces != 0) {		//we have ambiguity
						while(samePieces!=0) {	
							long temp = samePieces&-samePieces;		//get right most bit (an attacker of same piece type)
							int pos = chessBoard.getPos(temp);
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
			if(chessBoard.piece_in_square[to]!=-1)      {				//capture move
				if(chessBoard.piece_in_square[from]%6==5)				//pawn is moving
					moveSt = moveSt.concat(boardString[from].substring(0,1));
				moveSt = moveSt.concat("x");
			}
			else if(chessBoard.piece_in_square[from] == 5 && Board.getInstance().getPassant(Global.COLOUR_BLACK) == to)
			{
				moveSt = moveSt.concat(boardString[from].substring(0,1));
				moveSt = moveSt.concat("x");
			}
			else if(chessBoard.piece_in_square[from] == 11 && Board.getInstance().getPassant(Global.COLOUR_WHITE) == to)
			{
				moveSt = moveSt.concat(boardString[from].substring(0,1));
				moveSt = moveSt.concat("x");
			}
			moveSt = moveSt.concat(boardString[to]);
			if(chessBoard.piece_in_square[from]%6==5) {			//pawn moving
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
			case 1:				//white knight
			case 3:
			case 6:				//white rook
			case 7:				//white knight
			case 9:
				pieces = chessBoard.pieceBits[pType/6][pType%6];
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