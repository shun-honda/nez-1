package mincaml;

import nez.ast.Tag;

public class MCTag {
	public static Tag TAG_BOOL = Tag.tag("BOOL");
	public static Tag TAG_INT = Tag.tag("INT");
	public static Tag TAG_FLOAT = Tag.tag("FLOAT");
	public static Tag TAG_NOT = Tag.tag("NOT");
	public static Tag TAG_MINUS = Tag.tag("MINUS");
	public static Tag TAG_PLUS = Tag.tag("PLUS");
	public static Tag TAG_MINUS_DOT = Tag.tag("MINUS_DOT");
	public static Tag TAG_PLUS_DOT = Tag.tag("PLUS_DOT");
	public static Tag TAG_AST_DOT = Tag.tag("AST_DOT");
	public static Tag TAG_SLASH_DOT = Tag.tag("SLASH_DOT");
	public static Tag TAG_EQUAL = Tag.tag("EQUAL");
	public static Tag TAG_LESS_GREATER = Tag.tag("LESS_GREATER");
	public static Tag TAG_LESS_EQUAL = Tag.tag("LESS_EQUAL");
	public static Tag TAG_GREATER_EQUAL = Tag.tag("GREATER_EQUAL");
	public static Tag TAG_LESS = Tag.tag("LESS");
	public static Tag TAG_GREATER = Tag.tag("GREATER");
	public static Tag TAG_IF = Tag.tag("IF");
	public static Tag TAG_THEN = Tag.tag("THEN");
	public static Tag TAG_ELSE = Tag.tag("ELSE");
	public static Tag TAG_IDENT = Tag.tag("IDENT");
	public static Tag TAG_LET = Tag.tag("LET");
	public static Tag TAG_IN = Tag.tag("IN");
	public static Tag TAG_REC = Tag.tag("REC");
	public static Tag TAG_COMMA = Tag.tag("COMMA");
	public static Tag TAG_ARRAY_CREATE = Tag.tag("ARRAY_CREATE");
	public static Tag TAG_DOT = Tag.tag("DOT");
	public static Tag TAG_LESS_MINUS = Tag.tag("LESS_MINUS");
	public static Tag TAG_SEMICOLON = Tag.tag("SEMICOLON");
	public static Tag TAG_LPAREN = Tag.tag("LPAREN");
	public static Tag TAG_RPAREN = Tag.tag("RPAREN");
	public static Tag TAG_EOF = Tag.tag("EOF");
}
