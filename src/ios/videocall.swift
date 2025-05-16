
@objc(videocall) class videocall : CDVPlugin{
// MARK: Properties
var pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
//    private var room: Room?
    private  var navigationController: UINavigationController?
@objc(coolMethod:) func coolMethod(_ command: CDVInvokedUrlCommand) {
var pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR)
    let roomName = command.arguments[0]  as? String
    let token = command.arguments[1]  as? String
    let identity = command.arguments[2]  as? String
    let id = command.arguments[3]  as? String
    
    // Making an alert for roomname and token //
    let alertView = UIAlertController(title: roomName, message: token, preferredStyle: .alert)

    alertView.addAction(UIAlertAction(title: "Done", style: .default, handler: nil))
    
    
    // Making an alert for roomname and token //
    
    DispatchQueue.main.async {
        // Background Thread
        let storyboard = UIStoryboard(name: "Main", bundle: nil)
       let lobyViewController = storyboard.instantiateViewController(withIdentifier: "lobbyViewController") as! LobbyViewController
//        let lobyViewController = LobbyViewController()
       
        
//        lobyViewController.userID = id!
        lobyViewController.userRoomName =  roomName!
        lobyViewController.userToken = token!
        lobyViewController.userIdentity = identity!
        print("Avinash -- Video Call")
        let navigationController = UINavigationController(rootViewController: lobyViewController)
        navigationController.modalPresentationStyle = .fullScreen
//                self.presentViewController(navigationController, animated: true, completion: nil)
        
      self.viewController?.present(navigationController, animated: false)
        
        //{ [self] in
//            let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: "ok")
//
//            commandDelegate.send(pluginResult, callbackId: command.callbackId)
//        }
    
    }
    }
    
}
