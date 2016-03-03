package nez.bx;

import nez.ast.Tree;

public class ASTEqualChecker {
	boolean printed = false;

	public ASTEqualChecker() {
	}

	public boolean isEqual(Tree a, Tree x) {
		if (a.getTag() != null && x.getTag() != null) {
			if (!a.getTag().equals(x.getTag())) {
				if (!printed) {
					printError(a, x);
				}
				return false;
			}
		} else if (!(a.getTag() == null && x.getTag() == null)) {
			if (!printed) {
				printError(a, x);
			}
			return false;
		}
		if (a.size() != x.size()) {
			if (!printed) {
				printError(a, x);
			}
			return false;
		}
		for (int i = 0; i < a.size(); i++) {
			if (!isEqual(a.get(i), x.get(i))) {
				if (!printed) {
					printError(a, x);
				}
				return false;
			}
			if (a.getLabel(i) != null && x.getLabel(i) != null) {
				if (!a.getLabel(i).equals(x.getLabel(i))) {
					if (!printed) {
						printError(a, x);
					}
					return false;
				}
			} else if (!(a.getLabel(i) == null && x.getLabel(i) == null)) {
				if (!printed) {
					printError(a, x);
				}
				return false;
			}
		}
		if (a.size() == 0) {
			if (!a.getValue().equals(x.getValue())) {
				if (!printed) {
					printError(a, x);
				}
				return false;
			}
		}
		return true;
	}

	private void printError(Tree a, Tree x) {
		System.out.println(a);
		System.out.println("==============================");
		System.out.println(x);
		printed = true;
	}
}