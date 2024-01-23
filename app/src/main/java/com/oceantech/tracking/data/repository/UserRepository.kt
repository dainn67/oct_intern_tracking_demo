package com.oceantech.tracking.data.repository

import com.oceantech.tracking.data.model.request.UserBody
import com.oceantech.tracking.data.model.response.ModifyResponse
import com.oceantech.tracking.data.model.response.CheckTokenResponse
import com.oceantech.tracking.data.model.response.DateListResponse
import com.oceantech.tracking.data.model.response.DateObject
import com.oceantech.tracking.data.model.response.Member
import com.oceantech.tracking.data.model.response.MemberResponse
import com.oceantech.tracking.data.model.response.Project
import com.oceantech.tracking.data.model.response.ProjectResponse
import com.oceantech.tracking.data.model.response.Team
import com.oceantech.tracking.data.model.response.TeamResponse
import com.oceantech.tracking.data.model.response.User
import com.oceantech.tracking.data.model.response.UserResponse
import com.oceantech.tracking.data.network.RemoteDataSource.Companion.AUTH_CONTENT_TYPE
import com.oceantech.tracking.data.network.RemoteDataSource.Companion.CHECK_TOKEN_AUTH
import com.oceantech.tracking.data.network.UserApi
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.RequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    val api: UserApi
) {
    fun getList(
        startDate: String,
        endDate: String,
        pageIndex: Int,
        pageSize: Int,
    ): Observable<DateListResponse> = api.getList(
        startDate, endDate, pageIndex, pageSize
    ).subscribeOn(Schedulers.io())

    fun getTrackingList(
        startDate: String?,
        endDate: String?,
        teamId: String?,
        memberId: String?,
        pageIndex: String?,
        pageSize: String?,
    ): Observable<DateListResponse> = api.getTrackingList(
        startDate, endDate, teamId, memberId, pageIndex, pageSize
    ).subscribeOn(Schedulers.io())

    fun getProjects(pageIndex: Int, pageSize: Int): Observable<ProjectResponse> = api.getProjects(
        pageIndex, pageSize
    ).subscribeOn(Schedulers.io())

    fun addProject(newProject: Project): Observable<ModifyResponse> = api.addProject(
        newProject
    ).subscribeOn(Schedulers.io())

    fun editProject(id: String, project: Project): Observable<ModifyResponse> = api.updateProject(
        id, project
    ).subscribeOn(Schedulers.io())

    fun deleteProject(prjId: String): Observable<ModifyResponse> = api.deleteProject(
        prjId
    ).subscribeOn(Schedulers.io())

    fun getTeams(pageIndex: String, pageSize: String): Observable<TeamResponse> = api.getTeams(
        pageIndex, pageSize
    ).subscribeOn(Schedulers.io())

    fun updateTeam(id: String, team: Team): Observable<ModifyResponse> = api.updateTeam(
        id, team
    ).subscribeOn(Schedulers.io())

    fun getMembers(
        teamId: String?,
        pageIndex: String,
        pageSize: String
    ): Observable<MemberResponse> = api.getMembers(
        teamId, pageIndex, pageSize
    ).subscribeOn(Schedulers.io())

    fun updateMember(id: String, member: Member): Observable<ModifyResponse> = api.updateMember(
        id, member
    ).subscribeOn(Schedulers.io())

    fun getUsers(pageIndex: String, pageSize: String): Observable<UserResponse> = api.getUsers(
        pageIndex, pageSize
    ).subscribeOn(Schedulers.io())

    fun deleteUser(uId: Int) : Observable<ModifyResponse> = api.deleteUser(
        uId
    ).subscribeOn(Schedulers.io())

    fun postTask(
        date: DateObject
    ): Observable<ModifyResponse> = api.postTask(
        date
    ).subscribeOn(Schedulers.io())

    fun putTask(
        dateId: String,
        date: DateObject
    ): Observable<ModifyResponse> = api.putTask(
        dateId, date
    ).subscribeOn(Schedulers.io())

    fun addNewUser(user: UserBody): Observable<ModifyResponse> = api.addNewUser(
        user
    ).subscribeOn(Schedulers.io())
}