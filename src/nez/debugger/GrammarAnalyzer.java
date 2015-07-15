package nez.debugger;

import java.util.ArrayList;
import java.util.List;

import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Choice;
import nez.lang.Empty;
import nez.lang.Expression;
import nez.lang.Failure;
import nez.lang.Grammar;
import nez.lang.GrammarOptimizer;
import nez.lang.GrammarReshaper;
import nez.lang.NonTerminal;
import nez.lang.Not;
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
		for(Production p : this.peg.getProductionList()) {
			if(p.getLocalName().equals("Statement")) {
				System.out.println();
			}
			this.analizeConsumption(p.getExpression());
		}
	}

	private boolean analizeConsumption(Expression p) {
		if(p instanceof Repetition || p instanceof Repetition1) {
			if(!this.analizeInnerOfRepetition(p.get(0))) {
				ConsoleUtils.println(p.getSourcePosition().formatSourceMessage("warning", "unconsumed Repetition"));
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
		if(p instanceof Failure) {
			return false;
		}
		if(p instanceof Not) {
			if(p.get(0) instanceof AnyChar) {
				return false;
			}
			return this.analizeInnerOfRepetition(p.get(0));
		}
		if(p instanceof Unary) {
			return this.analizeInnerOfRepetition(p.get(0));
		}
		if(p instanceof Sequence) {
			for(int i = 0; i < p.size(); i++) {
				if(p.get(i).isConsumed()) {
					if(this.analizeInnerOfRepetition(p.get(i))) {
						return true;
					}
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

class UnreachableAnalyzer extends GrammarReshaper {
	ArrayList<NonTerminal> recursiveList = new ArrayList<NonTerminal>();

	class Consume {
		boolean isChoiceElementConsumption = false;
		Choice choice;
		NonTerminal ne;
		Consume prev;
		List<Consume> next;
		int index;
		List<Integer> consumeList;

		public Consume(Choice choice, int index, Consume prev) {
			this.isChoiceElementConsumption = true;
			this.choice = choice;
			this.index = index;
			this.prev = prev;
			if(prev != null) {
				this.prev.next.add(this);
			}
			this.next = new ArrayList<Consume>();
			this.consumeList = new ArrayList<Integer>();
		}

		public Consume(NonTerminal ne) {
			this.ne = ne;
		}

		public Consume append(int ch) {
			this.consumeList.add(ch);
			return this;
		}

		@Override
		public String toString() {
			if(this.isChoiceElementConsumption) {
				return "(" + this.index + ")" + this.consumeList.toString();
			}
			return this.ne.getLocalName();
		}
	}

	Consume cur;

	public void pushConsume(Choice c, int index) {
		this.cur = new Consume(c, index, this.cur);
	}

	public Consume popConsume() {
		Consume c = this.cur;
		this.cur = this.cur.prev;
		return c;
	}

	public Consume peekConsume() {
		return this.cur;
	}

	@Override
	public Expression reshapeNonTerminal(NonTerminal p) {
		NonTerminal ne = (NonTerminal) p;
		boolean contains = recursiveList.contains(ne);
		boolean isRecursive = ne.getProduction().isRecursive();
		if(!contains && isRecursive) {
			recursiveList.add(ne);
		} else if(contains) {
			return p;
		}
		return GrammarOptimizer.resolveNonTerminal(p).reshape(this);
	}

	@Override
	public Expression reshapeEmpty(Empty e) {
		return e;
	}

	@Override
	public Expression reshapeFailure(Failure e) {
		return e;
	}

	@Override
	public Expression reshapeByteChar(ByteChar e) {
		return e;
	}

	@Override
	public Expression reshapeByteMap(ByteMap e) {
		return e;
	}

	@Override
	public Expression reshapeAnyChar(AnyChar e) {
		return e;
	}

}
