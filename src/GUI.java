
/*  THIS IS THE GUI CLASS */


/******************************************************************
Cosc 3P71 Project
Authored By; 	Eric Stock
							Joseph W. Troop
*******************************************************************/
//import java.io.*;
import java.io.File;
import java.io.IOException;
import java.lang.Integer;
import java.util.BitSet;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;
import java.lang.Integer;
import java.util.Vector;
import java.util.Observable;
/*******************************************************************
	Name: GUI
	Description: This class is responsible for all of the GUI actions
********************************************************************/
public final class GUI implements DragSourceListener, DragGestureListener, java.util.Observer {
	private static Engine theSearch;		//searching function
	private static Board Magnum;			//move generation interface
	private static BitSet legal;						//legal moves for selected piece
	private static BitSet legal2;						//legal moves along secondary axis
	private static Image theImage;
	private static ImageIcon Pieces[]; 
	private static String pNames[];				//names of all pieces
	private static JFrame theFrame;
	private static JPanel thePanel;
	private static Container contentPane;
	private static cPanel Pos[];						//64 positions on board
	private static DragSource dragSource;
	private static aLabel theLabel[];
	private static aLabel moveLabel;					//label being draged from
	private static DropTarget target;
	private static int index;							//vars used to set indexes to correspond to bitboards
	private static int index2;
	private static JMenuBar menu;			
	private static JMenu file, options, difficulty, transTable;
	private static JMenuItem exit, newGame, undo, save, load;
	private static JMenuItem ply5, ply4, ply6, ply7;
	private static JMenuItem second1, second2, second5, second10;
	private static JMenuItem white, black;
	private static JMenuItem mb8, mb16, mb32, mb64, mb128, mb256;
	private static int theInt;							//index of Piece being dragged
	private static JTextArea theText, theText2; 
	
	private static JScrollPane scrollPane, scrollPane2; 
	private static JLabel label, label2;
	private static JPanel	thePanel2;
	private static JPanel thePanel3;
	private static JPanel buttonPanel;			//pannel to have check boxes on it
	private static boolean compWhite;			//computer plays white flag
	private static boolean compBlack;				//computer plays black flag
	private static Vector moveHistory;
	private static int moveCount;						//the number of moves made
	private static int drawCount;					//50 move int variable
	private static boolean	gameOn;				//flag to indicate if game has been won
	private static int searchDepth = 4;
	private static int searchTime = 9999999;
	private static int computerPlayer = 1;			//1 for black, -1 for white
	private static MenuAction menuListener;
	
	
		
