package com.kumazaku.pdfcreator.exceptions;

/**
 * Created by kuma on 2015/02/21.
 */
public class SizeTooLowException extends Exception {
    public SizeTooLowException() {
    }

    public SizeTooLowException(String detailMessage) {
        super(detailMessage);
    }
}
