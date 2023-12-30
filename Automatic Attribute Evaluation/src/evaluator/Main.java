package evaluator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

public class Main {

	public static void main(String[] args) throws IOException {
		Grammar g = new Parser().init();
//		System.out.println(g);
//		System.out.println(g.printDependencies());
//		System.out.println("------------------------");
		g.computeTransitiveClosure();
		System.out.println(g.printDependencies());

//		System.out.println(g);
		g.computeAttributeGroups();
//		System.out.println(g.printAttributeGroups());

		g.determineLocalExecutionOrdersSynchronized();
		System.out.println(g.printLocalExecutionOrders());

//		g.determineLocalExecutionOrders();
//		System.out.println(g.printLocalExecutionOrders());

//		write(g);
//		String result = g.printDependencies();
//		String actual = read();
//		System.out.println(result.equals(actual));
	}

	public static void write(Grammar g) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("files\\output\\calculator_dependency_output.txt"));
		writer.write(g.printDependencies());
		writer.close();
	}

	public static String read() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader("files\\output\\calculator_dependency_output.txt"));
		String input = reader.lines().collect(Collectors.joining("\n"));
		reader.close();
		return input;
	}
}
