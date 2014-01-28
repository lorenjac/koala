package checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import parser.analysis.DepthFirstAdapter;
import parser.node.ADivExpr;
import parser.node.AEqExpr;
import parser.node.AFalseExpr;
import parser.node.AGeqExpr;
import parser.node.AGtExpr;
import parser.node.ALeqExpr;
import parser.node.AListConstructorExpr;
import parser.node.AListExpr;
import parser.node.ALtExpr;
import parser.node.AMinusExpr;
import parser.node.AModExpr;
import parser.node.AMultExpr;
import parser.node.ANeqExpr;
import parser.node.ANumberExpr;
import parser.node.APlusExpr;
import parser.node.ARule;
import parser.node.ATrueExpr;
import parser.node.AVarExpr;
import parser.node.AWildcardExpr;
import parser.node.Node;
import parser.node.PExpr;
import parser.node.TFalse;
import parser.node.TIdent;
import parser.node.TNumber;
import util.PrettyPrinter;
import checker.ContextChecker.Type;

/*
 * [AskExprCtxCheck]
 * # all variables used in an ask-constraint (lvalue + rvalue) must be defined in parameter list unless invoked inside list expression
 * # constraints must be stated over equal types, int vs. int, list vs. list
 * # no other operation than "=" or "=//=" can be applied to lists
 * # list tail cannot be of type integer
 * # emit warning if ask-constraint is false (will block its rule forever)
 * # emit warning if rvalue is lvalue variable (will always return true for ==, <=, >=; always false for =//=, <, >)
 */

public final class AskContextChecker extends DepthFirstAdapter {
	
	protected enum Context {
		ARITHMETIC,
		LIST_HEAD,
		LIST_TAIL,
		NONE
	}
	
	protected enum ConstraintOp {
		EQ,
		NEQ,
		LT,
		LEQ,
		GT,
		GEQ
	}
	
	/**
	 * stores local variable names and their inferred types
	 */
	private final Map<String, Type>	localSymbolTable;
	
	/**
	 * stores all error/warning messages
	 */
	private final List<String> 		messages;
	
	/**
	 * keeps track of the current context in a rvalue expression
	 */
	private Context 			context;
	
	/**
	 * constructs a dfs tree walker that checks semantic properties of ask-constraints;
	 * it receives a reference to a symbol table of local variables and their types
	 * and another reference to a list of error/warning messages
	 * to store additional messages if need be
	 * 
	 * @param localSymbolTable
	 * 							mutable symbol table of all local variables and their types
	 * @param messages
	 * 							mutable list of error/warning messages
	 */
	public AskContextChecker(Map<String, Type> localSymbolTable, List<String> messages) {
		this.localSymbolTable = localSymbolTable;
		this.context = Context.NONE;
		this.messages = messages;
	}
	
	/**
	 * mostly equivalent to default implementation
	 * does not visit 'head' node of this rule node
	 */
	@Override
	public void caseARule(ARule node) {
		List<PExpr> copy = new ArrayList<PExpr>(node.getAsk());
        for(PExpr e : copy) {
            e.apply(this);
        }
	}

	@Override
	public void caseAEqExpr(AEqExpr node) {
		context = Context.NONE;
		checkConstraints(node, node.getVar(), node.getExpr(), ConstraintOp.EQ);
	}

	@Override
	public void caseANeqExpr(ANeqExpr node) {
		context = Context.NONE;
		checkConstraints(node, node.getVar(), node.getExpr(), ConstraintOp.NEQ);
	}

	@Override
	public void caseALtExpr(ALtExpr node) {
		context = Context.NONE;
		checkConstraints(node, node.getVar(), node.getExpr(), ConstraintOp.LT);
	}

	@Override
	public void caseAGtExpr(AGtExpr node) {
		context = Context.NONE;
		checkConstraints(node, node.getVar(), node.getExpr(), ConstraintOp.GT);
	}

	@Override
	public void caseALeqExpr(ALeqExpr node) {
		context = Context.NONE;
		checkConstraints(node, node.getVar(), node.getExpr(), ConstraintOp.LEQ);
	}

