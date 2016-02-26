package nez.bx;

import java.io.IOException;

import nez.ParserGenerator;
import nez.ast.Source;
import nez.ast.Tree;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.io.CommonSource;
import nez.tool.ast.TreeWriter;

public class Command extends nez.main.Command {
	@Override
	public void exec() throws IOException {
		/* Setting requird options */
		strategy.Optimization = false;
		Grammar grammar = this.newGrammar();
		FormatGenerator gen = new FormatGenerator(outputDirectory, grammarFile);
		gen.generate(grammar);
		checkInputSource();
		Parser parser = newParser();
		TreeWriter tw = this.getTreeWriter("ast xml json");
		while (hasInputSource()) {
			Source input = nextInputSource();
			Tree<?> node = parser.parse(input);
			if (node == null) {
				parser.showErrors();
				continue;
			}
			if (this.outputDirectory != null) {
				tw.init(getOutputFileName(input, tw.getFileExtension()));
			}
			tw.writeTree(node);
			ParserGenerator pg = new ParserGenerator();
			grammar = pg.loadGrammar("format.nez");
			Parser formatParser = this.strategy.newParser(grammar);
			input = CommonSource.newFileSource(gen.getOutputFileName());
			Tree<?> formatNode = formatParser.parse(input);
			if (formatNode == null) {
				formatParser.showErrors();
				continue;
			}
			tw.writeTree(formatNode);
			FormatterBuilder builder = new FormatterBuilder();
			builder.visit(formatNode);
			Formatter formatter = new Formatter(builder.getContext());
			String source = formatter.format(node);
			input = CommonSource.newStringSource(source);
			Tree<?> newNode = parser.parse(input);
			if (newNode == null) {
				parser.showErrors();
				continue;
			}
			tw.writeTree(newNode);
			System.out.println(new ASTEqualChecker().isEqual(node, newNode));
		}
	}

}
