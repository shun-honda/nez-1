package nez.bx;

import java.util.Arrays;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Nez.And;
import nez.lang.Nez.Any;
import nez.lang.Nez.BeginTree;
import nez.lang.Nez.BlockScope;
import nez.lang.Nez.Byte;
import nez.lang.Nez.ByteSet;
import nez.lang.Nez.Choice;
import nez.lang.Nez.Detree;
import nez.lang.Nez.Empty;
import nez.lang.Nez.EndTree;
import nez.lang.Nez.Fail;
import nez.lang.Nez.FoldTree;
import nez.lang.Nez.IfCondition;
import nez.lang.Nez.Label;
import nez.lang.Nez.LinkTree;
import nez.lang.Nez.LocalScope;
import nez.lang.Nez.MultiByte;
import nez.lang.Nez.Not;
import nez.lang.Nez.OnCondition;
import nez.lang.Nez.OneMore;
import nez.lang.Nez.Option;
import nez.lang.Nez.Pair;
import nez.lang.Nez.Repeat;
import nez.lang.Nez.Replace;
import nez.lang.Nez.Scan;
import nez.lang.Nez.Sequence;
import nez.lang.Nez.SymbolAction;
import nez.lang.Nez.SymbolExists;
import nez.lang.Nez.SymbolMatch;
import nez.lang.Nez.SymbolPredicate;
import nez.lang.Nez.Tag;
import nez.lang.Nez.ZeroMore;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.util.FileBuilder;

public class FormatGenerator {
	private String dir = null;
	private String outputFile = null;
	private FileBuilder file;
	private Grammar grammar = null;

	private Element currentLeft = null;
	private Elements[] productionList = new Elements[4];
	private String[] productionNameList = new String[4];
	private Captured[] capturedList = new Captured[4];
	private String[] tagList = new String[4];
	private Elements[] elementsStack = new Elements[4];
	private StackState[] stackStates = new StackState[4];
	private Elements checkedTag = new Elements();
	private int productionId = 0;
	private int capturedId = 0;
	private int tagId = 0;
	private int stackTop = 0;
	private boolean[] checkedProduction;
	private boolean[] checkedNullLabelTag;
	private int repetitionId = 0;
	private boolean hasRepetition;

	public FormatGenerator(String dir, String outputFile) {
		this.outputFile = outputFile;
		this.dir = dir;
	}

	public void generate(Grammar grammar) {
		this.grammar = grammar;
		try {
			this.makeStandardform();
			this.reshapeStandardform();
			this.reshapeCaptured();
			this.openOutputFile();
			this.writeBxnez();
		} catch (UnTagedException e) {
			System.out.println(e);
		} catch (UnLabeledException e) {
			System.out.println(e);
		}
	}

	public void openOutputFile() {
		if (dir == null || outputFile == null) {
			this.file = new FileBuilder(null);
		} else {
			String path = FileBuilder.toFileName(outputFile, dir, "bxnez");
			this.file = new FileBuilder(path);
			System.out.println("generating " + path + " ... ");
		}
	}

	public String getOutputFileName() {
		return FileBuilder.toFileName(outputFile, dir, "bxnez");
	}

	public void makeStandardform() {
		for (Production rule : grammar) {
			String nonterminalName = rule.getLocalName();
			int nonterminalId = convertNonterminalName(nonterminalName);
			elementsStack[stackTop] = new Elements();
			stackStates[stackTop] = new StackState();
			makeProductionFormat(rule);
			productionList[nonterminalId] = elementsStack[stackTop];
		}
	}

	public int convertNonterminalName(String nonterminalName) {
		for (int i = 0; i < productionId; i++) {
			if (nonterminalName.equals(productionNameList[i])) {
				return i;
			}
		}
		if (productionId == productionNameList.length) {
			String[] newList = new String[productionNameList.length * 2];
			System.arraycopy(productionNameList, 0, newList, 0, productionNameList.length);
			productionNameList = newList;
			Elements[] newList2 = new Elements[productionList.length * 2];
			System.arraycopy(productionList, 0, newList2, 0, productionList.length);
			productionList = newList2;
		}
		productionNameList[productionId] = nonterminalName;
		return productionId++;
	}

	public void reshapeStandardform() {
		for (int i = 0; i < productionId; i++) {
			productionList[i] = reshapeElements(productionList[i]);
		}
	}

	public Elements reshapeElements(Elements elements) {
		if (elements.hasCapturedElement()) {
			elementsStack[stackTop] = new Elements();
			stackStates[stackTop] = new StackState();
			CapturedElement captured = null;
			for (int i = 0; i < elements.size; i++) {
				if (elements.get(i) instanceof CapturedElement) {
					elementsStack[++stackTop] = new Elements();
					stackStates[stackTop] = new StackState();
					captured = (CapturedElement) elements.get(i);
				} else {
					elementsStack[stackTop].addElement(elements.get(i));
				}
			}
			capturedList[captured.id].right = elementsStack[stackTop--];
			capturedList[captured.id].left = elementsStack[stackTop];
			return new Elements(captured);
		}
		ChoiceElement choiceElement = elements.getChoiceElement();
		if (choiceElement != null) {
			for (int i = 0; i < choiceElement.branch.length; i++) {
				choiceElement.branch[i] = reshapeElements(choiceElement.branch[i]);
			}
		}
		return elements;
	}

	public void reshapeCaptured() throws UnTagedException {
		for (int i = 0; i < this.capturedList.length; i++) {
			if (this.capturedList[i] == null) {
				break;
			}
			capturedList[i].makeFormatSet(i);
			for (int j = 0; j < checkedTag.size; j++) {
				((TagElement) checkedTag.get(j)).unused = true;
			}
			checkedTag = new Elements();
		}
	}

