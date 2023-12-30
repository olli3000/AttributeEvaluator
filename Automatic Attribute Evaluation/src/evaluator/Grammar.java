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
			removeNotNeededAttributes(varOcc.get(0));
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
					Attribute copy = current.getAttributes().get(a.getName() + current.getIndex());
					for (var entry : copy.getUsedFor().entrySet()) {
						Attribute other = entry.getValue();
						if (copy.getIndex() == other.getIndex()) {
							other.removeAttributeFromDependsOn(copy);
						}
					}
					if (copy.isNeeded()) {
						subset.add(copy);
					}
				}
				current.getExecutionGroups().add(subset);
			}
		}
	}

	private void removeNotNeededAttributes(Variable var) {
		for (int i = 0; i < var.getExecutionGroups().size(); i++) {
			for (int j = 0; j < var.getExecutionGroups().get(i).size(); j++) {
				if (!var.getExecutionGroups().get(i).get(j).isNeeded()) {
					var.getExecutionGroups().get(i).remove(j);
					j--;
				}
			}
			if (var.getExecutionGroups().get(i).isEmpty()) {
				var.getExecutionGroups().remove(i);
				i--;
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

	public void determineLocalExecutionOrdersSynchronized() {
		for (var prodList : productions.entrySet()) {
			for (Production prod : prodList.getValue()) {
				prod.determineLocalExecutionOrderSynchronized();
			}
		}
	}

	@Override
	public String toString() {
		return productions.toString();
	}
}
