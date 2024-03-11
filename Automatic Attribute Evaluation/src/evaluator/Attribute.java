package evaluator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Attribute {

	public static enum Type {
		INHERITED("inh"), INIT_BY_VALUE("val"), SYNTHESIZED("syn");

		private final String str;

		private Type(String str) {
			this.str = str;
		}

		@Override
		public String toString() {
			return str;
		}
	}

	private final int index;
	private final String name;
	private Type type;
	private boolean needed;

	public final int ID;
	private static int id;

	private int countOfSameIndexDependency = 0;
	private Map<String, Attribute> dependsOn = new HashMap<>();
	private Map<String, Attribute> usedFor = new HashMap<>();

	private boolean visited;

	public Attribute(int index, String name) {
		this(index, name, index == 0 ? Type.INHERITED : Type.SYNTHESIZED, true);
	}

	public Attribute(int index, String name, Type type, boolean needed) {
		this.index = index;
		this.name = name;
		this.type = type;
		this.needed = needed;
		this.ID = id;
		id++;
	}

	/**
	 * Adds the respective other attribute for both attributes to the dependency
	 * sets such that other-->this holds as a dependence relation
	 * 
	 * @param other Other attribute of the dependence relation
	 * @return true if the dependency has not existed before
	 */
	public boolean addDependencyOn(Attribute other) {
		Attribute prev1 = dependsOn.putIfAbsent(other.name + other.index, other);
		Attribute prev2 = other.usedFor.putIfAbsent(name + index, this);
		boolean ret = prev1 == null || prev2 == null;
		if (index == other.index && ret) {
			countOfSameIndexDependency++;
		}
		return ret;
	}

	/**
	 * Removes the given attribute from the dependsOn-map. If it is located at the
	 * same variable, the number of attributes {@code this} attribute depends on is
	 * decreased.
	 * 
	 * @param other The attribute to be removed
	 */
	public void removeAttributeFromDependsOn(Attribute other) {
		dependsOn.remove(other.name + other.index, other);
		if (index == other.index) {
			countOfSameIndexDependency--;
		}
	}

	/**
	 * Searches for a new path to an attribute at the same index. After leaving the
	 * variable of this index, the first return to this index is considered and the
	 * search is stopped at this point. The parameter {@code skip} handles the case
	 * where the the search traverses attributes of the same variable first.
	 * 
	 * @param searchIndex The index of the starting attribute
	 * @param result      List of attributes to which possibly new paths are found
	 * @param skip        {@code true} if this attribute should be skipped
	 */
	public void findPath(int searchIndex, List<Attribute> result, boolean skip) {
		if (searchIndex == index && !skip) {
			result.add(this);
			return;
		}

		visited = true;
		for (var entry : usedFor.entrySet()) {
			Attribute attr = entry.getValue();
			if (!attr.visited) {
				attr.findPath(searchIndex, result, false);
			}
		}
		visited = false;
	}

	/**
	 * Builds a string representation of the outgoing dependencies of this
	 * attribute.
	 * 
	 * @return the string representation
	 */
	public String printDependencies() {
		StringBuilder sb = new StringBuilder();
		for (var entry : usedFor.entrySet()) {
			sb.append(this);
			sb.append(" -> ");
			sb.append(entry.getValue());
			sb.append("\t");
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Attribute o) {
			return index == o.index && name.equals(o.name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, index);
	}

	@Override
	public String toString() {
		return "" + name + index + " " + type;
	}

	public String prettyPrint(String varPrefix) {
		return varPrefix + "." + name;
	}

	// For debugging
//	@Override
//	public String toString() {
//		StringBuilder sb = new StringBuilder();
//		sb.append(name);
//		sb.append(index);
//		sb.append(" ID");
//		sb.append(ID);
//		sb.append(" ");
//		sb.append(type);
//		sb.append(" needed: ");
//		sb.append(needed);
//		sb.append(" usedFor: [");
//		for (var a : usedFor.entrySet()) {
//			sb.append(a.getValue().name + a.getValue().index + " ");
//		}
//		sb.append("]");
//		return sb.toString();
//	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public boolean isNeeded() {
		return needed;
	}

	public int getIndex() {
		return index;
	}

	public String getName() {
		return name;
	}

	public int getCountOfSameIndexDependency() {
		return countOfSameIndexDependency;
	}

	public Map<String, Attribute> getDependsOn() {
		return dependsOn;
	}

	public Map<String, Attribute> getUsedFor() {
		return usedFor;
	}

	public boolean isRoot() {
		return index == 0;
	}

	private String laTexIdent;

	public String getLaTexIdent() {
		return laTexIdent;
	}

	public void setLaTexIdent(String laTexIdent) {
		this.laTexIdent = laTexIdent;
	}
}
