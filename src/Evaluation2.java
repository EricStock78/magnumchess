/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Eric
 */
public class Evaluation2 {
    private static TransTable PawnTable = new TransTable(Global.PawnHASHSIZE,1);
    private static TransTable EvalTable = new TransTable(Global.EvalHASHSIZE,2);
    //private static final long centreMask = (long)1<<27 | (long)1<<28 | (long)1<<35 | (long)1<<36;
    //private static final int PROMO_IN_2 = 50;
    //private static final int PROMO_IN_1 = 120;
    private static final int[] wPassedPawnBonus = {0,7,13,17,25,50,100,0};
    private static final int[] bPassedPawnBonus = {0,100,50,25,17,13,7,0};
    private static final int CONNECTED_PASSED_BONUS = 30;
    private static final int PROTECTED_PASSED_BONUS = 15;
    private static final int PASSED_PAWN_ROOK_BONUS = 40;       
    private static final int ISOLATED_PAWN = 10;
    private static final int WEAK_PAWN = 10;
    private static final int WEAK_OPEN_PAWN = 4;
    private static final int DOUBLED_PAWN = 20;
    private static final int DOUBLED_ISOLATED_PAWN = 15;
    private static final int TRIPLE_ISOLATED_PAWN = 20;
    private static final int TRIPLE_PAWN = 50;
    private static final int MOBILITY_BONUS = 1;
    private static final int BISHOP_MOBILITY = 4;
    private static final int ROOK_MOBILITY = 1;
    private static final int KNIGHT_MOBILITY = 4;   
    private static final int QUEEN_MOBILITY = 1;
    private static final int PAWN_MOBILITY = 2;
    private static final int CENTER_BONUS = 1;
    private static final int BACKRANK_MINOR = 20;               //penalty for back rank minor piece
    private static final int TEMPO_BONUS = 10;
    private static final int ROOK_OPEN = 20;
    private static final int ROOK_SEMI = 10;
    private static final int ROOK_DOUBLED = 20;
    private static final int DOUBLED_ROOKS_7TH = 60;
    private static final int ROOK_IN_FRONT_QUEEN = 25;
    private static final int ROOK_BEHIND_QUEEN = 12;
    private static final int ROOK_7TH_RANK = 35;
    private static final int QUEEN_ROOK_7TH_RANK = 100;
    private static final int HUNG_PENALTY = 25;
    private static final int TWO_BISHOP_BONUS = 10;
    private static final int KING_PAWN_OPEN_FILE = 5;
    private static final int ENEMY_KING_PAWN_OPEN_FILE = 20;
    private static final int ENEMY_KING_PAWN_SEMI_OPEN = 8;
    //private static final int END_KING_PAWN_DEFENDER = 10;
    //private static final int END_ENEMY_KING_PAWN_ATTACKER = 10;
    
    public static byte[] WB;                                       //white attack information
    public static byte[] BB;                                       //black attack information
    private static int PAWN_BIT = 8;                            //pawn bit 2^3 in WB and BB
    private static int MINOR_BIT = 16;
    private static int ROOK_BIT = 32;
    private static int QUEEN_BIT = 64;
    private static int KING_BIT = -128;
    private static int COUNT_MASK = 7;                          //used to mask off number of attacks in WB and BB
    private static byte[] wPawnFileVals;                         //stores lowest rank for white pawns across each file
    private static byte[] bPawnFileVals;                         //stores highest rank for black pawns across each file
    private static int finalScore;
    private static int material;                                //material eval term
    private static int pawnScore;                               //pawn score eval term
    private static int passScore;                               //passed pawn bonus
    private static int weakAttackScore;                         //weak pawn attack bonus
    private static int hung;                                    //hung piece evaluation
    private static int centre;                                  //counter for attacks to center of board
    private static int mobility;                                //counter for piece mobility    
    private static int develop;                             //score for developing bishop, pawn and knight
    private static int castle;                              //castling bonus
    private static int trapped;                             //penalty for trapped pieces
    private static int wkingSafety;                          //king safety score - based on attacks to king zone
    private static int bkingSafety;
    private static int endKingSafety;
    private static int tempo;
    private static long wWeakPawns;                         //bit mask for white weak pawns
    private static long bWeakPawns;                         //bit mask for black weak pawns
    public static int bKingPos;                            //position of black king
    public static int wKingPos;                            //position of white king
    private static int bQueenPos;                           //position of black queen
    private static int wQueenPos;                           //position of white queen
    private static int noWhiteBishops;                      //counter for white bishops
    private static int noBlackBishops;                      //counter for black bishops
    private static long whiteKingZone;                      //squares of interest around white king
    private static long blackKingZone;                      //squares of interest around black king
    
    
    private static double endGameCoefficient;               //value between 0.0 and 1.1 - 1.0 represents the end game, 0 start of game
    private static int bishopEval;
    private static int knightEval;
    private static int queenEval;
    private static int rookEval;
    
    private static final int[] whitePawnProtection = {0,15,8,2,1,0,0,0};
    private static final int[] blackPawnProtection = {0,0,0,1,2,8,15,0};
    //lookup table for attacks to king zone
    private static final int[] kingSafetyEval = {   0,  2,  3,  6, 12, 18, 25, 37, 50, 75,
                                                100,125,150,175,200,225,250,275,300,325,
                                                350,375,400,425,450,475,500,525,550,575, 
                                                600,600,600,600,600};

    
    private static final byte TABLE [] = {
  //      . P N N R R R R Q Q Q Q Q Q Q Q K K K K K K K K K K K K K K K K
  //            P   P N N   P N N R R R R   P N N R R R R Q Q Q Q Q Q Q Q
  //                    P       P   N N N       P   P N N   P N N R R R R
          0,0,0,0,0,0,1,1,0,1,2,2,2,3,3,3,0,0,0,0,1,1,2,2,2,3,3,3,3,3,3,3 };

    
    
    private static final int wKnightVals[] = new int[]	{-2,-2,-2,-2,-2,-2,-2,-2,
							-2,1,1,1,1,1,1,-2,
							-2,1,4,4,4,4,1,-2,
							-2,1,5,6,6,5,1,-2,
                                                        -2,1,6,8,8,6,1,-2,
                                                        -2,1,6,8,8,6,1,-2,
							-2,1,1,1,1,1,1,-2,
                                        		-2,-2,-2,-2,-2,-2,-2,-2};													 
        
    private static final int bKnightVals[] = new int[]	{-2,-2,-2,-2,-2,-2,-2,-2,
                                                        -2,1,1,1,1,1,1,-2,
							-2,1,6,8,8,6,1,-2,
							-2,1,6,8,8,6,1,-2,
                                                        -2,1,5,6,6,5,1,-2,
							-2,1,4,4,4,4,1,-2,
							-2,1,1,1,1,1,1,-2,
							-2,-2,-2,-2,-2,-2,-2,-2};
	
    
    private static final int kingVals[] = new int[]     {-2,-2,-2,-2,-2,-2,-2,-2,
							-2,1,1,1,1,1,1,-2,
							-2,1,4,4,4,4,1,-2,
							-2,1,4,8,8,4,1,-2,
							-2,1,4,8,8,4,1,-2,
							-2,1,4,4,4,4,1,-2,
							-2,1,1,1,1,1,1,-2,
							-2,-2,-2,-2,-2,-2,-2,-2};	
    
  
    private static final int wPawnVals[] = new int[]    {0, 0, 0, 0, 0, 0, 0, 0,
                                                         0, 0, 0, -10, -10, 0, 0, 0,
                                                         1, 1, 1, 10, 10, 1, 1, 1,
                                                         3, 3, 3, 13, 13, 3, 3, 3,
                                                         6, 6, 6, 13, 13, 6, 6, 6,
                                                         10, 10, 10, 20, 20, 10, 10, 10,
                                                         10, 10, 10, 20, 20, 10, 10, 10,
                                                         0, 0, 0, 0, 0, 0, 0, 0};
    
    private static final int bPawnVals[] = new int[]     {0, 0, 0, 0, 0, 0, 0, 0,
                                                         10, 10, 10, 20, 20, 10, 10, 10,
                                                         10, 10, 10, 20, 20, 10, 10, 10,
                                                         6, 6, 6, 13, 13, 6, 6, 6,
                                                         3, 3, 3, 13, 13, 3, 3, 3,
                                                         1, 1, 1, 10, 10, 1, 1, 1,
                                                         0, 0, 0, -10, -10, 0, 0, 0,
                                                         0, 0, 0, 0, 0, 0, 0, 0};

    private static final int centreArr[] = new int[]     {0,0,0,0,0,0,0,0,
                                                        0,0,0,0,0,0,0,0,
							0,0,1,1,1,1,0,0,
							0,0,1,5,5,1,0,0,
							0,0,1,5,5,1,0,0,
							0,0,1,1,1,1,0,0,
							0,0,0,0,0,0,0,0,
							0,0,0,0,0,0,0,0,};
    
    
    private static final int wKingAttackVal[] = new int[]  {2,0,0,0,0,0,0,2,
                                                          2,1,1,1,1,1,1,2,
                                                          4,3,3,3,3,3,3,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4,
                                                          4,4,4,4,4,4,4,4};
    
