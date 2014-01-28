package interpreter.eval;

import interpreter.data.Environment;
import interpreter.data.ListVariable;
import interpreter.data.Literal;
import interpreter.data.Value;

import java.util.LinkedList;
import java.util.List;

import parser.analysis.DepthFirstAdapter;
import parser.node.AListConstructorExpr;
import parser.node.AListExpr;
import parser.node.ANumberExpr;
import parser.node.APredCallExpr;
import parser.node.ARule;
import parser.node.AVarExpr;
import parser.node.AWildcardExpr;
import parser.node.PExpr;
import choco.kernel.model.variables.integer.IntegerConstantVariable;

public final class BodyEvaluator extends DepthFirstAdapter {

	private Environment environment;
	private List<Literal> goal;
	private int evaluatedLiteralIndex;
	private List<Literal> literals;
	private List<Value> args;
	
	public BodyEvaluator(Environment environment, 
						 List<Literal> goal,
						 int evaluatedLiteralIndex) {
		this.environment = environment;
		this.goal = goal;
		this.evaluatedLiteralIndex = evaluatedLiteralIndex;
		this.literals = new LinkedList<>();
	}

	@Override
	public void caseARule(ARule node) {
		for(PExpr e : node.getBody()) {
			e.apply(this);
		}
		
		if(evaluatedLiteralIndex != -1) {
			goal.remove(evaluatedLiteralIndex);
			if(!literals.isEmpty()) {
				goal.addAll(evaluatedLiteralIndex, literals);
			}
		} else {
			goal.addAll(literals);
		}
	}

	@Override
	public void caseAPredCallExpr(APredCallExpr node) {
		args = new LinkedList<>();
		String predName = node.getName().getText();
		for(PExpr e : node.getArgs()) {
			e.apply(this);
		}
		literals.add(new Literal(predName, args));
	}
	
	@Override
	public void caseAVarExpr(AVarExpr node) {
		String varName = node.getName().getText();
		args.add(environment.get(varName));
	}

	@Override
	public void caseANumberExpr(ANumberExpr node) {
		String intString = node.getValue().getText();
		int intValue = Integer.parseInt(intString);
		Value value = new Value("");
		value.init(new IntegerConstantVariable(intValue));
		args.add(value);
	}

	@Override
	public void caseAListExpr(AListExpr node) {
		ListVariable list = new ListVariable();
		createList(list, node);
		Value value = new Value("");
		value.init(list);
		args.add(value);
	}
	
	@Override
	public void caseAListConstructorExpr(AListConstructorExpr node) {
		ListVariable list = new ListVariable();
		createList(list, node);
		Value value = new Value("");
		value.init(list);
		args.add(value);
	}
	
	private void createList(ListVariable list, AListExpr node) {
		for(PExpr elem : node.getElements()) {
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
				createList(listVar, (AListExpr) elem);
				Value head = new Value("");
				head.init(listVar);
				list.setHead(head);
			} else if(elem instanceof AListConstructorExpr) {
				ListVariable listVar = new ListVariable();
				createList(listVar, (AListConstructorExpr) elem);
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

	private void createList(ListVariable list, AListConstructorExpr node) {
		ListVariable previousListNode = null;
		for(PExpr elem : node.getHead()) {
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
				createList(listVar, (AListExpr) elem);
				Value head = new Value("");
				head.init(listVar);
				list.setHead(head);
			} else if(elem instanceof AListConstructorExpr) {
				ListVariable listVar = new ListVariable();
				createList(listVar, (AListConstructorExpr) elem);
				Value head = new Value("");
				head.init(listVar);
				list.setHead(head);
			} else if(elem instanceof AWildcardExpr) {
				list.setHead(new Value("_"));
			}
			previousListNode = list;
			list = list.getTail().getListVar();
		}
		
		PExpr tail = node.getTail();
		if(tail instanceof AVarExpr) {
			String varName = ((AVarExpr) tail).getName().getText();
			Value value = environment.get(varName);
			value.init(new ListVariable());
			previousListNode.setTail(value);
		} else if(tail instanceof AListExpr) {
			createList(list, (AListExpr)tail);
		} else if(tail instanceof AListConstructorExpr) {
			createList(list, (AListConstructorExpr)tail);
		} else if(tail instanceof AWildcardExpr) {
			Value value = new Value("_");
			value.init(new ListVariable());
			previousListNode.setTail(value);
		}
	}
}
