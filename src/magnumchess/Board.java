package magnumchess;

/**
 * Board.java
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


import magnumchess.Bitbase;
import java.io.File;
import java.util.Arrays;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
//import java.util.Random;

/**
 * Board.java - This class follows the singleton design pattern
 * Represents the chess board and all functionality needed to play chess excluding the search and evaluation
 * This includes gathering all possible moves for each piece
 * making and unmaking moves 
 * loading a fen position
 * starting a new game
 *
 * @version 	4.00 25 March 2012
 * @author 	Eric Stock
 */

public final class Board {

   public class CheckInfo {
        public long[] checkSquares = new long[Global.PIECE_ALL];
        public long dcCandidates;
        public long pinned; 
        public boolean pinnedReady;
        public boolean checksReady;
   }
    
    
    
    InputStream inputStream;
    DataInputStream dataInputStream;

    /** count of all moves made over the board*/
    public int moveCount;					
    
    /** boardMoves is an array of all moves made in the game
     * -To do - switch this to a list as currently if the game goes over 256 moves, there is a crash
     */
    public static final int boardMoves[] = new int[2048];
	
    /** array containing occupancy of pieces on the 64 board positions */
    public final int piece_in_square[] = new int[64];
    public final int pieceListIndices[] = new int[64];
    public final int pieceList[][] = new int[12][16];
    public final int pieceTotals[] = new int[12];

    public static int materialValues[] = new int[9 * 9 * 3 * 3 * 3 * 3 * 3 * 3 * 2 * 2];
    public static int materialKey = 0;
    
    public int[] noPieces = new int[2];
    
    /** value used to adjust material table when 2 queens of a side or both are present */
    private int materialAdjust;

    private int[][] QueenMaterialAdjustArray = new int[8][8];

    /** total material on the board */
    public int totalValue;
	
    /** knight moves for each square */
    private static final long[] KnightMoveBoard = new long[64];
    /** white pawn moves for each square */
    private static final long[][] PawnMoveBoard	= new long[2][64];
    
    /** pawn attack moves for each sqaure */
    private static final long[][] PawnAttackBoard = new long[2][64];
	
    /** The following 64 bit longs represent the bitboards used to
     * represent the occupancy for various pieces on the board
     */
    public long bitboard;
	 
    public long[][] pieceBits = new long[2][7];
   
    public long slidePieces;
	
    /** castle status variables for each side*/
    public int[] castleFlag = new int[2];
	
    /** this is the occupancy information for the initial chess position */
    private static final int[] init =     {0,1,2,3,4,2,1,0,
                                          5,5,5,5,5,5,5,5,
                                          -1,-1,-1,-1,-1,-1,-1,-1,
                                          -1,-1,-1,-1,-1,-1,-1,-1,
                                          -1,-1,-1,-1,-1,-1,-1,-1,
                                          -1,-1,-1,-1,-1,-1,-1,-1,
                                          11,11,11,11,11,11,11,11,
                                          6,7,8,9,10,8,7,6};
    

    /** these arrays are used to mask off and generate moves using magic bitboard move generation */
    private final int bishopShift[] = new int[64];			//size of move database for each square
    private static final int rookShift[] = new int[64];			//size of move database for each square
    private static final long bMask[] = new long[64];			//bishop occupancy masks
    private static final long rMask[] = new long[64];			//rook occupancy masks
    private static final long bMagics[] = new long[64];			//64 9-bit bishop magic numbers
    private static final long rMagics[] = new long[64];			//64 rook magic numbers
    private static final long bishopTable[][] = new long[64][];	//9 bit bishop table of moves
    private static final long rookTable[][] = new long[64][];		//rook table of moves

    private final long kingMoveTable[] = new long[64];
    private final long kingCastleTable[] = new long[256];

    private int flagHistory[] = new int[2048];
	
    /** enPassant capture squares for each side */
    //private int passantW, passantB;			
    private int[] passant = new int[2];
    
    /** 50 move draw count variable */
    private int drawCount;						
	
    /** -1 white to move, 1 black moves */
    private int turn;							
	
    /** 64 bit represent the hash code for each position*/
    public long hashValue;
    public long excludedHash;   //used to modify hash table for storing and retrieving results from excluded move searches
    /** hash values for all 12 pieces on all 64 squares
     * note this is optimized for 32 bit computers
     */
    private static final long[][] pHash = new long[64][12];
    private static final long[][] pawnKingHash = new long[64][12];
	
    
    
    /** these are the hash values for the status of each sides castling */
    private static final long CastleHash[][] = new long[2][8];
	 
    /** side to move hash value */
    private long bHashMove;						//hash for when black is to move
	
    /** hash value for each passant square */
    private static final long[] passantHashW = new long[9];				//hash for passant squares
    private static final long[] passantHashB = new long[9];
	
    /*
     * These two constants are the values for the passant squares when there is no actual passant square
     * There is a clever/idiot trick I thought of using the passant square % 9 to index the hash for the passant square
     * Plus, these values will never cause a false attack...ex/  black pawns never attack sq 51 and white pawns never attack 3
     */
    private static final int NO_PASSANT_WHITE = 51;
    private static final int NO_PASSANT_BLACK = 3;

    /** hash history arrays stores the previous hash as each move is made
     * note - this should be changed to a list or vector as after 256 moves, crash
     */
    private long[] hashHistory = new long[2048];
    
    /** hash value for the pawns on the board
     * used for seperate pawn hash table
     */
    private long pawnHash;
	
    /** HistoryWrite object is used to convert moves to and from algebraic notation and fen notation */
    private HistoryWriter writer;					//stores and writes the history of all moves made
	
    /** Array of values representing the number of squares a queen would traverse
     * between any two squares on the board
     */
    private static final int[][] queenDist = new int[64][64];
    
    /** Array of values representing the number of squares a rook would traverse
     * between any two squares on the board
     */
    private static final int[][] rookDist = new int[64][64];
	
    /** zorbist key history array -- used for repetition detection */
    private static final long[] zorbistHistory = new long[202];
    
    /** array containing index of last reversable move -- used for repetition detection */
    private static final int[] lastReversableMove = new int[202];
    
    /** index into zorbistHistory array -- used for repetition detection */
    private int zorbistDepth;

    /** flag to indicate if the last move made was reversable or not */
    private boolean reversable = true;

    /** variables used to store the current line of moves from the root of the search **/
    private static final int[] arrCurrentMoves = new int[128];
    private int iCurrentMovesDepth;

    private Bitbase bitbase;
    /** call to private constructor - a la singleton pattern */
    private static final Board INSTANCE = new Board();

    public void SetHistoryWriter( HistoryWriter theWriter )
    {
            writer = theWriter;
    }
    /**
    * Constructor Board
    * 
    * is private so only 1 instance is created
    * 
    * Variables necessary for move generation are initialized
    */

    private Board()
    {
        //Random rand = new Random();
        initQueenDist();
        initRookDist();
        InitializeDataFromFile();
        InitializeData();
        zorbistDepth = 1;
        lastReversableMove[0] = 1;
        lastReversableMove[1] = 1;
        InitializeMaterialArray();
        
        //excludedHash = rand.nextLong() & Long.MAX_VALUE;
    }
	
    private void InitializeDataFromFile()
    {
        try
        {
            final String inputfile = "initialization.dat";
            inputStream = Board.class.getResourceAsStream(inputfile);
            dataInputStream = new DataInputStream(inputStream);

            for(int i=0; i<64; i++)
            {
                rMagics[i] = dataInputStream.readLong();
                rookShift[i] = dataInputStream.readInt();
                rookTable[i] = new long[1 << rookShift[i]];
                bMagics[i] = dataInputStream.readLong();
                bishopShift[i] = dataInputStream.readInt();
                bishopTable[i] = new long[1 << bishopShift[i]];
                rMask[i] = dataInputStream.readLong();
                bMask[i] = dataInputStream.readLong();
            }

            for(int i=0; i<64; i++)
            {
                for(int j=0; j<(1<<rookShift[i]); j++ )
                {
                    rookTable[i][j] = dataInputStream.readLong();
                }
            }

            for(int i=0; i<64; i++)
            {
                for(int j=0; j<(1<<bishopShift[i]); j++ )
                {
                    bishopTable[i][j] = dataInputStream.readLong();
                }
            }

            for(int i=0; i<64; i++)
            {
                kingMoveTable[i] = dataInputStream.readLong();
            }

            for(int i=0; i<256; i++)
            {
                kingCastleTable[i] = dataInputStream.readLong();
            }

            for(int i=0; i<64; i++)
            {
                KnightMoveBoard[i] = dataInputStream.readLong();
                PawnMoveBoard[Global.COLOUR_WHITE][i] = dataInputStream.readLong();
                PawnAttackBoard[Global.COLOUR_WHITE][i] = dataInputStream.readLong();
                PawnMoveBoard[Global.COLOUR_BLACK][i] = dataInputStream.readLong();
                PawnAttackBoard[Global.COLOUR_BLACK][i] = dataInputStream.readLong();
            }

            //read the generated random 64 bit hash values used to generate the zorbist key
            for(int i=0;i<64;i++) {
                for(int j=0;j<12;j++) {
                    pHash[i][j] = dataInputStream.readLong();
                    if( j%6 == 5 || j%6 == 4)
                        pawnKingHash[i][j] = pHash[i][j];
                }
            }

            for(int i=0;i<9;i++) {
                passantHashW[i] = dataInputStream.readLong();
                passantHashB[i] = dataInputStream.readLong();
            }

            bHashMove = dataInputStream.readLong();

            for(int i=0;i<8;i++) {
                CastleHash[Global.COLOUR_BLACK][i] = dataInputStream.readLong();
                CastleHash[Global.COLOUR_WHITE][i] = dataInputStream.readLong();
            }

            bitbase = new Bitbase();
            bitbase.LoadData( dataInputStream );
            
            dataInputStream.close();
        }
        catch( IOException iox )
        {
            System.out.println("unable to open file stream");
        }
    }

