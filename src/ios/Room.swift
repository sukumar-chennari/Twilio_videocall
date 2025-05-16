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

import TwilioVideo
import AppCenter
import AppCenterDistribute
import AppCenterAnalytics
import UIKit
@objc class Room:NSObject {
    enum Update {
        case didStartConnecting
        case didConnect
        case didFailToConnect(error: Error)
        case didDisconnect(error: Error?)
        case didAddRemoteParticipants(participants: [Participant])
        case didRemoveRemoteParticipants(participants: [Participant])
        case didUpdateParticipants(participants: [Participant])
        case didStartRecording
        case didStopRecording
    }
    var delegate : roomViewControllerDelegate?
       
    let localParticipant: LocalParticipant
    var isRecording: Bool { room?.isRecording ?? false }
    private(set) var remoteParticipants: [RemoteParticipant] = []
    private(set) var state: RoomState = .disconnected
    @objc private(set) var room: TwilioVideo.Room? // Only exposed for stats and should not be used for anything else
    private let accessTokenStore: TwilioAccessTokenStoreReading
    private let connectOptionsFactory: ConnectOptionsFactory
    private let notificationCenter: NotificationCenter
    private let twilioVideoSDKType: TwilioVideoSDK.Type
    
   
    init(
        localParticipant: LocalParticipant,
        accessTokenStore: TwilioAccessTokenStoreReading,
        connectOptionsFactory: ConnectOptionsFactory,
        notificationCenter: NotificationCenter,
        twilioVideoSDKType: TwilioVideoSDK.Type
    ) {
        self.localParticipant = localParticipant
        self.accessTokenStore = accessTokenStore
        self.connectOptionsFactory = connectOptionsFactory
        self.notificationCenter = notificationCenter
        self.twilioVideoSDKType = twilioVideoSDKType
        super.init()
        localParticipant.delegate = self
    }

    func dismis() {
          self.delegate?.LeaveTappped()
       }
    func connect(roomName: String,tokenIS:String) {
        print("Avinash -- connect")
        guard state == .disconnected else { fatalError("Connection already in progress.") }

        state = .connecting
//        post(.didStartConnecting)
        let options = self.connectOptionsFactory.makeConnectOptions(
            accessToken: tokenIS,
            roomName: roomName,
            audioTracks: [self.localParticipant.micTrack].compactMap { $0 },
            videoTracks: [self.localParticipant.localCameraTrack].compactMap { $0 }
        )
        self.room = self.twilioVideoSDKType.connect(options: options, delegate: self)

//        accessTokenStore.fetchTwilioAccessToken(roomName: roomName) { [weak self] result in
//            guard let self = self else { return }
//
//            switch result {
//            case let .success(accessToken):
//                let options = self.connectOptionsFactory.makeConnectOptions(
//                    accessToken: accessToken,
//                    roomName: roomName,
//                    audioTracks: [self.localParticipant.micTrack].compactMap { $0 },
//                    videoTracks: [self.localParticipant.localCameraTrack].compactMap { $0 }
//                )
//                self.room = self.twilioVideoSDKType.connect(options: options, delegate: self)
//            case let .failure(error):
//                self.state = .disconnected
//                self.post(.didFailToConnect(error: error))
//            }
        //}
    }

    func disconnect() {
        print("Avinash -- disconnect")
        room?.disconnect()
    }
    
    func togglePin(participant new: Participant) {
        let allParticipants: [Participant] = [localParticipant] + remoteParticipants
        let old = allParticipants.first(where: { $0.isPinned })
        
        if let old = old, old !== new {
            old.isPinned = false
        }
        
        new.isPinned.toggle()
        post(.didUpdateParticipants(participants: [old, new].compactMap { $0 }))
    }
    
    private func post(_ update: Update) {
        notificationCenter.post(name: .roomUpdate, object: self, payload: update)
    }
}

