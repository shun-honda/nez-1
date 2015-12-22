package nez.tool.parser;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.Production;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.lang.expr.Xsymbol;
import nez.parser.MemoPoint;
import nez.parser.ParseFunc;
import nez.parser.ParserGrammar;
import nez.util.StringUtils;
import nez.util.Verbose;

public class CParserGenerator extends ParserGrammarSourceGenerator {

	ParserGrammar gg;

	@Override
	protected String getFileExtension() {
		return "c";
	}

	private final CParserGenerator Include(String path) {
		L("#include \"" + path + "\"");
		return this;
	}

	private final CParserGenerator Define(String macro) {
		L("#define " + macro);
		return this;
	}

	private final CParserGenerator Comment(String comment) {
		L("// " + comment);
		return this;
	}

	private final CParserGenerator Begin() {
		L("{");
		file.incIndent();
		return this;
	}

	private final CParserGenerator End() {
		file.decIndent();
		L("}");
		return this;
	}

	protected CParserGenerator N() {
		this.file.writeNewLine();
		return this;
	}

	private final CParserGenerator Prototype(String type, String name, String... args) {
		L(type + " " + name + "(");
		for (int i = 0; i < args.length; i++) {
			W(args[i]);
		}
		W(");");
		return this;
	}

	private final CParserGenerator Func(String type, String name, String... args) {
		L(type + " " + name + "(");
		for (int i = 0; i < args.length; i++) {
			W(args[i]);
			if (i < args.length - 1) {
				W(", ");
			}
		}
		W(")");
		return this;
	}

	private final CParserGenerator FuncCall(String name, String... args) {
		L(name + "(");
		for (int i = 0; i < args.length; i++) {
			W(args[i]);
			if (i < args.length - 1) {
				W(", ");
			}
		}
		W(");");
		return this;
	}

	private final String _FuncCall(String name, String... args) {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append("(");
		for (int i = 0; i < args.length; i++) {
			sb.append(args[i]);
			if (i < args.length - 1) {
				sb.append(", ");
			}
		}
		sb.append(")");
		return sb.toString();
	}

	private final CParserGenerator Let(String type, String name, String expr) {
		L(type + " " + name + " = " + expr + ";");
		return this;
	}

	private final CParserGenerator Let(String name, String expr) {
		L(name + " = " + expr + ";");
		return this;
	}

	private final CParserGenerator If(String cond) {
		L("if(" + cond + ")");
		return this;
	}

	private final CParserGenerator While(String cond) {
		L("while(" + cond + ")");
		return this;
	}

	private final CParserGenerator Break() {
		L("break;");
		return this;
	}

	private final String _Equals(String left, String right) {
		return left + " == " + right;
	}

	private final String _NotEquals(String left, String right) {
		return left + " != " + right;
	}

	private final String _Not(String expr) {
		return "!(" + expr + ")";
	}

	private final String _Mul(String left, String right) {
		return left + " * " + right;
	}

	private final CParserGenerator Consume() {
		L("ctx->cur++;");
		return this;
	}

	private final CParserGenerator Consume(String len) {
		L("ctx->cur += " + len + ";");
		return this;
	}

	private int memoId = 0;

	private final CParserGenerator Lookup(MemoPoint memo, NonTerminal n, String label) {
		String name = "entry" + memoId++;
		L("MemoEntry_t *" + name + " = memo_get(ctx->memo, ctx->cur, " + memo.id + ", 0);");
		L("if(" + name + " != NULL)");
		Begin("{");
		{
			L("if(" + name + "->failed == MEMO_ENTRY_FAILED)");
			Begin();
			{
				jumpFailureJump();
			}
			End();
			L("else ");
			Begin();
			L("ctx->cur += " + name + "->consumed;");
			gotoLabel(label);
			End();
		}
		End("}");
		return this;
	}

	private final CParserGenerator Memo(MemoPoint memo, String pos, String len) {
		FuncCall("memo_set", "ctx->memo", pos, String.valueOf(memo.id), "NULL", len, "0");
		return this;
	}

	private final CParserGenerator TLookup(MemoPoint memo, NonTerminal n, String label) { // FIXME
		String name = "entry" + memoId++;
		L("MemoEntry_t *" + name + " = memo_get(ctx->memo, ctx->cur, " + memo.id + ", 0);");
		L("if(" + name + " != NULL)");
		Begin("{");
		{
			L("if(" + name + "->failed == MEMO_ENTRY_FAILED)");
			Begin();
			{
				jumpFailureJump();
			}
			End();
			L("else ");
			Begin();
			{
				// String tag = n.getLocalName();
				// tag = StringUtils.quoteString('"', tag, '"');
				Symbol sym = Symbol.NullSymbol;
				L("ast_log_link(ctx->ast, " + sym.id() + ", " + name + "->result);");
			}
			L("ctx->cur += " + name + "->consumed;");
			gotoLabel(label);
			End();
		}
		End("}");
		return this;
	}

	private CParserGenerator TMemo(MemoPoint memo, String pos, String node, String len) {
		FuncCall("memo_set", "ctx->memo", pos, String.valueOf(memo.id), node, len, "0");
		return this;
	}

