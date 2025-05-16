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

import IGListDiffKit
import UIKit
import SwiftyPickerPopover
import AppCenter
import AppCenterDistribute
import AppCenterAnalytics
protocol roomViewControllerDelegate {
    func LeaveTappped()
}
class RoomViewController: UIViewController {
    var leaveDelegate:roomViewControllerDelegate!
    @IBOutlet weak var disableMicButton: CircleToggleButton!
    @IBOutlet weak var disableCameraButton:  CircleToggleButton!
    @IBOutlet weak var leaveButton: UIButton!
    @IBOutlet weak var addButton: UIButton!
    @IBOutlet weak var switchCameraButton: UIButton!
    @IBOutlet weak var roomNameLabel: UILabel!
    @IBOutlet weak var participantCollectionView: UICollectionView!
    @IBOutlet weak var mainVideoView: VideoView!
    @IBOutlet weak var mainIdentityLabel: UILabel!
    @IBOutlet weak var recordingView: UIView!
    var viewModel: RoomViewModel!
    var statsViewController: StatsViewController!
    var application: UIApplication!
    var tokenIS = ""
    var spinner = UIActivityIndicatorView(activityIndicatorStyle: .whiteLarge)
    var loadingView: UIView = UIView()
    var payload = [String:Any]()
    var sucesspayload = [String:Any]()
    var isthirdPartyList = Bool()
    var isParticipantInCall = Bool()
    var participentDict = [String:Any]();
    var roleseArray = [String]();
    var participentArray = [Any]()
    var namesArray = [String]()
    var idsArray = [NSNumber] ()
    var rolesArray = [String] ()
    var userIdentity = String()
    var userRoomname = String()
    var userID = String()
    var loginUser = String()
    var selectedRole = String();
    var isPatient = Bool()
    override func viewDidLoad() {
        super.viewDidLoad()
//        self.getAcessTokenAPI();
        
//        overrideUserInterfaceStyle = .light
        
        self.addButton.isHidden = true
        isPatient = false
       
        let nc = NotificationCenter.default
        nc.addObserver(forName:Notification.Name(rawValue:"receiveThirdPartyList"),
                       object:nil, queue:nil) {
            notification in
            
            var jsonString: String? = nil
            jsonString = notification.userInfo?["thirdPartyList"] as? String
            
            guard let myString = jsonString, !myString.isEmpty else {
                print("String is nil or empty.")
                self.hideActivityIndicator()
                // or break, continue, throw
                let alert = UIAlertController(title: "Add Participant",message: "Participant is not available for the call.",preferredStyle: .alert)

                   alert.addAction(UIAlertAction(title: "Ok", style: .default, handler: nil))
                   self.present(alert, animated: true)
                return
            }
            
            
            guard let data = jsonString?.data(using: .utf8) else {
                return
            }
            do {
                
                if(jsonString == "[]")
                {
                    self.hideActivityIndicator()
                    let alert = UIAlertController(title: "Add Participent",message: "Participant is not available for the call.",preferredStyle: .alert)

                       alert.addAction(UIAlertAction(title: "Ok", style: .default, handler: nil))
                       self.present(alert, animated: true)
                }
                else
                {
                let json = try! JSONSerialization.jsonObject(with: data, options: .allowFragments)
                self.hideActivityIndicator()
                if let myJSON = json as? [Any]
                {
                self.hideActivityIndicator()
                self.isthirdPartyList = true
                print("get json",myJSON)
                self.participentArray = []
                self.namesArray = []
                self.idsArray = []
                self.participentArray = myJSON
                for item in self.participentArray {
                    
                    let dict = item as! NSDictionary
                    self.namesArray.append(dict["full_name"] as! String)
                    self.idsArray.append(dict["user_id"] as! NSNumber)
                }
                    self.namesArray = self.namesArray.removeDuplicates()
                    self.idsArray = self.idsArray.removeDuplicates()
                    
//                    let range = ("Add Participent" as NSString).range(of: "Add Participent")
//                    let mutableAttributedString = NSMutableAttributedString.init(string: "Add Participent")
//                    mutableAttributedString.addAttribute(NSAttributedStringKey.foregroundColor, value: UIColor.white, range: range)

                    self.showPopOver(namesarray: self.namesArray, idsarray: self.idsArray,roles: [],titleS: "Add Participant")
 
                }
                else
                {
                    let alert = UIAlertController(title: "Add Participant",message: "Participant is not available for the call.",preferredStyle: .alert)

                       alert.addAction(UIAlertAction(title: "Ok", style: .default, handler: nil))
                       self.present(alert, animated: true)
                }
                }
                
            } catch let error as NSError {
                print(error)
            }
            
        }
        
        
        participantCollectionView.dataSource = self
        participantCollectionView.delegate = self
        participantCollectionView.register(ParticipantCell.self)

        disableMicButton.didToggle = { [weak self] in self?.viewModel.isMicOn = !$0 }
        disableCameraButton.didToggle = { [weak self] in
            self?.viewModel.isCameraOn = !$0
            self?.updateView()
        }

        viewModel.delegate = self
        viewModel.connect(accseesToken: self.tokenIS)
        statsViewController.addAsSwipeableView(toParentViewController: self)
        NotificationCenter.default.addObserver(self, selector: #selector(self.methodOfReceivedNotification(notification:)), name: Notification.Name("NotificationIdentifier"), object: nil)
    }
    
    func showPopOver(namesarray:[String],idsarray:[NSNumber],roles:[String],titleS:String)
    {
        isParticipantInCall = false
        StringPickerPopover(title: titleS, choices: namesarray)
            .setSelectedRow(0)
            .setValueChange(action: { _, selectedDate,_ in
                
                print("selected",selectedDate)
//
//                        DispatchQueue.main.async {
//                            let payload:[String:Any] = [
//                                "selectedParticipantID": self.idsArray[selectedDate],
//                                "selectedParticipantName": self.namesArray[selectedDate],
//                                "roomName":self.userRoomname]
//                            let nc = NotificationCenter.default
//                            nc.post(name:Notification.Name("sendNotification"), object: nil, userInfo: payload)
//                        }
            })
            .setDoneButton(action: { (popover, selectedRow, selectedString) in
                print("done row \(selectedRow) \(selectedString)")
            
                if(titleS != "Add Participant")
                {
                DispatchQueue.main.async {
                    
                    for participant in self.viewModel.data.participants {
                        let identity = participant.identity
                        let splityArray1 = identity.split(separator: "@")
                        var roleIs = ""
                        if(roles[selectedRow] == "doctor")
                        {
                            roleIs = "practitioner"
                        }
                        else if  (roles[selectedRow] == "responder")
                        {
                            roleIs = "responder"
                        }
                        else
                        {
                            roleIs = "patient"
                        }
                        let userIdentity = "\(splityArray1[0])@\(splityArray1[1])"
                        let name = "\(namesarray[selectedRow])@\(roleIs)"
                        if(userIdentity == name)
                        {
                            self.isParticipantInCall = true
                            let alert = UIAlertController(title: titleS ,message: "Participant already on call.",preferredStyle: .alert)

                               alert.addAction(UIAlertAction(title: "Ok", style: .default, handler: nil))
                               self.present(alert, animated: true)
                            break
                        }
                    }
                    
                    if (self.isParticipantInCall == false)
                    {
//                        let payload:[String:Any] = [
//                            "selectedParticipantID": idsarray[selectedRow],
//                            "selectedParticipantName": namesarray[selectedRow],
//                            "roomName":self.userRoomname,"roomSid":Constants.roomSID]
                        
                        let localParticipant = self.viewModel.data.participants[0].identity
                                let splityArray1 = localParticipant.split(separator: "@")
                                let localrole = String(splityArray1[1])
                                var role = ""
                                if(localrole == "MHT")
                                {
                                    role = "doctor"
                                }
                                else if(localrole == "CCT")
                                {
                                    role = "responder"
                                }
                        
                        let params = ["roomName": self.userRoomname,
                                      "roomSid": Constants.roomSID,
                                      "identity":namesarray[selectedRow],
                                      "user_type":role,
                                      "senderId":splityArray1[3],
                                      "called_in":"participant_list",
                                      "receivedId":idsarray[selectedRow]] as [String : Any]
                        var request = URLRequest(url: URL(string: "\(Constants.BaseURL)/Users/sendnotification")!)
                        request.httpMethod = "POST"
                             request.httpBody = try? JSONSerialization.data(withJSONObject: params, options: [])
                             request.addValue("application/json", forHTTPHeaderField: "Content-Type")
                            request.setValue("token=\"\(splityArray1[2])\"", forHTTPHeaderField: "Authorization")
                             let session = URLSession.shared
                             let task = session.dataTask(with: request, completionHandler: { [self] data, response, error -> Void in
                                 print(response!)
                                 do {
                                     let json = try JSONSerialization.jsonObject(with: data!) as! [String:Any]
                                     print("json",json)
                                    
                                 } catch {
                                     print("error")
                                 }
                             })
                     
                             task.resume()
                        
                        
                        
                        
                       // self.sendAPI(participantID:idsarray[selectedRow],participantName:namesarray[selectedRow])
//                        let nc = NotificationCenter.default
//                        nc.post(name:Notification.Name("sendNotification"), object: nil, userInfo: payload)
                    }
                }
                }
                else
                {
                    
                        let payload:[String:Any] = [
                            "selectedParticipantID": idsarray[selectedRow],
                            "selectedParticipantName": namesarray[selectedRow],
                            "roomName":self.userRoomname,"roomSid":Constants.roomSID]
                        let nc = NotificationCenter.default
                        nc.post(name:Notification.Name("sendNotification"), object: nil, userInfo: payload)
                
                }
                
            })
            .setCancelButton(action: { (_, _, _) in print("cancel")}
            )
            .appear(originView: self.addButton!, baseViewController: self)
        
    }
    
    
    //MARK: - - - - - Method for receiving Data through Post Notificaiton - - - - -

            @objc func methodOfReceivedNotification(notification: Notification) {
                

                print("Value of notification : ", notification.object ?? "")
                
                DispatchQueue.main.async {

                    self.viewModel.disconnect()

                }
                self.dismiss(animated: true, completion: nil)
                self.sucesspayload = ["callSuccess":true,"roomName":self.userRoomname,"roomSid":Constants.roomSID]
                let nc = NotificationCenter.default
              nc.post(name:Notification.Name("aftercallends"), object: nil, userInfo: self.sucesspayload)

//                leaveDelegate.LeaveTappped()
                self.leaveButtonTapped(self)


            }


    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.navigationItem.hidesBackButton = true
//        self.navigationController!.navigationBar.barTintColor = UIColor.white
//        self.navigationController!.navigationBar.tintColor = .white
//        self.navigationController!.navigationBar.titleTextAttributes = [
//            NSAttributedStringKey.foregroundColor : UIColor.white
//        ]
        application.isIdleTimerDisabled = true
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        
        application.isIdleTimerDisabled = false
    }
    
