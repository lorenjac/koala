package interpreter.strat;

import interpreter.data.Closure;
import interpreter.data.Literal;

import java.util.List;

import util.Tuple;

public final class CustomLiteralSelector implements LiteralSelector {

	private int index;
	
	public CustomLiteralSelector() {
		index = -1;
	}

	public CustomLiteralSelector(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int select(List<Tuple<Literal, List<Closure>>> alternatives) {
		return index;
	}

	@Override
	public String toString() {
		return "Custom";
	}
}
