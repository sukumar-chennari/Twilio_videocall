package src.cordova.plugin.videocall.CompositeDisposableExtensions

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

operator fun CompositeDisposable.plus(newDisposable: Disposable) = add(newDisposable)
