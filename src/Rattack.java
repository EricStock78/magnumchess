

public final class Rattack{	

private static final int size = 64;
private static final int attacks = 256;
private static int[] Board	= new int[64];
private static int[] BoardR90 	= new int[64];
private static final long[][] RookRankAttacksBoard	= new long[size][attacks];
private static final long[][] RookFileAttacksBoard = new long[size][attacks];
private static final long[][] BishopDiag1AttackBoard = new long[size][attacks];
private static final long[][] BishopDiag2AttackBoard = new long[size][attacks];
private static final long[] WKingBoard = new long[attacks];
private static final int Diag1Length[] = new int[]  {1,2,3,4,5,6,7,8,
													 2,3,4,5,6,7,8,7,
													 3,4,5,6,7,8,7,6,
													 4,5,6,7,8,7,6,5,
													 5,6,7,8,7,6,5,4,
													 6,7,8,7,6,5,4,3,
													 7,8,7,6,5,4,3,2,
													 8,7,6,5,4,3,2,1};



private static final long[] powOf2 = new long[64];;

private static final int Diag2Length[] = new int[] {8,7,6,5,4,3,2,1,
															 7,8,7,6,5,4,3,2,
															 6,7,8,7,6,5,4,3,
															 5,6,7,8,7,6,5,4,
															 4,5,6,7,8,7,6,5,
															 3,4,5,6,7,8,7,6,
															 2,3,4,5,6,7,8,7,
															 1,2,3,4,5,6,7,8,};


public Rattack() {
	int length;
	
	for(int x=0;x<63;x++) {
		powOf2[x] = (long)Math.pow(2,x);
	}
	powOf2[63] = (long)Math.pow(-2,63);
	
	for(int i=0;i<64;i++) {
		for(int j=0;j<256;j++) {
			setRooksRank(i,j);
			setRooksFile(i,j);
		}
	}
	for(int i=0;i<64;i++) {
		length = (int)Math.pow(2.0,(double)Diag1Length[i]);
		for(int j=0;j<length;j++) {
		//for(int j=0;j<1;j++) {	
			setDiag1Attack(i,j);
		}
	}
	for(int i=0;i<64;i++) {
		length = (int)Math.pow(2.0,(double)(Diag2Length[i]));
		for(int j=0;j<length;j++) {
			setDiag2Attack(i,j);
		}
	}
	for(int i=0;i<256;i++) {
			setWKingCastle(i);
			//setBKingCastle(i);
	}
}
public static final long getDiag1Attack(int square,int file){
		return BishopDiag1AttackBoard[square][file];
		//return RookRankAttacksBoard[square][file];
	}// End getKingPosition	
public static final long getDiag2Attack(int square, int file) {
	return BishopDiag2AttackBoard[square][file];
}
public static final long getKingCastle(int file) {
	return WKingBoard[file];
}

private static final void setWKingCastle(int file) {
	//WKingBoard[file] = new long[64];
	if(file>=144 && file < 160) {
		WKingBoard[file] = (long)1<<6;
	}
	long temp = file&31;
	//file &= 31;
	//if(file == 145 || file == 49 || file == 81 || file==113 || file==145 || file==209 || file ==177 || file==241) {
	if(temp==17)
		WKingBoard[file] |= (long)1<<2;
	
}

private static final void setDiag1Attack(int index1, int index2) {
	long temp;			//BitSet of all pieces in diagonal
	int length;				//length of diagonal
	int curPos;				//current position in diagonal(start lower left-move up right)
	int i;						//loop counter
	int Remainder;		//result of remainder
	int Result;				//result of division	
	long bit;
	BishopDiag1AttackBoard[index1][index2] = 0;
	temp = 0;
	length = Diag1Length[index1];			//length of diagonal
	//move to lower left of diagonal
	curPos = index1;
	while(curPos%8!=0 && curPos/8!=7)	{  
		curPos = curPos+7;			//move to lower left;
	}
	Remainder = 0;
	Result = index2;
	for(i=0;i<length;i++) {
		Remainder = Result%2;
		Result = Result/2;
		//bit = (Remainder>>>curPos)&1;
		if(Remainder==1) {
			temp += powOf2[curPos];
		}
		if(Result==0) break;
		curPos = curPos-7;
	}
	//NOW search to lower right of piece
	
	curPos = index1;
	
	if(curPos%8!=7 && curPos/8!=0)	{
		curPos = curPos-7;
		BishopDiag1AttackBoard[index1][index2]+=powOf2[curPos]; 
		while(curPos%8!=7&&curPos/8!=0) {
			bit = (temp>>>curPos)&1;
			if(bit==1) break;
			curPos=curPos-7;
			BishopDiag1AttackBoard[index1][index2]+=powOf2[curPos]; 
		}
	}  	//else do nothing as already on lower left...ie never pices to attack on lower left
			
	
	//NOW search to upper left of piece
	curPos = index1;
	if(curPos%8!=0 && curPos/8!=7)	{
		curPos = curPos+7;
		BishopDiag1AttackBoard[index1][index2]+=powOf2[curPos]; 
		while(curPos%8!=0&&curPos/8!=7) {
			bit = (temp>>>curPos)&1;
			if(bit==1) break;
			curPos=curPos+7;
			BishopDiag1AttackBoard[index1][index2]+=powOf2[curPos]; 
		}
	}	
}
private static final void setDiag2Attack(int index1, int index2) {
	long temp;			//BitSet of all pieces in diagonal
	long bit;
	int length;				//length of diagonal
	int curPos;				//current position in diagonal(start lower left-move up right)
	int i;						//loop counter
	int Remainder;		//result of remainder
	int Result;				//result of division	
	BishopDiag2AttackBoard[index1][index2] = 0;
	temp = 0;
	length = Diag2Length[index1];			//8-length of other diagonal
	//move to lower left of diagonal
	curPos = index1;
	while(curPos%8!=0 && curPos/8!=0)	{  
		curPos = curPos-9;			//move to lower right
	}
	Remainder = 0;
	Result = index2;
	for(i=0;i<length;i++) {
		Remainder = Result%2;
		Result = Result/2;
		if(Remainder==1) {
			temp+=powOf2[curPos];
		}
		if(Result==0) break;
		curPos = curPos+9;
	}
	//NOW search to lower left of piece
	
	curPos = index1;
	
	if(curPos%8!=0 && curPos/8!=0)	{
		curPos = curPos-9;
		//System.out.println(curPos);
		BishopDiag2AttackBoard[index1][index2]+=powOf2[curPos]; 
		while(curPos%8!=0&&curPos/8!=0) {
			bit = (temp>>>curPos)&1;
			if(bit==1) break;
			curPos=curPos-9;
			BishopDiag2AttackBoard[index1][index2]+=powOf2[curPos]; 
		}
	}  	//else do nothing as already on lower left...ie never pices to attack on lower left
			
	
	//NOW search to upper right of piece
	curPos = index1;
	if(curPos%8!=7 && curPos/8!=7)	{
		curPos = curPos+9;
		BishopDiag2AttackBoard[index1][index2]+=powOf2[curPos]; 
		while(curPos%8!=7&&curPos/8!=7) {
			bit = (temp>>>curPos)&1;
			if(bit==1) break;
			curPos=curPos+9;
			BishopDiag2AttackBoard[index1][index2]+=powOf2[curPos]; 
		}
	}	
   
}

private static final void setRooksRank(int index1, int index2) {
	long temp;			//BitSet of all pieces in file	
	int Row;	  			//index1 div 8  ROW	
	int curIndex;			//variable used to index BitSet during traversal
	long binRem;				//Remainder of calculation used to convert int to Binary
	long Bresult;			//Division result of calc used to convert int to Binary
	long tResult;			//temp result
	long rBound;				//if 0 stop	
	long bit;
	long pow2;
	long shifted;
	Row = index1/8;
	curIndex = Row*8;
	Bresult = index2;
	binRem = 0;
	temp = 0;
	RookRankAttacksBoard[index1][index2] = 0;
	bit = index2;
	temp = bit<<(Row*8);    //Shift index2 into place in chess board
	curIndex = index1+1;			//start at first piece to right of Rook
	rBound = curIndex/8;		  //get row of piece(make sure same row as Rook)
	while(rBound == Row) {		//check to make sure same row
		pow2 = powOf2[curIndex];
		RookRankAttacksBoard[index1][index2] += pow2;	//set the position as attackable
		shifted = (temp>>>curIndex)&1;
		if(shifted==1) break;
		curIndex++;								//move right
		rBound = curIndex/8;			//calculate row
	}
		//NOW we search to the left of ROOK;	
	curIndex = index1-1;		//start at first piece to left of Rook
	rBound = curIndex/8;		//get row of piece(make sure same row as Rook)
	while(rBound == Row) {	//check to make sure same row
		if(curIndex==-1) break;		//if out of bounds stop
		pow2 = powOf2[curIndex];
		RookRankAttacksBoard[index1][index2]+=pow2;		//set position as attackable
		shifted = (temp>>>curIndex)&1;
		if(shifted == 1) break;			//if current pos contains a piece stop
		curIndex--;							//move left
		rBound = curIndex/8;		//calculate row
	}
}



private static final void setRooksFile(int index1, int index2) {
	long temp;			//BitSet of all pieces in file	
	long bit;
	int file;	  			//index1 div 8  ROW	
	int curIndex;			//variable used to index BitSet during traversal
	int binRem;				//Remainder of calculation used to convert int to Binary
	int Bresult;			//Division result of calc used to convert int to Binary
	int tResult;			//temp result
	int rBound;				//if 0 stop	
	file = index1%8;
	RookFileAttacksBoard[index1][index2] = 0;
	temp = 0;
	curIndex = file;					//start at file in row 1
	Bresult = index2;					//binary no to convert to int
	binRem = 0;
	for(int i=0;i<8;i++) {   	//move left to right along row
			if(Bresult==0) break;	//if numerator zero done conversion
			tResult = Bresult/2;	//divide numberator by 2
			binRem = Bresult%2;		//store remainder
			Bresult = tResult;	
			if(binRem==1) {				//if remainder is one
				temp += powOf2[curIndex];
			}
			curIndex=curIndex+8;						// move to next position on right
	}			
		//NOW we search above ROOK
		curIndex = index1+8;			//start at first piece above Rook
		while(curIndex<64) {		//check to make sure same row
			RookFileAttacksBoard[index1][index2]+= powOf2[curIndex];	//set the position as attackable
			bit = (temp>>>curIndex)&1;
			if(bit==1) break;
			curIndex=curIndex+8;								//move right
		}
		//NOW we search below ROOK;	
		curIndex = index1-8;		//start at first piece below Rook
		while(curIndex>=0) {	//check bounds
			RookFileAttacksBoard[index1][index2]+= powOf2[curIndex];	//set the position as attackable
			bit = (temp>>>curIndex)&1;
			if(bit==1) break;
			curIndex=curIndex-8;								//move right
		}
}
public static final long getRookRank(int square,int file){
		return RookRankAttacksBoard[square][file];
	} 	
public static final long getRookFile(int square,int file){
		return RookFileAttacksBoard[square][file];
	}

}