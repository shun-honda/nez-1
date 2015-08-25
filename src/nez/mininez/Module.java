package nez.mininez;

import java.util.ArrayList;
import java.util.List;

import nez.lang.Grammar;

public class Module {
	List<Function> funcList;
	Grammar g;

	public Module() {
		this.funcList = new ArrayList<Function>();
	}

	public void setGrammar(Grammar g) {
		this.g = g;
	}

	public Function get(int index) {
		return this.funcList.get(index);
	}

	public Function get(String name) {
		for(Function func : this.funcList) {
			if(func.funcName.equals(name)) {
				return func;
			}
		}
		// ConsoleUtils.exit(1, "error: NonTerminal is not found " + name);
		return null;
	}

	public void append(Function func) {
		this.funcList.add(func);
	}

	public int size() {
		return funcList.size();
	}

	public String stringfy(StringBuilder sb) {
		for(int i = 0; i < size(); i++) {
			this.get(i).stringfy(sb);
		}
		return sb.toString();
	}
}
