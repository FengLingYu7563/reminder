package com.example.reminder

import android.graphics.Typeface
import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SwitchCompat
import java.sql.Date
import java.util.Locale

class ReminderAdapter(
    private val reminders: MutableList<Note>,
    private val onSwitchToggle: (Note, Boolean) -> Unit,
    private val onItemClick: (Note) -> Unit
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    class ReminderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.ed_title)
        val reminderSwitch: SwitchCompat  = view.findViewById(R.id.reminderSwitch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.reminder_item, parent, false)
        return ReminderViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        val reminder = reminders[position]

        // 格式化時間
        val sdf = SimpleDateFormat("HH:mm a", Locale.getDefault())
        val formattedTime = sdf.format(Date(reminder.time))

        // 時間跟標題串接
        val truncatedTitle = reminder.title.take(10) // 獲取最 10 個字元的標題
        val displayText = "$formattedTime \t\t\t\t\t\t $truncatedTitle"

        holder.titleTextView.text = displayText

        // 修改字體粗度
        holder.titleTextView.setTypeface(null, Typeface.BOLD)

        holder.reminderSwitch.setOnCheckedChangeListener(null)
        holder.reminderSwitch.isChecked = reminder.isNotificationEnabled

        holder.reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            reminder.isNotificationEnabled = isChecked
            onSwitchToggle(reminder, isChecked)
        }

        holder.itemView.setOnClickListener {
            onItemClick(reminder)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(timestamp))
    }

    override fun getItemCount(): Int = reminders.size

    fun updateReminders(newReminders: List<Note>) {
        reminders.clear()
        reminders.addAll(newReminders)
        notifyDataSetChanged()
    }
}