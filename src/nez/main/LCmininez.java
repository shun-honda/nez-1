package nez.main;

import nez.lang.Grammar;
import nez.vm.ByteCoder;
import nez.vm.MiniNezByteCoder;
import nez.vm.MiniNezCompiler;
import nez.vm.NezCode;
import nez.vm.NezCompiler;

public class LCmininez extends Command {

	@Override
	public String getDesc() {
		return "compiling MiniNez bytecode";
	}

	@Override
	public void exec(CommandContext config) {
		Grammar g = config.getGrammar();
		NezCompiler compile = new MiniNezCompiler(config.getNezOption());
		ByteCoder c = new MiniNezByteCoder();
		NezCode code = compile.compile(g, c);
		c.writeTo(config.getGrammarFileName("mnzc"));
	}

}
