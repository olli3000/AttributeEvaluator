package evaluator;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

	public static void main(String[] args) throws IOException {
		if (args.length == 0) {
			consoleInput();
		}
		for (String s : args) {
			switch (s) {
			case "f3.1" -> figure_3_1();
			case "f3.2" -> figure_3_2();
			case "f3.3" -> figure_3_3();
			case "f3.4" -> figure_3_4();
			case "f3.5" -> figure_3_5();
			case "f3.6" -> figure_3_6();
			case "f3.7" -> figure_3_7();
			case "f3.8" -> example_3_6_2();
			case "f4.1" -> figure_4_1();
			default -> consoleInput();
			}
		}
	}

	private static void consoleInput() {
		Grammar g = new Parser().init();
		g.computeTransitiveClosure();
		g.computeAttributeGroups();
		g.determineCompatibleLocalExecutionOrders();

		StringBuilder sb = new StringBuilder();
		sb.append("\n--------------------\n\nExecuting the complete algorithm\n")
				.append("These are the production-local orders:\n").append(g.printLocalExecutionOrders()).append("\n\n")
				.append(g.getLaTex()).append("\n--------------------\n");
		System.out.println(sb);
	}

	private static void figure_3_1() {
		String[] rules = { "A->BC : y[0]=z[2]; x[1]=x[0]; x[2]=y[1]; y[2]=x[2]", //
				"B->a", //
				"B->C : y[0]=z[1]; x[1]=x[0]", //
				"C->b : z[0]=y[0]" };
		Grammar g = new Parser().init(List.of(rules));
		g.computeTransitiveClosure();

		StringBuilder sb = new StringBuilder();
		sb.append("\n--------------------\n\nFigure 3.1\n\nComputing the dependence relation for \n")
				.append(Arrays.stream(rules).collect(Collectors.joining("\n"))).append("\n\n").append(g.getLaTex())
				.append("\n--------------------\n");
		System.out.println(sb);
	}

	private static void figure_3_2() {
		String[] rules = { "B->C : x[0]=x[1]; y[0]=y[1]", //
				"A->B", //
				"B->D : x[0]=y[1]; y[0]=x[1]" };
		Grammar g = new Parser().init(List.of(rules));
		g.computeTransitiveClosure();
		g.determineSimpleLocalExecutionOrders();

		StringBuilder sb = new StringBuilder();
		sb.append("\n--------------------\n\nFigure 3.2\n\nDetermining a simple order for \n")
				.append(Arrays.stream(rules).collect(Collectors.joining("\n"))).append("\n\n")
				.append("These are the production-local orders:\n").append(g.printLocalExecutionOrders()).append("\n\n")
				.append(g.getLaTex()).append("\n--------------------\n");
		System.out.println(sb);
	}

	private static void figure_3_3() {
		String[] rules = {
				"A->BC: x[0]=s[0]+y[1]; y[0]=t[0]+y[2]; z[0]=z[1]+x[2]+z[2]; r[1]=r[0]; s[1]=r[0]+r[1]; t[1]=r[1]+t[2]; z[1]=y[0]+t[1]+y[1]; r[2]=r[0]+x[1]; s[2]=s[0]; t[2]=r[2]; x[2]=t[2]" };
		Grammar g = new Parser().init(List.of(rules));
		g.computeTransitiveClosure();
		g.computeAttributeGroups();

		StringBuilder sb = new StringBuilder();
		sb.append("\n--------------------\n\nFigure 3.3\n\nGrouping attributes of \n")
				.append(Arrays.stream(rules).collect(Collectors.joining("\n"))).append("\n\n")
				.append("These are the groups:\n").append(g.printAttributeGroups()).append("\n\n").append(g.getLaTex())
				.append("\n--------------------\n");
		System.out.println(sb);
	}

	private static void figure_3_4() {
		String[] rules = { "A->B : x[0]=0; y[0]=x[0]+y[1]", //
				"C->D : x[0]=0; x[1]=x[0]; y[0]=x[0]+x[1]" };
		Grammar g = new Parser().init(List.of(rules));
		g.computeTransitiveClosure();
		g.computeAttributeGroups();

		StringBuilder sb = new StringBuilder();
		sb.append("\n--------------------\n\nFigure 3.4\n\nGrouping attributes of \n")
				.append(Arrays.stream(rules).collect(Collectors.joining("\n"))).append("\n\n")
				.append("These are the groups:\n").append(g.printAttributeGroups()).append("\n\n").append(g.getLaTex())
				.append("\n--------------------\n");
		System.out.println(sb);
	}

	private static void figure_3_5() {
		String[] rules = {
				"A->BC: x[0]=s[0]+y[1]; y[0]=t[0]+y[2]; z[0]=z[1]+x[2]+z[2]; r[1]=r[0]; s[1]=r[0]+r[1]; t[1]=r[1]+t[2]; z[1]=y[0]+t[1]+y[1]; r[2]=r[0]+x[1]; s[2]=s[0]; t[2]=r[2]; x[2]=t[2]" };
		Grammar g = new Parser().init(List.of(rules));
		g.computeTransitiveClosure();
		g.computeAttributeGroups();
		g.determineCompatibleLocalExecutionOrders();

		StringBuilder sb = new StringBuilder();
		sb.append("\n--------------------\n\nFigure 3.5\n\nDetermining a compatible order for \n")
				.append(Arrays.stream(rules).collect(Collectors.joining("\n"))).append("\n\n")
				.append("These are the production-local orders:\n").append(g.printLocalExecutionOrders()).append("\n\n")
				.append(g.getLaTex()).append("\n--------------------\n");
		System.out.println(sb);
	}

	private static void figure_3_6() {
		String[] rules = { "A->B : x[0]=0; x[1]=x[0]; y[0]=y[1]; y[1]=0", //
				"A->a", //
				"A->c" };
		Grammar g = new Parser().init(List.of(rules));
		g.computeTransitiveClosure();
		g.computeAttributeGroups();

		StringBuilder sb = new StringBuilder();
		sb.append("\n--------------------\n\nFigure 3.6\n\nGrouping attributes of \n")
				.append(Arrays.stream(rules).collect(Collectors.joining("\n"))).append("\n\n")
				.append("These are the groups:\n").append(g.printAttributeGroups()).append("\n\n").append(g.getLaTex())
				.append("\n--------------------\n");
		System.out.println(sb);
	}

	private static void figure_3_7() {
		String[] rules = { "A->aB : x[0]=0; x[2]=x[0]; y[0]=y[2]; y[2]=0", //
				"A->bB : y[0]=0; x[0]=x[2]; y[2]=y[0]; x[2]=0", //
				"A->cB : y[0]=0; x[0]=y[2]; x[2]=y[0]; y[2]=0", //
				"A->dB : x[0]=0; y[0]=x[2]; y[2]=x[0]; x[2]=0" };
		Grammar g = new Parser().init(List.of(rules));
		g.computeTransitiveClosure();
		g.computeAttributeGroups();
		g.determineCompatibleLocalExecutionOrders();

		StringBuilder sb = new StringBuilder();
		sb.append("\n--------------------\n\nFigure 3.7\n\nDetermining a compatible order for \n")
				.append(Arrays.stream(rules).collect(Collectors.joining("\n"))).append("\n\n")
				.append("These are the production-local orders:\n").append(g.printLocalExecutionOrders()).append("\n\n")
				.append(g.getLaTex()).append("\n--------------------\n");
		System.out.println(sb);
	}

	private static void example_3_6_2() {
		String[] rules = { "A->aB : x[0]=0; x[2]=x[0]; y[0]=y[2]; y[2]=0", //
				"A->bB : y[0]=0; x[0]=x[2]; y[2]=y[0]; x[2]=0", //
				"A->cB : y[0]=0; x[0]=y[2]; x[2]=y[0]; y[2]=0" };
		Grammar g = new Parser().init(List.of(rules));
		g.computeTransitiveClosure();
		g.computeAttributeGroups();
		g.determineCompatibleLocalExecutionOrders();

		StringBuilder sb = new StringBuilder();
		sb.append("\n--------------------\n\nExample 3.6.2\n\nDetermining a compatible order for \n")
				.append(Arrays.stream(rules).collect(Collectors.joining("\n"))).append("\n\n")
				.append("These are the production-local orders:\n").append(g.printLocalExecutionOrders()).append("\n\n")
				.append(g.getLaTex()).append("\n--------------------\n");
		System.out.println(sb);
	}

	private static void figure_4_1() {
		String[] rules = {
				"L->SL : mOK[0]=mOK[1]+mOK[2]; auxEnv[0]=symb[1]+auxEnv[2]; mOK[1]=cMod[0]+sMod[1]; env[1]=auxEnv[0]+env[0]; env[2]=auxEnv[0]+env[0]; cMod[2]=cMod[0]", //
				"C->ML : cMod[2]=cMod[1]; mOK[0]=mOK[2]" };
		Grammar g = new Parser().init(List.of(rules));
		g.computeTransitiveClosure();
		g.computeAttributeGroups();
		g.determineCompatibleLocalExecutionOrders();

		StringBuilder sb = new StringBuilder();
		sb.append("\n--------------------\n\nFigure 4.1\n\nExecuting the complete algorithm on a realistic example \n")
				.append(Arrays.stream(rules).collect(Collectors.joining("\n"))).append("\n\n")
				.append("These are the production-local orders:\n").append(g.printLocalExecutionOrders()).append("\n\n")
				.append(g.getLaTex()).append("\n--------------------\n");
		System.out.println(sb);
	}
}
