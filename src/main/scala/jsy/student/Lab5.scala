package jsy.student

import jsy.lab5.Lab5Like

object Lab5 extends jsy.util.JsyApplication with Lab5Like {
  import jsy.lab5.ast._
  import jsy.util.DoWith
  import jsy.util.DoWith._

  /*
   * CSCI 3155: Lab 5
   * <Drew Casner>
   *
   * Partner: <None>
   * Collaborators: <Any Collaborators>
   */

  /*
   * Fill in the appropriate portions above by replacing things delimited
   * by '<'... '>'.
   *
   * Replace the '???' expression with your code in each function.
   *
   * Do not make other modifications to this template, such as
   * - adding "extends App" or "extends Application" to your Lab object,
   * - adding a "main" method, and
   * - leaving any failing asserts.
   *
   * Your lab will not be graded if it does not compile.
   *
   * This template compiles without error. Before you submit comment out any
   * code that does not compile or causes a failing assert. Simply put in a
   * '???' as needed to get something that compiles without error. The '???'
   * is a Scala expression that throws the exception scala.NotImplementedError.
   */

  /*** Exercise with DoWith ***/

  def rename[W](env: Map[String,String], e: Expr)(fresh: String => DoWith[W,String]): DoWith[W,Expr] = {
    def ren(env: Map[String,String], e: Expr): DoWith[W,Expr] = e match {
      case N(_) | B(_) | Undefined | S(_) | Null | A(_) => doreturn(e)
      case Print(e1)                                    => ren(env,e1) map { e1p => Print(e1p) }
      case Unary(uop, e1)                               => ren(env, e1) map {e1p => Unary(uop, e1p)}
      case Binary(bop, e1, e2)                          => ren(env, e1) flatMap {e1p => ren(env, e2) map {e2p => Binary(bop, e1p, e2p)}}
      case If(e1, e2, e3)                               => ren(env, e1) flatMap {e1p => ren(env,e2) flatMap {e2p => ren(env, e3) map{e3p => If(e1p, e2p, e3p)}}}
      case Var(x)                                       => if (env.contains(x)) doreturn(Var(env(x))) else doreturn(Var(x))
      case Decl(m, x, e1, e2)                           => fresh(x) flatMap { xp => ren(env, e1) flatMap {e1p => ren(extend(env, x, xp), e2) map {e2p => Decl(m, xp, e1p, e2p)}}}

      case Function(p, params, retty, e1)               => {
        val w: DoWith[W,(Option[String], Map[String,String])] = p match {
          case None => doreturn(None, env)
          case Some(x) => fresh(x) map {pp => (Some(pp), extend(env, x, pp))}
        }
        w flatMap { case (pp, envp) =>
          params.foldRight[DoWith[W,(List[(String,MTyp)],Map[String,String])]]( doreturn((Nil, envp)) ) {
            case ((x,mty), acc) => acc flatMap {
              case (paramspp, envpp) => fresh(x) map {xp => ((xp,mty)::paramspp, extend(envpp, x, xp)) }
            }
          } flatMap {
            case (paramsppp, envppp) => ren(envppp, e1) map {e1p => Function(pp, paramsppp, retty, e1p)}
          }
        }
      }

      case Call(e1, args)                               => ren(env, e1) flatMap {e1p => mapWith(args)(arg => ren(env, arg)) map {argsp => Call(e1p, argsp)}}
      case Obj(fields)                                  => mapWith(fields) {case (fi, ei) => ren(env, ei) map { eip => (fi, eip)}} map {fieldsp => Obj(fieldsp)}
      case GetField(e1, f)                              => ren(env, e1) map {ep1 => GetField(ep1, f)}
      case Assign(e1, e2)                               => ren(env, e1) flatMap { e1p => ren(env, e2) map { e2p => Assign(e1p, e2p)}}
    }
    ren(env, e)
  }

  def myuniquify(e: Expr): Expr = {
    val fresh: String => DoWith[Int,String] = { _ =>
      doget[Int].map((i:Int) => i.toString())
    }
    val (_, r) = rename(empty, e)(fresh)(0)
    r
  }

  /*** Helper: mapFirst to DoWith ***/

  // List map with an operator returning a DoWith
  def mapWith[W,A,B](l: List[A])(f: A => DoWith[W,B]): DoWith[W,List[B]] = {
    l.foldRight[DoWith[W,List[B]]]( doreturn(Nil) ) {
      case (a, dwbs) => dwbs.flatMap((bs) => f(a).map((b) => b::bs))
    }
  }

