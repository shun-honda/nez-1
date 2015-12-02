package nez.lang.macro;

import java.util.HashMap;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.macro.MacroBuilder.DefaultVisitor;
import nez.util.ConsoleUtils;
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

	public NezMacro visit(Tree<?> node) {
		return find(node.getTag().getSymbol()).accept(node);
	}

	public final static Symbol _name = Symbol.tag("name");
	public final static Symbol _param = Symbol.tag("param");
	public final static Symbol _body = Symbol.tag("body");
	public final static Symbol _val = Symbol.tag("val");
	public final static Symbol _label = Symbol.tag("label");

	public void addMacro(Tree<?> node) {
		try {
			visit(node);
		} catch (Exception e) {
			ConsoleUtils.exit(1, e.toString());
		}
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
			DesugarFunction func = new DesugarFunction(name, node, visit(node.get(_body)));
			Tree<?> params = node.get(_param);
			for (Tree<?> param : params) {
				func.addParam(new TransVariable(param.toText(), param));
			}
			desugarFunctionMap.put(name, func);
			return func;
		}
	}

	public class Block extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			nez.lang.macro.Block block = new nez.lang.macro.Block(node);
			for (Tree<?> child : node) {
				block.add(visit(child));
			}
			return block;
		}
	}

	public class NodeLiteral extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			nez.lang.macro.NodeLiteral nodeLiteral = new nez.lang.macro.NodeLiteral(node);
			Tree<?> list = node.get(_val);
			for (Tree<?> element : list) {

			}
			return nodeLiteral;
		}
	}

	public class Element extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			NodeElement element = new NodeElement(node, Symbol.tag(node.get(_label).getText(_name, null)));

			return element;
		}
	}

}