	public void writeBxnez() throws UnLabeledException {
		checkedNullLabelTag = new boolean[tagId];
		for (int i = 0; i < this.capturedList.length; i++) {
			if (this.capturedList[i] == null) {
				break;
			}
			capturedList[i].writeFormat(i);
		}
		writeln("");
	}

	private FormatVisitor formatVisitor = new FormatVisitor();

	public void makeProductionFormat(Production rule) {
		formatVisitor.visit(rule.getExpression());
	}

	private class FormatVisitor extends Expression.Visitor {

		public void visit(Expression e) {
			e.visit(this, null);
		}

		@Override
		public Object visitNonTerminal(NonTerminal e, Object a) {
			Element nonterminal = new NonTerminalElement(convertNonterminalName(e.getLocalName()));
			if (inCapture(e.getLocalName())) {
				currentLeft = nonterminal;
			}
			addElement(nonterminal);
			return null;
		}

		@Override
		public Object visitEmpty(Empty e, Object a) {
			return null;
		}

		@Override
		public Object visitFail(Fail e, Object a) {
			return null;
		}

		@Override
		public Object visitByte(Byte e, Object a) {
			addElement(new ByteElement(e.byteChar));
			return null;
		}

		@Override
		public Object visitByteSet(ByteSet e, Object a) {
			boolean[] byteSet = e.byteMap;
			for (int i = 0; i < byteSet.length; i++) {
				if (byteSet[i]) {
					addElement(new ByteElement(i));
					break;
				}
			}
			return null;
		}

		@Override
		public Object visitAny(Any e, Object a) {
			return null;
		}

		@Override
		public Object visitMultiByte(MultiByte e, Object a) {
			byte[] byteSeq = e.byteSeq;
			for (int i = 0; i < byteSeq.length; i++) {
				addElement(new ByteElement(byteSeq[i]));
			}
			return null;
		}

		@Override
		public Object visitPair(Pair e, Object a) {
			visit(e.first);
			visit(e.next);
			return null;
		}

		@Override
		public Object visitSequence(Sequence e, Object a) {
			return null;
		}

		@Override
		public Object visitChoice(Choice e, Object a) {
			Elements[] branch = new Elements[e.size()];
			if (stackTop + 1 == elementsStack.length) {
				Elements[] newList = new Elements[elementsStack.length * 2];
				System.arraycopy(elementsStack, 0, newList, 0, elementsStack.length);
				elementsStack = newList;
				StackState[] newList2 = new StackState[stackStates.length * 2];
				System.arraycopy(stackStates, 0, newList2, 0, stackStates.length);
				stackStates = newList2;
			}
			for (int i = 0; i < e.size(); i++) {
				elementsStack[++stackTop] = new Elements();
				stackStates[stackTop] = new StackState();
				visit(e.get(i));
				branch[i] = elementsStack[stackTop--];
			}
			Element choice = new ChoiceElement(branch);
			currentLeft = choice;
			addElement(choice);
			return null;
		}

		@Override
		public Object visitOption(Option e, Object a) {
			if (stackTop + 1 == elementsStack.length) {
				Elements[] newList = new Elements[elementsStack.length * 2];
				System.arraycopy(elementsStack, 0, newList, 0, elementsStack.length);
				elementsStack = newList;
				StackState[] newList2 = new StackState[stackStates.length * 2];
				System.arraycopy(stackStates, 0, newList2, 0, stackStates.length);
				stackStates = newList2;
			}
			elementsStack[++stackTop] = new Elements();
			stackStates[stackTop] = new StackState();
			stackStates[stackTop].inOptional = true;
			visit(e.get(0));
			stackTop--;
			if (stackStates[stackTop + 1].left != null) {
				elementsStack[stackTop].remove(stackStates[stackTop + 1].left);
				Elements[] branch = { null, new Elements() };
				branch[0] = elementsStack[stackTop + 1];
				branch[1].addElement(stackStates[stackTop + 1].left);
				addElement(new ChoiceElement(branch));
			} else {
				addElement(new OptionElement(elementsStack[stackTop + 1]));
			}
			return null;
		}

		@Override
		public Object visitZeroMore(ZeroMore e, Object a) {
			if (stackTop + 1 == elementsStack.length) {
				Elements[] newList = new Elements[elementsStack.length * 2];
				System.arraycopy(elementsStack, 0, newList, 0, elementsStack.length);
				elementsStack = newList;
				StackState[] newList2 = new StackState[stackStates.length * 2];
				System.arraycopy(stackStates, 0, newList2, 0, stackStates.length);
				stackStates = newList2;
			}
			elementsStack[++stackTop] = new Elements();
			stackStates[stackTop] = new StackState();
			visit(e.get(0));
			stackTop--;
			if (stackStates[stackTop + 1].left != null) {
				elementsStack[stackTop].remove(stackStates[stackTop + 1].left);
				Elements[] branch = { null, new Elements() };
				branch[0] = elementsStack[stackTop + 1];
				branch[1].addElement(stackStates[stackTop + 1].left);
				addElement(new ChoiceElement(branch));
			} else {
				addElement(new ZeroElement(elementsStack[stackTop + 1]));
			}
			return null;
		}

