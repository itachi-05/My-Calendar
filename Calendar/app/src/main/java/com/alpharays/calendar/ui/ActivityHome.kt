package com.alpharays.calendar.ui


import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.*
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.alpharays.calendar.SplashScreenVM
import com.alpharays.calendar.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.time.LocalDate
import java.util.*
import kotlin.system.exitProcess
import com.alpharays.calendar.R
import com.alpharays.calendar.about.ActivityAbout
import com.alpharays.calendar.data.TasksAdapter
import com.alpharays.calendar.data.UserTask
import java.text.SimpleDateFormat


class ActivityHome : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var builderLogOut: AlertDialog.Builder
    private val viewModel: SplashScreenVM by viewModels()
    private lateinit var sharedPref: SharedPreferences
    private var email: String = ""
    private var displayName: String = ""
    private lateinit var databaseReference: DatabaseReference
    private var myDialog: Dialog? = null
    private var aStr = "Task Name can not be empty"
    private var bStr = "Wrong Start Time entered"
    private var cStr = "Wrong End Time entered"
    private var dStr = "Task Venue can not be empty"
    private var finalTaskName = ""
    private var finalTaskStartTime = ""
    private var finalTaskEndTime = ""
    private var finalTaskLocation = ""
    private lateinit var tasksAdapter: TasksAdapter
    private lateinit var tasksModelArrayList: ArrayList<UserTask>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            setKeepOnScreenCondition {
                viewModel.isLoading.value
            }
        }
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set the activity to be in portrait mode
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if(binding.drawerLayout.isDrawerOpen(GravityCompat.START)){
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                }
                else{
                    finishAffinity()
                    exitProcess(0)
                }
            }
        })

        auth = FirebaseAuth.getInstance()
        myDialog = Dialog(this)
        binding.addTaskBtn.setOnClickListener {
            addingTask()
        }
        drawerFunction()
        val today = LocalDate.now().toString()
        tasksToday(today)
        recyclerViewData()
    }


    private fun drawerFunction() {
        sharedPref = getSharedPreferences("sharingUSER", MODE_PRIVATE)
        email = sharedPref.getString("email", "Email").toString()
        displayName = sharedPref.getString("displayName", "User").toString()
        val drawerLayout = binding.drawerLayout
        binding.navBarButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
            val navView = binding.navView
            findViewById<TextView>(R.id.navHeaderUserName).text = displayName
            findViewById<TextView>(R.id.navHeaderUserEmail).text = email
            toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
            drawerLayout.addDrawerListener(toggle)
            toggle.isDrawerSlideAnimationEnabled = true
            toggle.syncState()
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
            navView.setNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.Log_out -> {
                        confirmingLogOut()
                    }
                    R.id.About -> {
                        startActivity(Intent(this,ActivityAbout::class.java))
                    }
                }
                true
            }
        }
    }

    private fun confirmingLogOut() {
        builderLogOut = AlertDialog.Builder(this)
        builderLogOut.setTitle(getString(R.string.alert_message))
            .setMessage("Do you wish to Log Out?")
            .setCancelable(true)
            .setPositiveButton("Yes") { _, _ ->   // dialogInterface, it
                logout()
            }
            .setNegativeButton("No") { dialogInterface, _ ->     // dialogInterface, it
                dialogInterface.cancel()
            }
            .show()
    }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, ActivitySignIn::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        finish()
        startActivity(intent)
    }

    @SuppressLint("CutPasteId")
    private fun addingTask() {
        myDialog?.setContentView(R.layout.popup_window)
        myDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        myDialog?.findViewById<ImageView>(R.id.dialogClose)?.setOnClickListener {
            myDialog?.dismiss()
        }

        // ##########################################  collecting data from the dialog box   ##########################################
        val myCalendar = Calendar.getInstance()
        val datePicker = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            myCalendar.set(Calendar.YEAR, year)
            myCalendar.set(Calendar.MONTH, month)
            myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val format = "yyyy-MM-dd"
            val sdf = SimpleDateFormat(format, Locale.UK)
            myDialog?.findViewById<TextView>(R.id.event_date)?.text = sdf.format(myCalendar.time)
        }

        val taskDatePicker = myDialog?.findViewById<Button>(R.id.task_datePicker_btn)
        taskDatePicker?.setOnClickListener {
            DatePickerDialog(
                this,
                datePicker,
                myCalendar.get(Calendar.YEAR),
                myCalendar.get(Calendar.MONTH),
                myCalendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        val taskSubmitBtn = myDialog?.findViewById<Button>(R.id.task_submit_btn)
        taskSubmitBtn?.setOnClickListener {
            var taskName = myDialog?.findViewById<EditText>(R.id.event_task_name)?.text.toString()
            var taskStartTime =
                myDialog?.findViewById<EditText>(R.id.event_task_start_time)?.text.toString()
            var taskEndTime =
                myDialog?.findViewById<EditText>(R.id.event_task_end_time)?.text.toString()
            var taskVenue =
                myDialog?.findViewById<EditText>(R.id.event_task_location)?.text.toString()

            val checkingDatePicked = myDialog?.findViewById<TextView>(R.id.event_date)?.text

//            Log.i("---","$taskName  ***  $taskStartTime *** $taskEndTime *** $taskVenue")

            if (checkingDatePicked == "No Date Picked") Toast.makeText(
                this,
                "Please Select the Date...",
                Toast.LENGTH_SHORT
            ).show()
            else if (!checkStrings(taskName)) Toast.makeText(this, aStr, Toast.LENGTH_SHORT).show()
            else if (!checkStrings(taskStartTime)) Toast.makeText(this, bStr, Toast.LENGTH_SHORT)
                .show()
            else if (!checkStrings(taskEndTime)) Toast.makeText(this, cStr, Toast.LENGTH_SHORT)
                .show()
            else if (!checkStrings(taskVenue)) Toast.makeText(this, dStr, Toast.LENGTH_SHORT).show()
            else {
                taskName = reorderStrings(taskName)
                taskStartTime = reorderStrings(taskStartTime)
                taskEndTime = reorderStrings(taskEndTime)
                taskVenue = reorderStrings(taskVenue)
                finalTaskName = taskName; finalTaskStartTime = taskStartTime; finalTaskEndTime =
                    taskEndTime; finalTaskLocation = taskVenue
                myDialog?.findViewById<EditText>(R.id.event_task_name)?.setText("")
                myDialog?.findViewById<EditText>(R.id.event_task_start_time)?.setText("")
                myDialog?.findViewById<EditText>(R.id.event_task_end_time)?.setText("")
                myDialog?.findViewById<EditText>(R.id.event_task_location)?.setText("")
                // ****************************************   Adding to database ****************************************
                val userTask = UserTask(
                    "NA",
                    checkingDatePicked.toString(),
                    finalTaskName,
                    finalTaskStartTime,
                    finalTaskEndTime,
                    finalTaskLocation
                )
                databaseReference = FirebaseDatabase.getInstance().getReference("Users")
                    .child(auth.currentUser?.uid.toString())
                databaseReference.child("My Tasks").child(checkingDatePicked.toString()).push()
                    .setValue(userTask)
                Toast.makeText(this, "Task Added", Toast.LENGTH_SHORT).show()
                binding.progressBarTask.visibility = View.VISIBLE
                tasksToday(checkingDatePicked.toString())
                myDialog?.dismiss()
                // ****************************************   Adding to database ****************************************
            }
        }
        // ##########################################  collecting data from the dialog box   ##########################################


        myDialog?.setCancelable(false)
        myDialog?.setCanceledOnTouchOutside(false)
        myDialog?.show()
    }

    private fun reorderStrings(task: String): String {
        val ls = task.split(" ")
        var s = ""
        for (each in ls) {
            s += "$each "
        }
        return s
    }

    private fun checkStrings(taskName: String): Boolean {
        if (taskName.isEmpty()) return false
        for (i in taskName.indices) {
            if (taskName[i] != ' ') {
                Log.i("OK char", i.toString())
                return true
            } else {
                Log.i("NULL char", i.toString())
            }
        }
        return false
    }

    private fun recyclerViewData() {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->

            binding.progressBarTask.visibility = View.VISIBLE
            val tempMonth = month + 1
            var finalMonth = (month + 1).toString()
            var finalDay = dayOfMonth.toString()
            if (tempMonth <= 9) finalMonth = "0$tempMonth"
            if (dayOfMonth <= 9) finalDay = "0$dayOfMonth"
            val date = "$year-${finalMonth}-$finalDay"
//            Log.i("A","$year $month $dayOfMonth")
//            Log.i("B", "$year $tempMonth $dayOfMonth")
//            Log.i("C","$year $finalMonth $finalDay")
            onClickDatedTask(date)
//            sharedPrefAdapter = getSharedPreferences("sharingDataUsingSP",MODE_PRIVATE)
//            val ok = sharedPrefAdapter.getString(
//                "OK" ,
//                "-1"
//            ).toString()
//            if(ok=="true"){
//                onClickDatedTask(date)
//            }
        }
    }

    private fun onClickDatedTask(date: String) {
//        Log.i("mouse2",date)
        tasksToday(date)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun tasksToday(date: String) {
        binding.tasksDate.text = date
        tasksModelArrayList = ArrayList()
        tasksAdapter = TasksAdapter(this, tasksModelArrayList)
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")
            .child(auth.currentUser?.uid.toString()).child("My Tasks").child(date)
        databaseReference.get().addOnSuccessListener {
            if (it.exists()) {
                for (ds in it.children) {
                    val msg = ds.getValue(UserTask::class.java)
//                    Log.i("user task key",ds.key.toString())
                    tasksModelArrayList.add(
                        UserTask(
                            taskDate = date,
                            taskKey = ds.key.toString(),
                            taskName = msg?.taskName.toString(),
                            taskStartTime = msg?.taskStartTime.toString(),
                            taskEndTime = msg?.taskEndTime.toString(),
                            taskVenue = msg?.taskVenue.toString(),
                        )
                    )
                }
                tasksAdapter.notifyDataSetChanged()
            }
            binding.progressBarTask.visibility = View.GONE
        }
            .addOnFailureListener {
                Log.i("addOnFailureListener", it.toString())
                Toast.makeText(this, "Something went wrong...", Toast.LENGTH_SHORT).show()
            }
        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewTasks.adapter = tasksAdapter
    }

}