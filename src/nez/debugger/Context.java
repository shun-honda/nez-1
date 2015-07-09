package nez.debugger;

import nez.ast.CommonTreeTransducer;
import nez.ast.Source;
import nez.ast.Tag;
import nez.ast.TreeTransducer;
import nez.vm.SymbolTable;

public abstract class Context implements Source {
	long pos;
	long longest_pos;
	boolean result;
	StackEntry[] stack = null;
	StackEntry[] callStack = null;
	StackEntry[] longestTrace = null;
	int StackTop;
	int callStackTop;
	int longestStackTop;
	private static int StackSize = 128;

	public final void initContext() {
		this.result = true;
		this.lastAppendedLog = new ASTLog();
		this.symbolTable = new SymbolTable();
		this.stack = new StackEntry[StackSize];
		this.callStack = new StackEntry[StackSize];
		for(int i = 0; i < this.stack.length; i++) {
			this.stack[i] = new StackEntry();
			this.callStack[i] = new StackEntry();
		}
		this.callStack[0].jump = new Iexit(null);
		this.callStack[0].failjump = new Iexit(null);
		this.stack[0].mark = this.lastAppendedLog;
		this.StackTop = 0;
		this.callStackTop = 0;
		this.treeTransducer = new CommonTreeTransducer();
	}

	public final long getPosition() {
		return this.pos;
	}

	final void setPosition(long pos) {
		this.pos = pos;
	}

	public boolean hasUnconsumed() {
		return this.pos != length();
	}

	public final boolean consume(int length) {
		this.pos += length;
		return true;
	}

	public final void rollback(long pos) {
		if(this.longest_pos < this.pos) {
			this.longest_pos = this.pos;
			this.longestTrace = this.callStack.clone();
			this.longestStackTop = this.callStackTop;
		}
		this.pos = pos;
	}

	public final StackEntry newStackEntry() {
		this.StackTop++;
		if(this.StackTop == this.stack.length) {
			StackEntry[] newStack = new StackEntry[this.stack.length * 2];
			System.arraycopy(this.stack, 0, newStack, 0, stack.length);
			for(int i = this.stack.length; i < newStack.length; i++) {
				newStack[i] = new StackEntry();
			}
			this.stack = newStack;
		}
		return this.stack[this.StackTop];
	}

	public final StackEntry newCallStackEntry() {
		this.callStackTop++;
		if(this.callStackTop == this.callStack.length) {
			StackEntry[] newStack = new StackEntry[this.callStack.length * 2];
			System.arraycopy(this.callStack, 0, newStack, 0, callStack.length);
			for(int i = this.callStack.length; i < newStack.length; i++) {
				newStack[i] = new StackEntry();
			}
			this.callStack = newStack;
		}
		return this.callStack[this.callStackTop];
	}

	public final StackEntry popStack() {
		return this.stack[this.StackTop--];
	}

	public final StackEntry popCallStack() {
		return this.callStack[this.callStackTop--];
	}

	public final StackEntry peekStack() {
		return this.stack[this.StackTop];
	}

	public final String getSyntaxErrorMessage() {
		return this.formatPositionLine("error", this.longest_pos, "syntax error");
	}

	public final String getUnconsumedMessage() {
		return this.formatPositionLine("unconsumed", this.pos, "");
	}

	public final DebugVMInstruction opIexit(Iexit inst) throws MachineExitException {
		throw new MachineExitException(result);
	}

	public final DebugVMInstruction opIcall(Icall inst) {
		StackEntry top = this.newCallStackEntry();
		top.jump = inst.jump;
		top.failjump = inst.failjump;
		top.val = inst.ne;
		top.pos = this.pos;
		return inst.next;
	}

	public final DebugVMInstruction opIret(Iret inst) {
		StackEntry top = this.popCallStack();
		if(this.result) {
			return top.jump;
		}
		return top.failjump;
	}

	public final DebugVMInstruction opIjump(Ijump inst) {
		return inst.jump;
	}

	public final DebugVMInstruction opIiffail(Iiffail inst) {
		if(this.result) {
			return inst.next;
		}
		return inst.jump;
	}

	public final DebugVMInstruction opIpush(Ipush inst) {
		StackEntry top = this.newStackEntry();
		top.pos = this.pos;
		top.mark = this.lastAppendedLog;
		return inst.next;
	}

	public final DebugVMInstruction opIpop(Ipop inst) {
		this.popStack();
		return inst.next;
	}

	public final DebugVMInstruction opIpeek(Ipeek inst) {
		StackEntry top = this.peekStack();
		rollback(top.pos);
		if(top.mark != this.lastAppendedLog && this.result == false) {
			this.logAbort(top.mark, true);
		}
		return inst.next;
	}

