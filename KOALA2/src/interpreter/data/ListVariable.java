/**
 * Brandenburg University of Technology Cottbus
 * 
 * Programming Languages and Compiler Construction
 * 
 * ListVar.java
 */
package interpreter.data;


/**
 * @author Peter Sauer
 * 
 *         created: 06.02.2013
 * 
 *         A class representing list variables.
 */
public class ListVariable {

	/**
	 * A list element can be unbound, which is indicated by this flag.
	 */
	private Boolean isUnbound;
	
	/**
	 * This flag determines if the list is an empty list.
	 */
	private boolean isEmpty;

	/**
	 * If the list is not an empty list, this is a reference to the head element
	 * of the list.
	 */
	private Value head;

	/**
	 * If the list is not an empty list, this is a reference to the tail of the
	 * list.
	 */
	private Value tail;

	/**
	 * Creates a new unbound list variable.
	 * 
	 * @param anElemFactory
	 *            A factory for new list elements.
	 * @param listName
	 *            The name of the list.
	 */
	public ListVariable() {
		isUnbound = true;
		head = null;
		tail = null;
	}

	/**
	 * @return Returns Choco.TRUE, if the list element is unbound.
	 */
	public boolean isUnbound() {
		return isUnbound;
	}
	
	public boolean isBound() {
		return !isUnbound;
	}

	/**
	 * @return If the list is not unbound, this method returns if the list is an
	 *         empty list.
	 */
	public boolean isEmpty() {
		if (isUnbound) {
			return false;
		}
		if (isEmpty) {
			return true;
		}
		return false;
	}

	/**
	 * @return If the list is not unbound, this method return if the list is not
	 *         an empty list.
	 */
	public boolean isCons() {
		if (isUnbound) {
			return false;
		}
		if (isEmpty) {
			return false;
		}
		return true;
	}

	/**
	 * @return If the list is not an empty list, this method returns the head of
	 *         the list, otherwise null.
	 */
	public Value getHead() {
		if (isUnbound || isEmpty) {
			return null;
		}
		return head;
	}

	/**
	 * @return If the list is not an empty list, this method returns the tail of
	 *         the list, otherwise null. The tail is a list variable again. If
	 *         the tail doesn't exists, a new list variable will be created.
	 */
	public Value getTail() {
		if (isUnbound || isEmpty) {
			return null;
		}
		if (tail == null) {
			tail = new Value("");
			tail.init(new ListVariable());
		}
		return tail;
	}
	
	public void setEmpty() {
		this.isUnbound = false;
		this.isEmpty = true;
	}
	

    public void setHead(Value head) {
        isUnbound = false;
        isEmpty = false;
        this.head = head;
    }
	
	/**
	 * Sets the tail.
	 * Gives us the possibility to link two different lists.
	 */
    public void setTail(Value tail) {
	    this.tail = tail;
	}


	@Override
	public String toString() {
		return "List [isBound=" + !isUnbound + 
			   ", head=" + head + ", tail=" + tail + "]";
	}
}
