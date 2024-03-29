package com.oceantech.tracking.ui.admin.projects

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.oceantech.tracking.R
import com.oceantech.tracking.data.model.Constants.Companion.PROJECT_STATUS_LIST
import com.oceantech.tracking.data.model.response.Project
import com.oceantech.tracking.databinding.DialogEditProjectBinding
import com.oceantech.tracking.utils.checkWhileListening
import com.oceantech.tracking.utils.setupSpinner

class DialogEditOrAddNewProject(
    private val context: Context,
    private val listener: AdminProjectFragment,
    private val project: Project? = null
) : DialogFragment() {
    private lateinit var binding: DialogEditProjectBinding

    private var initialStatus = ""
    private var selectedStatus = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogEditProjectBinding.inflate(layoutInflater)

        binding.spinnerStatus.setupSpinner( { position ->
            selectedStatus = PROJECT_STATUS_LIST[position]
            if(project != null) checkEnabledEditProject()
        }, PROJECT_STATUS_LIST)

        if (project != null) {
            binding.title.text = getString(R.string.edit_prj)
            binding.etCode.hint = project.code
            binding.etName.hint = project.name
            binding.etDesc.hint = project.description
            for (i in PROJECT_STATUS_LIST.indices)
                if (project.status.equals(PROJECT_STATUS_LIST[i], ignoreCase = true)){
                    selectedStatus = PROJECT_STATUS_LIST[i]
                    initialStatus = PROJECT_STATUS_LIST[i]
                    binding.spinnerStatus.setSelection(i)
                }
            binding.etCode.checkWhileListening (::checkEnabledEditProject)
            binding.etName.checkWhileListening (::checkEnabledEditProject)
            binding.etDesc.checkWhileListening (::checkEnabledEditProject)
        } else {
            binding.title.text = getString(R.string.new_prj)
            binding.etCode.checkWhileListening (::checkEnabledAddNewProject)
            binding.etName.checkWhileListening (::checkEnabledAddNewProject)
        }


        binding.cancel.setOnClickListener { dismiss() }
        binding.confirmAdd.setOnClickListener {
            if(project != null)
                listener.notifyEditProject(
                    project.id!!,
                    if(binding.etCode.text.isNullOrEmpty()) project.code else binding.etCode.text.toString(),
                    if(binding.etName.text.isNullOrEmpty()) project.name else binding.etName.text.toString(),
                    selectedStatus,
                    if(binding.etDesc.text.isNullOrEmpty()) project.description ?: "" else binding.etDesc.text.toString()
                )
            else listener.notifyAddProject(binding.etCode.text.toString(), binding.etName.text.toString(), selectedStatus, binding.etDesc.text.toString())
            dismiss()
        }

        val alertDialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .create()
            .also {
                it.window?.let {it1 ->
                    it1.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
            }

        return alertDialog
    }

    private fun checkEnabledAddNewProject(){
        binding.confirmAdd.isEnabled = !binding.etCode.text.isNullOrBlank() && !binding.etName.text.isNullOrBlank()
    }

    private fun checkEnabledEditProject(){
        binding.confirmAdd.isEnabled =
            !binding.etCode.text.isNullOrBlank()
                || !binding.etName.text.isNullOrBlank()
                || !binding.etDesc.text.isNullOrBlank()
                || initialStatus != selectedStatus
    }
}