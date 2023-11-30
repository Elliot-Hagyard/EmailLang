import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Scanner;
import java.util.HashSet;
import java.util.HashMap;
import java.io.File;
import java.io.FileNotFoundException; 
import java.util.Scanner; 
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.io.PrintWriter;

public class Parser {

	enum Type {
		BOOL,
		INT,
		CHAR,
		STRING,
		WRONG
	}

	// managing scope in functions?
	// declaring a new function for parser
	// Subject: Names...
	private static Pattern removeWhiteSpace = Pattern.compile(".+");
	private String javaFile;

	private Pattern prolog = Pattern.compile("(^(Dear)( [BICS]([a-zA-Z]+), )+|To whom it may concern, )");
	// change epilog?
	private Pattern epilog = Pattern.compile("(Best, ([BICS]([a-zA-Z]+)) )$");
	private Pattern sentence = Pattern.compile(".+?(\\.|!)");
	// THIS MIGHT CAUSE BUGS?
	private Pattern statement = Pattern.compile("(.+)[^.!]");
	private Pattern equality = Pattern.compile("^(.+) says (.+)\\.");
	private Pattern varAssign = Pattern.compile("^([a-zA-Z]+) said (.+)$");
	
	private Pattern intInc = Pattern.compile("^piggybacking off of (.+)$");

	private Pattern intDec = Pattern.compile("^drill down on (.+)$");
	private Pattern intSub = Pattern.compile("^(.+) drill down on (.+)$");
	private Pattern intMult = Pattern.compile("^(.+) joins forces with (.+)$");
	private Pattern intDiv = Pattern.compile("^(.+) leverages (.+)$");
	
	private Pattern int_expr = Pattern.compile("((.+) piggybacking off of (.+)|^(.+) drill down on (.+))");
	private Pattern int_expr_2 = Pattern.compile("([^\\(]?.+) drill down on ([^\\(]?.+?)");
	private Pattern int_expr1 = Pattern.compile("(^(.+) joins forces with (.+)|^(.+) leverages (.+))");

	private Pattern bool_expr1 = Pattern.compile("(.+) or (.+)"); // OR
	private Pattern bool_expr2 = Pattern.compile("^(.+) and (.+)$"); // AND
	private Pattern bool_expr3= Pattern.compile("^not (.+)"); // NOT

	private Pattern comp_expr = Pattern.compile("((.+) is on the same page as (.+)|(.+) greater than (.+)|(.+) less than (.+))");
	private Pattern loop_start = Pattern.compile("^Suppose (.+):");
	private Pattern conditional = Pattern.compile("^Suppose (.+): then (.+); otherwise, (.+)$");
	
	private Pattern loop = Pattern.compile("^Keep (.+) in the loop regarding: (.+)");
	private Pattern list = Pattern.compile("(.+?), (.+)");
	
	private Pattern print = Pattern.compile("[hH]{1}ighlight (.+)");
	
	private Pattern var = Pattern.compile("([BICS]([a-zA-Z]+))");
	private Pattern boolVar = Pattern.compile("^B.+$");
	private Pattern intVar = Pattern.compile("^I.+$");
	private Pattern charVar = Pattern.compile("^C.+$");
	private Pattern stringVar = Pattern.compile("^S.+$");
	
	private Pattern boolVal = Pattern.compile("^yep$|^nope$");
	private Pattern intVal = Pattern.compile("^\\d+$");
	private Pattern charVal = Pattern.compile("^[a-zA-Z]$");
	private Pattern stringVal = Pattern.compile("^\"[a-zA-Z]+\"$");
	
	private HashSet<String> ints;
	private HashSet<String> strings;
	private HashSet<String> bools;
	private HashSet<String> chars;
	private HashMap<String, String> operations;

	Parser() {
		ints = new HashSet<>();
		strings = new HashSet<>();
		bools = new HashSet<>();
		chars = new HashSet<>();
		// Defining a map of all operations
		String [][] opPairs = {
			{"piggybacking off of", "+"},
			{"drill down on", "-"},
			{"joins forces with", "*"},
			{"leverages", "/"},
			{"or", "||"},
			{"and", "&&"},
			{"not", "!"},
			{"is on the same page as", "=="},
			{"greater than", ">"},
			{"less than", "<"}
		};
		operations = new HashMap<>();
		for (String[] pair : opPairs) {
            operations.put(pair[0], pair[1]);
        }
	}
	
