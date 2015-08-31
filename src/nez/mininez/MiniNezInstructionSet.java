package nez.mininez;

public class MiniNezInstructionSet {
	public final static byte Nop = 0; // Do nothing
	public final static byte Fail = 1; // Fail
	public final static byte Alt = 2; // Alt
	public final static byte Succ = 3; // Succ
	public final static byte Jump = 4; // Jump
	public final static byte Call = 5; // Call
	public final static byte Ret = 6; // Ret
	public final static byte Pos = 7; // Pos
	public final static byte Back = 8; // Back
	public final static byte Skip = 9; // Skip

	public final static byte Byte = 10; // match a byte character
	public final static byte Any = 11; // match any
	public final static byte Str = 12; // match string
	public final static byte Set = 13; // match set

	public final static byte Exit = 14; // 7-bit only

	public final static byte Label = 15; // 7-bit

	public static String stringfy(byte opcode) {
		switch(opcode) {
		case Nop:
			return "nop";
		case Fail:
			return "fail";
		case Alt:
			return "alt";
		case Succ:
			return "succ";
		case Jump:
			return "jump";
		case Call:
			return "call";
		case Ret:
			return "ret";
		case Pos:
			return "pos";
		case Back:
			return "back";
		case Skip:
			return "skip";

		case Byte:
			return "byte";
		case Any:
			return "any";
		case Str:
			return "str";
		case Set:
			return "set";

		case Exit:
			return "exit";

		case Label:
			return "label";

		default:
			return "-";
		}
	}
}