	private CParserGenerator MemoFail(MemoPoint memo) {
		FuncCall("memo_fail", "ctx->memo", "ctx->cur", String.valueOf(memo.id));
		return this;
	}

	@Override
	public void makeHeader(ParserGrammar gg) {
		this.gg = gg;
		final String __FILE__ = new Throwable().getStackTrace()[1].getFileName();
		Comment("This file is auto generated by nez.jar");
		Comment("If you want to fix something, you must edit " + __FILE__);
		L();
		Include("cnez.h");
		for (Production r : gg.getProductionList()) {
			if (!r.getLocalName().startsWith("\"")) {
				Prototype("int", "p" + name(r.getLocalName()), "ParsingContext ctx");
			}
		}
		Prototype("void", "init_set", "ParsingContext ctx");
		Prototype("void", "init_str", "ParsingContext ctx");
		Prototype("void", "init_tables", "ParsingContext ctx");
		Prototype("void", "init_jump_tables", "ParsingContext ctx");
		L();
	}

	boolean createAST = false;

	@Override
	public void makeFooter(ParserGrammar gg) {
		int flagTableSize = 0 /* this.flagTable.size() */;
		int prodSize = gg.getProductionList().size();
		// L("#define CNEZ_FLAG_TABLE_SIZE " + flagTableSize);
		Define("CNEZ_FLAG_TABLE_SIZE " + flagTableSize);
		// L("#define CNEZ_MEMO_SIZE " + 0 /* this.memoId */);
		if (gg.memoPointList != null) {
			Define("CNEZ_MEMO_SIZE       " + gg.memoPointList.size());
		} else {
			Define("CNEZ_MEMO_SIZE       0");
		}
		// L("#define CNEZ_GRAMMAR_URN \"" + urn + "\"");
		// L("#define CNEZ_PRODUCTION_SIZE " + prodSize);
		Define("CNEZ_PRODUCTION_SIZE " + prodSize);
		Define("CNEZ_SET_SIZE " + manager.setId);
		Define("CNEZ_STR_SIZE " + manager.strId);
		if (this.strategy.TreeConstruction && createAST) {
			// L("#define CNEZ_ENABLE_AST_CONSTRUCTION 1");
			Define("CNEZ_ENABLE_AST_CONSTRUCTION 1");
		}
		Define("CNEZ_JTABLE1_SIZE " + manager.jumpTable1Id);
		Define("CNEZ_JTABLE2_SIZE " + manager.jumpTable2Id);
		Define("CNEZ_JTABLE3_SIZE " + manager.jumpTable3Id);
		// L("#include \"cnez_main.c\"");
		L("const char* global_tag_list[] = {");
		for (int i = 0; i < tagList.size(); i++) {
			W("\"" + tagList.get(i).getSymbol() + "\"");
			if (i < tagList.size() - 1) {
				W(", ");
			}
		}
		W("};");
		Include("cnez_main.c");
		Func("void", "init_set", "ParsingContext ctx").Begin();
		Let("ctx->sets", _FuncCall("malloc", _Mul(_FuncCall("sizeof", "bitset_t"), "CNEZ_SET_SIZE"))).N();
		W(manager.setBuilder.toString());
		End();
		Func("void", "init_str", "ParsingContext ctx").Begin();
		Let("ctx->strs", _FuncCall("malloc", _Mul(_FuncCall("sizeof", "const char *"), "CNEZ_STR_SIZE"))).N();
		W(manager.strBuilder.toString());
		End();
		Func("void", "init_tables", "ParsingContext ctx").Begin();
		Object[] tables = tableMap.keySet().toArray();
		L("ctx->tables = (const char **)VM_MALLOC(sizeof(const char *) * " + tables.length + ");");
		for (int i = 0; i < tables.length; i++) {
			L("table_init(ctx, \"" + tables[i] + "\", " + i + ");");
		}
		End();
		Func("void", "init_jump_tables", "ParsingContext ctx").Begin();
		L("ctx->jumps1 = (jump_table1_t *)VM_MALLOC(sizeof(jump_table1_t) * CNEZ_JTABLE1_SIZE);");
		L("ctx->jumps2 = (jump_table2_t *)VM_MALLOC(sizeof(jump_table2_t) * CNEZ_JTABLE2_SIZE);");
		L("ctx->jumps3 = (jump_table3_t *)VM_MALLOC(sizeof(jump_table3_t) * CNEZ_JTABLE3_SIZE);");
		W(manager.jumpTableBuilder.toString());
		End();
	}

	private HashMap<String, ParseFunc> funcMap = null;

	protected ParseFunc getParseFunc(Production p) {
		if (gg != null) {
			ParseFunc f = gg.getParseFunc(p.getLocalName());
			if (f == null) {
				f = gg.getParseFunc(p.getUniqueName());
			}
			if (f == null) {
				Verbose.debug("unfound parsefunc: " + p.getLocalName() + " " + p.getUniqueName());
			}
			return f;
		}
		if (this.funcMap != null) {
			return funcMap.get(p.getUniqueName());
		}
		return null;
	}

	/* Failure handling */

	int fid = 0;

