package interpreter.eval;

import interpreter.data.Environment;
import interpreter.data.Value;
import parser.analysis.DepthFirstAdapter;
import parser.node.ADivExpr;
import parser.node.AEqExpr;
import parser.node.AGeqExpr;
import parser.node.AGtExpr;
import parser.node.ALeqExpr;
import parser.node.ALtExpr;
import parser.node.AMinusExpr;
import parser.node.AModExpr;
import parser.node.AMultExpr;
import parser.node.ANeqExpr;
import parser.node.ANumberExpr;
import parser.node.APlusExpr;
import parser.node.AVarExpr;
import parser.node.PExpr;
import choco.Choco;
import choco.kernel.model.constraints.Constraint;
import choco.kernel.model.variables.integer.IntegerConstantVariable;
import choco.kernel.model.variables.integer.IntegerExpressionVariable;
import choco.kernel.model.variables.integer.IntegerVariable;

public final class IntConstraintTransformer extends DepthFirstAdapter {

	private Environment environment;
	private Constraint constraint;
	
	public IntConstraintTransformer(Environment environment) {
		this.environment = environment;
		this.constraint = null;
	}

	public Constraint getConstraint() {
		return constraint;
	}

	@Override
	public void caseAEqExpr(AEqExpr node) {
		String varName = node.getVar().getText();
		IntegerVariable lvalue = environment.get(varName).getIntVar();
		IntegerExpressionVariable rvalue = createIntExpression(node.getExpr());
		constraint = Choco.eq(lvalue, rvalue);
	}

	@Override
	public void caseANeqExpr(ANeqExpr node) {
		String varName = node.getVar().getText();
		IntegerVariable lvalue = environment.get(varName).getIntVar();
		IntegerExpressionVariable rvalue = createIntExpression(node.getExpr());
		constraint = Choco.neq(lvalue, rvalue);
	}

	@Override
	public void caseALtExpr(ALtExpr node) {
		String varName = node.getVar().getText();
		IntegerVariable lvalue = environment.get(varName).getIntVar();
		IntegerExpressionVariable rvalue = createIntExpression(node.getExpr());
		constraint = Choco.lt(lvalue, rvalue);
	}

	@Override
	public void caseAGtExpr(AGtExpr node) {
		String varName = node.getVar().getText();
		IntegerVariable lvalue = environment.get(varName).getIntVar();
		IntegerExpressionVariable rvalue = createIntExpression(node.getExpr());
		constraint = Choco.gt(lvalue, rvalue);
	}

	@Override
	public void caseALeqExpr(ALeqExpr node) {
		String varName = node.getVar().getText();
		IntegerVariable lvalue = environment.get(varName).getIntVar();
		IntegerExpressionVariable rvalue = createIntExpression(node.getExpr());
		constraint = Choco.leq(lvalue, rvalue);
	}

	@Override
	public void caseAGeqExpr(AGeqExpr node) {
		String varName = node.getVar().getText();
		IntegerVariable lvalue = environment.get(varName).getIntVar();
		IntegerExpressionVariable rvalue = createIntExpression(node.getExpr());
		constraint = Choco.geq(lvalue, rvalue);
	}
	
	private IntegerExpressionVariable createIntExpression(PExpr expr) {
		if(expr instanceof ANumberExpr) {
			int n = Integer.parseInt(((ANumberExpr) expr).getValue().getText());
			return new IntegerConstantVariable(n);
			
		} else if(expr instanceof AVarExpr) {
			String varName = ((AVarExpr) expr).getName().getText();
			Value entry = environment.get(varName);
			if(entry.isInit()) {
				if(entry.isInt()) {
					return entry.getInt();
				} else if(entry.isIntVar()) {
					return entry.getIntVar();
				}
				throw new AssertionError("unexpected list type variable in constraint creation: " + varName + " : " + entry);
			}
			throw new AssertionError("uninitialized variable in constraint creation: " + varName + " : " + entry);
			
		} else if(expr instanceof APlusExpr) {
			PExpr left 	= ((APlusExpr) expr).getLeft();
			PExpr right	= ((APlusExpr) expr).getRight();
			return Choco.plus(createIntExpression(left), createIntExpression(right));
			
		} else if(expr instanceof AMinusExpr) {
			PExpr left 	= ((AMinusExpr) expr).getLeft();
			PExpr right	= ((AMinusExpr) expr).getRight();
			return Choco.minus(createIntExpression(left), createIntExpression(right));
			
		} else if(expr instanceof AMultExpr) {
			PExpr left 	= ((AMultExpr) expr).getLeft();
			PExpr right	= ((AMultExpr) expr).getRight();
			return Choco.mult(createIntExpression(left), createIntExpression(right));
			
		} else if(expr instanceof ADivExpr) {
			PExpr left 	= ((ADivExpr) expr).getLeft();
			PExpr right	= ((ADivExpr) expr).getRight();
			return Choco.div(createIntExpression(left), createIntExpression(right));
			
		} else if(expr instanceof AModExpr) {
			PExpr left 	= ((AModExpr) expr).getLeft();
			PExpr right	= ((AModExpr) expr).getRight();
			return Choco.mod(createIntExpression(left), createIntExpression(right));
		}
		throw new AssertionError("unexpected ast node in constraint creation");
	}
}