	@Override
	public void caseAGeqExpr(AGeqExpr node) {
		context = Context.NONE;
		checkConstraints(node, node.getVar(), node.getExpr(), ConstraintOp.GEQ);
	}
	
	
	private void checkConstraints(Node node, TIdent var, PExpr expr, ConstraintOp op) {
		//visit lvalue
		var.apply(this);
		
		//visit rvalue
		expr.apply(this);
		
		//stop here if lvalue is undefined
		if(!localSymbolTable.containsKey(var.getText())) {
			return;
		}
		
		//get inferred type of lvalue
		Type lvalueT = localSymbolTable.get(var.getText());
		
		/* faulty type inference: 
		 * inferring lvalue type based upon operator might be too eager is some cases,
		 * e.g. let the rvalue be a valid expression of type list then a lvalue
		 * variable of previously unknown type (-> obviously of type list) is
		 * inferred to be of type integer because an invalid operator was used;
		 * the rather obvious misuse of an integer-specific operator is
		 * however interpreted as a simple type mismatch leading to
		 * an error message stating that the lvalue variable is of
		 * type integer, whereas the rvalue is inferred to be
		 * of type list, which is a type mismatch. 
		 *  
		 * the result is 
		 * 	# a misleading error message and
		 *  # a wrong type for the lvalue variable where the latter causes
		 *    more type mismatches in obviously correct usage patterns
		 * 
		 * solution: 
		 * 	# delay this offensive type inference to a situation,
		 *    where both operands are of unknown type
		 */
		
		/*
		if(lvalueT == Type.UNKNOWN) {
			//infer lvalue type from rel op
			switch(op) {
			case GEQ:
			case GT:
			case LEQ:
			case LT:
				lvalueT = Type.INTEGER;
				localSymbolTable.put(var.getText(), lvalueT);
				break;
				
			default:
				break;
			}
		}
		*/
		
		//infer rvalue type from its expression context
		Type rvalueT = null;
		switch(context) {
		case NONE: //only entered if rvalue is variable
			TIdent id = ((AVarExpr)expr).getName();
			if(localSymbolTable.containsKey(id.getText())) {
				//retrieve type, may be UNKNOWN
				rvalueT = localSymbolTable.get(id.getText());
			} else {
				//variable undefined, type unknown
				rvalueT = Type.UNKNOWN;
			}
			break;
			
		case ARITHMETIC:
			rvalueT = Type.INTEGER;
			break;
			
		case LIST_HEAD: //no break here
		case LIST_TAIL:
			rvalueT = Type.LIST;
			break;
			
		default:
			throw new AssertionError("UNKOWN CONTEXT " + context);
		}
		
		//infer lvalue type from rel op if no type could be inferred so far
		if(lvalueT == Type.UNKNOWN && rvalueT == Type.UNKNOWN) {
			switch(op) {
			case GEQ:
			case GT:
			case LEQ:
			case LT:
				lvalueT = Type.INTEGER;
				localSymbolTable.put(var.getText(), lvalueT);
				break;
				
			default:
				break;
			}
		}
		
		//type inference variants
		//u = u  -
		//u = I (!) -> annotate
		//u = L (!) -> annotate
		//I = u (?) -> rvalue must be variable -> annotate
		//I = I  -
		//I = L (!) -> error
		//L = u (?) -> rvalue must be variable -> annotate
		//L = I (!) -> error
		//L = L  -
		
		//check if lvalue/rvalue types give rise to type error
		if(lvalueT != rvalueT) {
			if(lvalueT == Type.UNKNOWN) {
				//annotate missing lvalue type
				lvalueT = rvalueT;
				localSymbolTable.put(var.getText(), lvalueT);
				
			} else if(rvalueT == Type.UNKNOWN && expr instanceof AVarExpr) {
				//lvalue has type, rvalue is variable with unresolved type, trying to annotate rvalue if var exists
				String id = ((AVarExpr) expr).getName().getText();
				if(localSymbolTable.containsKey(id)) {
					rvalueT = lvalueT;
					localSymbolTable.put(id, rvalueT);
				}
			} else {
				PrettyPrinter p = new PrettyPrinter();
				node.apply(p);
				messages.add(
					"Semantic error at ["+ var.getLine() + ":" + var.getPos() + "]:\n" +
					"Incompatible types in constraint '" + p.getString() + "'.\n" + 
					"Variable '" + var.getText() + "' is of type '" + lvalueT + "'.\n" +
					"Expression is inferred to be of type '" + rvalueT + "'.\n"
				);
			}
		}
		
		//check only valid operators are being used on list constraint
		if(lvalueT == Type.LIST && rvalueT == Type.LIST &&
		   op != ConstraintOp.EQ && op != ConstraintOp.NEQ) {
			
			messages.add(
				"Semantic error at ["+ var.getLine() + ":" + var.getPos() + "]:\n" +
				"Invalid relation operator applied to list constraint.\n" + 
				"Valid operators are '==' and '=\\='.\n"
			);
		}
		
		//check if constraint is stated over equal variables (forming a tautology)
		if(expr instanceof AVarExpr) {
			TIdent rvalueVar = ((AVarExpr) expr).getName();
			if(var.getText().equals(rvalueVar.getText())) {
				PrettyPrinter p = new PrettyPrinter();
				node.apply(p);
				switch(op) {
				case EQ:
				case LEQ:
				case GEQ:
					messages.add(
						"Warning at ["+ var.getLine() + ":" + var.getPos() + "]:\n" +
						"Tautology in constraint '" + p.getString() + "'.\n" +
						"This constraint is always satisfied.\n"
					);
					break;
					
				case GT:
				case LT:
				case NEQ:
					messages.add(
						"Warning at ["+ var.getLine() + ":" + var.getPos() + "]:\n" +
						"Negative tautology in constraint '" + p.getString() + "'.\n" +
						"This constraint is unsatisfiable. Checking this rule for \n" +
						"application will always fail so the rule will never be evaluated.\n"
					);
					break;
					
				default:
					break;
					
				}
			}
		}
	}
	
	@Override
	public void caseAPlusExpr(APlusExpr node) {
		context = Context.ARITHMETIC;
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}

