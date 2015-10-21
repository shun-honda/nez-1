package nez.lang;

import nez.ast.Tree;

public abstract class Macro {
	Tree<?> macroNode;

	public Macro(Tree<?> node) {
		this.macroNode = node;
	}

	public static final Macro newDesugerFunction(Tree<?> node) {
		return new DesugarFunction(node);
	}

	public static final Macro newTransFunction(Tree<?> node) {
		return new TransFunction(node);
	}

	public static final Macro newTransVariable(Tree<?> node) {
		return new TransVariable(node);
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
