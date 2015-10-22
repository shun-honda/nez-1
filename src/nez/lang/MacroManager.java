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

	/* label */
	public final static Symbol _name = Symbol.tag("name");
	public final static Symbol _param = Symbol.tag("param");
	public final static Symbol _body = Symbol.tag("body");
}
