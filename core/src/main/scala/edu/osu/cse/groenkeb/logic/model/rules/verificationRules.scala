package edu.osu.cse.groenkeb.logic.model.rules

import edu.osu.cse.groenkeb.logic._
import edu.osu.cse.groenkeb.logic.proof.rules._
import edu.osu.cse.groenkeb.logic.proof._
import edu.osu.cse.groenkeb.logic.Domain

abstract class VerificationRule extends BaseRule {
  def major(sentence: Sentence) = false
}

case object NegationVerification extends VerificationRule {  
  def yields(conclusion: Sentence) = conclusion match {
    case Not(_) => true
    case _ => false
  }
  
  def params(major: Option[Sentence] = None)(implicit context: ProofContext) = goal match {
    case Not(sentence) if major == None => Some(UnaryParams(RelevantProof(Absurdity, Required(Assumption(sentence)))))
    case _ => None
  }

  def infer(args: RuleArgs)(implicit context: ProofContext) = goal match {
    case Not(sentence) => args match {
      case UnaryArgs(Proof(Conclusion(Absurdity,_,_), prems)) if exists(sentence).in(prems) =>
        val discharge = Assumption(sentence)
        Some(Proof(Conclusion(goal, this, args), prems - discharge))
      case _ => None
    }
    case _ => None
  }
  
  override def toString = "~V"
}

case object AndVerification extends VerificationRule {
  def yields(conclusion: Sentence) = conclusion match {
    case And(_,_) => true
    case _ => false
  }
  
  def params(major: Option[Sentence] = None)(implicit context: ProofContext) = goal match {
    case And(left, right) if major == None => Some(BinaryParams(AnyProof(left), AnyProof(right)))
    case _ => None
  }
  
  def infer(args: RuleArgs)(implicit context: ProofContext) = args match {
    case BinaryArgs(Proof(Conclusion(left, _, _), pleft), Proof(Conclusion(right, _, _), pright)) => Some(Proof(Conclusion(goal, this, args), pleft ++ pright))
    case _ => None
  }
  
  override def toString = "&V"
}

case object OrVerification extends VerificationRule {
  def yields(conc: Sentence) = conc match {
    case Or(_,_) => true
    case _ => false
  }

  def params(major: Option[Sentence] = None)(implicit context: ProofContext) = goal match {
    case Or(left, right) if major == None =>
      Some(OptionParams(
        UnaryParams(AnyProof(left)),
        UnaryParams(AnyProof(right))))
    case _ => None
  }

  def infer(args: RuleArgs)(implicit context: ProofContext) = goal match {
    case Or(left, right) => args match {
      case UnaryArgs(Proof(Conclusion(c, _, _), prems)) if c.matches(left) || c.matches(right) =>
        Some(Proof(Conclusion(goal, this, args), prems))
      case _ => None
    }
    case _ => None
  }
  
  override def toString = "+V"
}

case object ConditionalVerification extends VerificationRule {
  def yields(conc: Sentence) = conc match {
    case Implies(_,_) => true
    case _ => false
  }

  def params(major: Option[Sentence] = None)(implicit context: ProofContext) = goal match {
    case Implies(ante, conseq) if major == None =>
      Some(OptionParams(
        UnaryParams(AnyProof(conseq)),
        UnaryParams(RelevantProof(Absurdity, Required(Assumption(ante)), Assumption(Implies(ante, conseq))))))
    case _ => None
  }
  
  def infer(args: RuleArgs)(implicit context: ProofContext) = goal match {
    case Implies(ante, conseq) => args match {
      case UnaryArgs(Proof(Conclusion(`conseq`, _, _), prems)) =>
        Some(Proof(Conclusion(Implies(ante, conseq), this, args), prems))
      case UnaryArgs(Proof(Conclusion(Absurdity, _, _), prems)) if exists(ante).in(prems) =>
        val discharge = Assumption(ante)
        Some(Proof(Conclusion(Implies(ante, conseq), this, args), prems - discharge))
      case _ => None
    }
    case _ => None
  }
  
  override def toString = ">V"
}

case class UniversalVerification(domain: Domain) extends VerificationRule {
  def yields(conc: Sentence) = conc match {
    case ForAll(_, _) => true
    case _ => false
  }

  def params(major: Option[Sentence] = None)(implicit context: ProofContext) = goal match {
    case ForAll(term, sentence) if major == None =>
      Some(NParams(domain.terms.toSeq.map {
        t => AnyProof(sentence.substitute(term, t))
      }))
    case _ => None
  }

  def infer(args: RuleArgs)(implicit context: ProofContext) = {
    def validate(proofs: Seq[Proof], sentence: Sentence, term: Term): Boolean =
      proofs.length == domain.size && domain.terms.forall {
        t => proofs.exists { p => p.conclusion.sentence.matches(sentence.substitute(term, t)) }
      }

    goal match {
      case ForAll(term, sentence) => args match {
        case NArgs(proofs) if validate(proofs, sentence, term) =>
          Some(Proof(Conclusion(goal, UniversalVerification.this, args), proofs.flatMap { p => p.premises }.toSet))
        case _ => None
      }
      case _ => None
    }
  }

  override def toString = "UV"
}

case class ExistentialVerification(domain: Domain) extends VerificationRule() {
  def yields(conc: Sentence) = conc match {
    case Exists(_,_) => true
    case _ => false
  }

  def params(major: Option[Sentence] = None)(implicit context: ProofContext) = goal match {
    case Exists(term, sentence) if major == None =>
      Some(OptionParams(domain.terms.toSeq.map {
        t => UnaryParams(AnyProof(sentence.substitute(term, t)))
      }: _*))
    case _ => None
  }
  
  def infer(args: RuleArgs)(implicit context: ProofContext) = {
    def validate(proof: Proof, sentence: Sentence, term: Term): Boolean =
      domain.terms.exists { t => proof.conclusion.matches(sentence.substitute(term, t)) }
    
    goal match {
      case Exists(term, sentence) => args match {
        case UnaryArgs(proof) if validate(proof, sentence, term) =>
          Some(Proof(Conclusion(goal, ExistentialVerification.this, args), proof.premises))
        case _ => None
      }
      case _ => None
    }
  }
  
  override def toString = "EV"
}

