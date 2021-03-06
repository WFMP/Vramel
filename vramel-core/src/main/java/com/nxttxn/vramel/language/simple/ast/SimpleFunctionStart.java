/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nxttxn.vramel.language.simple.ast;

import com.nxttxn.vramel.Exchange;
import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.language.simple.types.SimpleIllegalSyntaxException;
import com.nxttxn.vramel.language.simple.types.SimpleParserException;
import com.nxttxn.vramel.util.StringHelper;
import org.apache.camel.language.simple.types.SimpleToken;


/**
 * Starts a function
 */
public class SimpleFunctionStart extends BaseSimpleNode implements BlockStart {

    private CompositeNodes block;

    public SimpleFunctionStart(SimpleToken token) {
        super(token);
        this.block = new CompositeNodes(token);
    }

    @Override
    public String toString() {
        // output a nice toString so it makes debugging easier as we can see the entire block
        return "${" + block + "}";
    }

    @Override
    public Expression createExpression(String expression) {
        // a function can either be a simple literal function, or contain nested functions
        if (block.getChildren().size() == 1 && block.getChildren().get(0) instanceof LiteralNode) {
            return doCreateLiteralExpression(expression);
        } else {
            return doCreateCompositeExpression(expression);
        }
    }

    private Expression doCreateLiteralExpression(final String expression) {
        SimpleFunctionExpression function = new SimpleFunctionExpression(this.getToken());
        LiteralNode literal = (LiteralNode) block.getChildren().get(0);
        function.addText(literal.getText());
        return function.createExpression(expression);
    }

    private Expression doCreateCompositeExpression(final String expression) {
        final SimpleToken token = getToken();
        return new Expression() {
            @Override
            public <T> T evaluate(Exchange exchange, Class<T> type) {
                StringBuilder sb = new StringBuilder();
                boolean quoteEmbeddedFunctions = false;

                // we need to concat the block so we have the expression
                for (SimpleNode child : block.getChildren()) {
                    if (child instanceof LiteralNode) {
                        String text = ((LiteralNode) child).getText();
                        sb.append(text);
                        quoteEmbeddedFunctions |= ((LiteralNode) child).quoteEmbeddedNodes();
                    // if its a function or quoted literal, then embed that as text
                    } else if (child instanceof SimpleFunctionStart || child instanceof SingleQuoteStart || child instanceof DoubleQuoteStart) {
                        try {
                            // pass in null when we evaluate the nested expressions
                            Expression nested = child.createExpression(null);
                            String text = nested.evaluate(exchange, String.class);
                            if (text != null) {
                                if (quoteEmbeddedFunctions && !StringHelper.isQuoted(text)) {
                                    sb.append("'").append(text).append("'");
                                } else {
                                    sb.append(text);
                                }
                            }
                        } catch (SimpleParserException e) {
                            // must rethrow parser exception as illegal syntax with details about the location
                            throw new SimpleIllegalSyntaxException(expression, e.getIndex(), e.getMessage(), e);
                        }
                    }
                }

                // we have now concat the block as a String which contains the function expression
                // which we then need to evaluate as a function
                String exp = sb.toString();
                SimpleFunctionExpression function = new SimpleFunctionExpression(token);
                function.addText(exp);
                try {
                    return function.createExpression(exp).evaluate(exchange, type);
                } catch (SimpleParserException e) {
                    // must rethrow parser exception as illegal syntax with details about the location
                    throw new SimpleIllegalSyntaxException(expression, e.getIndex(), e.getMessage(), e);
                }
            }

            @Override
            public String toString() {
                return expression;
            }
        };
    }

    @Override
    public boolean acceptAndAddNode(SimpleNode node) {
        // only accept literals, quotes or embedded functions
        if (node instanceof LiteralNode || node instanceof SimpleFunctionStart
                || node instanceof SingleQuoteStart || node instanceof DoubleQuoteStart) {
            block.addChild(node);
            return true;
        } else {
            return false;
        }
    }

}
