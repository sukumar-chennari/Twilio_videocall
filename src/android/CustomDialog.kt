package src.cordova.plugin.videocall


import android.app.ProgressDialog
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloud9.telehealth.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cordova.plugin.videocall.ApiService.ApiService
import cordova.plugin.videocall.RetrofitAPi.RetrofitAPi
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.select_participant_dialog.view.*
import src.cordova.plugin.videocall.ParticipantViewState.ParticipantViewState
import src.cordova.plugin.videocall.RoomActivity.RoomActivity


class CustomDialog() : DialogFragment() {

    private lateinit var receiver: BroadcastReceiver

    companion object {

        private lateinit var roomName: String
        private lateinit var progressDialog: ProgressDialog
        private lateinit var participantId: String
        private lateinit var roomParticipants: List<ParticipantViewState>
        private lateinit var identity: String
        private lateinit var list: MutableList<String>

        fun newInstance(
            roomName: String?, participantId: String?,
            roomParticipants: List<ParticipantViewState>?, identity: String,
            list: MutableList<String>
        ): CustomDialog {
            CustomDialog.roomName = roomName!!
            CustomDialog.participantId = participantId!!
            CustomDialog.roomParticipants = roomParticipants!!
            CustomDialog.identity = identity!!
            progressDialog = ProgressDialog(RoomActivity.activity)
            progressDialog.setCanceledOnTouchOutside(false)
            CustomDialog.list = list
            return CustomDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.select_participant_dialog, container)
    }

