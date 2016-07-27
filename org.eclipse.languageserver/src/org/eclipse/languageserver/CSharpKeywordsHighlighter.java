package org.eclipse.languageserver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

public class CSharpKeywordsHighlighter extends PresentationReconciler {

	private Set<String> keywords = new HashSet<>(Arrays.asList(new String[] {
		"abstract", 
		"as",
		"base",
		"bool",
		"break",
		"byte",
		"case",
		"catch",
		"char",
		"checked",
		"class",
		"const",
		"continue",
		"decimal",
		"default",
		"delegate",
		"do",
		"double",
		"else",
		"enum",
		"event",
		"explicit",
		"extern",
		"false",
		"finally",
		"fixed",
		"float",
		"for",
		"foreach",
		"goto",
		"if",
		"implicit",
		"in",
		"int",
		"interface",
		"internal",
		"is",
		"lock",
		"long",
		"namespace",
		"new",
		"null",
		"object",
		"operator",
		"out",
		"override",
		"params",
		"private",
		"protected",
		"public",
		"readonly",
		"ref",
		"return",
		"sbyte",
		"sealed",
		"short",
		"sizeof",
		"stackalloc",
		"static",
		"string",
		"struct",
		"switch",
		"this",
		"throw",
		"true",
		"try",
		"typeof",
		"uint",
		"ulong",
		"unchecked",
		"unsafe",
		"ushort",
		"using",
		"virtual",
		"void",
		"volatile",
		"while",
	}));
	
	private Set<String> contextualKeywords = new HashSet<>(Arrays.asList(new String[] {
		"add",
		"alias",
		"ascending",
		"async",
		"await",
		"descending",
		"dynamic",
		"from",
		"get",
		"global",
		"group",
		"into",
		"join",
		"let",
		"orderby",
		"partial",
		"remove",
		"select",
		"set",
		"value",
		"var",
		"where",
		"yield",
	}));

	public CSharpKeywordsHighlighter() {
		RuleBasedScanner scanner= new RuleBasedScanner();
		Token keywordToken = new Token(new TextAttribute(Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE), null, SWT.BOLD));
		WordRule wordRule = new WordRule(new IWordDetector() {
			@Override
			public boolean isWordStart(char c) {
				return Character.isAlphabetic(c);
			}
			
			@Override
			public boolean isWordPart(char c) {
				return Character.isAlphabetic(c) || Character.isDigit(c);
			}
		});
		for (String keyword : keywords) {
			wordRule.addWord(keyword, keywordToken);
		}
		scanner.setRules(new IRule[] { wordRule });
		DefaultDamagerRepairer dr= new DefaultDamagerRepairer(scanner);
		this.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		this.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);
	}
}
