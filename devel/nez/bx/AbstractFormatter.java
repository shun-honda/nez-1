package nez.bx;

import nez.ast.Tree;

public abstract class AbstractFormatter {
	public abstract String format(TagFormat format, Tree<?> node);

	public abstract String format(TagParam format, Tree<?> node);

	public abstract String format(UserDefinedFormat format, Tree<?> node);

	public abstract String format(ListParam format, Tree<?> node);

	public abstract String format(EmptyListParam format, Tree<?> node);

	public abstract String format(Text format, Tree<?> node);

	public abstract String format(Name format, Tree<?> node);

	public abstract String format(SystemVariable format, Tree<?> node);

	public abstract String format(ApplyUserDefinedFormat format, Tree<?> node);
}
