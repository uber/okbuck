package com.uber.okbuck.core.dependency

class InValidDependencyException extends IllegalStateException {

    InValidDependencyException(String msg) {
        super(msg)
    }
}
