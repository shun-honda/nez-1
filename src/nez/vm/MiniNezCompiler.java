package nez.vm;

import java.util.HashMap;

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
import nez.lang.Grammar;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
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
import nez.main.Verbose;
import nez.mininez.BasicBlock;
import nez.mininez.Function;
import nez.mininez.MiniNezInstructionSet;
import nez.mininez.Module;
import nez.util.UList;

public class MiniNezCompiler extends NezCompiler {

	Module module;
	Function startFunc;
	MiniNezIRBuilder builder;
	HashMap<String, BasicBlock> codePointMap = new HashMap<String, BasicBlock>();

	public MiniNezCompiler(NezOption option) {
		super(option);
		this.module = new Module();
		this.builder = new MiniNezIRBuilder(this.module);
	}

	public NezCode compile(Grammar grammar, ByteCoder coder) {
		long t = System.nanoTime();
		UList<Instruction> codeList = new UList<Instruction>(new Instruction[64]);
		Production startProduction = grammar.getStartProduction();
		this.encodeStartProduction(startProduction);
		for(Production p : grammar.getProductionList()) {
			if(p != startProduction) {
				this.encodeProduction(p, null);
			}
		}
		this.labeling(codeList);
		for(Instruction inst : codeList) {
			if(inst.opcode == MiniNezInstructionSet.Call) {
				MiniNezICall callInst = (MiniNezICall) inst;
				callInst.jBB = this.codePointMap.get(((Production) callInst.e).getLocalName());
			}
		}
		long t2 = System.nanoTime();
		Verbose.printElapsedTime("CompilingTime", t, t2);
		if(coder != null) {
			coder.setHeader(codeList.size(), this.module.size(), 0);
			coder.setInstructions(codeList.ArrayValues, codeList.size());
			for(Instruction inst : codeList) {
				System.out.println("[" + inst.id + "]" + inst.toString());
			}
		}
		return new NezCode(codeList.ArrayValues[0], codeList.size(), null);
	}

	private void labeling(UList<Instruction> codeList) {
		int id = 0;
		for(int i = 0; i < this.module.size(); i++) {
			Function func = this.module.get(i);
			this.codePointMap.put(func.getName(), func.get(0));
			for(int j = 0; j < func.size(); j++) {
				BasicBlock bb = func.get(j);
				bb.setCodePoint(id);
				for(int k = 0; k < bb.size(); k++) {
					Instruction inst = bb.get(k);
					inst.id = id++;
					codeList.add(inst);
				}
			}
		}
	}

	protected void encodeStartProduction(Production p) {
		this.startFunc = new Function(p);
		this.builder.setFunction(this.startFunc);
		this.builder.setInsertPoint(new BasicBlock());
		this.builder.createIexit(false);
		this.builder.createIexit(true);
		this.encode(p, null, null);
		this.builder.createIret(p);
	}

	protected void encodeProduction(Production p, Instruction next) {
		Function func = new Function(p);
		this.builder.setFunction(func);
		this.builder.setInsertPoint(new BasicBlock());
		this.encode(p, next, null);
		this.builder.createIret(p);
	}

	@Override
	public Instruction encode(Expression e, Instruction next, Instruction failjump) {
		e.encode(this, next, failjump);
		return null;
	}

	@Override
	public Instruction encodeFail(Expression p) {
		this.builder.createIfail(p);
		return null;
	}

	@Override
	public Instruction encodeAnyChar(AnyChar p, Instruction next, Instruction failjump) {
		this.builder.createIany(p);
		return null;
	}

	@Override
	public Instruction encodeByteChar(ByteChar p, Instruction next, Instruction failjump) {
		this.builder.createIbyte(p);
		return null;
	}

	@Override
	public Instruction encodeByteMap(ByteMap p, Instruction next, Instruction failjump) {
		this.builder.createIset(p);
		return null;
	}

	@Override
	public Instruction encodeMultiChar(MultiChar p, Instruction next, Instruction failjump) {
		this.builder.createIstr(p);
		return null;
	}

	@Override
	public Instruction encodeOption(Option p, Instruction next) {
		BasicBlock fBB = new BasicBlock();
		this.builder.createIalt(p, fBB);
		this.encode(p.get(0), next, null);
		this.builder.createIsucc(p);
		this.builder.setInsertPoint(fBB);
		return null;
	}

	@Override
	public Instruction encodeRepetition(Repetition p, Instruction next) {
		BasicBlock fBB = new BasicBlock();
		BasicBlock topBB = new BasicBlock();
		this.builder.createIalt(p, fBB);
		this.builder.setInsertPoint(topBB);
		this.encode(p.get(0), next, null);
		this.builder.createIskip(p, topBB);
		this.builder.setInsertPoint(fBB);
		return null;
	}

	@Override
	public Instruction encodeRepetition1(Repetition1 p, Instruction next, Instruction failjump) {
		this.encode(p.get(0), next, failjump);
		this.encodeRepetition(p, next);
		return null;
	}

	@Override
	public Instruction encodeAnd(And p, Instruction next, Instruction failjump) {
		BasicBlock fBB = new BasicBlock();
		this.builder.createIalt(p, fBB);
		this.encode(p.get(0), next, failjump);
		this.builder.createIsucc(p);
		this.builder.setInsertPoint(fBB);
		return null;
	}

	@Override
	public Instruction encodeNot(Not p, Instruction next, Instruction failjump) {
		BasicBlock fBB = new BasicBlock();
		this.builder.createIalt(p, fBB);
		this.encode(p.get(0), next, failjump);
		this.builder.createIsucc(p);
		this.builder.createIfail(p);
		this.builder.setInsertPoint(fBB);
		return null;
	}

	@Override
	public Instruction encodeSequence(Sequence p, Instruction next, Instruction failjump) {
		for(int i = 0; i < p.size(); i++) {
			this.encode(p.get(i), next, failjump);
		}
		return null;
	}

	@Override
	public Instruction encodeChoice(Choice p, Instruction next, Instruction failjump) {
		BasicBlock fBB = new BasicBlock();
		BasicBlock endBB = new BasicBlock();
		for(int i = 0; i < p.size(); i++) {
			if(i < p.size() - 1) {
				this.builder.createIalt(p, fBB);
				this.encode(p.get(i), next, failjump);
				this.builder.createIsucc(p);
				this.builder.createIjump(p, endBB);
				this.builder.setInsertPoint(fBB);
				fBB = new BasicBlock();
			} else {
				this.encode(p.get(i), next, failjump);
			}
		}
		this.builder.setInsertPoint(endBB);
		return null;
	}

	@Override
	public Instruction encodeNonTerminal(NonTerminal p, Instruction next, Instruction failjump) {
		Production rule = p.getProduction();
		this.builder.createIcall(rule);
		return null;
	}

	@Override
	public Instruction encodeLink(Link p, Instruction next, Instruction failjump) {
		this.encode(p.get(0), next, failjump);
		return null;
	}

	@Override
	public Instruction encodeNew(New p, Instruction next) {
		return null;
	}

	@Override
	public Instruction encodeCapture(Capture p, Instruction next) {
		return null;
	}

	@Override
	public Instruction encodeTagging(Tagging p, Instruction next) {
		return null;
	}

	@Override
	public Instruction encodeReplace(Replace p, Instruction next) {
		return null;
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
