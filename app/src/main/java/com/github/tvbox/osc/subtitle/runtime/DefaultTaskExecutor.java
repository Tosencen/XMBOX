/*
 *                       Copyright (C) of Avery
 *
 *                              _ooOoo_
 *                             o8888888o
 *                             88" . "88
 *                             (| -_- |)
 *                             O\  =  /O
 *                          ____/`- -'\____
 *                        .'  \\|     |//  `.
 *                       /  \\|||  :  |||//  \
 *                      /  _||||| -:- |||||-  \
 *                      |   | \\\  -  /// |   |
 *                      | \_|  ''\- -/''  |   |
 *                      \  .-\__  `-`  ___/-. /
 *                    ___`. .' /- -.- -\  `. . __
 *                 ."" '<  `.___\_<|>_/___.'  >'"".
 *                | | :  `- \`.;`\ _ /`;.`/ - ` : | |
 *                \  \ `-.   \_ __\ /__ _/   .-` /  /
 *           ======`-.____`-.___\_____/___.-`____.-'======
 *                              `=- -='
 *           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 *              Buddha bless, there will never be bug!!!
 */

package com.github.tvbox.osc.subtitle.runtime;

import android.os.Looper;
import androidx.annotation.Nullable;

import com.github.tvbox.osc.util.ThreadPoolManager;

/**
 * @author AveryZhong.
 */

public class DefaultTaskExecutor extends TaskExecutor {

    @Override
    public void executeOnDeskIO(final Runnable task) {
        ThreadPoolManager.executeIO(task);
    }

    @Override
    public void postToMainThread(final Runnable task) {
        ThreadPoolManager.executeMain(task);
    }

    @Override
    public boolean isMainThread() {
        return Thread.currentThread() == Looper.getMainLooper().getThread();
    }
}
