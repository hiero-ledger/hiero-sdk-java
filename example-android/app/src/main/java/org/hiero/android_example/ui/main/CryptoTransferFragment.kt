// SPDX-License-Identifier: Apache-2.0
package org.hiero.android_example.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import org.hiero.android_example.R
import org.hiero.sdk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.math.BigDecimal
import java.util.concurrent.TimeoutException


class CryptoTransferFragment : Fragment() {
    private lateinit var recipientAccountId: EditText
    private lateinit var amountToSend: EditText
    private lateinit var button: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var resultText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_crypto_transfer, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recipientAccountId = view.findViewById(R.id.recipientAccountId)
        amountToSend = view.findViewById(R.id.amountToSend)
        button = view.findViewById(R.id.button)
        progressBar = view.findViewById(R.id.progressBar)
        resultText = view.findViewById(R.id.transferResult)
        recipientAccountId.setText("0.0.3333333")
        amountToSend.setText("0.1")
        button.setOnClickListener {

            lifecycleScope.executeAsyncTask(
                onPreExecute = {
                    resultText.text = ""
                    button.visibility = View.INVISIBLE
                    progressBar.visibility = View.VISIBLE
                }, doInBackground = {
                    try {
                        val operatorId =
                            AccountId.fromString(view.resources.getString(R.string.operator_id))
                        val operatorKey =
                            PrivateKey.fromString(view.resources.getString(R.string.operator_key))
                        val client = Client.forTestnet().setOperator(operatorId, operatorKey)
                        val recipientId = AccountId.fromString(recipientAccountId.text.toString())
                        val amount = Hbar(BigDecimal(amountToSend.text.toString()).toLong())
                        val transactionResponse = TransferTransaction()
                            .addHbarTransfer(operatorId, amount.negated())
                            .addHbarTransfer(recipientId, amount)
                            .setTransactionMemo("transfer test")
                            .execute(client)
                         transactionResponse.transactionId.toString()
                    } catch (e: TimeoutException) {
                        "Error: " + e.message
                    } catch (e: PrecheckStatusException) {
                        "Error: " + e.message
                    } catch (e: Exception) {
                        "Error: " + e.message
                    }
                }, onPostExecute = {
                    resultText.text = it
                    button.visibility = View.VISIBLE
                    progressBar.visibility = View.INVISIBLE
                })
        }
    }

    private fun <R> CoroutineScope.executeAsyncTask(
        onPreExecute: () -> Unit,
        doInBackground: () -> R,
        onPostExecute: (R) -> Unit
    ) = launch {
        onPreExecute() // runs in Main Thread
        val result = withContext(Dispatchers.IO) {
            doInBackground() // runs in background thread without blocking the Main Thread
        }
        onPostExecute(result) // runs in Main Thread
    }
}