		@Override
		public Object visitOneMore(OneMore e, Object a) {
			if (stackTop + 1 == elementsStack.length) {
				Elements[] newList = new Elements[elementsStack.length * 2];
				System.arraycopy(elementsStack, 0, newList, 0, elementsStack.length);
				elementsStack = newList;
				StackState[] newList2 = new StackState[stackStates.length * 2];
				System.arraycopy(stackStates, 0, newList2, 0, stackStates.length);
				stackStates = newList2;
			}
			elementsStack[++stackTop] = new Elements();
			stackStates[stackTop] = new StackState();
			visit(e.get(0));
			stackTop--;
			if (stackStates[stackTop + 1].left != null) {
				elementsStack[stackTop].remove(stackStates[stackTop + 1].left);
				addElement(elementsStack[stackTop + 1].get(0));
			} else {
				addElement(new OneElement(elementsStack[stackTop + 1]));
			}
			return null;
		}

		@Override
		public Object visitAnd(And e, Object a) {
			return null;
		}

		@Override
		public Object visitNot(Not e, Object a) {
			return null;
		}

		@Override
		public Object visitBeginTree(BeginTree e, Object a) {
			if (stackTop + 1 == elementsStack.length) {
				Elements[] newList = new Elements[elementsStack.length * 2];
				System.arraycopy(elementsStack, 0, newList, 0, elementsStack.length);
				elementsStack = newList;
				StackState[] newList2 = new StackState[stackStates.length * 2];
				System.arraycopy(stackStates, 0, newList2, 0, stackStates.length);
				stackStates = newList2;
			}
			elementsStack[++stackTop] = new Elements();
			stackStates[stackTop] = new StackState();
			return null;
		}

		@Override
		public Object visitFoldTree(FoldTree e, Object a) {
			stackStates[stackTop].left = currentLeft;
			if (stackTop + 1 == elementsStack.length) {
				Elements[] newList = new Elements[elementsStack.length * 2];
				System.arraycopy(elementsStack, 0, newList, 0, elementsStack.length);
				elementsStack = newList;
				StackState[] newList2 = new StackState[stackStates.length * 2];
				System.arraycopy(stackStates, 0, newList2, 0, stackStates.length);
				stackStates = newList2;
			}
			elementsStack[++stackTop] = new Elements();
			stackStates[stackTop] = new StackState();
			addElement(new LinkedElement(e.label, new Elements(currentLeft)));
			return null;
		}

		@Override
		public Object visitLinkTree(LinkTree e, Object a) {
			if (stackTop + 1 == elementsStack.length) {
				Elements[] newList = new Elements[elementsStack.length * 2];
				System.arraycopy(elementsStack, 0, newList, 0, elementsStack.length);
				elementsStack = newList;
				StackState[] newList2 = new StackState[stackStates.length * 2];
				System.arraycopy(stackStates, 0, newList2, 0, stackStates.length);
				stackStates = newList2;
			}
			elementsStack[++stackTop] = new Elements();
			stackStates[stackTop] = new StackState();
			visit(e.get(0));
			stackTop--;
			addElement(new LinkedElement(e.label, elementsStack[stackTop + 1]));
			return null;
		}

		@Override
		public Object visitTag(Tag e, Object a) {
			String tagName = "#" + e.symbol();
			for (int i = 0; i < tagId; i++) {
				if (tagName.equals(tagList[i])) {
					addElement(new TagElement(i));
					return null;
				}
			}
			if (tagId == tagList.length) {
				String[] newList = new String[tagList.length * 2];
				System.arraycopy(tagList, 0, newList, 0, tagList.length);
				tagList = newList;
			}
			tagList[tagId] = tagName;
			addElement(new TagElement(tagId++));
			return null;
		}

