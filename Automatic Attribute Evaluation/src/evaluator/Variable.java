package evaluator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import evaluator.Attribute.Type;
import evaluator.Production.AttributePrioNode;

public class Variable {

	private final char name;
	private final int index;
	private final Production production;
	private Map<String, Attribute> attributes = new HashMap<>();

	public static record Group(Variable var, int groupIndex, List<Attribute> group) {
		@Override
		public String toString() {
			return "{" + group.stream().map(a -> a.prettyPrint("" + var.getName() + var.getIndex()))
					.collect(Collectors.joining(", ")) + "}";
		}
	}

	private boolean acyclic = true;
	private List<Group> executionGroups = new ArrayList<>();

	public Variable(char name, int index, Production production) {
		this.name = name;
		this.index = index;
		this.production = production;
	}

	/**
	 * Tries to find new paths from attributes to attributes of this variable. This
	 * variable is stable, if there are no new paths discovered in this iteration.
	 * If a new path is found, the dependency is established and also added to the
	 * other variable occurrences of the same identifier.
	 * 
	 * @param variableOccurences The map of variable occurrences
	 * @return {@code true} if the variable is stable
	 */
	public boolean findNewPathToSelf(Map<Character, List<Variable>> variableOccurences) {
		boolean stable = true;
		for (var entry : attributes.entrySet()) {
			Attribute start = entry.getValue();
			List<Attribute> result = new ArrayList<>();
			start.findPath(index, result, true);

			for (Attribute attr : result) {
				stable &= !attr.addDependencyOn(start);

				for (Variable var : variableOccurences.get(name)) {
					Attribute vStart = var.getAttributes().get(start.getName() + var.getIndex());
					Attribute vTarget = var.getAttributes().get(attr.getName() + var.getIndex());
					stable &= !vTarget.addDependencyOn(vStart);
				}
			}
		}
		return stable;
	}

