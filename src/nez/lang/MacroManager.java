package nez.lang;

import java.util.HashMap;

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

}