		@Override
		public Object visitReplace(Replace e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitEndTree(EndTree e, Object a) {
			if (capturedId == capturedList.length) {
				Captured[] newList = new Captured[capturedList.length * 2];
				System.arraycopy(capturedList, 0, newList, 0, capturedList.length);
				capturedList = newList;
			}
			capturedList[capturedId] = new Captured(elementsStack[stackTop--]);
			if (stackStates[stackTop].left != null && !stackStates[stackTop].inOptional) {
				Elements[] branch = { new Elements(), new Elements() };
				branch[0].addElement(new CapturedElement(capturedId));
				branch[1].addElement(stackStates[stackTop].left);
				((LinkedElement) capturedList[capturedId].elements.get(0)).inner = new Elements(new ChoiceElement(branch));
			}
			Element captured = new CapturedElement(capturedId++);
			currentLeft = captured;
			addElement(captured);
			return null;
		}

		@Override
		public Object visitDetree(Detree e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitBlockScope(BlockScope e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitLocalScope(LocalScope e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitSymbolAction(SymbolAction e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitSymbolPredicate(SymbolPredicate e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitSymbolMatch(SymbolMatch e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitSymbolExists(SymbolExists e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitIf(IfCondition e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitOn(OnCondition e, Object a) {
			visit(e.inner);
			return null;
		}

		@Override
		public Object visitScan(Scan scanf, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitRepeat(Repeat e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object visitLabel(Label e, Object a) {
			// TODO Auto-generated method stub
			return null;
		}

	}

	// TODO modify me
	public boolean inCapture(String name) {
		byte cbyte = (byte) name.charAt(0);
		if (cbyte == 34 || cbyte == 95 || (cbyte >= 97 && cbyte <= 122)) {
			return false;
		}
		for (int i = 0; i < name.length(); i++) {
			cbyte = (byte) name.charAt(i);
			if (cbyte < 65 || cbyte > 90) {
				return true;
			}
		}
		return false;
	}

	public void addElement(Element element) {
		elementsStack[stackTop].addElement(element);
	}

	class Captured {
		Elements elements;
		TagAndLinks[] formatSet = new TagAndLinks[4];
		Elements left;
		Elements right;
		int size = 0;

		public Captured(Elements elements) {
			this.elements = elements;
		}

		public void makeFormatSet(int capturedId) throws UnTagedException {
			while (true) {
				checkedProduction = new boolean[productionId];
				int tag = elements.searchTag();
				if (tag != -1) {
					if (size == formatSet.length) {
						TagAndLinks[] newList = new TagAndLinks[formatSet.length * 2];
						System.arraycopy(formatSet, 0, newList, 0, formatSet.length);
						formatSet = newList;
					}
					formatSet[size] = new TagAndLinks();
					formatSet[size].tag = tag;
					checkedProduction = new boolean[productionId];
					formatSet[size++].link = elements.searchLink();
				} else {
					break;
				}
			}
		}

		// TODO fix verbose
		public void writeFormat(int capturedId) throws UnLabeledException {
			for (int i = 0; i < size; i++) {
				// if (formatSet[i].link != null ||
				// !checkedNullLabelTag[formatSet[i].tag]) {
				for (int labelProgression = 0, tagProgression = 0; true; tagProgression++) {
					hasRepetition = false;
					LabelSet labelSet = new LabelSet(labelProgression, tagProgression);
					String label = optionFix(formatSet[i].link, labelSet, formatSet[i].tag);
					if (labelSet.labelProgression > 0) {
						break;
					}
					if (labelSet.tagProgression > 0) {
						labelProgression++;
						tagProgression = -1;
						continue;
					}
					if (label == null) {
						label = "";
						checkedNullLabelTag[formatSet[i].tag] = true;
					}
					if (hasRepetition) {
						write("format " + tagList[formatSet[i].tag] + "(n" + label + ":ns)");
					} else {
						write("format " + tagList[formatSet[i].tag] + "(" + label + ")");
					}
					writeln("`");
					if (left != null) {
						String format = left.toFormat(formatSet[i].tag);
						if (format != null) {
							write(format);
						}
					}
					String format = toFormat(formatSet[i].tag);
					if (format == null) {
						write("${$value}");
					} else {
						write(format);
					}
					if (right != null) {
						format = right.toFormat(formatSet[i].tag);
						if (format != null) {
							write(format);
						}
					}
					write("`");
					writeln("");
				}
				activateDelay();
				// }
			}
		}

		public String optionFix(Elements links, LabelSet labelSet, int tag) throws UnLabeledException {
			if (links == null) {
				return null;
			}
			for (int i = 0; i < links.size; i++) {
				Element link = links.get(i);
				if (link instanceof OneElement || link instanceof ZeroElement) {
					hasRepetition = true;
					break;
				}
			}
			for (int i = 0; i < links.size; i++) {
				Element link = links.get(i);
				labelSet = link.optionFix(labelSet, tag);
			}
			return labelSet.label;
		}

		public String toFormat(int tag) throws UnLabeledException {
			String formats = elements.toFormat(tag);
			if (formats == null || formats.indexOf("${") == -1) {
				return null;
			}
			return formats;
		}

		@Override
		public String toString() {
			return this.elements.toString();
		}
	}

	class Elements {
		Element[] elementList;
		int size;

		public Elements() {
			elementList = new Element[4];
			size = 0;
		}

		public Elements(Element element) {
			elementList = new Element[4];
			elementList[0] = element;
			size = 1;
		}

		public Element get(int i) {
			return elementList[i];
		}

		public int searchTag() {
			for (int i = 0; i < this.size; i++) {
				int tag = elementList[i].searchTag();
				if (tag != -1) {
					return tag;
				}
			}
			return -1;
		}

		public Elements searchLink() throws UnTagedException {
			Elements links = null;
			for (int i = 0; i < this.size; i++) {
				Elements link = elementList[i].searchLink();
				if (link != null) {
					if (links == null) {
						links = link;
					} else {
						links.addElements(link);
					}
				}
			}
			return links;
		}

		public LinkedInner[] checkInner() {
			LinkedInner[] inners = null;
			for (int i = 0; i < size; i++) {
				if (elementList[i] != null) {
					LinkedInner[] inner = elementList[i].checkInner();
					if (inner != null) {
						if (inners == null) {
							inners = inner;
						} else {
							LinkedInner[] newList = new LinkedInner[inner.length * inners.length];
							for (int j = 0; j < inners.length; j++) {
								for (int k = 0; k < inner.length; k++) {
									newList[j * inner.length + k] = new LinkedInner();
									newList[j * inner.length + k].join(inners[j]);
									newList[j * inner.length + k].join(inner[k]);
								}
							}
							inners = newList;
						}
					}
				}
			}
			return inners;
		}

		public String toFormat(int tag) throws UnLabeledException {
			String formats = null;
			for (int i = 0; i < this.size; i++) {
				Element element = elementList[i];
				String format = element.toFormat(tag);
				if (format != null) {
					if (formats == null) {
						formats = format;
					} else {
						formats += format;
					}
				}
			}
			return formats;
		}

		public boolean hasCapturedElement() {
			for (int i = 0; i < size; i++) {
				if (elementList[i] instanceof CapturedElement) {
					return true;
				}
			}
			return false;
		}

		public ChoiceElement getChoiceElement() {
			for (int i = 0; i < size; i++) {
				if (elementList[i] instanceof ChoiceElement) {
					return (ChoiceElement) elementList[i];
				}
			}
			return null;
		}

		public void addElement(Element element) {
			if (size == elementList.length) {
				Element[] newList = new Element[elementList.length * 2];
				System.arraycopy(elementList, 0, newList, 0, elementList.length);
				elementList = newList;
			}
			elementList[size++] = element;
		}

		public void addElements(Elements elements) {
			for (int i = 0; i < elements.size; i++) {
				addElement(elements.elementList[i]);
			}
		}

		public void remove(Element element) {
			for (int i = 0; i < size; i++) {
				if (elementList[i] == element) {
					size--;
					System.arraycopy(elementList, i + 1, elementList, i, size - i);
					break;
				}
			}
		}

		@Override
		public String toString() {
			if (elementList[0] == null) {
				return " ";
			}
			String text = elementList[0].toString();
			for (int i = 1; i < elementList.length; i++) {
				if (elementList[i] == null) {
					break;
				}
				text += " ";
				text += elementList[i].toString();
			}
			return text;
		}
	}

	abstract class Element {
		public int searchTag() {
			return -1;
		}

		public Elements searchLink() throws UnTagedException {
			return null;
		}

		public LinkedInner[] checkInner() {
			return null;
		}

		public LabelSet optionFix(LabelSet labelSet, int tag) throws UnLabeledException {
			assert true;
			return null;
		}

		public int countOption(int tag) {
			return 1;
		}

		public int[] checkNeedTag() {
			return null;
		}

		public String toFormat(int tag) throws UnLabeledException {
			return null;
		}

		public boolean hasUnclarity() {
			return false;
		}

		@Override
		abstract public String toString();
	}

	class NonTerminalElement extends Element {
		int id;
		Elements elements;

		public NonTerminalElement(int id) {
			this.id = id;
		}

		@Override
		public Elements searchLink() throws UnTagedException {
			nullCheck();
			if (checkedProduction[id]) {
				return null;
			}
			checkedProduction[id] = true;
			return elements.searchLink();
		}

		@Override
		public int searchTag() {
			nullCheck();
			if (checkedProduction[id]) {
				return -1;
			}
			checkedProduction[id] = true;
			return elements.searchTag();
		}

		@Override
		public LinkedInner[] checkInner() {
			nullCheck();
			if (checkedProduction[id]) {
				return null;
			}
			checkedProduction[id] = true;
			return elements.checkInner();
		}

		@Override
		public String toFormat(int tag) throws UnLabeledException {
			nullCheck();
			return elements.toFormat(tag);
		}

		@Override
		public String toString() {
			return "[" + productionNameList[id] + "]";
		}

		public void nullCheck() {
			if (elements == null) {
				this.elements = productionList[id];
			}
		}
	}

	class CapturedElement extends Element {
		int id;

		public CapturedElement(int id) {
			this.id = id;
		}

		@Override
		public LinkedInner[] checkInner() {
			LinkedInner[] ret = { new LinkedInner() };
			ret[0].id = this.id;
			return ret;
		}

		@Override
		public String toString() {
			return "[" + this.id + "]";
		}
	}

	class LinkedElement extends Element {
		Symbol label;
		Elements inner;
		LinkedInner[] linkedInner;
		int[] groupId;
		int[] mainId = new int[4];
		int size;
		int groupSize = 0;
		int labelFix;

		public LinkedElement(Symbol label, Elements inner) {
			this.label = label;
			this.inner = inner;
		}

		@Override
		public Elements searchLink() throws UnTagedException {
			if (linkedInner == null) {
				boolean[] currentCheckedNonterminal = checkedProduction;
				checkedProduction = new boolean[productionId];
				linkedInner = inner.checkInner();
				if (linkedInner == null) {
					throw new UnTagedException("UNTAGGED BLOCK EXISTS in " + this);
					// System.out.println("CAUTION:UNTAGGED BLOCK EXISTS in " +
					// this);
					// linkedInner = new LinkedInner[1];
					// linkedInner[0] = new LinkedInner();
					// linkedInner[0].id = -2;
				}
				checkedProduction = currentCheckedNonterminal;
				size = linkedInner.length;
				optimizeLinkedInner();
			}
			return new Elements(this);
		}

		public void optimizeLinkedInner() {
			boolean[] checked = new boolean[capturedId];
			LinkedInner[] newLinkedinner = new LinkedInner[size];
			int newSize = 0;
			for (int i = 0; i < size; i++) {
				if (linkedInner[i].id == -2) {
					groupSize = 1;
					return;
				}
				if (linkedInner[i].id != -1 && !checked[linkedInner[i].id]) {
					checked[linkedInner[i].id] = true;
					newLinkedinner[newSize] = linkedInner[i];
					newSize++;
				}
			}
			linkedInner = new LinkedInner[newSize];
			System.arraycopy(newLinkedinner, 0, linkedInner, 0, newSize);
			size = newSize;
			groupId = new int[size];
			Arrays.fill(groupId, -1);
			for (int i = 0; i < size; i++) {
				for (int j = 0; j < i; j++) {
					if (linkedInner[i].before.equals(linkedInner[j].before) && linkedInner[i].after.equals(linkedInner[j].after)) {
						groupId[i] = j;
						break;
					}
				}
				if (groupId[i] == -1) {
					if (groupSize == mainId.length) {
						int[] newList = new int[mainId.length * 2];
						System.arraycopy(mainId, 0, newList, 0, mainId.length);
						mainId = newList;
					}
					mainId[groupSize++] = i;
					groupId[i] = i;
				}
			}
		}

		@Override
		public LabelSet optionFix(LabelSet labelSet, int tag) throws UnLabeledException {
			labelFix = mainId[labelSet.tagProgression % groupSize];
			labelSet.tagProgression = labelSet.tagProgression / groupSize;
			boolean[] needTag = new boolean[tagId];
			String ret = null;
			if (linkedInner[labelFix].id == -2) {
				if (hasRepetition) {
					ret = " #Tree";
				} else {
					ret = toLabel() + " #Tree";
				}
			} else {
				TagAndLinks[] formatSet = capturedList[linkedInner[labelFix].id].formatSet;
				needTag[formatSet[0].tag] = true;
				for (int i = 1; i < capturedList[linkedInner[labelFix].id].size; i++) {
					needTag[formatSet[i].tag] = true;
				}
				for (int i = 0; i < size; i++) {
					if (groupId[i] == labelFix && i != labelFix) {
						formatSet = capturedList[linkedInner[i].id].formatSet;
						for (int j = 0; j < capturedList[linkedInner[i].id].size; j++) {
							needTag[formatSet[j].tag] = true;
						}
					}
				}
				for (int i = 0; i < tagId; i++) {
					if (needTag[i]) {
						if (ret == null) {
							if (hasRepetition) {
								ret = " " + tagList[i];
							} else {
								ret = toLabel() + " " + tagList[i];
							}
						} else {
							ret += "|" + tagList[i];
						}
					}
				}
			}
			if (labelSet.label == null) {
				labelSet.label = ret;
			} else {
				labelSet.label += "," + ret;
			}
			return labelSet;
		}

		public String toLabel() throws UnLabeledException {
			if (label == null) {
				throw new UnLabeledException("UNLABELED LINKTREE EXISTS " + this);
				// return "$unlabeled";
			} else {
				return label.toString();
			}
		}

		@Override
		public String toFormat(int tag) throws UnLabeledException {
			String ret = "";

			if (hasRepetition) {
				if (!linkedInner[labelFix].before.equals("")) {
					ret += linkedInner[labelFix].before;
				}
				ret += "${n}";
				if (!linkedInner[labelFix].after.equals("")) {
					ret += linkedInner[labelFix].after;
				}
			} else if (label == null) {
				throw new UnLabeledException("UNLABELED LINKTREE EXISTS " + this);
				// if (!linkedInner[labelFix].before.equals("")) {
				// ret += linkedInner[labelFix].before;
				// }
				// ret += "${$unlabeled}";
				// if (!linkedInner[labelFix].after.equals("")) {
				// ret += linkedInner[labelFix].after;
				// }
			} else {
				if (!linkedInner[labelFix].before.equals("")) {
					ret += linkedInner[labelFix].before;
				}
				ret += "${" + label + "}";
				if (!linkedInner[labelFix].after.equals("")) {
					ret += linkedInner[labelFix].after;
				}
			}

			return ret;
		}

		@Override
		public String toString() {
			return "$" + this.label + this.inner;
		}
	}

	class ByteElement extends Element {
		char cByte;

		public ByteElement(char cByte) {
			this.cByte = cByte;
		}

		public ByteElement(int byteChar) {
			cByte = (char) byteChar;
		}

		public ByteElement(byte byteChar) {
			cByte = (char) byteChar;
		}

		@Override
		public LinkedInner[] checkInner() {
			LinkedInner[] ret = { new LinkedInner() };
			ret[0].before = String.valueOf(cByte);
			return ret;
		}

		@Override
		public String toFormat(int tag) {
			if (cByte == '\b') {
				return "\\b";
			} else if (cByte == '\t') {
				return "\\t";
			} else if (cByte == '\n') {
				return "\\n";
			} else if (cByte == '\f') {
				return "\\f";
			} else if (cByte == '\r') {
				return "\\r";
			} else {
				return String.valueOf(cByte);
			}
		}

		@Override
		public String toString() {
			if (cByte == '\b') {
				return "\\b";
			} else if (cByte == '\t') {
				return "\\t";
			} else if (cByte == '\n') {
				return "\\n";
			} else if (cByte == '\f') {
				return "\\f";
			} else if (cByte == '\r') {
				return "\\r";
			} else {
				return String.valueOf(cByte);
			}
		}
	}

	class TagElement extends Element {
		int id;
		boolean unused = true;

		public TagElement(int id) {
			this.id = id;
		}

		@Override
		public int searchTag() {
			if (unused) {
				checkedTag.addElement(this);
				unused = false;
				return id;
			}
			return -1;
		}

		@Override
		public String toString() {
			return tagList[id];
		}
	}

	class ChoiceElement extends Element {
		Elements[] branch;
		Elements[] link;
		int rate;
		int[] linkRate;
		int currentTagFixBranch;
		int[] tagFixBranch;
		int linkFixBranch = -1;
		boolean hasNullBranch = false;

		public ChoiceElement(Elements[] branch) {
			this.branch = branch;
		}

		@Override
		public int searchTag() {
			if (tagFixBranch == null) {
				tagFixBranch = new int[tagId];
				Arrays.fill(tagFixBranch, -1);
			}
			for (int i = 0; i < branch.length; i++) {
				int tag = branch[i].searchTag();
				if (tag != -1) {
					currentTagFixBranch = i;
					tagFixBranch[tag] = i;
					return tag;
				}
			}
			currentTagFixBranch = -1;
			return -1;
		}

		@Override
		public Elements searchLink() throws UnTagedException {
			if (currentTagFixBranch == -1) {
				for (int i = 0; i < branch.length; i++) {
					Elements choicedLink = branch[i].searchLink();
					if (choicedLink != null) {
						if (link == null) {
							link = new Elements[branch.length];
						}
						link[i] = choicedLink;
					}
				}
				if (link == null) {
					return null;
				}
				return new Elements(this);
			}
			return branch[currentTagFixBranch].searchLink();
		}

		@Override
		public LinkedInner[] checkInner() {
			LinkedInner[] inners = null;
			for (int i = 0; i < branch.length; i++) {
				LinkedInner[] inner = branch[i].checkInner();
				if (inner != null) {
					if (inners == null) {
						inners = inner;
					} else {
						LinkedInner[] newList = new LinkedInner[inner.length + inners.length];
						System.arraycopy(inners, 0, newList, 0, inners.length);
						System.arraycopy(inner, 0, newList, inners.length, inner.length);
						inners = newList;
					}
				}
			}
			return inners;
		}

		@Override
		public LabelSet optionFix(LabelSet labelSet, int tag) throws UnLabeledException {
			if (linkRate == null) {
				countOption(tag);
			}
			int generalProgression = labelSet.labelProgression / rate;
			labelSet.labelProgression = labelSet.labelProgression % rate;
			for (int i = 0; i <= linkRate.length; i++) {
				if (i == linkRate.length) {
					linkFixBranch = -1;
					break;
				}
				if (labelSet.labelProgression < linkRate[i]) {
					for (int j = 0; j < link[i].size; j++) {
						labelSet = link[i].get(j).optionFix(labelSet, tag);
					}
					linkFixBranch = i;
					break;
				} else {
					labelSet.labelProgression -= linkRate[i];
				}
			}
			labelSet.labelProgression = generalProgression;
			return labelSet;
		}

		@Override
		public int countOption(int tag) {
			rate = 0;
			linkRate = new int[link.length];
			for (int i = 0; i < linkRate.length; i++) {
				if (link[i] != null) {
					linkRate[i] = 1;
					for (int j = 0; j < link[i].size; j++) {
						linkRate[i] *= link[i].get(j).countOption(tag);
					}
				} else {
					hasNullBranch = true;
				}
				rate += linkRate[i];
			}
			if (hasNullBranch) {
				rate++;
			}
			return rate;
		}

		@Override
		public String toFormat(int tag) throws UnLabeledException {
			if (tagFixBranch == null) {
				return branch[0].toFormat(tag);
			}
			if (tagFixBranch[tag] == -1) {
				if (linkFixBranch == -1) {
					return branch[0].toFormat(tag);
				} else {
					return branch[linkFixBranch].toFormat(tag);
				}
			} else {
				return branch[tagFixBranch[tag]].toFormat(tag);
			}
		}

		@Override
		public String toString() {
			String text = "( " + branch[0];
			for (int i = 1; i < branch.length; i++) {
				text += " / " + branch[i].toString();
			}
			return text + " )";
		}
	}

	class OneElement extends Element {
		Elements inner;
		Elements links;
		int rate;
		int id = -1;

		public OneElement(Elements inner) {
			this.inner = inner;
		}

		@Override
		public int searchTag() {
			return inner.searchTag();
		}

		@Override
		public Elements searchLink() throws UnTagedException {
			Elements oneLink = inner.searchLink();
			if (oneLink == null) {
				return null;
			}
			links = oneLink;
			return new Elements(this);
		}

		@Override
		public LabelSet optionFix(LabelSet labelSet, int tag) throws UnLabeledException {
			if (rate == 0) {
				countOption(tag);
			}
			if (id == -1) {
				id = repetitionId++;
				for (int labelProgression = 0, tagProgression = 0; true; tagProgression++) {
					LabelSet repetitionLabelSet = new LabelSet(labelProgression, tagProgression);
					for (int i = 0; i < links.size; i++) {
						Element link = links.get(i);
						repetitionLabelSet = link.optionFix(repetitionLabelSet, tag);
					}
					String label = repetitionLabelSet.label;
					if (repetitionLabelSet.labelProgression > 0) {
						break;
					}
					if (repetitionLabelSet.tagProgression > 0) {
						labelProgression++;
						tagProgression = -1;
						continue;
					}
					if (label == null) {
						label = "";
					}
					delayWrite("format rep" + id + "(n" + label + ":ns)");
					delayWriteln("`");
					String format = inner.toFormat(tag);
					if (format != null) {
						delayWrite(format + " ${rep" + id + "(ns)}`");
					} else {
						delayWrite("${rep" + id + "(ns)}`");
					}
					delayWriteln("");
					delayWriteln("");
				}
				delayWrite("format rep" + id + "([])");
				delayWriteln("``");
				delayWriteln("");
				delayWriteln("");
			}
			int generalProgression = labelSet.labelProgression / rate;
			labelSet.labelProgression = labelSet.labelProgression % rate;
			for (int i = 0; i < links.size; i++) {
				labelSet = links.get(i).optionFix(labelSet, tag);
			}
			labelSet.labelProgression = generalProgression;
			return labelSet;
		}

		@Override
		public int countOption(int tag) {
			rate = 1;
			if (links != null) {
				for (int j = 0; j < links.size; j++) {
					rate *= links.get(j).countOption(tag);
				}
			}
			return rate;
		}

		@Override
		public String toFormat(int tag) throws UnLabeledException {
			if (id == -1) {
				return inner.toFormat(tag);
			}
			String format = inner.toFormat(tag);
			if (format != null) {
				return format + " ${rep" + id + "(ns)}";
			}
			return "${rep" + id + "(ns)}";
		}

		@Override
		public String toString() {
			return "(" + this.inner + ")+";
		}
	}

	class ZeroElement extends Element {
		Elements inner;
		Elements links;
		boolean[] tagFix;
		int rate;
		int id = -1;

		public ZeroElement(Elements inner) {
			this.inner = inner;
		}

		@Override
		public int searchTag() {
			if (tagFix == null) {
				tagFix = new boolean[tagId];
			}
			int tag = inner.searchTag();
			if (tag != -1) {
				tagFix[tag] = true;
				return tag;
			}
			return -1;
		}

		@Override
		public Elements searchLink() throws UnTagedException {
			Elements zeroLink = inner.searchLink();
			if (zeroLink == null) {
				return null;
			}
			links = zeroLink;
			return new Elements(this);
		}

		@Override
		public LabelSet optionFix(LabelSet labelSet, int tag) throws UnLabeledException {
			if (rate == 0) {
				countOption(tag);
			}
			if (id == -1) {
				id = repetitionId++;
				for (int labelProgression = 0, tagProgression = 0; true; tagProgression++) {
					LabelSet repetitionLabelSet = new LabelSet(labelProgression, tagProgression);
					for (int i = 0; i < links.size; i++) {
						Element link = links.get(i);
						repetitionLabelSet = link.optionFix(repetitionLabelSet, tag);
					}
					String label = repetitionLabelSet.label;
					if (repetitionLabelSet.labelProgression > 0) {
						break;
					}
					if (repetitionLabelSet.tagProgression > 0) {
						labelProgression++;
						tagProgression = -1;
						continue;
					}
					if (label == null) {
						label = "";
					}
					delayWrite("format rep" + id + "(n" + label + ":ns)");
					delayWriteln("`");
					String format = inner.toFormat(tag);
					if (format != null) {
						delayWrite(inner.toFormat(tag) + " ${rep" + id + "(ns)}`");
					} else {
						delayWrite("${rep" + id + "(ns)}`");
					}
					delayWriteln("");
				}
				delayWrite("format rep" + id + "([])");
				delayWriteln("``");
				delayWriteln("");
			}
			if (tagFix[tag]) {
				int generalProgression = labelSet.labelProgression / rate;
				labelSet.labelProgression = labelSet.labelProgression % rate;
				for (int i = 0; i < links.size; i++) {
					labelSet = links.get(i).optionFix(labelSet, tag);
				}
				labelSet.labelProgression = generalProgression;
			}
			return labelSet;
		}

		@Override
		public int countOption(int tag) {
			rate = 1;
			if (links != null) {
				for (int j = 0; j < links.size; j++) {
					rate *= links.get(j).countOption(tag);
				}
			}
			return rate;
		}

		@Override
		public String toFormat(int tag) throws UnLabeledException {
			if (id == -1) {
				return null;
			}
			if (tagFix[tag]) {
				String format = inner.toFormat(tag);
				if (format != null) {
					return format + " ${rep" + id + "(ns)}";
				}
			}
			return "${rep" + id + "(ns)}";
		}

		@Override
		public String toString() {
			return "(" + this.inner + ")*";
		}
	}

	class OptionElement extends Element {
		Elements inner;
		Elements link;
		boolean hasTag;
		boolean[] tagFix;
		int rate;
		boolean linkFix = false;

		public OptionElement(Elements inner) {
			this.inner = inner;
		}

		@Override
		public int searchTag() {
			if (tagFix == null) {
				tagFix = new boolean[tagId];
			}
			int tag = inner.searchTag();
			if (tag != -1) {
				tagFix[tag] = true;
				hasTag = true;
				return tag;
			}
			hasTag = false;
			return -1;
		}

		@Override
		public Elements searchLink() throws UnTagedException {
			Elements optionalLink = inner.searchLink();
			if (hasTag) {
				return optionalLink;
			}
			if (optionalLink == null) {
				return null;
			}
			link = optionalLink;
			return new Elements(this);
		}

		@Override
		public LabelSet optionFix(LabelSet labelSet, int tag) throws UnLabeledException {
			if (rate == 0) {
				countOption(tag);
			}
			int currentBranch = labelSet.labelProgression % rate;
			int generalProgression = labelSet.labelProgression / rate;
			if (currentBranch != rate - 1) {
				labelSet.labelProgression = currentBranch;
				for (int j = 0; j < link.size; j++) {
					Element l = link.get(j);
					if (l instanceof OneElement || l instanceof ZeroElement) {
						hasRepetition = true;
						break;
					}
				}
				for (int j = 0; j < link.size; j++) {
					labelSet = link.get(j).optionFix(labelSet, tag);
				}
				linkFix = true;
			} else {
				linkFix = false;
			}
			labelSet.labelProgression = generalProgression;
			return labelSet;
		}

		@Override
		public int countOption(int tag) {
			rate = 1;
			if (link != null) {
				for (int j = 0; j < link.size; j++) {
					rate *= link.get(j).countOption(tag);
				}
			}
			return ++rate;
		}

		@Override
		public String toFormat(int tag) throws UnLabeledException {
			if (tagFix == null) {
				return null;
			}
			if (!tagFix[tag]) {
				if (!linkFix) {
					return null;
				}
				return inner.toFormat(tag);
			}
			return inner.toFormat(tag);
		}

		@Override
		public String toString() {
			return "(" + this.inner + ")?";
		}

	}

	class StackState {
		Element left = null;
		boolean inOptional = false;
	}

	class LabelSet {
		String label;
		int labelProgression;
		int tagProgression;

		public LabelSet(int labelProgression, int tagProgression) {
			this.labelProgression = labelProgression;
			this.tagProgression = tagProgression;
		}
	}

	class LinkedInner {
		int id = -1;
		String before = "";
		String after = "";

		public void join(LinkedInner target) {
			if (target.id != -1) {
				assert(id == -1);
				this.id = target.id;
				this.before += target.before;
				this.after = target.after;
			} else {
				if (this.id == -1) {
					this.before += target.before;
				} else {
					this.after += target.before;
				}
			}
		}

		@Override
		public String toString() {
			return before + "{" + id + "}" + after;
		}
	}

	class TagAndLinks {
		int tag;
		Elements link;
	}

	public void writeln(String line) {
		file.writeIndent(line);
	}

	public void write(String word) {
		file.write(word);
	}

	private String delayString = "";

	public void delayWriteln(String line) {
		delayString += "\n" + line;
	}

	public void delayWrite(String word) {
		delayString += word;
	}

	public void activateDelay() {
		file.write(delayString);
		delayString = "";
	}
}
