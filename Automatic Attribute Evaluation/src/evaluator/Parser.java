package evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import evaluator.Attribute.Type;

public class Parser {

	private Map<Character, List<Variable>> variableOccurences = new HashMap<>();
	private Map<Character, Integer> productionNumbers = new HashMap<>();

	/**
	 * RegEx for pattern matching of attribute occurrences of the form n[i]
	 */
	private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\w+?\\[(\\d+?)]");

	/**
	 * Initializes a new grammar with the inputed production and semantic rules.
	 * Each input line contains exactly one production and the corresponding system
	 * of equation (optional).
	 * 
	 * @return The newly initialized Grammar
	 */
	public Grammar init() {
		List<String> lines = new ArrayList<>();
		Scanner scanner = new Scanner(System.in);
		for (String line = scanner.nextLine(); !line.isEmpty(); line = scanner.nextLine()) {
			lines.add(line);
		}
		scanner.close();
		return new Grammar(
				lines.stream().map(line -> parseLine(line))
						.collect(Collectors.groupingBy(production -> production.getVariableAt(0).getName())),
				variableOccurences);
	}

	/**
	 * Parses a given input line and creates the production. Dependence relations
	 * will be initialized and it will be decided which attribute occurrence is
	 * inherited or synthesized
	 * 
	 * @param line The line to be parsed
	 * @return The newly created production
	 */
	private Production parseLine(String line) {
		String[] rules = line.split(":", 2);

		Variable[] variables = parseProduction(rules[0]);
		Production production = new Production(variables);

		char prodRoot = production.getVariableAt(0).getName();

		if (!productionNumbers.containsKey(prodRoot)) {
			productionNumbers.put(prodRoot, 0);
		}
		production.setIndex(productionNumbers.put(prodRoot, productionNumbers.get(prodRoot) + 1));

		if (rules.length == 2) {
			Arrays.stream(rules[1].split(";")).map(eq -> eq.trim()).forEach(eq -> parseEquation(eq, production));
		}
		return production;
	}

	private int variableCount = 0;

	/**
	 * Parses the left half of the line, which is by convention the production
	 * itself
	 * 
	 * @param rule The production rule
	 * @return The array of parsed variables
	 */
	private Variable[] parseProduction(String rule) {
		variableCount = 0;
		return Arrays.stream(rule.split("->")).mapMulti((string, consumer) -> {
			string.chars().filter(varName -> varName != ' ').forEachOrdered(varName -> consumer.accept(varName));
		}).map(varName -> {
			Variable var = new Variable((char) ((Integer) varName).intValue(), variableCount++);
			if (!variableOccurences.containsKey(var.getName())) {
				variableOccurences.put(var.getName(), new ArrayList<>());
			}
			variableOccurences.get(var.getName()).add(var);
			getAttributeFromAllVariables(var);
			return var;
		}).toArray(Variable[]::new);
	}

	/**
	 * Parses the right half of the line, which is by convention the system of
	 * equations of the corresponding production rule. Adds the attribute
	 * occurrences to their respective variable and creates the dependence relations
	 * 
	 * @param equation The system of equations
	 * @param p        The corresponding production
	 */
	private void parseEquation(String equation, Production production) {
		String[] sides = equation.split("=");
		Matcher leftMatch = ATTRIBUTE_PATTERN.matcher(sides[0]);
		Matcher rightMatch = ATTRIBUTE_PATTERN.matcher(sides[1]);

		if (leftMatch.find()) {
			Attribute lAttribute = parseAttribute(leftMatch);
			Variable lVariable = production.getVariableAt(lAttribute.getIndex());

			lAttribute = putAttributeIfAbsentOrNeutral(lVariable.getAttributes(), lAttribute);

			addAttributeToAllVariables(lVariable.getName(), lAttribute.getName(), lAttribute.getType());

			if (parseRightAttributes(rightMatch, lAttribute, production) == 0) {
				lAttribute.setType(Type.INIT_BY_VALUE);
			} else {
				lAttribute.setType(lAttribute.isRoot() ? Type.SYNTHESIZED : Type.INHERITED);
			}
		}
	}

	private int parseRightAttributes(Matcher match, Attribute leftAttribute, Production production) {
		int countRightAttr = 0;
		while (match.find()) {
			countRightAttr++;
			Attribute rAttribute = parseAttribute(match);
			Variable rVariable = production.getVariableAt(rAttribute.getIndex());

			rAttribute = putAttributeIfAbsentOrNeutral(rVariable.getAttributes(), rAttribute);

			leftAttribute.addDependencyOn(rAttribute);

			addAttributeToAllVariables(rVariable.getName(), rAttribute.getName(), rAttribute.getType());
		}
		return countRightAttr;
	}

	private Attribute parseAttribute(Matcher match) {
		String attribute = match.group();
		int bracketIndex = attribute.indexOf("[");
		String name = attribute.substring(0, bracketIndex);
		int index = Integer.parseInt(attribute.substring(bracketIndex + 1, attribute.length() - 1));
		return new Attribute(index, name);
	}

	private Attribute putAttributeIfAbsentOrNeutral(Map<String, Attribute> attrSet, Attribute attribute) {
		if (attrSet.containsKey(attribute.getName() + attribute.getIndex())
				&& !attrSet.get(attribute.getName() + attribute.getIndex()).isNeeded()) {
			attrSet.put(attribute.getName() + attribute.getIndex(), attribute);
		} else {
			Attribute prev = attrSet.putIfAbsent(attribute.getName() + attribute.getIndex(), attribute);
			if (prev != null) {
				attribute = prev;
			}
		}
		return attribute;
	}

	private void addAttributeToAllVariables(char ident, String attributeName, Type type) {
		for (Variable variable : variableOccurences.get(ident)) {
			Attribute attribute = new Attribute(variable.getIndex(), attributeName, type, false);
			variable.getAttributes().putIfAbsent(attribute.getName() + attribute.getIndex(), attribute);
		}
	}

	private void getAttributeFromAllVariables(Variable variable) {
		for (var entry : variableOccurences.get(variable.getName()).get(0).getAttributes().entrySet()) {
			Attribute attribute = new Attribute(variable.getIndex(), entry.getValue().getName(),
					entry.getValue().getType(), false);
			variable.getAttributes().putIfAbsent(attribute.getName() + attribute.getIndex(), attribute);
		}
	}
}