    private static final int bKingAttackVal[] = new int[] {4,4,4,4,4,4,4,4,
                                                           4,4,4,4,4,4,4,4,
                                                           4,4,4,4,4,4,4,4,
                                                           4,4,4,4,4,4,4,4,
                                                           4,4,4,4,4,4,4,4,
                                                           4,3,3,3,3,3,3,4,
                                                           2,1,1,1,1,1,1,2,
                                                           2,0,0,0,0,0,0,2,};
    
    private static final int wPawnEnemyKingTropism[] = new int[] {0,0,0,0,0,0,0,0,
                                                                  1,1,1,1,1,1,1,1,
                                                                  3,3,3,3,3,3,3,3,
                                                                  5,5,5,5,5,5,5,5,
                                                                  8,8,8,8,8,8,8,8,
                                                                  15,15,15,15,15,15,15,15,
                                                                  30,30,30,30,30,30,30,30,
                                                                  0,0,0,0,0,0,0,0,};
    
    private static final int bPawnEnemyKingTropism[] = new int[] {0,0,0,0,0,0,0,0,
                                                                  30,30,30,30,30,30,30,30,
                                                                  15,15,15,15,15,15,15,15,
                                                                  8,8,8,8,8,8,8,8,
                                                                  5,5,5,5,5,5,5,5,
                                                                  3,3,3,3,3,3,3,3,
                                                                  1,1,1,1,1,1,1,1,
                                                                  0,0,0,0,0,0,0,0,};
    
    public static final void clearPawnHash() {
    	PawnTable.clearPawnHash();
    }
    public static final void clearEvalHash() {
    	EvalTable.clearEvalHash();

    }

    //this method creates a new eval hash based on input from the UCI gui
    public static final void reSizeEvalHash() {
        EvalTable = new TransTable(Global.EvalHASHSIZE,2);
    }
    //this method creates a new pawn hash based on input from the UCI gui
    public static final void reSizePawnHash() {
        PawnTable = new TransTable(Global.PawnHASHSIZE,1);
    }
    
    // method int popCount(long bitset)
    // takes a long integer and returns the number of bits set
   
    private static final int popCount(long bitset) {
	int count = 0;
	while(bitset != 0) {
            count++;
            bitset &= (bitset-1);
	}
	return count;
    }
    // int getEval(int side)
    // this calls evaluation functions and sums the terms and returns the final evaluation
    // from the point of view of the side moving
    
    public static final int getEval(int side) {
        //see if there is a stored value in the hash table
        
        int evalKey = (int)(Board.hashValue % Global.EvalHASHSIZE);
	evalKey = Math.abs(evalKey);   
        
        if(EvalTable.hasEvalHash(evalKey,Board.hashValue,Board.hashValue2)) {
           //System.out.println("eval hit");    
            return (EvalTable.getEvalValue(evalKey));
        }
        if(Board.wPieceNo == 1) {
            if(Board.bPieceNo <= 2 && Board.blackpieces == (Board.blackking | Board.blackbishops | Board.blackknights))
                return 0;
            if(Board.bPieceNo == 3 && Board.blackpieces == (Board.blackking | Board.blackknights))
                return 0;
        
        } else if(Board.bPieceNo == 1) {
            if(Board.wPieceNo <= 2 && Board.whitepieces == (Board.whiteking | Board.whitebishops | Board.whiteknights))
                return 0;
            if(Board.wPieceNo == 3 && Board.whitepieces == (Board.whiteking | Board.whiteknights))
                return 0;
        }
        
        int wfileBit;
        int bfileBit;
        long passBits;
        
        wWeakPawns = 0;
        bWeakPawns = 0;
        
        endGameCoefficient =  Math.min(1.0,(8100.0 - (double)Board.totalValue)/ 5600.0);
        //endGameCoefficient = 0.0;
        tempo = 0;
        noWhiteBishops = 0;
        noBlackBishops = 0;
        weakAttackScore = 0;
        centre = 0;
        mobility = 0;
        develop = 0;
        castle = 0;
        trapped = 0;
        wkingSafety = 0;
        bkingSafety = 0;
        endKingSafety = 0;
        
        setDevelopmentBonus();                //set the develop and castle values
        
        WB = new byte[64];
        BB = new byte[64];
        
        
        
        setPawnAttack();                //set the BB and WB for pawns 
        
        material = getMaterialScore(side);  //the material advantage
        
        //see if there is a stored value in the pawn hash table
        
        long hash = Board.getPawnHash();        
	int key = (int)(hash % Global.PawnHASHSIZE);
	key = Math.abs(key);
	
        boolean hasHash = PawnTable.hasPawnHash(key,Board.getPawnHash2());	
	
        if(hasHash) {
            //System.out.println("pawn hit");
       
            wfileBit = PawnTable.getWPawnFile(key);
            /*
            if(wfileBit != getWhitePawnFile()) {
                System.out.println("key is "+key);
                //System.out.println("hash1 is "+Board.getPawnHash2());
                //System.out.println("hash2 is "+hash);
                System.out.println("white pawns are "+Board.whitepawns);
                System.out.println("black pawns are "+Board.blackpawns);
                System.out.println("wfileBit is "+wfileBit);
                System.out.println("info string pawn file white is are "+getWhitePawnFile());
                System.out.println("info string ERROR!!!!!!!1"+(5/0));
            }
             */ 
            bfileBit = PawnTable.getBPawnFile(key);
            /*
            if(bfileBit != getBlackPawnFile()) {
                
                System.out.println("info string pawn file black is are "+getBlackPawnFile());
                //System.out.println("info string ERROR!!!!!!!1"+(5/0));
            }
             */
            bitToArrayWhitePawnFile(wfileBit);
            bitToArrayBlackPawnFile(bfileBit);
            pawnScore = PawnTable.getPawnValue(key);
            //if(pawnScore != (getWPawnsScore2() + getBPawnsScore2()))
            //    System.out.println("info string pawn score ERROR!!!!!!!1"+(5/0));
            passBits = PawnTable.getPawnPassed(key);
            /*
            if(passBits != getPassedPawnsBits()) {
                System.out.println("info string pawn bits are "+getPassedPawnsBits());
                System.out.println("info string ERROR!!!!!!!1"+(5/0));
            }
             */
        
        
       
         } else {
            
            wfileBit = getWhitePawnFile();
            bfileBit = getBlackPawnFile();
            bitToArrayWhitePawnFile(wfileBit);
            bitToArrayBlackPawnFile(bfileBit);
            pawnScore = getWPawnsScore2();
            pawnScore += getBPawnsScore2();
            passBits = getPassedPawnsBits();
            PawnTable.addPawnHash(key, (int)Board.getPawnHash2(), pawnScore, wfileBit, bfileBit, passBits);
        }
        
       
        setWKingEval();
        setBKingEval();
        setTrapPenalties();
        
        knightEval = getWKnightEval();
        knightEval += getBKnightEval();
        queenEval = getWQueenEval();
        queenEval += getBQueenEval();
        bishopEval = getWBishopEval();
        bishopEval += getBBishopEval();
        rookEval = getWRookEval();
        rookEval += getBRookEval();
        
        passScore = getWPassedScore3(passBits);
        passScore += getBPassedScore3(passBits);
        
        if(side == 1) {
            tempo = TEMPO_BONUS;
            hung = blackHungPieces();
        }else {
           tempo = -TEMPO_BONUS;
           hung = whiteHungPieces();
       }
        
        if(Board.totalValue > 2500) {
                wkingSafety = getKingSafetyWhite();
                bkingSafety = getKingSafetyBlack();
               weakAttackScore += getWeakAttackScore();
        }else {
            endKingSafety = getEndGameKing();
            weakAttackScore += getWeakAttackScore();
            weakAttackScore *= 2;
        }
        
             
        finalScore = side * (material + pawnScore + weakAttackScore + (mobility * MOBILITY_BONUS) + (centre * CENTER_BONUS) + develop + castle + trapped + wkingSafety + bkingSafety + endKingSafety +tempo + hung + passScore + bishopEval + knightEval + rookEval + queenEval);
        /*
        if(EvalTable.hasEvalHash(evalKey,Board.hashValue,Board.hashValue2)) {
            //System.out.println("hit");    
            if (EvalTable.getEvalValue(evalKey) != finalScore) {
                  
                if(EvalTable.getEvalExtra1(evalKey) != castle)    { 
                    System.out.println("!!!!!!!!castle error");
                    System.out.println("castle is "+castle);
                    System.out.println("hashed castle is "+EvalTable.getEvalExtra1(evalKey));
                    System.out.println("white castle is "+Board.wCastle);
                    System.out.println("hashed white castle is "+EvalTable.getEvalExtra2(evalKey));
                    
                    System.out.println("black castle is "+Board.bCastle);
                    System.out.println("hashed white castle is "+EvalTable.getEvalExtra3(evalKey));
                }
                //if(EvalTable.getEvalExtra1(evalKey) != develop ||  EvalTable.getEvalExtra2(evalKey) != castle || EvalTable.getEvalExtra3(evalKey) != trapped || EvalTable.getEvalExtra4(evalKey) != wkingSafety) 
                //    System.out.println("info string !!!!!!!!!!!!!!!!!Error isolated");
                System.out.println("info string table value is "+EvalTable.getEvalValue(evalKey));
                System.out.println("info string final score is "+finalScore);
                System.out.println("info string key is "+evalKey);
                
                System.out.println("info string !!!!!!!!!!!!!!!!!!!!!!!1");
                System.out.println("info string ERROR!!!!!!!1"+(5/0));
            }
         }
        */
        EvalTable.addEvalHash(evalKey, Board.hashValue, Board.hashValue2, finalScore);
       
        //printEvalTerms();
        return finalScore;
    }
     
    
    // static final void printEvalTerms()
    // used to debug the evaluation routine by examining each term
    
