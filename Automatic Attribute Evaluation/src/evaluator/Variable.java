package evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
					Attribute vStart = var.getAttributes().get(start.getName() + var.getIndex()); // start.hashCode());
					Attribute vTarget = var.getAttributes().get(attr.getName() + var.getIndex()); // attr.hashCode());
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

	public char getName() {
		return name;
	}

	public int getIndex() {
		return index;
	}

	public Map<String, Attribute> getAttributes() {
		return attributes;
	}

	@Override
	public String toString() {
		return name + "" + index + " " + attributes.values();
	}

	public String toStringPlain() {
		return name + "";
	}
}
