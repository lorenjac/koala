package checker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import parser.analysis.DepthFirstAdapter;
import parser.node.AHead;
import parser.node.AProg;
import parser.node.ARule;
import parser.node.PRule;

public final class GlobalScopeBuilder extends DepthFirstAdapter 
{
	private Set<String> globalSymbolTable;
	
	
	public GlobalScopeBuilder(Set<String> globalSymbolTable) {
		this.globalSymbolTable = globalSymbolTable;
	}
	
	@Override
	public void caseAProg(AProg node) {
		List<PRule> copy = new ArrayList<PRule>(node.getRules());
        for(PRule e : copy) {
            e.apply(this);
        }
	}
	
	@Override
	public void caseARule(ARule node) {
		node.getHead().apply(this);
	}
	
	@Override
	public void caseAHead(AHead node) {
		String id = node.getName().getText() + "/" + node.getParams().size();
		if(!globalSymbolTable.contains(id)) {
			globalSymbolTable.add(id);
		}
	}
}
