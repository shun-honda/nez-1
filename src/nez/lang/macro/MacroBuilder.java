package nez.lang.macro;

import java.util.HashMap;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.macro.MacroBuilder.DefaultVisitor;
import nez.util.ConsoleUtils;
import nez.util.VisitorMap;

public class MacroBuilder extends VisitorMap<DefaultVisitor> {
	HashMap<java.lang.String, DesugarFunctionSet> desugarFunctionMap = new HashMap<>();
	HashMap<java.lang.String, TransFunctionSet> transFunctionMap = new HashMap<>();
	HashMap<java.lang.String, TransVariable> transVariableMap = new HashMap<>();

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
	public final static Symbol _expr = Symbol.tag("expr");
	public final static Symbol _recv = Symbol.tag("recv");
	public final static Symbol _index = Symbol.tag("index");
	public final static Symbol _first = Symbol.tag("first");
	public final static Symbol _list = Symbol.tag("list");

	public final static Symbol _String = Symbol.tag("String");
	public final static Symbol _Interpolation = Symbol.tag("Interpolation");

	public void addMacro(Tree<?> node) {
		try {
			NezMacro macro = visit(node);
			System.out.println(macro.toString());
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
			java.lang.String name = node.getText(_name, null);
			DesugarFunction func = new DesugarFunction(name, node, visit(node.get(_body)));
			Tree<?> params = node.get(_param);
			for (Tree<?> param : params) {
				func.addParam(new nez.lang.macro.Name(param, param.toText()));
			}
			if (desugarFunctionMap.containsKey(name)) {
				DesugarFunctionSet set = desugarFunctionMap.get(name);
				set.addFunc(func);
			} else {
				DesugarFunctionSet set = new DesugarFunctionSet(name);
				set.addFunc(func);
				desugarFunctionMap.put(name, set);
			}
			return func;
		}
	}

	public class TransFuncDecl extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			java.lang.String name = node.getText(_name, null);
			TransFunction func = new TransFunction(name, node, visit(node.get(_body)));
			Tree<?> params = node.get(_param);
			for (Tree<?> param : params) {
				func.addParam(visit(param));
			}
			if (transFunctionMap.containsKey(name)) {
				TransFunctionSet set = transFunctionMap.get(name);
				set.addFunc(func);
			} else {
				TransFunctionSet set = new TransFunctionSet(name);
				set.addFunc(func);
				transFunctionMap.put(name, set);
			}
			return func;
		}
	}

	public class TransVarDecl extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			java.lang.String name = node.getText(_name, null);
			TransVariable var = new TransVariable(name, node, visit(node.get(_expr, null)));
			transVariableMap.put(name, var);
			return var;
		}
	}

	public class ListParam extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			Tree<?> first = node.get(_first, null);
			Tree<?> list = node.get(_list, null);
			return new nez.lang.macro.ListParam(node, new nez.lang.macro.Name(first, first.toText()), new nez.lang.macro.Name(list, list.toText()));
		}
	}

	public class EmptyList extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			return new nez.lang.macro.EmptyList(node);
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
			nez.lang.macro.NodeLiteral nodeLiteral = new nez.lang.macro.NodeLiteral(node, node.getText(_name, null));
			Tree<?> val = node.get(_val);
			if (val.is(_String) || val.is(_Interpolation)) {
				nodeLiteral.add(visit(val), null);
			} else {
				for (Tree<?> element : val) {
					NodeElement retVal = (NodeElement) visit(element);
					nodeLiteral.add(retVal, retVal.desugarLabel);
				}
			}
			return nodeLiteral;
		}
	}

	public class Element extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			Tree<?> labelNode = node.get(_label, null);
			if (labelNode == null) {
				return new NodeElement(node, null, visit(node.get(_expr, null)));
			}
			return new NodeElement(node, Symbol.tag(labelNode.getText(_name, null)), visit(node.get(_expr, null)));
		}
	}

	public class Indexer extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			return new nez.lang.macro.Indexer(node, visit(node.get(_recv, null)), Integer.parseInt(node.getText(_index, null)));
		}
	}

	public class Field extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			return new nez.lang.macro.Field(node, visit(node.get(_recv, null)), Symbol.tag(node.getText(_name, null)));
		}
	}

	public class Name extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			return new nez.lang.macro.Name(node, node.toText());
		}
	}

	public class This extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			return new ThisExpression(node);
		}
	}

	public class String extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			return new StringLiteral(node, node.toText());
		}
	}

	public class Interpolation extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			StringInterpolation macro = new StringInterpolation(node);
			for (Tree<?> child : node) {
				macro.addElement(visit(child));
			}
			return macro;
		}
	}

	public class Text extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			return new nez.lang.macro.Text(node, node.toText());
		}
	}

	public class Apply extends DefaultVisitor {
		@Override
		public NezMacro accept(Tree<?> node) {
			java.lang.String name = node.getText(_name, null);
			nez.lang.macro.Apply func = new nez.lang.macro.Apply(name, node);
			Tree<?> args = node.get(_param);
			for (Tree<?> arg : args) {
				func.addArg(visit(arg));
			}
			return func;
		}
	}

}
