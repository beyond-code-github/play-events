/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.events.monitoring

import org.scalatest.{Matchers, WordSpec}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.events.handlers.EventHandler
import uk.gov.hmrc.play.http.{HttpException, Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.play.events.AlertLevel._

import scala.concurrent.{Await, Future}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration._

class HttpMonitorSpec extends WordSpec with MockitoSugar with Matchers {

  implicit val hc = new HeaderCarrier()

  "HttpMonitor" should {

    "generate Alert and Monitor events for 500 error with no alert code" in new HttpMonitor {
      override def source: String = "This-Test"

      val mockHandler = mock[EventHandler]

      override def eventHandlers = Set(mockHandler)

      val response = new Upstream5xxResponse("Error Msg", 500, 60)

      intercept[Upstream5xxResponse] {
        Await.result(

          monitor() {
            Future(throw response)
          },

          200 millis
        )

        verify(mockHandler).handle(DefaultHttp500ErrorEvent(source, response, None))
      }

    }
    "generate Alert and Monitor events for 500 error with alert code" in new HttpMonitor {
      override def source: String = "This-Test"

      val mockHandler = mock[EventHandler]

      override def eventHandlers = Set(mockHandler)

      val response = new Upstream5xxResponse("Error Msg", 500, 60)

      intercept[Upstream5xxResponse] {
        Await.result(

          monitor(Some("test-code")) {
            Future(throw response)
          },

          200 millis
        )

        verify(mockHandler).handle(DefaultHttp500ErrorEvent(source, response, Some("test-code")))
      }

    }

    "generate Alert and Monitor events for 400 error with alert code" in new HttpMonitor {

      override def source: String = "This-Test"

      val mockHandler = mock[EventHandler]

      override def eventHandlers = Set(mockHandler)

      val response = new Upstream4xxResponse("Error Msg", 403, 60)

      intercept[Upstream4xxResponse] {
        Await.result(

          monitor(Some("test-code")) {
            Future(throw response)
          },

          200 millis
        )

        verify(mockHandler).handle(DefaultHttp400ErrorEvent(source, response, Some("test-code")))
      }
    }

    "generate Alert and Monitor events for 400 error with NO alert code" in new HttpMonitor {

      override def source: String = "This-Test"

      val mockHandler = mock[EventHandler]

      override def eventHandlers = Set(mockHandler)

      val response = new Upstream4xxResponse("Error Msg", 403, 60)

      intercept[Upstream4xxResponse] {
        Await.result(

          monitor() {
            Future(throw response)
          },

          200 millis
        )

        verify(mockHandler).handle(DefaultHttp400ErrorEvent(source, response, None))
      }
    }

    "generate Alert and Monitor events for http-exceptions HttpException" in new HttpMonitor {

      override def source: String = "This-Test"

      val mockHandler = mock[EventHandler]

      override def eventHandlers = Set(mockHandler)

      val exception4XX = new HttpException("Error Msg", 400)
      val event4XX = DefaultHttpExceptionEvent(source, exception4XX, None)

      event4XX.level should be (MAJOR)

      intercept[HttpException] {
        Await.result(

          monitor() {
            Future(throw exception4XX)
          },

          200 millis
        )

        verify(mockHandler).handle(event4XX)
      }

      val exception5XX = new HttpException("Error Msg", 502)
      val event5XX = DefaultHttpExceptionEvent(source, exception5XX, None)

      event5XX.level should be (CRITICAL)

      intercept[HttpException] {
        Await.result(

          monitor() {
            Future(throw exception5XX)
          },

          200 millis
        )

        verify(mockHandler).handle(event5XX)
      }
    }

    "generate Alert and Monitor events for http-exceptions HttpException with alert code" in new HttpMonitor {

      override def source: String = "This-Test"

      val mockHandler = mock[EventHandler]

      override def eventHandlers = Set(mockHandler)

      val exception4XX = new HttpException("Error Msg", 400)
      val event4XX = DefaultHttpExceptionEvent(source, exception4XX, Some("test-code"))

      event4XX.level should be (MAJOR)

      intercept[HttpException] {
        Await.result(

          monitor(Some("test-code")) {
            Future(throw exception4XX)
          },

          200 millis
        )

        verify(mockHandler).handle(event4XX)
      }

      val exception5XX = new HttpException("Error Msg", 502)
      val event5XX = DefaultHttpExceptionEvent(source, exception5XX, Some("test-code-2"))

      event5XX.level should be (CRITICAL)

      intercept[HttpException] {
        Await.result(

          monitor(Some("test-code-2")) {
            Future(throw exception5XX)
          },

          200 millis
        )

        verify(mockHandler).handle(event5XX)
      }
    }

    "not generate Alert and Monitor events for successful response" in new HttpMonitor {

      override def source: String = "This-Test"

      val mockHandler = mock[EventHandler]

      override def eventHandlers = Set(mockHandler)

      Await.result(

        monitor() {
          Future("Hello")
        },

        200 millis
      )

      verifyZeroInteractions(mockHandler)
    }

  }

}