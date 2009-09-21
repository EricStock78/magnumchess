import java.util.Arrays;
import java.util.LinkedList;
/**
 * @(#)SEE.java
 *
 *
 * @author 
 * @version 1.00 2008/4/14
 */


public class SEE {

    public SEE() {
    }
    public static boolean isPinned(int side, int to, int from) {
    	int relation;
    	int fromPos;								//position of moving piece 
    	long enemies;
    	long king;							
    	long temp;
    	int nextPos;
    	if(side==1) {
			king = Board.blackking;
			enemies = Board.whitepieces;
		}
		else {
			king = Board.whiteking;
			enemies = Board.blackpieces;
		}

    	int kingPos = Board.getPos(king);
    	
    	if(kingPos == from) {
    	
    		return false;	
    	}
    	enemies &= Board.slidePieces;
    	
    	int difference = kingPos - from;
    	int rankDifference = kingPos/8 - from/8;
    	if(difference < 0)
    		rankDifference *= -1;
    	if(rankDifference != 0) {
    		if((difference % rankDifference) != 0) return false;
    		relation = difference / rankDifference;
    	
    	} else  {
    		if(kingPos < from)
    			relation = -99;
    		else
    			relation = 99;		
    	}
    	switch(relation) {
    		case(-9):	
    			if(Global.Diag2Groups[from] == Global.Diag2Groups[to]) return false;
    			
    			temp = Board.bitboard & Global.plus9[kingPos];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag2Masks[Global.Diag2Groups[from]]) == 0) return false;
    			return true;
    		case(9):
    			if(Global.Diag2Groups[from] == Global.Diag2Groups[to]) return false;
    			
