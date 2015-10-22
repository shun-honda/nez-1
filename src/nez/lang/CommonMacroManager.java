package nez.lang;

import java.util.HashMap;

import nez.ast.CommonTree;
import nez.ast.Tree;

public class CommonMacroManager extends MacroManager {
	private Tree<?> parseNode;
	HashMap<String, DesugarFunction> desugarFunctionMap = new HashMap<String, DesugarFunction>();
	HashMap<String, TransFunction> transFunctionMap = new HashMap<String, TransFunction>();
	MacroScope scope;

	public CommonMacroManager() {
		super();
		this.scope = new MacroScope();
	}

	public void setParseNode(Tree<?> node) {
		this.parseNode = node;
	}

	public void pushScope() {
		this.scope = new MacroScope(this.scope);
	}

	public void popScope() {
		this.scope = this.scope.parent;
	}

	public Tree<?> desugar(Tree<?> node) {
		for (int i = 0; i < node.size(); i++) {
			CommonTree child = (CommonTree) node.get(i);
			String name = child.getTag().getSymbol();
			if (this.macroMap.containsKey(name)) {
				this.parseNode = child;
				child = (CommonTree) this.macroMap.get(name).desugar(child);
				((CommonTree) node).set(i, child);
			}
		}
		return node;
	}

	public Object visit(CommonTree node) {
		return find(node).accept(node);
	}

	public class Undefined implements CommonMacroVisitor {
		@Override
		public Object accept(CommonTree node) {
			throw new RuntimeException("[TODO] Undefined visitor: " + node);
		}
	}

	public class DesugarFuncDecl extends Undefined {
		@Override
		public Object accept(CommonTree node) {
			// String name = node.getText(_name, null);
			// DesugarFunction func = (DesugarFunction) macroMap.get(name);
			pushScope();
			CommonTree params = node.get(_param);
			for (int i = 0; i < params.size(); i++) {
				CommonTree param = params.get(i);
				if (param.is(_ListArg)) {
					visit(param);
				} else {
					scope.setVariable(param.getText(i, null), new TransVariable(parseNode));
				}
			}
			Tree<?> retNode = (Tree<?>) visit(node.get(_body));
			popScope();
			return retNode;
		}
	}

	public class Block extends Undefined {
		@Override
		public Object accept(CommonTree node) {
			CommonTree retNode = null;
			for (CommonTree child : node) {
				retNode = (CommonTree) visit(child);
			}
			return retNode;
		}
	}

}

class MacroScope {
	HashMap<String, TransVariable> transVariableMap;
	MacroScope parent;

	public MacroScope() {
		this.transVariableMap = new HashMap<String, TransVariable>();
	}

	public MacroScope(MacroScope parent) {
		this();
		this.parent = parent;
	}

	public void setVariable(String name, TransVariable var) {
		this.transVariableMap.put(name, var);
	}

	public TransVariable getVariable(String name) {
		return this.transVariableMap.get(name);
	}
}
