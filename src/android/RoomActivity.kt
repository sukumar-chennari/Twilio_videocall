/*
 * Copyright (C) 2019 Twilio, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package src.cordova.plugin.videocall.RoomActivity


import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.observe
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloud9.telehealth.R
import com.cloud9.telehealth.databinding.RoomActivityBinding
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioDevice.*
import com.twilio.audioswitch.AudioSwitch
import com.uxcam.UXCam
import cordova.plugin.videocall.BaseActivity.BaseActivity
import cordova.plugin.videocall.InputUtils.InputUtils
import io.uniflow.android.livedata.onEvents
import io.uniflow.android.livedata.onStates
import kotlinx.android.synthetic.main.select_participant.view.*
import kotlinx.android.synthetic.main.select_participant_dialog.view.*
import src.cordova.plugin.videocall.CustomDialog
import src.cordova.plugin.videocall.LocalParticipantManager.LocalParticipantManager
import src.cordova.plugin.videocall.ParticipantAdapter.ParticipantAdapter
import src.cordova.plugin.videocall.ParticipantViewState.ParticipantViewState
import src.cordova.plugin.videocall.PermissionUtil.PermissionUtil
import src.cordova.plugin.videocall.Preferences.Preferences
import src.cordova.plugin.videocall.PrimaryParticipantController.PrimaryParticipantController
import src.cordova.plugin.videocall.RoomManager.RoomManager
import src.cordova.plugin.videocall.RoomViewEffect.RoomViewEffect
import src.cordova.plugin.videocall.RoomViewEvent.RoomViewEvent
import src.cordova.plugin.videocall.RoomViewModel.RoomViewModel
import src.cordova.plugin.videocall.RoomViewState.RoomViewConfiguration
import src.cordova.plugin.videocall.RoomViewState.RoomViewState
import src.cordova.plugin.videocall.SettingsActivity.SettingsActivity
import src.cordova.plugin.videocall.StatsListAdapter.StatsListAdapter
import src.cordova.plugin.videocall.UriRoomParser.UriRoomParser
import src.cordova.plugin.videocall.UriWrapper.UriWrapper
import timber.log.Timber
import java.util.*
import javax.inject.Inject


class RoomActivity : BaseActivity() {
    private lateinit var binding: RoomActivityBinding
    private lateinit var switchCameraMenuItem: MenuItem
    private lateinit var pauseVideoMenuItem: MenuItem
    private lateinit var pauseAudioMenuItem: MenuItem
    private lateinit var screenCaptureMenuItem: MenuItem
    private lateinit var settingsMenuItem: MenuItem
    private lateinit var deviceMenuItem: MenuItem
    private var savedVolumeControlStream = 0
    private var displayName: String? = null
    private var localParticipantSid = LOCAL_PARTICIPANT_STUB_SID
    private lateinit var statsListAdapter: StatsListAdapter
    private lateinit var newThumbnails: List<ParticipantViewState>

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    lateinit var roomManager: RoomManager

    @Inject
    lateinit var audioSwitch: AudioSwitch

    /** Coordinates participant thumbs and primary participant rendering.  */
    private lateinit var primaryParticipantController: PrimaryParticipantController
    private lateinit var participantAdapter: ParticipantAdapter
    private lateinit var roomViewModel: RoomViewModel
    private lateinit var recordingAnimation: ObjectAnimator
    private lateinit var roomId: String
    private lateinit var id: String
    private lateinit var loginUser: String
    private lateinit var identity: String
    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity = this
        progressDialog = ProgressDialog(activity)
        binding = RoomActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        UXCam.occludeSensitiveScreen(true)
        isDialogVisible = false
        roomId = intent.getStringExtra("room")!!
        identity = intent.getStringExtra("identity")!!
        loginUser = intent.getStringExtra("identity")!!.split("@")[1]
        accessToken = intent.getStringExtra("token")!!
        id = intent.getStringExtra("id")!!