    Bitbase GetBitbase() {
        return bitbase;
    }
    private void InitializeData()
    {
        for(int j=0;j<64;j++) {
            int temp = Global.Diag1Groups[j];
            Global.diag1Masks[temp] |=(long)1<<j;
        }

        for(int j=0;j<64;j++) {
            int temp = Global.Diag2Groups[j];
            Global.diag2Masks[temp] |= (long)1<<j;
        }

        for(int i=0;i<64;i++) {
            Global.set_Mask[i] = (long)1<<i;
        }

        for(int i=0;i<8;i++) {
            for(int j=0;j<64;j++) {
                if(j%8==i)
                    Global.fileMasks[i] |= (long)1<<j;
            }
        }
        for(int i=0;i<8;i++) {
            for(int j=0;j<64;j++) {
                if(j/8==i)
                Global.rankMasks[i] |= (long)1<<j;
            }
        }
        for (int i = 0; i < 64; i++) {
            for(int j = i + 9; j < 64; j += 9)	{
                if(j % 8 == 0) break;
                Global.plus9[i] |= Global.set_Mask[j];
            }
        }
        for(int i = 0; i < 64; i++) {
            for(int j = i - 9; j >= 0; j -= 9) {
                if(j % 8 == 7) break;
                Global.minus9[i] |= Global.set_Mask[j];
            }
        }
        for( int i = 0; i < 64; i++) {
            Global.plus7[i] = 0;
            for(int j = i + 7; j < 64; j += 7) {
                if(j % 8 == 7) break;
                Global.plus7[i] |= Global.set_Mask[j];
            }
        }
        for(int i = 0; i < 64; i++) {
            for(int j = i - 7; j >= 0; j -= 7) {
                if(j % 8 == 0) break;
                Global.minus7[i] |= Global.set_Mask[j];
            }
        }
        for( int i = 0; i < 64; i++) {
            for(int j = i + 8; j < 64; j += 8) {
                Global.plus8[i] |= Global.set_Mask[j];
            }
        }
        for (int i = 0; i < 64; i++) {
            for(int j = i - 8; j >= 0; j -= 8) {
                Global.minus8[i] |= Global.set_Mask[j];
            }
        }
        for( int i = 0; i < 64; i++) {
            for(int j = i+1; j < 64; j++) {
                if(j%8 == 0) break;
                Global.plus1[i] |= Global.set_Mask[j];
            }
        }
        for( int i = 0; i < 64; i++) {
            for(int j = i - 1; j >= 0; j -= 1) {
                if(j % 8 == 7) break;
                Global.minus1[i] |= Global.set_Mask[j];
            }
        }

        for(int i=8; i<64; i++) {
            int file = i%8;
            if(file == 0)
                Global.passed_masks[Global.COLOUR_WHITE][i] = Global.plus8[i] | Global.plus8[i+1];
            else  if (file == 7)
                Global.passed_masks[Global.COLOUR_WHITE][i] = Global.plus8[i] | Global.plus8[i-1];
            else
                Global.passed_masks[Global.COLOUR_WHITE][i] = Global.plus8[i] | Global.plus8[i+1] | Global.plus8[i-1];
        }

        for(int i=55 ; i>=0; i--) {
            int rank = i%8;
            if(rank == 0)
                Global.passed_masks[Global.COLOUR_BLACK][i] = Global.minus8[i] | Global.minus8[i+1];
            else if(rank == 7)
                Global.passed_masks[Global.COLOUR_BLACK][i] = Global.minus8[i] | Global.minus8[i-1];
            else
                Global.passed_masks[Global.COLOUR_BLACK][i] = Global.minus8[i] | Global.minus8[i+1] | Global.minus8[i-1];
        }

        for(int i=Global.COLOUR_WHITE; i<=Global.COLOUR_BLACK; i++)
        {
            for(int j=0; j<64; j++)
            {
                Global.mask_behind[i][j] = (i == Global.COLOUR_WHITE) ? Global.minus8[j] : Global.plus8[j];
                Global.mask_in_front[i][j] = (i == Global.COLOUR_WHITE) ? Global.plus8[j] : Global.minus8[j];
            }
        }

        for(int i=Global.COLOUR_WHITE; i<=Global.COLOUR_BLACK; i++)
        {
            for(int j=0; j<64; j++)
            {
                Global.mask_forward[i][j] = 0;
                if(i == Global.COLOUR_WHITE)
                {
                    int rank = j / 8;
                    for(int k = rank + 1; k < 8; k++)
                    {
                        Global.mask_forward[i][j] |= Global.rankMasks[k];
                    }
                }
                else
                {
                    int rank = j / 8;
                    for(int k = rank - 1; k >= 0; k--)
                    {
                        Global.mask_forward[i][j] |= Global.rankMasks[k];
                    }
                }
            }
        }

        for(int i=0; i<8; i++)
        {
            Global.neighbour_files[i] = 0;
            Global.neighbour_files[i] |= (i > 0) ? Global.fileMasks[i-1] : 0;
            Global.neighbour_files[i] |= (i < 7) ? Global.fileMasks[i+1] : 0;
        } 
        
        for(int i=0; i<64; i++) {
            for(int j=0; j<64; j++) {
                long queenAttacks = getMagicBishopMoves(i, 0) | getMagicRookMoves(i, 0);
                queenAttacks &= 1L << j;
                if( queenAttacks != 0 )
                {
                    int delta = (j - i) / Math.max( Math.abs(i/8 - j/8), Math.abs(i%8 - j%8));
                    for( int k = i+delta; k!= j; k+= delta)
                    {
                        Global.mask_between[i][j] |= 1L << k;
                    }
                    
                }
            }
        }
        
        for( int i=0; i<64; i++)
        {
            Global.bishopMasks[i] = (Global.diag1Masks[Global.Diag1Groups[i]] | Global.diag2Masks[Global.Diag2Groups[i]]) ^ Global.set_Mask[i];
            Global.rookMasks[i] = (Global.rankMasks[i/8] | Global.fileMasks[i%8]) ^ Global.set_Mask[i];
        }
    }

    /**
     *  method getInstance
     * 
     * returns only instance of Board Class
     */
    public static Board getInstance(){
        return INSTANCE;
    }
    
    /** 
     *  method undoAll
     * 
     * un-does every move in game
     */
    
    public final void undoAll(){
        int noMoves = moveCount;
        for(int i=noMoves-1;i>=0;i--) {
            UnMake(boardMoves[i] ,true);
            writer.removeLastHistory();
        }
        zorbistDepth = 1;
        iCurrentMovesDepth = 0;
    }
    
    static int GetRank(int square) {
        return square >> 3;
    }

    static int GetRelativeRank(int p_iColour, int square) {
        return GetRank(square ^ (p_iColour * 56));
    }

    static int GetRelativePosition(int colour, int position) {
        return GetRelativeRank(colour, position) * 8 + position % 8;
    }

    static int GetNextRank(int colour, int rank) {
        return colour == Global.COLOUR_WHITE ? rank + 1 : rank - 1;
    }

    static int GetPreviousRank(int colour, int rank) {
        return colour == Global.COLOUR_WHITE ? rank - 1 : rank + 1;
    }

    /**
     *  method newGame
     * 
     * sets the board up for a new game
     */
    public final void newGame() {
        ClearBoard();
        for(int i=0;i<64;i++) {
            if(init[i]!=-1) {
                setBoard(i,init[i]);
                hashValue ^= pHash[i][init[i]];
            }	
        }
        hashValue ^= CastleHash[Global.COLOUR_BLACK][castleFlag[Global.COLOUR_BLACK]];
        hashValue ^= CastleHash[Global.COLOUR_WHITE][castleFlag[Global.COLOUR_WHITE]];
        hashValue ^= passantHashW[passant[Global.COLOUR_WHITE]%9];
        hashValue ^= passantHashB[passant[Global.COLOUR_BLACK]%9];
        Engine.resetHash();
        Evaluation2.clearEvalHash();
        Evaluation2.clearPawnHash();
        writer.reset();	
    }

