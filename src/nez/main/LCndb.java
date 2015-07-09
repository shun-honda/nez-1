package nez.main;

import nez.debugger.DebugInputManager;
import nez.debugger.DebugSourceContext;
import nez.lang.Grammar;
import nez.lang.GrammarFile;
import nez.util.ConsoleUtils;

public class LCndb extends Command {
	@Override
	public String getDesc() {
		return "Nez debugger";
	}

	String command = null;
	String text = null;
	int linenum = 0;

	@Override
	public void exec(CommandContext config) {
		// config.setNezOption(NezOption.DebugOption);
		Command.displayVersion();
		config.getNezOption().setOption("asis", true);
		config.getNezOption().setOption("intern", false);
		if(!config.hasInputSource()) {
			GrammarFile gfile = config.getGrammarFile(true);
			ConsoleUtils.addCompleter(gfile.getNonterminalList());
			while(readLine("StartPoint:")) {
				Grammar g = getGrammar(config, command);
				if(this.text != null) {
					DebugSourceContext sc = DebugSourceContext.newStringSourceContext("<stdio>", linenum, text);
					g.debug(sc);
				}
			}
		} else {
			Grammar peg = config.getGrammar();
			DebugInputManager manager = new DebugInputManager(config.inputFileLists);
			manager.exec(peg);
		}
	}

	private boolean readLine(String prompt) {
		Object console = ConsoleUtils.getConsoleReader();
		String line = ConsoleUtils.readSingleLine(console, prompt);
		if(line == null) {
			return false;
		}
		// int index = -1;
		// for(int i = 0; i < line.length(); i++) {
		// char c = line.charAt(i);
		// if(c == ' ' && index == -1) {
		// index = i;
		// }
		// }
		// if(index != -1) {
		// this.command = line.substring(0, index).trim();
		// this.text = line.substring(index).trim();
		// ConsoleUtils.addHistory(console, line);
		// }
		// else {
		this.command = line.trim();
		this.text = null;
		ConsoleUtils.addHistory(console, line);
		if(this.command.equals("q") || this.command.equals("exit")) {
			return false;
		}
		// }
		linenum++;
		StringBuilder sb = new StringBuilder();
		ConsoleUtils.println("Input:");
		while((line = ConsoleUtils.readSingleLine(console, "... ")) != null) {
			if(line.endsWith(";;")) {
				break;
			}
			sb.append(line);
			sb.append("\n");
		}
		this.text = sb.toString();
		return true;
	}

	private Grammar getGrammar(CommandContext config, String text) {
		String name = text.replace('\n', ' ').trim();
		Grammar g = config.getGrammar(name, config.getNezOption());
		if(g == null) {
			ConsoleUtils.println("NameError: name '" + name + "' is not defined");
		}
		return g;
	}
}