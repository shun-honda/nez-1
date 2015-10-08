package nez.ast.script;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import konoha.Array;
import konoha.IArray;
import nez.ast.TreeVisitor2;
import nez.ast.script.asm.ScriptCompiler;

public class Interpreter extends TreeVisitor2<nez.ast.script.Interpreter.Undefined> implements CommonSymbols {
	ScriptContext context;
	TypeSystem typeSystem;
	private ScriptCompiler compiler;

	public Interpreter(ScriptContext sc, TypeSystem base) {
		super();
		init(new Undefined());
		this.context = sc;
		this.typeSystem = base;
		this.compiler = new ScriptCompiler(this.typeSystem);
	}

	static EmptyResult empty = new EmptyResult();

	public Object eval(TypedTree node) {
		switch (node.hint) {
		case Apply:
			return evalApplyHint(node);
		case Constant:
			return node.getValue();
		case MethodApply:
			return evalMethodApplyHint(node);
		case StaticInvocation:
			return evalStaticInvocationHint(node);
		case GetField:
			return evalFieldHint(node);
		case SetField:
			return evalSetFieldHint(node);
		case Unique:
		default:
			return evalImpl(node);
		}
	}

	public Object nullEval(TypedTree node) {
		return (node == null) ? null : eval(node);
	}

	final Object evalImpl(TypedTree node) {
		Object o = find(node).visit(node);
		// System.out.println("#" + node.getTag() + ": " + o);
		return o;
	}

	public class Undefined {
		Object visit(TypedTree node) {
			throw new ScriptRuntimeException("TODO: Interpreter " + node);
		}
	}

	public class Source extends Undefined {
		@Override
		public Object visit(TypedTree node) {
			boolean foundError = false;
			Object result = empty;
			for (TypedTree sub : node) {
				if (sub.is(_Error)) {
					context.log(sub.getText(_msg, ""));
					foundError = true;
				}
				if (!foundError) {
					result = eval(sub);
				}
			}
			return foundError ? empty : result;
		}
	}

	/* TopLevel */

	public class FuncDecl extends Undefined {
		@Override
		public Object visit(TypedTree node) {
			compiler.compileFuncDecl(node);
			return empty;
		}
	}

	/* boolean */

	public class Block extends Undefined {
		@Override
		public Object visit(TypedTree node) {
			Object retVal = null;
			for (TypedTree child : node) {
				retVal = eval(child);
			}
			return retVal;
		}
	}

	public class If extends Undefined {
		@Override
		public Object visit(TypedTree node) {
			boolean cond = (Boolean) eval(node.get(_cond));
			if (cond) {
				return eval(node.get(_then));
			} else {
				TypedTree elseNode = node.get(_else, null);
				if (elseNode != null) {
					return eval(elseNode);
				}
			}
			return empty;
		}
	}

	/* Expression Statement */
	public class Expression extends Undefined {
		@Override
		public Object visit(TypedTree node) {
			if (node.getType() == void.class) {
				eval(node.get(0));
				return empty;
			}
			return eval(node.get(0));
		}
	}

	public class Empty extends Undefined {
		@Override
		public Object visit(TypedTree node) {
			return empty;
		}
	}

	/* Expression */

	public class Cast extends Undefined {
		@Override
		public Object visit(TypedTree node) {
			Object v = eval(node.get(_expr));
			Method m = node.getMethod();
			if (m != null) {
				return Reflector.invokeStaticMethod(m, v);
			}
			return v;
		}
	}

	public class InstanceOf extends Undefined {
		@Override
		public Object visit(TypedTree node) {
			Object v = eval(node.get(_left));
			if (v == null) {
				return false;
			}
			return ((Class<?>) node.getValue()).isAssignableFrom(v.getClass());
		}
	}

	// public Object evalName(TypedTree node) {
	// String name = node.toText();
	// if (!this.globalVariables.containsKey(name)) {
	// // perror(node, "undefined name: " + name);
	// }
	// return this.globalVariables.get(name);
	// }
	//
	// public Object evalAssign(TypedTree node) {
	// Object right = eval(node.get(_right));
	// String name = node.getText(_left, null);
	// return right;
	// }

	public Object evalFieldHint(TypedTree node) {
		Field f = node.getField();
		Object recv = null;
		if (!Modifier.isStatic(f.getModifiers())) {
			recv = eval(node.get(_recv));
		}
		// System.out.println("eval field:" + recv + " . " + f);
		return Reflector.getField(recv, f);
	}

	private Object evalSetFieldHint(TypedTree node) {
		Object recv = null;
		Object value = eval(node.get(_expr));
		Field f = node.getField();
		if (!Modifier.isStatic(f.getModifiers())) {
			recv = nullEval(node.get(_recv, null));
		}
		Reflector.setField(recv, f, value);
		return value;
	}

	private Object evalStaticInvocationHint(TypedTree node) {
		Object[] args = this.evalApplyArgument(node);
		return Reflector.invokeStaticMethod(node.getMethod(), args);
	}

	public Object evalMethodApplyHint(TypedTree node) {
		Object recv = eval(node.get(_recv));
		Object[] args = evalApplyArgument(node.get(_param));
		return Reflector.invokeMethod(recv, node.getMethod(), args);
	}

	public Object evalApplyHint(TypedTree node) {
		// String name = node.getText(_name, null);
		Object[] args = evalApplyArgument(node.get(_param));
		return Reflector.invokeStaticMethod(node.getMethod(), args);
	}

	private Object[] evalApplyArgument(TypedTree node) {
		Object[] args = new Object[node.size()];
		for (int i = 0; i < node.size(); i++) {
			args[i] = eval(node.get(i));
		}
		return args;
	}

	public class Interpolation extends Undefined {
		@Override
		public Object visit(TypedTree node) {
			Object[] args = evalApplyArgument(node);
			return Reflector.invokeStaticMethod(node.getMethod(), new Object[] { args });
		}
	}

	public class _Array extends Undefined {
		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Object visit(TypedTree node) {
			Object[] args = evalApplyArgument(node);
			Class<?> atype = node.getClassType();
			if (atype == IArray.class) {
				return new IArray<Object>(args, args.length);
				// System.out.println("AAA " + a + " " + a.getClass());
			}
			return new Array(args, args.length);
		}
	}
}