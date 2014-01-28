package interpreter.strat;

import interpreter.data.Closure;
import interpreter.data.Literal;

import java.util.List;

import util.Tuple;

public final class CustomRuleSelector implements RuleSelector {

	private int index;
	
	public CustomRuleSelector() {
		index = -1;
	}

	public CustomRuleSelector(int index) {
		super();
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	@Override
	public int select(Tuple<Literal, List<Closure>> tuple) {
		return index;
	}
	
	@Override
	public String toString() {
		return "Custom";
	}
}
