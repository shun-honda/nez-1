package nez.lang;

import java.util.HashMap;

import nez.ast.Symbol;
import nez.ast.Tree;

public class MacroManager {
	private HashMap<String, Macro> macroMap;

	public MacroManager() {
		this.macroMap = new HashMap<String, Macro>();
	}

	public void addMacro(String name, Macro macro) {
		this.macroMap.put(name, macro);
	}

	public Macro getMacro(String name) {
		return this.macroMap.get(name);
	}

	public Macro newDesugerFunction(Tree<?> node) {
		Macro macro = new DesugarFunction(node);
		this.addMacro(node.getText(Symbol.tag("name"), null), macro);
		return macro;
	}

	public Macro newTransFunction(Tree<?> node) {
		Macro macro = new TransFunction(node);
		this.addMacro(node.getText(Symbol.tag("name"), null), macro);
		return macro;
	}

	public Macro newTransVariable(Tree<?> node) {
		Macro macro = new TransVariable(node);
		this.addMacro(node.getText(Symbol.tag("name"), null), macro);
		return macro;
	}

}
