/*
 * Copyright 2008  Reg Whitton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.dev.eval.example.calculator;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import junit.framework.TestCase;

public class CalculatorTest extends TestCase
{

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	}

	public final void testExecute() throws Exception
	{
		String input = "x = 4*4\n" // 
				+ "y=5*5\n" //
				+ "x + y \n" //
				+ "\n" //
				+ "show\n" //
				+ "x==y?7:17\n" //
				+ "x!=y?7:17\n" //
				+ "quit\n";

		InputStream inputStream = new ByteArrayInputStream(input.getBytes());
		PipedInputStream resultsStream = new PipedInputStream();
		OutputStream outputStream = new PipedOutputStream(resultsStream);
		OutputStream errorStream = new ByteArrayOutputStream();

		new Calculator().execute(inputStream, outputStream, errorStream);

		BufferedReader resultsReader = new BufferedReader(
				new InputStreamReader(resultsStream));
		assertEquals("41", resultsReader.readLine());
		assertEquals("{x=16, y=25}", resultsReader.readLine());
		assertEquals("17", resultsReader.readLine());
		assertEquals("7", resultsReader.readLine());

		assertEquals("", errorStream.toString());
	}
}
