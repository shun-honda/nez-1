package nez.bx;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.ast.TreeVisitorMap;

public class Formatter extends TreeVisitorMap<FomatterVisitor> {

	public class Undefined implements FomatterVisitor {
		@Override
		public void accept(Tree<?> node) {
			throw new RuntimeException("undefined node: " + node);
		}
	}
}

interface FomatterVisitor {
	public void accept(Tree<?> node);
}

interface FormatSymbols {
	static final Symbol Format = Symbol.unique("Format");
	static final Symbol Name = Symbol.unique("Name");
	static final Symbol List = Symbol.unique("List");
	static final Symbol Param = Symbol.unique("Param");
	static final Symbol TagParam = Symbol.unique("TagParam");
	static final Symbol ListParam = Symbol.unique("ListParam");
	static final Symbol Text = Symbol.unique("Text");
	static final Symbol Apply = Symbol.unique("Apply");
	static final Symbol SystemVariable = Symbol.unique("SystemVariable");
	static final Symbol Field = Symbol.unique("Field");

	static final Symbol name = Symbol.unique("name");
	static final Symbol param = Symbol.unique("param");
	static final Symbol body = Symbol.unique("body");
	static final Symbol label = Symbol.unique("label");
	static final Symbol tag = Symbol.unique("tag");
	static final Symbol list = Symbol.unique("list");
}