    @IBAction func leaveButtonTapped(_ sender: Any) {
        
        // Making an alert in RoomViewController //
        print("Avinash -- Leave Button Tapped")
        
        // Making an alert for roomname and token //
        Constants.isParticipantJOIN = false
        Constants.LeaveparticipantStatus = true;
        
        DispatchQueue.main.async {
            self.viewModel.disconnect()
        }
       
        self.dismiss(animated: true, completion: nil)
//        leaveDelegate.LeaveTappped()
        self.sucesspayload = ["callSuccess":true,"roomName":self.userRoomname,"roomSid":Constants.roomSID]
        let nc = NotificationCenter.default
      nc.post(name:Notification.Name("aftercallends"), object: nil, userInfo: self.sucesspayload)
//        AppCenter.start(withAppSecret: "{Your App Secret}", services: [Analytics.self, Crashes.self])
        //navigationController?.popViewController(animated: true)
    }
    
    @IBAction func switchCameraButtonTapped(_ sender: Any) {
        viewModel.cameraPosition = viewModel.cameraPosition == .front ? .back : .front
    }
    
    private func updateView() {
        roomNameLabel.text = viewModel.data.roomName
        disableMicButton.isSelected = !viewModel.isMicOn
        disableCameraButton.isSelected = !viewModel.isCameraOn
        switchCameraButton.isEnabled = viewModel.isCameraOn
        let participant = viewModel.data.mainParticipant
        mainVideoView.configure(config: participant.videoConfig)
        let identity = participant.identity
        let splityArray = identity.split(separator: "@")
        
        
        mainIdentityLabel.text = splityArray[0].description
        
        recordingView.isHidden = !viewModel.data.isRecording
       
        mainIdentityLabel.text = splityArray[0].description
        recordingView.isHidden = !viewModel.data.isRecording

        
        if (loginUser != "patient" && viewModel.data.participants.count == 2)
        {
            self.addButton.isHidden = false
        }
        else
        {
            self.addButton.isHidden = true
           
        }
        print("status",Constants.isParticipantJOIN)

    }
    func completeRoomAPI(name:String)
        {
           
            Constants.LeaveparticipantStatus = false;
            let params = ["sid":Constants.roomSID] as Dictionary<String, String>
            var request = URLRequest(url: URL(string: "\(Constants.BaseURL)/twilio-video/completeRoom")!)
            MSAnalytics.trackEvent("completeRoomAPI Calling Name:\((name , params ,"\(Constants.BaseURL)/twilio-video/completeRoom")) \(Date().string(format: "MM/dd/yyyy HH:mm:ss"))");
            request.httpMethod = "POST"
                 request.httpBody = try? JSONSerialization.data(withJSONObject: params, options: [])
                 request.addValue("application/json", forHTTPHeaderField: "Content-Type")
         
                 let session = URLSession.shared
                 let task = session.dataTask(with: request, completionHandler: { [self] data, response, error -> Void in
                     print(response!)
                     do {
                         let json = try JSONSerialization.jsonObject(with: data!) as! [String:Any]
                         print("json",json)
//                         let data = json["data"] as! [String:Any]
//
//                         if(data["error"] as! Bool == false)
//                         {
//                             let button = UIButton()
//
//                         }
//                         let alert = UIAlertController(title: "",message: data["message"] as? String ,preferredStyle: .alert)
//
//                            alert.addAction(UIAlertAction(title: "Ok", style: .default, handler: nil))
//                            self.present(alert, animated: true)
//                         print("token is ", data["token"]!)
//                         self.tokenIS = data["token"] as! String
         //                viewModel.delegate = self
         //                viewModel.connect()
         
                        
                     } catch {
                         print("error")
                     }
                 })
         
                 task.resume()
            
        }
    func showActivityIndicator() {
        DispatchQueue.main.async {
            self.loadingView = UIView()
            self.loadingView.frame = CGRect(x: 0.0, y: 0.0, width: 100.0, height: 100.0)
            self.loadingView.center = self.view.center
            self.loadingView.backgroundColor = UIColor(named: "#444444")
            self.loadingView.alpha = 0.7
            self.loadingView.clipsToBounds = true
            self.loadingView.layer.cornerRadius = 10

            self.spinner = UIActivityIndicatorView(activityIndicatorStyle: .whiteLarge)
            self.spinner.frame = CGRect(x: 0.0, y: 0.0, width: 80.0, height: 80.0)
            self.spinner.center = CGPoint(x:self.loadingView.bounds.size.width / 2, y:self.loadingView.bounds.size.height / 2)

            self.loadingView.addSubview(self.spinner)
            self.view.addSubview(self.loadingView)
            self.spinner.startAnimating()
        }
    }

