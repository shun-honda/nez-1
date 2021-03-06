package nez.type;

import java.lang.reflect.Type;

public class OneMoreType implements Type {
	public Type type;

	public OneMoreType(Type type) {
		this.type = type;
	}

	public final static Type enforce(Type t) {
		if (t instanceof OneMoreType || t instanceof ZeroMoreType) {
			return t;
		}
		if (t instanceof OptionType) {
			return new ZeroMoreType(((OneMoreType) t).type);
		}
		if (t instanceof UnionType) {
			UnionType u = (UnionType) t;
			for (int i = 0; i < u.unions.length; i++) {
				u.unions[i] = enforce(u.unions[i]);
			}
			return u;
		}
		return t;
	}

	@Override
	public final String toString() {
		return type + "?";
	}
}
