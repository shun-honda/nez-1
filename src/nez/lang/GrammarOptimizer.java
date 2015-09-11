package nez.lang;

import java.util.HashMap;

import nez.NezOption;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;

public class GrammarOptimizer extends GrammarTransducer {
	/* local optimizer option */
	boolean enabledCommonLeftFactoring = true; // true;
	boolean enabledCostBasedReduction = true;
	boolean enabledOutOfOrder = false; // bugs!!

	NezOption option;
	HashMap<String, String> optimizedMap = new HashMap<String, String>();

	public GrammarOptimizer(NezOption option) {
		this.option = option;
		// if (option.enabledPrediction) {
		// // seems slow when the prediction option is enabled
		// this.enabledCommonLeftFactoring = false;
		// }
	}

	public final Expression optimize(Production p) {
		// String uname = p.getUniqueName();
		// if (!optimizedMap.containsKey(uname)) {
		// optimizedMap.put(uname, uname);
		// // System.out.println("prod: " + p + p.getUniqueName() + " <- " +
		// // p.getExpression());
		// Expression body = p.getExpression();
		// Expression optimized = resolveNonTerminal(body); // reshape(this);
		// if (p.getExpression() != optimized) {
		// System.out.println("uname: " + uname + "\n\t" + body.toString() +
		// " of " + body.getClass() + "\n\t" + optimized);
		// }
		// p.setExpression(optimized);
		// return optimized;
		// }
		return p.getExpression();
	}

	// private void rewrite_outoforder(Expression e, Expression e2) {
	// // Verbose.debug("out-of-order " + e + " <==> " + e2);
	// }
	//
	// private void rewrite(String msg, Expression e, Expression e2) {
	// // Verbose.debug(msg + " " + e + "\n\t=>" + e2);
	// }
	//
	// private void rewrite_common(Expression e, Expression e2, Expression e3) {
	// // Verbose.debug("common (" + e + " / " + e2 + ")\n\t=>" + e3);
	// }

	// used to test inlining
	public final static boolean isSingleCharacter(Expression e) {
		if (e instanceof Cset || e instanceof Cbyte || e instanceof Cany) {
			return true;
		}
		return false;
	}

	// @Override
	// public Expression reshapeNonTerminal(NonTerminal n) {
	// Production p = n.getProduction();
	// // if (p.isRecursive()) {
	// // return n;
	// // }
	// // Expression optimized = this.optimize(p);
	// // if (option.enabledInlining && p.isInline()) {
	// // rewrite("inline", n, optimized);
	// // return optimized;
	// // }
	// // Expression deref = resolveNonTerminal(optimized).reshape(this);
	// // if (isSingleCharacter(deref)) {
	// // rewrite("deref", n, deref);
	// // return deref;
	// // }
	// // if (deref instanceof Pempty || deref instanceof Pfail) {
	// // rewrite("deref", n, deref);
	// // return deref;
	// // }
	// return n;
	// }
	//
	// private boolean isOutOfOrderExpression(Expression e) {
	// if (e instanceof Ttag) {
	// return true;
	// }
	// if (e instanceof Treplace) {
	// return true;
	// }
	// if (e instanceof Tnew) {
	// ((Tnew) e).shift -= 1;
	// return true;
	// }
	// if (e instanceof Tcapture) {
	// ((Tcapture) e).shift -= 1;
	// return true;
	// }
	// return false;
	// }
	//
	// @Override
	// public Expression reshapePsequence(Psequence p) {
	// Expression first = p.getFirst().reshape(this);
	// Expression next = p.getNext().reshape(this);
	// if (this.enabledOutOfOrder) {
	// if (next instanceof Psequence) {
	// Psequence nextSequence = (Psequence) next;
	// if (isSingleCharacter(nextSequence.first) &&
	// isOutOfOrderExpression(first)) {
	// rewrite_outoforder(first, nextSequence.first);
	// Expression temp = nextSequence.first;
	// nextSequence.first = first;
	// first = temp;
	// }
	// } else {
	// if (isSingleCharacter(next) && isOutOfOrderExpression(first)) {
	// rewrite_outoforder(first, next);
	// Expression temp = first;
	// first = next;
	// next = temp;
	// }
	// }
	// }
	// if (isNotChar(first)) {
	// Expression optimized = convertBitMap(next, first.get(0));
	// if (optimized != null) {
	// rewrite("not-merge", p, optimized);
	// return optimized;
	// }
	// }
	// return p.newSequence(first, next);
	// }

