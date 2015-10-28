package nez.lang.macro;

import nez.ast.CommonTree;

public interface CommonMacroVisitor {
	public Object accept(CommonTree node);
}
