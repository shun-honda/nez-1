package nez.vm;

import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Expression;
import nez.lang.MultiChar;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.mininez.BasicBlock;
import nez.mininez.MiniNezInstructionSet;
import nez.util.StringUtils;
import nez.vm.RuntimeContext.StackData;

public abstract class MiniNezInstruction extends Instruction {

	public MiniNezInstruction(byte opcode, Expression e) {
		super(opcode, e, null);
	}

}

class MiniNezIFail extends MiniNezInstruction {
	MiniNezIFail(Expression e) {
		super(MiniNezInstructionSet.Fail, e);
	}

	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		return sc.fail();
	}
}

class MiniNezIAlt extends MiniNezInstruction {
	public final BasicBlock fBB;

	MiniNezIAlt(Expression e, BasicBlock fBB) {
		super(MiniNezInstructionSet.Alt, e);
		this.fBB = fBB;
	}

	// @Override
	// Instruction branch() {
	// return this.fBB;
	// }

	@Override
	protected String getOperand() {
		return this.fBB.getName() + "  ## " + e;
	}

	@Override
	void encodeA(ByteCoder c) {
		// c.encodeJumpAddr(this.failjump);
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		// sc.pushAlt(this.failjump);
		return this.next;
	}
}

class MiniNezISucc extends MiniNezInstruction {
	MiniNezISucc(Expression e) {
		super(MiniNezInstructionSet.Succ, e);
	}

	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.popAlt();
		return this.next;
	}
}

class MiniNezISkip extends MiniNezInstruction {
	BasicBlock jBB;

	MiniNezISkip(Expression e, BasicBlock jBB) {
		super(MiniNezInstructionSet.Skip, e);
		this.jBB = jBB;
	}

	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		return sc.skip(this.next);
	}
}

class MiniNezIJump extends MiniNezInstruction {
	BasicBlock jBB;

	MiniNezIJump(Expression e, BasicBlock jBB) {
		super(MiniNezInstructionSet.Jump, e);
		this.jBB = jBB;
	}

	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		return sc.skip(this.next);
	}
}

class MiniNezIPos extends MiniNezInstruction {
	MiniNezIPos(Expression e) {
		super(MiniNezInstructionSet.Pos, e);
	}

	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.value = sc.getPosition();
		return this.next;
	}
}

class MiniNezIBack extends MiniNezInstruction {
	public MiniNezIBack(Expression e) {
		super(MiniNezInstructionSet.Back, e);
	}

	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		sc.setPosition(s.value);
		return this.next;
	}
}

class MiniNezILabel extends MiniNezInstruction {
	Production rule;

	MiniNezILabel(Production rule) {
		super(MiniNezInstructionSet.Label, rule);
		this.rule = rule;
	}

	@Override
	protected String getOperand() {
		return rule.getLocalName();
	}

	@Override
	void encodeA(ByteCoder c) {
		c.encodeNonTerminal(rule.getLocalName());
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		return this.next;
	}
}

class MiniNezICall extends MiniNezInstruction {
	Production rule;
	NonTerminal ne;
	BasicBlock jBB;

	MiniNezICall(Production rule) {
		super(MiniNezInstructionSet.Call, rule);
		this.rule = rule;
	}

	@Override
	protected String getOperand() {
		return jBB.getName();
	}

	@Override
	void encodeA(ByteCoder c) {
		// c.encodeJumpAddr(this.jump);
		c.encodeNonTerminal(rule.getLocalName()); // debug information
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		// s.ref = this.jump;
		return this.next;
	}
}

class MiniNezIRet extends MiniNezInstruction {
	MiniNezIRet(Production e) {
		super(MiniNezInstructionSet.Ret, e);
	}

	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		return (Instruction) s.ref;
	}
}

class MiniNezIExit extends MiniNezInstruction {
	boolean status;

	MiniNezIExit(boolean status) {
		super(MiniNezInstructionSet.Exit, null);
		this.status = status;
	}

	@Override
	void encodeA(ByteCoder c) {
		c.write_b(status);
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		throw new TerminationException(status);
	}
}

abstract class MiniNezAbstractByteInstruction extends MiniNezInstruction {
	public final int byteChar;

	MiniNezAbstractByteInstruction(byte bytecode, ByteChar e) {
		super(bytecode, e);
		this.byteChar = e.byteChar;
	}

	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharacter(byteChar);
	}

	@Override
	void encodeA(ByteCoder c) {
		c.encodeByteChar(byteChar);
	}
}

class MiniNezIByte extends MiniNezAbstractByteInstruction {
	MiniNezIByte(ByteChar e) {
		super(MiniNezInstructionSet.Byte, e);
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		if(sc.byteAt(sc.getPosition()) == this.byteChar) {
			sc.consume(1);
			return this.next;
		}
		return sc.fail();
	}
}

abstract class MiniNezAbstractAnyInstruction extends MiniNezInstruction {
	MiniNezAbstractAnyInstruction(byte opcode, Expression e) {
		super(opcode, e);
	}

	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
}

class MiniNezIAny extends MiniNezAbstractAnyInstruction {
	MiniNezIAny(Expression e) {
		super(InstructionSet.Any, e);
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		if(sc.hasUnconsumed()) {
			sc.consume(1);
			return this.next;
		}
		return sc.fail();
	}
}

abstract class MiniNezAbstractSetInstruction extends MiniNezInstruction {
	public final boolean[] byteMap;

	MiniNezAbstractSetInstruction(byte opcode, ByteMap e) {
		super(opcode, e);
		this.byteMap = e.byteMap;
	}

	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharacterClass(byteMap);
	}

	@Override
	void encodeA(ByteCoder c) {
		c.encodeByteMap(byteMap);
	}
}

class MiniNezISet extends MiniNezAbstractSetInstruction {
	MiniNezISet(ByteMap e) {
		super(InstructionSet.Set, e);
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		int byteChar = sc.byteAt(sc.getPosition());
		if(byteMap[byteChar]) {
			sc.consume(1);
			return this.next;
		}
		return sc.fail();
	}
}

abstract class MiniNezAbstractStrInstruction extends MiniNezInstruction {
	final byte[] utf8;

	public MiniNezAbstractStrInstruction(byte opcode, MultiChar e, byte[] utf8) {
		super(opcode, e);
		this.utf8 = utf8;
	}

	@Override
	protected String getOperand() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < utf8.length; i++) {
			if(i > 0) {
				sb.append(" ");
			}
			sb.append(StringUtils.stringfyCharacter(utf8[i] & 0xff));
		}
		return sb.toString();
	}

	@Override
	void encodeA(ByteCoder c) {
		c.encodeMultiByte(utf8);
	}
}

class MiniNezIStr extends MiniNezAbstractStrInstruction {
	public MiniNezIStr(MultiChar e) {
		super(InstructionSet.Str, e, e.byteSeq);
	}

	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		if(sc.match(sc.getPosition(), this.utf8)) {
			sc.consume(utf8.length);
			return this.next;
		}
		return sc.fail();
	}
}