	Parser(String filename) {
		ints = new HashSet<>();
		strings = new HashSet<>();
		bools = new HashSet<>();
		chars = new HashSet<>();
		// Defining a map of all operations
		String [][] opPairs = {
			{"piggybacking off of", "+"},
			{"drill down on", "-"},
			{"joins forces with", "*"},
			{"leverages", "/"},
			{"or", "||"},
			{"and", "&&"},
			{"not", "!"},
			{"is on the same page as", "=="},
			{"greater than", ">"},
			{"less than", "<"}
		};
		operations = new HashMap<>();
		for (String[] pair : opPairs) {
            operations.put(pair[0], pair[1]);
        }
		
		String text = readFile(filename);

		// class name is file name
		javaFile =  "public class " + filename.substring(0,filename.length()-6) + "{\n";

		if (text == null) {
			// prints file name -- BUG?
			System.out.println("Invalid input file " + filename);
			return;
		}

		// System.out.println(text); // for debugging purposes

		try {
			javaFile += parseProlog(text);
			// System.out.println(javaFile); // for debugging
			String body = getBody(text);
			// System.out.println(body); // for debugging
			parseBody(body);
			javaFile += parseEpilog(text);
		}
		catch (SyntaxError e){
			System.out.println(e.getMessage());
		}
		javaFile += "\n}";

		// Final line to end class def
		javaFile += "\n}";

		try (PrintWriter out = new PrintWriter(filename.substring(0,filename.length()-6) +".java")) {
			out.println(javaFile);
		}
		catch (FileNotFoundException e){}
	}
	
	// main() code adapted from Parser.java from the class resources
	public static void main (String[] args) {
		if (args.length == 0) {
			// if no file is supplied, return
			System.out.println("Please input a file name.");
			return;
		}
		Parser parser = new Parser(args[0]);
    }
    
	/*
	 * Get the body of the email
	 */
    private String getBody(String text){
        Matcher pm = prolog.matcher(text);
        Matcher em = epilog.matcher(text);
        String p = "";
        String e = "";

        if(pm.find()) p = pm.group();
        if(em.find()) e = em.group();
		System.out.println(e);
		// substract out prologue and epilogue to get body of text
        text = text.substring(p.length(), text.length() - 1);
        text = text.substring(0, text.length() - e.length());

        return text;
    }
    
    private String parseBody(String text) throws SyntaxError {
        var sentences = this.sentence.matcher(text);
		String body = "";
		System.out.println(text);
		int idx = 0;
		try {
			while(sentences.find()) {
				String sentence = sentences.group();
				sentence = sentence.trim();
				// System.out.println(sentence.trim()); // debugging
				javaFile += parseSentence(sentence);
				idx++;
			}
		}
		catch(SyntaxError e){
			throw new SyntaxError(
				"Hey, sorry to bother you with this but" +
				" we found the following error in sentence" 
				+ (idx + 1) + ":\n" +
				e.getMessage()
			);
		}
        return body;
    }

    /*
	* Read the file into a string.
	*/
    private static String readFile(String filename){
        try {
			String text = Files.readString(Paths.get(filename));
			Matcher m = removeWhiteSpace.matcher(text);
			String t = "";
			while(m.find()){
				t += m.group().trim() + " ";
			}
			t = t.trim().replaceAll(" +", " ");
			// System.out.println(t); // debugging 
			return t;
		} catch (IOException e) {
			return null;
		}
    }
	
	private String parseEpilog(String text) throws SyntaxError{
		Matcher prologMatch = prolog.matcher(text);

		// No match found... throw error
		
		javaFile += "\n}";
		return "";
	}
	
