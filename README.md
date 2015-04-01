# UCI-BlueChip

Note that you will need <a href="http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html">Java</a> to compile and run many of the programs in this repo and <a href="https://java.net/projects/javacc/downloads">JavaCC 5</a> to re-build the VHDL parser that is used for UCI analysis.

# UCI Analysis

VHDLParser
  UCI analysis program.

VHDLUtils
  Support classes used by the UCI program.
  
VHDLExpressionEval
  Copy of external java library for evaluating complex expressions.
  
# Post UCI Analysis

PairsToVHDL
  Program that creates run-time hardware monitors that trigger BlueChip for the pairs that remain after UCI analysis.

pair_remover
  Program that removes code from a VHDL file based upon pairs that remain after UCI analysis.

Visuals
  Contains visualizations of the Leon3 and highlighting based-upon UCI analysis.
  
Highlighter
  Contains a program to highlight nodes in a graph given UCI analysis.
