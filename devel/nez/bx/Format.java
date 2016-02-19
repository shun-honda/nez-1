package nez.bx;

import java.util.ArrayList;
import java.util.List;

import nez.ast.Symbol;
import nez.ast.Tree;

public abstract class Format {
	public abstract String format(AbstractFormatter formatter, Tree<?> node);
}

class FormatFunctionSet<T> {
	List<T> set;

	public FormatFunctionSet() {
		set = new ArrayList<T>();
	}

	public void addFunction(T func) {
		set.add(func);
	}

	public T getFunc(int index) {
		return set.get(index);
	}
}

class TagFormat extends Format {
	String name;
	List<Format> params;
	List<Format> body;

	public TagFormat(String name, List<Format> body, List<Format> params) {
		this.name = name;
		this.params = params;
		this.body = body;
	}

	@Override
	public String format(AbstractFormatter formatter, Tree<?> node) {
		return formatter.format(this, node);
	}
}

class TagParam extends Format {
	String name;
	List<Symbol> tags;

	public TagParam(String name, List<Symbol> tags) {
		this.name = name;
		this.tags = tags;
	}

	@Override
	public String format(AbstractFormatter formatter, Tree<?> node) {
		return formatter.format(this, node);
	}

}

class ListParam extends Format {
	Format name;
	// String name;
	String listName;

	public ListParam(Format name, String listName) {
		this.name = name;
		this.listName = listName;
	}

	@Override
	public String format(AbstractFormatter formatter, Tree<?> node) {
		return formatter.format(this, node);
	}

}

class EmptyListParam extends Format {

	@Override
	public String format(AbstractFormatter formatter, Tree<?> node) {
		return formatter.format(this, node);
	}
}

class UserDefinedFormat extends Format {
	String name;
	List<Format> params;
	List<Format> body;

	public UserDefinedFormat(String name, List<Format> body, List<Format> params) {
		this.name = name;
		this.params = params;
		this.body = body;
	}

	@Override
	public String format(AbstractFormatter formatter, Tree<?> node) {
		return formatter.format(this, node);
	}
}

class Text extends Format {
	String text;

	public Text(String text) {
		this.text = text;
	}

	@Override
	public String format(AbstractFormatter formatter, Tree<?> node) {
		return formatter.format(this, node);
	}
}

class Name extends Format {
	String name;

	public Name(String name) {
		this.name = name;
	}

	@Override
	public String format(AbstractFormatter formatter, Tree<?> node) {
		return formatter.format(this, node);
	}
}

class SystemVariable extends Format {
	String name;

	public SystemVariable(String name) {
		this.name = name;
	}

	@Override
	public String format(AbstractFormatter formatter, Tree<?> node) {
		return formatter.format(this, node);
	}
}

class ApplyUserDefinedFormat extends Format {
	String name;
	List<Format> args;

	public ApplyUserDefinedFormat(String name, List<Format> args) {
		this.name = name;
		this.args = args;
	}

	@Override
	public String format(AbstractFormatter formatter, Tree<?> node) {
		return formatter.format(this, node);
	}
}
