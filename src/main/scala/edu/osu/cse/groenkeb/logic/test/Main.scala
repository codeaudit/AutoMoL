package edu.osu.cse.groenkeb.logic.test

import edu.osu.cse.groenkeb.logic.proof.rules._
import edu.osu.cse.groenkeb.logic.parse.DefaultPropOpMatcher
import edu.osu.cse.groenkeb.logic.parse.NodeRecursiveTokenizer
import edu.osu.cse.groenkeb.logic.parse.SentenceParser
import edu.osu.cse.groenkeb.logic.proof.types.ProudPremise
import edu.osu.cse.groenkeb.logic.proof.types.ProofContext
import edu.osu.cse.groenkeb.logic.proof.types.ProofContext
import edu.osu.cse.groenkeb.logic.proof.PropSolver
import edu.osu.cse.groenkeb.logic.proof.NaiveProofStrategy

object Main extends App {
  implicit val matcher = new DefaultPropOpMatcher()
  val tokenizer = new NodeRecursiveTokenizer()
  val parser = new SentenceParser(tokenizer)
  val sentenceA = parser.parse("A")
  val sentenceB = parser.parse("B")
  val sentenceC = parser.parse("C")
  val complexSentence = parser.parse("(and (and A B) C)")
  val rules = RuleSet(Seq(AndIntroductionRule()))
  implicit val proofContext = ProofContext(complexSentence, List(ProudPremise(sentenceA), ProudPremise(sentenceB), ProudPremise(sentenceC)), rules)
  implicit val proofStrategy = NaiveProofStrategy()
  val propSolver = new PropSolver()
  
  println(propSolver.proof)
  
  //val complexSentence = parser.parse("(A and (not (C or B)))", Notation("infix"))
  //println(complexSentence)
}