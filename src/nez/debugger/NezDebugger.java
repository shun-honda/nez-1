package nez.debugger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeFactory;
import nez.ast.CommonTreeWriter;
import nez.ast.ParsingFactory;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.main.Recorder;
import nez.vm.Context;
import nez.vm.Instruction;
import nez.vm.Machine;
import nez.vm.TerminationException;
import nez.util.ConsoleUtils;

public class NezDebugger extends NezDebugExecuter {
	HashMap<String, BreakPoint> breakPointMap = new HashMap<String, BreakPoint>();
	HashMap<String, Production> ruleMap = new HashMap<String, Production>();
	NezDebugOperator command = null;
	SourceContext sc = null;
	Grammar peg = null;
	Instruction code = null;
	String text = null;
	int linenum = 0;
	boolean running = false;
	
	public NezDebugger(Grammar peg) {
		this.peg = peg;
	}
	
	class BreakPoint {
		Production pr;
		Integer id;
		
		public BreakPoint(Production pr, int id) {
			this.pr = pr;
			this.id = id;
		}
	}
	
	class CommandStatus {
		boolean isStepOver = false;
		boolean isProductionStepOver = false;
		boolean isStepIn = false;
		boolean isStepOut = false;
		boolean isExec = false;
	}
	
	CommandStatus status = new CommandStatus();
	
	public Object parse(SourceContext sc, ParsingFactory treeFactory) {
		this.sc = sc;
		long startPosition = sc.getPosition();
		sc.setFactory(treeFactory);
		if(!this.run()) {
			return null;
		}
		Object node = sc.getParsingObject();
		if(node == null) {
			node = treeFactory.newNode(null, sc, startPosition, sc.getPosition(), 0, null);
		}
		return treeFactory.commit(node);
	}
	
	public boolean run() {
		code = peg.compile();
		sc.initJumpStack(64, peg.getMemoTable(sc));
		for(Production p : peg.getProductionList()) {
			ruleMap.put(p.getLocalName(), p);
		}
		this.exec();
		boolean result = false;
		try {
			Expression e = null;
			while(true) {
				if(e != code.getExpression() && !(code.getExpression() instanceof Production)) {
					e = code.getExpression();
					if(status.isStepOver) {
						status.isStepOver = false;
						if(e instanceof NonTerminal) {
							status.isProductionStepOver = true;
						}
						else {
							this.exec();
						}
					}
					else if(status.isStepIn) {
						status.isStepOver = false;
						this.exec();
					}
					else if(status.isExec) {
						status.isExec = false;
						this.exec();
					}
				}
				if(e instanceof NonTerminal) {
					if(this.breakPointMap.containsKey(((NonTerminal)e).getLocalName())) {
						status.isExec = true;
					}
				}
				else if(e instanceof Production) {
					if(status.isStepOut) {
						status.isStepOut = false;
						status.isExec = true;
					}
				}
				code = Machine.runDebugger(code, sc);
			}
		}
		catch (TerminationException e) {
			result = e.getStatus();
		}
		return result;
	}
	
	public void exec() {
		showCurrentExpression();
		while(readLine("(nezdb) ")) {
			System.out.println("command: " + command.type.name());
			if(!command.exec(this)) {
				return;
			}
			showCurrentExpression();
		}
	}
	
	Expression current = null;
	public void showCurrentExpression() {
		Expression e = code.getExpression();
		if(running && current != e) {
			if(e.getSourcePosition() == null) {
				ConsoleUtils.println(e.toString());
			}
			else {
				ConsoleUtils.println(e.getSourcePosition().formatSourceMessage("debug", ""));
			}
			current = e;
		}
	}
	
