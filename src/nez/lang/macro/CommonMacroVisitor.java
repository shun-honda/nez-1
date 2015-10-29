package nez.lang.macro;

import nez.ast.Tree;

public interface CommonMacroVisitor {
	public Object accept(Tree<?> node);
}
