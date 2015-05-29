package nez.vm;

public abstract class NezDebugExecuter {
	public abstract boolean exec(Print o);
	public abstract boolean exec(Break o);
	public abstract boolean exec(StepOver o) throws TerminationException;
	public abstract boolean exec(StepIn o) throws TerminationException;
	public abstract boolean exec(StepOut o) throws TerminationException;
	public abstract boolean exec(Continue o) throws TerminationException;
	public abstract boolean exec(Run o) throws TerminationException;
	public abstract boolean exec(Exit o);
}
