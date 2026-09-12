import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    let root: RootComponent
    let backDispatcher: BackDispatcher
    let onFirstVisible: (UIViewController) -> Void

    func makeUIViewController(context: Context) -> UIViewController {
        let styleRelay = StatusBarStyleRelay()
        let controller = MainViewControllerKt.MainViewController(
            root: root,
            backDispatcher: backDispatcher,
            onSystemChromeChanged: { darkIcons in
                styleRelay.update(darkIcons: darkIcons.boolValue)
            }
        )

        if #available(iOS 15.0, *) {
            controller.view.minimumContentSizeCategory = .large
            controller.view.maximumContentSizeCategory = .large
        }

        let container = VisibleComposeViewController(
            contentController: controller,
            onFirstVisible: onFirstVisible
        )
        styleRelay.owner = container
        return container
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

private final class StatusBarStyleRelay {
    weak var owner: VisibleComposeViewController?

    func update(darkIcons: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.owner?.updateStatusBarStyle(darkIcons: darkIcons)
        }
    }
}

private final class VisibleComposeViewController: UIViewController {
    private let contentController: UIViewController
    private let onFirstVisible: (UIViewController) -> Void
    private var didNotifyVisible = false
    private var darkStatusBarIcons = true

    override var preferredStatusBarStyle: UIStatusBarStyle {
        darkStatusBarIcons ? .darkContent : .lightContent
    }

    override var childForStatusBarStyle: UIViewController? { nil }

    init(contentController: UIViewController, onFirstVisible: @escaping (UIViewController) -> Void) {
        self.contentController = contentController
        self.onFirstVisible = onFirstVisible
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        addChild(contentController)
        view.addSubview(contentController.view)
        contentController.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            contentController.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            contentController.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            contentController.view.topAnchor.constraint(equalTo: view.topAnchor),
            contentController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])
        contentController.didMove(toParent: self)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        notifyVisibleIfNeeded()
    }

    func updateStatusBarStyle(darkIcons: Bool) {
        guard darkStatusBarIcons != darkIcons else { return }
        darkStatusBarIcons = darkIcons
        setNeedsStatusBarAppearanceUpdate()
    }

    private func notifyVisibleIfNeeded() {
        guard !didNotifyVisible else { return }
        didNotifyVisible = true
        onFirstVisible(self)
    }
}