  def mapWith[W,A,B,C,D](m: Map[A,B])(f: ((A,B)) => DoWith[W,(C,D)]): DoWith[W,Map[C,D]] = {
    m.foldRight[DoWith[W,Map[C,D]]]( doreturn(Map.empty) ) {
      case (a, dwbs) => dwbs.flatMap((bs) => f(a).map((b) => bs+b))
    }
  }

  def mapFirstWith[W,A](l: List[A])(f: A => Option[DoWith[W,A]]): DoWith[W,List[A]] = l match {
    case Nil => doreturn(Nil)
    case h :: t => f(h) match {
      case None => mapFirstWith(t)(f) map {lp => h::lp}
      case Some(x) => x map {xp => xp::t}
    }
  }

  // There are better ways to deal with the combination of data structures like List, Map, and
  // DoWith, but we won't tackle that in this assignment.

  /*** Casting ***/

  def castOk(t1: Typ, t2: Typ): Boolean = (t1, t2) match {
    /***** Make sure to replace the case _ => ???. */
    case (TNull, TObj(_)) => true
    case (_, _) if (t1 == t2) => true
    case (TObj(fields1), TObj(fields2)) => {
      val check1 = fields2 forall {
        case (field_1, t_1) => fields1.get(field_1) match {
          case Some(t_2) => if (t_1 == t_2) true else false
          case None => true
        }
      }

      val check2 = fields1 forall {
        case (field_1, t_1) => fields2.get(field_1) match {
          case Some (t_2) => if (t_1 == t_2) true else false
          case None => true
        }
      }
      check1 || check2
    }
    /***** Cases for the extra credit. Do not attempt until the rest of the assignment is complete. */
    /***** Otherwise, false. */
    case _ => false
  }

  /*** Type Inference ***/

  // A helper function to check whether a jsy type has a function type in it.
  // While this is completely given, this function is worth studying to see
  // how library functions are used.
  def hasFunctionTyp(t: Typ): Boolean = t match {
    case TFunction(_, _) => true
    case TObj(fields) if (fields exists { case (_, t) => hasFunctionTyp(t) }) => true
    case _ => false
  }

  def isBindex(m: Mode, e: Expr): Boolean = m match {
    case MConst | MName | MVar => true
    case _                     => false
  }

