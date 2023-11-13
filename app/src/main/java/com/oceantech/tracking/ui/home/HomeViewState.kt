package com.oceantech.tracking.ui.home

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import com.oceantech.tracking.data.model.response.DateListResponse
import com.oceantech.tracking.data.model.response.ModifyTaskResponse
import com.oceantech.tracking.data.model.response.Project
import com.oceantech.tracking.data.model.response.ProjectTypeResponse

data class HomeViewState(
    val asyncListResponse: Async<DateListResponse> = Uninitialized,
    val projects: Async<List<Project>> = Uninitialized,
    val asyncProjectTypes: Async<ProjectTypeResponse> = Uninitialized,

    val asyncModify: Async<ModifyTaskResponse> = Uninitialized

) : MvRxState {
//    fun isLoading() = asyncListResponse is Loading || asyncProjectTypes is Loading
}