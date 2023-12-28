package evaluator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import evaluator.Production.AttributePrioNode;

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
		Comparator<AttributePrioNode> cmp = (t, o) -> {
			int cmpPrio = Integer.compare(t.priority(), o.priority());
			if (cmpPrio == 0) {
				int cmpType = t.attribute().getType().compareTo(o.attribute().getType());
				if (cmpType == 0) {
					return t.attribute().getName().compareTo(o.attribute().getName());
				}
				return cmpType;
			}
			return cmpPrio;
		};

		for (var entry : variableOccurences.entrySet()) {
			Queue<AttributePrioNode> queue = new PriorityQueue<>(cmp);
			Variable var = entry.getValue().get(0);
		}
	}

	@Override
	public String toString() {
		return productions.toString();
	}
}
