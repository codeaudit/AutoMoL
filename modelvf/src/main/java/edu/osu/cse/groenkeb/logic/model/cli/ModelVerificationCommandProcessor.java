package edu.osu.cse.groenkeb.logic.model.cli;

import static edu.osu.cse.groenkeb.logic.model.implicits.standardRules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Stream;

import edu.osu.cse.groenkeb.logic.*;
import edu.osu.cse.groenkeb.logic.parse.*;
import edu.osu.cse.groenkeb.logic.model.*;
import edu.osu.cse.groenkeb.logic.proof.*;
import edu.osu.cse.groenkeb.logic.proof.engine.*;
import edu.osu.cse.groenkeb.logic.proof.rules.*;
import edu.osu.cse.groenkeb.utils.Convert;
import scala.collection.JavaConversions;

public class ModelVerificationCommandProcessor implements CommandProcessor<ModelVerificationContext>
{
  private static final String QUERY = "?";
  private static final String SET = "$";
  
  private final Scanner input;
  private final SentenceParser parser;
  private final SentenceParserOpts opts;
  
  public ModelVerificationCommandProcessor(final Scanner input, final SentenceParser parser, final SentenceParserOpts opts)
  {
    this.input = input;
    this.parser = parser;
    this.opts = opts;
  }
  
  @Override
  public Command<ModelVerificationContext> tryParseNextCommand() throws InvalidCommandException
  {
    final String next = input.nextLine();
    if (next == null || next.isEmpty()) return null;
    
    final String cmdStr = next.substring(1, next.length()).trim();
    try
    {
      if (next.startsWith(QUERY))
      {
        return parseQuery(cmdStr);
      }
      else if (next.startsWith(SET))
      {
        return parseSet(cmdStr);
      }
    }
    catch (Exception e)
    {
      e.printStackTrace ();
      throw new InvalidCommandException("unable to parse input: " + e.getMessage());
    }
    
    throw new InvalidCommandException("unrecognized command string: " + next);
  }
  
  public QueryCommand parseQuery(final String input)
  {
    final Sentence querySentence = this.parser.parse(input, opts);
    return new QueryCommand(querySentence);
  }
  
  public SetCommand parseSet(final String input)
  {
    Domain auxDomain = new Domain();
    final List<Sentence> sentences = new ArrayList<Sentence>();
    for (final String pt : input.split(";"))
    {
      final String str = pt.trim();
      if (str.startsWith("{") && str.endsWith("}"))
      {
        final List<String> termStrs = Arrays.asList(str.substring(1, str.length() - 1).split(","));
        final Stream<Term> terms = termStrs.stream().map((String s) -> new Term(s));
        auxDomain = auxDomain.merge(Domain.apply(Convert.toScalaItr(terms::iterator)));
      }
      else
      {
        sentences.add(this.parser.parse(pt, this.opts));
      }
    }
    
    final FirstOrderModel model = FirstOrderModel.from(Convert.toScalaSeq(sentences));
    return new SetCommand(model.withDomain(auxDomain));
  }
  
  private class QueryCommand implements Command<ModelVerificationContext>
  {
    private final Sentence sentence;
    private final ProofSolver solver = new ProofSolver(new EvaluationProofStrategy(), Convert.<SolverOpts>emptyScalaSeq());
    
    QueryCommand(final Sentence sentence)
    {
      this.sentence = sentence;
    }
    
    @Override
    public ModelVerificationContext execute(ModelVerificationContext current)
    {
      final RuleSet rules = standardRules (current.getModel ());
      final ProofContext verifyProofContext = new ProofContext(this.sentence, rules, Convert.<Premise>emptyScalaSeq());
      final HashSet<Premise> falsifyPremiseSet = new HashSet<Premise> ();
      falsifyPremiseSet.add (new Assumption(this.sentence));
      final ProofContext falsifyProofContext = new ProofContext(Sentences.absurdity (), rules, Convert.toScalaSet (falsifyPremiseSet));
      java.util.Iterator<ProofResult> results;
      final scala.collection.immutable.Stream <ProofResult> verifyResults = solver.prove(verifyProofContext);
      results = JavaConversions.asJavaIterator (verifyResults.iterator ());
      while (results.hasNext())
      {
        ProofResult result = results.next();
        if (result instanceof Success)
        {
          ProofUtils.prettyPrint (((Success) result).proof ());
          System.out.println ("---------");
        }
      }
      
      final scala.collection.immutable.Stream<ProofResult> falsifyResults = solver.prove(falsifyProofContext);
      results = JavaConversions.asJavaIterator (falsifyResults.iterator());
      while (results.hasNext())
      {
        ProofResult result = results.next();
        if (result instanceof Success)
        {
          ProofUtils.prettyPrint (((Success) result).proof ());
          System.out.println ("---------");
        }
      }
      
      return current;
    }
  }
  
  private class SetCommand implements Command<ModelVerificationContext>
  {
    private final FirstOrderModel model;
    
    SetCommand(FirstOrderModel model)
    {
      this.model = model;
    }
    
    public ModelVerificationContext execute(ModelVerificationContext current)
    {
      return new ModelVerificationContext(model);
    }
  }
}