	class FailurePoint {
		int id;
		FailurePoint prev;

		public FailurePoint(int label, FailurePoint prev) {
			this.id = label;
			this.prev = prev;
		}
	}

	FailurePoint fLabel;

	private CParserGenerator initFalureJumpPoint() {
		this.fid = 0;
		this.fLabel = null;
		return this;
	}

	private CParserGenerator pushFailureJumpPoint() {
		this.fLabel = new FailurePoint(this.fid++, this.fLabel);
		return this;
	}

	private CParserGenerator popFailureJumpPoint(Production r) {
		Label("CATCH_FAILURE" + this.fLabel.id);
		this.fLabel = this.fLabel.prev;
		return this;
	}

	private CParserGenerator popFailureJumpPoint(Expression e) {
		Label("CATCH_FAILURE" + this.fLabel.id);
		this.fLabel = this.fLabel.prev;
		return this;
	}

	private CParserGenerator jumpFailureJump() {
		return gotoLabel("CATCH_FAILURE" + this.fLabel.id);
	}

	private CParserGenerator jumpPrevFailureJump() {
		return gotoLabel("CATCH_FAILURE" + this.fLabel.prev.id);
	}

	private CParserGenerator gotoLabel(String label) {
		L("goto " + label + ";");
		return this;
	}

	private CParserGenerator Label(String label) {
		N();
		W(label + ": ;");
		return this;
	}

	/* Id Manager */
	class PoolManager {
		int setId = 0;
		int strId = 0;
		int jumpTable1Id = 0;
		int jumpTable2Id = 0;
		int jumpTable3Id = 0;
		StringBuilder setBuilder;
		StringBuilder strBuilder;
		StringBuilder jumpTableBuilder;

		public PoolManager() {
			setBuilder = new StringBuilder();
			strBuilder = new StringBuilder();
			jumpTableBuilder = new StringBuilder();
		}

		public void addSet(String init) {
			setBuilder.append(init).append("\n");
			setBuilder.append("   bitset_create_impl(ctx, " + this.setId + ", init_set" + this.setId++ + ");\n");
		}

		public void addStr(String init) {
			strBuilder.append("   str_create_impl(ctx, " + this.strId++ + ", \"" + init + "\");\n");
		}

		public void addJumpTable(String init, int size) {
			jumpTableBuilder.append(init).append("\n");
			if (size == 2) {
				jumpTableBuilder.append("   int target1_" + jumpTable1Id + "[" + size + "] = {");
				for (int i = 0; i < size; i++) {
					jumpTableBuilder.append(String.valueOf(i));
					if (i < size - 1) {
						jumpTableBuilder.append(", ");
					}
				}
				jumpTableBuilder.append("};\n");
				jumpTableBuilder.append("   jump_table_create_impl(ctx, " + this.jumpTable1Id + ", target1_" + jumpTable1Id + ", init_jump_table1_" + this.jumpTable1Id++ + ", " + size + ");\n");
			} else if (size == 4) {
				jumpTableBuilder.append("   int target2_" + jumpTable2Id + "[" + size + "] = {");
				for (int i = 0; i < size; i++) {
					jumpTableBuilder.append(String.valueOf(i));
					if (i < size - 1) {
						jumpTableBuilder.append(", ");
					}
				}
				jumpTableBuilder.append("};\n");
				jumpTableBuilder.append("   jump_table_create_impl(ctx, " + this.jumpTable2Id + ", target2_" + jumpTable2Id + ", init_jump_table2_" + this.jumpTable2Id++ + ", " + size + ");\n");
			} else {
				jumpTableBuilder.append("   int target3_" + jumpTable3Id + "[" + size + "] = {");
				for (int i = 0; i < size; i++) {
					jumpTableBuilder.append(String.valueOf(i));
					if (i < size - 1) {
						jumpTableBuilder.append(", ");
					}
				}
				jumpTableBuilder.append("};\n");
				jumpTableBuilder.append("   jump_table_create_impl(ctx, " + this.jumpTable3Id + ", target3_" + jumpTable3Id + ", init_jump_table3_" + this.jumpTable3Id++ + ", " + size + ");\n");
			}

		}

	}

	PoolManager manager = new PoolManager();
	Production currentProduction = null;

	@Override
	public void visitProduction(Grammar gg, Production p) {
		currentProduction = p;
		initFalureJumpPoint();
		Func("int", "p" + name(p.getLocalName()), "ParsingContext ctx");
		Begin();
		pushFailureJumpPoint();
		String pos = "c" + this.fid;
		Let("char *", pos, "ctx->cur");
		Expression e = p.getExpression();
		visitExpression(e);
		L("return 0;");
		popFailureJumpPoint(p);
		L("return 1;");
		End();
		L();
	}

	@Override
	public void visitEmpty(Expression p) {
	}

	@Override
	public void visitFail(Expression p) {
		jumpFailureJump();
	}

	@Override
	public void visitAny(Nez.Any p) {
		If(_Equals("*ctx->cur", "0")).Begin();
		jumpFailureJump();
		End();
		Consume();
	}

