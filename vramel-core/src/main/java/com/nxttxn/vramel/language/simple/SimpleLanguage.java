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
package com.nxttxn.vramel.language.simple;


import com.nxttxn.vramel.Expression;
import com.nxttxn.vramel.Predicate;
import com.nxttxn.vramel.builder.ExpressionBuilder;
import com.nxttxn.vramel.support.LanguageSupport;
import com.nxttxn.vramel.util.ObjectHelper;
import org.apache.camel.language.simple.SimpleTokenizer;

/**
 * A <a href="http://camel.apache.org/simple.html">simple language</a>
 * which maps simple property style notations to access headers and bodies.
 * Examples of supported expressions are:
 * <ul>
 * <li>exchangeId to access the exchange id</li>
 * <li>id to access the inbound message id</li>
 * <li>in.body or body to access the inbound body</li>
 * <li>in.body.OGNL or body.OGNL to access the inbound body using an OGNL expression</li>
 * <li>mandatoryBodyAs(&lt;classname&gt;) to convert the in body to the given type, will throw exception if not possible to convert</li>
 * <li>bodyAs(&lt;classname&gt;) to convert the in body to the given type, will return null if not possible to convert</li>
 * <li>headerAs(&lt;key&gt;, &lt;classname&gt;) to convert the in header to the given type, will return null if not possible to convert</li>
 * <li>out.body to access the inbound body</li>
 * <li>in.header.foo or header.foo to access an inbound header called 'foo'</li>
 * <li>in.header.foo[bar] or header.foo[bar] to access an inbound header called 'foo' as a Map and lookup the map with 'bar' as key</li>
 * <li>in.header.foo.OGNL or header.OGNL to access an inbound header called 'foo' using an OGNL expression</li>
 * <li>out.header.foo to access an outbound header called 'foo'</li>
 * <li>property.foo to access the exchange property called 'foo'</li>
 * <li>property.foo.OGNL to access the exchange property called 'foo' using an OGNL expression</li>
 * <li>sys.foo to access the system property called 'foo'</li>
 * <li>sysenv.foo to access the system environment called 'foo'</li>
 * <li>exception.messsage to access the exception message</li>
 * <li>threadName to access the current thread name</li>
 * <li>date:&lt;command&gt;:&lt;pattern&gt; for date formatting using the {@link java.text.SimpleDateFormat} patterns.
 *     Supported commands are: <tt>now</tt> for current timestamp,
 *     <tt>in.header.xxx</tt> or <tt>header.xxx</tt> to use the Date object in the in header.
 *     <tt>out.header.xxx</tt> to use the Date object in the out header.
 * </li>
 * <li>bean:&lt;bean expression&gt; to invoke a bean using the
 * {@link org.apache.camel.language.bean.BeanLanguage BeanLanguage}</li>
 * <li>properties:&lt;[locations]&gt;:&lt;key&gt; for using property placeholders using the
 *     {@link org.apache.camel.component.properties.PropertiesComponent}.
 *     The locations parameter is optional and you can enter multiple locations separated with comma.
 * </li>
 * </ul>
 * <p/>
 * The simple language supports OGNL notation when accessing either body or header.
 * <p/>
 * The simple language now also includes file language out of the box which means the following expression is also
 * supported:
 * <ul>
 *   <li><tt>file:name</tt> to access the file name (is relative, see note below))</li>
 *   <li><tt>file:name.noext</tt> to access the file name with no extension</li>
 *   <li><tt>file:name.ext</tt> to access the file extension</li>
 *   <li><tt>file:ext</tt> to access the file extension</li>
 *   <li><tt>file:onlyname</tt> to access the file name (no paths)</li>
 *   <li><tt>file:onlyname.noext</tt> to access the file name (no paths) with no extension </li>
 *   <li><tt>file:parent</tt> to access the parent file name</li>
 *   <li><tt>file:path</tt> to access the file path name</li>
 *   <li><tt>file:absolute</tt> is the file regarded as absolute or relative</li>
 *   <li><tt>file:absolute.path</tt> to access the absolute file path name</li>
 *   <li><tt>file:length</tt> to access the file length as a Long type</li>
 *   <li><tt>file:size</tt> to access the file length as a Long type</li>
 *   <li><tt>file:modified</tt> to access the file last modified as a Date type</li>
 *   <li><tt>date:&lt;command&gt;:&lt;pattern&gt;</tt> for date formatting using the {@link java.text.SimpleDateFormat} patterns.
 *     Additional Supported commands are: <tt>file</tt> for the last modified timestamp of the file.
 *     All the commands from {@link SimpleLanguage} is also available.
 *   </li>
 * </ul>
 * The <b>relative</b> file is the filename with the starting directory clipped, as opposed to <b>path</b> that will
 * return the full path including the starting directory.
 * <br/>
 * The <b>only</b> file is the filename only with all paths clipped.
 *
 */
