package nez.lang.macro;

import java.util.ArrayList;
import java.util.List;

import nez.ast.Symbol;
import nez.ast.Tree;

public abstract class NezMacro {
	Tree<?> macroNode;

	public NezMacro(Tree<?> node) {
		this.macroNode = node;
	}

	public static final NezMacro newDesugerFunction(Tree<?> node) {
		return new DesugarFunction(node);
	}

	public static final NezMacro newTransFunction(Tree<?> node) {
		return new TransFunction(node);
	}

	public static final NezMacro newTransVariable(Tree<?> node) {
		return new TransVariable(node);
	}

	public abstract Tree<?> desugar(MacroManager manager);

	@Override
	public String toString() {
		return this.macroNode.toString();
	}
}

class DesugarFunction extends NezMacro {

	public DesugarFunction(Tree<?> node) {
		super(node);
	}

	@Override
	public Tree<?> desugar(MacroManager manager) {
		// TODO Auto-generated method stub
		return null;
	}

}

class TransFunction extends NezMacro {
	String name = null;
	List<Argument> params;
	// Tree<?> params;

	public TransFunction(Tree<?> node) {
		super(node);
		this.name = this.macroNode.getText(Symbol.tag("name"), null);
		this.params = new ArrayList<Argument>();
		Tree<?> params = node.get(Symbol.tag("param"));
		for (Tree<?> param : params) {
			if (param.is(Symbol.tag("ListArg"))) {
				this.setListArgParam(param);
			} else if (param.is(Symbol.tag("EmptyList"))) {
				this.setEmptyListParam(param);
			} else {
				this.setNodeParam(param);
			}
		}
	}

	@Override
	public Tree<?> desugar(MacroManager manager) {
		// TODO Auto-generated method stub
		return null;
	}

	public void setNodeParam(Tree<?> var) {
		this.params.add(new Argument(Type.Node, var));
	}

	public void setListArgParam(Tree<?> var) {
		this.params.add(new Argument(Type.ListArg, var));
	}

	public void setEmptyListParam(Tree<?> var) {
		this.params.add(new Argument(Type.EmptyList, var));
	}

	public Argument getParam(int index) {
		return this.params.get(index);
	}

	public String getName() {
		return this.name;
	}

	public boolean is(String name, Tree<?> params) {
		if (this.name.equals(name)) {
			if (params.size() == this.params.size()) {
				for (int i = 0; i < params.size(); i++) {
					Tree<?> param = params.get(i);
					Argument arg = this.params.get(i);
					if (param.is(Symbol.tag("ListArg")) && arg.type == Type.ListArg) {
						continue;
					}
					if (param.size() == 0 && arg.type == Type.EmptyList) {
						continue;
					}
					if (arg.type == Type.Node) {
						continue;
					}
					return false;
				}
			}
		}
		return true;
	}

}

class TransVariable extends NezMacro {

	public TransVariable(Tree<?> node) {
		super(node);
	}

	@Override
	public Tree<?> desugar(MacroManager manager) {
		// TODO Auto-generated method stub
		return null;
	}

}

class Argument {
	int type;
	Tree<?> var;

	public Argument(int type, Tree<?> var) {
		this.type = type;
		this.var = var;
	}
}

class Type {
	static int Node = 0;
	static int ListArg = 1;
	static int EmptyList = 2;
}
