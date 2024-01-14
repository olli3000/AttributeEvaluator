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

	/**
	 * Sums up the number of execution groups of all variables in this production.
	 * 
	 * @return the number of execution groups or {@code -1} if at least one variable
	 *         is cyclic
	 */
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

	/**
	 * Checks all nodes beginning at {@code lastIndex} for a group where all
	 * predecessors are known at this point.
	 * 
	 * @param lastIndex The index of the first node to check
	 * @return the index of the next node or {@code -1} iff no group can be executed
	 */
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

	/**
	 * Removes all attributes of the given group from the predecessor set of all
	 * depending attributes.
	 * 
	 * @param group the group which will be executed
	 */
	private void updateDependencies(Group group) {
		for (Attribute attribute : group.group()) {
			for (var entry : attribute.getUsedFor().entrySet()) {
				if (attribute.getIndex() != entry.getValue().getIndex()) {
					entry.getValue().removeAttributeFromDependsOn(attribute);
				}
			}
		}
	}

	/**
	 * If no group can be executed, there are cyclic dependencies between these
	 * remaining groups. It still might be possible to execute all groups, if one of
	 * them contains attributes with 0 predecessors. This group is then split into a
	 * group of attributes without predecessors and a group with the remaining
	 * attributes. Afterwards the algorithm can continue execution as if normal. The
	 * split of a group has to be also applied to the other variable occurrences of
	 * the same identifier.
	 * 
	 * @param variableOccurences A map with all variables in this grammar
	 * @return {@code true} iff there is a group which can be split
	 */
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

	/**
	 * Splits the given group by removing all attributes without predecessors and
	 * collecting them in a new list.
	 * 
	 * @param toSplit the group which needs to be split
	 * @return a new list of attributes without predecessors
	 */
	private List<Attribute> splitGroup(Group toSplit) {
		List<Attribute> newSplit = new ArrayList<>();
		for (int i = 0; i < toSplit.group().size(); i++) {
			if (toSplit.group().get(i).getDependsOn().isEmpty()) {
				newSplit.add(toSplit.group().remove(i--));
			}
		}
		return newSplit;
	}

	/**
	 * Searches for the given group {@code toSplit} in other variable occurrences of
	 * the same identifier and splits their respective groups. If a variable has no
	 * groups left, then for this variable the execution order of its production has
	 * already been determined and the corresponding group can be found and split
	 * there. Otherwise the group has not been consumed yet and it is found at the
	 * variable itself.
	 * 
	 * @param variableOccurences A map with all variables in this grammar
	 * @param varIndex           The index of the original variable where the cycle
	 *                           occurred
	 * @param toSplit            The group which has to be split
	 * @param newSplit           The list of attributes without predecessors
	 */
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

	/**
	 * Splits the given group in the list of execution groups of this variable by
	 * cloning the split.
	 * 
	 * @param var             the variable where the split occurs
	 * @param executionGroups the list where the group can be found
	 * @param toSplit         the original group which has to be split
	 * @param newSplit        the list of attributes that form a new group
	 */
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

	/**
	 * Splits the given group by removing attributes found in {@code newSplit} and
	 * puts them into a new list.
	 * 
	 * @param other    the group which will be split
	 * @param newSplit the list of attributes that have no predecessors
	 * @return the cloned list of attributes of {@code newSplit}
	 */
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

	public Variable[] getNodes() {
		return nodes;
	}

	public void setIndex(int index) {
		this.index = index;
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

	public String getLaTex(StringBuilder sb, String top, Map<Character, List<Variable>> variableOccurences) {
		String left = "";
		for (Variable v : nodes) {
			left = v.getLaTex(sb, new String[] { top, left }, variableOccurences);
			if (nodes[0] == v) {
				top = v.getLeftMost();
			} else {
				top = "";
			}
		}

		sb.append("\n");

		for (Variable v : nodes) {
			if (v.getIndex() != 0) {
				sb.append("\\draw[->,very thick] (").append(nodes[0].getLaTexIdent()).append(".south) -- (")
						.append(v.getLaTexIdent()).append(".north);\n");
			}

			for (var aEntry : v.getAttributes().entrySet()) {
				Attribute from = aEntry.getValue();
				for (var oEntry : from.getUsedFor().entrySet()) {
					Attribute to = oEntry.getValue();
					laTexDependencyHelper(from, to, sb, variableOccurences);
				}
			}
		}

		sb.append("\n");
		return nodes[1].getLeftMost();
	}

	private void laTexDependencyHelper(Attribute from, Attribute to, StringBuilder sb,
			Map<Character, List<Variable>> variableOccurences) {
		sb.append("\\draw[->,thick] (").append(from.getLaTexIdent());
		if (from.isRoot()) {
			if (to.isRoot()) {
				sb.append(".north) .. controls +(up:5mm) and +(up:5mm) .. (").append(to.getLaTexIdent())
						.append(".north");
			} else {
				sb.append(".south) -- (").append(to.getLaTexIdent()).append(".north");
			}
		} else if (to.isRoot()) {
			sb.append(".north) -- (").append(to.getLaTexIdent()).append(".south");
		} else {
			sb.append(".south) .. controls +(down:5mm) and +(down:5mm) .. (").append(to.getLaTexIdent())
					.append(".south");
		}
		sb.append(");\n");
	}
}
