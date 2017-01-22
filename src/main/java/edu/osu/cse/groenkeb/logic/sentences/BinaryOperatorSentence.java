package edu.osu.cse.groenkeb.logic.sentences;

import edu.osu.cse.groenkeb.logic.Term;
import edu.osu.cse.groenkeb.logic.TermVisitor;
import edu.osu.cse.groenkeb.logic.operators.BinaryOperator;

public class BinaryOperatorSentence implements Sentence<BinaryOperator>
{
  private final BinaryOperator op;
  private final Term a, b;
  
  public BinaryOperatorSentence(Term a, Term b, BinaryOperator op)
  {
    this.a = a;
    this.b = b;
    this.op = op;
  }
  
  public BinaryOperator getOperator()
  {
    return this.op;
  }
  
  public Term getFirstTerm()
  {
    return this.a;
  }
  
  public Term getSecondTerm()
  {
    return this.b;
  }
  
  @Override
  public void visit (TermVisitor visitor)
  {
    visitor.binarySentence (this);
  }
  
  @Override
  public String toString()
  {
    return String.format ("%s(%s,%s)", op, a, b);
  }

  @Override
  public String getName ()
  {
    return toString();
  }
}