    override fun onViewCreated(view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog!!.setCanceledOnTouchOutside(false)
        view.cancel.setOnClickListener {
            dismiss()
            RoomActivity.isDialogVisible = false
        }
        view.select_recycler.layoutManager = LinearLayoutManager(activity)
        listenBroadcast()

        val spinner = view.select_spnr
        val arrayAdapter =
            ArrayAdapter<String>(RoomActivity.activity, android.R.layout.simple_list_item_1, list)
        spinner.adapter = arrayAdapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                view?.select_recycler?.visibility = View.GONE
                val user = list[p2]
                if (user != "--Select Role--") {
                    progressDialog.show()
                    val progressRunnable = Runnable {
                        when (progressDialog.isShowing) {
                            true -> {
                                Toast.makeText(RoomActivity.activity, "You are not authorized to add a user.", Toast.LENGTH_LONG).show()
                                progressDialog.dismiss()
                            }
                        }
                    }
                    val handler = Handler()
                    handler.postDelayed(progressRunnable, 10 * 1000)
                    if (user == "Participants list") {
                        RetrofitAPi.getRetrofitService().create(ApiService::class.java)
                            .getAllRoomParticipants(roomName)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(object : SingleObserver<Response> {
                                override fun onSubscribe(d: Disposable) {
                                }

                                override fun onSuccess(t: Response) {
                                    var participantDetails = mutableListOf<ParticipantDetails>()
                                    val gson = Gson()
                                    Log.e("drop part->", t.data.toString())
                                    val itemType =
                                        object : TypeToken<List<RoomParticipantsResponse>>() {}.type

                                    val data = gson.fromJson<List<RoomParticipantsResponse>>(
                                        gson.toJson(t.data),
                                        itemType
                                    )

                                    for (list in data) {
                                        val role = when (list.role) {
                                            "doctor" -> "practitioner"
                                            "responder" -> "responder"
                                            else -> "patient"
                                        }
                                        participantDetails.add(
                                            ParticipantDetails(
                                                "${list.user_id}",
                                                "${list.full_name}@$role"
                                            )
                                        )
                                    }

                                    bindToRecycle(participantDetails, user)
                                }

                                override fun onError(e: Throwable) {
                                    when (progressDialog.isShowing) {
                                        true -> progressDialog.dismiss()
                                    }
                                    Toast.makeText(
                                        RoomActivity.activity,
                                        "Something went wrong, ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                            })
                    } else {
                        val intent = Intent("callingThirdParticipant");
                        var b = Bundle()
                        Log.e("selected->", user)
                        when (user) {
                            "Patient" -> b.putString("requiredPartcipant", "patient")
                            "CCT" -> b.putString("requiredPartcipant", "responder")
                            "MHT" -> b.putString("requiredPartcipant", "practitioner")
                        }

                        b.putString("presentParticipantID", participantId)
                        b.putString("roomName", roomName)
                        b.putString("roomSid", RoomActivity.roomSid)
                        for (participantViewState in roomParticipants) {
                            if (participantViewState.identity != null) {
                                Log.e(
                                    "presentParticipantRole",
                                    participantViewState.identity.split("@")[1]
                                )
                                b.putString(
                                    "presentParticipantRole",
                                    participantViewState.identity.split("@")[1]
                                )
                                break
                            }
                        }
                        intent.putExtras(b);
                        Log.e("partici-> ", "adding");
                        LocalBroadcastManager.getInstance(RoomActivity.activity)
                            .sendBroadcastSync(intent)

                    }
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }

    }

    fun listenBroadcast() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (!intent.extras!!.isEmpty) {
                    val s3 = intent.extras!!.getString("thirdPartyList")
                    val participantDetails: List<ParticipantDetails> = Gson().fromJson(
                        s3, object : TypeToken<List<ParticipantDetails>>() {}.type
                    )
                    bindToRecycle(participantDetails, "MHT/CCT")
                } else {
                    when (progressDialog.isShowing) {
                        true -> progressDialog.dismiss()
                    }
                    Toast.makeText(
                        RoomActivity.activity,
                        "No Participants Available",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        LocalBroadcastManager.getInstance(RoomActivity.activity)
            .registerReceiver(receiver, IntentFilter("receiveThirdPartyList"))
    }

    private fun bindToRecycle(participantDetails: List<ParticipantDetails>, type: String) {
        when (progressDialog.isShowing) {
            true -> progressDialog.dismiss()
        }
        Log.e("data received-> ", participantDetails.toString())
        var flag = false
        var selectParticipantRecycler =
            SelectParticipantRecycler(participantDetails, object :
                SelectParticipantRecycler.ParticipantSelectListener {
                override fun onParticipantSelected(position: Int) {
                    dismiss()
                    RoomActivity.isDialogVisible = false
                    val splitValue = identity.split("@")
                    val userAndRole = splitValue[0] + "@" + splitValue[1]
                    loop@ for (roomParticipant in roomParticipants) {
                        if (userAndRole == participantDetails[position].name
                            || participantDetails[position].name == roomParticipant.identity
                        ) {
                            flag = true
                            Toast.makeText(
                                RoomActivity.activity,
                                "Participant already on call",
                                Toast.LENGTH_LONG
                            ).show()
                            break@loop

                        }
                    }
                    when (flag) {
                        false -> {
                            var name = when (participantDetails[position].name.contains("@")) {
                                true -> participantDetails[position].name.split("@")[0]
                                false -> participantDetails[position].name
                            }

                            Toast.makeText(
                                RoomActivity.activity,
                                "Selected: $name",
                                Toast.LENGTH_LONG
                            ).show()
                            when (type) {
                                "MHT/CCT" -> {
                                    val intent = Intent("sendNotification");
                                    var b = Bundle()
                                    b.putString(
                                        "selectedParticipantID",
                                        participantDetails[position].userId
                                    )
                                    b.putString(
                                        "selectedParticipantName",
                                        name
                                    )
                                    b.putString("roomName", roomName)
                                    b.putString("roomSid", RoomActivity.roomSid)
                                    intent.putExtras(b)
                                    Log.e(
                                        "calling ->",
                                        "sendNotification" + participantDetails[position].userId + "-" + name
                                    )
                                    LocalBroadcastManager.getInstance(activity!!)
                                        .sendBroadcastSync(intent)
                                }
                                "Participants list" -> {
                                    var role = when (identity.split("@")[1]) {
                                        "mht" -> "doctor"
                                        else -> "responder"
                                    }
                                    RetrofitAPi.getRetrofitService().create(ApiService::class.java)
                                        .sendNotification(
                                            roomName,
                                            RoomActivity.roomSid,
                                            name,
                                            role,
                                            identity.split("@")[3],
                                            participantDetails[position].userId,
                                            "participant_list",
                                            identity.split("@")[2]
                                        )
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe { v ->
                                            Log.e(
                                                "sendNotification API->",
                                                "${v.toString()}"
                                            )
                                        }
                                }
                            }


                        }
                    }

                }
            })
        if (participantDetails.isEmpty()) {
            view?.empty?.visibility = View.VISIBLE
            view?.select_recycler?.visibility = View.GONE
        } else {
            view?.empty?.visibility = View.GONE
            view?.select_recycler?.visibility = View.VISIBLE
            view?.select_recycler?.adapter = selectParticipantRecycler
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        try {
            RoomActivity.activity.unregisterReceiver(receiver)
        } catch (e: Exception) {
            e.message
        }
    }
}
