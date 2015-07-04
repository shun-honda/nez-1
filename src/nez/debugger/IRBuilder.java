package nez.debugger;

import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Expression;
import nez.lang.NonTerminal;

public class IRBuilder {
	private BasicBlock curBB;
	private Module module;
	private Function func;

	public IRBuilder(Module m) {
		this.module = m;
	}

	public Module getModule() {
		return this.module;
	}

	public Function getFunction() {
		return this.func;
	}

	public void setFunction(Function func) {
		this.module.append(func);
		this.func = func;
	}

	public void setInsertPoint(BasicBlock bb) {
		this.func.append(bb);
		bb.setName("B" + this.func.size());
		this.curBB = bb;
	}

	public void setCurrentBB(BasicBlock bb) {
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

	public DebugVMInstruction createIexit(Expression e) {
		return this.curBB.append(new Iexit(e));
	}

	public DebugVMInstruction createIcall(NonTerminal e, BasicBlock jump, BasicBlock failjump) {
		return this.curBB.append(new Icall(e, jump, failjump));
	}

	public DebugVMInstruction createIret(Expression e) {
		return this.curBB.append(new Iret(e));
	}

	public DebugVMInstruction createIjump(Expression e, BasicBlock jump) {
		return this.curBB.append(new Ijump(e, jump));
	}

	public DebugVMInstruction createIiffail(Expression e, BasicBlock jump) {
		return this.curBB.append(new Iiffail(e, jump));
	}

	public DebugVMInstruction createIpush(Expression e) {
		return this.curBB.append(new Ipush(e));
	}

	public DebugVMInstruction createIpop(Expression e) {
		return this.curBB.append(new Ipop(e));
	}

	public DebugVMInstruction createIpeek(Expression e) {
		return this.curBB.append(new Ipeek(e));
	}

	public DebugVMInstruction createIsucc(Expression e) {
		return this.curBB.append(new Isucc(e));
	}

	public DebugVMInstruction createIfail(Expression e) {
		return this.curBB.append(new Ifail(e));
	}

	public DebugVMInstruction createIchar(ByteChar e, BasicBlock jump) {
		return this.curBB.append(new Ichar(e, jump));
	}

	public DebugVMInstruction createIcharclass(ByteMap e, BasicBlock jump) {
		return this.curBB.append(new Icharclass(e, jump));
	}

	public DebugVMInstruction createIany(AnyChar e, BasicBlock jump) {
		return this.curBB.append(new Iany(e, jump));
	}
}