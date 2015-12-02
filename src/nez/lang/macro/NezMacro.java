package nez.lang.macro;

import java.util.ArrayList;
import java.util.List;

import nez.ast.Tree;

public class NezMacro {
	String name;
	Tree<?> node;

	public NezMacro(String name, Tree<?> node) {
		this.name = name;
		this.node = node;
	}

	public String getName() {
		return this.name;
	}
}

class DesugarFunction extends NezMacro {
	List<NezMacro> params;

	public DesugarFunction(String name, Tree<?> node) {
		super(name, node);
		params = new ArrayList<>();
	}

	public void addParam(NezMacro macro) {
		this.params.add(macro);
	}

	public void setParam(int index, NezMacro macro) {
		this.params.add(index, macro);
	}

}

class TransFunction extends NezMacro {

	public TransFunction(String name, Tree<?> node) {
		super(name, node);
	}

}

class TransVariable extends NezMacro {
	Tree<?> desugarNode;

	public TransVariable(String name, Tree<?> node) {
		super(name, node);
	}

	public void setDesugarNode(Tree<?> node) {
		this.desugarNode = node;
	}

}

class NodeLiteral extends NezMacro {

	public NodeLiteral(String name, Tree<?> node) {
		super(name, node);
	}

}

class Block extends NezMacro {

	public Block(String name, Tree<?> node) {
		super(name, node);
	}

}

class ListParam extends NezMacro {

	public ListParam(String name, Tree<?> node) {
		super(name, node);
	}

}

class EmptyList extends NezMacro {

	public EmptyList(String name, Tree<?> node) {
		super(name, node);
	}

}