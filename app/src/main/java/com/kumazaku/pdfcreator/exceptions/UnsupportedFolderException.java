package com.kumazaku.pdfcreator.exceptions;

/**
 * Created by kuma on 2015/07/10.
 */
public class UnsupportedFolderException extends Exception {


    public UnsupportedFolderException() {
    }

    public UnsupportedFolderException(String detailMessage) {
        super(detailMessage);
    }
}
