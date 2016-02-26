package nez.bx;

import nez.ast.Tree;

public class ASTEqualChecker {

	public ASTEqualChecker() {
	}

	public boolean isEqual(Tree a, Tree x) {
		if (a.getTag() != null && x.getTag() != null) {
			if (!a.getTag().equals(x.getTag())) {
				System.out.println(a);
				System.out.println("==============================");
				System.out.println(x);
				return false;
			}
		} else if (!(a.getTag() == null && x.getTag() == null)) {
			System.out.println(a);
			System.out.println("==============================");
			System.out.println(x);
			return false;
		}
		if (a.size() != x.size()) {
			System.out.println(a);
			System.out.println("==============================");
			System.out.println(x);
			return false;
		}
		for (int i = 0; i < a.size(); i++) {
			if (!isEqual(a.get(i), x.get(i))) {
				System.out.println(a);
				System.out.println("==============================");
				System.out.println(x);
				return false;
			}
			if (a.getLabel(i) != null && x.getLabel(i) != null) {
				if (!a.getLabel(i).equals(x.getLabel(i))) {
					System.out.println(a);
					System.out.println("==============================");
					System.out.println(x);
					return false;
				}
			} else if (!(a.getLabel(i) == null && x.getLabel(i) == null)) {
				System.out.println(a);
				System.out.println("==============================");
				System.out.println(x);
				return false;
			}
		}
		if (a.size() == 0) {
			if (!a.getValue().equals(x.getValue())) {
				System.out.println(a);
				System.out.println("==============================");
				System.out.println(x);
				return false;
			}
		}
		return true;
	}
}
