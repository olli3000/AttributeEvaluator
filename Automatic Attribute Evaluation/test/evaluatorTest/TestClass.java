package evaluatorTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import evaluator.Grammar;
import evaluator.Parser;

public class TestClass {

	String read(String path) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(path));
		String input = reader.lines().collect(Collectors.joining("\n"));
		reader.close();
		return input;
	}

	@Test
	void testCalculatorGrammarTransitiveClosure() throws IOException {
		System.setIn(new ByteArrayInputStream(read("files\\input\\calculator_grammar_letters.txt").getBytes()));

		Grammar g = new Parser().init();
		g.computeTransitiveClosure();
		System.out.println(g.printDependencies());
		assertEquals(g.printDependencies(), read("files\\output\\calculator_dependency_output.txt"));
	}
	
	@Test
	void example_3_1_5() {
	}
	
	@Test
	void example_3_2_2() {
	}
	
	@Test
	void example_3_3_2() {
	}
	
	@Test
	void example_3_3_3() {
	}
	
	@Test
	void example_3_4_2() {
	}
	@Test
	void example_3_5_1() {
	}
	
	@Test
	void example_3_6_1() {
	}
	
	@Test
	void example_3_6_2() {
	}
	
	@Test
	void example_4() {
	}
}
