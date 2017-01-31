package ch.epfl.scala.index
package server
package routes

import views.search.html._

import model.misc.SearchParams

import TwirlSupport._

import akka.http.scaladsl._
import model._
import server._
import server.Directives._
import Uri._

class SearchPages(dataRepository: DataRepository, session: GithubUserSession) {

  import session.executionContext

  private def search(params: SearchParams, user: Option[UserState], uri: String) = {
    import dataRepository._
    complete(
      for {
        (pagination, projects) <- find(params)
        keywords <- keywords(params)
        targetTypes <- targetTypes(params)
        scalaVersions <- scalaVersions(params)
        scalaJsVersions <- scalaJsVersions(params)
        scalaNativeVersions <- scalaNativeVersions(params)
      } yield {
        searchresult(
          params,
          uri,
          pagination,
          projects,
          user.map(_.user),
          params.userRepos.nonEmpty,
          keywords,
          targetTypes,
          scalaVersions,
          scalaJsVersions,
          scalaNativeVersions
        )
      }
    )
  }

  def searchParams(user: Option[UserState]): Directive1[SearchParams] =
    parameters(
      ('q ? "*",
       'page.as[Int] ? 1,
       'sort.?,
       'keywords.*,
       'targetTypes.*,
       'scalaVersions.*,
       'scalaJsVersions.*,
       'scalaNativeVersions.*,
       'you.?)).tmap {
      case (q,
            page,
            sort,
            keywords,
            targetTypes,
            scalaVersions,
            scalaJsVersions,
            scalaNativeVersions,
            you) =>
        val userRepos = you.flatMap(_ => user.map(_.repos)).getOrElse(Set())
        SearchParams(
          q,
          page,
          sort,
          userRepos,
          keywords = keywords.toList,
          targetTypes = targetTypes.toList,
          scalaVersions = scalaVersions.toList,
          scalaJsVersions = scalaJsVersions.toList,
          scalaNativeVersions = scalaNativeVersions.toList
        )
    }

  private val searchPath = "search"

  def searchPageBehavior(user: Option[UserState],
                         query: String,
                         page: Int,
                         sorting: Option[String],
                         you: Option[String]): Route = {
    searchParams(user) { params =>
      search(params, user, searchPath)
    }
  }

  def organizationBehavior(organization: String, user: Option[UserState]): Route = {
    searchParams(user) { params =>
      search(
        params.copy(queryString = s"${params.queryString} AND organization:$organization"),
        user,
        organization
      )
    }
  }
}
