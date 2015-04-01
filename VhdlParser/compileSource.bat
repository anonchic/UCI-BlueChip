cd source
javac -classpath .;..\..\VHDLExpressionEval\src Vhdl.java
"C:\Program Files\7-Zip\7z.exe" a -tzip -mx0 -y vhdlParser.zip *.class
move vhdlParser.zip ..\vhdlParser.zip
del *.class
pause