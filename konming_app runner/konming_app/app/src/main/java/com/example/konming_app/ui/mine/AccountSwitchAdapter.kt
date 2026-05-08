package com.example.konming_app.ui.mine

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.konming_app.R

class AccountSwitchAdapter(
    private val currentUsername: String,
    private val onSwitchClick: (String, String) -> Unit,
    private val onRemoveClick: (String) -> Unit
) : RecyclerView.Adapter<AccountSwitchAdapter.ViewHolder>() {

    private val accounts = mutableListOf<Pair<String, String>>()

    fun submitList(newAccounts: List<Pair<String, String>>) {
        accounts.clear()
        accounts.addAll(newAccounts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_account, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(accounts[position])
    }

    override fun getItemCount(): Int = accounts.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        private val tvUsername: TextView = itemView.findViewById(R.id.tv_username)
        private val tvCurrent: TextView = itemView.findViewById(R.id.tv_current)
        private val btnSwitch: Button = itemView.findViewById(R.id.btn_switch)

        fun bind(account: Pair<String, String>) {
            val username = account.first
            val password = account.second
            tvUsername.text = username
            tvCurrent.visibility = if (username == currentUsername) View.VISIBLE else View.GONE
            btnSwitch.visibility = if (username == currentUsername) View.GONE else View.VISIBLE

            btnSwitch.setOnClickListener {
                onSwitchClick(username, password)
            }

            itemView.setOnLongClickListener {
                if (username != currentUsername) {
                    onRemoveClick(username)
                }
                true
            }
        }
    }
}