public class SimpleLanguage extends LanguageSupport {

    // singleton for expressions without a result type
    private static final SimpleLanguage SIMPLE = new SimpleLanguage();

    protected Class<?> resultType;
    protected boolean allowEscape = true;

    /**
     * Default constructor.
     */
    public SimpleLanguage() {
    }

    public Class<?> getResultType() {
        return resultType;
    }

    public void setResultType(Class<?> resultType) {
        this.resultType = resultType;
    }

    public boolean isAllowEscape() {
        return allowEscape;
    }

    public void setAllowEscape(boolean allowEscape) {
        this.allowEscape = allowEscape;
    }

    @Override
    public boolean isSingleton() {
        // we cannot be singleton as we have state
        return false;
    }

    public Predicate createPredicate(String expression) {
        ObjectHelper.notNull(expression, "expression");

        expression = loadResource(expression);

        // support old simple language syntax
        //removed deprecated old parser
        Predicate answer = null;
        if (answer == null) {
            // use the new parser
            SimplePredicateParser parser = new SimplePredicateParser(expression, allowEscape);
            answer = parser.parsePredicate();
        }
        return answer;
    }

    public Expression createExpression(String expression) {
        ObjectHelper.notNull(expression, "expression");

        expression = loadResource(expression);

        SimpleExpressionParser parser = new SimpleExpressionParser(expression, allowEscape);
        Expression answer  = parser.parseExpression();

        if (resultType != null) {
            answer = ExpressionBuilder.convertToExpression(answer, resultType);
        }
        return answer;
    }

    public static Expression simple(String expression) {
        return SIMPLE.createExpression(expression);
    }

    public static Expression simple(String expression, Class<?> resultType) {
        SimpleLanguage answer = new SimpleLanguage();
        answer.setResultType(resultType);
        return answer.createExpression(expression);
    }

    /**
     * Change the start tokens used for functions.
     * <p/>
     * This can be used to alter the function tokens to avoid clashes with other
     * frameworks etc.
     * <p/>
     * The default start tokens is <tt>${</tt> and <tt>$simple{</tt>.
     *
     * @param startToken new start token(s) to be used for functions
     */
    public static void changeFunctionStartToken(String... startToken) {
        SimpleTokenizer.changeFunctionStartToken(startToken);
    }

    /**
     * Change the end tokens used for functions.
     * <p/>
     * This can be used to alter the function tokens to avoid clashes with other
     * frameworks etc.
     * <p/>
     * The default end token is <tt>}</tt>
     *
     * @param endToken new end token(s) to be used for functions
     */
    public static void changeFunctionEndToken(String... endToken) {
        SimpleTokenizer.changeFunctionEndToken(endToken);
    }

    /**
     * Change the start token used for functions.
     * <p/>
     * This can be used to alter the function tokens to avoid clashes with other
     * frameworks etc.
     * <p/>
     * The default start tokens is <tt>${</tt> and <tt>$simple{</tt>.
     *
     * @param startToken new start token to be used for functions
     */
    public void setFunctionStartToken(String startToken) {
        changeFunctionStartToken(startToken);
    }

    /**
     * Change the end token used for functions.
     * <p/>
     * This can be used to alter the function tokens to avoid clashes with other
     * frameworks etc.
     * <p/>
     * The default end token is <tt>}</tt>
     *
     * @param endToken new end token to be used for functions
     */
    public void setFunctionEndToken(String endToken) {
        changeFunctionEndToken(endToken);
    }
}