    			temp = Board.bitboard & Global.plus9[from];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag2Masks[Global.Diag2Groups[from]]) == 0) return false;
    			return true;
    		case(-7):
    			if(Global.Diag1Groups[from] == Global.Diag1Groups[to]) return false;
    			
    			temp = Board.bitboard & Global.plus7[kingPos];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag1Masks[Global.Diag1Groups[from]]) == 0) return false;
    			return true;
    		case(7):
    			if(Global.Diag1Groups[from] == Global.Diag1Groups[to]) return false;
    			temp = Board.bitboard & Global.plus7[from];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.diag1Masks[Global.Diag1Groups[from]]) == 0) return false;
    			return true;	
    		case(-8):
    			if(from%8 == to%8) return false;
    			temp = Board.bitboard & Global.plus8[kingPos];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.fileMasks[from%8]) == 0) return false;
    			return true;			
    		case(8):
    			if(from%8 == to%8) return false;
    			temp = Board.bitboard & Global.plus8[from];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.fileMasks[from%8]) == 0) return false;
    			return true;
    		case(-99):
    			if(from/8 == to/8) return false;
    			temp = Board.bitboard & Global.plus1[kingPos];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < from) return false;
    			if((Board.getAttack2(from) & enemies & Global.rankMasks[from/8]) == 0) return false;
    			return true;	
 			case(99):
 				if(from/8 == to/8) return false;
    			temp = Board.bitboard & Global.plus1[from];
    			temp &= -temp;
    			nextPos = Board.getPos(temp);	
    			if(nextPos < kingPos) return false;
    			if((Board.getAttack2(from) & enemies & Global.rankMasks[from/8]) == 0) return false;
    			return true;
 				
    	}
    	return false;
    	
    }
    private static int hasHidden(int to, int from) {
    	int relation;
    	int difference = to - from;
    	int rankDifference = to/8 - from/8;
    		long temp;
    	int pos;
    	if(difference < 0)
    			rankDifference *= -1;
    	if(rankDifference != 0) {
   			if((difference % rankDifference) != 0) return -1;
    		relation = difference / rankDifference;	
    	}else {
    		if( to < from)
    			relation = -99;
    		else
    			relation = 99;
    	}
    	
    	switch(relation) {
    		case(-9):	
    			temp = Global.plus9[from] & Board.getMagicBishopMoves(from);// & Global.diag2Masks[Global.Diag2Groups[from]];
    			//temp = Global.plus9[from] & Board.attack2[from] & Global.diag2Masks[Global.Diag2Groups[from]];
    			//if(temp != 0)
    				temp &= Board.slidePieces;
    			if (temp != 0) {
					pos = Board.getPos(temp);    			
    				if(Board.piece_in_square[pos]%6 == 0)	//	its a rook so this can be a hidden attacker
    					return -1;
    				return Global.SEEvalues[Board.piece_in_square[pos]]<<6 | pos;
    			}
    			break;
    		case(9):
    			temp = Global.minus9[from] & Board.getMagicBishopMoves(from);// & Global.diag2Masks[Global.Diag2Groups[from]];
    			//if(temp != 0)
    				temp &= Board.slidePieces;
    			if (temp != 0) {
					pos = Board.getPos(temp);    			
    				if(Board.piece_in_square[pos]%6 == 0)	//	its a rook so this can be a hidden attacker
    					return -1;
    				return Global.SEEvalues[Board.piece_in_square[pos]]<<6 | pos;
    			}
    			break;
    		case(-7):
    			temp = Global.plus7[from] & Board.getMagicBishopMoves(from); //& Global.diag1Masks[Global.Diag1Groups[from]];
    			//if(temp != 0)
    				temp &= Board.slidePieces;
    			if (temp != 0) {
					pos = Board.getPos(temp);    			
    				if(Board.piece_in_square[pos]%6 == 0)	//	its a rook so this can be a hidden attacker
    					return -1;
    				return Global.SEEvalues[Board.piece_in_square[pos]]<<6 | pos;
    			}
    			break;
    		case(7):
    			temp = Global.minus7[from] & Board.getMagicBishopMoves(from); //& Global.diag1Masks[Global.Diag1Groups[from]];
    			//if(temp != 0)
    				temp &= Board.slidePieces;
    			if (temp != 0) {
					pos = Board.getPos(temp);    			
    				if(Board.piece_in_square[pos]%6 == 0)	//	its a rook so this can be a hidden attacker
    					return -1;
    				return Global.SEEvalues[Board.piece_in_square[pos]]<<6 | pos;
    			}
    			break;
    		case(-8):
    			temp = Global.plus8[from] & Board.getMagicRookMoves(from); //& Global.fileMasks[from % 8];
    			//if(temp != 0)
    				temp &= Board.slidePieces;
    			if (temp != 0) {
					pos = Board.getPos(temp);    			
    				if(Board.piece_in_square[pos]%6 == 2)	//	its a bishop so this can be a hidden attacker
    					return -1;
    				return Global.SEEvalues[Board.piece_in_square[pos]]<<6 | pos;
    			}
    			break;
    		case(8):
    			temp = Global.minus8[from] & Board.getMagicRookMoves(from); //& Global.fileMasks[from % 8];
    			//if(temp != 0)
    				temp &= Board.slidePieces;
    			if (temp != 0) {
					pos = Board.getPos(temp);    			
    				if(Board.piece_in_square[pos]%6 == 2)	//	its a rook so this can be a hidden attacker
    					return -1;
    				return Global.SEEvalues[Board.piece_in_square[pos]]<<6 | pos;
    			}
    			break;
    		case(-99):
    			temp = Global.plus1[from] & Board.getMagicRookMoves(from); //& Global.rankMasks[from / 8];
    			//if(temp != 0)
    				temp &= Board.slidePieces;
    			if (temp != 0) {
					pos = Board.getPos(temp);    			
    				if(Board.piece_in_square[pos]%6 == 2)	//	its a rook so this can be a hidden attacker
    					return -1;
    				return Global.SEEvalues[Board.piece_in_square[pos]]<<6 | pos;
    			}
    			break;
    		case(99):
    			temp = Global.minus1[from] & Board.getMagicRookMoves(from); //& Global.rankMasks[from / 8];
    			//if(temp != 0)
    				temp &= Board.slidePieces;
    			if (temp != 0) {
					pos = Board.getPos(temp);    			
    				if(Board.piece_in_square[pos]%6 == 2)	//	its a rook so this can be a hidden attacker
    					return -1;
    				return Global.SEEvalues[Board.piece_in_square[pos]]<<6 | pos;
    			}
    			break;	
    	}
    	return -1;
    }

	public static int getSEE(int side, int to, int from, int passant) {
		
		//if (isPinned(side, to, from)) 
		//	return -60;
		long friends;
		long enemies;
		long temp;
		long temp2;
		int hidden;
		
		if (side == 1) {
			friends = Board.blackpieces;
			enemies = Board.whitepieces;
		} else {
			friends = Board.whitepieces;
			enemies = Board.blackpieces;
		}	
		
		int[] ePieces = new int[10];
		int[] fPieces = new int[10];
		
		int eCount = 0;
		int fCount = 0;
		
		if(to == passant)
			ePieces[0] = Global.SEEvalues[5]<<6;//6400;
		else 
			ePieces[0] = (Global.SEEvalues[Board.piece_in_square[to]]<<6);
		eCount = 1;
		fPieces[0] = (Global.SEEvalues[Board.piece_in_square[from]]<<6 | from);
		fCount = 1;
		
		hidden = hasHidden(to,from);
			
		if(hidden != -1) {
			if((Global.set_Mask[hidden&63] & friends)!= 0)	{	//adding a friend hidden piece		
				//if(!isPinned(side,to,hidden&63)) {
					fPieces[fCount++] = hidden;
				//}
			}else {												//adding an enemy hidden piece
				//if(!isPinned(-side,to,hidden&63)) {
					ePieces[eCount++] = hidden;
			//	}
			}
		}
		long attack = Board.getAttack2(to);
		temp = attack;
		//temp = Board.getAttack2(to);//attack2[to];
		
		if(eCount == 1 && (temp & enemies)==0)
			return ePieces[0]>>6;
		
		temp &= enemies;
		
		while (temp != 0)	{
			
			temp2 = temp & -temp;
			temp ^= temp2;
			int pos = Board.getPos(temp2);
			//if(!isPinned(-side,to,pos)) {
				int value = Global.SEEvalues[Board.piece_in_square[pos]]<<6;
				ePieces[eCount++] = (value | pos);		
			//}
		}
		
		if(eCount == 1)
			return ePieces[0]>>6;
		else if(eCount > 2)
			Arrays.sort(ePieces,1,eCount);
		
		
		temp = friends & attack;
		temp ^= Global.set_Mask[from];
		
		if(fCount == 1 && temp == 0)
			return (ePieces[0]>>6) - (fPieces[0]>>6);
		
		
		while (temp != 0)	{
			temp2 = temp & -temp;
			temp ^= temp2;
			int pos = Board.getPos(temp2);
			//if(!isPinned(side,to,pos)) {
				int value = Global.SEEvalues[Board.piece_in_square[pos]];
				fPieces[fCount++] = (value << 6 | pos);
			//}
		}
		if(fCount == 1)
			return (ePieces[0]>>6) - (fPieces[0]>>6);
		else if(fCount > 2)
			Arrays.sort(fPieces,1,fCount);	
		
		
		int tempVal = 0;
		int alpha = -20000;
		int beta = 20000;
		int moveNumber = 0;
		
		while (true) {
		if(moveNumber > 0) {
			hidden = hasHidden(to,fPieces[moveNumber] & 63);
			
			if(hidden != -1 ) {
				if(((Global.set_Mask[hidden&63] & friends)!= 0) )	{	//adding a friend hidden piece		
					if((eCount-1 > moveNumber)){ //&&!isPinned(side,to,hidden&63)) {
						fPieces[fCount++] = hidden;
						if(fCount > moveNumber + 1)
							Arrays.sort(fPieces,moveNumber+1,fCount);
					}
				}else {												//adding an enemy hidden piece
					//if(!isPinned(-side,to,hidden&63)) {
						ePieces[eCount++] = hidden;
						if(eCount > moveNumber + 1)
							Arrays.sort(ePieces, moveNumber + 1, eCount);
					//}
				}
					
			}
		}
			
			
			tempVal += ePieces[moveNumber]>>6;		//friend moves
			if(tempVal < beta)
				beta = tempVal;
			if(alpha >= beta) {
				tempVal = alpha;
				break;
			}	
			if(moveNumber == eCount - 1) {
				tempVal = beta;
				break;
			}
			
			hidden = hasHidden(to,ePieces[moveNumber+1]&63);
			if(hidden != -1 ) {
				if((Global.set_Mask[hidden&63] & friends)!= 0)	{	//adding a friend hidden piece		
					//if(!isPinned(side,to,hidden&63)) {
						fPieces[fCount++] = hidden;
						if(fCount > moveNumber + 1)
							Arrays.sort(fPieces, moveNumber+1,fCount);
				//	}
				} else {								
					if((fCount-1 > moveNumber)){// &&!isPinned(-side,to,hidden&63)) {	
						ePieces[eCount++] = hidden;
						if(eCount > moveNumber + 2)
							Arrays.sort(ePieces,moveNumber+2,eCount);
					}
				}
					
			}
			
			
			tempVal -= fPieces[moveNumber]>>6;		//enemy re-captures
			if(tempVal > alpha)
				alpha = tempVal;
			if(alpha >= beta) {
				tempVal = beta;
				break;
			}
			if(moveNumber == fCount - 1) {
				tempVal = alpha;
				break;
			}
			moveNumber++;	
		}
		/*
		System.out.println("to is "+to);
		System.out.println("from is "+from);
		System.out.println("friends are ");
		for(int i = 0;i<fCount;i++) {
			System.out.print((fPieces[i]>>6)+" ");
		}
		System.out.println("enemeies are ");
		for(int i = 0;i<eCount;i++) {
			System.out.print((ePieces[i]>>6)+" ");
		}
		System.out.println("value is "+tempVal);
		*/
		
		return tempVal;
	}
	
	public static int getSEE2(int side, int to, int from) {
		//System.out.println("see2 called");
		//if (isPinned(side, to, from)) 
		//	return -60;
		long friends;
		long enemies;
		long temp;
		long temp2;
		int hidden;
		
		if (side == 1) {
			friends = Board.blackpieces;
			enemies = Board.whitepieces;
		} else {
			friends = Board.whitepieces;
			enemies = Board.blackpieces;
		}	
		
		int[] ePieces = new int[10];
		int[] fPieces = new int[10];
		
		int eCount = 0;
		int fCount = 0;
		ePieces[0] = 0;
		eCount = 1;

		fPieces[0] = (Global.SEEvalues[Board.piece_in_square[from]]<<6 | from);
		fCount = 1;
		
		hidden = hasHidden(to,from);
			
		if(hidden != -1) {
			if((Global.set_Mask[hidden&63] & friends)!= 0)	{	//adding a friend hidden piece		
					fPieces[fCount++] = hidden;
			}else {												//adding an enemy hidden piece
					ePieces[eCount++] = hidden;
			}
		}
		long attack = Board.getAttack2(to);
		temp = attack;
		
		//if(eCount == 1 && (temp & enemies)==0)
		//	return ePieces[0]>>6;
		
		temp &= enemies;
		
		while (temp != 0)	{
			temp2 = temp & -temp;
			temp ^= temp2;
			int pos = Board.getPos(temp2);
				int value = Global.SEEvalues[Board.piece_in_square[pos]]<<6;
				ePieces[eCount++] = (value | pos);		
		}
		
		if(eCount == 1)
			return 0;
		//else if(eCount > 1)
		if(eCount > 2)	
			Arrays.sort(ePieces,1,eCount);
		
		
		temp = friends & attack;
		temp ^= Global.set_Mask[from];
		
		//if(fCount == 1 && temp == 0)
		//	return (ePieces[0]>>6) - (fPieces[0]>>6);
		
		
		while (temp != 0)	{
			temp2 = temp & -temp;
			temp ^= temp2;
			int pos = Board.getPos(temp2);
				int value = Global.SEEvalues[Board.piece_in_square[pos]];
				fPieces[fCount++] = (value << 6 | pos);
		}
		if(fCount == 1)
			return -(fPieces[0]>>6);
		//if(fCount == 1 && eCount == )
		//	return (ePieces[0]>>6) - (fPieces[0]>>6);
		//else 
		if(fCount > 2)
			Arrays.sort(fPieces,1,fCount);	
		
		
		int tempVal = 0;
		int alpha = -20000;
		int beta = 20000;
		int moveNumber = 0;
		
		while (true) {
		if(moveNumber > 0) {
			hidden = hasHidden(to,fPieces[moveNumber] & 63);
			
			if(hidden != -1 ) {
				if(((Global.set_Mask[hidden&63] & friends)!= 0) )	{	//adding a friend hidden piece		
					if((eCount-1 > moveNumber)){ //&&!isPinned(side,to,hidden&63)) {
						fPieces[fCount++] = hidden;
						if(fCount > moveNumber + 1)
							Arrays.sort(fPieces,moveNumber+1,fCount);
					}
				}else {												//adding an enemy hidden piece
					//if(!isPinned(-side,to,hidden&63)) {
						ePieces[eCount++] = hidden;
						if(eCount > moveNumber + 1)
							Arrays.sort(ePieces, moveNumber + 1, eCount);
					//}
				}
					
			}
		}
			
			
			tempVal += ePieces[moveNumber]>>6;		//friend moves
			if(tempVal < beta)
				beta = tempVal;
			if(alpha >= beta) {
				tempVal = alpha;
				break;
			}	
			if(moveNumber == eCount - 1) {
				tempVal = beta;
				break;
			}
			
			hidden = hasHidden(to,ePieces[moveNumber+1]&63);
			if(hidden != -1 ) {
				if((Global.set_Mask[hidden&63] & friends)!= 0)	{	//adding a friend hidden piece		
					//if(!isPinned(side,to,hidden&63)) {
						fPieces[fCount++] = hidden;
						if(fCount > moveNumber + 1)
							Arrays.sort(fPieces, moveNumber+1,fCount);
				//	}
				} else {								
					if((fCount-1 > moveNumber)){// &&!isPinned(-side,to,hidden&63)) {	
						ePieces[eCount++] = hidden;
						if(eCount > moveNumber + 2)
							Arrays.sort(ePieces,moveNumber+2,eCount);
					}
				}
					
			}
			
			
			tempVal -= fPieces[moveNumber]>>6;		//enemy re-captures
			if(tempVal > alpha)
				alpha = tempVal;
			if(alpha >= beta) {
				tempVal = beta;
				break;
			}
			if(moveNumber == fCount - 1) {
				tempVal = alpha;
				break;
			}
			moveNumber++;	
		}
		//if(tempVal == 0 && (fCount >= eCount))
		//	tempVal += (fCount+1-eCount);
		/*
		System.out.println("to is "+to);
		System.out.println("from is "+from);
		System.out.println("friends are ");
		for(int i = 0;i<fCount;i++) {
			System.out.print((fPieces[i]>>6)+" ");
		}
		System.out.println("enemeies are ");
		for(int i = 0;i<eCount;i++) {
			System.out.print((ePieces[i]>>6)+" ");
		}
		System.out.println("value is "+tempVal);
		*/
		
		return tempVal;
	}
	
}