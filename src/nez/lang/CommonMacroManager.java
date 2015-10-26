package nez.lang;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nez.ast.CommonTree;
import nez.ast.Symbol;
import nez.ast.Tree;

public class CommonMacroManager extends MacroManager {
	private Tree<?> parseNode;
	HashMap<String, DesugarFunction> desugarFunctionMap = new HashMap<String, DesugarFunction>();
	HashMap<String, TransFunction> transFunctionMap = new HashMap<String, TransFunction>();
	List<TransFunction> transFunctionList = new ArrayList<TransFunction>();
	MacroScope scope;

	public CommonMacroManager() {
		super();
		this.scope = new MacroScope();
		init(new Undefined());
	}

	@Override
	public void addMacro(String name, Macro macro) {
		if (macro instanceof DesugarFunction) {
			this.desugarFunctionMap.put(name, (DesugarFunction) macro);
		} else if (macro instanceof TransFunction) {
			this.transFunctionList.add((TransFunction) macro);
			// this.transFunctionMap.put(name, (TransFunction) macro);
		} else if (macro instanceof TransVariable) {
			TransVariable var = (TransVariable) macro;
			var.macroNode = visit((CommonTree) var.macroNode);
			scope.setVariable(name, var);
		} else {
			System.out.println("[TODO] Undefined Macro: " + macro);
			this.macroMap.put(name, macro);
		}
	}

	public DesugarFunction getDesugarFunction(String name) {
		return this.desugarFunctionMap.get(name);
	}

	public TransFunction getTransFunction(String name, Tree<?> params) {
		for (TransFunction func : this.transFunctionList) {
			if (func.is(name, params)) {
				return func;
			}
		}
		return null;
	}

	public TransVariable getTransVariable(String name) {
		return this.scope.getVariable(name);
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
			desugar(child);
			if (this.desugarFunctionMap.containsKey(name)) {
				this.parseNode = child;
				Macro macro = this.desugarFunctionMap.get(name);
				child = visit((CommonTree) macro.macroNode);
				((CommonTree) node).set(i, child);
			}
		}
		return node;
	}

	public CommonTree visit(CommonTree node) {
		return (CommonTree) find(node).accept(node);
	}

	public class Undefined implements CommonMacroVisitor {
		@Override
		public Object accept(CommonTree node) {
			throw new RuntimeException("[TODO] Undefined visitor:\n" + node);
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
					scope.setVariable(param.toText(), new TransVariable(parseNode));
				}
			}
			Tree<?> retNode = visit(node.get(_body));
			popScope();
			return retNode;
		}
	}

	public class TransVarDecl extends Undefined {
		@Override
		public Object accept(CommonTree node) {
			CommonTree valueNode = visit(node.get(_expr));
			scope.setVariable(node.getText(_name, null), new TransVariable(valueNode));
			return valueNode;
		}
	}

	public class Block extends Undefined {
		@Override
		public Object accept(CommonTree node) {
			CommonTree retNode = null;
			for (CommonTree child : node) {
				retNode = visit(child);
			}
			return retNode;
		}
	}

	public class NodeLiteral extends Undefined {
		@Override
		public Object accept(CommonTree node) {
			CommonTree valueNode = node.get(_val);
			CommonTree desugarNode = new CommonTree(Symbol.tag(node.getText(_name, null)), null, 0, 0, valueNode.size(), null);
			if (valueNode.is(_String)) {
				desugarNode.setValue(valueNode.toText());
			} else {
				for (int i = 0; i < valueNode.size(); i++) {
					setNode(valueNode.get(i), desugarNode, i);
				}
			}
			return desugarNode;
		}

		private void setNode(CommonTree macroNode, CommonTree desugarNode, int index) {
			CommonTree dNode = visit(macroNode.get(_expr));
			desugarNode.set(index, Symbol.tag(macroNode.get(_label).getText(_name, null)), dNode);
		}
	}

	public class Field extends Undefined {
		@Override
		public Object accept(CommonTree node) {
			CommonTree field = visit(node.get(_recv));
			return field.get(Symbol.tag(node.getText(_name, null)));
		}
	}

	public class Name extends Undefined {
		@Override
		public Object accept(CommonTree node) {
			return getTransVariable(node.toText()).macroNode;
		}
	}

	public class Apply extends Undefined {
		CommonTree value = null;

		@Override
		public Object accept(CommonTree node) {
			TransFunction func = getTransFunction(node.getText(_name, null), evalParams(node.get(_param)));
			CommonTree retNode = null;
			pushScope();
			CommonTree params = node.get(_param);
			for (int i = 0; i < func.params.size(); i++) {
				value = params.get(i);
				CommonTree param = (CommonTree) func.getParam(i).var;
				if (param.is(_ListArg)) {
					scope.setVariable(param.getText(_first, null), new TransVariable(removeNode(value, i)));
					scope.setVariable(param.getText(_list, null), new TransVariable(value));
				} else if (param.is(_EmptyList)) {
					continue;
				} else {
					scope.setVariable(param.toText(), new TransVariable(value));
				}
			}
			retNode = visit((CommonTree) func.macroNode.get(_body));
			popScope();
			return retNode;
		}

		public CommonTree evalParams(CommonTree params) {
			for (int i = 0; i < params.size(); i++) {
				params.set(i, visit(params.get(i)));
			}
			return params;
		}

		public CommonTree removeNode(CommonTree node, int index) {
			CommonTree ret = node.get(index);
			CommonTree oldValue = value;
			value = new CommonTree(value.getTag(), value.getSource(), value.getSourcePosition(), value.getLength(), value.size() - 1, value.getValue());
			int newIndex = 0;
			for (int i = 0; i < oldValue.size(); i++) {
				if (i != index) {
					value.set(newIndex, oldValue.get(i));
					newIndex++;
				}
			}
			return ret;
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
		MacroScope scope = this;
		while (scope != null) {
			TransVariable var = scope.transVariableMap.get(name);
			if (var == null) {
				scope = scope.parent;
			} else {
				return var;
			}
		}
		return null;
	}
}
