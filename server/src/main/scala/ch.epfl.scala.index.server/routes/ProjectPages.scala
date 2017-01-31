package ch.epfl.scala.index
package server
package routes

import model._
import release._
import data.project.ProjectForm
import model.misc._
import TwirlSupport._

import akka.http.scaladsl._
import model._
import server.Directives._
import Uri._
import StatusCodes._

import scala.concurrent.Future

class ProjectPages(dataRepository: DataRepository, session: GithubUserSession) {
  import session._

  private def canEdit(owner: String, repo: String, userState: Option[UserState]) =
    userState.map(s => s.isAdmin || s.repos.contains(GithubRepo(owner, repo))).getOrElse(false)

  private def editPage(owner: String, repo: String, userState: Option[UserState]) = {
    val user = userState.map(_.user)
    if (canEdit(owner, repo, userState)) {
      for {
        keywords <- dataRepository.keywords()
        project <- dataRepository.project(Project.Reference(owner, repo))
      } yield {
        project.map { p =>
          val allKeywords = (p.keywords ++ keywords.map(_._1).toSet).toList.sorted
          (OK, views.project.html.editproject(p, allKeywords, user))
        }.getOrElse((NotFound, views.html.notfound(user)))
      }
    } else Future.successful((Forbidden, views.html.forbidden(user)))
  }

  private def projectPage(owner: String,
                          repo: String,
                          target: Option[String],
                          artifact: Option[String],
                          version: Option[String],
                          userState: Option[UserState]) = {

    val user = userState.map(_.user)

    val selection = ReleaseSelection.parse(
      target = target,
      artifactName = artifact,
      version = version
    )

    dataRepository
      .projectPage(Project.Reference(owner, repo), selection)
      .map(_.map {
        case (project, options) =>
          import options._
          val twitterCard = for {
            github <- project.github
            description <- github.description
          } yield
            TwitterSummaryCard(
              site = "@scala_lang",
              title = s"${project.organization}/${project.repository}",
              description = description,
              image = github.logo
            )

          (OK,
           views.project.html.project(
             project,
             options.artifacts,
             versions,
             targets,
             release,
             user,
             canEdit(owner, repo, userState),
             twitterCard
           ))
      }.getOrElse((NotFound, views.html.notfound(user))))
  }

  // TODO: The user argument not being used seems suspicious, suggests there may actually be no authentication on the update
  def updateProjectBehavior(organization: String, repository: String, user: Option[UserState], fields: Seq[(String, String)], contributorsWanted: Boolean, keywords: Iterable[String], defaultArtifact: Option[String], defaultStableVersion: Boolean, deprecated: Boolean, artifactDeprecations: Iterable[String], cliArtifacts: Iterable[String], customScalaDoc: Option[String]) = {
    val documentationLinks = getDocumentationLinks(fields)

    onSuccess(
      dataRepository.updateProject(
        Project.Reference(organization, repository),
        ProjectForm(
          contributorsWanted,
          keywords.toSet,
          defaultArtifact,
          defaultStableVersion,
          deprecated,
          artifactDeprecations.toSet,
          cliArtifacts.toSet,
          customScalaDoc,
          documentationLinks
        )
      )
    ) { ret =>
      Thread.sleep(1000) // oh yeah
      redirect(Uri(s"/$organization/$repository"), SeeOther)
    }
  }

  def getEditPageBehavior(organization: String, repository: String, user: Option[UserState]) = {
    complete(editPage(organization, repository, user))
  }

  private def getDocumentationLinks(fields: Seq[(String, String)]) = {
    val documentationLinks = {
      val name = "documentationLinks"
      val end = "]".head

      fields.filter { case (key, _) => key.startsWith(name) }.groupBy {
        case (key, _) => key.drop("documentationLinks[".length).takeWhile(_ != end)
      }.values.map {
        case Vector((a, b), (c, d)) =>
          if (a.contains("label")) (b, d)
          else (d, b)
      }.toList
    }
    documentationLinks
  }

  def legacyArtifactQueryBehavior(organization: String, repository: String, artifact: Option[String], version: Option[String], target: Option[String], user: Option[UserState]) = {
    val rest = (artifact, version) match {
      case (Some(a), Some(v)) => s"$a/$v"
      case (Some(a), None) => a
      case _ => ""
    }
    val targetQuery = target match {
      case Some(t) => s"?target=$t"
      case _ => ""
    }
    if (artifact.isEmpty && version.isEmpty) {
      complete(
        projectPage(organization, repository, target, None, None, user))
    } else {
      redirect(s"/$organization/$repository/$rest$targetQuery",
        StatusCodes.PermanentRedirect)
    }
  }

  def projectPageBehavior(organization: String, repository: String, user: Option[UserState], target: Option[String]) = {
    complete(
      projectPage(organization,
        repository,
        target,
        None,
        None,
        user)
    )
  }

  def artifactPageBehavior(organization: String, repository: String, artifact: String, user: Option[UserState], target: Option[String]) = {
    complete(
      projectPage(organization,
        repository,
        target,
        Some(artifact),
        None,
        user)
    )
  }

  def artifactWithVersionBehavior(organization: String, repository: String, artifact: String, version: String, user: Option[UserState], target: Option[String]) = {
    complete(
      projectPage(organization,
        repository,
        target,
        Some(artifact),
        Some(version),
        user)
    )
  }
}