//
        binding.joinRoom.roomName.doOnTextChanged { text: CharSequence?, _, _, _ ->
            roomNameTextChanged(text)
        }
        binding.joinRoom.connect.setOnClickListener { connectButtonClick() }
        binding.disconnect.setOnClickListener { disconnectButtonClick() }

        binding.add.setOnClickListener {
            var isPatient = false
            val list = mutableListOf<String>()
            list.add("--Select Role--")
            for (p in newThumbnails) {
                if (p.identity?.split("@")?.get(1) == "patient") {
                    isPatient = true
                    break
                }
            }
            when (isPatient) {
                false -> {
                    when(roomManager.isThreeParticipantsJoined){
                        false -> list.add("Patient")
                    }

                }
            }
            when(roomManager.isThreeParticipantsJoined){
                false -> {
                    list.add("MHT")
                    list.add("CCT")
                }
            }

            list.add("Participants list")
            Log.e("visi->", "${isFinishing}, ${isDialogVisible}")
            if (!isFinishing && !isDialogVisible) {
                val customDialog =
                    CustomDialog.newInstance(roomId, id, newThumbnails, identity, list)
                isDialogVisible = true
                customDialog.isCancelable = false
                customDialog.show(supportFragmentManager, "")
            }
        }
        binding.localVideo.setOnClickListener { toggleLocalVideo() }
        binding.localAudio.setOnClickListener { toggleLocalAudio() }

        var permissionUtil = PermissionUtil(this)
        val factory =
            RoomViewModel.RoomViewModelFactory(roomManager, audioSwitch, permissionUtil)
        roomViewModel = ViewModelProvider(this, factory).get(RoomViewModel::class.java)

        // So calls can be answered when screen is locked
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        // Grab views
        setupThumbnailRecyclerView()

        // Setup toolbar
        setSupportActionBar(binding.toolbar)

        // Cache volume control stream
        savedVolumeControlStream = volumeControlStream

        // Setup participant controller
        primaryParticipantController = PrimaryParticipantController(binding.room.primaryVideo)

