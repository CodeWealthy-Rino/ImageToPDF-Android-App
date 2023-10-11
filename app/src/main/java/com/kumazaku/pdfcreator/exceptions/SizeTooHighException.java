package com.kumazaku.pdfcreator.exceptions;

/**
 * Created by kuma on 2015/02/21.
 */
public class SizeTooHighException extends Exception {
    public SizeTooHighException() {
    }

    public SizeTooHighException(String detailMessage) {
        super(detailMessage);
    }
}