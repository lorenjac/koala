package interpreter.strat;

import interpreter.data.Closure;
import interpreter.data.Literal;

import java.util.List;

import util.Tuple;

public interface LiteralSelector {
	int select(List<Tuple<Literal, List<Closure>>> alternatives);
}
