package nez.debugger;

import java.util.ArrayList;
import java.util.List;

import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.DefSymbol;
import nez.lang.Empty;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.Failure;
import nez.lang.Grammar;
import nez.lang.GrammarOptimizer;
import nez.lang.GrammarReshaper;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.Match;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;
import nez.lang.Unary;
import nez.util.ConsoleUtils;

public class GrammarAnalyzer {
	Grammar peg;

	public GrammarAnalyzer(Grammar peg) {
		this.peg = peg;
	}

	public void analyze() {
		for(Production p : this.peg.getProductionList()) {
			this.analizeConsumption(p.getExpression());
		}
		UnreachableAnalyzer analyzer = new UnreachableAnalyzer();
		this.peg.getStartProduction().reshape(analyzer);
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
				if(!isUnconsumedASTConstruction(p.get(i))) {
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

	public boolean isUnconsumedASTConstruction(Expression p) {
		if(p instanceof New || p instanceof Capture || p instanceof Tagging || p instanceof Replace) {
			return true;
		}
		return false;
	}

}

class UnreachableAnalyzer extends GrammarReshaper {
	ArrayList<NonTerminal> recursiveList = new ArrayList<NonTerminal>();

	class Consume {
		static final int Choice = 0;
		static final int ByteMap = 1;
		static final int AnyChar = 2;
		static final int NonTerminal = 3;
		static final int Start = 4;
		Expression e;
		Consume prev;
		List<Consume> next;
		int index;
		List<Integer> consumeList;
		int type;

		public Consume() {
			this.type = Start;
			this.prev = null;
			this.next = new ArrayList<Consume>();
			this.consumeList = new ArrayList<Integer>();
		}

		public Consume(Choice choice, int index, Consume prev) {
			this.type = Choice;
			this.e = choice;
			this.index = index;
			this.prev = prev;
			this.prev.next.add(this);
			this.next = new ArrayList<Consume>();
			this.consumeList = new ArrayList<Integer>();
		}

		public Consume(ByteMap bm, Consume prev) {
			this.type = ByteMap;
			this.e = bm;
			this.prev = prev;
			this.prev.next.add(this);
			this.next = new ArrayList<Consume>();
			this.consumeList = new ArrayList<Integer>();
		}

		public Consume(AnyChar any, Consume prev) {
			this.type = AnyChar;
			this.e = any;
			this.prev = prev;
			this.prev.next.add(this);
			this.next = new ArrayList<Consume>();
			this.consumeList = new ArrayList<Integer>();
		}

		public Consume(NonTerminal ne, Consume prev) {
			this.type = NonTerminal;
			this.e = ne;
			this.prev = prev;
			this.prev.next.add(this);
			this.next = new ArrayList<Consume>();
			this.consumeList = new ArrayList<Integer>();
		}

		public Consume append(int ch) {
			this.consumeList.add(ch);
			return this;
		}

		@Override
		public String toString() {
			if(this.type == Choice) {
				return "(" + this.index + ")" + this.consumeList.toString();
			} else if(this.type == ByteMap || this.type == AnyChar) {
				return this.e.toString();
			}
			return ((NonTerminal) this.e).getLocalName();
		}
	}

	Consume cur = new Consume();
	int top = 0;

	public void pushConsume(Choice c, int index) {
		top++;
		this.cur = new Consume(c, index, this.cur);
	}

	public void pushConsume(ByteMap p) {
		top++;
		this.cur = new Consume(p, this.cur);
	}

	public void pushConsume(AnyChar p) {
		top++;
		this.cur = new Consume(p, this.cur);
	}

	public void pushConsume(NonTerminal p) {
		top++;
		this.cur = new Consume(p, this.cur);
	}

	public Consume popConsume() {
		top--;
		Consume c = this.cur;
		this.cur = this.cur.prev;
		return c;
	}

	public Consume peekConsume() {
		return this.cur;
	}

	@Override
	public Expression reshapeProduction(Production p) {
		return p.getExpression().reshape(this);
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
	public Expression reshapeEmpty(Empty p) {
		return p;
	}

	@Override
	public Expression reshapeFailure(Failure p) {
		return p;
	}

	@Override
	public Expression reshapeByteChar(ByteChar p) {
		this.cur.append(p.byteChar);
		return p;
	}

	@Override
	public Expression reshapeByteMap(ByteMap p) {
		this.pushConsume(p);
		return p;
	}

	@Override
	public Expression reshapeAnyChar(AnyChar p) {
		this.pushConsume(p);
		return p;
	}

	@Override
	public Expression reshapeSequence(Sequence p) {
		p.getFirst().reshape(this);
		p.getNext().reshape(this);
		return p;
	}

	@Override
	public Expression reshapeChoice(Choice p) {
		for(int i = 0; i < p.size(); i++) {
			int top = this.top;
			this.pushConsume(p, i);
			p.get(i).reshape(this);
			this.popConsumeStack(top);
		}
		if(this.checkPrefix()) {
			System.out.println("Unreachable Choice!!");
		}
		return p;
	}

	public Consume popConsumeStack(int top) {
		Consume c = null;
		while(top < this.top) {
			c = this.popConsume();
		}
		return c;
	}

	public boolean checkPrefix() {
		for(int i = 1; i < this.cur.next.size(); i++) {
			Consume c1 = this.cur.next.get(i);
			for(int j = 0; j < i; j++) {
				Consume c2 = this.cur.next.get(j);
				int size = c2.consumeList.size();
				boolean result = true;
				for(int k = 0; k < c1.consumeList.size(); k++) {
					if(k < size) {
						if(c1.consumeList.get(k) != c2.consumeList.get(k)) {
							result = false;
							break;
						}
					} else {
						result = false;
						break;
					}
				}
				if(result) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Expression reshapeOption(Option p) {
		Expression inner = p.get(0).reshape(this);
		return updateInner(p, inner);
	}

	@Override
	public Expression reshapeRepetition(Repetition p) {
		Expression inner = p.get(0).reshape(this);
		return updateInner(p, inner);
	}

	@Override
	public Expression reshapeRepetition1(Repetition1 p) {
		Expression inner = p.get(0).reshape(this);
		return updateInner(p, inner);
	}

	@Override
	public Expression reshapeAnd(And p) {
		Expression inner = p.get(0).reshape(this);
		return updateInner(p, inner);
	}

	@Override
	public Expression reshapeNot(Not p) {
		Expression inner = p.get(0).reshape(this);
		return updateInner(p, inner);
	}

	@Override
	public Expression reshapeMatch(Match p) {
		Expression inner = p.get(0).reshape(this);
		return updateInner(p, inner);
	}

	@Override
	public Expression reshapeNew(New p) {
		return p;
	}

	@Override
	public Expression reshapeLink(Link p) {
		Expression inner = p.get(0).reshape(this);
		return updateInner(p, inner);
	}

	@Override
	public Expression reshapeTagging(Tagging p) {
		return p;
	}

	@Override
	public Expression reshapeReplace(Replace p) {
		return p;
	}

	@Override
	public Expression reshapeCapture(Capture p) {
		return p;
	}

	@Override
	public Expression reshapeBlock(Block p) {
		Expression inner = p.get(0).reshape(this);
		return updateInner(p, inner);
	}

	@Override
	public Expression reshapeLocalTable(LocalTable p) {
		Expression inner = p.get(0).reshape(this);
		return updateInner(p, inner);
	}

	@Override
	public Expression reshapeDefSymbol(DefSymbol p) {
		Expression inner = p.get(0).reshape(this);
		return updateInner(p, inner);
	}

	@Override
	public Expression reshapeIsSymbol(IsSymbol p) {
		return p;
	}

	@Override
	public Expression reshapeExistsSymbol(ExistsSymbol p) {
		return p;
	}

	@Override
	public Expression reshapeIsIndent(IsIndent p) {
		return p;
	}

}
