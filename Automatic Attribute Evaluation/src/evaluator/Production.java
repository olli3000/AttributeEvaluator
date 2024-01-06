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

import evaluator.Variable.Group;

public class Production {

	private Variable[] nodes;
	private int index;

	private List<Group> localExecutionOrder = new ArrayList<>();
	private boolean acyclic = true;

//	public Production(Variable... nodes) {
//		this.nodes = nodes;
//	}

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
				acyclic = false;
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
					localExecutionOrder.add(new Group(nodes[current.getIndex()], current.getIndex(), List.of(current)));
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
		return toStringPlain() + "\t\t" + localExecutionOrder.toString() + " cycle-free: " + acyclic;
	}

	/**
	 * Merges sequences of attribute groups of all variables into one linear
	 * execution order if possible. A group is added to the final order iff all
	 * predecessors are already considered in the order.
	 * 
	 * @param variableOccurrences A map with all variables in this grammar
	 * @return {@code true} if this production is cycle-free
	 */
	public boolean determineLocalExecutionOrderSynchronized(Map<Character, List<Variable>> variableOccurences) {
		int lastIndex = 0;
		int remainingGroups = numOfExecutionGroups();

		execution: while (remainingGroups > 0) {
			int retIndex = findGroupWithoutPredecessors(lastIndex);
			if (retIndex != -1) {
				Group currentGroup = nodes[retIndex].getExecutionGroups().remove(0);
				updateDependencies(currentGroup);
				localExecutionOrder.add(currentGroup);
				remainingGroups--;
				lastIndex = retIndex;
			} else {
				if (breakCycleBetweenGroups(variableOccurences)) {
					remainingGroups++;
					continue execution;
				}
				acyclic = false;
				return false;
			}
		}
		return remainingGroups != -1;
	}

	private int numOfExecutionGroups() {
		int remainingGroups = 0;
		for (Variable var : nodes) {
			remainingGroups += var.getExecutionGroups().size();
			if (!var.isAcyclic()) {
				acyclic = false;
				return -1;
			}
		}
		return remainingGroups;
	}

	private int findGroupWithoutPredecessors(int lastIndex) {
		nextVar: for (int i = 0; i < nodes.length; i++) {
			int indexOffset = (i + lastIndex) % nodes.length;
			if (!nodes[indexOffset].getExecutionGroups().isEmpty()) {
				for (Attribute attribute : nodes[indexOffset].getExecutionGroups().get(0).group()) {
					if (attribute.getDependsOn().size() > 0) {
						continue nextVar;
					}
				}
				return indexOffset;
			}
		}
		return -1;
	}

	private void updateDependencies(Group group) {
		for (Attribute attribute : group.group()) {
			for (var entry : attribute.getUsedFor().entrySet()) {
				if (attribute.getIndex() != entry.getValue().getIndex()) {
					entry.getValue().removeAttributeFromDependsOn(attribute);
				}
			}
		}
	}

	private boolean breakCycleBetweenGroups(Map<Character, List<Variable>> variableOccurences) {
		for (int varIndex = 0; varIndex < nodes.length; varIndex++) {
			if (!nodes[varIndex].getExecutionGroups().isEmpty()) {

				Group toSplit = nodes[varIndex].getExecutionGroups().get(0);
				List<Attribute> newSplit = splitGroup(toSplit);

				if (!newSplit.isEmpty()) {
					splitOtherVariableOccurrences(variableOccurences, varIndex, toSplit, newSplit);
					return true;
				}
			}
		}
		return false;
	}

	private List<Attribute> splitGroup(Group toSplit) {
		List<Attribute> newSplit = new ArrayList<>();
		for (int i = 0; i < toSplit.group().size(); i++) {
			if (toSplit.group().get(i).getDependsOn().isEmpty()) {
				newSplit.add(toSplit.group().remove(i--));
			}
		}
		return newSplit;
	}

	private void splitOtherVariableOccurrences(Map<Character, List<Variable>> variableOccurences, int varIndex,
			Group toSplit, List<Attribute> newSplit) {
		for (Variable var : variableOccurences.get(nodes[varIndex].getName())) {
			if (var != nodes[varIndex]) {
				if (var.getExecutionGroups().isEmpty()) {
					splitGroupInVariableOrProduction(var, var.getProduction().localExecutionOrder, toSplit, newSplit);
				} else {
					splitGroupInVariableOrProduction(var, var.getExecutionGroups(), toSplit, newSplit);
				}
			}
		}
		nodes[varIndex].getExecutionGroups().add(0,
				new Group(toSplit.var(), toSplit.groupIndex() - newSplit.size(), newSplit));
	}

	private void splitGroupInVariableOrProduction(Variable var, List<Group> executionGroups, Group toSplit,
			List<Attribute> newSplit) {
		for (int i = 0; i < executionGroups.size(); i++) {
			Group other = executionGroups.get(i);
			if (other.var().getName() == var.getName() && other.var() != toSplit.var()
					&& other.groupIndex() == toSplit.groupIndex()
					&& other.group().size() == toSplit.group().size() + newSplit.size()) {

				List<Attribute> otherSplit = cloneSplit(other, newSplit);
				executionGroups.add(i++, new Group(other.var(), other.groupIndex() - otherSplit.size(), otherSplit));
			}
		}
	}

	private List<Attribute> cloneSplit(Group other, List<Attribute> newSplit) {
		List<Attribute> otherSplit = new ArrayList<>();
		for (Attribute attribute : newSplit) {
			for (int j = 0; j < other.group().size(); j++) {
				if (attribute.getName().equals(other.group().get(j).getName())) {
					otherSplit.add(other.group().remove(j--));
					break;
				}
			}
		}
		return otherSplit;
	}

	/**
	 * Removes all not needed attributes to prevent execution if possible.
	 */
	public void removeNotNeededAttributes() {
		for (int i = 0; i < localExecutionOrder.size(); i++) {
			for (int j = 0; j < localExecutionOrder.get(i).group().size(); j++) {
				if (!localExecutionOrder.get(i).group().get(j).isNeeded()) {
					localExecutionOrder.get(i).group().remove(j--);
				}
			}
			if (localExecutionOrder.get(i).group().isEmpty()) {
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

	public void setNodes(Variable[] nodes) {
		this.nodes = nodes;
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
