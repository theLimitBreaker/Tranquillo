package com.pervysage.thelimitbreaker.foco.actvities

import android.app.Activity
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.pervysage.thelimitbreaker.foco.R
import com.pervysage.thelimitbreaker.foco.adapters.MyContactsAdapter
import com.pervysage.thelimitbreaker.foco.database.Repository
import com.pervysage.thelimitbreaker.foco.database.entities.ContactInfo
import com.pervysage.thelimitbreaker.foco.dialogs.ContactInfoDialog
import com.pervysage.thelimitbreaker.foco.services.ContactSyncIntentService
import kotlinx.android.synthetic.main.activity_my_contacts.*
import java.util.*
import kotlin.collections.ArrayList

class MyContactsActivity : AppCompatActivity() {

    private var areContactEmpty = true

    private fun toggleViews() {
        if (areContactEmpty) {
            ivContactHeader.visibility = View.VISIBLE
            tvNoContacts.visibility = View.VISIBLE
            rvMyContacts.visibility = View.GONE
        } else {
            ivContactHeader.visibility = View.GONE
            tvNoContacts.visibility = View.GONE
            rvMyContacts.visibility = View.VISIBLE
        }
    }


    private lateinit var repo: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_contacts)

        repo = Repository.getInstance(application)
        val contactAdapter = MyContactsAdapter(ArrayList(),this)
        rvMyContacts.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvMyContacts.adapter = contactAdapter

        val contacts = repo.getMyContacts()
        contacts.observe(this, Observer<List<ContactInfo>> {
            areContactEmpty = it!!.isEmpty()
            contactAdapter.updateList(it)
            toggleViews()

        })
        contactAdapter.setOnContactClickListener { name, numbers, colors ->
            val list = contacts.value
            val dialog = ContactInfoDialog()
            dialog.setParams(name, numbers, colors)
            dialog.setOnContactDeleteListener {
                for (contact in list!!) {
                    if (name == contact.name) {
                        repo.deleteContact(contact)
                    }
                }
                dialog.dismiss()
            }
            dialog.show(supportFragmentManager, "ContactInfoDialog")
        }
        ivAddContact.setOnClickListener {
            startActivity(Intent(this@MyContactsActivity, PickContactsActivity::class.java))
        }

        ivBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        startService(Intent(this, ContactSyncIntentService::class.java))
    }

}