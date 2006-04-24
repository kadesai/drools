package org.drools.rule;

/*
 * Copyright 2005 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.beans.IntrospectionException;

import junit.framework.TestCase;

import org.drools.Cheese;
import org.drools.FactHandle;
import org.drools.WorkingMemory;
import org.drools.base.ClassFieldExtractor;
import org.drools.base.EvaluatorFactory;
import org.drools.common.InternalFactHandle;
import org.drools.reteoo.InstrumentedReteTuple;
import org.drools.reteoo.RuleBaseImpl;
import org.drools.spi.Evaluator;
import org.drools.spi.FieldExtractor;
import org.drools.spi.FieldValue;
import org.drools.spi.MockField;
import org.drools.spi.PredicateExpression;
import org.drools.spi.ReturnValueExpression;
import org.drools.spi.Tuple;

public class FieldConstraintTest extends TestCase {
    public FieldConstraintTest() {
        super();
    }

    /**
     * <pre>
     *        
     *         
     *                ( Cheese (type &quot;cheddar&quot;) )
     *          
     *         
     * </pre>
     * 
     * This is currently the same as using a ReturnValueConstraint just that it
     * doesn't need any requiredDeclarations
     * 
     * @throws IntrospectionException
     */
    public void testLiteralConstraint() throws IntrospectionException {
        RuleBaseImpl ruleBase = new RuleBaseImpl();
        WorkingMemory workingMemory = ruleBase.newWorkingMemory();

        ClassFieldExtractor extractor = new ClassFieldExtractor( Cheese.class,
                                                                 "type" );

        FieldValue field = new MockField( "cheddar" );

        Evaluator evaluator = EvaluatorFactory.getInstance().getEvaluator( Evaluator.OBJECT_TYPE,
                                                                           Evaluator.EQUAL );
        LiteralConstraint constraint = new LiteralConstraint( field,
                                                              extractor,
                                                              evaluator );

        Cheese cheddar = new Cheese( "cheddar",
                                     5 );

        InternalFactHandle cheddarHandle = (InternalFactHandle) workingMemory.assertObject( cheddar );

        // check constraint
        assertTrue( constraint.isAllowed( cheddarHandle,
                                          null,
                                          workingMemory ) );

        Cheese stilton = new Cheese( "stilton",
                                     5 );

        InternalFactHandle stiltonHandle = (InternalFactHandle) workingMemory.assertObject( stilton );

        // check constraint
        assertFalse( constraint.isAllowed( stiltonHandle,
                                           null,
                                           workingMemory ) );
    }

    /**
     * <pre>
     *        
     *         
     *                (Cheese (price ?price1 ) 
     *                (Cheese (price ?price2&amp;:(= ?price2 (* 2 ?price1) )
     *          
     *         
     * </pre>
     * 
     * @throws IntrospectionException
     */
    public void testPredicateConstraint() throws IntrospectionException {
        RuleBaseImpl ruleBase = new RuleBaseImpl();
        WorkingMemory workingMemory = ruleBase.newWorkingMemory();

        FieldExtractor priceExtractor = new ClassFieldExtractor( Cheese.class,
                                                                 "price" );

        // Bind the extractor to a decleration
        // Declarations know the column they derive their value form
        Declaration price1Declaration = new Declaration( "price1",
                                                         priceExtractor,
                                                         0 );

        // Bind the extractor to a decleration
        // Declarations know the column they derive their value form
        Declaration price2Declaration = new Declaration( "price2",
                                                         priceExtractor,
                                                         1 );

        PredicateExpression evaluator = new PredicateExpression() {

            public boolean evaluate(Tuple tuple,
                                    FactHandle factHandle,
                                    Declaration declaration,
                                    Declaration[] declarations,
                                    WorkingMemory workingMemory) {
                int price1 = ((Integer) declarations[0].getValue( workingMemory.getObject( tuple.get( declarations[0] ) ) )).intValue();
                int price2 = ((Integer) declaration.getValue( workingMemory.getObject( factHandle ) )).intValue();

                return (price2 == (price1 * 2));

            }
        };

        PredicateConstraint constraint1 = new PredicateConstraint( evaluator,
                                                                   price2Declaration,
                                                                   new Declaration[]{price1Declaration} );

        Cheese cheddar0 = new Cheese( "cheddar",
                                      5 );
        FactHandle f0 = workingMemory.assertObject( cheddar0 );
        InstrumentedReteTuple tuple = new InstrumentedReteTuple( f0 );

        Cheese cheddar1 = new Cheese( "cheddar",
                                      10 );
        InternalFactHandle f1 = (InternalFactHandle) workingMemory.assertObject( cheddar1 );

        tuple = new InstrumentedReteTuple( tuple,
                                           f1 );

        assertTrue( constraint1.isAllowed( f1,
                                           tuple,
                                           workingMemory ) );
    }

    /**
     * <pre>
     *        
     *         
     *                (Cheese (price ?price ) 
     *                (Cheese (price =(* 2 ?price) )
     *                (Cheese (price &gt;(* 2 ?price) )
     *          
     *         
     * </pre>
     * 
     * @throws IntrospectionException
     */
    public void testReturnValueConstraint() throws IntrospectionException {
        RuleBaseImpl ruleBase = new RuleBaseImpl();
        WorkingMemory workingMemory = ruleBase.newWorkingMemory();

        FieldExtractor priceExtractor = new ClassFieldExtractor( Cheese.class,
                                                                 "price" );

        // Bind the extractor to a decleration
        // Declarations know the column they derive their value form
        Declaration priceDeclaration = new Declaration( "price1",
                                                        priceExtractor,
                                                        0 );

        ReturnValueExpression isDoubleThePrice = new ReturnValueExpression() {
            public Object evaluate(Tuple tuple, // ?price
                                   Declaration[] declarations,
                                   WorkingMemory workingMemory) {
                int price = ((Integer) declarations[0].getValue( workingMemory.getObject( tuple.get( declarations[0] ) ) )).intValue();
                return new Integer( 2 * price );

            }
        };

        ReturnValueConstraint constraint1 = new ReturnValueConstraint( priceExtractor,
                                                                       isDoubleThePrice,
                                                                       new Declaration[]{priceDeclaration},
                                                                       EvaluatorFactory.getInstance().getEvaluator( Evaluator.INTEGER_TYPE,
                                                                                                                    Evaluator.EQUAL ) );

        ReturnValueConstraint constraint2 = new ReturnValueConstraint( priceExtractor,
                                                                       isDoubleThePrice,
                                                                       new Declaration[]{priceDeclaration},
                                                                       EvaluatorFactory.getInstance().getEvaluator( Evaluator.INTEGER_TYPE,
                                                                                                                    Evaluator.GREATER ) );

        Cheese cheddar0 = new Cheese( "cheddar",
                                      5 );
        FactHandle f0 = workingMemory.assertObject( cheddar0 );

        InstrumentedReteTuple tuple = new InstrumentedReteTuple( f0 );

        Cheese cheddar1 = new Cheese( "cheddar",
                                      10 );
        InternalFactHandle f1 = (InternalFactHandle) workingMemory.assertObject( cheddar1 );
        tuple = new InstrumentedReteTuple( tuple,
                                           f1 );

        assertTrue( constraint1.isAllowed( f1,
                                           tuple,
                                           workingMemory ) );

        assertFalse( constraint2.isAllowed( f1,
                                            tuple,
                                            workingMemory ) );

        Cheese cheddar2 = new Cheese( "cheddar",
                                      11 );

        InternalFactHandle f2 = (InternalFactHandle) workingMemory.assertObject( cheddar2 );

        assertTrue( constraint2.isAllowed( f2,
                                           tuple,
                                           workingMemory ) );
    }

}