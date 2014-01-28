package interpreter.eval;

import interpreter.data.ChocoStore;
import interpreter.data.Environment;
import interpreter.data.ListVariable;
import interpreter.data.Value;
import parser.analysis.DepthFirstAdapter;
import parser.node.AEqExpr;
import parser.node.AGeqExpr;
import parser.node.AGtExpr;
import parser.node.ALeqExpr;
import parser.node.AListConstructorExpr;
import parser.node.AListExpr;
import parser.node.ALtExpr;
import parser.node.ANeqExpr;
import parser.node.ANumberExpr;
import parser.node.ARule;
import parser.node.AVarExpr;
import parser.node.AWildcardExpr;
import parser.node.PExpr;
import parser.node.TIdent;
import util.PrettyPrinter;
import choco.Choco;
import choco.kernel.model.variables.integer.IntegerConstantVariable;

public final class TellEvaluator extends DepthFirstAdapter {

	private Environment environment;
	private ChocoStore store;
	private IntConstraintTransformer transform;
	
	public TellEvaluator(Environment environment, ChocoStore store) {
		this.environment = environment;
		this.store = store;
		this.transform = new IntConstraintTransformer(environment);
	}

	@Override
	public void caseARule(ARule node) {
		PrettyPrinter p = new PrettyPrinter();
		for(PExpr e : node.getTell()) {
			e.apply(p);
			//System.out.println("<< EVAL: " + p.getString() + "\n");
			e.apply(this);
			p.reset();
		}
	}
	
	@Override
	public void caseAEqExpr(AEqExpr node) {
		PExpr expr = node.getExpr();
		if(expr instanceof AVarExpr) {
			String leftVarName = node.getVar().getText();
			String rightVarName = ((AVarExpr) expr).getName().getText();
			Value lvalue = environment.get(leftVarName);
			Value rvalue = environment.get(rightVarName);
			if(lvalue.isInit()) {
				if(lvalue.isIntVar()) {
					tellIntConstraint(node, node.getVar(), expr);
				} else if(lvalue.isListVar()) {
					//tellListConstraint(node, node.getVar(), expr);
					tellList(lvalue.getListVar(), rvalue.getListVar());
				}
			} else if(rvalue.isInit()) {
				if(rvalue.isInt()) {
					tellIntConstraint(node, node.getVar(), expr);
				} else if(rvalue.isIntVar()) {
					tellIntConstraint(node, node.getVar(), expr);
				} else if(rvalue.isListVar()) {
					//tellListConstraint(node, node.getVar(), expr);
					if(!lvalue.isInit()) {
						lvalue.init(new ListVariable());
					}
					tellList(lvalue.getListVar(), rvalue.getListVar());
				}
			}
		} else if(expr instanceof AListExpr || expr instanceof AListConstructorExpr) {
			tellListConstraint(node, node.getVar(), expr);
		} else {
			tellIntConstraint(node, node.getVar(), expr);
		}
	}

	@Override
	public void caseANeqExpr(ANeqExpr node) {
		PExpr expr = node.getExpr();
		if(expr instanceof AVarExpr) {
			String leftVarName = node.getVar().getText();
			String rightVarName = ((AVarExpr) expr).getName().getText();
			Value lvalue = environment.get(leftVarName);
			Value rvalue = environment.get(rightVarName);
			if(lvalue.isInit()) {
				if(lvalue.isIntVar()) {
					tellIntConstraint(node, node.getVar(), expr);
				} else if(lvalue.isListVar()) {
					throw new UnsupportedOperationException("Inequality constraints on lists are not supported (yet)!");
				}
			} else if(rvalue.isInit()) {
				if(rvalue.isInt()) {
					tellIntConstraint(node, node.getVar(), expr);
				} else if(rvalue.isIntVar()) {
					tellIntConstraint(node, node.getVar(), expr);
				} else if(rvalue.isListVar()) {
					throw new UnsupportedOperationException("Inequality constraints on lists are not supported (yet)!");
				}
			}
		} else if(expr instanceof AListExpr || expr instanceof AListConstructorExpr) {
			throw new UnsupportedOperationException("Inequality constraints on lists are not supported (yet)!");
		} else {
			tellIntConstraint(node, node.getVar(), expr);
		}
	}

