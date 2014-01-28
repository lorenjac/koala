package interpreter.eval;

import interpreter.data.ChocoStore;
import interpreter.data.Environment;
import interpreter.data.ListVariable;
import interpreter.data.Value;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import parser.analysis.DepthFirstAdapter;
import parser.node.AEqExpr;
import parser.node.AFalseExpr;
import parser.node.AGeqExpr;
import parser.node.AGtExpr;
import parser.node.ALeqExpr;
import parser.node.AListConstructorExpr;
import parser.node.AListExpr;
import parser.node.ALtExpr;
import parser.node.ANeqExpr;
import parser.node.ARule;
import parser.node.ATrueExpr;
import parser.node.AVarExpr;
import parser.node.PExpr;
import parser.node.TIdent;
import choco.Choco;
import choco.kernel.model.constraints.Constraint;

public final class TellCheckEvaluator extends DepthFirstAdapter {

	private Environment environment;
	private ChocoStore store;
	private boolean isSatisfied;
	private IntConstraintTransformer transform;
	private List<Constraint> constraintList;
	private Set<String> boundVariables;
	
	public TellCheckEvaluator(Environment environment, ChocoStore store) {
		this.environment = environment;
		this.store = store;
		this.isSatisfied = true;
		this.transform = new IntConstraintTransformer(environment);
		this.constraintList = new LinkedList<>();
		this.boundVariables = new HashSet<>();
	}

	public boolean isSatisfied() {
		return isSatisfied;
	}

	@Override
	public void caseARule(ARule node) {
		for(PExpr e : node.getTell()) {
			e.apply(this);
			if(!isSatisfied) {
				break;
			}
		}
		isIntTellOk();
	}

	@Override
	public void caseAEqExpr(AEqExpr node) {
		PExpr expr = node.getExpr();
		if(expr instanceof AVarExpr) {
			String leftVarName = node.getVar().getText();
			if(boundVariables.contains(leftVarName)) {
				isSatisfied = false;
				return;
			}
			
			String rightVarName = ((AVarExpr) expr).getName().getText();
			Value lvalue = environment.get(leftVarName);
			Value rvalue = environment.get(rightVarName);
			
			if(lvalue.isInit()) {
				if(lvalue.isInt()) {
					isSatisfied = false;
				} else if(lvalue.isIntVar()) {
					if(!rvalue.isInit() || rvalue.isInt() || rvalue.isIntVar()) {
						addIntConstraint(node, node.getVar(), expr);
					} else {
						isSatisfied = false;
					}
				} else if(lvalue.isListVar()) {
					if(lvalue.getListVar().isUnbound()) {
						if(rvalue.isListVar()) {
							isListTellOk(node, node.getVar());
						} else {
							isSatisfied = false;
						}
					} else {
						isSatisfied = false;
					}
				}
			} else if(rvalue.isInit()) {
				if(rvalue.isInt()) {
					addIntConstraint(node, node.getVar(), expr);
				} else if(rvalue.isIntVar()) {
					if(lvalue.isListVar()) {
						isSatisfied = false;
					} else {
						addIntConstraint(node, node.getVar(), expr);
					}
				} else if(rvalue.isListVar()) {
					if(!lvalue.isInit() || lvalue.isListVar()) {
						isListTellOk(node, node.getVar());
					} else {
						isSatisfied = false;
					}
				}
			} else {
				isSatisfied = false;
			}
		} else if(expr instanceof AListExpr || expr instanceof AListConstructorExpr) {
			isListTellOk(node, node.getVar());
		} else {
			addIntConstraint(node, node.getVar(), expr);
		}
	}

	@Override
	public void caseANeqExpr(ANeqExpr node) {
		PExpr expr = node.getExpr();
		if(expr instanceof AVarExpr) {
			String leftVarName = node.getVar().getText();
			if(boundVariables.contains(leftVarName)) {
				isSatisfied = false;
				return;
			}
			
			String rightVarName = ((AVarExpr) expr).getName().getText();
			Value lvalue = environment.get(leftVarName);
			Value rvalue = environment.get(rightVarName);
			
			if(lvalue.isInit()) {
				if(lvalue.isInt()) {
					isSatisfied = false;
				} else if(lvalue.isIntVar()) {
					if(!rvalue.isInit() || rvalue.isInt() || rvalue.isIntVar()) {
						addIntConstraint(node, node.getVar(), expr);
					} else {
						isSatisfied = false;
					}
				}
			} else if(rvalue.isInit()) {
				if(rvalue.isInt()) {
					addIntConstraint(node, node.getVar(), expr);
				} else if(rvalue.isIntVar()) {
					if(lvalue.isListVar()) {
						isSatisfied = false;
					} else {
						addIntConstraint(node, node.getVar(), expr);
					}
				}
			} else {
				isSatisfied = false;
			}
		} else if(expr instanceof AListExpr || expr instanceof AListConstructorExpr) {
			throw new UnsupportedOperationException("Inequality constraints on lists are not supported (yet)!");
		} else {
			addIntConstraint(node, node.getVar(), expr);
		}
	}

	@Override
	public void caseALtExpr(ALtExpr node) {
		addIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseAGtExpr(AGtExpr node) {
		addIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseALeqExpr(ALeqExpr node) {
		addIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseAGeqExpr(AGeqExpr node) {
		addIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseAVarExpr(AVarExpr node) {
		//this node must be contained in a right value (numeric) expression
		String varName = node.getName().getText();
		Value entry = environment.get(varName);
		if(!entry.isInit()) {
			entry.init(Choco.makeIntVar(varName));
		}
	}

	@Override
	public void caseTIdent(TIdent node) {
		//this node must be contained in a left value (numeric) expression
		String varName = node.getText();
		Value entry = environment.get(varName);
		if(boundVariables.contains(varName)) {
			isSatisfied = false;
		} else if(!entry.isInit()) {
			entry.init(Choco.makeIntVar(varName));
		} else if(entry.isInt()) {
			isSatisfied = false;
		}
	}
	
	@Override
	public void caseAFalseExpr(AFalseExpr node) {
		isSatisfied = false;
	}

	@Override
	public void caseATrueExpr(ATrueExpr node) {
		//empty, base result is 'true' already
	}
	
	private void addIntConstraint(PExpr node, TIdent var, PExpr expr) {
		var.apply(this);
		expr.apply(this);
		if(isSatisfied) {
			node.apply(transform);
			constraintList.add(transform.getConstraint());
			boundVariables.add(var.getText());
		}
	}
	
	private void isIntTellOk() {
		//check tell of conjunction of all numeric constraints
		if(!constraintList.isEmpty()) {
			Constraint[] constraints = new Constraint[constraintList.size()];
			constraintList.<Constraint>toArray(constraints);
			isSatisfied &= store.isTellOk(Choco.and(constraints));
		}
	}
	
	private void isListTellOk(AEqExpr node, TIdent var) {
		String varName = var.getText();
		Value entry = environment.get(varName);
		if(boundVariables.contains(varName)) {
			isSatisfied = false;
		} else if(!entry.isInit()) {
			entry.init(new ListVariable());
			boundVariables.add(varName);
		} else {
			isSatisfied = entry.getListVar().isUnbound();
			boundVariables.add(varName);
		}
	}
}
