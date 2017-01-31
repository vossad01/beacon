package ch.epfl.scala.index.server.routes

import akka.http.scaladsl.model.headers.HttpCredentials
import akka.http.scaladsl.server.Directives.{complete, reject, provide}
import akka.http.scaladsl.server.{Directive1, Route, StandardRoute}
import ch.epfl.scala.index.data.github.GithubCredentials
import ch.epfl.scala.index.model.PageIndex
import ch.epfl.scala.index.server.UserState
import org.joda.time.DateTime

import scala.collection.immutable.Seq

class AlwaysCompleteBehavior extends HttpBehavior {
  override val frontPage: (Option[UserState]) => StandardRoute = _ => complete("")
  override val updateProject: (String, String, Option[UserState], Seq[(String, String)], Boolean, Iterable[String], Option[String], Boolean, Boolean, Iterable[String], Iterable[String], Option[String]) => Route = (_, _, _, _, _, _, _, _, _, _, _, _) => complete("")
  override val editProject: (String, String, Option[UserState]) => StandardRoute = (_, _, _) => complete("")
  override val projectPageArtifactQuery: (String, String, Option[String], Option[String], Option[String], Option[UserState]) => StandardRoute = (_, _, _, _, _, _) => complete("")
  override val projectPage: (String, String, Option[UserState], Option[String]) => StandardRoute = (_, _, _, _) => complete("")
  override val artifactPage: (String, String, String, Option[UserState], Option[String]) => StandardRoute = (_, _, _, _, _) => complete("")
  override val artifactPageWithVersion: (String, String, String, String, Option[UserState], Option[String]) => StandardRoute = (_, _, _, _, _, _) => complete("")
  override val searchResultsPage: (Option[UserState], String, Int, Option[String], Option[String]) => StandardRoute = (_, _, _, _, _) => complete("")
  override val organizationPage: (String, Option[UserState]) => StandardRoute = (_, _) => complete("")
  override val releaseStatus: (String) => StandardRoute = _ => complete("")
  override val publishRelease: (String, DateTime, Boolean, Boolean, Boolean, Iterable[String], Boolean, String, (GithubCredentials, UserState)) => StandardRoute = (_, _, _, _, _, _, _, _, _) => complete("")
  override val projectSearchApi: (String, String, String, Option[String], Option[String], Boolean) => StandardRoute = (_, _, _, _, _, _) => complete("")
  override val releaseInfoApi: (String, String, Option[String], Option[String], Option[String], Option[String], Option[String]) => StandardRoute = (_, _, _, _, _, _, _) => complete("")
  override val autocomplete: (String) => StandardRoute = _ => complete("")
  override val versionBadge: (String, String, String, Option[String], Option[String], Option[String], Option[String], Option[Int]) => Route = (_, _, _, _, _, _, _, _) => complete("")
  override val countBadge: (String, Option[String], Option[String], Option[String], Option[PageIndex], String) => Route = (_, _, _, _, _, _) => complete("")
  override val oAuth2routes: Route = reject()
  override val credentialsTransformation: (Option[HttpCredentials]) => Directive1[(GithubCredentials, UserState)] = c => provide[(GithubCredentials, UserState)]((null, null))

}