    private static final void printEvalTerms() {
        
        System.out.println("score is "+finalScore); 
        System.out.println("material is "+material);
        System.out.println("pawn score is "+pawnScore);
        System.out.println("weak attack is "+weakAttackScore);
        System.out.println("mobility is "+mobility * MOBILITY_BONUS);
        System.out.println("center is "+centre * CENTER_BONUS);
        System.out.println("develop is "+develop);
        System.out.println("castle is "+castle);
        System.out.println("trapped is "+trapped);
        System.out.println("whtie king safety is "+wkingSafety);
        System.out.println("black king safety is "+bkingSafety);
        System.out.println("tempo is "+tempo);
        System.out.println("hung is "+hung);
        System.out.println("pass score is "+passScore);
        System.out.println("bishop eval is "+bishopEval);
        System.out.println("knight eval is "+knightEval);
        System.out.println("rook eval is "+rookEval);
        System.out.println("queen eval is "+queenEval);
        System.out.println("end game coefficient is "+endGameCoefficient);
        System.out.println("total value is "+Board.totalValue);
    }
    
    
    // void setTrapPenalties
    // method recognizes several trapped piece patterns for bishops and rooks
    // penalties are given
    
    
    private static final void setTrapPenalties() {
       if((Board.whitebishops & Global.set_Mask[48]) != 0) {					//bishop may be trapped		
            if (((Board.blackpawns & Global.set_Mask[41]) != 0) && ((Board.blackpawns & Global.set_Mask[50]) != 0))
                trapped += 150;
        }		
	if((Board.whitebishops & Global.set_Mask[55]) != 0) {					//bishop may be trapped			
            if (((Board.blackpawns & Global.set_Mask[53]) != 0) && (Board.blackpawns & Global.set_Mask[46]) != 0)
                trapped += 150;
        }
        if((Board.blackbishops & Global.set_Mask[15]) != 0) {
            if(((Board.whitepawns & Global.set_Mask[13])!= 0) && ((Board.whitepawns & Global.set_Mask[22])!= 0))
		trapped -= 150;				//trapped bishop and may be captured
	}
	if((Board.blackbishops & Global.set_Mask[8]) != 0) {		
            if((Board.whitepawns & Global.set_Mask[10]) != 0 && ((Board.whitepawns & Global.set_Mask[17])!= 0))
                trapped -= 150;				//trapped bishop and may be captured
	}
	if(Board.wCastle == Global.NO_CASTLE) {
            if((Board.whiterooks & Global.wRookTrap[0]) != 0) {
		if(wKingPos < 4 && wKingPos > 0)
                    trapped += 50;
            }
            if((Board.whiterooks & Global.wRookTrap[1]) != 0) {
		if(wKingPos > 4 && wKingPos < 7)
                    trapped += 50;
            }
	}
	if(Board.bCastle == Global.NO_CASTLE) {
            if((Board.blackrooks & Global.bRookTrap[0]) != 0) {
		if(bKingPos < 60 && bKingPos > 56)
                    trapped -= 50;
            }
            if((Board.blackrooks & Global.bRookTrap[1]) != 0)  {
		if(bKingPos > 60 && bKingPos < 63)
                    trapped -= 50;
            }
	}     
    }
    
       // void setPawnAttack() 
       // this method sets the WB and BB structures for pawns
    
    private static final void setDevelopmentBonus() {
        if(Board.wCastle > Global.CASTLED && Board.bCastle > Global.CASTLED) {
            if((Board.whitequeen & Global.set_Mask[3])!= 0)
		develop-=20;
            if((Board.blackqueen & Global.set_Mask[59]) != 0)
                develop+=20;
       }	
      
       switch(Board.wCastle) {
            case(Global.CASTLED):
                    castle -= 40;
                    break;
            case(Global.SHORT_CASTLE):
                    castle -= 10;	
                    break;	
            case(Global.LONG_CASTLE):
                    castle -= 10;
                    break;
            case(Global.BOTH_CASTLE):	
                    castle -= 20;
                    break;		
            }
		
	switch(Board.bCastle) {
            case(Global.CASTLED):
                    castle += 40;
                    break;
            case(Global.SHORT_CASTLE):
                    castle += 10;	
                    break;	
            case(Global.LONG_CASTLE):
                    castle += 10;
                    break;
            case(Global.BOTH_CASTLE):	
                    castle += 20;
                    break;		
	}
	/*
        if((Board.blackpawns&Global.set_Mask[51])!=0) 
            develop-=15;	
	if((Board.blackpawns&Global.set_Mask[52])!=0) 
            develop-=15;
	if((Board.whitepawns&Global.set_Mask[11])!=0) 
            develop+=15;
        if((Board.whitepawns&Global.set_Mask[12])!=0) 
            develop+=15;
	*/
    }
    
    
    private static final void setPawnAttack() {
        long leftAttack = Board.whitepawns << 7 & ~Global.fileMasks[7];
        int position;
        while(leftAttack != 0) {
            long move = leftAttack & -leftAttack;
            leftAttack ^= move;
            position = Board.getPos(move);
            centre -= centreArr[position];
            WB[position]++;                                 //increment number of attackers
            WB[position] |= PAWN_BIT;
        }
        long rightAttack = Board.whitepawns << 9 & ~Global.fileMasks[0];
        while(rightAttack != 0) {
            long move = rightAttack & -rightAttack;
            rightAttack ^= move;
            position = Board.getPos(move);
            centre -= centreArr[position];
            WB[position]++;                                 //increment number of attackers
            WB[position] |= PAWN_BIT;
        }
        
        long forwardAttack = Board.whitepawns << 8;
        while(forwardAttack != 0) {
            long move = forwardAttack & -forwardAttack;
            forwardAttack ^= move;
            position = Board.getPos(move);
            if(Board.piece_in_square[position] == -1)
                mobility -= PAWN_MOBILITY;
        }
        
        leftAttack = Board.blackpawns >>> 9 & ~Global.fileMasks[7];
        while(leftAttack != 0) {
            long move = leftAttack & -leftAttack;
            leftAttack ^= move;
            position = Board.getPos(move);
            centre += centreArr[position];
            BB[position]++;                                 //increment number of attackers
            BB[position] |= PAWN_BIT;
        }
        rightAttack = Board.blackpawns >>> 7 & ~Global.fileMasks[0];
        while(rightAttack != 0) {
            long move = rightAttack & -rightAttack;
            rightAttack ^= move;
            position = Board.getPos(move);
            centre += centreArr[position];
            BB[position]++;                                 //increment number of attackers
            BB[position] |= PAWN_BIT;
        }
    
        forwardAttack = Board.blackpawns >>> 8;
        while(forwardAttack != 0) {
            long move = forwardAttack & -forwardAttack;
            forwardAttack ^= move;
            position = Board.getPos(move);
            if(Board.piece_in_square[position] == -1)
                mobility += PAWN_MOBILITY;
            
        }
    
    }
    
    // getMaterialScore(int side)
    //method calculates the material score
    //encourages side ahead to trade material not pawns
    
    public static final int getMaterialScore(int side) {
        return Board.getValue();
    }
    
    // int getWhitePawnFile
    // this method calculates an integer bit string based on the state of the white pawn file
    // value is 7 if no pawn exists on the file
    
    private static final int getWhitePawnFile() {
        
        int wPawnFile = 0;       
        for(int i=0; i<=7; i++) {       //compute the lowest rank of white pawns in each file
            
            long temp = Board.whitepawns & Global.fileMasks[i];
            if(temp != 0) {
               long temp2 = temp & -temp; 
               temp ^= temp2;
               wPawnFile |= (Board.getPos(temp2)/8)<<(3*i);         //shift to position for file
            } else 
               wPawnFile |= 7<<(3*i);
        }
        return wPawnFile;
    }
    
    // int getBlacPawnFile
    // this method calculates an integer bit string based on the state of the black pawn file
    // value is 0 if no pawn exists on the file
    
    private static final int getBlackPawnFile() {
        
        int bPawnFile = 0;       
        for(int i=0; i<=7; i++) {       //compute the highest rank of enemy pawns in each file
            
            long temp = Board.blackpawns & Global.fileMasks[i];
            while(temp != 0) {
               long temp2 = temp & -temp;
               temp ^= temp2;
               if(temp == 0)
                   bPawnFile |= (Board.getPos(temp2)/8)<<(3*i);
            }
        }     
        return bPawnFile;
    }    
    
    // void bitToAttayWhitePawnFile(int fileBit)
    // this method takes the bit string representing the most forward pawn position on the file
    // and sets up an array that will be used in the rest of the evaluation
    
