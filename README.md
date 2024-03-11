This Attribute Evaluator is the proof of concept implementation for my Bachelor Thesis. It computes for a given Attribute Grammar transitive dependence relations and for each production a local execution order.

To try out all examples from my thesis, simply pass the example number as argument to main, e.g. "f3.1 f3.5 f4.1" for figures 3.1, 3.5 and 4.1.
Chapter 3.6.2 uses an example that is not displayed in my thesis. To try it out, use f3.8.

You can enter your own attribute grammars via console by leaving the arguments empty.
They follow the format for each production:

\<production-rule\> : \<semantic-rules\>

\<production-rule\> is:

\<nonterminal\> -> \<(non-)terminal\> \<(non-)terminal\> ...

with \<(non-)terminal\> being a single character. Whitespaces are ignored.
\<semantic-rules\> is a ;-separated list of semantic rules, each of them consisting of:

\<attribute\> = \<attribute\> \<attribute\> ...

with \<attribute\> being \<attr-name\>[\<index\>], and \<attr-name\> being an arbitrary string and \<index\> being the index of the annotated (non-)terminal.
