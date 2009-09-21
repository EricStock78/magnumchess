/**********************************************************************************
	Cosc 3P71:		Final Project
	Authored by: 	Eric Stock
					Joseph Troop
	Name:	ChessMoves.java
**********************************************************************************/

import java.io.*;
import java.util.*;
import java.lang.*;

/**********************************************************************************
	Name:  		ChessMoves
	Description:This class is responsible for the generation of all of the moves
				in the game. This is accomplished primarily by applying boolean
				algebra to BitSets. 
**********************************************************************************/
public final class ChessMoves{
	
	private BitSet mask = new BitSet(64);
	private int size = 64;
	private BitSet tempMask = new BitSet(64);
	
	/**********************************************************************************
		Name:		ChessMoves
		Parameters:	None
		Return:		None	
		Description:This is the constructor of this class. The mask BitSet that is
					used in the NOT operation is initialized here. All of the values
					int the mask BitSet are set to true
	**********************************************************************************/	
	public ChessMoves(){
		int x;
		for(x=0;x<size;x++){
			mask.set(x);
		}
	}// End Constructor
	
	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/
	public boolean enPessant(int square, boolean color){
		return false;
	}
	
	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/
	public boolean castleKingSide(boolean color){
		return false;
	}
	
	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/
	public boolean castleQueenSide(boolean color){
		return false;
	}
	
	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/
	public BitSet QueenCaptures(BitSet queenMoves, BitSet Pieces, boolean color){
		BitSet tempQueenMoves = new BitSet(64);
		BitSet tempPieces = new BitSet(64);
		tempQueenMoves = (BitSet)queenMoves.clone();
		tempPieces = (BitSet)Pieces.clone();
		
		tempPieces.and(tempQueenMoves);
		return tempPieces;
	}// End QueenCaptures

	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/	
	public BitSet RookCaptures(BitSet rookMoves, BitSet Pieces, boolean color){
		BitSet tempRookMoves = new BitSet(64);
		BitSet tempPieces = new BitSet(64);
		tempRookMoves = (BitSet)rookMoves.clone();
		tempPieces = (BitSet)Pieces.clone();

		tempRookMoves.and(tempPieces);
		return tempRookMoves;
	}// End rook captures
	
	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/	
	public BitSet BishopCaptures(BitSet bishopMoves, boolean color){
		if(color){
			//BitSet temp = allBlackPieces.clone();
			//temp.and(bishopMoves);
			return bishopMoves;
		}
		else{
			//BitSet temp = allWhitePieces.clone();
			//temp.and(bishopMoves);
			return bishopMoves;
		}
	}// End rook
	
	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/	
	public BitSet KnightCaptures(BitSet allPieces, BitSet knightMoves, boolean color){
		BitSet tempKnightMoves = new BitSet(64);
		BitSet tempAllPieces = new BitSet(64);
		tempAllPieces = (BitSet)allPieces.clone();
		tempKnightMoves = (BitSet)knightMoves.clone();

	/*	if(color){
			tempKnightMoves.and(tempAllPieces);
			return tempKnightMoves;
		}
		else{
			tempKnightPosition.and(tempAllPieces);
			return tempKnightPosition;
		}
*/		return tempKnightMoves;
	}
	
	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/	
	public BitSet KnightMoves(BitSet allPieces, BitSet knightMoves , boolean color){
		BitSet tempAllPieces = new BitSet(64);
		BitSet tempKnightMoves = new BitSet(64);
		tempAllPieces = (BitSet)allPieces.clone();
		tempKnightMoves = (BitSet)knightMoves.clone();
		
		if(color){
			tempKnightMoves.and(NOT(tempAllPieces));	
			return tempKnightMoves;
		}
		else{			
			tempKnightMoves.and(NOT(tempAllPieces));
			return tempKnightMoves;
		}
	}// End KnightMoves
	
	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/	
	public BitSet KingMoves(int sqaure, boolean color){		
	/*	if(color){
			BitSet temp = allBlackPieces.clone();
			
			return kingMoves[square].and(NOT(temp));
		}
		else{
			BitSet temp = allOtherWhitePieces().clone();
			return kingMoves[square].and(NOT(temp));
		}
		resetMask();		
		return kingMoves;
	*/
	BitSet temp = new BitSet(64);
	return temp;
	}// End KingMoves
	
	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/	
	public BitSet whitePawnMovesAttacks(int square){
	/*	BitSet pawnMove = new BitSet(64);
		pawnMove = WhitePawnMoveBoard[square].clone ();
		BitSet temp = new BitSet(64);
		Bitset temp2 = new BitSet(64);
		
		if(square > 55){
			resetMask();
			return tempMask; 
		}
		
		BitSet allOtherPieces = new BitSet(64);
		
		resetMask();
		return pawnMove.and(allOtherPieces);
*/		BitSet temp = new BitSet(64);
		return temp;
	}// End whitePawnMovesAttacks
	
	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/
	public void blackPawnMoves(int square){
	/*	BitSet pawnMove = new BitSet(64);
		pawnMove = BlackPawnMoveBoard[square].clone();
		BitSet temp = new BitSet(64);
		BitSet temp2 = new BitSet(64);
		
		if(square < 8 ){
			resetMask();
			return tempMask;
		}
		
		BitSet allOtherPieces;;
		resetMask();
		return pawnMove.and(allOtherPieces);
*/
		
	}// End blackPawnMoves
	
	/**********************************************************************************
		Name:		NOT	
		Parameters:	BitSet 
		Return:		BitSet
		Description:This method uses the mask BitSet to implement the NOT operation.
					This is accomplished by applying the mask an xor operation and 
					the paramter BitSet.
	**********************************************************************************/
	private BitSet NOT(BitSet board){
		mask.xor(board);
		return mask;
	}// End NOT
	
	/**********************************************************************************
		Name:		resetMask
		Parameters:	None
		Return:		None
		Description:This method resets the class level variable mask back to its'
					original state. That being all of the elements are set to true. 
					This mask is used primarily to implement a NOT operation
	**********************************************************************************/	
	private void resetMask(){
		int x;
		for(x=0;x<size;x++){
			mask.set(x);
		}
	}// End resetMask
}// End class ChessMoves

	/**********************************************************************************
		Name:
		Parameters:
		Return:
		Description:
	**********************************************************************************/