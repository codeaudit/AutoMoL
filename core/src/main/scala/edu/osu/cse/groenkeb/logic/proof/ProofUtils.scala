package edu.osu.cse.groenkeb.logic.proof

import edu.osu.cse.groenkeb.logic.proof.rules.NullRule

object ProofUtils {
  def prettyPrint(proof: Proof) {
    var itr = ProofTraverser.preOrderTraversal(proof).iterator
    prettyPrint(itr, "")
  }
  
  private def prettyPrint(itr: Iterator[Proof], prefix: String) {
    if (!itr.hasNext) return
    var proof = itr.next()
    
    var conclusion = proof.conclusion
    if (conclusion.rule == NullRule) return;
    println(String.format("%s %s %s {%s}\n", prefix, conclusion.sentence, conclusion.rule, proof.premises.mkString(", ")))
    conclusion.args.prems foreach { p => prettyPrint(itr, prefix + "   ") }
  }
}