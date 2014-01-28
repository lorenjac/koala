package util;

import java.util.ArrayList;
import java.util.List;

import parser.analysis.DepthFirstAdapter;
import parser.node.ADivExpr;
import parser.node.AEqExpr;
import parser.node.AFalseExpr;
import parser.node.AGeqExpr;
import parser.node.AGtExpr;
import parser.node.AHead;
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
import parser.node.APredCallExpr;
import parser.node.AProg;
import parser.node.ARule;
import parser.node.ATrueExpr;
import parser.node.AVarExpr;
import parser.node.AWildcardExpr;
import parser.node.PExpr;
import parser.node.PRule;
import parser.node.TIdent;

public final class PrettyPrinter extends DepthFirstAdapter 
{
	private String string;
	
	public PrettyPrinter() {
		string = "";
	}
	
	public String getString() {
		return string;
	}
	
	public void reset() {
		string = "";
	}

	@Override
	public void caseAProg(AProg node) {
		List<PRule> copy = new ArrayList<PRule>(node.getRules());
        for(int i=0; i<copy.size(); ++i)
        {
        	if(i>0) {
        		string += "\n";
        	}
        	copy.get(i).apply(this);
        }
	}

	@Override
	public void caseARule(ARule node) {
		node.getHead().apply(this);
		string += " : ";
		{
			List<PExpr> copy = new ArrayList<PExpr>(node.getAsk());
			for(int i=0; i<copy.size(); ++i)
	        {
	        	if(i>0) {
	        		string += ", ";
	        	}
	        	copy.get(i).apply(this);
	        }
		}
		string += " : ";
		{
			List<PExpr> copy = new ArrayList<PExpr>(node.getTell());
			for(int i=0; i<copy.size(); ++i)
	        {
	        	if(i>0) {
	        		string += ", ";
	        	}
	        	copy.get(i).apply(this);
	        }
		}
		string += " | ";
		{
			List<PExpr> copy = new ArrayList<PExpr>(node.getBody());
			for(int i=0; i<copy.size(); ++i)
	        {
	        	if(i>0) {
	        		string += ", ";
	        	}
	        	copy.get(i).apply(this);
	        }
		}
		string += ".";
	}
	
	@Override
	public void caseTIdent(TIdent node) {
		string += node.getText();
	}

	@Override
	public void caseAHead(AHead node) {
		string += node.getName();
		List<TIdent> copy = new ArrayList<TIdent>(node.getParams());
		if(!copy.isEmpty()) {
			string += "(";
		}
		for(int i=0; i<copy.size(); ++i)
        {
        	if(i>0) {
        		string += ", ";
        	}
        	copy.get(i).apply(this);
        }
		if(!copy.isEmpty()) {
			string += ")";
		}
	}

	@Override
	public void caseAFalseExpr(AFalseExpr node) {
		string += "false";
	}

	@Override
	public void caseATrueExpr(ATrueExpr node) {
		string += "true";
	}

	@Override
	public void caseAEqExpr(AEqExpr node) {
		node.getVar().apply(this);
		string += " = ";
		node.getExpr().apply(this);
	}

	@Override
	public void caseANeqExpr(ANeqExpr node) {
		node.getVar().apply(this);
		string += " =//= ";
		node.getExpr().apply(this);
	}

	@Override
	public void caseALtExpr(ALtExpr node) {
		node.getVar().apply(this);
		string += " < ";
		node.getExpr().apply(this);
	}

	@Override
	public void caseAGtExpr(AGtExpr node) {
		node.getVar().apply(this);
		string += " > ";
		node.getExpr().apply(this);
	}

	@Override
	public void caseALeqExpr(ALeqExpr node) {
		node.getVar().apply(this);
		string += " <= ";
		node.getExpr().apply(this);
	}

	@Override
	public void caseAGeqExpr(AGeqExpr node) {
		node.getVar().apply(this);
		string += " >= ";
		node.getExpr().apply(this);
	}

	@Override
	public void caseAPlusExpr(APlusExpr node) {
		node.getLeft().apply(this);
		string += " + ";
		node.getRight().apply(this);
	}

	@Override
	public void caseAMinusExpr(AMinusExpr node) {
		node.getLeft().apply(this);
		string += " - ";
		node.getRight().apply(this);
	}

	@Override
	public void caseAMultExpr(AMultExpr node) {
		node.getLeft().apply(this);
		string += " * ";
		node.getRight().apply(this);
	}

	@Override
	public void caseADivExpr(ADivExpr node) {
		node.getLeft().apply(this);
		string += " / ";
		node.getRight().apply(this);
	}

	@Override
	public void caseAModExpr(AModExpr node) {
		node.getLeft().apply(this);
		string += " % ";
		node.getRight().apply(this);
	}

	@Override
	public void caseAListExpr(AListExpr node) {
		string += "[";
		List<PExpr> copy = new ArrayList<PExpr>(node.getElements());
        for(int i=0; i<copy.size(); ++i)
        {
        	if(i > 0) {
        		string += ", ";
        	}
            copy.get(i).apply(this);
        }
		string += "]";
	}

	@Override
	public void caseAListConstructorExpr(AListConstructorExpr node) {
		string += "[";
		{
            List<PExpr> copy = new ArrayList<PExpr>(node.getHead());
            for(int i=0; i<copy.size(); ++i)
            {
            	if(i > 0) {
            		string += ", ";
            	}
                copy.get(i).apply(this);
            }
        }
        if(node.getTail() != null)
        {
        	string += " | ";
            node.getTail().apply(this);
        }
        string += "]";
	}

	@Override
	public void caseAVarExpr(AVarExpr node) {
		string += node.getName().getText();
	}

	@Override
	public void caseANumberExpr(ANumberExpr node) {
		string += node.getValue().getText();
	}

	@Override
	public void caseAWildcardExpr(AWildcardExpr node) {
		string += "_";
	}

	@Override
	public void caseAPredCallExpr(APredCallExpr node) {
		node.getName().apply(this);
        {
            List<PExpr> copy = new ArrayList<PExpr>(node.getArgs());
            if(!copy.isEmpty()) {
            	string += "(";
            }
            for(int i=0; i<copy.size(); ++i)
            {
            	if(i > 0) {
            		string += ", ";
            	}
                copy.get(i).apply(this);
            }
            if(!copy.isEmpty()) {
            	string += ")";
            }
        }
	}
}
