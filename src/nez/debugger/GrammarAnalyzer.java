package nez.debugger;

import nez.lang.Choice;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarOptimizer;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Sequence;
import nez.lang.Unary;
import nez.util.ConsoleUtils;

public class GrammarAnalyzer {
	Grammar peg;

	public GrammarAnalyzer(Grammar peg) {
		this.peg = peg;
	}

	public void analyze() {
		boolean error = false;
		for(Production p : this.peg.getProductionList()) {
			if(!this.analizeConsumption(p.getExpression())) {
				error = true;
			}
		}
		if(error) {
			ConsoleUtils.exit(1, "Grammar Error");
		}
	}

	private boolean analizeConsumption(Expression p) {
		if(p instanceof Repetition || p instanceof Repetition1) {
			if(!this.analizeInnerOfRepetition(p.get(0))) {
				ConsoleUtils.println(p.getSourcePosition().formatSourceMessage("error", "unconsumed Repetition"));
				return false;
			}
		}
		if(p instanceof Unary) {
			return this.analizeConsumption(p.get(0));
		}
		if(p instanceof Sequence || p instanceof Choice) {
			for(int i = 0; i < p.size(); i++) {
				if(!this.analizeConsumption(p.get(i))) {
					return false;
				}
			}
			return true;
		}
		return true;
	}

	private boolean analizeInnerOfRepetition(Expression p) {
		p = GrammarOptimizer.resolveNonTerminal(p);
		if(p instanceof Repetition1) {
			return true;
		}
		if(p instanceof Repetition || p instanceof Option) {
			return false;
		}
		if(p instanceof Unary) {
			return this.analizeInnerOfRepetition(p.get(0));
		}
		if(p instanceof Sequence) {
			for(int i = 0; i < p.size(); i++) {
				if(this.analizeInnerOfRepetition(p.get(i))) {
					return true;
				}
			}
			return false;
		}
		if(p instanceof Choice) {
			for(int i = 0; i < p.size(); i++) {
				if(!this.analizeInnerOfRepetition(p.get(i))) {
					return false;
				}
			}
			return true;
		}
		return true;
	}

}
