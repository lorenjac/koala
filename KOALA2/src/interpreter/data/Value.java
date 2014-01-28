package interpreter.data;

import choco.kernel.model.variables.integer.IntegerConstantVariable;
import choco.kernel.model.variables.integer.IntegerVariable;
import util.Just;
import util.Maybe;
import util.Nothing;

public final class Value {
	private static final Nothing<Object> NOTHING = new Nothing<>();

	private String name;
	private Maybe<Object> value;
	
	public Value(String name) {
		this.name = name;
		value = NOTHING;
	}

	public String getName() {
		return name;
	}
	
	public boolean isInit() {
		return value.hasValue();
	}
	
	public boolean isInt() {
		return isInit() && value.getValue() instanceof IntegerConstantVariable;
	}
	
	public boolean isIntVar() {
		return isInit() && value.getValue() instanceof IntegerVariable;
	}
	
	public boolean isListVar() {
		return isInit() && value.getValue() instanceof ListVariable;
	}
	
	public IntegerConstantVariable getInt() {
		return isInit() ? (IntegerConstantVariable)value.getValue() : null;
	}
	
	public IntegerVariable getIntVar() {
		return isInit() ? (IntegerVariable)value.getValue() : null;
	}
	
	public ListVariable getListVar() {
		return isInit() ? (ListVariable)value.getValue() : null;
	}
	
	public void init(IntegerConstantVariable integer) {
		value = new Just<>((Object)integer);
	}
	
	public void init(IntegerVariable variable) {
		value = new Just<>((Object)variable);
	}
	
	public void init(ListVariable list) {
		value = new Just<>((Object)list);
	}

	@Override
	public String toString() {
		return "Value [name=" + name + (value.hasValue() ? ", value=" + value.getValue() : "") + "]";
	}
}
