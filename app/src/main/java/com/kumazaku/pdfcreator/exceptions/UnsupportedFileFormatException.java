package com.kumazaku.pdfcreator.exceptions;

/**
 * サポートしてないファイル形式が指定された場合にスローされる。
 */
public class UnsupportedFileFormatException extends Exception {


    public UnsupportedFileFormatException() {
    }

    public UnsupportedFileFormatException(String detailMessage) {
        super(detailMessage);
    }
}