//        listenBroadcast()
        setupRecordingAnimation()
        connectButtonClick()


        /* when(permissionUtil.isPermissionGranted(Manifest.permission.CAMERA) &&
                 permissionUtil.isPermissionGranted(Manifest.permission.RECORD_AUDIO)){
             true -> connectButtonClick()
             false -> roomViewModel.checkPermissions()
         }*/

    }

    /*fun listenBroadcast() {
        var participantDetails: List<ParticipantDetails>
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                if (!intent.extras!!.isEmpty) {
                    progressDialog.dismiss()

                    val s3 = intent.extras!!.getString("thirdPartyList")

                    participantDetails = Gson().fromJson(
                        s3, object : TypeToken<List<ParticipantDetails>>() {}.type
                    )
                    Log.e("data received-> ", participantDetails.toString())
                    if (!isFinishing && !isDialogVisible) {
                        loginUser
                                *//*object: CustomDialog.ParticipantSelectedListener {
                                    override fun onParticipantSelected(
                                        id: String,
                                        name: String,
                                        room: String
                                    ) {
                                        val intent = Intent("sendNotification");
                                        var b = Bundle()
                                        b.putString("selectedParticipantID", id)
                                        b.putString("selectedParticipantName", name)
                                        b.putString("roomName", room)
                                        intent.putExtras(b);
                                        LocalBroadcastManager.getInstance(this@RoomActivity).sendBroadcastSync(intent);
                                    }

                                }*//*
                            )

                        isDialogVisible = true
                        customDialog.show(supportFragmentManager, "")
                        return
                    }
                    *//*val viewGroup = findViewById<ViewGroup>(android.R.id.content)
                    val dialogView: View =
                      LayoutInflater.from(this@RoomActivity)
                        .inflate(R.layout.select_participant_dialog, viewGroup, false)*//*
                    *//*val dialogView: View =
                      this@RoomActivity.layoutInflater.inflate(R.layout.select_participant_dialog, null)
                    val builder = AlertDialog.Builder(this@RoomActivity)
                    builder.setView(dialogView)
                    val alertDialog = builder.create()
                    dialogView.select_recycler.layoutManager = LinearLayoutManager(this@RoomActivity)
          //          if(newThumbnails.size == 1){
          //            dialogView.rb1.visibility = View.VISIBLE
          //            dialogView.rb2.visibility = View.VISIBLE
          //            if(loginUser == "cct")
          //              dialogView.rb1.text = "mht"
          //            else
          //              dialogView.rb1.text = "cct"
          //            dialogView.rb1.text = "patient"
          //          }else if(newThumbnails.size == 2){
          //            dialogView.rb1.visibility = View.VISIBLE
          //            dialogView.rb2.visibility = View.INVISIBLE
          //            if (loginUser == "cct")
          //              dialogView.rb1.text = "mht"
          //            else
          //              dialogView.rb1.text = "cct"
          //          }

                    alertDialog.show()

                    *//**//*val spinnerData = arrayListOf<String>()
          for (participant in participantDetails)
            spinnerData.add(participant.name)

          val arrayAdapter =
            ArrayAdapter(
              this@RoomActivity,
              R.layout.simple_spinner_item,
              spinnerData
            )
          arrayAdapter.setDropDownViewResource(R.layout.simple_spinner_drop_down)
          dialogView.select.adapter = arrayAdapter*//**//*

          *//**//*dialogView.cancel.setOnClickListener(View.OnClickListener {
            alertDialog.dismiss()

          })
          dialogView.ok.setOnClickListener(View.OnClickListener {
            alertDialog.dismiss()

          })*//**//*
          var selectParticipantRecycler = SelectParticipantRecycler(participantDetails, object :
            SelectParticipantRecycler.ParticipantSelectListener {
            override fun onParticipantSelected(position: Int) {
              alertDialog.dismiss()

              Toast.makeText(
                this@RoomActivity,
                "Selected: ${participantDetails[position].name}",
                Toast.LENGTH_LONG
              ).show()
              val intent = Intent("selectedParticipant")
              val b = Bundle()
              b.putString("identity", participantDetails[position].name)
              intent.putExtras(b)
              LocalBroadcastManager.getInstance(this@RoomActivity).sendBroadcastSync(intent)
            }
          })

          dialogView.select_recycler.adapter = selectParticipantRecycler*//*
                }
            }
        }
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter("receiveThirdPartyList"))
    }

    fun addParticipant() {

        var isDoctor = false
        var isPatient = false
        var isResponder = false

        progressDialog.show();
        val intent = Intent("callingThirdParticipant");
        var b = Bundle()
        for (p in newThumbnails) {
            when (p.identity == null) {
                true -> when (loginUser) {
                    "practitioner" -> isDoctor = true
                    "responder" -> isResponder = true
                    else -> isPatient = true
                }
                false -> when (p.identity?.split("@")?.get(1)) {
                    "practitioner" -> isDoctor = true
                    "responder" -> isResponder = true
                    else -> isPatient = true
                }
            }
        }

        when (false) {
            isDoctor -> b.putString("requiredPartcipant", "practitioner")
            isResponder -> b.putString("requiredPartcipant", "responder")
            isPatient -> b.putString("requiredPartcipant", "patient")
        }


        b.putString("presentParticipantID", id)
        intent.putExtras(b);
        Log.e("partici-> ", "adding");
        LocalBroadcastManager.getInstance(this).sendBroadcastSync(intent);

    }*/

    override fun onDestroy() {
        super.onDestroy()
        UXCam.occludeSensitiveScreen(false)
        recordingAnimation.cancel()
    }

    override fun onStart() {
        super.onStart()
        checkIntentURI()
    }

    override fun onResume() {
        super.onResume()
        displayName = identity
        setTitle(displayName)
        roomViewModel.processInput(RoomViewEvent.OnResume)
    }

    override fun onPause() {
        super.onPause()
        roomViewModel.processInput(RoomViewEvent.OnPause)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            val recordAudioPermissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            val cameraPermissionGranted = grantResults[1] == PackageManager.PERMISSION_GRANTED
            if (recordAudioPermissionGranted && cameraPermissionGranted) {
                roomViewModel.processInput(RoomViewEvent.OnResume)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.room_menu, menu)
        settingsMenuItem = menu.findItem(R.id.settings_menu_item)
        // Grab menu items for updating later
        switchCameraMenuItem = menu.findItem(R.id.switch_camera_menu_item)
        pauseVideoMenuItem = menu.findItem(R.id.pause_video_menu_item)
        pauseAudioMenuItem = menu.findItem(R.id.pause_audio_menu_item)
        screenCaptureMenuItem = menu.findItem(R.id.share_screen_menu_item)
        screenCaptureMenuItem.isVisible = false
        deviceMenuItem = menu.findItem(R.id.device_menu_item)

        onStates(roomViewModel) { state ->
            if (state is RoomViewState) bindRoomViewState(state)
        }
        onEvents(roomViewModel) { event ->
            if (event is RoomViewEffect) bindRoomViewEffects(event)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.switch_camera_menu_item -> {
                roomViewModel.processInput(RoomViewEvent.SwitchCamera)
                true
            }
            R.id.share_screen_menu_item -> {
                if (item.title == getString(R.string.share_screen)) {
                    requestScreenCapturePermission()
                } else {
                    roomViewModel.processInput(RoomViewEvent.StopScreenCapture)
                }
                true
            }
            R.id.device_menu_item -> {
                displayAudioDeviceList()
                true
            }
            R.id.pause_audio_menu_item -> {
                if (item.title == getString(R.string.pause_audio))
                    roomViewModel.processInput(RoomViewEvent.DisableLocalAudio)
                else
                    roomViewModel.processInput(RoomViewEvent.EnableLocalAudio)
                true
            }
            R.id.pause_video_menu_item -> {
                if (item.title == getString(R.string.pause_video))
                    roomViewModel.processInput(RoomViewEvent.DisableLocalVideo)
                else
                    roomViewModel.processInput(RoomViewEvent.EnableLocalVideo)
                true
            }
            R.id.settings_menu_item -> {
                val intent = Intent(this@RoomActivity, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MEDIA_PROJECTION_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Snackbar.make(
                    binding.room.primaryVideo,
                    R.string.screen_capture_permission_not_granted,
                    BaseTransientBottomBar.LENGTH_LONG
                )
                    .show()
                return
            }
            data?.let { data ->
                roomViewModel.processInput(RoomViewEvent.StartScreenCapture(resultCode, data))
            }
        }
    }

    override fun onBackPressed() {
//        super.onBackPressed()
//        roomViewModel.processInput(RoomViewEvent.Disconnect)
    }

    private fun setupRecordingAnimation() {
        val recordingDrawable = ContextCompat.getDrawable(this, R.drawable.ic_recording)
        recordingAnimation = ObjectAnimator.ofPropertyValuesHolder(
            recordingDrawable,
            PropertyValuesHolder.ofInt("alpha", 100, 255)
        ).apply {
            target = recordingDrawable
            duration = 750
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            start()
        }
        binding.recordingIndicator.setCompoundDrawablesWithIntrinsicBounds(
            recordingDrawable, null, null, null
        )
    }

    private fun checkIntentURI(): Boolean {
        var isAppLinkProvided = false
        val uri = intent.data
        val roomName = UriRoomParser(UriWrapper(uri)).parseRoom()
        if (roomName != null) {
            binding.joinRoom.roomName.setText(roomName)
            isAppLinkProvided = true
        }
        return isAppLinkProvided
    }

    private fun setupThumbnailRecyclerView() {
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.room.remoteVideoThumbnails.layoutManager = layoutManager
        participantAdapter = ParticipantAdapter()
        participantAdapter.viewHolderEvents.observe(this) { viewEvent -> roomViewModel.processInput(viewEvent) }
//      .observe(this, { viewEvent: RoomViewEvent -> roomViewModel.processInput(viewEvent) })
        binding.room.remoteVideoThumbnails.adapter = participantAdapter
    }

    private fun roomNameTextChanged(text: CharSequence?) {
        binding.joinRoom.connect.isEnabled = !TextUtils.isEmpty(text)
    }

    private fun connectButtonClick() {
        InputUtils.hideKeyboard(this)
        binding.joinRoom.connect.isEnabled = false
        // obtain room name
        val text = roomId
//        if (text != null) {
        val roomName = text.toString()
        val viewEvent = RoomViewEvent.Connect(displayName ?: "", roomName)
        roomViewModel.processInput(viewEvent)
        if(LocalParticipantManager.isAudioMuted)
            toggleLocalAudio()
        if(LocalParticipantManager.isVideoMuted)
            toggleLocalVideo()
//        }
    }

    private fun disconnectButtonClick() {
        roomViewModel.processInput(RoomViewEvent.Disconnect)
        finish()
        // TODO Handle screen share
    }

    private fun toggleLocalVideo() {
        roomViewModel.processInput(RoomViewEvent.ToggleLocalVideo)
    }

    private fun toggleLocalAudio() {
        roomViewModel.processInput(RoomViewEvent.ToggleLocalAudio)
        binding.room.remoteVideoThumbnails.adapter = participantAdapter
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                ),
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    private fun updateLayout(roomViewState: RoomViewState) {
        var disconnectButtonState = View.GONE
        var joinRoomLayoutState = View.VISIBLE
        var joinStatusLayoutState = View.GONE
        var settingsMenuItemState = true
        var screenCaptureMenuItemState = false
        val roomEditable = binding.joinRoom.roomName.text
        val isRoomTextNotEmpty = roomEditable != null && roomEditable.toString().isNotEmpty()
        var connectButtonEnabled = isRoomTextNotEmpty
        var roomName = displayName
        var toolbarTitle = displayName
        var joinStatus = ""
        var recordingWarningVisibility = View.GONE
        when (roomViewState.configuration) {
            RoomViewConfiguration.Connecting -> {
                //disconnectButtonState = View.VISIBLE
                joinRoomLayoutState = View.GONE
                joinStatusLayoutState = View.VISIBLE
                recordingWarningVisibility = View.VISIBLE
                settingsMenuItemState = false
                connectButtonEnabled = false
                if (roomEditable != null) {
                    roomName = roomEditable.toString()
                }
                joinStatus = "Joining..."
            }
            RoomViewConfiguration.Connected -> {
                disconnectButtonState = View.VISIBLE
                joinRoomLayoutState = View.GONE
                joinStatusLayoutState = View.GONE
                settingsMenuItemState = false
                screenCaptureMenuItemState = false
                connectButtonEnabled = false
                roomName = roomViewState.title
                toolbarTitle = roomName
                joinStatus = ""
                binding.recordingIndicator.visibility =
                    if (roomViewState.isRecording) View.VISIBLE else View.GONE
            }
            RoomViewConfiguration.Lobby -> {
                connectButtonEnabled = isRoomTextNotEmpty
                screenCaptureMenuItemState = false
                binding.recordingIndicator.visibility = View.GONE
            }
        }

        val isMicEnabled = roomViewState.isMicEnabled
        val isCameraEnabled = roomViewState.isCameraEnabled
        val isLocalMediaEnabled = isMicEnabled && isCameraEnabled
        binding.localAudio.isEnabled = isLocalMediaEnabled
        binding.localVideo.isEnabled = isLocalMediaEnabled
        val micDrawable =
            if (roomViewState.isAudioMuted || !isLocalMediaEnabled) R.drawable.ic_mute else R.drawable.ic_mic_white_24px
        val videoDrawable =
            if (roomViewState.isVideoOff || !isLocalMediaEnabled) R.drawable.ic_off_video else R.drawable.ic_videocam_white_24px
        binding.localAudio.setImageResource(micDrawable)
        binding.localVideo.setImageResource(videoDrawable)
        statsListAdapter = StatsListAdapter(this)
        binding.statsRecyclerView.adapter = statsListAdapter
        binding.statsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.disconnect.visibility = disconnectButtonState
        binding.joinRoom.joinRoomLayout.visibility = joinRoomLayoutState
        binding.joinStatusLayout.visibility = joinStatusLayoutState
        binding.joinRoom.connect.isEnabled = connectButtonEnabled
        setTitle(toolbarTitle)
        binding.joinStatus.text = joinStatus
        binding.joinRoomName.text = roomName
        binding.recordingNotice.visibility = recordingWarningVisibility
        val pauseAudioTitle =
            getString(if (roomViewState.isAudioEnabled) R.string.pause_audio else R.string.resume_audio)
        val pauseVideoTitle =
            getString(if (roomViewState.isVideoEnabled) R.string.pause_video else R.string.resume_video)
        pauseAudioMenuItem.title = pauseAudioTitle
        pauseVideoMenuItem.title = pauseVideoTitle

        // TODO: Remove when we use a Service to obtainTokenAndConnect to a room
        settingsMenuItem.isVisible = settingsMenuItemState
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            screenCaptureMenuItem.isVisible = screenCaptureMenuItemState
            val screenCaptureResources = if (roomViewState.isScreenCaptureOn) {
                R.drawable.ic_stop_screen_share_white_24dp to getString(R.string.stop_screen_share)
            } else {
                R.drawable.ic_screen_share_white_24dp to getString(R.string.share_screen)
            }
            screenCaptureMenuItem.icon = ContextCompat.getDrawable(
                this,
                screenCaptureResources.first
            )
            screenCaptureMenuItem.title = screenCaptureResources.second
        }
        if (loginUser != "patient" && newThumbnails.size == 2) {
            Log.e("logging for app kill state", loginUser +" - "+ newThumbnails.size)
            binding.add.visibility = View.VISIBLE
        }else
            binding.add.visibility = View.GONE
    }

    private fun setTitle(toolbarTitle: String?) {
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.title = toolbarTitle
        }
    }

    private fun setVolumeControl(setVolumeControl: Boolean) {
        volumeControlStream = if (setVolumeControl) {
            /*
             * Enable changing the volume using the up/down keys during a conversation
             */
            AudioManager.STREAM_VOICE_CALL
        } else {
            savedVolumeControlStream
        }
    }

    @TargetApi(21)
    private fun requestScreenCapturePermission() {
        Timber.d("Requesting permission to capture screen")
        val mediaProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // This initiates a prompt dialog for the user to confirm screen projection.
        startActivityForResult(
            mediaProjectionManager.createScreenCaptureIntent(), MEDIA_PROJECTION_REQUEST_CODE
        )
    }

    private fun updateStatsUI(roomViewState: RoomViewState) {
        val enableStats = sharedPreferences.getBoolean(
            Preferences.ENABLE_STATS, Preferences.ENABLE_STATS_DEFAULT
        )
        if (enableStats) {
            when (roomViewState.configuration) {
                RoomViewConfiguration.Connected -> {
                    statsListAdapter.updateStatsData(roomViewState.roomStats)
                    binding.statsRecyclerView.visibility = View.VISIBLE
                    binding.statsDisabled.visibility = View.GONE

                    // disable stats if there is room but no participants (no media)
                    val isStreamingMedia = roomViewState.participantThumbnails?.let { thumbnails ->
                        thumbnails.size > 1
                    } ?: false
                    if (!isStreamingMedia) {
                        binding.statsDisabledTitle.text = getString(R.string.stats_unavailable)
                        binding.statsDisabledDescription.text =
                            getString(R.string.stats_description_media_not_shared)
                        binding.statsRecyclerView.visibility = View.GONE
                        binding.statsDisabled.visibility = View.VISIBLE
                    }
                }
                else -> {
                    binding.statsDisabledTitle.text = getString(R.string.stats_unavailable)
                    binding.statsDisabledDescription.text =
                        getString(R.string.stats_description_join_room)
                    binding.statsRecyclerView.visibility = View.GONE
                    binding.statsDisabled.visibility = View.VISIBLE
                }
            }
        } else {
            binding.statsDisabledTitle.text = getString(R.string.stats_gathering_disabled)
            binding.statsDisabledDescription.text = getString(R.string.stats_enable_in_settings)
            binding.statsRecyclerView.visibility = View.GONE
            binding.statsDisabled.visibility = View.VISIBLE
        }
    }

    private fun toggleAudioDevice(enableAudioDevice: Boolean) {
        setVolumeControl(enableAudioDevice)
        val viewEvent =
            if (enableAudioDevice) RoomViewEvent.ActivateAudioDevice else RoomViewEvent.DeactivateAudioDevice
        roomViewModel.processInput(viewEvent)
    }

    private fun bindRoomViewState(roomViewState: RoomViewState) {
        deviceMenuItem.isVisible = roomViewState.availableAudioDevices?.isNotEmpty() ?: false
        renderPrimaryView(roomViewState.primaryParticipant)
        renderThumbnails(roomViewState)
        updateLayout(roomViewState)
        updateAudioDeviceIcon(roomViewState.selectedDevice)
        updateStatsUI(roomViewState)
    }

    private fun bindRoomViewEffects(roomViewEffect: RoomViewEffect) {
        when (roomViewEffect) {
            RoomViewEffect.PermissionsDenied -> requestPermissions()
            is RoomViewEffect.Connected -> {
                toggleAudioDevice(true)
            }
            RoomViewEffect.Disconnected -> {

                localParticipantSid = LOCAL_PARTICIPANT_STUB_SID
                // TODO Update stats
                toggleAudioDevice(false)
            }
            RoomViewEffect.ShowConnectFailureDialog, RoomViewEffect.ShowMaxParticipantFailureDialog -> {
                AlertDialog.Builder(this, R.style.AppTheme_Dialog)
                    .setTitle(getString(R.string.room_screen_connection_failure_title))
                    .setMessage(getConnectFailureMessage(roomViewEffect))
                    .setNeutralButton(getString(android.R.string.ok), null)
                    .show()
                toggleAudioDevice(false)
            }
//            is ShowTokenErrorDialog -> {
//                val error = roomViewEffect.serviceError
//                handleTokenError(error)
//            }
//            RoomViewEffect.PermissionsDenied -> requestPermissions()
        }
    }

    private fun getConnectFailureMessage(roomViewEffect: RoomViewEffect) =
        getString(
            when (roomViewEffect) {
                RoomViewEffect.ShowMaxParticipantFailureDialog -> R.string.room_screen_max_participant_failure_message
                else -> R.string.room_screen_connection_failure_message
            }
        )

    private fun updateAudioDeviceIcon(selectedAudioDevice: AudioDevice?) {
        val audioDeviceMenuIcon = when (selectedAudioDevice) {
            is BluetoothHeadset -> R.drawable.ic_bluetooth_white_24dp
            is WiredHeadset -> R.drawable.ic_headset_mic_white_24dp
            is Speakerphone -> R.drawable.ic_volume_up_white_24dp
            else -> R.drawable.ic_phonelink_ring_white_24dp
        }
        this.deviceMenuItem.setIcon(audioDeviceMenuIcon)
    }

    private fun renderPrimaryView(primaryParticipant: ParticipantViewState) {
        primaryParticipant.run {
            primaryParticipantController.renderAsPrimary(
                if (isLocalParticipant) getString(R.string.you) else identity,
                screenTrack,
                videoTrack,
                isMuted,
                isMirrored
            )
            binding.room.primaryVideo.showIdentityBadge(!primaryParticipant.isLocalParticipant)
        }
    }

    private fun renderThumbnails(roomViewState: RoomViewState) {
        try {
            newThumbnails = if (roomViewState.configuration is RoomViewConfiguration.Connected)
                roomViewState.participantThumbnails!! else emptyList()
            participantAdapter.submitList(newThumbnails)
        } catch (e: Exception) {
            e.message
        }

    }

    private fun displayAudioDeviceList() {
        (roomViewModel.getState() as RoomViewState).let { viewState ->
            val selectedDevice = viewState.selectedDevice
            val audioDevices = viewState.availableAudioDevices
            if (selectedDevice != null && audioDevices != null) {
                val index = audioDevices.indexOf(selectedDevice)
                val audioDeviceNames = ArrayList<String>()
                for (a in audioDevices) {
                    audioDeviceNames.add(a.name)
                }

                createAudioDeviceDialog(this, index, audioDeviceNames,
                    DialogInterface.OnClickListener { dialogInterface, i ->
                        dialogInterface.dismiss()
                        val viewEvent = RoomViewEvent.SelectAudioDevice(audioDevices[i])
                        roomViewModel.processInput(viewEvent)
                    })
                    /*createAudioDeviceDialog(
                      this,
                      index,
                      audioDeviceNames
                    ) {
                        dialogInterface: DialogInterface, i: Int ->
                      dialogInterface.dismiss()
                      val viewEvent = SelectAudioDevice(audioDevices[i])
                      roomViewModel.processInput(viewEvent)
                    }*/
                    .show()
            }
        }
    }

    private fun createAudioDeviceDialog(
        activity: Activity,
        currentDevice: Int,
        availableDevices: ArrayList<String>,
        audioDeviceClickListener: DialogInterface.OnClickListener
    ): AlertDialog {
        val builder = AlertDialog.Builder(activity, R.style.AppTheme_Dialog)
        builder.setTitle(activity.getString(R.string.room_screen_select_device))
        builder.setSingleChoiceItems(
            availableDevices.toTypedArray<CharSequence>(),
            currentDevice,
            audioDeviceClickListener
        )
        return builder.create()
    }


    companion object {
        lateinit var activity: Activity
        var isDialogVisible = false
        var accessToken: String = ""
        var roomSid: String = ""
        private const val PERMISSIONS_REQUEST_CODE = 100
        private const val MEDIA_PROJECTION_REQUEST_CODE = 101

        // This will be used instead of real local participant sid,
        // because that information is unknown until room connection is fully established
        private const val LOCAL_PARTICIPANT_STUB_SID = ""
        fun startActivity(context: Context, appLink: Uri?) {
            val intent = Intent(context, RoomActivity::class.java)
            intent.data = appLink
            context.startActivity(intent)
        }
    }
}