	public final DebugVMInstruction opIsucc(Isucc inst) {
		this.result = true;
		return inst.next;
	}

	public final DebugVMInstruction opIfail(Ifail inst) {
		this.result = false;
		return inst.next;
	}

	public final DebugVMInstruction opIchar(Ichar inst) {
		if(this.byteAt(this.pos) == inst.byteChar) {
			this.consume(1);
			return inst.next;
		}
		this.result = false;
		return inst.jump;
	}

	public final DebugVMInstruction opIcharclass(Icharclass inst) {
		int byteChar = this.byteAt(this.pos);
		if(inst.byteMap[byteChar]) {
			this.consume(1);
			return inst.next;
		}
		this.result = false;
		return inst.jump;
	}

	public final DebugVMInstruction opIany(Iany inst) {
		if(hasUnconsumed()) {
			this.consume(1);
			return inst.next;
		}
		this.result = false;
		return inst.jump;
	}

	/*
	 * AST Construction Part
	 */

	private TreeTransducer treeTransducer;
	private Object left;
	private ASTLog lastAppendedLog = null;
	private ASTLog unusedDataLog = null;

	public Object getLeftObject() {
		return this.left;
	}

	private final void pushDataLog(int type, long pos, Object value) {
		ASTLog l;
		if(this.unusedDataLog == null) {
			l = new ASTLog();
		} else {
			l = this.unusedDataLog;
			this.unusedDataLog = l.next;
		}
		l.type = type;
		l.pos = pos;
		l.value = value;
		l.prev = lastAppendedLog;
		l.next = null;
		lastAppendedLog.next = l;
		lastAppendedLog = l;
	}

	public final Object logCommit(ASTLog start) {
		assert(start.type == ASTLog.LazyNew);
		long spos = start.pos, epos = spos;
		Tag tag = null;
		Object value = null;
		int objectSize = 0;
		Object left = null;
		for(ASTLog cur = start.next; cur != null; cur = cur.next) {
			switch(cur.type) {
			case ASTLog.LazyLink:
				int index = (int) cur.pos;
				if(index == -1) {
					cur.pos = objectSize;
					objectSize++;
				} else if(!(index < objectSize)) {
					objectSize = index + 1;
				}
				break;
			case ASTLog.LazyCapture:
				epos = cur.pos;
				break;
			case ASTLog.LazyTag:
				tag = (Tag) cur.value;
				break;
			case ASTLog.LazyReplace:
				value = cur.value;
				break;
			case ASTLog.LazyLeftNew:
				left = commitNode(start, cur, spos, epos, objectSize, left, tag, value);
				start = cur;
				spos = cur.pos;
				epos = spos;
				tag = null;
				value = null;
				objectSize = 1;
				break;
			}
		}
		return commitNode(start, null, spos, epos, objectSize, left, tag, value);
	}

	private Object commitNode(ASTLog start, ASTLog end, long spos, long epos, int objectSize, Object left, Tag tag,
			Object value) {
		Object newnode = this.treeTransducer.newNode(tag, this, spos, epos, objectSize, value);
		if(left != null) {
			this.treeTransducer.link(newnode, 0, left);
		}
		if(objectSize > 0) {
			for(ASTLog cur = start.next; cur != end; cur = cur.next) {
				if(cur.type == ASTLog.LazyLink) {
					// System.out.println("Link >> " + cur);
					this.treeTransducer.link(newnode, (int) cur.pos, cur.value);
				}
			}
		}
		// System.out.println("Commit >> " + newnode);
		return this.treeTransducer.commit(newnode);
	}

	public final void logAbort(ASTLog checkPoint, boolean isFail) {
		assert(checkPoint != null);
		// System.out.println("Abort >> " + checkPoint);
		this.lastAppendedLog.next = this.unusedDataLog;
		this.unusedDataLog = checkPoint.next;
		this.unusedDataLog.prev = null;
		this.lastAppendedLog = checkPoint;
		this.lastAppendedLog.next = null;
	}

	public final Object newTopLevelNode() {
		for(ASTLog cur = this.lastAppendedLog; cur != null; cur = cur.prev) {
			if(cur.type == ASTLog.LazyNew) {
				this.left = logCommit(cur);
				logAbort(cur.prev, false);
				return this.left;
			}
		}
		return null;
	}

	public final DebugVMInstruction opInew(Inew inst) {
		this.pushDataLog(ASTLog.LazyNew, this.pos, null);
		return inst.next;
	}

