package nez.lang;

import nez.ast.Tree;

public abstract class Macro {
	Tree<?> macroNode;

	public Macro(Tree<?> node) {
		this.macroNode = node;
	}

	@Override
	public String toString() {
		return this.macroNode.toString();
	}
}

class DesugarFunction extends Macro {

	public DesugarFunction(Tree<?> node) {
		super(node);
	}

}

class TransFunction extends Macro {

	public TransFunction(Tree<?> node) {
		super(node);
	}

}

class TransVariable extends Macro {

	public TransVariable(Tree<?> node) {
		super(node);
	}

}
