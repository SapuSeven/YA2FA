package com.sapuseven.ya2fa.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sapuseven.ya2fa.R
import com.sapuseven.ya2fa.data.Token
import com.sapuseven.ya2fa.utils.TokenCalculator
import com.sapuseven.ya2fa.utils.TokenCalculator.TOTP_DEFAULT_PERIOD
import com.sapuseven.ya2fa.utils.TokenCalculator.TOTP_RFC6238
import com.sapuseven.ya2fa.utils.Tools
import org.apache.commons.codec.binary.Base32

class TokenListAdapter(
    private val tokens: List<Token>,
    private val onItemClickListener: View.OnClickListener,
    private val onItemLongClickListener: View.OnLongClickListener
) :
    RecyclerView.Adapter<TokenListAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_entry, parent, false)
        view.setOnClickListener(onItemClickListener)
        view.setOnLongClickListener(onItemLongClickListener)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = tokens[position]
        holder.tvCode.text = Tools.formatToken(
            TOTP_RFC6238(
                Base32().decode(item.secret),
                item.period ?: TOTP_DEFAULT_PERIOD,
                item.length,
                TokenCalculator.HashAlgorithm.valueOf(item.algorithm)
            ), 3
        )

        holder.tvLabel.text =
            if (item.issuer?.isNotBlank() == true) "${item.issuer} (${item.label})" else item.label
    }

    override fun getItemCount(): Int = tokens.size

    fun getItemAt(position: Int): Token = tokens[position]

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCode: TextView = itemView.findViewById(R.id.tvCode)
        val tvLabel: TextView = itemView.findViewById(R.id.tvLabel)
    }
}
