package nez.debugger;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeFactory;
import nez.ast.CommonTreeWriter;
import nez.lang.Grammar;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.main.Recorder;
import nez.util.ConsoleUtils;

public class NezDebuggerCommand extends Command {

	@Override
	public void exec(CommandConfigure config) {
		Command.displayVersion();
		Recorder rec = config.getRecorder();
		Grammar peg = config.getGrammar();
		peg.record(rec);
		while(config.hasInput()) {
			SourceContext file = config.getInputSourceContext();
			file.start(rec);
			NezDebugger d = new NezDebugger(peg);
			CommonTree node = (CommonTree) d.parse(file, new CommonTreeFactory());
			file.done(rec);
			if(node == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			file = null;
			new CommonTreeWriter().transform(config.getOutputFileName(file), node);
		}
	}

	@Override
	public String getDesc() {
		return "debug";
	}

}
