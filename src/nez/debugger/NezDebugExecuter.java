package nez.debugger;

public abstract class NezDebugExecuter {
	public abstract boolean exec(Print o);
	public abstract boolean exec(Break o);
	public abstract boolean exec(StepOver o);
	public abstract boolean exec(StepIn o);
	public abstract boolean exec(StepOut o);
	public abstract boolean exec(Continue o);
	public abstract boolean exec(Run o);
	public abstract boolean exec(Exit o);
}
