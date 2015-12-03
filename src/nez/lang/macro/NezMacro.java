package nez.lang.macro;

import java.util.ArrayList;
import java.util.List;

import nez.ast.Symbol;
import nez.ast.Tree;

public abstract class NezMacro {
	Tree<?> node;
	NezMacro child;

	public NezMacro(Tree<?> node, NezMacro macro) {
		this.node = node;
		this.child = macro;
	}

	@Override
	public String toString() {
		return "";
	}

	public abstract Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node);
}

class FunctionSet {
	String name;
	List<NezMacro> set;

	public FunctionSet(String name) {
		this.name = name;
		this.set = new ArrayList<>();
	}

	public void addFunc(NezMacro macro) {
		this.set.add(macro);
	}

}

class DesugarFunction extends NezMacro {
	String name;
	List<Name> params;

	public DesugarFunction(String name, Tree<?> node, NezMacro macro) {
		super(node, macro);
		this.name = name;
		params = new ArrayList<>();
	}

	public void addParam(Name macro) {
		this.params.add(macro);
	}

	public void setParam(int index, Name macro) {
		this.params.add(index, macro);
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		String str = "desugar #" + this.getName() + "(";
		for (int i = 0; i < params.size(); i++) {
			str += params.get(i).toString();
			if (i < params.size() - 1) {
				str += ", ";
			}
		}
		str += ") ->";
		str += this.child.toString();
		return str;
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}

}

class TransFunction extends NezMacro {
	String name;
	List<NezMacro> params;

	public TransFunction(String name, Tree<?> node, NezMacro macro) {
		super(node, macro);
		this.name = name;
		this.params = new ArrayList<>();
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

	@Override
	public String toString() {
		String str = "desugar " + this.getName() + "(";
		for (int i = 0; i < params.size(); i++) {
			str += params.get(i).toString();
			if (i < params.size() - 1) {
				str += ", ";
			}
		}
		str += ") ->";
		str += this.child.toString();
		return str;
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}

}

class TransVariable extends NezMacro {
	String name;
	Tree<?> desugarNode;

	public TransVariable(String name, Tree<?> node, NezMacro macro) {
		super(node, macro);
		this.name = name;
	}

	public void setDesugarNode(Tree<?> node) {
		this.desugarNode = node;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		return "define " + this.name + " " + this.child.toString();
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}

}

class NodeLiteral extends NezMacro {
	String name;
	Tree<?> desugarNode;
	List<Symbol> labels;
	List<NezMacro> list;

	public NodeLiteral(Tree<?> node, String name) {
		super(node, null);
		this.name = name;
		this.labels = new ArrayList<>();
		this.list = new ArrayList<>();
	}

	public void createDesugarTree(Tree<?> node, Class<?> clazz) throws InstantiationException, IllegalAccessException {
		desugarNode = (Tree<?>) clazz.newInstance();
	}

	public void add(NezMacro macro, Symbol label) {
		this.list.add(macro);
		this.labels.add(label);
	}

	@Override
	public String toString() {
		String str = "#" + this.name + "[";
		for (int i = 0; i < list.size(); i++) {
			str += list.get(i).toString();
			if (i < list.size() - 1) {
				str += ", ";
			}
		}
		str += "]";
		return str;
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}

}

class NodeElement extends NezMacro {
	Symbol desugarLabel;

	public NodeElement(Tree<?> node, Symbol label, NezMacro child) {
		super(node, child);
		this.desugarLabel = label;
	}

	@Override
	public String toString() {
		return "$" + this.desugarLabel.getSymbol() + " = " + this.child.toString();
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}

}

class Field extends NezMacro {
	Symbol label;

	public Field(Tree<?> node, NezMacro macro, Symbol label) {
		super(node, macro);
		this.label = label;
	}

	@Override
	public String toString() {
		return this.child.toString() + "." + this.label.getSymbol();
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}
}

class Indexer extends NezMacro {
	int index;

	public Indexer(Tree<?> node, NezMacro macro, int index) {
		super(node, macro);
		this.index = index;
	}

	@Override
	public String toString() {
		return this.child.toString() + "[" + this.index + "]";
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}
}

class Name extends NezMacro {
	String name;

	public Name(Tree<?> node, String name) {
		super(node, null);
		this.name = name;
	}

	@Override
	public String toString() {
		return this.name;
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}
}

class StringLiteral extends NezMacro {
	String str;

	public StringLiteral(Tree<?> node, String str) {
		super(node, null);
		this.str = str;
	}

	@Override
	public String toString() {
		return '"' + this.str + '"';
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
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

	@Override
	public String toString() {
		String indent = "  ";
		String str = "";
		for (NezMacro macro : this.list) {
			str += indent + macro.toString() + "\n";
		}
		return str;
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}

}

class ListParam extends NezMacro {
	Name first;
	Name listNode;

	public ListParam(Tree<?> node, Name first, Name list) {
		super(node, null);
		this.first = first;
		this.listNode = list;
	}

	@Override
	public String toString() {
		return first.toString() + ":" + listNode.toString();
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}

}

class EmptyList extends NezMacro {

	public EmptyList(Tree<?> node) {
		super(node, null);
	}

	@Override
	public String toString() {
		return "[]";
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}

}

class Apply extends NezMacro {
	String name;
	List<NezMacro> args;

	public Apply(String name, Tree<?> node) {
		super(node, null);
		this.name = name;
		this.args = new ArrayList<>();
	}

	public void addArg(NezMacro macro) {
		this.args.add(macro);
	}

	public void setArg(int index, NezMacro macro) {
		this.args.add(index, macro);
	}

	public String getName() {
		return this.name;
	}

	@Override
	public String toString() {
		String str = this.getName() + "(";
		for (int i = 0; i < args.size(); i++) {
			str += args.get(i).toString();
			if (i < args.size() - 1) {
				str += ", ";
			}
		}
		str += ")";
		return str;
	}

	@Override
	public Tree<?> desugar(MacroInterpreter interpreter, Tree<?> node) {
		return interpreter.desugar(this, node);
	}

}