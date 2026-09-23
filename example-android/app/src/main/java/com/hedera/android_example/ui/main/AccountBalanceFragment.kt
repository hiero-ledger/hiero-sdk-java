// SPDX-License-Identifier: Apache-2.0
package com.hedera.android_example.ui.main

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
import com.hedera.android_example.R
import com.hedera.hashgraph.sdk.AccountId
import com.hedera.hashgraph.sdk.AccountInfoQuery
import com.hedera.hashgraph.sdk.Client
import com.hedera.hashgraph.sdk.PrecheckStatusException
import com.hedera.hashgraph.sdk.PrivateKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.util.concurrent.TimeoutException

class AccountBalanceFragment : Fragment() {
    private lateinit var accountBalance: TextView
    private lateinit var accountId: EditText
    private lateinit var button: Button
    private lateinit var progressBar: ProgressBar
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstance: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_account_balance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        accountBalance = view.findViewById(R.id.accountBalance)
        accountId = view.findViewById(R.id.accountId)
        button = view.findViewById(R.id.button)
        progressBar = view.findViewById(R.id.progressBar)
        val operatorId = AccountId.fromString(view.resources.getString(R.string.operator_id))
        accountId.setText(operatorId.toString())
        button.setOnClickListener {

            lifecycleScope.executeAsyncTask(
                onPreExecute = {
                    accountBalance.text = ""
                    button.visibility = View.INVISIBLE
                    progressBar.visibility = View.VISIBLE
                }, doInBackground = {
                    try {
                        val id = AccountId.fromString(accountId.text.toString())
                        val operatorKey =
                            PrivateKey.fromString(view.resources.getString(R.string.operator_key))
                        val client = Client.forTestnet().setOperator(operatorId, operatorKey)
                        val balance = AccountInfoQuery()
                            .setAccountId(id)
                            .execute(client).balance
                        balance.toString()
                    } catch (e: TimeoutException) {
                        "Error: " + e.message
                    } catch (e: PrecheckStatusException) {
                        "Error: " + e.message
                    } catch (e: Exception) {
                        "Error: " + e.message
                    }
                }, onPostExecute = {
                    accountBalance.text = it
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
