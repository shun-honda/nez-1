package nez.x.generator;

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
import nez.parser.ParserGenerator;

public class CParserGenerator extends ParserGenerator {

	@Override
	protected String getFileExtension() {
		return "c";
	}

	@Override
	public void visitProduction(GenerativeGrammar gg, Production p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitPempty(Expression p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitPfail(Expression p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitCany(Cany p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitCbyte(Cbyte p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitCset(Cset p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitCmulti(Cmulti p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitPoption(Poption p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitPzero(Pzero p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitPone(Pone p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitPand(Pand p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitPnot(Pnot p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitPsequence(Psequence p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitPchoice(Pchoice p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitNonTerminal(NonTerminal p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTlink(Tlink p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTnew(Tnew p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTlfold(Tlfold p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTcapture(Tcapture p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTtag(Ttag p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitTreplace(Treplace p) {
		// TODO Auto-generated method stub

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
