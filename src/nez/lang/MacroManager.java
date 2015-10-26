package nez.lang;

import java.util.HashMap;

import nez.ast.Symbol;
import nez.ast.TreeVisitor2;

public abstract class MacroManager extends TreeVisitor2<CommonMacroVisitor> {
	protected HashMap<String, Macro> macroMap;

	public MacroManager() {
		this.macroMap = new HashMap<String, Macro>();
	}

	public void addMacro(String name, Macro macro) {
		this.macroMap.put(name, macro);
	}

	public Macro getMacro(String name) {
		return this.macroMap.get(name);
	}

	/* tag */

	public final static Symbol _DesugarFuncDecl = Symbol.tag("DesugarFuncDecl");
	public final static Symbol _TransFuncDecl = Symbol.tag("TransFuncDecl");
	public final static Symbol _TransVarDecl = Symbol.tag("TransVarDecl");
	public final static Symbol _ListArg = Symbol.tag("ListArg");
	public final static Symbol _EmptyList = Symbol.tag("EmptyList");
	public final static Symbol _Field = Symbol.tag("Field");
	public final static Symbol _String = Symbol.tag("String");
	public final static Symbol _NodeValue = Symbol.tag("NodeValue");
	public final static Symbol _ApplyVar = Symbol.tag("ApplyVar");

	/* label */
	public final static Symbol _name = Symbol.tag("name");
	public final static Symbol _param = Symbol.tag("param");
	public final static Symbol _body = Symbol.tag("body");
	public final static Symbol _val = Symbol.tag("val");
	public final static Symbol _expr = Symbol.tag("expr");
	public final static Symbol _label = Symbol.tag("label");
	public final static Symbol _recv = Symbol.tag("recv");
	public final static Symbol _first = Symbol.tag("first");
	public final static Symbol _list = Symbol.tag("list");
	public final static Symbol _index = Symbol.tag("index");
}
