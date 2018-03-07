import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class FolResolver {
	private static final String INPUTFILENAME = "input.txt";
	private static final String OUTPUTFILENAME = "output.txt";
	static Map<String, predicateDictionary> KB = new HashMap<String, predicateDictionary>();
	private static int nfCount;
	private static int maxCount;

	public static void main(String[] args) {
		try (BufferedReader br = new BufferedReader(new FileReader(INPUTFILENAME))) {
			int nq = Integer.parseInt(br.readLine());
			List<String> queries = new ArrayList<>();
			for (int i = 0; i < nq; i++) {
				queries.add(br.readLine());
			}
			int ns = Integer.parseInt(br.readLine());
			List<String> sentences = new ArrayList<>();
			for (int i = 0; i < ns; i++) {
				sentences.add(br.readLine());
			}
			populateKb(sentences);
			// printKB();
			Boolean res[] = new Boolean[nq];
			int j = 0;
			maxCount = ns <= 20 ? ns * 50 : ns * 10;
			for (String query : queries) {
				String negQ = generateNegQuery(query);
				Map<String, predicateDictionary> newKB = new HashMap<String, predicateDictionary>();
				for (Entry<String, predicateDictionary> entry : KB.entrySet()) {
					newKB.put(entry.getKey(), new predicateDictionary(entry.getValue()));
				}
				nfCount = 0;
				res[j] = solve(negQ, newKB);
				System.out.println(res[j]);
				j++;
			}
			writeOutput(res);
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static Boolean solve(String query, Map<String, predicateDictionary> newKB) {
		if (nfCount >= maxCount)
			return false;
		String[] clauses = query.split(" \\| ");
		for (String clause : clauses) {
			predicate p = findPredicate(clause);
			String[] queryArgs = findArguments(clause);
			String queryString = p.name + "(" + String.join(",", queryArgs) + ")";
			resolvedClause queryClause = p.isPositive ? resolveClause(query, queryString)
					: resolveClause(query, "~" + queryString);

			if (newKB.containsKey(p.name)) {
				List<String> sentences = new ArrayList<>();
				if (p.isPositive) {
					sentences = newKB.get(p.name).negativeSentences;
				} else {
					sentences = newKB.get(p.name).positiveSentences;
				}
				for (int i =0; i<sentences.size();i++) {
					String sentence = sentences.get(i);
					resolvedClause t = p.isPositive ? resolveClause(sentence, "~" + p.name + "(")
							: resolveClause(sentence, p.name + "(");
					String sentArgs[] = findArguments(t.reqd);
					Map<String, String> substitution = new HashMap<String, String>();
					Boolean isUnifiable = unify(queryArgs, sentArgs, substitution);
					System.out.print(query);
					System.out.print("   " + sentence);
					System.out.println("\n" + substitution);
					if (!isUnifiable) {
						System.out.println("\nEND - Not Unifiable");
						continue;
					}
					String newFact = folResolve(queryClause.rest, t.rest, substitution);
					System.out.println("\nnewFact= " + newFact + "\n");
					if (newFact == "") {
						return true;
					} else {
						Boolean existsInNewKb = checkKb(newFact, newKB);
						if (existsInNewKb) {
							System.out.println("\nEND - Sentence exists in KB");
							continue;
						} else {
							addFact(newFact, newKB);
							nfCount++;
							if (nfCount >= maxCount)
								return false;
							System.out.println(nfCount);
							Boolean res = solve(newFact, newKB);
							if (res) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	private static String folResolve(String sent1, String sent2, Map<String, String> substitution) {
		if (sent1 == "" && sent2 == "")
			return "";
		String[] s1Clauses = sent1 == "" ? null : sent1.split(" \\| ");
		String[] s2Clauses = sent2 == "" ? null : sent2.split(" \\| ");
		String[] clauses;
		if (s1Clauses == null) {
			clauses = s2Clauses;
		} else if (s2Clauses == null) {
			clauses = s1Clauses;
		} else {
			clauses = new String[s1Clauses.length + s2Clauses.length];
			System.arraycopy(s1Clauses, 0, clauses, 0, s1Clauses.length);
			System.arraycopy(s2Clauses, 0, clauses, s1Clauses.length, s2Clauses.length);
			clauses = new HashSet<String>(Arrays.asList(clauses)).toArray(new String[0]);
		}
		String fact = "";
		Boolean changed = false;
		for (int j = 0; j < clauses.length; j++) {
			String args[] = findArguments(clauses[j]);
			Boolean varsChanged = false;
			for (int i = 0; i < args.length; i++) {
				if (substitution.containsKey(args[i])) {
					changed = true;
					varsChanged = true;
					args[i] = substitution.get(args[i]);
				}
			}
			if (varsChanged) {
				String vars = String.join(",", args);
				predicate p = findPredicate(clauses[j]);
				String temp = "";
				if (!p.isPositive)
					temp = "~";
				temp = temp + p.name + "(" + vars + ")";
				clauses[j] = temp;
			}
		}
		if (changed) {
			fact = String.join(" | ", clauses);
		} else {
			if (sent1 == "") {
				fact = sent2;
			} else if (sent2 == "") {
				fact = sent1;
			} else {
				fact = sent1 + " | " + sent2;
			}
		}
		return fact;
	}

	private static Boolean checkKb(String fact, Map<String, predicateDictionary> kb) {
		String newFactClauses[] = fact.split(" \\| ");
		predicate p = findPredicate(newFactClauses[0]);
		List<String> sentences = new ArrayList<String>();
		if (kb.containsKey(p.name)) {
			if (p.isPositive) {
				sentences = kb.get(p.name).positiveSentences;
			} else {
				sentences = kb.get(p.name).negativeSentences;
			}
			if (sentences.contains(fact))
				return true;
			else
				return false;
		} else {
			return false;
		}
	}

	private static void addFact(String fact, Map<String, predicateDictionary> kb) {
		String[] literals = fact.split(" \\| ");
		for (String literal : literals) {
			predicate p = findPredicate(literal);
			if (kb.containsKey(p.name)) {
				if (p.isPositive) {
					kb.get(p.name).positiveSentences.add(fact);
				} else {
					kb.get(p.name).negativeSentences.add(fact);
				}
			} else {
				predicateDictionary dict = new predicateDictionary();
				if (p.isPositive) {
					dict.positiveSentences.add(fact);
				} else {
					dict.negativeSentences.add(fact);
				}
				kb.put(p.name, dict);
			}
		}
	}

	private static Boolean unify(String[] queryArgs, String[] sentArgs, Map<String, String> substitution) {
		int equalConstants = 0;
		for (int i = 0; i < queryArgs.length; i++) {
			Boolean isConstantQuery = isConstant(queryArgs[i]);
			Boolean isConstantStmt = isConstant(sentArgs[i]);
			if (isConstantQuery && isConstantStmt && queryArgs[i].equals(sentArgs[i])) {
				equalConstants++;
				continue;
			} else if (isConstantQuery && isConstantStmt && !queryArgs[i].equals(sentArgs[i])) {
				return false;
			} else if (isConstantQuery && !isConstantStmt) {
				substitution.put(sentArgs[i], queryArgs[i]);
			} else if (isConstantStmt && !isConstantQuery) {
				substitution.put(queryArgs[i], sentArgs[i]);
			} else {
				substitution.put(sentArgs[i], queryArgs[i]);
			}
		}
		if (substitution.isEmpty() && equalConstants != queryArgs.length) {
			return false;
		}
		return true;
	}

	private static Boolean isConstant(String stmt) {
		if (Character.isUpperCase(stmt.charAt(0)))
			return true;
		else
			return false;
	}

	private static resolvedClause resolveClause(String sentence, String reqdPredicate) {
		resolvedClause t = new resolvedClause();
		String[] clauses = sentence.split(" \\| ");
		for (int i = 0; i < clauses.length; i++) {
			if (clauses[i].length() >= reqdPredicate.length()
					&& clauses[i].substring(0, reqdPredicate.length()).equals(reqdPredicate)) {
				t.reqd = clauses[i];
			} else if (t.rest.isEmpty()) {
				t.rest = clauses[i];
			} else {
				t.rest = t.rest + " | " + clauses[i];
			}
		}
		return t;
	}

	private static String[] findArguments(String clause) {
		int begin = clause.indexOf('(') + 1;
		int end = clause.indexOf(')');
		String args = clause.substring(begin, end);
		String[] argsList = args.split(",");
		return argsList;
	}

	private static String generateNegQuery(String query) {
		if (query.charAt(0) == '~') {
			return query.substring(1);
		} else {
			return ("~" + query);
		}
	}

	private static void printKB() {
		Iterator itr = KB.keySet().iterator();
		int num = 0;
		while (itr.hasNext()) {
			String key = itr.next().toString();
			System.out.println(key);
			System.out.println("Positive Sentences");
			predicateDictionary p = KB.get(key);
			for (String pos : p.positiveSentences) {
				System.out.print(pos + ",");
				num++;
			}
			System.out.println("\nNegative Sentences");
			for (String neg : p.negativeSentences) {
				System.out.print(neg + ",");
				num++;
			}
			System.out.println();
		}
		System.out.println(num);
	}

	private static void populateKb(List<String> sentences) {
		for (int i = 0; i < sentences.size(); i++) {
			String temp = standardize(sentences.get(i), i);
			addFact(temp, KB);
		}
	}

	private static String standardize(String sent, int n) {
		String[] clauses = sent.split(" \\| ");
		String fact = "";
		Boolean changed = false;
		for (int j = 0; j < clauses.length; j++) {
			String args[] = findArguments(clauses[j]);
			Boolean varsChanged = false;
			for (int i = 0; i < args.length; i++) {
				if (Character.isLowerCase(args[i].charAt(0))) {
					changed = true;
					varsChanged = true;
					args[i] = args[i] + n;
				}
			}
			if (varsChanged) {
				String vars = String.join(",", args);
				predicate p = findPredicate(clauses[j]);
				String temp = "";
				if (!p.isPositive)
					temp = "~";
				temp = temp + p.name + "(" + vars + ")";
				clauses[j] = temp;
			}
		}
		if (changed) {
			fact = String.join(" | ", clauses);
		} else {
			fact = sent;
		}
		return fact;
	}

	private static predicate findPredicate(String literal) {
		predicate p = new predicate(null, "");
		int i = literal.indexOf('(');
		if (literal.charAt(0) == '~') {
			p.name = literal.substring(1, i);
			p.isPositive = false;
		} else {
			p.name = literal.substring(0, i);
			p.isPositive = true;
		}
		return p;
	}

	public static void writeOutput(Boolean[] res) {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(OUTPUTFILENAME))) {
			for (int i = 0; i < res.length; i++) {
				if (res[i])
					bw.write("TRUE");
				else
					bw.write("FALSE");
				if (i != res.length - 1)
					bw.write("\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

class resolvedClause {
	public String reqd;
	public String rest;

	public resolvedClause() {
		reqd = "";
		rest = "";
	}
}

class predicate {
	public Boolean isPositive;
	public String name;

	public predicate(Boolean pos, String name) {
		this.isPositive = pos;
		this.name = name;
	}
}

class predicateDictionary {
	public List<String> positiveSentences;
	public List<String> negativeSentences;

	public predicateDictionary() {
		positiveSentences = new ArrayList<>();
		negativeSentences = new ArrayList<>();
	}

	public predicateDictionary(predicateDictionary p) {
		positiveSentences = new ArrayList<>();
		positiveSentences.addAll(p.positiveSentences);
		negativeSentences = new ArrayList<>();
		negativeSentences.addAll(p.negativeSentences);
	}

}
