package interpreter.data;

import parser.node.AHead;
import parser.node.ARule;

public final class Closure {
	private ARule rule;
	private Environment environment;
	public Closure(ARule rule, Environment environment) {
		super();
		this.rule = rule;
		this.environment = environment;
	}
	public ARule getRule() {
		return rule;
	}
	public Environment getEnvironment() {
		return environment;
	}
	@Override
	public String toString() {
		String name = ((AHead)rule.getHead()).getName().getText();
		return "Closure [rule=" + name + ", environment=" + environment + "]";
	}
}
