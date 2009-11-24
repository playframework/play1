package org.scalatest.tools

import org.scalatest._
import org.scalatest.events._
import org.scalatest.tools._
import play.test.TestEngine.{TestResults, TestResult}

object ScalaTestRunner {

    def run(suiteClass: Class[Suite]) = {
        val suite = suiteClass.newInstance
        val reporter = new PlayReporter
        val dispatch = new DispatchReporter(List(reporter), System.out)
        val runner = new SuiteRunner(suite, dispatch, NoStop, new Filter(None, Set[String]()), Map[String,Any](), None, new Tracker(new Ordinal(1)))
        runner.run()
        dispatch.dispatchDisposeAndWaitUntilDone()
        reporter.results
    }

}

class PlayReporter extends Reporter {

    val results = new TestResults

    def apply(event : Event) {
        event match {

            case TestSucceeded(ordinal, suiteName, suiteClassName, testName, duration, formatter, rerunnable, payload, threadName, timeStamp) =>
                val result = new TestResult
                result.name = testName
                result.time = duration.getOrElse(0)
                results add result

            case TestPending(ordinal, suiteName, suiteClassName, testName, formatter, payload, threadName, timeStamp) =>
                val result = new TestResult
                result.name = testName
                result.time = -1
                results add result

            case TestFailed(ordinal, message, suiteName, suiteClassName, testName, throwable, duration, formatter, rerunnable, payload, threadName, timeStamp) => 
                val result = new TestResult
                result.name = testName
                result.time = duration.getOrElse(0)
                result.error = message
                result.passed = false
                results add result
                results.passed = false

            case _ => 

        }
    }

}

object NoStop extends Stopper {

    override def apply = false

}