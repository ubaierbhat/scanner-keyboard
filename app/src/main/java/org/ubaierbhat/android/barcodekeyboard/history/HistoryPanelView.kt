package org.ubaierbhat.android.barcodekeyboard.history

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.ubaierbhat.android.barcodekeyboard.R
import org.ubaierbhat.android.barcodekeyboard.keyboard.KeyView

class HistoryPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val listView: RecyclerView
    private val emptyView: TextView
    private val adapter = EntryAdapter { entry -> onEntrySelected?.invoke(entry) }

    private var onEntrySelected: ((String) -> Unit)? = null
    private var onClose: (() -> Unit)? = null
    private var onClear: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.history_panel_view, this)
        listView = findViewById(R.id.history_list)
        emptyView = findViewById(R.id.history_empty)
        listView.layoutManager = LinearLayoutManager(context)
        listView.adapter = adapter
        findViewById<KeyView>(R.id.history_clear_key).onPress = { onClear?.invoke() }
        findViewById<KeyView>(R.id.history_close_key).onPress = { onClose?.invoke() }
    }

    fun setCallbacks(
        onEntrySelected: (String) -> Unit,
        onClose: () -> Unit,
        onClear: () -> Unit,
    ) {
        this.onEntrySelected = onEntrySelected
        this.onClose = onClose
        this.onClear = onClear
    }

    fun refresh(entries: List<String>) {
        adapter.submitList(entries)
        emptyView.visibility = if (entries.isEmpty()) VISIBLE else GONE
        listView.visibility = if (entries.isEmpty()) GONE else VISIBLE
    }

    private class EntryAdapter(
        private val onClick: (String) -> Unit,
    ) : ListAdapter<String, EntryViewHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.history_entry_item, parent, false) as TextView
            return EntryViewHolder(view)
        }

        override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
            val entry = getItem(position)
            holder.textView.text = entry
            holder.textView.contentDescription = entry
            holder.textView.setOnClickListener { onClick(entry) }
        }
    }

    private class EntryViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    private companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean =
                oldItem == newItem
        }
    }
}