  def typeof(env: TEnv, e: Expr): Typ = {
    def err[T](tgot: Typ, e1: Expr): T = throw StaticTypeError(tgot, e1, e)

    e match {
      case Print(e1)      => typeof(env, e1); TUndefined
      case N(_)           => TNumber
      case B(_)           => TBool
      case Undefined      => TUndefined
      case S(_)           => TString
      case Var(x)         => val MTyp(m, t) = lookup(env, x); t
      case Unary(Neg, e1) => typeof(env, e1) match {
        case TNumber      => TNumber
        case tgot         => err(tgot, e1)
      }

      /** *** Cases directly from Lab 4. We will minimize the test of these cases in Lab 5. */
      case Unary(Not, e1) => typeof(env, e1) match {
        case TBool        => TBool
        case tgot         => err(tgot, e1)
      }
      case Binary(Plus, e1, e2) => (typeof(env, e1), typeof(env, e2)) match {
        case (TNumber, TNumber) => TNumber
        case (TString, TString) => TString
        case (tgot, _)          => err(tgot, e1)
        case (_, tgot)          => err(tgot, e2)
      }
      case Binary(Minus | Times | Div, e1, e2) => (typeof(env, e1), typeof(env, e2)) match {
        case (TNumber, TNumber)                => TNumber
        case (tgot, _)                         => err(tgot, e1)
        case (_, tgot)                         => err(tgot, e2)
      }
      case Binary(Eq | Ne, e1, e2)           => (typeof(env, e1), typeof(env, e2)) match {
        case (t1, _) if hasFunctionTyp(t1)   => err(t1, e1)
        case (_, t2) if hasFunctionTyp(t2)   => err(t2, e2)
        case (t1, t2)                        => if (t1 == t2) TBool else err(t2, e2)
      }
      case Binary(Lt | Le | Gt | Ge, e1, e2) => (typeof(env, e1), typeof(env, e2)) match {
        case (TNumber, TNumber)              => TBool
        case (TString, TString)              => TBool
        case (TNumber | TString, tgot)       => err(tgot, e2)
        case (tgot, TString | TNumber)       => err(tgot, e1)
        case (tgot1, _)                      => err(tgot1, e1)
      }
      case Binary(And|Or, e1, e2) => (typeof(env, e1), typeof(env, e2)) match {
        case (TBool, TBool)       => TBool
        case (TBool, tgot)        => err(tgot, e2)
        case (tgot,_)             => err(tgot, e1)
      }
      case Binary(Seq, e1, e2) => (typeof(env, e1), typeof(env, e2)) match {
        case (_, t2)           => t2
      }
      case If(e1, e2, e3)    => (typeof(env, e1), typeof(env, e2), typeof(env, e3)) match {
        case (TBool, t1, t2) => if(t1 == t2) t1 else err(t2, e2)
      }
      case Obj(fields) => fields foreach {  (ei) => typeof(env, ei._2)}; TObj(fields mapValues { (ei) => typeof(env, ei)})

      case GetField(e1, f) => typeof(env, e1) match {
        case TObj(tfields) => tfields.get(f) match {
          case Some(v)     => v
          case None        => err(TObj(tfields), e1)
        }
      }

      /***** Cases from Lab 4 that need a small amount of adapting. */
      case Decl(m, x, e1, e2) => if(isBindex(m,e1)) typeof(env + (x -> MTyp(m, typeof(env, e1))),e2) else err(typeof(env, e1), e1)
      case Function(p, params, tann, e1) => {
        // Bind to env1 an environment that extends env with an appropriate binding if
        // the function is potentially recursive.
        val env1 = (p, tann) match {
          case (Some(f), Some(tret)) =>
            val tprime = TFunction(params, tret)
            env + (f -> MTyp(MConst,tprime))
          case (None, _) => env
          case _ => err(TUndefined, e1)
        }
        // Bind to env2 an environment that extends env1 with bindings for params.
        val env2 = env1 ++ params.toMap
        // Match on whether the return type is specified.
        tann match {
          case None => ()
          case Some(tret) => if(tret != typeof(env2, e1)) err(tret, e1)
          case _ => ()
        }
        TFunction(params, typeof(env2, e1))
      }

      case Call(e1, args) => typeof(env, e1) match {
        case TFunction(params, tret) if (params.length == args.length) =>
          (params, args).zipped.foreach {
            case ((x, MTyp(m, t1)), ei) => if(t1 != typeof(env,ei) || !isBindex(m, ei)) err(t1, ei)
          }
          tret
        case tgot => err(tgot, e1)
      }

      /***** New cases for Lab 5. ***/
      case Assign(Var(x), e1) => env(x) match {
        case MTyp(MVar | MRef, t) => typeof(env, e1) match {
          case `t` => t
          case tgot => err(tgot, Var(x))
        }
        case MTyp(_, tgot) => err(tgot, Var(x))
      }
      case Assign(GetField(e1, f), e2) => typeof(env, e1) match {
        case t @ TObj(tfields) =>
          if(tfields contains f) typeof(env, e2) match {
            case t if(t == tfields(f)) => t
            case tgot                  => err(tgot, e2)
          }
          else err(t, e1)
        case tgot => err(tgot, e1)
      }

      case Assign(_, _) => err(TUndefined, e)

      case Null => null

      case Unary(Cast(t), e1) => typeof(env, e1) match {
        case tgot if castOk(tgot, t) => t
        case tgot => err(tgot, e1)
      }
    }
  }

  /*** Small-Step Interpreter ***/

  /*
   * Helper function that implements the semantics of inequality
   * operators Lt, Le, Gt, and Ge on values.
   *
   * We suggest a refactoring of code from Lab 2 to be able to
   * use this helper function in eval and step.
   *
   * This should the same code as from Lab 3 and Lab 4.
   */
  def inequalityVal(bop: Bop, v1: Expr, v2: Expr): Boolean = {
    require(isValue(v1), s"inequalityVal: v1 ${v1} is not a value")
    require(isValue(v2), s"inequalityVal: v2 ${v2} is not a value")
    require(bop == Lt || bop == Le || bop == Gt || bop == Ge)
    (v1, v2) match {
      case (S(s1), S(s2)) => bop match {
        case Lt => if (s1 < s2) true else false
        case Le => if (s1 <= s2) true else false
        case Gt => if (s1 > s2) true else false
        case Ge => if (s1 >= s2) true else false
      }
      case (N(n1), N(n2)) => bop match {
        case Lt => if (n1 < n2) true else false
        case Le => if (n1 <= n2) true else false
        case Gt => if (n1 > n2) true else false
        case Ge => if (n1 >= n2) true else false
      }
    }
  }