	/*
	 * transpile the prologue.
	 */
	private String parseProlog(String text) throws SyntaxError{
		Matcher prologMatch = prolog.matcher(text);

		// No match found... throw error
		if(!prologMatch.find()){
			throw new SyntaxError("AHHH");
		}
		
		String functionStart = "";
		// Check that the correct number of command line arguments was supplied
		String opening = prologMatch.group();
		var var = this.var.matcher(opening); // get individual variable names from comma-separated list
		int idx = 0;

		String body = "";
		System.out.println(opening); // debugging

		while(var.find()){
			String curVar = var.group();

			if (curVar == "To whom it may concern,"){
				break;
			}

			switch (findVarType(curVar)){
				case BOOL:
					bools.add(curVar);
					body += "Boolean " + curVar + " = Boolean.valueOf(args[" + idx+ "]);\n";
				break;
				case INT:
					ints.add(curVar);
					body += "Integer " + curVar + " = Integer.valueOf(args[" + idx + "]);\n";
				break;
				case CHAR:
					chars.add(curVar);
					body += "Char " + curVar + " = Character.valueOf(args[" + idx + "]);\n";
				break;
				case WRONG:
					throw new SyntaxError(
						"We took issue with your addressing of " + curVar + "\n"
						+ "Your email must be addressed to person(s) with name(s) starting with B, I, or S.\n"
						+ "Please do better.\n"
						+ "Sincerely, the email-team."
					);
			}
			idx++;
		}

		functionStart += "public static void main(String[] args) {\n";
		functionStart += "if(args.length != " + (idx) + "){\n";
		functionStart += "System.out.println(";
		functionStart += "\"There was an error encountered in delivering the contents of your email\");\n";
		functionStart += "System.out.println(\"(this means that there were too few arguments supplied)\");\n";
		functionStart += "return;";
		functionStart += "}";
		return functionStart + body + "\n";
	}
	
	public String parseSentence(String cmd) throws SyntaxError {
		Matcher m = statement.matcher(cmd);
		String match = "";
		
		if (m.find()) {
			String expression = m.group();

			System.out.println(expression);
			match = varAssign(expression);
			System.out.println(match);
			System.out.println(match.length());
			if(match.length() > 0) return match;
			System.out.println("HEre");
			match = print(expression);
			if (match.length() >  0) return match;
			match = evalExpr(expression);
			if(match.length() > 0) return match;
			match = condition(expression);
			if(match.length() >  0) return match;
			match = loop(expression);
			if(match.length() > 0) return match;
			// if (!match) match = parseEquality(expression);
			// if (!match) match = parseIncrement(expression);
			// if (!match) match = parseAdd(expression);
			// if (!match) match = parseDecrement(expression);
			// if (!match) match = parseSubtract(expression);
			// if (!match) match = parseMultiply(expression);
			// if (!match) match = parseDivide(expression);
			throw new SyntaxError("Missing period.");
		}
		else {
			System.out.println("Syntax error: missing period.");
			throw new SyntaxError("Missing period.");
		}
	}

	private String toBool(String val) {
		if (val.equals("yep")) return "true";
		else if(val.equals("nope")) return "false";
		else return val;
	}

	private String print(String p) throws SyntaxError{
		Matcher m = print.matcher(p);
		if(!m.find())
			return "";
		String expr = m.group(1);
		String toString = evalExpr(expr);
		System.out.println(toString);
		return "System.out.println(" + toString + ");\n";
	}

	private String varAssign(String expression) throws SyntaxError {
		Matcher assignment = varAssign.matcher(expression);

		if (assignment.find()) {
			String var = assignment.group(1);
			String val = assignment.group(2);
			System.out.println(var);
			System.out.println(val);
			Type type = findVarType(var);
			System.out.println(type);
			// add declaration and assignment to output file
			switch(type) {
				case WRONG:
					return "";
				case BOOL:
					if (bools.contains(var)) return  var + " = " + parseBoolExpr(val) + ";\n";
					else {
						bools.add(var);
						System.out.printf("Assigning bool value of %s to variable name %s\n", val, var);
						return "boolean " + var + " = " + parseBoolExpr(val) + ";\n";
					}

				case INT:
					if (ints.contains(var)) return var + " = " + parseIntExpr(val) + ";\n";
					else {
						ints.add(var);
						System.out.printf("Assigning int value of %s to variable name %s\n", val, var);
						return "int " + var + " = " + parseIntExpr(val) + ";\n";
						
					}

				case CHAR:
					if (chars.contains(var)) return var + " = " + "'" + val + "'" + ";\n";
					else {
						chars.add(var);
						System.out.printf("Assigning char value of %s to variable name %s\n", val, var);

						return "char " + var + " = " + "'" + val + "'" + ";\n";
					}

				case STRING:
					if (strings.contains(var)) return var + " = " + "'" + val + "'" + ";\n";
					else {
						strings.add(var);
						System.out.printf("Assigning string value of %s to variable name %s\n", val, var);

						return "String " + var + " = " + "" + val + "" + ";\n";
					}
			}
		}

		return "";
	}
	
