package nez.lang.macro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nez.ast.Symbol;
import nez.ast.Tree;

public class MacroInterpreter {
	MacroBuilder pool;
	Scope scope;

	public MacroInterpreter(MacroBuilder builder) {
		this.pool = builder;
		this.scope = new Scope();
	}

	public Tree<?> desugar(Tree<?> cur, Tree<?> parent, int index) {
		String name = cur.getTag().getSymbol();
		if (this.pool.desugarFunctionMap.containsKey(name)) {
			FunctionSet set = this.pool.desugarFunctionMap.get(name);
			for (NezMacro macro : set.set) {
				DesugarFunction func = (DesugarFunction) macro;
				if (func.params.size() == cur.size()) {
					boolean desugaring = true;
					for (Name param : func.params) {
						if (cur.get(Symbol.tag(param.name), null) == null) {
							desugaring = false;
							break;
						}
					}
					if (desugaring) {
						Tree<?> newNode = func.desugar(this, cur);
						if (parent != null) {
							parent.link(index, parent.getLabel(index), newNode);
						}
						return newNode;
					}
				}
			}
		}
		for (int i = 0; i < cur.size(); i++) {
			this.desugar(cur.get(i), cur, i);
		}
		return cur;
	}

	public Tree<?> desugar(DesugarFunction macro, Tree<?> node) {
		this.scope = new Scope(this.scope);
		for (Name param : macro.params) {
			this.scope.setVariable(param.name, node.get(Symbol.tag(param.name)));
		}
		return macro.child.desugar(this, node);
	}

	public Tree<?> desugar(TransFunction macro, Tree<?> node) {
		return macro.child.desugar(this, node);
	}

	public Tree<?> desugar(TransVariable macro, Tree<?> node) {
		return node;
	}

	public Tree<?> desugar(NodeLiteral macro, Tree<?> node) {
		Tree<?> newNode = node.newInstance(Symbol.tag(macro.name), node.getSource(), node.getSourcePosition(), node.getLength(), macro.list.size(), null);
		NezMacro val = macro.list.get(0);
		if (val instanceof StringLiteral) {
			return val.desugar(this, newNode);
		}
		for (int i = 0; i < macro.list.size(); i++) {
			newNode.link(i, macro.labels.get(i), macro.list.get(i).desugar(this, node));
		}
		return newNode;
	}

	public Tree<?> desugar(NodeElement macro, Tree<?> node) {
		return macro.child.desugar(this, node);
	}

	public Tree<?> desugar(Field macro, Tree<?> node) {
		Tree<?> field = macro.child.desugar(this, node);
		return field.get(macro.label, null);
	}

	public Tree<?> desugar(Indexer macro, Tree<?> node) {
		Tree<?> indexer = macro.child.desugar(this, node);
		return indexer.get(macro.index, null);
	}

	public Tree<?> desugar(Name macro, Tree<?> node) {
		return this.scope.getVariable(macro.name);
	}

	public Tree<?> desugar(StringLiteral macro, Tree<?> node) {
		return node;
	}

	public Tree<?> desugar(Block macro, Tree<?> node) {
		Tree<?> ret = null;
		for (NezMacro element : macro.list) {
			ret = element.desugar(this, node);
		}
		return ret;
	}

	public Tree<?> desugar(ListParam macro, Tree<?> node) {
		return node;
	}

	public Tree<?> desugar(EmptyList macro, Tree<?> node) {
		return node;
	}

	Tree<?> argNode = null;

	public Tree<?> desugar(Apply macro, Tree<?> node) {
		FunctionSet set = this.pool.transFunctionMap.get(macro.name);
		List<Tree<?>> args = new ArrayList<>();
		for (NezMacro element : set.set) {
			TransFunction func = (TransFunction) element;
			if (func.params.size() == macro.args.size()) {
				int i = 0;
				for (; i < macro.args.size(); i++) {
					argNode = macro.args.get(i).desugar(this, node);
					args.add(argNode);
					NezMacro param = func.params.get(i);
					if (param instanceof ListParam) {
						if (argNode.size() > 0) {
							continue;
						}
						break;
					} else if (param instanceof EmptyList) {
						if (argNode.size() == 0) {
							continue;
						}
						break;
					} else if (param instanceof Name) {
						continue;
					}
				}
				if (i < macro.args.size()) {
					args.clear();
					continue;
				}
				this.scope = new Scope(this.scope);
				for (i = 0; i < macro.args.size(); i++) {
					argNode = args.get(i);
					NezMacro param = func.params.get(i);
					if (param instanceof ListParam) {
						if (argNode.size() > 0) {
							this.scope.setVariable(((ListParam) param).first.name, removeArgNode(0));
							this.scope.setVariable(((ListParam) param).listNode.name, argNode);
						}
					} else if (param instanceof EmptyList) {
						if (argNode.size() == 0) {
							continue;
						}
					} else if (param instanceof Name) {
						this.scope.setVariable(((Name) param).name, argNode);
					}
				}
				node = func.desugar(this, node);
				this.scope = this.scope.prev;
				return node;
			}
		}
		return node;
	}

	public Tree<?> removeArgNode(int index) {
		Tree<?> ret = argNode.get(index);
		Tree<?> oldValue = argNode;
		argNode = argNode.newInstance(argNode.getTag(), argNode.getSource(), argNode.getSourcePosition(), argNode.getLength(), argNode.size() - 1, argNode.getValue());
		int newIndex = 0;
		for (int i = 0; i < oldValue.size(); i++) {
			if (i != index) {
				argNode.link(newIndex, argNode.getLabel(newIndex), oldValue.get(i));
				newIndex++;
			}
		}
		return ret;
	}

}

class Scope {
	HashMap<String, Tree<?>> varMap = new HashMap<>();
	Scope prev;

	public Scope() {
		this.prev = null;
	}

	public Scope(Scope prev) {
		this.prev = prev;
	}

	public void setVariable(String name, Tree<?> node) {
		this.varMap.put(name, node);
	}

	public Tree<?> getVariable(String name) {
		return this.varMap.get(name);
	}

}
