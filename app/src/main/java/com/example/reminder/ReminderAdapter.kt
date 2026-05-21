package com.example.reminder

import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SwitchCompat
import java.util.Date
import java.util.Locale

class ReminderAdapter(
    private val reminders: MutableList<Note>,
    private val onSwitchToggle: (Note, Boolean) -> Unit,
    private val onItemClick: (Note) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    class ReminderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.tv_time)
        val titleTextView: TextView = view.findViewById(R.id.ed_title)
        val reminderSwitch: SwitchCompat = view.findViewById(R.id.reminderSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.reminder_item, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]

        // 時間（24 小時制 HH:mm）與標題分開顯示
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        holder.timeTextView.text = sdf.format(Date(reminder.time))
        holder.titleTextView.text = reminder.title.ifBlank { "（無標題）" }

        holder.reminderSwitch.setOnCheckedChangeListener(null)
        holder.reminderSwitch.isChecked = reminder.isNotificationEnabled
        holder.reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            reminder.isNotificationEnabled = isChecked
            onSwitchToggle(reminder, isChecked)
        }

        holder.itemView.setOnClickListener { onItemClick(reminder) }
    }

    override fun getItemCount(): Int = reminders.size

    fun updateReminders(newReminders: List<Note>) {
        reminders.clear()
        reminders.addAll(newReminders)
        notifyDataSetChanged()
    }
}