	public GUI() throws IOException {			
			
			Pieces = new ImageIcon[64];
			Pos = new cPanel[64];
			theLabel = new aLabel[64];
			theFrame = new JFrame(); 
			theFrame.setBounds(300,100,800,800);
			theFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			theFrame.setJMenuBar(menu);
			thePanel = new JPanel(new GridLayout(8,8));
			thePanel.setMaximumSize(new Dimension(700,700));
			contentPane = theFrame.getContentPane();
			contentPane.setLayout(new FlowLayout(FlowLayout.CENTER,50,50));	
			theText = new JTextArea(10,30);
			scrollPane = new JScrollPane(theText);
			scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
			scrollPane.setWheelScrollingEnabled(true);
			
			theText2 = new JTextArea(10,30);
			scrollPane2 = new JScrollPane(theText2);
			scrollPane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
			thePanel2 = new JPanel();
			thePanel2.setLayout(new BorderLayout());
			label = new JLabel("GAME STATUS");
			label2 = new JLabel("MOVE HISTORY");
			thePanel2.add(label,BorderLayout.NORTH);
			thePanel2.add(scrollPane,BorderLayout.CENTER);
			thePanel3 = new JPanel();
			thePanel3.setLayout(new BorderLayout());
			thePanel3.add(label2,BorderLayout.NORTH);
			thePanel3.add(scrollPane2,BorderLayout.SOUTH);
			thePanel2.add(thePanel3,BorderLayout.SOUTH);
			menuListener = new MenuAction();
			menu = new JMenuBar();
			file = new JMenu("File");
			difficulty = new JMenu("Difficulty");
			transTable = new JMenu("TransTable");
			options = new JMenu("Options");
			newGame = new JMenuItem("New Game");
			save = new JMenuItem("Save Game");
			load = new JMenuItem("Load Game");
			exit = new JMenuItem("Exit");
			undo = new JMenuItem("Undo");
			ply4 = new JMenuItem("4 Ply");
			ply5 = new JMenuItem("5 Ply");
			ply6 = new JMenuItem("6 Ply");
			ply7 = new JMenuItem("7 Ply");
			second1 = new JMenuItem("1 Second");
			second2 = new JMenuItem("2 Seconds");
			second5 = new JMenuItem("5 Seconds");
			second10 = new JMenuItem("10 Seconds");
			white = new JMenuItem("Comp plays white");
			black = new JMenuItem("Comp plays black");
			mb8 = new JMenuItem("8   Megabytes");
			mb16 = new JMenuItem("16  Megabytes");
			mb32 = new JMenuItem("32  Megabytes");
			mb64 = new JMenuItem("64  Megabytes");
			mb128 = new JMenuItem("128 Megabytes");
			mb256 = new JMenuItem("256 Megabytes");
			undo.addActionListener(menuListener);
			newGame.addActionListener(menuListener);
			exit.addActionListener(menuListener);
			save.addActionListener(menuListener);
			load.addActionListener(menuListener);
			ply4.addActionListener(menuListener);
			ply5.addActionListener(menuListener);
			ply6.addActionListener(menuListener);
			ply7.addActionListener(menuListener);
			second1.addActionListener(menuListener);
			second2.addActionListener(menuListener);
			second5.addActionListener(menuListener);
			second10.addActionListener(menuListener);
			white.addActionListener(menuListener);
			black.addActionListener(menuListener);
			mb8.addActionListener(menuListener);
			mb16.addActionListener(menuListener);
			mb32.addActionListener(menuListener);
			mb64.addActionListener(menuListener);
			mb128.addActionListener(menuListener);
			mb256.addActionListener(menuListener);
			file.add(newGame);
			file.add(save);
			file.add(load);
			file.add(exit);
			options.add(undo);
			options.add(white);
			options.add(black);
			difficulty.add(ply4);
			difficulty.add(ply5);
			difficulty.add(ply6);
			difficulty.add(ply7);
			difficulty.add(second1);
			difficulty.add(second2);
			difficulty.add(second5);
			difficulty.add(second10);
			transTable.add(mb8);
			transTable.add(mb16);
			transTable.add(mb32);
			transTable.add(mb64);
			transTable.add(mb128);
			transTable.add(mb256);
			menu.add(file);
			menu.add(options);
			menu.add(difficulty);
			menu.add(transTable);
			theFrame.setJMenuBar(menu);
			dragSource = DragSource.getDefaultDragSource();	
			getImages();								//read in chess piece images
		
			for(int i=0;i<64;i++) {
					Pos[i] = new cPanel(new GridLayout(1,1),i);
					Pos[i].setVisible(true);
					Pos[i].setBorder(BorderFactory.createLineBorder(Color.black));
					theLabel[i] = new aLabel();
					target = new DropTarget(theLabel[i],new TextDropListener(i));
					Pos[i].add(theLabel[i]);
					dragSource.createDefaultDragGestureRecognizer(theLabel[i],DnDConstants.ACTION_COPY_OR_MOVE,this);
			}
			for(int j=0;j<8;j++) {
				for(int i=7;i>=0;i--) {	
					index = (j*8)+i;
					index = 63-index;
					
					thePanel.add(Pos[index]); 
				}	
			}
		
			
			moveHistory = new Vector(100);
			moveCount = 0;
			drawCount = 0;
			contentPane.add(thePanel);
			contentPane.add(thePanel2);
			thePanel.setVisible(true);
			theFrame.setVisible(true);	
			//contentPane.add(buttonPanel);
			gameOn = true;
			compWhite = false;
			compBlack = false;
			Magnum = new Board();
			Magnum.addObserver(this);
			Magnum.newGame();
			theSearch = new Engine(Magnum);
	
		//while(gameOn) {
		//	theSearch.search(999000,9);	
		//}	
	}
	public void update(Observable t, Object o) {
		updateBoard();
	}	
	public static void updateBoard() {
		int[] thePieces;
		int piece;
		thePieces = Magnum.getPiecesInSquare();
		Magnum.callWriter();
		for(int i=0;i<=63;i++) {
				piece = thePieces[i];
				if(piece>=0) {
					theLabel[i].setIcon(Pieces[piece]);
					theLabel[i].setPiece(piece);
				}
				else {
					theLabel[i].setPiece(-1);
					theLabel[i].setIcon(null);
				}
		}
	}			
				
	public static void gameOff() {
		gameOn = false;
	}	
	
