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

	/**
	 * Computes the transitive closure until each production is stable, meaning that
	 * there are no new necessary transitive relations found.
	 */
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

	/**
	 * Builds a string representation of the entire dependence relation.
	 * 
	 * @return the string representation
	 */
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

	/**
	 * Determines the local execution order for each production by trying to put
	 * attributes at root level, meaning index 0, as early as possible.
	 */
	public void determineLocalExecutionOrders() {
		for (var prodList : productions.entrySet()) {
			for (Production prod : prodList.getValue()) {
				prod.determineLocalExecutionOrder();
			}
		}
	}

	/**
	 * Builds a string representation of all local execution orders.
	 * 
	 * @return the string representation
	 */
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

	/**
	 * Computes for the first occurrence of each variable attribute execution
	 * groups. A group is formed if the attributes are of the same type and only
	 * depend on attributes of the same index of previously computed groups or non
	 * at all. Dependencies to attributes of other variables of the same production
	 * are ignored.
	 */
	public void computeAttributeGroups() {
		for (var varEntry : variableOccurences.entrySet()) {
			List<Variable> varOcc = varEntry.getValue();
			varOcc.get(0).createGroups();
			cloneExecutionGroups(varOcc);
		}
	}

	/**
	 * Copies the execution group of the first variable occurrence to all other
	 * variable occurrences.
	 * 
	 * @param varOcc The list of variable occurrences of the same identifier
	 */
	private void cloneExecutionGroups(List<Variable> varOcc) {
		Variable var = varOcc.get(0);
		for (int i = 1; i < varOcc.size(); i++) {
			Variable current = varOcc.get(i);
			for (var group : var.getExecutionGroups()) {
				List<Attribute> subset = new ArrayList<>();
				for (Attribute a : group) {
					// TODO remove attributes of same index from dependsOn of all attributes
					subset.add(a);
				}
				current.getExecutionGroups().add(subset);
			}
		}
	}

	/**
	 * Builds a string representation of all attribute groups of all variables.
	 * 
	 * @return the string representation
	 */
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