	@Override
	public void caseAMinusExpr(AMinusExpr node) {
		context = Context.ARITHMETIC;
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}

	@Override
	public void caseAMultExpr(AMultExpr node) {
		context = Context.ARITHMETIC;
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}

	@Override
	public void caseADivExpr(ADivExpr node) {
		context = Context.ARITHMETIC;
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}

	@Override
	public void caseAModExpr(AModExpr node) {
		context = Context.ARITHMETIC;
		node.getLeft().apply(this);
		node.getRight().apply(this);
	}

	@Override
	public void caseAListExpr(AListExpr node) {
		context = Context.LIST_HEAD;
		List<PExpr> copy = new ArrayList<PExpr>(node.getElements());
        for(PExpr e : copy) {
            e.apply(this);
            context = Context.LIST_HEAD;
        }
	}

	@Override
	public void caseAListConstructorExpr(AListConstructorExpr node) {
		context = Context.LIST_HEAD;
		//traverse head
		List<PExpr> copy = new ArrayList<PExpr>(node.getHead());
        for(PExpr e : copy) {
            e.apply(this);
            context = Context.LIST_HEAD;
        }
        //traverse tail
        if(node.getTail() != null) {
        	context = Context.LIST_TAIL;
        	node.getTail().apply(this);
        }
	}

	@Override
	public void caseAVarExpr(AVarExpr node) {
		super.caseAVarExpr(node);
	}

	@Override
	public void caseTIdent(TIdent var) {
		switch(context) {
		case NONE:
			if(!localSymbolTable.containsKey(var.getText())) {
				messages.add(
					"Semantic error at ["+ var.getLine() + ":" + var.getPos() + "]:\n" +
					"Variable '" + var.getText() + "' is undefined in this scope.\n" + 
				    "A variable must be defined prior to usage.\n"
				);
			}
			break;
			
		case ARITHMETIC:
			if(!localSymbolTable.containsKey(var.getText())) {
				messages.add(
					"Semantic error at ["+ var.getLine() + ":" + var.getPos() + "]:\n" +
					"Variable '" + var.getText() + "' is undefined in this scope.\n" + 
				    "A variable must be defined prior to usage.\n"
				);
			} else {
				switch(localSymbolTable.get(var.getText())) {
				case LIST:
					messages.add(
						"Semantic error at ["+ var.getLine() + ":" + var.getPos() + "]:\n" +
						"Conflicting types for variable '" + var.getText() + "'.\n" +
						"Variable has type '" + Type.INTEGER + "' in this context,\n" +
						"but was inferred to be type of '" + Type.LIST + "' earlier.\n"
					);
					break;
					
				case UNKNOWN:
					localSymbolTable.put(var.getText(), Type.INTEGER);
					break;
					
				default:
					break;
				}
			}
			break;
			
		case LIST_HEAD:
			//introduce variable if not yet present
			if(!localSymbolTable.containsKey(var.getText())) {
				localSymbolTable.put(var.getText(), Type.UNKNOWN);
			}
			break;
		
		case LIST_TAIL:
			//if variable is not present: introduce variable
			//if variable is present: annotate type or detect type error
			if(!localSymbolTable.containsKey(var.getText())) {
				localSymbolTable.put(var.getText(), Type.LIST);
			} else {
				Type varT = localSymbolTable.get(var.getText());
				if(varT == Type.UNKNOWN) {
					localSymbolTable.put(var.getText(), Type.LIST);
				} else if(varT != Type.LIST) {
					messages.add(
						"Semantic error at ["+ var.getLine() + ":" + var.getPos() + "]:\n" +
						"Conflicting types for variable '" + var.getText() + "'.\n" +
						"Variable has type '" + Type.LIST + "' in this context, but was" +
						"inferred to be type of '" + varT + "' earlier.\n"
					);
				}
			}
			break;
			
		default:
			throw new AssertionError("Unexpected context " + context);
		}
	}

	@Override
	public void caseANumberExpr(ANumberExpr node) {
		if(context == Context.LIST_TAIL) {
			TNumber num = node.getValue();
			messages.add(
				"Semantic error at ["+ num.getLine() + ":" + num.getPos() + "]:\n" +
				"Invalid list tail '" + num.getText() + "'.\n" +
				"A list tail must be of type '" + Type.LIST + "'.\n" +
				"Valid list tails are wildcards, variables or immediate lists.\n"
			);
		} else {
			context = Context.ARITHMETIC;
		}
	}
	
	@Override
	public void caseAWildcardExpr(AWildcardExpr node) {
		//empty body
	}
	
	/**
	 * emits warning because 'false' constraint will block rule forever
	 */
	@Override
	public void caseAFalseExpr(AFalseExpr node) {
		TFalse var = node.getFalse();
		messages.add(
			"Warning at ["+ var.getLine() + ":" + var.getPos() + "]:\n" +
			"This constraint will always block the associated rule from being evaluated,\n" +
			"because 'false' cannot be deduced from a consistent constraint store.\n"
		);
	}

	/**
	 * does not effect any context conditions
	 */
	@Override
	public void caseATrueExpr(ATrueExpr node) {
		//empty body
	}
}