	public void getImages() {
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/rook_white.gif");	
		Pieces[0] = new ImageIcon(theImage);
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/knight_white.gif");
		Pieces[1] = new ImageIcon(theImage);
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/bishop_white.gif");
		Pieces[2] = new ImageIcon(theImage);
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/queen_white.gif");
		Pieces[3] = new ImageIcon(theImage);
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/king_white.gif");
		Pieces[4] = new ImageIcon(theImage);
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/pawn_white.gif");
		Pieces[5] = new ImageIcon(theImage);
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/rook_black.gif");	
		Pieces[6] = new ImageIcon(theImage);
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/knight_black.gif");
		Pieces[7] = new ImageIcon(theImage);
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/bishop_black.gif");
		Pieces[8] = new ImageIcon(theImage);
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/queen_black.gif");
		Pieces[9] = new ImageIcon(theImage);
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/king_black.gif");
		Pieces[10] = new ImageIcon(theImage);
		theImage = Toolkit.getDefaultToolkit().getImage("./pieces/pawn_black.gif");
		Pieces[11] = new ImageIcon(theImage);
		
	}
	/*
	public void setBits() {
		BitSet bitBoard;
		int piece;
		for(int i=0;i<64;i++) {
			if(theLabel[i].getPiece()!=-1) {
				Magnum.setBoard(i,theLabel[i].getPiece());
				Magnum.setBoards();
			}	
		}
	}
	*/
			
	public BitSet convertToBSet(long bits) {
		BitSet theSet;
		long bit;
		theSet = new BitSet(64);
		for(int i=0;i<64;i++) {
			bit = bits >> i;
			bit = bit & 1;
			if(bit == 1)
				theSet.set(i);
		}
		return theSet;
	}
			
	public static void removeText2() {
		theText2.setText("");
	}	
	
	public static void printText2(String text) {
		
		theText2.append(text);
	}	
	
	public static void printText(String text) {
		
		theText.append(text);
		theText.append("\n");
	}
	
	
	public static void SetPos(int to,int st) {		//position and string of piece
		int i;
		theLabel[to].setPiece(st);								//set text 
		theLabel[to].setIcon(Pieces[st]);					//set the image
	}
	public static void ClearPos(int to) {
		theLabel[to].setPiece(-1);
		theLabel[to].setIcon(null);
	}
		
