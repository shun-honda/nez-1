package nez.mininez;

import java.util.ArrayList;
import java.util.List;

import nez.vm.Instruction;

public class BasicBlock {
	String name;
	int codePoint;
	List<Instruction> insts;
	List<BasicBlock> preds;
	List<BasicBlock> succs;

	public BasicBlock() {
		this.insts = new ArrayList<Instruction>();
		this.preds = new ArrayList<BasicBlock>();
		this.succs = new ArrayList<BasicBlock>();
	}

	public Instruction get(int index) {
		return this.insts.get(index);
	}

	public Instruction getStartInstruction() {
		BasicBlock bb = this;
		while(bb.size() == 0) {
			bb = bb.getSingleSuccessor();
		}
		return bb.get(0);
	}

	public Instruction append(Instruction inst) {
		this.insts.add(inst);
		return inst;
	}

	public BasicBlock add(int index, Instruction inst) {
		this.insts.add(index, inst);
		return this;
	}

	public Instruction remove(int index) {
		return this.insts.remove(index);
	}

	public int size() {
		return this.insts.size();
	}

	public int indexOf(Instruction inst) {
		return this.insts.indexOf(inst);
	}

	public void stringfy(StringBuilder sb) {
		// TODO
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<BasicBlock> getPredecessors() {
		return this.preds;
	}

	public List<BasicBlock> getSuccessors() {
		return this.succs;
	}

	public BasicBlock getSingleSuccessor() {
		return this.succs.get(0);
	}

	public BasicBlock getFailSuccessor() {
		return this.succs.get(1);
	}

	public void setSingleSuccessor(BasicBlock bb) {
		this.succs.add(0, bb);
	}

	public void setFailSuccessor(BasicBlock bb) {
		this.succs.add(1, bb);
	}
}