	private boolean readLine(String prompt) {
		Object console = ConsoleUtils.getConsoleReader();
		String line = ConsoleUtils.readSingleLine(console, prompt);
		if(line == null || line.equals("")) {
			return true;
		}
		String[] tokens = line.split("\\s+");
		String command = tokens[0];
		int pos = 1;
		if(command.equals("p") || command.equals("print")) {
			Print p = new Print();
			if(tokens.length < 2) {
				p.showDebugUsage();
				return true;
			}
			if(tokens[pos].startsWith("-")) {
				if(tokens[pos].equals("-ctx")) {
					p.setType(Print.printContext);
				}
				else if (tokens[pos].equals("-pr")) {
					p.setType(Print.printProduction);
				}
				pos++;
			}
			p.setCode(tokens[pos]);
			this.command = p;
		}
		else if(command.equals("b") || command.equals("break")) {
			this.command = new Break();
			if(tokens.length < 2) {
				return true;
			}
			this.command.setCode(tokens[pos]);
		}
		else if(command.equals("n")) {
			if(!running) {
				ConsoleUtils.println("error: invalid process");
			}
			else {
				this.command = new StepOver();
			}
		}
		else if(command.equals("s")) {
			if(!running) {
				ConsoleUtils.println("error: invalid process");
			}
			else {
				this.command = new StepIn();
			}
		}
		else if(command.equals("f") || command.equals("finish")) {
			if(!running) {
				ConsoleUtils.println("error: invalid process");
			}
			else {
				this.command = new StepOut();
			}
		}
		else if(command.equals("c")) {
			if(!running) {
				ConsoleUtils.println("error: invalid process");
			}
			else {
				this.command = new Continue();
			}
		}
		else if(command.equals("r") || command.equals("run")) {
			if(!running) {
				this.command = new Run();
				running = true;
			}
			else {
				ConsoleUtils.println("error: now running");
			}
		}
		else if(command.equals("q") || command.equals("exit")) {
			this.command = new Exit();
		}
		else {
			ConsoleUtils.println("command not found: " + command);
		}
		ConsoleUtils.addHistory(console, line);
		linenum++;
		return true;
	}

	@Override
	public boolean exec(Print o) {
		if(o.type == Print.printContext) {
			if(o.code.equals("pos")) {
				ConsoleUtils.println(sc.formatPositionLine(((Context)sc).getPosition()));
			}
			else {
				ConsoleUtils.println("error: no member nameed \'" + o.code + "\' in context");
			}
		}
		else if(o.type == Print.printProduction) {
			Production rule = ruleMap.get(o.code);
			if(rule != null) {
				ConsoleUtils.println(rule.toString());
			}
			else {
				ConsoleUtils.println("error: production not found '" + o.code + "'");
			}
		}
		return true;
	}

	@Override
	public boolean exec(Break o) {
		if(this.command.code != null) {
			Production rule = ruleMap.get(this.command.code);
			if(rule != null) {
				this.breakPointMap.put(rule.getLocalName(), new BreakPoint(rule, this.breakPointMap.size()+1));
				ConsoleUtils.println("breakpoint " + (this.breakPointMap.size()) + ": where = " + rule.getLocalName() + " " + rule.getSourcePosition().formatSourceMessage("notice", ""));
			}
			else {
				ConsoleUtils.println("production not found");
			}
		}
		else {
			this.showBreakPointList();
		}
		return true;
	}
	
	public void showBreakPointList() {
		if(this.breakPointMap.isEmpty()) {
			ConsoleUtils.println("No breakpoints currently set");
		}
		else {
			List<Map.Entry> mapValuesList = new ArrayList<Map.Entry>(this.breakPointMap.entrySet());
			Collections.sort(mapValuesList, new Comparator<Map.Entry>() {
				@Override
				public int compare(Entry entry1, Entry entry2) {
					return (((BreakPoint) entry1.getValue()).id).compareTo(((BreakPoint) entry2.getValue()).id);
				}
			});
			for(Entry s : mapValuesList) {
				BreakPoint br = (BreakPoint)s.getValue();
				Production rule = (br.pr);
				ConsoleUtils.println(br.id + ": " + rule.getLocalName() + " " + rule.getSourcePosition().formatSourceMessage("notice", ""));
			}
		}
	}

	@Override
	public boolean exec(StepOver o) {
		status.isStepOver = true;
		return false;
	}

	@Override
	public boolean exec(StepIn o) {
		status.isStepIn = true;
		return false;
	}

	@Override
	public boolean exec(StepOut o) {
		status.isStepOut = true;
		return false;
	}

	@Override
	public boolean exec(Continue o) {
		return false;
	}

	@Override
	public boolean exec(Run o) {
		return false;
	}

	@Override
	public boolean exec(Exit o) {
		ConsoleUtils.exit(0, "");
		return false;
	}
}