  /* Capture-avoiding substitution in e replacing variables x with esub. */
  def substitute(e: Expr, esub: Expr, x: String): Expr = {
    def subst(e: Expr): Expr = e match {
      case N(_) | B(_) | Undefined | S(_) | Null | A(_) => e
      case Print(e1)            => Print(subst(e1))
      /***** Cases from Lab 3 */
      case Unary(uop, e1)       => Unary(uop, substitute(e1, esub, x))
      case Binary(bop, e1, e2)  => Binary(bop, substitute(e1, esub, x), substitute(e2, esub, x))
      case If(e1, e2, e3)       => If(substitute(e1, esub, x), substitute(e2, esub, x), substitute(e3, esub, x))
      case Var(y)               => if(x==y) esub else Var(y)
      /***** Cases need a small adaption from Lab 3 */
      case Decl(mut, y, e1, e2) => Decl(mut, y, subst(e1), if (x == y) e2 else subst(e2))
      /***** Cases needing adapting from Lab 4 */
      case Function(p, paramse, retty, e1) => if(paramse.exists((params) => params._1 == x) || Some(x) == p) e else Function(p, paramse, retty, subst(e1))
      /***** Cases directly from Lab 4 */
      case Call(e1, args)  => Call(substitute(e1, esub, x), args map {ei => substitute(e1, esub, x)})
      case Obj(fields)     => Obj(fields mapValues { (exp) => substitute(exp, esub, x)})
      case GetField(e1, f) => GetField(substitute(e1, esub, x), f)
      /***** New case for Lab 5 */
      case Assign(e1, e2)  => Assign(subst(e1), subst(e2))
    }

    def myrename(e: Expr): Expr = {
      val fvs = freeVars(esub)
      def fresh(x: String): String = if (fvs contains x) fresh(x + "$") else x
      rename[Unit](e)(){ x => doreturn(fresh(x)) }
    }

    subst(myrename(e))
  }

  /* Check whether or not an expression is reduced enough to be applied given a mode. */
  def isRedex(mode: Mode, e: Expr): Boolean = mode match {
    case MConst | MVar => !isValue(e)
    case MRef          => !isLValue(e)
    case _             => false
  }

  def getBinding(mode: Mode, e: Expr): DoWith[Mem,Expr] = {
    require(!isRedex(mode,e), s"expression ${e} must not reducible under mode ${mode}")
    mode match {
      case MConst | MName | MRef => doreturn(e)
      case MVar                  => memalloc(e) map { ads => Unary(Deref, ads)}
    }
  }

