package com.oceantech.tracking.ui.client.tasksInteractionScreen

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.mvrx.Success
import com.airbnb.mvrx.activityViewModel
import com.airbnb.mvrx.withState
import com.google.gson.Gson
import com.oceantech.tracking.R
import com.oceantech.tracking.core.TrackingBaseFragment
import com.oceantech.tracking.data.model.response.DateObject
import com.oceantech.tracking.databinding.FragmentTaskInteractionBinding
import com.oceantech.tracking.databinding.ItemTaskNumberBinding
import com.oceantech.tracking.ui.client.homeScreen.HomeViewEvent
import com.oceantech.tracking.ui.client.homeScreen.HomeViewModel
import com.oceantech.tracking.utils.checkWhileListening
import com.oceantech.tracking.utils.setupSpinner

class TaskInteractionFragment : TrackingBaseFragment<FragmentTaskInteractionBinding>() {
    private val viewModel: HomeViewModel by activityViewModel()
    private val args: TaskInteractionFragmentArgs by navArgs()

    private val gson = Gson()

    private var selectedTaskId = 0
    private var selectedTypeId = 0
    private lateinit var dateObject: DateObject

    private lateinit var dialog: DialogFragment

    override fun getBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentTaskInteractionBinding {
        return FragmentTaskInteractionBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //decode current date
        dateObject = gson.fromJson(args.dateWorking, DateObject::class.java)
        requireActivity().findViewById<TextView>(R.id.title).text = dateObject.dateWorking

        loadScreen()
        listenToEditTextChanges()

        //buttons
        with(views.switchDayOff) {
            this.setOnClickListener {
                viewModel.updateTask(dateObject, null, null, this.isChecked)
            }
        }
        views.addTask.setOnClickListener {
            dialog = DialogNewTask(viewModel.remainTypes, this, requireContext())
            dialog.show(requireActivity().supportFragmentManager, "new_task")
        }
        views.deleteTask.setOnClickListener {
            dialog = DialogConfirmDeleteTask(requireContext(), this)
            dialog.show(requireActivity().supportFragmentManager, "confirm_action")
        }
        views.saveTask.setOnClickListener {
            handleSave()
        }

        viewModel.observeViewEvents {
            handleEvent(it)
        }
    }

    private fun handleEvent(it: HomeViewEvent){
        when (it){
            is HomeViewEvent.DataModified -> {

            }
        }
    }

    private fun loadScreen() {
        viewModel.updateRemainingTypes(dateObject)

        setUpTotalHours()

        if (dateObject.dayOff == true) {
            views.switchDayOff.isChecked = true
            views.addTask.isEnabled = false
            views.currentTask.visibility = View.GONE
            views.rvTaskNumber.visibility = View.GONE
        } else if (dateObject.tasks.isNullOrEmpty()) {
            views.addTask.isEnabled = true
            views.rvTaskNumber.visibility = View.GONE
            views.currentTask.visibility = View.GONE
        } else {
            views.addTask.isEnabled = true
            views.rvTaskNumber.visibility = View.VISIBLE
            views.currentTask.visibility = View.VISIBLE

            selectedTaskId = 0
            selectedTypeId = 0
            updateTaskNumberList()
            resetCurrentTask()
        }
    }

    fun notifyFromViewHolder() {
        with(dateObject.tasks!![selectedTaskId]) {
            viewModel.remainTypes.removeAt(0)
            viewModel.remainTypes.add(0, this.project.code)
            updateSpinner()

            views.etOH.text = null
            views.etOT.text = null
            views.etOHContent.text = null
            views.etOTContent.text = null

            views.etOH.hint = this.officeHour.toString()
            views.etOT.hint = this.overtimeHour.toString()
            views.etOHContent.hint = this.taskOffice
            views.etOTContent.hint = this.taskOverTime
        }
    }

    fun notifyAddNewTask(oh: Double,ot: Double,ohContent: String,otContent: String,prjId: String) {
        if (viewModel.checkNewInput(oh, ot, dateObject, requireContext())) {
            viewModel.addNewTask(oh, ot, ohContent, otContent, dateObject, prjId)
            dialog.dismiss()
        }
    }

    fun notifyDeleteTask() {
        viewModel.updateTask(dateObject, selectedTaskId, null, false)
    }

    private fun handleSave() {
        val oh =
            if (!views.etOH.text.isNullOrEmpty()) views.etOH.text.toString().toDouble() else null
        val ot =
            if (!views.etOT.text.isNullOrEmpty()) views.etOT.text.toString().toDouble() else null
        if (viewModel.checkEditInput(oh, ot, dateObject.tasks!![selectedTaskId])) {
            Toast.makeText(requireContext(), getString(R.string.invalid_input), Toast.LENGTH_SHORT)
                .show()
            return
        }

        val newTask = dateObject.tasks!![selectedTaskId].copy(
            overtimeHour = if (views.etOT.text.isNullOrEmpty()) dateObject.tasks!![selectedTaskId].overtimeHour else ot!!,
            officeHour = if (views.etOH.text.isNullOrEmpty()) dateObject.tasks!![selectedTaskId].officeHour else oh!!,
            taskOffice = if (views.etOHContent.text.isNullOrEmpty()) dateObject.tasks!![selectedTaskId].taskOffice else views.etOHContent.text.toString(),
            taskOverTime = if (views.etOTContent.text.isNullOrEmpty()) dateObject.tasks!![selectedTaskId].taskOverTime else views.etOTContent.text.toString(),
            project = viewModel.getProject(viewModel.remainTypes[selectedTypeId])
        )

        viewModel.updateTask(dateObject, selectedTaskId, newTask, false)
    }

