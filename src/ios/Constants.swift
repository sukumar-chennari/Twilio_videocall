//
//  Constants.swift
//  Cloud9
//
//  Created by Ramachandra on 11/08/21.
//

import Foundation

struct Constants {

    static var isParticipantJOIN: Bool = false
    static var isthereParticipantJOIN: Bool = false
    static var islevae: Bool = false
    static var iscallCount: Bool = false
    static var LeaveparticipantStatus : Bool = false
    static var roomSID:String = ""
//   static var BaseURL = "https://c9demo.cloud9download.com:8080/api"
  //  static var BaseURL = "https://mhid.cloud9download.com:8080/api"
//    static var BaseURL = "https://c9dev2.cloud9download.com:8080/api"
   static var BaseURL = "https://uat.cloud9download.com:8080/api"
//    static var BaseURL = "https://alaska.cloud9download.com:8080/api"
    
    func msg(message: String, title: String = "")
    {
        let alertView = UIAlertController(title: title, message: message, preferredStyle: .alert)

        alertView.addAction(UIAlertAction(title: "Done", style: .default, handler: nil))
        
        
//        UIApplication.shared().keyWindow?.rootViewController?.presentViewController(alertView, animated: true, completion: nil)
    }

}