    func hideActivityIndicator() {
        DispatchQueue.main.async {
            self.spinner.stopAnimating()
            self.loadingView.removeFromSuperview()
        }
    }
    
    @IBAction func addButtonTapped(_ sender: Any) {
        print("participents",viewModel.data.participants)
        self.showActivityIndicator()
//        var isDoctor = false
       
//        var isResponder = false

        for participant in self.viewModel.data.participants {
            let identity = participant.identity
            let splityArray = identity.split(separator: "@")
            let role = String(splityArray[1])
            if(role == "patient")
            {
                isPatient = true
                break
            }
        }
        if(isPatient == true)
        {
            roleseArray = ["MHT","CCT","Participants list"]
        }
        else
        {
            roleseArray = ["Patient","MHT","CCT","Participants list"]
        }
        if( Constants.isthereParticipantJOIN == true)
        {
            roleseArray = ["Participants list"]
        }
        
        self.hideActivityIndicator()
//        let range = ("Select Roles" as NSString).range(of: "Select Roles")
//        let mutableAttributedString = NSMutableAttributedString.init(string: "Select Roles")
//        mutableAttributedString.addAttribute(NSAttributedStringKey.foregroundColor, value: UIColor.white, range: range)

        
        StringPickerPopover(title:"Select Role", choices: roleseArray)
            .setSelectedRow(0)
            .setValueChange(action: { _, selectedDate,_ in
                
                print("selected",selectedDate)
                
            
//                self.selectedRole = self.roleseArray[selectedDate]
////                let localParticipant = self.viewModel.data.participants[0].identity
//                let remoteParticipant = self.viewModel.data.participants[1].identity
////                  let splityArray = localParticipant.split(separator: "@")
////                  let localrole = String(splityArray[1])
//                    let splityArray1 = remoteParticipant.split(separator: "@")
//                    let remotelrole = String(splityArray1[1])
//
//                self.payload = [
//                    "requiredPartcipant": self.selectedRole,
//                                   "presentParticipantRole":remotelrole,
//                                    "presentParticipantID":"3",
//                                         "data":"test",
//                                         "valid": true,
//                                         "child":[ "name": "joker" ]
//                                     ]
//                let nc = NotificationCenter.default
//              nc.post(name:Notification.Name("callingThirdParticipant"), object: nil, userInfo: self.payload)
            })
            .setDoneButton(action: { (popover, selectedRow, selectedString) in
                print("done row \(selectedRow) \(selectedString)")
                var roleIS = ""
                if(self.roleseArray[selectedRow] == "MHT")
                {
                    roleIS = "practitioner"
                }
                else if(self.roleseArray[selectedRow] == "CCT")
                {
                    roleIS = "responder"
                }
                else
                {
                    roleIS = "patient"
                }
                if(self.roleseArray[selectedRow] == "Participants list")
                {
                    //calling API
                    
                
                    
                    self.dropedParticipantAPI(rommname: self.userRoomname)
                    
                    
                }
                else
                {
                    
                self.selectedRole = roleIS
              //  let localParticipant = self.viewModel.data.participants[0].identity
                let remoteParticipant = self.viewModel.data.participants[1].identity
//                  let splityArray = localParticipant.split(separator: "@")
//                  let localrole = String(splityArray[1])
                    let splityArray1 = remoteParticipant.split(separator: "@")
                    let remotelrole = String(splityArray1[1])

                self.payload = [
                    "requiredPartcipant": self.selectedRole,
                                   "presentParticipantRole":remotelrole,
                                    "roomName":self.userRoomname,
                                    "roomSid":Constants.roomSID,
                                    "presentParticipantID":"3",
                                         "data":"test",
                                         "valid": true,
                                         "child":[ "name": "joker" ]
                                     ]
                let nc = NotificationCenter.default
              nc.post(name:Notification.Name("callingThirdParticipant"), object: nil, userInfo: self.payload)
                }
                
            })
            .setCancelButton(action: { (_, _, _) in print("cancel")}
            )
            .appear(originView: self.addButton!, baseViewController: self)
        
        
        
        
        
//        DispatchQueue.main.async {
//
//
////                print("identity",participant.identity)
////                let identity = participant.identity
//
//            print(self.payload )
//
//
//        }
        
    }
    
//    func sendAPI(participantID:NSNumber,participantName:String)
//    {
//        let localParticipant = self.viewModel.data.participants[0].identity
//        let splityArray1 = localParticipant.split(separator: "@")
//        let localrole = String(splityArray1[1])
//        var role = ""
//        if(localrole == "MHT")
//        {
//            role = "doctor"
//        }
//        else if(localrole == "CCT")
//        {
//            role = "responder"
//        }
//        let params = ["roomName": self.userRoomname,
//
//                      "roomSid": Constants.roomSID,
//
//                      "identity":participantName,
//
//                      "user_type":role,
//
//                      "senderId":splityArray1[3],
//
//                      "receivedId":participantID] as Dictionary<String, String>
//
//        var request = URLRequest(url: URL(string: "\(Constants.BaseURL)/Users/sendnotification")!)
//        request.httpMethod = "POST"
//        request.httpBody = try? JSONSerialization.data(withJSONObject: params, options: [])
//        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
//        request.setValue("token=\"\(splityArray1[2])\"", forHTTPHeaderField: "Authorization")
////        request.addValue("Authorization", forHTTPHeaderField: "Content-Type")
//
//        let session = URLSession.shared
//        let task = session.dataTask(with: request, completionHandler: { [self] data, response, error -> Void in
//            print(response!)
//            do {
//                let json = try JSONSerialization.jsonObject(with: data!) as! [String:Any]
//                print("json",json)
////                let data = json["data"] as! [String:Any]
////                print("token is ", data["token"]!)
////                self.tokenIS = data["token"] as! String
////                viewModel.delegate = self
////                viewModel.connect()
//
////                DispatchQueue.main.async {
////                    viewModel.delegate = self
////                    viewModel.connect(accseesToken: self.tokenIS)
////                    statsViewController.addAsSwipeableView(toParentViewController: self)
////
////                    updateView()
////                }
//
////                updateView()
//            } catch {
//                print("error")
//            }
//        })
//
//        task.resume()
//
//    }
    