    private static final void bitToArrayWhitePawnFile(int fileBit) {
        
        wPawnFileVals = new byte[8];
        
        for(int i=0; i<=7; i++) {       //compute the highest rank of enemy pawns in each file
            wPawnFileVals[i] = (byte)((fileBit >>> (3*i)) & 7);    
        }
    }     
        
    // void bitToAttayBlackPawnFile(int fileBit)
    // this method takes the bit string representing the most forward pawn position on the file
    // and sets up an array that will be used in the rest of the evaluation
    
    private static final void bitToArrayBlackPawnFile(int fileBit) {
        
        bPawnFileVals = new byte[8];
        
        for(int i=0; i<=7; i++) {       //compute the highest rank of enemy pawns in each file
            bPawnFileVals[i] = (byte)((fileBit >>> (3*i)) & 7);    
        }
    }      
    
    // getWPawnsScore2
    // this method calculates penalties for doubled pawns, isolated pawns, weak pawns and the positional score
    // for white pawns
    
    private static final int getWPawnsScore2() {
        
        long piece;
	
        int position;
        
        int isolatedScore = 0;          //isolated pawn score
        int doubledScore = 0;           //doubled pawn score
        int weakScore = 0;              //backward pawn score
        int positionScore = 0;          //positional piece square table score
        
        int doubleCount = 0;
        boolean tempWeak;
        boolean isolated;
        long file;
        
        for(int i = 0 ; i <= 7 ; i++ ) {
            file = Board.whitepawns & Global.fileMasks[i];
            isolated = true;
            doubleCount = 0;
            if( i > 0 && wPawnFileVals[i-1] < 7 ) 
                isolated = false;
            else if( i < 7 && wPawnFileVals[i+1] < 7 )
                isolated = false;
            
            while(file != 0) {
                piece = file & -file;
                file ^= piece;
                position = Board.getPos(piece);
                positionScore -= wPawnVals[position];
                if(doubleCount == 1)
                    doubledScore += DOUBLED_PAWN;
                else if(doubleCount > 1)
                    doubledScore += TRIPLE_PAWN;
                doubleCount++;
                if(isolated)
                    isolatedScore += ISOLATED_PAWN;
                if(isolated) continue;
                
                if(WB[position] == 0 && BB[position] == 0) {           //if pawn is not isolated, check for weakness if pawn not defended or attacked
                    tempWeak = true;
            
                    if(Board.piece_in_square[position+8] != 5 && (WB[position+8] >= BB[position+8]))   //if pawn can advance not weak
                        tempWeak = false;
                    else if(tempWeak & position/8 >= 3 ) {  //if a pawn behind can advance to help not weak
                        if(((i - 1) >= 0) && Board.piece_in_square[position-17] == 5) {     //check for friend to left
                            //check to see if this friend pawn can advance to support the pawn
                            if((Board.piece_in_square[position - 9] != 11) && (WB[position-9] >= BB[position-9]))
                                tempWeak = false;
                        }
                        if(tempWeak == true && (i + 1 <= 7) && Board.piece_in_square[position-15] == 5) {//check for friend to right
                             //check to see if this friend pawn can advance to support the pawn
                            if((Board.piece_in_square[position - 7] != 11) && (WB[position-7] >= BB[position-7]))
                                tempWeak = false;
                        }
                    }
                    if(tempWeak) {
                        if(bPawnFileVals[i] == 0)           //if weak pawn is on an open file - bad
                            weakScore += WEAK_OPEN_PAWN;
                        weakScore += WEAK_PAWN;  
                    }    
                    
                } 
            }
        }
      
        //System.out.println("WHITE isolated score is "+isolatedScore);
        //System.out.println("WHITE weakScore is "+weakScore);
        //System.out.println("WHITE positionScore is "+positionScore);
        //System.out.println("WHITE doubledScore is "+doubledScore);
        return isolatedScore + weakScore + positionScore + doubledScore;                       
    }			
    
    // getBPawnsScore2
    // this method calculates penalties for doubled pawns, isolated pawns, weak pawns and the positional score
    // for Black pawns
    
    private static final int getBPawnsScore2() {
        
        long piece;
	
        int position;
        
        int isolatedScore = 0;          //isolated pawn score
        int doubledScore = 0;           //doubled pawn score
        int weakScore = 0;              //backward pawn score
        int positionScore = 0;          //positional piece square table score
        
        int doubleCount = 0;
        boolean tempWeak;
        boolean isolated;
        long file;
        
        for(int i = 0 ; i <= 7 ; i++ ) {
            file = Board.blackpawns & Global.fileMasks[i];
            isolated = true;
            doubleCount = 0;
            if( i > 0 && bPawnFileVals[i-1] > 0 ) 
                isolated = false;
            else if( i < 7 && bPawnFileVals[i+1] > 0 )
                isolated = false;
            
            while(file != 0) {
                piece = file & -file;
                file ^= piece;
                position = Board.getPos(piece);
                positionScore += bPawnVals[position];
                if(doubleCount == 1)
                    doubledScore -= DOUBLED_PAWN;
                else if(doubleCount > 1)
                    doubledScore -= TRIPLE_PAWN;
                doubleCount++;
                if(isolated)
                    isolatedScore -= ISOLATED_PAWN;
                if(isolated) continue;
                
                if(BB[position] == 0 && WB[position] == 0) {           //if pawn is not isolated, check for weakness if pawn not defended or attacked
                    tempWeak = true;
            
                    if(Board.piece_in_square[position-8] != 11 && (BB[position-8] >= WB[position-8]))   //if pawn can advance not weak
                        tempWeak = false;
                    else if(tempWeak & position/8 <= 4 ) {  //if a pawn behind can advance to help not weak
                        if(((i - 1) >= 0) && Board.piece_in_square[position+15] == 11) {     //check for friend to left
                            //check to see if this friend pawn can advance to support the pawn
                            if((Board.piece_in_square[position + 7] != 5) && (BB[position+7] >= WB[position+7]))
                                tempWeak = false;
                        }
                        if(tempWeak == true && (i + 1 <= 7) && Board.piece_in_square[position+17] == 11) {//check for friend to right
                             //check to see if this friend pawn can advance to support the pawn
                            if((Board.piece_in_square[position + 9] != 5) && (BB[position+9] >= WB[position+9]))
                                tempWeak = false;
                        }
                    }
                    if(tempWeak) {
                        if(wPawnFileVals[i] == 7)           //if weak pawn is on an open file - bad
                            weakScore -= WEAK_OPEN_PAWN;
                        weakScore -= WEAK_PAWN;  
                    }    
                    
                } 
            }
        }
      
        //System.out.println("BLACK isolated score is "+isolatedScore);
        //System.out.println("BLACK weakScore is "+weakScore);
        //System.out.println("BLACK positionScore is "+positionScore);
        //System.out.println("BLACK doubledScore is "+doubledScore);
        return isolatedScore + weakScore + positionScore + doubledScore;                       
    }			
    
    
    // long getPassedPawnBits
    // method returns a bit string representing all passed pawns on the board
    
    private static final long getPassedPawnsBits() {
        
        long passers = 0;
        
        long pawns = Board.whitepawns;
        long piece;
        int position;
        boolean passed;
        int rank;
        int file;
        while (pawns != 0) {
            
            passed = false;
			
            piece = pawns&(-pawns);
            pawns ^= piece;
            position = Board.getPos(piece); 
			
            rank = position / 8;                //get rank of pawn we are testing
			
            file = position % 8;                //file of pawn testing
            if (file == 0) {
                 if(bPawnFileVals[file] <= rank && bPawnFileVals[file+1] <= rank )
                      passed = true;
            } else if(file == 7) {
                 if(bPawnFileVals[file] <= rank && bPawnFileVals[file-1] <= rank )
                      passed = true;
            } else {
                if(bPawnFileVals[file] <= rank && bPawnFileVals[file-1] <= rank && bPawnFileVals[file+1] <= rank )
                     passed = true;
            }        
            if(passed) 
                passers |= piece;
        }       
        pawns = Board.blackpawns;
        
        while (pawns != 0) {
            
            passed = false;
            
            piece = pawns&(-pawns);
            pawns ^= piece;
            position = Board.getPos(piece); 
			
            rank = position / 8;                //get rank of pawn we are testing
			
            file = position % 8;                //file of pawn testing
            if (file == 0) {
                 if(wPawnFileVals[file] >= rank && wPawnFileVals[file+1] >= rank )
                      passed = true;
            } else if(file == 7) {
                 if(wPawnFileVals[file] >= rank && wPawnFileVals[file-1] >= rank )
                      passed = true;
            } else {
                if(wPawnFileVals[file] >= rank && wPawnFileVals[file-1] >= rank && wPawnFileVals[file+1] >= rank )
                     passed = true;
            }        
            if(passed) 
                passers |= piece;    
        }          
        //while(passers != 0) {
        //   piece = passers & -passers;
        //    passers ^= piece;
        //    System.out.println("passed pawn is "+Board.getPos(piece));
        //}
        
        return passers;
    }
    
    // int getWPassedScore(long passers)
    // calculates the passed pawn bonus for white passed pawns 
    // based on the bit string passers
    
