package interpreter.data;

/**
 * Brandenburg University of Technology Cottbus
 * 
 * Programming Languages and Compiler Construction
 * 
 * ChocoStore.java
 */

import choco.Choco;
import choco.cp.model.CPModel;
import choco.cp.solver.CPSolver;
import choco.kernel.common.util.iterators.DisposableIterator;
import choco.kernel.model.Model;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerVariable;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.Solver;
import choco.kernel.solver.variables.integer.IntDomainVar;
import choco.kernel.solver.variables.integer.IntVar;

/**
 * @author Peter Sauer
 * 
 *         created: 11.01.2013
 */
public class ChocoStore {

	/**
	 * The model for the used Choco constraint solver.
	 */
	private Model model;

	/**
	 * A reference to an instance of the Choco constraint solver.
	 */
	private Solver solver;

	/**
	 * The main method holds some simple testing stuff.
	 * 
	 * @param args
	 *            ignored
	 */
//	public static void main(String[] args) {
//		// create a store
//		ChocoStore store = new ChocoStore();
//
//		// create two variables, x and y
//		IntegerVariable x = VariablePool.getIntegerVariable("x");
//		IntegerVariable y = VariablePool.getIntegerVariable("y");
//
//		// both should be greater or equal to zero
//		store.tell(Choco.geq(x, 0));
//		store.tell(Choco.geq(y, 0));
//
//		// both should be lower than 10
//		store.tell(Choco.lt(x, 10));
//		store.tell(Choco.lt(y, 10));
//
//		// x should be greater than y
//		store.tell(Choco.gt(x, y));
//
//		// print the current status of the store
//		store.printStatus();
//
//		// ask if x is greater than zero
//		System.out.println("Is x greater than zero? "
//				+ store.ask(Choco.gt(x, 0)));
//
//		// ask if y is greater than zero
//		System.out.println("Is y greater than zero? "
//				+ store.ask(Choco.gt(y, 0)));
//
//		// create a new list variable
//		ListVar<IntegerVariable> alist =
//				VariablePool.getIntegerVariableList("alist");
//
//		// ask if alist is unbound
//		System.out.println("Is alist unbound? " + store.ask(alist.isUnbound()));
//
//		// alist should have at least one element
//		alist.bind(true);
//
//		// ask if alist is an empty list
//		System.out.println("Is alist an empty list? "
//				+ store.ask(alist.isEmpty()));
//
//		// ask if alist is a non empty list
//		System.out.println("Is alist a non empty list? "
//				+ store.ask(alist.isCons()));
//
//		// the head of alist should always be equal to x
//		store.tell(Choco.eq(alist.head(), x));
//
//		// show the status of the store
//		store.printStatus();
//
//		// ask if the tail of alist is unbound
//		System.out.println("Is tail alist unbound? "
//				+ store.ask(alist.tail().isUnbound()));
//
//		// create a second list element
//		ListVar<IntegerVariable> alistTail = alist.tail();
//		alistTail.bind(true);
//
//		// the second list element should be two times the first
//		store.tell(Choco.eq(Choco.mult(2, alist.head()), alistTail.head()));
//
//		// show the status of the store
//		store.printStatus();
//	}

	/**
	 * The constructor creates a new store instance and instantiates the model
	 * and the solver.
	 */
	public ChocoStore() {
		model = new CPModel();
		solver = new CPSolver();
	}

	/**
	 * Asks if the given Constraint {@code aConstraint} is entailed by the
	 * current store content.
	 * 
	 * @param aConstraint
	 *            The constraint to test for entailment.
	 * @return {@code true} if the constraint is entailed by the current store
	 *         content, {@code false} otherwise.
	 */
	public synchronized boolean ask(Constraint aConstraint) {
		// the negative constraint of the asked constraint
		Constraint c = Choco.not(aConstraint);

		// to store if the constraint is entailed
		Boolean solved;

		model.addConstraint(c);
		solver.clear();
		solver.read(model);
		
		solved = solver.solve();

		model.removeConstraint(c);

		// the constraint is entailed, if there is no solution with the negative
		// constraint
		return !solved;
	}

