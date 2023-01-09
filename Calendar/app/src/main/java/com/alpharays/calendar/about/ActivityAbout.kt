package com.alpharays.calendar.about

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import androidx.recyclerview.widget.LinearLayoutManager
import com.alpharays.calendar.data.AboutAdapter
import com.alpharays.calendar.databinding.ActivityAboutBinding

class ActivityAbout : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding
    private lateinit var mAdapter: AboutAdapter
    private lateinit var infoList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.appInfo.layoutManager = LinearLayoutManager(this)
        infoList = ArrayList()

        val sa = "1. This calendar application saves your email address and can easily login and logout the user with their data safely stored."
        val sb = "2. The user can save events and tasks any time and utilize them when needed"
        val sc = "3. The user can create, update and delete a task anytime and anywhere."
        val sd = "4. Internet Permission is required for this application to work."
        val se = Html.fromHtml( "<strong>LOG IN SCREEN</strong>",Html.FROM_HTML_MODE_COMPACT).toString()
        val sf = "1. While Signing in with Email ID and password, you can click on forgot password to reset your password. Don't forget to check the spam folder."
        val sg = "NOTE: Do not use the same account for both log in with EMAIL ID & PASSWORD and GOOGLE SIGN IN, this can lead to data loss."
        val sh = "In this case, you will need to forgot your password or only login with GOOGLE SIGN IN."
        val si = Html.fromHtml( "Developer Contact: <a>uchihamadara2022g@gmail.com</a>\nContact for any queries",Html.FROM_HTML_MODE_LEGACY).toString()
        val sj = "Don't forget to review the application on Google Play Store"

        infoList.add(sa);
        infoList.add(sb);
        infoList.add(sc);
        infoList.add(sd);
        infoList.add(se);
        infoList.add(sf);
        infoList.add(sg);
        infoList.add(sh);
        infoList.add(si);
        infoList.add(sj);


        mAdapter = AboutAdapter(infoList)
        binding.appInfo.adapter = mAdapter
    }
}