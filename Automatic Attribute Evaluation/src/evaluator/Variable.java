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

import evaluator.Attribute.Type;
import evaluator.Production.AttributePrioNode;

public class Variable {

	private final char name;
	private final int index;
	private Map<String, Attribute> attributes = new HashMap<>();

	private List<List<Attribute>> executionGroups = new ArrayList<>();

	public Variable(char name, int index) {
		this.name = name;
		this.index = index;
	}

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

	public void populateQueue(Queue<AttributePrioNode> queue) {
		for (var entry : attributes.entrySet()) {
			queue.add(new AttributePrioNode(entry.getValue(), entry.getValue().getDependsOn().size()));
		}
	}

	public void createGroups() {
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

		consumeQueues(inherited, synthesized);
	}

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

	private void consumeQueues(Queue<AttributePrioNode> inherited, Queue<AttributePrioNode> synthesized) {
		Set<Attribute> visited = new HashSet<>(inherited.size() + synthesized.size());
		while (!inherited.isEmpty() || !synthesized.isEmpty()) {
			List<Attribute> inhSubset = createGroup(inherited, inherited, synthesized, visited);
			if (!inhSubset.isEmpty()) {
				executionGroups.add(inhSubset);
			}

			List<Attribute> synSubset = createGroup(synthesized, inherited, synthesized, visited);
			if (!synSubset.isEmpty()) {
				executionGroups.add(synSubset);
			}
		}
	}

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

	public Map<String, Attribute> getAttributes() {
		return attributes;
	}

	public List<List<Attribute>> getExecutionGroups() {
		return executionGroups;
	}

	@Override
	public String toString() {
		return name + "" + index + " " + attributes.values();
	}

	public String toStringPlain() {
		return name + "";
	}
}
