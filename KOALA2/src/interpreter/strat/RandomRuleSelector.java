package interpreter.strat;

import interpreter.data.Closure;
import interpreter.data.Literal;

import java.util.List;
import java.util.Random;

import util.Tuple;

public class RandomRuleSelector implements RuleSelector {

	@Override
	public int select(Tuple<Literal, List<Closure>> tuple) {
		//System.out.println("random space = 0 .. " + (tuple.getSecond().size()-1));
		return new Random().nextInt(tuple.getSecond().size());
	}

	@Override
	public String toString() {
		return "Random";
	}
}