	public void dragGestureRecognized(DragGestureEvent event) {
		BitSet 	temp;
		int 	text;	
		long legal2;
		long legal3;
		int 	Decimal1;					//numbers returned from rotating boards
		int 	Decimal2;
		int 	length1;				//length of diagonals
		int 	length2;
		int turn;
		int[] piece_in_square;
		BitSet 	moves = new BitSet(64);
		moveLabel = (aLabel)event.getComponent(); 
		cPanel cP = (cPanel)moveLabel.getParent();
		theInt = cP.getIndex();
		text = moveLabel.getPiece();
		legal2 = 0;
		turn = Magnum.getTurn();
		//piece_in_square = Magnum.getPiecesInSquare();
		//printHistory(pieceSt[piece_in_square[theInt]]+boardString[theInt]);
		//printText(boardString[theInt]);
		
		if(text<6 && turn==1) return;			//not whites turn to move
		
		if(text>5 && turn==-1) return;		//not blacks turn to move
		
		
		if(text==1) {
			legal2 = Magnum.getWKnightMoves(theInt);
		}
		else if(text==7) {
			legal2 = Magnum.getBKnightMoves(theInt);
		}
		else if(text==5) {
			legal2 = Magnum.getWPawnMoves(theInt,Magnum.getPassantB());
		}
		else if(text==11){
			legal2 = Magnum.getBPawnMoves(theInt,Magnum.getPassantW());
			//System.out.println("passant is "+Magnum.getPassantW());
		}
		else if(text==4) {
			legal2 = Magnum.getWKingMoves(theInt);
			legal3 = Magnum.getWKingCastle(theInt);
			/*	
				if(Magnum.getWLeftC()) {				
					legal3 &= (long)1<<6;
				}
				if(!Magnum.getWRightC())  {	
					legal3 &= (long)1<<2;
			}
			*/
				legal2 |= legal3;
		
		}
		else if(text==10) {
			
			legal2 = Magnum.getBKingMoves(theInt);
			legal3 = Magnum.getBKingCastle(theInt);
			if(Board.bCastle != Global.NO_CASTLE) {
				
				if(Board.bCastle == Global.SHORT_CASTLE) 	
					legal3 &= (long)1<<62;
				if(Board.bCastle == Global.LONG_CASTLE) 	
					legal3 &= (long)1<<58;	
			} else 
				legal3 = 0;	
			legal2 |= legal3;
		
		}
		else if(text==0) {
			legal2 = Magnum.getWRookMoves(theInt);
		}
		else if(text==6) {
			legal2 = Magnum.getBRookMoves(theInt);
		}
		else if(text==2) {
			legal2 = Magnum.getWBishopMoves(theInt);
		}
		else if(text==8) {
			legal2 = Magnum.getBBishopMoves(theInt);
		}
		else if(text==3) {
			legal2 = Magnum.getWQueenMoves(theInt);
		}
		else if(text==9) {
			legal2 = Magnum.getBQueenMoves(theInt);
		}
		if(text!=-1) {
			Transferable trans = new StringSelection(new Integer(text).toString());
			event.startDrag(null,trans,this);
		}
		legal = convertToBSet(legal2);
	}
	public void dragEnter(DragSourceDragEvent event){}
	public void dragOver(DragSourceDragEvent event){}	
	public void dragExit(DragSourceEvent event){}
	public void dropActionChanged(DragSourceDragEvent event) {}	
	public void dragDropEnd(DragSourceDropEvent event) {		
		//BoardStatus theMove;
		int side;
		int same;
		int turn;
		turn = Magnum.getTurn();
		//printText("turn is "+turn);
		same = 0;
		if(event.getDropSuccess()) {
			System.out.println("eval2 black is "+Evaluation2.getEval(1));
			//System.out.println("val is "+((int)1<<0));
			//System.out.println("pos is "+Magnum.getPos((int)1<<0));
			//System.out.println("pos2 is "+Magnum.getPos(0));
			//Magnum.printHash();
			System.out.println("hash is "+Board.getHash());
			
			//printText("whtie eval is "+Evaluation.getEval(-1,-1000));
		
                        //printText("black eval is "+Evaluation.getEval(1,-1000));
			//System.out.println("pawn hash is "+Board.getPawnHash());
			//System.out.println("black king safety is "+Magnum.getKingSafety(1));
			//System.out.println("white king safety is "+Magnum.getKingSafety(-1));
			//int dist = Magnum.getDistance(Magnum.getPos(Magnum.getWhiteQueen()),Magnum.getPos(Magnum.getBlackKing()));
			//System.out.println("distance is "+dist);
			long hash = Magnum.generateHash();
			
			System.out.println("generated hash is "+hash);
			System.out.println("hash 2 is "+Board.getHash2());
			System.out.println("genereated hash2 is "+Magnum.generateHash2());
			//printText("eval2 is "+Magnum.getEval2());
			//printText("count is "+Magnum.getCount());
			//Move[] moveArr;
			//moveArr = new Move[50];
			int index = theSearch.getMoves(turn,new int[120],0);
			System.out.println("number of white pieces is "+Board.wPieceNo);
			System.out.println("Number of black pieces is "+Board.bPieceNo);
			
			//int index = theSearch.initMoves3(turn,moveArr,new Move[2],0,Integer.MIN_VALUE);
			//System.out.println("passantB is "+Board.getPassantB());
			//System.out.println("passantW is "+Board.getPassantW());
			//int index = theSearch.getCaptures(turn,new int[57],-20000);
			//System.out.println("total value is "+Board.getTotalValue());
			
			if(turn == computerPlayer) {
				//String move = theSearch.search(searchTime, searchDepth, false);
				//System.out.println("best move is "+move);
			}
		
		}
	}
	