    func dropedParticipantAPI(rommname:String)
    {
       
        //create the url with NSURL
           let url = URL(string: "\(Constants.BaseURL)/AvailabilityLogs/by-room-name?roomName=\(rommname)")!

           //create the session object
           let session = URLSession.shared

           //now create the URLRequest object using the url object
           let request = URLRequest(url: url)

           //create dataTask using the session object to send data to the server
           let task = session.dataTask(with: request as URLRequest, completionHandler: { data, response, error in

               guard error == nil else {
                   return
               }

               guard let data = data else {
                   return
               }

              do {
                 //create json object from data
                 if let json = try JSONSerialization.jsonObject(with: data, options: .mutableContainers) as? [String: Any] {
                    print(json)
                     
                     self.participentArray = []
                     self.namesArray = []
                     self.idsArray = []
                     self.rolesArray = []
                    // let data =
                     self.participentArray = json["data"] as! [Any]
                     
                     for item in self.participentArray {

                         let dict = item as! NSDictionary
                         self.namesArray.append(dict["full_name"] as! String)
                         self.idsArray.append(dict["user_id"] as! NSNumber)
                         self.rolesArray.append(dict["role"] as! String)
                     }
                         self.namesArray = self.namesArray.removeDuplicates()
                         self.idsArray = self.idsArray.removeDuplicates()
                     DispatchQueue.main.async {
                         self.showPopOver(namesarray: self.namesArray, idsarray: self.idsArray,roles:self.rolesArray,titleS: "Participants list")
                     }
                     
//
     //                    let range = ("Add Participent" as NSString).range(of: "Add Participent")
     //                    let mutableAttributedString = NSMutableAttributedString.init(string: "Add Participent")
     //                    mutableAttributedString.addAttribute(NSAttributedStringKey.foregroundColor, value: UIColor.white, range: range)

                    
//                     StringPickerPopover(title: "Droped Participant", choices: self.namesArray)
//                         .setSelectedRow(0)
//                         .setValueChange(action: { _, selectedDate,_ in
//
//                             print("selected",selectedDate)
     //
     //                        DispatchQueue.main.async {
     //                            let payload:[String:Any] = [
     //                                "selectedParticipantID": self.idsArray[selectedDate],
     //                                "selectedParticipantName": self.namesArray[selectedDate],
     //                                "roomName":self.userRoomname]
     //                            let nc = NotificationCenter.default
     //                            nc.post(name:Notification.Name("sendNotification"), object: nil, userInfo: payload)
     //                        }
//                         })
//                         .setDoneButton(action: { (popover, selectedRow, selectedString) in
//                             print("done row \(selectedRow) \(selectedString)")
//                             DispatchQueue.main.async {
//                                 let payload:[String:Any] = [
//                                     "selectedParticipantID": self.idsArray[selectedRow],
//                                     "selectedParticipantName": self.namesArray[selectedRow],
//                                     "roomName":self.userRoomname,"roomSid":Constants.roomSID]
//                                 let nc = NotificationCenter.default
//                                 nc.post(name:Notification.Name("sendNotification"), object: nil, userInfo: payload)
//                             }
//                         })
//                         .setCancelButton(action: { (_, _, _) in print("cancel")}
//                         )
//                         .appear(originView: self.addButton!, baseViewController: self)
//
                 }
              } catch let error {
                print(error.localizedDescription)
              }
           })

           task.resume()
    }
   