    /**
     *  method acceptFen
     *
     * reads in a fen string which represents a chess position
     * this position is loaded
     *
     *  @ param String fen - fen position to load
     */
    public  void acceptFen(String fen)
    {
        ClearBoard();

        /** now process the fen string where it contains the placement of pieces */
        int count = 63;
        for(int i=0; i<8; i++) {
            String rank;
            int endOfRank = fen.indexOf("/");
            if(endOfRank == -1) {
                endOfRank = fen.indexOf(" ");
                rank = fen.substring(0,endOfRank);
                fen = fen.substring(endOfRank+1);
            }else {
                rank = fen.substring(0,endOfRank);
                fen = fen.substring(endOfRank+1);
            }

            for(int j=0; j<endOfRank; j++) {
                char c = rank.charAt(endOfRank-1-j);
                switch(c) {
                    case('r'):
                        setBoard(count,6);
                        hashValue ^= pHash[count][6];
                        count--;
                        break;
                    case('n'):
                        setBoard(count,7);
                        hashValue ^= pHash[count][7];
                        count--;
                        break;
                    case('b'):
                        setBoard(count,8);
                        hashValue ^= pHash[count][8];
                        count--;
                        break;
                    case('q'):
                        setBoard(count,9);
                        hashValue ^= pHash[count][9];
                        count--;
                        break;
                    case('k'):
                        setBoard(count,10);
                        hashValue ^= pHash[count][10];
                        count--;
                        break;
                    case('p'):
                        setBoard(count,11);
                        hashValue ^= pHash[count][11];
                        count--;
                        break;
                    case('R'):
                        setBoard(count,0);
                        hashValue ^= pHash[count][0];
                        count--;
                        break;
                    case('N'):
                        setBoard(count,1);
                        hashValue ^= pHash[count][1];
                        count--;
                        break;
                    case('B'):
                        setBoard(count,2);
                        hashValue ^= pHash[count][2];
                        count--;
                        break;
                    case('Q'):
                        setBoard(count,3);
                        hashValue ^= pHash[count][3];
                        count--;
                        break;
                    case('K'):
                        setBoard(count,4);
                        hashValue ^= pHash[count][4];
                        count--;
                        break;
                    case('P'):
                        setBoard(count,5);
                        hashValue ^= pHash[count][5];
                        count--;
                        break;
                    case('1'):
                        count--;
                        break;
                    case('2'):
                        count-=2;
                        break;
                    case('3'):
                        count-=3;
                        break;
                    case('4'):
                        count-=4;
                        break;
                    case('5'):
                        count-=5;
                        break;
                    case('6'):
                        count-=6;
                        break;
                    case('7'):
                        count-=7;
                        break;
                    case('8'):
                        count-=8;
                        break;
                }
            }
        }

        /** now process the side to move information */
        char c = fen.charAt(0);
        fen = fen.substring(fen.indexOf(" ")+1);
        if(c == 'w') {
            turn = Global.COLOUR_WHITE;
        } else {
            turn = Global.COLOUR_BLACK;
        }
        if(turn == Global.COLOUR_BLACK) {
            hashValue ^= bHashMove;
        }
        /** now process the castling rights */
        String token = fen.substring(0,fen.indexOf(" "));
        fen = fen.substring(fen.indexOf(" ")+1);
        int tokenSize = token.length();
        for(int i=0;i<tokenSize;i++) {
        c = token.charAt(i);
        switch(c) {
            case('-'):
                castleFlag[Global.COLOUR_WHITE] = Global.NO_CASTLE;
                castleFlag[Global.COLOUR_BLACK] = Global.NO_CASTLE;
                break;
            case('K'):
                castleFlag[Global.COLOUR_WHITE] = Global.SHORT_CASTLE;
                break;
            case('Q'):
                if(castleFlag[Global.COLOUR_WHITE] == Global.NO_CASTLE)
                    castleFlag[Global.COLOUR_WHITE] = Global.LONG_CASTLE;
                else
                    castleFlag[Global.COLOUR_WHITE] = Global.BOTH_CASTLE;
                break;
            case('k'):
                castleFlag[Global.COLOUR_BLACK] = Global.SHORT_CASTLE;
                break;
            case('q'):
                if(castleFlag[Global.COLOUR_BLACK] == Global.NO_CASTLE)
                    castleFlag[Global.COLOUR_BLACK] = Global.LONG_CASTLE;
                else
                    castleFlag[Global.COLOUR_BLACK] = Global.BOTH_CASTLE;
                break;
            }
        }

        /**process the passant square
        *get the first character - if it is a '-', then no passant square
        */
        c = fen.charAt(0);
        if(c != '-') {
            token = fen.substring(0,fen.indexOf(" "));
            int pSq = HistoryWriter.getNumericPosition(token);
            if(turn == Global.COLOUR_WHITE)                  
                passant[Global.COLOUR_BLACK] = pSq;
            else
                passant[Global.COLOUR_WHITE] = pSq;
        }
        fen = fen.substring(fen.indexOf(" "));

        /** now process the drawCount */
        fen = fen.substring(fen.indexOf(" ")+1);
        token = fen.substring(0,fen.indexOf(" "));
        Integer noMoves = new Integer(token);
        drawCount = noMoves.intValue();

        /** now process the moveCount
        *   Note there are problems with this part of the fen reader
        *   To Do...change internal representation of number of moves
        * - fix unmaking moves based on this the no of moves
        * - currently will only work for no of moves at 1
        */

        fen = fen.substring(fen.indexOf(" ")+1);
        token = fen;
        noMoves = new Integer(token);
        moveCount = noMoves.intValue()-1;

        hashValue ^= passantHashW[passant[Global.COLOUR_WHITE]%9];
        hashValue ^= passantHashB[passant[Global.COLOUR_BLACK]%9];

        /** set the has values for the recently set castling rights */
        hashValue ^= CastleHash[Global.COLOUR_BLACK][castleFlag[Global.COLOUR_BLACK]];
        hashValue ^= CastleHash[Global.COLOUR_WHITE][castleFlag[Global.COLOUR_WHITE]];
    }

    public void ClearBoard()
    {
        materialKey = 0;
        materialAdjust = 0;
        zorbistDepth = 1;
        moveCount = 0;
        drawCount = 0;
        bitboard = 0;
        Arrays.fill( pieceBits[Global.COLOUR_BLACK], 0 );
        Arrays.fill( pieceBits[Global.COLOUR_WHITE], 0 );
        slidePieces = 0;
        totalValue = -Global.values[Global.PIECE_KING] * 2;
        turn = Global.COLOUR_WHITE;						
        noPieces[0] = noPieces[1] = 0;
        iCurrentMovesDepth = 0;
        Arrays.fill(piece_in_square, -1);
        Arrays.fill(pieceListIndices, -1);
        for(int i = 0; i < pieceList.length; i++) {
            Arrays.fill( pieceList[i], 0);
        }
        Arrays.fill( pieceTotals, 0);
        castleFlag[Global.COLOUR_BLACK] = Global.BOTH_CASTLE;
        castleFlag[Global.COLOUR_WHITE] = Global.BOTH_CASTLE;
        passant[Global.COLOUR_WHITE] = NO_PASSANT_WHITE;
        passant[Global.COLOUR_BLACK] = NO_PASSANT_BLACK;
        hashValue = 0;
        pawnHash = 0;
    }
    
    public void FlipPosition() {
        hashValue = 0;
        materialKey = 0;
        materialAdjust = 0;
        bitboard = 0;
        Arrays.fill( pieceBits[Global.COLOUR_BLACK], 0 );
        Arrays.fill( pieceBits[Global.COLOUR_WHITE], 0 );
        slidePieces = 0;
        totalValue = -Global.values[Global.PIECE_KING] * 2;
        turn ^= 1;	
        if(turn == Global.COLOUR_BLACK) {
            hashValue ^= bHashMove;
        }
        noPieces[0] = noPieces[1] = 0;

        int tempPassantB = passant[Global.COLOUR_BLACK];
        if(passant[Global.COLOUR_WHITE] != NO_PASSANT_WHITE) {
            int flipRank = 7 - passant[Global.COLOUR_WHITE]/8;
            int flipPos = flipRank * 8 + passant[Global.COLOUR_WHITE]%8;
            passant[Global.COLOUR_BLACK] = flipPos;
        } else
            passant[Global.COLOUR_BLACK] = NO_PASSANT_BLACK;

        if(tempPassantB != NO_PASSANT_BLACK) {
            int flipRank = 7 - tempPassantB/8;
            int flipPos = flipRank * 8 +tempPassantB%8;
            passant[Global.COLOUR_WHITE] = flipPos;
        } else
            passant[Global.COLOUR_WHITE] = NO_PASSANT_WHITE;

        hashValue ^= passantHashB[passant[Global.COLOUR_BLACK]%9];
        hashValue ^= passantHashW[passant[Global.COLOUR_WHITE]%9];

        pawnHash = 0;

        int tempCastle = castleFlag[Global.COLOUR_BLACK];
        castleFlag[Global.COLOUR_BLACK] = castleFlag[Global.COLOUR_WHITE];
        castleFlag[Global.COLOUR_WHITE] = tempCastle;
        hashValue ^= CastleHash[Global.COLOUR_BLACK][castleFlag[Global.COLOUR_BLACK]];
        hashValue ^= CastleHash[Global.COLOUR_WHITE][castleFlag[Global.COLOUR_WHITE]];

        Arrays.fill(pieceListIndices, -1);
        for(int i = 0; i < pieceList.length; i++) {
            Arrays.fill( pieceList[i], 0);
        }
        Arrays.fill( pieceTotals, 0);

        int[] temp_piece_in_square = new int[64];
        for(int i=0;i<64;i++) {										
            temp_piece_in_square[i] = piece_in_square[i];
            piece_in_square[i] = -1;
        }

        for(int i=0;i<64;i++) {
            int flipRank = 7 - i/8;
            int flipPos = flipRank * 8 + i%8;
            if(temp_piece_in_square[i] != -1) {
                if( temp_piece_in_square[i] > 5)
                {
                    setBoard(flipPos, temp_piece_in_square[i] - 6 );
                    hashValue ^= pHash[flipPos][temp_piece_in_square[i] - 6];
                }
                else
                {
                    setBoard(flipPos, temp_piece_in_square[i] + 6 );
                    hashValue ^= pHash[flipPos][temp_piece_in_square[i] + 6];
                }
            }
        }

        //hashValue = generateHash();
        //if(hashValue != generateHash()) {
        //	System.out.println("info string generatehash is +"+generateHash());
        //}
    }

