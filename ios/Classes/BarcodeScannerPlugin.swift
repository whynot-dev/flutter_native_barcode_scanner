import Flutter
import UIKit

public class BarcodeScannerPlugin: NSObject, FlutterPlugin {
    
    private var cameraController: BarcodeScannerController
    
    init(cameraController: BarcodeScannerController) {
        self.cameraController = cameraController
    }
    
    /// Flutter ~3.38+ exposes `viewController` on the concrete registrar object, but older SDKs
    /// keep `FlutterPluginRegistrar` without that property (compile error on Flutter 3.35.x).
    /// KVC keeps one source compiling across versions; at runtime `responds(to:)` is false on old embedders.
    private static func registrarViewController(_ registrar: FlutterPluginRegistrar) -> UIViewController? {
        guard let obj = registrar as? NSObject else { return nil }
        let key = "viewController"
        guard obj.responds(to: NSSelectorFromString(key)) else { return nil }
        return obj.value(forKey: key) as? UIViewController
    }

    private static func resolveMainUIController(with registrar: FlutterPluginRegistrar) -> UIViewController? {
        if let vc = registrarViewController(registrar) {
            return vc
        }

        // Pre-UIScene / classic Flutter: `FlutterAppDelegate.window` is usually set.
        if let window = (UIApplication.shared.delegate as? FlutterAppDelegate)?.window {
            if let root = window.rootViewController {
                return root
            }
        }

        // UIScene: window may live on the scene, not on AppDelegate.
        if #available(iOS 13.0, *) {
            let scenes = UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
            let allWindows = scenes.flatMap { $0.windows }
            return allWindows.first(where: { $0.isKeyWindow })?.rootViewController
                ?? allWindows.first?.rootViewController
        }

        return UIApplication.shared.keyWindow?.rootViewController
    }

    public static func register(with registrar: FlutterPluginRegistrar) {

        let viewController = resolveMainUIController(with: registrar) ?? UIViewController()
        let cameraController = BarcodeScannerController()
        let instance = BarcodeScannerPlugin(cameraController: cameraController)
        let factory = BarcodeScannerViewFactory(mainUIController: viewController, cameraController: cameraController)

        registrar.register(factory, withId: "be.freedelity/native_scanner/view")
        
        let channel = FlutterMethodChannel(name: "be.freedelity/native_scanner/method", binaryMessenger: registrar.messenger())
        registrar.addMethodCallDelegate(instance, channel: channel)
        
        let eventChannel = FlutterEventChannel(name: "be.freedelity/native_scanner/imageStream", binaryMessenger: registrar.messenger())
        eventChannel.setStreamHandler(cameraController)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        cameraController.handle(call, result: result)
    }
}