	@Override
	public void caseALtExpr(ALtExpr node) {
		tellIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseAGtExpr(AGtExpr node) {
		tellIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseALeqExpr(ALeqExpr node) {
		tellIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseAGeqExpr(AGeqExpr node) {
		tellIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseTIdent(TIdent node) {
		Value entry = environment.get(node.getText());
		if(!entry.isInit()) {
			entry.init(Choco.makeIntVar(node.getText()));
		}
	}
	
	private void tellIntConstraint(PExpr node, TIdent var, PExpr expr) {
		var.apply(this);
		expr.apply(this);
		node.apply(transform);
		store.tell(transform.getConstraint());
	}
	
	private void tellListConstraint(AEqExpr node, TIdent var, PExpr expr) {
		Value entry = environment.get(var.getText());
		if(!entry.isInit()) {
			entry.init(new ListVariable());
		}
		
		if(expr instanceof AListExpr) {
			tellList(entry.getListVar(), (AListExpr)expr);
		} else {
			tellList(entry.getListVar(), (AListConstructorExpr)expr);
		}
	}

	private void tellList(ListVariable list, AListExpr pattern) {
		for(PExpr elem : pattern.getElements()) {
			if(elem instanceof ANumberExpr) {
				String intString = ((ANumberExpr) elem).getValue().getText();
				int intValue = Integer.parseInt(intString);
				Value head = new Value("");
				head.init(new IntegerConstantVariable(intValue));
				list.setHead(head);
			} else if(elem instanceof AVarExpr) {
				String varName = ((AVarExpr) elem).getName().getText();
				list.setHead(environment.get(varName));
			} else if(elem instanceof AListExpr) {
				ListVariable listVar = new ListVariable();
				tellList(listVar, (AListExpr) elem);
				Value head = new Value("");
				head.init(listVar);
				list.setHead(head);
			} else if(elem instanceof AListConstructorExpr) {
				ListVariable listVar = new ListVariable();
				tellList(listVar, (AListConstructorExpr) elem);
				Value head = new Value("");
				head.init(listVar);
				list.setHead(head);
			} else if(elem instanceof AWildcardExpr) {
				list.setHead(new Value("_"));
			}
			list = list.getTail().getListVar();
		}
		list.setEmpty();
	}
	
	private void tellList(ListVariable list, AListConstructorExpr pattern) {
		ListVariable previousListNode = null;
		for(PExpr elem : pattern.getHead()) {
			if(elem instanceof ANumberExpr) {
				String intString = ((ANumberExpr) elem).getValue().getText();
				int intValue = Integer.parseInt(intString);
				Value head = new Value("");
				head.init(new IntegerConstantVariable(intValue));
				list.setHead(head);
			} else if(elem instanceof AVarExpr) {
				String varName = ((AVarExpr) elem).getName().getText();
				list.setHead(environment.get(varName));
			} else if(elem instanceof AListExpr) {
				ListVariable listVar = new ListVariable();
				tellList(listVar, (AListExpr) elem);
				Value head = new Value("");
				head.init(listVar);
				list.setHead(head);
			} else if(elem instanceof AListConstructorExpr) {
				ListVariable listVar = new ListVariable();
				tellList(listVar, (AListConstructorExpr) elem);
				Value head = new Value("");
				head.init(listVar);
				list.setHead(head);
			} else if(elem instanceof AWildcardExpr) {
				list.setHead(new Value("_"));
			}
			previousListNode = list;
			list = list.getTail().getListVar();
		}
		
		PExpr tail = pattern.getTail();
		if(tail instanceof AVarExpr) {
			String varName = ((AVarExpr) tail).getName().getText();
			Value value = environment.get(varName);
			if(!value.isInit()) {
				value.init(new ListVariable());
			}
			previousListNode.setTail(value);
		} else if(tail instanceof AListExpr) {
			tellList(list, (AListExpr) tail);
		} else if(tail instanceof AListConstructorExpr) {
			tellList(list, (AListConstructorExpr) tail);
		} else if(tail instanceof AWildcardExpr) {
			Value value = new Value("_");
			value.init(new ListVariable());
			previousListNode.setTail(value);
		}
	}
	
	private void tellList(ListVariable list1, ListVariable list2) {
		list1.setHead(list2.getHead());
		list1.setTail(list2.getTail());
	}
}
