package nez.lang.macro;

import java.util.HashMap;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.macro.MacroBuilder.DefaultVisitor;
import nez.util.VisitorMap;

public class MacroBuilder extends VisitorMap<DefaultVisitor> {
	HashMap<String, NezMacro> macroMap = new HashMap<>();
	HashMap<String, DesugarFunction> desugarFunctionMap = new HashMap<>();

	public MacroBuilder() {
		this.init(MacroBuilder.class, new DefaultVisitor());
	}

	public Tree<?> desugar(Tree<?> node) {
		return node;
	}

	public NezMacro accept(Tree<?> node) {
		return find(node.getTag().getSymbol()).accept(node);
	}

	public final static Symbol _name = Symbol.tag("name");
	public final static Symbol _param = Symbol.tag("param");

	public void addMacro(Tree<?> node) {
		accept(node);
	}

	public class DefaultVisitor {
		public NezMacro accept(Tree<?> node) {
			return null;
		}
	}

	public class DesugarFuncDecl extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			String name = node.getText(_name, null);
			DesugarFunction func = new DesugarFunction(name, node);
			Tree<?> params = node.get(_param);
			for (Tree<?> param : params) {
				func.addParam(new TransVariable(param.toText(), param));
			}
			System.out.println(node);
			return null;
		}
	}

}