	private String evalExpr(String expr) throws SyntaxError{
		try {
			return parseIntExpr(expr);
		}
		catch(SyntaxError e){}
		try {
			return parseBoolExpr(expr);
		}
		catch(SyntaxError e){}
		try {
			return parseStringExpr(expr);
		}
		catch (SyntaxError e) {
			throw new SyntaxError(
				"Expration?"
			);
		}
	}
	
	
	private String condition(String cond) throws SyntaxError{
		Matcher m = conditional.matcher(cond);
		if(!m.find()){
			return "";
		}
		String condition = parseBoolExpr(m.group(1));
		String list = parseList(m.group(2));
		String otherwise = parseList(m.group(3));
		String out =  "if (" +condition + ")";
		out += "\n" + " {" + list + "}";
		out +=  "\n" + "else {" + otherwise + "}";
		return out;
	}
	
	private String loop(String loop) throws SyntaxError {
		Matcher m = this.loop.matcher(loop);
		if(!m.find()){
			return "";
		}
		String condition = parseBoolExpr(m.group(1));
		String list = parseList(m.group(2));
		String out =  "while (" +condition + ")";
		out += "\n" + " {" + list + "}";
		return out;
	}
	
	private String parseList(String in) throws SyntaxError{
		Matcher l = list.matcher(in);
		if(l.find()){
			String cur = parseSentence(l.group(1));
			String next = parseList(l.group(2));
			return  cur + "\n" + next;
		}
		return parseSentence(in);
	}
	
	
	private interface Expr {
		public String call(String expr) throws SyntaxError;
	}
	private String unaryExpr(
		String expr, 
		Pattern p, 
		String op, 
		Expr match,
		Expr noMatch
	) throws SyntaxError {
		String out = "";
		Matcher m = p.matcher(expr);
		
		if(m.find()){
			String e1 = m.group(1);
			return "(" + op + match.call(e1) + ")";
		}
		return noMatch.call(expr);
	}
	private String getOp(String op) throws SyntaxError{
		op = op.trim();
		if(operations.get(op) != null){
			return operations.get(op);
		}
		else {
			throw new SyntaxError("Invalid operation: " + op);
		}
	}
	/* Function that applies the supplied pattern to expr and 
	* calls the corresponding expr according to the operation
	*/
	private String binaryExpr(
		String expr,
		Pattern p, 
		Expr left, 
		Expr right,
		Expr noMatch
	) throws SyntaxError {
		String out = "";
		Matcher m = p.matcher(expr);
		if(m.find()){
			String e1 = m.group(1);
			String e2 = m.group(2);
			System.out.println(m.groupCount());

			// Hack to generalize around the fact that using '|'
			// to match multiple groups ends up putting the values in weird groups
			// I think it relies on left associativity (late as I'm coding)
			if(m.groupCount() > 2){
				for(int i = 2; i < m.groupCount(); i+=2){
					if(m.group(i) != null){
						e1 = m.group(i);
						e2 = m.group(i + 1);
						System.out.println(e1);
						System.out.println(e2);
					}
				}
			}
			String op = expr.substring(e1.length(), expr.length() - e2.length()).trim();
			return "(" + left.call(e1) + " " +  getOp(op) + " " + right.call(e2) + ")";
		}
		return noMatch.call(expr);
	}
	// Calling it parseAtom since it's for parsing the most basic level (variable or literals)
	private String parseAtom(
		String atom, 
		Pattern varPattern, 
		Pattern valPattern, 
		HashSet<String> scope
	) throws SyntaxError {
		Matcher varMatcher = varPattern.matcher(atom);
		Matcher valMatcher = valPattern.matcher(atom);
		System.out.println(atom);
		if(varMatcher.find()){
			// Check if we have already declared the variable
			if(scope.contains(atom))
				return atom;
			// Maybe need a new type of error for this
			throw new SyntaxError(
				atom + " doesn't seem to be in your contacts.\n"
				+ "You currently have the following contacts: \n"
				+ scope.toString()
			);
		} 
		else if(valMatcher.find()){
			return atom;
		}
		else {
			throw new SyntaxError(
				"Hey, you may have sent this to the wrong person.\n"
				+ atom + " usually isn't responsible for this work."
			);
		}
	}
	
	private String parseComp(String comp) throws SyntaxError{
		Matcher m = comp_expr.matcher(comp);
		if(m.find()){
			try {
				return binaryExpr(
					comp,
					comp_expr,
					x->parseBoolExpr1(x),
					x->parseBoolExpr1(x),
					x->parseBoolExpr1(x)
				);
			}
			catch(SyntaxError e){}
			try {
				return binaryExpr(
					comp,
					comp_expr,
					x->parseIntExpr(x),
					x->parseIntExpr(x),
					x->parseIntExpr(x)
				);
			}
			catch (SyntaxError e) {
				throw new SyntaxError(
					"Hey, I wanted to sync up about your comparisons.\n"
					+ "You said \"" + comp + "\"\n"
					+ "But, I wasn't totally sure what you meant."
				);
			}
		}
		throw new SyntaxError("Encountered the following invalid comparison:" + comp);
	}

