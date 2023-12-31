package com.kumazaku.pdfcreator.utility;

import android.util.Log;

import com.kumazaku.pdfcreator.BuildConfig;

import java.util.regex.Pattern;

/**
 *  ログ出力の支援クラス
 */
public class LogHelper {

    /**
     * デバック用ログを出力する。 本番リリース時は出力されない。
     *
     * @param msg 出力するメッセージ
     */
    public static void d(String msg) {
        if (!BuildConfig.DEBUG) return;
        Log.d(getTag(), msg);
    }

    /**
     * エラー用ログを出力する。 <br>
     * catchの中や想定外の動作でログを出力する場合に使用すること。<br>
     * 本番リリース時も、起きたエラーを解析するために本ログは出力される想定。
     *
     * @param msg 出力するメッセージ
     */
    public static void e(String msg) {
        Log.e(getTag(), msg);
    }

    /**
     *  情報のログを出力する。
     *
     *  @param msg 出力するメッセージ
     */
    public static void i(String msg) {
        Log.i(getTag(), msg);
    }

    /**
     * コールされた関数名、クラス名、行数を出力する
     */
    public static void trace() {
        Log.i(getTag(), "trace");
    }

    /**
     * タグを生成する
     *
     * @return className#methodName:line
     */
    private static String getTag() {
        final StackTraceElement trace = Thread.currentThread().getStackTrace()[4];
        final String cla = trace.getClassName();
        Pattern pattern = Pattern.compile("[\\.]+");
        final String[] splitedStr = pattern.split(cla);
        final String simpleClass = splitedStr[splitedStr.length - 1];
        final String mthd = trace.getMethodName();
        final int line = trace.getLineNumber();
        final String tag = simpleClass + "#" + mthd + ":" + line;
        return tag;
    }

}
