

public final class Position {
	
	private long code;					//unique hash code to this position
	private int reps;					//no of repetitions of this position in the history
	private Position next;
	public Position(long l) {
		code = l;
		reps = 1;
	}
	public final long getCode() {
		return code;
	}
	public final int getReps() {
		return reps;
	}
	public final void incReps() {
		reps++;
	}
	public final void subReps() {
		reps--;
	}	
	public final void setNext(Position p) {
		next = p;
	}
	public final Position getNext() {
		return next;
	}		
}				