    private static final int getWPassedScore3(long passers) {
        long piece;
        int position;
        int rank;
        int file;
        int currentScore = 0;
        int totalPass = 0;
        passers &= Board.whitepawns;            //get only the white pawns
        long whitePassers = passers;            //whitePassers keeps original white passed pawns
        while(passers != 0) {
            piece = passers & -passers;
            passers ^= piece;
            position = Board.getPos(piece);
            long rookAttacks = Board.getMagicRookMoves(position);
            rank = position / 8;                //get rank of pawn we are testing		
            file = position % 8;      
            currentScore = -wPassedPawnBonus[rank];	
            currentScore -= (int)((double)(10 + rank * 6) * endGameCoefficient);    //interpolated endgame bonus     
            if(( file-1 >= 0) && ((whitePassers & Global.fileMasks[file-1]) != 0)) {  //if passed pawn beside current pawn
                currentScore -= 10 + CONNECTED_PASSED_BONUS * endGameCoefficient;
            } else {
                if((file-1 >= 0) && (Board.piece_in_square[position-9] == 5 || Board.piece_in_square[position-1] == 5))
                    currentScore -= 5 + PROTECTED_PASSED_BONUS * endGameCoefficient;
            }
            if(( file+1 <= 7) && ((whitePassers & Global.fileMasks[file+1]) != 0)) {
                currentScore -= 10 + CONNECTED_PASSED_BONUS * endGameCoefficient;
            } else {
                if(( file+1 <= 7) && (Board.piece_in_square[position-7] == 5 || Board.piece_in_square[position+1] == 5))   
                   currentScore -= 5 + PROTECTED_PASSED_BONUS * endGameCoefficient;  
            }
            
            if((rookAttacks & Global.minus8[position] & Board.whiterooks) != 0)
                currentScore +=  currentScore* 0.2 * endGameCoefficient;	
            if(Board.piece_in_square[position+8] >= 0) {        //if the pawn is blocked
                if(Board.piece_in_square[position+8] > 5)               //blocked by an enemy piece
                    currentScore *= 0.6;
                else
                    currentScore *= 0.8;
            }      
            if(BB[position+8] != 0) 
                currentScore *= 0.85;
            if(WB[position+8] >= MINOR_BIT)
                currentScore *= 1.15;
            if(endGameCoefficient >= 0.99) {             //if this is the endgame
                if((WB[position] & KING_BIT) != 0)         
                    currentScore -= wPassedPawnBonus[rank]/2;	
                else if((WB[position+8] & KING_BIT) != 0)
                    currentScore -= wPassedPawnBonus[rank]/2;	
            }
            if(Board.totalValue < 1500) {           //late end game 
                int x = Math.abs(bKingPos%8 - file);
                int y = Math.abs(bKingPos/8 - rank);
                if(y > x)
                    x = y;
                currentScore -= x * wPawnEnemyKingTropism[position];
                
            }
            
            if((Global.plus8[position] & Board.whitepawns) != 0)            //doubled pawn - divide bonus in half
                currentScore /= 3;    
            
             
            totalPass += currentScore;
        }
        return totalPass;
    }   
    
    // int getBPassedScore(long passers)
    // calculates the passed pawn bonus for black passed pawns 
    // based on the bit string passers
    
    private static final int getBPassedScore3(long passers) {
        long piece;
        int position;
        int rank;
        int file;
        int currentScore = 0;
        int totalPass = 0;
        passers &= Board.blackpawns;            //get only the black passed pawns
        long blackPassers = passers;            //blackPassers keeps original black passed pawns
        while(passers != 0) {
            piece = passers & -passers;
            passers ^= piece;
            position = Board.getPos(piece);
            long rookAttacks = Board.getMagicRookMoves(position);
            rank = position / 8;                //get rank of pawn we are testing
            file = position % 8;                //file of pawn testing   
            currentScore = bPassedPawnBonus[rank];
            currentScore += (int)((double)(10 + (7-rank) * 6) * endGameCoefficient);    // interpolated endgame bonus
            if(( file-1 >= 0) && ((blackPassers & Global.fileMasks[file-1]) != 0)) {
                currentScore += 10 + CONNECTED_PASSED_BONUS * endGameCoefficient;
            } else {
                if((file-1 >= 0) && (Board.piece_in_square[position+7] == 11 || Board.piece_in_square[position-1] == 11))
                    currentScore += 5 + PROTECTED_PASSED_BONUS * endGameCoefficient;
            }    
            if(( file+1 <= 7) && ((blackPassers & Global.fileMasks[file+1]) != 0)) {
                 currentScore += 10 + CONNECTED_PASSED_BONUS * endGameCoefficient;   
            } else {
               if(( file+1 <= 7) && (Board.piece_in_square[position+9] == 11 || Board.piece_in_square[position+1] == 11))
                    currentScore += 5 + PROTECTED_PASSED_BONUS * endGameCoefficient;
            }
            if((rookAttacks & Global.plus8[position] & Board.blackrooks) != 0)  //if a rook supports the pawn along the file, bonus
                currentScore += currentScore * 0.2 * endGameCoefficient;
            if(Board.piece_in_square[position-8] >= 0) {        //if the pawn is blocked
                if(Board.piece_in_square[position-8] < 6)               //blocked by an enemy piece
                    currentScore *= 0.6;
                else
                    currentScore *= 0.8;
            }          
            if(WB[position-8] != 0) 
               currentScore *= 0.85;
            if(BB[position-8] >= MINOR_BIT)
               currentScore *= 1.15;        
            if(endGameCoefficient >= 0.99) {             //if this is the endgame
                if((BB[position] & KING_BIT) != 0)
                    currentScore += bPassedPawnBonus[rank]/2;
                else if((BB[position-8] & KING_BIT) != 0)
                    currentScore += bPassedPawnBonus[rank]/2;
                
            }
            if(Board.totalValue < 1500) {           //late end game 
                int x = Math.abs(wKingPos%8 - file);
                int y = Math.abs(wKingPos/8 - rank);
                if(y > x)
                    x = y;
                currentScore += x * bPawnEnemyKingTropism[position];
                
            }
            if((Global.minus8[position] & Board.blackpawns) != 0)            //doubled pawn - divide bonus in half
                currentScore /= 3;     
            totalPass += currentScore;     
        }                   
        return totalPass;  
        
     }
    
    
    // int getWeakAttackScore
    // calculates the bonus for enemy attacks against a weak pawn
    
    private static final int getWeakAttackScore() {
        
        long pawns = wWeakPawns;
        long pawn;
        int position;
        int weakAttack = 0;
        
        while(pawns != 0) {
            pawn = pawns & -pawns;
            pawns ^= pawn;
            position = Board.getPos(pawn);
            if((BB[position] & PAWN_BIT) != 0) continue;       //if white pawn can capture a black pawn no penalty
            if(BB[position] == 0) continue;                    //if no black attackers then no penalty
            int defenders = WB[position]>>3;
            int attackers = BB[position]>>3;       
            defenders = defenders & -defenders;             //get smallest defender
            attackers = attackers & -attackers;             //get smallest attacker         
            if(defenders < attackers) continue;          //dont care if bishop defendes pawn attacked by rook etc    
            weakAttack += 5 * (BB[position] & 7);       //award a penalty of 10 for each attacker on the weak pawn
           
        }
      
        
        pawns = bWeakPawns;
        while(pawns != 0) {
            pawn = pawns & -pawns;
            pawns ^= pawn;
            position = Board.getPos(pawn);
            if((WB[position] & PAWN_BIT) != 0) continue;       //if black pawn can capture a black pawn no penalty
            if(WB[position] == 0) continue;                    //if no white attackers then no penalty
            int defenders = BB[position]>>3;
            int attackers = WB[position]>>3;       
            defenders = defenders & -defenders;             //get smallest defender
            attackers = attackers & -attackers;             //get smallest attacker         
            if(defenders < attackers) continue;          //dont care if bishop defendes pawn attacked by rook etc    
            weakAttack -= 5 * (WB[position] & 7);       //award a penalty of 10 for each attacker on the weak pawn
           
        }
   
        return weakAttack;
    }
    
    // int getWKnightEval()
    // calculates - mobility, centre attack, tropism, development, piece square score 
    // for white knight
    
    private static int getWKnightEval() {
        long knights = Board.whiteknights;
        int position;
        long attacks;
        int score = 0;
        
        while(knights != 0) {
            long piece = knights & -knights;
            knights ^= piece;
            position = Board.getPos(piece);
            if(position/8 == 0)                         //knight on 1st rank
                develop += BACKRANK_MINOR;              //penalize
            attacks = Board.getKnightMoves(position);
            while(attacks != 0) {                       //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Board.getPos(attack);
               centre -= centreArr[pos];
               if(Board.piece_in_square[pos] == -1 || Board.piece_in_square[pos] > 5 )  {     //if not attacking own piece add mobility
                    if((BB[pos] & PAWN_BIT) == 0)
                        mobility-= KNIGHT_MOBILITY;
               }
               WB[pos]++;
               WB[pos] |= MINOR_BIT; 
            }
            score -= wKnightVals[position];
            score += Board.getDistance(position, bKingPos);
            
        }
        return score;
    }   
       
    
    // int getBKnightEval()
    // calculates - mobility, centre attack, tropism, development, piece square score 
    // for black knight
    
