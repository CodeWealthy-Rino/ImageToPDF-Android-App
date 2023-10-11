package com.kumazaku.pdfcreator.exceptions;

/**
 *  重複するファイルが存在した場合にスローされる。
 */
public class DuplicatedFileException extends Exception {
    public DuplicatedFileException() {
    }

    public DuplicatedFileException(String detailMessage) {
        super(detailMessage);
    }
}