	private String parseStringExpr(String expr) throws SyntaxError {
		try {
			return parseAtom(expr, stringVar, stringVal, strings);
		}
		catch(SyntaxError e)
		{
			throw new SyntaxError(e.getMessage());
		}
	}

	private String parseBoolExpr(String expr) throws SyntaxError {
		//<bool_expr> ::= <comp> | <bool_expr1>
		// unaryExpr probably should be renamed 
		// but basically just to call parseCompExpr if comp is a match
		return unaryExpr(
			expr, 
			comp_expr,
			"",
			x->parseComp(x),
			x->parseBoolExpr1(x)
		);
	}
	
	private String parseBoolExpr1(String expr) throws SyntaxError {
		// <bool_expr1> ::= <bool_expr1> or <bool_expr2> | <bool_expr2>

		return binaryExpr(
			expr, 
			bool_expr1, 
			x->parseBoolExpr1(x), 
			x->parseBoolExpr2(x),
			x->parseBoolExpr2(x)
		);
	}
	private String parseBoolExpr2(String expr) throws SyntaxError {
		//<bool_expr2> ::= <bool_expr2> and <bool_expr3> | <bool_expr3>
		System.out.println(expr);
		return binaryExpr(
			expr, 
			bool_expr2, 
			x->parseBoolExpr2(x), 
			x->parseBoolExpr3(x),
			x->parseBoolExpr3(x)
		);
	}
	
	private String parseBoolExpr3(String expr) throws SyntaxError {
		//<bool_expr3> ::= not <bool_expr4> | <bool_expr4>
		return unaryExpr(
			expr, 
			bool_expr3, 
			"!", 
			x->parseBool(x), 
			x->parseBool(x) 
		);
	}
	
	private String parseBool(String bool) throws SyntaxError{
		return toBool(parseAtom(
			bool,
			boolVar,
			boolVal,
			bools
		));
	}

	private String parseIntExpr(String expr) throws SyntaxError {
		System.out.println(expr);
		return binaryExpr(
			expr,
			int_expr, 
			x->parseIntExpr(x), 
			x->parseIntExpr1(x), 
			x->parseIntExpr1(x)
		);
	}
	private String parseIntExpr1(String expr) throws SyntaxError {
		System.out.println(expr);
		return binaryExpr(
			expr,
			int_expr1, 
			x->parseIntExpr1(x), 
			x->parseInt(x), 
			x->parseInt(x)
		);
	}
	
	private String parseInt(String _int) throws SyntaxError {
		System.out.println(_int);
		return parseAtom(
			_int,
			intVar,
			intVal,
			ints
		);
	}

	private String parseCharExpr(String expr) {
		// TODO
		return "";
	}

	private String string_expr(String expr) {
		//TODO
		return "";
	}
	
	/*
		Given the left (var) and right (val) sides of an assignment statement,
		determines the type of assignment performed.
	
		Return Values: TYPE of var (INT, BOOL, CHAR, STRING, or WRONG)
	*/
	private Type findVarType(String var) {
		Matcher[] m = {
			boolVar.matcher(var), 
			intVar.matcher(var), 
			charVar.matcher(var),
			stringVar.matcher(var)
		};
		for(int i = 0; i < m.length; i++)
			if(m[i].find()) return Type.values()[i];
		return Type.WRONG;
	}
	
	private Type findValType(String val) {
		Matcher[] m = {
			boolVal.matcher(val),
			intVal.matcher(val), 
			charVal.matcher(val),
			stringVal.matcher(val)
		};
		for(int i = 0; i < m.length; i++)
			if(m[i].find()) return Type.values()[i];
		return Type.WRONG;
	}
	
	private Type findAssignmentType(String var, String val) {
		Type varType = findVarType(var);
		Type valType = findValType(val);
		
		if(varType == valType) return varType;
		
		return Type.WRONG;
	}
	
	// private  boolean parseConditional(String expression) {

	// }

}

class SyntaxError extends Exception {
	public SyntaxError(String message) {
		super(message);
	}
}