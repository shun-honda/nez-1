package nez.debugger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nez.ast.CommonTree;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.util.ConsoleUtils;

public class NezDebugger {
	HashMap<String, BreakPoint> breakPointMap = new HashMap<String, BreakPoint>();
	HashMap<String, Production> ruleMap = new HashMap<String, Production>();
	List<String> nameList = new ArrayList<String>();
	DebugOperator command = null;
	Grammar peg;
	Module module;
	DebugVMInstruction code;
	DebugSourceContext sc;
	String text = null;
	int linenum = 0;
	boolean running = false;

	public NezDebugger(Grammar peg, DebugVMInstruction code, DebugSourceContext sc, Module m) {
		this.peg = peg;
		this.code = code;
		this.sc = sc;
		this.module = m;
		for(Production p : peg.getProductionList()) {
			this.ruleMap.put(p.getLocalName(), p);
			this.nameList.add(p.getLocalName());
		}
		ConsoleUtils.addCompleter(this.nameList);
	}

	class BreakPoint {
		Production pr;
		Integer id;

		public BreakPoint(Production pr, int id) {
			this.pr = pr;
			this.id = id;
		}
	}

	// public boolean exec(DebugVMInstruction code, DebugSourceContext sc) {
	// boolean result = false;
	// try {
	// while(true) {
	// code = code.exec(sc);
	// }
	// } catch (MachineExitException e) {
	// result = e.result;
	// }
	// return result;
	// }

	public boolean exec() {
		boolean result = false;
		showCurrentExpression();
		try {
			while(true) {
				readLine("(nezdb) ");
				command.exec(this);
				if(code instanceof Iexit) {
					code.exec(sc);
				}
				showCurrentExpression();
			}
		} catch (MachineExitException e) {
			result = e.result;
		}
		return result;
	}

	public boolean execCode() throws MachineExitException {
		if(this.code instanceof Icall) {
			if(this.breakPointMap.containsKey(((Icall) this.code).ne.getLocalName())) {
				this.code = this.code.exec(this.sc);
				return false;
			}
		}
		this.code = this.code.exec(this.sc);
		return true;
	}

	public void showCurrentExpression() {
		Expression e = null;
		if(code instanceof Icall) {
			e = ((Icall) code).ne;
		} else {
			e = code.getExpression();
		}
		if(running && e != null) {
			ConsoleUtils.println(e.getSourcePosition().formatSourceMessage("debug", ""));
		} else if(e == null) {
			ConsoleUtils.println("e = null");
		}
	}

	public void showDebugUsage() {
		ConsoleUtils.println("Nez Debugger support following commands:");
		ConsoleUtils.println("  p | print [-ctx field? | ProductionName]  Print");
		ConsoleUtils.println("  b | break [ProductionName]                BreakPoint");
		ConsoleUtils.println("  n                                         StepOver");
		ConsoleUtils.println("  s                                         StepIn");
		ConsoleUtils.println("  f | finish                                StepOut");
		ConsoleUtils.println("  c                                         Continue");
		ConsoleUtils.println("  r | run                                   Run");
		ConsoleUtils.println("  q | exit                                  Exit");
		ConsoleUtils.println("  h | help                                  Help");
	}

	private void readLine(String prompt) {
		while(true) {
			Object console = ConsoleUtils.getConsoleReader();
			String line = ConsoleUtils.readSingleLine(console, prompt);
			if(line == null || line.equals("")) {
				if(this.command == null) {
					continue;
				}
				return;
			}
			String[] tokens = line.split("\\s+");
			String command = tokens[0];
			int pos = 1;
			if(command.equals("p") || command.equals("print")) {
				Print p = new Print();
				if(tokens.length < 2) {
					this.showDebugUsage();
					return;
				}
				if(tokens[pos].startsWith("-")) {
					if(tokens[pos].equals("-ctx")) {
						p.setType(Print.printContext);
					} else if(tokens[pos].equals("-pr")) {
						p.setType(Print.printProduction);
					} else if(tokens[pos].equals("-call")) {
						p.setType(Print.printCallers);
					}
					pos++;
				}
				if(pos < tokens.length) {
					p.setCode(tokens[pos]);
				}
				this.command = p;
				return;
			} else if(command.equals("bt")) {
				if(!running) {
					ConsoleUtils.println("error: invalid process");
				} else {
					this.command = new BackTrace();
					return;
				}
			} else if(command.equals("b") || command.equals("break")) {
				this.command = new Break();
				if(tokens.length < 2) {
					return;
				}
				this.command.setCode(tokens[pos]);
				return;
			} else if(command.equals("n")) {
				if(!running) {
					ConsoleUtils.println("error: invalid process");
				} else {
					this.command = new StepOver();
					return;
				}
			} else if(command.equals("s")) {
				if(!running) {
					ConsoleUtils.println("error: invalid process");
				} else {
					this.command = new StepIn();
					return;
				}
			} else if(command.equals("f") || command.equals("finish")) {
				if(!running) {
					ConsoleUtils.println("error: invalid process");
				} else {
					this.command = new StepOut();
					return;
				}
			} else if(command.equals("c")) {
				if(!running) {
					ConsoleUtils.println("error: invalid process");
				} else {
					this.command = new Continue();
					return;
				}
			} else if(command.equals("r") || command.equals("run")) {
				if(!running) {
					this.command = new Run();
					running = true;
					return;
				} else {
					ConsoleUtils.println("error: now running");
				}
			} else if(command.equals("q") || command.equals("exit")) {
				this.command = new Exit();
				return;
			} else if(command.equals("h") || command.equals("help")) {
				this.showDebugUsage();
			} else {
				ConsoleUtils.println("command not found: " + command);
				this.showDebugUsage();
			}
			ConsoleUtils.addHistory(console, line);
			linenum++;
		}
	}

