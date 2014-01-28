package checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parser.analysis.DepthFirstAdapter;
import parser.node.AListConstructorExpr;
import parser.node.AListExpr;
import parser.node.ANumberExpr;
import parser.node.APredCallExpr;
import parser.node.ARule;
import parser.node.AVarExpr;
import parser.node.AWildcardExpr;
import parser.node.PExpr;
import parser.node.TIdent;
import parser.node.TNumber;
import util.PrettyPrinter;
import checker.ContextChecker.Type;

/*
 * [BodyExprCtxCheck]
 * # a rule must not contain calls to undefined predicates in their body
 * # a predicate call in the body of a rule may not contain full expressions (only variables or constants (immediate numbers or lists))
 */

public final class BodyContextChecker extends DepthFirstAdapter {

	/**
	 * stores local variable names and their types
	 */
	private Map<String, Type> 	localSymbolTable;
	
	/**
	 * stores global predicate names
	 */
	private Set<String> 		globalSymbolTable;
	
	/**
	 * stores all error/warning messages
	 */
	private List<String> 		messages;
	
	/**
	 * constructs a dfs tree walker that checks semantic properties of the rule body;
	 * it receives a reference to a symbol table of local variables and their types
	 * and another reference to a list of error/warning messages
	 * to store additional messages if need be
	 * 
	 * @param localSymbolTable
	 * 							mutable symbol table of all local variables and their types
	 * @param messages
	 * 							mutable list of error/warning messages
	 */
	public BodyContextChecker(Map<String, Type> localSymbolTable,
							Set<String> globalSymbolTable, 
							List<String> messages) {
		this.localSymbolTable = localSymbolTable;
		this.globalSymbolTable = globalSymbolTable;
		this.messages = messages;
	}

	@Override
	public void caseARule(ARule node) {
		List<PExpr> copy = new ArrayList<PExpr>(node.getBody());
        for(PExpr e : copy) {
            e.apply(this);
        }
	}
	
	@Override
    public void caseAPredCallExpr(APredCallExpr node) {
		TIdent predName = node.getName();
		int arity = node.getArgs().size();
		if(!globalSymbolTable.contains(predName.getText() + "/" + arity)) {
			messages.add(
    			"Semantic error at ["+ predName.getLine() + ":" + predName.getPos() + "]:\n" +
				"Predicate '" + predName.getText() + "' is undefined for arity " + arity + ".\n"
    		);
		}
		
		List<PExpr> copy = new ArrayList<PExpr>(node.getArgs());
        for(PExpr e : copy) {
        	if(e instanceof ANumberExpr) {
        		e.apply(this);
        		
        	} else if(e instanceof AVarExpr) {
        		e.apply(this);
        		
        	} else if(e instanceof AListExpr) {
        		e.apply(this);
        		
        	} else if(e instanceof AListConstructorExpr) {
        		e.apply(this);
        		
        	} else {
        		PrettyPrinter p = new PrettyPrinter();
        		e.apply(p);
        		messages.add(
        			"Semantic error at ["+ predName.getLine() + ":" + predName.getPos() + "]:\n" +
        			"Expression '" + p.getString() + "' is not a constant.\n" +
    				"A predicate call may only contain constant expressions like numbers or variables.\n"
        		);
        	}
        }
    }
	
	@Override
	public void caseAVarExpr(AVarExpr node) {
		String var = node.getName().getText();
		if(!localSymbolTable.containsKey(var)) {
			localSymbolTable.put(var, Type.UNKNOWN);
		}
	}
	
	@Override
	public void caseANumberExpr(ANumberExpr node) {
		//empty so far
	}
	
	@Override
	public void caseAListExpr(AListExpr node) {
		for(PExpr elem : node.getElements()) {
			if(elem instanceof ANumberExpr) {
				//ok
			} else if(elem instanceof AVarExpr) {
				elem.apply(this);
			} else if(elem instanceof AListExpr) {
				elem.apply(this);
			} else if(elem instanceof AListConstructorExpr) {
				elem.apply(this);
			} else if(elem instanceof AWildcardExpr) {
				//ok
			}
		}
	}
	
	@Override
	public void caseAListConstructorExpr(AListConstructorExpr node) {
		for(PExpr elem : node.getHead()) {
			if(elem instanceof ANumberExpr) {
				
			} else if(elem instanceof AVarExpr) {
				elem.apply(this);
			} else if(elem instanceof AListExpr) {
				elem.apply(this);
			} else if(elem instanceof AListConstructorExpr) {
				elem.apply(this);
			} else if(elem instanceof AWildcardExpr) {
				//ok
			}
		}
		
		PExpr tail = node.getTail();
		if(tail instanceof ANumberExpr) {
			TNumber num = ((ANumberExpr) tail).getValue();
			messages.add("Semantic error at ["+ num.getLine() + ":" + num.getPos() + "]:\n" +
						 "Invalid list tail '" + num.getText() + "'.\n" +
						 "A list tail must be of type '" + Type.LIST + "'.\n" +
						 "Valid list tails are wildcards, variables or immediate lists.\n");
		} else if(tail instanceof AVarExpr) {
			tail.apply(this);
		} else if(tail instanceof AListExpr) {
			tail.apply(this);
		} else if(tail instanceof AListConstructorExpr) {
			tail.apply(this);
		} else if(tail instanceof AWildcardExpr) {
			//ok
		}
	}
}
