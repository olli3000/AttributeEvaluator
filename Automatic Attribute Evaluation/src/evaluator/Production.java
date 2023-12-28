package evaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class Production {

	private Variable[] nodes;
	private int index;

	private List<Attribute> localExecutionOrder = new ArrayList<>();
	private boolean hasCycle;

	public Production(Variable... nodes) {
		this.nodes = nodes;
	}

	public boolean findProjections(Map<Character, List<Variable>> variableOccurences) {
		boolean stable = true;
		for (Variable var : nodes) {
			stable &= var.findNewPathToSelf(variableOccurences);
		}
		return stable;
	}

	public String printDependencies() {
		StringBuilder sb = new StringBuilder();
		for (Variable var : nodes) {
			sb.append(var.getName() + ": ");
			sb.append(var.printDependencies());
			sb.append("\n");
		}
		return sb.toString();
	}

	public static record AttributePrioNode(Attribute attribute, int priority) {
	};

	public void determineLocalExecutionOrder() {
		Comparator<AttributePrioNode> cmp = (t, o) -> {
			int cmpPrio = Integer.compare(t.priority(), o.priority());
			if (cmpPrio == 0) {
				int cmpIndex = Integer.compare(t.attribute().getIndex(), o.attribute().getIndex());
				if (cmpIndex == 0) {
					return t.attribute().getName().compareTo(o.attribute().getName());
				}
				return cmpIndex;
			}
			return cmpPrio;
		};

		Queue<AttributePrioNode> prioQueue = new PriorityQueue<>(cmp);
		for (Variable v : nodes) {
			v.populateQueue(prioQueue);
		}
		Set<Attribute> visited = new HashSet<>(prioQueue.size());

		while (!prioQueue.isEmpty()) {
			Attribute current = prioQueue.poll().attribute;
			if (current.getDependsOn().size() > 0) {
				hasCycle = true;
				break;
			}
			if (!visited.contains(current)) {
				visited.add(current);

				for (var entry : current.getUsedFor().entrySet()) {
					Attribute other = entry.getValue();
					other.removeAttributeFromDependsOn(current);
					prioQueue.add(new AttributePrioNode(other, other.getDependsOn().size()));
				}

				if (current.isNeeded()) {
					localExecutionOrder.add(current);
				}
			}
		}

	}

	public String printLocalExecutionOrder() {
		return toStringPlain() + "\t\t" + localExecutionOrder.toString() + " cycle-free: " + !hasCycle;
	}

	/**
	 * Returns the (non-)terminal at the given index
	 * 
	 * @param index
	 * @return variable at the specified index
	 */
	public Variable getVariableAt(int index) {
		return nodes[index];
	}

	@Override
	public String toString() {
		return "Production: " + nodes[0] + " -> "
				+ Arrays.stream(nodes).skip(1).map(var -> var.toString()).collect(Collectors.joining(" "));
	}

	public String toStringPlain() {
		return "Production " + nodes[0].toStringPlain() + index + ": " + nodes[0].toStringPlain() + " -> "
				+ Arrays.stream(nodes).skip(1).map(var -> var.toStringPlain()).collect(Collectors.joining(" "));
	}

	public Variable[] getNodes() {
		return nodes;
	}

	public void setIndex(int index) {
		this.index = index;
	}
}
