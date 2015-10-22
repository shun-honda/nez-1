package nez.lang;

import nez.ast.CommonTree;

public interface CommonMacroVisitor {
	public Object accept(CommonTree node);
}