	public final DebugVMInstruction opIleftnew(Ileftnew inst) {
		this.pushDataLog(ASTLog.LazyLeftNew, this.pos + inst.index, null);
		return inst.next;
	}

	public final DebugVMInstruction opIcapture(Icapture inst) {
		this.pushDataLog(ASTLog.LazyCapture, this.pos, null);
		return inst.next;
	}

	public final DebugVMInstruction opImark(Imark inst) {
		StackEntry top = this.newStackEntry();
		top.mark = this.lastAppendedLog;
		return inst.next;
	}

	public final DebugVMInstruction opItag(Itag inst) {
		this.pushDataLog(ASTLog.LazyTag, 0, inst.tag);
		return inst.next;
	}

	public final DebugVMInstruction opIreplace(Ireplace inst) {
		this.pushDataLog(ASTLog.LazyReplace, 0, inst.value);
		return inst.next;
	}

	public final DebugVMInstruction opIcommit(Icommit inst) {
		StackEntry top = this.popStack();
		if(top.mark.next != null) {
			Object child = this.logCommit(top.mark.next);
			this.logAbort(top.mark, false);
			if(child != null) {
				this.pushDataLog(ASTLog.LazyLink, inst.index, child);
			}
			this.left = child;
		}
		return inst.next;
	}

	public final DebugVMInstruction opIabort(Iabort inst) {
		StackEntry top = this.popStack();
		if(top.mark != this.lastAppendedLog) {
			this.logAbort(top.mark, true);
		}
		return inst.next;
	}

	/*
	 * Symbol Table Part
	 */

	private SymbolTable symbolTable;

	public final DebugVMInstruction opIdef(Idef inst) {
		StackEntry top = this.popStack();
		byte[] captured = this.subbyte(top.pos, this.pos);
		this.symbolTable.addTable(inst.tableName, captured);
		return inst.next;
	}

	public final DebugVMInstruction opIis(Iis inst) {
		byte[] t = this.symbolTable.getSymbol(inst.tableName);
		if(t != null && this.match(this.pos, t)) {
			this.consume(t.length);
			return inst.next;
		}
		return inst.jump;
	}

	public final DebugVMInstruction opIisa(Iisa inst) {
		StackEntry top = this.popStack();
		byte[] captured = this.subbyte(top.pos, this.pos);
		if(this.symbolTable.contains2(inst.tableName, captured)) {
			this.consume(captured.length);
			return inst.next;
		}
		return inst.jump;
	}

	public final DebugVMInstruction opIexists(Iexists inst) {
		byte[] t = this.symbolTable.getSymbol(inst.tableName);
		return t != null ? inst.next : inst.jump;
	}

	public final DebugVMInstruction opIbeginscope(Ibeginscope inst) {
		StackEntry top = this.newStackEntry();
		top.pos = this.symbolTable.savePoint();
		return inst.next;
	}

	public final DebugVMInstruction opIbeginlocalscope(Ibeginlocalscope inst) {
		StackEntry top = this.newStackEntry();
		top.pos = this.symbolTable.saveHiddenPoint(inst.tableName);
		return inst.next;
	}

	public final DebugVMInstruction opIendscope(Iendscope inst) {
		StackEntry top = this.popStack();
		this.symbolTable.rollBack((int) top.pos);
		return inst.next;
	}

}

class ASTLog {
	final static int LazyLink = 0;
	final static int LazyCapture = 1;
	final static int LazyTag = 2;
	final static int LazyReplace = 3;
	final static int LazyLeftNew = 4;
	final static int LazyNew = 5;

	int type;
	long pos;
	Object value;
	ASTLog prev;
	ASTLog next;

	int id() {
		if(prev == null) {
			return 0;
		}
		return prev.id() + 1;
	}

	@Override
	public String toString() {
		switch(type) {
		case LazyLink:
			return "[" + id() + "] link<" + this.pos + "," + this.value + ">";
		case LazyCapture:
			return "[" + id() + "] cap<pos=" + this.pos + ">";
		case LazyTag:
			return "[" + id() + "] tag<" + this.value + ">";
		case LazyReplace:
			return "[" + id() + "] replace<" + this.value + ">";
		case LazyNew:
			return "[" + id() + "] new<pos=" + this.pos + ">" + "   ## " + this.value;
		case LazyLeftNew:
			return "[" + id() + "] leftnew<pos=" + this.pos + "," + this.value + ">";
		}
		return "[" + id() + "] nop";
	}
}

class StackEntry {
	DebugVMInstruction jump;
	DebugVMInstruction failjump;
	long pos;
	ASTLog mark;
	Object val;
}