	// private boolean isNotChar(Expression p) {
	// if (p instanceof Unot) {
	// return (p.get(0) instanceof Cset || p.get(0) instanceof Cbyte);
	// }
	// return false;
	// }
	//
	// private Expression convertBitMap(Expression next, Expression not) {
	// boolean[] bany = null;
	// boolean isBinary = false;
	// Expression nextNext = next.getNext();
	// if (nextNext != null) {
	// next = next.getFirst();
	// }
	// if (next instanceof Cany) {
	// Cany any = (Cany) next;
	// isBinary = any.isBinary();
	// bany = Cset.newMap(true);
	// if (isBinary) {
	// bany[0] = false;
	// }
	// }
	// if (next instanceof Cset) {
	// Cset bm = (Cset) next;
	// isBinary = bm.isBinary();
	// bany = bm.byteMap.clone();
	// }
	// if (next instanceof Cbyte) {
	// Cbyte bc = (Cbyte) next;
	// isBinary = bc.isBinary();
	// bany = Cset.newMap(false);
	// if (isBinary) {
	// bany[0] = false;
	// }
	// bany[bc.byteChar] = true;
	// }
	// if (bany == null) {
	// return null;
	// }
	// if (not instanceof Cset) {
	// Cset bm = (Cset) not;
	// for (int c = 0; c < bany.length - 1; c++) {
	// if (bm.byteMap[c] && bany[c] == true) {
	// bany[c] = false;
	// }
	// }
	// }
	// if (not instanceof Cbyte) {
	// Cbyte bc = (Cbyte) not;
	// if (bany[bc.byteChar] == true) {
	// bany[bc.byteChar] = false;
	// }
	// }
	// Expression e = not.newByteMap(isBinary, bany);
	// if (nextNext != null) {
	// return not.newSequence(e, nextNext);
	// }
	// return e;
	// }

	// @Override
	// public Expression reshapeTlink(Tlink p) {
	// if (p.get(0) instanceof Pchoice) {
	// Expression inner = p.get(0);
	// UList<Expression> l = new UList<Expression>(new
	// Expression[inner.size()]);
	// for (Expression subChoice : inner) {
	// subChoice = subChoice.reshape(this);
	// l.add(ExpressionCommons.newTlink(p.getSourcePosition(), p.getLabel(),
	// subChoice));
	// }
	// return inner.newChoice(l);
	// }
	// return super.reshapeTlink(p);
	// }

	// @Override
	// public Expression reshapePchoice(Pchoice p) {
	// // if (!p.isFlatten) {
	// // p.isFlatten = true;
	// // UList<Expression> choiceList = new UList<Expression>(new
	// // Expression[p.size()]);
	// // flattenChoiceList(p, choiceList);
	// // // Expression optimized = convertByteMap(p, choiceList);
	// // // if (optimized != null) {
	// // // rewrite("choice-map", p, optimized);
	// // // return optimized;
	// // // }
	// // // boolean isFlatten = p.size() != choiceList.size();
	// // // for (int i = 0; i < choiceList.size(); i++) {
	// // // Expression sub = choiceList.ArrayValues[i];
	// // // if (!isFlatten) {
	// // // if (sub.equalsExpression(p.get(i))) {
	// // // continue;
	// // // }
	// // // }
	// // // choiceList.ArrayValues[i] = sub.reshape(this);
	// // // }
	// // // if (choiceList.size() == 1) {
	// // // rewrite("choice-single", p, choiceList.ArrayValues[0]);
	// // // return choiceList.ArrayValues[0];
	// // // }
	// // // if (option.enabledPrediction) {
	// // // int count = 0;
	// // // int selected = 0;
	// // // p.predictedCase = new Expression[257];
	// // // Expression singleChoice = null;
	// // // for (int ch = 0; ch <= 255; ch++) {
	// // // Expression predicted = selectChoice(p, choiceList, ch);
	// // // p.predictedCase[ch] = predicted;
	// // // if (predicted != null) {
	// // // singleChoice = predicted;
	// // // count++;
	// // // if (predicted instanceof Pchoice) {
	// // // selected += predicted.size();
	// // // } else {
	// // // selected += 1;
	// // // }
	// // // }
	// // // }
	// // // double reduced = (double) selected / count;
	// // // // Verbose.debug("reduced: " + choiceList.size() + " => " +
	// // // // reduced);
	// // // if (count == 1 && singleChoice != null) {
	// // // rewrite("choice-single", p, singleChoice);
	// // // return singleChoice;
	// // // }
	// // // if (this.enabledCostBasedReduction && reduced / choiceList.size()
	// // // > 0.55) {
	// // // p.predictedCase = null;
	// // // }
	// // // }
	// // // if (!isFlatten) {
	// // // return p;
	// // // }
	// // Expression c = p.newChoice(choiceList);
	// // if (c instanceof Pchoice) {
	// // ((Pchoice) c).isFlatten = true;
	// // ((Pchoice) c).predictedCase = p.predictedCase;
	// // }
	// // // rewrite("flatten", p, c);
	// // return c;
	// // }
	// return p;
	// }
	//
	// private void flattenChoiceList(Pchoice parentExpression,
	// UList<Expression> l) {
	// for (Expression subExpression : parentExpression) {
	// subExpression = resolveNonTerminal(subExpression);
	// if (subExpression instanceof Pchoice) {
	// flattenChoiceList((Pchoice) subExpression, l);
	// } else {
	// subExpression = subExpression.reshape(this);
	// if (l.size() > 0 && this.enabledCommonLeftFactoring) {
	// Expression lastExpression = l.ArrayValues[l.size() - 1];
	// Expression first = lastExpression.getFirst();
	// if (first.equalsExpression(subExpression.getFirst())) {
	// Expression next = lastExpression.newChoice(lastExpression.getNext(),
	// subExpression.getNext());
	// Expression common = lastExpression.newSequence(first, next);
	// rewrite_common(lastExpression, subExpression, common);
	// l.ArrayValues[l.size() - 1] = common;
	// continue;
	// }
	// }
	// l.add(subExpression);
	// }
	// }
	// }

