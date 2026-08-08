package com.example.propertyconsultancy.ui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.propertyconsultancy.R
import com.example.propertyconsultancy.data.dto.CategoryOptionDTO
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.util.Log
import android.text.InputType

class SelectionDialogFragment(
    private val title: String,
    private val options: List<CategoryOptionDTO>,
    private val initialSelectedId: Int? = null,
    private val isMultiSelect: Boolean = false,
    private val initialSelectedIds: List<Int>? = null,
    private val onSelected: (String) -> Unit = {},
    private val onMultiSelected: ((List<Int>, String) -> Unit)? = null
) : DialogFragment() {

    private var selectedOption: CategoryOptionDTO? = null
    private val selectedIds = mutableSetOf<Int>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        view.findViewById<TextView>(R.id.tvDialogTitle).text = title
        val rv = view.findViewById<RecyclerView>(R.id.rvOptions)
        val tilExtraData = view.findViewById<TextInputLayout>(R.id.tilExtraData)
        val etExtraData = view.findViewById<TextInputEditText>(R.id.etExtraData)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)

        if (isMultiSelect) {
            btnConfirm.visibility = View.VISIBLE
            btnConfirm.text = "Apply"
            initialSelectedIds?.let { selectedIds.addAll(it) }
        }

        rv.layoutManager = GridLayoutManager(requireContext(), 3)
        val adapter = OptionsAdapter(options, initialSelectedId, isMultiSelect, selectedIds) { option ->
            if (isMultiSelect) {
                if (selectedIds.contains(option.categoryId)) {
                    selectedIds.remove(option.categoryId)
                } else {
                    selectedIds.add(option.categoryId)
                }
            } else {
                if (option.hasValue == 1) {
                    selectedOption = option
                    rv.visibility = View.GONE
                    tilExtraData.visibility = View.VISIBLE
                    tilExtraData.hint = option.hasCaption ?: "Enter details"
                    setEtInputType(etExtraData, option.hasType)
                    btnConfirm.visibility = View.VISIBLE
                    etExtraData.requestFocus()
                } else {
                    onSelected(option.option)
                    dismiss()
                }
            }
        }
        rv.adapter = adapter

        btnConfirm.setOnClickListener {
            if (isMultiSelect) {
                val selectedOptions = options.filter { selectedIds.contains(it.categoryId) }
                val summary = if (selectedOptions.isEmpty()) "All" else selectedOptions.joinToString(", ") { it.option }
                onMultiSelected?.invoke(selectedIds.toList(), summary)
                dismiss()
            } else {
                val extraText = etExtraData.text.toString()
                if (extraText.isNotEmpty()) {
                    onSelected("${selectedOption?.option} - $extraText")
                    dismiss()
                } else {
                    etExtraData.error = "Please enter details"
                }
            }
        }

        view.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setEtInputType(et: TextInputEditText, type: String?) {
        et.inputType = when (type?.lowercase()) {
            "number" -> InputType.TYPE_CLASS_NUMBER
            "decimal" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            "date" -> InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE
            "phone" -> InputType.TYPE_CLASS_PHONE
            else -> InputType.TYPE_CLASS_TEXT
        }
    }

    private class OptionsAdapter(
        private val list: List<CategoryOptionDTO>,
        private val initialSelectedId: Int?,
        private val isMultiSelect: Boolean,
        private val selectedIds: Set<Int>,
        private val onClick: (CategoryOptionDTO) -> Unit
    ) : RecyclerView.Adapter<OptionsAdapter.VH>() {
        
        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tv: TextView = view.findViewById(R.id.tvOption)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_selection_option, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.tv.text = item.option
            
            val isSelected = if (isMultiSelect) {
                selectedIds.contains(item.categoryId)
            } else {
                initialSelectedId != null && item.categoryId == initialSelectedId
            }

            if (isSelected) {
                holder.tv.setBackgroundResource(R.drawable.bg_selection_option_selected)
            } else if (!isMultiSelect && item.hasValue == 1) {
                holder.tv.setBackgroundResource(R.drawable.bg_selection_option_highlight)
            } else {
                holder.tv.setBackgroundResource(R.drawable.bg_selection_option)
            }
            
            holder.itemView.setOnClickListener { 
                onClick(item)
                if (isMultiSelect) notifyItemChanged(position)
            }
        }

        override fun getItemCount() = list.size
    }
}
