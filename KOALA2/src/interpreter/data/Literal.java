package interpreter.data;

import java.util.List;

public final class Literal {
	private String name;
	private List<Value> args;
	
	public Literal(String name, List<Value> args) {
		super();
		this.name = name;
		this.args = args;
	}
	
	public String getName() {
		return name;
	}
	
	public List<Value> getArgs() {
		return args;
	}

	@Override
	public String toString() {
		return "Literal [name=" + name + ", args=" + args + "]";
	}
}
