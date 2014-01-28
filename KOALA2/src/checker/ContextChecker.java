package checker;

import interpreter.data.Closure;
import interpreter.data.Environment;
import interpreter.data.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parser.analysis.DepthFirstAdapter;
import parser.node.AHead;
import parser.node.AProg;
import parser.node.ARule;
import parser.node.PRule;
import parser.node.TIdent;

/* rules to check:
 * 
 * [ContextChecker]
 * # all variables inside rule head must be unique
 * # TODO emit warning if a variable remains unused throughout the entire rule
 * 
 * [AskExprCtxCheck]
 * # all variables used in an ask-constraint (lvalue + rvalue) must be defined in parameter list unless invoked inside list expression
 * # constraints must be stated over equal types, int vs. int, list vs. list
 * # no other operation than "=" or "=//=" can be applied to lists
 * # list tail cannot be of type integer
 * # emit warning if ask-constraint is false (will block its rule forever)
 * # emit warning if rvalue is lvalue variable (will always return true for ==, <=, >=; always false for =//=, <, >)
 * 
 * [TellExprCtxCheck]
 * # all variables used in rvalues of tell-constraints must be defined in parameter list, list pattern (ask) or lvalue of previous tell-constraint
 * # constraints must be stated over equal types, int vs. int, list vs. list
 * # no other operation than "=" or "=//=" can be applied to lists
 * # TODO emit warning if ask-lvalue is used as tell-lvalue at the same time (will block rule forever)
 * # emit warning if lvalue occurs in multiple tell-constraints (will block rule forever) 
 * # emit warning if tell-constraint is false (will block rule forever)
 * 
 * [BodyExprCtxCheck]
 * # a rule must not contain calls to undefined predicates in their body
 * # a predicate call in the body of a rule may not contain full expressions (only variables or constants (immediate numbers or lists))
 */

public final class ContextChecker extends DepthFirstAdapter 
{
	protected enum Type {
		UNKNOWN,
		INTEGER,
		LIST
	}
	
	//private Map<String, List<Closure>>
	
	/**
	 * this map stores rule names and their arity 
	 */
	private Set<String> 		globalSymbolTable;
	
	/**
	 * this list stores all variables declared within a single rule and their types if inferred
	 */
	private Map<String, Type> 	localSymbolTable;
	
	/**
	 * this list holds a all messages generated during this context check
	 */
	private List<String> 		messages;
	
	/**
	 * 
	 */
	private Map<String, List<Closure>> program;
	
	
	public ContextChecker(Map<String, List<Closure>> program) {
		globalSymbolTable = new HashSet<>();//for fast adding and lookup of unique names
		localSymbolTable = new HashMap<>(); //for fast adding and lookup of names/types
		messages = new LinkedList<>(); 	    //for fast adding of new messages
		this.program = program;
	}
	
	public List<String> getMessages() {
		return messages;
	}

	@Override
	public void caseAProg(AProg node) {
		//build table of predicate names
		node.apply(new GlobalScopeBuilder(globalSymbolTable));
		
		//prepare future symbol table
		for(String id : globalSymbolTable) {
			program.put(id, new LinkedList<Closure>());
		}
		
		//add existing global names
		for(Map.Entry<String, List<Closure>> e : program.entrySet()) {
			globalSymbolTable.add(e.getKey());
		}
		
		//apply context check to all rules
        for(PRule e : node.getRules()) {
            e.apply(this);
    		localSymbolTable.clear();
        }
	}
	

	@Override
	public void caseARule(ARule node) {
		//apply context check to this rule
		node.getHead().apply(this);
        node.apply(new AskContextChecker(localSymbolTable, messages));
        node.apply(new TellContextChecker(localSymbolTable, messages));
        node.apply(new BodyContextChecker(localSymbolTable, globalSymbolTable, messages));
        
        //create closure for this rule
        AHead head = ((AHead)node.getHead());
        String ruleName = head.getName().getText();
        int arity = head.getParams().size();
        List<Closure> rules = program.get(ruleName + "/" + arity);
        Environment environment = new Environment();
        for(Map.Entry<String, Type> e : localSymbolTable.entrySet()) {
        	environment.put(e.getKey(), new Value(e.getKey()));
        }
        rules.add(new Closure(node, environment));
	}
	
	@Override
	public void caseAHead(AHead node) {
		List<TIdent> copy = new ArrayList<TIdent>(node.getParams());
        for(TIdent e : copy) {
        	if(localSymbolTable.containsKey(e.getText())) {
        		messages.add(
        			"Semantic error at ["+ e.getLine() + ":" + e.getPos() + "]:\n" +
        			"Conflicting declaration of variable < " + e.getText() + " >" + 
        			"in head of rule < " + node.getName().getText() + " >.\n" +
        			"A variable can only be declared once within a rule.\n"
        		);
        	} else {
        		localSymbolTable.put(e.getText(), Type.UNKNOWN);
        	}
        }
	}
}