	private class MenuAction implements ActionListener {
		public MenuAction()
		{
		}
		public void actionPerformed(ActionEvent event) {
			Object source = event.getSource();
			JFileChooser chooser;
			File gameFile;
			if(source == undo)	{							//undo last move
				//if(!Magnum.isEmpty()) {
					System.out.println("undo");
					Magnum.unMake(Board.boardMoves[Board.getCount()-1],true,true);
					//Magnum.undoAll();
				//}
			}
			if(source==newGame) {
				Magnum.newGame();
				if(Magnum.getTurn() == computerPlayer) {
					String move = theSearch.search(searchTime, searchDepth, false);
					System.out.println("best move is "+move);
				}
			}	
			if(source==exit) 
				System.exit(0);
			if(source==save) {
				chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File("."));
				int result = chooser.showSaveDialog(theFrame);
				gameFile = chooser.getSelectedFile();	
				Magnum.callFileWrite(gameFile);
			
			}	
			if(source==load) {
				Magnum.newGame();
				chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File("."));
				int result = chooser.showOpenDialog(theFrame);
				gameFile = chooser.getSelectedFile();
				Magnum.callFileRead(gameFile);
				
			}
			if(source==ply4) {
				searchDepth = 4;
				searchTime = 9999999;
				printText("Search Depth is 4 ply");
				
			}
			if(source == ply5) {
				searchDepth = 5;
				searchTime = 9999999;
				printText("Search Depth is 5 ply");
			}
			if(source == ply6) {
				searchDepth = 6;
				searchTime = 9999999;
				printText("Search Depth is 6 ply");
			}
			if(source == ply7) {
				searchDepth = 7;
				searchTime = 9999999;
				printText("Search Depth is 7 ply");
			}
			if(source == second1) {
				searchDepth = 49;
				searchTime = 1000;
				printText("Search Time is 1 second");
			}
			if(source == second2) {
				searchDepth = 49;
				searchTime = 2000;
				printText("Search Time is 2 seconds");
			}
			if(source == second5) {
				searchDepth = 49;
				searchTime = 5000;
				printText("Search Time is 5 seconds");
			}
			if(source == second10) {
				searchDepth = 49;
				searchTime = 10000;
				printText("Search Time is 10 seconds");
			}
			if(source == white) {
				computerPlayer = -1;
				printText("Computer plays white");
				if(Magnum.getTurn() == computerPlayer) {
					String move = theSearch.search(searchTime, searchDepth, false);
					System.out.println("best move is "+move);
				}
				
				
			}
			if(source == black) {
				computerPlayer = 1;
				printText("Computer plays black");
				if(Magnum.getTurn() == computerPlayer) {
					String move = theSearch.search(searchTime, searchDepth, false);
					System.out.println("best move is "+move);
				}
			}
			if(source == mb8) {
				Global.HASHSIZE = 8*32768;
				theSearch.resetHash();
				printText("hashsize is 8 megabytes");
			}
			if(source == mb16) {
				Global.HASHSIZE = 16*32768;
				theSearch.resetHash();
				printText("hashsize is 16 megabytes");
			}
			if(source == mb32) {
				Global.HASHSIZE = 32*32768;
				theSearch.resetHash();
				printText("hashsize is 32 megabytes");
			}
			if(source == mb64) {
				Global.HASHSIZE = 64*32768;
				theSearch.resetHash();
				printText("hashsize is 64 megabytes");
			}
			if(source == mb128) {
				Global.HASHSIZE = 128*32768;
				theSearch.resetHash();
				printText("hashsize is 128 megabytes");
			}
			if(source == mb256) {
				Global.HASHSIZE = 256*32768;
				theSearch.resetHash();
				printText("hashsize is 256 megabytes");
			}
		}
		
	}	
		
	private class TextDropListener implements DropTargetListener {
		private int posIndex;
		private aLabel lab;
		private JPanel thePl;
		
		public TextDropListener(int i) {
					posIndex = i;
		}
		
		public void dragEnter(DropTargetDragEvent event) {}
		public void dragExit(DropTargetEvent event) {}	
		public void dragOver(DropTargetDragEvent event) {}
		public void dropActionChanged(DropTargetDragEvent event) {}
		public void drop(DropTargetDropEvent event) {
			BitSet allP;			//all pieces
			int allSquare[];		// info about all pieces in squares 
			long temp;
			long attack;
			long king;
			int reps;
			int posKing;				//int position of king
			//allP = Magnum.getAllPieces();
			allSquare = Magnum.getPiecesInSquare();
			boolean ok;		
			ok = legal.get(posIndex);
			Transferable trans = event.getTransferable();
			if(theLabel[posIndex]==moveLabel) {
				event.rejectDrop();
			}
			else if(!ok) {
				event.rejectDrop();
			}		
			else {
				event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
				try {	
				String s = (String)trans.getTransferData(DataFlavor.stringFlavor);
				System.out.println("plus 7 is "+Global.plus7[4]);
				//if(SEE.isPinned(Board.getTurn(),posIndex,theInt)) {
				//	System.out.println("pinned SEE");
				//}
				//if(Engine.isPinned(Board.getTurn(),posIndex,theInt)) {
				//	System.out.println("pinned SIX");
				//}
				int mv = MoveFunctions.makeMove(posIndex, theInt);
				reps = Magnum.makeMove(mv,true,true);	//make move in search..update bitboards
				//printText("move count is "+Magnum.getCount());
				//printText("draw count is "+Magnum.getDraw());
				printText("3 move draw count is "+reps);
				Pos[posIndex].updateUI();
				}
				catch(Exception e) { 
					e.printStackTrace();
				}
			event.dropComplete(true);
			
			
			}				

		}
	
	}
}