/*
 * Copyright (c) 2015-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
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
package org.opencypher.tools.grammar;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import javafx.util.Pair;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.LexerInterpreter;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.tool.ast.GrammarRootAST;
import org.junit.Test;

import static org.junit.Assert.fail;

public class Antlr4ParserTest
{

    @Test
    public void shouldReportInvalidCypher() throws FileNotFoundException, URISyntaxException
    {
        List<String> queries = getQueries( "/cypher-error.txt" );
        Stream<Pair<Boolean,String>> results = queries.stream().map( query ->
        {
            SyntaxError lexerListener = new SyntaxError();
            SyntaxError parserListener = new SyntaxError();
            Antlr4TestUtils.parseLegacyWithListeners( query, lexerListener, parserListener );
            return new Pair<>( parserListener.errorFound, query );
        } );

        results.forEach( r ->
        {
            if ( !r.getKey() )
            {
                fail( "Expected query to raise syntax error, but it did not: " + r.getValue() );
            }
        } );
    }

    @Test
    public void shouldParseValidCypher() throws FileNotFoundException, URISyntaxException
    {
        getQueries( "/cypher.txt" ).forEach( Antlr4TestUtils::parse );
    }

    @Test
    public void shouldParseLegacyCypher() throws FileNotFoundException, URISyntaxException
    {
        getQueries( "/cypher-legacy.txt" ).forEach( Antlr4TestUtils::parseLegacy );
    }

//    @Test
    public void investigateTokenStream() throws IOException
    {
        // Keep: Not really testing things but quite useful for debugging antlr lexing
        String query = "CREATE (a)";
        org.antlr.v4.Tool tool = new org.antlr.v4.Tool();

        GrammarRootAST ast = tool.parseGrammarFromString( new String( Files.readAllBytes(Paths.get("../../grammar/generated/Cypher.g4"))) );
        org.antlr.v4.tool.Grammar g = tool.createGrammar( ast );
        tool.process( g, false );

        LexerInterpreter lexer = g.createLexerInterpreter( new ANTLRInputStream( query ) );
        CommonTokenStream tokenStream = new CommonTokenStream( lexer );
    }

    private List<String> getQueries( String queryFile ) throws FileNotFoundException, URISyntaxException
    {
        URL resource = getClass().getResource( queryFile );
        Scanner scanner = new Scanner( new FileReader( Paths.get( resource.toURI() ).toFile() ) );
        scanner.useDelimiter( "§\n(//.*\n)*" );
        ArrayList<String> queries = new ArrayList<>();
            while ( scanner.hasNext() )
            {
                String next = scanner.next();
                queries.add( next );
            }
        return queries;
    }

    private static class SyntaxError implements ANTLRErrorListener
    {
        boolean errorFound = false;

        @Override
        public void syntaxError( Recognizer<?,?> recognizer, Object o, int i, int i1, String s, RecognitionException e )
        {
            errorFound = true;
        }

        @Override
        public void reportAmbiguity( Parser parser, DFA dfa, int i, int i1, boolean b, BitSet bitSet,
                ATNConfigSet atnConfigSet )
        {
        }

        @Override
        public void reportAttemptingFullContext( Parser parser, DFA dfa, int i, int i1, BitSet bitSet,
                ATNConfigSet atnConfigSet )
        {
        }

        @Override
        public void reportContextSensitivity( Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet )
        {
        }
    }
}
