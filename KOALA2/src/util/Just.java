package util;

public final class Just<T> implements Maybe<T> {

	private T value;
	
	public Just(T value) {
		this.value = value;
	}

	@Override
	public boolean hasValue() {
		return true;
	}

	@Override
	public T getValue() {
		return value;
	}
}
