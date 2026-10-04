package org.ubaierbhat.android.barcodekeyboard

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class TestFieldsActivity : AppCompatActivity() {

    private lateinit var logView: TextView
    private val entries = mutableListOf<String>()
    private var logField: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_fields)
        logView = findViewById(R.id.action_log)

        val actionTargets = listOf(
            R.id.field_done to "done",
            R.id.field_search to "search",
            R.id.field_next to "next",
            R.id.field_go to "go",
            R.id.field_email to "email",
            R.id.field_phone to "phone",
            R.id.field_number to "number",
            R.id.field_password to "password",
        )
        for ((id, name) in actionTargets) {
            val field = findViewById<EditText>(id)
            keepCaretVisible(field)
            field.setOnEditorActionListener { _, actionId, _ ->
                appendLog("$name:action$actionId")
                true
            }
            field.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    recordFocus(name)
                }
            }
        }
        findViewById<EditText>(R.id.field_multiline).apply {
            keepCaretVisible(this)
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    recordFocus("multiline")
                }
            }
        }
    }

    private fun recordFocus(name: String) {
        if (logField != name) {
            logField = name
            entries.clear()
        }
        appendLog("$name:focus")
    }

    private fun keepCaretVisible(field: EditText) {
        field.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: android.text.Editable?) {
                field.post {
                    field.bringPointIntoView(field.selectionEnd)
                }
            }
        })
    }

    private fun appendLog(entry: String) {
        entries.add(entry)
        while (entries.size > MAX_LOG_ENTRIES) {
            entries.removeAt(0)
        }
        logView.text = entries.joinToString("  ")
    }

    companion object {
        private const val MAX_LOG_ENTRIES = 6
    }
}