	public final static Expression resolveNonTerminal(Expression e) {
		while (e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}

	// // OptimizerLibrary
	//
	// private Expression convertByteMap(Pchoice choice, UList<Expression>
	// choiceList) {
	// boolean byteMap[] = Cset.newMap(false);
	// boolean binary = false;
	// for (Expression e : choiceList) {
	// if (e instanceof Pfail) {
	// continue;
	// }
	// if (e instanceof Cbyte) {
	// byteMap[((Cbyte) e).byteChar] = true;
	// if (((Cbyte) e).isBinary()) {
	// binary = true;
	// }
	// continue;
	// }
	// if (e instanceof Cset) {
	// Cset.appendBitMap(byteMap, ((Cset) e).byteMap);
	// if (((Cset) e).isBinary()) {
	// binary = true;
	// }
	// continue;
	// }
	// if (e instanceof Cany) {
	// return e;
	// }
	// if (e instanceof Pempty) {
	// break;
	// }
	// return null;
	// }
	// return choice.newByteMap(binary, byteMap);
	// }
	//
	// private Expression selectChoice(Pchoice choice, UList<Expression>
	// choiceList, int ch) {
	// Expression first = null;
	// UList<Expression> newChoiceList = null;
	// boolean commonPrifixed = false;
	// for (Expression p : choiceList) {
	// short r = p.acceptByte(ch);
	// if (r == PossibleAcceptance.Reject) {
	// continue;
	// }
	// if (first == null) {
	// first = p;
	// continue;
	// }
	// if (newChoiceList == null) {
	// Expression common = tryCommonFactoring(choice, first, p, true);
	// if (common != null) {
	// first = common;
	// commonPrifixed = true;
	// continue;
	// }
	// newChoiceList = new UList<Expression>(new Expression[2]);
	// newChoiceList.add(first);
	// newChoiceList.add(p);
	// } else {
	// Expression last = newChoiceList.ArrayValues[newChoiceList.size() - 1];
	// Expression common = tryCommonFactoring(choice, last, p, true);
	// if (common != null) {
	// newChoiceList.ArrayValues[newChoiceList.size() - 1] = common;
	// continue;
	// }
	// newChoiceList.add(p);
	// }
	// }
	// if (newChoiceList != null) {
	// return ExpressionCommons.newPchoice(choice.getSourcePosition(),
	// newChoiceList);
	// }
	// return commonPrifixed == true ? first.reshape(this) : first;
	// }
	//
	// public final static Expression tryCommonFactoring(Pchoice base,
	// Expression e, Expression e2, boolean ignoredFirstChar) {
	// UList<Expression> l = null;
	// while (e != null && e2 != null) {
	// Expression f = e.getFirst();
	// Expression f2 = e2.getFirst();
	// if (ignoredFirstChar) {
	// ignoredFirstChar = false;
	// if (Expression.isByteConsumed(f) && Expression.isByteConsumed(f2)) {
	// l = ExpressionCommons.newList(4);
	// l.add(f);
	// e = e.getNext();
	// e2 = e2.getNext();
	// continue;
	// }
	// return null;
	// }
	// if (!f.equalsExpression(f2)) {
	// break;
	// }
	// if (l == null) {
	// l = ExpressionCommons.newList(4);
	// }
	// l.add(f);
	// e = e.getNext();
	// e2 = e2.getNext();
	// // System.out.println("l="+l.size()+",e="+e);
	// }
	// if (l == null) {
	// return null;
	// }
	// if (e == null) {
	// e = base.newEmpty();
	// }
	// if (e2 == null) {
	// e2 = base.newEmpty();
	// }
	// Expression alt = base.newChoice(e, e2);
	// l.add(alt);
	// return base.newSequence(l);
	// }

}