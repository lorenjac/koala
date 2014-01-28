package interpreter.eval;

import interpreter.data.ChocoStore;
import interpreter.data.Environment;
import interpreter.data.ListVariable;
import interpreter.data.Value;
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
import parser.node.ANumberExpr;
import parser.node.ARule;
import parser.node.ATrueExpr;
import parser.node.AVarExpr;
import parser.node.AWildcardExpr;
import parser.node.PExpr;
import parser.node.TIdent;
import util.PrettyPrinter;

public class AskEvaluator extends DepthFirstAdapter {

	private enum AskResult {
		TRUE,
		FALSE,
		UNKNOWN
	}
	
	private Environment environment;
	private ChocoStore store;
	private boolean isSatisfied;
	private IntConstraintTransformer transform;

	public AskEvaluator(Environment environment, ChocoStore store) {
		this.environment = environment;
		this.store = store;
		this.isSatisfied = true;
		this.transform = new IntConstraintTransformer(environment);
	}

	public boolean isSatisfied() {
		return isSatisfied;
	}

	@Override
	public void caseARule(ARule node) {
		PrettyPrinter p = new PrettyPrinter();
		for(PExpr e : node.getAsk()) {
			e.apply(p);
			//System.out.println("<< CHECKING ASK: " + p.getString() + "\n");
			e.apply(this);
			p.reset();
			if(!isSatisfied) {
				break;
			}
		}
	}

	@Override
	public void caseAEqExpr(AEqExpr node) {
		PExpr expr = node.getExpr();
		if(expr instanceof AVarExpr) {
			Value lvalue = environment.get(node.getVar().getText());
			Value rvalue = environment.get(((AVarExpr) expr).getName().getText());
			if((lvalue.isInt() || lvalue.isIntVar()) && (rvalue.isInt() || rvalue.isIntVar())) {
				askIntConstraint(node, node.getVar(), expr);
			} else if(lvalue.isListVar() && rvalue.isListVar()) {
				AskResult askResult = compare(lvalue.getListVar(), rvalue.getListVar());
				if(askResult != AskResult.TRUE) {
					isSatisfied = false;
				}
			} else {
				isSatisfied = false;
			}
		} else if(expr instanceof AListExpr || 
				  expr instanceof AListConstructorExpr) {
			//System.out.println("calling askListConstraint()");
			askListConstraint(node, node.getVar(), expr);
		} else {
			askIntConstraint(node, node.getVar(), expr);
		}
	}

	@Override
	public void caseANeqExpr(ANeqExpr node) {
		PExpr expr = node.getExpr();
		if(expr instanceof AVarExpr) {
			Value lvalue = environment.get(node.getVar().getText());
			Value rvalue = environment.get(((AVarExpr) expr).getName().getText());
			if((lvalue.isInt() || lvalue.isIntVar()) && (rvalue.isInt() || rvalue.isIntVar())) {
				askIntConstraint(node, node.getVar(), expr);
			} else if(lvalue.isListVar() && rvalue.isListVar()) {
				AskResult askResult = compare(lvalue.getListVar(), rvalue.getListVar());
				if(askResult != AskResult.FALSE) {
					isSatisfied = false;
				}
			} else {
				isSatisfied = false;
			}
		} else if(expr instanceof AListExpr || 
				  expr instanceof AListConstructorExpr) {
			askListConstraint(node, node.getVar(), expr);
		} else {
			askIntConstraint(node, node.getVar(), expr);
		}
	}

