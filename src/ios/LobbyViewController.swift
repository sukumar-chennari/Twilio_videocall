//
//  Copyright (C) 2020 Twilio, Inc.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//

import UIKit


class LobbyViewController: UIViewController, roomViewControllerDelegate {
    func LeaveTappped() {
        Constants.islevae = true
        self.navigationController?.popViewController(animated: true)//(animated: true, completion: nil)
    }
    
    @IBOutlet weak var loggedInUser: UILabel!
    @IBOutlet weak var videoView: VideoView?
    @IBOutlet weak var containerView: UIView!
    @IBOutlet weak var roomTextField: UITextField!
    @IBOutlet weak var audioToggleButton: UIButton!
    @IBOutlet weak var videoToggleButton: UIButton!
    @IBOutlet weak var flipCameraButton: UIButton!
    private let roomFactory = RoomFactory()
   
    private let deepLinkStore: DeepLinkStoreWriting = DeepLinkStore.shared
    private let notificationCenter = NotificationCenter.default
    private var room: Room!
    private var participant: LocalParticipant { room.localParticipant }
    private var shouldRenderVideo = true
    var userToken = String()
    var userIdentity = String()
    var userID = String()
    var userRoomName = String()
    override func viewDidLoad() {
        super.viewDidLoad()
        print("Avinash -- Lobbyviewcontroller ViewDidLoad")
        resetRoom()
        configureVideoView()

//        roomTextField.attributedPlaceholder = NSAttributedString(
//            string: userRoomName,
//            attributes: [.foregroundColor: UIColor.lightGray]
//        )
        
        if let deepLink = deepLinkStore.consumeDeepLink() {
            switch deepLink {
            case let .room(roomName): roomTextField.text = roomName
            }
        }

//        roomTextField .addTarget(self, action: #selector(joinRoomButtonPressed(_:)), for: .editingDidEndOnExit)
        
        view.addGestureRecognizer(UITapGestureRecognizer(target: self, action: #selector(dismissKeyboard)))
        
       
        
        
        refresh()
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        print("Avinash -- Lobbyviewcontroller viewDidDisappear")
        shouldRenderVideo = false
        configureVideoView()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        print("Avinash -- Lobbyviewcontroller ViewwillAppear")
        self.navigationItem.hidesBackButton = true
        shouldRenderVideo = true
        refresh()
        configureVideoView()
       
    }
    
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        
        let guide = view.safeAreaLayoutGuide
        let height = guide.layoutFrame.origin.y + 108
        containerView.frame = CGRect(x: 0, y: 0, width: view.bounds.width, height: height)
    }

    override func viewSafeAreaInsetsDidChange() {
        super.viewSafeAreaInsetsDidChange()
        
        view.setNeedsLayout()
    }
    override func viewDidAppear(_ animated:Bool) {
        super.viewDidAppear(true)
     //   LoadRoomController()
        print("Avinash -- Lobbyviewcontroller viewDidAppear")
        if( Constants.islevae == false)
        {
            //LoadRoomController()
            DispatchQueue.main.async(){
               // self.LoadRoomController()
               // let sender: Any?
         self.performSegue(withIdentifier: "roomSegue", sender: self)
            }
        }
        else
        {
            Constants.islevae = false
            self.dismiss(animated: true)//(animated: true, completion: nil)
        }
    }
    
    func LoadRoomController() {
        
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let roomViewController = storyboard.instantiateViewController(withIdentifier: "roomViewController") as! RoomViewController
        roomViewController.leaveDelegate = self
        roomViewController.modalPresentationStyle = .fullScreen
        roomViewController.application = .shared
        roomViewController.tokenIS = userToken
        roomViewController.userRoomname = userRoomName
        let splityArray = self.userIdentity.split(separator: "@")
        roomViewController.loginUser = String(splityArray[1])
        roomViewController.userIdentity = userIdentity
        roomViewController.viewModel = RoomViewModelFactory().makeRoomViewModel(
            roomName: userRoomName , //roomTextField.text ?? "",
            room: room
        )
//        let storyboard = UIStoryboard(name: "Main", bundle: nil)
        let statsViewController = storyboard.instantiateViewController(withIdentifier: "statsViewController") as! StatsViewController
        statsViewController.videoAppRoom = room
        roomViewController.statsViewController = statsViewController
    
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        switch segue.identifier {
        case "roomSegue":
            
            // Making an alert to understand user joined in lobby view controller //
//            let alertView = UIAlertController(title: "lobby View Controller", message: "user Joined in Lobby View Controller", preferredStyle: .alert)
//
//            alertView.addAction(UIAlertAction(title: "Done", style: .default, handler: nil))
//            self.present(alertView, animated: true, completion: nil)
            
            
            let roomViewController = segue.destination as! RoomViewController
            roomViewController.application = .shared
            roomViewController.modalPresentationStyle = .fullScreen
            roomViewController.leaveDelegate = self
            roomViewController.tokenIS = userToken
            roomViewController.userRoomname = userRoomName
            let splityArray = self.userIdentity.split(separator: "@")
            roomViewController.loginUser = String(splityArray[1])
            roomViewController.userIdentity = userIdentity
            roomViewController.viewModel = RoomViewModelFactory().makeRoomViewModel(
                roomName: userRoomName , //roomTextField.text ?? "",
                room: room
            )
            let storyboard = UIStoryboard(name: "Main", bundle: nil)
            let statsViewController = storyboard.instantiateViewController(withIdentifier: "statsViewController") as! StatsViewController
            statsViewController.videoAppRoom = room
            roomViewController.statsViewController = statsViewController
            
        case "showSettings":
            let navigationController = segue.destination as! UINavigationController
            let settingsViewController = navigationController.viewControllers.first as! SettingsViewController
            settingsViewController.viewModel = GeneralSettingsViewModel(
                appInfoStore: AppInfoStoreFactory().makeAppInfoStore(),
                appSettingsStore: AppSettingsStore.shared,
                authStore: AuthStore.shared
            )
        default:
            break
        }
    }
        
    @IBAction func toggleAudioPressed(_ sender: Any) {
        participant.isMicOn = !participant.isMicOn
        refresh()
    }
    
    @IBAction func toggleVideoPressed(_ sender: Any) {
        participant.isCameraOn = !participant.isCameraOn
        refresh()
    }
    
    @IBAction func flipCameraPressed(_ sender: Any) {
        participant.cameraPosition = participant.cameraPosition == .front ? .back : .front
    }
    
    @IBAction func joinRoomButtonPressed(_ sender: Any) {
        guard let roomName = roomTextField.text, !roomName.isEmpty else {
            roomTextField.becomeFirstResponder()
            return
        }
        
        dismissKeyboard()
      performSegue(withIdentifier: "roomSegue", sender: self)
    }
    
    private func resetRoom() {
        room = roomFactory.makeRoom(identity: userIdentity)
        participant.isMicOn = true
        participant.isCameraOn = true
    }
    
    @objc private func handleSettingChange() {
        resetRoom() // Pick up settings like identity and video codec
        refresh()
    }

    @objc private func handleRoomUpdate(_ notification: Notification) {
        guard let payload = notification.payload as? Room.Update else { return }
        
        switch payload {
        case let .didUpdateParticipants(participants):
            guard participants.contains(where: { $0 === participant }) else { return }

            configureVideoView()
        default:
            break
        }
    }
    
    private func configureVideoView() {
    
        let config = VideoView.Config(
            videoTrack: shouldRenderVideo ? participant.cameraTrack : nil,
            shouldMirror: participant.shouldMirrorCameraVideo
        )
        videoView?.configure(config: config)
    }
    
    private func refresh() {
//        loggedInUser.text = participant.identity
//        audioToggleButton.isSelected = !participant.isMicOn
//        videoToggleButton.isSelected = !participant.isCameraOn
//        flipCameraButton.isEnabled = participant.isCameraOn
    }

    @objc private func dismissKeyboard() {
        roomTextField.resignFirstResponder()
    }
}
