package nez.x.generator;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import nez.Strategy;
import nez.Verbose;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tdetree;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xon;
import nez.lang.expr.Xsymbol;
import nez.parser.GenerativeGrammar;
import nez.parser.MemoPoint;
import nez.parser.ParseFunc;
import nez.parser.ParserGenerator;
import nez.util.StringUtils;

public class CParserGenerator extends ParserGenerator {

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
	public void makeHeader(GenerativeGrammar gg) {
		this.setGenerativeGrammar(gg);
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
		L();
	}

	boolean createAST = false;

	@Override
	public void makeFooter(GenerativeGrammar gg) {
		int flagTableSize = 0 /* this.flagTable.size() */;
		int prodSize = gg.getProductionList().size();
		// L("#define CNEZ_FLAG_TABLE_SIZE " + flagTableSize);
		Define("CNEZ_FLAG_TABLE_SIZE " + flagTableSize);
		// L("#define CNEZ_MEMO_SIZE " + 0 /* this.memoId */);
		Define("CNEZ_MEMO_SIZE       " + gg.memoPointList.size());
		// L("#define CNEZ_GRAMMAR_URN \"" + urn + "\"");
		// L("#define CNEZ_PRODUCTION_SIZE " + prodSize);
		Define("CNEZ_PRODUCTION_SIZE " + prodSize);
		Define("CNEZ_SET_SIZE " + manager.setId);
		Define("CNEZ_STR_SIZE " + manager.strId);
		if (this.enabledASTConstruction && createAST) {
			// L("#define CNEZ_ENABLE_AST_CONSTRUCTION 1");
			Define("CNEZ_ENABLE_AST_CONSTRUCTION 1");
		}
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
		StringBuilder setBuilder;
		StringBuilder strBuilder;

		public PoolManager() {
			setBuilder = new StringBuilder();
			strBuilder = new StringBuilder();
		}

		public void addSet(String init) {
			setBuilder.append(init).append("\n");
			setBuilder.append("   bitset_create_impl(ctx, " + this.setId + ", init_set" + this.setId++ + ");\n");
		}

		public void addStr(String init) {
			strBuilder.append("   str_create_impl(ctx, " + this.strId++ + ", \"" + init + "\");\n");
		}

	}

	PoolManager manager = new PoolManager();
	Production currentProduction = null;

	@Override
	public void visitProduction(GenerativeGrammar gg, Production p) {
		currentProduction = p;
		initFalureJumpPoint();
		// L("int p" + name(p.getLocalName()) + "(ParsingContext ctx)");
		Func("int", "p" + name(p.getLocalName()), "ParsingContext ctx");
		Begin();
		pushFailureJumpPoint();
		// if (this.enabledPackratParsing) {
		// lookup(p, this.memoId);
		// }
		String pos = "c" + this.fid;
		Let("char *", pos, "ctx->cur");
		Expression e = p.getExpression();
		visitExpression(e);
		// if (this.enabledPackratParsing) {
		// memoize(p, this.memoId, pos);
		// }
		L("return 0;");
		popFailureJumpPoint(p);
		// if (this.enabledPackratParsing) {
		// memoizeFail(p, this.memoId, pos);
		// }
		L("return 1;");
		End();
		L();
		// if (this.enabledPackratParsing) {
		// this.memoId++;
		// }
	}

	@Override
	public void visitPempty(Expression p) {
	}

	@Override
	public void visitPfail(Expression p) {
		jumpFailureJump();
	}

	@Override
	public void visitCany(Cany p) {
		If(_Equals("*ctx->cur", "0")).Begin();
		jumpFailureJump();
		End();
		Consume();
	}

	@Override
	public void visitCbyte(Cbyte p) {
		If(_NotEquals("*ctx->cur", String.valueOf(p.byteChar)));
		Begin();
		this.jumpFailureJump();
		End();
		Consume();
	}

