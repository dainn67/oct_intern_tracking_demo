package com.oceantech.tracking.ui.admin

import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.*
import com.oceantech.tracking.core.TrackingViewModel
import com.oceantech.tracking.data.model.Constants.Companion.FEMALE
import com.oceantech.tracking.data.model.Constants.Companion.LGBT
import com.oceantech.tracking.data.model.Constants.Companion.MALE
import com.oceantech.tracking.data.model.Constants.Companion.OTHER_GENDER
import com.oceantech.tracking.data.model.request.UserBody
import com.oceantech.tracking.data.model.response.Member
import com.oceantech.tracking.data.model.response.Project
import com.oceantech.tracking.data.model.response.Task
import com.oceantech.tracking.data.model.response.Team
import com.oceantech.tracking.data.network.RemoteDataSource
import com.oceantech.tracking.data.repository.UserRepository
import com.oceantech.tracking.ui.security.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class AdminViewModel @AssistedInject constructor(
    @Assisted state: AdminViewState,
    val repository: UserRepository,
) : TrackingViewModel<AdminViewState, AdminViewAction, AdminViewEvent>(state) {
    @Inject
    lateinit var userPref: UserPreferences

    var teamList: List<Team>? = null

    private lateinit var projectList: List<Project>
    private lateinit var projectTypeList: MutableList<String>

    private val mediaType = RemoteDataSource.DEFAULT_CONTENT_TYPE.toMediaTypeOrNull()
    private var accessToken: String? = null
    var language: Int = 1

    override fun handle(action: AdminViewAction) {
        when (action) {
            is AdminViewAction.ResetLang -> handResetLang()
        }
    }

    private fun handResetLang() {
        _viewEvents.post(AdminViewEvent.ResetLanguage)
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: AdminViewState): AdminViewModel
    }

    companion object : MvRxViewModelFactory<AdminViewModel, AdminViewState> {
        @JvmStatic
        override fun create(
            viewModelContext: ViewModelContext,
            state: AdminViewState
        ): AdminViewModel {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state)
                ?: error("You should let your activity/fragment implements Factory interface")
        }
    }

    fun initLoad() {
        viewModelScope.launch {
            val job = async { userPref.accessToken.firstOrNull() }
            accessToken = job.await()

            val startCalendar = Calendar.getInstance()
            startCalendar.set(Calendar.DAY_OF_MONTH, 1)
            val endCalendar = Calendar.getInstance()
            endCalendar.set(
                Calendar.DAY_OF_MONTH,
                endCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            )

            reloadTracking(startCalendar, endCalendar) //load default tracking list
            loadProjectTypes()
            loadTeams()
            loadMembers()
        }
    }

    private fun loadTrackingList(
        startDate: String,
        endDate: String,
        teamId: String?,
        memberId: String?,
        pageIndex: Int,
        pageSize: Int
    ) {
        setState { copy(asyncListResponse = Loading()) }

        repository.getTrackingList(
            startDate, endDate, teamId, memberId, pageIndex.toString(), pageSize.toString()
        ).execute {
            copy(asyncListResponse = it)
        }
    }

    fun loadProjectTypes(pageIndex: Int = 1, pageSize: Int = 10) {
        setState { copy(asyncProjectsResponse = Loading()) }

        repository.getProjects(pageIndex, pageSize)
            .execute {
                projectTypeList = mutableListOf()

                it.invoke()?.data?.content?.forEach { it1 ->
                    projectTypeList.add(it1.code)
                }

                //get the list of projects if successful
                it.invoke()?.data?.content?.let { it1 ->
                    projectList = it1
                }

                copy(asyncProjectsResponse = it)
            }
    }

    fun addProject(code: String, name: String, status: String, desc: String) {
        setState { copy(asyncModify = Loading()) }

        val newProject = Project(
            name = name,
            code = code,
            status = status.toUpperCase(Locale.ROOT),
            description = desc,
            tasks = null
        )
        repository.addProject(newProject).execute {
            _viewEvents.post(AdminViewEvent.DataModified)
            copy(asyncModify = it)
        }

    }

    fun editProject(id: String, code: String, name: String, status: String, desc: String) {
        setState { copy(asyncModify = Loading()) }

        val newProject = Project(id, name, code, status.toUpperCase(Locale.ROOT), desc, null)
        repository.editProject(
            id,
            newProject
        ).execute {
            _viewEvents.post(AdminViewEvent.DataModified)
            copy(asyncModify = it)
        }

    }

    fun deleteProject(id: String) {
        setState { copy(asyncModify = Loading()) }

        repository.deleteProject(id).execute {
            _viewEvents.post(AdminViewEvent.DataModified)
            copy(asyncModify = it)
        }

    }

    fun loadTeams(pageIndex: Int = 1, pageSize: Int = 1000) {
        setState { copy(asyncTeamResponse = Loading()) }

        repository.getTeams(pageIndex.toString(), pageSize.toString())
            .execute {
                if (pageSize == 1000) teamList = asyncTeamResponse.invoke()?.data?.content
                copy(asyncTeamResponse = it)
            }
    }

    fun editTeam(
        id: String,
        name: String,
        code: String,
        desc: String
    ) {
        setState { copy(asyncModify = Loading()) }

        val editedTeam = Team(name = name, code = code, description = desc)
        repository.updateTeam(id, editedTeam).execute {
            _viewEvents.post(AdminViewEvent.DataModified)
            copy(asyncModify = it)
        }
    }

    fun loadMembers(teamId: String? = null, pageIndex: Int = 1, pageSize: Int = 1000) {
        setState { copy(asyncMemberResponse = Loading()) }

        repository.getMembers(
            teamId,
            pageIndex.toString(),
            pageSize.toString()
        ).execute {
            if (!teamId.isNullOrEmpty() && !teamList.isNullOrEmpty()) {
                for (team in teamList!!) {
                    if (team.id == teamId) team.members =
                        asyncMemberResponse.invoke()?.data?.content
                    break
                }
            }
            copy(asyncMemberResponse = it)
        }
    }

    fun editMember(
        id: String,
        code: String,
        dateJoin: String,
        email: String,
        gender: String,
        level: String,
        name: String,
        position: String,
        status: String,
        team: Team,
        type: String
    ) {
        val newMember = Member(
            name = name,
            code = code,
            email = email,
            dateJoin = dateJoin,
            gender = gender,
            level = level,
            position = position,
            status = status,
            team = team,
            type = type
        )
        setState { copy(asyncModify = Loading()) }

        repository.updateMember(id, newMember).execute {
            _viewEvents.post(AdminViewEvent.DataModified)
            copy(asyncModify = it)
        }
    }

    fun loadUsers(pageIndex: Int = 1, pageSize: Int = 10) {
        setState { copy(asyncUserResponse = Loading()) }

        repository.getUsers(pageIndex.toString(), pageSize.toString())
            .execute {
                copy(asyncUserResponse = it)
            }
    }

    fun deleteUser(uId: Int) {
        setState { copy(asyncModify = Loading()) }

        repository.deleteUser(uId).execute {
            _viewEvents.post(AdminViewEvent.DataModified)
            copy(asyncModify = it)
        }
    }

    fun addNewUser(
        username: String,
        email: String,
        gender: String,
        roles: List<String>,
        password: String
    ) {
        val genderCode = when (gender) {
            MALE -> 1
            FEMALE -> 2
            LGBT -> 3
            OTHER_GENDER -> 4
            else -> 1
        }
        val newUser = UserBody(
            username = username,
            email = email,
            gender = genderCode,
            roles = roles,
            password = password,
            confirmPassword = password
        )
        setState { copy(asyncModify = Loading()) }
        repository.addNewUser(newUser).execute {
            _viewEvents.post(AdminViewEvent.DataModified)
            copy(asyncModify = it)
        }
    }

    fun reloadTracking(
        fromDate: Calendar,
        toDate: Calendar,
        teamId: String? = null,
        memberId: String? = null,
        pageIndex: Int = 1,
        pageSize: Int = 10
    ) {
        var year = fromDate.get(Calendar.YEAR)
        var month = fromDate.get(Calendar.MONTH)
        var day = fromDate.get(Calendar.DAY_OF_MONTH)
        val fromDateString =
            "$year-${if (month < 9) "0${month + 1}" else month + 1}-${if (day < 10) "0$day" else day}"

        year = toDate.get(Calendar.YEAR)
        month = toDate.get(Calendar.MONTH)
        day = toDate.get(Calendar.DAY_OF_MONTH)
        val toDateString =
            "$year-${if (month < 9) "0${month + 1}" else month + 1}-${if (day < 10) "0$day" else day}"

        loadTrackingList(fromDateString, toDateString, teamId, memberId, pageIndex, pageSize)
        if (teamId != null) loadMembers(teamId)
    }

    fun getTotalHour(tasks: List<Task>?): String {
        if (tasks.isNullOrEmpty()) return 0.0.toString()

        var res = 0.0;
        for (task in tasks) res += task.officeHour + task.overtimeHour
        return res.toString()
    }
}