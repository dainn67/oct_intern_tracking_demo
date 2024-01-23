package com.oceantech.tracking.ui.client.homeScreen

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.airbnb.mvrx.*
import com.google.gson.Gson
import com.oceantech.tracking.R
import com.oceantech.tracking.core.TrackingViewModel
import com.oceantech.tracking.data.model.response.DateListResponse
import com.oceantech.tracking.data.model.response.DateObject
import com.oceantech.tracking.data.model.response.Project
import com.oceantech.tracking.data.model.response.Task
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
import okhttp3.RequestBody
import java.util.Calendar
import javax.inject.Inject

class HomeViewModel @AssistedInject constructor(
    @Assisted state: HomeViewState,
    val repository: UserRepository,
) : TrackingViewModel<HomeViewState, HomeViewAction, HomeViewEvent>(state) {
    @Inject
    lateinit var userPref: UserPreferences

    private var selectedCalendar = Calendar.getInstance()
    private var selectedPageIndex = 0
    private var selectedPageSize = 0

    private lateinit var projectList: List<Project>
    private lateinit var projectTypeList: MutableList<String>
    private lateinit var listResponse: DateListResponse
    lateinit var remainTypes: MutableList<String>

    private val mediaType = RemoteDataSource.DEFAULT_CONTENT_TYPE.toMediaTypeOrNull()
    private var accessToken: String? = null
    var language: Int = 1

    override fun handle(action: HomeViewAction) {
        when (action) {
            is HomeViewAction.ResetLang -> handResetLang()
        }
    }

    private fun handResetLang() {
        _viewEvents.post(HomeViewEvent.ResetLanguage)
    }

    @AssistedFactory
    interface Factory {
        fun create(initialState: HomeViewState): HomeViewModel
    }

    companion object : MvRxViewModelFactory<HomeViewModel, HomeViewState> {
        @JvmStatic
        override fun create(
            viewModelContext: ViewModelContext,
            state: HomeViewState
        ): HomeViewModel {
            val factory = when (viewModelContext) {
                is FragmentViewModelContext -> viewModelContext.fragment as? Factory
                is ActivityViewModelContext -> viewModelContext.activity as? Factory
            }
            return factory?.create(state)
                ?: error("You should let your activity/fragment implements Factory interface")
        }

        val listParams = mutableMapOf<String, String>()
    }

    fun initLoad(selectedCalendar: Calendar, pageIndex: Int, pageSize: Int) {
        viewModelScope.launch {
            val job = async { userPref.accessToken.firstOrNull() }
            accessToken = job.await()

            loadList(selectedCalendar, pageIndex, pageSize)
            loadProjectTypes()
        }
    }

    fun loadList(selectedCalendar: Calendar, pageIndex: Int, pageSize: Int) {
        setState { copy(asyncListResponse = Loading()) }

        this.selectedCalendar = selectedCalendar
        this.selectedPageIndex = pageIndex
        this.selectedPageSize = pageSize

        val month = selectedCalendar.get(Calendar.MONTH)
        val year = selectedCalendar.get(Calendar.YEAR)
        val daysInMonth = selectedCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val startDate = "$year-${if (month < 9) "0${month + 1}" else (month + 1)}-01"
        val endDate = "$year-${if (month < 9) "0${month + 1}" else (month + 1)}-$daysInMonth"

        repository.getList(startDate, endDate, pageIndex, pageSize).execute {
            copy(asyncListResponse = it)
        }
    }

    private fun loadProjectTypes() {
        setState { copy(asyncProjectTypes = Loading()) }

        repository.getProjects(1, 1000).execute {
            projectTypeList = mutableListOf()
            it.invoke()?.data?.content?.forEach { it1 ->
                projectTypeList.add(it1.code)
            }

            //get the list of projects if successful
            it.invoke()?.data?.content?.let { it1 ->
                projectList = it1
            }

            copy(asyncProjectTypes = it)
        }
    }

    fun reloadDateObject(date: String, newListResponse: DateListResponse): DateObject {
        listResponse = newListResponse
        for (currentDate in listResponse.data?.content!!) {
            if (currentDate.dateWorking == date)
                return currentDate
        }

        return listResponse.data!!.content!![0]
    }

    fun addNewTask(
        officeHour: Double,
        overTimeHour: Double,
        ohContent: String,
        otContent: String,
        currentDate: DateObject,
        projectCode: String
    ) {
        setState { copy(asyncModify = Loading()) }

        val selectedProject = getProject(projectCode)
        currentDate.tasks?.add(
            Task(
                officeHour = officeHour,
                overtimeHour = overTimeHour,
                taskOffice = ohContent,
                taskOverTime = otContent,
                project = selectedProject
            )
        )

        if (currentDate.id == null) {
            repository.postTask(currentDate).execute {
                if (it.invoke() != null && it.invoke()!!.code == 200) loadList(selectedCalendar, selectedPageIndex, selectedPageSize)
                copy(asyncModify = it)
            }
        } else {
            repository.putTask(currentDate.id, currentDate).execute {
                if (it.invoke() != null && it.invoke()!!.code == 200) loadList(selectedCalendar, selectedPageIndex, selectedPageSize)
                copy(asyncModify = it)
            }
        }

        _viewEvents.post(HomeViewEvent.DataModified)
    }

    fun updateTask(
        currentDate: DateObject,
        position: Int?,
        newTask: Task?,
        isDayOff: Boolean
    ) {
        setState { copy(asyncModify = Loading()) }

        //if position is null -> day off. If position isn't null but newTask is null -> delete. Otherwise -> update
        //update the task locally to send to server
        if (isDayOff) {
            currentDate.tasks?.clear()
            currentDate.dayOff = true
        } else {
            if (position == null)
                currentDate.dayOff = false
            else {
                if (newTask != null) currentDate.tasks?.set(position, newTask)
                else currentDate.tasks?.removeAt(position)
            }
        }
        if (currentDate.id != null) {
            repository.putTask(currentDate.id, currentDate).execute {
                if (it.invoke() != null && it.invoke()!!.code == 200) loadList(selectedCalendar, selectedPageIndex, selectedPageSize)
                copy(asyncModify = it)
            }
        } else {
            repository.postTask(currentDate).execute {
                if (it.invoke() != null && it.invoke()!!.code == 200) loadList(selectedCalendar, selectedPageIndex, selectedPageSize)
                copy(asyncModify = it)
            }
        }
    }

    fun getTaskNumberList(tasks: List<Task>): List<Int> {
        val list = mutableListOf<Int>()
        for (i in 1..tasks.size)
            list.add(i)

        return list
    }

    fun getTotalOfficeHour(tasks: List<Task>?): Double {
        if (tasks.isNullOrEmpty()) return 0.0

        var res = 0.0;
        for (task in tasks) res += task.officeHour
        return res
    }

    fun getTotalOvertimeHour(tasks: List<Task>?): Double {
        if (tasks.isNullOrEmpty()) return 0.0

        var res = 0.0;
        for (task in tasks) res += task.overtimeHour
        return res
    }

    fun updateRemainingTypes(dateObject: DateObject): MutableList<String> {
        remainTypes = projectTypeList.toMutableList()
        if (dateObject.tasks.isNullOrEmpty()) return remainTypes

        for (task in dateObject.tasks)
            if (remainTypes.contains(task.project.code))
                remainTypes.remove(task.project.code)

        return remainTypes
    }

    fun getProject(code: String): Project {
        for (project in projectList)
            if (project.code == code)
                return project
        return projectList[0]
    }

    fun checkEditInput(oh: Double?, ot: Double?, task: Task): Boolean {
        return oh != null && ot == null && (oh > 8 || oh + task.overtimeHour > 24)
                || (ot != null && oh == null && (ot > 24 || ot + task.officeHour > 24))
                || (oh != null && ot != null && oh + ot > 24)
    }

    fun checkNewInput(oh: Double, ot: Double, dateObject: DateObject, context: Context): Boolean {
        if(oh + getTotalOfficeHour(dateObject.tasks) > 8) {
            Toast.makeText(context, context.getString(R.string.invalid_total_hour), Toast.LENGTH_SHORT).show()
            return false
        }
        if(ot + oh + getTotalOvertimeHour(dateObject.tasks) + getTotalOfficeHour(dateObject.tasks) > 24 || ot > 24){
            Toast.makeText(context, context.getString(R.string.total_hour_exceed), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}