	public void initSet(boolean b[]) {
		String initializeExpr = "   int init_set" + manager.setId + "[] = { ";
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
	public void visitCset(Cset p) {
		Let("bitset_t*", "set" + manager.setId, "&ctx->sets[" + manager.setId + "]");
		If(_Not(_FuncCall("bitset_get", "set" + manager.setId, "*ctx->cur"))).Begin();
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

	@Override
	public void visitCmulti(Cmulti p) {
		String strName = "str" + manager.strId;
		String lenName = "len" + manager.strId;
		Let("const char*", strName, "ctx->strs[" + manager.strId + "]");
		Let("unsigned", lenName, _FuncCall("pstring_length", strName));
		If(_Equals(_FuncCall("pstring_starts_with", "ctx->cur", strName, lenName), "0")).Begin();
		jumpFailureJump();
		End();
		Consume(lenName);
		initStr(p.byteSeq);
	}

	private boolean specializeOption(Poption p) {
		if (strategy.isEnabled("Olex", Strategy.Olex)) {
			Expression inner = p.get(0);
			if (inner instanceof Cbyte) {
				If(_Equals("*ctx->cur", String.valueOf(((Cbyte) inner).byteChar))).Begin();
				Consume();
				End();
				return true;
			}
			if (inner instanceof Cset) {
				Let("bitset_t*", "set" + manager.setId, "&ctx->sets[" + manager.setId + "]");
				If(_FuncCall("bitset_get", "set" + manager.setId, "*ctx->cur")).Begin();
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
	public void visitPoption(Poption p) {
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

	private boolean specializeZeroMore(Pzero p) {
		if (strategy.isEnabled("Olex", Strategy.Olex)) {
			Expression inner = p.get(0);
			if (inner instanceof Cbyte) {
				While("1").Begin();
				If(_NotEquals("*ctx->cur", String.valueOf(((Cbyte) inner).byteChar))).Begin();
				Break();
				End();
				Consume();
				End();
				return true;
			}
			if (inner instanceof Cset) {
				Let("bitset_t*", "set" + manager.setId, "&ctx->sets[" + manager.setId + "]");
				While("1").Begin();
				If(_Not(_FuncCall("bitset_get", "set" + manager.setId, "*ctx->cur"))).Begin();
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
	public void visitPzero(Pzero p) {
		if (!specializeZeroMore(p)) {
			pushFailureJumpPoint();
			String backtrack = "c" + fid;
			Let("char *", backtrack, "ctx->cur");
			While("1").Begin();
			visitExpression(p.get(0));
			Let(backtrack, "ctx->cur");
			End();
			popFailureJumpPoint(p);
			Let("ctx->cur", backtrack);
		}
	}

	@Override
	public void visitPone(Pone p) {
		visitExpression(p.get(0));
		visitPzero(p);
	}

	@Override
	public void visitPand(Pand p) {
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

	private boolean specializeNot(Pnot p) {
		if (strategy.isEnabled("Olex", Strategy.Olex)) {
			Expression inner = p.get(0);
			if (inner instanceof Cbyte) {
				If(_Equals("*ctx->cur", String.valueOf(((Cbyte) inner).byteChar))).Begin();
				jumpFailureJump();
				End();
				return true;
			}
			if (inner instanceof Cset) {
				Let("bitset_t*", "set" + manager.setId, "&ctx->sets[" + manager.setId + "]");
				If(_FuncCall("bitset_get", "set" + manager.setId, "*ctx->cur")).Begin();
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
	public void visitPnot(Pnot p) {
		if (specializeNot(p)) {
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
	public void visitPsequence(Psequence p) {
		for (int i = 0; i < p.size(); i++) {
			visitExpression(p.get(i));
		}
	}

	@Override
	public void visitPchoice(Pchoice p) {
		if (p.predictedCase != null) {
			int fid = this.fid++;
			String label = "EXIT_CHOICE" + fid;
			HashMap<String, Expression> m = new HashMap<String, Expression>();
			ArrayList<Expression> l = new ArrayList<Expression>();
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
				visitExpression(pe);
				gotoLabel(label);
			}
			Label("PREDICATE_JUMP" + fid + "" + 0);
			this.jumpFailureJump();
			Label(label);
		} else {
			fid++;
			String label = "EXIT_CHOICE" + this.fid;
			String backtrack = "c" + this.fid;
			Let("char *", backtrack, "ctx->cur");
			for (int i = 0; i < p.size(); i++) {
				pushFailureJumpPoint();
				visitExpression(p.get(i));
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
		if (f.getInlining()) {
			this.optimizedInline(p);
			return true;
		}
		MemoPoint memo = f.getMemoPoint();
		if (memo != null) {
			if (!enabledASTConstruction || p.isNoNTreeConstruction()) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: " + n.getLocalName() + " at " + currentProduction.getLocalName());
				}
				String label = "EXIT_CALL" + this.fid;
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

	private boolean memoizeLink(Tlink p) {
		if (enabledPackratParsing && enabledASTConstruction && p.get(0) instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) p.get(0);
			ParseFunc f = this.getParseFunc(n.getProduction());
			MemoPoint memo = f.getMemoPoint();
			if (memo != null) {
				if (Verbose.PackratParsing) {
					Verbose.println("memoize: @" + n.getLocalName() + " at " + currentProduction.getLocalName());
				}
				pushFailureJumpPoint();
				String mark = "mark" + this.fid++;
				String pos = "pos" + this.fid;
				String label = "EXIT_LINK" + this.fid;
				TLookup(memo, n, label);
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
	public void visitTlink(Tlink p) {
		if (!memoizeLink(p)) {
			pushFailureJumpPoint();
			String mark = "mark" + this.fid++;
			if (this.enabledASTConstruction) {
				Let("int", mark, _FuncCall("ast_save_tx", "ctx->ast"));
			}
			visitExpression(p.get(0));
			if (this.enabledASTConstruction) {
				String po = "ctx->left";
				String label = "EXIT_LINK" + this.fid;
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

	Stack<String> markStack = new Stack<String>();

	@Override
	public void visitTnew(Tnew p) {
		if (this.enabledASTConstruction) {
			markStack.push(null);
			String mark = "mark" + this.fid++;
			Let("int", mark, _FuncCall("ast_save_tx", "ctx->ast"));
			FuncCall("ast_log_new", "ctx->ast", "ctx->cur + " + p.shift);
		}
	}

	@Override
	public void visitTlfold(Tlfold p) {
		if (this.enabledASTConstruction) {
			String mark = "mark" + this.fid++;
			markStack.push(mark);
			pushFailureJumpPoint();
			Let("int", mark, _FuncCall("ast_save_tx", "ctx->ast"));
			FuncCall("ast_log_swap", "ctx->ast", "ctx->cur + " + p.shift, String.valueOf(p.getLabel().id()));
		}
	}

	@Override
	public void visitTcapture(Tcapture p) {
		if (this.enabledASTConstruction) {
			createAST = true;
			String mark = markStack.pop();
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
	public void visitTtag(Ttag p) {
		if (this.enabledASTConstruction) {
			tagList.add(p.tag);
			FuncCall("ast_log_tag", "ctx->ast", "\"" + p.tag.getSymbol() + "\"");
		}
	}

	@Override
	public void visitTreplace(Treplace p) {
		if (this.enabledASTConstruction) {
			FuncCall("ast_log_replace", "ctx->ast", "\"" + p.value + "\"");
		}
	}

	@Override
	public void visitTdetree(Tdetree p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXblock(Xblock p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXlocal(Xlocal p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXdef(Xsymbol p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXexists(Xexists p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXmatch(Xmatch p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXis(Xis p) {
		// TODO Auto-generated method stub

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
	public void visitXdefindent(Xdefindent p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitXindent(Xindent p) {
		// TODO Auto-generated method stub

	}

}