	@Override
	public void caseALtExpr(ALtExpr node) {
		askIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseAGtExpr(AGtExpr node) {
		askIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseALeqExpr(ALeqExpr node) {
		askIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseAGeqExpr(AGeqExpr node) {
		askIntConstraint(node, node.getVar(), node.getExpr());
	}

	@Override
	public void caseTIdent(TIdent node) {
		if(!environment.get(node.getText()).isInit()) {
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

	private void askIntConstraint(PExpr node, TIdent var, PExpr expr) {
		var.apply(this);
		expr.apply(this);
		if(isSatisfied) {
			node.apply(transform);
			isSatisfied &= store.ask(transform.getConstraint());
		}
	}
	
	private void askListConstraint(PExpr node, TIdent var, PExpr expr) {
		var.apply(this);
		if(!environment.get(var.getText()).isInit()) {
			//System.out.println("lvalue not initialized");
			isSatisfied = false;
			return;
		}
		ListVariable lvalue = environment.get(var.getText()).getListVar();
		AskResult askResult = null;
		if(expr instanceof AListExpr) {
			askResult = askList(lvalue, (AListExpr)expr);
		} else {
			askResult = askList(lvalue, (AListConstructorExpr)expr);
		}
		
		//System.out.println("matching returned " + askResult);
		
		//ask-constraint with unbound variables must always fail 
		if(askResult == AskResult.UNKNOWN) {
			isSatisfied = false;
		}
		
		//apply relational constraint operator
		if(node instanceof AEqExpr) {
			isSatisfied &= (askResult == AskResult.TRUE);
		} else {
			isSatisfied &= (askResult == AskResult.FALSE);
		}
	}

	private AskResult askList(ListVariable list, AListExpr pattern) {
		if(list.isUnbound()) {
			return AskResult.UNKNOWN;
		}
		
		if(list.isEmpty() != pattern.getElements().isEmpty()) {
			return AskResult.FALSE;
		}
		
		//match each list element to its pattern
		for(PExpr elem : pattern.getElements()) {
			//check if list is bound and non-empty (pattern isn't)
			if(list.isUnbound()) {
				return AskResult.UNKNOWN;
			} if(list.isEmpty()) {
				return AskResult.FALSE;
			}
			
			//retrieve actual list head
			Value head = list.getHead();
			
			//match element with pattern
			if(elem instanceof ANumberExpr) {
				String intString = ((ANumberExpr) elem).getValue().getText();
				int n = Integer.parseInt(intString);
				if(!head.isInit()) {
					return AskResult.UNKNOWN;
				} else if(head.isInt()) {
					if(head.getInt().getValue() != n) {
						return AskResult.FALSE;
					}
				} else if(head.isIntVar()) {
					Integer integer = store.getValueOfVar(head.getIntVar());
					if(integer == null) {
						return AskResult.UNKNOWN;
					} else if(integer.intValue() != n) {
						return AskResult.FALSE;
					}
				} else {
					//head element is a list -> mismatch
					return AskResult.FALSE;
				}
			} else if(elem instanceof AVarExpr) {
				String varName = ((AVarExpr) elem).getName().getText();
				Value var = environment.get(varName);
				
				if(var.isInt()) {
					//local variable is integer constant
					int n = var.getInt().getValue();
					if(head.isInt()) {
						if(head.getInt().getValue() != n) {
							return AskResult.FALSE;
						}
					} else if(head.isIntVar()) {
						Integer integer = store.getValueOfVar(head.getIntVar());
						if(integer == null) {
							return AskResult.UNKNOWN;
						} else if(integer.intValue() != n) {
							return AskResult.FALSE;
						}
					} else if(head.isListVar()) {
						return AskResult.FALSE;
					} else {
						return AskResult.UNKNOWN;
					}
				} else if(var.isIntVar()) {
					//local variable is integer variable
					Integer integer = store.getValueOfVar(var.getIntVar());
					if(integer != null) {
						int n = integer.intValue();
						if(head.isInt()) {
							if(head.getInt().getValue() != n) {
								return AskResult.FALSE;
							}
						} else if(head.isIntVar()) {
							integer = store.getValueOfVar(head.getIntVar());
							if(integer == null) {
								return AskResult.UNKNOWN;
							} else if(integer.intValue() != n) {
								return AskResult.FALSE;
							}
						} else if(head.isListVar()) {
							return AskResult.FALSE;
						} else {
							return AskResult.UNKNOWN;
						}
					} else {
						return AskResult.UNKNOWN;
					}
				} else if(var.isListVar()) {
					//local variable is list
					if(head.isListVar()) {
						AskResult askResult = compare(var.getListVar(), head.getListVar());
						if(askResult != AskResult.TRUE) {
							return askResult;
						}
					} else if(head.isInit()) {
						return AskResult.FALSE;
					} else {
						return AskResult.UNKNOWN;
					}
				} else {
					//var is not initialized -> init var with list head
					if(head.isInt()) {
						var.init(head.getInt());
					} else if(head.isIntVar()) {
						var.init(head.getIntVar());
					} else if(head.isListVar()) {
						var.init(head.getListVar());
					} else {
						//TODO check what happens if both local variable and head variable are uninitialized
						return AskResult.UNKNOWN;
					}
				}
			} else if(elem instanceof AListExpr) {
				if(head.isListVar()) {
					AskResult askResult = askList(head.getListVar(), (AListExpr)elem);
					if(askResult != AskResult.TRUE) {
						return askResult;
					}
				} else {
					//head element is NOT a list -> mismatch
					return AskResult.FALSE;
				}
			} else if(elem instanceof AListConstructorExpr) {
				if(head.isListVar()) {
					AskResult askResult = askList(head.getListVar(), (AListConstructorExpr)elem);
					if(askResult != AskResult.TRUE) {
						return askResult;
					}
				} else {
					//head element is NOT a list -> mismatch
					return AskResult.FALSE;
				}
			} else if(elem instanceof AWildcardExpr) {
				//empty, head is arbitrary
			}
			
			//get list tail
			list = list.getTail().getListVar();
		}
		
		//check if tail is bound and empty (list is immediate)
		if(list.isUnbound()) {
			return AskResult.UNKNOWN;
		} else if(!list.isEmpty()) {
			return AskResult.FALSE;
		}
		return AskResult.TRUE;
	}

	private AskResult askList(ListVariable list, AListConstructorExpr pattern) {
		if(list.isUnbound()) {
			return AskResult.UNKNOWN;
		}
		
		if(list.isEmpty() != pattern.getHead().isEmpty()) {
			return AskResult.FALSE;
		}
		
		//match each list element to its pattern
		for(PExpr elem : pattern.getHead()) {
			//check if list is bound and non-empty (pattern isn't)
			if(list.isUnbound()) {
				return AskResult.UNKNOWN;
			} if(list.isEmpty()) {
				return AskResult.FALSE;
			}
			
			//retrieve actual list head
			Value head = list.getHead();
			
			//match element with pattern
			if(elem instanceof ANumberExpr) {
				String intString = ((ANumberExpr) elem).getValue().getText();
				int n = Integer.parseInt(intString);
				if(!head.isInit()) {
					return AskResult.UNKNOWN;
				} else if(head.isInt()) {
					if(head.getInt().getValue() != n) {
						return AskResult.FALSE;
					}
				} else if(head.isIntVar()) {
					Integer integer = store.getValueOfVar(head.getIntVar());
					if(integer == null) {
						return AskResult.UNKNOWN;
					} else if(integer.intValue() != n) {
						return AskResult.FALSE;
					}
				} else {
					//head element is a list -> mismatch
					return AskResult.FALSE;
				}
			} else if(elem instanceof AVarExpr) {
				String varName = ((AVarExpr) elem).getName().getText();
				Value var = environment.get(varName);
				
				if(var.isInt()) {
					//local variable is integer constant
					int n = var.getInt().getValue();
					if(head.isInt()) {
						if(head.getInt().getValue() != n) {
							return AskResult.FALSE;
						}
					} else if(head.isIntVar()) {
						Integer integer = store.getValueOfVar(head.getIntVar());
						if(integer == null) {
							return AskResult.UNKNOWN;
						} else if(integer.intValue() != n) {
							return AskResult.FALSE;
						}
					} else if(head.isListVar()) {
						return AskResult.FALSE;
					} else {
						return AskResult.UNKNOWN;
					}
				} else if(var.isIntVar()) {
					//local variable is integer variable
					Integer integer = store.getValueOfVar(var.getIntVar());
					if(integer != null) {
						int n = integer.intValue();
						if(head.isInt()) {
							if(head.getInt().getValue() != n) {
								return AskResult.FALSE;
							}
						} else if(head.isIntVar()) {
							integer = store.getValueOfVar(head.getIntVar());
							if(integer == null) {
								return AskResult.UNKNOWN;
							} else if(integer.intValue() != n) {
								return AskResult.FALSE;
							}
						} else if(head.isListVar()) {
							return AskResult.FALSE;
						} else {
							return AskResult.UNKNOWN;
						}
					} else {
						return AskResult.UNKNOWN;
					}
				} else if(var.isListVar()) {
					//local variable is list
					if(head.isListVar()) {
						AskResult askResult = compare(var.getListVar(), head.getListVar());
						if(askResult != AskResult.TRUE) {
							return askResult;
						}
					} else if(head.isInit()) {
						return AskResult.FALSE;
					} else {
						return AskResult.UNKNOWN;
					}
				} else {
					//var is not initialized -> init var with list head
					if(head.isInt()) {
						var.init(head.getInt());
					} else if(head.isIntVar()) {
						var.init(head.getIntVar());
					} else if(head.isListVar()) {
						var.init(head.getListVar());
					} else {
						//TODO check what happens if both local variable and head variable are uninitialized
						return AskResult.UNKNOWN;
					}
				}
			} else if(elem instanceof AListExpr) {
				if(head.isListVar()) {
					AskResult askResult = askList(head.getListVar(), (AListExpr)elem);
					if(askResult != AskResult.TRUE) {
						return askResult;
					}
				} else if(head.isInit()) {
					//head element is NOT a list -> mismatch
					return AskResult.FALSE;
				} else {
					return AskResult.UNKNOWN;
				}
			} else if(elem instanceof AListConstructorExpr) {
				if(head.isListVar()) {
					AskResult askResult = askList(head.getListVar(), (AListConstructorExpr)elem);
					if(askResult != AskResult.TRUE) {
						return askResult;
					}
				} else if(head.isInit()) {
					//head element is NOT a list -> mismatch
					return AskResult.FALSE;
				} else {
					return AskResult.UNKNOWN;
				}
			} else if(elem instanceof AWildcardExpr) {
				//empty, head is arbitrary
			}
			
			//get list tail
			list = list.getTail().getListVar();
		}
		
		PExpr tail = pattern.getTail();
		if(tail instanceof AVarExpr) {
			String varName = ((AVarExpr) tail).getName().getText();
			Value var = environment.get(varName);
			if(var.isListVar()) {
				AskResult askResult = compare(list, var.getListVar());
				if(askResult != AskResult.TRUE) {
					return askResult;
				}
			} else if(var.isInit()) {
				return AskResult.FALSE;
			} else {
				var.init(list);
			}
		} else if(tail instanceof AListExpr) {
			AskResult askResult = askList(list, (AListExpr)tail);
			if(askResult != AskResult.TRUE) {
				return askResult;
			}
		} else if(tail instanceof AListConstructorExpr) {
			AskResult askResult = askList(list, (AListConstructorExpr)tail);
			if(askResult != AskResult.TRUE) {
				return askResult;
			}
		} else if(tail instanceof AWildcardExpr) {
			//empty, head is arbitrary
		}
		return AskResult.TRUE;
	}
	
	private AskResult compare(ListVariable list1, ListVariable list2) {
		while(true) {
			if(list1.isUnbound() || list2.isUnbound()) {
				return AskResult.UNKNOWN;
			}
			
			if(list1.isEmpty() != list2.isEmpty()) {
				return AskResult.FALSE;
			} else if(list1.isEmpty() && list2.isEmpty()) {
				return AskResult.TRUE;
			}
			
			Value head1 = list1.getHead();
			Value head2 = list2.getHead();
			if(head1.isInt() && head2.isInt()) {
				if(head1.getInt().getValue() != head2.getInt().getValue()) {
					return AskResult.FALSE;
				}
			} else if(head1.isIntVar() && head2.isIntVar()) {
				Integer integer1 = store.getValueOfVar(head1.getIntVar());
				Integer integer2 = store.getValueOfVar(head2.getIntVar());
				if(integer1 == null || integer2 == null) {
					return AskResult.UNKNOWN;
				} else if(integer1.intValue() != integer2.intValue()) {
					return AskResult.FALSE;
				}
			} else if(head1.isListVar() && head2.isListVar()) {
				AskResult askResult = compare(head1.getListVar(), head2.getListVar());
				if(askResult != AskResult.TRUE) {
					return askResult;
				}
			} else if(!head1.isInit() || !head2.isInit()) {
				return AskResult.UNKNOWN;
			}
			list1 = list1.getTail().getListVar();
			list2 = list2.getTail().getListVar();
		}
	}
}
