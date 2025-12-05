package com.example.stepcounter.ui.alarm

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stepcounter.data.Alarm
import com.example.stepcounter.databinding.AlarmListBinding

class AlarmAdapter(
    private val clickListener: (Alarm) -> Unit,
    private val switchClickListener: (Alarm, Boolean) -> Unit
) : ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder>(DiffCallBack) {


    class AlarmViewHolder(
        val binding: AlarmListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(alarm: Alarm) {
            binding.alarm = alarm
            binding.executePendingBindings()
        }
    }

    companion object DiffCallBack : DiffUtil.ItemCallback<Alarm>() {

        override fun areItemsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Alarm, newItem: Alarm): Boolean {
            return oldItem == newItem
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return AlarmViewHolder(AlarmListBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        val alarm = getItem(position)
        holder.binding.switchAlarmEnabled.setOnCheckedChangeListener(null)
        holder.bind(alarm)
        holder.binding.switchAlarmEnabled.isChecked = alarm.isEnabled

        holder.itemView.setOnClickListener {
            clickListener(alarm)
        }

        holder.binding.switchAlarmEnabled.setOnCheckedChangeListener { _, isChecked ->
            switchClickListener(alarm, isChecked)
        }
    }
}