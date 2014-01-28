package util;


public final class Nothing<T> implements Maybe<T> {

	@Override
	public boolean hasValue() {
		return false;
	}

	@Override
	public T getValue() {
		throw new UnsupportedOperationException();
	}
}
