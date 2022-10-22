# scala-zio-test-aspects-property-based-testing-workshop

* references
  * [What's Cooking in ZIO Test by Adam Fraser](https://www.youtube.com/watch?v=JtfdcxgQ71E)
  * [Using Aspects To Transform Your Code With ZIO Environment](https://www.youtube.com/watch?v=gcqWdNwNEPg)
  * [Zymposium — Smart Assertions](https://www.youtube.com/watch?v=lgCb4-4M-fw)
  * [ZIO TEST - The Ultimate Guide by Marcin Krykowski](https://www.youtube.com/watch?v=rYUU2AtwKLU)
  * [100 with ZIO Test by Adam Fraser: Scala in the City Conference](https://www.youtube.com/watch?v=qDFfVinjDPQ)
  * [ZIO Test - What, Why and How? - Functional World #4](https://www.youtube.com/watch?v=HqV2R5_VbCw)

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