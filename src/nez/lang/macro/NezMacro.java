package nez.lang.macro;

import java.util.ArrayList;
import java.util.List;

import nez.ast.Symbol;
import nez.ast.Tree;

public class NezMacro {
	Tree<?> node;
	NezMacro child;

	public NezMacro(Tree<?> node, NezMacro macro) {
		this.node = node;
	}
}

class DesugarFunction extends NezMacro {
	String name;
	List<NezMacro> params;

	public DesugarFunction(String name, Tree<?> node, NezMacro macro) {
		super(node, macro);
		this.name = name;
		params = new ArrayList<>();
	}

	public void addParam(NezMacro macro) {
		this.params.add(macro);
	}

	public void setParam(int index, NezMacro macro) {
		this.params.add(index, macro);
	}

	public String getName() {
		return this.name;
	}

}

class TransFunction extends NezMacro {
	String name;

	public TransFunction(String name, Tree<?> node, NezMacro macro) {
		super(node, macro);
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

}

class TransVariable extends NezMacro {
	String name;
	Tree<?> desugarNode;

	public TransVariable(String name, Tree<?> node) {
		super(node, null);
		this.name = name;
	}

	public void setDesugarNode(Tree<?> node) {
		this.desugarNode = node;
	}

	public String getName() {
		return this.name;
	}

}

class NodeLiteral extends NezMacro {
	Tree<?> desugarNode;

	public NodeLiteral(Tree<?> node) {
		super(node, null);
	}

	public void createDesugarTree(Tree<?> node, Class<?> clazz) throws InstantiationException, IllegalAccessException {
		desugarNode = (Tree<?>) clazz.newInstance();
	}

}

class NodeElement extends NezMacro {
	Symbol desugarLabel;
	Symbol nodeLabel;
	int index;

	public NodeElement(Tree<?> node, Symbol label) {
		super(node, null);
		this.desugarLabel = label;
	}

	public NodeElement(Tree<?> node, Symbol label, Symbol nodeLabel) {
		this(node, label);
		this.nodeLabel = nodeLabel;
	}

	public NodeElement(Tree<?> node, Symbol label, int index) {
		this(node, label);
		this.index = index;
	}

}

class Block extends NezMacro {
	List<NezMacro> list;

	public Block(Tree<?> node) {
		super(node, null);
		this.list = new ArrayList<>();
	}

	public void add(NezMacro macro) {
		this.list.add(macro);
	}

	public void set(int index, NezMacro macro) {
		this.list.add(index, macro);
	}

}

class ListParam extends NezMacro {

	public ListParam(Tree<?> node, NezMacro macro) {
		super(node, macro);
	}

}

class EmptyList extends NezMacro {

	public EmptyList(Tree<?> node, NezMacro macro) {
		super(node, macro);
	}

}