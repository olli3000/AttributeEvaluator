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
		int remainingGroups = 0;
		for (Variable var : nodes) {
			remainingGroups += var.getExecutionGroups().size();
			if (!var.isAcyclic()) {
				acyclic = false;
				return false;
			}
		}

		execution: while (remainingGroups > 0) {
			Group currentGroup = null;
			nextVar: for (int i = 0; i < nodes.length; i++) {
				int indexOff = (i + lastIndex) % nodes.length;
				if (!nodes[indexOff].getExecutionGroups().isEmpty()) {
					for (Attribute a : nodes[indexOff].getExecutionGroups().get(0).group()) {
						if (a.getDependsOn().size() > 0) {
							continue nextVar;
						}
					}
					currentGroup = nodes[indexOff].getExecutionGroups().remove(0);
					lastIndex = indexOff;
					break;
				}
			}

			if (currentGroup == null) {
				// find group to be split (at least 1 element without preds)
				for (int varIndex = 0; varIndex < nodes.length; varIndex++) {
					if (!nodes[varIndex].getExecutionGroups().isEmpty()) {
						Group toSplit = nodes[varIndex].getExecutionGroups().get(0);
						List<Attribute> newSplit = new ArrayList<>();
						for (int j = 0; j < toSplit.group().size(); j++) {
							if (toSplit.group().get(j).getDependsOn().isEmpty()) {
								newSplit.add(toSplit.group().remove(j--));
							}
						}

						if (!newSplit.isEmpty()) {
							// first handle all other variable occurrences
							for (Variable var : variableOccurences.get(nodes[varIndex].getName())) {
								// skip reference variable
								if (var != nodes[varIndex]) {
									if (var.getExecutionGroups().isEmpty()) {
										// find and split groups in handled variables (= productions with already
										// computed orders)
										Production p = var.getProduction();
										for (int i = 0; i < p.localExecutionOrder.size(); i++) {
											Group other = p.localExecutionOrder.get(i);
											if (other.var().getName() == var.getName() && other.var() != toSplit.var()
													&& toSplit.groupIndex() == other.groupIndex() && other.group()
															.size() == toSplit.group().size() + newSplit.size()) {
												// split other
												List<Attribute> otherSplit = new ArrayList<>();
												for (Attribute a : newSplit) {
													for (int j = 0; j < other.group().size(); j++) {
														if (a.getName().equals(other.group().get(j).getName())) {
															otherSplit.add(other.group().remove(j--));
														}
													}
												}
												p.localExecutionOrder.add(i++, new Group(other.var(),
														other.groupIndex() - otherSplit.size(), otherSplit));
												break;
											}
										}
									} else {
										// split same group at not-handled variables
										for (int i = 0; i < var.getExecutionGroups().size(); i++) {
											Group other = var.getExecutionGroups().get(i);
											if (other.groupIndex() == toSplit.groupIndex()) {
												List<Attribute> otherSplit = new ArrayList<>();
												for (Attribute a : newSplit) {
													for (int j = 0; j < other.group().size(); j++) {
														if (a.getName().equals(other.group().get(j).getName())) {
															otherSplit.add(other.group().remove(j--));
														}
													}
												}
												var.getExecutionGroups().add(i++, new Group(other.var(),
														other.groupIndex() - otherSplit.size(), otherSplit));
											}
										}
									}
								}
							}
							nodes[varIndex].getExecutionGroups().add(0,
									new Group(toSplit.var(), toSplit.groupIndex() - newSplit.size(), newSplit));
							remainingGroups++;
							continue execution;
						}
					}
				}

				acyclic = false;
				return false;
			} else

			{
				for (Attribute a : currentGroup.group()) {
					for (var entry : a.getUsedFor().entrySet()) {
						if (a.getIndex() != entry.getValue().getIndex()) {
							entry.getValue().removeAttributeFromDependsOn(a);
						}
					}
				}
				localExecutionOrder.add(currentGroup);
				remainingGroups--;
			}
		}
		return true;
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
