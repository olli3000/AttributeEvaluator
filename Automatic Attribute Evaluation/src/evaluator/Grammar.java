package evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import evaluator.Variable.Group;

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
				sb.append(prod.printDependencies()).append("\n");
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
				sb.append(prod.printLocalExecutionOrder()).append("\n");
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
	 * are ignored. Group order is then cloned to all other variable occurrences.
	 */
	public void computeAttributeGroups() {
		for (var varEntry : variableOccurences.entrySet()) {
			List<Variable> varOcc = varEntry.getValue();
			if (varOcc.get(0).createGroups()) {
				cloneExecutionGroups(varOcc);
			} else {
				for (Variable var : varOcc) {
					var.markCyclic();
				}
			}
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
				for (Attribute a : group.group()) {
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
				current.getExecutionGroups().add(new Group(current, group.groupIndex(), subset));
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
			sb.append(var.getName()).append(": ").append(var.getExecutionGroups()).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Determines for each production an execution order, which keeps all pairs of
	 * attributes of a variable in the same order for all other variables of the
	 * same identifier. Finally, not needed attributes are removed.
	 */
	public void determineLocalExecutionOrdersSynchronized() {
		for (var prodList : productions.entrySet()) {
			for (Production prod : prodList.getValue()) {
				// TODO abort if cycle found?
				prod.determineLocalExecutionOrderSynchronized(variableOccurences);
				prod.removeNotNeededAttributes();
			}
		}
	}

	public String getLaTex() {
		StringBuilder sb = new StringBuilder();
		sb.append("\\begin{tikzpicture}[\n")
				.append("terminal/.style={rectangle, draw=black!100, fill=blue!30, thick, minimum size=5mm},\n")
				.append("attribute/.style={rectangle, draw=black!100, fill=lime!60, thick, rounded corners=2mm, minimum size =5mm}\n]\n\n");
		String previous = "";
		for (var pEntry : productions.entrySet()) {
			for (Production p : pEntry.getValue()) {
				previous = p.getLaTex(sb, previous, variableOccurences);
//				Variable v1 = p.getVariableAt(1);
//				previous = v1.getName() + "" + variableOccurences.get(v1.getName()).indexOf(v1);
			}
		}
		sb.append("\\end{tikzpicture}\n");
		return sb.toString();
	}

	@Override
	public String toString() {
		return productions.toString();
	}
}
