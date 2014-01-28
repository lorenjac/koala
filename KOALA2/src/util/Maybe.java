package util;

public interface Maybe<T> {
	public boolean hasValue();
	public T getValue();
}