    private static int getBKnightEval() {
        long knights = Board.blackknights;
        int position;
        long attacks;
        int score = 0;
        while(knights != 0) {
            long piece = knights & -knights;
            knights ^= piece;
            position = Board.getPos(piece);
            if(position/8 == 7)                         //knight on 8th rank
                develop -= BACKRANK_MINOR;              //penalize
            attacks = Board.getKnightMoves(position);
            while(attacks != 0) {                       //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Board.getPos(attack);
               centre += centreArr[pos];
               if(Board.piece_in_square[pos] < 6)  {     //if not attacking own piece add mobility
                    if((WB[pos] & PAWN_BIT) == 0)
                        mobility+= KNIGHT_MOBILITY;
               }
               BB[pos]++;
               BB[pos] |= MINOR_BIT; 
            }
            score += bKnightVals[position];
            score -= Board.getDistance(position, wKingPos);
            
        }
        return score;
        
    }
    
    // int getWQueenEval
    // calculates - centre score, mobility, enemy king attack, tropism and 
    // fils out WB structure for white queen
    
    private static int getWQueenEval() {
        long queens = Board.whitequeen;
        int position;
        long attacks;
        int score = 0;
        while(queens != 0) {
            long piece = queens & -queens;
            queens ^= piece;
            position = Board.getPos(piece);
            attacks = Board.getQueenMoves(position);                //get attacks for queen
            while(attacks != 0) {                                   //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Board.getPos(attack);
               centre -= centreArr[pos];
               if(Board.piece_in_square[pos] < 0 || Board.piece_in_square[pos] >5)
                   mobility -= QUEEN_MOBILITY;
               
               WB[pos]++;
               WB[pos] |= QUEEN_BIT; 
            }
            score += 3 * Board.getDistance(position, bKingPos);        //tropism
        }
        
        
        return score;
    }
    
    // int getBQueenEval
    // calculates - centre score, mobility, enemy king attack, tropism and 
    // fils out WB structure for black queen
    
    private static int getBQueenEval() {
        long queens = Board.blackqueen;
        int position;
        long attacks;
        int score = 0;
        while(queens != 0) {
            long piece = queens & -queens;
            queens ^= piece;
            position = Board.getPos(piece);
            attacks = Board.getQueenMoves(position);            //get queen moves
            while(attacks != 0) {                       //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Board.getPos(attack);
               centre += centreArr[pos];
               if(Board.piece_in_square[pos] < 6)
                   mobility += QUEEN_MOBILITY;
               BB[pos]++;
               BB[pos] |= QUEEN_BIT; 
            }
            score -= 3 * Board.getDistance(position, wKingPos);        //tropism
        }
        
        return score;
    }
    
    
    // void setWKingEval()
    // sets the white king position, attack zone, and updates the WB table
    
    private static void setWKingEval() {
        long king = Board.whiteking;
        long attacks;
        wKingPos = Board.getPos(king);                          //get position of white king
        int kingFile = wKingPos%8;
        if(kingFile < 3)                                        //if king in corner, use corner king zone
            kingFile = 0;
        else if(kingFile > 4)
            kingFile = 7;
        if((wKingPos/8) < 6)                                    //set the white king zone
            whiteKingZone = Global.wKingMask[kingFile]<<((wKingPos/8)*8);
        else
            whiteKingZone = 0;
        attacks = Board.getAttackBoard(wKingPos);
        while(attacks != 0) {                       //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Board.getPos(attack);
               WB[pos]++;                          
               WB[pos] |= KING_BIT; 
        }
    }

    
    // void setBKingEval()
    // sets the black king position, attack zone, and updates the BB table
    
    private static void setBKingEval() {
        long king = Board.blackking;
        long attacks;
        bKingPos = Board.getPos(king);                          //get position of white king
        int kingFile = bKingPos%8;
        if (kingFile < 3)
            kingFile = 0;
        else if(kingFile > 4)
            kingFile = 7;
        if((bKingPos/8) > 1)                                    //set the black king zone
            blackKingZone = Global.bKingMask[kingFile]>>>((7-(bKingPos/8))*8);
        else
            blackKingZone = 0;
        attacks = Board.getAttackBoard(bKingPos);
        while(attacks != 0) {                       //update the BB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Board.getPos(attack);
               BB[pos]++;                          
               BB[pos] |= KING_BIT; 
        }
    }
    
    
    private static int getEndGameKing() {
        int score = 0;
        score -= kingVals[wKingPos];
        score += kingVals[bKingPos];
        return score;
    }
    