extension Room: TwilioVideo.RoomDelegate {
    func roomDidConnect(room: TwilioVideo.Room) {
        print("Avinash -- roomDidConnect")
        localParticipant.participant = room.localParticipant
        Constants.roomSID = room.sid
        remoteParticipants = room.remoteParticipants.map {
            RemoteParticipant(participant: $0, delegate: self)
        }
        state = .connected
        post(.didConnect)
        Constants.isthereParticipantJOIN = false
        if(room.remoteParticipants.count == 2)
        {
            Constants.isthereParticipantJOIN = true
        }
       
        
        if (remoteParticipants.count >= 1)
        {
            Constants.isParticipantJOIN = true
            print("roomDidConnect :", Constants.isParticipantJOIN)
        }
        
        if !remoteParticipants.isEmpty {
            post(.didAddRemoteParticipants(participants: remoteParticipants))
        }
    }
    
    func roomDidFailToConnect(room: TwilioVideo.Room, error: Error) {
        print("Avinash -- roomDidFailToConnect")
            state = .disconnected

        MSAnalytics.trackEvent("Name:\(String(describing: room.localParticipant?.identity)),roomDidFailToConnect: ,\(error),\(Date().string(format: "MM/dd/yyyy HH:mm:ss"))");


            post(.didFailToConnect(error: error))

        }

    
    func roomDidDisconnect(room: TwilioVideo.Room, error: Error?) {
        print("Avinash -- roomDidDisconnect")
        localParticipant.participant = nil
        let errorMsg = error?.localizedDescription
        MSAnalytics.trackEvent("Name:\(String(describing: room.localParticipant?.identity)),roomDidDisconnect: ,\(String(describing: errorMsg)) \(Date().string(format: "MM/dd/yyyy HH:mm:ss"))");
        Constants.isParticipantJOIN = false
        
        let participants = remoteParticipants
        remoteParticipants.removeAll()
        state = .disconnected
        post(.didDisconnect(error: error))
        
        if !remoteParticipants.isEmpty {
            post(.didRemoveRemoteParticipants(participants: participants))
        }
    }
    
    func participantDidConnect(room: TwilioVideo.Room, participant: TwilioVideo.RemoteParticipant) {
        Constants.isParticipantJOIN = true
        if(room.remoteParticipants.count == 2)
        {
            Constants.isthereParticipantJOIN = true
        }
        
        remoteParticipants.append(RemoteParticipant(participant: participant, delegate: self))
    
        post(.didAddRemoteParticipants(participants: [remoteParticipants.last!]))
    }
    
    func participantDidDisconnect(room: TwilioVideo.Room, participant: TwilioVideo.RemoteParticipant) {
        MSAnalytics.trackEvent("REMOTE PARTCICPANT DISCONNECTED");
        print("remote participant disconnected");
        
        if(room.remoteParticipants.count < 1 && Constants.isParticipantJOIN == true)
        {
            self.completeRoomAPI(name: "")
            let objToBeSent = "Test Message from Notification"
        
            
            NotificationCenter.default.post(name: Notification.Name("NotificationIdentifier"), object: objToBeSent)
            Constants.isthereParticipantJOIN = false
        }
        guard let index = remoteParticipants.firstIndex(where: { $0.identity == participant.identity }) else { return }
        
        post(.didRemoveRemoteParticipants(participants: [remoteParticipants.remove(at: index)]))
    }
    
    func dominantSpeakerDidChange(room: TwilioVideo.Room, participant: TwilioVideo.RemoteParticipant?) {
        guard let new = remoteParticipants.first(where: { $0.identity == participant?.identity }) else { return }

        let old = remoteParticipants.first(where: { $0.isDominantSpeaker })
        old?.isDominantSpeaker = false
        new.isDominantSpeaker = true
        post(.didUpdateParticipants(participants: [old, new].compactMap { $0 }))
    }
    
    func roomDidStartRecording(room: TwilioVideo.Room) {
        post(.didStartRecording)
    }
    
    func roomDidStopRecording(room: TwilioVideo.Room) {
        post(.didStopRecording)
    }
}

extension Room: ParticipantDelegate {
    func didUpdate(participant: Participant) {
        post(.didUpdateParticipants(participants: [participant]))
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
}

