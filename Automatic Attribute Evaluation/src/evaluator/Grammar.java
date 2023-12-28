package evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Grammar {

	private Map<Character, List<Production>> productions;
	private Map<Character, List<Variable>> variableOccurences;

	public Grammar(Map<Character, List<Production>> productions, Map<Character, List<Variable>> variableOccurences) {
		this.productions = productions;
		this.variableOccurences = variableOccurences;
	}

	public void computeTransitiveClosure() {
		boolean stable = false;
		while (!stable) {
			stable = true;

			for (var prodList : productions.entrySet()) {
				for (Production prod : prodList.getValue()) {
					stable &= prod.findProjections(variableOccurences);
				}
			}
		}
	}

	public String printDependencies() {
		StringBuilder sb = new StringBuilder();
		for (var prodList : productions.entrySet()) {
			for (Production prod : prodList.getValue()) {
				sb.append(prod.printDependencies());
				sb.append("\n");
			}
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	public void determineLocalExecutionOrders() {
		for (var prodList : productions.entrySet()) {
			for (Production prod : prodList.getValue()) {
				prod.determineLocalExecutionOrder();
			}
		}
	}

	public String printLocalExecutionOrders() {
		StringBuilder sb = new StringBuilder();
		for (var prodList : productions.entrySet()) {
			for (Production prod : prodList.getValue()) {
				sb.append(prod.printLocalExecutionOrder());
				sb.append("\n");
			}
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	public void computeAttributeGroups() {
		for (var varEntry : variableOccurences.entrySet()) {
			List<Variable> varOcc = varEntry.getValue();
			varOcc.get(0).createGroups();
			cloneExecutionGroups(varOcc);
		}
	}

	private void cloneExecutionGroups(List<Variable> varOcc) {
		Variable var = varOcc.get(0);
		for (int i = 1; i < varOcc.size(); i++) {
			Variable current = varOcc.get(i);
			for (var group : var.getExecutionGroups()) {
				List<Attribute> subset = new ArrayList<>();
				for (Attribute a : group) {
					subset.add(a);
				}
				current.getExecutionGroups().add(subset);
			}
		}
	}

	public String printAttributeGroups() {
		StringBuilder sb = new StringBuilder();
		for (var entry : variableOccurences.entrySet()) {
			Variable var = entry.getValue().get(0);
			sb.append(var.getName());
			sb.append(": ");
			sb.append(var.getExecutionGroups());
			sb.append("\n");
		}
		return sb.toString();
	}

	@Override
	public String toString() {
		return productions.toString();
	}
}
