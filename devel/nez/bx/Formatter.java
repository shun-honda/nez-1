package nez.bx;

import java.util.List;

import nez.ast.Symbol;
import nez.ast.Tree;

public class Formatter extends AbstractFormatter {
	FormatterContext context;

	public Formatter(FormatterContext context) {
		this.context = context;
	}

	public TagFormat findTagFormatter(Tree<?> node) {
		FormatFunctionSet<TagFormat> set = context.getTagFormatter(node.getTag().getSymbol());
		if (set != null) {
			int paramSize = node.size();
			for (int i = 0; i < set.set.size(); i++) {
				TagFormat formatter = set.getFunc(i);
				if (matchFormatter(formatter, node, paramSize)) {
					return formatter;
				}
			}
		}
		return null;
	}

	public boolean matchFormatter(TagFormat formatter, Tree<?> node, int paramSize) {
		if (paramSize == formatter.params.size()) {
			for (Format param : formatter.params) {
				if (param instanceof TagParam) {
					TagParam tagParam = (TagParam) param;
					if (!checkTagParam(tagParam, node)) {
						return false;
					}
				}
			}
			return true;
		} else if (formatter.params.size() == 0) {
			return true;
		}
		return false;
	}

	public boolean checkTagParam(TagParam param, Tree<?> node) {
		Tree<?> targetNode = node.get(Symbol.unique(param.name));
		if (targetNode != null) {
			Symbol symbol = targetNode.getTag();
			for (Symbol tag : param.tags) {
				if (symbol.equals(tag)) {
					return true;
				}
			}
			return false;
		}
		throw new RuntimeException("label \"" + param.name + "\" is not found");
	}

	public UserDefinedFormat findUserDefinedFormat(String name, List<Format> args, Tree<?> node) {
		FormatFunctionSet<UserDefinedFormat> set = this.context.getUserDefinedFormatter(name);
		if (set != null) {
			for (int i = 0; i < set.set.size(); i++) {
				UserDefinedFormat formatter = set.getFunc(i);
				if (matchUserDefinedFormatter(formatter, node, args)) {
					return formatter;
				}
			}
		}
		throw new RuntimeException("UserDefinedFormatter \"" + name + "\" is not found");
	}

	public boolean matchUserDefinedFormatter(UserDefinedFormat formatter, Tree<?> node, List<Format> args) {
		if (args.size() == formatter.params.size()) {
			for (int i = 0; i < args.size(); i++) {
				Format param = formatter.params.get(i);
				if (param instanceof TagParam) {
					TagParam tagParam = (TagParam) param;
					if (!checkTagParam(tagParam, getUserDefinedArgument(args.get(i)))) {
						return false;
					}
				} else if (param instanceof ListParam) {
					ListParam listParam = (ListParam) param;
					if (!checkListParam(listParam, getUserDefinedArgument(args.get(i)))) {
						return false;
					}
				}
			}
			return true;
		}
		return false;
	}

	public boolean checkListParam(ListParam param, Tree<?> node) {
		if (node != null && node.size() != 1) {
			return true;
		}
		return false;
	}

	public String format(Tree<?> node) {
		TagFormat formatter = findTagFormatter(node);
		if (formatter != null) {
			return formatter.format(this, node);
		}
		throw new RuntimeException("tag formatter \"" + node.getTag() + "\" is not found");
	}

	@Override
	public String format(TagFormat format, Tree<?> node) {
		this.context.scope = new Scope(this.context.scope);
		this.context.scope.setVariable("this", node); // FIXME
		for (Format param : format.params) {
			if (param instanceof Name) {
				String name = ((Name) param).name;
				Tree<?> paramNode = node.get(Symbol.unique(name));
				if (paramNode != null) {
					this.context.scope.setVariable(name, node.get(Symbol.unique(name)));
				} else {
					throw new RuntimeException("label \"" + name + "\" is not found");
				}
			}
		}
		String ret = "";
		for (Format element : format.body) {
			ret += element.format(this, node);
		}
		this.context.scope = this.context.scope.prev;
		return ret;
	}

