package interpreter;

import interpreter.data.ChocoStore;
import interpreter.data.Closure;
import interpreter.data.Environment;
import interpreter.data.ListVariable;
import interpreter.data.Literal;
import interpreter.data.Value;
import interpreter.eval.AskEvaluator;
import interpreter.eval.BodyEvaluator;
import interpreter.eval.TellCheckEvaluator;
import interpreter.eval.TellEvaluator;
import interpreter.strat.LiteralSelector;
import interpreter.strat.RandomLiteralSelector;
import interpreter.strat.RandomRuleSelector;
import interpreter.strat.RuleSelector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import parser.node.AHead;
import parser.node.ARule;
import parser.node.TIdent;
import util.PrettyPrinter;
import util.Tuple;

public final class Interpreter {

	public static final LiteralSelector DEFAULT_LITERAL_SELECTOR = new RandomLiteralSelector();
	public static final RuleSelector DEFAULT_RULE_SELECTOR = new RandomRuleSelector();
	
	private final Map<String, List<Closure>> program;
	private final List<Literal> goal;
	
	private final List<Tuple<Literal, List<Closure>>> alternatives;
	private final Tuple<Integer, Integer> selection;
	private final List<Integer> literalIndexList;
	
	private final ChocoStore store;
	
	public Interpreter(Map<String, List<Closure>> program, List<Literal> goal) {
		this.program = program;
		this.goal = goal;
		this.alternatives = new ArrayList<>();
		this.literalIndexList = new ArrayList<>();
		this.selection = new Tuple<>(-1, -1);
		this.store = new ChocoStore();
	}
	
	public List<Tuple<Literal, List<Closure>>> detectAlternatives() {
		alternatives.clear();
		literalIndexList.clear();
		for(int i=0; i<goal.size(); ++i) {
			Literal literal = goal.get(i);
			String literalName = literal.getName() + "/" + literal.getArgs().size();
			Tuple<Literal, List<Closure>> tuple = new Tuple<>();
			tuple.setFirst(literal);
			tuple.setSecond(new LinkedList<Closure>());
			for(Closure c : program.get(literalName)) {
				if(isValid(literal, c)) {
					tuple.getSecond().add(c);
				}
				System.out.println();
			}
			if(!tuple.getSecond().isEmpty()) {
				alternatives.add(tuple);
				literalIndexList.add(i);
			}
		}
		return alternatives;
	}
	
	public int selectLiteral(LiteralSelector selector) {
		int index = selector.select(alternatives);
		selection.setFirst(index);
		return index;
	}
	
	public int selectRule(RuleSelector selector) {
		int literalIndex = selection.getFirst();
		if(literalIndex != -1) {
			int ruleIndex = selector.select(alternatives.get(literalIndex));
			//System.out.println("rule index = " + ruleIndex);
			if(ruleIndex != -1) {
				selection.setSecond(ruleIndex);
			}
			return ruleIndex;
		}
		return -1;
	}
	
	public void interpret() {
		//apply selection
		int literalIndex = selection.getFirst();
		int ruleIndex = selection.getSecond();
		Tuple<Literal, List<Closure>> tuple = alternatives.get(literalIndex);
		Literal literal = tuple.getFirst();
		Closure closure = tuple.getSecond().get(ruleIndex);
		Environment environment = closure.getEnvironment();
		ARule rule = closure.getRule();
		
		//refresh environment
		//System.out.println("\n(before reset): " + environment);
		environment.reset();
		//System.out.println("(after reset) : " + environment);
		
		//bind head variables from literal
		bindParameters(literal, closure, true);
		
		//evaluate ask
		System.out.println("evaluating ask... ");
		rule.apply(new AskEvaluator(environment, store));
		
		//evaluate tell
		System.out.println("evaluating tell... ");
		rule.apply(new TellEvaluator(environment, store));
		
		//evaluate body
		System.out.println("evaluating body... ");
		rule.apply(new BodyEvaluator(environment, goal, literalIndexList.get(literalIndex)));
	}

	private boolean isValid(Literal literal, Closure closure) {
		Environment environment = closure.getEnvironment();
		ARule rule = closure.getRule();
		
		PrettyPrinter printer = new PrettyPrinter();
		rule.apply(printer);
		System.out.println("checking " + printer.getString());
		
		//check name
		AHead head = (AHead)rule.getHead();
		if(!literal.getName().equals(head.getName().getText())) {
			return false;
		}
		
		//check arity
		if(literal.getArgs().size() != head.getParams().size()) {
			return false;
		}
		
		//refresh environment
		environment.reset();
		
		//bind head variables from literal
		bindParameters(literal, closure, false);
		
		//evaluate ask
		System.out.print("checking ask... ");
		AskEvaluator askEvaluator = new AskEvaluator(environment, store);
		rule.apply(askEvaluator);
		if(!askEvaluator.isSatisfied()) {
			System.out.println("failed");
			return false;
		}
		System.out.println("ok");
		
		//evaluate tell
		System.out.print("checking tell... ");
		TellCheckEvaluator tellCheckEvaluator = new TellCheckEvaluator(environment, store);
		rule.apply(tellCheckEvaluator);
		if(!tellCheckEvaluator.isSatisfied()) {
			System.out.println("failed");
			return false;
		}
		System.out.println("ok");
		return true;
	}
	
	private void bindParameters(Literal literal, Closure closure, boolean persistent) {
		Environment environment = closure.getEnvironment();
		AHead head = (AHead)closure.getRule().getHead();
		Iterator<TIdent> params = head.getParams().iterator();
		//System.out.println();
		for(Value arg : literal.getArgs()) {
			String paramName = params.next().getText();
			if(arg.isInit() || persistent) {
				System.out.println(paramName + " ---> " + getResultString(arg));
				environment.put(paramName, arg);
			} else {
				System.out.println(paramName + " ~~~> " + arg.getName());
				environment.put(paramName, new Value(arg.getName()));
			}
		}
	}
	
	public String getResultString(Value value) {
		if(value.isInt()) {
			return ""+value.getInt().getValue();
		} else if(value.isIntVar()) {
			Integer n = store.getValueOfVar(value.getIntVar());
			if(n == null) {
				return value.getName().isEmpty() ? "?" : value.getName();
			} else {
				return ""+n.intValue();
			}
		} else if(value.isListVar()) {
			ListVariable list = value.getListVar();
			if(list.isUnbound()) {
				return value.getName().isEmpty() ? "?" : value.getName();
			} else {
				return getResultString(value.getListVar());
			}
		} else {
			return value.getName().isEmpty() ? "?" : value.getName();
		}
	}

	private String getResultString(ListVariable list) {
		if(list.isUnbound()) {
			return "?";
		} else if(list.isEmpty()) {
			return "[]";
		}
		return getResultString(list.getHead()) + " : " + getResultString(list.getTail());
	}
}