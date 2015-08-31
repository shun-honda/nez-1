package nez.vm;

public class MiniNezByteCoder extends ByteCoder {

	public void encodeJumpAddr(int jump) {
		write_u24(jump);
	}
}
