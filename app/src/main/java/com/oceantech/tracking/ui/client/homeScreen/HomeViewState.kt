package com.oceantech.tracking.ui.client.homeScreen

import com.airbnb.mvrx.Async
import com.airbnb.mvrx.Fail
import com.airbnb.mvrx.Loading
import com.airbnb.mvrx.MvRxState
import com.airbnb.mvrx.Uninitialized
import com.oceantech.tracking.data.model.response.DateListResponse
import com.oceantech.tracking.data.model.response.ModifyResponse
import com.oceantech.tracking.data.model.response.Project
import com.oceantech.tracking.data.model.response.ProjectResponse

data class HomeViewState(
    val asyncListResponse: Async<DateListResponse> = Uninitialized,
    val projects: Async<List<Project>> = Uninitialized,
    val asyncProjectTypes: Async<ProjectResponse> = Uninitialized,

    val asyncModify: Async<ModifyResponse> = Uninitialized

) : MvRxState {
    fun isLoading() =
        asyncListResponse is Loading
                || asyncProjectTypes is Loading
                || projects is Loading
                || asyncModify is Loading

    fun isFailed() =
        asyncListResponse is Fail
                || asyncProjectTypes is Fail
                || projects is Fail
                || asyncModify is Fail
}