# scala-zio2-test-aspects-property-based-testing-workshop

* references
    * [What's Cooking in ZIO Test by Adam Fraser](https://www.youtube.com/watch?v=JtfdcxgQ71E)
    * [Using Aspects To Transform Your Code With ZIO Environment](https://www.youtube.com/watch?v=gcqWdNwNEPg)
    * https://zio.dev/reference/observability/logging
    * https://zio.dev/reference/test/aspects/
    * https://zio.dev/reference/test/property-testing/
    * https://www.zionomicon.com
    * https://github.com/adamgfraser/0-to-100-with-zio-test
    * https://docs.spring.io/spring-framework/docs/4.3.15.RELEASE/spring-framework-reference/html/aop.html
    * https://dotty.epfl.ch/docs/reference/new-types/polymorphic-function-types.html

## preface

* goals of this workshop
    * introduction to
        * functional programming aspects
        * property based testing
    * understanding how to use test aspects in practice
    * creating data generators
* workshops
    * task1: implement generator of `Accounts`
        * then derive it using `zio-test/magnolia`
        * solution: `AccountGenerators`
    * task2: derive generator of `Contributors`
        * then switch it to generate `Contributor` from file `src/test/resources/contributors.txt`
        * solution: `ContributorGenerators`
    * task3: implement and plug aspect to set specific seed (`TestSeed.seed`) before each test
        * solution: `MainSpec`
    * task4: experiment with intentionally failing some tests to see a seed
        * try to reproduce problem by setting correct seed in TestSeed

## aspect oriented programming
* lib: caliban
    * example
        ```
        val api =
            graphQL(???) @@
                maxDepth(50) @@
                timeout(3 seconds) @@
                printSlowQueries(500 millis) @@
                apolloTracing @@
                apolloCaching
        ```
    * supports aspects (called wrappers) that allow modifying: query parsing, validation and execution
* introduction
    * in any domain there are cross-cutting concerns that are shared among different parts of our main program logic
    * often these concerns are tangled with each part of our main program logic and scattered across different parts
    * we want to increase the modularity of our programs by separating these concerns from our main program logic
    * cross-cutting concerns are typically related to how we do something rather than what we are doing
        * what level of authorization should this transfer require?
        * how should this transfer be logged?
        * how should this transfer be recorded to our database
    * example: testing
        * main program logic: tests
        * concerns
            * how many times should we run a test?
            * what environments should we run the test on?
            * what sample size should we use for property based tests?
            * what degree of parallelism?
            * what timeout to use?
    * example: graphql
        * main program logic: queries
        * concerns
            * what is the maximum depth of nested queries we should support
            * what is the maximum number of fields we should support
            * what timeout should we use?
            * how should we handle slow queries?
            * what kind of tracing and caching should we use?
* traditional approach: metaprogramming
    * example: AspectJ
        ```
        @Aspect
        public class BeforeExample {

            @Before("execution(* com.xyz.myapp.dao.*.*(..))")
            public void doAccessCheck() {
                // ...
            }

        }
        ```
    * relies on implementation details such as class and method names that may change
    * no longer able to statically type check if code is dynamically generated
* functional approach: polymorphic functions
    * aspects are polymorphic functions
    * polymorphic function: scala3
        ```
        // A polymorphic method:
        def foo[A](xs: List[A]): List[A] = xs.reverse

        // A polymorphic function value:
        val bar: [A] => List[A] => List[A]
        //       ^^^^^^^^^^^^^^^^^^^^^^^^^
        //       a polymorphic function type
               = [A] => (xs: List[A]) => foo[A](xs)

        ```
    * example: zio
        ```
        trait Aspect[-R, +E] {
            def apply[R1 <: R, E1 >: E, A](zio: ZIO[R1, E1, A]): ZIO[R1, E1, A]
        }
        ```
        * potentially constraining the environment or widening the error type
        * transforms the how but not the what
        * composable
            ```
            implicit final class AspectSyntax[-R, +E, +A)(private val zio: ZIO[R, E, A]) {
                def @@[R1 <: R, E1 >: E](aspect: Aspect[R1, E1]): ZIO[R1, E1, A] =
                    aspect(zio)
            }
            ```
## test aspects
* example
    ```
    test("concurrency test") {
        ???
    } timeout(60.seconds)
    ```
* seamlessly control how tests are executed
    * example
        * without aspects
            ```
            test("foreachPar preserves ordering") {
                val zio = ZIO.foreach(1 to 100) { _ =>
                    ZIO.foreachPar(1 to 100)(ZIO.succeed(_)).map(_ == (1 to 100))
                }.map(_.forall(identity))
                assert(zio)(isTrue)
                }
            }
            ```
        * with aspects
            ```
            test("foreachPar preserves ordering") {
                assert(ZIO.foreachPar(1 to 100)(ZIO.succeed(_)))(equalTo(1 to 100))
                }
            } @@ nonFlaky
            ```
* common test aspects
    * diagnose - do a localized fiber dump if a test times out
    * nonFlaky - run a test repeatedly to make sure it is stable
    * timed - time a test to identify slow tests
    * timeout - time out a test after specified duration
    * tag - tag a test for reporting
        * example: "this test is about database"
* composable
    * test @@ nonFlaky @@ timeout(60.seconds)
    * apply to tests, suites or entire specs
    * order matters
        * repeat(10) @@ timeout(60s)
        * timeout(60s) @@ repeat(10)
* implementing aspects
    * when we need access to test itself
        ```
        new TestAspect.PerTest.AtLeastR[TestEnvironment] {
          override def perTest[R >: Nothing <: TestEnvironment, E >: Nothing <: Any]
          (test: ZIO[R, TestFailure[E], TestSuccess])(implicit trace: Trace): ZIO[R, TestFailure[E], TestSuccess] = for {
            result <- test // here comes the logic and we have handle to test itself
          } yield result
        }
        ```
    * when we want to do something independent of test itself
        * TestAspect.before(zio.Console.printLine("before each"))
        * TestAspect.beforeAll(zio.Console.printLine("before all")) 
        * etc

## property based testing
* example: ZIO test
    ```
    test("encode and decode is an identity") {
        // check operator, one or more generators, assertion
        check(genEvents) { event =>
            assert(decode(encode(event)))(equalTo(event))
        }
    }
    ```
* is an approach where the framework generates test cases
* advantage: allows to quickly test a large number of test cases
    * potentially: reveal not obvious counterexamples
* typically generate ~ 100-200 test cases
    * Int ~2 billion values
    * complex data types => number of possibilities increases exponentially
        * complement property based testing with traditional tests (for particular degenerate cases)
* common mistake: generator is not general enough
    * example: generating user input using ASCII
        * what about: 普通话 ?
* generator represents a distribution of potential values
    * each time we run a property based test we sample values from that distribution
        ```
        final case class Gen[-R, +A](
            sample: ZStream[R, Nothing, Sample[R, A]]
        )

        final case class Sample[-R, +A](
            value: A,
            shrinks: ZStream[R, Nothing, Sample[R, A]]
        )
        ```
* create generators
    * construct generators for each field
    * combine with operators
        * example: flatMap, map, oneOf, zip
    * example
        ```
        val genAccount2: Gen[Any, Account] =
          (Gen.uuid <*> // symbolic alias for zip and zipWith; generate values in parallel
            Gen.fromIterable(AccountStatus.values) <*>
            Gen.string1(Gen.char)
            ).map { case (uuid, status, str) => Account(AccountId(uuid), status, NonEmptyString.unsafeFrom(str))
          }
        ```
* auto-deriving generator
    * example
        ```
        val genAccount: Gen[Any, Account] = DeriveGen[Account] // implicit for each field
        ```
    * lib: https://zio.dev/api/zio/test/magnolia/index.html
* don't use filter - transform instead
    * filtering = "throw away" data that doesn’t satisfy our predicate
    * example
        ```
        val evens: Gen[Random, Int] = ints.map(n => if (n % 2 == 0) n else n + 1) // transformation
        ```
* shrinking
    * counterexample will typically not be the "simplest"
        * test framework tries to shrink failures to ones that
            * are "simpler" (in some sense)
                * example: smaller integers, smaller collections
            * and still violate the property
    * ZIO Test uses "integrated shrinking"
        * every generator already knows how to shrink itself
            * all operators keep this property
            * example: generator of even integers can’t shrink to 1
    * under the hood
        * Sample contains a "tree" of possible "shrinkings" for the value
            * root: original value
        * invariants
            * any given level: value earlier in the stream, must be "smaller" than later values
            * all children must be "smaller" than their parents
        * machinery
            1. generate the first Sample in the shrink stream
            1. test whether its value is also a counterexample to the property being tested
                * counterexample => recurse on that sample
                * not => repeat with the next Sample in shrink stream
            * example: shrinking logic for int
                * first tries to shrink to zero
                * then to half the distance between counterexample and zero
                * then to half that distance, and so on

## seed
* TestRandom service
    * provides a testable implementation of the Random service
    * serves as a purely functional random number generator
        * implementation takes care of passing the updated seed
        * we can set the seed and generate a value based on that seed
            * default seed
                ```
                /**
                 * An arbitrary initial seed for the `TestRandom`.
                 */
                val DefaultData: Data = Data(1071905196, 1911589680)
                ```
            * we could set/get seed using: TestRandom.getSeed / TestRandom.setSeed