	/**
	 * Returns {@code True}, if a tell of the given constraint would be ok.
	 * Returns {@code False}, if a tell of the given constraint would lead to an
	 * inconsistent store.
	 * 
	 * @param aConstraint
	 *            The constraint for which to check if a tell would be ok
	 * @return {@code True}, if a tell of the given constraint would be ok.
	 */
	public synchronized boolean isTellOk(Constraint aConstraint) {
		boolean ok;

		// the number of constraint currently in the model
		int nbConstraints = model.getNbConstraints();
		model.addConstraint(aConstraint);

		solver.clear();
		solver.read(model);

		ok = solver.solve();

		try {
			model.removeConstraint(aConstraint);
		} catch (Exception e) {
			if (model.getNbConstraints() > nbConstraints) {
				System.err.println("Removing of the constraint " + aConstraint
						+ " failed");
		 	}
		}

		return ok;
	}

	/**
	 * Adds the given constraint to the store, if the tell wouldn't lead to an
	 * inconsistent store.
	 * 
	 * @param aConstraint
	 *            The constraint which should be added to the store.
	 * @return {@code True} if the constraint could be added.
	 */
	public synchronized boolean tell(Constraint aConstraint) {
		if (isTellOk(aConstraint)) {
			model.addConstraint(aConstraint);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Prints each variable with their name and current assignment.
	 */
	public synchronized void printSolution() {
		solver.clear();
		solver.read(model);

		solver.solve();

		DisposableIterator<IntDomainVar> intVarIterator =
				solver.getIntVarIterator();

		System.out.println("Current Solution:");
		System.out.println("=================");
		while (intVarIterator.hasNext()) {
			IntDomainVar var = intVarIterator.next();

			System.out.println(var.getName() + " = " + var.getVal());
		}
		System.out.println("=================");
	}

	/**
	 * Prints each variable with their name, current assignment, and the current
	 * domain.
	 */
	public synchronized void printStatus() {
		solver.clear();
		solver.read(model);

		try {
			solver.propagate();
		} catch (ContradictionException e) {
			e.printStackTrace();
		}

		DisposableIterator<IntDomainVar> intVarIterator =
				solver.getIntVarIterator();

		System.out.println("Store Status:");
		System.out
				.println("================================================================");
		System.out.format("%16s%32s%16s", "Name", "Domain", "Value");
		System.out.println();
		System.out
				.println("----------------------------------------------------------------");

		while (intVarIterator.hasNext()) {
			IntDomainVar var = intVarIterator.next();
			// System.out.print(" Name: " + var.getName());
			// System.out.print(" \t\tDomain: " + var.getDomain());
			// System.out.println(" \t\tValue: " + var.getVal());

			System.out.format("%16s%32s%16s", var.getName(), var.getDomain(),
					var.getVal());
			System.out.println();
		}
		System.out
				.println("================================================================");
	}
	
	/**
	 * Writes each variable with their name, current assignment, and the current
	 * domain to a string.
	 */
	public synchronized String getStatus() {
		solver.clear();
		solver.read(model);

		try {
			solver.propagate();
		} catch (ContradictionException e) {
			e.printStackTrace();
		}

		DisposableIterator<IntDomainVar> intVarIterator =
				solver.getIntVarIterator();

		String s = "Store Status:\n";
		s += "================================================================\n";
		s += String.format("%16s%32s%16s", "Name", "Domain", "Value");
		s += "\n";
		s += "----------------------------------------------------------------\n";

		while (intVarIterator.hasNext()) {
			IntDomainVar var = intVarIterator.next();
			// System.out.print(" Name: " + var.getName());
			// System.out.print(" \t\tDomain: " + var.getDomain());
			// System.out.println(" \t\tValue: " + var.getVal());

			s += String.format("%16s%32s%16s", var.getName(), var.getDomain(), var.getVal());
			s += "\n";
		}
		s += "================================================================\n";
		return s;
	}
	
	/**
	 * Retrieve a value for the given variable.
	 * 
	 * @author Benny H�ckner
	 * @data   12.02.2013
	 */
	public synchronized Integer getValueOfVar(IntegerVariable var) {
	    solver.clear();
	    solver.read(model);
	    try {
            solver.propagate();
        } catch (ContradictionException e) {
            e.printStackTrace();
        }
	    /*DisposableIterator<IntDomainVar> intVarIterator =
                solver.getIntVarIterator();
	    while (intVarIterator.hasNext()) {
            IntDomainVar var = intVarIterator.next();
            if (var.getName().equals(name)) {
                return var.getVal();
            }
        }*/
	    IntVar v = solver.getVar(var);
	    return v == null ? null : v.getVal();
	}
}