	@Override
	public String format(TagParam format, Tree<?> node) {
		return null;
	}

	@Override
	public String format(ListParam format, Tree<?> node) {
		return null;
	}

	@Override
	public String format(UserDefinedFormat format, Tree<?> node) {
		String ret = "";
		for (Format element : format.body) {
			ret += element.format(this, node);
		}
		return ret;
	}

	@Override
	public String format(Text format, Tree<?> node) {
		return format.text;
	}

	@Override
	public String format(Name format, Tree<?> node) {
		Tree<?> targetNode = this.context.scope.getVariable(format.name);
		TagFormat formatter = findTagFormatter(targetNode);
		if (formatter != null) {
			return formatter.format(this, targetNode);
		}
		throw new RuntimeException("tag formatter \"" + targetNode.getTag() + "\" is not found");
	}

	@Override
	public String format(SystemVariable format, Tree<?> node) {
		if (format.name.equals("value")) {
			return node.toText();
		}
		throw new RuntimeException("system variable \"" + format.name + "\" is not found");
	}

	@Override
	public String format(ApplyUserDefinedFormat format, Tree<?> node) {
		UserDefinedFormat formatter = findUserDefinedFormat(format.name, format.args, node);
		Scope currentScope = this.context.scope;
		this.context.scope = new Scope(currentScope);
		if (formatter != null) {
			for (int i = 0; i < format.args.size(); i++) {
				Format arg = format.args.get(i);
				if (arg instanceof Name) {
					Name nameArg = (Name) arg;
					Tree<?> argNode = currentScope.getVariable(nameArg.name);
					setUserDefinedFormatParameter(formatter.params.get(i), argNode);
				} // TODO
				else if (arg instanceof SystemVariable) {
					SystemVariable sysVar = (SystemVariable) arg;
					if (sysVar.name.equals("this")) {
						setUserDefinedFormatParameter(formatter.params.get(i), node);
					}
				}
			}
			String ret = formatter.format(this, node);
			this.context.scope = this.context.scope.prev;
			return ret;
		}
		this.context.scope = this.context.scope.prev;
		throw new RuntimeException("user defined formatter \"" + format.name + "\" is not found");
	}

	public void setUserDefinedFormatParameter(Format formatter, Tree<?> node) {
		if (formatter instanceof Name) {
			Name nameParam = (Name) formatter;
			this.context.scope.setVariable(nameParam.name, node);
			return;
		} else if (formatter instanceof ListParam) {
			ListParam listParam = (ListParam) formatter;
			argNode = node;
			this.context.scope.setVariable(listParam.name, removeArgNode(0));
			this.context.scope.setVariable(listParam.listName, argNode);
			return;
		} else if (formatter instanceof TagParam) {
			TagParam tagParam = (TagParam) formatter;
			this.context.scope.setVariable(tagParam.name, node);
			return;
		}
		throw new RuntimeException("unexpected format:" + formatter);
	}

	public Tree<?> getUserDefinedArgument(Format formatter) {
		if (formatter instanceof Name) {
			Name nameArg = (Name) formatter;
			return this.context.scope.getVariable(nameArg.name);
		}
		if (formatter instanceof SystemVariable) {
			SystemVariable sysVar = (SystemVariable) formatter;
			return this.context.scope.getVariable(sysVar.name);
		}
		throw new RuntimeException("unexpected format:" + formatter);
	}

	Tree<?> argNode; // FIXME

	public Tree<?> removeArgNode(int index) {
		Tree<?> ret = argNode.get(index);
		Tree<?> oldValue = argNode;
		argNode = argNode.newInstance(argNode.getTag(), argNode.getSource(), argNode.getSourcePosition(), argNode.getLength(), argNode.size() - 1, argNode.getValue());
		int newIndex = 0;
		for (int i = 0; i < oldValue.size(); i++) {
			if (i != index) {
				argNode.link(newIndex, argNode.getLabel(newIndex), oldValue.get(i));
				newIndex++;
			}
		}
		return ret;
	}

}
