package com.justdoit.arcmenu

import android.support.annotation.NonNull
import io.reactivex.Observer
import io.reactivex.disposables.Disposable

abstract class CommonObserver<T> : Observer<T> {

    override fun onSubscribe(@NonNull d: Disposable) {
    }

    override fun onComplete() {
    }
}
