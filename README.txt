# scala-zio-test-aspects-property-based-testing-workshop

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

## property based testing
* example
    ```
    test("encode and decode is an identity") {
        check(genEvents) { event =>
            assert(decode(encode(event)))(equalTo(event))
        }
    }
    ```
* support for random and deterministic property based testing
* integrated shrinking
* Property based testing is an approach to testing where the framework generates test cases for us instead of having to come up with test cases ourselves.
* The obvious advantage of property based testing is that it allows us to quickly test a large number of test cases, potentially revealing counterexamples that might not have been obvious.
* Property based tests typically only generate one hundred to two hundred test cases. In contrast, even a single Int can take on more than a billion different values.
* If we are generating more complex data types the number of possibilities increases exponentially
    * So in most real world applications of property based testing are only testing a very small portion of the sample space.
    * if counterexamples require multiple generated values to take on very specific values then we may not generate an appropriate counterexample even though such a counterexample does exist
        * A solution to this is to complement property based testing with traditional tests for particular degenerate cases identified by developers.
* A good generator should also be general enough to generate test cases covering the full range of values over which we expect the property to hold.
    * For example, a common mistake would be to test a form that validates user input with a generator of ASCII characters.
    * This is probably very natural for many of us to do, but what happens if the user input is in Mandarin?
* In ZIO Test, a property based test always has three parts:
    * A check operator
        * This tells ZIO Test that we are performing a property based test
    * One of more Gen values
        * You can think of each Gen as representing a distribution of potential values and each time we run a property based test we sample values from that distribution.
        * Generators As Streams Of Samples
            ```
            final case class Gen[-R, +A](
                sample: ZStream[R, Nothing, Sample[R, A]]
            )

            final case class Sample[-R, +A](
                value: A,
                shrinks: ZStream[R, Nothing, Sample[R, A]]
            )
            ```
        * For example, we could imagine a generator that generates values by opening a local file with test data, reading the contents of that file into memory, and each time generating a value based on one of the lines in that file.
    * assertion
* https://zio.dev/api/zio/test/magnolia/index.html
    * traitDeriveGen[A] extends AnyRef
      A DeriveGen[A] can derive a generator of A values.
* To create generators for a data type we will generally follow a two step process.
    * First, construct generators for each part of the data type.
    * Second, combine these generators using operators on Gen such as flatMap, map,
      and oneOf to build a generator for your data type out of these simpler generators.
      ```
      final case class Stock(ticker: String, price: Double, currency: Currency)

      lazy val genStock: Gen[Any, Stock] = for {
          ticker   <- genTicker
          price    <- genPrice
          currency <- genCurrency
      } yield Stock(ticker, price, currency)
      ```
      * One potential inefficiency you may have noticed in some of the examples above is that the flatMap operator requires us to run our generators sequentially, because the second generator we use can depend on the value generated by the first generator
        * ZIO Test supports this through the zipWith and zip operators and their symbolic alias <&>
        * These will generate the two values in parallel and then combine them into a tuple or using the specified function.
* This illustrates a helpful principle for working with generators, which is to prefer transforming generators instead of filtering generators.
    ```
    val ints: Gen[Random, Int] = Gen.int(1, 100)
    val evens: Gen[Random, Int] = ints.map(n => if (n % 2 == 0) n else n + 1)
    ```
    * We will see below that we can also filter the values produced by generators, but this has a cost because we have to “throw away” all of the generated data that
    doesn’t satisfy our predicate
* The either operator is helpful for when we want to generate data for sum types that can be one type or another, such as the Currency data type above.
    ```
    def genTry[R <: Random, A](gen: Gen[R, A]): Gen[R, Try[A]] = Gen.either(Gen.throwable, gen).map {
    case Left(e) => Failure(e)
    case Right(a) => Success(a) }
    ```
    * The first is the oneOf operator, which picks from one of the specified generators with equal probability
    * The second is the elements operator, which is like oneOf but just samples from one of a collection of concrete values instead of from one of a collection of generators
        ```
        sealed trait Currency
        case object USD extends Currency
        case object EUR extends Currency case object JPY extends Currency

        val genCurrency: Gen[Random, Currency] = Gen.elements(JPY, USD, EUR)
        ```
* For example, if we wanted to generate a pair of integers we could do it like this:
  val pairs: Gen[Random, (Int, Int)] = Gen.int <*> Gen.int
* Random And Deterministic Generators
    * Traditionally in property based testing there has been a distinction between random and deterministic property based testing.
    * In random property based testing, values are generated using a pseudorandom number generator based on some initial seed.
    * The disadvantage of property based testing is that it is impossible for us to ever prove a property with random property based testing, we can merely fail to falsify it.
* Samples And Shrinking
    * Typically when we run property based tests the values will be generated randomly and so when we find a counterexample to a property it will typically not be the
      “simplest” counterexample.
    * To help us it is useful if the test framework tries to shrink failures to ones that are “simpler” in some sense and still violate the property.
        * First, we want to generate values that are “smaller” than the original value in some sense. This could mean closer to zero in terms of numbers or closer to zero size in terms of collections.
    * Instead of doing this ZIO Test uses a technique called integrated shrinking where every generator already knows how to shrink itself and all operators on generators also appropriately combine the shrinking logic of the original generators.
        * So a generator of even integers can’t possibly shrink to anything other than an even integer because it is built that way.
    * In addition to a value a Sample also contains a “tree” of possible “shrinkings” of that value. It may not be obvious from the signature but ZStream[R, Nothing, Sample[A]] represents a tree.
        * The root of the tree is the original value.
        * The next level of the tree consists of all the values for the samples in the shrink collection.
            * Each of these values in turn may have its own children, represented by its own shrink tree.
        * The shrink tree must obey the following invariants.
            * First, within any given level, values to the “left”, that is earlier in the stream, must be “smaller” than values that are later in the stream.
            * Second, all children of a value in the tree must be “smaller” than their parents.
            * We begin by generating the first Sample in the shrink stream and testing whether its value is also a counterexample to the property being tested.
                * If it is a valid counterexample, we recurse on that sample. If it is not, we repeat the process with the next Sample in the original shrink stream.
        * For example, the default shrinking logic for integral values first tries to shrink to zero, then to half the distance between the value and zero, then to half that distance, and so on. At each level we repeat the same logic.
    * Sample itself has its own operators such as map and flatMap for combining Sample values.
        * The map/flatMap operator conceptually transforms both the value of the sample as well as all of its potential shrinkings with the specified function
        * example
            * map to transform a generator to generate only even integers we are guaranteed that the shrinkings will also contain only even integers.

## seed
* TestRandom service provides a testable implementation of the Random service.
    * works in two modes
        * serves as a purely functional random number generator
            * We can set the seed and generate a value based on that seed
            * The implementation takes care of passing the updated seed through to the next call of the random number generator so we don’t have to deal with it ourselves.
        * second mode, the TestRandom service can be used where we can “feed” it values of a particular type and then subsequent calls to generate values of that type will return the data we fed to i
* in TestRandom
    ```
    /**
     * An arbitrary initial seed for the `TestRandom`.
     */
    val DefaultData: Data = Data(1071905196, 1911589680)
    ```
* we could set/get seed using: TestRandom.getSeed / TestRandom.setSeed