	@Override
	public void visitByte(Nez.Byte p) {
		If(_NotEquals("(uint8_t)*ctx->cur", String.valueOf(p.byteChar)));
		Begin();
		this.jumpFailureJump();
		End();
		Consume();
	}

	public void initSet(boolean b[]) {
		String initializeExpr = "   int init_set" + manager.setId + "[" + b.length + "] = { ";
		for (int i = 0; i < 256; i++) {
			if (b[i]) {
				initializeExpr += 1;
			} else {
				initializeExpr += 0;
			}
			if (i < 255) {
				initializeExpr += ", ";
			} else {
				initializeExpr += " };";
			}
		}
		manager.addSet(initializeExpr);
	}

	@Override
	public void visitByteset(Nez.Byteset p) {
		Let("bitset_t*", "set" + manager.setId, "&ctx->sets[" + manager.setId + "]");
		If(_Not(_FuncCall("bitset_get", "set" + manager.setId, "(uint8_t)*ctx->cur"))).Begin();
		jumpFailureJump();
		End();
		Consume();
		initSet(p.byteMap);
	}

	private void initStr(byte bseq[]) {
		try {
			manager.addStr(new String(bseq, StringUtils.DefaultEncoding));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	// @Override
	// public void visitCmulti(Cmulti p) {
	// String strName = "str" + manager.strId;
	// String lenName = "len" + manager.strId;
	// Let("const char*", strName, "ctx->strs[" + manager.strId + "]");
	// Let("unsigned", lenName, _FuncCall("pstring_length", strName));
	// If(_Equals(_FuncCall("pstring_starts_with", "ctx->cur", strName,
	// lenName), "0")).Begin();
	// jumpFailureJump();
	// End();
	// Consume(lenName);
	// initStr(p.byteSeq);
	// }

	private boolean specializeOption(Nez.Option p) {
		if (strategy.Olex) {
			Expression inner = p.get(0);
			if (inner instanceof Cbyte) {
				If(_Equals("(uint8_t)*ctx->cur", String.valueOf(((Cbyte) inner).byteChar))).Begin();
				Consume();
				End();
				return true;
			}
			if (inner instanceof Cset) {
				Let("bitset_t*", "set" + manager.setId, "&ctx->sets[" + manager.setId + "]");
				If(_FuncCall("bitset_get", "set" + manager.setId, "(uint8_t)*ctx->cur")).Begin();
				Consume();
				End();
				initSet(((Cset) inner).byteMap);
				return true;
			}
			if (inner instanceof Cmulti) {
				String strName = "str" + manager.strId;
				String lenName = "len" + manager.strId;
				Let("const char*", strName, "ctx->strs[" + manager.strId + "]");
				Let("unsigned", lenName, _FuncCall("pstring_length", strName));
				If(_NotEquals(_FuncCall("pstring_starts_with", "ctx->cur", strName, lenName), "0")).Begin();
				Consume(lenName);
				End();
				initStr(((Cmulti) inner).byteSeq);
				return true;
			}
		}
		return false;
	}

	@Override
	public void visitOption(Nez.Option p) {
		if (!specializeOption(p)) {
			pushFailureJumpPoint();
			String label = "EXIT_OPTION" + fid;
			String backtrack = "c" + fid;
			Let("char *", backtrack, "ctx->cur");
			visitExpression(p.get(0));
			gotoLabel(label);
			popFailureJumpPoint(p);
			Let("ctx->cur", backtrack);
			Label(label);
		}
	}

	private boolean specializeZeroMore(Nez.ZeroMore p) {
		if (strategy.Olex) {
			Expression inner = p.get(0);
			if (inner instanceof Cbyte) {
				While("1").Begin();
				If(_NotEquals("(uint8_t)*ctx->cur", String.valueOf(((Cbyte) inner).byteChar))).Begin();
				Break();
				End();
				Consume();
				End();
				return true;
			}
			if (inner instanceof Cset) {
				Let("bitset_t*", "set" + manager.setId, "&ctx->sets[" + manager.setId + "]");
				While("1").Begin();
				If(_Not(_FuncCall("bitset_get", "set" + manager.setId, "(uint8_t)*ctx->cur"))).Begin();
				Break();
				End();
				Consume();
				End();
				initSet(((Cset) inner).byteMap);
				return true;
			}
			if (inner instanceof Cmulti) {
				String strName = "str" + manager.strId;
				String lenName = "len" + manager.strId;
				Let("const char*", strName, "ctx->strs[" + manager.strId + "]");
				Let("unsigned", lenName, _FuncCall("pstring_length", strName));
				While("1").Begin();
				If(_Equals(_FuncCall("pstring_starts_with", "ctx->cur", strName, lenName), "0")).Begin();
				Break();
				End();
				Consume(lenName);
				End();
				initStr(((Cmulti) inner).byteSeq);
				return true;
			}
		}
		return false;
	}

	@Override
	public void visitZeroMore(Nez.ZeroMore p) {
		if (!specializeZeroMore(p)) {
			pushFailureJumpPoint();
			String backtrack = "c" + fid;
			Let("char *", backtrack, "ctx->cur");
			While("1").Begin();
			visitExpression(p.get(0));
			If("ctx->cur == " + backtrack);
			Begin();
			jumpFailureJump();
			End();
			Let(backtrack, "ctx->cur");
			End();
			popFailureJumpPoint(p);
			Let("ctx->cur", backtrack);
		}
	}

	@Override
	public void visitOneMore(Nez.OneMore p) {
		visitExpression(p.get(0));
		pushFailureJumpPoint();
		String backtrack = "c" + fid;
		Let("char *", backtrack, "ctx->cur");
		While("1").Begin();
		visitExpression(p.get(0));
		If("ctx->cur == " + backtrack);
		Begin();
		jumpFailureJump();
		End();
		Let(backtrack, "ctx->cur");
		End();
		popFailureJumpPoint(p);
		Let("ctx->cur", backtrack);
	}

	@Override
	public void visitAnd(Nez.And p) {
		pushFailureJumpPoint();
		String label = "EXIT_AND" + this.fid;
		String backtrack = "c" + this.fid;
		Let("char *", backtrack, "ctx->cur");
		visitExpression(p.get(0));
		Let("ctx->cur", backtrack);
		gotoLabel(label);
		popFailureJumpPoint(p);
		Let("ctx->cur", backtrack);
		jumpFailureJump();
		Label(label);
	}

	private boolean specializeNot(Nez.Not p) {
		if (strategy.Olex) {
			Expression inner = p.get(0);
			if (inner instanceof Cbyte) {
				If(_Equals("(uint8_t)*ctx->cur", String.valueOf(((Cbyte) inner).byteChar))).Begin();
				jumpFailureJump();
				End();
				return true;
			}
			if (inner instanceof Cset) {
				Let("bitset_t*", "set" + manager.setId, "&ctx->sets[" + manager.setId + "]");
				If(_FuncCall("bitset_get", "set" + manager.setId, "(uint8_t)*ctx->cur")).Begin();
				jumpFailureJump();
				End();
				initSet(((Cset) inner).byteMap);
				return true;
			}
			if (inner instanceof Cmulti) {
				String strName = "str" + manager.strId;
				String lenName = "len" + manager.strId;
				Let("const char*", strName, "ctx->strs[" + manager.strId + "]");
				Let("unsigned", lenName, _FuncCall("pstring_length", strName));
				If(_Equals(_FuncCall("pstring_starts_with", "ctx->cur", strName, lenName), "0")).Begin();
				jumpFailureJump();
				End();
				initStr(((Cmulti) inner).byteSeq);
				return true;
			}
		}
		return false;
	}

	@Override
	public void visitNot(Nez.Not p) {
		if (!specializeNot(p)) {
			pushFailureJumpPoint();
			String backtrack = "c" + this.fid;
			Let("char *", backtrack, "ctx->cur");
			visitExpression(p.get(0));
			Let("ctx->cur", backtrack);
			jumpPrevFailureJump();
			popFailureJumpPoint(p);
			Let("ctx->cur", backtrack);
		}
	}

	@Override
	public void visitPair(Nez.Pair p) {
		for (int i = 0; i < p.size(); i++) {
			visitExpression(p.get(i));
		}
	}

	@Override
	public void visitChoice(Nez.Choice p) {
		if (p.predictedCase != null && this.strategy.Odchoice) {
			int fid = this.fid++;
			String label = "EXIT_CHOICE" + fid;
			HashMap<String, Expression> m = new HashMap<String, Expression>();
			ArrayList<Expression> l = new ArrayList<Expression>();
			l.add(null);
			StringBuilder sb = new StringBuilder();
			sb.append(" = {");
			// L("void* jump_table" + fid + "[] = {");
			for (int ch = 0; ch < p.predictedCase.length; ch++) {
				Expression pCase = p.predictedCase[ch];
				if (pCase != null) {
					Expression me = m.get(unique(pCase));
					if (me == null) {
						m.put(unique(pCase), pCase);
						l.add(pCase);
					}
					sb.append(String.valueOf(l.indexOf(pCase)));
				} else {
					sb.append("0");
				}
				if (ch < p.predictedCase.length - 1) {
					sb.append(", ");
				}
			}
			sb.append("};");
			String name = "\n   int init_jump_table";
			int size = l.size();
			if (size <= 8) {
				L("void* jump_table" + fid + "[] = {");
				for (int i = 0; i < l.size(); i++) {
					Expression pCase = l.get(i);
					if (pCase != null) {
						W("&&PREDICATE_JUMP" + fid + "" + unique(pCase));
					} else {
						W("&&PREDICATE_JUMP" + fid + "" + 0);
					}
					if (i < l.size() - 1) {
						W(", ");
					}
				}
				W("};");
				int jumpTableId = 0;
				if (size <= 2) {
					jumpTableId = manager.jumpTable1Id;
					name += "1_" + jumpTableId + "[257]";
					manager.addJumpTable(name + sb.toString(), 2);
					L("goto *jump_table" + fid + "[jump_table" + 1 + "_jump(&ctx->jumps" + 1 + "[" + jumpTableId + "], *ctx->cur)];");
				} else if (size <= 4) {
					jumpTableId = manager.jumpTable2Id;
					name += "2_" + jumpTableId + "[257]";
					manager.addJumpTable(name + sb.toString(), 4);
					L("goto *jump_table" + fid + "[jump_table" + 2 + "_jump(&ctx->jumps" + 2 + "[" + jumpTableId + "], *ctx->cur)];");
				} else if (size <= 8) {
					jumpTableId = manager.jumpTable3Id;
					name += "3_" + jumpTableId + "[257]";
					manager.addJumpTable(name + sb.toString(), 8);
					L("goto *jump_table" + fid + "[jump_table" + 3 + "_jump(&ctx->jumps" + 3 + "[" + jumpTableId + "], *ctx->cur)];");
				}
				if (size == 1) {

				} else if (size == 3) {

				}

				for (int i = 0; i < l.size(); i++) {
					Expression pe = l.get(i);
					if (pe == null) {
						continue;
					}
					Label("PREDICATE_JUMP" + fid + "" + unique(pe));
					MarkStack cur = markStack;
					visitExpression(pe);
					markStack = cur;
					gotoLabel(label);
				}
				Label("PREDICATE_JUMP" + fid + "" + 0);
				this.jumpFailureJump();
				Label(label);
			} else {
				m = new HashMap<String, Expression>();
				l = new ArrayList<Expression>();
				L("void* jump_table" + fid + "[] = {");
				for (int ch = 0; ch < p.predictedCase.length; ch++) {
					Expression pCase = p.predictedCase[ch];
					if (pCase != null) {
						Expression me = m.get(unique(pCase));
						if (me == null) {
							m.put(unique(pCase), pCase);
							l.add(pCase);
						}
						W("&&PREDICATE_JUMP" + fid + "" + unique(pCase));
					} else {
						W("&&PREDICATE_JUMP" + fid + "" + 0);
					}
					if (ch < p.predictedCase.length - 1) {
						W(", ");
					}
				}
				W("};");
				L("goto *jump_table" + fid + "[(uint8_t)*ctx->cur];");
				for (int i = 0; i < l.size(); i++) {
					Expression pe = l.get(i);
					Label("PREDICATE_JUMP" + fid + "" + unique(pe));
					MarkStack cur = markStack;
					visitExpression(pe);
					markStack = cur;
					gotoLabel(label);
				}
				Label("PREDICATE_JUMP" + fid + "" + 0);
				this.jumpFailureJump();
				Label(label);
			}
		} else {
			fid++;
			String label = "EXIT_CHOICE" + this.fid;
			String backtrack = "c" + this.fid;
			Let("char *", backtrack, "ctx->cur");
			for (int i = 0; i < p.size(); i++) {
				pushFailureJumpPoint();
				MarkStack cur = markStack;
				visitExpression(p.get(i));
				markStack = cur;
				gotoLabel(label);
				popFailureJumpPoint(p.get(i));
				Let("ctx->cur", backtrack);
			}
			jumpFailureJump();
			Label(label);
		}
	}

	private boolean memoizeNonTerminal(NonTerminal n) {
		Production p = n.getProduction();
		if (p == null) {
			Verbose.debug("[PANIC] unresolved: " + n.getLocalName() + " ***** ");
			return false;
		}
		ParseFunc f = this.getParseFunc(p);
		if (f.isInlined()) {
			this.optimizedInline(p);
			return true;
		}
		MemoPoint memo = f.getMemoPoint();
		if (memo != null) {
			if (!strategy.TreeConstruction /*
											 * || p.isNoNTreeConstruction()
											 * FIXME
											 */) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: " + n.getLocalName() + " at " + currentProduction.getLocalName());
				}
				String label = "EXIT_CALL" + this.fid++;
				String pos = "pos" + this.fid;
				Lookup(memo, n, label);
				Let("char *", pos, "ctx->cur");
				pushFailureJumpPoint();
				String name = "p" + name(p.getLocalName());
				If(_FuncCall(name, "ctx")).Begin();
				jumpFailureJump();
				End();
				Memo(memo, pos, "ctx->cur - " + pos);
				gotoLabel(label);
				popFailureJumpPoint(n);
				MemoFail(memo);
				jumpFailureJump();
				Label(label);
				return true;
			}
		}
		return false;
	}