    public final void InitializeMaterialArray() {
        
        Global.totalValue = Global.values[0] * 4 + Global.values[1] * 4 + Global.values[2] * 4 + Global.values[3] * 2 + Global.values[5] * 16;
        int noReps = 0;

        QueenMaterialAdjustArray[2][0] = 2 * -Global.values[3] - Global.values[9];
        QueenMaterialAdjustArray[2][1] = -Global.values[3] + Global.values[5];
        QueenMaterialAdjustArray[2][2] = -Global.values[9] + Global.values[5];
        QueenMaterialAdjustArray[1][2] = 2 * Global.values[9] + Global.values[5];
        QueenMaterialAdjustArray[0][2] = 2 * Global.values[9] + Global.values[5];

        for(int wRook = 0; wRook < 3; wRook++) {

            for(int bRook = 0; bRook < 3; bRook++) {

                for(int wBishop = 0; wBishop < 3; wBishop++) {

                    for(int bBishop = 0; bBishop < 3; bBishop++) {

                        for(int wKnight = 0; wKnight < 3; wKnight++) {

                            for(int bKnight = 0; bKnight < 3; bKnight++) {

                                for(int wQueen = 0; wQueen < 2; wQueen++) {

                                    for(int bQueen = 0; bQueen < 2; bQueen++) {

                                        for(int wPawn = 0; wPawn < 9; wPawn++) {

                                            for(int bPawn = 0; bPawn < 9; bPawn++) {

                                                noReps++;
                                                int noWhitePieces = wQueen + wRook + wBishop + wKnight + wPawn;
                                                int noBlackPieces = bQueen + bRook + bBishop + bKnight +  bPawn;

                                                int noWhiteMinors = wBishop + wKnight;
                                                int noBlackMinors = bBishop + bKnight;

                                                int index = wRook + bRook   * 3 +
                                                                    wBishop * 3 * 3 +
                                                                    bBishop * 3 * 3 * 3 +
                                                                    wKnight * 3 * 3 * 3 * 3 +
                                                                    bKnight * 3 * 3 * 3 * 3 * 3 +
                                                                    wQueen  * 3 * 3 * 3 * 3 * 3 * 3 +
                                                                    bQueen  * 3 * 3 * 3 * 3 * 3 * 3 * 2 +
                                                                    wPawn   * 3 * 3 * 3 * 3 * 3 * 3 * 2 * 2 +
                                                                    bPawn   * 3 * 3 * 3 * 3 * 3 * 3 * 2 * 2 * 9;

                                                // set global constants used to recognize special endgames
                                                int test;
                                                if( noBlackPieces == 0 && noWhitePieces == 2 && wBishop == 1 && wKnight == 1)
                                                    test = index;//Global.whiteKBNK = index;
                                                else if( noWhitePieces == 0 && noBlackPieces == 2 && bBishop == 1 && bKnight == 1)
                                                    test = index;
                                                else if( noWhitePieces == 1 && noBlackPieces == 1 && wRook == 1 && bBishop == 1 )
                                                    test = index;
                                                else if( noWhitePieces == 1 && noBlackPieces == 1 && bRook == 1 && wBishop == 1 )
                                                    test = index;
                                                else if( noWhitePieces == 1 && noBlackPieces == 1 && wRook == 1 && bKnight == 1 )
                                                    test = index;
                                                else if( noWhitePieces == 1 && noBlackPieces == 1 && bRook == 1 && wKnight == 1 )
                                                    test = index;
                                                else if( noWhitePieces == 1 && noBlackPieces == 1 && wRook == 1 && bPawn == 1 )
                                                    test = index;
                                                else if( noWhitePieces == 1 && noBlackPieces == 1 && bRook == 1 && wPawn == 1 )
                                                    test = index;
                                                else if( noWhitePieces == 1 && wPawn == 1 && noBlackPieces == 0)
                                                    test = index;
                                                else if( noBlackPieces == 1 && bPawn == 1 && noWhitePieces == 0)
                                                    test = index;
                                                //Global.blackKBNK = index;
                                                
                                                
                                                
                                                //+ values for back, -ve for white
                                                /** recognize some draw situations */
                                                int materialImbalence = 0;  

                                                if(noWhitePieces <= 1 && noBlackPieces <= 1 && noWhiteMinors == noWhitePieces && noBlackMinors == noBlackPieces) //kings and 1 or less minors draw
                                                    materialImbalence = Global.materialDraw;
                                                else if(noWhitePieces <= 2 && noBlackPieces == 2 && noBlackPieces == bKnight && noWhitePieces == noWhiteMinors && wBishop != 2)  //2 black knights against 2 or less minors
                                                        materialImbalence = Global.materialDraw;
                                                else if(noBlackPieces <= 2 && noWhitePieces == 2 && noWhitePieces == bKnight && noBlackPieces == noBlackMinors && bBishop != 2)  //2 white knights against 2 or less minors
                                                        materialImbalence = Global.materialDraw;
                                                else if(noWhitePieces == 1 && wPawn == 1 && noBlackPieces == 1 && noBlackPieces == noBlackMinors)  //1 white pawn against a minor
                                                        materialImbalence = 0;
                                                else if(noBlackPieces == 1 && bPawn == 1 && noWhitePieces == 1 && noWhitePieces == noWhiteMinors)  //1 black pawn against a minor
                                                        materialImbalence = 0;
                                                else if(noWhiteMinors == 2 && noWhitePieces == 2 && noWhitePieces != wBishop && noBlackPieces == 1 && noBlackMinors == 1)  //white has 2 minors, black has 1, white doesn't have bishop pair
                                                        materialImbalence = 0;
                                                else if(noBlackMinors == 2 && noBlackPieces == 2 && noBlackPieces != bBishop && noWhitePieces == 1 && noWhiteMinors == 1)  //black has 2 minors, white has 1, black doesn't have bishop pair
                                                        materialImbalence = 0;
                                                else if(noBlackPieces == 2 && noBlackPieces == noBlackMinors && noBlackPieces != bBishop && noWhitePieces == 1 && wRook == 1) //kbn vs KR
                                                        materialImbalence = 0;
                                                else if(noWhitePieces == 2 && noWhitePieces == noWhiteMinors && noWhitePieces != wBishop && noBlackPieces == 1 && bRook == 1) //KBN vs kr
                                                        materialImbalence = 0;
                                                else {
                                                    materialImbalence = -Global.values[3] * wQueen +
                                                                         Global.values[9] * bQueen -
                                                                         Global.values[0] * wRook +
                                                                         Global.values[6] * bRook -
                                                                         Global.values[2] * wBishop +
                                                                         Global.values[8] * bBishop -
                                                                         Global.values[1] * wKnight +
                                                                         Global.values[7] * bKnight -
                                                                         Global.values[5] * wPawn +
                                                                         Global.values[11] * bPawn;
                                                    if(wBishop == 2 && bBishop < 2)            //apply bishop pair bonus
                                                       materialImbalence -= 50;
                                                    else if(bBishop == 2 && wBishop < 2)
                                                       materialImbalence += 50;
                                                    //penalty for 3 pawns for a minor piece trade
                                                    if( wPawn - bPawn >= 3 && noBlackMinors - noWhiteMinors > 0  )
                                                       materialImbalence += 75;
                                                    else if(bPawn - wPawn >= 3 && noWhiteMinors - noBlackMinors > 0  )
                                                       materialImbalence -= 75;
                                                    //penalty for rook and pawn for two minors
                                                    if(wPawn - bPawn >= 1 && (wRook - bRook) == 1 && (noBlackMinors - noWhiteMinors) == 2  )
                                                       materialImbalence += 25;
                                                    else if(bPawn - wPawn >= 1 && (bRook - wRook) == 1 && (noWhiteMinors - noBlackMinors) == 2  )
                                                       materialImbalence -= 25;
                                                   //adjustment for queen vs rook, minor and pawn
                                                   if(((wQueen - bQueen) == 1) && ((bRook - wRook) == 1) && ((noBlackMinors - noWhiteMinors) == 1) && ((bPawn - wPawn) == 1))
                                                      materialImbalence -= 75;
                                                   else if(((bQueen - wQueen) == 1) && ((wRook - bRook) == 1) && ((noWhiteMinors - noBlackMinors) == 1) && ((wPawn - bPawn) == 1))
                                                      materialImbalence += 75;
                                                }
                                                
                                                if(wPawn == 0 && bPawn == 0 && materialImbalence < 0 && materialImbalence >= -(Global.values[1] + 50) )
                                                {
                                                   materialImbalence = (materialImbalence * (16 + (Math.min( wBishop, 2) * 10))) / 64; 
                                                }
                                                else if(wPawn == 0 && bPawn == 0 && materialImbalence <= (Global.values[1]+ 50) && materialImbalence > 0 )
                                                {
                                                   materialImbalence = (materialImbalence * (16 + (Math.min( bBishop, 2) * 10))) / 64; 
                                                }
                                                
                                                materialValues[index] = materialImbalence;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     *  method initQueenDist
     * 
     * initializes arrays containing distance between two squares 
     */
    private  final void initQueenDist() {
        for(int i=0;i<64;i++) {
            for(int j=0;j<64;j++) {
                    queenDist[i][j] = Math.max( Math.abs(i/8 - j/8), Math.abs(i%8 - j%8) );
            }		
        }						 
    }
    
    /**
     *  method initQueenDist
     * 
     * initializes arrays containing distance between two squares 
     */
    private  final void initRookDist() {
        for(int i=0;i<64;i++) {
            for(int j=0;j<64;j++) {
                     rookDist[i][j] = Math.abs(i/8 - j/8) + Math.abs(i%8 - j%8);
            }		
        }						 
    }
    
    
   
    /** 
     *  method getQueenDistance
     * 
     * Returns the queen distance between two squres
     * @param to 
     * @param from 
     * 
     */
    public final  int getQueenDistance(int to,int from) {
        return queenDist[to][from];
    }		
    
    /** 
     *  method getRookDistance
     * 
     * Returns the rook distance between two squres
     * @param to 
     * @param from 
     * 
     */
    public final  int getRookDistance(int to,int from) {
        return rookDist[to][from];
    }		
	
    /** 
     *  method callFileWrite
     * 
     * writes all moves to a file
     * 
     *  @ param File f - the file to write to
     */
    public  final void callFileWrite(File f) {
        writer.historyToFile(f);
    }	
    
    /** 
     *  method callFileRead
     * 
     * reads in all moves from a file
     * 
     *  @ param File f - the file to read from
     */
    public  final void callFileRead(File f) {
        writer.readHistory(f);
    }	
    
    /** 
    *  method getPawnHash
    * 
    * returns the first 32 bits of the current pawn hash code
    * 
    */
    public  final long getPawnHash() {
        return pawnHash;
    }
	
    /** 
     *  method getHash
     * 
     * returns the first 32 bits of the current hash code
     * 
     */
    public  final long getHash() {
        return hashValue;
    }
	
    /** 
     *  method generateHash()
     * 
     * generates the first 32 bits of the hash code from scratch
     * 
     * can be used to debug and verify the hash is correct during the search
     * 
     */
    public final long generateHash() {
        long temp2;
        long temp = bitboard;
        long hash = 0;
        while(temp!=0) {
            temp2 = temp&-temp;
            int pos = Long.numberOfTrailingZeros(temp2);
            temp&=~temp2;

            hash ^=pHash[pos][piece_in_square[pos]];
        }			
        hash ^= passantHashW[passant[Global.COLOUR_WHITE]%9];
        hash ^= passantHashB[passant[Global.COLOUR_BLACK]%9];

        hash ^= CastleHash[Global.COLOUR_BLACK][castleFlag[Global.COLOUR_BLACK]];
        hash ^= CastleHash[Global.COLOUR_WHITE][castleFlag[Global.COLOUR_WHITE]];

        if(turn == Global.COLOUR_BLACK) {
            hash^=bHashMove;
        }
        return hash;
    }	

    int GetLazyPieceTotals()
    {
        return pieceTotals[0] + pieceTotals[1] + pieceTotals[2] + pieceTotals[3] + pieceTotals[6] + pieceTotals[7] + pieceTotals[8] + pieceTotals[9];
    }

    int GetPieceTotal(int pieceType)
    {
        return pieceTotals[pieceType];
    }

    /** 
    *  method setBoard
    * 
    * This method places a piece on a square
    * 
    * @param int i - the square 
    * @param int piece - the piece to place on the board
    * 
    */
    public  final void setBoard(int i,int piece) {
        bitboard |= Global.set_Mask[i];
        int type = piece % 6;
        int side = piece / 6;
        piece_in_square[i] = piece;
        pieceListIndices[i] = pieceTotals[piece];
        pieceList[piece][pieceTotals[piece]++] = i;
        noPieces[side]++;
        pieceBits[side][type] |= Global.set_Mask[i];
        pieceBits[side][Global.PIECE_ALL] |= Global.set_Mask[i];
        materialKey += Global.materialOffset[piece];
        totalValue +=Global.values[piece];
        materialAdjust = QueenMaterialAdjustArray[pieceTotals[3]][pieceTotals[9]];
        pawnHash ^= pawnKingHash[i][piece];
    }
	
    /***********************************************************************
            Name:		updateBoard
            Parameters:	int, String
            Returns:	None
            Description:This method updates all of the boards so that on index
                                    int there exists piece String
    ***********************************************************************/	
    private  final void updateBoard(int i,int j) 
    {
        long bit = 1L << i | 1L << j;
        bitboard ^= bit;
        int piece = piece_in_square[j];
        int type = piece % 6;
        int side = piece / 6;
        piece_in_square[i] = piece;
        piece_in_square[j] = -1;
        pieceListIndices[i] = pieceListIndices[j];
        pieceList[piece][pieceListIndices[i]] = i;
        pieceBits[side][type] ^= bit;
        pieceBits[side][Global.PIECE_ALL] ^= bit;
        pawnHash ^= pawnKingHash[i][piece];
        pawnHash ^= pawnKingHash[j][piece];
    }
    
    /***********************************************************************
            Name:		clearBoard
            Parameters:	int, String
            Returns:	None
            Description:On index i String s is removed by this method
    ***********************************************************************/
    public  final void clearBoard(int i) {
        bitboard ^= Global.set_Mask[i];
        int piece = piece_in_square[i];
        int type = piece % 6;
        int side = piece / 6;
        pieceList[piece][pieceListIndices[i]] = pieceList[piece][pieceTotals[piece]-1];
        pieceListIndices[pieceList[piece][--pieceTotals[piece]]] = pieceListIndices[i];
        noPieces[side]--;
        pieceBits[side][type] ^= Global.set_Mask[i];
        pieceBits[side][Global.PIECE_ALL] ^= Global.set_Mask[i];
        materialKey -= Global.materialOffset[piece];
        totalValue -=Global.values[piece];
        piece_in_square[i] = -1;
        materialAdjust = QueenMaterialAdjustArray[pieceTotals[3]][pieceTotals[9]];
        pawnHash ^= pawnKingHash[i][piece];
    }
	
    /** 
     *  method getMaterialScore
     * 
     * This method returns the material evaluation from the perspective of a certain side
     * The value Global.materialDraw is returned to flag draw scores, thus this method is only to be called during evaluation
     * 
     * @param int side - the side whose perspective the evaluation is for (-1 white, 1 black)
     * 
     */
    public  final int GetRawMaterialScore() {
         return materialValues[materialKey] + materialAdjust;
    }

    /**
     *  method getMaterialScore
     *
     * This method returns the material evaluation from the perspective of a certain side
     * The value Global.materialDraw is returned to flag draw scores, thus this method is only to be called during evaluation
     *
     * @param int side - the side whose perspective the evaluation is for (-1 white, 1 black)
     *
     */
    public  final int GetMaterialScore(int side) {
        if(materialValues[materialKey] == Global.materialDraw)
        {
            return 0;
        }
        else
        {
            return (-1 + side * 2) * (materialValues[materialKey] + materialAdjust);  
        }
    }
    
    /** 
     *  method getTotalValue
     * 
     * This method returns the total material value on the board
     *
     */
    public  final int getTotalValue() {
        return totalValue;
    }

    /** 
     *  method Long.numberOfTrailingZeros
     * 
     * This method will return a board position given a proper long using the debruijn technique
     * 
     * @param long pos - the position represented by 1 set bit in the 64 bit long variable
     *
     */
    public int getPos(long pos) {
        return Long.numberOfTrailingZeros(pos);
   }

    /** 
     *  method getAttackBoard
     * 
     * This method will return a bitset repesenting all squares attacked by the piece on a give square
     * 
     * @param int i - the position where the attacking piece is 
     * 
     * @return long - the bitset representing all attacked sqaures
     *
     */
    public  final long getAttackBoard(int i) {
        switch(piece_in_square[i]) {
            case 0:
                return getMagicRookMoves(i);
            case 1:
                return KnightMoveBoard[i];
            case 2:
                return getMagicBishopMoves(i);	
            case 3:
                return getQueenMoves(i);
            case 4:
                return kingMoveTable[i];
            case 5:
                return PawnAttackBoard[Global.COLOUR_WHITE][i];
            case 6:
                return getMagicRookMoves(i);
            case 7:
                return KnightMoveBoard[i];
            case 8:
                return getMagicBishopMoves(i);
            case 9:
                return getQueenMoves(i);
            case 10:
                return kingMoveTable[i];
            case 11:
                return PawnAttackBoard[Global.COLOUR_BLACK][i];
            default:
                return 0;
        } 
    }
    
    public  final long getAttackBoard(int i, long bits) {
        switch(piece_in_square[i]) {
            case 0:
                return getMagicRookMoves(i, bits);
            case 1:
                return KnightMoveBoard[i];
            case 2:
                return getMagicBishopMoves(i, bits);	
            case 3:
                return getMagicBishopMoves(i, bits) | getMagicRookMoves(i, bits);
            case 4:
                return kingMoveTable[i];
            case 5:
                return PawnAttackBoard[Global.COLOUR_WHITE][i];
            case 6:
                return getMagicRookMoves(i, bits);
            case 7:
                return KnightMoveBoard[i];
            case 8:
                return getMagicBishopMoves(i, bits);
            case 9:
                return getMagicBishopMoves(i, bits) | getMagicRookMoves(i, bits);
            case 10:
                return kingMoveTable[i];
            case 11:
                return PawnAttackBoard[Global.COLOUR_BLACK][i];
            default:
                return 0;
        } 
    }

    /** 
     *  method getAttack2(int i)
     * 
     * This method will a bitset representing all pieces which attack a given square
     * 
     * @param int i - the position of the square being attacked
     * 
     * @return long - the bitset representing pieces attacking the square
     *
     */	
    public  final long getAttack2(int i) {
        long attack = getMagicBishopMoves(i) & ( pieceBits[Global.COLOUR_BLACK][Global.PIECE_BISHOP] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_BISHOP] 
                    | pieceBits[Global.COLOUR_BLACK][Global.PIECE_QUEEN] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_QUEEN] );
        attack |= getMagicRookMoves(i) & ( pieceBits[Global.COLOUR_BLACK][Global.PIECE_ROOK] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_ROOK] 
                    | pieceBits[Global.COLOUR_BLACK][Global.PIECE_QUEEN] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_QUEEN] );
        attack |= KnightMoveBoard[i] & ( pieceBits[Global.COLOUR_BLACK][Global.PIECE_KNIGHT] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_KNIGHT] );
        attack |= kingMoveTable[i] & ( pieceBits[Global.COLOUR_BLACK][Global.PIECE_KING] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_KING] );
        attack |= PawnAttackBoard[Global.COLOUR_WHITE][i] & pieceBits[Global.COLOUR_BLACK][Global.PIECE_PAWN];
        attack |= PawnAttackBoard[Global.COLOUR_BLACK][i] & pieceBits[Global.COLOUR_WHITE][Global.PIECE_PAWN];
        return attack;
    }

    public  final long GetSlideAttacks2SEE(int i)
    {
        return getMagicRookMoves(i) &  ( pieceBits[Global.COLOUR_BLACK][Global.PIECE_ROOK] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_ROOK] 
                | pieceBits[Global.COLOUR_BLACK][Global.PIECE_QUEEN] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_QUEEN] )
                | getMagicBishopMoves(i) & ( pieceBits[Global.COLOUR_BLACK][Global.PIECE_BISHOP] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_BISHOP] 
                | pieceBits[Global.COLOUR_BLACK][Global.PIECE_QUEEN] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_QUEEN] );
    }
    
    /** 
     *  method getMoves2(int i)
     * 
     * This method will a bitset representing all pieces which can move to a square
     * (similar to getAttack2, but no king moves generated here, and pawn pushes, not attacks generated here)
     * (used to generate moves to block an attack on a king)
     * 
     * @param int i - the position of the square to move to
     * 
     * @return long - the bitset representing pieces moving to the square
     *
     */	
    public  final long getMovesTo(int i) {
        long temp = getMagicBishopMoves(i);
        long movers = temp & ( pieceBits[Global.COLOUR_BLACK][Global.PIECE_BISHOP] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_BISHOP] 
                    | pieceBits[Global.COLOUR_BLACK][Global.PIECE_QUEEN] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_QUEEN] );
        temp = getMagicRookMoves(i);
        movers |= temp & ( pieceBits[Global.COLOUR_BLACK][Global.PIECE_ROOK] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_ROOK] 
                    | pieceBits[Global.COLOUR_BLACK][Global.PIECE_QUEEN] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_QUEEN] );
        temp = KnightMoveBoard[i];
        movers |= temp & ( pieceBits[Global.COLOUR_BLACK][Global.PIECE_KNIGHT] | pieceBits[Global.COLOUR_WHITE][Global.PIECE_KNIGHT] );
        if(piece_in_square[i] == -1) {
            if(i/8 >0) {
                temp = Global.set_Mask[i-8];
                movers |= (temp & pieceBits[Global.COLOUR_WHITE][Global.PIECE_PAWN]);
                if((i/8 == 3) && (piece_in_square[i-8] == -1)) {
                    temp = Global.set_Mask[i-16];
                    movers |= (temp & pieceBits[Global.COLOUR_WHITE][Global.PIECE_PAWN] );
                }
            }
            if(i/8<7) {
                temp = Global.set_Mask[i+8];
                movers |= (temp & pieceBits[Global.COLOUR_BLACK][Global.PIECE_PAWN] );
                if((i/8 == 4)&& (piece_in_square[i+8] == -1)) {
                    temp = Global.set_Mask[i+16];
                    movers |= (temp & pieceBits[Global.COLOUR_BLACK][Global.PIECE_PAWN] );
                }
            }
        }
        return movers;
    }	

    /** 
     *  method isAttacked(int i)
     * 
     * This method determine if a given square occupied by a piece is attacked
     * 
     * @param int i - the position of the square  piece
     * @param int side - side who has piece 
     * @return boolean - is the square attacked?
     *
     */	
    public  final boolean isAttacked(int side, int i) {
        int eSide = side ^ 1;
        return ((PawnAttackBoard[side][i] & pieceBits[eSide][Global.PIECE_PAWN] ) != 0L ||
                (KnightMoveBoard[i] & pieceBits[eSide][Global.PIECE_KNIGHT] ) != 0L ||
		(kingMoveTable[i] & pieceBits[eSide][Global.PIECE_KING] ) != 0L ||
		(getMagicBishopMoves(i) & ( pieceBits[eSide][Global.PIECE_BISHOP] | pieceBits[eSide][Global.PIECE_QUEEN])) != 0L ||
		(getMagicRookMoves(i) & ( pieceBits[eSide][Global.PIECE_ROOK] | pieceBits[eSide][Global.PIECE_QUEEN] )) != 0L );
		
    }
    
    public final CheckInfo GetEmptyCheckInfo() {
        CheckInfo info = new CheckInfo();
        info.checksReady = false;
        info.pinnedReady = false;
        return info;
    }
    
    //the side on move
    public final void GetCheckInfo(int side, CheckInfo info) {
        info.pinnedReady = true;
        info.pinned = GetPinnedPieces( side, false );
    }
    
    public final void GetGivesCheckInfo(int side, CheckInfo info) {
        info.checksReady = true;
        int enemyKing = pieceList[4 + (side^1) * 6][0];
        info.checkSquares[Global.PIECE_PAWN] = PawnAttackBoard[side^1][enemyKing];
        info.checkSquares[Global.PIECE_BISHOP] = getMagicBishopMoves( enemyKing );
        info.checkSquares[Global.PIECE_KNIGHT] = getKnightMoves( enemyKing );
        info.checkSquares[Global.PIECE_ROOK] = getMagicRookMoves( enemyKing );
        info.checkSquares[Global.PIECE_QUEEN] = getMagicBishopMoves( enemyKing ) | getMagicRookMoves( enemyKing );
        info.checkSquares[Global.PIECE_KING] = 0L;
        info.dcCandidates = GetPinnedPieces( side, true );
    }
    
    public final boolean MoveGivesCheck(int side, int move, CheckInfo info ) {
        if( !info.checksReady) {
            GetGivesCheckInfo(side, info);
        }
        
        int from = MoveFunctions.getFrom( move );
        int to = MoveFunctions.getTo( move );
        int type = MoveFunctions.moveType( move );
        int piece = piece_in_square[move&63]%6;
        int enemyKing = pieceList[Global.PIECE_KING + (side^1) * 6][0];
        if( (info.checkSquares[piece] & 1L << to) != 0 ) {
            return true;
        }
        
        else if( (info.dcCandidates & (1L << from)) != 0  && ( piece < Global.PIECE_KING  ||
                (((Global.mask_between[enemyKing][from] & (1L << to)) == 0 )
                && (Global.mask_between[enemyKing][to] & (1L << from)) == 0) ))
        {
            return true;
        }
        
        switch( type ) {
            case( Global.PROMO_Q ):
                return ( (getMagicBishopMoves( to, bitboard ^ (1L << from) ) & (1L << enemyKing)) != 0) || 
                        ( (getMagicRookMoves( to, bitboard ^ (1L << from) ) & (1L << enemyKing)) != 0);
           
            case( Global.PROMO_R ):
                return ( (getMagicRookMoves( to, bitboard ^ (1L << from) ) & (1L << enemyKing)) != 0 );
                
            case( Global.PROMO_B ):
                return ( (getMagicBishopMoves( to, bitboard ^ (1L << from) ) & (1L << enemyKing)) != 0 );
              
            case( Global.PROMO_N ):
                return ( (KnightMoveBoard[to] & (1L << enemyKing)) != 0 );
                
            case( Global.EN_PASSANT_CAP ):
                int enemyPawnPush = side == Global.COLOUR_WHITE ? -8 : 8;
                int capSquare = to + enemyPawnPush;
                long occ = bitboard ^ (1L << capSquare) ^ (1L << from) ^ (1L << to);

                return ( ((getMagicBishopMoves(enemyKing, occ) & ( pieceBits[side][Global.PIECE_BISHOP] | pieceBits[side][Global.PIECE_QUEEN] ))
                        | ( getMagicRookMoves(enemyKing, occ) & ( pieceBits[side][Global.PIECE_ROOK] | pieceBits[side][Global.PIECE_QUEEN] ))) != 0 );

            case( Global.SHORT_CASTLE ):
                occ = bitboard ^ ( 1L << to ) ^ ( 1L << (to-1)) ^ ( 1L << (to+1)) ^ ( 1L << (from));
                return ( getMagicRookMoves( enemyKing, occ) & (1L << (to-1))) != 0;
                
            case( Global.LONG_CASTLE ):
                 occ = bitboard ^ ( 1L << to ) ^ ( 1L << (to-2)) ^ ( 1L << (to+1)) ^ ( 1L << (from));
                return ( getMagicRookMoves( enemyKing, occ) & (1L << (to+1))) != 0;
            default:
                return false;
        } 
    }
    
    
    public final boolean CheckMove(int side, int move, CheckInfo info) {
        if( !info.pinnedReady ) {
            GetCheckInfo(side, info);
        }
        int from = MoveFunctions.getFrom( move );
        int to = MoveFunctions.getTo( move );
        int type = MoveFunctions.moveType( move );
        int king = pieceList[4 + side*6][0];
        int piece = piece_in_square[from];
        
        if( type == Global.EN_PASSANT_CAP ) {
            int enemyPawnPush = side == Global.COLOUR_WHITE ? -8 : 8;
            int capSquare = to + enemyPawnPush;
            long occ = bitboard ^ (1L << from) ^ (1L << to) ^ (1L << capSquare);
            
            return ( ((getMagicBishopMoves(king, occ) & ( pieceBits[side^1][Global.PIECE_BISHOP] | pieceBits[side^1][Global.PIECE_QUEEN] ))
                    | ( getMagicRookMoves(king, occ) & ( pieceBits[side^1][Global.PIECE_ROOK] | pieceBits[side^1][Global.PIECE_QUEEN] ))) == 0 );
        }
        else if( piece % 6 == Global.PIECE_KING) {
            return ( getAttack2( to ) & pieceBits[side^1][Global.PIECE_ALL]) == 0; 
        }
            
        return( ( info.pinned == 0) || ((info.pinned & (1L << from)) == 0) || ((Global.mask_between[king][from] & (1L << to)) != 0 )
                || (Global.mask_between[king][to] & (1L << from)) != 0);          
    }
    
    private final boolean MoreThanOne(long bits) {
        return( (bits & (bits-1)) != 0);
    }
    
    private final long GetPinnedPieces(int side, boolean bDiscoverChecks) {
        
        long pinned = 0;
        int sideToMove = side;
        if( bDiscoverChecks) {
            side ^= 1;
        }
        
        long pinners = pieceBits[side^1][Global.PIECE_ALL];
        int kingSquare = pieceList[4 + 6 * side][0];
        
        pinners &= ((getMagicRookMoves(kingSquare, 0) & ( pieceBits[side^1][Global.PIECE_ROOK] | pieceBits[side^1][Global.PIECE_QUEEN]) )
                | (getMagicBishopMoves(kingSquare, 0) & ( pieceBits[side^1][Global.PIECE_BISHOP] | pieceBits[side^1][Global.PIECE_QUEEN]) ) ); 
    
        while( pinners != 0 )
        {
            int pinPos = Long.numberOfTrailingZeros(pinners);
            pinners ^= 1L << pinPos;
            long pieces_between = Global.mask_between[kingSquare][pinPos] & bitboard;
            if( pieces_between != 0 && !MoreThanOne(pieces_between) && (pieceBits[sideToMove][Global.PIECE_ALL] & pieces_between) != 0 ) {
                pinned |= pieces_between;
            }
        }
        
        return pinned;
    }
    
    /***********************************************************************
            Name:		getPiecesInSquare
            Parameters:	None
            Returns:	int[]
            Description:This method returns an integer array representing the 
                                    status of the chessboard
    * array entries contain no from 0 to 11 for pieces, -1 means no piece
    ***********************************************************************/	
    public  final int[] getPiecesInSquare() {	
        return piece_in_square;
    }

    /** 
     *  method getPassant()
     * 
     * This accessor method returns white's passant square
     * 
     * @return int - passant sqaure position
     *
     */	
    public  final int getPassant(int side) {			//this method gets whtie passant square
        return passant[side];
    }
    
    /**
     *  method getPawnAttack(int side, int index)
     * 
     * This method returns a bitset representing all the squares attacked by a pawn
     * 
     * @param int - side of attacking pawns
     * @param int - position of pawn
     * @return long - bitset of attacks for pawn
     *
     */
    public  final long getPawnAttack(int side, int index) {
        return PawnAttackBoard[side][index];
    }
    
     public  final long getPawnMovesNoAttack(int side, int index) {   //get White Pawn Moves Based on index of pawn
        long moves = PawnMoveBoard[side][index];
        if( piece_in_square[index + Global.forwardRank[side]] != -1 )//bitboard & Global.set_Mask[index + Global.forwardRank[side]]) != 0)
            moves = 0;
        else if( Global.RelativeRanks[side][index/8] == 1 && piece_in_square[index + 2 * Global.forwardRank[side]] != -1)
            moves = Global.set_Mask[index + Global.forwardRank[side]];
        return moves;
     }
    /***********************************************************************		
            Name:		getPawnMoves
            Parameters:	int
            Returns:	BitSet
            Description:This method returns a BitSet representing all of the 
                                    white pawn moves
    ***********************************************************************/		
    public  final long getPawnMoves(int side, int index) {   //get White Pawn Moves Based on index of pawn
        long moves = PawnMoveBoard[side][index];
        if( piece_in_square[index + Global.forwardRank[side]] != -1 )//bitboard & Global.set_Mask[index + Global.forwardRank[side]]) != 0)
            moves = 0;
        else if( Global.RelativeRanks[side][index/8] == 1 && piece_in_square[index + 2 * Global.forwardRank[side]] != -1)
            moves = Global.set_Mask[index + Global.forwardRank[side]];
        return moves | (PawnAttackBoard[side][index] & (pieceBits[side ^ 1][Global.PIECE_ALL] | Global.set_Mask[passant[side ^ 1]]));
    }

    /** 
     *  method getKnightMoves(int index)
     * 
     * This method returns a bitset representing all the squares attacked by knight
     * 
     * @param int - position of knight
     * 
     * @return long - bitset of attacks for knight
     *
     */
    public  final long getKnightMoves(int index) {
        return KnightMoveBoard[index];
    }
	
    public final long getKingMoves(int index)
    {
        return kingMoveTable[index];
    }

    public final long getKingCastle(int rank)
    {
        return kingCastleTable[rank];
    }
   
    /***********************************************************************		
            Name:		getMagicRookMoves
            Parameters:	int
            Returns:	long
            Description:This method returns a BitSet representing all of the 
                                    RookMoves moves from square int
    ***********************************************************************/
    public  final long getMagicRookMoves(int index) {
        long occ = bitboard & rMask[index];
        occ *= rMagics[index];
        occ >>>= (64-rookShift[index]);
        return rookTable[index][(int)(occ)];	
    }
    
     public  final long getMagicRookMoves(int index, long bits) {
        long occ = bits & rMask[index];
        occ *= rMagics[index];
        occ >>>= (64-rookShift[index]);
        return rookTable[index][(int)(occ)];	
    }
	
    /***********************************************************************		
            Name:		getMagicBishopMoves
            Parameters:	int
            Returns:	long
            Description:This method returns a BitSet representing all of the 
                                    BishopMoves moves from square int
    ***********************************************************************/

    public  final long getMagicBishopMoves(int index) {
        long occ = bitboard & bMask[index];
        occ *= bMagics[index];
        occ >>>= (64-bishopShift[index]);
        return bishopTable[index][(int)(occ)];	
    }
    
    public  final long getMagicBishopMoves(int index, long bits) {
        long occ = bits & bMask[index];
        occ *= bMagics[index];
        occ >>>= (64-bishopShift[index]);
        return bishopTable[index][(int)(occ)];	
    }
	
    /***********************************************************************		
            Name:		getQueenMoves
            Parameters:	int
            Returns:	BitSet
            Description:This method returns a BitSet representing all of the 
                                    queen moves from square int
    ***********************************************************************/			
    public  final long getQueenMoves(int index) {
        return getMagicBishopMoves(index) | getMagicRookMoves(index);
    }
  	
    /** 
     *  method switchTurn()
     * 
     * This method switches the player on move
     * 
     */
    public  final void SwitchTurn() {
        hashValue ^= bHashMove;
        turn ^= 1;
    }
    
    /** 
     *  method getMaxNumberOfPieces()
     * 
     * This method returns the maximum number of pieces for either side
     * 
     * @return int - the max number of pieces
     *
     */
    public  final int getMaxNumberOfPieces() {
        return Math.max(noPieces[0], noPieces[1]);
    }
    
    /** 
     *  method getMinNumberOfPieces()
     * 
     * This method returns the minimum number of pieces for either side
     * 
     * @return int - the minimum number of pieces
     *
     */
    public  final int getMinNumberOfPieces() {
        return Math.min(noPieces[0], noPieces[1]);
    }
 	
    /** 
     *  method getNumberOfPieces()
     * 
     * This method returns the  number of pieces for a specific side
     * 
     * @param side - the side to get the number of pieces for
     * @return int - the number of pieces
     *
     */
    public  final int getNumberOfPieces(int side) {
        return noPieces[side];
    }		
   
   /**
     *  method AddRepetitionRoot()
     *
     * This method adds a position to the repetition table and returns the number of previous times this table has been added
     * This version of Add Repetition is to be used when the best move is being made after a search as
     * this version will reset the repetition table if the move is not reversable
     *
     * @return - the number of repetitions of the position added
     *
     */
    public int AddRepetitionRoot() {
        iCurrentMovesDepth = 0;
        if(reversable) {
           zorbistDepth++;
           zorbistHistory[zorbistDepth] = hashValue;
           lastReversableMove[zorbistDepth] = lastReversableMove[zorbistDepth - 1];

           if((zorbistDepth - lastReversableMove[zorbistDepth]) >= 100) //draw by 50 move rule
              return 5;

           //check for repetitions
           int count = 1;
           for(int i = zorbistDepth - 2; i >= lastReversableMove[zorbistDepth]; i -= 2) {
              if(zorbistHistory[i] == hashValue)        //return true on first repetition
                 count++;
           }
           return count;

        } else {
           zorbistDepth = 1;
           zorbistHistory[zorbistDepth] = hashValue;
           lastReversableMove[zorbistDepth] = lastReversableMove[zorbistDepth - 1];
           return 1;
        }
   }

   /**
     *  method AddRepetition()
     *
     * This method adds a position to the repetition table and returns the number of previous times this table has been added
     * This version of Add Repetition is to be used during the recursive search
     *
     * @return - the number of repetitions of the position added
     *
     */
   public int AddRepetition() {
      
      if(reversable) {
         zorbistDepth++;
         zorbistHistory[zorbistDepth] = hashValue;
         lastReversableMove[zorbistDepth] = lastReversableMove[zorbistDepth - 1];
         
         if((zorbistDepth - lastReversableMove[zorbistDepth]) >= 100)               //draw by 50 move rule
            return 5;
         
         //check for repetitions
         int count = 1;
         for(int i = zorbistDepth - 2; i >= lastReversableMove[zorbistDepth]; i -= 2) {
            if(zorbistHistory[i] == hashValue)                                
               count++;
         }
         
         return count;
      
      } else {
         zorbistDepth++;
			zorbistHistory[zorbistDepth] = hashValue;
         lastReversableMove[zorbistDepth] = zorbistDepth;
         return 1;
      }
   }


    public final int GetBoardMove(int index)
    {
        return boardMoves[index];
    }

    /**
     *  method AddMove()
     *
     * This method adds a move to the array of moves made in the game so far
     *
     * @param move - the move to be added
     */
    public final void AddMove(int move) {
        int to = (move >> 6) & 63;//MoveFunctions.getTo(move);
        int from = move & 63; //MoveFunctions.getFrom(move);
        boardMoves[moveCount] = move;
        writer.addHistory(to,from,moveCount);
    }

    /**
     *  method GetMoveAtDepth()
     *
     * This method returns the move made at the requested depth in the current line of the recursive search
     *
     * @param depth - the requested depth for the move
     *
     * @return int - the move made
     *
     */
    public int GetMoveAtDepth(int depth)   {
        return arrCurrentMoves[depth];
    }


    public void ResetMovesDepth() {
        iCurrentMovesDepth = 0;
    }

    /**
     *  method MakeMove()
     * 
     * This method performs all necessary tasks to make a chess move during the search 
     * 
     * @param int move - 32 bits with move information stuffed in
     * @param boolean add - true if this move is being made on the board and not just the search
     * @param boolean board - indicates to store as a repetition
     * 
     * 
     * @return int - the number of repetitions for this position
     *
     */

    public final int MakeMove(int move, boolean board) {

        int to = (move >> 6) & 63;
        int from = move & 63;
        int type = (move >> 12) & 15;

        arrCurrentMoves[iCurrentMovesDepth] = move;
        iCurrentMovesDepth++;

        hashHistory[moveCount] = hashValue;

        flagHistory[moveCount] = (passant[Global.COLOUR_WHITE]) | (passant[Global.COLOUR_BLACK]) << 6 
                | castleFlag[Global.COLOUR_WHITE] << 12 | castleFlag[Global.COLOUR_BLACK] << 15 
                | turn << 18 | (piece_in_square[to] + 1) << 19;

        moveCount++;

        int oldPassantW = passant[Global.COLOUR_WHITE];
        int oldPassantB = passant[Global.COLOUR_BLACK];
      
        passant[Global.COLOUR_WHITE] = NO_PASSANT_WHITE;
        passant[Global.COLOUR_BLACK] = NO_PASSANT_BLACK;

        switch(type) {

            case(Global.ORDINARY_MOVE):
            {
                reversable = piece_in_square[from] % 6 != 5;
            }
            break;

            case(Global.ORDINARY_CAPTURE):
            {
                reversable = false;
                hashValue ^= pHash[to][piece_in_square[to]];
                clearBoard(to); 
            }
            break;

            case(Global.SHORT_CASTLE):
            {
                reversable = false;
                hashValue ^= CastleHash[turn][castleFlag[turn]];
                castleFlag[turn] = Global.CASTLED;
                hashValue ^= CastleHash[turn][castleFlag[turn]];
                updateBoard(5 + turn * 56, 7 + turn * 56);
                hashValue ^= pHash[5 + turn * 56][Global.pieceAdd[turn]];
                hashValue ^= pHash[7 + turn * 56][Global.pieceAdd[turn]];
            }
            break;

            case(Global.DOUBLE_PAWN):
            {
                reversable = false;
                passant[turn] = to + Global.behindRank[turn];
            }
            break;

            case(Global.LONG_CASTLE):
            {
                reversable = false;
                hashValue ^= CastleHash[turn][castleFlag[turn]];
                castleFlag[turn] = Global.CASTLED;
                hashValue ^= CastleHash[turn][castleFlag[turn]];
                updateBoard(3 + turn * 56, 0 + turn * 56);
                hashValue ^= pHash[3 + turn * 56][Global.pieceAdd[turn]];
                hashValue ^= pHash[0 + turn * 56][Global.pieceAdd[turn]];
            }
            break;

            case(Global.PROMO_Q):
            {
                int thePiece =  piece_in_square[from];
                reversable = false;
                clearBoard(from);
                hashValue ^= pHash[from][thePiece];
                setBoard(from, thePiece-2);
                hashValue ^= pHash[from][thePiece-2];
                int cP = piece_in_square[to];
                if(cP != -1) {
                    UpdateCastleFlags(thePiece, to , from);
                    clearBoard(to);
                    hashValue ^= pHash[to][cP];
                }
            }
            break;


            case(Global.PROMO_N):
            {
                int thePiece =  piece_in_square[from];
                reversable = false;
                clearBoard(from);
                hashValue ^= pHash[from][thePiece];
                setBoard(from, thePiece-4);
                hashValue ^= pHash[from][thePiece-4];
                int cP = piece_in_square[to];
                if(cP != -1) {
                    UpdateCastleFlags(thePiece, to , from);
                    clearBoard(to);
                    hashValue ^= pHash[to][cP];
                }
            }
            break;
            case(Global.PROMO_R):
            {
                int thePiece =  piece_in_square[from];
                reversable = false;
                clearBoard(from);
                hashValue ^= pHash[from][thePiece];
                setBoard(from, thePiece-5);
                hashValue ^= pHash[from][thePiece-5];
                int cP = piece_in_square[to];
                if(cP != -1) {
                    UpdateCastleFlags(thePiece, to , from);
                    clearBoard(to);
                    hashValue ^= pHash[to][cP];
                }
            }
            break;
            case(Global.PROMO_B):
            {
                int thePiece =  piece_in_square[from];
                reversable = false;
                clearBoard(from);
                hashValue ^= pHash[from][thePiece];
                setBoard(from, thePiece-3);
                hashValue ^= pHash[from][thePiece-3];
               int cP = piece_in_square[to];
                if(cP != -1) {
                    UpdateCastleFlags(thePiece, to , from);
                    clearBoard(to);
                    hashValue ^= pHash[to][cP];
                }
            }
            break;

            case(Global.EN_PASSANT_CAP):
            {
                reversable = false;
                clearBoard(to + Global.behindRank[turn]);
                hashValue ^= pHash[to + Global.behindRank[turn]][11 - turn * 6];
            }
            break;

            case(Global.MOVE_KING_LOSE_CASTLE):
            {
                reversable = false;
                
                hashValue ^= CastleHash[turn][castleFlag[turn]];
                castleFlag[turn] = Global.NO_CASTLE;
                hashValue ^= CastleHash[turn][castleFlag[turn]];
               
                int cP = piece_in_square[to];
                if(cP != -1)
                {
                    clearBoard(to);
                    hashValue ^= pHash[to][cP];
                }
            }
            break;

            case(Global.CAPTURE_ROOK_LOSE_CASTLE):
            {
                int thePiece =  piece_in_square[from];
                UpdateCastleFlags(thePiece, to , from);
                reversable = false;
                hashValue ^= pHash[to][piece_in_square[to]];
                clearBoard(to);
            }
            break;

            case(Global.MOVE_ROOK_LOSE_CASTLE):
            {
                int thePiece =  piece_in_square[from];
                reversable = false;
                UpdateCastleFlags(thePiece, to , from);
                int cP = piece_in_square[to];
                if(cP != -1)
                {
                    clearBoard(to);
                    hashValue ^= pHash[to][cP];
                }
            }
            break;
        }

        hashValue ^= pHash[from][piece_in_square[from]];
        hashValue ^= pHash[to][piece_in_square[from]];
        updateBoard(to,from);

        if(oldPassantW != passant[Global.COLOUR_WHITE])
        {
            hashValue ^= passantHashW[oldPassantW%9];
            hashValue ^= passantHashW[passant[Global.COLOUR_WHITE]%9];
        }

        if(oldPassantB != passant[Global.COLOUR_BLACK])
        {
            hashValue ^= passantHashB[oldPassantB%9];
            hashValue ^= passantHashB[passant[Global.COLOUR_BLACK]%9];
        }

        SwitchTurn();

        //if(hashValue != generateHash()) {
        //	System.out.println("info string generatehash is +"+generateHash());
        //}

        if( board )
            return AddRepetition();
        else
            return 1;
    }

    public final void UpdateCastleFlags(int thePiece, int to, int from) {

        if(castleFlag[Global.COLOUR_WHITE] > Global.CASTLED) {
            if(to == 7 || from == 7) {
                hashValue ^= CastleHash[Global.COLOUR_WHITE][castleFlag[Global.COLOUR_WHITE]];
                castleFlag[Global.COLOUR_WHITE] &= Global.LONG_CASTLE;
                hashValue ^= CastleHash[Global.COLOUR_WHITE][castleFlag[Global.COLOUR_WHITE]];
            } else if(to == 0 || from == 0) {
                hashValue ^= CastleHash[Global.COLOUR_WHITE][castleFlag[Global.COLOUR_WHITE]];
                castleFlag[Global.COLOUR_WHITE] &= Global.SHORT_CASTLE;
                hashValue ^= CastleHash[Global.COLOUR_WHITE][castleFlag[Global.COLOUR_WHITE]];
            }
        }

        if(castleFlag[Global.COLOUR_BLACK] > Global.CASTLED) {
            if(to == 63 || from == 63) {
                hashValue ^= CastleHash[Global.COLOUR_BLACK][castleFlag[Global.COLOUR_BLACK]];
                castleFlag[Global.COLOUR_BLACK] &= Global.LONG_CASTLE;
                hashValue ^= CastleHash[Global.COLOUR_BLACK][castleFlag[Global.COLOUR_BLACK]];
            } else if(to == 56 || from == 56) {
                hashValue ^= CastleHash[Global.COLOUR_BLACK][castleFlag[Global.COLOUR_BLACK]];
                castleFlag[Global.COLOUR_BLACK] &= Global.SHORT_CASTLE;
                hashValue ^= CastleHash[Global.COLOUR_BLACK][castleFlag[Global.COLOUR_BLACK]];
            }
        }
    }

   /**
     *  method getDraw()
     * 
     * This accessor method returns the 50 move draw count variable
     * 
     * @return int - 50 move draw count var
     *
     */
    public  final int getDraw() {
        return drawCount;
    }
    
    /** 
     *  method getCount()
     * 
     * This accessor method returns the number of moves made
     * 
     * @return int - the move count
     *
     */
    public  final int getCount() {
        return moveCount;	
    }	
	
    /** 
     *  method getTurn()
     * 
     * This accessor method returns a var indicating whose turn it is to move
     * (-1 white, 1 black)
     * 
     * @return int - whose turn is it?
     *
     */
    public  final int getTurn() {
        return turn;
    }	
    
    /** 
     *  method unMake()
     * 
     * This method performs all necessary tasks to undo a chess move during the search 
     * 
     * @param int move - 32 bits with move information stuffed in
     * @param boolean add - true if this move is being made on the board and not just the search
     * @param boolean board - indicates to remove as a repetition
     * 
     * 
     * @return int - the number of repetitions for this position
     *
     */
    public final void UnMake(int move, boolean board) {
        if(board)
            zorbistDepth--;	
        moveCount--;
        turn = (flagHistory[moveCount] >> 18) & 1;
        castleFlag[Global.COLOUR_WHITE] = (flagHistory[moveCount] >> 12) & 7;
        castleFlag[Global.COLOUR_BLACK] = (flagHistory[moveCount] >> 15) & 7;
        iCurrentMovesDepth--;
        int to = (move >> 6) & 63;
        int from = move & 63;

        switch( (move >> 12) & 15) {
            case(Global.ORDINARY_MOVE):
                updateBoard(from, to);
            break;

            case(Global.ORDINARY_CAPTURE):
                updateBoard(from, to);
                setBoard(to, (flagHistory[moveCount] >>19) - 1);
            break;

            case(Global.SHORT_CASTLE):
            {
                updateBoard(from, to);
                updateBoard(7 + turn * 56, 5 + turn * 56);
            }
            break;

            case(Global.DOUBLE_PAWN):
            {
                updateBoard(from, to);
            }
            break;

            case(Global.PROMO_Q):
            {
                int piece = 5 + turn * 6;
                clearBoard(to);
                setBoard(to, piece);
                updateBoard(from, to);
                int capPiece = (flagHistory[moveCount] >>19) - 1;
                if(capPiece != -1)
                    setBoard(to, capPiece);
            }
            break;

            case(Global.LONG_CASTLE):
            {
                updateBoard(from, to);
                updateBoard(0 + turn * 56, 3 + turn * 56);
            }
            break;

            case(Global.PROMO_N):
            {
                int piece = 1 + turn * 6;
                clearBoard(to);
                setBoard(to, piece);
                updateBoard(from, to);
                int capPiece = (flagHistory[moveCount] >>19) - 1;
                if(capPiece != -1)
                    setBoard(to, capPiece);
            }
            break;

            case(Global.PROMO_R):
            {
                int piece = 0 + turn * 6;
                clearBoard(to);
                setBoard(to, piece);
                updateBoard(from, to);
                int capPiece = (flagHistory[moveCount] >>19) - 1;
                if(capPiece != -1)
                    setBoard(to, capPiece);
            }
            break;

            case(Global.PROMO_B):
            {
                int piece = 2 + turn * 6;
                clearBoard(to);
                setBoard(to, piece);
                updateBoard(from, to);
                int capPiece = (flagHistory[moveCount] >>19) - 1;
                if(capPiece != -1)
                    setBoard(to, capPiece);
            }
            break;

            case(Global.EN_PASSANT_CAP):
            {
                updateBoard(from, to);
                setBoard(to + Global.behindRank[turn], 11 - turn * 6);
            }
            break;

            case(Global.MOVE_KING_LOSE_CASTLE):
            {
                updateBoard(from, to);
                int capPiece = (flagHistory[moveCount] >>19) - 1;
                if(capPiece != -1)
                    setBoard(to, capPiece);
            }
            break;

            case(Global.CAPTURE_ROOK_LOSE_CASTLE):
            {
                updateBoard(from, to);
                setBoard(to, (flagHistory[moveCount] >>19) - 1);
            }
            break;

            case(Global.MOVE_ROOK_LOSE_CASTLE):
            {
                updateBoard(from, to);
                int capPiece = (flagHistory[moveCount] >>19) - 1;
                if(capPiece != -1)
                    setBoard(to, capPiece);
            }
            break;
        }
        hashValue = hashHistory[moveCount];
        passant[Global.COLOUR_WHITE] = (flagHistory[moveCount] & 63);
        passant[Global.COLOUR_BLACK] = ((flagHistory[moveCount] >> 6) & 63);

        // if(hashValue != generateHash()) {
        //    System.out.println("info string generatehash is +"+generateHash());
        // }
    }			
}