	public boolean exec(Print o) {
		if(o.type == Print.printContext) {
			Context ctx = (Context) sc;
			if(o.code == null) {
				ConsoleUtils.println("context {");
				ConsoleUtils.println("  input_name = " + ctx.getResourceName());
				ConsoleUtils.println("  pos = " + ctx.getPosition());
				Object obj = ctx.getLeftObject();
				if(obj == null) {
					ConsoleUtils.println("  left = " + ctx.getLeftObject());
				} else {
					ConsoleUtils.println("  left = " + ctx.getLeftObject().hashCode());
				}
				ConsoleUtils.println("}");
			} else if(o.code.equals("pos")) {
				ConsoleUtils.println("pos = " + ctx.getPosition());
				ConsoleUtils.println(sc.formatDebugPositionLine(((Context) sc).getPosition(), ""));
			} else if(o.code.equals("input_name")) {
				ConsoleUtils.println("input_name = " + ctx.getResourceName());
			} else if(o.code.equals("left")) {
				ConsoleUtils.println("left = " + ctx.getLeftObject());
			} else {
				ConsoleUtils.println("error: no member nameed \'" + o.code + "\' in context");
			}
		} else if(o.type == Print.printProduction) {
			Production rule = ruleMap.get(o.code);
			if(rule != null) {
				ConsoleUtils.println(rule.toString());
			} else {
				ConsoleUtils.println("error: production not found '" + o.code + "'");
			}
		} else if(o.type == Print.printCallers) {
			Function f = this.module.get(o.code);
			if(f != null) {
				ConsoleUtils.println(f.funcName + " >>");
				for(int i = 0; i < f.callers.size(); i++) {
					ConsoleUtils.println("  [" + i + "] " + f.callers.get(i).funcName);
				}
			} else {
				ConsoleUtils.println("error: production not found '" + o.code + "'");
			}
		}
		return true;
	}

	public boolean exec(BackTrace o) {
		for(int i = 1; i <= this.sc.callStackTop; i++) {
			NonTerminal ne = (NonTerminal) this.sc.callStack[i].val;
			CommonTree tree = (CommonTree) ne.getSourcePosition();
			ConsoleUtils.println("[" + i + "] " + ne.getLocalName() + " ("
					+ tree.getSource().linenum(tree.getSourcePosition()) + ")");
		}
		return true;
	}

	public boolean exec(Break o) {
		if(this.command.code != null) {
			Production rule = ruleMap.get(this.command.code);
			if(rule != null) {
				this.breakPointMap.put(rule.getLocalName(), new BreakPoint(rule, this.breakPointMap.size() + 1));
				ConsoleUtils.println("breakpoint " + (this.breakPointMap.size()) + ": where = " + rule.getLocalName()
						+ " " + rule.getSourcePosition().formatDebugSourceMessage(""));
			} else {
				ConsoleUtils.println("production not found");
			}
		} else {
			this.showBreakPointList();
		}
		return true;
	}

	public void showBreakPointList() {
		if(this.breakPointMap.isEmpty()) {
			ConsoleUtils.println("No breakpoints currently set");
		} else {
			List<Map.Entry> mapValuesList = new ArrayList<Map.Entry>(this.breakPointMap.entrySet());
			Collections.sort(mapValuesList, new Comparator<Map.Entry>() {
				@Override
				public int compare(Entry entry1, Entry entry2) {
					return (((BreakPoint) entry1.getValue()).id).compareTo(((BreakPoint) entry2.getValue()).id);
				}
			});
			for(Entry s : mapValuesList) {
				BreakPoint br = (BreakPoint) s.getValue();
				Production rule = (br.pr);
				ConsoleUtils.println(br.id + ": " + rule.getLocalName() + " "
						+ rule.getSourcePosition().formatDebugSourceMessage(""));
			}
		}
	}

	public boolean exec(StepOver o) throws MachineExitException {
		if(this.code.op.equals(Opcode.Icall)) {
			int stackTop = this.sc.callStackTop;
			while(stackTop <= this.sc.callStackTop) {
				if(!this.execCode()) {
					break;
				}
			}
			return true;
		} else {
			Expression e = this.code.getExpression();
			Expression current = this.code.getExpression();
			while(e.equals(current)) {
				this.code = this.code.exec(this.sc);
				current = this.code.getExpression();
			}
		}
		if(this.code.op.equals(Opcode.Iret)) {
			this.code = this.code.exec(this.sc);
		}
		return true;
	}

	public boolean exec(StepIn o) throws MachineExitException {
		Expression e = this.code.getExpression();
		Expression current = this.code.getExpression();
		while(e.equals(current)) {
			this.code = this.code.exec(this.sc);
			current = this.code.getExpression();
		}
		if(this.code.op.equals(Opcode.Iret)) {
			this.code = this.code.exec(this.sc);
		}
		return true;
	}

	public boolean exec(StepOut o) throws MachineExitException {
		int stackTop = this.sc.callStackTop;
		while(stackTop <= this.sc.callStackTop) {
			if(!this.execCode()) {
				break;
			}
		}
		return true;
	}

	public boolean exec(Continue o) throws MachineExitException {
		while(true) {
			if(!this.execCode()) {
				break;
			}
		}
		return true;
	}

	public boolean exec(Run o) throws MachineExitException {
		while(true) {
			if(!this.execCode()) {
				return true;
			}
		}
	}

	public boolean exec(Exit o) {
		ConsoleUtils.exit(0, "debugger (status=0)");
		return false;
	}

}
