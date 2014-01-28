package interpreter.strat;

import interpreter.data.Closure;
import interpreter.data.Literal;

import java.util.List;

import util.Tuple;

public interface RuleSelector {
	int select(Tuple<Literal, List<Closure>> tuple);
}
