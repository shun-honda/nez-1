package nez.lang.macro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nez.ast.Symbol;
import nez.ast.Tree;

public class MacroManager extends AbstractMacroManager {
	private Tree<?> parseNode;
	HashMap<String, DesugarFunction> desugarFunctionMap = new HashMap<String, DesugarFunction>();
	HashMap<String, TransFunction> transFunctionMap = new HashMap<String, TransFunction>();
	List<TransFunction> transFunctionList = new ArrayList<TransFunction>();
	MacroScope scope;

	public MacroManager() {
		super();
		this.scope = new MacroScope();
		init(MacroManager.class, new Undefined());
	}

	@Override
	public void addMacro(String name, NezMacro macro) {
		if (macro instanceof DesugarFunction) {
			this.desugarFunctionMap.put(name, (DesugarFunction) macro);
		} else if (macro instanceof TransFunction) {
			this.transFunctionList.add((TransFunction) macro);
			// this.transFunctionMap.put(name, (TransFunction) macro);
		} else if (macro instanceof TransVariable) {
			TransVariable var = (TransVariable) macro;
			var.macroNode = visit(var.macroNode);
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
			Tree<?> child = node.get(i);
			String name = child.getTag().getSymbol();
			desugar(child);
			if (this.desugarFunctionMap.containsKey(name)) {
				this.parseNode = child;
				NezMacro macro = this.desugarFunctionMap.get(name);
				child = visit(macro.macroNode);
				node.link(i, node.getLabel(i), child);
			}
		}
		return node;
	}

	// return generated node
	public Tree<?> visit(Tree<?> node) {
		return (Tree<?>) find(node.getTag().toString()).accept(node);
	}

	public class Undefined implements CommonMacroVisitor {
		@Override
		public Object accept(Tree<?> node) {
			throw new RuntimeException("[TODO] Undefined visitor:\n" + node);
		}
	}

	public class DesugarFuncDecl extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			pushScope();
			Tree<?> params = node.get(_param);
			for (int i = 0; i < params.size(); i++) {
				Tree<?> param = params.get(i);
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
		public Object accept(Tree<?> node) {
			Tree<?> valueNode = visit(node.get(_expr));
			scope.setVariable(node.getText(_name, null), new TransVariable(valueNode));
			return valueNode;
		}
	}

	public class Block extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			Tree<?> retNode = null;
			for (Tree<?> child : node) {
				retNode = visit(child);
			}
			return retNode;
		}
	}

	public class NodeLiteral extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			Tree<?> valueNode = node.get(_val); // String | List<Node>
			Tree<?> newNode = parseNode.newInstance(Symbol.tag(node.getText(_name, null)), parseNode.getSource(), 0, 0, valueNode.size(), null);
			if (valueNode.is(_String)) {
				newNode.setValue(valueNode.toText());
			} else { // case of List<Node>
				for (int i = 0; i < valueNode.size(); i++) {
					setNode(valueNode.get(i), newNode, i);
				}
			}
			return newNode;
		}

		private void setNode(Tree<?> elementNode, Tree<?> newNode, int index) {
			Tree<?> valueNode = visit(elementNode.get(_expr));
			if (elementNode.get(_label, null) == null) {
				newNode.link(index, null, valueNode);
				return;
			}
			newNode.link(index, Symbol.tag(elementNode.get(_label, null).getText(_name, null)), valueNode);
		}
	}

	public class Field extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			Tree<?> field = visit(node.get(_recv, null));
			return field.get(Symbol.tag(node.getText(_name, null)));
		}
	}

	public class Name extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			return getTransVariable(node.toText()).macroNode;
		}
	}

	public class Apply extends Undefined {
		Tree<?> value = null;

		@Override
		public Object accept(Tree<?> node) {
			Tree<?> args = evalParams(node.get(_param));
			TransFunction func = getTransFunction(node.getText(_name, null), args);
			Tree<?> retNode = null;
			pushScope();
			for (int i = 0; i < func.params.size(); i++) {
				value = args.get(i);
				Tree<?> param = func.getParam(i).var;
				if (param.is(_ListArg)) {
					scope.setVariable(param.getText(_first, null), new TransVariable(removeNode(i)));
					scope.setVariable(param.getText(_list, null), new TransVariable(value));
				} else if (param.is(_EmptyList)) {
					continue;
				} else {
					scope.setVariable(param.toText(), new TransVariable(value));
				}
			}
			retNode = visit(func.macroNode.get(_body));
			popScope();
			return retNode;
		}

		public Tree<?> evalParams(Tree<?> params) {
			Tree<?> args = parseNode.newInstance(params.getTag(), null, 0, 0, params.size(), null);
			for (int i = 0; i < params.size(); i++) {
				args.link(i, params.getLabel(i), visit(params.get(i)));
			}
			return args;
		}

		public Tree<?> removeNode(int index) {
			Tree<?> ret = value.get(index);
			Tree<?> oldValue = value;
			value = parseNode.newInstance(value.getTag(), value.getSource(), value.getSourcePosition(), value.getLength(), value.size() - 1, value.getValue());
			int newIndex = 0;
			for (int i = 0; i < oldValue.size(); i++) {
				if (i != index) {
					value.link(newIndex, value.getLabel(newIndex), oldValue.get(i));
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
