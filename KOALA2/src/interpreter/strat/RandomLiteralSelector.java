package interpreter.strat;

import interpreter.data.Closure;
import interpreter.data.Literal;

import java.util.List;
import java.util.Random;

import util.Tuple;

public class RandomLiteralSelector implements LiteralSelector {

	@Override
	public int select(List<Tuple<Literal, List<Closure>>> alternatives) {
		return new Random().nextInt(alternatives.size());
	}

	@Override
	public String toString() {
		return "Random";
	}
}
