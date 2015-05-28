package nez.vm;

@SuppressWarnings("serial")
public class TerminationException extends Exception {
	boolean status;
	TerminationException(boolean status) {
		super();
		this.status = status;
	}

	public boolean getStatus() {
		return this.status;
	}
}
