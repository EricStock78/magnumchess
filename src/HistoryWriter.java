import java.io.*;
import javax.swing.JFileChooser;
import java.util.ArrayList;
public final class HistoryWriter {
	private static String[] boardString;			//algrebraic notation names of board positions
	private static String[] pieceSt;			//abreviated names of pieces
	private static Board Magnum;
	private static ArrayList<String> history;
	//private static ArrayList 
	private static ArrayList loadedGame;
	
	
	public HistoryWriter(Board m) {			//constructor
		Magnum = m;
		boardString = new String[64];
		pieceSt = new String[12];
		setAlgebraicNotes();		
		history = new ArrayList<String>(100);
		loadedGame = new ArrayList();
		//System.out.println("removed is "+removeChar("R3=Q","="));
	
	}
	public void writeHistory() {
		GUI.removeText2();
		for(int i=0;i<history.size();i++) {
			GUI.printText2(history.get(i)+" ");
		}	
        }
	
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
			Magnum.makeMove(mv,true,true);
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
			int mv = MoveFunctions.makeMove(to,from,piece,cp,type,0);
			Magnum.makeMove(mv,true,true);
			if(moves.indexOf(" ")==-1) break;
			moves = moves.substring(moves.indexOf(" "));
			moves = removeFrontWhiteSpace(moves);
		}	
	
	}	
		
		
	
	}
	public static String getUCIMove(int to,int from,int piece) {
		String st = "";
		st = st.concat(boardString[from]);
		st = st.concat(boardString[to]);
		if(piece%6==5 && (to/8==0 || to/8==7))
			st = st.concat("q");
		return st;
		
		
	}
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


	public void reset() {
		history.clear();
	}	
	public static String getLastMove() {
		return history.get(history.size()-1);
		
	}
	public void removeLastHistory() {
		history.remove(history.size()-1);	
		//writeHistory();
	}	
	public void historyToFile(File f) {
		File gameFile;
		FileWriter fileWriter;
		BufferedWriter writer;
		JFileChooser chooser; 
		try {
			gameFile = f;
			fileWriter = new FileWriter(gameFile);
			writer = new BufferedWriter(fileWriter);
		writer.write("[Event \"\"]\n[Site \"\"]\n[Date \"\"]\n[Round \"\"]\n[White \"\"]\n[Black \"\"]\n[TimeControl \"\"]\n[Result \"\"]\n");
		for(int i=0;i<history.size();i++) {
			writer.write(history.get(i));	
				
		}
		writer.close();
	} catch(Exception ex) {};
}	
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
			while((line= reader.readLine()) != null&& line.indexOf("]")!=-1||isEmptyLine(line));			//skip game info
			processLine(line);
			while((line=reader.readLine()) !=null&&!isEmptyLine(line))  {
				processLine(line);
			}	
			
			reader.close();	
		}
		catch(Exception ex) {
		//ex.printStackTrace(System.out);
		System.out.println("File Initialization Failed");
		Magnum.newGame();};
	
	}	
	public boolean isEmptyLine(String st) {
		int index;
		index = 0;
		for(int i=0;i<st.length();i++) {	
			if(st.charAt(i)>=33&&st.charAt(i)<=122)
				index=i;
		}
		if(index==0) return true;
		else return false;
	}		
	
	public static String removeFrontWhiteSpace(String st) {
		int index;
		index = 0;
		for(int i=0;i<st.length();i++) {
			index=i;
			if(st.charAt(i)>=33&&st.charAt(i)<=122) break;
		}
		return st.substring(index);
	}
	
	public String removeEndWhiteSpace(String st) {
		int index;
		index = 0;
		for(int i=0;i<st.length();i++) {
			if(st.charAt(i)>=33&&st.charAt(i)<=122)
				index = i;
		}
		return st.substring(0,index+1);	
	}
	public String removeChar(String st, String s) {
		int index;
		String temp;
		String temp2;
		index = -1;
		temp = "";
		temp2 = "";
		for(int i=0;i<st.length();i++) {
			if(st.charAt(i)==s.charAt(0))
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
	public void processLine(String st) {
		String move1;
		String move2;
		String toSt;			//string rep of board position being moved to
		int start;
		int from;
		int end;
		int temp;
		int to;					//index of move to
		int pos;
		int[] piece_in_square;
		long attackers;
		to = -1;
		from = -1;
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
	public void processMove(String move1) 	{
		String toSt;			//string rep of board position being moved to
		int start;
		int from;
		int end;
		int temp;
		int to;					//index of move to
		int pos;
		int[] piece_in_square;
		long attackers;
		long pieces;
		long bit;
		int rank;
		int file;
		boolean fileFlag;
		boolean rankFlag;
		
		to = -1;
		from = -1;
		bit = 0;
		//System.out.println("move is "+move1);
		move1 = removeChar(move1,"+");
		move1 = removeChar(move1,"=");
		if((int)move1.charAt(0)==79) {
			if(Magnum.getTurn()==1) {				//black moving
				if(move1.lastIndexOf(79)>3)	{
					int mv = MoveFunctions.makeMove(58,60);
					Magnum.makeMove(mv,true,true);
				}else{
					int mv = MoveFunctions.makeMove(62,60);
					Magnum.makeMove(mv,true,true);
				}
			}else {
				if(move1.lastIndexOf(79)>3){
					int mv = MoveFunctions.makeMove(2,4);
					Magnum.makeMove(mv,true,true);
				}else{
					int mv = MoveFunctions.makeMove(6,4);
					Magnum.makeMove(mv,true,true);	
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
			//System.out.println("to st is "+toSt);
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
				Magnum.makeMove(mv,true,true);

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
				Magnum.makeMove(mv,true,true);
			}
		}
	}	
	
	public long getPieces(String st, boolean side) {
		int p;
		long temp;
		p = (int)st.charAt(0);
		switch(p) {
			case(66):			//bishop 
				if(side)
					temp = Magnum.getBlackBishops();
				else
					temp = Magnum.getWhiteBishops();
					break;
			case(75):			//king
				if(side)
					temp = Magnum.getBlackKing();
				else
					temp = Magnum.getWhiteKing();
					break;
			case(78):			//knight
				if(side)
					temp = Magnum.getBlackKnights();
				else
					temp = Magnum.getWhiteKnights();
					break;
			case(82):			//rook
				if(side)
					temp = Magnum.getBlackRooks();
				else
					temp = Magnum.getWhiteRooks();
					break;
			case(81):			//queen
				if(side)
					temp = Magnum.getBlackQueen();
				else
					temp = Magnum.getWhiteQueen();
					break;
			default:				//pawn move
				//if(p!=79) {
				if(side)
					temp = Magnum.getBlackPawns();
				else
					temp = Magnum.getWhitePawns();
					break;
				//}
	}
	return temp;	
		
}	
		
	public void addHistory(int to,int from,int[] p,int c) {
		int count;
		int[] piece_in_square;
		String moveSt;
		
		long samePieces;			//long representing same type of pieces as the piece capturing
		long attackers;				//pieces attacking to square
		int rank;
		int file;
		boolean sameFile;
		boolean sameRank;
		
		count = c;
		piece_in_square = p;
		moveSt = "";
		//if(count%2==0)
		//	moveSt = moveSt.concat(Integer.toString(1+count/2)+". ");
		if(piece_in_square[from]==4 && Math.abs(to-from)==2) {		//white castle move
			if(to>from)				// kingside caslte
				moveSt = moveSt.concat("O-O");
			else					//queen side
				moveSt = moveSt.concat("O-O-O");
		}	
		else if(piece_in_square[from]==10 && Math.abs(to-from)==2) {		//black castle move
			if(to>from)				// kingside caslte
				moveSt = moveSt.concat("O-O");
			else					//queen side
				moveSt = moveSt.concat("O-O-O");	
		}
		else {	
			moveSt = moveSt.concat(pieceSt[piece_in_square[from]]);
			int pType = piece_in_square[from]%6;
				if(pType==3||pType==1||pType==0) {			//if queen,knight or rook, could have ambiguity
					rank = from/8;
					file = from%8;
					samePieces = getSame(piece_in_square[from]);
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
			if(piece_in_square[to]!=-1)      {				//capture move
				if(piece_in_square[from]%6==5)				//pawn is moving
					moveSt = moveSt.concat(boardString[from].substring(0,1));
				//else 	
				moveSt = moveSt.concat("x");
			}
			moveSt = moveSt.concat(boardString[to]);
			if(piece_in_square[from]%6==5) {			//pawn moving
				if(to/8==0||to/8==7)					//promotion
					moveSt = moveSt.concat("Q");
			}
		}
		moveSt = moveSt.concat(" ");
		if(count%2==1)
			moveSt = moveSt.concat("\n");
		//GUI.printText2(moveSt);
		history.add(moveSt);
	}
	
	
	
	public long getSame(int pType) {
		long pieces;
		switch(pType) {
			case 0:				//white rook
				pieces = Magnum.getWhiteRooks();
				break;
			case 1:				//white knight
				pieces = Magnum.getWhiteKnights();
				break;
			case 3:
				pieces = Magnum.getWhiteQueen();
				break;
			case 6:				//white rook
				pieces = Magnum.getBlackRooks();
				break;
			case 7:				//white knight
				pieces = Magnum.getBlackKnights();
				break;
			case 9:
				pieces = Magnum.getBlackQueen();
				break;
			default:
				//this shouldn't happen
				pieces = 0;
		}
		return pieces;
	}

    //method converts a 2 character string representing an algebraic chess position
    //to an integer representing a square

    public static int getNumericPosition(String st) {

        for(int i=0; i<64; i++) {
            if(st.equals(boardString[i]))
                return i;
        }
        return -1;
    }

	public static void setAlgebraicNotes() {
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