  /* A small-step transition. */
  def step(e: Expr): DoWith[Mem, Expr] = {
    require(!isValue(e), "stepping on a value: %s".format(e))
    e match {
      /* Base Cases: Do Rules */
      case Print(v1) if isValue(v1) => doget map { m => println(pretty(m, v1)); Undefined }
      /***** Cases needing adapting from Lab 3. */
      case Unary(Neg, N(v1))                                 => doreturn(N(-v1))
      case Unary(Not, B(v1))                                 => doreturn(B(!v1))
      case Binary(Plus, S(s1), S(s2))                        => doreturn(S(s1+s2))
      case Binary(Plus , N(v1), N(v2))                       => doreturn(N(v1 + v2))
      case Binary(Minus, N(v1), N(v2))                       => doreturn(N(v1 - v2))
      case Binary(Times, N(v1), N(v2))                       => doreturn(N(v1 * v2))
      case Binary(Div  , N(v1), N(v2))                       => doreturn(N(v1 / v2))
      case Binary(Lt, S(v1), S(v2))                          => doreturn(B(v1 <  v2))
      case Binary(Le, S(v1), S(v2))                          => doreturn(B(v1 <= v2))
      case Binary(Gt, S(v1), S(v2))                          => doreturn(B(v1 >  v2))
      case Binary(Ge, S(v1), S(v2))                          => doreturn(B(v1 >= v2))
      case Binary(Lt, N(v1), N(v2))                          => doreturn(B(v1 <  v2))
      case Binary(Le, N(v1), N(v2))                          => doreturn(B(v1 <= v2))
      case Binary(Gt, N(v1), N(v2))                          => doreturn(B(v1 >  v2))
      case Binary(Ge, N(v1), N(v2))                          => doreturn(B(v1 >= v2))
      case Binary(Eq, v1, v2) if isLValue(v1) && isValue(v2) => doreturn(B(v1 == v2))
      case Binary(Ne, v1, v2) if isLValue(v1) && isValue(v2) => doreturn(B(v1 != v2))
      case Binary(And, B(true), e2)                          => doreturn(e2)
      case Binary(And, B(false), _)                          => doreturn(B(false))
      case Binary(Or, B(true), _)                            => doreturn(B(true))
      case Binary(Or, B(false), e2)                          => doreturn(e2)
      case If(B(true), e2, _)                                => doreturn(e2)
      case If(B(false), _, e3)                               => doreturn(e3)
      /***** Cases needing adapting from Lab 4. */
      case Obj(fields) if (fields forall { case (_, vi) => isValue(vi)}) =>
        memalloc(Obj(fields))
      case GetField(a @ A(_), f) =>
        doget map {
          mem => mem(a) match {
            case Obj(fields) => fields(f)
            case _ => throw StuckError(e)
          }
        }
      case Decl(MConst, x, v1, e2) if isValue(v1) =>
        getBinding(MConst, v1) map {
          v1p => substitute(e2, v1p, x)
        }
      /***** New cases for Lab 5. */
      case Unary(Deref, a @ A(_)) => doget map {
        memmory=> memmory(a)
      }

      case Assign(Unary(Deref, a @ A(_)), v) if isValue(v) =>
        domodify[Mem] { m => m + (a,v) } map { _ => v }

      case Assign(GetField(a @ A(_), f), v) if isValue(v) =>
        throw NullDereferenceError(e)

      case Call(v @ Function(p, params, _, e), args) => {
        val pazip = params zip args
        if (???) {
          val dwep = pazip.foldRight( ??? : DoWith[Mem,Expr] )  {
            case (((xi, MTyp(mi, _)), ei), dwacc) => ???
          }
          p match {
            case None => ???
            case Some(x) => ???
          }
        }
        else {
          val dwpazipp = mapFirstWith(pazip) {
            ???
          }
          ???
        }
      }

      /* Base Cases: Error Rules */
      /***** Replace the following case with a case to throw NullDeferenceError.  */
      //case _ => throw NullDeferenceError(e)

      /* Inductive Cases: Search Rules */
      /***** Cases needing adapting from Lab 3. Make sure to replace the case _ => ???. */
      case Print(e1) => step(e1) map {
        e1p => Print(e1p)
      }
      case Unary(uop, e1) => step(e1) map {
        e1p => Unary(uop, e1p)
      }
      case Binary(bop, v1, e2) if isValue(v1) => step(e2) map {
        e2p => Binary(bop, v1, e2p)
      }
      case Binary(bop, e1, e2) => step(e1) map {
        e1p => Binary(bop, e1p, e2)
      }
      case If(e1, e2, e3)=> step(e1) map {
        e1p => If(e1p, e2, e3)
      }
      case GetField(e1, f) => step(e1) map {
        e1p => GetField(e1p, f)
      }
      case Obj(fields) => {
        val item = fields.find(kv => kv match{case(k,v) => !isValue(v)})
        item match {
          case None => doreturn(Obj(fields))
          case Some(t1) => step(t1._2) map {
            t12p => Obj(fields + (t1._1 -> t12p))
          }
        }
      }
      case Decl(mode, x, e1, e2) => step(e1) map {
        e1p => Decl(mode, x, e1p, e2)
      }
      case Call(e1, args) if !isValue(e1) => step(e1) map {
        e1p => Call(e1p, args)
      }
      case Call(f @ Function(_,params,_,_),args) => ???

      /***** New cases for Lab 5.  */
      case Assign(e1, e2) if !isLValue(e1) => step(e1) map { e1p => Assign(e1p, e2) }
      case Assign(v1, e2) if isLValue(v1) => step(e2) map { e2p => Assign(v1, e2p) }

      /* Everything else is a stuck error. */
      case _ => throw StuckError(e)
    }
  }

  /*** Extra Credit: Lowering: Remove Interface Declarations ***/

  def lower(e: Expr): Expr =
  /* Do nothing by default. Change to attempt extra credit. */
    e

  /*** External Interfaces ***/

  //this.debug = true // comment this out or set to false if you don't want print debugging information
  this.maxSteps = Some(1000) // comment this out or set to None to not bound the number of steps.
  this.keepGoing = true // comment this out if you want to stop at first exception when processing a file
}
