package nez.bx;

import nez.ast.Tree;

public class ASTEqualChecker {

	public ASTEqualChecker() {
	}

	public boolean isEqual(Tree a, Tree x) {
		if (!a.getTag().equals(x.getTag())) {
			return false;
		}
		if (a.size() != x.size()) {
			return false;
		}
		for (int i = 0; i < a.size(); i++) {
			if (!isEqual(a.get(i), x.get(i))) {
				return false;
			}
			if (!a.getLabel(i).equals(x.getLabel(i))) {
				return false;
			}
		}
		if (a.size() == 0) {
			if (!a.getValue().equals(x.getValue())) {
				return false;
			}
		}
		return true;
	}
}