    private static int getKingSafetyWhite() {
        int score = 0;
        int attackScore = 0;
        long pawnProtect;
        int pawnShieldScore = 0;
        int openFilePenalty = 0;
        int file;
        int rank;
        int position;
        
        
        if(Board.wCastle <= Global.CASTLED || Board.bCastle <= Global.CASTLED) {
             pawnProtect = whiteKingZone & Board.whitepawns;
        
            while(pawnProtect != 0) {
                long pawn = pawnProtect & -pawnProtect;
                pawnProtect ^= pawn;
                position = Board.getPos(pawn);
                pawnShieldScore += whitePawnProtection[position/8];    
            }
            score -= pawnShieldScore;
            file = wKingPos%8;
            rank = wKingPos/8;
            
       
        if(Board.blackqueen != 0 && endGameCoefficient <=  0.6) {    
             int counter = 0;
             int mask = 0;
            
            //collect the scores of the squares two positions in front of the king
            
             if(rank < 6) {
                if(file - 1 >= 0) 
                    mask |= BB[wKingPos+15];
                if(file + 1 <= 7) 
                    mask |= BB[wKingPos+17];
                mask |= BB[wKingPos+16];
            }
            
                //collect the scores of the squares beside and behind the white king
            if(file - 1 >= 0) {
                if(BB[wKingPos-1] > 0) {        //if square beside king attacked
                    mask |= BB[wKingPos-1];
                    counter++;                  //increment counter
                    if(WB[wKingPos-1] == -127)   //if square beside king attacked and not defended
                        counter++;              //increment counter
                }
                if(rank > 0) { 
                    if(BB[wKingPos-9] > 0) {        //if square behind  king attacked
                        mask |= BB[wKingPos-9];
                        counter++;                  //increment counter
                        if(WB[wKingPos-9] == -127)   //if square behind king attacked and not defended
                            counter++;              //increment counter
                    }  
                }
            }
            if(file + 1 <= 7) {
                if(BB[wKingPos+1] > 0) {        //if square beside king attacked
                    mask |= BB[wKingPos+1];
                    counter++;                  //increment counter
                    if(WB[wKingPos+1] == -127)   //if square beside king attacked and not defended
                        counter++;              //increment counter
                }
                if(rank > 0) {
                   if(BB[wKingPos-7] > 0) {        //if square behind king attacked
                        mask |= BB[wKingPos-7];
                        counter++;                  //increment counter
                        if(WB[wKingPos-7] == -127)   //if square behind king attacked and not defended
                            counter++;              //increment counter
                   }   
                }
            }
            
           if(rank > 0) {                       
               if(BB[wKingPos-8] > 0) {        //if square behind king attacked
                   mask |= BB[wKingPos-8];
                   counter++;                  //increment counter
                   if(WB[wKingPos-8] == -127)   //if square behind king attacked and not defended
                        counter++;              //increment counter
               }    
           }
            
            
            // collect the scores of the squares directly in front of the enemy king
            
            if( rank < 7) {
                
                if(BB[wKingPos + 8] > 0) {        //if square in front of king attacked
                    mask |= BB[wKingPos+8];
                    counter++;                  //increment counter
                    if(WB[wKingPos+ 8] == -127)   //if square in front of king attacked and not defended
                        counter++;              //increment counter
                    if(Board.piece_in_square[wKingPos+8] > 5)           //if there is an enemy black piece at this square
                        counter++;
                }
            
                
                
                if(file - 1 >= 0) {
                    if(BB[wKingPos + 7] > 0) {        //if square beside king attacked
                        mask |= BB[wKingPos+7];
                        counter++;                  //increment counter
                        if(WB[wKingPos+ 7] == -127)   //if square beside king attacked and not defended
                            counter++;              //increment counter
                        if(Board.piece_in_square[wKingPos+7] > 5)           //if there is an enemy black piece at this square
                            counter++;
                    }
                }
            
                if(file + 1 <= 7) {
                    if(BB[wKingPos+9] > 0) {        //if square beside king attacked
                        mask |= BB[wKingPos+9];
                        counter++;                  //increment counter
                        if(WB[wKingPos+9] == -127)   //if square beside king attacked and not defended
                            counter++;              //increment counter
                        if(Board.piece_in_square[wKingPos+9] > 5)
                            counter++;
                    }
                }
            
            }
            
            mask>>=3;
            mask &= 31;
            counter += TABLE[mask];
            attackScore += kingSafetyEval[counter + wKingAttackVal[wKingPos]];
            
        }    
          
        if(file == 0) {
            if(wPawnFileVals[0] == 7) {
                openFilePenalty += KING_PAWN_OPEN_FILE;
                if(bPawnFileVals[0] == 0)
                    openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
            } else if(bPawnFileVals[0] == 0)
                openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(wPawnFileVals[1] == 7) {
                openFilePenalty += KING_PAWN_OPEN_FILE;
                if(bPawnFileVals[1] == 0)
                    openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
            } else if(bPawnFileVals[1] == 0)
                openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(wPawnFileVals[2] == 7) {
                openFilePenalty += KING_PAWN_OPEN_FILE;
                if(bPawnFileVals[2] == 0)
                    openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
            } else if(bPawnFileVals[2] == 0)
                openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;
            
        } else if(file == 7) {
            if(wPawnFileVals[5] == 7) {
                openFilePenalty += KING_PAWN_OPEN_FILE;
                if(bPawnFileVals[5] == 0)
                    openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
            } else if(bPawnFileVals[5] == 0)
                openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(wPawnFileVals[6] == 7) {
                openFilePenalty += KING_PAWN_OPEN_FILE;
                if(bPawnFileVals[6] == 0)
                    openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
            } else if(bPawnFileVals[6] == 0)
                openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(wPawnFileVals[7] == 7) {
                openFilePenalty += KING_PAWN_OPEN_FILE;
                if(bPawnFileVals[7] == 0)
                    openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
            } else if(bPawnFileVals[7] == 0)
                openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;
        } else {
            if(wPawnFileVals[file] == 7) {
                openFilePenalty += KING_PAWN_OPEN_FILE;
                if(bPawnFileVals[file] == 0)
                    openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
            } else if(bPawnFileVals[file] == 0)
                openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(wPawnFileVals[file-1] == 7) {
                openFilePenalty += KING_PAWN_OPEN_FILE;
                if(bPawnFileVals[file-1] == 0)
                    openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
            } else if(bPawnFileVals[file-1] == 0)
                openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(wPawnFileVals[file+1] == 7) {
                openFilePenalty += KING_PAWN_OPEN_FILE;
                if(bPawnFileVals[file+1] == 0)
                    openFilePenalty += ENEMY_KING_PAWN_OPEN_FILE;
            } else if(bPawnFileVals[file+1] == 0)
                openFilePenalty += ENEMY_KING_PAWN_SEMI_OPEN;
        }
        
        }
        
        return (int)(((double)score + (double)openFilePenalty) * (1.0-endGameCoefficient)) + attackScore;
        
        //return Math.max(0,(score + openFilePenalty) * (Board.totalValue-2500)/5600);
    }     
    
    
    
    
    private static int getKingSafetyBlack() {
        int score = 0;
        int attackScore = 0;
        long pawnProtect;
        int pawnShieldScore = 0;
        int openFilePenalty = 0;
        int file;
        int rank;
        int position;
        
        
        if(Board.bCastle <= Global.CASTLED || Board.wCastle <= Global.CASTLED ) {
            pawnShieldScore = 0;
            pawnProtect = blackKingZone & Board.blackpawns;
        
            while(pawnProtect != 0) {
                long pawn = pawnProtect & -pawnProtect;
                pawnProtect ^= pawn;
                position = Board.getPos(pawn);
                pawnShieldScore += blackPawnProtection[position/8];    
            }
            score += pawnShieldScore;
        
            file = bKingPos%8;
            rank = bKingPos/8;
            
           
         if(Board.whitequeen != 0 && endGameCoefficient <=  0.6) {   
             byte mask = 0;
             int counter = 0;
             //collect the scores of the squares two places in front of the black king
             if(rank > 1) {
                 if(file - 1 >= 0) 
                     mask |= WB[bKingPos-17];
                 if(file + 1 <= 7 )
                     mask |= WB[bKingPos-15];   
                 mask |= WB[bKingPos-16];
             }
             //collect the scores of the squares beside and behind the black king
             if(file - 1 >= 0) {
                if(WB[bKingPos-1] > 0) {        //if square beside king attacked
                    mask |= WB[bKingPos-1];
                    counter++;                  //increment counter
                    if(BB[bKingPos-1] == -127)   //if square beside king attacked and not defended
                        counter++;              //increment counter
                }
                if(rank < 7) { 
                    if(WB[bKingPos+7] > 0) {        //if square behind  king attacked
                        mask |= WB[bKingPos+7];
                        counter++;                  //increment counter
                        if(BB[bKingPos+7] == -127)   //if square behind king attacked and not defended
                            counter++;              //increment counter
                    }  
                }
            }
            if(file + 1 <= 7) {
                if(WB[bKingPos+1] > 0) {        //if square beside king attacked
                    mask |= WB[bKingPos+1];
                    counter++;                  //increment counter
                    if(BB[bKingPos+1] == -127)   //if square beside king attacked and not defended
                        counter++;              //increment counter
                }
                if(rank < 7) {
                   if(WB[bKingPos+9] > 0) {        //if square behind king attacked
                       mask |= WB[bKingPos+9]; 
                       counter++;                  //increment counter
                        if(BB[bKingPos+9] == -127)   //if square behind king attacked and not defended
                            counter++;              //increment counter
                   }   
                }
            }
            
           if(rank < 7) {                       
               if(WB[bKingPos+8] > 0) {        //if square behind king attacked
                   mask |= WB[bKingPos+8];
                   counter++;                  //increment counter
                   if(BB[bKingPos+8] == -127)   //if square behind king attacked and not defended
                        counter++;              //increment counter
               }    
           }
            
            
            // collect the scores of the squares directly in front of the enemy king
            
            if( rank > 0) {
                
                if(WB[bKingPos - 8] > 0) {        //if square in front of king attacked
                    mask |= WB[bKingPos-8];
                    counter++;                  //increment counter
                    if(BB[bKingPos - 8] == -127)   //if square in front of king attacked and not defended
                        counter++;              //increment counter
                    if(Board.piece_in_square[bKingPos - 8] < 6 && Board.piece_in_square[bKingPos - 8] >= 0)    //if there is an enemy white piece at this square
                        counter++;
                }
            
                
                
                if(file - 1 >= 0) {
                    if(WB[bKingPos - 9] > 0) {        //if square beside king attacked
                        mask |= WB[bKingPos-9];
                        counter++;                  //increment counter
                        if(BB[bKingPos - 9] == -127)   //if square beside king attacked and not defended
                            counter++;              //increment counter
                        if(Board.piece_in_square[bKingPos - 9] < 6 && Board.piece_in_square[bKingPos - 9] >= 0)    //if there is an enemy white piece at this square
                            counter++;
                    }
                }
            
                if(file + 1 <= 7) {
                    if(WB[bKingPos - 7] > 0) {        //if square beside king attacked
                        mask |= WB[bKingPos - 7];
                        counter++;                  //increment counter
                        if(BB[bKingPos - 7] == -127)   //if square beside king attacked and not defended
                            counter++;              //increment counter
                         if(Board.piece_in_square[bKingPos - 7] < 6 && Board.piece_in_square[bKingPos - 7] >= 0)    //if there is an enemy white piece at this square
                            counter++;
                    }
                }
            
            }
            mask>>=3;
            mask &=31;
            counter += TABLE[mask];
            attackScore -= kingSafetyEval[counter + bKingAttackVal[bKingPos]];
         }    
           
        if(file == 0) {
            if(bPawnFileVals[0] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[0] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[0] == 7)
                openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(bPawnFileVals[1] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[1] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[1] == 7)
                 openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(bPawnFileVals[2] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[2] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[2] == 7)
                 openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;
            
        } else if(file == 7) {
            if(bPawnFileVals[5] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[5] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[5] == 7)
                 openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(bPawnFileVals[6] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[6] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[6] == 7)
                openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(bPawnFileVals[7] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[7] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[7] == 7)
                 openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;
            
        } else {
            if(bPawnFileVals[file] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[file] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[file] == 7)
                openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(bPawnFileVals[file-1] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[file-1] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[file-1] == 7)
                openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;
            
            if(bPawnFileVals[file+1] == 0) {
                openFilePenalty -= KING_PAWN_OPEN_FILE;
                if(wPawnFileVals[file+1] == 7)
                    openFilePenalty -= ENEMY_KING_PAWN_OPEN_FILE;
            } else if(wPawnFileVals[file+1] == 7)
                openFilePenalty -= ENEMY_KING_PAWN_SEMI_OPEN;
        }
        }
        return (int)(((double)score + (double)openFilePenalty) * (1.0-endGameCoefficient)) + attackScore;
        
    }
    
    private static int getWBishopEval() {
        long bishops = Board.whitebishops;
        int position;
        long attacks;
        int score = 0;
        while(bishops != 0) {
            noWhiteBishops++;
            long piece = bishops & -bishops;
            bishops ^= piece;
            position = Board.getPos(piece);
            if(position/8 == 0)                         //knight on 8th rank
                develop += BACKRANK_MINOR;              //penalize
            attacks = Board.getAttackBoard(position);
            while(attacks != 0) {                                   //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Board.getPos(attack);
               centre -= centreArr[pos];
               if(Board.piece_in_square[pos] < 0 || Board.piece_in_square[pos] >5)
                   mobility -= BISHOP_MOBILITY;
               WB[pos]++;
               WB[pos] |= MINOR_BIT; 
            }
            score += Board.getDistance(position, bKingPos);        //tropism
        }
        if(noWhiteBishops == 2)                         //2 bishop bonus
            score -= TWO_BISHOP_BONUS;
        return score;
    }
    
