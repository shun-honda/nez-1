package nez.vm;

import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Expression;
import nez.lang.MultiChar;
import nez.lang.Production;
import nez.mininez.BasicBlock;
import nez.mininez.Function;
import nez.mininez.Module;

public class MiniNezIRBuilder {
	Module module;
	Function func;
	BasicBlock curBB;

	public MiniNezIRBuilder(Module m) {
		this.module = m;
	}

	public void setFunction(Function func) {
		this.module.append(func);
		this.func = func;
	}

	public void setInsertPoint(BasicBlock bb) {
		this.func.append(bb);
		bb.setName("bb" + this.func.size());
		if(this.curBB != null) {
			// if(bb.size() != 0) {
			// Instruction last = this.curBB.get(this.curBB.size() - 1);
			// if(!(last.opcode == InstructionSet.Ijump.ordinal() || last.opcode
			// == InstructionSet.Icall.ordinal())) {
			// this.curBB.setSingleSuccessor(bb);
			// }
			// } else {
			// this.curBB.setSingleSuccessor(bb);
			// }
		}
		this.curBB = bb;
	}

	public BasicBlock getCurrentBB() {
		return this.curBB;
	}

	class FailureBB {
		BasicBlock fbb;
		FailureBB prev;

		public FailureBB(BasicBlock bb, FailureBB prev) {
			this.fbb = bb;
			this.prev = prev;
		}
	}

	FailureBB fLabel = null;

	public void pushFailureJumpPoint(BasicBlock bb) {
		this.fLabel = new FailureBB(bb, this.fLabel);
	}

	public BasicBlock popFailureJumpPoint() {
		BasicBlock fbb = this.fLabel.fbb;
		this.fLabel = this.fLabel.prev;
		return fbb;
	}

	public BasicBlock jumpFailureJump() {
		return this.fLabel.fbb;
	}

	public BasicBlock jumpPrevFailureJump() {
		return this.fLabel.prev.fbb;
	}

	public MiniNezIRBuilder createIexit(boolean status) {
		this.curBB.append(new MiniNezIExit(status));
		return this;
	}

	public MiniNezIRBuilder createInop() {
		return this;
	}

	public MiniNezIRBuilder createIfail(Expression e) {
		this.curBB.append(new MiniNezIFail(e));
		return this;
	}

	public MiniNezIRBuilder createIalt(Expression e, BasicBlock fail) {
		this.curBB.append(new MiniNezIAlt(e, fail));
		return this;
	}

	public MiniNezIRBuilder createIsucc(Expression e) {
		this.curBB.append(new MiniNezISucc(e));
		return this;
	}

	public MiniNezIRBuilder createIjump(Expression e, BasicBlock jBB) {
		this.curBB.append(new MiniNezIJump(e, jBB));
		return this;
	}

	public MiniNezIRBuilder createIcall(Production rule) {
		this.curBB.append(new MiniNezICall(rule));
		return this;
	}

	public MiniNezIRBuilder createIret(Production rule) {
		this.curBB.append(new MiniNezIRet(rule));
		return this;
	}

	public MiniNezIRBuilder createIpos(Expression e) {
		this.curBB.append(new MiniNezIPos(e));
		return this;
	}

	public MiniNezIRBuilder createIback(Expression e) {
		this.curBB.append(new MiniNezIBack(e));
		return this;
	}

	public MiniNezIRBuilder createIskip(Expression e, BasicBlock jBB) {
		this.curBB.append(new MiniNezISkip(e, jBB));
		return this;
	}

	public MiniNezIRBuilder createIbyte(ByteChar e) {
		this.curBB.append(new MiniNezIByte(e));
		return this;
	}

	public MiniNezIRBuilder createIany(AnyChar e) {
		this.curBB.append(new MiniNezIAny(e));
		return this;
	}

	public MiniNezIRBuilder createIstr(MultiChar e) {
		this.curBB.append(new MiniNezIStr(e));
		return this;
	}

	public MiniNezIRBuilder createIset(ByteMap e) {
		this.curBB.append(new MiniNezISet(e));
		return this;
	}
}