	private void optimizedInline(Production p) {
		visitExpression(p.getExpression());
	}

	@Override
	public void visitNonTerminal(NonTerminal p) {
		if (!memoizeNonTerminal(p)) {
			String name = "p" + name(p.getLocalName());
			If(_FuncCall(name, "ctx")).Begin();
			jumpFailureJump();
			End();
		}
	}

	private boolean memoizeLink(Nez.Link p) {
		if (this.strategy.TreeConstruction && p.get(0) instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) p.get(0);
			ParseFunc f = this.getParseFunc(n.getProduction());
			MemoPoint memo = f.getMemoPoint();
			if (memo != null) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: @" + n.getLocalName() + " at " + currentProduction.getLocalName());
				}
				String mark = "mark" + this.fid++;
				String pos = "pos" + this.fid;
				String label = "EXIT_LINK" + this.fid;
				TLookup(memo, n, label);
				pushFailureJumpPoint();
				Let("int", mark, _FuncCall("ast_save_tx", "ctx->ast"));
				Let("char *", pos, "ctx->cur");
				String name = "p" + name(n.getLocalName());
				If(_FuncCall(name, "ctx")).Begin();
				jumpFailureJump();
				End();
				String po = "ctx->left";
				Symbol sym = p.getLabel();
				if (sym == null) {
					sym = Symbol.NullSymbol;
				}
				FuncCall("ast_commit_tx", "ctx->ast", String.valueOf(sym.id()), mark);
				Let(po, "ast_get_last_linked_node(ctx->ast)");
				TMemo(memo, pos, po, "ctx->cur - " + pos);
				gotoLabel(label);
				popFailureJumpPoint(p);
				FuncCall("ast_rollback_tx", "ctx->ast", mark);
				MemoFail(memo);
				jumpFailureJump();
				Label(label);
				return true;
			}
		}
		return false;
	}

	@Override
	public void visitLink(Nez.Link p) {
		if (!memoizeLink(p)) {
			pushFailureJumpPoint();
			int fid = this.fid++;
			String mark = "mark" + fid;
			if (this.strategy.TreeConstruction) {
				Let("int", mark, _FuncCall("ast_save_tx", "ctx->ast"));
			}
			visitExpression(p.get(0));
			if (this.strategy.TreeConstruction) {
				String po = "ctx->left";
				String label = "EXIT_LINK" + fid;
				Symbol sym = p.getLabel();
				if (sym == null) {
					sym = Symbol.NullSymbol;
				}
				FuncCall("ast_commit_tx", "ctx->ast", String.valueOf(sym.id()), mark);
				Let(po, "ast_get_last_linked_node(ctx->ast)");
				gotoLabel(label);
				popFailureJumpPoint(p);
				FuncCall("ast_rollback_tx", "ctx->ast", mark);
				jumpFailureJump();
				Label(label);
			}
		}
	}

	/* mark stack */

	class MarkStack {
		String mark;
		MarkStack prev;

		public MarkStack(String mark, MarkStack prev) {
			this.mark = mark;
			this.prev = prev;
		}
	}

	MarkStack markStack;

	public void pushMark(String mark) {
		markStack = new MarkStack(mark, markStack);
	}

	public MarkStack popMark() {
		MarkStack cur = markStack;
		markStack = markStack.prev;
		return cur;
	}

	@Override
	public void visitPreNew(Nez.PreNew p) {
		if (this.strategy.TreeConstruction) {
			pushMark(null);
			String mark = "mark" + this.fid++;
			Let("int", mark, _FuncCall("ast_save_tx", "ctx->ast"));
			FuncCall("ast_log_new", "ctx->ast", "ctx->cur + " + p.shift);
		}
	}

	@Override
	public void visitLeftFold(Nez.LeftFold p) {
		if (this.strategy.TreeConstruction) {
			String mark = "mark" + this.fid++;
			pushMark(mark);
			pushFailureJumpPoint();
			Let("int", mark, _FuncCall("ast_save_tx", "ctx->ast"));
			Symbol label = p.getLabel();
			if (label != null) {
				FuncCall("ast_log_swap", "ctx->ast", "ctx->cur + " + p.shift, String.valueOf(p.getLabel().id()));
			} else {
				FuncCall("ast_log_swap", "ctx->ast", "ctx->cur + " + p.shift, "0");
			}
		}
	}

	@Override
	public void visitNew(Nez.New p) {
		if (this.strategy.TreeConstruction) {
			createAST = true;
			String mark = popMark().mark;
			if (mark != null) {
				String label = "EXIT_LFOLD" + this.fid;
				gotoLabel(label);
				popFailureJumpPoint(p);
				FuncCall("ast_rollback_tx", "ctx->ast", mark);
				jumpFailureJump();
				Label(label);
			}
			FuncCall("ast_log_capture", "ctx->ast", "ctx->cur");
		}
	}

	List<Symbol> tagList = new ArrayList<Symbol>();

	@Override
	public void visitTag(Nez.Tag p) {
		if (this.strategy.TreeConstruction) {
			tagList.add(p.tag);
			FuncCall("ast_log_tag", "ctx->ast", "\"" + p.tag.getSymbol() + "\"");
		}
	}

	@Override
	public void visitReplace(Nez.Replace p) {
		if (this.strategy.TreeConstruction) {
			FuncCall("ast_log_replace", "ctx->ast", "\"" + p.value + "\"");
		}
	}

	@Override
	public void visitXblock(Xblock p) {
		// TODO Auto-generated method stub
		int id = this.fid++;
		String pos = "saved" + id;
		String tbl = "tbl" + id;
		Let("symtable_t*", tbl, "ctx->table");
		Let("long", pos, "symtable_savepoint(" + tbl + ");");
		visitExpression(p.get(0));
		L("symtable_rollback(" + tbl + ", " + pos + ");");
	}

	@Override
	public void visitXlocal(Xlocal p) {
		/*
		 * symtable_t *tbl = SYMTABLE_GET(); long saved = POP();
		 * symtable_rollback(tbl, saved);
		 */
		int tableId = 0;
		if (tableMap.containsKey(p.tableName.getSymbol())) {
			tableId = tableMap.get(p.tableName.getSymbol());
		} else {
			tableId = tableMap.size();
			tableMap.put(p.tableName.getSymbol(), tableId);
		}
		int id = this.fid++;
		String pos = "saved" + id;
		String tbl = "tbl" + id;
		String tableName = "tableName" + id;
		Let("symtable_t*", tbl, "ctx->table");
		Let("long", pos, "symtable_savepoint(" + tbl + ");");
		Let("const char*", tableName, "ctx->tables[" + tableId + "]");
		L("symtable_add_symbol_mask(" + tbl + ", " + tableName + ");");
		visitExpression(p.get(0));
		L("symtable_rollback(" + tbl + ", " + pos + ");");
	}

	HashMap<String, Integer> tableMap = new HashMap<>();

	@Override
	public void visitXdef(Xsymbol p) {
		/*
		 * symtable_t *tbl = SYMTABLE_GET(); tag_t *tableName =
		 * TBL_GET_IMPL(runtime, tagId); token_t captured; token_init(&captured,
		 * (const char *)POP(), GET_CURRENT()); symtable_add_symbol(tbl,
		 * tableName, &captured);
		 */
		int tableId = 0;
		if (tableMap.containsKey(p.tableName.getSymbol())) {
			tableId = tableMap.get(p.tableName.getSymbol());
		} else {
			tableId = tableMap.size();
			tableMap.put(p.tableName.getSymbol(), tableId);
		}
		int id = this.fid++;
		String pos = "pos" + id;
		Let("char *", pos, "ctx->cur");
		visitExpression(p.get(0));
		String tbl = "tbl" + id;
		String tableName = "tableName" + id;
		Let("symtable_t*", tbl, "ctx->table");
		Let("const char*", tableName, "ctx->tables[" + tableId + "]");
		L("token_t t" + id + ";");
		FuncCall("token_init", "&t" + id, pos, "ctx->cur");
		FuncCall("symtable_add_symbol", tbl, tableName, "&t" + id);
	}

	@Override
	public void visitXexists(Xexists p) {

	}

	@Override
	public void visitXmatch(Xmatch p) {
		/*
		 * symtable_t *tbl = SYMTABLE_GET(); tag_t *tableName =
		 * TBL_GET_IMPL(runtime, tagId); token_t t; if (symtable_get_symbol(tbl,
		 * tableName, &t)) { if (token_equal_string(&t, GET_CURRENT())) {
		 * CONSUME_N(token_length(&t)); NEXT(); } } FAIL();
		 */
		int tableId = 0;
		if (tableMap.containsKey(p.tableName.getSymbol())) {
			tableId = tableMap.get(p.tableName.getSymbol());
		} else {
			tableId = tableMap.size();
			tableMap.put(p.tableName.getSymbol(), tableId);
		}
		int id = this.fid++;
		String tbl = "tbl" + id;
		String tableName = "tableName" + id;
		Let("symtable_t*", tbl, "ctx->table");
		Let("const char*", tableName, "ctx->tables[" + tableId + "]");
		L("token_t t" + id + ";");
		If(_FuncCall("symtable_get_symbol", tbl, tableName, "&t" + id));
		Begin();
		If(_Not(_FuncCall("token_equal_string", "&t" + id, "ctx->cur"))).Begin();
		jumpFailureJump();
		End();
		Consume(_FuncCall("token_length", "&t" + id));
		End();
	}

	@Override
	public void visitXis(Xis p) {
		/*
		 * symtable_t *tbl = SYMTABLE_GET(); tag_t *tableName =
		 * TBL_GET_IMPL(runtime, tagId); token_t t; if (symtable_get_symbol(tbl,
		 * tableName, &t)) { token_t captured; token_init(&captured, (const char
		 * *)POP(), GET_CURRENT()); if (token_equal(&t, &captured)) { //
		 * CONSUME_N(token_length(&t)); NEXT(); } } FAIL();
		 */
		int tableId = 0;
		if (tableMap.containsKey(p.tableName.getSymbol())) {
			tableId = tableMap.get(p.tableName.getSymbol());
		} else {
			tableId = tableMap.size();
			tableMap.put(p.tableName.getSymbol(), tableId);
		}
		int id = this.fid++;
		String tbl = "tbl" + id;
		String tableName = "tableName" + id;
		Let("symtable_t*", tbl, "ctx->table");
		Let("const char*", tableName, "ctx->tables[" + tableId + "]");
		L("token_t t" + id + ";");
		If(_FuncCall("symtable_get_symbol", tbl, tableName, "&t" + id));
		Begin();
		If(_Not(_FuncCall("token_equal_string", "&t" + id, "ctx->cur"))).Begin();
		jumpFailureJump();
		End();
		Consume(_FuncCall("token_length", "&t" + id));
		End();
		L("else");
		Begin();
		jumpFailureJump();
		End();
	}

	@Override
	public void visitXif(Xif p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXon(Xon p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXindent(Xindent p) {
		// TODO Auto-generated method stub

	}
}