	/**
	 * Builds the string representation of the dependency relation at this variable.
	 * 
	 * @return the string representation
	 */
	public String printDependencies() {
		StringBuilder sb = new StringBuilder();
		for (var entry : attributes.entrySet()) {
			String s = entry.getValue().printDependencies();
			if (!s.isEmpty()) {
				sb.append(s);
				sb.append("\t");
			}
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	/**
	 * Adds all attributes to the given priority queue depending on their number of
	 * predecessors.
	 * 
	 * @param queue The priority queue to be filled
	 */
	public void populateQueue(Queue<AttributePrioNode> queue) {
		for (var entry : attributes.entrySet()) {
			queue.add(new AttributePrioNode(entry.getValue(), entry.getValue().getDependsOn().size()));
		}
	}

	/**
	 * Groups the attributes of this variable. A group is formed if the attributes
	 * are of the same type and only depend on attributes of this variable of
	 * previously computed groups or non at all. Dependencies to attributes of other
	 * variables of the same production are ignored.
	 * 
	 * @return {@code true} if the variable is cycle-free
	 */
	public boolean createGroups() {
		Comparator<AttributePrioNode> cmp = (t, o) -> {
			int cmpPrio = Integer.compare(t.priority(), o.priority());
			if (cmpPrio == 0) {
				return t.attribute().getName().compareTo(o.attribute().getName());
			}
			return cmpPrio;
		};

		Queue<AttributePrioNode> inherited = new PriorityQueue<>(cmp);
		Queue<AttributePrioNode> synthesized = new PriorityQueue<>(cmp);

		populateQueues(inherited, synthesized);

		return consumeQueues(inherited, synthesized);
	}

	/**
	 * Divides the attributes into queues of inherited and synthesized attributes.
	 * 
	 * @param inherited   The queue of inherited attributes
	 * @param synthesized The queue of synthesized attributes
	 */
	private void populateQueues(Queue<AttributePrioNode> inherited, Queue<AttributePrioNode> synthesized) {
		for (var attrEntry : attributes.entrySet()) {
			Attribute attr = attrEntry.getValue();
			if (attr.getType() == Type.INHERITED || attr.getType() == Type.INIT_BY_VALUE && attr.getIndex() != 0) {
				inherited.add(new AttributePrioNode(attr, attr.getCountOfSameIndexDependency()));
			} else {
				synthesized.add(new AttributePrioNode(attr, attr.getCountOfSameIndexDependency()));
			}
		}
	}

	/**
	 * Polls alternating attributes of both queues with currently 0 predecessors of
	 * the same variable until both queues are empty. If they cannot be emptied, a
	 * cycle is found.
	 * 
	 * @param inherited   The queue of inherited attributes
	 * @param synthesized The queue of synthesized attributes
	 * @return {@code true} if this variable is cycle-free
	 */
	private boolean consumeQueues(Queue<AttributePrioNode> inherited, Queue<AttributePrioNode> synthesized) {
		int groupIndex = -1;
		Set<Attribute> visited = new HashSet<>(inherited.size() + synthesized.size());
		while (!inherited.isEmpty() || !synthesized.isEmpty()) {
			boolean cycle = true;
			List<Attribute> inhSubset = createGroup(inherited, inherited, synthesized, visited);
			cycle &= inhSubset.isEmpty();
			if (!inhSubset.isEmpty()) {
				executionGroups.add(new Group(this, groupIndex += inhSubset.size(), inhSubset));
			}

			List<Attribute> synSubset = createGroup(synthesized, inherited, synthesized, visited);
			cycle &= synSubset.isEmpty();
			if (!synSubset.isEmpty()) {
				executionGroups.add(new Group(this, groupIndex += synSubset.size(), synSubset));
			}

			if (cycle) {
				acyclic = false;
				return false;
			}
		}
		return true;
	}

	/**
	 * Retrieves all attributes with currently 0 predecessors of the same variable,
	 * which have not been visited yet. Afterwards it removes those attributes from
	 * all other attributes, that are still in the queue.
	 * 
	 * @param queue       The queue to be emptied. Is the same as either the
	 *                    inherited or the synthesized queue
	 * @param inherited   The queue of remaining inherited attributes
	 * @param synthesized The queue of remaining synthesized attributes
	 * @param visited     The set of already visited attributes
	 * @return the group of attributes which can be executed in no specific order
	 */
	private List<Attribute> createGroup(Queue<AttributePrioNode> queue, Queue<AttributePrioNode> inherited,
			Queue<AttributePrioNode> synthesized, Set<Attribute> visited) {
		List<Attribute> subset = new ArrayList<>();
		while (!queue.isEmpty() && queue.peek().attribute().getCountOfSameIndexDependency() == 0) {
			Attribute current = queue.poll().attribute();
			if (!visited.contains(current)) {
				visited.add(current);
				subset.add(current);
			}
		}
		for (Attribute a : subset) {
			for (var entry : a.getUsedFor().entrySet()) {
				Attribute other = entry.getValue();
				if (a.getIndex() == other.getIndex()) {
					other.removeAttributeFromDependsOn(a);
					if (other.getType() == Type.INHERITED
							|| other.getType() == Type.INIT_BY_VALUE && other.getIndex() != 0) {
						inherited.add(new AttributePrioNode(other, other.getCountOfSameIndexDependency()));
					} else {
						synthesized.add(new AttributePrioNode(other, other.getCountOfSameIndexDependency()));
					}
				}
			}
		}
		return subset;
	}

	public char getName() {
		return name;
	}

	public int getIndex() {
		return index;
	}

	public Production getProduction() {
		return production;
	}

	public Map<String, Attribute> getAttributes() {
		return attributes;
	}

	public boolean isAcyclic() {
		return acyclic;
	}

	public void markCyclic() {
		this.acyclic = false;
	}

	public List<Group> getExecutionGroups() {
		return executionGroups;
	}

	@Override
	public String toString() {
		return name + "" + index + " " + attributes.values();
	}

	public String toStringPlain() {
		return name + "";
	}

	private String leftMost = "";

	public String getLeftMost() {
		return leftMost;
	}

	private String laTexIdent;

	public String getLaTexIdent() {
		return laTexIdent;
	}

	public String getLaTex(StringBuilder sb, String[] posHelper, Map<Character, List<Variable>> variableOccurences) {

		List<Attribute> synthesized = new ArrayList<>();
		String ident = "";
		for (var aEntry : attributes.entrySet()) {
			Attribute a = aEntry.getValue();
			if (a.getType() == Type.SYNTHESIZED || index == 0 && a.getType() == Type.INIT_BY_VALUE) {
				synthesized.add(a);
			} else {
				ident = name + "" + variableOccurences.get(name).indexOf(this) + a.getName();
				a.setLaTexIdent(ident);
				laTexPositionHelper("inherited", ident, a.getName(), sb, posHelper, variableOccurences);
			}
		}

		ident = name + "" + variableOccurences.get(name).indexOf(this);
		laTexIdent = ident;
		laTexPositionHelper("terminal", ident, name + "", sb, posHelper, variableOccurences);

		for (Attribute a : synthesized) {
			ident = name + "" + variableOccurences.get(name).indexOf(this) + a.getName();
			a.setLaTexIdent(ident);
			laTexPositionHelper("synthesized", ident, a.getName(), sb, posHelper, variableOccurences);
		}
		return ident;
	}

	private void laTexPositionHelper(String type, String ident, String name_, StringBuilder sb, String[] posHelper,
			Map<Character, List<Variable>> variableOccurences) {
		sb.append("    \\node[").append(type).append("] (").append(ident).append(") ");
		if (posHelper[0].isEmpty() && posHelper[1].isEmpty()) {
			posHelper[1] = ident;
		} else {
			if (!posHelper[0].isEmpty()) {
				sb.append("[below = 2cm of ").append(posHelper[0]).append("] ");
				posHelper[0] = "";
				posHelper[1] = ident;
			} else {
				sb.append("[right = 2mm of ").append(posHelper[1]).append("] ");
				posHelper[1] = ident;
			}
		}
		sb.append("{").append(name_).append("};\n");

		if (leftMost.isEmpty()) {
			leftMost = ident;
		}
	}
}
