package com.sapuseven.ya2fa

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.ya2fa.TokenCalculator.DEFAULT_ALGORITHM
import com.sapuseven.ya2fa.TokenCalculator.TOTP_DEFAULT_DIGITS
import com.sapuseven.ya2fa.TokenCalculator.TOTP_DEFAULT_PERIOD
import com.sapuseven.ya2fa.TokenCalculator.TOTP_RFC6238
import org.apache.commons.codec.binary.Base32

class EntryListAdapter(private val list: ArrayList<TokenEntry>) :
    RecyclerView.Adapter<EntryListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // create a new view
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvCode.text = Tools.formatToken(
            TOTP_RFC6238(
                Base32().decode(item.secret),
                TOTP_DEFAULT_PERIOD,
                TOTP_DEFAULT_DIGITS,
                DEFAULT_ALGORITHM
            ), 3
        )
        holder.tvSite.text = item.label
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        val tvSite: TextView = itemView.findViewById(R.id.tvSite)
    }
}
