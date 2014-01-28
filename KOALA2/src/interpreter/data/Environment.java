package interpreter.data;

import java.util.HashMap;
import java.util.Map;

public final class Environment {

	private Map<String, Value> table;
	
	public Environment() {
		this.table = new HashMap<>();
	}

	public void put(String key, Value value) {
		table.put(key, value);
	}
	
	public Value get(String key) {
		return table.get(key);
	}

	public void reset() {
		for(Map.Entry<String, Value> e : table.entrySet()) {
			e.setValue(new Value(e.getValue().getName()));
		}
	}

	@Override
	public String toString() {
		return "" + table + "";
	}
}
