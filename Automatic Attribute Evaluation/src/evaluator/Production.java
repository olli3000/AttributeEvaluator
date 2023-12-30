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

	/**
	 * Tries recursively to find new paths from a starting attribute to another
	 * attribute at the same index. If no new path is found, the production is
	 * stable. If a new path is found, the dependency between these to attributes is
	 * established and it is also added to all other variable occurrences of the
	 * same identifier.
	 * 
	 * @param variableOccurences The map of variable occurrences
	 * @return {@code true} if the production is stable
	 */
	public boolean findProjections(Map<Character, List<Variable>> variableOccurences) {
		boolean stable = true;
		for (Variable var : nodes) {
			stable &= var.findNewPathToSelf(variableOccurences);
		}
		return stable;
	}

	/**
	 * Builds a string representation of the dependence relation of this production.
	 * 
	 * @return the string representation
	 */
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

	/**
	 * Determines the local execution order of this production by favoring the
	 * attributes of index 0 if there is a tie of priority (as in two attributes
	 * have no unvisited predecessor).
	 */
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

	/**
	 * Builds a string representation of the execution order.
	 * 
	 * @return the string representation
	 */
	public String printLocalExecutionOrder() {
		return toStringPlain() + "\t\t" + localExecutionOrder.toString() + " cycle-free: " + !hasCycle;
	}

	/**
	 * Merges sequences of attribute groups of all variables into one linear
	 * execution order if possible. A group is added to the final order iff all
	 * predecessors are already considered in the order.
	 */
	public void determineLocalExecutionOrderSynchronized() {
		int[] indices = new int[nodes.length];
		int lastIndex = 0;
		int remainingGroups = 0;
		for (Variable var : nodes) {
			remainingGroups += var.getExecutionGroups().size();
		}

		while (remainingGroups > 0) {
			List<Attribute> currentGroup = null;
			nextVar: for (int i = 0; i < nodes.length; i++) {
				int indexOff = (i + lastIndex) % nodes.length;
				if (nodes[indexOff].getExecutionGroups().size() > indices[indexOff]) {
					for (Attribute a : nodes[indexOff].getExecutionGroups().get(indices[indexOff])) {
						if (a.getDependsOn().size() > 0) {
							continue nextVar;
						}
					}
					currentGroup = nodes[indexOff].getExecutionGroups().get(indices[indexOff]);
					indices[indexOff]++;
					lastIndex = indexOff;
					break;
				}
			}

			if (currentGroup == null) {
				// TODO cycle found between groups
			} else {
				for (Attribute a : currentGroup) {
					for (var entry : a.getUsedFor().entrySet()) {
						entry.getValue().removeAttributeFromDependsOn(a);
					}
					localExecutionOrder.add(a);
				}
				remainingGroups--;
			}
		}
	}

	/**
	 * Removes all not needed attributes to prevent execution if possible.
	 */
	public void removeNotNeededAttributes() {
		for (int i = 0; i < localExecutionOrder.size(); i++) {
			if (!localExecutionOrder.get(i).isNeeded()) {
				localExecutionOrder.remove(i--);
			}
		}
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
