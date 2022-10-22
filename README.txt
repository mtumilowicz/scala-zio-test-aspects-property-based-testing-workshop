# scala-zio-test-aspects-property-based-testing-workshop

* references
  * [What's Cooking in ZIO Test by Adam Fraser](https://www.youtube.com/watch?v=JtfdcxgQ71E)
  * [Using Aspects To Transform Your Code With ZIO Environment](https://www.youtube.com/watch?v=gcqWdNwNEPg)

## zio test
* Test Services
    * testable versions of all standard services
    * handle complex concepts such as passage of time
    * example
        ```
        test("timeout") {
            for {
                fiber <- ZIO.sleep(5.minutes).timeout(1.minute).fork
                _ <- TestClock.adjust(1.minute)
                result <- fiber.join
            } yield assert(result)(isNone)
        }
        ```
* ZIO Test is a next generation testing library for functional Scala
    * tests as first class values
        * compose tests, retry tests, do everything you do with zio effects
    * interruptibility
    * resource safety
    * environment type
    * property based testing
## test aspects
* example
    ```
    test("concurrency test") {
        ???
    } @@ nonFlaky @@ timeout(60.seconds)
    ```
* seamlessly control how tests are executed
* apply to tests, suites or entire specs
* wide variety for different use cases
* common test aspects
    * diagnose - do a localized fiber dump if a test times out
    * nonFlaky - run a test repeatedly to make sure it is stable
    * timed - time a test to identify slow tests
    * timeout - time out a test after specified duration
    * tag - tag a test for reporting
        * example: "this test is about database"
* test aspects compose
    * test @@ nonFlaky @@ timeout(60.seconds)
* aspects in caliban
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
    * caliban supports a concept of aspects, called wrappers, that
    allow modifying query parsing, validation and execution
* aspect oriented programming
    * in any domain there are cross cutting concerns that are shared among different parts of our main program logic
    * often these concerns are tangled with each part of our main program logic and scattered across different parts
    * we want to increase the modularity of our programs by separating these concerns from our main program logic
    * cross cutting concerns are typically related to how we do something rather than what we are doing
        * what level of authorization should this transfer require?
        * how should this transfer be logged?
        * how should this transfer be recorded to our database
    * example: testing
        * there are a variety of concerns of how we run tests that are ditinct from the tests themselves
            * how many times should we run a test?
            * what environments should we run the test on?
            * what sample size should we use for property based tests?
            * what degree of parallelism?
            * what timeout to use?
    * example: graphql
        * main program logic is queries
        * there are a varierty of concerns of how we run queries that are distinct from the queries themselves
            * what is the maximum depth of nested queries we should support
            * what is the meximum number of fields we should support
            * what timeout should we use?
            * how should we handle slow queries?
            * what kind of tracing and caching should we use?
* example
    ```
    test("foreachPar preserves ordering") {
        val zio = ZIO.foreach(1 to 100) { _ =>
            ZIO.foreachPar(1 to 100)(ZIO.succeed(_)).map(_ == (1 to 100))
        }.map(_.forall(identity))
        assert(zio)(isTrue)
        }
    }
    ```
    * it easy to tangle questions of how with our main program logic of what
    * how many times will we scatter code like this across our main program logic?
    * separating concerns
        ```
        test("foreachPar preserves ordering") {
            assert(ZIO.foreachPar(1 to 100)(ZIO.succeed(_)))(equalTo(1 to 100))
            }
        } @@ nonFlaky
        ```
* metaprogramming
    * traditional approaches to aspect oriented programming
    * AspectJ
    * relies on implementation details such as class and method names that may change
    * no longer able to statically type check if code is dynamically generated
* aspects are polymorphic functions from an effect type to the same effect type
    * potentially constraining the environment or widening the error type
    * transforms the how but not the what
    * example
        ```
        trait Aspect[-R, +E] {
            def apply[R1 <: R, E1 >: E, A](zio: ZIO[R1, E1, A]): ZIO[R1, E1, A]
        }
        ```
* composing
    ```
    implicit final class AspectSyntax[-R, +E, +A)(private val zio: ZIO[R, E, A]) {
        def @@[R1 <: R, E1 >: E](aspect: Aspect[R1, E1]): ZIO[R1, E1, A] =
            aspect(zio)
    }
    ```
* order matters
    * repeat(10) @@ timeout(60s) vs timeout(60s) @@ repeat(10)
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

## seed