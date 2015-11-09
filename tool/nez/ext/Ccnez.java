package nez.ext;

import nez.main.CommandContext;
import nez.parser.ParserGenerator;

public class Ccnez extends Cpeg {
	protected ParserGenerator newParserGenerator(CommandContext config) {
		return config.newParserGenerator(nez.x.generator.CParserGenerator.class);
	}
}
