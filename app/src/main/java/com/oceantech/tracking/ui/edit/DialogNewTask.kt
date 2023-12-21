package com.oceantech.tracking.ui.edit

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import com.oceantech.tracking.R
import com.oceantech.tracking.databinding.DialogNewTaskBinding
import com.oceantech.tracking.ui.edit.EditFragment.Companion.setupEditTextBehavior

class DialogNewTask(
    remainTypes: List<String>,
    private val listener: OnCallBackListenerClient,
) : DialogFragment() {

    private lateinit var binding: DialogNewTaskBinding
    private val types = remainTypes.toMutableList()
    private var selectedType = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogNewTaskBinding.inflate(layoutInflater)

        //remove the 1st element since it is used for display current types in EditFragment
        types.removeAt(0)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerNewTaskType.adapter = adapter
        binding.spinnerNewTaskType.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedType = position
            }
        }

        listenToChanges()

        binding.cancelNewTask.setOnClickListener { dismiss() }
        binding.confirmNewTask.setOnClickListener {
            val oh =
                with(binding.etNewOH.text.toString()) { if (this.isEmpty()) 0.0 else this.toDouble() }
            val ot =
                with(binding.etNewOT.text.toString()) { if (this.isEmpty()) 0.0 else this.toDouble() }
            listener.notifyAddNewTask(
                oh,
                ot,
                binding.etNewOHContent.text.toString(),
                binding.etNewOTContent.text.toString(),
                types[selectedType]
            )
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setView(binding.root)

        return builder.create()
    }

    private fun listenToChanges() {
        binding.etNewOH.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAddEnable()
                binding.etNewOHContent.isEnabled = !s.isNullOrEmpty()
                binding.tvNewOHTask.text =
                    if (!s.isNullOrEmpty()) getString(R.string.office_task_star) else getString(R.string.office_task)
            }
        })
        binding.etNewOT.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkAddEnable()
                binding.etNewOTContent.isEnabled = !s.isNullOrEmpty()
                binding.tvNewOTTask.text =
                    if (!s.isNullOrEmpty()) getString(R.string.overtime_task_star) else getString(R.string.overtime_task)
            }
        })

        setupEditTextBehavior(binding.etNewOHContent, ::checkAddEnable)
        setupEditTextBehavior(binding.etNewOTContent, ::checkAddEnable)
    }

    private fun checkAddEnable() {
        binding.confirmNewTask.isEnabled =
            (!binding.etNewOH.text.isNullOrEmpty() && !binding.etNewOHContent.text.isNullOrEmpty())
                    || (!binding.etNewOT.text.isNullOrEmpty() && !binding.etNewOTContent.text.isNullOrEmpty())
    }
}