    private fun setUpTotalHours() {
        if (dateObject.dayOff == true || dateObject.tasks.isNullOrEmpty()) {
            views.totalOH.text = "0"
            views.totalOT.text = "0"
        } else {
            views.totalOH.text = viewModel.getTotalOfficeHour(dateObject.tasks!!).toString()
            views.totalOT.text = viewModel.getTotalOvertimeHour(dateObject.tasks!!).toString()
        }
    }

    private fun updateTaskNumberList() {
        val numberList = viewModel.getTaskNumberList(dateObject.tasks!!)
        views.rvTaskNumber.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        views.rvTaskNumber.adapter =
            TaskNumberAdapter(requireContext(), numberList, this@TaskInteractionFragment)
    }

    private fun resetCurrentTask() {
        if (!dateObject.tasks.isNullOrEmpty()) {
            with(dateObject.tasks!![0]) {
                viewModel.remainTypes.add(0, this.project.code)
                views.etOH.hint = this.officeHour.toString()
                views.etOT.hint = this.overtimeHour.toString()

                views.etOHContent.hint = this.taskOffice
                views.etOTContent.hint = this.taskOverTime

                views.etOHContent.isEnabled = this.officeHour != 0.0
                views.etOTContent.isEnabled = this.overtimeHour != 0.0
            }
            updateSpinner()
        }
    }

    private fun listenToEditTextChanges() {
        views.etOT.checkWhileListening(::checkEnableSave)
        views.etOH.checkWhileListening(::checkEnableSave)
        views.etOHContent.checkWhileListening(::checkEnableSave)
        views.etOTContent.checkWhileListening(::checkEnableSave)
    }

    private fun updateSpinner() {
        views.currentTaskType.setupSpinner( { position ->
            selectedTypeId = position
            checkEnableSave()
        }, viewModel.remainTypes)
    }

    private fun checkEnableSave() {
        views.saveTask.isEnabled = selectedTypeId != 0
                || with(views.etOH) { !this.text.isNullOrEmpty() && this.text != this.hint }
                || with(views.etOT) { !this.text.isNullOrEmpty() && this.text != this.hint }
                || with(views.etOHContent) { !this.text.isNullOrEmpty() && this.text != this.hint }
                || with(views.etOTContent) { !this.text.isNullOrEmpty() && this.text != this.hint }
    }

    private fun clearAllFocuses() {
        views.etOH.clearFocus()
        views.etOH.text = null
        views.etOT.clearFocus()
        views.etOT.text = null
        views.etOHContent.clearFocus()
        views.etOHContent.text = null
        views.etOTContent.clearFocus()
        views.etOTContent.text = null
    }

    override fun invalidate(): Unit = withState(viewModel) {
        when (it.asyncListResponse) {
            is Success -> {
                views.waitingView.visibility = View.GONE

                dateObject = viewModel.reloadDateObject(
                    dateObject.dateWorking,
                    it.asyncListResponse.invoke()
                )
                loadScreen()
                clearAllFocuses()
            }
        }
    }

    inner class TaskNumberAdapter(
        private val context: Context,
        private val list: List<Int>,
        private val listener: TaskInteractionFragment
    ) : RecyclerView.Adapter<TaskNumberAdapter.TaskNumberViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskNumberViewHolder {
            val binding =
                ItemTaskNumberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return TaskNumberViewHolder(binding, listener)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: TaskNumberViewHolder, position: Int) {
            holder.bind(list[position], position)
        }

        inner class TaskNumberViewHolder(
            private val binding: ItemTaskNumberBinding,
            private val listener: TaskInteractionFragment
        ) : RecyclerView.ViewHolder(binding.root) {
            @SuppressLint("SetTextI18n")
            fun bind(number: Int, position: Int) {
                binding.tvNumber.text = "${getString(R.string.task)} $number"
                if (selectedTaskId == position) {
                    binding.tvNumber.setTextColor(ContextCompat.getColor(context, R.color.blue))
                    binding.tvNumber.setTypeface(null, Typeface.BOLD)
                } else {
                    binding.tvNumber.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.text_color
                        )
                    )
                    binding.tvNumber.setTypeface(null, Typeface.NORMAL)
                }

                itemView.setOnClickListener {
                    selectedTaskId = position
                    listener.notifyFromViewHolder()
                    notifyDataSetChanged()
                }
            }
        }
    }
}
