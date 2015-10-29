package nez.lang.macro;

import java.util.HashMap;

import nez.ast.Symbol;
import nez.util.VisitorMap;

public abstract class AbstractMacroManager extends VisitorMap<CommonMacroVisitor> {
	protected HashMap<String, NezMacro> macroMap;

	public AbstractMacroManager() {
		this.macroMap = new HashMap<String, NezMacro>();
	}

	public void addMacro(String name, NezMacro macro) {
		this.macroMap.put(name, macro);
	}

	public NezMacro getMacro(String name) {
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
}