    @objc func cancelButtonClicked(_ button:UIBarButtonItem!){
        print("Done clicked")
    }
}

extension RoomViewController: RoomViewModelDelegate {
    func didConnect() {
        updateView()
    }
    
    func didFailToConnect(error: Error) {
        showError(error: error) { [weak self] in self?.navigationController?.popViewController(animated: true) }
    }
    
    func didDisconnect(error: Error?) {
        if let error = error {
            showError(error: error) { [weak self] in self?.navigationController?.popViewController(animated: true) }
        } else {
            navigationController?.popViewController(animated: true)
        }
    }

    func didUpdateList(diff: ListIndexSetResult) {
        participantCollectionView.performBatchUpdates(
            {
                participantCollectionView.insertItems(at: diff.inserts.indexPaths)
                participantCollectionView.deleteItems(at: diff.deletes.indexPaths)
                diff.moves.forEach { move in
                    participantCollectionView.moveItem(
                        at: IndexPath(item: move.from, section: 0),
                        to: IndexPath(item: move.to, section: 0)
                    )
                }
            },
            completion: nil
        )
    }

    func didUpdateParticipant(at index: Int) {
        guard let cell = participantCollectionView.cellForItem(at: IndexPath(item: index, section: 0)) as? ParticipantCell else { return }
        let localParticipant = self.viewModel.data.participants[0].identity
       // let splityArray = localParticipant.split(separator: "@")
        cell.configure(participant: viewModel.data.participants[index],localParticipantStr: localParticipant)
    }
    
    func didUpdateMainParticipant() {
        updateView()
    }

    func didUpdateRecording() {
        updateView()
    }
}

extension RoomViewController: UICollectionViewDataSource {
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        viewModel.data.participants.count
    }
    
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        let cell = collectionView.dequeueReusableCell(withReuseIdentifier: ParticipantCell.identifier, for: indexPath) as! ParticipantCell
        let localParticipant = self.viewModel.data.participants[0].identity
        cell.configure(participant: viewModel.data.participants[indexPath.item], localParticipantStr: localParticipant)
        return cell
    }
}

extension RoomViewController: UICollectionViewDelegate {
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        viewModel.togglePin(at: indexPath.item)
    }
}
extension Array where Element:Equatable {
    func removeDuplicates() -> [Element] {
        var result = [Element]()

        for value in self {
            if result.contains(value) == false {
                result.append(value)
            }
        }

        return result
    }
}
