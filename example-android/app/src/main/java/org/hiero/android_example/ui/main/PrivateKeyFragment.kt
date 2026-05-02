// SPDX-License-Identifier: Apache-2.0
package org.hiero.android_example.ui.main

import android.widget.TextView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.os.Bundle
import android.view.View
import org.hiero.android_example.R
import androidx.fragment.app.Fragment
import org.hiero.sdk.PrivateKey

class PrivateKeyFragment : Fragment() {
    private  lateinit var privateKey: TextView
    private lateinit var publicKey: TextView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstance: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_private_key, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        privateKey = view.findViewById(R.id.privateKey)
        publicKey = view.findViewById(R.id.publicKey)
        view.findViewById<View>(R.id.button).setOnClickListener {
            val key = PrivateKey.generateED25519()
            val pKey = key.publicKey
            privateKey.text = key.toString()
            publicKey.text = pKey.toString()
        }
    }
}


