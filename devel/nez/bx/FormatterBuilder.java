package nez.bx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.ast.TreeVisitorMap;

public class FormatterBuilder extends TreeVisitorMap<FomatterVisitor> {
	FormatterContext context;

	public FormatterBuilder() {
		this.init(FormatterBuilder.class, new Undefined());
		this.context = new FormatterContext();
	}

	public FormatterContext getContext() {
		return this.context;
	}

	public Format visit(Tree<?> node) {
		return this.find(node.getTag().getSymbol()).accept(node);
	}

	public class Undefined implements FomatterVisitor {
		@Override
		public Format accept(Tree<?> node) {
			throw new RuntimeException("undefined node: " + node);
		}
	}

	public class Source extends Undefined {
		@Override
		public Format accept(Tree<?> node) {
			for (Tree<?> child : node) {
				visit(child);
			}
			return null;
		}
	}

	public class TagFormat extends Undefined implements FormatSymbols {
		@Override
		public Format accept(Tree<?> node) {
			/* parameter part */
			Tree<?> params = node.get(param);
			List<Format> paramList = new ArrayList<>();
			for (Tree<?> param : params) {
				paramList.add(visit(param));
			}
			/* body part */
			Tree<?> bodyNode = node.get(body);
			List<Format> formatList = new ArrayList<Format>();
			for (Tree<?> child : bodyNode) {
				formatList.add(visit(child));
			}
			nez.bx.TagFormat format = new nez.bx.TagFormat(node.getText(name, null), formatList, paramList);
			context.setTagFormatter(format);
			return format;
		}
	}

	public class UserDefinedFormatter extends Undefined implements FormatSymbols {
		@Override
		public Format accept(Tree<?> node) {
			/* parameter part */
			Tree<?> params = node.get(param);
			List<Format> paramList = new ArrayList<>();
			for (Tree<?> param : params) {
				paramList.add(visit(param));
			}
			/* body part */
			Tree<?> bodyNode = node.get(body);
			List<Format> formatList = new ArrayList<Format>();
			for (Tree<?> child : bodyNode) {
				formatList.add(visit(child));
			}
			nez.bx.UserDefinedFormat format = new nez.bx.UserDefinedFormat(node.getText(name, null), formatList, paramList);
			context.setUserDefinedFormatter(format);
			return format;
		}
	}

	public class TagParam extends Undefined implements FormatSymbols {
		@Override
		public Format accept(Tree<?> node) {
			Tree<?> tagsNode = node.get(tag);
			List<Symbol> tags = new ArrayList<>();
			for (Tree<?> tag : tagsNode) {
				tags.add(Symbol.unique(tag.toText()));
			}
			nez.bx.TagParam tagParam = new nez.bx.TagParam(node.getText(label, null), tags);
			return tagParam;
		}
	}

	public class ListParam extends Undefined implements FormatSymbols {
		@Override
		public Format accept(Tree<?> node) {
			nez.bx.ListParam listParam = new nez.bx.ListParam(visit(node.get(label, null)), node.getText(list, null));
			return listParam;
		}
	}

	public class EmptyListParam extends Undefined {
		@Override
		public Format accept(Tree<?> node) {
			return new nez.bx.EmptyListParam();
		}
	}

	public class Text extends Undefined {
		@Override
		public Format accept(Tree<?> node) {
			return new nez.bx.Text(node.toText());
		}
	}

	public class Name extends Undefined {
		@Override
		public Format accept(Tree<?> node) {
			return new nez.bx.Name(node.toText());
		}
	}

	public class SystemVariable extends Undefined implements FormatSymbols {
		@Override
		public Format accept(Tree<?> node) {
			return new nez.bx.SystemVariable(node.getText(name, null));
		}
	}

	public class Apply extends Undefined implements FormatSymbols {
		@Override
		public Format accept(Tree<?> node) {
			Tree<?> args = node.get(list);
			List<Format> argList = new ArrayList<>();
			for (Tree<?> arg : args) {
				argList.add(visit(arg));
			}
			return new ApplyUserDefinedFormat(node.getText(name, null), argList);
		}
	}
}

interface FomatterVisitor {
	public Format accept(Tree<?> node);
}

interface FormatSymbols {
	static final Symbol Format = Symbol.unique("Format");
	static final Symbol Name = Symbol.unique("Name");
	static final Symbol List = Symbol.unique("List");
	static final Symbol Param = Symbol.unique("Param");
	static final Symbol TagParam = Symbol.unique("TagParam");
	static final Symbol ListParam = Symbol.unique("ListParam");
	static final Symbol Text = Symbol.unique("Text");
	static final Symbol Apply = Symbol.unique("Apply");
	static final Symbol SystemVariable = Symbol.unique("SystemVariable");
	static final Symbol Field = Symbol.unique("Field");

	static final Symbol name = Symbol.unique("name");
	static final Symbol param = Symbol.unique("param");
	static final Symbol body = Symbol.unique("body");
	static final Symbol label = Symbol.unique("label");
	static final Symbol tag = Symbol.unique("tag");
	static final Symbol list = Symbol.unique("list");
}

class FormatterContext {
	HashMap<String, FormatFunctionSet<TagFormat>> tagFomatterMap;
	HashMap<String, FormatFunctionSet<UserDefinedFormat>> userDefinedFormatterMap;
	Scope scope;

	public FormatterContext() {
		this.tagFomatterMap = new HashMap<>();
		this.userDefinedFormatterMap = new HashMap<>();
	}

	public void setTagFormatter(TagFormat formatter) {
		if (tagFomatterMap.containsKey(formatter.name)) {
			FormatFunctionSet<TagFormat> set = tagFomatterMap.get(formatter.name);
			set.addFunction(formatter);
		} else {
			FormatFunctionSet<TagFormat> set = new FormatFunctionSet<>();
			set.addFunction(formatter);
			tagFomatterMap.put(formatter.name, set);
		}
	}

	public FormatFunctionSet<TagFormat> getTagFormatter(String name) {
		return this.tagFomatterMap.get(name);
	}

	public void setUserDefinedFormatter(UserDefinedFormat formatter) {
		if (userDefinedFormatterMap.containsKey(formatter.name)) {
			FormatFunctionSet<UserDefinedFormat> set = userDefinedFormatterMap.get(formatter.name);
			set.addFunction(formatter);
		} else {
			FormatFunctionSet<UserDefinedFormat> set = new FormatFunctionSet<>();
			set.addFunction(formatter);
			userDefinedFormatterMap.put(formatter.name, set);
		}
	}

	public FormatFunctionSet<UserDefinedFormat> getUserDefinedFormatter(String name) {
		return this.userDefinedFormatterMap.get(name);
	}
}

class Scope {
	HashMap<String, Tree<?>> varMap;
	Scope prev = null;

	public Scope() {
		varMap = new HashMap<>();
	}

	public Scope(Scope prev) {
		this();
		this.prev = prev;
	}

	public void initSystemVariable(Tree<?> thisNode) {
		this.setVariable("this", thisNode);
	}

	public Tree<?> getVariable(String name) {
		return varMap.get(name);
	}

	public void setVariable(String name, Tree<?> node) {
		varMap.put(name, node);
	}
}