    private static int getBBishopEval() {
        long bishops = Board.blackbishops;
        int position;
        long attacks;
        int score = 0;
        while(bishops != 0) {
            noBlackBishops++;
            long piece = bishops & -bishops;
            bishops ^= piece;
            position = Board.getPos(piece);
            if(position/8 == 7)                         //knight on 8th rank
                develop -= BACKRANK_MINOR;              //penalize
            attacks = Board.getAttackBoard(position);
            while(attacks != 0) {                                   //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Board.getPos(attack);
               centre += centreArr[pos];
               if(Board.piece_in_square[pos] < 6)
                   mobility += BISHOP_MOBILITY;
               BB[pos]++;
               BB[pos] |= MINOR_BIT; 
            }
            score -= Board.getDistance(position, wKingPos);        //tropism
        }
        if(noBlackBishops == 2)
            score += TWO_BISHOP_BONUS;
        return score;
    }
    private static int getWRookEval() {
        long rooks = Board.whiterooks;
        int position;
        long attacks;
        int rookScore = 0;
        int file;
        int rank;
        int oldFile = -1;
        int oldRank = -1;
        int nearEnemyKing;
        while(rooks != 0) {
            nearEnemyKing = 1;
            long piece = rooks & -rooks;
            rooks ^= piece;
            position = Board.getPos(piece); 
            file = position % 8;
            rank = position / 8;
            if(bKingPos/8 == 7 && rank == 6 )   {          //7th rank bonus
                rookScore -= ROOK_7TH_RANK;
                if(rank == oldRank)                     //doubled rook bonus;
                    rookScore -= DOUBLED_ROOKS_7TH;
                    if((Global.rankMasks[rank] & Board.whitequeen) != 0)
                        rookScore -= QUEEN_ROOK_7TH_RANK;
            }
            if(wPawnFileVals[position%8] == 7) {           // semi open file
                if(Math.abs(position%8 - bKingPos%8) < 2)
                    nearEnemyKing = 2;
                long rookQueen = Global.fileMasks[file] & Board.whitequeen ;
                if((rookQueen) != 0)   {     //rook queen connectivity
                    if(position > Board.getPos(rookQueen &  -rookQueen))
                        rookScore -= ROOK_IN_FRONT_QUEEN;
                    else
                        rookScore -= ROOK_BEHIND_QUEEN;
                }    
                if(file == oldFile)
                    rookScore -= ROOK_DOUBLED * nearEnemyKing;
                if(bPawnFileVals[position%8] == 0)          // open file        
                    rookScore -= ROOK_OPEN * nearEnemyKing;
                else
                    rookScore -= ROOK_SEMI * nearEnemyKing;
            }
            oldFile = file;
            oldRank = rank;
            attacks = Board.getAttackBoard(position);
            while(attacks != 0) {                                   //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Board.getPos(attack);
               centre -= centreArr[pos];
               if(Board.piece_in_square[pos] < 0 || Board.piece_in_square[pos] >5)
                   mobility -= ROOK_MOBILITY;
               WB[pos]++;
               WB[pos] |= ROOK_BIT; 
            }
            rookScore += Board.getDistance(position, bKingPos);        //tropism
        }
        return rookScore;
    }
    private static int getBRookEval() {
        long rooks = Board.blackrooks;
        int position;
        long attacks;
        int rookScore = 0;
        int oldFile = -1;
        int oldRank = -1;
        int file;
        int rank;
        int nearEnemyKing;
        while(rooks != 0) {
            nearEnemyKing = 1;
            long piece = rooks & -rooks;
            rooks ^= piece;
            position = Board.getPos(piece);
            file = position % 8;
            rank = position / 8;
            if(wKingPos/8 == 0  && rank == 1)   {          //7th rank bonus
                rookScore += ROOK_7TH_RANK;
                if(rank == oldRank)                     //doubled rook bonus;
                    rookScore += DOUBLED_ROOKS_7TH;
                if((Global.rankMasks[rank] & Board.blackqueen) != 0)
                    rookScore += QUEEN_ROOK_7TH_RANK;
            }
            if(bPawnFileVals[position%8] == 0) {           // semi open file
                if(Math.abs(position%8 - wKingPos%8) < 2)
                    nearEnemyKing = 2;
                long rookQueen = Global.fileMasks[file] & Board.blackqueen ;
                if((rookQueen) != 0)   {     //rook queen connectivity
                    if(position < Board.getPos(rookQueen &  -rookQueen))
                        rookScore += ROOK_IN_FRONT_QUEEN;
                    else
                        rookScore += ROOK_BEHIND_QUEEN;
                }    
                if(file == oldFile)
                    rookScore += ROOK_DOUBLED * nearEnemyKing;
                if(wPawnFileVals[position%8] == 7)          // open file        
                    rookScore += ROOK_OPEN * nearEnemyKing;
                else
                    rookScore += ROOK_SEMI * nearEnemyKing;
            }
            oldFile = file;
            oldRank = rank;
            attacks = Board.getAttackBoard(position);
            while(attacks != 0) {                                   //update the WB table
               long attack = attacks & -attacks;
               attacks ^= attack;
               int pos = Board.getPos(attack);
               centre += centreArr[pos];
               if(Board.piece_in_square[pos] < 6)
                   mobility += ROOK_MOBILITY;
               BB[pos]++;
               BB[pos] |= ROOK_BIT; 
            }
            rookScore -= Board.getDistance(position, wKingPos);        //tropism
        }
        return rookScore;
    }
    
    private static int whiteHungPieces() {
        long side = Board.whitepieces & ~Board.whiteking;
        int hung = 0;
        int score = 0;
        long piece;
        int position;
        while(side != 0) {
            piece = side & -side;
            side ^= piece;
            position = Board.getPos(piece);
            if(BB[position] > 0 &&  WB[position] == 0)
                hung++;
            else if(BB[position] > 0) {
                
                switch(Board.piece_in_square[position]) {
                    
                    case 0:
                        if((BB[position] & (MINOR_BIT | PAWN_BIT)) != 0)
                            hung++;
                        break;
                    case 1:
                        if((BB[position] & PAWN_BIT) != 0) 
                            hung++;
                        break;
                    case 2:
                        if((BB[position] & PAWN_BIT) != 0) 
                            hung++;
                        break;
                    case 3:
                        if((BB[position] & (MINOR_BIT | PAWN_BIT | ROOK_BIT)) != 0)
                            hung++;
                        break;
                     
                } 
            }
        }
        if(hung == 2)
            score = HUNG_PENALTY;
        else if(hung > 2)
            score = 2*HUNG_PENALTY;
        
        return score;
    }  
        
    public static int blackHungPieces() {
        long side = Board.blackpieces & ~Board.blackking;
        int hung = 0;
        int score = 0;
        long piece;
        int position;
        while(side != 0) {
            piece = side & -side;
            side ^= piece;
            position = Board.getPos(piece);
            if(WB[position] > 0 &&  BB[position] == 0)
                hung++;
            else if(WB[position] > 0) {
                
                switch(Board.piece_in_square[position]) {
                    
                    case 6:
                        if((WB[position] & (MINOR_BIT | PAWN_BIT)) != 0)
                            hung++;
                        break;
                    case 7:
                        if((WB[position] & PAWN_BIT) != 0) 
                            hung++;
                        break;
                    case 8:
                        if((WB[position] & PAWN_BIT) != 0) 
                            hung++;
                        break;
                    case 9:
                        if((WB[position] & (MINOR_BIT | PAWN_BIT | ROOK_BIT)) != 0)
                            hung++;
                        break;
                     
                } 
            }
        }
        if(hung == 2)
            score = -HUNG_PENALTY;
        else if(hung > 2)
            score = -2*HUNG_PENALTY;
        
        return score;
    }      
        
        
}

/*
 if(hasHash) {
            wfileBit = PawnTable.getWPawnFile(key);
            if(wfileBit != getWhitePawnFile()) {
                System.out.println("wfileBit is "+wfileBit);
                System.out.println("info string pawn file white is are "+getWhitePawnFile());
                System.out.println("info string ERROR!!!!!!!1"+(5/0));
            }
            bfileBit = PawnTable.getBPawnFile(key);
            if(bfileBit != getBlackPawnFile()) {
                
                System.out.println("info string pawn file black is are "+getBlackPawnFile());
                System.out.println("info string ERROR!!!!!!!1"+(5/0));
            }
            bitToArrayWhitePawnFile(wfileBit);
            bitToArrayBlackPawnFile(bfileBit);
            pawnScore = PawnTable.getPawnValue(key);
            if(pawnScore != (getWPawnsScore2() + getBPawnsScore2()))
                System.out.println("info string pawn score ERROR!!!!!!!1"+(5/0));
            passBits = PawnTable.getPawnPassed(key);
            if(passBits != getPassedPawnsBits()) {
                System.out.println("info string pawn bits are "+getPassedPawnsBits());
                System.out.println("info string ERROR!!!!!!!1"+(5/0));
            }
 
 
 
 
 
 passBits |= (long)1<<31;
        int lowPassBits = (int)passBits;
        int highPassBits = (int)(passBits>>>32);
        int probBit = 1<<31;
        boolean probs = false;
        if( (lowPassBits & probBit) != 0) {
            probs = true;
        }
        if(probs)
            lowPassBits ^= probBit;
        long newPass = ((long)lowPassBits | ((long)highPassBits)<<32);
        if(probs)
            newPass |= probBit;
        if(passBits != passBits) 
            System.out.println("problem");
        
*/