package nez.vm;

import nez.NezOption;
import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.DefIndent;
import nez.lang.DefSymbol;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.MatchSymbol;
import nez.lang.MultiChar;
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
import nez.util.UList;

public class MiniNezCompiler extends NezCompiler {

	protected final Instruction commonFailure = new IFail(null);

	public MiniNezCompiler(NezOption option) {
		super(option);
	}

	public void labeling(UList<Instruction> codeList, Instruction inst) {
		if (inst == null) {
			return;
		}
		if (inst.id == -1) {
			inst.id = codeList.size();
			codeList.add(inst);
			if (inst.opcode != InstructionSet.Jump) {
				this.labeling(codeList, inst.next);
				if (inst.next != null && inst.id + 1 != inst.next.id) {
					Instruction.labeling(inst.next);
				}
				this.labeling(codeList, inst.branch());
			}
		}
	}

	@Override
	protected void encodeProduction(UList<Instruction> codeList, Production p, Instruction next) {
		String uname = p.getUniqueName();
		ProductionCode pcode = this.pcodeMap.get(uname);
		if (pcode != null) {
			pcode.compiled = this.encode(pcode.localExpression, next, null/* failjump */);
			Instruction block = new ILabel(p, pcode.compiled);
			this.labeling(codeList, block);
		}
	}

	@Override
	public Instruction encode(Expression e, Instruction next, Instruction failjump) {
		return e.encode(this, next, failjump);
	}

	@Override
	public Instruction encodeFail(Expression p) {
		return this.commonFailure;
	}

	@Override
	public Instruction encodeAnyChar(AnyChar p, Instruction next, Instruction failjump) {
		return new IAny(p, next);
	}

	@Override
	public Instruction encodeByteChar(ByteChar p, Instruction next, Instruction failjump) {
		return new IByte(p, next);
	}

	@Override
	public Instruction encodeByteMap(ByteMap p, Instruction next, Instruction failjump) {
		return new ISet(p, next);
	}

	@Override
	public Instruction encodeMultiChar(MultiChar p, Instruction next, Instruction failjump) {
		return new IStr(p, next);
	}

	@Override
	public Instruction encodeOption(Option p, Instruction next) {
		Instruction succ = new ISucc(p, next);
		Instruction inner = this.encode(p.get(0), succ, null);
		return new IAlt(p, next, inner);
	}

	@Override
	public Instruction encodeRepetition(Repetition p, Instruction next) {
		Instruction skip = new ISkip(p);
		Instruction inner = this.encode(p.get(0), skip, next);
		skip.next = inner;
		return new IAlt(p, next, inner);
	}

	@Override
	public Instruction encodeRepetition1(Repetition1 p, Instruction next, Instruction failjump) {
		return this.encode(p.get(0), this.encodeRepetition(p, next), failjump);
	}

	@Override
	public Instruction encodeAnd(And p, Instruction next, Instruction failjump) {
		Instruction inner = this.encode(p.get(0), new ISucc(p, next), failjump);
		return new IAlt(p, next, inner);
	}

	@Override
	public Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
		Instruction fail = new ISucc(p, new IFail(p));
		return new IAlt(p, next, this.encode(p.get(0), fail, failjump));
	}

	@Override
	public Instruction encodeSequence(Sequence p, Instruction next, Instruction failjump) {
		Instruction nextStart = next;
		for (int i = p.size() - 1; i >= 0; i--) {
			Expression e = p.get(i);
			nextStart = encode(e, nextStart, failjump);
		}
		return nextStart;
	}

	@Override
	public Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		Instruction nextChoice = this.encode(p.get(p.size() - 1), next, failjump);
		for (int i = p.size() - 2; i >= 0; i--) {
			Expression e = p.get(i);
			nextChoice = new IAlt(e, nextChoice, this.encode(e, new ISucc(e, new IJump(p, next)), nextChoice));
		}
		return nextChoice;
	}

	@Override
	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production rule = p.getProduction();
		return new ICall(rule, next);
	}

	@Override
	public Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		return this.encode(p.get(0), next, failjump);
	}

	@Override
	public Instruction encodeNew(New p, Instruction next) {
		return next;
	}

	@Override
	public Instruction encodeCapture(Capture p, Instruction next) {
		return next;
	}

	@Override
	public Instruction encodeTagging(Tagging p, Instruction next) {
		return next;
	}

	@Override
	public Instruction encodeReplace(Replace p, Instruction next) {
		return next;
	}

	@Override
	public Instruction encodeBlock(Block p, Instruction next, Instruction failjump) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Instruction encodeDefSymbol(DefSymbol p, Instruction next, Instruction failjump) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Instruction encodeMatchSymbol(MatchSymbol p, Instruction next, Instruction failjump) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Instruction encodeIsSymbol(IsSymbol p, Instruction next, Instruction failjump) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Instruction encodeDefIndent(DefIndent p, Instruction next, Instruction failjump) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Instruction encodeIsIndent(IsIndent p, Instruction next, Instruction failjump) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Instruction encodeExistsSymbol(ExistsSymbol existsSymbol, Instruction next, Instruction failjump) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Instruction encodeLocalTable(LocalTable localTable, Instruction next, Instruction failjump) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Instruction encodeExtension(Expression p, Instruction next, Instruction failjump) {
		// TODO Auto-generated method stub
		return null;
	}

}
