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

/*
 * HistoryWriter.java
 *
 * This class handles the follwoing:
 * -processing a list of moves and setting the board to the position specified by the sequence
 * -converting an internally represented move to a String which represents a UCI move
 * 
 */

public final class HistoryWriter {
	
    /** instance of singleton Board class */
    private static Board chessBoard;

    private static Engine search;
  
    public HistoryWriter(Engine pSearch) {
        chessBoard = Board.getInstance();
        search = pSearch;
    }
	
    public static int convertToBoardIndex( String strPos ) {
        char fileChar = strPos.charAt(0);
        char rankChar = strPos.charAt(1);
        return fileChar - 97 + (rankChar - 49) * 8;
    }
    
    public static String convertToBoardString( int boardIndex ) {
        char fileChar = (char)((boardIndex & 7) + 97);
        char rankChar = (char)((boardIndex >> 3) + 49);
        return Character.toString(fileChar) + Character.toString(rankChar);
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
        moves = moves.trim();
        int type = 0;
        int to = 0;
        int from = 0;
        int movesProcessed = 0;
       
        String[] strArr = moves.split("\\s");
        int numberBoardMoves = Board.getInstance().getCount();
        //if we are accepting less moves than we have played, lets just do it from scratch
        if( strArr.length < numberBoardMoves ) {
             chessBoard.undoAll();
             numberBoardMoves = 0;
        }
        
        //prune all moves which we already have from the moves list
        for( int i=0; i < strArr.length; i++ ) {
            
            if( movesProcessed >= numberBoardMoves) break;
            
            switch( strArr[i].length() ) {
                
                case( 4 ):
                {
                    from = convertToBoardIndex( strArr[i].substring(0,2) );
                    to = convertToBoardIndex( strArr[i].substring(2,4) );
                }       
                break;
                
                case( 5 ):
                    from = convertToBoardIndex( strArr[i].substring(0,2) );
                    to = convertToBoardIndex( strArr[i].substring(2,4) );
                    char pieceChar = strArr[i].charAt(4);
                    switch (pieceChar) {
                        case('q'):
                           type = Global.PROMO_Q; 
                        break;
                        case('n'):
                            type = Global.PROMO_N;
                        break;    
                        case('b'):
                            type = Global.PROMO_B;
                        break;
                        case('r'):
                            type = Global.PROMO_R;
                        break;
                        default:
                            assert( false );
                        break;
                    };
                break;
                
                default:
                {
                    System.out.println("info string unrecognized move "+strArr[i]);
                    return; 
                }
            }   
            
            int boardMove = Board.getInstance().GetBoardMove(movesProcessed);
            if(to != MoveFunctions.getTo(boardMove) || from != MoveFunctions.getFrom(boardMove))
            {
                break;
            }
            movesProcessed++;
        }       
         
        if(movesProcessed < numberBoardMoves)
        {
            //if we are accepting less moves than we have played, lets just do it from scratch
            chessBoard.undoAll();
            acceptMoves(moves);
            return;
        }
        
        chessBoard.ResetMovesDepth();

        for( int i=movesProcessed; i < strArr.length; i++ ) {
            int moveArr[] = new int[128];
            int numberOfMoves = search.GetAllMoves(chessBoard.getTurn(), moveArr);
            
             switch( strArr[i].length() ) {
                
                case( 4 ):
                {
                    from = convertToBoardIndex( strArr[i].substring(0,2) );
                    to = convertToBoardIndex( strArr[i].substring(2,4) );
                }       
                break;
                
                case( 5 ):
                {    
                    from = convertToBoardIndex( strArr[i].substring(0,2) );
                    to = convertToBoardIndex( strArr[i].substring(2,4) );
                    char pieceChar = strArr[i].charAt(4);
                    switch (pieceChar) {
                        case('q'):
                           type = Global.PROMO_Q; 
                        break;
                        case('n'):
                            type = Global.PROMO_N;
                        break;    
                        case('b'):
                            type = Global.PROMO_B;
                        break;
                        case('r'):
                            type = Global.PROMO_R;
                        break;
                        default:
                            assert( false );
                        break;
                    };
                }
                break;
                
                default:
                {
                    System.out.println("info string unrecognized move "+strArr[i]);
                    return; 
                }
            }   
            for(int j=0; j < numberOfMoves; j++)
            {
                Board.CheckInfo checkInfo = chessBoard.GetCheckInfo();
                if( strArr[i].length() == 4 && to == MoveFunctions.getTo( moveArr[j] ) && from == MoveFunctions.getFrom( moveArr[j] ))
                {
                    boolean bGivesCheck = chessBoard.MoveGivesCheck(chessBoard.getTurn(), moveArr[j], checkInfo);
                    chessBoard.AddMove( moveArr[j] );
                    chessBoard.MakeMove( moveArr[j], false, bGivesCheck, checkInfo);
                    chessBoard.AddRepetitionRoot();
                    break;
                }
                else if( strArr[i].length() == 5 && to == MoveFunctions.getTo( moveArr[j] ) && from == MoveFunctions.getFrom( moveArr[j] ) ) {
                    if( type == MoveFunctions.moveType( moveArr[j]) || (type >= Global.PROMO_R && type <= Global.PROMO_N) )
                    {
                        int mv = MoveFunctions.makeMove(to, from, type);
                        boolean bGivesCheck = chessBoard.MoveGivesCheck(chessBoard.getTurn(), moveArr[j], checkInfo);
                        chessBoard.AddMove( mv );
                        chessBoard.MakeMove( mv, false, bGivesCheck, checkInfo);
                        chessBoard.AddRepetitionRoot();
                    }        
                }
            }
        }
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
        String strMove = convertToBoardString(from);
        strMove = strMove.concat(convertToBoardString(to));
        switch(MoveFunctions.moveType(move)) {
            case(Global.PROMO_Q):
                strMove = strMove.concat("q");
            break;
            case(Global.PROMO_N):
                strMove = strMove.concat("n");
            break;    
            case(Global.PROMO_B):
                strMove = strMove.concat("b");
            break;
            case(Global.PROMO_R):
                strMove = strMove.concat("r");
            break;
            default:
            break;
        };
        return strMove; 
    }					
}		
